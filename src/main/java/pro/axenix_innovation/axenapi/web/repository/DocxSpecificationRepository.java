package pro.axenix_innovation.axenapi.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pro.axenix_innovation.axenapi.web.entity.DocxSpecification;

import java.time.Instant;

public interface DocxSpecificationRepository extends JpaRepository<DocxSpecification, String> {
    @Modifying
    @Query("DELETE FROM DocxSpecification d WHERE d.createdAt < :threshold")
    void deleteAllCreatedBefore(Instant threshold);
}

