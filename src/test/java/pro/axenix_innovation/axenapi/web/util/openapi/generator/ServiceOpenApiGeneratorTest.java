package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceOpenApiGeneratorTest {
    private ServiceOpenApiGenerator serviceOpenApiGenerator;

    @BeforeEach
    void setUp() {
        serviceOpenApiGenerator = new ServiceOpenApiGenerator();
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
    void testCreateOpenAPIMap() {
        // Arrange
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        EventGraphFacade eventGraph = new EventGraphFacade(eventGraphDTO);

        NodeDTO serviceNode1 = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        NodeDTO serviceNode2 = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceB")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("TopicA")
                .type(NodeDTO.TypeEnum.TOPIC)
                .belongsToGraph(new ArrayList<>())
                .build();

        eventGraph.addNode(serviceNode1);
        eventGraph.addNode(serviceNode2);
        eventGraph.addNode(topicNode);

        // Act
        Map<String, OpenAPI> result = serviceOpenApiGenerator.createOpenAPIMap(eventGraph);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey("ServiceA"));
        assertTrue(result.containsKey("ServiceB"));
        assertFalse(result.containsKey("TopicA"));

        OpenAPI serviceASpec = result.get("ServiceA");
        assertEquals("ServiceA", serviceASpec.getInfo().getTitle());
        assertEquals("AxenAPI Specification for ServiceA", serviceASpec.getInfo().getDescription());
        assertEquals("1.0.0", serviceASpec.getInfo().getVersion());
        assertNotNull(serviceASpec.getPaths());

        OpenAPI serviceBSpec = result.get("ServiceB");
        assertEquals("ServiceB", serviceBSpec.getInfo().getTitle());
        assertEquals("AxenAPI Specification for ServiceB", serviceBSpec.getInfo().getDescription());
        assertEquals("1.0.0", serviceBSpec.getInfo().getVersion());
        assertNotNull(serviceBSpec.getPaths());
    }
}
