package pro.axenix_innovation.axenapi.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.entity.GitRepoType;
import pro.axenix_innovation.axenapi.web.exception.AxenApiException;
import pro.axenix_innovation.axenapi.web.mapper.GitRepoMapper;
import pro.axenix_innovation.axenapi.web.model.GitRepoDto;
import pro.axenix_innovation.axenapi.web.repository.GitRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitRepositoryService {

    private final GitRepository gitRepository;
    private final GitRepoMapper gitRepoMapper;

    private final MessageHelper messageHelper;

    public GitRepo getDocumentationGitRepository() {
        return gitRepository.findFirstByType(GitRepoType.DOCUMENTATION);
    }

    @Transactional
    public List<GitRepo> getList() {
        List<GitRepo> gitRepos = gitRepository.findAll();
        log.info(messageHelper.getMessage("axenapi.info.git.get.list"), gitRepos.size());
        return gitRepos;
    }

    public GitRepo getById(String id) {
        log.info(messageHelper.getMessage("axenapi.info.git.get"), id);
        return gitRepository.findById(id)
                .orElseThrow(
                        () -> new AxenApiException.NotFoundException(
                                String.format("git repository with id %s not found", id)));
    }

    @Transactional
    public void delete(String id) {
        log.info(messageHelper.getMessage("axenapi.info.git.delete"), id);
        gitRepository.deleteById(id);
    }

    @Transactional
    public GitRepo put(String id, GitRepoDto gitRepoDto) {
        log.info(messageHelper.getMessage("axenapi.info.git.update"), id);
        GitRepo entity = getById(id);
        return gitRepository.save(fill(entity, gitRepoDto));
    }

    @Transactional
    public GitRepo create(GitRepoDto gitRepoDto) {
        log.info(messageHelper.getMessage("axenapi.info.git.create"), gitRepoDto);
        GitRepo entity = new GitRepo();
        return gitRepository.save(fill(entity, gitRepoDto));
    }

    private GitRepo fill(GitRepo entity, GitRepoDto gitRepoDto) {
        entity.setService(gitRepoDto.getService());
        entity.setUrl(gitRepoDto.getUrl());
        entity.setAuthType(gitRepoMapper.toGitRepoAuthType(gitRepoDto.getAuthType()));
        entity.setToken(gitRepoDto.getToken());
        entity.setLogin(gitRepoDto.getLogin());
        entity.setPassword(gitRepoDto.getPassword());
        entity.setStatus(gitRepoDto.getStatus());
        entity.setType(gitRepoMapper.toGitRepoType(gitRepoDto.getType()));
        return entity;
    }
}
