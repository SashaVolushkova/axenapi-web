package pro.axenix_innovation.axenapi.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllServicePdfGenerationService {

    private final MarkdownSpecService markdownSpecService;
    private final ConvertMdPdfDocumentService convertMdPdfDocumentService;

    public AllServicePdf generateAllServicesPDF(EventGraphDTO fullGraph) throws IOException {
        if (fullGraph == null || fullGraph.getNodes() == null || fullGraph.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Invalid EventGraph structure: nodes are empty");
        }

        List<NodeDTO> allServices = fullGraph.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .toList();

        if (allServices.isEmpty()) {
            log.warn("Нет сервисов для генерации.");
            return new AllServicePdf(Collections.emptyMap(), new byte[0]);
        }

        Map<String, String> individualMdFiles = new LinkedHashMap<>();
        List<String> tocLines = new ArrayList<>();
        List<String> combinedMdSections = new ArrayList<>();
        int index = 1;

        for (NodeDTO service : allServices) {
            log.info("Генерация Markdown для сервиса: {}", service.getName());

            EventGraphDTO subGraph = MarkdownSpecService.filterByServiceUUIDs(fullGraph, Set.of(service.getId()));

            Map<String, String> markdownMap;
            try {
                markdownMap = markdownSpecService.generateMarkdownMap(subGraph);
            } catch (Exception e) {
                log.error("Ошибка генерации Markdown для сервиса {}", service.getName(), e);
                continue;
            }

            if (markdownMap == null || markdownMap.isEmpty()) {
                log.warn("Markdown не сгенерирован для сервиса {}", service.getName());
                continue;
            }

            Map<String, String> realContentMap = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : markdownMap.entrySet()) {
                String fileId = extractFileId(entry.getValue());
                Optional<String> contentOpt = markdownSpecService.getMarkdownContentByFileId(fileId);
                contentOpt.ifPresentOrElse(
                        content -> realContentMap.put(entry.getKey(), content),
                        () -> log.warn("Markdown контент не найден по fileId: {}", fileId)
                );
            }

            if (realContentMap.isEmpty()) {
                log.warn("Не удалось получить содержимое Markdown для сервиса {}", service.getName());
                continue;
            }

            for (Map.Entry<String, String> entry : realContentMap.entrySet()) {
                String filename = sanitizeFilename(service.getName()) + "_" + entry.getKey();
                individualMdFiles.put(filename, entry.getValue());
            }

            String anchor = sanitizeAnchor(service.getName());
            tocLines.add(String.format("%d. [%s](#%s)", index++, service.getName(), anchor));

            StringBuilder serviceMd = new StringBuilder();
            serviceMd.append("## ").append(service.getName()).append("\n\n");

            List<String> generalSections = new ArrayList<>();
            List<String> yamlSections = new ArrayList<>();

            for (Map.Entry<String, String> entry : realContentMap.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();

                String fixedContent = rewriteLinksToInternalAnchors(content, service.getName());

                if (isYamlFile(filename)) {
                    String specSection = "### Спецификация сервиса " + service.getName() + "\n\n" +
                            "```yaml\n" + fixedContent.trim() + "\n```\n";
                    yamlSections.add(specSection);
                } else {
                    generalSections.add(fixedContent);
                }
            }

            generalSections.forEach(section -> serviceMd.append(section).append("\n\n"));
            yamlSections.forEach(section -> serviceMd.append(section).append("\n\n"));

            serviceMd.append("<div style=\"page-break-before: always;\"></div>\n");
            combinedMdSections.add(serviceMd.toString());
        }

        if (individualMdFiles.isEmpty()) {
            log.warn("Не удалось сгенерировать ни одного Markdown файла.");
            return new AllServicePdf(Collections.emptyMap(), new byte[0]);
        }

        StringBuilder combinedMd = new StringBuilder();
        combinedMd.append("# Оглавление\n\n");
        tocLines.forEach(line -> combinedMd.append(line).append("\n"));
        combinedMd.append("\n---\n\n");
        combinedMdSections.forEach(section -> combinedMd.append(section).append("\n\n"));

        byte[] pdfBytes = convertMdPdfDocumentService.convertMdToPdf(List.of(combinedMd.toString()));
        log.info("PDF с полным содержанием всех сервисов успешно сгенерирован, размер (байт): {}", pdfBytes.length);

        return new AllServicePdf(individualMdFiles, pdfBytes);
    }

    private boolean isYamlFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.contains("yaml");
    }

    private String rewriteLinksToInternalAnchors(String markdownContent, String serviceName) {
        if (markdownContent == null) return "";

        String sanitizedService = sanitizeAnchor(serviceName);

        Pattern pattern = Pattern.compile("\\[(.+?)\\]\\(([^)]+\\.md)(#[^)]+)?\\)");
        Matcher matcher = pattern.matcher(markdownContent);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String text = matcher.group(1);
            String filePart = matcher.group(2);
            String anchorPart = matcher.group(3);

            String fileBase = filePart.replace(".md", "").toLowerCase().replaceAll("[^a-z0-9\\-]", "-");

            String newAnchor = "#" + sanitizedService + "-" + fileBase;
            if (anchorPart != null) {
                String anchorClean = anchorPart.substring(1).toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
                newAnchor += "-" + anchorClean;
            }

            String replacement = "[" + text + "](" + newAnchor + ")";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String extractFileId(String url) {
        if (url == null) return "";
        return url.replace("/download/markdown/", "")
                .replace("/download/yaml/", "")
                .replace(".md", "")
                .replace(".yml", "")
                .replace(".yaml", "");
    }

    private String sanitizeFilename(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
    }

    private String sanitizeAnchor(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
    }
}
