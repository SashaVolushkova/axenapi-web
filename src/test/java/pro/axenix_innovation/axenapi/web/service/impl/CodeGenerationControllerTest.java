package pro.axenix_innovation.axenapi.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pro.axenix_innovation.axenapi.web.api.AxenAPIController;
import pro.axenix_innovation.axenapi.web.generate.CodeGenerator;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.repository.ServiceCodeRepository;
import pro.axenix_innovation.axenapi.web.service.CodeService;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CodeGenerationControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationControllerTest.class);
    @Autowired
    private AxenAPIController axenAPIController;
    @Autowired
    private ServiceCodeRepository serviceCodeRepository;
    @Autowired
    private CodeGenerator codeGenerator;
    @Mock
    private MessageHelper messageHelper;
    private CodeService codeService;

    @BeforeEach
    void setUp() throws Exception {
        codeService = new CodeServiceImpl(serviceCodeRepository, codeGenerator, messageHelper);

        Path jsonFilePath = Paths.get("src/test/resources/results/consume_one_event_service.json");
        if (!Files.exists(jsonFilePath)) {
            throw new IllegalStateException("Test file 'consume_one_event_service.json' not found in resources.");
        }
    }

    @Test
    void generateCodePost_ShouldReturnZipFile() throws Exception {
        Path jsonFilePath = Paths.get("src/test/resources/results/consume_one_event_service.json");
        String eventGraphJson = new String(Files.readAllBytes(jsonFilePath));
        ObjectMapper objectMapper = new ObjectMapper();
        EventGraphDTO eventGraph = objectMapper.readValue(eventGraphJson, EventGraphDTO.class);

        ResponseEntity<Resource> response = axenAPIController.generateCodePost(eventGraph);

        String contentDisposition = response.getHeaders().getContentDisposition().toString();
        log.info("Content-Disposition Header: {}", contentDisposition);

        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code 200 OK");
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType(), "Expected content type to be application/octet-stream");

        assertNotNull(response.getBody(), "Response body should not be null");
        byte[] responseBody = ((ByteArrayResource) response.getBody()).getByteArray();
        assertTrue(responseBody.length > 0, "Generated code in response body should not be empty");

        String filename = response.getHeaders().getContentDisposition().getFilename();
        log.info("Generated file name: {}", filename);
    }

    @Test
    void shouldSetContentDispositionHeaderToArchiveZip() throws Exception {
        Path jsonFilePath = Paths.get("src/test/resources/results/consume_one_event_service.json");
        String eventGraphJson = new String(Files.readAllBytes(jsonFilePath));
        ObjectMapper objectMapper = new ObjectMapper();
        EventGraphDTO eventGraph = objectMapper.readValue(eventGraphJson, EventGraphDTO.class);

        ResponseEntity<Resource> response = axenAPIController.generateCodePost(eventGraph);

        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code 200 OK");

        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertNotNull(contentDisposition, "Content-Disposition header should be present");
        assertTrue(contentDisposition.contains("attachment"),
                "Content-Disposition should contain 'attachment'");
        assertTrue(contentDisposition.contains("archive.zip"),
                "Content-Disposition should contain filename 'archive.zip'");

        assertTrue(contentDisposition.contains("form-data"), "Content-Disposition should indicate form-data");
        assertTrue(contentDisposition.contains("name=\"attachment\""), "Content-Disposition should specify name=attachment");
        assertTrue(contentDisposition.contains("filename=\"archive.zip\""), "Content-Disposition should specify filename=archive.zip");
    }

}
