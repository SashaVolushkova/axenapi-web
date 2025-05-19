package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import java.util.UUID;

/**
 * Service for building EventGraphFacade from OpenAPI specification.
 */
public interface EventGraphBuilder {
    /**
     * Builds an EventGraphFacade from the given OpenAPI object and optional service node ID.
     *
     * @param openAPI the OpenAPI object
     * @param serviceNodeId the UUID of the service node to associate, or null
     * @return the built EventGraphFacade
     */
    EventGraphFacade build(OpenAPI openAPI, UUID serviceNodeId) throws OpenAPISpecParseException;
}
