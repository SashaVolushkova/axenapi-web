package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import pro.axenix_innovation.axenapi.web.model.GenerateJsonSchemaPostRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostGenerateJsonExampleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    private final String rawSchemaJson = """
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

    @Test
    void shouldReturnValidGeneratedJson_whenSchemaIsValid() throws Exception {
        String wrappedRequestBody = objectMapper.writeValueAsString(
                Map.of("jsonSchema", rawSchemaJson)
        );

        mockMvc.perform(post("/generateJsonExample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrappedRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonExample").isNotEmpty());
    }


    @Test
    void shouldReturnBadRequest_whenSchemaIsNull() throws Exception {
        String nullSchema = """
            {
              "jsonSchema": null
            }
            """;

        mockMvc.perform(post("/generateJsonExample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullSchema))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("Error processing JSON schema: argument \"content\" is null")));
    }

    @Test
    void shouldReturnValidGeneratedJson_whenSchemaIsLoadedFromFile() throws Exception {
        String schemaFromFile = Files.readString(Path.of("src/test/resources/shemas/consume_one_event_service_shema.json"));

        String wrappedRequestBody = objectMapper.writeValueAsString(
                Map.of("jsonSchema", schemaFromFile)
        );

        mockMvc.perform(post("/generateJsonExample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrappedRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonExample").isNotEmpty());
    }

    @Test
    void shouldReturnValidGeneratedJson_whenSchemaIsLoadedFromFile1() throws Exception {
        String schemaFromFile = Files.readString(Path.of("src/test/resources/shemas/consume_one_event_service_with_http_shema.json"));

        String wrappedRequestBody = objectMapper.writeValueAsString(
                Map.of("jsonSchema", schemaFromFile)
        );

        mockMvc.perform(post("/generateJsonExample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrappedRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonExample").isNotEmpty());
    }

    @Test
    void shouldReturnValidSchema_whenJsonInputIsValid() throws Exception {
        String inputJson = "{\"name\":\"Kostya\", \"age\":20}";

        GenerateJsonSchemaPostRequest request = new GenerateJsonSchemaPostRequest();
        request.setJsonInput(inputJson);

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/generateJsonSchema")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema").isNotEmpty())
                .andExpect(jsonPath("$.schema").value(org.hamcrest.Matchers.containsString("\"type\":\"object\"")))
                .andExpect(jsonPath("$.schema", org.hamcrest.Matchers.containsString("\"name\":{\"type\":\"string\"}")))
                .andExpect(jsonPath("$.schema", org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.containsString("\"age\":{\"type\":\"integer\"}"),
                        org.hamcrest.Matchers.containsString("\"age\":{\"type\":\"number\"}")
                )));
    }
}
