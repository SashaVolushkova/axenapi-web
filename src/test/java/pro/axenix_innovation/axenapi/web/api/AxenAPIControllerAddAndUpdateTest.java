package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pro.axenix_innovation.axenapi.web.model.GetServiceSpecificationPostRequest;
import pro.axenix_innovation.axenapi.web.TestHelper;
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
                .andExpect(status().isOk())
                .andReturn();

        EventGraphDTO resultingGraph = objectMapper.readValue(addServiceResult.getResponse().getContentAsString(), EventGraphDTO.class);

        // 6. Compare the graphs
        TestHelper.deepCompare(initialGraph, resultingGraph);
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
        TestHelper.deepCompare(initialGraph, resultingGraph);
    }
}
