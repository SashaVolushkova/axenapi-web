package pro.axenix_innovation.axenapi.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pro.axenix_innovation.axenapi.web.entity.PdfSpecification;

import java.time.Instant;

public interface PdfSpecificationRepository extends JpaRepository<PdfSpecification, String> {
    @Modifying
    @Query("DELETE FROM PdfSpecification p WHERE p.createdAt < :threshold")
    void deleteAllCreatedBefore(Instant threshold);
}

