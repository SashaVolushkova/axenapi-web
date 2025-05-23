package pro.axenix_innovation.axenapi.web.validate;


import pro.axenix_innovation.axenapi.web.model.CalculateAllPathsPostRequest;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;

import java.util.UUID;

public class CalculateAllPathsValidator {

    public static boolean isInvalid(CalculateAllPathsPostRequest request) {
        return request == null
                || request.getFrom() == null
                || request.getTo() == null
                || request.getEventGraph() == null
                || !containsNode(request.getEventGraph(), request.getFrom())
                || !containsNode(request.getEventGraph(), request.getTo());
    }

    static boolean containsNode(EventGraphDTO graph, UUID nodeId) {
        if (graph.getNodes() == null) return false;
        return graph.getNodes().stream()
                .anyMatch(node -> nodeId.equals(node.getId()));
    }
}
