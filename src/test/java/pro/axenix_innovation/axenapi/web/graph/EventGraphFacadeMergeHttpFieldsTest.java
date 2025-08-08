package pro.axenix_innovation.axenapi.web.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.service.ConvertMdPdfDocumentService;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для проверки корректного слияния HTTP полей в методе merge класса EventGraphFacade
 */
class EventGraphFacadeMergeHttpFieldsTest {

    @BeforeEach
    void setUp() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MessageHelper.setStaticMessageSource(messageSource);
    }
    @Test
    void testMergeHttpFields_NewHttpFieldsAreCopied() {
        // Создаем первый граф с узлом без HTTP полей
        EventGraphDTO graph1 = new EventGraphDTO();
        graph1.setName("Graph1");
        graph1.setNodes(new ArrayList<>());
        graph1.setEvents(new ArrayList<>());
        graph1.setLinks(new ArrayList<>());
        graph1.setTags(new HashSet<>());
        
        UUID nodeId = UUID.randomUUID();
        UUID graphId = UUID.randomUUID();
        
        NodeDTO node1 = new NodeDTO();
        node1.setId(nodeId);
        node1.setName("test-http-endpoint");
        node1.setType(NodeDTO.TypeEnum.HTTP);
        node1.setBrokerType(NodeDTO.BrokerTypeEnum.UNDEFINED);
        node1.setBelongsToGraph(List.of(graphId));
        node1.setMethodType(NodeDTO.MethodTypeEnum.GET);
        node1.setNodeUrl("/api/test");
        node1.setTags(new HashSet<>());
        
        graph1.addNodesItem(node1);

        // Создаем второй граф с тем же узлом, но с HTTP полями
        EventGraphDTO graph2 = new EventGraphDTO();
        graph2.setName("Graph2");
        graph2.setNodes(new ArrayList<>());
        graph2.setEvents(new ArrayList<>());
        graph2.setLinks(new ArrayList<>());
        graph2.setTags(new HashSet<>());
        
        HttpParametersDTO httpParameters = new HttpParametersDTO();
        HttpRequestBodyDTO httpRequestBody = new HttpRequestBodyDTO();
        HttpResponseDTO httpResponse = new HttpResponseDTO();
        httpResponse.setStatusCode(HttpStatusCodeEnum._200);
        httpResponse.setDescription("Success response");
        
        NodeDTO node2 = new NodeDTO();
        node2.setId(UUID.randomUUID());
        node2.setName("test-http-endpoint");
        node2.setType(NodeDTO.TypeEnum.HTTP);
        node2.setBrokerType(NodeDTO.BrokerTypeEnum.UNDEFINED);
        node2.setBelongsToGraph(List.of(graphId));
        node2.setMethodType(NodeDTO.MethodTypeEnum.GET);
        node2.setNodeUrl("/api/test");
        node2.setTags(new HashSet<>());
        node2.setHttpParameters(httpParameters);
        node2.setHttpRequestBody(httpRequestBody);
        node2.setHttpResponses(List.of(httpResponse));
        
        graph2.addNodesItem(node2);

        // Выполняем слияние
        EventGraphDTO mergedGraph = EventGraphFacade.merge(graph1, graph2);

        // Проверяем результат
        assertNotNull(mergedGraph);
        assertEquals(1, mergedGraph.getNodes().size());
        
        NodeDTO mergedNode = mergedGraph.getNodes().get(0);
        assertEquals("test-http-endpoint", mergedNode.getName());
        assertEquals(NodeDTO.TypeEnum.HTTP, mergedNode.getType());
        
        // Проверяем, что HTTP поля были скопированы
        assertNotNull(mergedNode.getHttpParameters(), "httpParameters должны быть скопированы");
        assertNotNull(mergedNode.getHttpRequestBody(), "httpRequestBody должно быть скопировано");
        assertNotNull(mergedNode.getHttpResponses(), "httpResponses должны быть скопированы");
        assertEquals(1, mergedNode.getHttpResponses().size());
        assertEquals(HttpStatusCodeEnum._200, mergedNode.getHttpResponses().get(0).getStatusCode());
    }
}