package pro.axenix_innovation.axenapi.web.generate;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.Map;

public interface SpecificationHandler {

    Map<String, String> handle(Map<String, OpenAPI> openAPIMap, String format);

}
