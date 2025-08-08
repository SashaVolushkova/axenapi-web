package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import pro.axenix_innovation.axenapi.web.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiHelperTest {

    @Test
    void testCreateHttpResponsesWithDefinedResponses() throws JsonProcessingException {
        // GIVEN - узел с определенными HTTP ответами
        NodeDTO node = new NodeDTO();
        
        HttpResponseDTO response400 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._400)
                .description("Bad Request");
        
        HttpResponseDTO response500 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._500)
                .description("Internal Server Error");
        
        node.setHttpResponses(Arrays.asList(response400, response500));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - должны быть только определенные ответы
        assertEquals(2, responses.size());
        assertTrue(responses.containsKey("400"));
        assertTrue(responses.containsKey("500"));
        assertFalse(responses.containsKey("200"));
        assertFalse(responses.containsKey("404"));
        
        assertEquals("Bad Request", responses.get("400").getDescription());
        assertEquals("Internal Server Error", responses.get("500").getDescription());
    }

    @Test
    void testCreateHttpResponsesWithSuccessResponseWithoutContent() throws JsonProcessingException {
        // GIVEN - узел с 200 ответом без контента
        NodeDTO node = new NodeDTO();
        
        HttpResponseDTO response200 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._200)
                .description("Success");
        
        node.setHttpResponses(Collections.singletonList(response200));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - 200 ответ должен получить схему события по умолчанию
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey("200"));
        
        ApiResponse successResponse = responses.get("200");
        assertEquals("Success", successResponse.getDescription());
        assertNotNull(successResponse.getContent());
        
        MediaType mediaType = successResponse.getContent().get("application/json");
        assertNotNull(mediaType);
        assertNotNull(mediaType.getSchema());
        assertEquals("#/components/schemas/TestEvent", mediaType.getSchema().get$ref());
    }

    @Test
    void testCreateHttpResponsesWithCustomContent() throws JsonProcessingException {
        // GIVEN - узел с кастомным контентом
        NodeDTO node = new NodeDTO();
        
        HttpContentDTO contentDTO = new HttpContentDTO()
                .mediaType(HttpContentTypeEnum.APPLICATION_JSON)
                .schema("{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}");
        
        HttpResponseDTO response400 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._400)
                .description("Custom Bad Request")
                .content(Collections.singletonList(contentDTO));
        
        node.setHttpResponses(Collections.singletonList(response400));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - должен использоваться кастомный контент
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey("400"));
        
        ApiResponse badRequestResponse = responses.get("400");
        assertEquals("Custom Bad Request", badRequestResponse.getDescription());
        assertNotNull(badRequestResponse.getContent());
        
        MediaType mediaType = badRequestResponse.getContent().get("application/json");
        assertNotNull(mediaType);
        assertNotNull(mediaType.getSchema());
        assertEquals("object", mediaType.getSchema().getType());
        
        Map<String, Schema> properties = mediaType.getSchema().getProperties();
        assertNotNull(properties);
        assertTrue(properties.containsKey("message"));
        assertEquals("string", properties.get("message").getType());
    }

    @Test
    void testCreateHttpResponsesWithNullHttpResponses() throws JsonProcessingException {
        // GIVEN - узел без HTTP ответов
        NodeDTO node = new NodeDTO();
        node.setHttpResponses(null);
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - должен вернуться пустой объект ответов
        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    void testCreateHttpResponsesWithEmptyHttpResponses() throws JsonProcessingException {
        // GIVEN - узел с пустым списком HTTP ответов
        NodeDTO node = new NodeDTO();
        node.setHttpResponses(Collections.emptyList());
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - должен вернуться пустой объект ответов
        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    void testCreateHttpResponsesWithMultipleContentTypes() throws JsonProcessingException {
        // GIVEN - узел с несколькими типами контента
        NodeDTO node = new NodeDTO();
        
        HttpContentDTO jsonContent = new HttpContentDTO()
                .mediaType(HttpContentTypeEnum.APPLICATION_JSON)
                .schema("{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}");
        
        HttpContentDTO textContent = new HttpContentDTO()
                .mediaType(HttpContentTypeEnum.TEXT_PLAIN)
                .schema("{\"type\":\"string\"}");
        
        HttpResponseDTO response400 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._400)
                .description("Bad Request with multiple content types")
                .content(Arrays.asList(jsonContent, textContent));
        
        node.setHttpResponses(Collections.singletonList(response400));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - должны быть оба типа контента
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey("400"));
        
        ApiResponse badRequestResponse = responses.get("400");
        assertNotNull(badRequestResponse.getContent());
        assertEquals(2, badRequestResponse.getContent().size());
        
        // Проверяем JSON контент
        MediaType jsonMediaType = badRequestResponse.getContent().get("application/json");
        assertNotNull(jsonMediaType);
        assertEquals("object", jsonMediaType.getSchema().getType());
        
        // Проверяем текстовый контент
        MediaType textMediaType = badRequestResponse.getContent().get("text/plain");
        assertNotNull(textMediaType);
        assertEquals("string", textMediaType.getSchema().getType());
    }

    @Test
    void testCreateHttpResponsesWithNon200SuccessCode() throws JsonProcessingException {
        // GIVEN - узел с 201 ответом без контента
        NodeDTO node = new NodeDTO();
        
        HttpResponseDTO response201 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._201)
                .description("Created");
        
        node.setHttpResponses(Collections.singletonList(response201));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - 201 ответ НЕ должен получить схему события по умолчанию (только 200)
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey("201"));
        
        ApiResponse createdResponse = responses.get("201");
        assertEquals("Created", createdResponse.getDescription());
        assertNull(createdResponse.getContent()); // Контент не должен быть добавлен автоматически
    }

    @Test
    void testCreateHttpResponsesWithMixedResponses() throws JsonProcessingException {
        // GIVEN - узел со смешанными ответами: 200 без контента, 400 с контентом, 500 без контента
        NodeDTO node = new NodeDTO();
        
        HttpResponseDTO response200 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._200)
                .description("Success");
        
        HttpContentDTO errorContent = new HttpContentDTO()
                .mediaType(HttpContentTypeEnum.APPLICATION_JSON)
                .schema("{\"type\":\"object\",\"properties\":{\"error\":{\"type\":\"string\"}}}");
        
        HttpResponseDTO response400 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._400)
                .description("Bad Request")
                .content(Collections.singletonList(errorContent));
        
        HttpResponseDTO response500 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._500)
                .description("Internal Server Error");
        
        node.setHttpResponses(Arrays.asList(response200, response400, response500));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN
        assertEquals(3, responses.size());
        
        // 200 ответ должен получить схему события
        ApiResponse successResponse = responses.get("200");
        assertNotNull(successResponse.getContent());
        assertEquals("#/components/schemas/TestEvent", 
                successResponse.getContent().get("application/json").getSchema().get$ref());
        
        // 400 ответ должен использовать кастомную схему
        ApiResponse badRequestResponse = responses.get("400");
        assertNotNull(badRequestResponse.getContent());
        assertEquals("object", 
                badRequestResponse.getContent().get("application/json").getSchema().getType());
        
        // 500 ответ не должен иметь контента
        ApiResponse serverErrorResponse = responses.get("500");
        assertNull(serverErrorResponse.getContent());
    }

    @Test
    void testCreateHttpResponsesWithEmptyContent() throws JsonProcessingException {
        // GIVEN - узел с ответом, у которого пустой список контента
        NodeDTO node = new NodeDTO();
        
        HttpResponseDTO response400 = new HttpResponseDTO()
                .statusCode(HttpStatusCodeEnum._400)
                .description("Bad Request")
                .content(Collections.emptyList()); // Пустой список контента
        
        node.setHttpResponses(Collections.singletonList(response400));
        
        // WHEN
        ApiResponses responses = OpenApiHelper.createHttpResponses("TestEvent", node);
        
        // THEN - ответ не должен иметь контента
        assertEquals(1, responses.size());
        ApiResponse badRequestResponse = responses.get("400");
        assertNull(badRequestResponse.getContent());
    }
}