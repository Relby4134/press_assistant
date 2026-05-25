package by.presassistant.infrastructure.telegram;

import by.presassistant.application.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TelegramNotificationAdapter implements NotificationPort {

    private final TelegramClient telegramClient;
    private final Map<Long, Integer> lastSlideNotificationMsgId = new ConcurrentHashMap<>();

    public TelegramNotificationAdapter(@Lazy TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void sendMessage(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public void sendSlideNotification(Long chatId, int slideNumber) {
        try {
            deletePreviousNotification(chatId);
            var sent = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("👆 Лектор показывает слайд №" + slideNumber)
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboardRow(new InlineKeyboardRow(
                                    InlineKeyboardButton.builder()
                                            .text("📷 Показать слайд")
                                            .callbackData("show_slide")
                                            .build()))
                            .build())
                    .build());
            lastSlideNotificationMsgId.put(chatId, sent.getMessageId());
        } catch (TelegramApiException e) {
            log.error("Failed to send slide notification to {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public void sendSlideImage(Long chatId, byte[] imageBytes, int slideNumber) {
        try {
            telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(new ByteArrayInputStream(imageBytes),
                            "slide_" + slideNumber + ".png"))
                    .caption("Слайд " + slideNumber)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send slide image to {}: {}", chatId, e.getMessage());
            sendMessage(chatId, "📊 Слайд " + slideNumber);
        }
    }

    private void deletePreviousNotification(Long chatId) {
        Integer prevId = lastSlideNotificationMsgId.remove(chatId);
        if (prevId == null) return;
        try {
            telegramClient.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(prevId)
                    .build());
        } catch (Exception ignored) {}
    }
}