package pro.axenix_innovation.axenapi.web.generate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.axenix_innovation.axenapi.web.entity.Specification;
import pro.axenix_innovation.axenapi.web.repository.SpecificationRepository;

import javax.sql.rowset.serial.SerialClob;
import java.sql.Clob;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class SpecificationDbHandler implements SpecificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpecificationDbHandler.class);

    private final SpecificationRepository specificationRepository;
    private final ObjectMapper yamlMapper;

    public SpecificationDbHandler(SpecificationRepository specificationRepository) {
        this.specificationRepository = specificationRepository;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public Map<String, String> handle(Map<String, OpenAPI> openAPIMap, String format) {
        if (openAPIMap == null || openAPIMap.isEmpty()) {
            logger.warn("Received empty or null OpenAPI map");
            return Map.of("error", "No OpenAPI specifications provided");
        }

        Map<String, String> result = new HashMap<>();

        openAPIMap.forEach((graphName, openAPI) -> {
            try {
                String specContent = generateContent(openAPI, format);
                Clob specClob = new SerialClob(specContent.toCharArray());

                Specification specification = new Specification();
                specification.setGraphName(graphName);
                specification.setSpecFile(specClob);

                Specification saved = specificationRepository.save(specification);

                String fileExtension = isYamlFormat(format) ? "yaml" : "json";
                String downloadLink = "/download/specs/" + saved.getId() + "." + fileExtension;

                result.put(graphName, downloadLink);

            } catch (Exception e) {
                String errorMessage = "Failed to process graph '" + graphName + "': " + e.getMessage();
                logger.error(errorMessage, e);
                result.put(graphName, errorMessage);
            }
        });

        logger.debug("Result of spec handling: {}", result);
        return result;
    }

    private String generateContent(OpenAPI openAPI, String format) throws Exception {
        if (openAPI == null) {
            throw new IllegalArgumentException("OpenAPI specification is null");
        }

        return isYamlFormat(format)
                ? yamlMapper.writeValueAsString(openAPI)
                : Json.pretty(openAPI);
    }

    private boolean isYamlFormat(String format) {
        return "yaml".equalsIgnoreCase(format) || "yml".equalsIgnoreCase(format);
    }

    @Scheduled(fixedRate = 1 * 60 * 1000)
    @Transactional
    public void cleanTable() {
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        specificationRepository.deleteAllCreatedBefore(fiveMinutesAgo);
    }
}