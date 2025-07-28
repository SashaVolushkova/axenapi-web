package pro.axenix_innovation.axenapi.web.service.git;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;

public interface GitServiceCommand {

    String cloneProject() throws GitAPIException, GitLabApiException;

    void createCommit() throws GitAPIException, GitLabApiException;

    void addFile(String docPath) throws GitAPIException;

    String createMergeRequest(String title) throws GitAPIException, GitLabApiException;
}
