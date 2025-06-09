package pro.axenix_innovation.axenapi.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.entity.GitRepoType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class GitRepositoryServiceTest {
    @Autowired
    GitRepositoryService gitRepositoryService;

    @Test
    void getTokenTest() {
        GitRepo gitRepo = gitRepositoryService.getDocumentationGitRepository();
        assertNotNull(gitRepo);
        assertEquals(GitRepoType.DOCUMENTATION, gitRepo.getType());
    }
}
