package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
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

public class HttpToServiceLinkProcessorTest {
    private HttpToServiceLinkProcessor httpToServiceLinkProcessor;
    private Map<String, OpenAPI> openApiMap;
    private NodeDTO toNode;
    private NodeDTO fromNode;
    private EventDTO event;
    private LinkDTO link;

    @BeforeEach
    void setUp() {
        httpToServiceLinkProcessor = new HttpToServiceLinkProcessor();
        openApiMap = new HashMap<>();
        toNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test")
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .nodeDescription("Test description")
                .build();
        event = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EventA")
                .schema("{\"type\":\"object\"}")
                .build();
        link = new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), null, event.getId(), new HashSet<>(List.of("tag1")));

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new io.swagger.v3.oas.models.info.Info().title("ServiceA"));
        openAPI.setPaths(new Paths());
        openApiMap.put("ServiceA", openAPI);

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

    @Test
    void testProcess() throws JsonProcessingException {
        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        assertNotNull(openAPI);

        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
        assertEquals(2, pathItem.getGet().getTags().size());
        assertTrue(pathItem.getGet().getTags().contains("tag1"));
        assertTrue(pathItem.getGet().getTags().contains("HTTP"));

        assertNotNull(openAPI.getComponents().getSchemas().get("EventA"));
    }

    @Test
    void testProcessWithNullOpenAPI() throws JsonProcessingException {
        // Arrange - удаляем OpenAPI из карты
        openApiMap.clear();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert - проверяем, что ничего не было добавлено
        assertTrue(openApiMap.isEmpty());
    }

    @Test
    void testProcessWithNullHttpPath() throws JsonProcessingException {
        // Arrange
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl(null) // null URL
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert - проверяем, что путь не был добавлен
        OpenAPI openAPI = openApiMap.get("ServiceA");
        assertNotNull(openAPI);
        assertTrue(openAPI.getPaths().isEmpty());
    }

    @Test
    void testProcessWithBlankHttpPath() throws JsonProcessingException {
        // Arrange
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("   ") // пустая строка
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert - проверяем, что путь не был добавлен
        OpenAPI openAPI = openApiMap.get("ServiceA");
        assertNotNull(openAPI);
        assertTrue(openAPI.getPaths().isEmpty());
    }

    @Test
    void testProcessWithNullEvent() throws JsonProcessingException {
        // Act - передаем null event
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, null);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        assertNotNull(openAPI);

        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
        assertNotNull(pathItem.getGet().getResponses());
        // При null event создается простая операция с ответом 200 "OK"
        assertEquals(1, pathItem.getGet().getResponses().size());
        assertEquals("OK", pathItem.getGet().getResponses().get("200").getDescription());
    }

    @Test
    void testProcessWithNullMethodType() throws JsonProcessingException {
        // Arrange
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test")
                .methodType(null) // null method type
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, null);

        // Assert - должен использоваться GET по умолчанию
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
        assertNull(pathItem.getPost());
    }

    @Test
    void testProcessWithDifferentHttpMethods() throws JsonProcessingException {
        // Тестируем ра��личные HTTP методы
        NodeDTO.MethodTypeEnum[] methods = {
                NodeDTO.MethodTypeEnum.POST,
                NodeDTO.MethodTypeEnum.PUT,
                NodeDTO.MethodTypeEnum.DELETE,
                NodeDTO.MethodTypeEnum.PATCH,
                NodeDTO.MethodTypeEnum.HEAD,
                NodeDTO.MethodTypeEnum.OPTIONS,
                NodeDTO.MethodTypeEnum.TRACE
        };

        for (NodeDTO.MethodTypeEnum method : methods) {
            // Arrange
            String path = "/test-" + method.name().toLowerCase();
            NodeDTO testFromNode = NodeDTO.builder()
                    .id(UUID.randomUUID())
                    .name("Http" + method.name())
                    .type(NodeDTO.TypeEnum.HTTP)
                    .nodeUrl(path)
                    .methodType(method)
                    .build();

            // Act
            httpToServiceLinkProcessor.process(openApiMap, link, toNode, testFromNode, null);

            // Assert
            OpenAPI openAPI = openApiMap.get("ServiceA");
            PathItem pathItem = openAPI.getPaths().get(path);
            assertNotNull(pathItem, "PathItem should exist for method " + method);

            switch (method) {
                case POST:
                    assertNotNull(pathItem.getPost(), "POST operation should exist");
                    break;
                case PUT:
                    assertNotNull(pathItem.getPut(), "PUT operation should exist");
                    break;
                case DELETE:
                    assertNotNull(pathItem.getDelete(), "DELETE operation should exist");
                    break;
                case PATCH:
                    assertNotNull(pathItem.getPatch(), "PATCH operation should exist");
                    break;
                case HEAD:
                    assertNotNull(pathItem.getHead(), "HEAD operation should exist");
                    break;
                case OPTIONS:
                    assertNotNull(pathItem.getOptions(), "OPTIONS operation should exist");
                    break;
                case TRACE:
                    assertNotNull(pathItem.getTrace(), "TRACE operation should exist");
                    break;
            }
        }
    }

    @Test
    void testProcessWithEventNullTags() throws JsonProcessingException {
        // Arrange - событие без тегов
        EventDTO eventWithoutTags = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EventWithoutTags")
                .schema("{\"type\":\"object\"}")
                .tags(null) // null теги
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, eventWithoutTags);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        // Должны быть только теги из link и "HTTP"
        assertEquals(2, pathItem.getGet().getTags().size());
        assertTrue(pathItem.getGet().getTags().contains("tag1"));
        assertTrue(pathItem.getGet().getTags().contains("HTTP"));
    }

    @Test
    void testProcessWithLinkNullTags() throws JsonProcessingException {
        // Arrange - link без тегов
        LinkDTO linkWithoutTags = new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), null, event.getId(), null);

        // Act
        httpToServiceLinkProcessor.process(openApiMap, linkWithoutTags, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        // Должны быть теги из event и "HTTP"
        List<String> tags = pathItem.getGet().getTags();
        assertTrue(tags.contains("HTTP"));
        // Если у event есть теги, они должны быть включены
    }

    @Test
    void testProcessWithEmptyTags() throws JsonProcessingException {
        // Arrange - пустые теги
        LinkDTO linkWithEmptyTags = new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), null, event.getId(), new HashSet<>());
        EventDTO eventWithEmptyTags = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EventWithEmptyTags")
                .schema("{\"type\":\"object\"}")
                .tags(new HashSet<>())
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, linkWithEmptyTags, toNode, fromNode, eventWithEmptyTags);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        // Должен быть только тег "HTTP"
        assertEquals(1, pathItem.getGet().getTags().size());
        assertTrue(pathItem.getGet().getTags().contains("HTTP"));
    }

    @Test
    void testProcessWithDocumentationFileLinks() throws JsonProcessingException {
        // Arrange - добавляем документационные ссылки
        Set<String> docLinks = Set.of("http://docs.example.com/api", "http://wiki.example.com/guide");
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test")
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .documentationFileLinks(docLinks)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        // Проверяем, что документационные ссылки добавлены в PathItem
        Object pathExtension = pathItem.getExtensions().get("x-documentation-file-links");
        assertNotNull(pathExtension);
        assertTrue(pathExtension instanceof List);
        
        @SuppressWarnings("unchecked")
        List<String> pathDocLinks = (List<String>) pathExtension;
        assertEquals(2, pathDocLinks.size());
        assertTrue(pathDocLinks.containsAll(docLinks));

        // Проверяем, что документационные ссылки добавлены в Schema
        Schema<?> schema = openAPI.getComponents().getSchemas().get("EventA");
        assertNotNull(schema);
        Object schemaExtension = schema.getExtensions().get("x-documentation-file-links");
        assertNotNull(schemaExtension);
        assertTrue(schemaExtension instanceof List);
        
        @SuppressWarnings("unchecked")
        List<String> schemaDocLinks = (List<String>) schemaExtension;
        assertEquals(2, schemaDocLinks.size());
        assertTrue(schemaDocLinks.containsAll(docLinks));
    }

    @Test
    void testProcessVerifySchemaExtensions() throws JsonProcessingException {
        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        Schema<?> schema = openAPI.getComponents().getSchemas().get("EventA");
        assertNotNull(schema);

        // Проверяем x-http-name расширение
        assertEquals(fromNode.getName(), schema.getExtensions().get("x-http-name"));

        // Проверяем x-incoming расширение
        Object xIncoming = schema.getExtensions().get("x-incoming");
        assertNotNull(xIncoming);
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
        assertEquals(fromNode.getName(), topicsList.get(0));

        // Проверяем tags
        Object tags = incomingMap.get("tags");
        assertNotNull(tags);
        assertTrue(tags instanceof List);
        @SuppressWarnings("unchecked")
        List<String> tagsList = (List<String>) tags;
        assertTrue(tagsList.contains("HTTP"));
        assertTrue(tagsList.contains("tag1"));
    }

    @Test
    void testProcessWithNodeDescription() throws JsonProcessingException {
        // Arrange
        String description = "Test HTTP endpoint description";
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test")
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .nodeDescription(description)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        assertEquals(description, pathItem.getDescription());
    }

    @Test
    void testProcessVerifyOperationsStructure() throws JsonProcessingException {
        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);

        // Проверяем GET операцию
        assertNotNull(pathItem.getGet());
        assertNotNull(pathItem.getGet().getResponses());
        // Не должно быть стандартных ответов, так как fromNode.getHttpResponses() == null
        assertEquals(0, pathItem.getGet().getResponses().size());
        assertNull(pathItem.getGet().getParameters());
    }

    @Test
    void testProcessWithComplexTagsCombination() throws JsonProcessingException {
        // Arrange - комбинация тегов из разных источников
        Set<String> linkTags = Set.of("link-tag1", "link-tag2");
        Set<String> eventTags = Set.of("event-tag1", "event-tag2", "link-tag1"); // пересечение с link тегами
        
        LinkDTO complexLink = new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), null, event.getId(), linkTags);
        EventDTO complexEvent = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("ComplexEvent")
                .schema("{\"type\":\"object\"}")
                .tags(eventTags)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, complexLink, toNode, fromNode, complexEvent);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        List<String> resultTags = pathItem.getGet().getTags();
        // Должны быть все уникальные теги + "HTTP"
        assertTrue(resultTags.contains("HTTP"));
        assertTrue(resultTags.contains("link-tag1"));
        assertTrue(resultTags.contains("link-tag2"));
        assertTrue(resultTags.contains("event-tag1"));
        assertTrue(resultTags.contains("event-tag2"));
        
        // Проверяем, что дубликаты не добавляются (link-tag1 есть в обоих наборах)
        assertEquals(5, resultTags.size()); // HTTP + 4 уникальных тега
    }

    @Test
    void testProcessWithInvalidEventName() throws JsonProcessingException {
        // Arrange - событие с пустым именем
        EventDTO invalidEvent = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("") // пустое имя
                .schema("{\"type\":\"object\"}")
                .build();

        // Act & Assert - должно выбросить исключение
        assertThrows(IllegalArgumentException.class, () -> {
            httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, invalidEvent);
        });
    }

    @Test
    void testProcessWithNullEventName() throws JsonProcessingException {
        // Arrange - событие с null именем
        EventDTO invalidEvent = EventDTO.builder()
                .id(UUID.randomUUID())
                .name(null) // null имя
                .schema("{\"type\":\"object\"}")
                .build();

        // Act & Assert - должно выбросить исключение
        assertThrows(IllegalArgumentException.class, () -> {
            httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, invalidEvent);
        });
    }

    @Test
    void testProcessWithInvalidEventSchema() throws JsonProcessingException {
        // Arrange - событие с невалидной схемой
        EventDTO invalidEvent = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("InvalidEvent")
                .schema("invalid json") // невалидный JSON
                .build();

        // Act & Assert - должно выбросить JsonProcessingException
        assertThrows(JsonProcessingException.class, () -> {
            httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, invalidEvent);
        });
    }

    @Test
    void testProcessWithExistingSchema() throws JsonProcessingException {
        // Arrange - добавляем схему с тем же именем заранее
        OpenAPI openAPI = openApiMap.get("ServiceA");
        OpenApiHelper.ensureComponents(openAPI);
        
        Schema<?> existingSchema = new Schema<>();
        existingSchema.setType("string");
        existingSchema.setDescription("Existing schema");
        openAPI.getComponents().addSchemas("EventA", existingSchema);

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert - должна использоваться существующая схема
        Schema<?> resultSchema = openAPI.getComponents().getSchemas().get("EventA");
        assertNotNull(resultSchema);
        assertEquals("string", resultSchema.getType()); // сохраняется тип существующей схемы
        assertEquals("Existing schema", resultSchema.getDescription());
        
        // Но расширения должны быть добавлены
        assertNotNull(resultSchema.getExtensions().get("x-http-name"));
        assertNotNull(resultSchema.getExtensions().get("x-incoming"));
    }

    @Test
    void testProcessWithDefaultHttpMethod() throws JsonProcessingException {
        // Arrange - создаем неизвестный HTTP метод через reflection или используем default case
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test-default")
                .methodType(null) // null приведет к GET по умолчанию
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, null);

        // Assert - должен использоваться GET по умолчанию
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test-default");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
        assertNull(pathItem.getPost());
        assertNull(pathItem.getPut());
        assertNull(pathItem.getDelete());
    }

    @Test
    void testProcessMultipleCallsWithSameEvent() throws JsonProcessingException {
        // Arrange - вызываем процесс дважды с одним и тем же событием
        
        // Act - первый вызов
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);
        
        // Изменяем путь для второго вызова
        NodeDTO secondFromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpB")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test2")
                .methodType(NodeDTO.MethodTypeEnum.POST)
                .build();
        
        LinkDTO secondLink = new LinkDTO(UUID.randomUUID(), secondFromNode.getId(), toNode.getId(), null, event.getId(), new HashSet<>(List.of("tag2")));
        
        // Act - второй вызов с тем же событием
        httpToServiceLinkProcessor.process(openApiMap, secondLink, toNode, secondFromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        
        // Должны быть оба пути
        assertNotNull(openAPI.getPaths().get("/test"));
        assertNotNull(openAPI.getPaths().get("/test2"));
        
        // Схема должна быть одна, но с обновленными расширениями
        assertEquals(1, openAPI.getComponents().getSchemas().size());
        Schema<?> schema = openAPI.getComponents().getSchemas().get("EventA");
        assertNotNull(schema);
        
        // Последний вызов должен перезаписать x-http-name
        assertEquals("HttpB", schema.getExtensions().get("x-http-name"));
    }

    @Test
    void testProcessWithEmptyDocumentationLinks() throws JsonProcessingException {
        // Arrange - пустой набор документационных ссылок
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpA")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/test")
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .documentationFileLinks(new HashSet<>()) // пустой набор
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        // Расширения не должны быть добавлены для пустого набора
        assertNull(pathItem.getExtensions());
        
        Schema<?> schema = openAPI.getComponents().getSchemas().get("EventA");
        assertNotNull(schema);
        // Для схемы тоже не должно быть расширения
        assertNull(schema.getExtensions().get("x-documentation-file-links"));
    }

    @Test
    void testProcessVerifyHttpTagsAlwaysPresent() throws JsonProcessingException {
        // Arrange - полностью пустые теги
        LinkDTO emptyLink = new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), null, event.getId(), null);
        EventDTO emptyEvent = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EmptyEvent")
                .schema("{\"type\":\"object\"}")
                .tags(null)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, emptyLink, toNode, fromNode, emptyEvent);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/test");
        assertNotNull(pathItem);
        
        // Тег "HTTP" должен всегда присутствовать
        List<String> tags = pathItem.getGet().getTags();
        assertNotNull(tags);
        assertEquals(1, tags.size());
        assertEquals("HTTP", tags.get(0));
        
        // Проверяем в схеме тоже
        Schema<?> schema = openAPI.getComponents().getSchemas().get("EmptyEvent");
        @SuppressWarnings("unchecked")
        Map<String, Object> xIncoming = (Map<String, Object>) schema.getExtensions().get("x-incoming");
        @SuppressWarnings("unchecked")
        List<String> schemaTags = (List<String>) xIncoming.get("tags");
        assertNotNull(schemaTags);
        assertEquals(1, schemaTags.size());
        assertEquals("HTTP", schemaTags.get(0));
    }

    @Test
    void testProcessWithSpecialCharactersInPath() throws JsonProcessingException {
        // Arrange - п��ть со специальными символами
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpSpecial")
                .type(NodeDTO.TypeEnum.HTTP)
                .nodeUrl("/api/v1/users/{id}/orders/{orderId}")
                .methodType(NodeDTO.MethodTypeEnum.GET)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        PathItem pathItem = openAPI.getPaths().get("/api/v1/users/{id}/orders/{orderId}");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
    }

    @Test
    void testProcessVerifyComponentsInitialization() throws JsonProcessingException {
        // Arrange - OpenAPI без компонентов
        OpenAPI emptyOpenAPI = new OpenAPI();
        emptyOpenAPI.setInfo(new io.swagger.v3.oas.models.info.Info().title("EmptyService"));
        emptyOpenAPI.setPaths(new Paths());
        // Намеренно не устанавливаем components
        
        openApiMap.put("EmptyService", emptyOpenAPI);
        
        NodeDTO emptyToNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("EmptyService")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();

        // Act
        httpToServiceLinkProcessor.process(openApiMap, link, emptyToNode, fromNode, event);

        // Assert - компоненты должны быть инициализированы
        OpenAPI resultOpenAPI = openApiMap.get("EmptyService");
        assertNotNull(resultOpenAPI.getComponents());
        assertNotNull(resultOpenAPI.getComponents().getSchemas());
        assertNotNull(resultOpenAPI.getComponents().getSchemas().get("EventA"));
    }
}
