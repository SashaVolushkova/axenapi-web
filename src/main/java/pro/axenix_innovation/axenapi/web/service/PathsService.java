package pro.axenix_innovation.axenapi.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.util.*;
import java.util.stream.Collectors;

public class PathsService {

    private static final Logger logger = LoggerFactory.getLogger(PathsService.class);

    public static List<List<LinkDTO>> findAllPaths(EventGraphDTO graph, UUID from, UUID to) {
        Map<UUID, List<LinkDTO>> adjacencyMap = buildAdjacencyMap(graph);
        List<List<LinkDTO>> allPaths = new ArrayList<>();
        Set<String> uniquePaths = new HashSet<>();
        findPathsDFS(from, to, adjacencyMap, new ArrayList<>(), allPaths, new HashSet<>(), uniquePaths);
        logger.info("Total unique paths found: {}", allPaths.size());
        return allPaths;
    }

    public static List<List<LinkDTO>> findAllShortestPaths(EventGraphDTO graph, UUID from, UUID to) {
        Map<UUID, List<LinkDTO>> adjacencyMap = buildAdjacencyMap(graph);
        if (!adjacencyMap.containsKey(from) || !adjacencyMap.containsKey(to)) return Collections.emptyList();

        Queue<Object[]> queue = new LinkedList<>();
        List<List<LinkDTO>> shortestPaths = new ArrayList<>();
        int minPathLength = -1;

        queue.add(new Object[]{from, new ArrayList<LinkDTO>()});

        while (!queue.isEmpty()) {
            Object[] current = queue.poll();
            UUID currentNode = (UUID) current[0];
            List<LinkDTO> path = (List<LinkDTO>) current[1];

            if (currentNode.equals(to)) {
                if (minPathLength == -1 || path.size() == minPathLength) {
                    shortestPaths.add(new ArrayList<>(path));
                    minPathLength = path.size();
                }
                continue;
            }

            if (minPathLength != -1 && path.size() >= minPathLength) continue;

            for (LinkDTO link : adjacencyMap.getOrDefault(currentNode, Collections.emptyList())) {
                if (!pathContainsNode(path, link.getToId())) {
                    List<LinkDTO> newPath = new ArrayList<>(path);
                    newPath.add(link);
                    queue.add(new Object[]{link.getToId(), newPath});
                }
            }
        }

        return shortestPaths;
    }

    public static List<List<LinkDTO>> minimalSpanningPaths(EventGraphDTO graph) {
        if (!isGraphConnected(graph)) {
            logger.error("Graph is not connected. No spanning trees exist.");
            return null;
        }

        List<LinkDTO> links = graph.getLinks();
        Set<UUID> allNodes = graph.getNodes().stream().map(NodeDTO::getId).collect(Collectors.toSet());
        int requiredEdges = allNodes.size() - 1;

        List<List<LinkDTO>> mstResults = new ArrayList<>();
        buildSpanningTrees(links, new ArrayList<>(), 0, requiredEdges, allNodes, mstResults);

        logger.info("Found {} minimal spanning trees", mstResults.size());
        return mstResults;
    }

    public static Set<String> extractUniqueTags(List<List<LinkDTO>> paths) {
        return paths.stream()
                .flatMap(List::stream)
                .filter(link -> link.getTags() != null)
                .flatMap(link -> link.getTags().stream())
                .collect(Collectors.toSet());
    }
    public static List<String> findInvalidLinks(EventGraphDTO eventGraph) {
        logger.info("Starting validation of links in the EventGraph.");
        List<String> invalidLinks = new ArrayList<>();

        Map<UUID, String> nodeTypeMap = new HashMap<>();
        if (eventGraph.getNodes() != null) {
            for (NodeDTO node : eventGraph.getNodes()) {
                nodeTypeMap.put(node.getId(), node.getType().name());
            }
        }

        if (eventGraph.getLinks() != null) {
            for (LinkDTO link : eventGraph.getLinks()) {
                UUID fromId = link.getFromId();
                UUID toId = link.getToId();

                String fromType = nodeTypeMap.get(fromId);
                String toType = nodeTypeMap.get(toId);

                if (!isValidLink(fromType, toType)) {
                    String errorMessage = String.format(
                            "Invalid link: fromId=%s (type=%s) -> toId=%s (type=%s)",
                            fromId, fromType, toId, toType
                    );
                    invalidLinks.add(errorMessage);
                    logger.warn(errorMessage);
                }
            }
        }
        logger.info("Validation completed. Found {} invalid links.", invalidLinks.size());
        return invalidLinks;
    }
    private static boolean isValidLink(String fromType, String toType) {
        return ("SERVICE".equals(fromType) && "TOPIC".equals(toType)) || // Сервис → Топик
                ("TOPIC".equals(fromType) && "SERVICE".equals(toType)) || // Топик → Сервис
                ("HTTP".equals(fromType) && "SERVICE".equals(toType));    // HTTP → Сервис
    }

    private static void findPathsDFS(UUID current, UUID target,
                                     Map<UUID, List<LinkDTO>> adj,
                                     List<LinkDTO> path,
                                     List<List<LinkDTO>> allPaths,
                                     Set<UUID> visited,
                                     Set<String> uniquePathKeys) {
        if (!visited.add(current)) return;

        if (current.equals(target)) {
            String key = path.stream()
                    .map(l -> l.getFromId() + "->" + l.getToId())
                    .collect(Collectors.joining(","));
            if (uniquePathKeys.add(key)) {
                allPaths.add(new ArrayList<>(path));
                logger.debug("Unique path: {}", key);
            }
            visited.remove(current);
            return;
        }

        for (LinkDTO link : adj.getOrDefault(current, Collections.emptyList())) {
            path.add(link);
            findPathsDFS(link.getToId(), target, adj, path, allPaths, new HashSet<>(visited), uniquePathKeys);
            path.remove(path.size() - 1);
        }
    }

    private static Map<UUID, List<LinkDTO>> buildAdjacencyMap(EventGraphDTO graph) {
        Map<UUID, List<LinkDTO>> map = new HashMap<>();
        for (NodeDTO node : graph.getNodes()) {
            map.putIfAbsent(node.getId(), new ArrayList<>());
        }
        for (LinkDTO link : graph.getLinks()) {
            map.get(link.getFromId()).add(link);
        }
        return map;
    }

    private static boolean pathContainsNode(List<LinkDTO> path, UUID nodeId) {
        return path.stream().anyMatch(link -> link.getToId().equals(nodeId));
    }

    private static boolean isGraphConnected(EventGraphDTO graph) {
        Map<UUID, List<UUID>> adj = buildUndirectedAdjacencyMap(graph.getLinks());
        Set<UUID> visited = new HashSet<>();
        UUID start = adj.keySet().iterator().next();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            UUID node = stack.pop();
            for (UUID neighbor : adj.getOrDefault(node, Collections.emptyList())) {
                if (visited.add(neighbor)) stack.push(neighbor);
            }
        }

        Set<UUID> allNodeIds = graph.getNodes().stream().map(NodeDTO::getId).collect(Collectors.toSet());
        return visited.equals(allNodeIds);
    }

    public static Map<UUID, List<UUID>> buildUndirectedAdjacencyMap(List<LinkDTO> links) {
        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (LinkDTO link : links) {
            adj.computeIfAbsent(link.getFromId(), k -> new ArrayList<>()).add(link.getToId());
            adj.computeIfAbsent(link.getToId(), k -> new ArrayList<>()).add(link.getFromId());
        }
        return adj;
    }

    private static void buildSpanningTrees(List<LinkDTO> links, List<LinkDTO> current,
                                           int index, int remaining,
                                           Set<UUID> allNodes, List<List<LinkDTO>> result) {
        if (remaining == 0) {
            if (connectsAllNodes(current, allNodes)) result.add(new ArrayList<>(current));
            return;
        }

        for (int i = index; i < links.size(); i++) {
            LinkDTO link = links.get(i);
            if (!createsCycle(current, link)) {
                current.add(link);
                buildSpanningTrees(links, current, i + 1, remaining - 1, allNodes, result);
                current.remove(current.size() - 1);
            }
        }
    }

    private static boolean connectsAllNodes(List<LinkDTO> links, Set<UUID> allNodes) {
        if (links.isEmpty()) return false;
        Map<UUID, List<UUID>> adj = buildUndirectedAdjacencyMap(links);
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        UUID start = links.get(0).getFromId();
        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {
            UUID node = stack.pop();
            for (UUID neighbor : adj.getOrDefault(node, Collections.emptyList())) {
                if (visited.add(neighbor)) stack.push(neighbor);
            }
        }

        return visited.equals(allNodes);
    }

    private static boolean createsCycle(List<LinkDTO> links, LinkDTO newLink) {
        Map<UUID, UUID> parent = new HashMap<>();
        for (LinkDTO link : links) {
            parent.putIfAbsent(link.getFromId(), link.getFromId());
            parent.putIfAbsent(link.getToId(), link.getToId());
        }

        parent.putIfAbsent(newLink.getFromId(), newLink.getFromId());
        parent.putIfAbsent(newLink.getToId(), newLink.getToId());

        for (LinkDTO link : links) {
            union(link.getFromId(), link.getToId(), parent);
        }

        return find(newLink.getFromId(), parent).equals(find(newLink.getToId(), parent));
    }

    private static UUID find(UUID u, Map<UUID, UUID> parent) {
        if (!parent.get(u).equals(u)) {
            parent.put(u, find(parent.get(u), parent));
        }
        return parent.get(u);
    }

    private static void union(UUID u1, UUID u2, Map<UUID, UUID> parent) {
        UUID root1 = find(u1, parent);
        UUID root2 = find(u2, parent);
        if (!root1.equals(root2)) {
            parent.put(root2, root1);
        }
    }
}
