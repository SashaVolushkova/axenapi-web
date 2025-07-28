package pro.axenix_innovation.axenapi.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.model.SaveMarkdownPost201Response;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_OK_MD_FILE_SAVED;

@Service
@RequiredArgsConstructor
public class MarkdownService {
    private final MessageHelper messageHelper;

    public SaveMarkdownPost201Response saveMarkdownToFile(String markdownText, String relativeFilePath) {

        if (markdownText == null || markdownText.trim().isEmpty()) {
            throw new IllegalArgumentException("Markdown content is empty");
        }
        if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path is empty");
        }

        try {
            String baseTempDir = System.getProperty("java.io.tmpdir");
            Path targetPath = Paths.get(baseTempDir, relativeFilePath);
            File parentDir = targetPath.getParent().toFile();

            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            String baseName = targetPath.getFileName().toString();
            String nameWithoutExt = baseName.replaceFirst("\\.md$", "");
            int index = 1;
            File newFile = targetPath.toFile();

            while (newFile.exists()) {
                newFile = new File(parentDir, nameWithoutExt + "_" + index + ".md");
                index++;
            }

            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(markdownText);
            }

            SaveMarkdownPost201Response response = new SaveMarkdownPost201Response();
            response.setMessage(messageHelper.getMessage(RESP_OK_MD_FILE_SAVED.getMessageKey()));
            response.setFilePath(newFile.getAbsolutePath());
            response.setCode(RESP_OK_MD_FILE_SAVED.getCode());
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save Markdown file: " + e.getMessage(), e);
        }
    }
}