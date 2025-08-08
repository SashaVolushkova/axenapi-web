package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceToTopicLinkProcessorTest {
    private ServiceToTopicLinkProcessor serviceToTopicLinkProcessor;
    private Map<String, OpenAPI> openApiMap;
    private NodeDTO fromNode;
    private NodeDTO toNode;
    private EventDTO event;
    private LinkDTO link;

    @BeforeEach
    void setUp() {
        serviceToTopicLinkProcessor = new ServiceToTopicLinkProcessor();
        openApiMap = new HashMap<>();
        fromNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();
        toNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .brokerType(NodeDTO.BrokerTypeEnum.KAFKA)
                .build();
        event = EventDTO.builder()
                .id(UUID.randomUUID())
                .name("EventA")
                .schema("{\"type\":\"object\"}")
                .build();
        link = new LinkDTO(UUID.randomUUID(), fromNode.getId(), toNode.getId(), "groupA", event.getId(), new HashSet<>(List.of("tag1")));

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new io.swagger.v3.oas.models.info.Info().title("ServiceA"));
        openApiMap.put("ServiceA", openAPI);

        MessageSource mockMessageSource = mock(MessageSource.class);
        when(mockMessageSource.getMessage(anyString(), any(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try {
            Field field = MessageHelper.class.getDeclaredField("staticMessageSource");
            field.setAccessible(true);
            field.set(null, mockMessageSource);
        } catch (Exception e) {
            fail("Failed to set MessageSource in MessageHelper", e);
        }
    }

    @Test
    void testProcess() throws JsonProcessingException {
        // Act
        serviceToTopicLinkProcessor.process(openApiMap, link, fromNode, toNode, event);

        // Assert
        OpenAPI openAPI = openApiMap.get("ServiceA");
        assertNotNull(openAPI);

        assertNotNull(openAPI.getComponents().getSchemas().get("EventA"));
        Map<String, Object> extensions = openAPI.getComponents().getSchemas().get("EventA").getExtensions();
        assertNotNull(extensions);
        assertTrue(extensions.containsKey("x-outgoing"));
    }
}
