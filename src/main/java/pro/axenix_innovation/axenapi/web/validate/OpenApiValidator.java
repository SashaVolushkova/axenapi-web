package pro.axenix_innovation.axenapi.web.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_FAIL_EXTRACT_FROM_TOPIC;

public class OpenApiValidator {

    private static final Logger log = LoggerFactory.getLogger(OpenApiValidator.class);

    public static boolean validateOpenApiSpec(MultipartFile specFile) {
            JsonNode rootNode = parseSpec(specFile);

            JsonNode pathsNode = rootNode.path("paths");
            JsonNode componentsNode = rootNode.path("components").path("schemas");

            validateRequiredFields(rootNode);

            Set<String> definedEvents = new HashSet<>();
            Set<String> validTopics = new HashSet<>();
            Set<String> usedTopics = new HashSet<>();

            // 1. Проверяем пути
            validatePaths(pathsNode, componentsNode, definedEvents, validTopics);

            // 2. Проверяем компоненты
            validateComponents(componentsNode, validTopics, usedTopics);

            // 3. Проверяем, что все использованные топики присутствуют в validTopics
            validateUsedTopics(usedTopics, pathsNode, componentsNode);

            // 4. Проверяем уникальность имен событий
            validateUniqueEventNames(componentsNode);

            log.info(MessageHelper.getStaticMessage("axenapi.info.valid.open.api.spec"));
            return true;
    }

    private static JsonNode parseSpec(MultipartFile specFile) {
        try (InputStream inputStream = specFile.getInputStream()) {
            String fileName = specFile.getOriginalFilename();
            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                return new ObjectMapper(new YAMLFactory()).readTree(inputStream);
            } else if (fileName.endsWith(".json")) {
                return new ObjectMapper().readTree(inputStream);
            } else {
                throw new IllegalArgumentException("Файл должен быть в формате YAML или JSON");
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла OpenAPI: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ошибка формата файла: " + e.getMessage(), e);
        }
    }

    private static void validateRequiredFields(JsonNode rootNode) {
        checkField(rootNode, "openapi", requiredFieldMessage("openapi"));
        checkField(rootNode, "info", requiredFieldMessage("info"));

        JsonNode infoNode = rootNode.get("info");
        if (infoNode != null) {
            checkField(infoNode, "title", requiredSubFieldMessage("info", "title"));
            checkField(infoNode, "version", requiredSubFieldMessage("info", "version"));
        }
    }

    private static String requiredFieldMessage(String field) {
        return String.format("Спецификация должна содержать поле '%s'", field);
    }

    private static String requiredSubFieldMessage(String parent, String field) {
        return String.format("Спецификация должна содержать поле '%s' с обязательным полем '%s'", parent, field);
    }

    private static void checkField(JsonNode node, String field, String errorMessage) {
        if (node.get(field) == null) {
            throwValidationException(errorMessage);
        }
    }

    private static void throwValidationException(String message) {
        throw new NullPointerException(message);
    }

    /**
     *
     * @param pathsNode
     * @param componentsNode
     * @param definedEvents
     * @param validTopics
     * @return true - if is event handler, false - if not = simple http
     */
    private static void validatePaths(JsonNode pathsNode, JsonNode componentsNode, Set<String> definedEvents, Set<String> validTopics) {
        pathsNode.fieldNames().forEachRemaining(path -> {


            String[] pathParts = path.split("/");

            if (pathParts.length < 2) {
                log.info(MessageHelper.getStaticMessage("axenapi.info.valid.url.simple.http", path));
                return;
            }

            String broker = pathParts[1];
            String group = null;
            String topic = null;
            String event = null;

            if ("kafka".equals(broker)) {
                boolean hasGroup = pathParts.length > 4;
                group = hasGroup ? pathParts[2] : null;
                topic = pathParts.length > (hasGroup ? 3 : 2) ? pathParts[hasGroup ? 3 : 2] : null;
                event = pathParts.length > (hasGroup ? 4 : 3) ? pathParts[pathParts.length - 1] : null;
            }
            else if ("jms".equals(broker) || "rabbitmq".equals(broker)) {
                topic = pathParts.length > 2 ? pathParts[2] : null;
                event = pathParts.length > 3 ? pathParts[pathParts.length - 1] : null;
            } else {
                log.info(MessageHelper.getStaticMessage("axenapi.info.valid.url.simple.http", path));
                return;
            }
            validatePathFormat(path);

            log.debug("Путь: {}, Группа: {}, Тема: {}, Событие: {}", path, group, topic, event);

            validateEventInPath(event, componentsNode);

            addValidTopicIfNecessary(topic, validTopics);
        });
    }

    private static void validatePathFormat(String path) {
        if (!path.matches("^/(kafka|jms|rabbitmq)(/[a-zA-Z0-9-_]+){2,3}$")) {
            throw new IllegalArgumentException("Неверный формат пути: " + path);
        }
    }

    private static void validateEventInPath(String event, JsonNode componentsNode) {
        if (event == null || event.trim().isEmpty()) {
            throw new IllegalArgumentException("В пути отсутствует название события.");
        }
        // Allow undefined_event as a special case - it doesn't need to be defined in components.schemas
        if ("undefined_event".equals(event)) {
            return;
        }
        if (!componentsNode.has(event)) {
            throw new NullPointerException("Событие " + event + " в пути не найдено в components.schemas");
        }
    }

    private static void addValidTopicIfNecessary(String topic, Set<String> validTopics) {
        if (topic != null && !topic.trim().isEmpty()) {
            validTopics.add(topic);
        }
    }

    private static void validateComponents(JsonNode componentsNode, Set<String> validTopics, Set<String> usedTopics) {
        componentsNode.fieldNames().forEachRemaining(event -> {
            JsonNode eventNode = componentsNode.path(event);

            checkTopics(eventNode.path("x-incoming"), validTopics, usedTopics, "x-incoming");
        });
    }

    private static void checkTopics(JsonNode node, Set<String> validTopics, Set<String> usedTopics, String source) {
        if (!node.isObject()) {
            log.info(MessageHelper.getStaticMessage("axenapi.info.valid.missing.accept", source));
            return;
        }

        JsonNode topicsNode = node.get("topics");
        if (topicsNode == null) {
            logSourceWithoutTopics(source);
            return;
        }

        log.info(MessageHelper.getStaticMessage("axenapi.info.valid.check.topics", source));
        topicsNode.forEach(topicNode -> {
            String topic = null;
            try {
            if (topicNode.isObject()) {
                topic = extractTopicFromObject(topicNode, source);
            } else if (topicNode.isTextual()) {
                topic = topicNode.asText().trim();
            }
            } catch (NullPointerException e) {
                log.warn(MessageHelper.getStaticMessage(WARN_FAIL_EXTRACT_FROM_TOPIC, source, e.getMessage()));
            }


            addTopicToUsedIfNotValid(topic, validTopics, usedTopics);
        });
    }

    private static void logSourceWithoutTopics(String source) { log.info(MessageHelper.getStaticMessage("axenapi.info.valid.present.but.not.contain", source)); }

    private static String extractTopicFromObject(JsonNode topicNode, String source) {
        JsonNode topicNameNode = topicNode.get("name");
        if (topicNameNode == null || topicNameNode.asText().trim().isEmpty()) {
            logSourceWithoutTopics(source);
            throw new NullPointerException("Topic name is missing in source: " + source);
        }
        return topicNameNode.asText().trim();
    }

    // ассерт не использовать (это слово мешает тестам и не поддерживается в сервисах, не предсказуемо, не заменить иф элз)
    private static void addTopicToUsedIfNotValid(String topic, Set<String> validTopics, Set<String> usedTopics) {
        if (!validTopics.contains(topic)) {
            usedTopics.add(topic);
        }
    }

    private static void validateUsedTopics(Set<String> usedTopics, JsonNode pathsNode, JsonNode componentsNode) {
        Set<String> validTopics = new HashSet<>();

        pathsNode.fieldNames().forEachRemaining(path -> {
            JsonNode pathItem = pathsNode.path(path);
            if (pathItem.isObject()) {
                pathItem.fieldNames().forEachRemaining(operation -> {
                    JsonNode operationDetails = pathItem.path(operation);
                    if (operationDetails.isObject()) {
                        JsonNode responses = operationDetails.path("responses");

                        if (responses.has("x-incoming")) {
                            JsonNode incoming = responses.path("x-incoming");
                            if (incoming.has("topics")) {
                                incoming.path("topics").forEach(topicNode -> {
                                    validTopics.add(topicNode.asText().trim());
                                });
                            }
                        }
                    }
                });
            }
        });

        usedTopics.forEach(topic -> {
            if (topic == null || topic.trim().isEmpty()) {
                throw new NullPointerException("Обнаружен пустой топик в x-incoming. Проверьте, что все топики заданы корректно.");
            }
            if (!validTopics.contains(topic) && !isTopicPresentInComponents(topic, componentsNode)) {
                throw new NullPointerException("Топик '" + topic + "' из x-incoming отсутствует в paths.");
            }
        });
    }

    private static boolean isTopicPresentInComponents(String topic, JsonNode componentsNode) {
        for (Iterator<String> it = componentsNode.fieldNames(); it.hasNext(); ) {
            String schemaName = it.next();
            JsonNode schemaNode = componentsNode.path(schemaName);

            if (schemaNode.has("x-incoming")) {
                JsonNode incoming = schemaNode.path("x-incoming");
                if (incoming.has("topics")) {
                    for (JsonNode topicNode : incoming.path("topics")) {
                        if (topic.equals(topicNode.asText().trim())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void validateUniqueEventNames(JsonNode componentsNode) {
        Set<String> eventNamesInComponents = new HashSet<>();

        componentsNode.path("schemas").fieldNames().forEachRemaining(event -> {
            String eventName = event.trim();

            log.info(MessageHelper.getStaticMessage("axenapi.info.valid.process.event", eventName) );

            if (eventNamesInComponents.contains(eventName)) {
                throw new IllegalArgumentException("Событие с именем '" + eventName + "' уже существует в schemas.");
            }

            eventNamesInComponents.add(eventName);
        });
    }



}