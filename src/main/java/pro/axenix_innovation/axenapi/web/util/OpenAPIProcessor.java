package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;
import java.util.stream.Collectors;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.*;
import static pro.axenix_innovation.axenapi.web.model.NodeDTO.BrokerTypeEnum.KAFKA;
import static pro.axenix_innovation.axenapi.web.model.NodeDTO.BrokerTypeEnum.UNDEFINED;
import static pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.SERVICE;
import static pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.TOPIC;

/**
 * Utility class for processing OpenAPI specifications and building event graphs.
 */
@Slf4j
public class OpenAPIProcessor {

    public static UUID createServiceNode(EventGraphFacade eventGraph, String title, String description, List<String> documentationFileLinks, UUID serviceNodeId) {
        UUID serviceNodeUUId = serviceNodeId == null ? UUID.randomUUID() : serviceNodeId;

        NodeDTO serviceNode = NodeDTO.builder()
                .id(serviceNodeUUId)
                .name(title)
                .type(SERVICE)
                .brokerType(null)
                .nodeDescription(description)
                .build();

        serviceNode.setBelongsToGraph(List.of(serviceNodeUUId));

        if (documentationFileLinks != null && !documentationFileLinks.isEmpty()) {
            serviceNode.setDocumentationFileLinks(new HashSet<>(documentationFileLinks));
            log.info(MessageHelper.getStaticMessage("axenapi.info.set.document.file.links", serviceNode.getDocumentationFileLinks()));
        }

        eventGraph.addNode(serviceNode);
        log.info(MessageHelper.getStaticMessage("axenapi.info.service.node.create.id", serviceNodeUUId));
        return serviceNodeUUId;
    }

    public static void processPaths(Paths paths, EventGraphFacade eventGraph,
                                    UUID serviceNodeUUID, Components components,
                                    Map<String, EventDTO> createdEvents,
                                    Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                    Map<String, String> consumerGroup,
                                    Map<String, Set<String>> topicTags,
                                    Set<String> allTags) {
        log.info(MessageHelper.getStaticMessage("axenapi.info.processing.open.api.path"));
        paths.forEach((key, path) -> {
            log.debug("Processing path: {}", key);
            List<String> pathDocumentationLinks = null;
            if (path.getExtensions() != null) {
                Object docs = path.getExtensions().get("x-documentation-file-links");
                if (docs instanceof List<?>) {
                    pathDocumentationLinks = ((List<?>) docs).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toList());
                }
            }
            Set<String> pathTags = TagExtractor.getTagsFromPath(path);
            allTags.addAll(pathTags);

            NodeDTO serviceNode = eventGraph.getNodeById(serviceNodeUUID);
            if (serviceNode.getTags() == null) {
                serviceNode.setTags(new HashSet<>());
            }
            serviceNode.getTags().addAll(pathTags);

            BrokerPathProcessor.BrokerPathInfo brokerInfo = BrokerPathProcessor.processBrokerPath(key, path);
            if (brokerInfo != null) {
                processBrokerPath(brokerInfo, eventGraph, serviceNode, brokers, consumerGroup, topicTags);
                allTags.addAll(brokerInfo.getTags());

                if (pathDocumentationLinks != null && !pathDocumentationLinks.isEmpty()) {
                    Optional<NodeDTO> topicNodeOpt = eventGraph.getNodes().stream()
                            .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC)
                            .filter(n -> n.getName().equals(brokerInfo.getTopic()))
                            .findFirst();
                    if (topicNodeOpt.isPresent()) {
                        NodeDTO topicNode = topicNodeOpt.get();
                        if (topicNode.getDocumentationFileLinks() == null) {
                            topicNode.setDocumentationFileLinks(new HashSet<>());
                        }
                        topicNode.getDocumentationFileLinks().addAll(pathDocumentationLinks);
                        log.info(MessageHelper.getStaticMessage("axenapi.info.add.document.file.link.topic.node",
                                pathDocumentationLinks, topicNode.getName()));
                    } else {
                        log.warn(MessageHelper.getStaticMessage(WARN_TOPIC_NODE_NOT_FOUND_DOC_LINK, brokerInfo.getTopic()));
                    }
                }
            } else {

                List<NodeDTO> httpNodes = createHttpNodes(key, path, pathTags, serviceNode, components);
                if (pathDocumentationLinks != null && !pathDocumentationLinks.isEmpty()) {
                    for (NodeDTO node : httpNodes) {
                        if (node.getDocumentationFileLinks() == null) {
                            node.setDocumentationFileLinks(new HashSet<>());
                        }
                        node.getDocumentationFileLinks().addAll(pathDocumentationLinks);
                        log.debug("Added documentationFileLinks to HTTP node '{}' for path '{}'", node.getName(), key);
                    }
                }
                eventGraph.addAllNodes(httpNodes);
                for (Operation operation : getOperations(path)) {
                    EventDTO linkEvent = null;
                    
                    // Обрабатываем request body - только для него создаем связь
                    if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                        MediaType requestMedia = operation.getRequestBody().getContent().get("application/json");
                        if (requestMedia != null && requestMedia.getSchema() != null) {
                            Schema<?> requestSchema = requestMedia.getSchema();
                            EventDTO requestEvent = resolveEventFromSchema(requestSchema, components, createdEvents);
                            if (requestEvent != null) {
                                eventGraph.addEvent(requestEvent);
                                linkEvent = requestEvent;
                            }
                        }
                    }
                    
                    // Обрабатываем все responses - создаем события, но НЕ создаем связи
                    if (operation.getResponses() != null) {
                        for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                            ApiResponse response = responseEntry.getValue();
                            if (response.getContent() != null) {
                                MediaType media = response.getContent().get("application/json");
                                if (media != null && media.getSchema() != null) {
                                    Schema<?> schema = media.getSchema();
                                    EventDTO event = resolveEventFromSchema(schema, components, createdEvents);
                                    if (event != null) {
                                        // Добавляем событие в граф, но не создаем для него связь
                                        eventGraph.addEvent(event);
                                        
                                        // Добавляем теги к событию
                                        if (event.getTags() == null) {
                                            event.setTags(new HashSet<>());
                                        }
                                        if (pathTags != null && !pathTags.isEmpty()) {
                                            event.getTags().addAll(pathTags);
                                        }
//                                        event.getTags().add("HTTP");
                                    }
                                }
                            }
                        }
                    }
                    
                    // Создаем связь только для request события
                    if (linkEvent != null) {
                        if (linkEvent.getTags() == null) {
                            linkEvent.setTags(new HashSet<>());
                        }

                        if (pathTags != null && !pathTags.isEmpty()) {
                            linkEvent.getTags().addAll(pathTags);
                        }

//                        linkEvent.getTags().add("HTTP");

                        for (NodeDTO nodeDTO : httpNodes) {
                            createHttpLink(nodeDTO, serviceNode, linkEvent, eventGraph);
                        }
                    } else {
                        // Если нет request события, создаем связь без события
                        for (NodeDTO nodeDTO : httpNodes) {
                            createHttpLink(nodeDTO, serviceNode, null, eventGraph);
                        }
                    }
                }
            }
        });
    }

    public static void processSchemas(Map<String, Schema> schemas, EventGraphFacade eventGraph,
                                      UUID serviceNodeUUId,
                                      Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                      Map<String, String> consumerGroup,
                                      Map<String, Set<String>> topicTags,
                                      Set<String> allTags,
                                      Map<String, EventDTO> createdEvents) {

        if (schemas == null || schemas.isEmpty()) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_SCHEMAS_FOUND));
            return;
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.processing.schemas", schemas.size()));

        schemas.forEach((key, schema) -> {
            try {
                log.debug("Processing schema: {}", key);
                // Only process schemas that have extensions (x-incoming/x-outgoing) or are already referenced
                if (hasRelevantExtensions(schema) || createdEvents.containsKey(key)) {
                    processSchema(key, schema, eventGraph, serviceNodeUUId,
                            brokers, consumerGroup, topicTags, allTags, createdEvents);
                }
            } catch (Exception e) {
                log.error(MessageHelper.getStaticMessage(ERROR_PROCESSING_SCHEMA, key, e.getMessage()));
            }
        });
    }

    public static void processPathLinks(Paths paths, EventGraphFacade eventGraph,
                                        UUID serviceNodeUUId,
                                        Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                        Map<String, String> consumerGroup,
                                        Map<String, Set<String>> topicTags) {
        paths.forEach((key, path) -> {
            BrokerPathProcessor.BrokerPathInfo brokerInfo = BrokerPathProcessor.processBrokerPath(key, path);
            if (brokerInfo != null) {
                List<String> docs = extractDocumentationLinks(path);

                // Create event only if eventName is not null (not undefined_event)
                EventDTO linkEvent = null;
                if (brokerInfo.getEventName() != null) {
                    linkEvent = eventGraph.getEvent(brokerInfo.getEventName());
                    if (linkEvent == null) {
                        linkEvent = EventDTO.builder()
                                .id(UUID.randomUUID())
                                .name(brokerInfo.getEventName())
                                .eventDescription(brokerInfo.getEventName())
                                .schema("{}")
                                .tags(brokerInfo.getTags())
                                .build();
                        eventGraph.addEvent(linkEvent);
                    }
                }

                processTopicLink(brokerInfo, eventGraph, serviceNodeUUId,
                        brokers, consumerGroup, topicTags, docs, linkEvent);
            }
        });
    }

    private static void processBrokerPath(BrokerPathProcessor.BrokerPathInfo brokerInfo,
                                          EventGraphFacade eventGraph,
                                          NodeDTO serviceNode,
                                          Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                          Map<String, String> consumerGroup,
                                          Map<String, Set<String>> topicTags) {
        brokers.put(brokerInfo.getTopic(), brokerInfo.getBrokerType());
        if (brokerInfo.getBrokerType() == KAFKA && brokerInfo.getGroup() != null) {
            consumerGroup.put(brokerInfo.getTopic(), brokerInfo.getGroup());
        }
        
        // Only add to topicTags if eventName is not null
        if (brokerInfo.getEventName() != null) {
            topicTags.put(brokerInfo.getEventName(), brokerInfo.getTags());
            topicTags.put(brokerInfo.getTopic() + brokerInfo.getEventName(), new HashSet<>(brokerInfo.getTags()));
        }
        log.info(MessageHelper.getStaticMessage("axenapi.info.tags.path", brokerInfo.getTopic(), brokerInfo.getTags()));
    }

    private static List<Operation> getOperations(PathItem pathItem) {
        List<Operation> operations = new ArrayList<>();
        if (pathItem.getGet() != null) operations.add(pathItem.getGet());
        if (pathItem.getPost() != null) operations.add(pathItem.getPost());
        if (pathItem.getPut() != null) operations.add(pathItem.getPut());
        if (pathItem.getDelete() != null) operations.add(pathItem.getDelete());
        if (pathItem.getPatch() != null) operations.add(pathItem.getPatch());
        return operations;
    }

    private static void createHttpLink(NodeDTO fromNode, NodeDTO toNode,
                                       EventDTO event, EventGraphFacade eventGraph) {
        Set<String> tags = new HashSet<>();
        if (event != null && event.getTags() != null) {
            tags.addAll(event.getTags());
        }

        LinkDTO linkDTO = LinkDTO.builder()
                .id(UUID.randomUUID())
                .fromId(fromNode.getId())
                .toId(toNode.getId())
                .tags(tags)
                .eventId(event != null ? event.getId() : null)
                .group(null)
                .build();

        eventGraph.addLink(linkDTO);
    }

    private static EventDTO resolveEventFromSchema(Schema<?> schema,
                                                   Components components,
                                                   Map<String, EventDTO> createdEvents) {
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            if (createdEvents.containsKey(schemaName)) {
                return createdEvents.get(schemaName);
            }
            Schema<?> resolvedSchema = components.getSchemas().get(schemaName);
            if (resolvedSchema == null) return null;
            
            EventDTO event = EventDTO.builder()
                    .id(UUID.randomUUID())
                    .name(schemaName)
                    .eventDescription(resolvedSchema.getDescription() != null ? resolvedSchema.getDescription() : null)
                    .schema(Json.pretty(resolvedSchema))
                    .tags(new HashSet<>())
                    .build();
            createdEvents.put(schemaName, event);
            log.debug("Created event '{}' from schema", schemaName);
            return event;
        }
        return null;
    }

    private static List<NodeDTO> createHttpNodes(String key, PathItem path, Set<String> pathTags, NodeDTO serviceNode, Components components) {
        System.out.println("=== CREATE HTTP NODES for path: " + key + " ===");
        String desc = path.getDescription();
        Map<PathItem.HttpMethod, Operation> map = path.readOperationsMap();
        List<NodeDTO> res = new ArrayList<>();

        for (Map.Entry<PathItem.HttpMethod, Operation> entry : map.entrySet()) {
            System.out.println("=== Processing HTTP method: " + entry.getKey() + " ===");
            PathItem.HttpMethod method = entry.getKey();
            Operation operation = entry.getValue();

            String opDesc = operation.getDescription();

            Set<String> tagsCopy = (pathTags == null) ? new HashSet<>() : new HashSet<>(pathTags);

            String eventName = null;
            String nodeName = null;

            if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                MediaType media = operation.getRequestBody().getContent().get("application/json");
                if (media != null && media.getSchema() != null && media.getSchema().get$ref() != null) {
                    eventName = media.getSchema().get$ref();
                }
            }

            if (eventName == null) {
                ApiResponse response = operation.getResponses() != null ? operation.getResponses().get("200") : null;
                if (response != null && response.getContent() != null) {
                    MediaType media = response.getContent().get("application/json");
                    if (media != null && media.getSchema() != null && media.getSchema().get$ref() != null) {
                        eventName = media.getSchema().get$ref();
                    }
                }
            }

            if (eventName != null) {
                String simpleName = eventName.contains("/")
                        ? eventName.substring(eventName.lastIndexOf("/") + 1)
                        : eventName;

                Schema<?> schema = components.getSchemas().get(simpleName);
                if (schema != null && schema.getExtensions() != null) {
                    Object httpName = schema.getExtensions().get("x-http-name");
                    if (httpName instanceof String) {
                        nodeName = (String) httpName;
                    }
                }
            }

            NodeDTO node = NodeDTO.builder()
                    .id(UUID.randomUUID())
                    .name(nodeName != null ? nodeName : key)
                    .type(NodeDTO.TypeEnum.HTTP)
                    .tags(tagsCopy)
                    .belongsToGraph(List.of(serviceNode.getId()))
                    .methodType(NodeDTO.MethodTypeEnum.fromValue(method.name()))
                    .nodeDescription(opDesc != null ? opDesc : desc)
                    .nodeUrl(key)
                    .build();

            // Заполняем новые поля для HTTP взаимодействия
            populateHttpFields(node, operation, components);
            
            // Проверяем, что поля действительно установлены
            log.debug("After populateHttpFields - node {} has httpParameters: {}", node.getName(), node.getHttpParameters() != null);
            log.debug("After populateHttpFields - node {} has httpRequestBody: {}", node.getName(), node.getHttpRequestBody() != null);
            log.debug("After populateHttpFields - node {} has httpResponses: {}", node.getName(), node.getHttpResponses() != null ? node.getHttpResponses().size() : "null");

            res.add(node);
        }

        return res;
    }

    private static void processSchema(String schemaKey, Schema schema, EventGraphFacade eventGraph,
                                      UUID serviceNodeUUId,
                                      Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                      Map<String, String> consumerGroup,
                                      Map<String, Set<String>> topicTags,
                                      Set<String> allTags,
                                      Map<String, EventDTO> createdEvents) {
        if (createdEvents.containsKey(schemaKey)) {
            log.info(MessageHelper.getStaticMessage("axenapi.info.event.created.skip.duplicate", schemaKey));
            return;
        }
        Map<String, Object> extensions = schema.getExtensions();
        String schemaString = schema != null ? Json.pretty(schema) : "{}";
        Set<String> eventTags = topicTags.getOrDefault(schemaKey, new HashSet<>());
        allTags.addAll(eventTags);

        EventDTO event = new EventDTO()
                .id(UUID.randomUUID())
                .schema(schemaString)
                .name(schemaKey)
                .eventDescription(schema.getDescription() != null ? schema.getDescription() : null)
                .tags(eventTags);
        eventGraph.addEvent(event);
        createdEvents.put(schemaKey, event);
        log.info(MessageHelper.getStaticMessage("axenapi.info.tags.for.event", event.getId(), event.getTags()));

        if (extensions == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_EXTENSIONS_IN_SCHEMA, schemaKey));
            return;
        }

        List<String> docLinks = null;
        if (extensions.containsKey("x-documentation-file-links")) {
            Object ext = extensions.get("x-documentation-file-links");
            if (ext instanceof List<?>) {
                docLinks = ((List<?>) ext).stream()
                        .filter(o -> o instanceof String)
                        .map(String::valueOf)
                        .collect(Collectors.toList());
            }
        }

        if (extensions.containsKey("x-incoming")) {
            processIncomingExtensions(extensions, schemaKey, eventGraph,
                    serviceNodeUUId, brokers,
                    consumerGroup, topicTags, event, docLinks, allTags);
        }

        if (extensions.containsKey("x-outgoing")) {
            processOutgoingExtensions(extensions, schemaKey, eventGraph,
                    serviceNodeUUId, topicTags,
                    allTags, event);
        }
    }

    private static void processIncomingExtensions(Map<String, Object> extensions, String schemaKey,
                                                  EventGraphFacade eventGraph, UUID serviceNodeUUId,
                                                  Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                                  Map<String, String> consumerGroup,
                                                  Map<String, Set<String>> topicTags,
                                                  EventDTO event,
                                                  List<String> documentationLinks,
                                                  Set<String> allTags) {
        log.debug("Processing x-incoming for schema: {}", schemaKey);
        Map<String, Object> xIncoming = (Map<String, Object>) extensions.get("x-incoming");
        if (xIncoming == null || !xIncoming.containsKey("topics")) {
            log.warn("No topics found in x-incoming for schema: {}", schemaKey);
            return;
        }

        List<?> topics = (List<?>) xIncoming.get("topics");

        if (xIncoming.containsKey("tags")) {
            List<?> rawTags = (List<?>) xIncoming.get("tags");
            Set<String> incomingTags = rawTags.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            event.getTags().addAll(incomingTags);
            allTags.addAll(incomingTags);
        }

        topics.forEach(topicSpec -> {
            if (topicSpec instanceof String) {
                processIncomingTopic((String) topicSpec, eventGraph, serviceNodeUUId,
                        brokers, consumerGroup, topicTags, schemaKey, event, documentationLinks);
            } else if (topicSpec instanceof Map) {
                String topicName = (String) ((Map<?, ?>) topicSpec).get("name");
                List<String> tags = (List<String>) ((Map<?, ?>) topicSpec).get("tags");
                if (tags != null) {
                    event.getTags().addAll(tags);
                    allTags.addAll(tags);
                }
                processIncomingTopic(topicName, eventGraph, serviceNodeUUId,
                        brokers, consumerGroup, topicTags, schemaKey, event, documentationLinks);
            }
        });
    }
    private static void processIncomingTopic(String topicSpec, EventGraphFacade eventGraph,
                                             UUID serviceNodeUUId,
                                             Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                             Map<String, String> consumerGroup,
                                             Map<String, Set<String>> topicTags,
                                             String eventKey, EventDTO event,
                                             List<String> documentationLinks) {
        String[] split = topicSpec.split("/");
        String topic = topicSpec;
        NodeDTO.TypeEnum type = TOPIC;

        if (split.length == 2) {
            topic = split[1];
        }

        NodeDTO.BrokerTypeEnum brokerType = brokers.get(topic);
        if (brokerType == null) {
            brokerType = UNDEFINED;
        }

        Set<String> curTopicEvTags = topicTags.getOrDefault(topic + eventKey, new HashSet<>());
        log.info(MessageHelper.getStaticMessage("axenapi.info.tags.for.topic.event", topic, eventKey, curTopicEvTags));

        NodeDTO incomingTopic = new NodeDTO().id(UUID.randomUUID())
                .name(topic)
                .type(type)
                .addBelongsToGraphItem(serviceNodeUUId)
                .tags(new HashSet<>(curTopicEvTags))
                .brokerType(brokerType);

        if (documentationLinks != null && !documentationLinks.isEmpty()) {
            incomingTopic.setDocumentationFileLinks(new HashSet<>(documentationLinks));
        }

        if (!eventGraph.containsNode(topic, type, brokerType)) {
            eventGraph.addNode(incomingTopic);
        } else {
            incomingTopic = eventGraph.getNode(topic, type, brokerType);
            if (incomingTopic.getTags() == null) {
                incomingTopic.setTags(new HashSet<>());
            }
            incomingTopic.getTags().addAll(curTopicEvTags);

            if (documentationLinks != null && !documentationLinks.isEmpty()) {
                if (incomingTopic.getDocumentationFileLinks() == null) {
                    incomingTopic.setDocumentationFileLinks(new HashSet<>());
                }
                incomingTopic.getDocumentationFileLinks().addAll(documentationLinks);
            }
        }

        String group = consumerGroup.get(topic);

        LinkDTO incomingLink = new LinkDTO()
                .id(UUID.randomUUID())
                .fromId(incomingTopic.getId())
                .toId(serviceNodeUUId)
                .group(group)
                .eventId(event.getId())
                .tags(new HashSet<>(curTopicEvTags));

        eventGraph.addLink(incomingLink);
    }

    private static void processOutgoingExtensions(Map<String, Object> extensions, String schemaKey,
                                                  EventGraphFacade eventGraph, UUID serviceNodeUUId,
                                                  Map<String, Set<String>> topicTags,
                                                  Set<String> allTags, EventDTO event) {
        log.debug("Processing x-outgoing for schema: {}", schemaKey);
        Map<String, Object> xOutgoing = (Map<String, Object>) extensions.get("x-outgoing");
        List topics = (List<String>) xOutgoing.get("topics");

        if (xOutgoing.containsKey("tags")) {
            List<?> rawTags = (List<?>) xOutgoing.get("tags");
            Set<String> outgoingTags = rawTags.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            event.getTags().addAll(outgoingTags);
            allTags.addAll(outgoingTags);
        }

        topics.forEach(topicSpec -> {
            log.debug("Processing outgoing topic: {}", topicSpec);
            log.info(MessageHelper.getStaticMessage("axenapi.info.outgoing.topic", topicSpec, topicTags.get(topicSpec)));
            if (topicSpec instanceof String topic) {
                List<String> tags = new ArrayList<>();
                if (event != null && event.getTags() != null && !event.getTags().isEmpty()) {
                    tags.addAll(event.getTags());
                }
                computeOutgoing(topic, eventGraph.getNodeById(serviceNodeUUId),
                        eventGraph, event, tags);
            } else {
                Map<String, Object> topic = (Map<String, Object>) topicSpec;
                String topicName = (String) topic.get("name");
                List<String> tagsSet = (List<String>) topic.get("tags");
                allTags.addAll(tagsSet);
                NodeDTO serviceNode = eventGraph.getNodeById(serviceNodeUUId);
                if (serviceNode.getTags() == null) {
                    serviceNode.setTags(new HashSet<>());
                }
                serviceNode.getTags().addAll(tagsSet);
                computeOutgoing(topicName, serviceNode, eventGraph, event, tagsSet);
            }
        });
    }


    private static void processTopicLink(BrokerPathProcessor.BrokerPathInfo brokerInfo,
                                         EventGraphFacade eventGraph, UUID serviceNodeUUId,
                                         Map<String, NodeDTO.BrokerTypeEnum> brokers,
                                         Map<String, String> consumerGroup,
                                         Map<String, Set<String>> topicTags,
                                         List<String> documentationLinks,
                                         EventDTO linkEvent) {
        brokers.put(brokerInfo.getTopic(), brokerInfo.getBrokerType());

        if (brokerInfo.getBrokerType() == KAFKA && brokerInfo.getGroup() != null) {
            consumerGroup.put(brokerInfo.getTopic(), brokerInfo.getGroup());
        }

        NodeDTO serviceNode = eventGraph.getNodeById(serviceNodeUUId);

        // Check if link already exists - only if we have an event
        boolean hasLink = false;
        if (linkEvent != null) {
            hasLink = eventGraph.getLinks().stream().anyMatch(link -> {
                NodeDTO from = eventGraph.getNodeById(link.getFromId());
                NodeDTO to = eventGraph.getNodeById(link.getToId());
                EventDTO event = eventGraph.getEventById(link.getEventId());
                return from.getName().equals(brokerInfo.getTopic()) &&
                        to.getName().equals(serviceNode.getName()) &&
                        event != null && event.getName().equals(brokerInfo.getEventName());
            });
        }

        if (!hasLink) {
            createTopicLink(brokerInfo, serviceNodeUUId,
                    topicTags, consumerGroup, linkEvent, serviceNode, eventGraph, documentationLinks);
        }
    }

    private static void createTopicLink(BrokerPathProcessor.BrokerPathInfo brokerInfo,
                                        UUID serviceNodeUUId,
                                        Map<String, Set<String>> topicTags,
                                        Map<String, String> consumerGroup,
                                        EventDTO linkEvent, NodeDTO serviceNode,
                                        EventGraphFacade eventGraph,
                                        List<String> documentationLinks) {
        NodeDTO nodeTopic = eventGraph.getNode(brokerInfo.getTopic(), TOPIC, brokerInfo.getBrokerType());
        if (nodeTopic == null) {
            Set<String> tags = Collections.emptySet();
            if (linkEvent != null) {
                tags = topicTags.getOrDefault(brokerInfo.getTopic() + linkEvent.getName(), Collections.emptySet());
            }
            nodeTopic = NodeDTO.builder()
                    .id(UUID.randomUUID())
                    .type(TOPIC)
                    .brokerType(brokerInfo.getBrokerType())
                    .name(brokerInfo.getTopic())
                    .belongsToGraph(List.of(serviceNodeUUId))
                    .tags(tags)
                    .build();

            if (documentationLinks != null && !documentationLinks.isEmpty()) {
                nodeTopic.setDocumentationFileLinks(new HashSet<>(documentationLinks));
            }

            eventGraph.addNode(nodeTopic);
        }

        String group = consumerGroup.get(brokerInfo.getTopic());

        LinkDTO newLink = LinkDTO.builder()
                .id(UUID.randomUUID())
                .eventId(linkEvent != null ? linkEvent.getId() : null)
                .toId(serviceNode.getId())
                .fromId(nodeTopic.getId())
                .group(group)
                .build();
        eventGraph.addLink(newLink);
    }

    private static List<String> extractDocumentationLinks(PathItem pathItem) {
        if (pathItem.getExtensions() != null) {
            Object ext = pathItem.getExtensions().get("x-documentation-file-links");
            if (ext instanceof List<?>) {
                return ((List<?>) ext).stream()
                        .filter(e -> e instanceof String)
                        .map(String.class::cast)
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    private static boolean hasRelevantExtensions(Schema schema) {
        if (schema.getExtensions() == null) {
            return false;
        }
        return schema.getExtensions().containsKey("x-incoming") ||
                schema.getExtensions().containsKey("x-outgoing");
    }

    private static void computeOutgoing(String topic, NodeDTO serviceNode, EventGraphFacade eventGraph, EventDTO event, List<String> tags) {
        String[] split = topic.split("/");
        NodeDTO.BrokerTypeEnum brokerType = null;
        NodeDTO.TypeEnum type;
        if (split.length == 2) {
            topic = split[1];
            try {
                brokerType = NodeDTO.BrokerTypeEnum.valueOf(split[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                brokerType = UNDEFINED;
            }
        }
        if (brokerType == null) {
            brokerType = NodeDTO.BrokerTypeEnum.UNDEFINED;
        }
        type = NodeDTO.TypeEnum.TOPIC;

        Set<String> tagSet = tags.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());

        NodeDTO outgoingTopic = eventGraph.getNode(topic, type, brokerType);
        if (outgoingTopic == null) {
            outgoingTopic = new NodeDTO()
                    .id(UUID.randomUUID())
                    .name(topic)
                    .type(type)
                    .addBelongsToGraphItem(serviceNode.getId())
                    .tags(new HashSet<>(tagSet))
                    .brokerType(brokerType);
            eventGraph.addNode(outgoingTopic);
        } else {
            outgoingTopic.getTags().addAll(tagSet);
            eventGraph.updateNode(outgoingTopic);
        }

        if (serviceNode.getTags() == null) {
            serviceNode.setTags(new HashSet<>());
        }
        serviceNode.getTags().addAll(tagSet);
        eventGraph.updateNode(serviceNode);

        LinkDTO outgoungLink = new LinkDTO()
                .id(UUID.randomUUID())
                .fromId(serviceNode.getId())
                .toId(outgoingTopic.getId())
                .group(null)
                .eventId(event != null ? event.getId() : null)
                .tags(new HashSet<>(tagSet));
        eventGraph.addLink(outgoungLink);

        if (event != null) {
            event.getTags().addAll(tagSet);
        }
    }

    /**
     * Заполняет новые поля HTTP нод для детального описания HTTP взаимодействия
     */
    private static void populateHttpFields(NodeDTO node, Operation operation, Components components) {
        // Импортируем необходимые классы для новых DTO
        try {
            System.out.println("=== POPULATE HTTP FIELDS START for node: " + node.getName() + " ===");
            log.info("Populating HTTP fields for node: {}", node.getName());
            
            // Заполняем HTTP параметры
            pro.axenix_innovation.axenapi.web.model.HttpParametersDTO httpParams = createHttpParameters(operation);
            System.out.println("=== Created HTTP parameters: " + httpParams + " ===");
            log.info("Created HTTP parameters for node {}: {}", node.getName(), httpParams);
            node.setHttpParameters(httpParams);
            System.out.println("=== Set HTTP parameters on node ===");
            
            // Заполняем HTTP request body
            pro.axenix_innovation.axenapi.web.model.HttpRequestBodyDTO httpRequestBody = createHttpRequestBody(operation, components);
            log.debug("Created HTTP request body for node {}: {}", node.getName(), httpRequestBody);
            node.setHttpRequestBody(httpRequestBody);
            
            // Заполняем HTTP responses
            List<pro.axenix_innovation.axenapi.web.model.HttpResponseDTO> httpResponses = createHttpResponses(operation, components);
            log.debug("Created HTTP responses for node {}: {}", node.getName(), httpResponses);
            node.setHttpResponses(httpResponses);
            
            // Заполняем старые поля для обратной совместимости
            populateLegacyFields(node, operation, components);
            
            log.debug("Successfully populated HTTP fields for node: {}", node.getName());
            
        } catch (Exception e) {
            log.warn("Failed to populate HTTP fields for node {}: {}", node.getName(), e.getMessage(), e);
        }
    }

    private static pro.axenix_innovation.axenapi.web.model.HttpParametersDTO createHttpParameters(Operation operation) {
        pro.axenix_innovation.axenapi.web.model.HttpParametersDTO httpParams = 
            new pro.axenix_innovation.axenapi.web.model.HttpParametersDTO();
        
        List<pro.axenix_innovation.axenapi.web.model.HttpParameterDTO> pathParams = new ArrayList<>();
        List<pro.axenix_innovation.axenapi.web.model.HttpParameterDTO> queryParams = new ArrayList<>();
        List<pro.axenix_innovation.axenapi.web.model.HttpParameterDTO> headerParams = new ArrayList<>();

        if (operation.getParameters() != null && !operation.getParameters().isEmpty()) {
            for (io.swagger.v3.oas.models.parameters.Parameter param : operation.getParameters()) {
                pro.axenix_innovation.axenapi.web.model.HttpParameterDTO httpParam = 
                    new pro.axenix_innovation.axenapi.web.model.HttpParameterDTO();
                
                httpParam.setName(param.getName());
                httpParam.setDescription(param.getDescription());
                httpParam.setRequired(param.getRequired() != null ? param.getRequired() : false);
                
                // Определяем тип параметра
                if (param.getSchema() != null) {
                    String type = param.getSchema().getType();
                    if (type != null) {
                        try {
                            httpParam.setType(pro.axenix_innovation.axenapi.web.model.HttpParameterTypeEnum.fromValue(type));
                        } catch (Exception e) {
                            httpParam.setType(pro.axenix_innovation.axenapi.web.model.HttpParameterTypeEnum.STRING);
                        }
                    } else {
                        httpParam.setType(pro.axenix_innovation.axenapi.web.model.HttpParameterTypeEnum.STRING);
                    }
                    
                    // Сохраняем полную схему как JSON
                    httpParam.setSchema(Json.pretty(param.getSchema()));
                } else {
                    httpParam.setType(pro.axenix_innovation.axenapi.web.model.HttpParameterTypeEnum.STRING);
                }
                
                // Добавляем пример
                if (param.getExample() != null) {
                    httpParam.setExample(param.getExample().toString());
                }

                // Распределяем по типам
                switch (param.getIn()) {
                    case "path":
                        pathParams.add(httpParam);
                        break;
                    case "query":
                        queryParams.add(httpParam);
                        break;
                    case "header":
                        headerParams.add(httpParam);
                        break;
                }
            }
        }

        httpParams.setPathParameters(pathParams);
        httpParams.setQueryParameters(queryParams);
        httpParams.setHeaderParameters(headerParams);

        return httpParams;
    }

    private static pro.axenix_innovation.axenapi.web.model.HttpRequestBodyDTO createHttpRequestBody(Operation operation, Components components) {
        if (operation.getRequestBody() == null) {
            return null;
        }

        pro.axenix_innovation.axenapi.web.model.HttpRequestBodyDTO requestBody = 
            new pro.axenix_innovation.axenapi.web.model.HttpRequestBodyDTO();
        
        requestBody.setDescription(operation.getRequestBody().getDescription());
        requestBody.setRequired(operation.getRequestBody().getRequired() != null ? operation.getRequestBody().getRequired() : false);

        List<pro.axenix_innovation.axenapi.web.model.HttpContentDTO> contentList = new ArrayList<>();
        
        if (operation.getRequestBody().getContent() != null) {
            for (Map.Entry<String, MediaType> entry : operation.getRequestBody().getContent().entrySet()) {
                pro.axenix_innovation.axenapi.web.model.HttpContentDTO content = 
                    new pro.axenix_innovation.axenapi.web.model.HttpContentDTO();
                
                try {
                    content.setMediaType(pro.axenix_innovation.axenapi.web.model.HttpContentTypeEnum.fromValue(entry.getKey()));
                } catch (Exception e) {
                    // Если mediaType не найден в enum, пропускаем
                    continue;
                }
                
                MediaType mediaType = entry.getValue();
                if (mediaType.getSchema() != null) {
                    content.setSchema(Json.pretty(mediaType.getSchema()));
                }
                
                if (mediaType.getExample() != null) {
                    content.setExample(Json.pretty(mediaType.getExample()));
                }
                
                content.setExamples(new ArrayList<>());
                contentList.add(content);
            }
        }

        requestBody.setContent(contentList);
        return requestBody;
    }

    private static List<pro.axenix_innovation.axenapi.web.model.HttpResponseDTO> createHttpResponses(Operation operation, Components components) {
        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            return new ArrayList<>();
        }

        List<pro.axenix_innovation.axenapi.web.model.HttpResponseDTO> responses = new ArrayList<>();

        for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
            pro.axenix_innovation.axenapi.web.model.HttpResponseDTO response = 
                new pro.axenix_innovation.axenapi.web.model.HttpResponseDTO();
            
            try {
                response.setStatusCode(pro.axenix_innovation.axenapi.web.model.HttpStatusCodeEnum.fromValue(entry.getKey()));
            } catch (Exception e) {
                // Если статус код не найден в enum, пропускаем
                continue;
            }
            
            response.setDescription(entry.getValue().getDescription());
            response.setHeaders(new ArrayList<>());

            List<pro.axenix_innovation.axenapi.web.model.HttpContentDTO> contentList = new ArrayList<>();
            
            if (entry.getValue().getContent() != null) {
                for (Map.Entry<String, MediaType> contentEntry : entry.getValue().getContent().entrySet()) {
                    pro.axenix_innovation.axenapi.web.model.HttpContentDTO content = 
                        new pro.axenix_innovation.axenapi.web.model.HttpContentDTO();
                    
                    try {
                        content.setMediaType(pro.axenix_innovation.axenapi.web.model.HttpContentTypeEnum.fromValue(contentEntry.getKey()));
                    } catch (Exception e) {
                        // Если mediaType не найден в enum, пропускаем
                        continue;
                    }
                    
                    MediaType mediaType = contentEntry.getValue();
                    if (mediaType.getSchema() != null) {
                        content.setSchema(Json.pretty(mediaType.getSchema()));
                    }
                    
                    if (mediaType.getExample() != null) {
                        content.setExample(Json.pretty(mediaType.getExample()));
                    }
                    
                    content.setExamples(new ArrayList<>());
                    contentList.add(content);
                }
            }

            response.setContent(contentList);
            responses.add(response);
        }

        return responses;
    }

    private static void populateLegacyFields(NodeDTO node, Operation operation, Components components) {
        // Заполняем старые поля requestBody и responseBody для обратной совместимости
        
        // requestBody - берем первый найденный request body
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            for (MediaType mediaType : operation.getRequestBody().getContent().values()) {
                if (mediaType.getSchema() != null) {
                    node.setRequestBody(Json.pretty(mediaType.getSchema()));
                    break;
                }
            }
        }

        // responseBody - берем все ответы и объединяем в один JSON
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            Map<String, Object> allResponses = new HashMap<>();
            
            for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("description", entry.getValue().getDescription());
                
                if (entry.getValue().getContent() != null) {
                    Map<String, Object> content = new HashMap<>();
                    for (Map.Entry<String, MediaType> contentEntry : entry.getValue().getContent().entrySet()) {
                        Map<String, Object> mediaData = new HashMap<>();
                        if (contentEntry.getValue().getSchema() != null) {
                            try {
                                mediaData.put("schema", Json.mapper().readValue(Json.pretty(contentEntry.getValue().getSchema()), Object.class));
                            } catch (Exception e) {
                                mediaData.put("schema", Json.pretty(contentEntry.getValue().getSchema()));
                            }
                        }
                        if (contentEntry.getValue().getExample() != null) {
                            mediaData.put("example", contentEntry.getValue().getExample());
                        }
                        content.put(contentEntry.getKey(), mediaData);
                    }
                    responseData.put("content", content);
                }
                
                allResponses.put(entry.getKey(), responseData);
            }
            
            try {
                node.setResponseBody(Json.pretty(allResponses));
            } catch (Exception e) {
                log.warn("Failed to serialize response body for node {}: {}", node.getName(), e.getMessage());
            }
        }
    }

    /**
     * Пост-обработка для установки usageContext событий на основе их использования в графе
     */
    public static void postProcessUsageContext(EventGraphFacade eventGraph) {
        log.info("Starting post-processing to set usageContext for events");
        
        // Создаем множества для отслеживания событий по контексту использования
        Set<UUID> httpEventIds = new HashSet<>();
        Set<UUID> asyncEventIds = new HashSet<>();
        
        // 1. Проходим по всем связям и определяем контекст использования событий через связи
        for (pro.axenix_innovation.axenapi.web.model.LinkDTO link : eventGraph.getLinks()) {
            if (link.getEventId() == null) {
                continue; // Пропускаем связи без событий
            }
            
            // Получаем узлы связи
            pro.axenix_innovation.axenapi.web.model.NodeDTO fromNode = eventGraph.getNodeById(link.getFromId());
            pro.axenix_innovation.axenapi.web.model.NodeDTO toNode = eventGraph.getNodeById(link.getToId());
            
            if (fromNode == null || toNode == null) {
                continue;
            }
            
            // Определяем контекст на основе типов узлов
            boolean isHttpContext = (fromNode.getType() == pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.HTTP) ||
                                   (toNode.getType() == pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.HTTP);
            
            boolean isAsyncContext = (fromNode.getType() == pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.TOPIC) ||
                                    (toNode.getType() == pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.TOPIC);
            
            if (isHttpContext) {
                httpEventIds.add(link.getEventId());
                log.debug("Event {} marked as HTTP context via link", link.getEventId());
            }
            
            if (isAsyncContext) {
                asyncEventIds.add(link.getEventId());
                log.debug("Event {} marked as ASYNC context via link", link.getEventId());
            }
        }
        
        // 2. Дополнительно проверяем события, которые могут не иметь связей, но используются в HTTP контексте
        // Если в графе есть HTTP узлы, то все события считаются HTTP событиями
        boolean hasHttpNodes = eventGraph.getNodes().stream()
                .anyMatch(node -> node.getType() == pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.HTTP);
        
        boolean hasTopicNodes = eventGraph.getNodes().stream()
                .anyMatch(node -> node.getType() == pro.axenix_innovation.axenapi.web.model.NodeDTO.TypeEnum.TOPIC);
        
        // Устанавливаем usageContext для всех событий
        for (pro.axenix_innovation.axenapi.web.model.EventDTO event : eventGraph.eventGraph().getEvents()) {
            Set<pro.axenix_innovation.axenapi.web.model.EventUsageContextEnum> usageContext = new HashSet<>();
            
            // Событие имеет HTTP контекст, если:
            // 1. Оно участвует в связи с HTTP узлом, ИЛИ
            // 2. В графе есть HTTP узлы (значит, это HTTP спецификация)
            if (httpEventIds.contains(event.getId()) || hasHttpNodes) {
                usageContext.add(pro.axenix_innovation.axenapi.web.model.EventUsageContextEnum.HTTP);
                log.debug("Event '{}' marked as HTTP context", event.getName());
            }
            
            // Событие имеет ASYNC контекст, если оно участвует в связи с TOPIC узлом
            if (asyncEventIds.contains(event.getId())) {
                usageContext.add(pro.axenix_innovation.axenapi.web.model.EventUsageContextEnum.ASYNC_MESSAGING);
                log.debug("Event '{}' marked as ASYNC context", event.getName());
            }
            
            event.setUsageContext(usageContext);
            log.debug("Set usageContext for event '{}': {}", event.getName(), usageContext);
        }
        
        log.info("Completed post-processing usageContext for {} events", eventGraph.eventGraph().getEvents().size());
    }
}