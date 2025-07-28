package pro.axenix_innovation.axenapi.web.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConvertMdPdfDocumentService {

    private static final String FONT_PATH = "fonts/Roboto-VariableFont_wdth,wght.ttf";
    private static final String FONT_FAMILY = "Roboto";

    private final MessageHelper messageHelper;

    public byte[] convertMdToPdf(List<String> mdDocuments) {
        log.info(messageHelper.getMessage("axenapi.info.convert.document.md.pdf"));

        if (mdDocuments == null || mdDocuments.isEmpty()) {
            return createEmptyPdf();
        }

        List<String> filteredDocuments = mdDocuments.stream()
                .filter(doc -> doc != null && !doc.trim().isEmpty())
                .toList();

        if (filteredDocuments.isEmpty()) {
            return createEmptyPdf();
        }

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                AnchorLinkExtension.create(),
                AttributesExtension.create()
        ));
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(HtmlRenderer.RENDER_HEADER_ID, true);

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        StringBuilder combinedMd = new StringBuilder();
        List<Map<String, Object>> tocItems = new ArrayList<>();

        for (int i = 0; i < filteredDocuments.size(); i++) {
            String md = filteredDocuments.get(i);
            List<Map<String, Object>> headersInSection = extractHeaders(md);

            headersInSection.stream()
                    .filter(h -> (int) h.get("level") == 1)
                    .filter(h -> ((String) h.get("title")).startsWith("Сервис:"))
                    .forEach(tocItems::add);

            if (i > 0) {
                combinedMd.append("<div style=\"page-break-before: always;\"></div>\n\n");
            }
            combinedMd.append(md).append("\n");
        }

        StringBuilder tocHtml = new StringBuilder();
        tocHtml.append("<h1>Содержание</h1><ul>");
        for (Map<String, Object> item : tocItems) {
            int level = (int) item.get("level");
            String title = (String) item.get("title");
            String id = (String) item.get("id");

            tocHtml.append(String.format("<li style=\"margin-left:%dpx\"><a href=\"#%s\">%s</a></li>",
                    (level - 1) * 20, id, title));
        }
        tocHtml.append("</ul><div style=\"page-break-before: always;\"></div>\n\n");

        String fullMarkdown = tocHtml.toString() + combinedMd;
        Document document = parser.parse(fullMarkdown);
        String htmlContent = renderer.render(document);

        String htmlWithIds = injectHeaderIds(htmlContent);

        String html = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\"/>\n" +
                "  <style>\n" +
                "    @page {\n" +
                "      size: A4;\n" +
                "      margin: 1cm;\n" +
                "    }\n" +
                "    body {\n" +
                "      font-family: " + FONT_FAMILY + ", sans-serif;\n" +
                "      font-size: 12pt;\n" +
                "      word-wrap: break-word;\n" +
                "      overflow-wrap: break-word;\n" +
                "      hyphens: auto;\n" +
                "      white-space: normal;\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "    }\n" +
                "    h1 { font-size: 18pt; color: #2e6c80; margin: 0.5em 0; }\n" +
                "    h2 { font-size: 16pt; color: #2e6c80; margin: 0.5em 0; }\n" +
                "    h3 { font-size: 14pt; color: #2e6c80; margin: 0.5em 0; }\n" +
                "    a { text-decoration: none; color: #2e6c80; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                htmlWithIds +
                "\n</body>\n</html>";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(getFontFile(), FONT_FAMILY);
            builder.withHtmlContent(html, "");

            builder.toStream(out);
            builder.run();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Markdown to PDF", e);
        }
    }

    private String injectHeaderIds(String html) {
        Pattern headerPattern = Pattern.compile("(<h[1-6]>)\\s*(.*?)\\s*(</h[1-6]>)");
        Matcher matcher = headerPattern.matcher(html);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String tagOpen = matcher.group(1);
            String content = matcher.group(2);

            String id = content.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();

            matcher.appendReplacement(sb,
                    "<" + tagOpen.replace("<", "").replace(">", "") + " id=\"" + id + "\">" + content + "</" + tagOpen.replace("<", "").replace(">", "") + ">"
            );
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private List<Map<String, Object>> extractHeaders(String markdown) {
        List<Map<String, Object>> headers = new ArrayList<>();
        Matcher matcher = Pattern.compile("^(#{1})\\s*(.*?)(?:\\{#([^}]+)})?$", Pattern.MULTILINE).matcher(markdown);

        while (matcher.find()) {
            String hashes = matcher.group(1);
            String title = matcher.group(2).trim();
            String explicitId = matcher.group(3);

            int level = hashes.length();
            String id = Optional.ofNullable(explicitId)
                    .orElse(title.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase());

            headers.add(Map.of(
                    "level", level,
                    "title", title,
                    "id", id
            ));
        }

        return headers;
    }

    private File getFontFile() throws IOException {
        log.debug("Try to get .ttf file");
        File tempFile = File.createTempFile(FONT_FAMILY, ".ttf");
        tempFile.deleteOnExit();
        try (InputStream is = new ClassPathResource(FONT_PATH).getInputStream()) {
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private byte[] createEmptyPdf() {
        log.info(messageHelper.getMessage("axenapi.info.convert.md.document.empty.pdf"));
        String emptyHtml = "<html><head><style>@page { size: A4; margin: 1cm; }</style></head><body></body></html>";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(getFontFile(), FONT_FAMILY);
            builder.withHtmlContent(emptyHtml, "");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create empty PDF", e);
        }
    }

}