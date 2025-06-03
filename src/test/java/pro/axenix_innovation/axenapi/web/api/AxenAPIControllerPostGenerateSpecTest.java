package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessage;
import pro.axenix_innovation.axenapi.web.entity.Specification;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.repository.SpecificationRepository;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;
import pro.axenix_innovation.axenapi.web.validate.EventGraphDTOValidator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_ID_DUPLICATE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_ID_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_NAME_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_EVENT_SCHEMA_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_GRAPH_NAME_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_GRAPH_NULL_NODES;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_LINK_ID_FROM_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_LINK_ID_TO_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_ID_DUPLICATE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_ID_FROM_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_ID_TO_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_IN_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_VALID_NODE_NAME_NULL;


@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostGenerateSpecTest {

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageHelper messageHelper;

    private static final Logger logger = LoggerFactory.getLogger(AxenAPIControllerPostGenerateSpecTest.class);

    private void testEventGraph(String caseName, String expectedErrorMessage) throws Exception {
        Resource fileResource = new ClassPathResource(caseName + ".json");
        assertNotNull(fileResource);

        String fileContent = new String(Files.readAllBytes(fileResource.getFile().toPath()), StandardCharsets.UTF_8);
        EventGraphDTO eventGraph = objectMapper.readValue(fileContent, EventGraphDTO.class);

        logger.info("EventGraphDTO before validation: {}", eventGraph);

        AppCodeMessage validationResult = EventGraphDTOValidator.validateEventGraph(eventGraph);

        if (validationResult == null) {
            logger.info("Validation passed successfully.");
        } else {
            logger.error("Validation error: {}", validationResult);

            logger.info("Expected error message: {}", expectedErrorMessage);
            logger.info("Actual error message: {}", messageHelper.getMessage(validationResult.getEnumItem().getMessageKey(),
                    validationResult.getArgs()));

            assertEquals(expectedErrorMessage, messageHelper.getMessage(validationResult.getEnumItem().getMessageKey(),
                            validationResult.getArgs()),
                    "Ошибка валидации не совпадает с ожидаемой. Ожидалось: " + expectedErrorMessage + " но получено: " + validationResult);
        }
    }


    @Test
    public void testGenerateSpecPostWithServiceNoCommonConsumeTopics() throws Exception {
        Resource fileResource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        assertNotNull(fileResource, "Файл service_no_common_consume_topics_common_events_common_events_common_outgoing_topics_2.json не найден!");

        String fileContent = new String(Files.readAllBytes(fileResource.getFile().toPath()), StandardCharsets.UTF_8);

        MvcResult result = mockMvc.perform(post("/generateSpec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileContent))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").isNotEmpty())
                .andExpect(jsonPath("$.downloadLinks.*").exists())
                .andExpect(jsonPath("$.downloadLinks.*", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        logger.info("Full JSON response:\n{}", jsonResponse);
    }

    @Test
    public void testGenerateSpecPostWithServiceNoCommonConsumeTopics_YAML() throws Exception {
        Resource fileResource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        assertNotNull(fileResource, "Файл service_no_common_consume_topics_common_events_common_outgoing_topics_2.json не найден!");

        String fileContent = new String(Files.readAllBytes(fileResource.getFile().toPath()), StandardCharsets.UTF_8);

        MvcResult result = mockMvc.perform(post("/generateSpec?format=yaml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileContent))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").isNotEmpty())
                .andExpect(jsonPath("$.downloadLinks.*").exists())
                .andExpect(jsonPath("$.downloadLinks.*", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.downloadLinks.*", Matchers.everyItem(Matchers.endsWith(".yaml")))) // Проверяем окончание на .yaml
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        logger.info("Full JSON response (YAML format):\n{}", jsonResponse);
    }

    @Test
    public void testGenerateSpecPostWithServiceNoCommonConsumeTopics_YAML1() throws Exception {
        Resource fileResource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        assertNotNull(fileResource, "Файл service_no_common_consume_topics_common_events_common_outgoing_topics_2.json не найден!");

        String fileContent = new String(Files.readAllBytes(fileResource.getFile().toPath()), StandardCharsets.UTF_8);

        MvcResult result = mockMvc.perform(post("/generateSpec?format=yaml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").exists())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        logger.info("Full JSON response (YAML format):\n{}", jsonResponse);

        JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);
        JsonNode downloadLinksNode = rootNode.path("downloadLinks");

        assertTrue(downloadLinksNode.isObject() && downloadLinksNode.size() > 0, "downloadLinks должен содержать хотя бы одну ссылку");

        Iterator<JsonNode> values = downloadLinksNode.elements();
        assertTrue(values.hasNext(), "downloadLinks должен содержать хотя бы одну ссылку");
        String yamlDownloadUrl = values.next().asText();
        logger.info("Download URL: {}", yamlDownloadUrl);

        mockMvc.perform(get(yamlDownloadUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/x-yaml"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".yaml")))
                .andExpect(content().string(Matchers.not(Matchers.isEmptyOrNullString())));
    }

    @Test
    public void testGenerateSpecPostWithServiceNoCommonConsumeTopics_JSON() throws Exception {
        Resource fileResource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        assertNotNull(fileResource, "Файл service_no_common_consume_topics_common_events_common_outgoing_topics_2.json не найден!");

        String fileContent = new String(Files.readAllBytes(fileResource.getFile().toPath()), StandardCharsets.UTF_8);

        MvcResult result = mockMvc.perform(post("/generateSpec?format=json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").exists())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        logger.info("Full JSON response (JSON format):\n{}", jsonResponse);

        JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);
        JsonNode downloadLinksNode = rootNode.path("downloadLinks");

        assertTrue(downloadLinksNode.isObject() && downloadLinksNode.size() > 0, "downloadLinks должен содержать хотя бы одну ссылку");

        Iterator<JsonNode> values = downloadLinksNode.elements();
        assertTrue(values.hasNext(), "downloadLinks должен содержать хотя бы одну ссылку");
        String jsonDownloadUrl = values.next().asText();
        logger.info("Download URL: {}", jsonDownloadUrl);

        mockMvc.perform(get(jsonDownloadUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".json")))
                .andExpect(content().string(Matchers.not(Matchers.isEmptyOrNullString())));
    }

    @Test
    void shouldAddServiceAndGenerateSpecContainingHttpInfoFromGeneratedFile() throws Exception {
        String jsonSpec = Files.readString(
                Paths.get("src/test/resources/specs/json/consume_one_event_service_with_http.json"),
                StandardCharsets.UTF_8
        );

        ObjectMapper mapper = new ObjectMapper();
        EventGraphDTO emptyGraph = new EventGraphDTO();
        String emptyGraphJson = mapper.writeValueAsString(emptyGraph);

        MockMultipartFile specFile = new MockMultipartFile(
                "files",
                "spec.json",
                MediaType.APPLICATION_JSON_VALUE,
                jsonSpec.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile graphPart = new MockMultipartFile(
                "eventGraph",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                emptyGraphJson.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult addResult = mockMvc.perform(multipart("/addServiceToGraph")
                        .file(specFile)
                        .file(graphPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String updatedGraphJson = addResult.getResponse().getContentAsString();

        MvcResult genResult = mockMvc.perform(post("/generateSpec?format=json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedGraphJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").exists())
                .andReturn();

        String responseJson = genResult.getResponse().getContentAsString();
        JsonNode jsonNode = mapper.readTree(responseJson);

        JsonNode downloadLinksNode = jsonNode.get("downloadLinks");
        assertTrue(downloadLinksNode.isObject() && downloadLinksNode.size() > 0, "downloadLinks должен содержать хотя бы одну ссылку");

        Iterator<JsonNode> values = downloadLinksNode.elements();
        assertTrue(values.hasNext(), "downloadLinks должен содержать хотя бы одну ссылку");
        String link = values.next().asText();

        MvcResult fileResult = mockMvc.perform(get(link))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".json")))
                .andExpect(content().string(Matchers.not(Matchers.isEmptyOrNullString())))
                .andReturn();

        String fileContent = fileResult.getResponse().getContentAsString();

        Path outputPath = Paths.get("src/test/resources/httpinf/generatedSpec.json");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, fileContent, StandardCharsets.UTF_8);
    }

    @Test
    public void testGenerateAndDownloadSpecification() throws Exception {
        Resource fileResource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        assertNotNull(fileResource, "Файл не найден!");

        String fileContent = new String(Files.readAllBytes(fileResource.getFile().toPath()), StandardCharsets.UTF_8);

        MvcResult generateResult = mockMvc.perform(post("/generateSpec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").isNotEmpty())
                .andExpect(jsonPath("$.downloadLinks.*", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andReturn();

        String jsonResponse = generateResult.getResponse().getContentAsString();
        logger.info("POST /generateSpec response: {}", jsonResponse);

        JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);
        String downloadUrl = rootNode.path("downloadLinks").elements().next().asText();
        logger.info("Download URL: {}", downloadUrl);

        String fileId = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1, downloadUrl.lastIndexOf(".json"));
        logger.info("Extracted fileId: {}", fileId);

        Specification specification = specificationRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Specification not found in DB"));

        Clob specClob = specification.getSpecFile();
        String expectedJson = specClob.getSubString(1, (int) specClob.length());
        logger.info("Expected JSON from DB (ID: {}): {}", fileId, expectedJson);

        mockMvc.perform(get(downloadUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + fileId + ".json\""))
                .andExpect(content().string(expectedJson));
    }

    @Test
    void testGenerateSpecPost_LinkWithoutEventId_ShouldReturn400() throws Exception {
        String jsonGraph = Files.readString(Path.of("src/test/resources/validate/graph/no_event_in_link.json"));

        mockMvc.perform(post("/generateSpec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("format", "json")
                        .content(jsonGraph))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("eventId")))
                .andDo(print());
    }

    private void runFullSpecTest(String resourcePath) throws Exception {
        String filename = Paths.get(resourcePath).getFileName().toString();

        MockMultipartFile filePart = loadSpecFile(resourcePath, filename);

        String emptyGraphJson = "{}";
        MockMultipartFile eventGraphPart = new MockMultipartFile(
                "eventGraph",
                "eventGraph.json",
                MediaType.APPLICATION_JSON_VALUE,
                emptyGraphJson.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult addServiceResult = mockMvc.perform(multipart("/addServiceToGraph")
                        .file(filePart)
                        .file(eventGraphPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String updatedGraphJson = addServiceResult.getResponse().getContentAsString();

        MvcResult generateResult = mockMvc.perform(post("/generateSpec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedGraphJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.downloadLinks").isNotEmpty())
                .andReturn();

        String jsonResponse = generateResult.getResponse().getContentAsString();

        JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);
        String downloadUrl = rootNode.path("downloadLinks").elements().next().asText();

        int lastSlash = downloadUrl.lastIndexOf("/");
        int lastDot = downloadUrl.lastIndexOf(".");
        if (lastDot == -1 || lastDot < lastSlash) {
            lastDot = downloadUrl.length();
        }
        String fileId = downloadUrl.substring(lastSlash + 1, lastDot);

        Specification specification = specificationRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Specification not found in DB"));

        Clob specClob = specification.getSpecFile();
        String expectedJson = specClob.getSubString(1, (int) specClob.length());

        Path outputPath = Paths.get("src/test/resources/validate", fileId + ".json");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, expectedJson, StandardCharsets.UTF_8);

        try (InputStream originalInputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(originalInputStream, "Оригинальный файл не найден: " + resourcePath);
            String originalJson = new String(originalInputStream.readAllBytes(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode originalNode = mapper.readTree(originalJson);
            JsonNode generatedNode = mapper.readTree(expectedJson);

            if (!originalNode.equals(generatedNode)) {
                List<String> diffs = new ArrayList<>();
                compareJsonNodes("", originalNode, generatedNode, diffs);

                StringBuilder diffOutput = new StringBuilder("Содержимое сгенерированного файла не совпадает с исходным JSON:\n");
                for (String diff : diffs) {
                    diffOutput.append(diff).append("\n");
                }

                Assertions.fail(diffOutput.toString());
            }
        } finally {
            if (Files.exists(outputPath)) {
                Files.delete(outputPath);
            }
        }
    }

    private MockMultipartFile loadSpecFile(String resourcePath, String filename) throws IOException {
        InputStream fileInputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(fileInputStream, "Файл не найден: " + resourcePath);

        return new MockMultipartFile(
                "files",
                filename,
                MediaType.APPLICATION_JSON_VALUE,
                fileInputStream
        );
    }



    @Test
    public void testConsumeOneEventServiceHttp() throws Exception {
        runFullSpecTest("specs/json/consume_one_event_service_with_http.json");
    }

    @Test
    public void testConsumeOneEventService() throws Exception {
        runFullSpecTest("specs/json/consume_one_event_service.json");
    }

    @Test
    public void testConsume_three_events_from_different_brokers_service() throws Exception {
        runFullSpecTest("specs/json/consume_three_events_from_different_brokers_service.json");
    }

    @Test
    public void testConsumeOneEventServiceXDocumentationFileLinks() throws Exception {
        runFullSpecTest("x-documentation-file-links/consume_one_event_service.json");
    }



    private void compareJsonNodes(String path, JsonNode expected, JsonNode actual, List<String> diffs) {
        if (expected == null && actual != null) {
            diffs.add(path + ": expected null, found " + actual);
            return;
        }
        if (expected != null && actual == null) {
            diffs.add(path + ": expected " + expected + ", found null");
            return;
        }
        if (!expected.getNodeType().equals(actual.getNodeType())) {
            diffs.add(path + ": node type mismatch. Expected " + expected.getNodeType() + ", found " + actual.getNodeType());
            return;
        }

        switch (expected.getNodeType()) {
            case OBJECT:
                Iterator<String> fieldNames = expected.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    String currentPath = path + "/" + fieldName;
                    compareJsonNodes(currentPath, expected.get(fieldName), actual.get(fieldName), diffs);
                }

                Iterator<String> actualFields = actual.fieldNames();
                while (actualFields.hasNext()) {
                    String fieldName = actualFields.next();
                    if (!expected.has(fieldName)) {
                        diffs.add(path + ": unexpected field in actual: " + fieldName);
                    }
                }
                break;

            case ARRAY:
                if (expected.size() != actual.size()) {
                    diffs.add(path + ": array size mismatch. Expected " + expected.size() + ", found " + actual.size());
                } else {
                    for (int i = 0; i < expected.size(); i++) {
                        compareJsonNodes(path + "[" + i + "]", expected.get(i), actual.get(i), diffs);
                    }
                }
                break;

            default:
                if (!expected.equals(actual)) {
                    diffs.add(path + ": expected " + expected + ", found " + actual);
                }
        }
    }





    @Test
    public void testGenerateSpecPostWithValidGraph1() throws Exception {
        testEventGraph("results/consume_one_event_service",  null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph2() throws Exception {
        testEventGraph("results/consume_one_event_service1",  null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph3() throws Exception {
        testEventGraph("results/consume_one_event_service_with_http",  null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph4() throws Exception {
        testEventGraph("results/consume_three_events_from_different_brokers_service",  null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph5() throws Exception {
        testEventGraph("results/consume_three_events_from_different_brokers_service_no_group",  null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph6() throws Exception {
        testEventGraph("results/consume_three_events_from_different_brokers_service_no_x_incoming",  null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph7() throws Exception {
        testEventGraph("results/consume_two_events_in_one_topic", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph8() throws Exception {
        testEventGraph("results/consume_two_events_in_one_topic_no_group", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph9() throws Exception {
        testEventGraph("results/empty_at_all", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph10() throws Exception {
        testEventGraph("results/empty_service", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph11() throws Exception {
        testEventGraph("results/no_consumes_one_outgoing_no_broker_type_service", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph12() throws Exception {
        testEventGraph("results/no_consumes_outgoing_with_broker_type_service_same_name", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph13() throws Exception {
        testEventGraph("results/service_no_common_consume_topics_common_events_common_outgoing_topics", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph14() throws Exception {
        testEventGraph("results/service_no_common_consume_topics_common_events_common_outgoing_topics_1", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph15() throws Exception {
        testEventGraph("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph16() throws Exception {
        testEventGraph("results/service_no_common_consume_topics_common_events_common_outgoing_topics_with_tags", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph17() throws Exception {
        testEventGraph("addresult/consume_one_event_service&consume_one_event_service", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph18() throws Exception {
        testEventGraph("addresult/consume_one_event_service&consume_three_events_from_different_brokers_service&empty_service&no_consumes_one_outgoing_no_broker_type_service", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph19() throws Exception {
        testEventGraph("addresult/consume_one_event_service&service_no_common_consume_topics_common_events_common_outgoing_topics_2", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph20() throws Exception {
        testEventGraph("addresult/consume_three_events_from_different_brokers_service&service_no_common_consume_topics_common_events_common_outgoing_topics_1", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph21() throws Exception {
        testEventGraph("addresult/empty_at_all&consume_two_events_in_one_topic_no_group", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph22() throws Exception {
        testEventGraph("addresult/empty_service&empty_service", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph23() throws Exception {
        testEventGraph("addresult/no_consumes_outgoing_with_broker_type_service_same_name&no_consumes_outgoing_with_broker_type_service_same_name", null);
    }

    @Test
    public void testGenerateSpecPostWithValidGraph24() throws Exception {
        testEventGraph("validate/graph/graph_null_node", messageHelper.getMessage(RESP_ERROR_VALID_GRAPH_NULL_NODES.getMessageKey()));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph25() throws Exception {
        testEventGraph("validate/graph/graph_null_name", messageHelper.getMessage(RESP_ERROR_VALID_GRAPH_NAME_NULL.getMessageKey()));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph26() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_null_node_name", messageHelper.getMessage(RESP_ERROR_VALID_NODE_NAME_NULL.getMessageKey(), "a1b2c3d4-e5f6-7890-1234-567890abcdef"));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph27() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_null_node_id", messageHelper.getMessage(RESP_ERROR_VALID_NODE_IN_NULL.getMessageKey()));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph28() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_dublicate_node_id", messageHelper.getMessage(RESP_ERROR_VALID_NODE_ID_DUPLICATE.getMessageKey(), "00f3b3ca-b815-408c-afdc-b7ebd9ae2202"));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph29() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_event_no_id", messageHelper.getMessage(RESP_ERROR_VALID_EVENT_ID_NULL.getMessageKey()));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph30() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_dublicate_event_id", messageHelper.getMessage(RESP_ERROR_VALID_EVENT_ID_DUPLICATE.getMessageKey(), "c3d4e5f6-7890-1234-5678-90abcdef0123"));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph31() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_event_no_name", messageHelper.getMessage(RESP_ERROR_VALID_EVENT_NAME_NULL.getMessageKey(), "c3d4e5f6-7890-1234-5678-90abcdef0123"));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph32() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_event_no_shema", messageHelper.getMessage(RESP_ERROR_VALID_EVENT_SCHEMA_NULL.getMessageKey(), "430c8a18-dc63-4e09-bb91-47d5cabcc562"));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph33() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_missing_link_from", messageHelper.getMessage(RESP_ERROR_VALID_LINK_ID_FROM_NULL.getMessageKey()));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph34() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_missing_link_to", messageHelper.getMessage(RESP_ERROR_VALID_LINK_ID_TO_NULL.getMessageKey()));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph35() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_invalid_link_from", messageHelper.getMessage(RESP_ERROR_VALID_NODE_ID_FROM_NULL.getMessageKey(), "0d3b2d62-ea2e-443f-9133-ab223454ff31"));
    }

    @Test
    public void testGenerateSpecPostWithValidGraph36() throws Exception {
        testEventGraph("validate/graph/consume_one_event_service_with_invalid_link_to", messageHelper.getMessage(RESP_ERROR_VALID_NODE_ID_TO_NULL.getMessageKey(), "a1b2c3d4-e5f6-7890-1234-567890abcde4"));
    }

    @Test
    void testGenerateDocx_withParsedEventGraphDTO() throws Exception {
        String graphJson = """
    {
      "name": "eeaf818b-4def-4c4d-8579-5b6889601491",
      "nodes": [
        {
          "id": "06beeae9-be4c-45fe-800c-0ec1030efd4b",
          "name": "Service",
          "type": "SERVICE",
          "belongsToGraph": [],
          "brokerType": null
        },
        {
          "id": "0b9bec06-688f-4e0a-9165-beea064bb88b",
          "name": "Service",
          "type": "SERVICE",
          "belongsToGraph": [],
          "brokerType": null
        },
        {
          "id": "9c9edebb-803b-4900-8481-b8e4cc28a19c",
          "name": "Topic",
          "type": "TOPIC",
          "belongsToGraph": [],
          "brokerType": null
        },
        {
          "id": "13599a31-eaea-4d3b-86df-02108192a103",
          "name": "Topic",
          "type": "TOPIC",
          "belongsToGraph": [],
          "brokerType": null
        }
      ],
      "events": [
        {
          "id": "093a6d51-1b7f-48ad-8ee8-c9f1dea7e553",
          "name": "1",
          "schema": "{}",
          "eventDescription": "",
          "tags": []
        },
        {
          "id": "bb52c489-f748-41d6-aded-a1f35f0bc40c",
          "name": "2",
          "schema": "{}",
          "eventDescription": "",
          "tags": []
        }
      ],
      "links": [
        {
          "id": "b5cb5a23-339e-4d62-9694-22b3f4adccfe",
          "fromId": "06beeae9-be4c-45fe-800c-0ec1030efd4b",
          "toId": "9c9edebb-803b-4900-8481-b8e4cc28a19c",
          "eventId": "093a6d51-1b7f-48ad-8ee8-c9f1dea7e553"
        },
        {
          "id": "e6db23b6-8ad5-4501-a2d5-cb617a833d67",
          "fromId": "0b9bec06-688f-4e0a-9165-beea064bb88b",
          "toId": "13599a31-eaea-4d3b-86df-02108192a103",
          "eventId": "bb52c489-f748-41d6-aded-a1f35f0bc40c"
        }
      ],
      "errors": [],
      "tags": []
    }
    """;

        MvcResult result = mockMvc.perform(post("/generateDocx")
                        .param("serviceIds", "06beeae9-be4c-45fe-800c-0ec1030efd4b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode downloadLinksNode = root.get("downloadLinks");

        assertNotNull(downloadLinksNode, "Ответ не содержит 'downloadLinks'");
        Iterator<String> keys = downloadLinksNode.fieldNames();
        assertTrue(keys.hasNext(), "downloadLinks пуст");

        String fileName = keys.next();
        String downloadPath = downloadLinksNode.get(fileName).asText();

        String fileId = downloadPath.replaceAll(".*/download/docx/(.*)\\.docx", "$1");
        assertFalse(fileId.isBlank(), "fileId не должен быть пустым");

        MvcResult downloadResult = mockMvc.perform(get("/download/docx/" + fileId + ".docx"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] fileBytes = downloadResult.getResponse().getContentAsByteArray();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry, "DOCX-файл должен быть валидным ZIP-архивом");
        }

        File outFile = new File("build/generated/DOCX/" + fileId + ".docx");
        outFile.getParentFile().mkdirs();
        Files.write(outFile.toPath(), fileBytes);

        logger.info("DOCX файл успешно сохранён: " + outFile.getAbsolutePath());
    }

    @Test
    void testGenerateDocx_withParsedEventGraphDTO1() throws Exception {
        ClassPathResource resource = new ClassPathResource("results/consume_one_event_service.json");
        String graphJson = Files.readString(resource.getFile().toPath());

        MvcResult result = mockMvc.perform(post("/generateDocx")
                        .param("serviceIds", "a1b2c3d4-e5f6-7890-1234-567890abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode downloadLinksNode = root.get("downloadLinks");

        assertNotNull(downloadLinksNode, "Ответ не содержит 'downloadLinks'");
        Iterator<String> keys = downloadLinksNode.fieldNames();
        assertTrue(keys.hasNext(), "downloadLinks пуст");

        String fileName = keys.next();
        String downloadPath = downloadLinksNode.get(fileName).asText();

        String fileId = downloadPath.replaceAll(".*/download/docx/(.*)\\.docx", "$1");
        assertFalse(fileId.isBlank(), "fileId не должен быть пустым");

        MvcResult downloadResult = mockMvc.perform(get("/download/docx/" + fileId + ".docx"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] fileBytes = downloadResult.getResponse().getContentAsByteArray();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry, "DOCX-файл должен быть валидным ZIP-архивом");
        }

        File outFile = new File("build/generated/DOCX/" + fileId + ".docx");
        outFile.getParentFile().mkdirs();
        Files.write(outFile.toPath(), fileBytes);

        logger.info("DOCX файл успешно сохранён: " + outFile.getAbsolutePath());
    }

}
