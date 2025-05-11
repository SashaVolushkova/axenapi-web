package pro.axenix_innovation.axenapi.web.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.generate.DocxSpecificationDbHandler;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.GenerateDocxPost200Response;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_DURING_DOCX_GEN;

@Service
@RequiredArgsConstructor
public class DocxGenerationService {

    private final MarkdownSpecService markdownSpecService;
    private final DocxSpecificationDbHandler docxSpecificationDbHandler;
    private final ConvertMdDocxDocumentService convertMdDocxDocumentService;
    private final MessageHelper messageHelper;

    private final Logger log = LoggerFactory.getLogger(DocxGenerationService.class);

    public GenerateDocxPost200Response generateDocxFromEventGraph(EventGraphDTO eventGraphDTO) {
        try {
            String docxDownloadUrl = generateDocxDownloadLink(eventGraphDTO);

            Map<String, String> downloadLinks = Map.of(
                    docxDownloadUrl.substring(docxDownloadUrl.lastIndexOf('/') + 1), docxDownloadUrl
            );

            log.info(messageHelper.getMessage("axenapi.info.docx.gen.success.url", docxDownloadUrl));
            return new GenerateDocxPost200Response()
                    .status("OK")
                    .message("DOCX generated successfully")
                    .downloadLinks(downloadLinks);

        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_DURING_DOCX_GEN, e.getMessage()), e);
            return buildError("Exception during DOCX generation: " + e.getMessage());
        }
    }

    public String generateDocxDownloadLink(EventGraphDTO eventGraphDTO) throws Exception {
        Map<String, String> markdownLinks = markdownSpecService.generateMarkdownMap(eventGraphDTO);
        if (markdownLinks == null || markdownLinks.isEmpty()) {
            throw new IllegalStateException("Failed to generate Markdown specification or no download links");
        }

        List<String> allMarkdownContents = new ArrayList<>();

        for (Map.Entry<String, String> entry : markdownLinks.entrySet()) {
            String filename = entry.getKey();
            String relativeUrl = entry.getValue();

            if (filename.endsWith(".md")) {
                String fileId = extractFileId(relativeUrl);

                if (fileId == null || fileId.isEmpty()) {
                    log.warn("Invalid fileId extracted from markdown link: {}", relativeUrl);
                    continue;
                }

                Optional<String> markdownContentOpt = markdownSpecService.getMarkdownContentByFileId(fileId);
                if (markdownContentOpt.isEmpty()) {
                    log.warn("Markdown content not found for fileId: {}", fileId);
                    continue;
                }

                allMarkdownContents.add(markdownContentOpt.get());
            }
        }

        if (allMarkdownContents.isEmpty()) {
            throw new IllegalStateException("No valid Markdown content found for conversion to DOCX");
        }

        byte[] docxBytes = convertMdDocxDocumentService.convertMdToDocx(allMarkdownContents); // ← временно оставлено для совместимости

        String docxBase64 = Base64.getEncoder().encodeToString(docxBytes);
        String randomFileName = UUID.randomUUID().toString() + ".docx";
        Map<String, String> docxMap = Map.of(randomFileName, docxBase64);
        Map<String, String> downloadLinks = docxSpecificationDbHandler.handleDocx(docxMap); // ← временно используем тот же handler

        String docxDownloadUrl = downloadLinks.get(randomFileName);

        if (docxDownloadUrl == null || docxDownloadUrl.isEmpty()) {
            throw new IllegalStateException("Failed to save DOCX file and generate download link");
        }

        return docxDownloadUrl;
    }

    private String extractFileId(String relativeUrl) {
        return relativeUrl.replace("/download/markdown/", "").replace(".md", "");
    }

    private GenerateDocxPost200Response buildError(String message) {
        return new GenerateDocxPost200Response()
                .status("ERROR")
                .message(message);
    }
}
