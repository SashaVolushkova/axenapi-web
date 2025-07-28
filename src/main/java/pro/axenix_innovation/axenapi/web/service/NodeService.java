package pro.axenix_innovation.axenapi.web.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import java.util.*;
import java.util.stream.Collectors;
import static pro.axenix_innovation.axenapi.web.service.PathsService.buildUndirectedAdjacencyMap;


public class NodeService {

    private static final Logger logger = LoggerFactory.getLogger(NodeService.class);

    public static List<NodeDTO> findIncorrectNodes(EventGraphDTO graph) {
        Set<UUID> graphNodes = graph.getNodes().stream()
                .map(NodeDTO::getId).collect(Collectors.toSet());
        logger.info("Множество всех Nodes создано {}!", graphNodes);
        Map<UUID, List<UUID>> adj = buildUndirectedAdjacencyMap(graph.getLinks());
        logger.info("Матрица смежности связнных Nodes создана {}!", adj);
//        UUID start = adj.keySet().iterator().next();
        Set<UUID> difference = new HashSet<>(graphNodes);
        difference.removeAll(adj.keySet());
        return difference.isEmpty()
                ? null : difference.stream().map(x->
                graph.getNodes().stream().filter(n -> n.getId().equals(x))
                        .findFirst().orElse(null)).toList();
    }
}
