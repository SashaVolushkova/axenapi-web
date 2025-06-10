package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
public class PathsServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(PathsServiceTest.class);

    @Autowired
    private ObjectMapper objectMapper;

    private EventGraphDTO graph;

    public void setUp(String graphFilePath) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(graphFilePath)) {
            assertNotNull(inputStream, "Граф не найден по пути: " + graphFilePath);
            graph = objectMapper.readValue(inputStream, EventGraphDTO.class);
        }
    }

    private void testAllPaths(String graphFilePath, UUID fromId, UUID toId, int expectedPathCount, List<String> expectedPathStrings) throws Exception {
        setUp(graphFilePath);

        List<List<LinkDTO>> allPaths = PathsService.findAllPaths(graph, toId, fromId);

        assertNotNull(allPaths, "Пути не найдены!");
        assertEquals(expectedPathCount, allPaths.size(), "Количество найденных путей не соответствует ожидаемому!");

        Set<String> actualPaths = new HashSet<>();

        for (List<LinkDTO> path : allPaths) {
            String pathKey = path.stream()
                    .map(link -> link.getFromId() + "->" + link.getToId())
                    .collect(Collectors.joining(","));

            actualPaths.add(pathKey);
            logger.info("Найден путь: {}", pathKey);
        }

        logger.info("Общее количество уникальных путей: {}", actualPaths.size());

        for (String expectedPath : expectedPathStrings) {
            assertTrue(actualPaths.contains(expectedPath), "Ожидаемый путь не найден: " + expectedPath);
        }
    }


    //от warehouse до orders
    @Test
    public void testFindAllPaths2() throws Exception {
        String graphFilePath = "path/for_test.json";
        UUID start = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID target = UUID.fromString("c3d4e5f6-7890-1234-5678-90abcdef0123");

        List<String> expectedPaths = List.of(
                "a1b2c3d4-e5f6-7890-1234-567890abcdef->d4e5f678-9012-3456-7890-abcdef012345," +
                        "d4e5f678-9012-3456-7890-abcdef012345->c3d4e5f6-7890-1234-5678-90abcdef0123",

                "a1b2c3d4-e5f6-7890-1234-567890abcdef->78901234-5678-90ab-cdef-0123456789ab," +
                        "78901234-5678-90ab-cdef-0123456789ab->c3d4e5f6-7890-1234-5678-90abcdef0123",

                "a1b2c3d4-e5f6-7890-1234-567890abcdef->90123456-7890-abcd-ef12-34567890abcd," +
                        "90123456-7890-abcd-ef12-34567890abcd->f6789012-3456-7890-abcd-ef0123456789," +
                        "f6789012-3456-7890-abcd-ef0123456789->78901234-5678-90ab-cdef-0123456789ab," +
                        "78901234-5678-90ab-cdef-0123456789ab->c3d4e5f6-7890-1234-5678-90abcdef0123",

                "a1b2c3d4-e5f6-7890-1234-567890abcdef->89012345-6789-0abc-def1-234567890abc," +
                        "89012345-6789-0abc-def1-234567890abc->c3d4e5f6-7890-1234-5678-90abcdef0123"
        );

        testAllPaths(graphFilePath, target, start, expectedPaths.size(), expectedPaths);
    }

    @Test
    public void testFindAllShortestPaths1Link() throws Exception {
        String graphFilePath = "path/for_test.json";
        setUp(graphFilePath);
        UUID fromId = UUID.fromString("d4e5f678-9012-3456-7890-abcdef012345");
        UUID toId = UUID.fromString("c3d4e5f6-7890-1234-5678-90abcdef0123");

        List<List<LinkDTO>> shortestPaths = PathsService.findAllShortestPaths(graph, fromId, toId);

        assertNotNull(shortestPaths, "Список кратчайших путей не должен быть null!");

        assertFalse(shortestPaths.isEmpty(), "Должен быть хотя бы один кратчайший путь!");

        // Проверяем содержимое единственного пути
        List<LinkDTO> singlePath = shortestPaths.get(0);
        assertNotNull(singlePath, "Путь не должен быть null!");
        assertEquals(1, singlePath.size(), "Кратчайший путь должен содержать ровно 1 линк.");

        LinkDTO link = singlePath.get(0);
        assertEquals(fromId, link.getFromId());
        assertEquals(toId, link.getToId());

    }
    @Test
    public void testFindAllShortestPaths0Links() throws Exception {
        String graphFilePath = "path/complex_event_driven_system_with_multiple_paths.json";
        setUp(graphFilePath);

        UUID fromId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID toId = UUID.fromString("d4e5f678-9012-3456-7890-abcdef012345");

        List<List<LinkDTO>> shortestPaths = PathsService.findAllShortestPaths(graph, fromId, toId);

        assertNotNull(shortestPaths, "Список кратчайших путей не должен быть null!");

        assertTrue(shortestPaths.isEmpty(), "Должен быть пустой список путей, так как пути не существует!");

    }
    @Test
    public void testFindAllShortestPaths2Links() throws Exception {
        String graphFilePath = "path/for_test.json";
        setUp(graphFilePath);

        UUID fromId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID toId = UUID.fromString("f6789012-3456-7890-abcd-ef0123456789");

        List<List<LinkDTO>> shortestPaths = PathsService.findAllShortestPaths(graph, fromId, toId);

        assertNotNull(shortestPaths, "Список кратчайших путей не должен быть null!");

        assertFalse(shortestPaths.isEmpty(), "Должен быть хотя бы один кратчайший путь!");

        List<LinkDTO> singlePath = shortestPaths.get(0);
        assertNotNull(singlePath, "Путь не должен быть null!");
        assertEquals(2, singlePath.size(), "Кратчайший путь должен содержать ровно 2 линка.");

    }
    @Test
    public void testMinimalSpanningPaths() throws Exception {
        String graphFilePath = "path/complex_event_driven_system_with_multiple_paths.json";
        setUp(graphFilePath);
        List<List<LinkDTO>> msts = PathsService.minimalSpanningPaths(graph);

        assertNotNull(msts, "не найдены связи для всех узлов!");
        assertEquals(90, msts.size());
//        assertEquals(2, msts.get(0).size());
    }

    @Test
    public void testMinimalSpanningPaths_DisconnectedGraph() throws Exception {
        // Создаем несвязный граф
        String graphFilePath = "path/complex_event_driven_system_with_multiple_paths.json";
        setUp(graphFilePath);
        EventGraphDTO disconnectedGraph = new EventGraphDTO();
        disconnectedGraph.setNodes(graph.getNodes());

        List<LinkDTO> links = new ArrayList<>(graph.getLinks());
        links.remove(1);
        links.remove(2);
        links.remove(0);
        links.remove(1);
        disconnectedGraph.setLinks(links);

        assertNull(PathsService.minimalSpanningPaths(disconnectedGraph),
                "Для несвязного графа должен возвращаться null");
    }
    @Test
    public void testMinimalSpanningPaths_simpleGraph() throws Exception {
        String graphFilePath = "path/for_test_minimalSpanningPaths.json";
        setUp(graphFilePath);
        LinkDTO link1 = new LinkDTO(UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("b2c3d4e5-f678-9012-3456-7890abcdef01"), UUID.fromString("e5f71890-1234-5678-90ab-cdef01234567" ));
        LinkDTO link2 = new LinkDTO(UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("c3d4e5f6-7890-1234-5678-90abcdef0123"),UUID.fromString("f3789012-3456-7890-abcd-ef0123456789") );
        LinkDTO link3 = new LinkDTO(UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("d4e5f678-9012-3456-7890-abcdef012345"), UUID.fromString("78601234-5678-90ab-cdef-0123456789ab"));
        LinkDTO link4 = new LinkDTO(UUID.fromString("b2c3d4e5-f678-9012-3456-7890abcdef01"),
                UUID.fromString("c3d4e5f6-7890-1234-5678-90abcdef0123"), UUID.fromString("89712345-6789-0abc-def1-234567890abc"));
        LinkDTO link5 = new LinkDTO(UUID.fromString("c3d4e5f6-7890-1234-5678-90abcdef0123"),
                UUID.fromString("d4e5f678-9012-3456-7890-abcdef012345"), UUID.fromString("90127456-7890-abcd-ef12-34567890abcd"));

        List<List<LinkDTO>> msts = PathsService.minimalSpanningPaths(graph);

        List<List<LinkDTO>>  expectedMsts = List.of(List.of(link1, link2, link3), List.of(link1, link4, link5),
                List.of(link2, link4, link5), List.of(link3, link4, link5), List.of(link1, link3, link5),
                List.of(link2, link3, link4), List.of(link1, link2, link5), List.of(link1, link3, link4));

        assertNotNull(msts, "не найдены связи для всех узлов!");

        assertFalse(msts.isEmpty(), "Должен быть хотя бы одно минимальное остовной путь !");

        assertEquals(8, msts.size());

        Set<Set<LinkDTO>> mstsSet = msts.stream().map(HashSet::new).collect(Collectors.toSet());
        Set<Set<LinkDTO>> expectedMstsSet = expectedMsts.stream().map(HashSet::new).collect(Collectors.toSet());

        assertEquals(expectedMstsSet, mstsSet);
    }

    @Test
    public void testMinimalSpanningPaths_simpleGraphDisconnectedGraph() throws Exception {

        String graphFilePath = "path/for_test_minimalSpanningPaths.json";
        setUp(graphFilePath);
        EventGraphDTO disconnectedGraph = new EventGraphDTO();
        disconnectedGraph.setNodes(graph.getNodes());

        List<LinkDTO> links = new ArrayList<>(graph.getLinks());
        links.remove(1);
        links.remove(2);
        links.remove(0);
        links.remove(1);
        disconnectedGraph.setLinks(links);

        assertNull(PathsService.minimalSpanningPaths(disconnectedGraph),
                "Для несвязного графа должен возвращаться null");
    }
    @Test
    public void testFindAllShortestPathsWithTwoPathsOf2Links() throws Exception {
        String graphFilePath = "path/for_test_shotest_paths.json";
        setUp(graphFilePath);

        UUID fromId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID toId = UUID.fromString("e5f67890-1234-5678-90ab-cdef01234567");

        List<List<LinkDTO>> shortestPaths = PathsService.findAllShortestPaths(graph, fromId, toId);

        assertNotNull(shortestPaths, "Список кратчайших путей не должен быть null!");

        assertEquals(2, shortestPaths.size(), "Должно быть найдено ровно два кратчайших пути!");

        for (List<LinkDTO> path : shortestPaths) {
            assertNotNull(path, "Путь не должен быть null!");
            assertEquals(2, path.size(), "Каждый кратчайший путь должен содержать ровно 2 линка.");

            LinkDTO firstLink = path.get(0);
            LinkDTO secondLink = path.get(1);
            assertEquals(fromId, firstLink.getFromId(), "Первый линк должен начинаться с fromId.");
            assertEquals(toId, secondLink.getToId(), "Последний линк должен заканчиваться на toId.");
        }

        Set<UUID> intermediateVertices = new HashSet<>();
        for (List<LinkDTO> path : shortestPaths) {
            UUID intermediateVertex = path.get(0).getToId();
            assertTrue(intermediateVertices.add(intermediateVertex), "Промежуточные вершины должны быть уникальными!");
        }
    }
    @Test
    public void testFindInvalidLinks_NoInvalidLinks() throws Exception {
        String graphFilePath = "path/complex_event_driven_system_with_multiple_paths.json";
        logger.info("Загрузка корректной спецификации графа из файла: {}", graphFilePath);
        setUp(graphFilePath);
        List<String> invalidLinks = PathsService.findInvalidLinks(graph);
        assertTrue(invalidLinks.isEmpty(), "There should be no invalid links in the correct specification.");
        logger.info("Проверка завершена. Некорректных линков не найдено.");
    }

    @Test
    public void testFindInvalidLinks_WithInvalidLinks() throws Exception {
        String graphFilePath = "path/for_test.json";
        logger.info("Загрузка спецификации графа с некорректными линками из файла: {}", graphFilePath);
        setUp(graphFilePath);
        List<String> invalidLinks = PathsService.findInvalidLinks(graph);
        assertFalse(invalidLinks.isEmpty(), "There should be invalid links in the specification.");
        logger.info("Найдены следующие некорректные линки:");
        for (String invalidLink : invalidLinks) {
            logger.warn("Некорректный линк: {}", invalidLink);
        }
        logger.info("Проверка завершена. Найдено {} некорректных линков.", invalidLinks.size());
    }

}
