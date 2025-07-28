package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.*;


import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostUploadTest {

    private static final Logger log = LoggerFactory.getLogger(AxenAPIControllerPostUploadTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private void testFileUpload(String caseName) throws Exception {
        testFileUpload(caseName, 1);
    }

    private void testFileUpload(String caseName, int n) throws Exception {
        MockHttpServletRequestBuilder fileBuilder  = null;
        if (n == 1) {
            Resource fileResource = new ClassPathResource("specs/json/" + caseName + ".json");
            assertNotNull(fileResource);
            MockMultipartFile firstFile = new MockMultipartFile(
                    "files", fileResource.getFilename(),
                    MediaType.MULTIPART_FORM_DATA_VALUE,
                    fileResource.getInputStream());
            fileBuilder = multipart("/upload")
                    .file(firstFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA);
        } else if (n > 1) {
            MockMultipartHttpServletRequestBuilder multipart = multipart("/upload");
            for (int i = 1; i <= n; i++) {
                Resource fileResource = new ClassPathResource("specs/json/" + caseName + "_" + i + ".json");
                assertNotNull(fileResource);
                MockMultipartFile firstFile = new MockMultipartFile(
                        "files", fileResource.getFilename(),
                        MediaType.MULTIPART_FORM_DATA_VALUE,
                        fileResource.getInputStream());
                multipart.file(firstFile);
            }
            fileBuilder = multipart.contentType(MediaType.MULTIPART_FORM_DATA);
        }

        MvcResult result = mockMvc.perform(fileBuilder)
                .andExpect(status().isOk())
                .andReturn();

        // Deserialize the response into EventGraphDTO
        String responseContent = result.getResponse().getContentAsString();
        EventGraphDTO eventGraphDTO = objectMapper.readValue(responseContent, EventGraphDTO.class);
        //read expected result from file resource
        Resource expectedFile = new ClassPathResource("results/" + caseName + ".json");
        EventGraphDTO expected = objectMapper.readValue(expectedFile.getInputStream(), EventGraphDTO.class);
        deepCompare(expected, eventGraphDTO);
    }

    private void testTwoIdenticalFilesUpload(String caseName) throws Exception {
        Resource fileResource1 = new ClassPathResource("specs/json/" + caseName + ".json");
        assertNotNull(fileResource1);
        MockMultipartFile firstFile = new MockMultipartFile(
                "files", fileResource1.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource1.getInputStream());

        Resource fileResource2 = new ClassPathResource("specs/json/" + caseName + ".json");
        assertNotNull(fileResource2);
        MockMultipartFile secondFile = new MockMultipartFile(
                "files", fileResource2.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource2.getInputStream());

        MvcResult result = mockMvc.perform(multipart("/upload")
                        .file(firstFile)
                        .file(secondFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        EventGraphDTO eventGraphDTO = objectMapper.readValue(responseContent, EventGraphDTO.class);

        Resource expectedFile = new ClassPathResource("results/" + caseName + ".json");
        EventGraphDTO expected = objectMapper.readValue(expectedFile.getInputStream(), EventGraphDTO.class);
        deepCompare(expected, eventGraphDTO);
    }


    @Test
    public void testUploadPostEmptyGraph() throws Exception {
        testFileUpload("empty_service");
    }

    @Test
    public void testUploadPostSpecWithOnePath() throws Exception {
        testFileUpload("consume_one_event_service");
    }

    @Test
    public void testUploadPostSpecWithOnePath1() throws Exception {
        testTwoIdenticalFilesUpload("consume_one_event_service1");
    }

    @Test
    public void testUploadPostSpecWithThreeDifferentBrokersPath() throws Exception {
        testFileUpload("consume_three_events_from_different_brokers_service");
    }

    @Test
    public void testUploadPostSpecWithThreeDifferentBrokersPathNoGroup() throws Exception {
        testFileUpload("consume_three_events_from_different_brokers_service_no_group");
    }

    @Test
    public void testTwoEventsOneTopic() throws Exception {
        testFileUpload("consume_two_events_in_one_topic");
    }

    @Test
    public void testUploadPostSpecOneOutgoingNoBrokerType() throws Exception {
        testFileUpload("no_consumes_one_outgoing_no_broker_type_service");
    }

    @Test
    public void testUploadUpdatedSpec() throws Exception {
        testFileUpload("consume_three_events_from_different_brokers_service_updated");
    }

    @Test
    public void testUploadPostSpecOutgoingWithBrokerType() throws Exception {
        testFileUpload("no_consumes_outgoing_with_broker_type_service_same_name");
    }

    @Test
    public void testUploadPostSpec2filesCommonOutgouingTopicsCommonAllEventsDifferentConsumeTopics() throws Exception {
        testFileUpload("service_no_common_consume_topics_common_events_common_outgoing_topics", 2);
    }

    @Test
    public void testUploadPostSpec1fileCommonOutgouingTopicsCommonAllEventsDifferentConsumeTopics_1() throws Exception {
        testFileUpload("service_no_common_consume_topics_common_events_common_outgoing_topics_1", 1);
    }

    @Test
    public void testUploadPostSpec1fileCommonOutgouingTopicsCommonAllEventsDifferentConsumeTopics_2() throws Exception {
        testFileUpload("service_no_common_consume_topics_common_events_common_outgoing_topics_2", 1);
    }

    @Test
    public void testUploadPostSpec2filesCommonOutgouingTopicsCommonAllEventsDifferentConsumeTopicsWithTags() throws Exception {
        testFileUpload("service_no_common_consume_topics_common_events_common_outgoing_topics_with_tags", 2);
    }

    @Test public void testUploadKafkaHandlerNoGroup() throws Exception {
        testFileUpload("consume_two_events_in_one_topic_no_group");
    }

    @Test public void testConsumeNoXIncoming() throws Exception {
        testFileUpload("consume_three_events_from_different_brokers_service_no_x_incoming");
    }

    @Test public void testImportWithHttp() throws Exception {
        testFileUpload("consume_one_event_service_with_http");
    }

    @Test
    public void testHttpRequestProcessing() throws Exception {
        testFileUpload("http_request_processing_service");
    }

    @Test
    public void testSomeServiceSpecWithHttp() throws Exception {
        testFileUpload("some-service-spec-with-http");
    }

    @Test
    public void testHttpRequestTest() throws Exception {
        testFileUpload("http-request-test");
    }



    public void deepCompare(EventGraphDTO expected, EventGraphDTO actual) {
        EventGraphFacade expectedFacade = new EventGraphFacade(expected);
        EventGraphFacade actualFacade = new EventGraphFacade(actual);

        log.info("Actual graph - nodes: {}, links: {}", actual.getNodes().size(), actual.getLinks().size());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String actualGraphJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual);
            log.info("Full actual graph content:\n{}", actualGraphJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize actual graph to JSON", e);
        }

        log.info("Comparing graphs: expected={} vs actual={}", expected.getName(), actual.getName());

        // --- TAGS ---
        assertEquals(expected.getTags().size(), actual.getTags().size(),
                "Tags count mismatch: expected " + expected.getTags().size() +
                        ", but got " + actual.getTags().size());
        assertTrue(expected.getTags().containsAll(actual.getTags()),
                "Actual tags contain unexpected values: " + actual.getTags());
        assertTrue(actual.getTags().containsAll(expected.getTags()),
                "Actual tags missing expected values: " + expected.getTags());

        // --- NAME ---
        assertEquals(expected.getName(), actual.getName(),
                "Graph name mismatch: expected '" + expected.getName() +
                        "', but got '" + actual.getName() + "'");

        // --- NODES ---
        assertEquals(expected.getNodes().size(), actual.getNodes().size(),
                "Node count mismatch: expected " + expected.getNodes().size() +
                        " nodes, but found " + actual.getNodes().size());

        log.info("Expected nodes count: {}, Actual nodes count: {}", expected.getNodes().size(), actual.getNodes().size());

        Map<String, NodeDTO> actualNodesMap = actual.getNodes().stream()
                .collect(Collectors.toMap(n -> n.getName() + n.getType() + n.getBrokerType() + n.getMethodType(), Function.identity()));

        Map<UUID, NodeDTO> expectedNodesIdMap = expected.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));
        Map<UUID, NodeDTO> actualNodesIdMap = actual.getNodes().stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));

        for (NodeDTO expectedNode : expected.getNodes()) {
            log.info("Checking expected node: {}", expectedNode);
            NodeDTO actualNode = actualNodesMap.get(expectedNode.getName() + expectedNode.getType() + expectedNode.getBrokerType() + expectedNode.getMethodType());
            assertNotNull(actualNode,
                    "Missing node with name: " + expectedNode.getName() +
                            ", type: " + expectedNode.getType() +
                            ", brokerType: " + expectedNode.getBrokerType());
            assertEquals(expectedNode.getType(), actualNode.getType(),
                    "Node type mismatch for name: " + expectedNode.getName());
            assertEquals(expectedNode.getBrokerType(), actualNode.getBrokerType(),
                    "Node broker type mismatch for name: " + expectedNode.getName());

            log.info("Node {} matched successfully", expectedNode.getName());

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
                assertTrue(equalBtg,
                        "BelongsToGraph mismatch for node name: " + expectedNode.getName());
            }
        }

        // --- LINKS ---
        assertEquals(expected.getLinks().size(), actual.getLinks().size(),
                "Link count mismatch: expected " + expected.getLinks().size() +
                        ", but got " + actual.getLinks().size());

        log.info("Expected links count: {}, Actual links count: {}", expected.getLinks().size(), actual.getLinks().size());

        Set<LinkDTO> actualLinksSet = new HashSet<>(actual.getLinks());
        assertEquals(actual.getLinks().size(), actualLinksSet.size(),
                "Duplicate links found in actual graph");

        for (LinkDTO expectedLink : expected.getLinks()) {
            NodeDTO fromNodeExpected = expectedNodesIdMap.get(expectedLink.getFromId());
            NodeDTO toNodeExpected = expectedNodesIdMap.get(expectedLink.getToId());
            EventDTO eventExpected = expectedFacade.getEventById(expectedLink.getEventId());

            log.info("Checking link from {} to {} with event {}", fromNodeExpected, toNodeExpected, eventExpected);

            boolean equalLink = false;
            if (fromNodeExpected != null && toNodeExpected != null && eventExpected != null) {
                for (LinkDTO actualLink : actualLinksSet) {
                    assertNotNull(actualLink.getId(), "Link ID is null " + actualLink);
                    NodeDTO fromNodeActual = actualNodesIdMap.get(actualLink.getFromId());
                    NodeDTO toNodeActual = actualNodesIdMap.get(actualLink.getToId());
                    EventDTO eventActual = actualFacade.getEventById(actualLink.getEventId());
                    if (fromNodeActual != null && toNodeActual != null && eventActual != null) {
                        if (fromNodeExpected.getName().equals(fromNodeActual.getName()) &&
                                toNodeExpected.getName().equals(toNodeActual.getName()) &&
                                eventExpected.getName().equals(eventActual.getName()) &&
                                fromNodeExpected.getType().equals(fromNodeActual.getType()) &&
                                toNodeExpected.getType().equals(toNodeActual.getType()) &&
                                fromNodeExpected.getBrokerType() == fromNodeActual.getBrokerType() &&
                                toNodeExpected.getBrokerType() == toNodeActual.getBrokerType()) {
                            equalLink = true;
                            break;
                        }
                    }
                }
            }
            assertTrue(equalLink,
                    "Link mismatch from " + (fromNodeExpected != null ? fromNodeExpected.getName() : "unknown") +
                            " to " + (toNodeExpected != null ? toNodeExpected.getName() : "unknown") +
                            " with event " + (eventExpected != null ? eventExpected.getName() : "unknown"));
        }

        // --- EVENTS ---
        assertEquals(expected.getEvents().size(), actual.getEvents().size(),
                "Event count mismatch: expected " + expected.getEvents().size() +
                        ", but got " + actual.getEvents().size());

        log.info("Expected events count: {}, Actual events count: {}", expected.getEvents().size(), actual.getEvents().size());

        for (EventDTO expectedEvent : expected.getEvents()) {
            EventDTO actualEvent = actualFacade.getEvent(expectedEvent.getName());
            assertNotNull(actualEvent,
                    "No event with name: " + expectedEvent.getName());
            assertEquals(expectedEvent.getName(), actualEvent.getName(),
                    "Event name mismatch: expected '" + expectedEvent.getName() +
                            "', but got '" + actualEvent.getName() + "'");
            assertEquals(expectedEvent.getSchema().replaceAll("\\s+", ""), actualEvent.getSchema().replaceAll("\\s+", ""),
                    "Event schema mismatch for key: " + expectedEvent.getName());
        }

        // --- ERRORS ---
        assertEquals(expected.getErrors().size(), actual.getErrors().size(),
                "Error count mismatch: expected " + expected.getErrors().size() +
                        ", but got " + actual.getErrors().size());

        log.info("Expected errors count: {}, Actual errors count: {}", expected.getErrors().size(), actual.getErrors().size());

        Set<ErrorDTO> actualErrorsSet = new HashSet<>(actual.getErrors());
        for (ErrorDTO expectedError : expected.getErrors()) {
            assertTrue(actualErrorsSet.contains(expectedError),
                    "Missing error: " + errorToString(expectedError));
        }
    }


    private String errorToString(ErrorDTO error) {
        return String.format("Error{filename=%s, error=%s}", error.getFileName(), error.getErrorMessage());
    }

    @Test
    void testAddServiceToGraphPost_withValidYamlFileAndEmptyGraph_shouldReturnGraph() throws Exception {
        ClassPathResource resource = new ClassPathResource("axenapi-be.yml");

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "axenapi-be.yml",
                "application/x-yaml",
                resource.getInputStream());

        String emptyGraphJson = "{}";

        MockMultipartFile graphPart = new MockMultipartFile(
                "eventGraph",
                "eventGraph.json",
                "application/json",
                emptyGraphJson.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/addServiceToGraph")
                        .file(file)
                        .file(graphPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.links").isArray())
                .andExpect(jsonPath("$.nodes.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        log.info("Final graph JSON:\n" + responseBody);
    }




}
