package pro.axenix_innovation.axenapi.web.graph;

import org.junit.jupiter.api.Test;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;


import static org.junit.jupiter.api.Assertions.*;
import static pro.axenix_innovation.axenapi.web.graph.EventGraphFacade.merge;

import java.util.*;

public class EventGraphFacadeMergeTest {
    @Test
    public void test_merge_with_null_graph() {
        UUID nodeId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        NodeDTO node = NodeDTO.builder()
                .id(nodeId)
                .name("Node1")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(nodeId))
                .build();

        EventDTO event = EventDTO.builder()
                .id(eventId)
                .name("Event1")
                .schema("schema1")
                .build();

        EventGraphDTO graph = EventGraphDTO.builder()
                .name("Graph1")
                .nodes(Collections.singletonList(node))
                .events(Collections.singletonList(event))
                .build();

        EventGraphDTO merged = merge(graph, null);

        assertNotNull(merged);
        assertEquals(graph, merged);
        assertEquals(1, merged.getNodes().size());
        assertEquals(1, merged.getEvents().size());
    }

    // ensure that g2 is not changed after merge
    @Test
    public void test_g2_unchanged_after_merge() {
        UUID node1Id = UUID.randomUUID();
        UUID node2Id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        NodeDTO node1 = NodeDTO.builder()
                .id(node1Id)
                .name("Node1")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(node1Id))
                .build();

        NodeDTO node2 = NodeDTO.builder()
                .id(node2Id)
                .name("Node2")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(node2Id))
                .build();

        EventDTO event = EventDTO.builder()
                .id(eventId)
                .name("Event1")
                .schema("schema1")
                .build();

        LinkDTO link = LinkDTO.builder()
                .id(UUID.randomUUID())
                .fromId(node1Id)
                .toId(node2Id)
                .eventId(eventId)
                .build();

        EventGraphDTO g1 = EventGraphDTO.builder()
                .name("Graph1")
                .nodes(Collections.singletonList(node1))
                .events(Collections.singletonList(event))
                .links(Collections.singletonList(link))
                .build();

        EventGraphDTO g2 = EventGraphDTO.builder()
                .name("Graph2")
                .nodes(Collections.singletonList(node2))
                .events(new ArrayList<>())
                .links(new ArrayList<>())
                .build();

        // Clone g2 for comparison after merge
        EventGraphDTO g2Clone = cloneEventGraph(g2);
        EventGraphDTO g1Clone = cloneEventGraph(g1);

        // Perform the merge
        EventGraphDTO merge = merge(g1, g2);

        // Assert that all elements in collections in g2 are the same as in g2Clone

        assertIterableEquals(g2Clone.getNodes(), g2.getNodes());
        for(int i = 0; i < g2.getNodes().size(); i++) {
            assertIterableEquals(g2Clone.getNodes().get(i).getBelongsToGraph(), g2.getNodes().get(i).getBelongsToGraph());
        }
        assertIterableEquals(g2Clone.getLinks(), g2.getLinks());
        assertIterableEquals(g2Clone.getEvents(), g2.getEvents());
        assertIterableEquals(g2Clone.getEvents(), g2.getEvents());
        assertEquals(g2Clone.getName(), g2.getName());

        // Assert that result is equals to g1
        assertNotSame(g1, merge);
        assertEquals(g1Clone.getName(), g1.getName());
        assertIterableEquals(g1Clone.getNodes(), g1.getNodes());
        for(int i = 0; i < g1.getNodes().size(); i++) {
            assertIterableEquals(g1Clone.getNodes().get(i).getBelongsToGraph(), g1.getNodes().get(i).getBelongsToGraph());
        }
        assertIterableEquals(g1Clone.getLinks(), g1.getLinks());
        assertIterableEquals(g1Clone.getEvents(), g1.getEvents());
    }



    // Merging two non-empty graphs with unique nodes and events
    @Test
    public void test_merge_two_non_empty_graphs() {
        UUID node1Id = UUID.randomUUID();
        UUID node2Id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        NodeDTO node1 = NodeDTO.builder()
                .id(node1Id)
                .name("node1")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(node1Id))
                .build();

        NodeDTO node2 = NodeDTO.builder()
                .id(node2Id)
                .name("node2")
                .type(NodeDTO.TypeEnum.TOPIC)
                .belongsToGraph(List.of(node2Id))
                .build();

        EventDTO event = EventDTO.builder()
                .id(eventId)
                .name("event1")
                .schema("schema1")
                .build();

        LinkDTO link = LinkDTO.builder()
                .id(UUID.randomUUID())
                .fromId(node1Id)
                .toId(node2Id)
                .eventId(eventId)
                .build();

        EventGraphDTO g1 = EventGraphDTO.builder()
                .name("graph1")
                .nodes(Collections.singletonList(node1))
                .events(Collections.singletonList(event))
                .links(Collections.singletonList(link))
                .build();

        EventGraphDTO g2 = EventGraphDTO.builder()
                .name("graph2")
                .nodes(Collections.singletonList(node2))
                .events(new ArrayList<>())
                .links(new ArrayList<>())
                .build();

        EventGraphDTO merged = merge(g1, g2);

        assertEquals(2, merged.getNodes().size());
        assertEquals(1, merged.getEvents().size());
        assertEquals(1, merged.getLinks().size());
    }


    // Handling null input for first graph returns second graph
    @Test
    public void test_merge_null_first_graph() {
        UUID nodeId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        NodeDTO node = new NodeDTO(List.of(nodeId), "node", NodeDTO.TypeEnum.SERVICE);
        node.setId(nodeId);
        EventDTO event = new EventDTO(eventId, "schema", "event");

        EventGraphDTO g2 = new EventGraphDTO();
        g2.addNodesItem(node);
        g2.addEventsItem(event);

        EventGraphDTO merged = EventGraphFacade.merge(null, g2);

        assertNotNull(merged);
        assertEquals(g2, merged);
        assertEquals(1, merged.getNodes().size());
        assertEquals(1, merged.getEvents().size());
        assertTrue(merged.getNodes().contains(node));
        assertEquals(event, merged.getEvents().stream().filter(e -> e.getId().equals(eventId)).findFirst().orElse(null));
    }

    // Handling broker type updates when merging nodes with different broker types
    @Test
    public void test_merge_nodes_with_different_broker_types() {
        UUID node1Id = UUID.randomUUID();
        UUID node2Id = UUID.randomUUID();

        NodeDTO node1 = NodeDTO.builder()
                .name("node1")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build().addBelongsToGraphItem(node1Id);
        node1.setId(node1Id);
        node1.setBrokerType(NodeDTO.BrokerTypeEnum.KAFKA);

        NodeDTO node2 = NodeDTO.builder()
                .id(node2Id)
                .name("node2")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build()
                .addBelongsToGraphItem(node2Id);
        node2.setId(node1Id); // Same ID to simulate merging
        node2.setBrokerType(NodeDTO.BrokerTypeEnum.RABBITMQ);

        EventGraphDTO g1 = new EventGraphDTO();
        g1.addNodesItem(node1);

        EventGraphDTO g2 = new EventGraphDTO();
        g2.addNodesItem(node2);

        EventGraphDTO merged = EventGraphFacade.merge(g1, g2);

        assertEquals(2, merged.getNodes().size());
    }

    // Handling null input for second graph returns first graph
    @Test
    public void test_merge_null_second_graph() {
        UUID nodeId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        NodeDTO node = new NodeDTO(List.of(nodeId), "node", NodeDTO.TypeEnum.SERVICE);
        node.setId(nodeId);
        EventDTO event = new EventDTO(eventId, "schema", "event");
        EventGraphDTO g1 = new EventGraphDTO();
        g1.addNodesItem(node);
        g1.addEventsItem(event);
        EventGraphDTO merged = EventGraphFacade.merge(g1, null);
        assertNotNull(merged);
        assertEquals(g1, merged);
        assertEquals(1, merged.getNodes().size());
        assertTrue(merged.getNodes().contains(node));
        assertEquals(event, merged.getEvents()
                .stream().filter(e -> e.getId().equals(eventId)).findFirst().orElse(null));

    }

    // both graphs are null
    @Test
    public void test_merge_both_graphs_null() {
        EventGraphDTO g1 = null;
        EventGraphDTO g2 = null;

        EventGraphDTO merged = EventGraphFacade.merge(g1, g2);

        assertNull(merged);
    }

    // Preserving original node and event IDs during merge
    @Test
    public void test_preserve_original_ids_during_merge() {
        UUID node1Id = UUID.randomUUID();
        UUID node2Id = UUID.randomUUID();
        UUID event1Id = UUID.randomUUID();
        UUID event2Id = UUID.randomUUID();

        NodeDTO node1 = new NodeDTO(List.of(node1Id), "node1", NodeDTO.TypeEnum.SERVICE);
        node1.setId(node1Id);
        NodeDTO node2 = new NodeDTO(List.of(node2Id), "node2", NodeDTO.TypeEnum.TOPIC);
        node2.setId(node2Id);

        EventDTO event1 = new EventDTO(event1Id, "schema1", "event1");
        EventDTO event2 = new EventDTO(event2Id, "schema2", "event2");

        EventGraphDTO g1 = new EventGraphDTO();
        g1.addNodesItem(node1);
        g1.addEventsItem(event1);

        EventGraphDTO g2 = new EventGraphDTO();
        g2.addNodesItem(node2);
        g2.addEventsItem(event2);

        EventGraphDTO merged = EventGraphFacade.merge(g1, g2);

        assertEquals(2, merged.getNodes().size());
        assertEquals(2, merged.getEvents().size());
        assertTrue(merged.getNodes().stream().anyMatch(n -> n.getId().equals(node1Id)));
        assertTrue(merged.getNodes().stream().anyMatch(n -> n.getId().equals(node2Id)));
        assertEquals(event1, merged.getEvents().stream().filter(e -> e.getId().equals(event1Id)).findFirst().orElse(null));
        assertEquals(event2, merged.getEvents().stream().filter(e -> e.getId().equals(event2Id)).findFirst().orElse(null));
    }

    // merge graph with common topic (by name and brokerType)
    @Test
    public void test_merge_graph_with_common_topic() {
        // Create first graph with a common topic node
        EventGraphDTO g1 = new EventGraphDTO();
        g1.setName("Graph1");

        UUID commonNodeId = UUID.randomUUID();
        NodeDTO commonNode = new NodeDTO();
        commonNode.setId(commonNodeId);
        commonNode.setName("CommonTopic");
        commonNode.setType(NodeDTO.TypeEnum.TOPIC);
        commonNode.setBrokerType(NodeDTO.BrokerTypeEnum.KAFKA);
        commonNode.addBelongsToGraphItem(commonNodeId);
        g1.addNodesItem(commonNode);

        // Add events to first graph
        UUID event1Id = UUID.randomUUID();
        EventDTO event1 = new EventDTO(event1Id, "schema1", "Event1");
        g1.addEventsItem(event1);

        // Create second graph with the same common topic node
        EventGraphDTO g2 = new EventGraphDTO();
        g2.setName("Graph2");

        NodeDTO sameCommonNode = new NodeDTO();
        sameCommonNode.setId(UUID.randomUUID());
        sameCommonNode.setName("CommonTopic");
        sameCommonNode.setType(NodeDTO.TypeEnum.TOPIC);
        sameCommonNode.setBrokerType(NodeDTO.BrokerTypeEnum.KAFKA);
        sameCommonNode.addBelongsToGraphItem(sameCommonNode.getId());
        g2.addNodesItem(sameCommonNode);

        // Add events to second graph
        UUID event2Id = UUID.randomUUID();
        EventDTO event2 = new EventDTO(event2Id, "schema2", "Event2");
        g2.addEventsItem(event2);

        // Merge graphs
        EventGraphDTO merged = EventGraphFacade.merge(g1, g2);

        // Verify merged graph
        assertEquals("Graph1&Graph2", merged.getName());
        assertEquals(1, merged.getNodes().size());// Common node should not be duplicated
        assertIterableEquals(List.of(commonNode.getId(), sameCommonNode.getId()), merged.getNodes().get(0).getBelongsToGraph());
        assertEquals(2, merged.getEvents().size());

        // Verify the common node is present and not duplicated
        boolean hasCommonNode = merged.getNodes().stream()
                .anyMatch(n -> n.getId().equals(commonNodeId) && n.getName().equals("CommonTopic"));
        assertTrue(hasCommonNode);

        // Verify events were merged correctly
        assertTrue(merged.getEvents().stream().anyMatch(e -> e.getName().equals(event1.getName())));
        assertTrue(merged.getEvents().stream().anyMatch(e -> e.getName().equals(event2.getName())));
    }

    // merge graphs with one service (different names) and common topic (same name and brokeType)
    @Test
    public void test_merge_graphs_with_common_topic() {
        // Create first graph with a service node
        EventGraphDTO g1 = new EventGraphDTO();
        g1.setName("ServiceNode1");

        UUID serviceNodeId1 = UUID.randomUUID();
        NodeDTO serviceNode1 = new NodeDTO();
        serviceNode1.setId(serviceNodeId1);
        serviceNode1.setName("ServiceNode1");
        serviceNode1.setType(NodeDTO.TypeEnum.SERVICE);
        serviceNode1.addBelongsToGraphItem(serviceNodeId1);
        g1.addNodesItem(serviceNode1);

        UUID topicNodeId1 = UUID.randomUUID();
        NodeDTO topicNode1 = new NodeDTO();
        topicNode1.setId(topicNodeId1);
        topicNode1.setName("CommonTopic");
        topicNode1.setType(NodeDTO.TypeEnum.TOPIC);
        topicNode1.setBrokerType(NodeDTO.BrokerTypeEnum.KAFKA);
        topicNode1.addBelongsToGraphItem(serviceNodeId1);
        g1.addNodesItem(topicNode1);

        // Create second graph with a topic node having the same name and broker type
        EventGraphDTO g2 = new EventGraphDTO();
        g2.setName("ServiceNode2");

        UUID serviceNodeId2 = UUID.randomUUID();
        NodeDTO serviceNode2 = new NodeDTO();
        serviceNode2.setId(serviceNodeId2);
        serviceNode2.setName("ServiceNode2");
        serviceNode2.setType(NodeDTO.TypeEnum.SERVICE);
        serviceNode2.addBelongsToGraphItem(serviceNodeId2);
        g2.addNodesItem(serviceNode2);


        UUID topicNodeId = UUID.randomUUID();
        NodeDTO topicNode = new NodeDTO();
        topicNode.setId(topicNodeId);
        topicNode.setName("CommonTopic");
        topicNode.setType(NodeDTO.TypeEnum.TOPIC);
        topicNode.setBrokerType(NodeDTO.BrokerTypeEnum.KAFKA);
        topicNode.addBelongsToGraphItem(serviceNodeId2);
        g2.addNodesItem(topicNode);

        // Merge graphs
        EventGraphDTO merged = EventGraphFacade.merge(g1, g2);

        // Verify merged graph
        assertEquals("ServiceNode1&ServiceNode2", merged.getName());
        assertEquals(3, merged.getNodes().size());
    }

    // merge two graphs with common input link (to - service from topic)
    @Test
    public void test_merge_with_common_input_link() {
        // Create first graph
        EventGraphDTO g1 = new EventGraphDTO();
        g1.setName("Graph1");

        // Add nodes to first graph
        UUID serviceNodeId = UUID.randomUUID();
        NodeDTO serviceNode = NodeDTO.builder()
                .id(serviceNodeId)
                .name("ServiceNode")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>(List.of(serviceNodeId)))
                .build();
        g1.addNodesItem(serviceNode);

        UUID topicNodeId = UUID.randomUUID();
        NodeDTO topicNode = NodeDTO.builder()
                .id(topicNodeId)
                .name("TopicNode")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.KAFKA)
                .belongsToGraph(new ArrayList<>(List.of(serviceNodeId)))
                .build();
        g1.addNodesItem(topicNode);

        // Add events to first graph
        UUID eventId = UUID.randomUUID();
        EventDTO event = EventDTO.builder()
                .id(eventId)
                .name("Event")
                .schema("Schema")
                .build();
        g1.addEventsItem(event);

        // Add link to first graph
        LinkDTO link1 = new LinkDTO(topicNodeId, serviceNodeId, eventId);
        g1.addLinksItem(link1);

        // Create second graph
        EventGraphDTO g2 = new EventGraphDTO();
        g2.setName("Graph2");

        // Add nodes to second graph
        UUID anotherServiceNodeId = UUID.randomUUID();
        NodeDTO anotherServiceNode = NodeDTO.builder()
                .id(anotherServiceNodeId)
                .name("AnotherServiceNode")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>(List.of(anotherServiceNodeId)))
                .build();

        UUID anotherTopicNodeId = UUID.randomUUID();
        NodeDTO anotherTopicNode = NodeDTO.builder()
                .id(anotherTopicNodeId)
                .name("TopicNode")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.KAFKA)
                .belongsToGraph(new ArrayList<>(List.of(anotherServiceNodeId)))
                .build();
        g2.addNodesItem(anotherTopicNode);
        g2.addNodesItem(anotherServiceNode);
        UUID anotherEventId = UUID.randomUUID();
        EventDTO anotherEvent = EventDTO.builder()
                .id(anotherEventId)
                .name("Event")
                .schema("Schema")
                .build();

        g2.addEventsItem(anotherEvent);
        // Add link to second graph with common input link
        LinkDTO link2 = new LinkDTO(anotherTopicNodeId, anotherServiceNodeId, anotherEventId);
        g2.addLinksItem(link2);

        // Merge the graphs
        EventGraphDTO merged = EventGraphFacade.merge(g1, g2);

        // Verify the merged graph
        assertEquals("Graph1&Graph2", merged.getName());
        assertEquals(3, merged.getNodes().size());
        assertEquals(1, merged.getEvents().size());
        assertEquals(2, merged.getLinks().size());

        // Verify nodes were merged correctly
        assertTrue(merged.getNodes().stream().anyMatch(n -> n.getName().equals("ServiceNode")));
        assertTrue(merged.getNodes().stream().anyMatch(n -> n.getName().equals("AnotherServiceNode")));
        assertTrue(merged.getNodes().stream().anyMatch(n -> n.getName().equals("TopicNode")));

        // Verify events were merged correctly
        assertTrue(merged.getEvents().stream().anyMatch(e -> e.getName().equals("Event")));

        // Verify links were merged correctly
        boolean hasLink1 = merged.getLinks().stream().anyMatch(l ->
                merged.getNodes().stream().anyMatch(n -> n.getId().equals(l.getFromId()) && n.getName().equals("TopicNode")) &&
                        merged.getNodes().stream().anyMatch(n -> n.getId().equals(l.getToId()) && n.getName().equals("ServiceNode")));

        boolean hasLink2 = merged.getLinks().stream().anyMatch(l ->
                merged.getNodes().stream().anyMatch(n -> n.getId().equals(l.getFromId()) && n.getName().equals("TopicNode")) &&
                        merged.getNodes().stream().anyMatch(n -> n.getId().equals(l.getToId()) && n.getName().equals("AnotherServiceNode")));

        assertTrue(hasLink1);
        assertTrue(hasLink2);
    }

    public EventGraphDTO cloneEventGraph(EventGraphDTO original) {
        EventGraphDTO clone = new EventGraphDTO();
        clone.setName(original.getName());
        // Deep copy nodes
        List<NodeDTO> clonedNodes = new ArrayList<>();
        for (NodeDTO node : original.getNodes()) {
            NodeDTO clonedNode = new NodeDTO();
            clonedNode.setId(node.getId());
            clonedNode.setBelongsToGraph(new ArrayList<>(node.getBelongsToGraph()));
            clonedNode.setName(node.getName());
            clonedNode.setType(node.getType());
            clonedNode.setBrokerType(node.getBrokerType());
            clonedNodes.add(clonedNode);
        }
        clone.setNodes(clonedNodes);

        // Deep copy links
        List<LinkDTO> clonedLinks = new ArrayList<>();
        for (LinkDTO link : original.getLinks()) {
            LinkDTO clonedLink = new LinkDTO();
            clonedLink.setId(link.getId());
            clonedLink.setFromId(link.getFromId());
            clonedLink.setToId(link.getToId());
            clonedLink.setEventId(link.getEventId());
            clonedLinks.add(clonedLink);
        }
        clone.setLinks(clonedLinks);

        // Deep copy events
        List<EventDTO> clonedEvents = new ArrayList<>();
        for (EventDTO originalEvent : original.getEvents()) {
            EventDTO clonedEvent = new EventDTO();
            clonedEvent.setId(originalEvent.getId());
            clonedEvent.setSchema(originalEvent.getSchema());
            clonedEvent.setName(originalEvent.getName());
            clonedEvents.add(clonedEvent);
        }
        clone.setEvents(clonedEvents);
        return clone;
    }


}
