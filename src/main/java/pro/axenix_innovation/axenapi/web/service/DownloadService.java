package pro.axenix_innovation.axenapi.web.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.axenix_innovation.axenapi.web.entity.DocxSpecification;
import pro.axenix_innovation.axenapi.web.entity.MarkdownSpecification;
import pro.axenix_innovation.axenapi.web.entity.PdfSpecification;
import pro.axenix_innovation.axenapi.web.entity.Specification;
import pro.axenix_innovation.axenapi.web.repository.DocxSpecificationRepository;
import pro.axenix_innovation.axenapi.web.repository.MarkdownSpecificationRepository;
import pro.axenix_innovation.axenapi.web.repository.PdfSpecificationRepository;
import pro.axenix_innovation.axenapi.web.repository.SpecificationRepository;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

@Service
public class DownloadService {

    private final DocxSpecificationRepository docxSpecificationRepository;
    private final PdfSpecificationRepository pdfSpecificationRepository;
    private final MarkdownSpecificationRepository markdownSpecificationRepository;
    private final SpecificationRepository specificationRepository;


    public DownloadService(DocxSpecificationRepository docxSpecificationRepository, PdfSpecificationRepository pdfSpecificationRepository,
                           MarkdownSpecificationRepository markdownSpecificationRepository, SpecificationRepository specificationRepository) {
        this.docxSpecificationRepository = docxSpecificationRepository;
        this.pdfSpecificationRepository = pdfSpecificationRepository;
        this.markdownSpecificationRepository = markdownSpecificationRepository;
        this.specificationRepository = specificationRepository;
    }

    @Transactional(readOnly = true)
    public byte[] getDocxFileBytes(String fileId) throws SQLException, FileNotFoundException {
        DocxSpecification docxSpecification = docxSpecificationRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("DOCX specification not found for fileId: " + fileId));

        Blob docxBlob = docxSpecification.getDocxFile();
        if (docxBlob == null) {
            throw new FileNotFoundException("DOCX BLOB is null for fileId: " + fileId);
        }

        int blobLength = (int) docxBlob.length();
        return docxBlob.getBytes(1, blobLength);
    }

    @Transactional(readOnly = true)
    public byte[] getPdfFileBytes(String fileId) throws SQLException, FileNotFoundException {
        PdfSpecification pdfSpecification = pdfSpecificationRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("PDF specification not found for fileId: " + fileId));

        Blob pdfBlob = pdfSpecification.getPdfFile();
        if (pdfBlob == null) {
            throw new FileNotFoundException("PDF BLOB is null for fileId: " + fileId);
        }

        int blobLength = (int) pdfBlob.length();
        return pdfBlob.getBytes(1, blobLength);
    }

    @Transactional(readOnly = true)
    public byte[] getMarkdownFileBytes(String fileId) throws SQLException, FileNotFoundException {
        MarkdownSpecification markdownSpecification = markdownSpecificationRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Markdown specification not found for fileId: " + fileId));

        Clob markdownClob = markdownSpecification.getMarkdownFile();
        if (markdownClob == null) {
            throw new FileNotFoundException("Markdown CLOB is null for fileId: " + fileId);
        }

        long clobLength = markdownClob.length();
        String markdownContent = markdownClob.getSubString(1, (int) clobLength);
        return markdownContent.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] getJsonSpecBytes(String fileId) throws Exception {
        Specification specification = specificationRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Specification not found by id: " + fileId));

        Clob specClob = specification.getSpecFile();
        if (specClob == null) {
            throw new FileNotFoundException("Spec Clob is null for id: " + fileId);
        }

        StringBuilder sb = new StringBuilder();
        try (Reader reader = specClob.getCharacterStream()) {
            char[] buffer = new char[2048];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] getYamlSpecBytes(String fileId) throws Exception {
        Specification specification = specificationRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Specification not found by id: " + fileId));

        Clob specClob = specification.getSpecFile();
        if (specClob == null) {
            throw new FileNotFoundException("Spec Clob is null for id: " + fileId);
        }

        StringBuilder sb = new StringBuilder();
        try (Reader reader = specClob.getCharacterStream()) {
            char[] buffer = new char[2048];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
