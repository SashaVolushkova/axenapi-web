package pro.axenix_innovation.axenapi.web.generate;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.util.openapi.generator.OpenApiGeneratorFacade;

import java.util.HashMap;
import java.util.Map;

@Service
public class SpecificationGenerator {

    @Autowired
    private SpecificationHandler specificationHandler;

    public Map<String, String> generate(EventGraphDTO eventGraph, String format) {
        if (eventGraph == null || eventGraph.getNodes() == null || eventGraph.getNodes().isEmpty()) {
            return Map.of("error", "EventGraph or its nodes cannot be null or empty");
        }

        try {
            EventGraphFacade facade = new EventGraphFacade(eventGraph);
            Map<String, OpenAPI> openAPIMap = OpenApiGeneratorFacade.getOpenAPISpecifications(facade);

            if (openAPIMap == null || openAPIMap.isEmpty()) {
                return Map.of("error", "Translator returned empty OpenAPI specification");
            }

            return specificationHandler.handle(openAPIMap, format);

        } catch (Exception e) {
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("error", "Exception during specification generation: " + e.getMessage());
            return errorResult;
        }
    }
}
