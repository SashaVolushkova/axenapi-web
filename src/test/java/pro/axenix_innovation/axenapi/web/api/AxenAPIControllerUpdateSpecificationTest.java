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
import pro.axenix_innovation.axenapi.web.TestHelper;
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
            TestHelper.deepCompare(expected, updatedEventGraph);
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
}
