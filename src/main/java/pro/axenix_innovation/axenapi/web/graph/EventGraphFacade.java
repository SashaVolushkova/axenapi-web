package pro.axenix_innovation.axenapi.web.graph;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.FAIL_FIND_NODES_FOR_LINK;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.FAIL_FIND_NODES_FOR_LINK_G2;


public record EventGraphFacade(EventGraphDTO eventGraph) {
    private static final Logger log = LoggerFactory.getLogger(EventGraphFacade.class);


    public void setName(String title) {
        eventGraph.setName(title);
    }

    public void addNode(NodeDTO serviceNode) {
        //null safety
        if (serviceNode == null && eventGraph.getNodes() != null) {
            return;
        }
        eventGraph.getNodes().add(serviceNode);
    }

    public EventDTO getEvent(String schemaName) {
        if (eventGraph.getEvents() == null) {
            return null;
        }
        return eventGraph.getEvents()
                .stream()
                .filter(e -> e.getName().equals(schemaName))
                .findFirst().orElse(null);
    }

    public void addEvent(EventDTO event) {
        eventGraph.addEventsItem(event);
    }

    public boolean containsNode(String nodeName, NodeDTO.TypeEnum nodeType, NodeDTO.BrokerTypeEnum brokerType) {
        return eventGraph.getNodes().stream()
                .anyMatch(node -> node.getName().equals(nodeName) &&
                        node.getType() == nodeType &&
                        node.getBrokerType() == brokerType);
    }


    public NodeDTO getNode(String topic, NodeDTO.TypeEnum typeEnum) {
        return eventGraph.getNodes().stream()
                .filter(node -> node.getName().equals(topic) && node.getType() == typeEnum)
                .findFirst().orElse(null);
    }

    public void addLink(LinkDTO incomingLink) {
        eventGraph.addLinksItem(incomingLink);
    }

    public static EventGraphDTO merge(EventGraphDTO g1, EventGraphDTO g2) {
        if (g1 == null) {
            log.info(MessageHelper.getStaticMessage("axenapi.info.graph.1.null.return.2"));
            return g2;
        }
        if (g2 == null) {
            log.info(MessageHelper.getStaticMessage("axenapi.info.graph.2.null.return.1"));
            return g1;
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.merging.two.graph"));
        EventGraphDTO merged = new EventGraphDTO();
        EventGraphFacade mergedFacade = new EventGraphFacade(merged);
        mergedFacade.addAllTagsInGraph(g1.getTags());
        mergedFacade.addAllTagsInGraph(g2.getTags());
        if (!Strings.isBlank(g2.getName())) {
            if(!Strings.isBlank(g1.getName())) {
                if(!g1.getName().contains(g2.getName())) {
                    merged.setName(g1.getName() + "&" + g2.getName());
                } else {
                    merged.setName(g1.getName());
                }
            } else {
                merged.setName(g2.getName());
            }
        } else {
            merged.setName(g1.getName());
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.add.node.from.graph"));
        g1.getNodes().forEach(node -> {
            log.debug("Adding node: {}", node.getName());
            log.debug("Node tags: {}", node.getTags());
            merged.addNodesItem(NodeDTO.builder()
                    .id(node.getId())
                    .name(node.getName())
                    .brokerType(node.getBrokerType())
                    .type(node.getType())
                    .belongsToGraph(new ArrayList<>(node.getBelongsToGraph()))
                    .tags(new HashSet<>(node.getTags()))
                    .methodType(node.getMethodType())
                    .requestBody(node.getRequestBody())
                    .nodeUrl(node.getNodeUrl())
                    .nodeDescription(node.getNodeDescription())
                    .documentationFileLinks(node.getDocumentationFileLinks() != null
                            ? new HashSet<>(node.getDocumentationFileLinks())
                            : null)
                    .build());
        });

        log.info(MessageHelper.getStaticMessage("axenapi.info.process.node.from.graph.2"));
        g2.getNodes().forEach(n -> {
            NodeDTO existingNode = merged.getNodes().stream()
                    .filter(node ->
                            node.getType() == n.getType() &&
                                    node.getName().equals(n.getName()) &&
                                    node.getBrokerType() == n.getBrokerType())
                    .findFirst().orElse(null);

            if (existingNode != null) {
                log.debug("Found existing node: {}", existingNode.getName());
                existingNode.getBelongsToGraph().addAll(n.getBelongsToGraph());
                existingNode.getTags().addAll(n.getTags());
            } else {
                log.debug("No existing node found, adding new node: {}", n.getName());
                merged.addNodesItem(NodeDTO.builder()
                        .id(n.getId())
                        .name(n.getName())
                        .brokerType(n.getBrokerType())
                        .type(n.getType())
                        .belongsToGraph(new ArrayList<>(n.getBelongsToGraph()))
                        .tags(new HashSet<>(n.getTags()))
                        .methodType(n.getMethodType())
                        .requestBody(n.getRequestBody())
                        .nodeUrl(n.getNodeUrl())
                        .nodeDescription(n.getNodeDescription())
                        .documentationFileLinks(n.getDocumentationFileLinks() != null
                                ? new HashSet<>(n.getDocumentationFileLinks())
                                : null)
                        .build());
            }
        });


        log.info(MessageHelper.getStaticMessage("axenapi.info.merge.events.both.graph"));
        g1.getEvents().forEach(( event) -> {
            log.debug("Adding event with ID {}: {}", event.getId(), event.getName());
            log.debug("Event tags: {}", event.getTags());
            EventDTO eventDTOMerged = merged.getEvents().stream().filter(e -> e.getName().equals(event.getName())).findFirst().orElse(null);
            if (eventDTOMerged != null) {
                log.debug("Event with name {} already exists in merged graph.", event.getName());
                eventDTOMerged.getTags().addAll(event.getTags());
                log.debug("Merged event tags: {}", eventDTOMerged.getTags());
            } else {
                merged.addEventsItem(EventDTO.builder()
                        .id(event.getId())
                        .name(event.getName())
                        .schema(event.getSchema())
                        .tags(new HashSet<>(event.getTags()))
                        .build());
            }
        });
        g2.getEvents().forEach((event) -> {
            log.debug("Adding event with ID {}: {}", event.getId(), event.getName());
            log.debug("Event tags: {}", event.getTags());
            EventDTO eventDTOMerged = merged.getEvents().stream().filter(e -> e.getName().equals(event.getName())).findFirst().orElse(null);
            if (eventDTOMerged != null) {
                log.debug("Event with name {} already exists in merged graph.", event.getName());
                eventDTOMerged.getTags().addAll(event.getTags());
                log.debug("Merged event tags: {}", eventDTOMerged.getTags());
            } else {
                merged.addEventsItem(EventDTO.builder()
                        .id(event.getId())
                        .name(event.getName())
                        .schema(event.getSchema())
                        .tags(new HashSet<>(event.getTags()))
                        .build());
            }
        });

        // Merge links from both graphs
        log.info(MessageHelper.getStaticMessage("axenapi.info.merge.links.both.graph"));
        // Add all links from g1 into merged
        g1.getLinks().forEach(l -> {
            log.debug("Processing link from g1: fromId = {}, toId = {}", l.getFromId(), l.getToId());

            boolean linkExists = merged.getLinks().stream().anyMatch(existingLink ->
                    existingLink.getFromId().equals(l.getFromId()) &&
                            existingLink.getToId().equals(l.getToId()) &&
                            Objects.equals(existingLink.getEventId(), l.getEventId())
            );

            if (!linkExists) {
                merged.addLinksItem(new LinkDTO(
                        l.getId(),
                        l.getFromId(), l.getToId(), l.getGroup(), l.getEventId(), l.getTags()
                ));
            } else {
                log.debug("Duplicate link found in g1, skipping: fromId = {}, toId = {}, eventId = {}", l.getFromId(), l.getToId(), l.getEventId());
            }
        });


        // Process links from g2 and avoid duplicates
        g2.getLinks().forEach(l -> {
            log.debug("Processing link from g2: fromId = {}, toId = {}", l.getFromId(), l.getToId());
            // Get nodes from g2
            NodeDTO fromG2 = g2.getNodes().stream().filter(node -> node.getId().equals(l.getFromId())).findFirst().orElse(null);
            NodeDTO toG2 = g2.getNodes().stream().filter(node -> node.getId().equals(l.getToId())).findFirst().orElse(null);
            
            // Handle case where eventId might be null (for undefined_event)
            EventDTO eventG2 = null;
            if (l.getEventId() != null) {
                eventG2 = g2.getEvents().stream().filter(e -> e.getId().equals(l.getEventId())).findFirst().orElse(null);
            }

            if (fromG2 != null && toG2 != null) {
                // Find merged nodes based on name, type, and brokerType
                NodeDTO mergedFrom = mergedFacade.getNode(fromG2.getName(), fromG2.getType(), fromG2.getBrokerType());
                NodeDTO mergedTo = mergedFacade.getNode(toG2.getName(), toG2.getType(), toG2.getBrokerType());
                
                // Handle event merging - can be null for undefined_event
                EventDTO eventMerged = null;
                if (eventG2 != null) {
                    eventMerged = mergedFacade.getEvent(eventG2.getName());
                }
                final UUID eventMergedId = eventMerged != null ? eventMerged.getId() : null;

                if (mergedFrom != null && mergedTo != null) {
                    // Check if the link already exists
                    boolean linkExists = merged.getLinks().stream().anyMatch(existingLink ->
                            existingLink.getFromId().equals(mergedFrom.getId()) &&
                                    existingLink.getToId().equals(mergedTo.getId()) &&
                                    Objects.equals(existingLink.getEventId(), eventMergedId)
                    );

                    if (!linkExists) {
                        // Add new link if not exists
                        merged.addLinksItem(new LinkDTO(UUID.randomUUID(), mergedFrom.getId(), mergedTo.getId(), l.getGroup(), eventMergedId, l.getTags()));
                    } else {
                        log.debug("Link already exists: fromId = {}, toId = {}, eventId = {}", mergedFrom.getId(), mergedTo.getId(), eventMergedId);
                    }
                } else {
                    log.error(MessageHelper.getStaticMessage(FAIL_FIND_NODES_FOR_LINK, l.getFromId(), l.getToId(), l.getEventId()));
                }
            } else {
                log.error(MessageHelper.getStaticMessage(FAIL_FIND_NODES_FOR_LINK_G2, l.getFromId(), l.getToId(), l.getEventId()));
            }
        });

        log.info(MessageHelper.getStaticMessage("axenapi.info.merge.graph.success"));
        return merged;
    }

    public NodeDTO getNode(@NotNull String name, NodeDTO.TypeEnum type, NodeDTO.BrokerTypeEnum brokerType) {
        return eventGraph.getNodes().stream()
                .filter(node -> node.getName().equals(name) &&
                        node.getType() == type
                        && node.getBrokerType() == brokerType)
                .findFirst().orElse(null);
    }

    public EventDTO getEventById(UUID id) {
        return eventGraph.getEvents().stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public NodeDTO getNodeById(@NotNull @Valid UUID fromId) {
        return eventGraph.getNodes().stream()
                .filter(node -> node.getId().equals(fromId))
                .findFirst().orElse(null);
    }

    public List<NodeDTO> getNodes() {
        return eventGraph.getNodes();
    }

    public List<LinkDTO> getLinks() {
        return eventGraph.getLinks();
    }

    public void addAllNodes(List<NodeDTO> httNode) {
        for (NodeDTO nodeDTO : httNode) {
            eventGraph.addNodesItem(nodeDTO);
        }

    }

    public void addAllTagsInGraph(Set<String> allTags) {
        if(allTags != null) {
            for (String allTag : allTags) {
                eventGraph.addTagsItem(allTag);
            }
        }
    }

    public void updateNode(NodeDTO updatedNode) {
        if (updatedNode == null || eventGraph == null || eventGraph.getNodes() == null) {
            return;
        }

        for (int i = 0; i < eventGraph.getNodes().size(); i++) {
            NodeDTO existing = eventGraph.getNodes().get(i);
            if (existing.getId().equals(updatedNode.getId())) {
                eventGraph.getNodes().set(i, updatedNode);
                return;
            }
        }
    }
}