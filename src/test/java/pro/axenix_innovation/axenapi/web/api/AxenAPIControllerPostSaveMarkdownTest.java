package pro.axenix_innovation.axenapi.web.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pro.axenix_innovation.axenapi.web.model.SaveMarkdownPost201Response;
import pro.axenix_innovation.axenapi.web.service.MarkdownService;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_OK_MD_FILE_SAVED;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostSaveMarkdownTest {

    private final List<Path> createdFiles = new ArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageHelper messageHelper;

    @MockitoBean
    private MarkdownService markdownService;

    @AfterEach
    public void cleanup() throws IOException {
        for (Path path : createdFiles) {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        }
        createdFiles.clear();
    }

    @Test
    public void testSaveMarkdownPost_Success() throws Exception {
        String markdownText = "# Hello, world";

        String uniqueFileName = "readme_" + UUID.randomUUID() + ".md";
        String relativePath = "markdown/" + uniqueFileName;

        String tempDir = System.getProperty("java.io.tmpdir");
        Path filePath = Paths.get(tempDir, relativePath);
        Path parentDir = filePath.getParent();

        SaveMarkdownPost201Response response = new SaveMarkdownPost201Response();
        response.setMessage(messageHelper.getMessage(RESP_OK_MD_FILE_SAVED.getMessageKey()));
        response.setCode(RESP_OK_MD_FILE_SAVED.getCode());
        response.setFilePath(filePath.toAbsolutePath().toString());

        when(markdownService.saveMarkdownToFile(markdownText, relativePath)).thenAnswer(invocation -> {
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(filePath, markdownText);
            createdFiles.add(filePath);
            return response;
        });

        String requestJson = String.format("""
            {
                "markdown": "%s",
                "filePath": "%s"
            }
            """, markdownText, relativePath);

        mockMvc.perform(post("/saveMarkdown")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(messageHelper.getMessage(RESP_OK_MD_FILE_SAVED.getMessageKey())))
                .andExpect(jsonPath("$.code").value(RESP_OK_MD_FILE_SAVED.getCode()))
                .andExpect(jsonPath("$.filePath").value(filePath.toAbsolutePath().toString()));

        verify(markdownService, times(1)).saveMarkdownToFile(markdownText, relativePath);
    }

    @Test
    public void testSaveMarkdownPost_EmptyMarkdown() throws Exception {
        String markdownText = " ";
        String relativePath = "markdown/readme_" + UUID.randomUUID() + ".md";

        String requestJson = String.format("""
            {
                "markdown": "%s",
                "filePath": "%s"
            }
            """, markdownText, relativePath);

        when(markdownService.saveMarkdownToFile(markdownText, relativePath))
                .thenThrow(new IllegalArgumentException("Markdown content is empty"));

        mockMvc.perform(post("/saveMarkdown")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Markdown content is empty"));

        verify(markdownService, times(1)).saveMarkdownToFile(markdownText, relativePath);
    }

    @Test
    public void testSaveMarkdownPost_InternalServerError() throws Exception {
        String markdownText = "# crash";
        String relativePath = "markdown/readme_" + UUID.randomUUID() + ".md";

        String requestJson = String.format("""
            {
                "markdown": "%s",
                "filePath": "%s"
            }
            """, markdownText, relativePath);

        when(markdownService.saveMarkdownToFile(markdownText, relativePath))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/saveMarkdown")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));

        verify(markdownService, times(1)).saveMarkdownToFile(markdownText, relativePath);
    }
}
