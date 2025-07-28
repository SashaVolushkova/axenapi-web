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

    public static UUID createServiceNode(EventGraphFacade eventGraph, String title, List<String> documentationFileLinks, UUID serviceNodeId) {
        UUID serviceNodeUUId = serviceNodeId == null ? UUID.randomUUID() : serviceNodeId;

        NodeDTO serviceNode = NodeDTO.builder()
                .id(serviceNodeUUId)
                .name(title)
                .type(SERVICE)
                .brokerType(null)
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
                    if (linkEvent == null) {
                        ApiResponse response = operation.getResponses() != null ? operation.getResponses().get("200") : null;
                        if (response != null && response.getContent() != null) {
                            MediaType media = response.getContent().get("application/json");
                            if (media != null && media.getSchema() != null) {
                                Schema<?> schema = media.getSchema();
                                EventDTO event = resolveEventFromSchema(schema, components, createdEvents);
                                if (event != null) {
                                    eventGraph.addEvent(event);
                                    linkEvent = event;
                                }
                            }
                        }
                    }
                    if (linkEvent != null) {
                        if (linkEvent.getTags() == null) {
                            linkEvent.setTags(new HashSet<>());
                        }

                        if (pathTags != null && !pathTags.isEmpty()) {
                            linkEvent.getTags().addAll(pathTags);
                        }

                        linkEvent.getTags().add("HTTP");

                        for (NodeDTO nodeDTO : httpNodes) {
                            createHttpLink(nodeDTO, serviceNode, linkEvent, eventGraph);
                        }
                    } else {
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
                    .eventDescription(resolvedSchema.getDescription() != null ? resolvedSchema.getDescription() : schemaName)
                    .schema(Json.pretty(resolvedSchema))
                    .tags(new HashSet<>())
                    .build();
            createdEvents.put(schemaName, event);
            return event;
        }
        return null;
    }

    private static List<NodeDTO> createHttpNodes(String key, PathItem path, Set<String> pathTags, NodeDTO serviceNode, Components components) {
        String desc = path.getDescription();
        Map<PathItem.HttpMethod, Operation> map = path.readOperationsMap();
        List<NodeDTO> res = new ArrayList<>();

        for (Map.Entry<PathItem.HttpMethod, Operation> entry : map.entrySet()) {
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
}