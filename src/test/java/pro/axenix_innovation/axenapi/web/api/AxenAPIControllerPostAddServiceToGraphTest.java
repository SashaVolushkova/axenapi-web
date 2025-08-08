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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pro.axenix_innovation.axenapi.web.TestHelper;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostAddServiceToGraphTest {

    private static final Logger logger = LoggerFactory.getLogger(AxenAPIControllerPostAddServiceToGraphTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;



    private void testAddServiceToGraph(String eventGraphFileName, List<String> fileNames) throws Exception {
        testAddServiceToGraph(eventGraphFileName, fileNames, false);
    }
    private void testAddServiceToGraph(String eventGraphFileName, List<String> fileNames, boolean updated) throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart("/addServiceToGraph");

        for (String fileName : fileNames) {
            String filePath = "specs/json/" + fileName + ".json";
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
                assertNotNull(inputStream, "Файл не найден: " + filePath);
                MockMultipartFile file = new MockMultipartFile("files", fileName + ".json",
                        MediaType.APPLICATION_JSON_VALUE, inputStream);
                requestBuilder.file(file);
            }
        }

        String eventGraphPath = "results/" + eventGraphFileName + ".json";
        try (InputStream eventGraphStream = getClass().getClassLoader().getResourceAsStream(eventGraphPath)) {
            assertNotNull(eventGraphStream, "Файл не найден: " + eventGraphPath);
            MockMultipartFile eventGraphFile = new MockMultipartFile("eventGraph", eventGraphFileName + ".json",
                    MediaType.APPLICATION_JSON_VALUE, eventGraphStream);
            requestBuilder.file(eventGraphFile);
        }

        logger.info("Отправка файлов: {} и {}", fileNames, eventGraphFileName);

        MvcResult result = mockMvc.perform(requestBuilder.contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        logger.info("Ответ: {}", responseContent);

        EventGraphDTO eventGraphDTO = objectMapper.readValue(responseContent, EventGraphDTO.class);

        //filter unique, remove _updated from names, if updated true
        List<String> uniqueFileNames = fileNames.stream()
                .map(s -> updated ? s.replace("_updated", "") : s)
                .distinct()
                .collect(Collectors.toList());
        String combinedFileName = eventGraphFileName + "&" + String.join("&", uniqueFileNames);  // изменено
        String expectedFilePath = "addresult/" + combinedFileName + (updated ? "_updated" : "") + ".json";
        try (InputStream expectedStream = getClass().getClassLoader().getResourceAsStream(expectedFilePath)) {
            assertNotNull(expectedStream, "Ожидаемый файл не найден: " + expectedFilePath);
            EventGraphDTO expected = objectMapper.readValue(expectedStream, EventGraphDTO.class);
            TestHelper.deepCompare(expected, eventGraphDTO);
        }

    }

    @Test
    public void addTwoEqualsSpecsFile() throws Exception {
        testAddServiceToGraph("consume_one_event_service",
                List.of("consume_one_event_service", "consume_one_event_service"));
    }

    @Test
    public void testSingleFile() throws Exception {
        testAddServiceToGraph("consume_one_event_service",
                List.of("consume_one_event_service"));
    }

    @Test
    public void testThreeEventsTopic1() throws Exception {
        testAddServiceToGraph("consume_three_events_from_different_brokers_service",
                List.of("service_no_common_consume_topics_common_events_common_outgoing_topics_1"));
    }

    @Test
    public void testThreeEventsTopic2() throws Exception {
        testAddServiceToGraph("consume_one_event_service",
                List.of("service_no_common_consume_topics_common_events_common_outgoing_topics_2"));
    }

    @Test
    public void testEmptyService() throws Exception {
        testAddServiceToGraph("empty_service",
                List.of("empty_service"));
    }

    @Test
    public void testSameName() throws Exception {
        testAddServiceToGraph("no_consumes_outgoing_with_broker_type_service_same_name",
                List.of("no_consumes_outgoing_with_broker_type_service_same_name"));
    }

    @Test
    public void testSomeSpecs() throws Exception {
        testAddServiceToGraph("consume_one_event_service",
                List.of("consume_three_events_from_different_brokers_service",
                        "empty_service", "no_consumes_one_outgoing_no_broker_type_service"));
    }

    @Test
    public void testSomeSpecsDouble() throws Exception {
        testAddServiceToGraph("consume_one_event_service",
                List.of("consume_three_events_from_different_brokers_service", "consume_three_events_from_different_brokers_service",
                        "empty_service", "no_consumes_one_outgoing_no_broker_type_service"));
    }

    @Test
    public void testSomeSpecsUpdateService() throws Exception {
        testAddServiceToGraph("consume_one_event_service",
                List.of(
                        "consume_three_events_from_different_brokers_service",
                        "consume_three_events_from_different_brokers_service_updated",
                        "empty_service", "no_consumes_one_outgoing_no_broker_type_service"), true);
    }

    @Test
    public void testEmptyServiceNoGroupKafka() throws Exception {
        testAddServiceToGraph("empty_at_all",
                List.of("consume_two_events_in_one_topic_no_group"));
    }

    @Test
    void testAddServiceToGraph_updatesExistingService() throws Exception {
        Path initialFilePath = Paths.get("src/test/resources/specs/json/double/consume_one_event_service.json");
        byte[] initialFileContent = Files.readAllBytes(initialFilePath);

        MockMultipartFile initialSpec = new MockMultipartFile(
                "files",
                "consume_one_event_service.json",
                "application/json",
                initialFileContent
        );

        EventGraphDTO emptyGraph = new EventGraphDTO();
        emptyGraph.setName("TestGraph");
        emptyGraph.setNodes(new ArrayList<>());
        emptyGraph.setLinks(new ArrayList<>());
        emptyGraph.setEvents(new ArrayList<>());
        emptyGraph.setTags(new HashSet<>());

        String emptyGraphJson = objectMapper.writeValueAsString(emptyGraph);

        MockMultipartFile emptyGraphPart = new MockMultipartFile(
                "eventGraph",
                "",
                "application/json",
                emptyGraphJson.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult initialResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/addServiceToGraph")
                        .file(initialSpec)
                        .file(emptyGraphPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        EventGraphDTO responseAfterInitial = objectMapper.readValue(
                initialResult.getResponse().getContentAsString(), EventGraphDTO.class);

        Path updatedFilePath = Paths.get("src/test/resources/specs/json/consume_one_event_service.json");
        byte[] updatedFileContent = Files.readAllBytes(updatedFilePath);

        MockMultipartFile updatedSpec = new MockMultipartFile(
                "files",
                "consume_one_event_service.json",
                "application/json",
                updatedFileContent
        );

        String responseGraphJson = objectMapper.writeValueAsString(responseAfterInitial);

        MockMultipartFile responseGraphPart = new MockMultipartFile(
                "eventGraph",
                "",
                "application/json",
                responseGraphJson.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult updatedResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/addServiceToGraph")
                        .file(updatedSpec)
                        .file(responseGraphPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        EventGraphDTO responseGraph = objectMapper.readValue(
                updatedResult.getResponse().getContentAsString(), EventGraphDTO.class);

        long serviceCount = responseGraph.getNodes().stream()
                .filter(n -> "consume_one_event_service".equals(n.getName()) && n.getType() == NodeDTO.TypeEnum.SERVICE)
                .count();
        Assertions.assertEquals(1, serviceCount, "Сервис должен быть только один и обновлённый");

        boolean topicExists = responseGraph.getNodes().stream()
                .anyMatch(n -> "topic1".equals(n.getName()) && n.getType() == NodeDTO.TypeEnum.TOPIC);
        Assertions.assertTrue(topicExists, "Топик topic1 должен быть добавлен");

        Optional<UUID> event1IdOpt = responseGraph.getEvents().stream()
                .filter(event -> "Event1".equals(event.getName()))
                .map(EventDTO::getId)
                .findFirst();

        Assertions.assertTrue(event1IdOpt.isPresent(), "Событие Event1 должно присутствовать в списке событий");

        UUID event1Id = event1IdOpt.get();

        boolean eventPresentInLinks = responseGraph.getLinks().stream()
                .peek(link -> {
                    String linkEventId = String.valueOf(link.getEventId());
                    logger.info("Link eventId='{}' (length: {}), Event1 UUID='{}' (length: {})",
                            linkEventId,
                            linkEventId != null ? linkEventId.length() : "null",
                            event1Id.toString(),
                            event1Id.toString().length());
                })
                .anyMatch(link -> {
                    String linkEventId = String.valueOf(link.getEventId());
                    return linkEventId != null && linkEventId.trim().equals(event1Id.toString());
                });

        Assertions.assertTrue(eventPresentInLinks, "Событие Event1 должно быть в ссылках");

    }

    @Test
    void testAddServiceToGraph_httpMethodWithEmptyRequestAndKafkaUndefinedEvent() throws Exception {
        // Create OpenAPI specification with HTTP method with empty request and kafka undefined_event
        String openApiSpec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "TestService",
            "description" : "AxenAPI Specification for TestService",
            "version" : "1.0.0"
          },
          "paths" : {
            "/api/data" : {
              "get" : {
                "responses" : {
                  "200" : {
                    "description" : "Success"
                  }
                }
              }
            },
            "/kafka/default/TestTopic/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        MockMultipartFile specFile = new MockMultipartFile(
                "files",
                "test_service.json",
                "application/json",
                openApiSpec.getBytes(StandardCharsets.UTF_8)
        );

        // Create empty event graph
        EventGraphDTO emptyGraph = new EventGraphDTO();
        emptyGraph.setName("TestGraph");
        emptyGraph.setNodes(new ArrayList<>());
        emptyGraph.setLinks(new ArrayList<>());
        emptyGraph.setEvents(new ArrayList<>());
        emptyGraph.setTags(new HashSet<>());
        emptyGraph.setErrors(new ArrayList<>());

        String emptyGraphJson = objectMapper.writeValueAsString(emptyGraph);

        MockMultipartFile emptyGraphPart = new MockMultipartFile(
                "eventGraph",
                "",
                "application/json",
                emptyGraphJson.getBytes(StandardCharsets.UTF_8)
        );

        // Perform the request
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/addServiceToGraph")
                        .file(specFile)
                        .file(emptyGraphPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        logger.info("Response: {}", responseContent);

        EventGraphDTO responseGraph = objectMapper.readValue(responseContent, EventGraphDTO.class);

        // Verify service node was created
        Optional<NodeDTO> serviceNode = responseGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst();
        assertTrue(serviceNode.isPresent(), "Сервисный узел должен существовать");
        assertEquals("TestService", serviceNode.get().getName(), "Имя сервиса должно быть TestService");

        // Verify HTTP node was created for GET method with empty request
        Optional<NodeDTO> httpNode = responseGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.HTTP)
                .findFirst();
        assertTrue(httpNode.isPresent(), "HTTP узел должен существовать");
        assertEquals("/api/data", httpNode.get().getNodeUrl(), "URL HTTP узла должен быть /api/data");
        assertEquals(NodeDTO.MethodTypeEnum.GET, httpNode.get().getMethodType(), "HTTP метод должен быть GET");

        // Verify Kafka topic node was created
        Optional<NodeDTO> kafkaTopicNode = responseGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst();
        assertTrue(kafkaTopicNode.isPresent(), "Kafka топик узел должен существовать");
        assertEquals("TestTopic", kafkaTopicNode.get().getName(), "Имя Kafka топика должно быть TestTopic");
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, kafkaTopicNode.get().getBrokerType(), "Тип брокера должен быть KAFKA");

        // Verify links with null eventId were created
        List<LinkDTO> linksWithNullEvent = responseGraph.getLinks().stream()
                .filter(link -> link.getEventId() == null)
                .collect(Collectors.toList());
        assertEquals(2, linksWithNullEvent.size(), "Должно быть 2 связи с null eventId");

        // Verify HTTP link
        Optional<LinkDTO> httpLink = responseGraph.getLinks().stream()
                .filter(link -> link.getFromId().equals(httpNode.get().getId()) && 
                               link.getToId().equals(serviceNode.get().getId()))
                .findFirst();
        assertTrue(httpLink.isPresent(), "Связь от HTTP узла к сервису должна существовать");
        assertNull(httpLink.get().getEventId(), "EventId должен быть null для HTTP запроса без события");

        // Verify Kafka link
        Optional<LinkDTO> kafkaLink = responseGraph.getLinks().stream()
                .filter(link -> link.getFromId().equals(kafkaTopicNode.get().getId()) && 
                               link.getToId().equals(serviceNode.get().getId()))
                .findFirst();
        assertTrue(kafkaLink.isPresent(), "Связь от Kafka топика к сервису должна существовать");
        assertNull(kafkaLink.get().getEventId(), "EventId должен быть null для undefined_event");

        // Verify no events were created
        assertTrue(responseGraph.getEvents().isEmpty(), "События не должны создаваться для undefined_event и пустых HTTP запросов");

        // Verify total node count
        assertEquals(3, responseGraph.getNodes().size(), "Должно быть 3 узла: 1 сервис, 1 HTTP, 1 Kafka топик");
    }
}
