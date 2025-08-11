package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerFullCycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullCycleWithEmptyGraph() throws Exception {
        // 1. Create an empty graph
        EventGraphDTO initialGraph = new EventGraphDTO();
        initialGraph.setName("Empty Graph");

        // 2. Generate spec from the graph
        MvcResult generateSpecResult = mockMvc.perform(post("/generateSpec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialGraph)))
                .andExpect(status().isOk())
                .andReturn();
        String generateSpecResponse = generateSpecResult.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(generateSpecResponse);
        String downloadUrl = responseNode.at("/downloadLinks").elements().next().asText();

        // 3. Download the spec
        MvcResult downloadSpecResult = mockMvc.perform(get(downloadUrl))
                .andExpect(status().isOk())
                .andReturn();
        byte[] specContent = downloadSpecResult.getResponse().getContentAsByteArray();

        // 4. Create a new graph from the spec
        MockMultipartFile specFile = new MockMultipartFile("files", "spec.json", "application/json", specContent);
        EventGraphDTO emptyGraphForUpload = new EventGraphDTO();
        MockMultipartFile graphPart = new MockMultipartFile(
                "eventGraph",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(emptyGraphForUpload)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/addServiceToGraph")
                        .file(specFile)
                        .file(graphPart))
                .andExpect(status().isOk())
                .andReturn();
        String newGraphJson = uploadResult.getResponse().getContentAsString();
        EventGraphDTO newGraph = objectMapper.readValue(newGraphJson, EventGraphDTO.class);

        // 5. Compare the graphs
        // For an empty graph, the generated graph will have a default name if the initial name was not persisted.
        // The comparison logic will need to be flexible.
        // For now, let's just check that the new graph has no nodes, links, or events.
        assertTrue(newGraph.getNodes() == null || newGraph.getNodes().isEmpty());
        assertTrue(newGraph.getEvents() == null || newGraph.getEvents().isEmpty());
        assertTrue(newGraph.getLinks() == null || newGraph.getLinks().isEmpty());
    }

    @Test
    void testFullCycleWithComplexGraph() throws Exception {
        // 1. Create a complex graph
        EventGraphDTO initialGraph = createComplexGraph();

        // 2. Generate spec from the graph
        MvcResult generateSpecResult = mockMvc.perform(post("/generateSpec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialGraph)))
                .andExpect(status().isOk())
                .andReturn();
        String generateSpecResponse = generateSpecResult.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(generateSpecResponse);
        String downloadUrl = responseNode.at("/downloadLinks").elements().next().asText();

        // 3. Download the spec
        MvcResult downloadSpecResult = mockMvc.perform(get(downloadUrl))
                .andExpect(status().isOk())
                .andReturn();
        byte[] specContent = downloadSpecResult.getResponse().getContentAsByteArray();

        // 4. Create a new graph from the spec
        MockMultipartFile specFile = new MockMultipartFile("files", "spec.json", "application/json", specContent);
        EventGraphDTO emptyGraphForUpload = new EventGraphDTO();
        MockMultipartFile graphPart = new MockMultipartFile(
                "eventGraph",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(emptyGraphForUpload)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/addServiceToGraph")
                        .file(specFile)
                        .file(graphPart))
                .andExpect(status().isOk())
                .andReturn();
        String newGraphJson = uploadResult.getResponse().getContentAsString();
        EventGraphDTO newGraph = objectMapper.readValue(newGraphJson, EventGraphDTO.class);

        // 5. Compare the graphs
        assertEventGraphEquals(initialGraph, newGraph);
    }

    private void assertEventGraphEquals(EventGraphDTO expected, EventGraphDTO actual) throws Exception {
        // Normalize graphs before comparison by converting to JSON and back.
        // This handles any potential differences in object structure that are not relevant to the data.
        String expectedJson = objectMapper.writeValueAsString(expected);
        String actualJson = objectMapper.writeValueAsString(actual);

        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        JsonNode actualNode = objectMapper.readTree(actualJson);

        List<String> diffs = new ArrayList<>();
        compareJsonNodes("", expectedNode, actualNode, diffs);

        if (!diffs.isEmpty()) {
            Assertions.fail("EventGraphs are not equal:\n" + String.join("\n", diffs));
        }
    }

    private void compareJsonNodes(String path, JsonNode expected, JsonNode actual, List<String> diffs) {
        if (expected == null && actual != null) {
            diffs.add(path + ": expected null, found " + actual);
            return;
        }
        if (expected != null && actual == null) {
            diffs.add(path + ": expected " + expected + ", found null");
            return;
        }
        if (expected != null && !expected.getNodeType().equals(actual.getNodeType())) {
            diffs.add(path + ": node type mismatch. Expected " + expected.getNodeType() + ", found " + actual.getNodeType());
            return;
        }

        if (expected == null) {
            return;
        }

        switch (expected.getNodeType()) {
            case OBJECT:
                Iterator<String> fieldNames = expected.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    // Ignore fields that are not expected to be the same, like id and name of the graph
                    if (fieldName.equals("id") || (path.isEmpty() && fieldName.equals("name"))) {
                        continue;
                    }
                    String currentPath = path + "/" + fieldName;
                    compareJsonNodes(currentPath, expected.get(fieldName), actual.get(fieldName), diffs);
                }

                Iterator<String> actualFields = actual.fieldNames();
                while (actualFields.hasNext()) {
                    String fieldName = actualFields.next();
                    if (!expected.has(fieldName)) {
                        diffs.add(path + ": unexpected field in actual: " + fieldName);
                    }
                }
                break;

            case ARRAY:
                if (expected.size() != actual.size()) {
                    diffs.add(path + ": array size mismatch. Expected " + expected.size() + ", found " + actual.size());
                } else {
                    for (int i = 0; i < expected.size(); i++) {
                        compareJsonNodes(path + "[" + i + "]", expected.get(i), actual.get(i), diffs);
                    }
                }
                break;

            default:
                if (!expected.equals(actual)) {
                    diffs.add(path + ": expected " + expected + ", found " + actual);
                }
        }
    }
    private EventGraphDTO createComplexGraph() {
        EventGraphDTO graph = new EventGraphDTO();
        graph.setName("Complex Graph");

        // Nodes
        NodeDTO service1 = new NodeDTO();
        service1.setId(UUID.randomUUID());
        service1.setName("Service1");
        service1.setType("SERVICE");
        service1.setBelongsToGraph(new ArrayList<>());

        NodeDTO topic1 = new NodeDTO();
        topic1.setId(UUID.randomUUID());
        topic1.setName("Topic1");
        topic1.setType("TOPIC");
        topic1.setBrokerType("KAFKA");
        topic1.setBelongsToGraph(new ArrayList<>());
        topic1.getBelongsToGraph().add(service1.getId());

        NodeDTO service2 = new NodeDTO();
        service2.setId(UUID.randomUUID());
        service2.setName("Service2");
        service2.setType("SERVICE");
        service2.setBelongsToGraph(new ArrayList<>());

        graph.setNodes(Arrays.asList(service1, topic1, service2));

        // Events
        EventDTO event1 = new EventDTO();
        event1.setId(UUID.randomUUID());
        event1.setName("Event1");
        event1.setSchema("{}");

        graph.setEvents(Arrays.asList(event1));

        // Links
        LinkDTO link1 = new LinkDTO();
        link1.setId(UUID.randomUUID());
        link1.setFromId(service1.getId());
        link1.setToId(topic1.getId());
        link1.setEventId(event1.getId());

        LinkDTO link2 = new LinkDTO();
        link2.setId(UUID.randomUUID());
        link2.setFromId(topic1.getId());
        link2.setToId(service2.getId());
        link2.setEventId(event1.getId());

        graph.setLinks(Arrays.asList(link1, link2));

        return graph;
    }
}
