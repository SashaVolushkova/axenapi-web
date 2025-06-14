package pro.axenix_innovation.axenapi.web.validate;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiValidatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static MockMultipartFile createMultipartFileFromFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return new MockMultipartFile(
                    "file",
                    file.getName(),
                    "application/json",
                    inputStream
            );
        }
    }

    @Test
    void testValidOpenApiSpecTestServiceJson() throws IOException {
        Path filePath = Path.of("src/test/resources/validate/test_service.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecTestServiceYaml() throws IOException {
        Path filePath = Path.of("src/test/resources/validate/test_service.yaml");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecComplexServiceYaml() throws IOException {
        Path filePath = Path.of("src/test/resources/validate/complex_service.yaml");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecComplexServiceJson() throws IOException {
        Path filePath = Path.of("src/test/resources/validate/complex_service.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecConsumeOneEventServiceJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/consume_one_event_service.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecConsumeThreeEventsFromDifferentBrockersServiceJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/consume_three_events_from_different_brokers_service.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecConsumeThreeEventsFromDifferentBrockersServiceNoGroupJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/consume_three_events_from_different_brokers_service_no_group.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecTwoEventsInTopicJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/consume_two_events_in_one_topic.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecEmptyJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/empty_service.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecTag2Json() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/service_no_common_consume_topics_common_events_common_outgoing_topics_with_tags_2.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecTag1Json() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/service_no_common_consume_topics_common_events_common_outgoing_topics_with_tags_1.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecTopic2Json() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecTopic1Json() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/service_no_common_consume_topics_common_events_common_outgoing_topics_1.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecSameNameJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/no_consumes_outgoing_with_broker_type_service_same_name.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testValidOpenApiSpecNoBrockerTypeJson() throws IOException {
        Path filePath = Path.of("src/test/resources/specs/json/no_consumes_one_outgoing_no_broker_type_service.json");
        File validSpecFile = filePath.toFile();
        assertTrue(validSpecFile.exists(), "Файл " + filePath + " не найден. Убедись, что он есть в проекте!");
        MockMultipartFile multipartFile = createMultipartFileFromFile(validSpecFile);
        assertDoesNotThrow(() -> OpenApiValidator.validateOpenApiSpec(multipartFile),
                "Файл должен пройти валидацию без исключений");
    }

    @Test
    void testParseSpecWithInvalidFileFormatShouldThrowException() throws Exception {
        // Загружаем файл с некорректным форматом (например, текстовый файл)
        File invalidSpecFile = new File("src/test/resources/validate/invalid.txt");
        // Проверяем, что файл существует
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Ошибка формата файла: Файл должен быть в формате YAML или JSON", exception.getMessage());

    }

    @Test
    void testValidateRequiredFieldsWithMissingOpenApiField(){
        File invalidSpecFile = Path.of("src/test/resources/validate/missing_openapi_field.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(NullPointerException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Спецификация должна содержать поле 'openapi'", exception.getMessage());
    }

    @Test
    void testValidateRequiredFieldsWithMissingInfoField(){
        File invalidSpecFile = Path.of("src/test/resources/validate/missing_info_field.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(NullPointerException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Спецификация должна содержать поле 'info'", exception.getMessage());
    }

    @Test
    void testValidateRequiredFieldsWithMissingTitleField(){
        File invalidSpecFile = Path.of("src/test/resources/validate/missing_title_field.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(NullPointerException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Спецификация должна содержать поле 'info' с обязательным полем 'title'", exception.getMessage());
    }

    @Test
    void testValidateRequiredFieldsWithMissingVersionField(){
        File invalidSpecFile = Path.of("src/test/resources/validate/missing_version_field.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(NullPointerException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Спецификация должна содержать поле 'info' с обязательным полем 'version'", exception.getMessage());
    }

    @Test
    void testValidatePathWithMissingGroupInKafkaPath() {
        File invalidSpecFile = Path.of("src/test/resources/validate/kafka_missing_group_path.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Неверный формат пути: /kafka//group1/topic1/Event1", exception.getMessage());
    }

    @Test
    void testValidatePathWithInvalidFormat() {
        File invalidSpecFile = Path.of("src/test/resources/validate/invalid_path_format.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Неверный формат пути: /kafka/group1/topic1/Event1/mmmmm", exception.getMessage());
    }

    @Test
    void testValidatePathWithMissingEvent() {
        File invalidSpecFile = Path.of("src/test/resources/validate/missing_event_in_path.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(NullPointerException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Событие Event в пути не найдено в components.schemas", exception.getMessage());
    }

    @Test
    void testValidatePathWithNonExistentEventInComponents() {
        File invalidSpecFile = Path.of("src/test/resources/validate/non_existent_event_in_components.json").toFile();
        assertTrue(invalidSpecFile.exists(), "Файл не найден: " + invalidSpecFile.getPath());

        Exception exception = assertThrows(NullPointerException.class,
                () -> OpenApiValidator.validateOpenApiSpec(createMultipartFileFromFile(invalidSpecFile)));

        assertEquals("Событие Event2 в пути не найдено в components.schemas", exception.getMessage());
    }
    @Test
    public void testContainsNode_NodeExists_ReturnsTrue() {
        UUID nodeId = UUID.randomUUID();

        NodeDTO node = new NodeDTO();
        node.setId(nodeId);

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(node));

        boolean result = CalculateAllPathsValidator.containsNode(graph, nodeId);

        assertTrue(result, "Ожидалось, что метод вернет true, так как узел с заданным ID присутствует в графе");
    }

}