package pro.axenix_innovation.axenapi.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.axenix_innovation.axenapi.web.entity.Specification;

import java.time.Instant;


public interface SpecificationRepository extends JpaRepository<Specification, String> {
    @Modifying
    @Query("DELETE FROM Specification s WHERE s.createdAt < :time")
    void deleteAllCreatedBefore(@Param("time") Instant time);


}
