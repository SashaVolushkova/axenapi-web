package pro.axenix_innovation.axenapi.web.service.git.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;
import pro.axenix_innovation.axenapi.web.service.git.LocalGitRepositoryService;
import pro.axenix_innovation.axenapi.web.service.git.GitServiceCommand;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_COMMIT_DOC_NOT_CHANGES;
import static pro.axenix_innovation.axenapi.web.service.git.LocalGitRepositoryService.DEFAULT_BRANCH;
import static pro.axenix_innovation.axenapi.web.service.git.LocalGitRepositoryService.DOC_REPOSITORY_PATH;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitServiceCommandImpl implements GitServiceCommand {

    private Git localGit;
    private final MessageHelper messageHelper;

    private final LocalGitRepositoryService localGitRepositoryService;

    @Override
    public String cloneProject() {
        try {
            if (localGitRepositoryService.isRepositoryExists()) {
                checkoutCommand(DEFAULT_BRANCH, false);
                pullCommand(DEFAULT_BRANCH);
            } else {
                cloneCommand();
                checkoutCommand(DEFAULT_BRANCH, true);
            }
            checkoutCommand(String.valueOf(UUID.randomUUID()), true);

            return localGit.getRepository().getDirectory().getAbsolutePath().replaceAll(".git", "");
        } catch (GitAPIException | GitLabApiException | IOException e) {
            throw new RuntimeException("Failed to clone repository: %s".formatted(e.getMessage()), e);
        }
    }

    @Override
    public void createCommit() {
        try {
            if (localGitRepositoryService.getLocalGitRepository().getBranch().equals(DEFAULT_BRANCH)) {
                checkoutCommand(String.valueOf(UUID.randomUUID()), true);
            }
            commitCommand();
        } catch (GitAPIException | GitLabApiException | IOException e) {
            throw new RuntimeException("Failed to create commit: %s".formatted(e.getMessage()), e);
        }
    }

    @Override
    public void addFile(String docPath) {
        try {
            if (localGitRepositoryService.getLocalGitRepository().getBranch().equals(DEFAULT_BRANCH)) {
                checkoutCommand(String.valueOf(UUID.randomUUID()), true);
            }
            addCommand(docPath);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException("Failed to add file: %s".formatted(e.getMessage()), e);
        }

    }

    @Override
    public String createMergeRequest(String title) {
        try {
            pushCommand();
            return createMergeRequestCommand(title);
        } catch (GitAPIException | GitLabApiException | IOException e) {
            throw new RuntimeException("Failed to create merge request: %s".formatted(e.getMessage()), e);
        }
    }

    private void cloneCommand() throws GitLabApiException, GitAPIException {
        Git.cloneRepository()
            .setURI(localGitRepositoryService.getRepositoryUrl())
            .setCredentialsProvider(localGitRepositoryService.getLocalCredentialsProvider())
            .setDirectory(new File(DOC_REPOSITORY_PATH))
            .setCloneAllBranches(true)
            .call();
    }

    private void checkoutCommand(String checkoutBranch, Boolean isNewLocalBranch) throws GitAPIException, IOException {
        localGit = localGitRepositoryService.getClonedLocalGit();

        CheckoutCommand checkoutCommand = localGit.checkout()
                .setCreateBranch(isNewLocalBranch)
                .setName(checkoutBranch);

        if (isNewLocalBranch) {
            checkoutCommand.setStartPoint(getRemoteBranch(DEFAULT_BRANCH, localGit));
        }
        checkoutCommand.call();
    }

    private void addCommand(String docPath) throws GitAPIException {
        localGit = localGitRepositoryService.getClonedLocalGit();
        localGit.add().addFilepattern(docPath).call();
        Status status = localGit.status().call();

        if (!status.getRemoved().isEmpty() || !status.getChanged().isEmpty() || !status.getMissing().isEmpty()) {
            localGit.add()
                    .addFilepattern(docPath.substring(0, docPath.lastIndexOf('/')))
                    .setUpdate(true)
                    .call();
        }

        boolean isAddedEmpty = status.getAdded().isEmpty() || !status.getAdded().contains(docPath);
        if (isAddedEmpty && status.getChanged().isEmpty()) {
            throw new RuntimeException("No changes to add");
        }
    }

    private void commitCommand() throws GitAPIException, GitLabApiException {
        localGit = localGitRepositoryService.getClonedLocalGit();

        Status status = localGit.status().call();
        Set<String> added = status.getAdded();
        if (added.isEmpty() && status.getChanged().isEmpty()) {
            log.error(messageHelper.getMessage(ERROR_COMMIT_DOC_NOT_CHANGES));
            throw new RuntimeException("No changes for the commit");
        }
        localGit.commit()
                .setMessage(String.valueOf(UUID.randomUUID()))
                .setCredentialsProvider(localGitRepositoryService.getLocalCredentialsProvider())
                .call();
    }

    private void pushCommand() throws GitLabApiException, GitAPIException {
        localGit = localGitRepositoryService.getClonedLocalGit();
        localGit.push()
                .setCredentialsProvider(localGitRepositoryService.getLocalCredentialsProvider())
                .call();
    }

    private PullResult pullCommand(String pullBranch) throws GitLabApiException, GitAPIException {
        localGit = localGitRepositoryService.getClonedLocalGit();

        return localGit.pull()
                .setCredentialsProvider(localGitRepositoryService.getLocalCredentialsProvider())
                .setRemoteBranchName(getRemoteBranch(pullBranch, localGit))
                .call();
    }

    private String createMergeRequestCommand(String title) throws GitLabApiException, IOException, GitAPIException {
        localGit = localGitRepositoryService.getClonedLocalGit();
        GitLabApi gitLabApi = localGitRepositoryService.getGitLabApi();
        Project project = localGitRepositoryService.getGitLabApiProject();
        MergeRequest mergeRequest = gitLabApi.getMergeRequestApi().createMergeRequest(
                project.getId(),
                localGitRepositoryService.getLocalGitRepository().getBranch(),
                DEFAULT_BRANCH,
                title,
                "",
                gitLabApi.getUserApi().getCurrentUser().getId()
        );
        checkoutCommand(String.valueOf(UUID.randomUUID()), true);

        return mergeRequest.getWebUrl();
    }

    private String getRemoteBranch(String sourceBranch, Git localGit) {
        try {
            List<Ref> allRefs = localGit.getRepository().getRefDatabase().getRefs();
            Optional<Ref> refSourceBranch = allRefs.stream()
                    .filter(ref -> ref.getName().contains(sourceBranch))
                    .findFirst();

            return refSourceBranch.map(Ref::getName).orElseThrow(() -> new RuntimeException("Branch not found"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getConflictingFiles(
            String sourceBranch,
            String targetBranch,
            String currentBranch,
            Git localGit
    ) {
        try {
            checkoutCommand(currentBranch, false);
            PullResult pullResult = pullCommand("refs/heads/" + targetBranch);
            MergeResult mergeResult = pullResult.getMergeResult();

            Map<String, String> conflictingFiles = new HashMap<>();
            for (String path : mergeResult.getConflicts().keySet()) {
                byte[] fileBytes = Files.readAllBytes(
                        Paths.get(localGit.getRepository().getDirectory().getPath() + "/" + path)
                );
                String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
                conflictingFiles.put(path, fileContent);
            }

            return conflictingFiles;
        } catch (IOException | GitAPIException | GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void resolveMergeConflicts(Map<String, String> updatedFiles, Git localGit) throws GitAPIException, GitLabApiException {
        for (Map.Entry<String, String> file : updatedFiles.entrySet()) {
            try (FileWriter fileWriter = new FileWriter(localGit.getRepository().getDirectory().getPath() + "/" + file.getKey())) {
                fileWriter.write(file.getValue());
            } catch (IOException e) {
                throw new RuntimeException("Could not write updated content", e);
            }
        }

        commitCommand();
        pushCommand();
    }

}
