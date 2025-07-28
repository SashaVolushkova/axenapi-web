package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConvertMdPdfDocumentTest {
    private static final String RESOURCES_PATH = "src/test/resources/MD/documents/";
    private static final String OUTPUT_PATH = "test-output/";

    private ConvertMdPdfDocumentService converter;

    @BeforeEach
    void setUp() {
        MessageHelper messageHelper = mock(MessageHelper.class);
        converter = new ConvertMdPdfDocumentService(messageHelper);
        when(messageHelper.getMessage(anyString(), any())).thenReturn("some message");
    }

    @Test
    void testConvertMdToPdf() throws Exception {
        List<String> mdDocuments = List.of(
                Files.readString(Paths.get(RESOURCES_PATH + "document1.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document2.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document3.md"), StandardCharsets.UTF_8)
        );

        byte[] pdfBytes = converter.convertMdToPdf(mdDocuments);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        assertEquals('%', (char)pdfBytes[0]);
        assertEquals('P', (char)pdfBytes[1]);
        assertEquals('D', (char)pdfBytes[2]);
        assertEquals('F', (char)pdfBytes[3]);
    }

//    Для просмотра самого файла и ссылок внутри pdf - отключить очистку @AfterEach void cleanup()

    @Test
    void testCrossDocumentLinks() throws Exception {
        List<String> mdDocuments = List.of(
                Files.readString(Paths.get(RESOURCES_PATH + "document1.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document2.md"), StandardCharsets.UTF_8),
                Files.readString(Paths.get(RESOURCES_PATH + "document3.md"), StandardCharsets.UTF_8)
        );

        byte[] pdfBytes = converter.convertMdToPdf(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.pdf"), pdfBytes);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testEmptyCollection() throws Exception {
        byte[] pdfBytes = converter.convertMdToPdf(new ArrayList<>());

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.pdf"), pdfBytes);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testNullCollection() throws Exception {
        byte[] pdfBytes = converter.convertMdToPdf(null);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.pdf"), pdfBytes);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testNullDocument() throws Exception {
        List<String> mdDocuments = new ArrayList<>();
        mdDocuments.add(null);
        byte[] pdfBytes = converter.convertMdToPdf(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.pdf"), pdfBytes);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testEmptyDocument() throws Exception {
        List<String> mdDocuments = new ArrayList<>();
        mdDocuments.add("");
        byte[] pdfBytes = converter.convertMdToPdf(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.pdf"), pdfBytes);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testBlankDocument() throws Exception {
        List<String> mdDocuments = new ArrayList<>();
        mdDocuments.add(" ");
        byte[] pdfBytes = converter.convertMdToPdf(mdDocuments);

        Files.createDirectories(Paths.get(OUTPUT_PATH));
        Files.write(Paths.get(OUTPUT_PATH + "documentation.pdf"), pdfBytes);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @AfterEach
    void cleanup() throws IOException {
        Path filePath = Paths.get(OUTPUT_PATH + "documentation.pdf");
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
}
