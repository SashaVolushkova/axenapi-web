package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllServicePdfGenerationServiceTest {
    @Mock
    private MarkdownSpecService markdownSpecService;
    @Mock
    private ConvertMdPdfDocumentService convertMdPdfDocumentService;
    @InjectMocks
    private AllServicePdfGenerationService service;

    private NodeDTO createServiceNode(UUID id, String name) {
        return NodeDTO.builder()
                .id(id)
                .name(name)
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();
    }

    @Test
    void testGenerateNullGraphThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.generateAllServicesPDF(null));
    }

    @Test
    void testGenerateEmptyNodesThrowsException() {
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> service.generateAllServicesPDF(graph));
    }

    @Test
    void testGenerateNoServiceNodesReturnsEmpty() throws IOException {
        NodeDTO node = NodeDTO.builder()
                .id(UUID.randomUUID())
                .type(NodeDTO.TypeEnum.TOPIC) // not SERVICE
                .build();
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(node));
        AllServicePdf result = service.generateAllServicesPDF(graph);
        assertNotNull(result);
        assertTrue(result.getIndividualMdFiles().isEmpty());
        assertArrayEquals(new byte[0], result.getCombinedPdfBytes());
    }

    @Test
    void testGenerateServiceNodeWithMarkdownAndPdf() throws Exception {
        NodeDTO serviceNode = createServiceNode(UUID.randomUUID(), "TestService");
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));

        EventGraphDTO filteredGraph = new EventGraphDTO();
        filteredGraph.setNodes(List.of(serviceNode));

        Map<String, String> markdownMap = Map.of("TestService.md", "/download/markdown/fileid1.md");
        when(markdownSpecService.generateMarkdownMap(any())).thenReturn(markdownMap);
        when(markdownSpecService.getMarkdownContentByFileId(eq("fileid1"))).thenReturn(Optional.of("# Markdown Content"));
        when(convertMdPdfDocumentService.convertMdToPdf(anyList())).thenReturn(new byte[]{1,2,3});

        AllServicePdf result = service.generateAllServicesPDF(graph);
        assertNotNull(result);
        assertFalse(result.getIndividualMdFiles().isEmpty());
        assertArrayEquals(new byte[]{1,2,3}, result.getCombinedPdfBytes());
        assertTrue(result.getIndividualMdFiles().keySet().iterator().next().contains("testservice"));
    }

    @Test
    void testGenerateMDGenFailsSkipsService() throws Exception {
        NodeDTO serviceNode = createServiceNode(UUID.randomUUID(), "FailService");
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));

        when(markdownSpecService.generateMarkdownMap(any())).thenThrow(new RuntimeException("fail"));
        AllServicePdf result = service.generateAllServicesPDF(graph);
        assertNotNull(result);
        assertTrue(result.getIndividualMdFiles().isEmpty());
        assertArrayEquals(new byte[0], result.getCombinedPdfBytes());
    }

    @Test
    void testGenerateMDMapIsEmptySkipsService() throws Exception {
        NodeDTO serviceNode = createServiceNode(UUID.randomUUID(), "EmptyMarkdownService");
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));
        when(markdownSpecService.generateMarkdownMap(any())).thenReturn(Collections.emptyMap());
        AllServicePdf result = service.generateAllServicesPDF(graph);
        assertNotNull(result);
        assertTrue(result.getIndividualMdFiles().isEmpty());
        assertArrayEquals(new byte[0], result.getCombinedPdfBytes());
    }

    @Test
    void testGenerateRealContentMapIsEmptySkipsService() throws Exception {
        NodeDTO serviceNode = createServiceNode(UUID.randomUUID(), "NoContentService");
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));
        Map<String, String> markdownMap = Map.of("NoContent.md", "/download/markdown/fileid2.md");
        when(markdownSpecService.generateMarkdownMap(any())).thenReturn(markdownMap);
        when(markdownSpecService.getMarkdownContentByFileId(eq("fileid2"))).thenReturn(Optional.empty());
        AllServicePdf result = service.generateAllServicesPDF(graph);
        assertNotNull(result);
        assertTrue(result.getIndividualMdFiles().isEmpty());
        assertArrayEquals(new byte[0], result.getCombinedPdfBytes());
    }

    @Test
    void testGenerateYamlFileSection() throws Exception {
        NodeDTO serviceNode = createServiceNode(UUID.randomUUID(), "YamlService");
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));
        Map<String, String> markdownMap = Map.of("YamlService.yaml", "/download/yaml/fileid3.yaml");
        when(markdownSpecService.generateMarkdownMap(any())).thenReturn(markdownMap);
        when(markdownSpecService.getMarkdownContentByFileId(eq("fileid3"))).thenReturn(Optional.of("openapi: 3.0.0\ninfo:\n  title: YamlService"));
        when(convertMdPdfDocumentService.convertMdToPdf(anyList())).thenReturn(new byte[]{9,9,9});
        AllServicePdf result = service.generateAllServicesPDF(graph);
        assertNotNull(result);
        assertFalse(result.getIndividualMdFiles().isEmpty());
        assertArrayEquals(new byte[]{9,9,9}, result.getCombinedPdfBytes());
        assertTrue(result.getIndividualMdFiles().keySet().iterator().next().contains("yamlservice"));
    }

    @Test
    void shouldGeneratePdfOnlyForServiceNodes() throws Exception {
        UUID serviceId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();

        NodeDTO serviceNode = NodeDTO.builder()
                .id(serviceId)
                .name("Service1")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();

        NodeDTO eventNode = NodeDTO.builder()
                .id(eventId)
                .name("HTTP1")
                .type(NodeDTO.TypeEnum.HTTP)
                .build();

        NodeDTO topicNode = NodeDTO.builder()
                .id(topicId)
                .name("Topic1")
                .type(NodeDTO.TypeEnum.TOPIC)
                .build();

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode, eventNode, topicNode));

        Map<String, String> markdownMap = Map.of("Service1.md", "/download/markdown/" + serviceId + ".md");
        when(markdownSpecService.generateMarkdownMap(any())).thenReturn(markdownMap);
        when(markdownSpecService.getMarkdownContentByFileId(anyString())).thenReturn(Optional.of("# Service1 documentation"));

        when(convertMdPdfDocumentService.convertMdToPdf(anyList())).thenReturn(new byte[]{1, 2, 3});

        AllServicePdf result = service.generateAllServicesPDF(graph);

        assertTrue(result.getIndividualMdFiles().keySet().stream().allMatch(name -> name.contains("service1")));
        assertEquals(1, result.getIndividualMdFiles().size());
        assertArrayEquals(new byte[]{1, 2, 3}, result.getCombinedPdfBytes());

        assertTrue(result.getIndividualMdFiles().keySet().stream().noneMatch(name -> name.contains("event1") || name.contains("topic1")));
    }

} 