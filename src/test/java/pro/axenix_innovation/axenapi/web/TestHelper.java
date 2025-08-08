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

        // Сравнение tags на уровне EventGraphDTO
        if (expected.getTags() != null && actual.getTags() != null) {
            Set<String> expectedGraphTags = new HashSet<>(expected.getTags());
            Set<String> actualGraphTags = new HashSet<>(actual.getTags());
            System.out.println("===TAGS " + Arrays.toString(actualGraphTags.toArray()));
            assertEquals(expectedGraphTags.size(), actualGraphTags.size(), "EventGraph tags collection size mismatch");
            assertTrue(actualGraphTags.containsAll(expectedGraphTags), "Not all tags are in actual EventGraph");
        } else {
            assertEquals(expected.getTags(), actual.getTags(), "EventGraph tags mismatch (null check)");
        }

        // Order-agnostic node comparison using name as unique key
        assertEquals(expected.getNodes().size(), actual.getNodes().size(), "Node count mismatch");
        Map<String, NodeDTO> actualNodesMap = actual.getNodes().stream()
                .collect(Collectors.toMap(n -> n.getName() + n.getType() + n.getBrokerType() + n.getMethodType(), Function.identity()));

        Map<UUID, NodeDTO> expectedNodesIdMap = expected.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));
        Map<UUID, NodeDTO> actualNodesIdMap = actual.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));

        for (NodeDTO expectedNode : expected.getNodes()) {
            NodeDTO actualNode = actualNodesMap.get(expectedNode.getName() + expectedNode.getType() + expectedNode.getBrokerType() + expectedNode.getMethodType());
            assertNotNull(actualNode, "Missing node with name: " + expectedNode.getName());

            // Сравнение основных полей NodeDTO
            assertEquals(expectedNode.getType(), actualNode.getType(), "Node type mismatch for name: " + expectedNode.getName());
            assertEquals(expectedNode.getBrokerType(), actualNode.getBrokerType(), "Node broker type mismatch for name: " + expectedNode.getName());

            // Более детальное сравнение с информативными сообщениями
            if (!Objects.equals(expectedNode.getNodeDescription(), actualNode.getNodeDescription())) {
                String message = String.format("Node description mismatch for name: %s\nExpected: '%s'\nActual: '%s'",
                        expectedNode.getName(), expectedNode.getNodeDescription(), actualNode.getNodeDescription());
                assertEquals(expectedNode.getNodeDescription(), actualNode.getNodeDescription(), message);
            }

            assertEquals(expectedNode.getNodeUrl(), actualNode.getNodeUrl(), "Node URL mismatch for name: " + expectedNode.getName());
            assertEquals(expectedNode.getMethodType(), actualNode.getMethodType(), "Node methodType mismatch for name: " + expectedNode.getName());

            // Сравнение belongsToGraph
            for (UUID btgExpected : expectedNode.getBelongsToGraph()) {
                NodeDTO btgExpectedNode = expectedNodesIdMap.get(btgExpected);
                boolean equalBtg = false;
                for (UUID btgActual : actualNode.getBelongsToGraph()) {
                    NodeDTO btgActualNode = actualNodesIdMap.get(btgActual);
                    if (btgExpectedNode != null && btgActualNode != null) {
                        if (btgExpectedNode.getName().equals(btgActualNode.getName()) &&
                                btgActualNode.getBrokerType() == btgExpectedNode.getBrokerType() &&
                                btgActualNode.getType() == btgExpectedNode.getType()) {
                            equalBtg = true;
                            break;
                        }
                    }
                }
                assertTrue(equalBtg, "BelongsToGraph mismatch for node name: " + expectedNode.getName());
            }

            // Сравнение tags
            if (expectedNode.getTags() != null && actualNode.getTags() != null) {
                Set<String> expectedTags = new HashSet<>(expectedNode.getTags());
                Set<String> actualTags = new HashSet<>(actualNode.getTags());
                assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for node " + expectedNode.getName());
                assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for node " + expectedNode.getName());
            } else {
                assertEquals(expectedNode.getTags(), actualNode.getTags(), "Node tags mismatch (null check) for node " + expectedNode.getName());
            }

            // Сравнение documentationFileLinks
            if (expectedNode.getDocumentationFileLinks() != null && actualNode.getDocumentationFileLinks() != null) {
                Set<String> expectedDocLinks = new HashSet<>(expectedNode.getDocumentationFileLinks());
                Set<String> actualDocLinks = new HashSet<>(actualNode.getDocumentationFileLinks());
                assertEquals(expectedDocLinks.size(), actualDocLinks.size(), "Node documentationFileLinks collection size mismatch for node " + expectedNode.getName());
                assertTrue(actualDocLinks.containsAll(expectedDocLinks), "Not all documentationFileLinks are in actualNode for node " + expectedNode.getName());
            } else {
                assertEquals(expectedNode.getDocumentationFileLinks(), actualNode.getDocumentationFileLinks(), "Node documentationFileLinks mismatch (null check) for node " + expectedNode.getName());
            }

            // Сравнение HTTP параметров
            compareHttpParameters(expectedNode.getHttpParameters(), actualNode.getHttpParameters(), "Node " + expectedNode.getName());

            // Сравнение HTTP request body
            compareHttpRequestBody(expectedNode.getHttpRequestBody(), actualNode.getHttpRequestBody(), "Node " + expectedNode.getName());

            // Сравнение HTTP responses
            compareHttpResponses(expectedNode.getHttpResponses(), actualNode.getHttpResponses(), "Node " + expectedNode.getName());
        }

        // Order-agnostic link comparison
        assertEquals(expected.getLinks().size(), actual.getLinks().size(), "Link count mismatch");
        Set<LinkDTO> actualLinksSet = new HashSet<>(actual.getLinks());
        for (LinkDTO expectedLink : expected.getLinks()) {
            // find link nodes
            NodeDTO fromNodeExpected = expectedNodesIdMap.get(expectedLink.getFromId());
            NodeDTO toNodeExpected = expectedNodesIdMap.get(expectedLink.getToId());
            // find event (может быть null)
            EventDTO eventExpected = expectedFacade.getEventById(expectedLink.getEventId());
            
            // compare nodes - проверяем только обязательные поля (fromNode и toNode)
            boolean equalLink = false;
            if (fromNodeExpected != null && toNodeExpected != null) {
                for (LinkDTO actualLink : actualLinksSet) {
                    // find link nodes
                    NodeDTO fromNodeActual = actualNodesIdMap.get(actualLink.getFromId());
                    NodeDTO toNodeActual = actualNodesIdMap.get(actualLink.getToId());
                    // find event (может быть null)
                    EventDTO eventActual = actualFacade.getEventById(actualLink.getEventId());
                    
                    // проверяем только обязательные поля (fromNode и toNode)
                    if (fromNodeActual != null && toNodeActual != null) {
                        // сравниваем узлы по имени, типу и brokerType
                        boolean nodesMatch = fromNodeExpected.getName().equals(fromNodeActual.getName()) &&
                                toNodeExpected.getName().equals(toNodeActual.getName()) &&
                                fromNodeExpected.getType().equals(fromNodeActual.getType()) &&
                                toNodeExpected.getType().equals(toNodeActual.getType()) &&
                                fromNodeExpected.getBrokerType() == fromNodeActual.getBrokerType() &&
                                toNodeExpected.getBrokerType() == toNodeActual.getBrokerType();
                        
                        // сравниваем события (оба могут быть null)
                        boolean eventsMatch = false;
                        if (eventExpected == null && eventActual == null) {
                            eventsMatch = true; // оба события null - это нормально
                        } else if (eventExpected != null && eventActual != null) {
                            eventsMatch = eventExpected.getName().equals(eventActual.getName());
                        }
                        // если одно событие null, а другое нет - eventsMatch остается false
                        
                        if (nodesMatch && eventsMatch) {
                            // Сравнение дополнительных полей LinkDTO
                            assertEquals(expectedLink.getGroup(), actualLink.getGroup(), "Link group mismatch");

                            // Сравнение tags
                            if (expectedLink.getTags() != null && actualLink.getTags() != null) {
                                Set<String> expectedTags = new HashSet<>(expectedLink.getTags());
                                Set<String> actualTags = new HashSet<>(actualLink.getTags());
                                assertEquals(expectedTags.size(), actualTags.size(), "Link tags collection size mismatch for link " + expectedLink);
                                assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actual link for link " + expectedLink);
                            } else {
                                assertEquals(expectedLink.getTags(), actualLink.getTags(), "Link tags mismatch (null check) for link " + expectedLink);
                            }

                            equalLink = true;
                            break;
                        }
                    }
                }
            }
            
            // улучшенное сообщение об ошибке с учетом null значений
            String fromNodeName = (fromNodeExpected == null) ? "null" : fromNodeExpected.getName();
            String toNodeName = (toNodeExpected == null) ? "null" : toNodeExpected.getName();
            String eventName = (eventExpected == null) ? "null" : eventExpected.getName();
            
            assertTrue(equalLink, "Link mismatch from " + fromNodeName + " to " + toNodeName + " with event " + eventName);
        }

        // Map-based event comparison
        assertEquals(expected.getEvents().size(), actual.getEvents().size(), "Event count mismatch");
        for (EventDTO expectedEvent : expected.getEvents()) {
            EventDTO actualEvent = actualFacade.getEvent(expectedEvent.getName());
            assertNotNull(actualEvent, "no event with name " + expectedEvent.getName());
            assertEquals(expectedEvent.getName(), actualEvent.getName(), "Event name mismatch for key: " + expectedEvent.getName());

            // Сравнение схем (убираем пробелы)
            String expectedEventSchema = expectedEvent.getSchema() == null ? null : expectedEvent.getSchema().replaceAll("\\s+", "");
            String actualEventSchema = actualEvent.getSchema() == null ? null : actualEvent.getSchema().replaceAll("\\s+", "");
            assertEquals(expectedEventSchema, actualEventSchema, "Event schema mismatch for key: " + expectedEvent.getName());

            // Сравнение дополнительных полей EventDTO
            assertEquals(expectedEvent.getEventType(), actualEvent.getEventType(), "Event type mismatch for event " + expectedEvent.getName());
            assertEquals(expectedEvent.getEventDescription(), actualEvent.getEventDescription(), "Event description mismatch for event " + expectedEvent.getName());

            // Сравнение tags
            if (expectedEvent.getTags() != null && actualEvent.getTags() != null) {
                Set<String> expectedTags = new HashSet<>(expectedEvent.getTags());
                Set<String> actualTags = new HashSet<>(actualEvent.getTags());
                assertEquals(expectedTags.size(), actualTags.size(), "Event tags collection size mismatch for event " + expectedEvent.getName());
                assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actual event for event " + expectedEvent.getName());
            } else {
                assertEquals(expectedEvent.getTags(), actualEvent.getTags(), "Event tags mismatch (null check) for event " + expectedEvent.getName());
            }

            // Сравнение usageContext
            if (expectedEvent.getUsageContext() != null && actualEvent.getUsageContext() != null) {
                Set<EventUsageContextEnum> expectedUsageContext = new HashSet<>(expectedEvent.getUsageContext());
                Set<EventUsageContextEnum> actualUsageContext = new HashSet<>(actualEvent.getUsageContext());
                assertEquals(expectedUsageContext.size(), actualUsageContext.size(), "Event usageContext collection size mismatch for event " + expectedEvent.getName());
                assertTrue(actualUsageContext.containsAll(expectedUsageContext), "Not all usageContext are in actual event for event " + expectedEvent.getName());
            } else {
                assertEquals(expectedEvent.getUsageContext(), actualEvent.getUsageContext(), "Event usageContext mismatch (null check) for event " + expectedEvent.getName());
            }
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

    /**
     * Сравнение HTTP параметров
     */
    private static void compareHttpParameters(HttpParametersDTO expected, HttpParametersDTO actual, String context) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, context + " - HttpParameters mismatch (null check)");
            return;
        }

        // Сравнение path parameters
        compareHttpParameterLists(expected.getPathParameters(), actual.getPathParameters(), context + " - pathParameters");

        // Сравнение query parameters
        compareHttpParameterLists(expected.getQueryParameters(), actual.getQueryParameters(), context + " - queryParameters");

        // Сравнение header parameters
        compareHttpParameterLists(expected.getHeaderParameters(), actual.getHeaderParameters(), context + " - headerParameters");
    }

    /**
     * Сравнение списков HTTP параметров
     */
    private static void compareHttpParameterLists(List<HttpParameterDTO> expected, List<HttpParameterDTO> actual, String context) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, context + " - parameter list mismatch (null check)");
            return;
        }

        assertEquals(expected.size(), actual.size(), context + " - parameter count mismatch");

        // Создаем мапы для сравнения по имени параметра
        Map<String, HttpParameterDTO> expectedMap = expected.stream()
                .collect(Collectors.toMap(HttpParameterDTO::getName, Function.identity()));
        Map<String, HttpParameterDTO> actualMap = actual.stream()
                .collect(Collectors.toMap(HttpParameterDTO::getName, Function.identity()));

        for (HttpParameterDTO expectedParam : expected) {
            HttpParameterDTO actualParam = actualMap.get(expectedParam.getName());
            assertNotNull(actualParam, context + " - missing parameter: " + expectedParam.getName());

            assertEquals(expectedParam.getName(), actualParam.getName(), context + " - parameter name mismatch");
            assertEquals(expectedParam.getDescription(), actualParam.getDescription(), context + " - parameter description mismatch for " + expectedParam.getName());
            assertEquals(expectedParam.getRequired(), actualParam.getRequired(), context + " - parameter required mismatch for " + expectedParam.getName());
            assertEquals(expectedParam.getType(), actualParam.getType(), context + " - parameter type mismatch for " + expectedParam.getName());
            //remove white spaces

            String exWithoutWhiteSpaces = null;
            String acWithoutWhiteSpaces = null;
            if (expectedParam.getSchema() != null && actualParam.getSchema() != null) {
                exWithoutWhiteSpaces = expectedParam.getSchema() == null ? null : expectedParam.getSchema().replaceAll("\\s+", "");
                acWithoutWhiteSpaces = actualParam.getSchema() == null ? null : actualParam.getSchema().replaceAll("\\s+", "");
                assertEquals(exWithoutWhiteSpaces, acWithoutWhiteSpaces, context + " - parameter schema mismatch for " + expectedParam.getName());
            }

            if (expectedParam.getExample() != null && actualParam.getExample() != null) {
                String expectedExampleNoWS = expectedParam.getExample() == null ? null : expectedParam.getExample().replaceAll("\\s+", "");
                String actualExampleNoWS = actualParam.getExample() == null ? null : actualParam.getExample().replaceAll("\\s+", "");
                assertEquals(expectedExampleNoWS, actualExampleNoWS, context + " - parameter example mismatch for " + expectedParam.getName());
            }
        }
    }

    /**
     * Сравнение HTTP request body
     */
    private static void compareHttpRequestBody(HttpRequestBodyDTO expected, HttpRequestBodyDTO actual, String context) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, context + " - HttpRequestBody mismatch (null check)");
            return;
        }
        // remove whitespaces (null-safe)
        String expectedDesc = expected.getDescription() == null ? null : expected.getDescription().replaceAll("\\s+", "");
        String actualDesc = actual.getDescription() == null ? null : actual.getDescription().replaceAll("\\s+", "");
        assertEquals(expectedDesc, actualDesc, context + " - request body description mismatch");
        assertEquals(expected.getRequired(), actual.getRequired(), context + " - request body required mismatch");

        // Сравнение content
        compareHttpContentLists(expected.getContent(), actual.getContent(), context + " - request body content");
    }

    /**
     * Сравнение HTTP responses
     */
    private static void compareHttpResponses(List<HttpResponseDTO> expected, List<HttpResponseDTO> actual, String context) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, context + " - HttpResponses mismatch (null check)");
            return;
        }

        assertEquals(expected.size(), actual.size(), context + " - response count mismatch");

        // Создаем мапы для сравнения по статус коду
        Map<HttpStatusCodeEnum, HttpResponseDTO> expectedMap = expected.stream()
                .collect(Collectors.toMap(HttpResponseDTO::getStatusCode, Function.identity()));
        Map<HttpStatusCodeEnum, HttpResponseDTO> actualMap = actual.stream()
                .collect(Collectors.toMap(HttpResponseDTO::getStatusCode, Function.identity()));

        for (HttpResponseDTO expectedResponse : expected) {
            HttpResponseDTO actualResponse = actualMap.get(expectedResponse.getStatusCode());
            assertNotNull(actualResponse, context + " - missing response for status code: " + expectedResponse.getStatusCode());

            assertEquals(expectedResponse.getStatusCode(), actualResponse.getStatusCode(), context + " - response status code mismatch");
            assertEquals(expectedResponse.getDescription(), actualResponse.getDescription(), context + " - response description mismatch for " + expectedResponse.getStatusCode());

            // Сравнение headers
            compareHttpParameterLists(expectedResponse.getHeaders(), actualResponse.getHeaders(), context + " - response headers for " + expectedResponse.getStatusCode());

            // Сравнение content
            compareHttpContentLists(expectedResponse.getContent(), actualResponse.getContent(), context + " - response content for " + expectedResponse.getStatusCode());
        }
    }

    /**
     * Сравнение списков HTTP content
     */
    private static void compareHttpContentLists(List<HttpContentDTO> expected, List<HttpContentDTO> actual, String context) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, context + " - content list mismatch (null check)");
            return;
        }

        assertEquals(expected.size(), actual.size(), context + " - content count mismatch");

        // Создаем мапы для сравнения по media type
        Map<HttpContentTypeEnum, HttpContentDTO> expectedMap = expected.stream()
                .collect(Collectors.toMap(HttpContentDTO::getMediaType, Function.identity()));
        Map<HttpContentTypeEnum, HttpContentDTO> actualMap = actual.stream()
                .collect(Collectors.toMap(HttpContentDTO::getMediaType, Function.identity()));

        for (HttpContentDTO expectedContent : expected) {
            HttpContentDTO actualContent = actualMap.get(expectedContent.getMediaType());
            assertNotNull(actualContent, context + " - missing content for media type: " + expectedContent.getMediaType());

            assertEquals(expectedContent.getMediaType(), actualContent.getMediaType(), context + " - content media type mismatch");
            if(expectedContent.getSchema() != null && actualContent.getSchema() != null) {
                String expectedContentSchemaNoWS = expectedContent.getSchema() == null ? null : expectedContent.getSchema().replaceAll("\\s+", "");
                String actualContentSchemaNoWS = actualContent.getSchema() == null ? null : actualContent.getSchema().replaceAll("\\s+", "");
                assertEquals(expectedContentSchemaNoWS, actualContentSchemaNoWS, context + " - content schema mismatch for " + expectedContent.getMediaType());
            } else {
                assertEquals(expectedContent.getSchema(), actualContent.getSchema(), context + " - content schema mismatch for " + expectedContent.getMediaType());
            }
            if(expectedContent.getExample() != null && actualContent.getExample() != null) {
                String expectedContentExampleNoWS = expectedContent.getExample() == null ? null : expectedContent.getExample().replaceAll("\\s+", "");
                String actualContentExampleNoWS = actualContent.getExample() == null ? null : actualContent.getExample().replaceAll("\\s+", "");
                assertEquals(expectedContentExampleNoWS, actualContentExampleNoWS, context + " - content example mismatch for " + expectedContent.getMediaType());
            } else {
                assertEquals(expectedContent.getExample(), actualContent.getExample(), context + " - content example mismatch for " + expectedContent.getMediaType());
            }

            // Сравнение examples
            compareHttpExampleLists(expectedContent.getExamples(), actualContent.getExamples(), context + " - content examples for " + expectedContent.getMediaType());
        }
    }

    /**
     * Сравнение списков HTTP examples
     */
    private static void compareHttpExampleLists(List<HttpExampleDTO> expected, List<HttpExampleDTO> actual, String context) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, context + " - examples list mismatch (null check)");
            return;
        }

        assertEquals(expected.size(), actual.size(), context + " - examples count mismatch");

        // Создаем мапы для сравнения по имени примера
        Map<String, HttpExampleDTO> expectedMap = expected.stream()
                .collect(Collectors.toMap(HttpExampleDTO::getName, Function.identity()));
        Map<String, HttpExampleDTO> actualMap = actual.stream()
                .collect(Collectors.toMap(HttpExampleDTO::getName, Function.identity()));

        for (HttpExampleDTO expectedExample : expected) {
            HttpExampleDTO actualExample = actualMap.get(expectedExample.getName());
            assertNotNull(actualExample, context + " - missing example: " + expectedExample.getName());

            assertEquals(expectedExample.getName(), actualExample.getName(), context + " - example name mismatch");
            assertEquals(expectedExample.getSummary(), actualExample.getSummary(), context + " - example summary mismatch for " + expectedExample.getName());
            assertEquals(expectedExample.getDescription(), actualExample.getDescription(), context + " - example description mismatch for " + expectedExample.getName());
            assertEquals(expectedExample.getValue(), actualExample.getValue(), context + " - example value mismatch for " + expectedExample.getName());
        }
    }
}



