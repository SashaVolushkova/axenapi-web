package pro.axenix_innovation.axenapi.web.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.lang.reflect.Field;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.util.openapi.generator.OpenApiGeneratorFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OpenAPITranslatorFromJsonToSpecTest {
    @BeforeEach
    void setUp() {
        MessageSource mockMessageSource = mock(MessageSource.class);
        when(mockMessageSource.getMessage(anyString(), any(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try {
            Field field = MessageHelper.class.getDeclaredField("staticMessageSource");
            field.setAccessible(true);
            field.set(null, mockMessageSource);
        } catch (Exception e) {
            fail("Failed to set MessageSource in MessageHelper", e);
        }
    }
    // Creates OpenAPI specifications for all service nodes in the event graph
    @Test
    public void test_creates_openapi_specifications_for_service_nodes() throws JsonProcessingException {
        // Arrange
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode1 = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        NodeDTO serviceNode2 = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceB")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        eventGraph.addNode(serviceNode1);
        eventGraph.addNode(serviceNode2);

        // Act
        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey("ServiceA"));
        assertTrue(result.containsKey("ServiceB"));

        OpenAPI serviceASpec = result.get("ServiceA");
        assertEquals("ServiceA", serviceASpec.getInfo().getTitle());
        assertEquals("AxenAPI Specification for ServiceA", serviceASpec.getInfo().getDescription());
        assertEquals("1.0.0", serviceASpec.getInfo().getVersion());
        assertNotNull(serviceASpec.getPaths());

        OpenAPI serviceBSpec = result.get("ServiceB");
        assertEquals("ServiceB", serviceBSpec.getInfo().getTitle());
        assertEquals("AxenAPI Specification for ServiceB", serviceBSpec.getInfo().getDescription());
        assertEquals("1.0.0", serviceBSpec.getInfo().getVersion());
        assertNotNull(serviceBSpec.getPaths());
    }

    // Processes incoming links from topics to services correctly
    @Test
    public void test_processes_incoming_links_from_topics_to_services() throws JsonProcessingException {
        // Arrange
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceNode")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicNode")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.KAFKA)
                .belongsToGraph(new ArrayList<>())
                .build();

        NodeDTO topicRabbit = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicRabbit")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.RABBITMQ)
                .belongsToGraph(new ArrayList<>())
                .build();
        NodeDTO topicJMS = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicJMS")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.JMS)
                .belongsToGraph(new ArrayList<>())
                .build();

        EventDTO event = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EventName")
                .schema("{\"type\":\"object\"}")
                .build();

        LinkDTO link = new LinkDTO(UUID.randomUUID(), topicNode.getId(), serviceNode.getId(), "group", event.getId(), new HashSet<>(List.of("tag1")));
        LinkDTO linkRabbit = new LinkDTO(UUID.randomUUID(), topicRabbit.getId(), serviceNode.getId(), null, event.getId(), new HashSet<>());
        LinkDTO linkJMS = new LinkDTO(UUID.randomUUID(), topicJMS.getId(), serviceNode.getId(), null, event.getId(), new HashSet<>());

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(topicNode);
        eventGraph.addNode(topicRabbit);
        eventGraph.addNode(topicJMS);

        eventGraph.addEvent(event);
        eventGraph.addLink(link);
        eventGraph.addLink(linkRabbit);
        eventGraph.addLink(linkJMS);

        // Act
        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceNode"));

        OpenAPI serviceSpec = result.get("ServiceNode");
        assertEquals(3, serviceSpec.getPaths().size());
        assertNotNull(serviceSpec.getPaths().get("/kafka/group/TopicNode/EventName"));
        PathItem pathItem = serviceSpec.getPaths().get("/kafka/group/TopicNode/EventName");
        assertNotNull(pathItem.getPost());
        assertNotNull(pathItem.getPost().getTags());
        assertIterableEquals(pathItem.getPost().getTags(), Set.of("tag1"));
        assertNotNull(serviceSpec.getPaths().get("/rabbitmq/TopicRabbit/EventName"));
        assertNotNull(serviceSpec.getPaths().get("/jms/TopicJMS/EventName"));
        assertNotNull(serviceSpec.getComponents().getSchemas().get("EventName"));
    }

    // Processes outgoing links from services to topics correctly
    @Test
    public void test_processes_outgoing_links_from_services_to_topics() throws JsonProcessingException {
        // Arrange
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.KAFKA)
                .belongsToGraph(new ArrayList<>())
                .build();

        EventDTO event = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EventA")
                .schema("{\"type\": \"object\"}")
                .build();

        LinkDTO link = new LinkDTO(UUID.randomUUID(), serviceNode.getId(), topicNode.getId(), "group1", event.getId(), Set.of("tag1", "tag2"));

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(topicNode);
        eventGraph.addEvent(event);
        eventGraph.addLink(link);

        // Act
        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI serviceASpec = result.get("ServiceA");
        assertNotNull(serviceASpec.getComponents());
        assertTrue(serviceASpec.getComponents().getSchemas().containsKey("EventA"));
        Schema schema = serviceASpec.getComponents().getSchemas().get("EventA");
        assertNotNull(schema);
        Object o = schema.getExtensions().get("x-outgoing");
        assertNotNull(o);
        Map<String, Object> outgoingMap = (Map<String, Object>) o;
        Object tags = outgoingMap.get("tags");
        assertNotNull(tags);
        Object topics = outgoingMap.get("topics");
        assertNotNull(topics);
        List<String> topicsList = (List<String>) topics;
        assertIterableEquals(Set.of("KAFKA/TopicA"), topicsList);
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO httpNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("/test")
                .type(NodeDTO.TypeEnum.HTTP)
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .nodeUrl("/test")
                .belongsToGraph(List.of(serviceNode.getId()))
                .build();

        LinkDTO link = new LinkDTO(httpNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(httpNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths().get("/test"));
        assertNotNull(openAPI.getPaths().get("/test").getGet());
        assertNull(openAPI.getPaths().get("/test").getGet().getRequestBody());

    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_topic_to_service() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.KAFKA)
                .belongsToGraph(new ArrayList<>())
                .build();

        LinkDTO link = new LinkDTO(topicNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(topicNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        // Должен быть создан путь с null request body
        assertNotNull(openAPI.getPaths());
        assertEquals(1, openAPI.getPaths().size());
        assertNotNull(openAPI.getPaths().get("/kafka/default/TopicA/undefined_event"));
        assertNotNull(openAPI.getPaths().get("/kafka/default/TopicA/undefined_event").getPost());
        assertNull(openAPI.getPaths().get("/kafka/default/TopicA/undefined_event").getPost().getRequestBody());
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_jms_topic_to_service() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.JMS)
                .belongsToGraph(new ArrayList<>())
                .build();

        LinkDTO link = new LinkDTO(topicNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(topicNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths());
        assertEquals(1, openAPI.getPaths().size());
        assertNotNull(openAPI.getPaths().get("/jms/TopicA/undefined_event"));
        assertNotNull(openAPI.getPaths().get("/jms/TopicA/undefined_event").getPost());
        assertNull(openAPI.getPaths().get("/jms/TopicA/undefined_event").getPost().getRequestBody());
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_rabbitmq_topic_to_service() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.RABBITMQ)
                .belongsToGraph(new ArrayList<>())
                .build();

        LinkDTO link = new LinkDTO(topicNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(topicNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths());
        assertEquals(1, openAPI.getPaths().size());
        assertNotNull(openAPI.getPaths().get("/rabbitmq/TopicA/undefined_event"));
        assertNotNull(openAPI.getPaths().get("/rabbitmq/TopicA/undefined_event").getPost());
        assertNull(openAPI.getPaths().get("/rabbitmq/TopicA/undefined_event").getPost().getRequestBody());
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_undefined_broker_topic_to_service() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(null) // undefined broker
                .belongsToGraph(new ArrayList<>())
                .build();

        LinkDTO link = new LinkDTO(topicNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(topicNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths());
        assertEquals(1, openAPI.getPaths().size());
        assertNotNull(openAPI.getPaths().get("/undefined_broker/TopicA/undefined_event"));
        assertNotNull(openAPI.getPaths().get("/undefined_broker/TopicA/undefined_event").getPost());
        assertNull(openAPI.getPaths().get("/undefined_broker/TopicA/undefined_event").getPost().getRequestBody());
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_http_post() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO httpNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("/test")
                .type(NodeDTO.TypeEnum.HTTP)
                .methodType(NodeDTO.MethodTypeEnum.POST)
                .nodeUrl("/test")
                .belongsToGraph(List.of(serviceNode.getId()))
                .build();

        LinkDTO link = new LinkDTO(httpNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(httpNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths().get("/test"));
        assertNotNull(openAPI.getPaths().get("/test").getPost());
        assertNull(openAPI.getPaths().get("/test").getPost().getRequestBody());
        assertNull(openAPI.getPaths().get("/test").getGet());
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_http_put() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO httpNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("/test")
                .type(NodeDTO.TypeEnum.HTTP)
                .methodType(NodeDTO.MethodTypeEnum.PUT)
                .nodeUrl("/test")
                .belongsToGraph(List.of(serviceNode.getId()))
                .build();

        LinkDTO link = new LinkDTO(httpNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(httpNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths().get("/test"));
        assertNotNull(openAPI.getPaths().get("/test").getPut());
        assertNull(openAPI.getPaths().get("/test").getPut().getRequestBody());
        assertNull(openAPI.getPaths().get("/test").getGet());
    }

    @Test
    public void test_creates_spec_from_graph_with_empty_event_link_http_delete() throws JsonProcessingException {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>()).build();

        NodeDTO httpNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("/test")
                .type(NodeDTO.TypeEnum.HTTP)
                .methodType(NodeDTO.MethodTypeEnum.DELETE)
                .nodeUrl("/test")
                .belongsToGraph(List.of(serviceNode.getId()))
                .build();

        LinkDTO link = new LinkDTO(httpNode.getId(), serviceNode.getId(), null);

        eventGraph.addNode(serviceNode);
        eventGraph.addNode(httpNode);
        eventGraph.addLink(link);

        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("ServiceA"));

        OpenAPI openAPI = result.get("ServiceA");
        assertNotNull(openAPI.getPaths().get("/test"));
        assertNotNull(openAPI.getPaths().get("/test").getDelete());
        assertNull(openAPI.getPaths().get("/test").getDelete().getRequestBody());
        assertNull(openAPI.getPaths().get("/test").getGet());
    }
}