package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_HTTP_URL_SKIP_LINK;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_NO_OPEN_API_SPEC_FOUND_SKIP_HTTP;

@Slf4j
public class HttpToServiceLinkProcessor {
    public void process(Map<String, OpenAPI> openApiMap, LinkDTO link, NodeDTO toNode, NodeDTO fromNode, EventDTO event)
            throws JsonProcessingException {
        OpenAPI openAPI = openApiMap.get(toNode.getName());
        if (openAPI == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_OPEN_API_SPEC_FOUND_SKIP_HTTP, toNode.getName()));
            return;
        }
        OpenApiHelper.ensureComponents(openAPI);

        String httpPath = fromNode.getNodeUrl();
        if (httpPath == null || httpPath.isBlank()) {
            log.warn(MessageHelper.getStaticMessage(WARN_HTTP_URL_SKIP_LINK, fromNode.getName()));
            return;
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.add.get.patch.path.service", httpPath, toNode.getName()));

        if (event == null) {
            Operation operation = new Operation().responses(new ApiResponses()
                    .addApiResponse("200", new ApiResponse().description("OK")));

            PathItem pathItem = new PathItem();

            // Use the HTTP method from the node, default to GET if not specified
            NodeDTO.MethodTypeEnum methodType = fromNode.getMethodType();
            if (methodType == null) {
                methodType = NodeDTO.MethodTypeEnum.GET;
            }

            switch (methodType) {
                case GET:
                    pathItem.get(operation);
                    break;
                case POST:
                    pathItem.post(operation);
                    break;
                case PUT:
                    pathItem.put(operation);
                    break;
                case DELETE:
                    pathItem.delete(operation);
                    break;
                case PATCH:
                    pathItem.patch(operation);
                    break;
                case HEAD:
                    pathItem.head(operation);
                    break;
                case OPTIONS:
                    pathItem.options(operation);
                    break;
                case TRACE:
                    pathItem.trace(operation);
                    break;
                default:
                    pathItem.get(operation);
                    break;
            }

            openAPI.getPaths().addPathItem(httpPath, pathItem);
            return;
        }

        Set<String> httpTags = new LinkedHashSet<>();
        httpTags.add("HTTP");
        if (link.getTags() != null) {
            httpTags.addAll(link.getTags());
        }
        if (event.getTags() != null) {
            httpTags.addAll(event.getTags());
        }

        List<String> finalTags = new ArrayList<>(httpTags);

        Operation getOp = new Operation()
                .responses(OpenApiHelper.createHttpResponses(event.getName()))
                .parameters(List.of(OpenApiHelper.createEventIdPathParameter()));
        Operation patchOp = new Operation()
                .responses(OpenApiHelper.createHttpResponses(event.getName()))
                .parameters(List.of(OpenApiHelper.createEventIdPathParameter()));

        if (!finalTags.isEmpty()) {
            getOp.tags(finalTags);
            patchOp.tags(finalTags);
        }

        PathItem pathItem = new PathItem()
                .summary("Retrieve a specific event by ID")
                .description(fromNode.getNodeDescription())
                .get(getOp)
                .patch(patchOp);

        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            pathItem.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }

        openAPI.getPaths().addPathItem(httpPath, pathItem);

        Schema<?> schema = OpenApiHelper.getOrCreateSchema(openAPI, event);

        schema.addExtension("x-http-name", fromNode.getName());

        Map<String, Object> xIncoming = new LinkedHashMap<>();
        xIncoming.put("topics", List.of(fromNode.getName()));
        if (!httpTags.isEmpty()) {
            xIncoming.put("tags", new ArrayList<>(httpTags));
        }
        schema.addExtension("x-incoming", xIncoming);

        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            schema.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }

        openAPI.getComponents().addSchemas(event.getName(), schema);
    }
}
