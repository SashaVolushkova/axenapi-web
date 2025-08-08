package pro.axenix_innovation.axenapi.web.service.git.impl;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import pro.axenix_innovation.axenapi.web.service.git.LocalGitRepositoryService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GitServiceCommandImplTest {

    @InjectMocks
    private GitServiceCommandImpl gitServiceCommandImpl;

    @Mock
    private LocalGitRepositoryService localGitRepositoryServiceMock;

    @Mock
    private Git localGitMock;

    @Mock
    private CheckoutCommand checkoutCommandMock;

    @Mock
    Repository repositoryMock;

    private final String docPath = "section1/1";
    private final String mrTitle = "title";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCloneProjectIfProjectExists() throws GitAPIException, IOException, GitLabApiException {
        PullCommand pullCommandMock = mock(PullCommand.class);
        UsernamePasswordCredentialsProvider credentialsProviderMock = mock(UsernamePasswordCredentialsProvider.class);

        when(localGitRepositoryServiceMock.isRepositoryExists()).thenReturn(true);
        checkoutCommand(false);
        when(localGitRepositoryServiceMock.getClonedLocalGit()).thenReturn(localGitMock);
        when(localGitMock.pull()).thenReturn(pullCommandMock);
        when(localGitRepositoryServiceMock.getLocalCredentialsProvider()).thenReturn(credentialsProviderMock);
        when(pullCommandMock.setCredentialsProvider(credentialsProviderMock)).thenReturn(pullCommandMock);
        when(pullCommandMock.setRemoteBranchName("develop")).thenReturn(pullCommandMock);
        when(pullCommandMock.call()).thenReturn(mock(PullResult.class));
        checkoutCommand(true);

        File fileMock = mock(File.class);
        when(localGitMock.getRepository()).thenReturn(repositoryMock);
        when(repositoryMock.getDirectory()).thenReturn(fileMock);
        when(fileMock.getAbsolutePath()).thenReturn("path");

        String result = gitServiceCommandImpl.cloneProject();

        assertNotNull(result);
        verify(pullCommandMock).setRemoteBranchName("develop");
    }

    @Test
    public void cloneProjectIfProjectNotExists() throws GitAPIException, IOException {
        when(localGitRepositoryServiceMock.isRepositoryExists()).thenReturn(false);

        CloneCommand cloneCommandMock = mock(CloneCommand.class);
        when(localGitRepositoryServiceMock.getRepositoryUrl()).thenReturn("uri");
        when(cloneCommandMock.setURI(anyString())).thenReturn(cloneCommandMock);
        when(cloneCommandMock.setCredentialsProvider(any())).thenReturn(cloneCommandMock);
        when(cloneCommandMock.setDirectory(any())).thenReturn(cloneCommandMock);
        when(cloneCommandMock.setCloneAllBranches(true)).thenReturn(cloneCommandMock);
        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(cloneCommandMock);
            when(cloneCommandMock.call()).thenReturn(localGitMock);

            checkoutCommand(true);

            File fileMock = mock(File.class);
            when(localGitMock.getRepository()).thenReturn(repositoryMock);
            when(repositoryMock.getDirectory()).thenReturn(fileMock);
            when(fileMock.getAbsolutePath()).thenReturn("path");

            String result = gitServiceCommandImpl.cloneProject();

            assertNotNull(result);
            verify(cloneCommandMock).setURI("uri");
            verify(cloneCommandMock).setCloneAllBranches(true);
            verify(cloneCommandMock).call();
        }
    }

    @Test
    void testCloneProjectWithException() throws GitAPIException {
        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            CloneCommand cloneCommandMock = mock(CloneCommand.class);
            gitMock.when(Git::cloneRepository).thenReturn(cloneCommandMock);
            when(localGitRepositoryServiceMock.isRepositoryExists()).thenReturn(false);
            when(cloneCommandMock.setURI(anyString())).thenReturn(cloneCommandMock);
            when(cloneCommandMock.call()).thenThrow(new RuntimeException("Error"));

            assertThrows(NullPointerException.class, () -> gitServiceCommandImpl.cloneProject());
        }
    }

    @Test
    void testCreateCommitSuccess() throws GitAPIException, IOException {
        CommitCommand commitCommandMock = mock(CommitCommand.class);
        StatusCommand statusCommandMock = mock(StatusCommand.class);
        Status statusMock = mock(Status.class);
        Set<String> added = new HashSet<>();
        Set<String> changed = new HashSet<>();
        added.add("file1");
        changed.add("file1");

        afterCommit(statusMock);
        when(statusMock.getAdded()).thenReturn(added);
        when(statusMock.getChanged()).thenReturn(changed);

        when(localGitMock.commit()).thenReturn(commitCommandMock);
        when(commitCommandMock.setMessage(anyString())).thenReturn(commitCommandMock);
        when(commitCommandMock.setCredentialsProvider(any())).thenReturn(commitCommandMock);
        when(commitCommandMock.call()).thenReturn(mock(RevCommit.class));

        gitServiceCommandImpl.createCommit();

        assertNotNull(commitCommandMock);
        assertNotNull(statusCommandMock);
        verify(commitCommandMock).call();
    }

    @Test
    void testCreateCommitWithExceptionNotChanges() throws GitAPIException, IOException {
        Status statusMock = mock(Status.class);
        afterCommit(statusMock);
        when(statusMock.getAdded()).thenReturn(new HashSet<>());
        when(statusMock.getChanged()).thenReturn(new HashSet<>());

        assertThrows(RuntimeException.class, () -> gitServiceCommandImpl.createCommit());
    }

    @Test
    void testAddFileSuccess() throws GitAPIException, IOException {
        Status statusMock = mock(Status.class);
        AddCommand addCommandMock = mock(AddCommand.class);
        Set<String> added = new HashSet<>();
        added.add(docPath);

        afterCommit(statusMock);
        when(localGitMock.add()).thenReturn(addCommandMock);
        when(addCommandMock.addFilepattern(docPath)).thenReturn(addCommandMock);
        when(addCommandMock.call()).thenReturn(mock(DirCache.class));
        when(statusMock.getRemoved()).thenReturn(new HashSet<>());
        when(statusMock.getChanged()).thenReturn(new HashSet<>());
        when(statusMock.getMissing()).thenReturn(new HashSet<>());
        when(statusMock.getAdded()).thenReturn(added);

        gitServiceCommandImpl.addFile(docPath);
        assertNotNull(addCommandMock);
        assertNotNull(statusMock);
        verify(addCommandMock).call();
    }

    @Test
    void testAddFileWithExceptionNotChanges() throws GitAPIException, IOException {
        Status statusMock = mock(Status.class);
        AddCommand addCommandMock = mock(AddCommand.class);

        afterCommit(statusMock);
        when(localGitMock.add()).thenReturn(addCommandMock);
        when(addCommandMock.addFilepattern(docPath)).thenReturn(addCommandMock);
        when(addCommandMock.call()).thenReturn(mock(DirCache.class));
        when(statusMock.getAdded()).thenReturn(new HashSet<>());

        assertThrows(RuntimeException.class, () -> gitServiceCommandImpl.addFile(docPath));
    }

    @Test
    void testCreateMergeRequestSuccess() throws GitAPIException, GitLabApiException, IOException {
        PushCommand pushCommandMock = mock(PushCommand.class);
        GitLabApi gitLabApiMock = mock(GitLabApi.class);
        Project projectMock = mock(Project.class);
        UserApi userApiMock = mock(UserApi.class);
        User userMock = mock(User.class);
        MergeRequestApi mergeRequestApiMock = mock(MergeRequestApi.class);
        MergeRequest mergeRequestMock = mock(MergeRequest.class);
        Iterable<PushResult> pushResults = new ArrayList<>();

        when(localGitRepositoryServiceMock.getClonedLocalGit()).thenReturn(localGitMock);
        when(localGitMock.push()).thenReturn(pushCommandMock);
        when(pushCommandMock.setCredentialsProvider(any())).thenReturn(pushCommandMock);
        when(pushCommandMock.call()).thenReturn(pushResults);

        when(localGitRepositoryServiceMock.getGitLabApi()).thenReturn(gitLabApiMock);
        when(localGitRepositoryServiceMock.getGitLabApiProject()).thenReturn(projectMock);
        when(localGitRepositoryServiceMock.getLocalGitRepository()).thenReturn(repositoryMock);
        when(repositoryMock.getBranch()).thenReturn("develop");
        when(projectMock.getId()).thenReturn(1L);
        when(gitLabApiMock.getUserApi()).thenReturn(userApiMock);
        when(userApiMock.getCurrentUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(1L);
        when(gitLabApiMock.getMergeRequestApi()).thenReturn(mergeRequestApiMock);
        when(
                mergeRequestApiMock.createMergeRequest(
                        anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong()
                )
        ).thenReturn(mergeRequestMock);
        when(mergeRequestMock.getWebUrl()).thenReturn("url");
        checkoutCommand(true);

        String result = gitServiceCommandImpl.createMergeRequest(mrTitle);
        assertNotNull(pushCommandMock);
        assertNotNull(gitLabApiMock);
        assertNotNull(mergeRequestMock);
        assertEquals("url", result);
        verify(pushCommandMock).call();
    }

    @Test
    void testCreateMergeRequestWithPushException() throws GitAPIException {
        PushCommand pushCommandMock = mock(PushCommand.class);

        when(localGitRepositoryServiceMock.getClonedLocalGit()).thenReturn(localGitMock);
        when(localGitMock.push()).thenReturn(pushCommandMock);
        when(pushCommandMock.setCredentialsProvider(any())).thenReturn(pushCommandMock);
        when(pushCommandMock.call()).thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class, () -> gitServiceCommandImpl.createMergeRequest(mrTitle));
    }

    private void afterCommit(Status statusMock) throws IOException, GitAPIException {
        StatusCommand statusCommandMock = mock(StatusCommand.class);

        when(localGitRepositoryServiceMock.getLocalGitRepository()).thenReturn(repositoryMock);
        when(repositoryMock.getBranch()).thenReturn("dev");
        when(localGitRepositoryServiceMock.getClonedLocalGit()).thenReturn(localGitMock);
        when(localGitMock.status()).thenReturn(statusCommandMock);
        when(statusCommandMock.call()).thenReturn(statusMock);
    }

    private void checkoutCommand(Boolean isNewLocalBranch) throws GitAPIException, IOException {
        when(localGitRepositoryServiceMock.getClonedLocalGit()).thenReturn(localGitMock);

        when(localGitMock.checkout()).thenReturn(checkoutCommandMock);
        when(checkoutCommandMock.setCreateBranch(isNewLocalBranch)).thenReturn(checkoutCommandMock);
        when(checkoutCommandMock.setName(anyString())).thenReturn(checkoutCommandMock);
        when(checkoutCommandMock.setStartPoint(anyString())).thenReturn(checkoutCommandMock);
        if (isNewLocalBranch) {
            getRemoteBranch();
        }

        when(checkoutCommandMock.call()).thenReturn(mock(Ref.class));
    }

    private void getRemoteBranch() throws IOException {
        RefDatabase refDatabaseMock = mock(RefDatabase.class);
        Ref refMock = mock(Ref.class);

        List<Ref> localRefs = new ArrayList<>();
        localRefs.add(refMock);
        when(localGitMock.getRepository()).thenReturn(repositoryMock);
        when(repositoryMock.getRefDatabase()).thenReturn(refDatabaseMock);
        when(refDatabaseMock.getRefs()).thenReturn(localRefs);
        when(refMock.getName()).thenReturn("develop");

        when(checkoutCommandMock.setStartPoint("develop")).thenReturn(checkoutCommandMock);
    }
}
