package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pro.axenix_innovation.axenapi.web.generate.PdfSpecificationDbHandler;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.GeneratePdfPost200Response;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfGenerationServiceTest {
    @Mock
    private MarkdownSpecService markdownSpecService;
    @Mock
    private PdfSpecificationDbHandler pdfSpecificationDbHandler;
    @Mock
    private ConvertMdPdfDocumentService convertMdPdfDocumentService;
    @Mock
    private MessageHelper messageHelper;

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pdfGenerationService = new PdfGenerationService(
                markdownSpecService,
                pdfSpecificationDbHandler,
                convertMdPdfDocumentService,
                messageHelper
        );
    }

    @Test
    void genPdfFromEventGraphMDLinksEmpty() throws Exception {
        EventGraphDTO dto = new EventGraphDTO();
        when(markdownSpecService.generateMarkdownMap(dto)).thenReturn(Collections.emptyMap());
        when(messageHelper.getMessage(anyString(), any())).thenReturn("error");

        GeneratePdfPost200Response response = pdfGenerationService.generatePdfFromEventGraph(dto);
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Failed to generate Markdown specification"));
    }

    @Test
    void genPdfFromEventGraphMDContentNotFound() throws Exception {
        EventGraphDTO dto = new EventGraphDTO();
        String fileId = "file123";
        String fileName = "file123.md";
        String relativeUrl = "/download/markdown/" + fileId + ".md";
        when(markdownSpecService.generateMarkdownMap(dto)).thenReturn(Map.of(fileName, relativeUrl));
        when(markdownSpecService.getMarkdownContentByFileId(fileId)).thenReturn(Optional.empty());
        when(messageHelper.getMessage(anyString(), any())).thenReturn("error");

        GeneratePdfPost200Response response = pdfGenerationService.generatePdfFromEventGraph(dto);
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("No valid Markdown content"));
    }

    @Test
    void genPdfFromEventGraphPdfDownloadUrlEmpty() throws Exception {
        EventGraphDTO dto = new EventGraphDTO();
        String fileId = "file123";
        String fileName = "file123.md";
        String relativeUrl = "/download/markdown/" + fileId + ".md";
        byte[] pdfBytes = new byte[]{1, 2, 3};
        String pdfFileName = "random.pdf";
        when(markdownSpecService.generateMarkdownMap(dto)).thenReturn(Map.of(fileName, relativeUrl));
        when(markdownSpecService.getMarkdownContentByFileId(fileId)).thenReturn(Optional.of("# Markdown content"));
        when(convertMdPdfDocumentService.convertMdToPdf(anyList())).thenReturn(pdfBytes);
        when(pdfSpecificationDbHandler.handlePdf(anyMap())).thenReturn(Map.of(pdfFileName, ""));
        when(messageHelper.getMessage(anyString(), any())).thenReturn("error");

        GeneratePdfPost200Response response = pdfGenerationService.generatePdfFromEventGraph(dto);
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Failed to save PDF file"));
    }

    @Test
    void genPdfFromEventGraphConvertMdToPdfThrows() throws Exception {
        EventGraphDTO dto = new EventGraphDTO();
        String fileId = "file123";
        String fileName = "file123.md";
        String relativeUrl = "/download/markdown/" + fileId + ".md";
        when(markdownSpecService.generateMarkdownMap(dto)).thenReturn(Map.of(fileName, relativeUrl));
        when(markdownSpecService.getMarkdownContentByFileId(fileId)).thenReturn(Optional.of("# Markdown content"));
        when(convertMdPdfDocumentService.convertMdToPdf(anyList())).thenThrow(new RuntimeException("PDF error"));
        when(messageHelper.getMessage(anyString(), any())).thenReturn("error");

        GeneratePdfPost200Response response = pdfGenerationService.generatePdfFromEventGraph(dto);
        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Exception during PDF generation"));
    }
} 