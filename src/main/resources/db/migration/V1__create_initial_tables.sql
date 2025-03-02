CREATE TABLE training_plans (
    plan_id VARCHAR(36) PRIMARY KEY,
    athlete_name VARCHAR(255) NOT NULL,
    athlete_age INTEGER NOT NULL,
    athlete_weight DOUBLE PRECISION NOT NULL,
    athlete_height INTEGER NOT NULL,
    experience_level VARCHAR(50) NOT NULL,
    training_goal VARCHAR(50) NOT NULL,
    availability INTEGER NOT NULL,
    injuries TEXT,
    training_history TEXT,
    plan_duration INTEGER NOT NULL,
    plan_content TEXT NOT NULL,
    excel_data BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE benchmark_data (
    id SERIAL PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL REFERENCES training_plans(plan_id) ON DELETE CASCADE,
    back_squat DOUBLE PRECISION,
    deadlift DOUBLE PRECISION,
    clean DOUBLE PRECISION,
    snatch DOUBLE PRECISION,
    fran VARCHAR(20),
    grace VARCHAR(20)
);

CREATE INDEX idx_plan_id ON benchmark_data(plan_id);