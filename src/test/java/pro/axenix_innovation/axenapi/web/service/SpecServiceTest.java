package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessage;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey;
import pro.axenix_innovation.axenapi.web.generate.SpecificationGenerator;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.GenerateSpecPost200Response;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.util.openapi.generator.OpenApiGeneratorFacade;
import pro.axenix_innovation.axenapi.web.validate.EventGraphDTOValidator;

import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

class SpecServiceTest {
    @Mock
    private SpecificationGenerator specificationGenerator;
    @Mock
    private MessageHelper messageHelper;
    @InjectMocks
    private SpecService specService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        specService = new SpecService(specificationGenerator, messageHelper);
    }

    private EventGraphDTO createValidEventGraph() {
        EventGraphDTO graph = new EventGraphDTO();
        graph.setName("test-graph");
        List<NodeDTO> nodes = new ArrayList<>();
        NodeDTO node = mock(NodeDTO.class);
        when(node.toString()).thenReturn("NodeDTO");
        nodes.add(node);
        graph.setNodes(nodes);
        return graph;
    }

    @Test
    void testValidAndGenSpecSuccess() {
        EventGraphDTO graph = createValidEventGraph();
        Map<String, String> result = Map.of("service1", "link1");
        when(specificationGenerator.generate(any(), eq("json"))).thenReturn(result);
        when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("ok");
        try (MockedStatic<EventGraphDTOValidator> validatorMock = mockStatic(EventGraphDTOValidator.class)) {
            validatorMock.when(() -> EventGraphDTOValidator.validateEventGraph(any())).thenReturn(null);
            validatorMock.when(() -> EventGraphDTOValidator.validateLinksHaveEvents(any())).thenReturn(null);
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(graph, "json");
            assertEquals("OK", response.getStatus());
            assertEquals(result, response.getDownloadLinks());
        }
    }

    @Test
    void testValidAndGenSpecNullEventGraph() {
        GenerateSpecPost200Response response = specService.validateAndGenerateSpec(null, "json");
        assertEquals("ERROR", response.getStatus());
        assertEquals("Input graphDTO is null", response.getMessage());
    }

    @Test
    void testValidAndGenSpecValidError() {
        EventGraphDTO graph = createValidEventGraph();
        AppCodeMessage error = mock(AppCodeMessage.class);
        when(error.getEnumItem()).thenReturn(AppCodeMessageKey.RESP_ERROR_VALID_GRAPH_NAME_NULL);
        when(error.getArgs()).thenReturn(new Object[] {"test"});
        try (MockedStatic<EventGraphDTOValidator> validatorMock = mockStatic(EventGraphDTOValidator.class)) {
            validatorMock.when(() -> EventGraphDTOValidator.validateEventGraph(any())).thenReturn(error);
            when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("validation error");
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(graph, "json");
            assertEquals("ERROR", response.getStatus());
            assertEquals("validation error", response.getMessage());
        }
    }

    @Test
    void testValidAndGenSpecEventLinkError() {
        EventGraphDTO graph = createValidEventGraph();
        AppCodeMessage error = mock(AppCodeMessage.class);
        when(error.getEnumItem()).thenReturn(AppCodeMessageKey.RESP_ERROR_VALID_LINKS_NULL_EVENTS);
        when(error.getArgs()).thenReturn(new Object[] {"test"});
        try (MockedStatic<EventGraphDTOValidator> validatorMock = mockStatic(EventGraphDTOValidator.class)) {
            validatorMock.when(() -> EventGraphDTOValidator.validateEventGraph(any())).thenReturn(null);
            validatorMock.when(() -> EventGraphDTOValidator.validateLinksHaveEvents(any())).thenReturn(error);
            when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("link error");
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(graph, "json");
            assertEquals("ERROR", response.getStatus());
            assertEquals("link error", response.getMessage());
        }
    }

    @Test
    void testValidAndGenSpecUnsupportFormat() {
        EventGraphDTO graph = createValidEventGraph();
        when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("unsupported format");
        when(specificationGenerator.generate(any(), eq("xml"))).thenReturn(Map.of());
        try (MockedStatic<EventGraphDTOValidator> validatorMock = mockStatic(EventGraphDTOValidator.class)) {
            validatorMock.when(() -> EventGraphDTOValidator.validateEventGraph(any())).thenReturn(null);
            validatorMock.when(() -> EventGraphDTOValidator.validateLinksHaveEvents(any())).thenReturn(null);
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(graph, "xml");
            assertEquals("ERROR", response.getStatus());
            assertEquals("unsupported format", response.getMessage());
        }
    }

    @Test
    void testValidAndGenSpecDirError() {
        EventGraphDTO graph = createValidEventGraph();
        Map<String, String> result = Map.of("directory_error", "dir fail");
        when(specificationGenerator.generate(any(), eq("json"))).thenReturn(result);
        when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("dir error");
        try (MockedStatic<EventGraphDTOValidator> validatorMock = mockStatic(EventGraphDTOValidator.class)) {
            validatorMock.when(() -> EventGraphDTOValidator.validateEventGraph(any())).thenReturn(null);
            validatorMock.when(() -> EventGraphDTOValidator.validateLinksHaveEvents(any())).thenReturn(null);
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(graph, "json");
            assertEquals("ERROR", response.getStatus());
            assertEquals("dir error", response.getMessage());
        }
    }

    @Test
    void testValidAndGenSpecFileAlreadyExists() {
        EventGraphDTO graph = createValidEventGraph();
        Map<String, String> result = Map.of("service1", "File already exists");
        when(specificationGenerator.generate(any(), eq("json"))).thenReturn(result);
        when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("file exists");
        try (MockedStatic<EventGraphDTOValidator> validatorMock = mockStatic(EventGraphDTOValidator.class)) {
            validatorMock.when(() -> EventGraphDTOValidator.validateEventGraph(any())).thenReturn(null);
            validatorMock.when(() -> EventGraphDTOValidator.validateLinksHaveEvents(any())).thenReturn(null);
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(graph, "json");
            assertEquals("ERROR", response.getStatus());
            assertEquals("file exists", response.getMessage());
        }
    }

    @Test
    void testGetSpecByServiceIdSuccess() throws JsonProcessingException {
        EventGraphDTO graph = createValidEventGraph();
        UUID serviceId = UUID.randomUUID();
        OpenAPI openAPI = mock(OpenAPI.class);
        try (MockedStatic<OpenApiGeneratorFacade> openApiGenMock = mockStatic(OpenApiGeneratorFacade.class)) {
            openApiGenMock.when(() -> OpenApiGeneratorFacade.getOpenAPISpecByServiceId(any(EventGraphFacade.class), eq(serviceId))).thenReturn(openAPI);
            when(messageHelper.getMessage(anyString(), ArgumentMatchers.<Object>any())).thenReturn("ok");
            String result = specService.getSpecByServiceId(graph, serviceId);
            assertNotNull(result);
        }
    }
} 