package pro.axenix_innovation.axenapi.web.generate;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.entity.PdfSpecification;
import pro.axenix_innovation.axenapi.web.repository.PdfSpecificationRepository;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class PdfSpecificationDbHandler {

    private static final Logger logger = LoggerFactory.getLogger(PdfSpecificationDbHandler.class);
    private final PdfSpecificationRepository repository;

    public PdfSpecificationDbHandler(PdfSpecificationRepository repository) {
        this.repository = repository;
    }
    public Map<String, String> handlePdf(Map<String, String> pdfMap) {
        Map<String, String> downloadLinks = new HashMap<>();

        pdfMap.forEach((key, pdfContent) -> {
            try {
                byte[] pdfBytes = Base64.getDecoder().decode(pdfContent);

                Blob pdfBlob = new SerialBlob(pdfBytes);
                PdfSpecification spec = new PdfSpecification();
                spec.setGraphName(key);
                spec.setPdfFile(pdfBlob);

                PdfSpecification saved = repository.save(spec);

                downloadLinks.put(key, "/download/pdf/" + saved.getId() + ".pdf");

            } catch (Exception e) {
                logger.error("Failed to store PDF for: {}", key, e);
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
