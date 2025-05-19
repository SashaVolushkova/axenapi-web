package pro.axenix_innovation.axenapi.web.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.*;

/**
 * Utility class for generating OpenAPI specifications from EventGraphFacade.
 */
@Slf4j
public class OpenAPIGenerator {

    /**
     * Generates OpenAPI specifications for all service nodes in the given EventGraphFacade.
     *
     * @param eventGraph the EventGraphFacade containing nodes and links
     * @return a map of service names to their OpenAPI specifications
     * @throws JsonProcessingException if serialization fails
     */
    public static Map<String, OpenAPI> getOpenAPISpecifications(EventGraphFacade eventGraph) throws JsonProcessingException {
        logGraphInfo(eventGraph);

        Map<String, OpenAPI> openAPIMap = createOpenAPIMap(eventGraph);
        processLinks(eventGraph, openAPIMap);

        log.info(MessageHelper.getStaticMessage("axenapi.info.finish.create.open.api.spec.service", openAPIMap.size()));
        return openAPIMap;
    }

    /**
     * Retrieves the OpenAPI specification for a specific service node by its UUID.
     *
     * @param eventGraph the EventGraphFacade containing the service node
     * @param serviceID the UUID of the service node
     * @return the OpenAPI specification for the service, or null if not found
     * @throws JsonProcessingException if serialization fails
     */
    public static OpenAPI getOpenAPISpecByServiceId(EventGraphFacade eventGraph, UUID serviceID) throws JsonProcessingException {
        NodeDTO nodeById = eventGraph.getNodeById(serviceID);
        if(nodeById == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NODE_NOT_FOUND, serviceID));
            return null;
        }
        String name = nodeById.getName();
        Map<String, OpenAPI> openAPISpecifications = getOpenAPISpecifications(eventGraph);
        return openAPISpecifications.get(name);
    }

    private static void logGraphInfo(EventGraphFacade eventGraph) {
        log.info(MessageHelper.getStaticMessage("axenapi.info.received.graph.nodes.links",
                eventGraph.getNodes().size(),
                eventGraph.getLinks().size()
        ));

        if (log.isDebugEnabled()) {
            for (NodeDTO node : eventGraph.getNodes()) {
                log.debug("Node: id={}, name='{}', type={}, brokerType={}",
                        node.getId(), node.getName(), node.getType(), node.getBrokerType());
            }
            for (LinkDTO link : eventGraph.getLinks()) {
                log.debug("Link: id={}, fromId={}, toId={}, eventId={}, group={}",
                        link.getId(), link.getFromId(), link.getToId(), link.getEventId(), link.getGroup());
            }
        }
    }

    private static Map<String, OpenAPI> createOpenAPIMap(EventGraphFacade eventGraph) {
        return eventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .collect(
                        HashMap::new,
                        (map, node) -> {
                            OpenAPI openAPI = createOpenAPI(node);
                            if (node.getDocumentationFileLinks() != null && !node.getDocumentationFileLinks().isEmpty()) {
                                openAPI.addExtension("x-documentation-file-links", new ArrayList<>(node.getDocumentationFileLinks()));
                            }
                            map.put(node.getName(), openAPI);
                            log.info(MessageHelper.getStaticMessage("axenapi.info.created.open.api.spec.service.node",
                                    node.getName()));
                        },
                        Map::putAll
                );
    }

    private static void processLinks(EventGraphFacade eventGraph, Map<String, OpenAPI> openAPIMap) throws JsonProcessingException {
        for (LinkDTO link : eventGraph.getLinks()) {
            NodeDTO toNode = eventGraph.getNodeById(link.getToId());
            NodeDTO fromNode = eventGraph.getNodeById(link.getFromId());
            EventDTO event = eventGraph.getEventById(link.getEventId());

            if (toNode == null || fromNode == null) {
                log.warn(MessageHelper.getStaticMessage(WARN_SKIPPING_LINK, link.getToId(), link.getFromId(), link.getEventId()));
                continue;
            }

            // TOPIC → SERVICE
            if (toNode.getType() == NodeDTO.TypeEnum.SERVICE && fromNode.getType() == NodeDTO.TypeEnum.TOPIC) {
                processIncomingTopicToService(eventGraph, openAPIMap, link, toNode, fromNode, event);
            }

            // SERVICE → TOPIC
            if (fromNode.getType() == NodeDTO.TypeEnum.SERVICE && toNode.getType() == NodeDTO.TypeEnum.TOPIC) {
                processOutgoingServiceToTopic(openAPIMap, link, fromNode, toNode, event);
            }

            // HTTP → SERVICE
            if (toNode.getType() == NodeDTO.TypeEnum.SERVICE && fromNode.getType() == NodeDTO.TypeEnum.HTTP) {
                processHttpToService(eventGraph, openAPIMap, link, toNode, fromNode, event);
            }
        }
    }

    private static void processIncomingTopicToService(EventGraphFacade eventGraph, Map<String, OpenAPI> openAPIMap,
                                                      LinkDTO link, NodeDTO toNode, NodeDTO fromNode, EventDTO event)
            throws JsonProcessingException {
        OpenAPI openAPI = openAPIMap.get(toNode.getName());
        if (openAPI == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_OPEN_API_SPEC_FOUND_SKIP_INC, toNode.getName()));
            return;
        }
        ensureComponents(openAPI);

        String broker;
        if (fromNode.getBrokerType() == null) {
            broker = "undefined";
        } else {
            broker = fromNode.getBrokerType().toString().toLowerCase();
        }

        String path;
        if (event == null) {
            // Handle case when event is null - create path with undefined_event
            switch (broker) {
                case "kafka":
                    String group = Optional.ofNullable(link.getGroup()).filter(g -> !g.isBlank()).orElse("default");
                    path = "/kafka/" + group + "/" + fromNode.getName() + "/undefined_event";
                    break;
                case "jms":
                    path = "/jms/" + fromNode.getName() + "/undefined_event";
                    break;
                case "rabbitmq":
                    path = "/rabbitmq/" + fromNode.getName() + "/undefined_event";
                    break;
                case "undefined":
                    path = "/undefined_broker/" + fromNode.getName() + "/undefined_event";
                    break;
                default:
                    path = "/" + broker + "/" + fromNode.getName() + "/undefined_event";
                    log.info(MessageHelper.getStaticMessage("axenapi.info.broker.unrecognized.use.default", broker, path));
                    break;
            }
        } else {
            switch (broker) {
                case "kafka":
                    String group = Optional.ofNullable(link.getGroup()).filter(g -> !g.isBlank()).orElse("default");
                    path = "/kafka/" + group + "/" + fromNode.getName() + "/" + event.getName();
                    break;
                case "jms":
                    path = "/jms/" + fromNode.getName() + "/" + event.getName();
                    break;
                case "rabbitmq":
                    path = "/rabbitmq/" + fromNode.getName() + "/" + event.getName();
                    break;
                case "undefined":
                    path = "/undefined_broker/" + fromNode.getName() + "/" + event.getName();
                    break;
                default:
                    path = "/" + broker + "/" + fromNode.getName() + "/" + event.getName();
                    log.info(MessageHelper.getStaticMessage("axenapi.info.broker.unrecognized.use.default", broker, path));
                    break;
            }
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.add.post.path.service", path, toNode.getName()));

        Set<String> tags = new LinkedHashSet<>();
        if (!link.getTags().isEmpty()) {
            tags.addAll(link.getTags());
        }
        if (event != null && !event.getTags().isEmpty()) {
            tags.addAll(event.getTags());
        }

        Operation postOp = new Operation().responses(createSimpleResponses());
        if (!tags.isEmpty()) {
            postOp.tags(new ArrayList<>(tags));
        }

        PathItem pathItem = new PathItem().post(postOp);
        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            pathItem.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }
        openAPI.getPaths().addPathItem(path, pathItem);

        if (event != null) {
            Schema<?> schema = getOrCreateSchema(openAPI, event);
            Map<String, Object> xIncoming = new LinkedHashMap<>();
            xIncoming.put("topics", List.of(fromNode.getName()));
            if (!tags.isEmpty()) {
                xIncoming.put("tags", new ArrayList<>(tags));
            }
            schema.addExtension("x-incoming", xIncoming);

            if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
                schema.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
            }

            openAPI.getComponents().addSchemas(event.getName(), schema);
        }
    }

    private static void processOutgoingServiceToTopic(Map<String, OpenAPI> openAPIMap, LinkDTO link,
                                                      NodeDTO fromNode, NodeDTO toNode, EventDTO event)
            throws JsonProcessingException {
        OpenAPI openAPI = openAPIMap.get(fromNode.getName());
        if (openAPI == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_OPEN_API_SPEC_FOUND_SKIP_OUT, fromNode.getName()));
            return;
        }
        ensureComponents(openAPI);

        if (event == null) {
            // If event is null, we can't create a schema, so just return
            log.warn("Event is null for outgoing link from service {} to topic {}, skipping schema creation", 
                    fromNode.getName(), toNode.getName());
            return;
        }

        Schema<?> schema = getOrCreateSchema(openAPI, event);
        Map<String, Object> xOutgoing = new LinkedHashMap<>();

        String topicSpec = toNode.getBrokerType() + "/" + toNode.getName();
        xOutgoing.put("topics", List.of(topicSpec));

        Set<String> tags = new LinkedHashSet<>();
        if (!link.getTags().isEmpty()) {
            tags.addAll(link.getTags());
        }
        if (!event.getTags().isEmpty()) {
            tags.addAll(event.getTags());
        }
        if (!tags.isEmpty()) {
            xOutgoing.put("tags", new ArrayList<>(tags));
        }

        schema.addExtension("x-outgoing", xOutgoing);

        if (toNode.getDocumentationFileLinks() != null && !toNode.getDocumentationFileLinks().isEmpty()) {
            schema.addExtension("x-documentation-file-links",
                    new ArrayList<>(toNode.getDocumentationFileLinks()));
        }

        openAPI.getComponents().addSchemas(event.getName(), schema);
    }

    private static void processHttpToService(EventGraphFacade eventGraph, Map<String, OpenAPI> openAPIMap,
                                             LinkDTO link, NodeDTO toNode, NodeDTO fromNode, EventDTO event)
            throws JsonProcessingException {
        OpenAPI openAPI = openAPIMap.get(toNode.getName());
        if (openAPI == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_OPEN_API_SPEC_FOUND_SKIP_HTTP, toNode.getName()));
            return;
        }
        ensureComponents(openAPI);

        String httpPath = fromNode.getNodeUrl();
        if (httpPath == null || httpPath.isBlank()) {
            log.warn(MessageHelper.getStaticMessage(WARN_HTTP_URL_SKIP_LINK, fromNode.getName()));
            return;
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.add.get.patch.path.service", httpPath, toNode.getName()));

        if (event == null) {
            Operation operation = new Operation().responses(new ApiResponses()
                    .addApiResponse("200", new ApiResponse().description("OK")));
            
            PathItem pathItem = new PathItem();
            
            // Use the HTTP method from the node, default to GET if not specified
            NodeDTO.MethodTypeEnum methodType = fromNode.getMethodType();
            if (methodType == null) {
                methodType = NodeDTO.MethodTypeEnum.GET;
            }
            
            switch (methodType) {
                case GET:
                    pathItem.get(operation);
                    break;
                case POST:
                    pathItem.post(operation);
                    break;
                case PUT:
                    pathItem.put(operation);
                    break;
                case DELETE:
                    pathItem.delete(operation);
                    break;
                case PATCH:
                    pathItem.patch(operation);
                    break;
                case HEAD:
                    pathItem.head(operation);
                    break;
                case OPTIONS:
                    pathItem.options(operation);
                    break;
                case TRACE:
                    pathItem.trace(operation);
                    break;
                default:
                    pathItem.get(operation);
                    break;
            }
            
            openAPI.getPaths().addPathItem(httpPath, pathItem);
            return;
        }

        Set<String> httpTags = new LinkedHashSet<>();
        httpTags.add("HTTP");
        if (link.getTags() != null) {
            httpTags.addAll(link.getTags());
        }
        if (event.getTags() != null) {
            httpTags.addAll(event.getTags());
        }

        List<String> finalTags = new ArrayList<>(httpTags);

        Operation getOp = new Operation()
                .responses(createHttpResponses(event.getName()))
                .parameters(List.of(createEventIdPathParameter()));
        Operation patchOp = new Operation()
                .responses(createHttpResponses(event.getName()))
                .parameters(List.of(createEventIdPathParameter()));

        if (!finalTags.isEmpty()) {
            getOp.tags(finalTags);
            patchOp.tags(finalTags);
        }

        PathItem pathItem = new PathItem()
                .summary("Retrieve a specific event by ID")
                .description(fromNode.getNodeDescription())
                .get(getOp)
                .patch(patchOp);

        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            pathItem.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }

        openAPI.getPaths().addPathItem(httpPath, pathItem);

        Schema<?> schema = getOrCreateSchema(openAPI, event);

        schema.addExtension("x-http-name", fromNode.getName());

        Map<String, Object> xIncoming = new LinkedHashMap<>();
        xIncoming.put("topics", List.of(fromNode.getName()));
        if (!httpTags.isEmpty()) {
            xIncoming.put("tags", new ArrayList<>(httpTags));
        }
        schema.addExtension("x-incoming", xIncoming);

        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            schema.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }

        openAPI.getComponents().addSchemas(event.getName(), schema);
    }

    private static ApiResponses createSimpleResponses() {
        ApiResponses apiResponses = new ApiResponses();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("Event sent successfully");
        apiResponses.addApiResponse("200", apiResponse);
        return apiResponses;
    }

    private static OpenAPI createOpenAPI(NodeDTO node) {
        OpenAPI openAPI = new OpenAPI();
        Info info = new Info();
        info.setVersion("1.0.0"); // TODO get version dynamically if needed
        info.setTitle(node.getName());
        info.setDescription("AxenAPI Specification for " + node.getName());
        openAPI.setInfo(info);
        openAPI.setPaths(new Paths());
        openAPI.setComponents(new Components());
        openAPI.getComponents().setSchemas(new HashMap<>());
        return openAPI;
    }

    private static void ensureComponents(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
    }

    private static Schema<?> getOrCreateSchema(OpenAPI openAPI, EventDTO event) throws JsonProcessingException {
        String eventName = event.getName();
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name is missing");
        }

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Schema<?> schema = schemas.get(eventName);
        if (schema == null) {
            schema = SchemaProcessor.deserializeSchema(event.getSchema());
            schema.setExtensions(new HashMap<>());
            schemas.put(eventName, schema);
        }
        return schema;
    }

    private static ApiResponses createHttpResponses(String refName) {
        if (refName == null || refName.isBlank()) {
            throw new IllegalArgumentException("Reference name for schema is missing");
        }

        ApiResponses responses = new ApiResponses();

        ApiResponse successResponse = new ApiResponse().description("Event retrieved successfully")
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + refName))));

        ApiResponse notFoundResponse = new ApiResponse().description("Event not found");

        responses.addApiResponse("200", successResponse);
        responses.addApiResponse("404", notFoundResponse);
        return responses;
    }

    private static Parameter createEventIdPathParameter() {
        return new Parameter()
                .name("eventId")
                .in("path")
                .description("ID of the event to retrieve")
                .required(true)
                .schema(new StringSchema());
    }
}