package pro.axenix_innovation.axenapi.web.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessage;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;
import java.util.stream.Collectors;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_GRAPH_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_ID_DUPLICATE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_ID_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_NAME_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_SCHEMA_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_GRAPH_NAME_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_GRAPH_NULL_NODES;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_INVALID_NODE_BROKER_TYPE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_INVALID_NODE_TYPE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_LINKS_NULL_EVENTS;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_LINK_ID_FROM_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_LINK_ID_TO_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_ID_DUPLICATE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_ID_FROM_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_ID_TO_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_IN_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_NAME_NULL;

public class EventGraphDTOValidator {

    private static final Logger log = LoggerFactory.getLogger(EventGraphDTOValidator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AppCodeMessage validateEventGraph(EventGraphDTO eventGraph) {

        log.info(MessageHelper.getStaticMessage("axenapi.info.valid.event.graph", eventGraph));

        try {
            if (eventGraph == null) {
                log.info(MessageHelper.getStaticMessage("axenapi.info.received.event.graph", "null"));
            } else {
                String json = objectMapper.writeValueAsString(eventGraph);
                log.info(MessageHelper.getStaticMessage("axenapi.info.received.event.graph", json));
            }
        } catch (Exception e) {
            log.warn(MessageHelper.getStaticMessage("axenapi.warn.unable.serialize.event.graph", e.getMessage()));
        }

        if (eventGraph == null) {
            return RESP_ERROR_VALID_EVENT_GRAPH_NULL.withArgs();
        }

        if (eventGraph.getNodes() == null) {
            return RESP_ERROR_VALID_GRAPH_NULL_NODES.withArgs();
        }

        if (isCompletelyEmpty(eventGraph)) {
            return null;
        }

        AppCodeMessage validationResult = validateEventGraphName(eventGraph);
        if (validationResult != null) return validationResult;

        validationResult = validateNodes(eventGraph.getNodes());
        if (validationResult != null) return validationResult;

        validationResult = validateEvents(eventGraph.getEvents());
        if (validationResult != null) return validationResult;

        validationResult = validateLinks(eventGraph.getLinks(), eventGraph.getNodes());
        if (validationResult != null) return validationResult;

        return null;
    }

    private static boolean isCompletelyEmpty(EventGraphDTO eventGraph) {
        if (eventGraph == null) {
            log.warn(MessageHelper.getStaticMessage("axenapi.warn.graph.complete.empty.null.object"));
            return true;
        }

        boolean isEmpty = true;

        if (eventGraph.getName() != null) {
            log.info(MessageHelper.getStaticMessage("axenapi.info.event.graph.name", eventGraph.getName()));
            isEmpty = false;
        } else {
            log.info(MessageHelper.getStaticMessage("axenapi.info.event.graph.name", "null"));
        }

        if (checkCollectionField(eventGraph.getNodes(), "nodes")) isEmpty = false;
        if (checkCollectionField(eventGraph.getEvents(), "events")) isEmpty = false;
        if (checkCollectionField(eventGraph.getLinks(), "links")) isEmpty = false;
        if (checkCollectionField(eventGraph.getErrors(), "errors")) isEmpty = false;
        if (checkCollectionField(eventGraph.getTags(), "tags")) isEmpty = false;

        if (isEmpty) {
            log.warn(MessageHelper.getStaticMessage("axenapi.warn.graph.complete.empty.null.fields"));
        }

        return isEmpty;
    }


    private static <T> boolean checkCollectionField(Collection<T> collection, String fieldName) {
        if (collection != null) {
            if (collection.isEmpty()) {
                log.info(MessageHelper.getStaticMessage("axenapi.info.event.graph.empty.field", fieldName));
            } else {
                log.info(MessageHelper.getStaticMessage("axenapi.info.event.graph.items.field", collection.size(), fieldName));
                return true;
            }
        } else {
            log.info(MessageHelper.getStaticMessage("axenapi.info.event.graph.field.null", fieldName));
        }
        return false;
    }


    private static AppCodeMessage validateEventGraphName(EventGraphDTO eventGraph) {
        if (eventGraph.getName() == null || eventGraph.getName().isEmpty()) {
            return RESP_ERROR_VALID_GRAPH_NAME_NULL.withArgs();
        }
        return null;
    }

    private static AppCodeMessage validateNodes(List<NodeDTO> nodes) {

        Set<String> nodeIds = new HashSet<>();
        for (NodeDTO node : nodes) {
            if (node.getType() == null || (!node.getType().equals(NodeDTO.TypeEnum.SERVICE) &&
                    !node.getType().equals(NodeDTO.TypeEnum.TOPIC) && !node.getType().equals(NodeDTO.TypeEnum.HTTP))) {
                return RESP_ERROR_VALID_INVALID_NODE_TYPE.withArgs(node.getType());
            }

            if (node.getBrokerType() != null &&
                    !node.getBrokerType().equals(NodeDTO.BrokerTypeEnum.UNDEFINED) &&
                    !node.getBrokerType().equals(NodeDTO.BrokerTypeEnum.KAFKA) &&
                    !node.getBrokerType().equals(NodeDTO.BrokerTypeEnum.JMS) &&
                    !node.getBrokerType().equals(NodeDTO.BrokerTypeEnum.RABBITMQ)) {
                return RESP_ERROR_VALID_INVALID_NODE_BROKER_TYPE.withArgs(node.getBrokerType());
            }

            if (node.getName() == null || node.getName().isEmpty()) {
                return RESP_ERROR_VALID_NODE_NAME_NULL.withArgs(node.getId());
            }

            if (node.getId() == null) {
                return RESP_ERROR_VALID_NODE_IN_NULL.withArgs();
            }

            if (!nodeIds.add(node.getId().toString())) {
                return RESP_ERROR_VALID_NODE_ID_DUPLICATE.withArgs(node.getId());
            }
        }

        return null;
    }

    private static AppCodeMessage validateEvents(List<EventDTO> events) {
        if (events != null && !events.isEmpty()) {
            Set<String> eventIds = new HashSet<>();
            for (EventDTO event : events) {

                if (event.getId() == null) {
                    return RESP_ERROR_VALID_EVENT_ID_NULL.withArgs();
                }

                if (!eventIds.add(event.getId().toString())) {
                    return RESP_ERROR_VALID_EVENT_ID_DUPLICATE.withArgs(event.getId());
                }

                if (event.getName() == null || event.getName().isEmpty()) {
                    return RESP_ERROR_VALID_EVENT_NAME_NULL.withArgs(event.getId());
                }

                if (event.getSchema() == null || event.getSchema().isEmpty()) {
                    return RESP_ERROR_VALID_EVENT_SCHEMA_NULL.withArgs(event.getId());
                }
            }
        }
        return null;
    }

    private static AppCodeMessage validateLinks(List<LinkDTO> links, List<NodeDTO> nodes) {
        if (links != null && !links.isEmpty()) {
            Set<String> validNodeIds = new HashSet<>();
            for (NodeDTO node : nodes) {
                validNodeIds.add(node.getId().toString());
            }

            for (LinkDTO link : links) {
                if (link.getFromId() == null) {
                    return RESP_ERROR_VALID_LINK_ID_FROM_NULL.withArgs();
                }

                if (link.getToId() == null) {
                    return RESP_ERROR_VALID_LINK_ID_TO_NULL.withArgs();
                }

                if (!validNodeIds.contains(link.getFromId().toString())) {
                    return RESP_ERROR_VALID_NODE_ID_FROM_NULL.withArgs(link.getFromId());
                }
                if (!validNodeIds.contains(link.getToId().toString())) {
                    return RESP_ERROR_VALID_NODE_ID_TO_NULL.withArgs(link.getToId());
                }
            }
        }
        return null;
    }

    public static AppCodeMessage validateLinksHaveEvents(EventGraphDTO eventGraph) {
        if (eventGraph == null) {
            log.warn(MessageHelper.getStaticMessage("axenapi.warn.event.graph.null"));
            return null;
        }
        if (eventGraph.getLinks() == null || eventGraph.getLinks().isEmpty()) {
            log.warn(MessageHelper.getStaticMessage("axenapi.warn.links.null.empty"));
            return null;
        }

        Set<UUID> existingEventIds = eventGraph.getEvents() == null
                ? Collections.emptySet()
                : eventGraph.getEvents().stream()
                .map(EventDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.debug("Existing event IDs: {}", existingEventIds);

        List<String> invalidLinkIds = eventGraph.getLinks().stream()
                .filter(link -> {
                    if (link.getEventId() == null) {
                        return true;
                    }
                    String eventIdStr = link.getEventId().toString();
                    if (eventIdStr.isBlank()) {
                        return true;
                    }
                    try {
                        UUID eventId = UUID.fromString(eventIdStr);
                        return !existingEventIds.contains(eventId);
                    } catch (IllegalArgumentException e) {
                        return true;
                    }
                })
                .map(link -> {
                    if (link.getId() != null) {
                        return link.getId().toString();
                    } else {
                        return "from:" + link.getFromId() + "-to:" + link.getToId();
                    }
                })
                .collect(Collectors.toList());

        if (!invalidLinkIds.isEmpty()) {
            return RESP_ERROR_VALID_LINKS_NULL_EVENTS.withArgs(invalidLinkIds);
        }

        return null;
    }
}
