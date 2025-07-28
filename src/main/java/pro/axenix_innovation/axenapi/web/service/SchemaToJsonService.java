package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.mifmif.common.regex.Generex;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaToJsonService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Faker faker = new Faker();
    private static final Logger logger = LoggerFactory.getLogger(SchemaToJsonService.class);
    private static final Random random = new Random();

    private static final List<String> NODE_TYPES = Arrays.asList("SERVICE", "TOPIC", "HTTP");
    private static final List<String> BROKER_TYPES = Arrays.asList("KAFKA", "JMS", "RABBITMQ", null);
    private static final List<String> METHOD_TYPES = Arrays.asList("GET", "POST", "PUT", "DELETE");
    private static final List<String> EVENT_TYPES = Arrays.asList("INCOMING", "OUTGOING", "INTERNAL");

    public static String generateJsonFromSchema(JsonNode schemaNode) {
        if (schemaNode == null) {
            throw new IllegalArgumentException("Схема не может быть null");
        }

        if (!schemaNode.has("properties")) {
            logger.warn("Схема не содержит 'properties'. Генерация дефолтного объекта.");
            return generateGenericObject(schemaNode, null).toString();
        }

        JsonNode propertiesNode = schemaNode.get("properties");

        boolean isGraph = propertiesNode.has("nodes") && propertiesNode.has("links") && propertiesNode.has("events");

        if (isGraph) {

            return generateGraphJson(schemaNode, propertiesNode, null);
        } else {
            logger.info("Не графовая схема. Генерация дефолтного объекта.");
            return generateGenericObject(schemaNode, null).toString();
        }
    }

    private static String generateGraphJson(JsonNode schemaNode, JsonNode propertiesNode, String contentType) {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        ArrayNode nodesArray = objectMapper.createArrayNode();
        ArrayNode eventsArray = objectMapper.createArrayNode();
        ArrayNode linksArray = objectMapper.createArrayNode();

        List<String> nodeIds = new ArrayList<>();
        List<String> eventIds = new ArrayList<>();

        jsonNode.put("name", "randomGraph");

        Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode fieldSchema = entry.getValue();

            switch (fieldName) {
                case "errors":
                    jsonNode.set("errors", objectMapper.createArrayNode());
                    break;

                case "nodes": {
                    JsonNode itemsNode = fieldSchema != null ? fieldSchema.get("items") : null;
                    if (itemsNode == null || !itemsNode.has("properties")) {
                        logger.warn("Schema for 'nodes' is invalid or missing 'items'. Skipping node generation.");
                        jsonNode.set("nodes", nodesArray);
                        break;
                    }
                    for (int i = 0; i < 5; i++) {
                        JsonNode node = generateNode(itemsNode);
                        if (node != null && node.has("id")) {
                            nodesArray.add(node);
                            nodeIds.add(node.get("id").asText());
                        }
                    }
                    jsonNode.set("nodes", nodesArray);
                    break;
                }

                case "events": {
                    JsonNode itemsNode = fieldSchema != null ? fieldSchema.get("items") : null;
                    if (itemsNode == null || !itemsNode.has("properties")) {
                        logger.warn("Schema for 'events' is invalid or missing 'items'. Skipping event generation.");
                        jsonNode.set("events", eventsArray);
                        break;
                    }
                    for (int i = 0; i < 3; i++) {
                        JsonNode event = generateEvent(itemsNode);
                        if (event != null && event.has("id")) {
                            eventsArray.add(event);
                            eventIds.add(event.get("id").asText());
                        }
                    }
                    jsonNode.set("events", eventsArray);
                    break;
                }

                case "links": {
                    JsonNode itemsNode = fieldSchema != null ? fieldSchema.get("items") : null;
                    if (itemsNode == null || !itemsNode.has("properties")) {
                        logger.warn("Schema for 'links' is invalid or missing 'items'. Skipping link generation.");
                        jsonNode.set("links", linksArray);
                        break;
                    }
                    for (int i = 0; i < 4; i++) {
                        JsonNode link = generateLink(itemsNode, nodeIds, eventIds);
                        if (link != null) {
                            linksArray.add(link);
                        }
                    }
                    jsonNode.set("links", linksArray);
                    break;
                }

                default:
                    if (fieldSchema != null) {
                        JsonNode fieldValue = generateFieldValue(fieldSchema, fieldName, contentType); // теперь передаётся 3-й аргумент

                        if (fieldValue != null) {
                            jsonNode.set(fieldName, fieldValue);
                        }
                    } else {
                        logger.warn("Field schema for '{}' is null. Skipping.", fieldName);
                    }
                    break;
            }
        }

        String generatedJson = jsonNode.toString();
        logger.info("Generated Graph JSON: {}", generatedJson);
        return generatedJson;
    }


    private static JsonNode generateNode(JsonNode nodeSchema) {
        ObjectNode node = objectMapper.createObjectNode();
        if (node == null) {
            return null;
        }

        node.put("id", java.util.UUID.randomUUID().toString());
        node.put("name", getTextOrDefault(nodeSchema, "name", "node" + faker.number().randomDigit()));

        // Handle node type
        String nodeType = getRandomValue(NODE_TYPES);
        if (nodeSchema != null && nodeSchema.has("type")) {
            JsonNode typeNode = nodeSchema.get("type");
            if (typeNode != null && typeNode.isTextual()) {
                String schemaType = typeNode.asText();
                nodeType = "object".equalsIgnoreCase(schemaType) ? getRandomValue(NODE_TYPES) : schemaType;
            }
        }
        if (!NODE_TYPES.contains(nodeType)) {
            nodeType = getRandomValue(NODE_TYPES);
        }
        node.put("type", nodeType);

        if ("HTTP".equalsIgnoreCase(nodeType)) {
            String methodType = getRandomValue(METHOD_TYPES);
            node.put("methodType", methodType);
            node.put("brokerType", (String) null);

            node.put("nodeUrl", "http://example.com/" + faker.lorem().word());
            node.put("requestBody", faker.lorem().sentence());
            node.put("responseBody", faker.lorem().sentence());
        } else {
            String brokerType = getTextOrDefault(nodeSchema, "brokerType", getRandomValue(BROKER_TYPES));
            node.put("brokerType", brokerType);
            node.remove("methodType");

            node.remove("nodeUrl");
            node.remove("requestBody");
            node.remove("responseBody");
        }

        node.put("nodeDescription", faker.lorem().sentence());

        JsonNode tags = generateTags();
        if (tags != null) {
            node.set("tags", tags);
        }

        ArrayNode belongsToGraph = objectMapper.createArrayNode();
        belongsToGraph.add(java.util.UUID.randomUUID().toString());
        node.set("belongsToGraph", belongsToGraph);

        return node;
    }

    private static JsonNode generateLink(JsonNode linkSchema, List<String> nodeIds, List<String> eventIds) {
        if (nodeIds == null || nodeIds.isEmpty() || eventIds == null) {
            return null;
        }

        ObjectNode link = objectMapper.createObjectNode();
        if (link == null) {
            return null;
        }

        link.put("id", java.util.UUID.randomUUID().toString());

        String fromId = getRandomValue(nodeIds);
        link.put("fromId", fromId);

        String toId = getRandomValue(nodeIds);
        while (nodeIds.size() > 1 && toId.equals(fromId)) {
            toId = getRandomValue(nodeIds);
        }
        link.put("toId", toId);

        if (!eventIds.isEmpty()) {
            link.put("eventId", getRandomValue(eventIds));
        }

        link.put("group", getTextOrDefault(linkSchema, "group", "group" + faker.number().randomDigit()));

        JsonNode tags = generateTags();
        if (tags != null) {
            link.set("tags", tags);
        }

        return link;
    }

    private static JsonNode generateEvent(JsonNode eventSchema) {
        ObjectNode event = objectMapper.createObjectNode();
        if (event == null) {
            return null;
        }

        event.put("id", java.util.UUID.randomUUID().toString());
        event.put("name", "event" + faker.number().randomDigit());

        event.put("schema", getTextOrDefault(eventSchema, "schema",
                "{\"type\":\"object\",\"x-incoming\":{\"topics\":[\"topic1\"]}}"));

        JsonNode tags = generateTags();
        if (tags != null) {
            event.set("tags", tags);
        }

        String eventType = getTextOrDefault(eventSchema, "eventType", getRandomValue(EVENT_TYPES));
        event.put("eventType", eventType);

        event.put("eventDescription", getTextOrDefault(eventSchema, "eventDescription",
                faker.lorem().sentence()));

        return event;
    }

    private static String getTextOrDefault(JsonNode node, String fieldName, String defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        }
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && fieldNode.isTextual()) ? fieldNode.asText() : defaultValue;
    }

    private static <T> T getRandomValue(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return faker.options().option(values.toArray((T[]) new Object[0]));
    }

    private static ArrayNode generateTags() {
        try {
            ArrayNode tags = objectMapper.createArrayNode();
            tags.add(faker.lorem().word());
            tags.add(faker.lorem().word());
            return tags;
        } catch (Exception e) {
            logger.warn("Failed to generate tags", e);
            return null;
        }
    }

    public static JsonNode generateFieldValue(JsonNode schemaNode, String fieldName, String contentType) {
        if (schemaNode == null || schemaNode.isNull()) {
            return NullNode.getInstance();
        }

        if (schemaNode.has("oneOf") && schemaNode.get("oneOf").isArray()) {
            ArrayNode oneOfArray = (ArrayNode) schemaNode.get("oneOf");
            JsonNode chosenSchema = null;

            if (contentType != null) {
                for (JsonNode candidate : oneOfArray) {
                    if (candidate.has("title") && contentType.equalsIgnoreCase(candidate.get("title").asText())) {
                        chosenSchema = candidate;
                        break;
                    }
                    if (candidate.has("description") && contentType.equalsIgnoreCase(candidate.get("description").asText())) {
                        chosenSchema = candidate;
                        break;
                    }
                    if (candidate.has("$ref")) {
                        String ref = candidate.get("$ref").asText();
                        if (ref.toLowerCase().contains(contentType.toLowerCase())) {
                            chosenSchema = candidate;
                            break;
                        }
                    }
                }
            }

            if (chosenSchema == null) {
                int idx = random.nextInt(oneOfArray.size());
                chosenSchema = oneOfArray.get(idx);
            }

            return generateFieldValue(chosenSchema, fieldName, contentType);
        }

        if (schemaNode.has("anyOf") && schemaNode.get("anyOf").isArray()) {
            ArrayNode anyOfArray = (ArrayNode) schemaNode.get("anyOf");
            int idx = random.nextInt(anyOfArray.size());
            JsonNode chosenSchema = anyOfArray.get(idx);
            return generateFieldValue(chosenSchema, fieldName, contentType);
        }

        if (schemaNode.has("allOf") && schemaNode.get("allOf").isArray()) {
            ObjectNode mergedNode = JsonNodeFactory.instance.objectNode();
            for (JsonNode part : schemaNode.get("allOf")) {
                JsonNode partial = generateFieldValue(part, fieldName, contentType);
                if (partial.isObject()) {
                    mergedNode.setAll((ObjectNode) partial);
                }
            }
            return mergedNode;
        }

        String type = resolveType(schemaNode);

        if (schemaNode.has("enum") && schemaNode.get("enum").isArray()) {
            ArrayNode enumArray = (ArrayNode) schemaNode.get("enum");
            if (enumArray.size() > 0) {
                int idx = random.nextInt(enumArray.size());
                return enumArray.get(idx);
            }
        }

        if ("string".equals(type) && schemaNode.has("pattern")) {
            String pattern = schemaNode.get("pattern").asText();
            if (pattern.startsWith("^")) pattern = pattern.substring(1);
            if (pattern.endsWith("$")) pattern = pattern.substring(0, pattern.length() - 1);
            try {
                Generex generex = new Generex(pattern);
                return TextNode.valueOf(generex.random());
            } catch (Exception e) {
                return TextNode.valueOf("example_" + fieldName);
            }
        }

        if ("integer".equals(type)) {
            int min = schemaNode.has("minimum") ? schemaNode.get("minimum").asInt() : 0;
            int max = schemaNode.has("maximum") ? schemaNode.get("maximum").asInt() : min + 100;
            int value = min + random.nextInt(max - min + 1);
            return IntNode.valueOf(value);
        }

        if ("number".equals(type)) {
            double min = schemaNode.has("minimum") ? schemaNode.get("minimum").asDouble() : 0.0;
            double max = schemaNode.has("maximum") ? schemaNode.get("maximum").asDouble() : min + 100.0;
            double value = min + (max - min) * random.nextDouble();
            return DoubleNode.valueOf(value);
        }

        if ("boolean".equals(type)) {
            return BooleanNode.valueOf(random.nextBoolean());
        }

        if ("string".equals(type)) {
            if (schemaNode.has("format")) {
                String format = schemaNode.get("format").asText();
                switch (format) {
                    case "email":
                        return TextNode.valueOf("example@example.com");
                    case "date-time":
                        return TextNode.valueOf("2025-01-01T12:00:00Z");
                    case "uri":
                        return TextNode.valueOf("https://example.com/resource");
                    default:
                        return TextNode.valueOf("example_" + fieldName);
                }
            }
            return TextNode.valueOf("example_" + fieldName);
        }

        if ("object".equals(type)) {
            return generateGenericObject(schemaNode, contentType);
        }

        if ("array".equals(type) && schemaNode.has("items")) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            int count = 1 + random.nextInt(3);
            JsonNode itemSchema = schemaNode.get("items");
            for (int i = 0; i < count; i++) {
                arrayNode.add(generateFieldValue(itemSchema, fieldName + "_" + i, contentType));
            }
            return arrayNode;
        }

        return NullNode.getInstance();
    }

    private static String resolveType(JsonNode schemaNode) {
        JsonNode typeNode = schemaNode.get("type");

        if (typeNode == null) {
            return "object";
        }

        if (typeNode.isTextual()) {
            return typeNode.asText();
        }

        if (typeNode.isArray()) {
            for (JsonNode node : typeNode) {
                if (!"null".equals(node.asText())) {
                    return node.asText();
                }
            }
        }

        return "object";
    }

    private static ObjectNode generateGenericObject(JsonNode schemaNode, String contentType) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        if (schemaNode.has("properties")) {
            JsonNode props = schemaNode.get("properties");
            props.fieldNames().forEachRemaining(field -> {
                JsonNode propSchema = props.get(field);
                result.set(field, generateFieldValue(propSchema, field, contentType));
            });
        }
        return result;
    }
}
