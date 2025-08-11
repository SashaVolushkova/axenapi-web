package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pro.axenix_innovation.axenapi.web.model.GetServiceSpecificationPostRequest;
import pro.axenix_innovation.axenapi.web.model.UpdateServiceSpecificationPostRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.ErrorDTO;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerAddAndUpdateTest {

    private static final Logger logger = LoggerFactory.getLogger(AxenAPIControllerAddAndUpdateTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAddServiceToGraph() throws Exception {
        // 1. Load the initial graph
        EventGraphDTO initialGraph;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("results/complex_service_graph.json")) {
            assertNotNull(is, "Cannot find complex_service_graph.json");
            initialGraph = objectMapper.readValue(is, EventGraphDTO.class);
        }

        NodeDTO serviceNode = initialGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Service node not found in the initial graph"));

        // 2. Read the service specification from file
        String specification;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("specs/json/complex_service_spec.json")) {
            assertNotNull(is, "Cannot find complex_service_spec.json");
            specification = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Read specification from file: " + specification);
        }

        // 3. Create a MockMultipartFile from the specification
        MockMultipartFile specFile = new MockMultipartFile(
                "files",
                "complex_service_spec.json",
                MediaType.APPLICATION_JSON_VALUE,
                specification.getBytes(StandardCharsets.UTF_8)
        );

        // 4. Create an empty EventGraphDTO
        String emptyGraphJson = "{}";
        MockMultipartFile eventGraphFile = new MockMultipartFile("eventGraph", "eventGraph.json",
                MediaType.APPLICATION_JSON_VALUE, emptyGraphJson.getBytes(StandardCharsets.UTF_8));


        // 5. Call addServiceToGraph
        MvcResult addServiceResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/addServiceToGraph")
                        .file(specFile)
                        .file(eventGraphFile))
                .andReturn();

        String responseContent = addServiceResult.getResponse().getContentAsString();
        System.out.println("Response status: " + addServiceResult.getResponse().getStatus());
        System.out.println("Response from /addServiceToGraph: " + responseContent);

        assertEquals(200, addServiceResult.getResponse().getStatus());
        EventGraphDTO resultingGraph = objectMapper.readValue(responseContent, EventGraphDTO.class);

        // 6. Compare the graphs
        deepCompare(initialGraph, resultingGraph);
    }

    public void deepCompare(EventGraphDTO expected, EventGraphDTO actual) {
        EventGraphFacade expectedFacade = new EventGraphFacade(expected);
        EventGraphFacade actualFacade = new EventGraphFacade(actual);

        // if (expected.getTags() != null || actual.getTags() != null) {
        //     logger.info("Expected tags: " + expected.getTags());
        //     logger.info("Actual tags: " + actual.getTags());
        //     assertEquals(expected.getTags().size(), actual.getTags().size());
        //     assertTrue(expected.getTags().containsAll(actual.getTags()));
        //     assertTrue(actual.getTags().containsAll(expected.getTags()));
        // }
        // Validate root properties
        // assertEquals(expected.getName(), actual.getName(), "Graph name mismatch");

        if (expected.getTags() != null || actual.getTags() != null) {
            assertEquals(expected.getTags().size(), actual.getTags().size());
            assertTrue(expected.getTags().containsAll(actual.getTags()));
            assertTrue(actual.getTags().containsAll(expected.getTags()));
        }

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
            if (expectedNode.getTags() != null || actualNode.getTags() != null) {
                Set<String> expectedTags = new HashSet<>(expectedNode.getTags());
                Set<String> actualTags = new HashSet<>(actualNode.getTags());
                assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for node " + expectedNode.getName());
                assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for node " + expectedNode.getName());
            }
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
            if (fromNodeExpected != null && toNodeExpected != null) {
                for (LinkDTO actualLink : actualLinksSet) {
                    assertNotNull(actualLink.getId(), "Link ID is null " + actualLink);
                    // find link nodes
                    NodeDTO fromNodeActual = actualNodesIdMap.get(actualLink.getFromId());
                    NodeDTO toNodeActual = actualNodesIdMap.get(actualLink.getToId());
                    // find event
                    EventDTO eventActual = actualFacade.getEventById(actualLink.getEventId());
                    //check if fromNode, toNode and event are not null
                    if (fromNodeActual != null && toNodeActual != null) {
                        // compare expected and actual fromNode, toNode and event by name
                        if (fromNodeExpected.getName().equals(fromNodeActual.getName()) &&
                                toNodeExpected.getName().equals(toNodeActual.getName()) &&
                                // compare expected and actual fromNode, toNode by type
                                fromNodeExpected.getType().equals(fromNodeActual.getType()) &&
                                toNodeExpected.getType().equals(toNodeActual.getType()) &&
                                // compare expected and actual fromNode, toNode by brokerType
                                fromNodeExpected.getBrokerType() == fromNodeActual.getBrokerType() &&
                                toNodeExpected.getBrokerType() == toNodeActual.getBrokerType()) {

                            if(eventExpected != null && eventActual != null && eventExpected.getName().equals(eventActual.getName())) {
                                if (expectedLink.getTags() != null || actualLink.getTags() != null) {
                                    Set<String> expectedTags = new HashSet<>(expectedLink.getTags());
                                    Set<String> actualTags = new HashSet<>(actualLink.getTags());
                                    assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for link " + expectedLink);
                                    assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for link " + expectedLink);
                                }
                                assertEquals(expectedLink.getGroup(), actualLink.getGroup(), "Group not equals for link " + expectedLink);
                                equalLink = true;
                                break;
                            } else if (eventExpected == null && eventActual == null) {
                                if (expectedLink.getTags() != null || actualLink.getTags() != null) {
                                    Set<String> expectedTags = new HashSet<>(expectedLink.getTags());
                                    Set<String> actualTags = new HashSet<>(actualLink.getTags());
                                    assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for link " + expectedLink);
                                    assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for link " + expectedLink);
                                }
                                assertEquals(expectedLink.getGroup(), actualLink.getGroup(), "Group not equals for link " + expectedLink);
                                equalLink = true;
                                break;
                            }
                        }
                    }


                }
            }
            if (!equalLink) {
                logger.error("Link mismatch for expected link: from={}, to={}, event={}",
                        fromNodeExpected.getName(), toNodeExpected.getName(),
                        eventExpected != null ? eventExpected.getName() : "null");
            }
            assertTrue(equalLink, "Link mismatch from " + fromNodeExpected.getName() + " to " + toNodeExpected.getName());
        }

        logger.info("Comparing events...");
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
            if (expectedEvent.getTags() != null || actualEvent.getTags() != null) {
                Set<String> expectedTags = new HashSet<>(expectedEvent.getTags());
                Set<String> actualTags = new HashSet<>(actualEvent.getTags());
                assertEquals(expectedTags.size(), actualTags.size(), "Node tags collection size mismatch for event " + expectedEvent.getName());
                assertTrue(actualTags.containsAll(expectedTags), "Not all tags are in actualNode for event " + expectedEvent.getName());
            }
        }

        // Order-agnostic error comparison
        if(expected.getErrors() != null && actual.getErrors() != null) {
            assertEquals(expected.getErrors().size(), actual.getErrors().size(), "Error count mismatch");
            Set<ErrorDTO> actualErrorsSet = new HashSet<>(actual.getErrors());
            for (ErrorDTO expectedError : expected.getErrors()) {
                assertTrue(actualErrorsSet.contains(expectedError), "Missing error: " + errorToString(expectedError));
            }
        } else if (expected.getErrors() != null || actual.getErrors() != null) {
            fail("One of the error lists is null while the other is not.");
        }
    }

    private String errorToString(ErrorDTO error) {
        return String.format("Error{filename=%s, error=%s}", error.getFileName(), error.getErrorMessage());
    }

    @Test
    void testUpdateServiceSpecification() throws Exception {
        // 1. Load the initial graph
        EventGraphDTO initialGraph;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("results/complex_service_graph.json")) {
            assertNotNull(is, "Cannot find complex_service_graph.json");
            initialGraph = objectMapper.readValue(is, EventGraphDTO.class);
        }

        NodeDTO serviceNode = initialGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Service node not found in the initial graph"));

        // 2. Read the service specification from file
        String specification;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("specs/json/complex_service_spec.json")) {
            assertNotNull(is, "Cannot find complex_service_spec.json");
            specification = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 3. Create a new EventGraphDTO with only the service node
        EventGraphDTO graphToUpdate = new EventGraphDTO();
        graphToUpdate.setName("Graph-To-Update");
        graphToUpdate.addNodesItem(NodeDTO.builder()
                .id(serviceNode.getId())
                .name(serviceNode.getName())
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>(List.of(serviceNode.getId())))
                .build());

        // 4. Call updateServiceSpecification
        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest();
        request.setEventGraph(graphToUpdate);
        request.setServiceNodeId(serviceNode.getId());
        request.setSpecification(specification);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/updateServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        EventGraphDTO resultingGraph = objectMapper.readValue(result.getResponse().getContentAsString(), EventGraphDTO.class);

        // 5. Compare the graphs
        deepCompare(initialGraph, resultingGraph);
    }
}
