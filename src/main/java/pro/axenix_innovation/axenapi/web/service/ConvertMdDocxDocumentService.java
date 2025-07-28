package pro.axenix_innovation.axenapi.web.service;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Механизм по конвертации нескольких .md файлов документации в строковом представлении
 * в .docx файл в виде byte[]
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConvertMdDocxDocumentService {
    private final MessageHelper messageHelper;

    /**
     * @param mdDocuments коллекция .md файлов в строковом представлении
     * @return byte[] - конвертированный .docx файл
     */
    public byte[] convertMdToDocx(List<String> mdDocuments) {
        log.info(messageHelper.getMessage("axenapi.info.convert.document.md.docx"));

        if (mdDocuments == null || mdDocuments.isEmpty()) {
            return createEmptyDocx();
        }
        try {
            List<String> filteredDocs = mdDocuments.stream()
                    .filter(doc -> doc != null && !doc.trim().isEmpty())
                    .collect(Collectors.toList());

            if (filteredDocs.isEmpty()) {
                return createEmptyDocx();
            }

            String html = generateHtmlWithTocAndPageBreaks(filteredDocs);

            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();

            XHTMLImporter xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);
            documentPart.getContent().addAll(xhtmlImporter.convert(html, null));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wordMLPackage.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Markdown to DOCX", e);
        }
    }

    private String generateHtmlWithTocAndPageBreaks(List<String> mdDocuments) {
        MutableDataSet options = new MutableDataSet();
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>");
        options.set(Parser.EXTENSIONS, Arrays.asList(
                AnchorLinkExtension.create(),
                AttributesExtension.create(),
                TablesExtension.create(),
                TocExtension.create()
        ));
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(HtmlRenderer.RENDER_HEADER_ID, true);

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        StringBuilder fullContentBuilder = new StringBuilder();
        List<Map<String, Object>> tocItems = new ArrayList<>();

        for (int i = 0; i < mdDocuments.size(); i++) {
            String doc = mdDocuments.get(i);
            Document document = parser.parse(doc);

            addIdToH1Headings(document);

            Node node = document.getFirstChild();
            while (node != null) {
                if (node instanceof Heading heading && heading.getLevel() == 1) {
                    String rawTitle = heading.getText().toString();
                    String title = cleanHeaderText(rawTitle);
                    String id = heading.getAnchorRefId();

                    if (title.startsWith("Сервис:")) {
                        tocItems.add(Map.of("title", title, "id", id));
                    }
                }
                node = node.getNext();
            }

            if (i > 0) {
                fullContentBuilder.append("<div style=\"page-break-before: always;\"></div>\n");
            }
            fullContentBuilder.append(renderer.render(document));
        }

        boolean withToc = !tocItems.isEmpty();
        StringBuilder htmlBuilder = new StringBuilder();

        if (withToc) {
            StringBuilder tocHtml = new StringBuilder();
            tocHtml.append("<h1>Оглавление</h1><ul>\n");
            for (Map<String, Object> item : tocItems) {
                tocHtml.append(String.format("<li><a href=\"#%s\">%s</a></li>\n",
                        item.get("id"), item.get("title")));
            }
            tocHtml.append("</ul>\n<div style=\"page-break-before: always;\"></div>\n");

            htmlBuilder.append(tocHtml);
        }

        htmlBuilder.append(fullContentBuilder);

        return "<html><body>" + htmlBuilder.toString() + "</body></html>";
    }

    private String generateSlug(String text) {
        return text
                .replaceAll("\\{#.*?}", "")
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-*|-*$", "");
    }

    private void addIdToH1Headings(Document document) {
        NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(Heading.class, node -> {
                    if (node.getLevel() == 1) {
                        if (node.getAnchorRefId() == null || node.getAnchorRefId().isEmpty()) {
                            String rawText = node.getText().toString();
                            String cleanedText = cleanHeaderText(rawText);
                            String slug = generateSlug(cleanedText);
                            node.setAnchorRefId(slug);
                        }
                    }
                })
        );
        visitor.visit(document);
    }

    private String cleanHeaderText(String header) {
        return header.replaceAll("\\s*\\{#.*?}", "").trim();
    }

    public byte[] createEmptyDocx() {
        log.info(messageHelper.getMessage("axenapi.info.convert.md.document.empty.docx"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            wordMLPackage.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create empty DOCX", e);
        }
    }
}
