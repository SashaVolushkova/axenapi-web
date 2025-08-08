package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.model.EventDTO;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Default implementation of EventGraphBuilder.
 */
public class DefaultEventGraphBuilder implements EventGraphBuilder {
    @Override
    public EventGraphFacade build(OpenAPI openAPI, UUID serviceNodeId) throws OpenAPISpecParseException {
        if (openAPI == null) {
            throw new OpenAPISpecParseException("Failed to parse OpenAPI specification");
        }
        
        io.swagger.v3.oas.models.info.Info info = openAPI.getInfo();
        if (info == null || info.getTitle() == null || info.getTitle().trim().isEmpty()) {
            throw new OpenAPISpecParseException("Service name is missing in specification");
        }
        
        EventGraphFacade eventGraph = new EventGraphFacade(new EventGraphDTO());
        
        Components components = openAPI.getComponents();
        String title = info.getTitle();
        Map<String, NodeDTO.BrokerTypeEnum> brokers = new HashMap<>();
        Map<String, String> consumerGroup = new HashMap<>();
        Map<String, Set<String>> topicTags = new HashMap<>();
        Set<String> allTags = new HashSet<>();
        
        List<String> serviceDocumentationLinks = null;
        if (openAPI.getExtensions() != null) {
            Object docs = openAPI.getExtensions().get("x-documentation-file-links");
            if (docs instanceof List<?>) {
                serviceDocumentationLinks = ((List<?>) docs).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }
        }
        
        String description = info.getDescription();
        UUID serviceNodeUUID = OpenAPIProcessor.createServiceNode(eventGraph, title, description, serviceDocumentationLinks, serviceNodeId);
        Map<String, EventDTO> createdEvents = new HashMap<>();
        
        if (openAPI.getPaths() != null) {
            OpenAPIProcessor.processPaths(openAPI.getPaths(), eventGraph, serviceNodeUUID, components,
                    createdEvents, brokers, consumerGroup, topicTags, allTags);
        }
        
        eventGraph.setName(title);
        
        if (components != null && components.getSchemas() != null) {
            OpenAPIProcessor.processSchemas(components.getSchemas(), eventGraph, serviceNodeUUID,
                    brokers, consumerGroup, topicTags, allTags, createdEvents);
        }
        
        if (openAPI.getPaths() != null) {
            OpenAPIProcessor.processPathLinks(openAPI.getPaths(), eventGraph, serviceNodeUUID, brokers, consumerGroup, topicTags);
        }
        
        eventGraph.addAllTagsInGraph(allTags);
        
        // Пост-обработка для установки usageContext событий
        OpenAPIProcessor.postProcessUsageContext(eventGraph);
        
        return eventGraph;
    }
}
