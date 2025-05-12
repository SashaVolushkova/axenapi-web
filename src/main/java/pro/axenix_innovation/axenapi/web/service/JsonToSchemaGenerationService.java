package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
@Service
@Getter
@NoArgsConstructor
public class JsonToSchemaGenerationService {


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonToSchemaGenerationService.class);

    private String schema;

    public String generateSchema(String jsonInput) throws IOException {
        logger.info("Начинаю генерацию схемы для входного JSON");

        JsonNode jsonNode = objectMapper.readTree(jsonInput);
        String schema = generateSchemaFromJson(jsonNode);

        logger.info("Генерация схемы завершена: {}", schema);
        return schema;
    }

    private String generateSchemaFromJson(JsonNode jsonNode) {
        ObjectNode schemaNode = objectMapper.createObjectNode();
        schemaNode.put("type", "object");
        ObjectNode propertiesNode = objectMapper.createObjectNode();

        jsonNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            ObjectNode fieldSchema = objectMapper.createObjectNode();
            String fieldType = determineType(fieldValue);
            fieldSchema.put("type", fieldType);

            if ("object".equals(fieldType)) {
                fieldSchema.set("properties", generateSchemaProperties(fieldValue));
            } else if ("array".equals(fieldType)) {
                JsonNode firstItem = fieldValue.isArray() && fieldValue.size() > 0 ? fieldValue.get(0) : null;
                if (firstItem != null && firstItem.isObject()) {
                    fieldSchema.set("items", generateSchemaProperties(firstItem));
                }
            }

            propertiesNode.set(fieldName, fieldSchema);
        });

        schemaNode.set("properties", propertiesNode);

        try {
            String schemaJson = objectMapper.writeValueAsString(schemaNode);
            logger.debug("Сгенерированная схема: {}", schemaJson);  // Логируем сгенерированную схему
            return schemaJson;
        } catch (IOException e) {
            logger.error("Ошибка при генерации схемы", e);
            return "{}";
        }
    }

    private ObjectNode generateSchemaProperties(JsonNode jsonNode) {
        ObjectNode propertiesNode = objectMapper.createObjectNode();
        jsonNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            String fieldType = determineType(fieldValue);
            ObjectNode fieldSchema = objectMapper.createObjectNode();
            fieldSchema.put("type", fieldType);

            if ("object".equals(fieldType)) {
                fieldSchema.set("properties", generateSchemaProperties(fieldValue));
            } else if ("array".equals(fieldType)) {
                JsonNode firstItem = fieldValue.isArray() && fieldValue.size() > 0 ? fieldValue.get(0) : null;
                if (firstItem != null && firstItem.isObject()) {
                    fieldSchema.set("items", generateSchemaProperties(firstItem));
                }
            }

            propertiesNode.set(fieldName, fieldSchema);
        });
        return propertiesNode;
    }

    private String determineType(JsonNode node) {
        if (node.isTextual()) {
            return "string";
        } else if (node.isInt() || node.isLong()) {
            return "integer";
        } else if (node.isDouble() || node.isFloat()) {
            return "number";
        } else if (node.isBoolean()) {
            return "boolean";
        } else if (node.isArray()) {
            return "array";
        } else if (node.isObject()) {
            return "object";
        } else {
            return "any";
        }
    }
}
