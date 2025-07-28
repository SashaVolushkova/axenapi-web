package pro.axenix_innovation.axenapi.web.generate;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.entity.DocxSpecification;
import pro.axenix_innovation.axenapi.web.repository.DocxSpecificationRepository;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class DocxSpecificationDbHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocxSpecificationDbHandler.class);
    private final DocxSpecificationRepository repository;

    public DocxSpecificationDbHandler(DocxSpecificationRepository repository) {
        this.repository = repository;
    }

    public Map<String, String> handleDocx(Map<String, String> docxMap) {
        Map<String, String> downloadLinks = new HashMap<>();

        docxMap.forEach((key, docxContent) -> {
            try {
                byte[] docxBytes = Base64.getDecoder().decode(docxContent);

                Blob docxBlob = new SerialBlob(docxBytes);
                DocxSpecification spec = new DocxSpecification();
                spec.setGraphName(key);
                spec.setDocxFile(docxBlob);

                DocxSpecification saved = repository.save(spec);

                downloadLinks.put(key, "/download/docx/" + saved.getId() + ".docx");

            } catch (Exception e) {
                logger.error("Failed to store DOCX for: {}", key, e);
            }
        });

        return downloadLinks;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cleanTable() {
        Instant threshold = Instant.now().minus(5, ChronoUnit.MINUTES);
        repository.deleteAllCreatedBefore(threshold);
    }
}
