-- Optional manual schema. Hibernate ddl-auto=update will also create/update these tables.

CREATE TABLE IF NOT EXISTS risk_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    weight INTEGER NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS risk_assessments (
    id UUID PRIMARY KEY,
    release_id UUID NOT NULL,
    score INTEGER NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS risk_factors (
    id UUID PRIMARY KEY,
    risk_assessment_id UUID NOT NULL REFERENCES risk_assessments (id) ON DELETE CASCADE,
    rule_name VARCHAR(128) NOT NULL,
    weight_applied INTEGER NOT NULL,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_risk_assessments_release_id ON risk_assessments (release_id);
CREATE INDEX IF NOT EXISTS idx_risk_assessments_calculated_at ON risk_assessments (release_id, calculated_at DESC);
CREATE INDEX IF NOT EXISTS idx_risk_factors_assessment_id ON risk_factors (risk_assessment_id);
