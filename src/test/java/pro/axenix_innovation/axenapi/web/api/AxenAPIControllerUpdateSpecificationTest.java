package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerUpdateSpecificationTest {

    private static final Logger logger = LoggerFactory.getLogger(AxenAPIControllerUpdateSpecificationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private void testUpdateServiceSpecification(String eventGraphFileName, String serviceSpecFileName, String expectedResultFileName, String serviceName) throws Exception {
        String eventGraphPath = "results/" + eventGraphFileName + ".json";
        EventGraphDTO eventGraph;
        try (InputStream eventGraphStream = getClass().getClassLoader().getResourceAsStream(eventGraphPath)) {
            assertNotNull(eventGraphStream, "Файл не найден: " + eventGraphPath);
            eventGraph = objectMapper.readValue(eventGraphStream, EventGraphDTO.class);
        }

        NodeDTO serviceNode = eventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE && (serviceName == null || node.getName().equals(serviceName)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сервис не найден в графе"));

        String specPath = "specs/json/" + serviceSpecFileName + ".json";
        String specification;
        try (InputStream specStream = getClass().getClassLoader().getResourceAsStream(specPath)) {
            assertNotNull(specStream, "Файл спецификации не найден: " + specPath);
            specification = new String(specStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest();
        request.setServiceNodeId(serviceNode.getId());
        request.setSpecification(specification);
        request.setEventGraph(eventGraph);

        String requestJson = objectMapper.writeValueAsString(request);
        logger.info("Отправка запроса на обновление спецификации сервиса: {}", serviceNode.getName());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/updateServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        logger.info("Ответ: {}", responseContent);

        EventGraphDTO updatedEventGraph = objectMapper.readValue(responseContent, EventGraphDTO.class);

        String expectedFilePath = "results/" + expectedResultFileName + ".json";
        try (InputStream expectedStream = getClass().getClassLoader().getResourceAsStream(expectedFilePath)) {
            assertNotNull(expectedStream, "Ожидаемый файл не найден: " + expectedFilePath);
            EventGraphDTO expected = objectMapper.readValue(expectedStream, EventGraphDTO.class);
            deepCompare(expected, updatedEventGraph);
        }
    }

    private void testUpdateServiceSpecification(String eventGraphFileName, String serviceSpecFileName, String expectedResultFileName) throws Exception {
        testUpdateServiceSpecification(eventGraphFileName, serviceSpecFileName, expectedResultFileName, null);
    }

    @Test
    public void testUpdateServiceSpecification_BasicUpdate() throws Exception {
        testUpdateServiceSpecification("consume_one_event_service", 
                "consume_one_event_service", 
                "consume_one_event_service");
    }

    @Test
    public void testUpdateServiceSpecification_EmptyService() throws Exception {
        testUpdateServiceSpecification("empty_service", 
                "empty_service", 
                "empty_service");
    }

    @Test
    public void testUpdateServiceSpecification_ComplexService() throws Exception {
        testUpdateServiceSpecification("consume_three_events_from_different_brokers_service", 
                "consume_three_events_from_different_brokers_service_updated", 
                "consume_three_events_from_different_brokers_service_updated");
    }

    @Test
    public void testUpdateServiceSpecification_WithDifferentSpec() throws Exception {
        String eventGraphPath = "results/consume_one_event_service.json";
        EventGraphDTO eventGraph;
        try (InputStream eventGraphStream = getClass().getClassLoader().getResourceAsStream(eventGraphPath)) {
            assertNotNull(eventGraphStream, "Файл не найден: " + eventGraphPath);
            eventGraph = objectMapper.readValue(eventGraphStream, EventGraphDTO.class);
        }

        NodeDTO serviceNode = eventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сервис не найден в графе"));

        String specPath = "specs/json/empty_service.json";
        String specification;
        try (InputStream specStream = getClass().getClassLoader().getResourceAsStream(specPath)) {
            assertNotNull(specStream, "Файл спецификации не найден: " + specPath);
            specification = new String(specStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest();
        request.setServiceNodeId(serviceNode.getId());
        request.setSpecification(specification);
        request.setEventGraph(eventGraph);

        String requestJson = objectMapper.writeValueAsString(request);
        logger.info("Отправка запроса на обновление спецификации сервиса другой спецификацией");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/updateServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        logger.info("Ответ: {}", responseContent);

        EventGraphDTO updatedEventGraph = objectMapper.readValue(responseContent, EventGraphDTO.class);

        assertNotNull(updatedEventGraph);
        assertTrue(updatedEventGraph.getErrors().isEmpty(), "Не должно быть ошибок");
        
        boolean serviceExists = updatedEventGraph.getNodes().stream()
                .anyMatch(node -> node.getId().equals(serviceNode.getId()) && node.getType() == NodeDTO.TypeEnum.SERVICE);
        assertTrue(serviceExists, "Сервис должен существовать после обновления");
    }

    @Test
    public void testUpdateServiceSpecification_InvalidServiceId() throws Exception {
        String eventGraphPath = "results/consume_one_event_service.json";
        EventGraphDTO eventGraph;
        try (InputStream eventGraphStream = getClass().getClassLoader().getResourceAsStream(eventGraphPath)) {
            assertNotNull(eventGraphStream, "Файл не найден: " + eventGraphPath);
            eventGraph = objectMapper.readValue(eventGraphStream, EventGraphDTO.class);
        }

        UUID invalidServiceId = UUID.randomUUID();

        String specPath = "specs/json/consume_one_event_service.json";
        String specification;
        try (InputStream specStream = getClass().getClassLoader().getResourceAsStream(specPath)) {
            assertNotNull(specStream, "Файл спецификации не найден: " + specPath);
            specification = new String(specStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest();
        request.setServiceNodeId(invalidServiceId);
        request.setSpecification(specification);
        request.setEventGraph(eventGraph);

        String requestJson = objectMapper.writeValueAsString(request);
        logger.info("Отправка запроса с несуществующим ID сервиса");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/updateServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        logger.info("Ответ с ошибкой: {}", responseContent);
    }

    @Test
    public void testUpdateServiceSpecification_CartServiceNoChange() throws Exception {
        testUpdateServiceSpecification("cart_notif_services", 
                "cart-service", 
                "cart_notif_services",
                "Cart-Service");
    }

    @Test
    public void testUpdateServiceSpecification_KafkaToUndefinedBroker() throws Exception {
        String eventGraphPath = "results/consume_one_event_service.json";
        EventGraphDTO eventGraph;
        try (InputStream eventGraphStream = getClass().getClassLoader().getResourceAsStream(eventGraphPath)) {
            assertNotNull(eventGraphStream, "Файл не найден: " + eventGraphPath);
            eventGraph = objectMapper.readValue(eventGraphStream, EventGraphDTO.class);
        }

        NodeDTO serviceNode = eventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сервис не найден в графе"));

        String newSpecification = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "consume_one_event_service",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/undefined_broker/topic1/Event1": {
                      "post": {
                        "responses": {
                          "200": {
                            "description": "OK"
                          }
                        }
                      }
                    }
                  },
                  "components": {
                    "schemas": {
                      "Event1": {
                        "type": "object",
                        "x-incoming": {
                          "topics": ["topic1"]
                        }
                      }
                    }
                  }
                }
                """;

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest();
        request.setServiceNodeId(serviceNode.getId());
        request.setSpecification(newSpecification);
        request.setEventGraph(eventGraph);

        String requestJson = objectMapper.writeValueAsString(request);
        logger.info("Отправка запроса на обновление спецификации сервиса с Kafka на undefined_broker");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/updateServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        logger.info("Ответ: {}", responseContent);

        EventGraphDTO updatedEventGraph = objectMapper.readValue(responseContent, EventGraphDTO.class);

        assertNotNull(updatedEventGraph);
        assertTrue(updatedEventGraph.getErrors().isEmpty(), "Не должно быть ошибок");
        
        NodeDTO updatedServiceNode = updatedEventGraph.getNodes().stream()
                .filter(node -> node.getId().equals(serviceNode.getId()) && node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Сервис должен существовать после обновления"));
        
        assertEquals(serviceNode.getName(), updatedServiceNode.getName(), "Имя сервиса должно остаться прежним");

        NodeDTO topicNode = updatedEventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC && node.getName().equals("topic1"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Топик topic1 должен существовать"));
        
        assertEquals(NodeDTO.BrokerTypeEnum.UNDEFINED, topicNode.getBrokerType(), 
                "Топик должен иметь тип брокера UNDEFINED");

        boolean linkExists = updatedEventGraph.getLinks().stream()
                .anyMatch(link -> link.getFromId().equals(topicNode.getId()) && 
                                link.getToId().equals(updatedServiceNode.getId()));
        assertTrue(linkExists, "Должна существовать связь от топика к сервису");

        boolean eventExists = updatedEventGraph.getEvents().stream()
                .anyMatch(event -> event.getName().equals("Event1"));
        assertTrue(eventExists, "Событие Event1 должно существовать");

        logger.info("Тест успешно завершен: спецификация обновлена с Kafka на undefined_broker");
    }

    public void deepCompare(EventGraphDTO expected, EventGraphDTO actual) {
        EventGraphFacade expectedFacade = new EventGraphFacade(expected);
        EventGraphFacade actualFacade = new EventGraphFacade(actual);

        assertEquals(expected.getTags().size(), actual.getTags().size());
        assertTrue(expected.getTags().containsAll(actual.getTags()));
        assertTrue(expected.getTags().containsAll(actual.getTags()));
        assertEquals(expected.getName(), actual.getName(), "Graph name mismatch");

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
                    assertNotNull(actualLink.getId(), "Link ID is null " + actualLink);
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
                            assertEquals(expectedLink.getGroup(), actualLink.getGroup(), "Group not equals for link " + expectedLink);
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

    private String errorToString(ErrorDTO error) {
        return String.format("Error{filename=%s, error=%s}", error.getFileName(), error.getErrorMessage());
    }
}