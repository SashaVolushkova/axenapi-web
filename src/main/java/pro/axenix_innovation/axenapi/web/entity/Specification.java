package pro.axenix_innovation.axenapi.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Clob;
import java.time.Instant;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "specifications")
@EntityListeners(AuditingEntityListener.class)
public class Specification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "graph_name")
    private String graphName;

    @Lob
    @Column(name = "spec_file")
    private Clob specFile;

    @Column(name = "format", nullable = false, length = 10)
    private String format = "json";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
