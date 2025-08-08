package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты для HttpToServiceLinkProcessor, проверяющие соответствие правилам
 * из EVENTGRAPH_TO_OPENAPI_RULES.md для связей типа "HTTP to SERVICE"
 */
public class HttpToServiceLinkProcessorIntegrationTest {
    
    private HttpToServiceLinkProcessor processor;
    private Map<String, OpenAPI> openApiMap;
    
    @BeforeEach
    void setUp() {
        processor = new HttpToServiceLinkProcessor();
        openApiMap = new HashMap<>();
        
        // Настройка MessageHelper для тестов
        MessageSource mockMessageSource = mock(MessageSource.class);
        when(mockMessageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try {
            Field field = MessageHelper.class.getDeclaredField("staticMessageSource");
            field.setAccessible(true);
            field.set(null, mockMessageSource);
        } catch (Exception e) {
            fail("Failed to set MessageSource in MessageHelper", e);
        }
    }

    /**
     * Тест правила 2.3: HTTP to SERVICE
     * "A new path is created in the OpenAPI specification based on the nodeUrl of the HTTP node."
     */
    @Test
    void testHttpToServiceCreatesPathFromNodeUrl() throws JsonProcessingException {
        // Arrange
        String httpPath = "/api/v1/users/{userId}";
        NodeDTO serviceNode = createServiceNode("UserService");
        NodeDTO httpNode = createHttpNode("GetUser", httpPath, NodeDTO.MethodTypeEnum.GET);
        EventDTO event = createEvent("UserEvent", "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("UserService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("UserService");
        assertNotNull(openAPI.getPaths().get(httpPath), 
                "Path should be created based on HTTP node's nodeUrl");
    }

    /**
     * Тест правила 2.3: HTTP to SERVICE
     * "An operation is added to the path corresponding to the methodType of the HTTP node."
     */
    @Test
    void testHttpToServiceCreatesOperationBasedOnMethodType() throws JsonProcessingException {
        // Arrange
        NodeDTO serviceNode = createServiceNode("OrderService");
        NodeDTO httpNode = createHttpNode("CreateOrder", "/orders", NodeDTO.MethodTypeEnum.POST);
        EventDTO event = createEvent("OrderCreated", "{\"type\":\"object\"}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("OrderService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("OrderService");
        PathItem pathItem = openAPI.getPaths().get("/orders");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getPost(), "POST operation should be created for POST methodType");
        assertNull(pathItem.getGet(), "GET operation should not be created");
    }

    /**
     * Тест правила 2.3: HTTP to SERVICE
     * "If the EventDTO associated with the link is not null, a new schema is created in the components/schemas section"
     */
    @Test
    void testHttpToServiceCreatesSchemaWhenEventNotNull() throws JsonProcessingException {
        // Arrange
        NodeDTO serviceNode = createServiceNode("ProductService");
        NodeDTO httpNode = createHttpNode("GetProduct", "/products/{id}", NodeDTO.MethodTypeEnum.GET);
        EventDTO event = createEvent("ProductEvent", "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("ProductService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ProductService");
        Schema<?> schema = openAPI.getComponents().getSchemas().get("ProductEvent");
        assertNotNull(schema, "Schema should be created when event is not null");
        assertEquals("object", schema.getType());
    }

    /**
     * Тест правила 2.3: HTTP to SERVICE
     * "The schema is extended with an x-http-name attribute, which contains the name of the HTTP node."
     */
    @Test
    void testHttpToServiceAddsXHttpNameExtension() throws JsonProcessingException {
        // Arrange
        String httpNodeName = "UpdateInventory";
        NodeDTO serviceNode = createServiceNode("InventoryService");
        NodeDTO httpNode = createHttpNode(httpNodeName, "/inventory/{id}", NodeDTO.MethodTypeEnum.PUT);
        EventDTO event = createEvent("InventoryUpdated", "{\"type\":\"object\"}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("InventoryService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("InventoryService");
        Schema<?> schema = openAPI.getComponents().getSchemas().get("InventoryUpdated");
        assertNotNull(schema);
        assertEquals(httpNodeName, schema.getExtensions().get("x-http-name"),
                "Schema should have x-http-name extension with HTTP node name");
    }

    /**
     * Тест правила 2.3: HTTP to SERVICE
     * "The schema is also extended with an x-incoming attribute."
     */
    @Test
    void testHttpToServiceAddsXIncomingExtension() throws JsonProcessingException {
        // Arrange
        String httpNodeName = "ProcessPayment";
        NodeDTO serviceNode = createServiceNode("PaymentService");
        NodeDTO httpNode = createHttpNode(httpNodeName, "/payments", NodeDTO.MethodTypeEnum.POST);
        EventDTO event = createEvent("PaymentProcessed", "{\"type\":\"object\"}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("PaymentService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("PaymentService");
        Schema<?> schema = openAPI.getComponents().getSchemas().get("PaymentProcessed");
        assertNotNull(schema);
        
        Object xIncoming = schema.getExtensions().get("x-incoming");
        assertNotNull(xIncoming, "Schema should have x-incoming extension");
        assertTrue(xIncoming instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> incomingMap = (Map<String, Object>) xIncoming;
        
        // Проверяем topics
        Object topics = incomingMap.get("topics");
        assertNotNull(topics);
        assertTrue(topics instanceof List);
        @SuppressWarnings("unchecked")
        List<String> topicsList = (List<String>) topics;
        assertEquals(1, topicsList.size());
        assertEquals(httpNodeName, topicsList.get(0));
    }

    /**
     * Тест правила 4: Extensions - x-documentation-file-links
     * "This extension is added to services and paths to link to external documentation."
     */
    @Test
    void testHttpToServiceAddsDocumentationFileLinksExtension() throws JsonProcessingException {
        // Arrange
        Set<String> docLinks = Set.of("https://docs.example.com/api", "https://wiki.example.com/guide");
        NodeDTO serviceNode = createServiceNode("DocumentedService");
        NodeDTO httpNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("DocumentedEndpoint")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/documented")
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .documentationFileLinks(docLinks)
                .build();
        EventDTO event = createEvent("DocumentedEvent", "{\"type\":\"object\"}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("DocumentedService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("DocumentedService");
        
        // Проверяем расширение в PathItem
        PathItem pathItem = openAPI.getPaths().get("/documented");
        assertNotNull(pathItem);
        Object pathExtension = pathItem.getExtensions().get("x-documentation-file-links");
        assertNotNull(pathExtension);
        assertTrue(pathExtension instanceof List);
        @SuppressWarnings("unchecked")
        List<String> pathDocLinks = (List<String>) pathExtension;
        assertTrue(pathDocLinks.containsAll(docLinks));
        
        // Проверяем расширение в Schema
        Schema<?> schema = openAPI.getComponents().getSchemas().get("DocumentedEvent");
        assertNotNull(schema);
        Object schemaExtension = schema.getExtensions().get("x-documentation-file-links");
        assertNotNull(schemaExtension);
        assertTrue(schemaExtension instanceof List);
        @SuppressWarnings("unchecked")
        List<String> schemaDocLinks = (List<String>) schemaExtension;
        assertTrue(schemaDocLinks.containsAll(docLinks));
    }

    /**
     * Тест обработки тегов в соответствии с правилами
     * Теги должны собираться из link и event и включать "HTTP"
     */
    @Test
    void testHttpToServiceProcessesTags() throws JsonProcessingException {
        // Arrange
        Set<String> linkTags = Set.of("api", "v1");
        Set<String> eventTags = Set.of("user", "management");
        
        NodeDTO serviceNode = createServiceNode("TaggedService");
        NodeDTO httpNode = createHttpNode("TaggedEndpoint", "/tagged", NodeDTO.MethodTypeEnum.GET);
        EventDTO event = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("TaggedEvent")
                .schema("{\"type\":\"object\"}")
                .tags(eventTags)
                .build();
        LinkDTO link = new LinkDTO(UUID.randomUUID(), httpNode.getId(), serviceNode.getId(), 
                null, event.getId(), linkTags);
        
        setupOpenAPI("TaggedService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("TaggedService");
        PathItem pathItem = openAPI.getPaths().get("/tagged");
        assertNotNull(pathItem);
        
        Operation getOperation = pathItem.getGet();
        assertNotNull(getOperation);
        List<String> operationTags = getOperation.getTags();
        
        // Должны быть все теги + "HTTP"
        assertTrue(operationTags.contains("HTTP"));
        assertTrue(operationTags.containsAll(linkTags));
        assertTrue(operationTags.containsAll(eventTags));
        
        // Проверяем теги в x-incoming расширении схемы
        Schema<?> schema = openAPI.getComponents().getSchemas().get("TaggedEvent");
        @SuppressWarnings("unchecked")
        Map<String, Object> xIncoming = (Map<String, Object>) schema.getExtensions().get("x-incoming");
        @SuppressWarnings("unchecked")
        List<String> schemaTags = (List<String>) xIncoming.get("tags");
        
        assertTrue(schemaTags.contains("HTTP"));
        assertTrue(schemaTags.containsAll(linkTags));
        assertTrue(schemaTags.containsAll(eventTags));
    }

    /**
     * Тест создания операции в соответствии с methodType для HTTP to SERVICE связей с событием
     */
    @Test
    void testHttpToServiceCreatesOperationBasedOnMethodTypeWithEvent() throws JsonProcessingException {
        // Arrange
        NodeDTO serviceNode = createServiceNode("EventService");
        NodeDTO httpNode = createHttpNode("EventEndpoint", "/events/{eventId}", NodeDTO.MethodTypeEnum.GET);
        EventDTO event = createEvent("EventData", "{\"type\":\"object\"}");
        LinkDTO link = createLink(httpNode, serviceNode, event);
        
        setupOpenAPI("EventService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("EventService");
        PathItem pathItem = openAPI.getPaths().get("/events/{eventId}");
        assertNotNull(pathItem);
        
        // Должна быть создана только GET операция (соответствует methodType)
        assertNotNull(pathItem.getGet(), "GET operation should be created");
        assertNull(pathItem.getPatch(), "PATCH operation should not be created");
        assertNull(pathItem.getPost(), "POST operation should not be created");

        assertNull(pathItem.getGet().getParameters());
        
        // Ответы должны соответствовать тому, что указано в EventGraphDTO
        // Поскольку httpNode.getHttpResponses() == null, ответов не должно быть
        assertEquals(0, pathItem.getGet().getResponses().size(), 
                "No responses should be created when httpResponses is null");
    }

    /**
     * Тест обработки случая без события (event == null)
     */
    @Test
    void testHttpToServiceWithoutEvent() throws JsonProcessingException {
        // Arrange
        NodeDTO serviceNode = createServiceNode("SimpleService");
        NodeDTO httpNode = createHttpNode("SimpleEndpoint", "/simple", NodeDTO.MethodTypeEnum.POST);
        LinkDTO link = createLink(httpNode, serviceNode, null);
        
        setupOpenAPI("SimpleService");

        // Act
        processor.process(openApiMap, link, serviceNode, httpNode, null);

        // Assert
        OpenAPI openAPI = openApiMap.get("SimpleService");
        PathItem pathItem = openAPI.getPaths().get("/simple");
        assertNotNull(pathItem);
        
        // Должна быть создана только POST операция с простым ответом
        assertNotNull(pathItem.getPost());
        assertNull(pathItem.getGet());
        assertNull(pathItem.getPatch());
        
        // Ответ должен быть простым "OK"
        assertEquals("OK", pathItem.getPost().getResponses().get("200").getDescription());
        
        // Схемы не должно быть создано
        assertTrue(openAPI.getComponents().getSchemas().isEmpty());
    }

    // Вспомогательные методы ��ля создания тестовых объектов
    
    private NodeDTO createServiceNode(String name) {
        return NodeDTO.builder()
                .id(UUID.randomUUID())
                .name(name)
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();
    }
    
    private NodeDTO createHttpNode(String name, String url, NodeDTO.MethodTypeEnum method) {
        return NodeDTO.builder()
                .id(UUID.randomUUID())
                .name(name)
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl(url)
                .methodType(method)
                .build();
    }
    
    private EventDTO createEvent(String name, String schema) {
        return EventDTO.builder()
                .id(UUID.randomUUID())
                .name(name)
                .schema(schema)
                .build();
    }
    
    private LinkDTO createLink(NodeDTO fromNode, NodeDTO toNode, EventDTO event) {
        return new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), 
                null, event != null ? event.getId() : null, new HashSet<>());
    }
    
    private void setupOpenAPI(String serviceName) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new io.swagger.v3.oas.models.info.Info().title(serviceName));
        openAPI.setPaths(new Paths());
        openApiMap.put(serviceName, openAPI);
    }
}