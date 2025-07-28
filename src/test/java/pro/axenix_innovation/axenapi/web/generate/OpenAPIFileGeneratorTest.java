//package pro.axenix_innovation.axenapi.web.generate;
//
//import io.swagger.v3.oas.models.OpenAPI;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
//import pro.axenix_innovation.axenapi.web.model.NodeDTO;
//
//import java.io.IOException;
//import java.nio.file.FileAlreadyExistsException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//class OpenAPIFileGeneratorTest {
//    private OpenAPIFileGenerator generator;
//    private NodeDTO serviceNode;
//    private EventGraphDTO eventGraph;
//    @TempDir
//    Path tempDir;
//
//    @BeforeEach
//    void setUp() {
//        generator = new OpenAPIFileGenerator(tempDir.toString());
//
//        serviceNode = mock(NodeDTO.class);
//        eventGraph = mock(EventGraphDTO.class);
//
//        when(serviceNode.getName()).thenReturn("ServiceA");
//        when(serviceNode.getType()).thenReturn(NodeDTO.TypeEnum.SERVICE);
//
//        when(eventGraph.getNodes()).thenReturn(List.of(serviceNode));
//    }
//
//    @Test
//    void generateSpecFiles_ShouldThrowException_WhenEventGraphIsNull() {
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> generator.generateSpecFiles(null)
//        );
//        assertEquals("EventGraph or its nodes cannot be null", exception.getMessage());
//    }
//
//    @Test
//    void generateSpecFiles_ShouldThrowException_WhenNodesAreNull() {
//        EventGraphDTO eventGraph = new EventGraphDTO();
//        eventGraph.setNodes(null);
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> generator.generateSpecFiles(eventGraph)
//        );
//        assertEquals("EventGraph or its nodes cannot be null", exception.getMessage());
//    }
//
//    @Test
//    void generateSpecFiles_ShouldReturnEmptyMap_WhenNoServices() {
//        EventGraphDTO eventGraph = new EventGraphDTO();
//        eventGraph.setNodes(List.of(new NodeDTO()
//                .id(UUID.randomUUID())
//                .name("NotService")
//                .type(NodeDTO.TypeEnum.TOPIC)));
//        Map<String, String> result = generator.generateSpecFiles(eventGraph);
//        assertTrue(result.isEmpty(), "Expected empty map when there are no SERVICE nodes");
//    }
//
//    @Test
//    void generateSpecFiles_ShouldCreateFile_WhenValidServiceNode() throws IOException {
//        NodeDTO serviceNode = new NodeDTO().id(UUID.randomUUID()).name("ServiceA").type(NodeDTO.TypeEnum.SERVICE);
//        EventGraphDTO eventGraph = new EventGraphDTO();
//        eventGraph.setNodes(List.of(serviceNode));
//        Map<String, String> result = generator.generateSpecFiles(eventGraph);
//        assertTrue(result.containsKey("ServiceA"), "File link should be present");
//        Path generatedFile = tempDir.resolve("ServiceA_spec.json");
//        assertTrue(Files.exists(generatedFile), "Expected OpenAPI spec file to be created");
//    }
//
//    @Test
//    void generateSpecFiles_ShouldReturnError_WhenFileAlreadyExists() throws IOException {
//        NodeDTO serviceNode = new NodeDTO().id(UUID.randomUUID()).name("DuplicateService").type(NodeDTO.TypeEnum.SERVICE);
//        EventGraphDTO eventGraph = new EventGraphDTO();
//        eventGraph.setNodes(List.of(serviceNode));
//        Files.createFile(tempDir.resolve("DuplicateService_spec.json"));
//        Map<String, String> result = generator.generateSpecFiles(eventGraph);
//        assertTrue(result.containsKey("DuplicateService"), "Expected error message for duplicate file");
//        assertTrue(result.get("DuplicateService").startsWith("File already exists"), "Expected file exists error");
//    }
//
//    @Test
//    void generateSpecFiles_ShouldReturnError_WhenIOExceptionOccurs() throws IOException {
//        OpenAPIFileGenerator spyGenerator = Mockito.spy(new OpenAPIFileGenerator(tempDir.toString()));
//        doThrow(new IOException("Disk error")).when(spyGenerator).prepareDirectory(anyString());
//        NodeDTO serviceNode = new NodeDTO().id(UUID.randomUUID()).name("ServiceWithIOError").type(NodeDTO.TypeEnum.SERVICE);
//        EventGraphDTO eventGraph = new EventGraphDTO();
//        eventGraph.setNodes(List.of(serviceNode));
//        Map<String, String> result = spyGenerator.generateSpecFiles(eventGraph);
//        assertTrue(result.containsKey("directory_error"), "Expected error due to IOException");
//        assertTrue(result.get("directory_error").contains("Error creating directory"), "Expected directory error message");
//    }
//
//
//
//    @Test
//    void generateSpecFiles_ShouldReturnLinks_WhenMultipleServiceNodes() throws IOException {
//        NodeDTO serviceNode1 = new NodeDTO().id(UUID.randomUUID()).name("ServiceA").type(NodeDTO.TypeEnum.SERVICE);
//        NodeDTO serviceNode2 = new NodeDTO().id(UUID.randomUUID()).name("ServiceB").type(NodeDTO.TypeEnum.SERVICE);
//        EventGraphDTO eventGraph = new EventGraphDTO();
//        eventGraph.setNodes(List.of(serviceNode1, serviceNode2));
//        Map<String, String> result = generator.generateSpecFiles(eventGraph);
//        assertTrue(result.containsKey("ServiceA"), "File link for ServiceA should be present");
//        assertTrue(result.containsKey("ServiceB"), "File link for ServiceB should be present");
//        Path generatedFile1 = tempDir.resolve("ServiceA_spec.json");
//        Path generatedFile2 = tempDir.resolve("ServiceB_spec.json");
//        assertTrue(Files.exists(generatedFile1), "Expected OpenAPI spec file for ServiceA to be created");
//        assertTrue(Files.exists(generatedFile2), "Expected OpenAPI spec file for ServiceB to be created");
//    }
//
//    @Test
//    void createSpecFile_ShouldCreateFile_WhenValidServiceNode() throws IOException {
//        Path filePath = tempDir.resolve("ServiceA_spec.json");
//
//        Files.deleteIfExists(filePath);
//
//        Path result = generator.createSpecFile(serviceNode, eventGraph, tempDir.toString());
//
//        assertTrue(Files.exists(result), "Spec file should be created in tempDir");
//        assertEquals(filePath, result, "File path should match the expected path");
//
//        Files.delete(filePath);
//    }
//
//
//    @Test
//    void createSpecFile_ShouldThrowException_WhenNodeNameIsNull() {
//        when(serviceNode.getName()).thenReturn(null);
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            generator.createSpecFile(serviceNode, eventGraph, tempDir.toString());
//        });
//    }
//
//    @Test
//    void createSpecFile_ShouldThrowException_WhenNodeNameIsEmpty() {
//        when(serviceNode.getName()).thenReturn("");
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            generator.createSpecFile(serviceNode, eventGraph, tempDir.toString());
//        });
//    }
//
//
//    @Test
//    void createSpecFile_ShouldThrowException_WhenFileAlreadyExists() throws IOException {
//
//        Path existingFilePath = tempDir.resolve("ServiceA_spec.json");
//
//        Files.createFile(existingFilePath);
//        assertThrows(IOException.class, () -> {
//            generator.createSpecFile(serviceNode, eventGraph, tempDir.toString());
//        });
//
//        Files.delete(existingFilePath);
//    }
//
//
//    @Test
//    void createSpecFile_ShouldThrowException_WhenNodeNameIsNull(@TempDir Path tempDir) {
//        NodeDTO serviceNode = mock(NodeDTO.class);
//        EventGraphDTO eventGraph = mock(EventGraphDTO.class);
//
//        when(serviceNode.getName()).thenReturn(null);
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            generator.createSpecFile(serviceNode, eventGraph, tempDir.toString());
//        });
//    }
//
//    @Test
//    void createSpecFile_ShouldThrowException_WhenFileAlreadyExists(@TempDir Path tempDir) throws IOException {
//        NodeDTO serviceNode = mock(NodeDTO.class);
//        EventGraphDTO eventGraph = mock(EventGraphDTO.class);
//
//        when(serviceNode.getName()).thenReturn("ServiceA");
//        when(serviceNode.getType()).thenReturn(NodeDTO.TypeEnum.SERVICE);
//        when(eventGraph.getNodes()).thenReturn(List.of(serviceNode));
//
//        OpenAPI mockOpenAPI = mock(OpenAPI.class);
//        try (MockedStatic<OpenAPIFileGenerator> generatorMock = Mockito.mockStatic(OpenAPIFileGenerator.class)) {
//            generatorMock.when(() -> OpenAPIFileGenerator.createOpenAPIFromService(any(), any()))
//                    .thenReturn(mockOpenAPI);
//
//            Path existingFile = tempDir.resolve("ServiceA_spec.json");
//            Files.createFile(existingFile);
//
//            assertThrows(FileAlreadyExistsException.class, () -> {
//                generator.createSpecFile(serviceNode, eventGraph, tempDir.toString());
//            });
//
//            Files.delete(existingFile);
//        }
//    }
//
//}