package pro.axenix_innovation.axenapi.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.axenix_innovation.axenapi.web.entity.MarkdownSpecification;

import java.time.Instant;

public interface MarkdownSpecificationRepository extends JpaRepository<MarkdownSpecification, String> {

    @Modifying
    @Query("DELETE FROM MarkdownSpecification m WHERE m.createdAt < :time")
    void deleteAllCreatedBefore(@Param("time") Instant time);
}
