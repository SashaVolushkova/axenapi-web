package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessage;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pro.axenix_innovation.axenapi.web.validate.EventGraphDTOValidator.validateEventGraph;

class SchemaToJsonServiceTest {

    private SchemaToJsonService schemaToJsonService;
    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(SchemaToJsonServiceTest.class);

    @BeforeEach
    void setUp() {
        schemaToJsonService = new SchemaToJsonService();
        objectMapper = new ObjectMapper();
    }
    @Test
    void generateJsonFromMinimalValidSchema_shouldProduceValidGraph() throws Exception {
        JsonNode schemaNode = objectMapper.readTree(getBasicSchemaJson());

        String generatedJson = schemaToJsonService.generateJsonFromSchema(schemaNode);

        assertNotNull(generatedJson, "Generated JSON must not be null");
        assertFalse(generatedJson.isEmpty(), "Generated JSON must not be empty");
        assertTrue(generatedJson.startsWith("{") && generatedJson.endsWith("}"),
                "Generated JSON must be a valid JSON object (starts with '{' and ends with '}')");

        EventGraphDTO eventGraph = objectMapper.readValue(generatedJson, EventGraphDTO.class);
        assertNotNull(eventGraph, "Deserialized EventGraphDTO must not be null");

        validateGraphStructure(eventGraph);

        AppCodeMessage validationMessage = validateEventGraph(eventGraph);
        assertNull(validationMessage, "Validation message must be null for a valid EventGraphDTO");
    }

    private void validateGraphStructure(EventGraphDTO graph) {
        assertNotNull(graph.getName(), "EventGraphDTO.name must not be null");
        assertNotNull(graph.getNodes(), "EventGraphDTO.nodes must not be null");
        assertNotNull(graph.getEvents(), "EventGraphDTO.events must not be null");
        assertNotNull(graph.getLinks(), "EventGraphDTO.links must not be null");
        assertNotNull(graph.getErrors(), "EventGraphDTO.errors must not be null");

        assertFalse(graph.getNodes().isEmpty(), "EventGraphDTO.nodes must not be empty");
        assertFalse(graph.getEvents().isEmpty(), "EventGraphDTO.events must not be empty");
        assertFalse(graph.getLinks().isEmpty(), "EventGraphDTO.links must not be empty");

        graph.getNodes().forEach(this::validateNode);
        graph.getEvents().forEach(this::validateEvent);
        graph.getLinks().forEach(link -> {
            assertNotNull(link.getFromId(), "Link.fromId must not be null");
            assertNotNull(link.getToId(), "Link.toId must not be null");
            assertNotNull(link.getEventId(), "Link.eventId must not be null");
            assertNotNull(link.getGroup(), "Link.group must not be null");
            assertNotEquals(link.getFromId(), link.getToId(),
                    "Link must not connect node to itself (fromId == toId)");
        });

        assertTrue(graph.getErrors().isEmpty(), "EventGraphDTO.errors must be empty");
    }

    private void validateNode(NodeDTO node) {
        assertNotNull(node.getId(), "Node.id must not be null");
        assertNotNull(node.getName(), "Node.name must not be null");
        assertNotNull(node.getType(), "Node.type must not be null");

        List<NodeDTO.TypeEnum> validTypes = List.of(NodeDTO.TypeEnum.SERVICE, NodeDTO.TypeEnum.TOPIC, NodeDTO.TypeEnum.HTTP);
        assertTrue(validTypes.contains(node.getType()),
                "Node.type must be one of: SERVICE, TOPIC, HTTP. Got: " + node.getType());

        if (node.getType() == NodeDTO.TypeEnum.HTTP) {
            assertNotNull(node.getMethodType(), "HTTP node must have a methodType");
            assertTrue(List.of(NodeDTO.MethodTypeEnum.GET, NodeDTO.MethodTypeEnum.POST, NodeDTO.MethodTypeEnum.PUT,
                            NodeDTO.MethodTypeEnum.HEAD, NodeDTO.MethodTypeEnum.DELETE, NodeDTO.MethodTypeEnum.PATCH,
                            NodeDTO.MethodTypeEnum.OPTIONS, NodeDTO.MethodTypeEnum.CONNECT, NodeDTO.MethodTypeEnum.TRACE).contains(node.getMethodType()),
                    "HTTP methodType must be one of GET, POST, PUT, DELETE. Got: " + node.getMethodType());
            assertNull(node.getBrokerType(), "HTTP node must not have a brokerType");
        } else {
            if (node.getBrokerType() != null) {
                assertTrue(List.of(NodeDTO.BrokerTypeEnum.KAFKA, NodeDTO.BrokerTypeEnum.JMS, NodeDTO.BrokerTypeEnum.RABBITMQ)
                                .contains(node.getBrokerType()),
                        "BrokerType must be one of: KAFKA, JMS, RABBITMQ. Got: " + node.getBrokerType());
            }
        }
    }

    private void validateEvent(pro.axenix_innovation.axenapi.web.model.EventDTO event) {
        assertNotNull(event.getId(), "Event.id must not be null");
        assertNotNull(event.getName(), "Event.name must not be null");
        assertNotNull(event.getSchema(), "Event.schema must not be null");
        assertNotNull(event.getEventType(), "Event.eventType must not be null");

        List<String> validTypes = List.of("INCOMING", "OUTGOING", "INTERNAL");
        assertTrue(validTypes.contains(event.getEventType()),
                "Event.eventType must be one of: INCOMING, OUTGOING, INTERNAL. Got: " + event.getEventType());
    }

    private String getBasicSchemaJson() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "nodes": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "id": { "type": "string" },
                          "belongsToGraph": { "type": "array", "items": { "type": "string" } },
                          "name": { "type": "string" },
                          "type": { "type": "string" },
                          "brokerType": { "type": ["string", "null"] },
                          "methodType": { "type": ["string", "null"] }
                        },
                        "required": ["id", "belongsToGraph", "name", "type"]
                      }
                    },
                    "events": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "id": { "type": "string" },
                          "name": { "type": "string" },
                          "schema": { "type": "string" },
                          "eventType": { "type": "string" }
                        },
                        "required": ["id", "name", "schema"]
                      }
                    },
                    "links": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "toId": { "type": "string" },
                          "fromId": { "type": "string" },
                          "eventId": { "type": "string" },
                          "group": { "type": "string" }
                        },
                        "required": ["toId", "fromId", "eventId", "group"]
                      }
                    },
                    "errors": {
                      "type": "array",
                      "items": { "type": "object" }
                    },
                    "tags": { "type": "array", "items": { "type": "string" } }
                  },
                  "required": ["name", "nodes", "links", "events"]
                }
                """;
    }

    @Test
    void generateJsonFromNullSchema_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            schemaToJsonService.generateJsonFromSchema(null);
        });
    }


    @Test
    void generatedJsonShouldMatchExpectedSchemaStructure() throws Exception {
        JsonNode schemaNode = objectMapper.readTree(getBasicSchemaJson());
        String json = schemaToJsonService.generateJsonFromSchema(schemaNode);
        JsonNode jsonNode = objectMapper.readTree(json);

        assertTrue(jsonNode.has("name"));
        assertTrue(jsonNode.has("nodes"));
        assertTrue(jsonNode.has("events"));
        assertTrue(jsonNode.has("links"));
        assertTrue(jsonNode.has("errors"));
    }

    @Test
    public void testGenerateUserProfileFromSchema() throws Exception {
        InputStream schemaStream = getClass().getResourceAsStream("/shemas/user_profile.json");
        assertNotNull(schemaStream, "Не удалось найти файл схемы user_profile.json");

        JsonNode schemaNode = objectMapper.readTree(schemaStream);
        String jsonString = SchemaToJsonService.generateJsonFromSchema(schemaNode);
        JsonNode generated = objectMapper.readTree(jsonString);

        logger.info("Сгенерированный JSON:\n{}", jsonString);

        assertTrue(generated.has("id"), "Поле 'id' должно присутствовать");
        JsonNode idNode = generated.get("id");
        assertTrue(idNode.isTextual(), "Поле 'id' должно быть строкой");
        assertEquals(24, idNode.asText().length(), "Поле 'id' должно содержать 24 символа");
        assertTrue(idNode.asText().matches("^[a-f0-9]{24}$"), "Поле 'id' должно быть hex 24 символа");

        assertTrue(generated.has("name"), "Поле 'name' должно присутствовать");
        JsonNode nameNode = generated.get("name");
        assertTrue(nameNode.isObject(), "Поле 'name' должно быть объектом");
        assertTrue(nameNode.has("first") && !nameNode.get("first").asText().isEmpty(), "'name.first' обязательное непустое поле");
        assertTrue(nameNode.has("last") && !nameNode.get("last").asText().isEmpty(), "'name.last' обязательное непустое поле");

        assertTrue(generated.has("email"), "Поле 'email' должно присутствовать");
        assertTrue(generated.get("email").isTextual(), "Поле 'email' должно быть строкой");
        assertTrue(generated.get("email").asText().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "Поле 'email' должно быть валидным email");

        assertTrue(generated.has("roles"), "Поле 'roles' должно присутствовать");
        JsonNode rolesNode = generated.get("roles");
        assertTrue(rolesNode.isArray() && rolesNode.size() > 0, "'roles' должен быть массивом с минимум одним элементом");

        assertTrue(generated.has("preferences"), "Поле 'preferences' должно присутствовать");
        JsonNode prefsNode = generated.get("preferences");
        assertTrue(prefsNode.has("newsletter") && prefsNode.get("newsletter").isBoolean(), "'preferences.newsletter' должно быть булевым");
        assertTrue(prefsNode.has("notifications"), "'preferences.notifications' должно присутствовать");

        Set<String> allowedRoot = Set.of("id", "name", "email", "age", "roles", "preferences", "address", "metadata");
        Set<String> rootFields = new HashSet<>();
        generated.fieldNames().forEachRemaining(rootFields::add);
        assertTrue(allowedRoot.containsAll(rootFields), "В корне обнаружены неизвестные поля");
    }

    @Test
    public void testGenerateCourseFromSchema() throws Exception {
        InputStream schemaStream = getClass().getResourceAsStream("/shemas/course.json");
        assertNotNull(schemaStream, "Не удалось найти файл схемы course.json");

        JsonNode schemaNode = objectMapper.readTree(schemaStream);
        String jsonString = SchemaToJsonService.generateJsonFromSchema(schemaNode);
        JsonNode generated = objectMapper.readTree(jsonString);

        logger.info("Сгенерированный JSON курса:\n{}", jsonString);

        assertTrue(generated.has("courseId"), "Поле 'courseId' должно присутствовать");
        String courseId = generated.get("courseId").asText();
        assertEquals(8, courseId.length(), "courseId должен содержать 8 символов");
        assertTrue(courseId.matches("^[A-Z0-9]{8}$"), "courseId должен содержать только заглавные буквы и цифры");

        assertTrue(generated.has("title"), "Поле 'title' должно присутствовать");
        String title = generated.get("title").asText();
        assertTrue(title.length() >= 5 && title.length() <= 100, "title должен быть от 5 до 100 символов");

        assertTrue(generated.has("authors"), "Поле 'authors' должно присутствовать");
        JsonNode authors = generated.get("authors");
        assertTrue(authors.isArray() && authors.size() > 0, "authors должен быть массивом с минимум одним элементом");

        JsonNode firstAuthor = authors.get(0);
        assertTrue(firstAuthor.has("name"), "Автор должен иметь поле 'name'");
        assertTrue(firstAuthor.get("name").asText().length() >= 3, "Имя автора должно быть минимум 3 символа");

        assertTrue(firstAuthor.has("role"), "Автор должен иметь поле 'role'");
        String role = firstAuthor.get("role").asText();
        assertTrue(Set.of("instructor", "assistant", "guest").contains(role), "role должен быть одним из 'instructor', 'assistant', 'guest'");

        assertTrue(firstAuthor.has("contact"), "Автор должен иметь поле 'contact'");
        JsonNode contact = firstAuthor.get("contact");
        assertTrue(contact.has("email"), "Контакт должен иметь поле 'email'");
        String email = contact.get("email").asText();
        assertTrue(email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "email должен быть валидным");

        if (contact.has("phone") && !contact.get("phone").isNull()) {
            String phone = contact.get("phone").asText();
            assertTrue(phone.matches("^\\+?\\d{10,15}$"), "phone должен соответствовать паттерну или быть null");
        }

        assertTrue(generated.has("lessons"), "Поле 'lessons' должно присутствовать");
        JsonNode lessons = generated.get("lessons");
        assertTrue(lessons.isArray() && lessons.size() > 0, "lessons должен быть массивом с минимум одним элементом");

        JsonNode firstLesson = lessons.get(0);
        assertTrue(firstLesson.has("lessonId"), "Урок должен иметь поле 'lessonId'");
        String lessonId = firstLesson.get("lessonId").asText();
        assertTrue(lessonId.matches("^L[0-9]{3}$"), "lessonId должен соответствовать паттерну Lxxx");

        assertTrue(firstLesson.has("title"), "Урок должен иметь поле 'title'");
        assertTrue(firstLesson.get("title").asText().length() >= 5, "title урока должен быть минимум 5 символов");

        assertTrue(firstLesson.has("contentType"), "Урок должен иметь поле 'contentType'");
        String contentType = firstLesson.get("contentType").asText();
        assertTrue(Set.of("video", "quiz", "article").contains(contentType), "contentType должен быть 'video', 'quiz' или 'article'");

        assertTrue(firstLesson.has("content"), "Урок должен иметь поле 'content'");

        Set<String> allowedRoot = Set.of("courseId", "title", "description", "authors", "lessons", "settings");
        Set<String> rootFields = new HashSet<>();
        generated.fieldNames().forEachRemaining(rootFields::add);
        assertTrue(allowedRoot.containsAll(rootFields), "В корне обнаружены неизвестные поля");
    }

    @Test
    public void testGenerateBookFromSchema() throws Exception {
        InputStream schemaStream = getClass().getResourceAsStream("/shemas/book.json");
        assertNotNull(schemaStream, "Не удалось найти файл схемы book.json");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode schemaNode = objectMapper.readTree(schemaStream);
        String jsonString = SchemaToJsonService.generateJsonFromSchema(schemaNode);
        JsonNode generated = objectMapper.readTree(jsonString);

        logger.info("Сгенерированный JSON книги:\n{}", jsonString);

        assertTrue(generated.has("title"), "Поле 'title' должно присутствовать");
        String title = generated.get("title").asText();
        assertTrue(title.length() >= 1, "title должен быть минимум 1 символ");

        assertTrue(generated.has("isbn"), "Поле 'isbn' должно присутствовать");
        String isbn = generated.get("isbn").asText();
        assertTrue(isbn.matches("^[0-9-]+$"), "isbn должен содержать только цифры и дефисы");

        assertTrue(generated.has("authors"), "Поле 'authors' должно присутствовать");
        JsonNode authors = generated.get("authors");
        assertTrue(authors.isArray() && authors.size() > 0, "authors должен быть массивом с минимум одним элементом");

        JsonNode firstAuthor = authors.get(0);
        assertTrue(firstAuthor.has("name"), "Автор должен иметь поле 'name'");
        assertFalse(firstAuthor.get("name").asText().isEmpty(), "Имя автора не должно быть пустым");

        if (firstAuthor.has("birthYear") && !firstAuthor.get("birthYear").isNull()) {
            int birthYear = firstAuthor.get("birthYear").asInt();
            assertTrue(birthYear >= 1800 && birthYear <= 2025, "birthYear должен быть между 1800 и 2025");
        }

        if (generated.has("reviews")) {
            JsonNode reviews = generated.get("reviews");
            assertTrue(reviews.isArray(), "reviews должен быть массивом");

            if (reviews.size() > 0) {
                JsonNode firstReview = reviews.get(0);
                assertTrue(firstReview.has("reviewer"), "review должен иметь поле 'reviewer'");
                assertFalse(firstReview.get("reviewer").asText().isEmpty(), "reviewer не должен быть пустым");

                assertTrue(firstReview.has("rating"), "review должен иметь поле 'rating'");
                int rating = firstReview.get("rating").asInt();
                assertTrue(rating >= 1 && rating <= 5, "rating должен быть от 1 до 5");

                if (firstReview.has("comment") && !firstReview.get("comment").isNull()) {
                    String comment = firstReview.get("comment").asText();
                }
            }
        }
    }

    @Test
    public void testGenerateAlbumFromSchema() throws Exception {
        InputStream schemaStream = getClass().getResourceAsStream("/shemas/album.json");
        assertNotNull(schemaStream, "Не удалось найти файл схемы album.json");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode schemaNode = objectMapper.readTree(schemaStream);
        String jsonString = SchemaToJsonService.generateJsonFromSchema(schemaNode);
        JsonNode generated = objectMapper.readTree(jsonString);

        logger.info("Сгенерированный JSON альбома:\n{}", jsonString);

        assertTrue(generated.has("albumId"), "Поле 'albumId' должно присутствовать");
        assertTrue(generated.get("albumId").asText().matches("ALB-\\d{5}"), "albumId должен соответствовать шаблону ALB-00000");

        assertTrue(generated.has("title"), "Поле 'title' должно присутствовать");
        assertTrue(generated.get("title").asText().length() >= 3, "title должен быть минимум 3 символа");

        assertTrue(generated.has("releaseDate"), "Поле 'releaseDate' должно присутствовать");

        assertTrue(generated.has("artists"), "Поле 'artists' должно присутствовать");
        JsonNode artists = generated.get("artists");
        assertTrue(artists.isArray() && artists.size() > 0, "artists должен быть массивом с минимум одним элементом");
        JsonNode firstArtist = artists.get(0);
        assertTrue(firstArtist.has("name"), "У артиста должно быть поле 'name'");
        assertTrue(firstArtist.get("name").asText().length() >= 2, "Имя артиста должно быть не короче 2 символов");
        assertTrue(firstArtist.has("role"), "У артиста должно быть поле 'role'");
        assertTrue(firstArtist.get("role").isTextual(), "role должен быть строкой");

        assertTrue(generated.has("tracks"), "Поле 'tracks' должно присутствовать");
        JsonNode tracks = generated.get("tracks");
        assertTrue(tracks.isArray() && tracks.size() > 0, "tracks должен быть массивом с минимум одним элементом");
        JsonNode firstTrack = tracks.get(0);
        assertTrue(firstTrack.has("trackNumber"), "track должен иметь trackNumber");
        assertTrue(firstTrack.get("trackNumber").asInt() >= 1, "trackNumber должен быть >= 1");
        assertTrue(firstTrack.has("title"), "track должен иметь title");
        assertTrue(firstTrack.get("title").asText().length() >= 2, "title трека должен быть минимум 2 символа");
        assertTrue(firstTrack.has("duration"), "track должен иметь duration");
        assertTrue(firstTrack.get("duration").asDouble() >= 0.5, "duration должен быть >= 0.5");

        if (firstTrack.has("features")) {
            JsonNode features = firstTrack.get("features");
            assertTrue(features.isArray(), "features должен быть массивом строк");
        }

        if (generated.has("label")) {
            JsonNode label = generated.get("label");
            assertTrue(label.has("name"), "label должен содержать name");
            assertTrue(label.get("name").asText().length() > 0, "Имя label не должно быть пустым");
        }
    }

    @Test
    void testGenerateStringWithPattern() throws Exception {
        String schemaJson = """
        {
          "type": "string",
          "pattern": "abc\\\\d{2}"
        }
    """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        JsonNode result = SchemaToJsonService.generateFieldValue(schemaNode, "test", null);
        assertTrue(result.asText().matches("abc\\d{2}"));
    }


    @Test
    void testGenerateEnum() throws Exception {
        String schemaJson = """
            {
              "type": "string",
              "enum": ["RED", "GREEN", "BLUE"]
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        JsonNode result = SchemaToJsonService.generateFieldValue(schemaNode, "color", null);
        assertTrue(result.isTextual());
        assertTrue(result.asText().matches("RED|GREEN|BLUE"));
    }

    @Test
    void testGenerateObject() throws Exception {
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 18, "maximum": 60 }
              }
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        String result = SchemaToJsonService.generateJsonFromSchema(schemaNode);
        JsonNode parsed = objectMapper.readTree(result.toString());
        assertTrue(parsed.has("name"));
        assertTrue(parsed.has("age"));
        assertTrue(parsed.get("age").asInt() >= 18 && parsed.get("age").asInt() <= 60);
    }

    @Test
    void testGenerateWithAllOf() throws Exception {
        String schemaJson = """
            {
              "allOf": [
                {
                  "type": "object",
                  "properties": {
                    "a": { "type": "string" }
                  }
                },
                {
                  "type": "object",
                  "properties": {
                    "b": { "type": "number" }
                  }
                }
              ]
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        JsonNode result = SchemaToJsonService.generateFieldValue(schemaNode, "merged", null);
        assertTrue(result.has("a"));
        assertTrue(result.has("b"));
    }

    @Test
    void testGenerateArray() throws Exception {
        String schemaJson = """
            {
              "type": "array",
              "items": { "type": "integer", "minimum": 1, "maximum": 10 }
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        JsonNode result = SchemaToJsonService.generateFieldValue(schemaNode, "nums", null);
        assertTrue(result.isArray());
        for (JsonNode n : result) {
            assertTrue(n.isInt());
            assertTrue(n.asInt() >= 1 && n.asInt() <= 10);
        }
    }

    @Test
    void testGenerateOneOfWithTitle() throws Exception {
        String schemaJson = """
            {
              "oneOf": [
                {
                  "title": "text",
                  "type": "string"
                },
                {
                  "title": "number",
                  "type": "number"
                }
              ]
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        JsonNode result = SchemaToJsonService.generateFieldValue(schemaNode, "field", "text");
        assertTrue(result.isTextual());
    }

    @Test
    void testGenerateFallbackObjectWhenNoProperties() throws Exception {
        String schemaJson = """
            {
              "type": "object"
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        String result = SchemaToJsonService.generateJsonFromSchema(schemaNode);
        assertNotNull(result);
        assertEquals("{}", result);
    }

    @Test
    void testGenerateBoolean() throws Exception {
        String schemaJson = """
            {
              "type": "boolean"
            }
        """;
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        JsonNode result = SchemaToJsonService.generateFieldValue(schemaNode, "flag", null);
        assertTrue(result.isBoolean());
    }
}
