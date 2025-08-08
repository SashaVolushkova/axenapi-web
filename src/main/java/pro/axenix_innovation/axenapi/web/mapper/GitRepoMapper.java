package pro.axenix_innovation.axenapi.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pro.axenix_innovation.axenapi.web.entity.GitRepo;
import pro.axenix_innovation.axenapi.web.entity.GitRepoAuthType;
import pro.axenix_innovation.axenapi.web.entity.GitRepoType;
import pro.axenix_innovation.axenapi.web.model.GitRepoDto;

@Mapper(componentModel = "spring")
public interface GitRepoMapper {

    GitRepoType toGitRepoType(GitRepoDto.TypeEnum type);

    GitRepoAuthType toGitRepoAuthType(GitRepoDto.AuthTypeEnum authType);

    @Mapping(target = "type", source = "type")
    GitRepoDto toGitRepoDto(GitRepo entity);

}
