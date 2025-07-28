package pro.axenix_innovation.axenapi.web.generate;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class SpecificationFileHandler implements SpecificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpecificationFileHandler.class);

    @Value("${generator.folder:src/main/resources/specs}")
    private String folder;

    @Override
    public Map<String, String> handle(Map<String, OpenAPI> openAPIMap, String format) {
        Map<String, String> errors = new HashMap<>();
        Map<String, String> downloadLinks = new HashMap<>();

        try {
            prepareDirectory(folder);
        } catch (IOException e) {
            String msg = "Error creating directory: " + folder;
            logger.error(msg, e);
            errors.put("directory_error", msg);
            return errors;
        }

        // Determine file extension based on format parameter
        String fileExtension = "json".equalsIgnoreCase(format) ? "json" : format;

        // write in each file each specification from the map.
        openAPIMap.forEach((key, value) -> {
            // key - the name of file with specified extension
            String fileName = folder + "/" + key.replaceAll("\\s+", "_") + "." + fileExtension;
            try {
                // map value (OpenAPI) to json or json-string
                String jsonValue = Json.pretty(value);
                // write json into file
                Files.writeString(Path.of(fileName), jsonValue);
                downloadLinks.put(key, fileName);
            } catch (IOException e) {
                logger.error("Error writing JSON to file: " + fileName, e);
                errors.put("file_write_error", e.getMessage());
            }
        });
        logger.debug("Generated download links: {}", downloadLinks);
        return errors.isEmpty() ? downloadLinks : errors;
    }

    void prepareDirectory(String directoryPath) throws IOException {
        Path path = Path.of(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Directory '{}' created successfully.", directoryPath);
        }
    }
}