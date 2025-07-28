package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;

public class MessageHelperTest {

    private MessageSource mockMessageSource;
    private MessageHelper messageHelper;

    @BeforeEach
    void setUp() {
        mockMessageSource = Mockito.mock(MessageSource.class);
        messageHelper = new MessageHelper(mockMessageSource);
    }

    @Test
    void testGetMessageByEnumWithLocale() {
        Mockito.when(mockMessageSource.getMessage(eq("axenapi.error.fail.prepare.dir"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Failed to prepare directory testDir.");
        String result = messageHelper.getMessage(AppCodeMessageKey.FAIL_TO_PREPARE_DIR, Locale.ENGLISH, "testDir");
        assertEquals("Code: [50014], message: Failed to prepare directory testDir.", result);
    }

    @Test
    void testGetMessageByEnumDefaultLocale() {
        Mockito.when(mockMessageSource.getMessage(eq("axenapi.error.fail.prepare.dir"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Failed to prepare directory abc.");
        String result = messageHelper.getMessage(AppCodeMessageKey.FAIL_TO_PREPARE_DIR, "abc");
        assertEquals("Code: [50014], message: Failed to prepare directory abc.", result);
    }

    @Test
    void testGetMessageByKeyWithLocale() {
        Mockito.when(mockMessageSource.getMessage(eq("axenapi.error.fail.prepare.dir"), any(), eq(new Locale("ru"))))
                .thenReturn("Не удалось подготовить директорию test.");
        String result = messageHelper.getMessage("axenapi.error.fail.prepare.dir", new Locale("ru"), "test");
        assertEquals("Не удалось подготовить директорию test.", result);
    }

    @Test
    void testGetMessageByKeyDefaultLocale() {
        Mockito.when(mockMessageSource.getMessage(eq("axenapi.error.fail.prepare.dir"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Failed to prepare directory test.");
        String result = messageHelper.getMessage("axenapi.error.fail.prepare.dir", "test");
        assertEquals("Failed to prepare directory test.", result);
    }

    @Test
    void testRealMessageSourceEnglishAndRussian() {
        ResourceBundleMessageSource realSource = new ResourceBundleMessageSource();
        realSource.setBasenames("messages");
        realSource.setDefaultEncoding("UTF-8");
        MessageHelper realHelper = new MessageHelper(realSource);

        // English
        String en = realHelper.getMessage("axenapi.error.fail.prepare.dir", Locale.ENGLISH, "testDir");
        assertEquals("Failed to prepare directory testDir.", en);

        // Russian
        String ru = realHelper.getMessage("axenapi.error.fail.prepare.dir", new Locale("ru"), "testDir");
        assertEquals("Не удалось подготовить директорию testDir.", ru);
    }

    @Test
    void testEnumWithRealMessageSource() {
        ResourceBundleMessageSource realSource = new ResourceBundleMessageSource();
        realSource.setBasenames("messages");
        realSource.setDefaultEncoding("UTF-8");
        MessageHelper realHelper = new MessageHelper(realSource);

        String en = realHelper.getMessage(AppCodeMessageKey.FAIL_TO_PREPARE_DIR, Locale.ENGLISH, "abc");
        assertEquals("Code: [50014], message: Failed to prepare directory abc.", en);

        String ru = realHelper.getMessage(AppCodeMessageKey.FAIL_TO_PREPARE_DIR, new Locale("ru"), "abc");
        assertEquals("Code: [50014], message: Не удалось подготовить директорию abc.", ru);
    }

    @Test
    void allKeysInMessagesPropertiesExists() throws IOException {
        Properties en = new Properties();
        Properties ru = new Properties();

        try (InputStream enStream = getClass().getClassLoader().getResourceAsStream("messages.properties");
             InputStream ruStream = getClass().getClassLoader().getResourceAsStream("messages_ru.properties")) {
            en.load(enStream);
            ru.load(ruStream);
        }

        Set<String> enKeys = en.stringPropertyNames();
        Set<String> ruKeys = ru.stringPropertyNames();

        for (String key : enKeys) {
            assertTrue(ruKeys.contains(key), "Ключ отсутствует в messages_ru.properties: " + key);
        }
    }

    @Test
    void testNoEmptyBracesInPlaceholders() throws IOException {
        Properties en = new Properties();
        Properties ru = new Properties();

        try (InputStream enStream = getClass().getClassLoader().getResourceAsStream("messages.properties");
             InputStream ruStream = getClass().getClassLoader().getResourceAsStream("messages_ru.properties")) {
            en.load(enStream);
            ru.load(ruStream);
        }

        for (String key : en.stringPropertyNames()) {
            String value = en.getProperty(key);
            assertNoEmptyBraces(key, value, "messages.properties");
        }
        for (String key : ru.stringPropertyNames()) {
            String value = ru.getProperty(key);
            assertNoEmptyBraces(key, value, "messages_ru.properties");
        }
    }

    private void assertNoEmptyBraces(String key, String value, String fileName) {
        if (value != null && value.contains("{}")) {
            assertTrue(false, "В файле " + fileName + " для ключа '" + key + "' найден недопустимый плейсхолдер {}. Используйте {0}, {1} и т.д.");
        }
    }
}