package pro.axenix_innovation.axenapi.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.axenix_innovation.axenapi.web.entity.ServiceCode;

import java.time.Instant;

public interface ServiceCodeRepository extends JpaRepository<ServiceCode, String> {

    @Modifying
    @Query("DELETE FROM ServiceCode s WHERE s.createdAt < :time")
    void deleteAllCreatedBefore(@Param("time") Instant time);

}
