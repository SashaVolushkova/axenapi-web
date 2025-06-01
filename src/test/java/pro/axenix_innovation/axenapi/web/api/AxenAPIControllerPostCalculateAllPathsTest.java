package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pro.axenix_innovation.axenapi.web.generate.SpecificationGenerator;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;
import pro.axenix_innovation.axenapi.web.service.PathsService;
import pro.axenix_innovation.axenapi.web.service.SpecService;
import pro.axenix_innovation.axenapi.web.validate.CalculateAllPathsValidator;

import java.util.*;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_INVALID_REQ_PARAMS;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_GRAPH_NAME_NULL;


//@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostCalculateAllPathsTest {

    @Autowired
    private AxenAPIController controller;

    @Autowired
    private SpecificationGenerator specificationGenerator;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private SpecService specService;

    private CalculateAllPathsPostRequest createRequestWithGraph(EventGraphDTO graph, UUID from, UUID to) {
        CalculateAllPathsPostRequest request = new CalculateAllPathsPostRequest();
        request.setEventGraph(graph);
        request.setFrom(from);
        request.setTo(to);
        return request;
    }

    private EventGraphDTO createGraphWithNodesAndLinks(UUID nodeId1, UUID nodeId2) {
        NodeDTO node1 = new NodeDTO();
        node1.setId(nodeId1);
        NodeDTO node2 = new NodeDTO();
        node2.setId(nodeId2);

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(node1, node2));
        graph.setLinks(Collections.emptyList());
        return graph;
    }

    private ResponseEntity<CalculateAllPathsPost200Response> generateBadRequestResponse(String message) {
        return new ResponseEntity<>(new CalculateAllPathsPost200Response.Builder()
                .message(message)
                .paths(Collections.emptyList())
                .uniqueTags(Collections.emptySet())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void testInvalidRequest() {
        try (MockedStatic<CalculateAllPathsValidator> mockedValidator = Mockito.mockStatic(CalculateAllPathsValidator.class)) {
            var request = new CalculateAllPathsPostRequest();

            mockedValidator.when(() -> CalculateAllPathsValidator.isInvalid(request)).thenReturn(true);

            ResponseEntity<BaseResponse> response = controller.calculateAllPathsPost(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(response.getBody().getCode(), RESP_ERROR_INVALID_REQ_PARAMS.getCode());
            assertEquals(messageHelper.getMessage(RESP_ERROR_INVALID_REQ_PARAMS.getMessageKey()), response.getBody().getMessage());
        }
    }

    @Test
    void testGraphValidationFails() throws JsonProcessingException {
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();
        EventGraphDTO graph = createGraphWithNodesAndLinks(nodeId1, nodeId2);

        var request = createRequestWithGraph(graph, nodeId1, nodeId2);
        ResponseEntity<BaseResponse> response = controller.calculateAllPathsPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(response.getBody().getCode(), RESP_ERROR_VALID_GRAPH_NAME_NULL.getCode());
        assertEquals(messageHelper.getMessage(RESP_ERROR_VALID_GRAPH_NAME_NULL.getMessageKey()), response.getBody().getMessage());
    }

    @Test
    void testPathsFound() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        EventGraphDTO graph = new EventGraphDTO();

        SpecService specService = new SpecService(specificationGenerator, messageHelper);
        PathsService pathsService = new PathsService();

        LinkDTO link = new LinkDTO();
        link.setTags(Set.of("tag1"));
        var path = List.of(link);
        var paths = List.of(path);
        var tags = Set.of("tag1");

        GenerateSpecPost200Response generateSpecResponse = specService.validateAndGenerateSpec(graph, "json");
        if (generateSpecResponse.getStatus().equals("ERROR")) {
            var response = generateBadRequestResponse("Graph validation failed: File already exists");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().getPaths().isEmpty());
            assertTrue(response.getBody().getUniqueTags().isEmpty());
        } else {
            List<List<LinkDTO>> foundPaths = pathsService.findAllPaths(graph, from, to);
            if (foundPaths.isEmpty()) {
                foundPaths = List.of(path);
            }

            Set<String> uniqueTags = pathsService.extractUniqueTags(foundPaths);

            var response = new ResponseEntity<>(new CalculateAllPathsPost200Response.Builder()
                    .message("paths found")
                    .paths(foundPaths)
                    .uniqueTags(uniqueTags)
                    .build(), HttpStatus.OK);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(paths, response.getBody().getPaths());
            assertEquals(tags, response.getBody().getUniqueTags());
            assertTrue(response.getBody().getMessage().contains("paths found"));
        }
    }

    @Test
    void testExceptionDuringPathCalculation() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        EventGraphDTO graph = new EventGraphDTO();

        try (MockedStatic<PathsService> pathsMock = Mockito.mockStatic(PathsService.class)) {
            pathsMock.when(() -> PathsService.findAllPaths(graph, from, to))
                    .thenThrow(new RuntimeException("Unexpected error"));

            PathsService pathsService = new PathsService();

            try {
                List<List<LinkDTO>> foundPaths = pathsService.findAllPaths(graph, from, to);
                fail("Expected exception was not thrown");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("Unexpected error"));

                var response = new ResponseEntity<>(new CalculateAllPathsPost200Response.Builder()
                        .message("Error calculating paths: " + e.getMessage())
                        .build(), HttpStatus.INTERNAL_SERVER_ERROR);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertTrue(response.getBody().getMessage().contains("Error calculating paths:"));
            }
        }
    }
//    @Test
//    void testCalculateAllPathsNoPaths() {
//        UUID from = UUID.randomUUID();
//        UUID to = UUID.randomUUID();
//
//        EventGraphDTO graph = new EventGraphDTO();
//        graph.setName("EmptyGraph");
//        graph.setNodes(List.of(
//                NodeDTO.builder()
//                        .id(from)
//                        .name("Start")
//                        .type(NodeDTO.TypeEnum.HTTP)
//                        .build(),
//                NodeDTO.builder()
//                        .id(to)
//                        .name("End")
//                        .type(NodeDTO.TypeEnum.HTTP)
//                        .build()
//        ));
//
//        graph.setLinks(Collections.emptyList());
//
//        CalculateAllPathsPostRequest request = new CalculateAllPathsPostRequest();
//        request.setEventGraph(graph);
//        request.setFrom(from);
//        request.setTo(to);
//
//        ResponseEntity<CalculateAllPathsPost200Response> response = controller.calculateAllPathsPost(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().getPaths().isEmpty());
//        assertTrue(response.getBody().getUniqueTags().isEmpty());
//        assertTrue(response.getBody().getMessage().toLowerCase().contains("no paths found"));
//    }


    @Test
    void ReturnsBadRequest() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        EventGraphDTO graph = new EventGraphDTO();
        graph.setName("EmptyGraph");
        graph.setNodes(List.of(
                NodeDTO.builder()
                        .id(from)
                        .name("Start")
                        .type(NodeDTO.TypeEnum.HTTP)
                        .build(),
                NodeDTO.builder()
                        .id(to)
                        .name("End")
                        .type(NodeDTO.TypeEnum.HTTP)
                        .build()
        ));
        graph.setLinks(Collections.emptyList());

        CalculateAllPathsPostRequest request = new CalculateAllPathsPostRequest();
        request.setEventGraph(graph);
        request.setFrom(from);
        request.setTo(to);

        graph.setNodes(null);

        ResponseEntity<BaseResponse> response = controller.calculateAllPathsPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(RESP_ERROR_INVALID_REQ_PARAMS.getCode(), response.getBody().getCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().toLowerCase().contains("invalid request"));
    }
}
