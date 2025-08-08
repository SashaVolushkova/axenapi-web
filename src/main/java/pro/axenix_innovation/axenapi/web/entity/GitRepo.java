package pro.axenix_innovation.axenapi.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gitrepo")
public class GitRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "service")
    private String service;

    @Column(name = "url")
    private String url;

    @Column(name = "auth_type")
    @Enumerated(EnumType.STRING)
    private GitRepoAuthType authType;

    @Column(name = "token")
    private String token;

    @Column(name = "login")
    private String login;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private GitRepoType type;
}
