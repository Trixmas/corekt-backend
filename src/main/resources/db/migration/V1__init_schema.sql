CREATE TABLE users
(
    user_id       UUID                     NOT NULL,
    email         VARCHAR(255)             NOT NULL UNIQUE,
    username      VARCHAR(50)              NOT NULL,
    mistral_token VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id)
);

CREATE TABLE achievement
(
    id          VARCHAR(50)              NOT NULL,
    name        VARCHAR(100)             NOT NULL,
    description VARCHAR(200)             NOT NULL,
    hidden      BOOLEAN                  NOT NULL,
    criteria    JSONB                    NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE user_achievements
(
    user_id        UUID                     NOT NULL,
    achievement_id VARCHAR(50)              NOT NULL,
    acquired_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, achievement_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id),
    FOREIGN KEY (achievement_id) REFERENCES achievement (id)
);

CREATE TABLE project
(
    project_id    UUID                     NOT NULL,
    project_title VARCHAR(50)              NOT NULL,
    description   TEXT                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    owner_id      UUID                     NOT NULL,
    PRIMARY KEY (project_id),
    FOREIGN KEY (owner_id) REFERENCES users (user_id)
);

CREATE TABLE version
(
    version_id     UUID                     NOT NULL,
    version_number INTEGER                  NOT NULL,
    project_id     UUID                     NOT NULL,
    summary        TEXT,
    title          VARCHAR(50)              NOT NULL,
    score          INTEGER,
    status         VARCHAR(50)              NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (version_id),
    UNIQUE (project_id, version_number),
    FOREIGN KEY (project_id) REFERENCES project (project_id)
);

CREATE TABLE file
(
    file_id           UUID        NOT NULL,
    file_type         VARCHAR(50) NOT NULL,
    file_name         VARCHAR(50) NOT NULL,
    file              BYTEA,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    version_id        UUID,
    PRIMARY KEY (file_id),
    UNIQUE (version_id, file_type),
    FOREIGN KEY (version_id) REFERENCES version (version_id)
);

CREATE TABLE parsed_data
(
    parsed_data_id UUID         NOT NULL,
    data_type      VARCHAR(50)  NOT NULL,
    content        VARCHAR(500) NOT NULL,
    file_id        UUID         NOT NULL,
    PRIMARY KEY (parsed_data_id),
    FOREIGN KEY (file_id) REFERENCES file (file_id)
);

CREATE TABLE data_link
(
    link_id               UUID        NOT NULL,
    match                 VARCHAR(50) NOT NULL,
    severity              VARCHAR(50),
    confidence_score      INTEGER,
    justification         TEXT,
    suggestion            TEXT,
    source_parsed_data_id UUID        NOT NULL,
    target_parsed_data_id UUID        NOT NULL,
    PRIMARY KEY (link_id),
    FOREIGN KEY (source_parsed_data_id) REFERENCES parsed_data (parsed_data_id),
    FOREIGN KEY (target_parsed_data_id) REFERENCES parsed_data (parsed_data_id)
);

CREATE TABLE ai_analysis
(
    file_id       UUID NOT NULL,
    prompt        TEXT,
    quality_score INTEGER,
    summary       TEXT,
    issues        TEXT,
    suggestions   TEXT,
    PRIMARY KEY (file_id),
    FOREIGN KEY (file_id) REFERENCES file (file_id)
);
