package pro.axenix_innovation.axenapi.web.service.git;

import lombok.AllArgsConstructor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.service.GitRepositoryService;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@AllArgsConstructor
public class LocalGitRepositoryService {

    private final GitRepositoryService gitRepositoryService;

    public final static String DOC_REPOSITORY_PATH = "/documentation/articles";
    public final static String DEFAULT_BRANCH = "develop";

    public String getRepositoryUrl() {
        GitRepo gitRepository = gitRepositoryService.getDocumentationGitRepository();

        return Optional.ofNullable(gitRepository)
                .orElseThrow(() -> new RuntimeException("Git repository to clone not found"))
                .getUrl();
    }

    public String getRepositoryToken() {
        GitRepo gitRepository = gitRepositoryService.getDocumentationGitRepository();

        return Optional.ofNullable(gitRepository)
                .orElseThrow(() -> new RuntimeException("Git repository to clone not found"))
                .getToken();

    }

    public boolean isRepositoryExists() {
        File gitDir = new File(new File(DOC_REPOSITORY_PATH), ".git");

        return gitDir.exists() && gitDir.isDirectory();
    }

    public Repository getLocalGitRepository() {
        try {
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            return repositoryBuilder
                    .setGitDir(new File("%s/.git".formatted(DOC_REPOSITORY_PATH)))
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Could not get git repository", e);
        }
    }

    public Git getClonedLocalGit() {
        return new Git(getLocalGitRepository());
    }

    public UsernamePasswordCredentialsProvider getLocalCredentialsProvider() throws GitLabApiException {
        return new UsernamePasswordCredentialsProvider(
                getGitLabApi().getUserApi().getCurrentUser().getUsername(),
                getRepositoryToken());
    }

    public GitLabApi getGitLabApi() {
        try {
            URL uri = new URL(getRepositoryUrl());
            return new GitLabApi("%s://%s".formatted(uri.getProtocol(), uri.getHost()), getRepositoryToken());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Project getGitLabApiProject() throws GitLabApiException {
        GitRepo gitRepository = gitRepositoryService.getDocumentationGitRepository();
        List<String> parts = Arrays.stream(gitRepository.getUrl().split("/"))
                .filter(s -> !s.isEmpty())
                .toList();

        return getGitLabApi().getProjectApi().getProject(parts.get(parts.size() - 2), parts.getLast());
    }
}
