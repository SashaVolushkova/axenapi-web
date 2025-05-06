package pro.axenix_innovation.axenapi.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.entity.GitRepoType;

/**
 *     репозиторий для таблицы gitrepo
 */
public interface GitRepository extends JpaRepository<GitRepo, String> {

    GitRepo findFirstByType(GitRepoType type);
}
