package pro.axenix_innovation.axenapi.web;

import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestHelper {
    public static EventGraphDTO cloneEventGraph(EventGraphDTO original) {
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

    public static void deepCompare(EventGraphDTO expected, EventGraphDTO actual) {
        EventGraphFacade expectedFacade = new EventGraphFacade(expected);
        EventGraphFacade actualFacade = new EventGraphFacade(actual);
        // Validate root properties
        assertEquals(expected.getName(), actual.getName(), "Graph name mismatch");

        // Order-agnostic node comparison using name as unique key
        assertEquals(expected.getNodes().size(), actual.getNodes().size(), "Node count mismatch");
        Map<String, NodeDTO> actualNodesMap = actual.getNodes().stream()
                .collect(Collectors.toMap(n -> n.getName() + n.getType() + n.getBrokerType(), Function.identity()));

        Map<UUID, NodeDTO> expectedNodesIdMap = expected.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));
        Map<UUID, NodeDTO> actualNodesIdMap = actual.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));

        for (NodeDTO expectedNode : expected.getNodes()) {
            NodeDTO actualNode = actualNodesMap.get(expectedNode.getName() + expectedNode.getType() + expectedNode.getBrokerType());
            assertNotNull(actualNode, "Missing node with name: " + expectedNode.getName());
            assertEquals(expectedNode.getType(), actualNode.getType(), "Node type mismatch for name: " + expectedNode.getName());
            assertEquals(expectedNode.getBrokerType(), actualNode.getBrokerType(), "Node broker type mismatch for name: " + expectedNode.getName());
            for(UUID btgExpected: expectedNode.getBelongsToGraph()) {
                NodeDTO btgExpectedNode = expectedNodesIdMap.get(btgExpected);
                boolean equalBtg = false;
                for(UUID btgActual: actualNode.getBelongsToGraph()) {
                    NodeDTO btgActualNode = actualNodesIdMap.get(btgActual);
                    if (btgExpectedNode != null && btgActualNode != null) {
                        if(btgExpectedNode.getName().equals(btgActualNode.getName()) &&
                                btgActualNode.getBrokerType() == btgExpectedNode.getBrokerType() &&
                                btgActualNode.getType() == btgExpectedNode.getType()) {
                            equalBtg = true;
                            break;
                        }
                    }
                }
                assertTrue(equalBtg, "BelongsToGraph mismatch for node name: " + expectedNode.getName());
            }
            // check tags
            Set<String> expectedTags = new HashSet<>(expectedNode.getTags());
            Set<String> actualTags = new HashSet<>(actualNode.getTags());
            assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for node " + expectedNode.getName());
            assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for node " + expectedNode.getName());
        }

        // Order-agnostic link comparison
        assertEquals(expected.getLinks().size(), actual.getLinks().size(), "Link count mismatch");
        Set<LinkDTO> actualLinksSet = new HashSet<>(actual.getLinks());
        for (LinkDTO expectedLink : expected.getLinks()) {
            // find link nodes
            NodeDTO fromNodeExpected = expectedNodesIdMap.get(expectedLink.getFromId());
            NodeDTO toNodeExpected = expectedNodesIdMap.get(expectedLink.getToId());
            // find event
            EventDTO eventExpected = expectedFacade.getEventById(expectedLink.getEventId());
            // compare nodes
            boolean equalLink = false;
            if (fromNodeExpected != null && toNodeExpected != null && eventExpected != null) {
                for (LinkDTO actualLink : actualLinksSet) {
                    // find link nodes
                    NodeDTO fromNodeActual = actualNodesIdMap.get(actualLink.getFromId());
                    NodeDTO toNodeActual = actualNodesIdMap.get(actualLink.getToId());
                    // find event
                    EventDTO eventActual = actualFacade.getEventById(actualLink.getEventId());
                    //check if fromNode, toNode and event are not null
                    if (fromNodeActual != null && toNodeActual != null && eventActual != null) {
                        // compare expected and actual fromNode, toNode and event by name
                        if (fromNodeExpected.getName().equals(fromNodeActual.getName()) &&
                                toNodeExpected.getName().equals(toNodeActual.getName()) &&
                                eventExpected.getName().equals(eventActual.getName()) &&
                                // compare expected and actual fromNode, toNode by type
                                fromNodeExpected.getType().equals(fromNodeActual.getType()) &&
                                toNodeExpected.getType().equals(toNodeActual.getType()) &&
                                // compare expected and actual fromNode, toNode by brokerType
                                fromNodeExpected.getBrokerType() == fromNodeActual.getBrokerType() &&
                                toNodeExpected.getBrokerType() == toNodeActual.getBrokerType()) {
                            Set<String> expectedTags = new HashSet<>(expectedLink.getTags());
                            Set<String> actualTags = new HashSet<>(actualLink.getTags());
                            assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for link " + expectedLink);
                            assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for link " + equalLink);
                            equalLink = true;
                            break;
                        }
                    }


                }
            }
            assertTrue(equalLink, "Link mismatch from " + fromNodeExpected.getName() + " to " + toNodeExpected.getName() + " with event " + eventExpected.getName());
        }

        // Map-based event comparison
        assertEquals(expected.getEvents().size(), actual.getEvents().size(), "Event count mismatch");
        for (EventDTO expectedEvent : expected.getEvents()) {
            EventDTO actualEvent = actualFacade.getEvent(expectedEvent.getName());
            assertNotNull(actualEvent, "no event with name " + expectedEvent.getName());
            assertEquals(expectedEvent.getName(), actualEvent.getName(), "Event name mismatch for key: " + expectedEvent.getName());
            //remove blanks from schemas before matching
            String expectedEventSchema = expectedEvent.getSchema().replaceAll("\\s+", "");
            String actualEventSchema = actualEvent.getSchema().replaceAll("\\s+", "");
            assertEquals(expectedEventSchema, actualEventSchema, "Event schema mismatch for key: " + expectedEvent.getName());
            Set<String> expectedTags = new HashSet<>(expectedEvent.getTags());
            Set<String> actualTags = new HashSet<>(actualEvent.getTags());
            assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for event " + expectedEvent.getName());
            assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for event " + expectedEvent.getName());

        }

        // Order-agnostic error comparison
        assertEquals(expected.getErrors().size(), actual.getErrors().size(), "Error count mismatch");
        Set<ErrorDTO> actualErrorsSet = new HashSet<>(actual.getErrors());
        for (ErrorDTO expectedError : expected.getErrors()) {
            assertTrue(actualErrorsSet.contains(expectedError), "Missing error: " + errorToString(expectedError));
        }
    }

    private static String errorToString(ErrorDTO error) {
        return String.format("Error{filename=%s, error=%s}", error.getFileName(), error.getErrorMessage());
    }
}



