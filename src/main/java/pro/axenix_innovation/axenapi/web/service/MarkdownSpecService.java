package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.MarkdownDocumentationCodegen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.axenix_innovation.axenapi.web.generate.SpecificationMarkdownHandler;
import pro.axenix_innovation.axenapi.web.generate.SpecificationMarkdownHandler;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.repository.MarkdownSpecificationRepository;
import pro.axenix_innovation.axenapi.web.util.openapi.generator.OpenApiGeneratorFacade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.*;

@Service
@Slf4j
public class MarkdownSpecService {

    private final SpecificationMarkdownHandler markdownHandler;
    private final MarkdownSpecificationRepository markdownSpecificationRepository;
    private final MessageHelper messageHelper;
    private final MarkdownDocumentationCodegen codegen;



    public MarkdownSpecService(SpecificationMarkdownHandler markdownHandler,
                               MarkdownSpecificationRepository markdownSpecificationRepository,
                               MessageHelper messageHelper) {
        this.markdownHandler = markdownHandler;
        this.markdownSpecificationRepository = markdownSpecificationRepository;
        this.messageHelper = messageHelper;
        codegen = new MarkdownDocumentationCodegen();
    }

    public GenerateMarkdownPost200Response generateFullMarkdown(EventGraphDTO graphDTO) {
        GenerateMarkdownPost200Response response = new GenerateMarkdownPost200Response();

        try {
            Map<String, String> markdownMap = generateMarkdownMap(graphDTO);

            if (markdownMap == null || markdownMap.isEmpty()) {
                log.error(messageHelper.getMessage(ERROR_NO_MD_FILES_GEN));
                return errorResponse(response, "No Markdown files generated.");
            }

            boolean hasErrors = markdownMap.values().stream()
                    .anyMatch(md -> md.toLowerCase().startsWith("error"));

            if (hasErrors) {
                response.setStatus("ERROR");
                response.setMessage("Errors occurred during Markdown generation.");
                response.setDownloadLinks(markdownMap);
                return response;
            }

            response.setStatus("OK");
            response.setDownloadLinks(markdownMap);
            return response;

        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_MD_GEN), e);
            return errorResponse(response, "Unexpected error: " + e.getMessage());
        }
    }

    public Map<String, String> generateMarkdownMap(EventGraphDTO graphDTO) throws Exception {
        if (graphDTO == null) {
            log.error(messageHelper.getMessage(ERROR_INPUT_GRAPHDTO_NULL));
            throw new IllegalArgumentException("Input graphDTO is null");
        }

        log.info(messageHelper.getMessage("axenapi.info.start.spec.gen.graph", graphDTO.getName()));

        Map<String, OpenAPI> openAPISpecifications = OpenApiGeneratorFacade.getOpenAPISpecifications(new EventGraphFacade(graphDTO));

        if (openAPISpecifications == null || openAPISpecifications.isEmpty()) {
            log.error(messageHelper.getMessage(ERROR_NO_YAML_CONTENT));
            throw new IllegalStateException("No OpenAPI specifications generated from graph");
        }

        StringBuilder fullMarkdownContent = new StringBuilder();

        for (Map.Entry<String, OpenAPI> entry : openAPISpecifications.entrySet()) {
            String serviceName = entry.getKey();
            OpenAPI openAPI = entry.getValue();

            ClientOptInput input = new ClientOptInput();
            input.openAPI(openAPI);

            codegen.setUseOneOfInterfaces(true);
            codegen.setLegacyDiscriminatorBehavior(false);
            codegen.additionalProperties().put("hideGenerationTimestamp", false);
            codegen.additionalProperties().put("generateAliasAsModel", true);
            codegen.additionalProperties().put("generateModelDocumentation", true);
            codegen.additionalProperties().put("generateApiDocumentation", true);
            codegen.additionalProperties().put("generateMarkdownDocumentation", true);
            codegen.additionalProperties().put("generateSupportingFiles", true);
            codegen.additionalProperties().put("useTags", true);

            try {
                String templatePath = Objects.requireNonNull(
                        getClass().getClassLoader().getResource("templates/markdown-documentation")
                ).getPath();
                codegen.setTemplateDir(templatePath);
            } catch (NullPointerException e) {
                log.error(messageHelper.getMessage(ERROR_TEMPLATES_MD_NOT_FOUND), e);
            }

            input.config(codegen);

            DefaultGenerator generator = new DefaultGenerator();
            generator.setGenerateMetadata(false);
            generator.setGeneratorPropertyDefault(CodegenConstants.MODELS, "true");
            generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_DOCS, "true");
            generator.setGeneratorPropertyDefault(CodegenConstants.APIS, "true");
            generator.setGeneratorPropertyDefault(CodegenConstants.SUPPORTING_FILES, "true");

            log.info("Start generating markdown content for service: {}", serviceName);

            List<File> generatedFiles = generator.opts(input).generate();

            String serviceTitle = "Сервис: " + serviceName;
            String id = toAnchorId(serviceTitle);
            fullMarkdownContent.append("# ").append(serviceTitle).append(" {#").append(id).append("}\n\n");

            for (File file : generatedFiles) {
                if (file.getName().endsWith(".md")) {
                    try {
                        List<String> lines = Files.readAllLines(file.toPath());
                        for (String line : lines) {
                            fullMarkdownContent.append(line).append(System.lineSeparator());
                        }
                        fullMarkdownContent.append(System.lineSeparator()).append("---").append(System.lineSeparator());
                    } catch (IOException e) {
                        log.warn("Ошибка при чтении сгенерированного файла: " + file.getAbsolutePath(), e);
                    }
                }
            }

            String yamlSpec = Yaml.mapper().writeValueAsString(openAPI);

            String safeYamlSpec = escapeForMarkdownCodeBlock(yamlSpec);

            fullMarkdownContent.append("\n\n### YAML спецификация — ").append(serviceName)
                    .append("\n\n```yaml\n")
                    .append(safeYamlSpec)
                    .append("\n```\n");
        }

        String finalMarkdown = fullMarkdownContent.toString();

        if (finalMarkdown.isBlank()) {
            log.error(messageHelper.getMessage(ERROR_NO_MD_FILES_GEN));
            throw new IllegalStateException("Generated Markdown content is empty");
        }

        Map<String, String> processedMarkdown = markdownHandler.handleMarkdown(Map.of("README.md", finalMarkdown));
        return processedMarkdown;
    }

    private String escapeForMarkdownCodeBlock(String content) {
        if (content == null) return "";

        return content.replace("```", "\\`\\`\\`");
    }

    private String toAnchorId(String title) {
        return title.replaceAll("[^a-zA-Z0-9а-яА-Я]", "-").toLowerCase();
    }

    public static EventGraphDTO filterByServiceUUIDs(EventGraphDTO graph, Set<UUID> serviceUUIDs) {
        log.debug("Filtering EventGraphDTO by service UUIDs: {}", serviceUUIDs);

        Set<UUID> filteredNodeIds = new HashSet<>();

        for (NodeDTO node : graph.getNodes()) {
            if (serviceUUIDs.contains(node.getId())) {
                filteredNodeIds.add(node.getId());
                log.trace("Node {} added directly because its ID is in serviceUUIDs", node.getId());
                continue;
            }
            List<UUID> belongs = node.getBelongsToGraph();
            if (belongs != null) {
                for (UUID belongsToId : belongs) {
                    if (serviceUUIDs.contains(belongsToId)) {
                        filteredNodeIds.add(node.getId());
                        log.trace("Node {} added due to belongsToGraph -> {}", node.getId(), belongsToId);
                        break;
                    }
                }
            }
        }

        List<LinkDTO> filteredLinks = graph.getLinks().stream()
                .filter(link -> filteredNodeIds.contains(link.getFromId()) && filteredNodeIds.contains(link.getToId()))
                .collect(Collectors.toList());

        for (LinkDTO link : filteredLinks) {
            filteredNodeIds.add(link.getFromId());
            filteredNodeIds.add(link.getToId());
        }

        List<NodeDTO> filteredNodes = graph.getNodes().stream()
                .filter(node -> filteredNodeIds.contains(node.getId()))
                .collect(Collectors.toList());

        Set<UUID> usedEventIds = filteredLinks.stream()
                .map(LinkDTO::getEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Проверяем, есть ли HTTP узлы в отфильтрованных узлах
        boolean hasHttpNodeInFilteredNodes = filteredNodes.stream()
                .anyMatch(node -> node.getType() == NodeDTO.TypeEnum.HTTP);

        List<EventDTO> filteredEvents = graph.getEvents().stream()
                .filter(event -> {
                    if (event.getId() == null) return false;
                    
                    // Включаем события, связанные через links
                    if (usedEventIds.contains(event.getId())) return true;
                    
                    // Включаем события с контекстом использования HTTP, если есть HTTP узлы в отфильтрованных узлах
                    if (hasHttpNodeInFilteredNodes && event.getUsageContext() != null && 
                        event.getUsageContext().contains(EventUsageContextEnum.HTTP)) {
                        return true;
                    }
                    
                    // Также включаем события с тегом HTTP, если есть HTTP узлы в отфильтрованных узлах (для обратной совместимости)
                    if (hasHttpNodeInFilteredNodes && event.getTags() != null && event.getTags().contains("HTTP")) {
                        return true;
                    }
                    
                    // Включаем HTTP события по наличию x-http-name в схеме, если есть HTTP узлы в отфильтрованных узлах
                    if (hasHttpNodeInFilteredNodes && event.getSchema() != null && event.getSchema().contains("x-http-name")) {
                        return true;
                    }
                    
                    // Включаем события ти��а REQUEST и RESPONSE, если есть HTTP узлы в отфильтрованных узлах
                    if (hasHttpNodeInFilteredNodes && event.getEventType() != null) {
                        return true;
                    }
                    
                    return false;
                })
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                EventDTO::getId,
                                Function.identity(),
                                (existing, duplicate) -> existing
                        ),
                        map -> {
                            map.values().forEach(event ->
                                    log.debug("Checking event id: {}, included: true", event.getId())
                            );
                            return new ArrayList<>(map.values());
                        }
                ));

        log.debug("Filtered nodes: {}, links: {}, events: {}", filteredNodes.size(), filteredLinks.size(), filteredEvents.size());

        EventGraphDTO result = new EventGraphDTO.Builder()
                .name(graph.getName() + "_filtered")
                .nodes(filteredNodes)
                .links(filteredLinks)
                .events(filteredEvents)
                .errors(graph.getErrors())
                .tags(graph.getTags())
                .build();

        log.debug("Resulting filtered graph name: {}", result.getName());
        return result;
    }

    private GenerateMarkdownPost200Response errorResponse(GenerateMarkdownPost200Response resp, String message) {
        resp.setStatus("ERROR");
        resp.setMessage(message);
        return resp;
    }

    @Transactional(readOnly = true)
    public Optional<String> getMarkdownContentByFileId(String fileId) {
        return markdownSpecificationRepository.findById(fileId)
                .flatMap(markdownSpecification -> {
                    try {
                        Clob clob = markdownSpecification.getMarkdownFile();
                        if (clob == null) return Optional.empty();
                        long length = clob.length();
                        String content = clob.getSubString(1, (int) length);
                        return Optional.of(content);
                    } catch (SQLException e) {
                        return Optional.empty();
                    }
                });
    }
}