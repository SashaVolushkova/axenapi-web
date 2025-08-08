package pro.axenix_innovation.axenapi.web.util.openapi.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
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

public class OpenApiGeneratorFacadeTest {
    private EventGraphFacade eventGraph;
    private NodeDTO serviceNode1;
    private NodeDTO serviceNode2;

    @BeforeEach
    void setUp() {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        eventGraph = new EventGraphFacade(eventGraphDTO);

        serviceNode1 = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceA")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        serviceNode2 = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceB")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(new ArrayList<>())
                .build();

        eventGraph.addNode(serviceNode1);
        eventGraph.addNode(serviceNode2);

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
    void testGetOpenAPISpecifications() throws JsonProcessingException {
        // Act
        Map<String, OpenAPI> result = OpenApiGeneratorFacade.getOpenAPISpecifications(eventGraph);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey("ServiceA"));
        assertTrue(result.containsKey("ServiceB"));
    }

    @Test
    void testGetOpenAPISpecByServiceId() throws JsonProcessingException {
        // Act
        OpenAPI result = OpenApiGeneratorFacade.getOpenAPISpecByServiceId(eventGraph, serviceNode1.getId());

        // Assert
        assertNotNull(result);
        assertEquals("ServiceA", result.getInfo().getTitle());
    }

    @Test
    void testGetOpenAPISpecByServiceId_NotFound() throws JsonProcessingException {
        // Act
        OpenAPI result = OpenApiGeneratorFacade.getOpenAPISpecByServiceId(eventGraph, UUID.randomUUID());

        // Assert
        assertNull(result);
    }
}
