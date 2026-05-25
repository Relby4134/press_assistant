package by.presassistant.application.port.out;

public interface NotificationPort {
    void sendMessage(Long chatId, String text);
    void sendSlideNotification(Long chatId, int slideNumber);
    void sendSlideImage(Long chatId, byte[] imageBytes, int slideNumber);
}
