package pro.axenix_innovation.axenapi.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.entity.GitRepoType;
import pro.axenix_innovation.axenapi.web.repository.GitRepository;

@Service
@RequiredArgsConstructor
public class GitRepositoryService {
    private final GitRepository gitRepository;

    public GitRepo getDocumentationGitRepository() {
        return gitRepository.findFirstByType(GitRepoType.DOCUMENTATION);
    }
}
