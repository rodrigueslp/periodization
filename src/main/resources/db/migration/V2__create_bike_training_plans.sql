CREATE TABLE bike_training_plans (
    plan_id VARCHAR(36) PRIMARY KEY,
    athlete_name VARCHAR(255) NOT NULL,
    athlete_age INTEGER NOT NULL,
    athlete_weight DOUBLE PRECISION NOT NULL,
    athlete_height INTEGER NOT NULL,
    experience_level VARCHAR(50) NOT NULL,
    training_goal VARCHAR(100) NOT NULL,
    dias_disponiveis INTEGER NOT NULL,
    volume_semanal_atual INTEGER NOT NULL,
    tipo_bike VARCHAR(50),
    ftp_atual INTEGER,
    potencia_media_atual INTEGER,
    melhor_tempo_40km VARCHAR(20),
    melhor_tempo_100km VARCHAR(20),
    melhor_tempo_160km VARCHAR(20),
    tempo_objetivo VARCHAR(100),
    data_prova VARCHAR(50),
    historico_lesoes TEXT,
    experiencia_anterior TEXT,
    preferencia_treino VARCHAR(50),
    equipamentos_disponiveis TEXT,
    zona_treino_preferida VARCHAR(50),
    plan_duration INTEGER NOT NULL,
    plan_content TEXT NOT NULL DEFAULT '',
    excel_file_path VARCHAR(500) NOT NULL DEFAULT '',
    pdf_file_path VARCHAR(500) DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'PAYMENT_PENDING',
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT,
    start_date DATE,
    end_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bike_training_plans_user_id ON bike_training_plans(user_id);
CREATE INDEX idx_bike_training_plans_status ON bike_training_plans(status);
CREATE INDEX idx_bike_training_plans_created_at ON bike_training_plans(created_at);
