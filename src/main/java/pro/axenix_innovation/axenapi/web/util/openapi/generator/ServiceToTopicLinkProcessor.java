package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_NO_OPEN_API_SPEC_FOUND_SKIP_OUT;

@Slf4j
public class ServiceToTopicLinkProcessor {
    public void process(Map<String, OpenAPI> openApiMap, LinkDTO link, NodeDTO fromNode, NodeDTO toNode, EventDTO event)
            throws JsonProcessingException {
        OpenAPI openAPI = openApiMap.get(fromNode.getName());
        if (openAPI == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NO_OPEN_API_SPEC_FOUND_SKIP_OUT, fromNode.getName()));
            return;
        }
        OpenApiHelper.ensureComponents(openAPI);

        if (event == null) {
            // If event is null, we can't create a schema, so just return
            log.warn("Event is null for outgoing link from service {} to topic {}, skipping schema creation",
                    fromNode.getName(), toNode.getName());
            return;
        }

        Schema<?> schema = OpenApiHelper.getOrCreateSchema(openAPI, event);
        Map<String, Object> xOutgoing = new LinkedHashMap<>();

        String topicSpec = toNode.getBrokerType() + "/" + toNode.getName();
        xOutgoing.put("topics", List.of(topicSpec));

        Set<String> tags = new LinkedHashSet<>();
        if (!link.getTags().isEmpty()) {
            tags.addAll(link.getTags());
        }
        if (!event.getTags().isEmpty()) {
            tags.addAll(event.getTags());
        }
        if (!tags.isEmpty()) {
            xOutgoing.put("tags", new ArrayList<>(tags));
        }

        schema.addExtension("x-outgoing", xOutgoing);

        if (toNode.getDocumentationFileLinks() != null && !toNode.getDocumentationFileLinks().isEmpty()) {
            schema.addExtension("x-documentation-file-links",
                    new ArrayList<>(toNode.getDocumentationFileLinks()));
        }

        openAPI.getComponents().addSchemas(event.getName(), schema);
    }
}
