import QRCode from 'qrcode';
/* global Office, QRCode */

const BOT_USERNAME = "asisstant_for_lecture_bot";
const REFRESH_INTERVAL_MS = 10000;

let autoRefreshTimer  = null;
let slidePollingTimer = null;

const state = {
  serverUrl: "https://localhost:8082",
  lectureId: null,
  lectureTitle: null,
  autoSend: true,
  currentSlide: null,
  connected: false,
};

// ── Инициализация ─────────────────────────────────────────────────
Office.onReady(async () => {
  el("btn-start").onclick            = onStart;
  el("btn-disconnect").onclick       = onDisconnect;
  el("btn-send-slide").onclick       = () => captureAndSend(state.currentSlide);
  el("btn-refresh").onclick          = loadQuestions;
  el("btn-refresh-students").onclick  = loadStudents;
  el("btn-refresh-analytics").onclick = loadAnalytics;
  el("btn-broadcast").onclick         = broadcastMessage;
  el("input-message").onkeydown = (e) => { if (e.key === "Enter") broadcastMessage(); };

  el("toggle-auto").onchange = (e) => {
    state.autoSend = e.target.checked;
    el("btn-send-slide").classList.toggle("hidden", state.autoSend);
  };

  document.querySelectorAll(".tab").forEach(tab => {
    tab.onclick = () => switchTab(tab.dataset.tab);
  });
});

// ── Начать лекцию ─────────────────────────────────────────────────
async function onStart() {
  const title = el("input-title").value.trim();

  if (!title) { showError("Введите название лекции"); return; }

  setBtn("btn-start", "Создание...", true);
  hideError();

  try {
    const fileUrl = Office.context.document.url;
    const requireNames = el("mode-named").checked;
    const res = await fetch(
      `${state.serverUrl}/lecture/start?title=${encodeURIComponent(title)}&fileUrl=${encodeURIComponent(fileUrl)}&requireNames=${requireNames}`,
      { method: "POST" }
    );
    if (!res.ok) throw new Error(`Ошибка сервера: ${res.status}`);
    const session = await res.json();

    state.lectureId    = session.id;
    state.lectureTitle = session.title;
    state.connected    = true;

    el("lecture-title").textContent = session.title;
    show("screen-active");
    hide("screen-setup");
    generateQr(session.id);
    switchTab("questions");

    Office.context.document.addHandlerAsync(
      Office.EventType.DocumentSelectionChanged,
      onSlideChanged
    );
    Office.context.document.addHandlerAsync(
      Office.EventType.ActiveViewChanged,
      onViewChanged
    );

    await syncCurrentSlide();
    if (state.currentSlide) await sendToBackend(state.currentSlide);
    await loadQuestions();
    startAutoRefresh();
  } catch (e) {
    showError(e.message);
  } finally {
    setBtn("btn-start", "Начать лекцию", false);
  }
}

// ── Генерация QR-кода ─────────────────────────────────────────────
async function generateQr(lectureId) {
  const deepLink = `https://t.me/${BOT_USERNAME}?start=${lectureId}`;
  el("qr-code").innerHTML = "";
  const canvas = document.createElement("canvas");
  el("qr-code").appendChild(canvas);
  await QRCode.toCanvas(canvas, deepLink, { width: 180, margin: 1 });
  el("qr-link").textContent = deepLink;
}

// ── Завершить лекцию ──────────────────────────────────────────────
async function onDisconnect() {
  if (state.lectureId) {
    try {
      await fetch(`${state.serverUrl}/lecture/${state.lectureId}/end`, { method: "POST" });
    } catch { /* ignore */ }
  }

  stopAutoRefresh();
  state.connected = false;
  state.lectureId = null;

  Office.context.document.removeHandlerAsync(
    Office.EventType.DocumentSelectionChanged,
    { handler: onSlideChanged }
  );
  Office.context.document.removeHandlerAsync(
    Office.EventType.ActiveViewChanged,
    { handler: onViewChanged }
  );
  stopSlidePolling();

  show("screen-setup");
  hide("screen-active");
  el("current-slide").textContent = "—";
  el("questions-list").innerHTML = '<div class="empty">Вопросов пока нет</div>';
  el("students-list").innerHTML = '<div class="empty">Студентов пока нет</div>';
  el("students-count").textContent = "";
  el("students-count").classList.add("hidden");
  el("analytics-list").innerHTML = '<div class="empty">Студенты ещё не запрашивали слайды</div>';
  el("input-message").value = "";
  el("qr-code").innerHTML = "";
  el("qr-link").textContent = "";
  switchTab("questions");
}

// ── Смена слайда ──────────────────────────────────────────────────
async function onSlideChanged() {
  if (!state.connected) return;
  try {
    const slideNumber = await getCurrentSlideNumber();
    if (slideNumber === state.currentSlide) return;
    state.currentSlide = slideNumber;
    el("current-slide").textContent = slideNumber;
    if (state.autoSend) {
      await captureAndSend(slideNumber);
    } else {
      setSyncStatus("Слайд изменён — нажмите «Отправить»", "");
    }
  } catch (e) {
    setSyncStatus("Ошибка при смене слайда", "err");
    console.error(e);
  }
}

// ── Режим показа: polling слайда ──────────────────────────────────

function onViewChanged(args) {
  if (!state.connected) return;
  if (args.activeView === "read") {
    startSlidePolling();
  } else {
    stopSlidePolling();
  }
}

function startSlidePolling() {
  stopSlidePolling();
  slidePollingTimer = setInterval(async () => {
    if (!state.connected) return;
    try {
      const slideNumber = await getCurrentSlideNumber();
      if (slideNumber === state.currentSlide) return;
      state.currentSlide = slideNumber;
      el("current-slide").textContent = slideNumber;
      if (state.autoSend) await captureAndSend(slideNumber);
      else setSyncStatus("Слайд изменён — нажмите «Отправить»", "");
    } catch { /* getSelectedDataAsync может не отвечать в slideshow */ }
  }, 800);
}

function stopSlidePolling() {
  if (slidePollingTimer) {
    clearInterval(slidePollingTimer);
    slidePollingTimer = null;
  }
}

// ── Получить номер текущего слайда ────────────────────────────────
function getCurrentSlideNumber() {
  return new Promise((resolve, reject) => {
    Office.context.document.getSelectedDataAsync(
      Office.CoercionType.SlideRange,
      (result) => {
        if (result.status === Office.AsyncResultStatus.Succeeded) {
          resolve(result.value.slides[0].index);
        } else {
          reject(new Error(result.error.message));
        }
      }
    );
  });
}

// ── Отправка при смене слайда ─────────────────────────────────────
async function captureAndSend(slideNumber) {
  setSyncStatus("Отправка...", "busy");
  try {
    await sendToBackend(slideNumber);
  } catch (e) {
    setSyncStatus("Ошибка отправки", "err");
    console.error(e);
  }
}

// ── Захват текущего слайда как base64 PNG ─────────────────────────
function captureSlideAsBase64() {
  return new Promise((resolve) => {
    Office.context.document.getSelectedDataAsync(
      Office.CoercionType.XmlSvg,
      (result) => {
        if (result.status !== Office.AsyncResultStatus.Succeeded) {
          resolve(null);
          return;
        }
        const svgStr = result.value;
        const blob = new Blob([svgStr], { type: "image/svg+xml;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const img = new Image();
        img.onload = () => {
          const canvas = document.createElement("canvas");
          canvas.width = 960;
          canvas.height = 540;
          const ctx = canvas.getContext("2d");
          ctx.fillStyle = "#fff";
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          URL.revokeObjectURL(url);
          resolve(canvas.toDataURL("image/png").split(",")[1]);
        };
        img.onerror = () => { URL.revokeObjectURL(url); resolve(null); };
        img.src = url;
      }
    );
  });
}

// ── Отправка на бэкенд ────────────────────────────────────────────
async function sendToBackend(slideNumber) {
  // Захватить и загрузить изображение слайда
  const imageBase64 = await captureSlideAsBase64();
  if (imageBase64) {
    try {
      await fetch(`${state.serverUrl}/lecture/${state.lectureId}/slides`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ slideNumber, imageBase64 }),
      });
    } catch { /* изображение опциональное */ }
  }

  // Уведомить студентов и обновить текущий слайд в БД
  const res = await fetch(`${state.serverUrl}/lecture/${state.lectureId}/slide-changed`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ slideNumber }),
  });
  if (!res.ok) throw new Error(`Backend error: ${res.status}`);
  setSyncStatus(`Слайд ${slideNumber} отправлен ✓`, "ok");
  await loadQuestions();
}

// ── Синхронизация текущего слайда при старте ──────────────────────
async function syncCurrentSlide() {
  try {
    const slideNumber = await getCurrentSlideNumber();
    state.currentSlide = slideNumber;
    el("current-slide").textContent = slideNumber;
  } catch {
    el("current-slide").textContent = "?";
  }
}

// ── Загрузить вопросы ─────────────────────────────────────────────
async function loadQuestions() {
  try {
    const res = await fetch(`${state.serverUrl}/students/questions/${state.lectureId}`);
    if (!res.ok) return;
    renderQuestions(await res.json());
  } catch (e) {
    console.error("Failed to load questions", e);
  }
}

function renderQuestions(questions) {
  const list = el("questions-list");
  if (!questions || questions.length === 0) {
    list.innerHTML = '<div class="empty">Вопросов пока нет</div>';
    return;
  }
  list.innerHTML = [...questions].reverse()
    .map(q => `
      <div class="question-item">
        <div class="q-name">${escapeHtml(q.studentName || "Студент")}</div>
        <div class="q-text">${escapeHtml(q.text)}</div>
      </div>`)
    .join("");
}

// ── Автообновление ────────────────────────────────────────────────
// ── Рассылка сообщения студентам ─────────────────────────────────
async function broadcastMessage() {
  const text = el("input-message").value.trim();
  if (!text) return;
  const btn = el("btn-broadcast");
  btn.disabled = true;
  try {
    const res = await fetch(`${state.serverUrl}/students/broadcast`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ lectureId: state.lectureId, message: text }),
    });
    if (!res.ok) throw new Error(`Error: ${res.status}`);
    el("input-message").value = "";
  } catch (e) {
    console.error("Failed to broadcast", e);
  } finally {
    btn.disabled = false;
  }
}

async function loadAnalytics() {
  try {
    const res = await fetch(`${state.serverUrl}/students/analytics/${state.lectureId}`);
    if (!res.ok) return;
    renderAnalytics(await res.json());
  } catch (e) {
    console.error("Failed to load analytics", e);
  }
}

function renderAnalytics(data) {
  const list = el("analytics-list");
  if (!data || data.length === 0) {
    list.innerHTML = '<div class="empty">Студенты ещё не запрашивали слайды</div>';
    return;
  }
  list.innerHTML = data.map(s => {
    const displayName = s.fullName || s.firstName || "Студент";
    const slidesStr = s.slides
      .map(sl => `слайд ${sl.slideNumber}${sl.count > 1 ? ` ×${sl.count}` : ""}`)
      .join(", ");
    return `
      <div class="analytics-item${s.kicked ? " kicked" : ""}">
        <div class="analytics-name">
          ${escapeHtml(displayName)}
          ${s.kicked ? '<span class="kicked-badge">выгнан</span>' : ""}
          <span class="analytics-total">${s.totalRequests}</span>
        </div>
        <div class="analytics-slides">${slidesStr}</div>
      </div>`;
  }).join("");
}

function startAutoRefresh() {
  stopAutoRefresh();
  autoRefreshTimer = setInterval(async () => {
    if (!state.connected) return;
    await loadQuestions();
    await loadStudents();
  }, REFRESH_INTERVAL_MS);
}

function stopAutoRefresh() {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }
}

// ── Загрузить студентов ───────────────────────────────────────────
async function loadStudents() {
  try {
    const res = await fetch(`${state.serverUrl}/students/list/${state.lectureId}`);
    if (!res.ok) return;
    renderStudents(await res.json());
  } catch (e) {
    console.error("Failed to load students", e);
  }
}

function renderStudents(students) {
  const list  = el("students-list");
  const badge = el("students-count");
  if (!students || students.length === 0) {
    list.innerHTML = '<div class="empty">Студентов пока нет</div>';
    badge.textContent = "";
    badge.classList.add("hidden");
    return;
  }
  const activeCount = students.filter(s => !s.kicked).length;
  badge.textContent = activeCount;
  badge.classList.remove("hidden");
  list.innerHTML = students
    .map(s => {
      const displayName = s.fullName || s.firstName || "Студент";
      const subtitle = s.fullName
        ? (s.username ? `@${escapeHtml(s.username)}` : "")
        : "";
      return `
      <div class="student-item${s.kicked ? " kicked" : ""}">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:4px">
          <div>
            <span class="s-name">${escapeHtml(displayName)}</span>
            ${s.kicked ? '<span class="kicked-badge">выгнан</span>' : ""}
            ${subtitle ? `<div class="s-username">${subtitle}</div>` : ""}
          </div>
          ${!s.kicked ? `<button class="btn-kick" onclick="kickStudent(${s.chatId})">Выгнать</button>` : ""}
        </div>
      </div>`;
    })
    .join("");
}

async function kickStudent(chatId) {
  try {
    const res = await fetch(`${state.serverUrl}/students/kick`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ lectureId: state.lectureId, chatId }),
    });
    if (!res.ok) throw new Error(`Error: ${res.status}`);
    await loadStudents();
  } catch (e) {
    console.error("Failed to kick student", e);
  }
}
window.kickStudent = kickStudent;

// ── Переключение вкладок ──────────────────────────────────────────
function switchTab(tabId) {
  document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
  document.querySelectorAll(".tab-content").forEach(c => c.classList.add("hidden"));
  const tabBtn = document.querySelector(`.tab[data-tab="${tabId}"]`);
  if (tabBtn) tabBtn.classList.add("active");
  show(`tab-${tabId}`);
  if (tabId === "students")  loadStudents();
  if (tabId === "analytics") loadAnalytics();
}

// ── Helpers ───────────────────────────────────────────────────────
function el(id)    { return document.getElementById(id); }
function show(id)  { el(id).classList.remove("hidden"); }
function hide(id)  { el(id).classList.add("hidden"); }
function setSyncStatus(t, c) {
  const s = el("sync-status");
  s.textContent = t;
  s.className = "sync-status " + c;
}
function showError(msg) {
  const e = el("setup-error");
  e.textContent = msg;
  e.classList.remove("hidden");
}
function hideError()  { el("setup-error").classList.add("hidden"); }
function setBtn(id, text, disabled) {
  const b = el(id);
  b.textContent = text;
  b.disabled = disabled;
}
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
