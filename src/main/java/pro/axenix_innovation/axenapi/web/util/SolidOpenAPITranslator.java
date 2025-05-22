package pro.axenix_innovation.axenapi.web.util;

import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import java.util.UUID;

/**
 * Static compatibility wrapper for SOLID-based OpenAPI translation.
 * Use in place of legacy OpenAPITranslator for tests and legacy code.
 */
public class SolidOpenAPITranslator {
    private static final OpenAPIToEventGraphFacade FACADE =
            new OpenAPIToEventGraphFacade(
                    new DefaultOpenAPIParserService(),
                    new DefaultEventGraphBuilder()
            );

    public static EventGraphFacade parseOPenAPI(String specification) throws OpenAPISpecParseException {
        return FACADE.fromSpec(specification);
    }

    public static EventGraphFacade parseOPenAPI(String specification, UUID serviceNodeId) throws OpenAPISpecParseException {
        return FACADE.fromSpec(specification, serviceNodeId);
    }
}
