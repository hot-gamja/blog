-- Users table (Google OAuth 기반)
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    profile_image TEXT,
    provider      VARCHAR(50)  NOT NULL,
    provider_id   VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP,
    deleted_at    TIMESTAMP,

    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT uk_users_email                UNIQUE (email)
);

-- Comments table
CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    post_slug  VARCHAR(255) NOT NULL,
    user_id    BIGINT       NOT NULL,
    parent_id  BIGINT,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_id)
        REFERENCES comments(id)
        ON DELETE RESTRICT
);

-- Indexes
CREATE INDEX idx_comments_post_slug_created_at ON comments(post_slug, created_at);
CREATE INDEX idx_comments_parent_id_created_at ON comments(parent_id, created_at);
CREATE INDEX idx_comments_user_id              ON comments(user_id);

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_comments_updated_at
BEFORE UPDATE ON comments
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
