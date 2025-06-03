package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pro.axenix_innovation.axenapi.web.model.GenerateJsonSchemaPostRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostGenerateJsonShemaTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGenerateJsonSchemaPost_withValidJsonInput_shouldReturnSchema() throws Exception {
        String jsonInput = "{\"name\": \"John\", \"age\": 30 }";

        GenerateJsonSchemaPostRequest request = new GenerateJsonSchemaPostRequest();
        request.setJsonInput(jsonInput);

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/generateJsonSchema")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema").exists())
                .andExpect(jsonPath("$.schema").isString())
                .andExpect(jsonPath("$.schema").value(org.hamcrest.Matchers.containsString("name")))
                .andExpect(jsonPath("$.schema").value(org.hamcrest.Matchers.containsString("age")));
    }
}
