package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;

/**
 * Service for parsing OpenAPI specification strings into OpenAPI objects.
 */
public interface OpenAPIParserService {
    /**
     * Parses the given OpenAPI specification string into an OpenAPI object.
     *
     * @param spec the OpenAPI specification as a string
     * @return the parsed OpenAPI object
     * @throws OpenAPISpecParseException if parsing fails
     */
    OpenAPI parse(String spec) throws OpenAPISpecParseException;
}
