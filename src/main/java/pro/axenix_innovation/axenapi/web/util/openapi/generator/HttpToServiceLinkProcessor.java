package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.HttpParameterDTO;
import pro.axenix_innovation.axenapi.web.model.HttpParametersDTO;
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
        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new Paths());
        }

        String httpPath = fromNode.getNodeUrl();
        if (httpPath == null || httpPath.isBlank()) {
            log.warn(MessageHelper.getStaticMessage(WARN_HTTP_URL_SKIP_LINK, fromNode.getName()));
            return;
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.add.get.patch.path.service", httpPath, toNode.getName()));

        // Определяем HTTP метод из узла, по умолчанию GET
        NodeDTO.MethodTypeEnum methodType = fromNode.getMethodType();
        if (methodType == null) {
            methodType = NodeDTO.MethodTypeEnum.GET;
        }

        if (event == null) {
            // Случай без события - создаем простую операцию
            Operation operation = new Operation().responses(new ApiResponses()
                    .addApiResponse("200", new ApiResponse().description("OK")));

            PathItem pathItem = new PathItem();

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

        // Случай с событием - создаем операцию в соответствии с methodType
        Set<String> httpTags = new LinkedHashSet<>();
        httpTags.add("HTTP");
        if (link.getTags() != null) {
            httpTags.addAll(link.getTags());
        }
        if (event.getTags() != null) {
            httpTags.addAll(event.getTags());
        }

        List<String> finalTags = new ArrayList<>(httpTags);

        // Создаем операцию в соответствии с methodType
        Operation operation = new Operation();
        operation.setResponses(OpenApiHelper.createHttpResponses(event.getName(), fromNode));

        // Добавляем параметры из httpParameters
        List<Parameter> parameters = new ArrayList<>();
        HttpParametersDTO httpParameters = fromNode.getHttpParameters();
        if (httpParameters != null) {
            if (httpParameters.getPathParameters() != null) {
                for (HttpParameterDTO paramDto : httpParameters.getPathParameters()) {
                    PathParameter p = new PathParameter();
                    p.setName(paramDto.getName());
                    p.setDescription(paramDto.getDescription());
                    p.setRequired(paramDto.getRequired());
                    if (paramDto.getType() != null) {
                        p.setSchema(new Schema().type(paramDto.getType().getValue()));
                    }
                    p.setExample(paramDto.getExample());
                    parameters.add(p);
                }
            }
            if (httpParameters.getQueryParameters() != null) {
                for (HttpParameterDTO paramDto : httpParameters.getQueryParameters()) {
                    QueryParameter p = new QueryParameter();
                    p.setName(paramDto.getName());
                    p.setDescription(paramDto.getDescription());
                    p.setRequired(paramDto.getRequired());
                    if (paramDto.getType() != null) {
                        p.setSchema(new Schema().type(paramDto.getType().getValue()));
                    }
                    p.setExample(paramDto.getExample());
                    parameters.add(p);
                }
            }
            if (httpParameters.getHeaderParameters() != null) {
                for (HttpParameterDTO paramDto : httpParameters.getHeaderParameters()) {
                    HeaderParameter p = new HeaderParameter();
                    p.setName(paramDto.getName());
                    p.setDescription(paramDto.getDescription());
                    p.setRequired(paramDto.getRequired());
                    if (paramDto.getType() != null) {
                        p.setSchema(new Schema().type(paramDto.getType().getValue()));
                    }
                    p.setExample(paramDto.getExample());
                    parameters.add(p);
                }
            }
        }

        if (!parameters.isEmpty()) {
            operation.parameters(parameters);
        }

        if (!finalTags.isEmpty()) {
            operation.tags(finalTags);
        }

        // Устанавливаем операцию в соответствии с HTTP методом
        PathItem pathItem = openAPI.getPaths().computeIfAbsent(httpPath, k -> new PathItem());
        openAPI.getPaths().addPathItem(httpPath, pathItem);

        switch (methodType) {
            case GET:
                operation.summary("Retrieve a specific event by ID");
                pathItem.setGet(operation);
                break;
            case POST:
                operation.summary("Create a new event");
                pathItem.setPost(operation);
                break;
            case PUT:
                operation.summary("Update an event");
                pathItem.setPut(operation);
                break;
            case DELETE:
                operation.summary("Delete an event");
                pathItem.setDelete(operation);
                break;
            case PATCH:
                operation.summary("Partially update an event");
                pathItem.setPatch(operation);
                break;
            case HEAD:
                operation.summary("Get event headers");
                pathItem.setHead(operation);
                break;
            case OPTIONS:
                operation.summary("Get allowed methods for event");
                pathItem.setOptions(operation);
                break;
            case TRACE:
                operation.summary("Trace event request");
                pathItem.setTrace(operation);
                break;
            default:
                operation.summary("Retrieve a specific event by ID");
                pathItem.setGet(operation);
                break;
        }

        pathItem.description(fromNode.getNodeDescription());

        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            pathItem.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }

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