package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonToSchemaGenerationServiceTest {

    @Test
    void testGenerateSchemaFromJson() throws Exception {
        JsonToSchemaGenerationService service = new JsonToSchemaGenerationService();

        String jsonInput = "{ \"name\": \"John\", \"age\": 30 }";

        String expectedSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}";

        String generatedSchema = service.generateSchema(jsonInput);

        assertEquals(expectedSchema, generatedSchema);
    }

//    @Test
//    void testGenerateSchemaFromJson1() throws Exception {
//        JsonSchemaGenerationService service = new JsonSchemaGenerationService();
//
//        String jsonInput = "{\n" +
//                "  \"name\": \"consume_one_event_service\",\n" +
//                "  \"tags\": [\"event1\",\"event2\"],\n" +
//                "  \"nodes\": [\n" +
//                "    {\n" +
//                "      \"id\": \"a1b2c3d4-e5f6-7890-1234-567890abcdef\",\n" +
//                "      \"belongsToGraph\": [\"a1b2c3d4-e5f6-7890-1234-567890abcdef\"],\n" +
//                "      \"name\": \"consume_one_event_service\",\n" +
//                "      \"type\": \"SERVICE\",\n" +
//                "      \"brokerType\": null,\n" +
//                "      \"tags\": [\"event1\",\"event2\"]\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"id\": \"b2c3d4e5-f678-9012-3456-7890abcdef01\",\n" +
//                "      \"belongsToGraph\": [\"a1b2c3d4-e5f6-7890-1234-567890abcdef\"],\n" +
//                "      \"name\": \"topic1\",\n" +
//                "      \"type\": \"TOPIC\",\n" +
//                "      \"brokerType\": \"KAFKA\",\n" +
//                "      \"tags\": [\"event1\",\"event2\"]\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"links\": [\n" +
//                "    {\n" +
//                "      \"toId\": \"a1b2c3d4-e5f6-7890-1234-567890abcdef\",\n" +
//                "      \"fromId\": \"b2c3d4e5-f678-9012-3456-7890abcdef01\",\n" +
//                "      \"eventId\": \"c3d4e5f6-7890-1234-5678-90abcdef0123\",\n" +
//                "      \"group\": null,\n" +
//                "      \"tags\": [\"event1\"]\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"toId\": \"a1b2c3d4-e5f6-7890-1234-567890abcdef\",\n" +
//                "      \"fromId\": \"b2c3d4e5-f678-9012-3456-7890abcdef01\",\n" +
//                "      \"eventId\": \"b61208c8-09d4-4e4d-89c0-b1da4e8b7d17\",\n" +
//                "      \"group\": null,\n" +
//                "      \"tags\": [\"event2\"]\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"events\": [\n" +
//                "    {\n" +
//                "      \"id\": \"c3d4e5f6-7890-1234-5678-90abcdef0123\",\n" +
//                "      \"name\": \"Event1\",\n" +
//                "      \"schema\": \"{\\n        \\\"type\\\": \\\"object\\\",\\n        \\\"x-incoming\\\": {\\n          \\\"topics\\\": [\\n            \\\"topic1\\\"\\n          ]\\n        }\\n      }\",\n" +
//                "      \"tags\": [\"event1\"]\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"id\": \"b61208c8-09d4-4e4d-89c0-b1da4e8b7d17\",\n" +
//                "      \"name\": \"Event2\",\n" +
//                "      \"schema\": \" {\\n        \\\"type\\\": \\\"object\\\",\\n        \\\"x-incoming\\\": {\\n          \\\"topics\\\": [\\n            \\\"topic1\\\"\\n          ]\\n        }\\n      }\",\n" +
//                "      \"tags\": [\"event2\"]\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"errors\": []\n" +
//                "}";
//
//        String expectedSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}";
//
//        String generatedSchema = service.generateSchema(jsonInput);
//
//        assertEquals(expectedSchema, generatedSchema);
//    }
}