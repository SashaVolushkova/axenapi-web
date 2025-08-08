package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.entity.GitRepoType;
import pro.axenix_innovation.axenapi.web.model.GitRepoDto;
import pro.axenix_innovation.axenapi.web.repository.GitRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerGitRepositoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitRepository gitRepository;

    private GitRepo createGitRepo() {
        GitRepo entity = new GitRepo();
        entity.setUrl("testUrl");
        entity.setToken("testToken");
        entity.setType(GitRepoType.CODE);
        return gitRepository.saveAndFlush(entity);
    }

    private GitRepoDto createGitRepoDto(String id) {
        return GitRepoDto.builder()
                .id(id)
                .url("testUrl")
                .token("testToken")
                .type(GitRepoDto.TypeEnum.CODE)
                .build();
    }

    @Test
    void gitPost_RepositorySuccess() {
        try {
            GitRepoDto dto = createGitRepoDto(null);
            MvcResult result = mockMvc.perform(post("/git")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            GitRepoDto savedEntity = objectMapper.readValue(responseBody, GitRepoDto.class);

            assertNotNull(savedEntity);
            assertNotNull(savedEntity.getId());
            assertEquals(dto.getToken(), savedEntity.getToken());
            assertEquals(dto.getUrl(), savedEntity.getUrl());
            assertEquals(dto.getType(), savedEntity.getType());
            assertTrue(gitRepository.existsById(savedEntity.getId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void gitIdGet_RepositoryByIdSuccess() {
        try {
            GitRepo entity = createGitRepo();

            MvcResult result = mockMvc.perform(get("/git/{id}", entity.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            GitRepoDto returnedRepo = objectMapper.readValue(responseBody, GitRepoDto.class);

            assertNotNull(returnedRepo);
            assertEquals(entity.getId(), returnedRepo.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void gitIdPut_RepositoryByIdSuccess() {
        try {
            GitRepo entity = createGitRepo();
            String id = entity.getId();
            GitRepoDto dto = createGitRepoDto(id);

            MvcResult result = mockMvc.perform(put("/git/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            GitRepoDto updatedEntity = objectMapper.readValue(responseBody, GitRepoDto.class);

            assertNotNull(updatedEntity);
            assertEquals(entity.getId(), updatedEntity.getId());
            assertEquals(dto.getToken(), updatedEntity.getToken());
            assertEquals(dto.getUrl(), updatedEntity.getUrl());

            GitRepo savedEntity = gitRepository.findById(entity.getId()).orElse(null);
            assertNotNull(savedEntity);
            assertEquals(dto.getToken(), savedEntity.getToken());
            assertEquals(dto.getUrl(), savedEntity.getUrl());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void gitIdDelete_RepositoryByIdSuccess() {
        try {
            GitRepo entity = createGitRepo();

            String id = entity.getId();
            assertTrue(gitRepository.existsById(id));

            mockMvc.perform(delete("/git/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            assertFalse(gitRepository.existsById(entity.getId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void gitGet_RepositorySuccess() {
        try {
            createGitRepo();
            createGitRepo();
            createGitRepo();

            MvcResult result = mockMvc.perform(get("/git")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            List<GitRepoDto> entities = objectMapper.readValue(responseBody, new TypeReference<List<GitRepoDto>>() {
            });

            assertNotNull(entities);
            assertEquals(5, entities.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
