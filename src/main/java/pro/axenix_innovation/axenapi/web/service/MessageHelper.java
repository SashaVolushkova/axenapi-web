package pro.axenix_innovation.axenapi.web.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey;

import java.util.Locale;

/**
 * Хелпер для сообщений интернационализации со статическими и не статическими методами
 * по умолчанию Locale будет английская и сообщения берутся из messages.properties
 * при передаче ru локали будут браться из messages_ru.properties
 */
@RequiredArgsConstructor
@Component
public class MessageHelper {
    private static MessageSource staticMessageSource;

    private final MessageSource messageSource;

    @PostConstruct
    public void init() {
        staticMessageSource = messageSource;
    }

//    Для тестов
    public static void setStaticMessageSource(MessageSource ms) {
        staticMessageSource = ms;
    }

    public String getMessage(AppCodeMessageKey item, Locale locale, Object... args) {
        String formatMessage = messageSource.getMessage(item.getMessageKey(), args, locale);
        return String.format("Code: [%s], message: %s", item.getCode(), formatMessage);
    }

    public String getMessage(AppCodeMessageKey item, Object... args) {
        String formatMessage = messageSource.getMessage(item.getMessageKey(), args, Locale.ENGLISH);
        return String.format("Code: [%s], message: %s", item.getCode(), formatMessage);
    }

    public String getMessage(String messageKey, Locale locale, Object... args) {
        return messageSource.getMessage(messageKey, args, locale);
    }

    public String getMessage(String messageKey, Object... args) {
        return messageSource.getMessage(messageKey, args, Locale.ENGLISH);
    }

    public static String getStaticMessage(AppCodeMessageKey item, Locale locale, Object... args) {
        String formatMessage = staticMessageSource.getMessage(item.getMessageKey(), args, locale);
        return String.format("Code: [%s], message: %s", item.getCode(), formatMessage);
    }

    public static String getStaticMessage(AppCodeMessageKey item, Object... args) {
        String formatMessage = staticMessageSource.getMessage(item.getMessageKey(), args, Locale.ENGLISH);
        return String.format("Code: [%s], message: %s", item.getCode(), formatMessage);
    }

    public static String getStaticMessage(String messageKey, Locale locale, Object... args) {
        return staticMessageSource.getMessage(messageKey, args, locale);
    }

    public static String getStaticMessage(String messageKey, Object... args) {
        return staticMessageSource.getMessage(messageKey, args, Locale.ENGLISH);
    }
}
