package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ServiceOpenApiGenerator {

    public Map<String, OpenAPI> createOpenAPIMap(EventGraphFacade eventGraph) {
        return eventGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .collect(
                        HashMap::new,
                        (map, node) -> {
                            OpenAPI openAPI = createOpenAPI(node);
                            if (node.getDocumentationFileLinks() != null && !node.getDocumentationFileLinks().isEmpty()) {
                                openAPI.addExtension("x-documentation-file-links", new ArrayList<>(node.getDocumentationFileLinks()));
                            }
                            map.put(node.getName(), openAPI);
                            log.info(MessageHelper.getStaticMessage("axenapi.info.created.open.api.spec.service.node",
                                    node.getName()));
                        },
                        Map::putAll
                );
    }

    private OpenAPI createOpenAPI(NodeDTO node) {
        OpenAPI openAPI = new OpenAPI();
        Info info = new Info();
        info.setVersion("1.0.0"); // TODO get version dynamically if needed
        info.setTitle(node.getName());
        info.setDescription("AxenAPI Specification for " + node.getName());
        openAPI.setInfo(info);
        openAPI.setPaths(new Paths());
        openAPI.setComponents(new Components());
        openAPI.getComponents().setSchemas(new HashMap<>());
        return openAPI;
    }
}
