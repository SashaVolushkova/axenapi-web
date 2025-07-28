package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.generate.SpecificationGenerator;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.repository.SpecificationRepository;
import pro.axenix_innovation.axenapi.web.service.CodeService;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;
import pro.axenix_innovation.axenapi.web.service.SpecService;
import pro.axenix_innovation.axenapi.web.util.ProcessingFiles;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_UNEXPECTED_ERROR;

@SpringBootTest
@AutoConfigureMockMvc
class AxenAPIControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(AxenAPIControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AxenAPIController axenAPIController;

    @MockitoBean
    private CodeService codeService;

    @MockitoBean
    private SpecService specService;

    @Autowired
    private SpecificationGenerator specificationGenerator;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageHelper messageHelper;

    private static final Logger log = LoggerFactory.getLogger(AxenAPIControllerTest.class);


    private MockMultipartFile createMockFile(String filename, String content) {
        return new MockMultipartFile("files", filename, "application/json", content.getBytes());
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        objectMapper = mock(ObjectMapper.class);


        EventGraphDTO eventGraph = new EventGraphDTO();
        NodeDTO serviceNode = new NodeDTO();
        serviceNode.setName("service1");
        serviceNode.id(UUID.randomUUID());
        eventGraph.setNodes(Collections.singletonList(serviceNode));

        SpecService specService = new SpecService(specificationGenerator, messageHelper);

        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn("stub message");
        MessageHelper.setStaticMessageSource(messageSource);
    }

    @Test
    void testUploadPostWithEmptyFileList() {
        ResponseEntity<EventGraphDTO> response = axenAPIController.uploadPost(List.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody(), "Body should be null when no files are provided.");
    }

    @Test
    void testUploadPostWithNullFileList() {
        ResponseEntity<EventGraphDTO> response = axenAPIController.uploadPost(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody(), "Body should be null when file list is null.");
    }

    @Test
    void testUploadPostWithNotFormatFiles() throws IOException {
        MultipartFile file1 = new MockMultipartFile("file1", "file1.json", "application/json", "{ invalid json }".getBytes());
        MultipartFile file2 = new MockMultipartFile("file2", "file2.json", "application/json", "{ invalid json }".getBytes());

        try (MockedStatic<ProcessingFiles> mockedStatic = mockStatic(ProcessingFiles.class)) {
            mockedStatic.when(() -> ProcessingFiles.processFiles(anyList()))
                    .thenThrow(new OpenAPISpecParseException("Invalid file format"));

            ResponseEntity<BaseResponse> response = axenAPIController.uploadPost(List.of(file1, file2));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(response.getBody().getCode(), RESP_UNEXPECTED_ERROR.getCode());
            assertTrue(response.getBody().getMessage().toLowerCase().contains("invalid file format"));
        }
    }

    @Test
    void testProcessFileWithEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.json", "application/json", new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ProcessingFiles.processFile(emptyFile, objectMapper));

        assertEquals("File is null, empty or its input stream is unavailable: empty.json", exception.getMessage());
    }


    @Test
    void testProcessFileWithValidFile() throws IOException, OpenAPISpecParseException {
        String jsonContent = """
                {
                  "openapi": "3.0.1",
                  "info": {
                    "title": "graph",
                    "version": "1.0.0",
                    "description": "Empty OpenAPI specification"
                  },
                  "paths": {}
                }
                """;
        MultipartFile validFile = new MockMultipartFile("file", "valid.json", "application/json", jsonContent.getBytes());

        ObjectMapper objectMapper = new ObjectMapper();

        EventGraphDTO result = ProcessingFiles.processFile(validFile, objectMapper);

        logger.info("Result: {}", result);

        assertNotNull(result, "Result should not be null for a valid file.");

        assertEquals("graph", result.getName(), "The name should be 'graph'.");
    }


    @Test
    void validateAndGenerateSpec_EmptyEventGraph_ReturnsError() {
        EventGraphDTO eventGraph = new EventGraphDTO();
        eventGraph.setNodes(null);

        SpecService specService = new SpecService(specificationGenerator, messageHelper);
        GenerateSpecPost200Response response = specService.validateAndGenerateSpec(eventGraph, "json");

        assertEquals("ERROR", response.getStatus());
        assertEquals("EventGraph contains null nodes.", response.getMessage());
    }


    @Test
    void validateAndGenerateSpec_EmptyNodesInEventGraph_ReturnsError() {
        EventGraphDTO eventGraph = new EventGraphDTO();
        eventGraph.setNodes(Collections.emptyList());

        SpecService specService = new SpecService(specificationGenerator, messageHelper);
        GenerateSpecPost200Response response = specService.validateAndGenerateSpec(eventGraph, "json");

        assertEquals("OK", response.getStatus());
        assertEquals(null, response.getMessage());
    }


    @Test
    void validateAndGenerateSpec_NodeWithoutName_ReturnsError() {
        NodeDTO serviceNode = new NodeDTO();
        serviceNode.setId(UUID.randomUUID());
        serviceNode.setType(NodeDTO.TypeEnum.SERVICE);
        EventGraphDTO eventGraph = new EventGraphDTO();
        eventGraph.setNodes(Collections.singletonList(serviceNode));
        SpecService specService = new SpecService(specificationGenerator, messageHelper);
        GenerateSpecPost200Response response = specService.validateAndGenerateSpec(eventGraph, "json");

        assertEquals("ERROR", response.getStatus());
        assertEquals("EventGraphDTO name is null.", response.getMessage());
    }

    @Test
    void validateAndGenerateSpec_NullEventGraph_ReturnsError() {
        EventGraphDTO eventGraph = null;
        SpecService specService = new SpecService(specificationGenerator, messageHelper);
        GenerateSpecPost200Response response = specService.validateAndGenerateSpec(eventGraph, "json");

        assertEquals("ERROR", response.getStatus());
        assertEquals("Input graphDTO is null", response.getMessage());
    }

    @Test
    public void testUploadPost_Success() throws Exception {
        String jsonGraph = """
                {
                  "openapi": "3.0.1",
                  "info": {
                    "title": "check_service",
                    "description": "axenapi Specification for check service",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/kafka/order_group/order_topic/Order": {
                      "description": "Operation for Order",
                      "post": {
                        "requestBody": {
                          "content": {
                            "application/json": {
                              "schema": {
                                "$ref": "#/components/schemas/Order"
                              }
                            }
                          },
                          "required": true
                        },
                        "responses": {
                          "200": {
                            "description": "Success. No content."
                          }
                        }
                      }
                    },
                    "/kafka/emp_group/emp_topic/Emp": {
                      "description": "Operation for Emp",
                      "post": {
                        "requestBody": {
                          "content": {
                            "application/json": {
                              "schema": {
                                "$ref": "#/components/schemas/Emp"
                              }
                            }
                          },
                          "required": true
                        },
                        "responses": {
                          "200": {
                            "description": "Success. No content."
                          }
                        }
                      }
                    }
                  },
                  "components": {
                    "schemas": {
                      "Order": {
                        "type": "object",
                        "description": "order message",
                        "properties": {
                          "id": {
                            "type": "string",
                            "format": "uuid"
                          }
                        },
                        "x-incoming": {
                          "topics": ["order_topic"]
                        }
                      },
                      "Emp": {
                        "type": "object",
                        "description": "emp message",
                        "properties": {
                          "name": {
                            "type": "string"
                          }
                        },
                        "x-incoming": {
                          "topics": ["emp_topic"]
                        }
                      },
                      "Check": {
                        "type": "object",
                        "description": "check message",
                        "properties": {
                          "id": {
                            "type": "string",
                            "format": "uuid"
                          }
                        },
                        "x-outgoing": {
                          "topics": ["check_topic"],
                          "types": ["Message", "nmnmnm", "kljkljkl"],
                          "tags": ["abcde", "outg"]
                        }
                      }
                    }
                  },
                  "name": "check_service",
                  "nodes": [
                    {
                      "id": "73516b89-3cea-459d-adc3-3557a6d60c3d",
                      "belongsToGraph": ["check_service"],
                      "name": "check_service",
                      "type": "SERVICE",
                      "brokerType": null
                    },
                    {
                      "id": "284c7c9a-93e6-4e49-b6ab-0567c624d48d",
                      "belongsToGraph": ["check_service"],
                      "name": "order_topic",
                      "type": "TOPIC",
                      "brokerType": "KAFKA"
                    },
                    {
                      "id": "b504560e-3e87-46dc-999e-f5a28b99cd46",
                      "belongsToGraph": ["check_service"],
                      "name": "emp_topic",
                      "type": "TOPIC",
                      "brokerType": "KAFKA"
                    },
                    {
                      "id": "9a4f71b6-54b5-4c0c-bb3a-19f2ab427e8a",
                      "belongsToGraph": ["check_service"],
                      "name": "check_topic",
                      "type": "TOPIC",
                      "brokerType": null
                    }
                  ],
                  "events": {
                    "Order": {
                      "id": "e5d43d1d-5068-422b-a658-78d3f953917b",
                      "schema": "{\\r\\n  \\"type\\" : \\"object\\",\\r\\n  \\"properties\\" : {\\r\\n    \\"id\\" : {\\r\\n      \\"type\\" : \\"string\\",\\r\\n      \\"format\\" : \\"uuid\\"\\r\\n    }\\r\\n  },\\r\\n  \\"description\\" : \\"order message\\",\\r\\n  \\"x-incoming\\" : {\\r\\n    \\"topics\\" : [ \\"order_topic\\" ]\\r\\n  }\\r\\n}",
                      "name": "Order"
                    },
                    "Check": {
                      "id": "84d2c32d-8a94-48f0-a2d6-2c11e8efbe44",
                      "schema": "{\\r\\n  \\"type\\" : \\"object\\",\\r\\n  \\"properties\\" : {\\r\\n    \\"id\\" : {\\r\\n      \\"type\\" : \\"string\\",\\r\\n      \\"format\\" : \\"uuid\\"\\r\\n    }\\r\\n  },\\r\\n  \\"description\\" : \\"check message\\",\\r\\n  \\"x-outgoing\\" : {\\r\\n    \\"topics\\" : [ \\"check_topic\\" ],\\r\\n    \\"types\\" : [ \\"Message\\", \\"nmnmnm\\", \\"kljkljkl\\" ],\\r\\n    \\"tags\\" : [ \\"abcde\\", \\"outg\\" ]\\r\\n  }\\r\\n}",
                      "name": "Check"
                    },
                    "Emp": {
                      "id": "cbd28047-cae8-43b7-a719-8f5614e5afd2",
                      "schema": "{\\r\\n  \\"type\\" : \\"object\\",\\r\\n  \\"properties\\" : {\\r\\n    \\"name\\" : {\\r\\n      \\"type\\" : \\"string\\"\\r\\n    }\\r\\n  },\\r\\n  \\"description\\" : \\"emp message\\",\\r\\n  \\"x-incoming\\" : {\\r\\n    \\"topics\\" : [ \\"emp_topic\\" ]\\r\\n  }\\r\\n}",
                      "name": "Emp"
                    }
                  },
                  "links": [],
                  "errors": []
                }""";

        MockMultipartFile file = createMockFile("check_service.json", jsonGraph);

        mockMvc.perform(multipart("/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.links").isArray())
                .andExpect(jsonPath("$.name").value("check_service"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }


    @Test
    public void testUploadPost_NoFilesProvided() throws Exception {
        mockMvc.perform(multipart("/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUploadPost_InvalidFileFormat() throws Exception {
        MockMultipartFile invalidFile = createMockFile("invalid.json", "{ invalid json }");

        mockMvc.perform(multipart("/upload")
                        .file(invalidFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    public void testUploadPost_EmptyFileList() throws Exception {
        mockMvc.perform(multipart("/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void corsTest() throws Exception {
        mockMvc.perform(options("/addServiceToGraph")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenValidationFails() {
        EventGraphDTO eventGraphDTO = new EventGraphDTO();
        eventGraphDTO.setName("Sample Event Graph");
        eventGraphDTO.setNodes(Arrays.asList(
                new NodeDTO(),
                new NodeDTO()
        ));
        eventGraphDTO.setEvents(new ArrayList<>());
        eventGraphDTO.setLinks(Arrays.asList(
                new LinkDTO()
        ));
        eventGraphDTO.setErrors(Arrays.asList(
                new ErrorDTO("service1.yaml", "Invalid service definition")
        ));
        eventGraphDTO.setTags(new LinkedHashSet<>(Arrays.asList("tag1", "tag2")));
        ResponseEntity<GenerateSpecPost200Response> result = axenAPIController
                .generateSpecPost(eventGraphDTO, "json");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatedGraphHasErrors() {

        MultipartFile mockFile = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(mockFile);
        EventGraphDTO graphWithErrors = new EventGraphDTO();
        graphWithErrors.setName("Sample Event Graph");
        graphWithErrors.setNodes(Arrays.asList(
                new NodeDTO(),
                new NodeDTO()
        ));
        graphWithErrors.setEvents(new ArrayList<>());
        graphWithErrors.setLinks(Arrays.asList(
                new LinkDTO()
        ));
        graphWithErrors.setErrors(Arrays.asList(
                new ErrorDTO("service1.yaml", "Invalid service definition")
        ));
        ResponseEntity<EventGraphDTO> result = axenAPIController.addServiceToGraphPost(files, graphWithErrors);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().getErrors().isEmpty(), "Должны быть ошибки в теле ответа");
    }

    @Test
    void updateServiceSpecificationPost_ShouldReturnOkWhenUpdateSuccessful() {
        UUID serviceNodeId = UUID.randomUUID();
        String specification = "{\n" +
                "  \"openapi\": \"3.0.1\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"updated_service\",\n" +
                "    \"version\": \"1.0.0\",\n" +
                "    \"description\": \"Empty OpenAPI specification for service 'updated_service'\"\n" +
                "  },\n" +
                "  \"paths\": {}\n" +
                "}";

        EventGraphDTO originalGraph = new EventGraphDTO();
        originalGraph.setName("original_graph");
        originalGraph.setNodes(List.of(
                NodeDTO.builder()
                        .id(serviceNodeId)
                        .name("service1")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .belongsToGraph(List.of(serviceNodeId))
                        .build()
        ));

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest(
                serviceNodeId, specification, originalGraph
        );

        ResponseEntity<EventGraphDTO> response = axenAPIController.updateServiceSpecificationPost(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        EventGraphDTO body = response.getBody();
        assertNotNull(body);
        assertTrue(body.getErrors().isEmpty());
        assertEquals(1, body.getNodes().size());
        assertEquals(0, body.getLinks().size());
        assertEquals(0, body.getEvents().size());
        assertEquals("updated_service", body.getNodes().getFirst().getName());
        assertTrue(body.getName().contains("original_graph"));
        assertTrue(body.getName().contains("updated_service"));
    }

    @Test
    void updateServiceSpecificationPost_ShouldReturnBadRequestWhenSpecificationEmpty() {
        UUID serviceNodeId = UUID.randomUUID();
        EventGraphDTO originalGraph = new EventGraphDTO();
        originalGraph.setNodes(List.of(
                NodeDTO.builder()
                        .id(serviceNodeId)
                        .name("service1")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .belongsToGraph(List.of(serviceNodeId))
                        .build()
        ));

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest(
                serviceNodeId, "", originalGraph
        );

        ResponseEntity<EventGraphDTO> response = axenAPIController.updateServiceSpecificationPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getErrors().isEmpty());
        assertEquals(null, response.getBody().getErrors().get(0).getFileName());
        assertEquals("Specification is empty", response.getBody().getErrors().get(0).getErrorMessage());
    }

    @Test
    void updateServiceSpecificationPost_ShouldReturnBadRequestWhenServiceNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        String specification = "{\"name\":\"updated_service\",\"nodes\":[],\"links\":[]}";

        EventGraphDTO originalGraph = new EventGraphDTO();
        originalGraph.setNodes(List.of(
                NodeDTO.builder()
                        .id(UUID.randomUUID())
                        .name("service1")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .belongsToGraph(List.of(UUID.randomUUID()))
                        .build()
        ));

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest(
                nonExistentId, specification, originalGraph
        );

        ResponseEntity<EventGraphDTO> response = axenAPIController.updateServiceSpecificationPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateServiceSpecificationPost_ShouldReturnBadRequestWhenInvalidJson() {
        UUID serviceNodeId = UUID.randomUUID();
        String invalidJson = "{invalid json}";

        EventGraphDTO originalGraph = new EventGraphDTO();
        originalGraph.setNodes(List.of(
                NodeDTO.builder()
                        .id(serviceNodeId)
                        .name("service1")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .belongsToGraph(List.of(serviceNodeId))
                        .build()
        ));

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest(
                serviceNodeId, invalidJson, originalGraph
        );

        ResponseEntity<EventGraphDTO> response = axenAPIController.updateServiceSpecificationPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getErrors().isEmpty());
        assertEquals("Failed to parse OpenAPI specification", response.getBody().getErrors().get(0).getErrorMessage());
    }

    @Test
    void updateServiceSpecificationPost_ShouldReturnBadRequestWhenServiceNameMissing() {
        UUID serviceNodeId = UUID.randomUUID();
        String specificationWithoutName = "{\"nodes\":[],\"links\":[]}";

        EventGraphDTO originalGraph = new EventGraphDTO();
        originalGraph.setNodes(List.of(
                NodeDTO.builder()
                        .id(serviceNodeId)
                        .name("service1")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .belongsToGraph(List.of(serviceNodeId))
                        .build()
        ));

        UpdateServiceSpecificationPostRequest request = new UpdateServiceSpecificationPostRequest(
                serviceNodeId, specificationWithoutName, originalGraph
        );

        ResponseEntity<EventGraphDTO> response = axenAPIController.updateServiceSpecificationPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getErrors().isEmpty());
        assertNull(response.getBody().getErrors().get(0).getFileName());
        assertEquals("Failed to parse OpenAPI specification",
                response.getBody().getErrors().get(0).getErrorMessage());
    }

    @Test
    void shouldReturnNoContentWhenGeneratedCodeIsEmpty() {
        EventGraphDTO eventGraph = new EventGraphDTO();
        eventGraph.setNodes(new ArrayList<>());
        eventGraph.setLinks(new ArrayList<>());

        ResponseEntity<Resource> response = axenAPIController.generateCodePost(eventGraph);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody(), "Ожидалось пустое тело ответа");
    }

    @Test
    void shouldReturnNotFoundWhenSpecificationDoesNotExist() {
        String nonExistentId = "non-existent-id";

        ResponseEntity<Resource> response = axenAPIController.downloadSpecsFileIdJsonGet(nonExistentId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Expected 404 Not Found");
    }

    @Test
    void getRequest_ShouldReturnEmptyOptional() {
        Optional<NativeWebRequest> request = axenAPIController.getRequest();

        assertTrue(request.isEmpty(), "Expected Optional to be empty");
    }


}
