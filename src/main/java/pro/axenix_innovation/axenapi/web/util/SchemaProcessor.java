package pro.axenix_innovation.axenapi.web.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.media.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaProcessor {
    
    public static Schema deserializeSchema(String text) throws JsonProcessingException {
        JsonNode node = Json.mapper().readTree(text);
        JsonNode additionalProperties = node.get("additionalProperties");
        Schema schema = null;
        
        if (additionalProperties != null) {
            if (additionalProperties.isBoolean()) {
                Boolean additionalPropsBoolean = Json.mapper().convertValue(additionalProperties, Boolean.class);
                ((ObjectNode)node).remove("additionalProperties");
                if (additionalPropsBoolean) {
                    schema = Json.mapper().convertValue(node, MapSchema.class);
                } else {
                    schema = Json.mapper().convertValue(node, ObjectSchema.class);
                }
                schema.setAdditionalProperties(additionalPropsBoolean);
            } else {
                Schema innerSchema = Json.mapper().convertValue(additionalProperties, Schema.class);
                ((ObjectNode)node).remove("additionalProperties");
                MapSchema ms = Json.mapper().convertValue(node, MapSchema.class);
                ms.setAdditionalProperties(innerSchema);
                schema = ms;
            }
        } else {
            schema = Json.mapper().convertValue(node, ObjectSchema.class);
        }

        if (schema != null) {
            schema.jsonSchema(Json31.jsonSchemaAsMap(node));
        }
        return schema;
    }

    @SuppressWarnings("unchecked")
    public static void addExtension(Schema schema, String key, String topic, List<String> tags) {
        Map<String, Object> map = (Map<String, Object>) schema.getExtensions().getOrDefault(key, new HashMap<>());
        List<String> topics = (List<String>) map.getOrDefault("topics", new ArrayList<>());
        if (!topics.contains(topic)) {
            topics.add(topic);
        }
        map.put("topics", topics);

        if (tags != null) {
            List<String> tagList = (List<String>) map.getOrDefault("tags", new ArrayList<>());
            for (String tag : tags) {
                if (!tagList.contains(tag)) {
                    tagList.add(tag);
                }
            }
            map.put("tags", tagList);
        }
        schema.getExtensions().put(key, map);
    }
} 