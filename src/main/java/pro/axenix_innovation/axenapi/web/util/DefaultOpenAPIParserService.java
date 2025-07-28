package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;

/**
 * Default implementation of OpenAPIParserService using OpenAPIParser utility.
 */
public class DefaultOpenAPIParserService implements OpenAPIParserService {
    @Override
    public OpenAPI parse(String spec) throws OpenAPISpecParseException {
        return OpenAPIParser.parseSpecification(spec);
    }
}
