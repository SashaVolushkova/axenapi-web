package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pro.axenix_innovation.axenapi.web.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerGetServiceSpecTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetServiceSpecification() throws Exception {
        // Test with a specific case
        test("consume_one_event_service", "consume_one_event_service");
    }

    @Test
    public void testGetServiceSpecificationWithUndefinedBroker() throws Exception {
        // Create EventGraphDTO with UNDEFINED broker type
        EventGraphDTO eventGraph = createEventGraphWithUndefinedBroker();
        
        // Find the service node ID
        UUID serviceNodeId = findServiceNodeId(eventGraph, "Undefined Broker Service");
        assertNotNull(serviceNodeId, "Could not find service node in the event graph");

        // Create the request object
        GetServiceSpecificationPostRequest request = new GetServiceSpecificationPostRequest();
        request.setEventGraph(eventGraph);
        request.setServiceNodeId(serviceNodeId);

        // Convert request to JSON
        String requestJson = objectMapper.writeValueAsString(request);

        // Execute the request
        MvcResult result = mockMvc.perform(post("/getServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        
        // Extract the specification string
        String specification = responseJson.get("specification").asText();
        assertNotNull(specification, "Specification should not be null");
        assertFalse(specification.isEmpty(), "Specification should not be empty");
        
        // Parse the specification as JSON to verify it contains the expected path
        JsonNode specJson = objectMapper.readTree(specification);
        
        // Verify the specification contains the undefined_broker path
        JsonNode paths = specJson.get("paths");
        assertNotNull(paths, "Paths should be present in specification");
        
        JsonNode undefinedBrokerPath = paths.get("/undefined_broker/topic1/Event1");
        assertNotNull(undefinedBrokerPath, "Should contain path /undefined_broker/topic1/Event1");
        
        // Verify it has a POST method
        JsonNode postMethod = undefinedBrokerPath.get("post");
        assertNotNull(postMethod, "Path should have POST method");
        
        System.out.println("Generated specification contains the expected undefined_broker path");
    }

    @Test
    public void testGetServiceSpecificationWithNullBrokerType() throws Exception {
        // Test with the specific request provided - topic with brokerType: null
        String requestJson = """
                {
                  "serviceNodeId": "1c3dcbd1-54f4-49c1-be72-f95d1653bac7",
                  "eventGraph": {
                    "name": "40d5fd4f-9e5a-448a-b449-479ea4093006",
                    "nodes": [
                      {
                        "id": "5b81ecd8-dcee-409e-a41b-8c9d3ca164fe",
                        "belongsToGraph": [],
                        "name": "Topic_0",
                        "type": "TOPIC",
                        "brokerType": null
                      },
                      {
                        "id": "1c3dcbd1-54f4-49c1-be72-f95d1653bac7",
                        "belongsToGraph": [],
                        "name": "Service_0",
                        "type": "SERVICE",
                        "brokerType": null
                      }
                    ],
                    "events": [
                      {
                        "id": "4bc9a400-fa94-415e-ab10-76266c2ccf79",
                        "name": "e",
                        "schema": "{\\"type\\": \\"object\\"}",
                        "eventDescription": "",
                        "tags": []
                      }
                    ],
                    "links": [
                      {
                        "id": "3865a957-6ac8-47c1-aa27-e8d2161a7cc9",
                        "fromId": "5b81ecd8-dcee-409e-a41b-8c9d3ca164fe",
                        "toId": "1c3dcbd1-54f4-49c1-be72-f95d1653bac7",
                        "group": "",
                        "eventId": "4bc9a400-fa94-415e-ab10-76266c2ccf79"
                      }
                    ],
                    "errors": [],
                    "tags": []
                  }
                }
                """;

        System.out.println("Testing getServiceSpecification with null brokerType");
        System.out.println("Request: " + requestJson);

        // Execute the request
        MvcResult result = mockMvc.perform(post("/getServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response
        String responseContent = result.getResponse().getContentAsString();
        System.out.println("Response: " + responseContent);
        
        JsonNode responseJson = objectMapper.readTree(responseContent);
        
        // Extract the specification string
        String specification = responseJson.get("specification").asText();
        assertNotNull(specification, "Specification should not be null");
        assertFalse(specification.isEmpty(), "Specification should not be empty");
        
        System.out.println("Generated specification: " + specification);
        
        // Parse the specification as JSON to verify its structure
        JsonNode specJson = objectMapper.readTree(specification);
        
        // Verify basic OpenAPI structure
        assertNotNull(specJson.get("openapi"), "Should have openapi version");
        assertNotNull(specJson.get("info"), "Should have info section");
        assertEquals("Service_0", specJson.get("info").get("title").asText(), "Service name should match");
        
        // Verify paths section exists
        JsonNode paths = specJson.get("paths");
        assertNotNull(paths, "Paths should be present in specification");
        
        // Since brokerType is null, it should be treated as undefined and generate /undefined_broker path
        JsonNode undefinedBrokerPath = paths.get("/undefined_broker/Topic_0/e");
        assertNotNull(undefinedBrokerPath, "Should contain path /undefined_broker/Topic_0/e for null brokerType");
        
        // Verify it has a POST method
        JsonNode postMethod = undefinedBrokerPath.get("post");
        assertNotNull(postMethod, "Path should have POST method");
        
        // Verify components section with schemas
        JsonNode components = specJson.get("components");
        assertNotNull(components, "Should have components section");
        
        JsonNode schemas = components.get("schemas");
        assertNotNull(schemas, "Should have schemas section");
        
        // Should have schema for event "e"
        JsonNode eventSchema = schemas.get("e");
        assertNotNull(eventSchema, "Should have schema for event 'e'");
        
        // Verify the schema has x-incoming extension
        JsonNode xIncoming = eventSchema.get("x-incoming");
        assertNotNull(xIncoming, "Event schema should have x-incoming extension");
        
        JsonNode topics = xIncoming.get("topics");
        assertNotNull(topics, "x-incoming should have topics array");
        assertTrue(topics.isArray(), "topics should be an array");
        assertEquals("Topic_0", topics.get(0).asText(), "Should reference Topic_0");
        
        System.out.println("Test completed successfully - null brokerType treated as undefined and generated /undefined_broker path");
    }

    private void test(String caseName, String serviceName) throws IOException, Exception {
        // Load event graph from test resources
        Resource eventGraphFile = new ClassPathResource("results/" + caseName + ".json");
        EventGraphDTO eventGraph = objectMapper.readValue(eventGraphFile.getInputStream(), EventGraphDTO.class);

        // Find a service node ID from the event graph
        UUID serviceNodeId = findServiceNodeId(eventGraph, serviceName);
        assertNotNull(serviceNodeId, "Could not find a service node in the event graph");

        // Create the request object
        GetServiceSpecificationPostRequest request = new GetServiceSpecificationPostRequest();
        request.setEventGraph(eventGraph);
        request.setServiceNodeId(serviceNodeId);

        // Convert request to JSON
        String requestJson = objectMapper.writeValueAsString(request);

        // Execute the request
        MvcResult result = mockMvc.perform(post("/getServiceSpecification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();
    }

    private UUID findServiceNodeId(EventGraphDTO eventGraph, String serviceName) {
        return eventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE && node.getName().equals(serviceName))
                .findFirst()
                .map(NodeDTO::getId)
                .orElse(null);
    }

    private EventGraphDTO createEventGraphWithUndefinedBroker() {
        EventGraphDTO eventGraph = new EventGraphDTO();
        eventGraph.setName("Test Graph with Undefined Broker");
        
        // Create service node
        UUID serviceNodeId = UUID.randomUUID();
        NodeDTO serviceNode = new NodeDTO();
        serviceNode.setId(serviceNodeId);
        serviceNode.setName("Undefined Broker Service");
        serviceNode.setType(NodeDTO.TypeEnum.SERVICE);
        serviceNode.setBelongsToGraph(List.of(serviceNodeId));
        
        // Create topic node with UNDEFINED broker type
        UUID topicNodeId = UUID.randomUUID();
        NodeDTO topicNode = new NodeDTO();
        topicNode.setId(topicNodeId);
        topicNode.setName("topic1");
        topicNode.setType(NodeDTO.TypeEnum.TOPIC);
        topicNode.setBrokerType(NodeDTO.BrokerTypeEnum.UNDEFINED);
        topicNode.setBelongsToGraph(List.of(serviceNodeId));
        
        // Create event
        UUID eventId = UUID.randomUUID();
        EventDTO event = new EventDTO();
        event.setId(eventId);
        event.setName("Event1");
        event.setSchema("{\"type\": \"object\", \"properties\": {\"message\": {\"type\": \"string\"}}}");
        
        // Create link from topic to service (incoming message)
        UUID linkId = UUID.randomUUID();
        LinkDTO link = new LinkDTO();
        link.setId(linkId);
        link.setFromId(topicNodeId);
        link.setToId(serviceNodeId);
        link.setEventId(eventId);
        
        // Set up the graph
        List<NodeDTO> nodes = new ArrayList<>();
        nodes.add(serviceNode);
        nodes.add(topicNode);
        eventGraph.setNodes(nodes);
        
        List<EventDTO> events = new ArrayList<>();
        events.add(event);
        eventGraph.setEvents(events);
        
        List<LinkDTO> links = new ArrayList<>();
        links.add(link);
        eventGraph.setLinks(links);
        
        return eventGraph;
    }

}
