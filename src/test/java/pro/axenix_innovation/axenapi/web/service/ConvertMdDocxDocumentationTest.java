package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class ConvertMdDocxDocumentationTest {
    private static final String RESOURCES_PATH = "src/test/resources/MD/documents/";
    private static final String OUTPUT_PATH = "test-output/";

    @Autowired
    private ConvertMdDocxDocumentService converter;

    @BeforeEach
    void setUp() {
        MessageHelper messageHelper = mock(MessageHelper.class);
        converter = new ConvertMdDocxDocumentService(messageHelper);
        when(messageHelper.getMessage(anyString(), any())).thenReturn("some message");
    }

    @Test
    void testConvertMdToDocx() throws Exception {
        List<String> mdDocuments = List.of(
                Files.readString(Paths.get(RESOURCES_PATH + "document1.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document2.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document3.md"), StandardCharsets.UTF_8)
        );

        byte[] docxBytes = converter.convertMdToDocx(mdDocuments);

        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);

        assertEquals('P', (char)docxBytes[0]);
        assertEquals('K', (char)docxBytes[1]);

    }

//    Для просмотра самого файла и ссылок внутри docx - отключить очистку @AfterEach void cleanup()

    @Test
    void testCrossDocumentLinks() throws Exception {
        List<String> mdDocuments = List.of(
                Files.readString(Paths.get(RESOURCES_PATH + "document1.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document2.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document3.md"), StandardCharsets.UTF_8)
        );

        byte[] docxBytes = converter.convertMdToDocx(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.docx"), docxBytes);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);
    }

    @Test
    void testNullCollection() throws Exception {
        byte[] docxBytes = converter.convertMdToDocx(null);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.docx"), docxBytes);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);
    }

    @Test
    void testNullDocument() throws Exception {
        List<String> mdDocuments = new ArrayList<>();
        mdDocuments.add(null);
        byte[] docxBytes = converter.convertMdToDocx(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.docx"), docxBytes);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);
    }

    @Test
    void testEmptyDocument() throws Exception {
        List<String> mdDocuments = new ArrayList<>();
        mdDocuments.add("");
        byte[] docxBytes = converter.convertMdToDocx(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.docx"), docxBytes);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);
    }

    @Test
    void testBlankDocument() throws Exception {
        List<String> mdDocuments = new ArrayList<>();
        mdDocuments.add(" ");
        byte[] docxBytes = converter.convertMdToDocx(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.docx"), docxBytes);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);
    }

    @Test
    void testCreateEmptyDocxIsCalled() {
        MessageHelper messageHelper = mock(MessageHelper.class);
        ConvertMdDocxDocumentService spyService = spy(new ConvertMdDocxDocumentService(messageHelper));

        spyService.convertMdToDocx(null);
        spyService.convertMdToDocx(new ArrayList<>());
        List<String> emptyStringList = new ArrayList<>();
        emptyStringList.add("");
        spyService.convertMdToDocx(emptyStringList);
        emptyStringList.add("   ");
        spyService.convertMdToDocx(emptyStringList);

        verify(spyService, times(4)).createEmptyDocx();
    }

    @AfterEach
    void cleanup() throws IOException {
        Path filePath = Paths.get(OUTPUT_PATH + "documentation.docx");
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
}
