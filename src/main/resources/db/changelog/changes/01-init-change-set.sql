create table if not exists gitrepo
(
    id                  varchar(255) default RANDOM_UUID() PRIMARY KEY,
    token               varchar(255),
    url                 varchar(255),
    type                varchar(255)
);

INSERT INTO gitrepo (token, url, type) VALUES ('token', 'link', 'DOCUMENTATION');

