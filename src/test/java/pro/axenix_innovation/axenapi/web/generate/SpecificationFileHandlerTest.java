package pro.axenix_innovation.axenapi.web.generate;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class SpecificationFileHandlerTest {

    private SpecificationFileHandler handler;
    private final String testFolder = "build/test-specs";

    @BeforeEach
    void setUp() throws IOException {
        handler = new SpecificationFileHandler();
        ReflectionTestUtils.setField(handler, "folder", testFolder);

        Path folderPath = Path.of(testFolder);
        if (Files.exists(folderPath)) {
            Files.walk(folderPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        }
    }

    @Test
    void shouldCreateDirectoryIfNotExists() throws IOException {
        Path path = Path.of(testFolder);
        assertFalse(Files.exists(path));

        handler.prepareDirectory(testFolder);
        assertTrue(Files.exists(path));
        assertTrue(Files.isDirectory(path));
    }

    @Test
    void shouldReturnEmptyMapForEmptyOpenAPIInput() {
        Map<String, String> result = handler.handle(Collections.emptyMap(), "json");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnErrorsIfFolderCannotBeCreated() throws IOException {
        SpecificationFileHandler spyHandler = spy(new SpecificationFileHandler());
        ReflectionTestUtils.setField(spyHandler, "folder", "any/path");

        doThrow(new IOException("No permission")).when(spyHandler).prepareDirectory("any/path");

        Map<String, OpenAPI> input = new HashMap<>();
        input.put("test", new OpenAPI());

        Map<String, String> result = spyHandler.handle(input, "json");

        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("directory_error"));
        assertTrue(result.get("directory_error").contains("Error creating directory"));
    }

    @Test
    void shouldWriteJsonFileAndReturnDownloadLink() {
        OpenAPI openAPI = new OpenAPI();
        Map<String, OpenAPI> input = new HashMap<>();
        input.put("testService", openAPI);

        Map<String, String> result = handler.handle(input, "json");

        assertEquals(1, result.size());
        assertTrue(result.containsKey("testService"));

        Path filePath = Path.of(result.get("testService"));
        assertTrue(Files.exists(filePath));
        assertTrue(filePath.toString().endsWith("testService.json"));
    }
}
