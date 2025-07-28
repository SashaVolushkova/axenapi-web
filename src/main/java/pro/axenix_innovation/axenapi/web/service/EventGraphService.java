package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pro.axenix_innovation.axenapi.web.api.AxenAPIController;
import pro.axenix_innovation.axenapi.web.exception.NotServiceNode;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.ErrorDTO;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.model.UpdateServiceSpecificationPostRequest;
import pro.axenix_innovation.axenapi.web.util.OpenAPIParser;
import pro.axenix_innovation.axenapi.web.util.SolidOpenAPITranslator;
import pro.axenix_innovation.axenapi.web.util.ProcessingFiles;
import pro.axenix_innovation.axenapi.web.validate.OpenApiValidator;

import java.util.*;
import java.util.stream.Collectors;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_UNKNOWN_ERROR_PROC_FILE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_UNKNOWN_ERROR_PROC_SPEC;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_ALL_ITEMS_HAD_ERRORS;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_INVALID_JSON_FORMAT_SPEC;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_INVALID_OPEN_API_FORMAT_TITLE;
import static pro.axenix_innovation.axenapi.web.graph.EventGraphFacade.merge;

@Service
public class EventGraphService {

    private static final Logger log = LoggerFactory.getLogger(AxenAPIController.class);

    public static EventGraphDTO addServiceToGraph(List<MultipartFile> files, EventGraphDTO eventGraph) {
        EventGraphDTO updatedGraph = initGraph(eventGraph);
        List<ErrorDTO> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (file.isEmpty()) {
                    errors.add(new ErrorDTO("File: " + file.getOriginalFilename(), "File is empty"));
                    continue;
                }

                if (!OpenApiValidator.validateOpenApiSpec(file)) {
                    errors.add(new ErrorDTO("File: " + file.getOriginalFilename(), "Invalid OpenAPI format"));
                    continue;
                }

                ObjectMapper objectMapper = getObjectMapperByFilename(file.getOriginalFilename());

                EventGraphDTO newGraph = ProcessingFiles.processFile(file, objectMapper);
                if (newGraph == null) {
                    continue;
                }
                updatedGraph = addGraphToService(updatedGraph, newGraph, errors);
            } catch (IllegalArgumentException e) {
                errors.add(new ErrorDTO("File: " + file.getOriginalFilename(),
                        "Invalid OpenAPI format"));
                log.warn(MessageHelper.getStaticMessage(WARN_INVALID_OPEN_API_FORMAT_TITLE, file.getOriginalFilename()), e);
            } catch (Exception e) {
                errors.add(new ErrorDTO("File: " + file.getOriginalFilename(),
                        "Unknown error occurred"));
                log.error(MessageHelper.getStaticMessage(ERROR_UNKNOWN_ERROR_PROC_FILE, file.getOriginalFilename()), e);
            }
        }

        return handleResult(updatedGraph, errors, files.size(), eventGraph);
    }

    private static EventGraphDTO addGraphToService(EventGraphDTO oldGraph, EventGraphDTO newGraph, List<ErrorDTO> errors) {
        String serviceName = newGraph.getName();

        if (serviceName == null || serviceName.isEmpty()) {
            errors.add(new ErrorDTO("File: some_file", "Service name is missing"));
            return null;
        }

        boolean serviceExists = oldGraph.getNodes() != null && oldGraph.getNodes().stream()
                .anyMatch(node -> NodeDTO.TypeEnum.SERVICE.equals(node.getType()) && serviceName.equals(node.getName()));

        if (serviceExists) {
            log.info(MessageHelper.getStaticMessage("axenapi.info.updating.exist.service", serviceName));
            logGraphState("Before removeServiceByName", oldGraph);
            oldGraph = removeServiceByName(oldGraph, serviceName);
            logGraphState("After removeServiceByName", oldGraph);
        }

        return mergeGraphs(oldGraph, newGraph, "File: " + serviceName);
    }

    public static EventGraphDTO updateServiceSpecification(UpdateServiceSpecificationPostRequest request) throws OpenAPISpecParseException, NotServiceNode {
        if (request.getSpecification() == null || request.getSpecification().trim().isEmpty()) {
            throw new OpenAPISpecParseException("Specification is empty");
        }
        EventGraphDTO updatedGraph = initGraph(request.getEventGraph());
        List<ErrorDTO> errors = new ArrayList<>();
        EventGraphFacade graphFacade = new EventGraphFacade(updatedGraph);
        NodeDTO nodeById = graphFacade.getNodeById(request.getServiceNodeId());
        if(nodeById == null || nodeById.getType() != NodeDTO.TypeEnum.SERVICE) {
            throw new NotServiceNode(request.getServiceNodeId());
        }
        EventGraphFacade eventGraphFacade = SolidOpenAPITranslator.parseOPenAPI(request.getSpecification(), request.getServiceNodeId());
        if (eventGraphFacade == null || eventGraphFacade.eventGraph() == null || eventGraphFacade.eventGraph().getName() == null || eventGraphFacade.eventGraph().getName().trim().isEmpty()) {
            throw new OpenAPISpecParseException("Service name is missing in specification");
        }
        updatedGraph = removeService(updatedGraph, request.getServiceNodeId(), "");
        updatedGraph = filterUnusedEvents(updatedGraph);
        EventGraphDTO result = addGraphToService(updatedGraph, eventGraphFacade.eventGraph(), errors);
        return handleResult(result, errors, 1, request.getEventGraph());
    }

    private static EventGraphDTO filterUnusedEvents(EventGraphDTO graph) {
        if (graph == null || graph.getEvents() == null || graph.getLinks() == null) {
            return graph;
        }
        Set<UUID> usedEventIds = graph.getLinks().stream()
                .map(LinkDTO::getEventId)
                .collect(Collectors.toSet());
        List<EventDTO> filteredEvents = graph.getEvents().stream()
                .filter(event -> usedEventIds.contains(event.getId()))
                .collect(Collectors.toList());
        graph.setEvents(filteredEvents);
        return graph;
    }


    private static EventGraphDTO initGraph(EventGraphDTO existingGraph) {
        return existingGraph != null ? existingGraph : new EventGraphDTO();
    }

    private static EventGraphDTO handleResult(EventGraphDTO updatedGraph, List<ErrorDTO> errors,
                                              int totalItems, EventGraphDTO originalGraph) {
        if (errors.size() == totalItems) {
            log.warn(MessageHelper.getStaticMessage(WARN_ALL_ITEMS_HAD_ERRORS));
            originalGraph.setErrors(errors);
            return originalGraph;
        }

        if (!errors.isEmpty()) {
            updatedGraph.setErrors(errors);
        }

        return updatedGraph;
    }

    private static ObjectMapper getObjectMapperByFilename(String filename) {
        if (filename != null && (filename.endsWith(".yaml") || filename.endsWith(".yml"))) {
            return new ObjectMapper(new YAMLFactory());
        } else {
            return new ObjectMapper();
        }
    }

    private static EventGraphDTO removeServiceById(EventGraphDTO graph, UUID serviceNodeId) {
        logGraphState("Before removeServiceById", graph);
        EventGraphDTO eventGraphDTO = removeService(graph,
                serviceNodeId,
                "service ID '" + serviceNodeId + "'");
        logGraphState("After removeServiceById", graph);

        return eventGraphDTO;
    }

    private static EventGraphDTO removeServiceByName(EventGraphDTO graph, String serviceName) {
        logGraphState("Before removeServiceByName", graph);
        UUID serviceUUID = Objects.requireNonNull(graph.getNodes().stream()
                .filter(node -> NodeDTO.TypeEnum.SERVICE.equals(node.getType())
                        && serviceName.equals(node.getName())).findFirst().orElse(null)).getId();
        EventGraphDTO eventGraphDTO = removeService(graph,
                serviceUUID,
                "service '" + serviceName + "'");
        logGraphState("After removeServiceByName", graph);

        return eventGraphDTO;
    }

    private static EventGraphDTO removeService(EventGraphDTO graph,
                                               UUID serviceUUID,
                                               String serviceDescription) {
        EventGraphDTO newGraph = new EventGraphDTO();
        newGraph.setName(graph.getName());
        newGraph.setTags(graph.getTags() != null ? new HashSet<>(graph.getTags()) : null);
        newGraph.setErrors(new ArrayList<>());

        List<NodeDTO> nodes = graph.getNodes() != null ? graph.getNodes() : new ArrayList<>();
        List<LinkDTO> links = graph.getLinks() != null ? graph.getLinks() : new ArrayList<>();
        List<EventDTO> events = graph.getEvents() != null ? graph.getEvents() : new ArrayList<>();

        // Identify nodes that should be completely removed (service node itself and nodes that only belong to this service)
        Set<UUID> nodeIdsToRemove = nodes.stream()
                .filter(node -> node.getId().equals(serviceUUID) ||
                        (node.getBelongsToGraph() != null && 
                         node.getBelongsToGraph().contains(serviceUUID) && 
                         node.getBelongsToGraph().size() == 1)
                )
                .map(NodeDTO::getId)
                .collect(Collectors.toSet());

        log.info(MessageHelper.getStaticMessage("axenapi.info.found.node.to.remove", nodeIdsToRemove.size(), serviceDescription));

        // Process nodes: remove completely or just remove service from belongsToGraph
        List<NodeDTO> filteredNodes = nodes.stream()
                .filter(node -> !nodeIdsToRemove.contains(node.getId()))
                .map(node -> {
                    // If node belongs to multiple graphs including this service, remove only this service from belongsToGraph
                    if (node.getBelongsToGraph() != null && 
                        node.getBelongsToGraph().contains(serviceUUID) && 
                        node.getBelongsToGraph().size() > 1) {
                        
                        NodeDTO updatedNode = new NodeDTO();
                        updatedNode.setId(node.getId());
                        updatedNode.setNodeDescription(node.getNodeDescription());
                        updatedNode.setNodeUrl(node.getNodeUrl());
                        updatedNode.setName(node.getName());
                        updatedNode.setType(node.getType());
                        updatedNode.setBrokerType(node.getBrokerType());
                        updatedNode.setRequestBody(node.getRequestBody());
                        updatedNode.setResponseBody(node.getResponseBody());
                        updatedNode.setMethodType(node.getMethodType());
                        updatedNode.setTags(node.getTags() != null ? new LinkedHashSet<>(node.getTags()) : null);
                        updatedNode.setDocumentationFileLinks(node.getDocumentationFileLinks() != null ? 
                            new LinkedHashSet<>(node.getDocumentationFileLinks()) : null);
                        
                        // Remove the service UUID from belongsToGraph
                        List<UUID> updatedBelongsToGraph = new ArrayList<>(node.getBelongsToGraph());
                        updatedBelongsToGraph.remove(serviceUUID);
                        updatedNode.setBelongsToGraph(updatedBelongsToGraph);
                        
                        return updatedNode;
                    }
                    return node;
                })
                .collect(Collectors.toList());

        List<LinkDTO> filteredLinks = links.stream()
                .filter(link -> !nodeIdsToRemove.contains(link.getFromId())
                        && !nodeIdsToRemove.contains(link.getToId()))
                .collect(Collectors.toList());

        List<EventDTO> filteredEvents = new ArrayList<>(events);

        log.info(MessageHelper.getStaticMessage("axenapi.info.removed.nodes.remaining", nodes.size() - filteredNodes.size(), filteredNodes.size()));
        log.info(MessageHelper.getStaticMessage("axenapi.info.removed.links.remaining", links.size() - filteredLinks.size(), filteredLinks.size()));
        log.info(MessageHelper.getStaticMessage("axenapi.info.events.kept", filteredEvents.size()));

        newGraph.setNodes(filteredNodes);
        newGraph.setLinks(filteredLinks);
        newGraph.setEvents(filteredEvents);

        return newGraph;
    }

    private static EventGraphDTO mergeGraphs(EventGraphDTO updatedGraph, EventGraphDTO newGraph, String sourceDescription) {
        logGraphState("Before merge from " + sourceDescription, updatedGraph);
        updatedGraph = merge(updatedGraph, newGraph);
        logGraphState("After merge from " + sourceDescription, updatedGraph);
        return updatedGraph;
    }

    private static void logGraphState(String prefix, EventGraphDTO graph) {
        log.debug("{}: nodes={}, links={}, events={}",
                prefix,
                sizeSafe(graph.getNodes()),
                sizeSafe(graph.getLinks()),
                sizeSafe(graph.getEvents()));
    }

    private static int sizeSafe(List<?> list) {
        return list != null ? list.size() : 0;
    }
}
