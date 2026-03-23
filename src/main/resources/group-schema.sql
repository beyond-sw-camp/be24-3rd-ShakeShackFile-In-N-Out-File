-- Reference schema for the group module.
-- Runtime table creation still follows JPA/Hibernate entity definitions.

CREATE TABLE IF NOT EXISTS relationship_invite (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NULL,
    email VARCHAR(255) NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationship_invite_from_user FOREIGN KEY (from_user_id) REFERENCES user (idx),
    CONSTRAINT fk_relationship_invite_to_user FOREIGN KEY (to_user_id) REFERENCES user (idx),
    INDEX idx_relationship_invite_from_user (from_user_id),
    INDEX idx_relationship_invite_to_user (to_user_id),
    INDEX idx_relationship_invite_email (email)
);

CREATE TABLE IF NOT EXISTS relationship (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationship_user FOREIGN KEY (user_id) REFERENCES user (idx),
    CONSTRAINT fk_relationship_target_user FOREIGN KEY (target_user_id) REFERENCES user (idx),
    CONSTRAINT uk_relationship_user_target UNIQUE (user_id, target_user_id),
    INDEX idx_relationship_user (user_id),
    INDEX idx_relationship_target (target_user_id)
);

CREATE TABLE IF NOT EXISTS relationship_group (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationship_group_user FOREIGN KEY (user_id) REFERENCES user (idx),
    CONSTRAINT uk_relationship_group_user_name UNIQUE (user_id, name),
    INDEX idx_relationship_group_user (user_id)
);

CREATE TABLE IF NOT EXISTS relationship_group_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    relationship_id BIGINT NOT NULL,
    group_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationship_group_mapping_relationship FOREIGN KEY (relationship_id) REFERENCES relationship (id),
    CONSTRAINT fk_relationship_group_mapping_group FOREIGN KEY (group_id) REFERENCES relationship_group (id),
    CONSTRAINT uk_relationship_group_mapping UNIQUE (relationship_id, group_id),
    INDEX idx_relationship_group_mapping_relationship (relationship_id),
    INDEX idx_relationship_group_mapping_group (group_id)
);

CREATE TABLE IF NOT EXISTS relationship_group_invite (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationship_group_invite_group FOREIGN KEY (group_id) REFERENCES relationship_group (id),
    CONSTRAINT fk_relationship_group_invite_from_user FOREIGN KEY (from_user_id) REFERENCES user (idx),
    CONSTRAINT fk_relationship_group_invite_to_user FOREIGN KEY (to_user_id) REFERENCES user (idx),
    INDEX idx_relationship_group_invite_group (group_id),
    INDEX idx_relationship_group_invite_to_user (to_user_id)
);

ALTER TABLE notification_list
    ADD COLUMN IF NOT EXISTS reference_id BIGINT NULL;
