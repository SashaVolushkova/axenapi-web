package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import java.util.UUID;

/**
 * Facade for converting OpenAPI specification string to EventGraphFacade using SOLID services.
 */
public class OpenAPIToEventGraphFacade {
    private final OpenAPIParserService parserService;
    private final EventGraphBuilder eventGraphBuilder;

    public OpenAPIToEventGraphFacade(OpenAPIParserService parserService, EventGraphBuilder eventGraphBuilder) {
        this.parserService = parserService;
        this.eventGraphBuilder = eventGraphBuilder;
    }

    public EventGraphFacade fromSpec(String spec, UUID serviceNodeId) throws OpenAPISpecParseException {
        OpenAPI openAPI = parserService.parse(spec);
        return eventGraphBuilder.build(openAPI, serviceNodeId);
    }

    public EventGraphFacade fromSpec(String spec) throws OpenAPISpecParseException {
        return fromSpec(spec, null);
    }
}
