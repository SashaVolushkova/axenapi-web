package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventGraphFilterTest {

    private static final Logger log = LoggerFactory.getLogger(EventGraphFilterTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("jsonFilesProvider")
    void testFilterByServiceUUIDs_readsFromFile_returnsSameGraphExceptName(Resource resource) throws Exception {
        log.info("Тестируем файл: {}", resource.getFilename());

        EventGraphDTO graph = readGraphFromResource(resource);
        EventGraphDTO filteredGraph = filterGraphByAllServiceUUIDs(graph);
        assertNameIsFiltered(graph, filteredGraph);
        assertGraphsEqualIgnoringName(graph, filteredGraph);
    }

    static Stream<Resource> jsonFilesProvider() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        return Stream.of(resolver.getResources("classpath:results/*.json"));
    }

    private EventGraphDTO readGraphFromResource(Resource resource) throws IOException {
        return objectMapper.readValue(resource.getInputStream(), EventGraphDTO.class);
    }

    private EventGraphDTO filterGraphByAllServiceUUIDs(EventGraphDTO graph) {
        Set<UUID> allServiceUUIDs = graph.getNodes().stream()
                .filter(node -> "SERVICE".equalsIgnoreCase(node.getType().toString()))
                .map(node -> node.getId())
                .collect(Collectors.toSet());
        return MarkdownSpecService.filterByServiceUUIDs(graph, allServiceUUIDs);
    }

    private void assertNameIsFiltered(EventGraphDTO original, EventGraphDTO filtered) {
        assertEquals(original.getName() + "_filtered", filtered.getName());
    }

    private void assertGraphsEqualIgnoringName(EventGraphDTO expected, EventGraphDTO actual) throws Exception {
        String expectedJson = objectMapper.writeValueAsString(expected);
        String actualJson = objectMapper.writeValueAsString(actual);

        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        JsonNode actualNode = objectMapper.readTree(actualJson);

        ((ObjectNode) expectedNode).remove("name");
        ((ObjectNode) actualNode).remove("name");

        JSONAssert.assertEquals(expectedNode.toString(), actualNode.toString(), JSONCompareMode.LENIENT);
    }
}
