package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EventServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void assertUnusedEvents(String resourcePath, int expectedCount) throws Exception {
        EventGraphDTO graph;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream, "Граф не найден по пути: " + resourcePath);
            graph = objectMapper.readValue(inputStream, EventGraphDTO.class);
        }

        List<EventDTO> unusedEvents = EventService.findUnusedEvents(graph);

        assertNotNull(unusedEvents, "Список неиспользуемых событий не должен быть null");
        assertEquals(expectedCount, unusedEvents.size(),
                "Количество неиспользуемых событий не соответствует ожидаемому.");
    }

    @Test
    public void testUnusedEventsSample1() throws Exception {
        assertUnusedEvents("validate/unused_event.json", 2);
    }

    @Test
    public void testUsedEventsSample1() throws Exception {
        assertUnusedEvents("results/consume_three_events_from_different_brokers_service_no_group.json", 0);
    }
}
