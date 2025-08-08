package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpToServiceLinkProcessorCustomResponsesIntegrationTest {

    private HttpToServiceLinkProcessor httpToServiceLinkProcessor;
    private Map<String, OpenAPI> openApiMap;
    private NodeDTO fromNode;
    private NodeDTO toNode;
    private EventDTO event;
    private LinkDTO link;

    @BeforeEach
    void setUp() {
        httpToServiceLinkProcessor = new HttpToServiceLinkProcessor();
        openApiMap = new HashMap<>();
        
        // Правильно инициализируем OpenAPI объект
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new io.swagger.v3.oas.models.info.Info().title("TestService"));
        openAPI.setPaths(new io.swagger.v3.oas.models.Paths());
        openApiMap.put("TestService", openAPI);

        // Настройка MessageHelper ��ля тестов
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

        final UUID httpNodeId = UUID.randomUUID();
        final UUID serviceNodeId = UUID.randomUUID();
        final UUID eventId = UUID.randomUUID();
        final UUID linkId = UUID.randomUUID();

        fromNode = new NodeDTO();
        fromNode.setId(httpNodeId);
        fromNode.setName("HTTP Endpoint");
        fromNode.setType(NodeDTO.TypeEnum.HTTP);
        fromNode.setNodeUrl("/test-path");
        fromNode.setMethodType(NodeDTO.MethodTypeEnum.GET);
        fromNode.setBelongsToGraph(Collections.singletonList(serviceNodeId));

        toNode = new NodeDTO();
        toNode.setId(serviceNodeId);
        toNode.setName("TestService");
        toNode.setType(NodeDTO.TypeEnum.SERVICE);
        toNode.setBelongsToGraph(Collections.singletonList(serviceNodeId));

        event = new EventDTO();
        event.setId(eventId);
        event.setName("TestEvent");
        event.setSchema("{\"type\":\"string\"}");

        link = new LinkDTO();
        link.setId(linkId);
        link.setFromId(httpNodeId);
        link.setToId(serviceNodeId);
        link.setEventId(eventId);
    }

    @Test
    void testOnlyDefinedResponsesAreAdded() throws JsonProcessingException {
        // GIVEN
        fromNode.setHttpResponses(Arrays.asList(
                new HttpResponseDTO().statusCode(HttpStatusCodeEnum._400).description("Bad Request"),
                new HttpResponseDTO().statusCode(HttpStatusCodeEnum._500).description("Internal Server Error")
        ));

        // WHEN
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // THEN
        Map<String, ApiResponse> responses = getResponses();
        assertEquals(2, responses.size());
        assertTrue(responses.containsKey("400"));
        assertTrue(responses.containsKey("500"));
        assertFalse(responses.containsKey("200"));
        assertFalse(responses.containsKey("404"));
        assertEquals("Bad Request", responses.get("400").getDescription());
        assertEquals("Internal Server Error", responses.get("500").getDescription());
    }

    @Test
    void testSuccessResponseWithoutContentGetsEventSchema() throws JsonProcessingException {
        // GIVEN
        fromNode.setHttpResponses(Collections.singletonList(
                new HttpResponseDTO().statusCode(HttpStatusCodeEnum._200).description("Success")
        ));

        // WHEN
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // THEN
        Map<String, ApiResponse> responses = getResponses();
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey("200"));
        ApiResponse successResponse = responses.get("200");
        assertEquals("Success", successResponse.getDescription());
        assertNotNull(successResponse.getContent());
        Schema<?> schema = successResponse.getContent().get("application/json").getSchema();
        assertNotNull(schema);
        assertEquals("#/components/schemas/TestEvent", schema.get$ref());
    }

    @Test
    void testResponseWithCustomContent() throws JsonProcessingException {
        // GIVEN
        HttpContentDTO contentDTO = new HttpContentDTO()
                .mediaType(HttpContentTypeEnum.APPLICATION_JSON)
                .schema("{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}");

        fromNode.setHttpResponses(Collections.singletonList(
                new HttpResponseDTO()
                        .statusCode(HttpStatusCodeEnum._400)
                        .description("Custom Bad Request")
                        .content(Collections.singletonList(contentDTO))
        ));

        // WHEN
        httpToServiceLinkProcessor.process(openApiMap, link, toNode, fromNode, event);

        // THEN
        Map<String, ApiResponse> responses = getResponses();
        assertEquals(1, responses.size());
        assertTrue(responses.containsKey("400"));
        ApiResponse badRequestResponse = responses.get("400");
        assertEquals("Custom Bad Request", badRequestResponse.getDescription());
        assertNotNull(badRequestResponse.getContent());
        Schema<?> schema = badRequestResponse.getContent().get("application/json").getSchema();
        assertNotNull(schema);
        assertEquals("object", schema.getType());
        assertNotNull(schema.getProperties().get("message"));
    }

    private Map<String, ApiResponse> getResponses() {
        OpenAPI openAPI = openApiMap.get("TestService");
        assertNotNull(openAPI);
        assertNotNull(openAPI.getPaths());
        PathItem pathItem = openAPI.getPaths().get("/test-path");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
        return pathItem.getGet().getResponses();
    }
}
