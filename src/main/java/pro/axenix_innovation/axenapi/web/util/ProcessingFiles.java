package pro.axenix_innovation.axenapi.web.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.io.IOException;
import java.util.List;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_MERGED_GRAPH_NO_EVENTS;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_PARSE_JSON_NO_EVENTS;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_PARSE_OPEN_API_NO_EVENTS;
import static pro.axenix_innovation.axenapi.web.graph.EventGraphFacade.merge;

public class ProcessingFiles {

    private static final Logger log = LoggerFactory.getLogger(ProcessingFiles.class);

    public static EventGraphDTO processFiles(List<MultipartFile> files) throws OpenAPISpecParseException {
        EventGraphDTO result = new EventGraphDTO();

        for (MultipartFile multipartFile : files) {
            try {
                String fileName = multipartFile.getOriginalFilename();
                String fileContent = new String(multipartFile.getBytes());

                ObjectMapper mapper;
                if (fileName != null && (fileName.endsWith(".yaml") || fileName.endsWith(".yml"))) {
                    mapper = new ObjectMapper(new YAMLFactory());
                } else {
                    mapper = new ObjectMapper();
                }

                JsonNode rootNode = mapper.readTree(fileContent);

                EventGraphDTO eventGraph;
                if (rootNode.has("openapi")) {
                    EventGraphFacade eventGraphFacade = SolidOpenAPITranslator.parseOPenAPI(fileContent);
                    eventGraph = eventGraphFacade != null ? eventGraphFacade.eventGraph() : null;
                    log.info(MessageHelper.getStaticMessage("axenapi.info.parse.open.api.event"));
                    if (eventGraph != null && eventGraph.getEvents() != null) {
                        eventGraph.getEvents().forEach(event ->
                                log.info(MessageHelper.getStaticMessage("axenapi.info.event.name.id", event.getName(), event.getId()))
                        );
                    } else {
                        log.warn(MessageHelper.getStaticMessage(WARN_PARSE_OPEN_API_NO_EVENTS));
                    }
                } else {
                    eventGraph = mapper.treeToValue(rootNode, EventGraphDTO.class);
                    log.info(MessageHelper.getStaticMessage("axenapi.info.parse.json.event"));
                    if (eventGraph.getEvents() != null) {
                        eventGraph.getEvents().forEach(event ->
                                log.info(MessageHelper.getStaticMessage("axenapi.info.event.name.id", event.getName(), event.getId()))
                        );
                    } else {
                        log.warn(MessageHelper.getStaticMessage(WARN_PARSE_JSON_NO_EVENTS));
                    }
                }

                if (eventGraph == null) {
                    throw new OpenAPISpecParseException(fileName, "Failed to parse OpenAPI specification. Please check the file content and try again.");
                }

                result = merge(eventGraph, result);

                log.info(MessageHelper.getStaticMessage("axenapi.info.after.merge.all.events"));
                if (result.getEvents() != null) {
                    result.getEvents().forEach(event ->
                            log.info(MessageHelper.getStaticMessage("axenapi.info.event.name.id", event.getName(), event.getId()))
                    );
                } else {
                    log.warn(MessageHelper.getStaticMessage(WARN_MERGED_GRAPH_NO_EVENTS));
                }

            } catch (OpenAPISpecParseException e) {
                throw new OpenAPISpecParseException(multipartFile.getOriginalFilename(), "Failed to parse OpenAPI specification for file: " + multipartFile.getOriginalFilename());
            } catch (IOException e) {
                throw new RuntimeException("Error reading file: " + multipartFile.getOriginalFilename(), e);
            }
        }
        return result;
    }

    public static String getFileContent(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is null, empty or its input stream is unavailable: " + (file != null ? file.getOriginalFilename() : "unknown"));
            }
            return new String(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error processing file: " + file.getOriginalFilename(), e);
        }
    }

    public static EventGraphDTO processFile(MultipartFile file, ObjectMapper objectMapper) throws OpenAPISpecParseException {
        try {
            log.info(MessageHelper.getStaticMessage("axenapi.info.process.file", file.getOriginalFilename()));
            String fileContent = getFileContent(file);
            JsonNode rootNode = objectMapper.readTree(fileContent);
            if (rootNode.has("openapi")) {
                EventGraphFacade eventGraphFacade = SolidOpenAPITranslator.parseOPenAPI(fileContent);
                return eventGraphFacade != null ? eventGraphFacade.eventGraph() : null;
            } else {
                log.error(MessageHelper.getStaticMessage(AppCodeMessageKey.ERROR_PARSE_OPEN_API_SPEC_FAIL));
                throw new OpenAPISpecParseException(file.getOriginalFilename(), "Failed to parse OpenAPI specification for file: " + file.getOriginalFilename() + "is not an openapi");
            }

        } catch (JsonProcessingException e) {
            throw new OpenAPISpecParseException(file.getOriginalFilename(), "Failed to parse OpenAPI specification for file: " + file.getOriginalFilename());
        }
    }
}
