package pro.axenix_innovation.axenapi.web.service.git;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.service.GitRepositoryService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LocalGitRepositoryServiceTest {

    @InjectMocks
    private LocalGitRepositoryService localGitRepositoryService;

    @Mock
    private GitRepositoryService gitRepositoryServiceMock;

    @Mock
    private GitRepo gitRepoMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetRepositoryUrlSuccess() {
        when(gitRepositoryServiceMock.getDocumentationGitRepository()).thenReturn(gitRepoMock);
        when(gitRepoMock.getUrl()).thenReturn("url");

        assertEquals(localGitRepositoryService.getRepositoryUrl(), gitRepoMock.getUrl());
        assertNotNull(gitRepoMock);
    }

    @Test
    void testGetRepositoryUrlWithException() {
        when(gitRepositoryServiceMock.getDocumentationGitRepository()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> localGitRepositoryService.getRepositoryUrl());
    }

    @Test
    void testGetRepositoryTokenSuccess() {
        when(gitRepositoryServiceMock.getDocumentationGitRepository()).thenReturn(gitRepoMock);
        when(gitRepoMock.getToken()).thenReturn("token");

        assertEquals(localGitRepositoryService.getRepositoryToken(), gitRepoMock.getToken());
        assertNotNull(gitRepoMock);
    }

    @Test
    void testGetRepositoryTokenWithException() {
        when(gitRepositoryServiceMock.getDocumentationGitRepository()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> localGitRepositoryService.getRepositoryToken());
    }

    @Test
    void testGetGitLabApiProjectWithException() throws GitLabApiException {
        when(gitRepositoryServiceMock.getDocumentationGitRepository()).thenReturn(gitRepoMock);
        when(gitRepoMock.getUrl()).thenReturn("http://url/url");
        GitLabApi gitLabApiMock = mock(GitLabApi.class);
        ProjectApi projectApiMock = mock(ProjectApi.class);

        when(gitLabApiMock.getProjectApi()).thenReturn(projectApiMock);
        when(projectApiMock.getProject("url", "url")).thenReturn(mock(Project.class));

        assertThrows(GitLabApiException.class, () -> localGitRepositoryService.getGitLabApiProject());
    }
}
