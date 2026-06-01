# Архитектура системы — Ассистент лектора

## Обзор компонентов

```
┌─────────────────────────────────────────────────────────────┐
│                        Компьютер лектора                    │
│                                                             │
│  ┌──────────────┐     ┌────────────────────────────────┐   │
│  │  PresAssistant│     │         PowerPoint             │   │
│  │  Launcher    │────►│   Office Add-in (JavaScript)   │   │
│  │  (Java Swing)│     │   https://localhost:8082/addin  │   │
│  └──────┬───────┘     └──────────────┬─────────────────┘   │
│         │                            │ REST / HTTPS         │
│         ▼                            ▼                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            Spring Boot Server (:8082)               │   │
│  │                                                     │   │
│  │  LectureController  StudentController               │   │
│  │  LectureService     StudentService                  │   │
│  │  TelegramNotificationAdapter                        │   │
│  │  InMemorySlideStorage  H2 Database                  │   │
│  └─────────────────────────┬───────────────────────────┘   │
└────────────────────────────┼────────────────────────────────┘
                             │ Telegram Bot API (HTTPS)
                             ▼
                    ┌─────────────────┐
                    │  Telegram Cloud │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │    Студент      │
                    │  (Telegram App) │
                    └─────────────────┘
```

---

## Модули

### Лаунчер (`LauncherApp.java`)

Java Swing приложение, упакованное через jpackage в MSI-установщик.

**Отвечает за:**
- Управление жизненным циклом Spring Boot сервера (`presassistant.jar`)
- Хранение конфигурации (`~/.presassistant/config.properties`)
- Установку SSL-сертификата в Windows Certificate Store
- Регистрацию надстройки в реестре Office

### Сервер (`PresAssistantApplication`)

Spring Boot приложение на Java 21 с архитектурой Ports & Adapters (Hexagonal).

```
src/main/java/by/presassistant/
├── application/
│   ├── command/        ← входящие команды (CQRS)
│   ├── port/in/        ← use case интерфейсы
│   ├── port/out/       ← репозиторий/уведомления интерфейсы
│   └── service/        ← бизнес-логика
├── domain/
│   ├── model/          ← LectureSession, Student, Question
│   └── event/          ← доменные события
└── infrastructure/
    ├── config/         ← CorsConfig, WebSocketConfig
    ├── persistence/    ← JPA адаптеры, H2
    ├── telegram/       ← LecturerBot, TelegramNotificationAdapter
    ├── web/            ← REST контроллеры
    └── websocket/      ← WebSocket адаптер
```

**REST API:**

| Метод | Путь | Назначение |
|-------|------|-----------|
| POST | `/lecture/start` | Создать сессию лекции |
| POST | `/lecture/{id}/end` | Завершить лекцию |
| POST | `/lecture/{id}/slide-changed` | Уведомить о смене слайда |
| POST | `/lecture/{id}/slides` | Загрузить изображение слайда |
| GET | `/students/questions/{lectureId}` | Список вопросов |
| DELETE | `/students/questions/{id}` | Удалить вопрос |
| GET | `/students/list/{lectureId}` | Список студентов |
| POST | `/students/kick` | Отключить студента |
| POST | `/students/broadcast` | Рассылка сообщения |
| GET | `/students/analytics/{lectureId}` | Аналитика запросов |

### Надстройка PowerPoint (`pres-assistant-addin/`)

Office Web Add-in на JavaScript, собирается через webpack 5.

**Ключевые части:**
- `taskpane.js` — основная логика: запуск лекции, смена слайда, QR-код, вопросы
- `manifest.xml` — конфигурация надстройки для Office

**Взаимодействие с PowerPoint:**
- `Office.context.document.addHandlerAsync(DocumentSelectionChanged)` — отслеживание смены слайда
- `Office.context.document.getSelectedDataAsync(SlideRange)` — получение номера слайда
- `Office.context.document.getSelectedDataAsync(XmlSvg)` — захват слайда как SVG → PNG

---

## SSL и безопасность

Office Add-in требует HTTPS. Схема:

1. При сборке генерируется `keystore.p12` (самоподписанный, CN=localhost, SAN=DNS:localhost/IP:127.0.0.1)
2. Тот же сертификат экспортируется как `presassistant-ca.crt`
3. При установке `presassistant-ca.crt` добавляется в `LocalMachine\Root`
4. WebView2 (движок Office) доверяет `localhost:8082` через системный хранилище

---

## База данных

H2 embedded, хранится в `%USERPROFILE%\.presassistant\data\presassistant`.

Миграции управляются через Flyway (`src/main/resources/db/migration/`).

**Основные таблицы:**
- `lecture_session` — сессии лекций
- `student` — подключённые студенты (chat_id, имя, статус)
- `question` — вопросы студентов

---

## Сборка

```bash
# Полная сборка MSI (требует WiX 3.11)
./gradlew packageApp

# Только сборка сервера
./gradlew bootJar

# Только сборка надстройки
cd pres-assistant-addin && npm run build
```
