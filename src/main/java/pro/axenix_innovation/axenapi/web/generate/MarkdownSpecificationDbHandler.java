package pro.axenix_innovation.axenapi.web.generate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.axenix_innovation.axenapi.web.entity.MarkdownSpecification;
import pro.axenix_innovation.axenapi.web.repository.MarkdownSpecificationRepository;

import javax.sql.rowset.serial.SerialClob;
import java.sql.Clob;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class MarkdownSpecificationDbHandler implements SpecificationMarkdownHandler {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownSpecificationDbHandler.class);

    private final MarkdownSpecificationRepository markdownSpecificationRepository;

    public MarkdownSpecificationDbHandler(MarkdownSpecificationRepository markdownSpecificationRepository) {
        this.markdownSpecificationRepository = markdownSpecificationRepository;
    }

    @Override
    public Map<String, String> handleMarkdown(Map<String, String> readmeMap) {
        Map<String, String> errors = new HashMap<>();
        Map<String, String> downloadLinks = new HashMap<>();

        readmeMap.forEach((key, markdownContent) -> {
            try {
                Clob mdClob = new SerialClob(markdownContent.toCharArray());

                MarkdownSpecification markdownSpecification = new MarkdownSpecification();
                markdownSpecification.setGraphName(key);
                markdownSpecification.setMarkdownFile(mdClob);
                MarkdownSpecification saved = markdownSpecificationRepository.save(markdownSpecification);

                downloadLinks.put(key, "/download/markdown/" + saved.getId() + ".md");

            } catch (SQLException e) {
                String errorMessage = "Error converting Markdown to CLOB for: " + key;
                errors.put(key, errorMessage + ": " + e.getMessage());
                logger.error(errorMessage, e);
            } catch (Exception e) {
                String errorMessage = "Unexpected error for key: " + key;
                errors.put(key, errorMessage + ": " + e.getMessage());
                logger.error(errorMessage, e);
            }
        });

        logger.debug("Generated Markdown download links: {}", downloadLinks);
        return errors.isEmpty() ? downloadLinks : errors;
    }

    @Scheduled(fixedRate = 1 * 60 * 1000)
    @Transactional
    public void cleanTable() {
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        markdownSpecificationRepository.deleteAllCreatedBefore(fiveMinutesAgo);
    }
}
