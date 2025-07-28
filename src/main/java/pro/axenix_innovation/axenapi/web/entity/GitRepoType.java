package pro.axenix_innovation.axenapi.web.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GitRepoType {
    CODE("Код"),
    DOCUMENTATION("Документация");

    private final String description;
}
