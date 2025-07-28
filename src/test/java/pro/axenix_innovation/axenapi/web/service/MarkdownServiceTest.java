package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import pro.axenix_innovation.axenapi.web.model.SaveMarkdownPost201Response;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_OK_MD_FILE_SAVED;

@SpringBootTest
@AutoConfigureMockMvc
class MarkdownServiceTest {

    @Autowired
    private MarkdownService markdownService;

    @Autowired
    private MessageHelper messageHelper;

    private String tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = "test-output";
        Files.createDirectories(Paths.get(tempDir));
    }

    @AfterEach
    void cleanUp() throws Exception {
        Files.walk(Paths.get(tempDir))
                .filter(p -> p.getFileName().toString().startsWith("test_md_") && p.getFileName().toString().endsWith(".md"))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
    }

    @Test
    void testSaveMarkdownToFile_success() throws Exception {
        String content = "# Test markdown";
        String relPath = "test_md_" + UUID.randomUUID() + ".md";
        SaveMarkdownPost201Response resp = markdownService.saveMarkdownToFile(content, relPath);
        assertNotNull(resp);
        assertEquals(messageHelper.getMessage(RESP_OK_MD_FILE_SAVED.getMessageKey()), resp.getMessage());
        assertEquals(RESP_OK_MD_FILE_SAVED.getCode(), resp.getCode());
        assertTrue(resp.getFilePath().endsWith(relPath) || resp.getFilePath().matches(".*test_md_.*\\.md"));
        assertTrue(new File(resp.getFilePath()).exists());
        assertEquals(content, Files.readString(Path.of(resp.getFilePath())));
    }

    @Test
    void testSaveMarkdownToFile_emptyMarkdown() {
        String relPath = "test_md_" + UUID.randomUUID() + ".md";
        assertThrows(IllegalArgumentException.class, () ->
                markdownService.saveMarkdownToFile("   ", relPath)
        );
    }

    @Test
    void testSaveMarkdownToFile_emptyPath() {
        assertThrows(IllegalArgumentException.class, () ->
                markdownService.saveMarkdownToFile("# test", "   ")
        );
    }

    @Test
    void testSaveMarkdownToFile_fileAlreadyExists_createsNewFile() throws Exception {
        String content1 = "# First";
        String content2 = "# Second";
        String relPath = "test_md_" + UUID.randomUUID() + ".md";
        SaveMarkdownPost201Response resp1 = markdownService.saveMarkdownToFile(content1, relPath);
        SaveMarkdownPost201Response resp2 = markdownService.saveMarkdownToFile(content2, relPath);
        assertNotEquals(resp1.getFilePath(), resp2.getFilePath());
        assertTrue(resp2.getFilePath().matches(".*_\\d+\\.md"));
        assertEquals(content2, Files.readString(Path.of(resp2.getFilePath())));
    }
}
