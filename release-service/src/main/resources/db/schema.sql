-- Optional manual schema. Hibernate ddl-auto=update will also create/update these tables.

CREATE TABLE IF NOT EXISTS releases (
    id UUID PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    version VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    deployed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS release_changes (
    id UUID PRIMARY KEY,
    release_id UUID NOT NULL REFERENCES releases (id) ON DELETE CASCADE,
    file_name VARCHAR(1024) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    lines_added INTEGER NOT NULL DEFAULT 0,
    lines_removed INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pull_request_info (
    id UUID PRIMARY KEY,
    release_id UUID NOT NULL REFERENCES releases (id) ON DELETE CASCADE,
    repo_name VARCHAR(255) NOT NULL,
    pr_number INTEGER NOT NULL,
    title VARCHAR(512),
    author VARCHAR(255),
    merged_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_releases_project_name ON releases (project_name);
CREATE INDEX IF NOT EXISTS idx_releases_status ON releases (status);
CREATE INDEX IF NOT EXISTS idx_release_changes_release_id ON release_changes (release_id);
CREATE INDEX IF NOT EXISTS idx_pull_request_info_release_id ON pull_request_info (release_id);
