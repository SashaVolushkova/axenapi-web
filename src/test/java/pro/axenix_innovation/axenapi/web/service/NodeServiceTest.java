package pro.axenix_innovation.axenapi.web.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NodeServiceTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(NodeService.class);

    private EventGraphDTO graph;
    public void setUp(String graphFilePath) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(graphFilePath)) {
            assertNotNull(inputStream, "Граф не найден по пути: " + graphFilePath);
            graph = objectMapper.readValue(inputStream, EventGraphDTO.class);
        }
    }

    @Test
    public void findIncorrectNodes_ReturnNull() throws Exception {

        String graphFilePath = "path/for_test_minimalSpanningPaths.json";
        setUp(graphFilePath);
        List<NodeDTO> result = NodeService.findIncorrectNodes(graph);

        assertNull(result, "Результат должен быть null.");
    }

    @Test
    public void findIncorrectNodes_ReturnAllNodes() throws Exception {

        String graphFilePath = "path/for_test_minimalSpanningPaths.json";
        setUp(graphFilePath);

        graph.setLinks(Collections.emptyList());
        List<NodeDTO> result = NodeService.findIncorrectNodes(graph);

        assertNotNull(result, "Результат не должен быть null. Все линки удалены.");
        assertEquals(graph.getNodes().size(), result.size(), "Все ноды не связаны. Их общее количество должно быть равно " +
                "количеству возвращаемых");
        assertEquals(graph.getNodes().stream().collect(Collectors.toSet()),
                result.stream().collect(Collectors.toSet()));

    }

    @Test
    void findIncorrectNodes_EmptyGraph() throws Exception {
        EventGraphDTO graph = new EventGraphDTO();

        graph.setNodes(Collections.emptyList());
        graph.setLinks(Collections.emptyList());

        List<NodeDTO> result = NodeService.findIncorrectNodes(graph);

        assertNull(result);
    }

    @Test
    void findIncorrectNodes_ReturnOneNode() throws Exception  {
        String graphFilePath = "path/for_test_minimalSpanningPaths.json";
        setUp(graphFilePath);
        graph.setLinks(graph.getLinks().stream().filter(link -> !link.getToId()
                .equals(UUID.fromString("d4e5f678-9012-3456-7890-abcdef012345"))).toList());

        List<NodeDTO> result = NodeService.findIncorrectNodes(graph);
        assertNotNull(result, "Должно вернуть список с одним элементом.");
        assertEquals(1, result.size(), "Должен вернуться один node в списке.");
        assertEquals(graph.getNodes().stream().filter(node ->
                node.getId().equals(UUID.fromString("d4e5f678-9012-3456-7890-abcdef012345"))).toList(), result, "Node должны быть равны.");
    }


}
