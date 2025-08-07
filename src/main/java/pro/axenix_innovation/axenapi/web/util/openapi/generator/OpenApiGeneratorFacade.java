package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.Map;
import java.util.UUID;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_NODE_NOT_FOUND;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_SKIPPING_LINK;

@Slf4j
public class OpenApiGeneratorFacade {

    private static final ServiceOpenApiGenerator serviceOpenApiGenerator = new ServiceOpenApiGenerator();
    private static final TopicToServiceLinkProcessor topicToServiceLinkProcessor = new TopicToServiceLinkProcessor();
    private static final ServiceToTopicLinkProcessor serviceToTopicLinkProcessor = new ServiceToTopicLinkProcessor();
    private static final HttpToServiceLinkProcessor httpToServiceLinkProcessor = new HttpToServiceLinkProcessor();

    public static Map<String, OpenAPI> getOpenAPISpecifications(EventGraphFacade eventGraph) throws JsonProcessingException {
        logGraphInfo(eventGraph);

        Map<String, OpenAPI> openAPIMap = serviceOpenApiGenerator.createOpenAPIMap(eventGraph);
        processLinks(eventGraph, openAPIMap);

        log.info(MessageHelper.getStaticMessage("axenapi.info.finish.create.open.api.spec.service", openAPIMap.size()));
        return openAPIMap;
    }

    public static OpenAPI getOpenAPISpecByServiceId(EventGraphFacade eventGraph, UUID serviceID) throws JsonProcessingException {
        NodeDTO nodeById = eventGraph.getNodeById(serviceID);
        if(nodeById == null) {
            log.warn(MessageHelper.getStaticMessage(WARN_NODE_NOT_FOUND, serviceID));
            return null;
        }
        String name = nodeById.getName();
        Map<String, OpenAPI> openAPISpecifications = getOpenAPISpecifications(eventGraph);
        return openAPISpecifications.get(name);
    }

    private static void logGraphInfo(EventGraphFacade eventGraph) {
        log.info(MessageHelper.getStaticMessage("axenapi.info.received.graph.nodes.links",
                eventGraph.getNodes().size(),
                eventGraph.getLinks().size()
        ));

        if (log.isDebugEnabled()) {
            for (NodeDTO node : eventGraph.getNodes()) {
                log.debug("Node: id={}, name='{}', type={}, brokerType={}",
                        node.getId(), node.getName(), node.getType(), node.getBrokerType());
            }
            for (LinkDTO link : eventGraph.getLinks()) {
                log.debug("Link: id={}, fromId={}, toId={}, eventId={}, group={}",
                        link.getId(), link.getFromId(), link.getToId(), link.getEventId(), link.getGroup());
            }
        }
    }

    private static void processLinks(EventGraphFacade eventGraph, Map<String, OpenAPI> openAPIMap) throws JsonProcessingException {
        for (LinkDTO link : eventGraph.getLinks()) {
            NodeDTO toNode = eventGraph.getNodeById(link.getToId());
            NodeDTO fromNode = eventGraph.getNodeById(link.getFromId());
            EventDTO event = eventGraph.getEventById(link.getEventId());

            if (toNode == null || fromNode == null) {
                log.warn(MessageHelper.getStaticMessage(WARN_SKIPPING_LINK, link.getToId(), link.getFromId(), link.getEventId()));
                continue;
            }

            // TOPIC → SERVICE
            if (toNode.getType() == NodeDTO.TypeEnum.SERVICE && fromNode.getType() == NodeDTO.TypeEnum.TOPIC) {
                topicToServiceLinkProcessor.process(openAPIMap, link, toNode, fromNode, event);
            }

            // SERVICE → TOPIC
            if (fromNode.getType() == NodeDTO.TypeEnum.SERVICE && toNode.getType() == NodeDTO.TypeEnum.TOPIC) {
                serviceToTopicLinkProcessor.process(openAPIMap, link, fromNode, toNode, event);
            }

            // HTTP → SERVICE
            if (toNode.getType() == NodeDTO.TypeEnum.SERVICE && fromNode.getType() == NodeDTO.TypeEnum.HTTP) {
                httpToServiceLinkProcessor.process(openAPIMap, link, toNode, fromNode, event);
            }
        }
    }
}
