package pro.axenix_innovation.axenapi.web.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.generate.PdfSpecificationDbHandler;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.GeneratePdfPost200Response;

import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_DURING_PDF_GEN;

@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final MarkdownSpecService markdownSpecService;
    private final PdfSpecificationDbHandler pdfSpecificationDbHandler;
    private final ConvertMdPdfDocumentService convertMdPdfDocumentService;
    private final MessageHelper messageHelper;

    private final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    public GeneratePdfPost200Response generatePdfFromEventGraph(EventGraphDTO eventGraphDTO) {
        try {
            String pdfDownloadUrl = generatePdfDownloadLink(eventGraphDTO);

            Map<String, String> downloadLinks = Map.of(
                    pdfDownloadUrl.substring(pdfDownloadUrl.lastIndexOf('/') + 1), pdfDownloadUrl
            );

            log.info(messageHelper.getMessage("axenapi.info.gen.pdf.success", pdfDownloadUrl));
            return new GeneratePdfPost200Response()
                    .status("OK")
                    .message("PDF generated successfully")
                    .downloadLinks(downloadLinks);

        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_DURING_PDF_GEN, e.getMessage()), e);
            return buildError("Exception during PDF generation: " + e.getMessage());
        }
    }

    public String generatePdfDownloadLink(EventGraphDTO eventGraphDTO) throws Exception {
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
            throw new IllegalStateException("No valid Markdown content found for conversion to PDF");
        }

        byte[] pdfBytes = convertMdPdfDocumentService.convertMdToPdf(allMarkdownContents);

        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
        String randomFileName = UUID.randomUUID().toString() + ".pdf";
        Map<String, String> pdfMap = Map.of(randomFileName, pdfBase64);
        Map<String, String> downloadLinks = pdfSpecificationDbHandler.handlePdf(pdfMap);

        String pdfDownloadUrl = downloadLinks.get(randomFileName);

        if (pdfDownloadUrl == null || pdfDownloadUrl.isEmpty()) {
            throw new IllegalStateException("Failed to save PDF file and generate download link");
        }

        return pdfDownloadUrl;
    }

    private String extractFileId(String relativeUrl) {
        return relativeUrl.replace("/download/markdown/", "").replace(".md", "");
    }

    private GeneratePdfPost200Response buildError(String message) {
        return new GeneratePdfPost200Response()
                .status("ERROR")
                .message(message);
    }
}
