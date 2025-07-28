package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.GenerateDocxPost200Response;
import pro.axenix_innovation.axenapi.web.model.GenerateMarkdownPost200Response;
import pro.axenix_innovation.axenapi.web.model.GeneratePdfPost200Response;
import pro.axenix_innovation.axenapi.web.service.MarkdownSpecService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AxenAPIControllerPostGenerateMarkdownTest {

    private static final Logger log = LoggerFactory.getLogger(AxenAPIControllerPostGenerateMarkdownTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Path MD_FILE = Path.of("build/generated/MD/test-doc.md");
    private static final Path PDF_FILE = Path.of("build/generated/MD/pdf/test-doc.pdf");
    private static final Path DOCX_FILE = Path.of("build/generated/MD/docx/test-doc.docx");

    @Test
    public void testGenerateMarkdownFromFileUuid() throws Exception {
        var resource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        UUID serviceUUID = UUID.fromString("12345678-90ab-cdef-1234-567890abcdef");
        graph = MarkdownSpecService.filterByServiceUUIDs(graph, Set.of(serviceUUID));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        ResponseEntity<GenerateMarkdownPost200Response> responseEntity = restTemplate.exchange(
                "/generateMarkdown",
                HttpMethod.POST,
                requestEntity,
                GenerateMarkdownPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GenerateMarkdownPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);
        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".md"))).isTrue();

        String fullMarkdownUrl = links.values().stream()
                .filter(link -> link.endsWith(".md"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No markdown link found"));

        String fileId = null;
        int idx = fullMarkdownUrl.lastIndexOf("/download/markdown/");
        if (idx != -1) {
            String after = fullMarkdownUrl.substring(idx + "/download/markdown/".length());
            fileId = after.replace(".md", "");
        }

        if (fileId == null) {
            throw new RuntimeException("Cannot extract fileId from markdown URL: " + fullMarkdownUrl);
        }

        String downloadUrl = "/download/markdown/" + fileId + ".md";

        ResponseEntity<String> markdownResponse = restTemplate.getForEntity(downloadUrl, String.class);
        assertThat(markdownResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String markdownContent = markdownResponse.getBody();
        assertThat(markdownContent).isNotNull();

        Path targetPath = Paths.get("build/generated/MD/test-doc.md");
        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, markdownContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testGenerateMarkdownFromFile() throws Exception {
        var resource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        ResponseEntity<GenerateMarkdownPost200Response> responseEntity = restTemplate.exchange(
                "/generateMarkdown",
                HttpMethod.POST,
                requestEntity,
                GenerateMarkdownPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GenerateMarkdownPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);

        log.info("Полученные downloadLinks:");
        for (Map.Entry<String, String> entry : links.entrySet()) {
            log.info("Файл: {}, Ссылка: {}", entry.getKey(), entry.getValue());
        }

        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".md"))).isTrue();

        Path targetDir = Paths.get("build/generated/MD");
        Files.createDirectories(targetDir);

        for (Map.Entry<String, String> entry : links.entrySet()) {
            String fileName = entry.getKey();
            String fullUrl = entry.getValue();

            int idx = fullUrl.lastIndexOf("/download/markdown/");
            if (idx == -1) {
                throw new RuntimeException("Cannot extract fileId from markdown URL: " + fullUrl);
            }

            String after = fullUrl.substring(idx + "/download/markdown/".length());
            String fileId = after.replace(".md", "");

            String downloadUrl = "/download/markdown/" + fileId + ".md";

            log.info("Попытка загрузить файл по ссылке: {} (fileId={})", downloadUrl, fileId);

            ResponseEntity<String> markdownResponse = restTemplate.getForEntity(downloadUrl, String.class);

            log.info("HTTP-статус для {}: {}", downloadUrl, markdownResponse.getStatusCode());

            if (!markdownResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Не удалось получить файл. Ответ сервера: {}", markdownResponse.getStatusCode());
                fail("Не удалось скачать файл: " + downloadUrl);
            }

            String markdownContent = markdownResponse.getBody();
            assertThat(markdownContent).isNotNull();

//            Path targetPath = targetDir.resolve(fileName);
//            Files.writeString(targetPath, markdownContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    @Test
    public void testGenerateDocxFromFileui() throws Exception {
        var resource = new ClassPathResource("uigraph.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        String serviceId1 = "38f03412-45ee-49bd-aa99-72650f887f18";
        String serviceId2 = "bd9a9202-aae4-4667-aa48-39d847937366";

        String url = "/generateDocx?serviceIds=" + serviceId1 + "&serviceIds=" + serviceId2;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        ResponseEntity<GenerateDocxPost200Response> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                GenerateDocxPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GenerateDocxPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);
        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".docx"))).isTrue();

        String fullDocxUrl = links.values().stream()
                .filter(link -> link.endsWith(".docx"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No DOCX link found"));

        String fileId = null;
        int idx = fullDocxUrl.lastIndexOf("/download/docx/");
        if (idx != -1) {
            String after = fullDocxUrl.substring(idx + "/download/docx/".length());
            fileId = after.replace(".docx", "");
        }

        if (fileId == null) {
            throw new RuntimeException("Cannot extract fileId from DOCX URL: " + fullDocxUrl);
        }

        String downloadUrl = "/download/docx/" + fileId + ".docx";

        ResponseEntity<byte[]> docxResponse = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        assertThat(docxResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] docxContent = docxResponse.getBody();
        assertThat(docxContent).isNotNull();
        assertThat(docxContent.length).isGreaterThan(0);

        Path targetPath = Paths.get("build/generated/DOCX/test-doc.docx");
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, docxContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("DOCX file saved at: " + targetPath.toAbsolutePath());
    }

    @Test
    public void testGenerateDocxFromFile() throws Exception {
        var resource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics_2.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        ResponseEntity<GenerateDocxPost200Response> responseEntity = restTemplate.exchange(
                "/generateDocx",
                HttpMethod.POST,
                requestEntity,
                GenerateDocxPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GenerateDocxPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);
        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".docx"))).isTrue();

        String fullDocxUrl = links.values().stream()
                .filter(link -> link.endsWith(".docx"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No DOCX link found"));

        String fileId = null;
        int idx = fullDocxUrl.lastIndexOf("/download/docx/");
        if (idx != -1) {
            String after = fullDocxUrl.substring(idx + "/download/docx/".length());
            fileId = after.replace(".docx", "");
        }

        if (fileId == null) {
            throw new RuntimeException("Cannot extract fileId from DOCX URL: " + fullDocxUrl);
        }

        String downloadUrl = "/download/docx/" + fileId + ".docx";

        ResponseEntity<byte[]> docxResponse = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        assertThat(docxResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] docxContent = docxResponse.getBody();
        assertThat(docxContent).isNotNull();
        assertThat(docxContent.length).isGreaterThan(0);

        Path targetPath = Paths.get("build/generated/DOCX/test-doc.docx");
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, docxContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testGenerateDocxFromFileUuid() throws Exception {
        var resource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        UUID serviceUUID = UUID.fromString("12345678-90ab-cdef-1234-567890abcdef");
        graph = MarkdownSpecService.filterByServiceUUIDs(graph, Set.of(serviceUUID));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        ResponseEntity<GenerateDocxPost200Response> responseEntity = restTemplate.exchange(
                "/generateDocx",
                HttpMethod.POST,
                requestEntity,
                GenerateDocxPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GenerateDocxPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);
        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".docx"))).isTrue();

        String fullDocxUrl = links.values().stream()
                .filter(link -> link.endsWith(".docx"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No DOCX link found"));

        String fileId = null;
        int idx = fullDocxUrl.lastIndexOf("/download/docx/");
        if (idx != -1) {
            String after = fullDocxUrl.substring(idx + "/download/docx/".length());
            fileId = after.replace(".docx", "");
        }

        if (fileId == null) {
            throw new RuntimeException("Cannot extract fileId from DOCX URL: " + fullDocxUrl);
        }

        String downloadUrl = "/download/docx/" + fileId + ".docx";

        ResponseEntity<byte[]> docxResponse = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        assertThat(docxResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] docxContent = docxResponse.getBody();
        assertThat(docxContent).isNotNull();
        assertThat(docxContent.length).isGreaterThan(0);

        Path targetPath = Paths.get("build/generated/DOCX/test-doc.docx");
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, docxContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testGeneratePdfFromFile() throws Exception {
        var resource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        ResponseEntity<GeneratePdfPost200Response> responseEntity = restTemplate.exchange(
                "/generatePdf",
                HttpMethod.POST,
                requestEntity,
                GeneratePdfPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GeneratePdfPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);
        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".pdf"))).isTrue();

        String fullPdfUrl = links.values().stream()
                .filter(link -> link.endsWith(".pdf"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No PDF link found"));

        String fileId = null;
        int idx = fullPdfUrl.lastIndexOf("/download/pdf/");
        if (idx != -1) {
            String after = fullPdfUrl.substring(idx + "/download/pdf/".length());
            fileId = after.replace(".pdf", "");
        }

        if (fileId == null) {
            throw new RuntimeException("Cannot extract fileId from PDF URL: " + fullPdfUrl);
        }

        String downloadUrl = "/download/pdf/" + fileId + ".pdf";

        ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        assertThat(pdfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] pdfContent = pdfResponse.getBody();
        assertThat(pdfContent).isNotNull();
        assertThat(pdfContent.length).isGreaterThan(0);

        Path targetPath = Paths.get("build/generated/PDF/test-pdf.pdf");
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, pdfContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testGeneratePdfFromFileUuid() throws Exception {
        var resource = new ClassPathResource("results/service_no_common_consume_topics_common_events_common_outgoing_topics.json");
        String json = Files.readString(resource.getFile().toPath());
        EventGraphDTO graph = objectMapper.readValue(json, EventGraphDTO.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EventGraphDTO> requestEntity = new HttpEntity<>(graph, headers);

        String serviceId = "12345678-90ab-cdef-1234-567890abcdef";
        String url = "/generatePdf?serviceIds=" + serviceId;

        ResponseEntity<GeneratePdfPost200Response> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                GeneratePdfPost200Response.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        GeneratePdfPost200Response response = responseEntity.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");

        Map<String, String> links = response.getDownloadLinks();
        assertThat(links).isNotNull();
        assertThat(links.size()).isGreaterThan(0);
        assertThat(links.values().stream().anyMatch(link -> link.endsWith(".pdf"))).isTrue();

        String fullPdfUrl = links.values().stream()
                .filter(link -> link.endsWith(".pdf"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No PDF link found"));

        String fileId = null;
        int idx = fullPdfUrl.lastIndexOf("/download/pdf/");
        if (idx != -1) {
            String after = fullPdfUrl.substring(idx + "/download/pdf/".length());
            fileId = after.replace(".pdf", "");
        }

        if (fileId == null) {
            throw new RuntimeException("Cannot extract fileId from PDF URL: " + fullPdfUrl);
        }

        String downloadUrl = "/download/pdf/" + fileId + ".pdf";

        ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        assertThat(pdfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] pdfContent = pdfResponse.getBody();
        assertThat(pdfContent).isNotNull();
        assertThat(pdfContent.length).isGreaterThan(0);

        Path targetPath = Paths.get("build/generated/PDF/test-pdf.pdf");
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, pdfContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

//    @Test
//    public void testMarkdownToPdfAndDocx() throws Exception {
//        Files.createDirectories(PDF_FILE.getParent());
//        Files.createDirectories(DOCX_FILE.getParent());
//
//        String markdown = Files.readString(MD_FILE, StandardCharsets.UTF_8);
//        List<String> markdownList = List.of(markdown);
//
//        String xhtml = ConvertMDService.mergeMarkdownToValidXhtml(markdownList);
//
//        ConvertMDService.validateXhtml(xhtml);
//
//        try (OutputStream pdfOut = new FileOutputStream(PDF_FILE.toFile())) {
//            ConvertMDService.htmlToPdf(xhtml, pdfOut);
//        }
//
//        try (OutputStream docxOut = new FileOutputStream(DOCX_FILE.toFile())) {
//            ConvertMDService.htmlToDocx(xhtml, docxOut);
//        }
//
//        assertTrue(Files.exists(PDF_FILE), "PDF файл не создан");
//        assertTrue(Files.exists(DOCX_FILE), "DOCX файл не создан");
//    }


}
