package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_NO_OPEN_API_SPEC_FOUND_SKIP_INC;

@Slf4j
public class TopicToServiceLinkProcessor {
    public void process(Map<String, OpenAPI> openApiMap, LinkDTO link, NodeDTO toNode, NodeDTO fromNode, EventDTO event)
            throws JsonProcessingException {
        OpenAPI openAPI = openApiMap.get(toNode.getName());
        if (openAPI == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_OPEN_API_SPEC_FOUND_SKIP_INC, toNode.getName()));
            return;
        }
        OpenApiHelper.ensureComponents(openAPI);

        String broker;
        if (fromNode.getBrokerType() == null) {
            broker = "undefined";
        } else {
            broker = fromNode.getBrokerType().toString().toLowerCase();
        }

        String path;
        if (event == null) {
            // Handle case when event is null - create path with undefined_event
            switch (broker) {
                case "kafka":
                    String group = Optional.ofNullable(link.getGroup()).filter(g -> !g.isBlank()).orElse("default");
                    path = "/kafka/" + group + "/" + fromNode.getName() + "/undefined_event";
                    break;
                case "jms":
                    path = "/jms/" + fromNode.getName() + "/undefined_event";
                    break;
                case "rabbitmq":
                    path = "/rabbitmq/" + fromNode.getName() + "/undefined_event";
                    break;
                case "undefined":
                    path = "/undefined_broker/" + fromNode.getName() + "/undefined_event";
                    break;
                default:
                    path = "/" + broker + "/" + fromNode.getName() + "/undefined_event";
                    log.info(MessageHelper.getStaticMessage("axenapi.info.broker.unrecognized.use.default", broker, path));
                    break;
            }
        } else {
            switch (broker) {
                case "kafka":
                    String group = Optional.ofNullable(link.getGroup()).filter(g -> !g.isBlank()).orElse("default");
                    path = "/kafka/" + group + "/" + fromNode.getName() + "/" + event.getName();
                    break;
                case "jms":
                    path = "/jms/" + fromNode.getName() + "/" + event.getName();
                    break;
                case "rabbitmq":
                    path = "/rabbitmq/" + fromNode.getName() + "/" + event.getName();
                    break;
                case "undefined":
                    path = "/undefined_broker/" + fromNode.getName() + "/" + event.getName();
                    break;
                default:
                    path = "/" + broker + "/" + fromNode.getName() + "/" + event.getName();
                    log.info(MessageHelper.getStaticMessage("axenapi.info.broker.unrecognized.use.default", broker, path));
                    break;
            }
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.add.post.path.service", path, toNode.getName()));

        Set<String> tags = new LinkedHashSet<>();
        if (!link.getTags().isEmpty()) {
            tags.addAll(link.getTags());
        }
        if (event != null && !event.getTags().isEmpty()) {
            tags.addAll(event.getTags());
        }

        Operation postOp = new Operation().responses(OpenApiHelper.createSimpleResponses());
        if (!tags.isEmpty()) {
            postOp.tags(new ArrayList<>(tags));
        }

        PathItem pathItem = new PathItem().post(postOp);
        if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
            pathItem.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
        }
        openAPI.getPaths().addPathItem(path, pathItem);

        if (event != null) {
            Schema<?> schema = OpenApiHelper.getOrCreateSchema(openAPI, event);
            Map<String, Object> xIncoming = new LinkedHashMap<>();
            xIncoming.put("topics", List.of(fromNode.getName()));
            if (!tags.isEmpty()) {
                xIncoming.put("tags", new ArrayList<>(tags));
            }
            schema.addExtension("x-incoming", xIncoming);

            if (fromNode.getDocumentationFileLinks() != null && !fromNode.getDocumentationFileLinks().isEmpty()) {
                schema.addExtension("x-documentation-file-links", new ArrayList<>(fromNode.getDocumentationFileLinks()));
            }

            openAPI.getComponents().addSchemas(event.getName(), schema);
        }
    }
}
