package pro.axenix_innovation.axenapi.web.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GitRepoAuthType {
    TOKEN("Токен"),
    LOGIN("Логин");

    private final String description;
}
