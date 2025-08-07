package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.util.SchemaProcessor;

import java.util.HashMap;
import java.util.Map;

public class OpenApiHelper {

    public static ApiResponses createSimpleResponses() {
        ApiResponses apiResponses = new ApiResponses();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("Event sent successfully");
        apiResponses.addApiResponse("200", apiResponse);
        return apiResponses;
    }

    public static void ensureComponents(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
    }

    public static Schema<?> getOrCreateSchema(OpenAPI openAPI, EventDTO event) throws JsonProcessingException {
        String eventName = event.getName();
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name is missing");
        }

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Schema<?> schema = schemas.get(eventName);
        if (schema == null) {
            schema = SchemaProcessor.deserializeSchema(event.getSchema());
            schema.setExtensions(new HashMap<>());
            schemas.put(eventName, schema);
        }
        return schema;
    }

    public static ApiResponses createHttpResponses(String refName) {
        if (refName == null || refName.isBlank()) {
            throw new IllegalArgumentException("Reference name for schema is missing");
        }

        ApiResponses responses = new ApiResponses();

        ApiResponse successResponse = new ApiResponse().description("Event retrieved successfully")
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + refName))));

        ApiResponse notFoundResponse = new ApiResponse().description("Event not found");

        responses.addApiResponse("200", successResponse);
        responses.addApiResponse("404", notFoundResponse);
        return responses;
    }

    public static Parameter createEventIdPathParameter() {
        return new Parameter()
                .name("eventId")
                .in("path")
                .description("ID of the event to retrieve")
                .required(true)
                .schema(new StringSchema());
    }
}
