CREATE DATABASE IF NOT EXISTS resumeanalyser;
USE resumeanalyser;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    locked BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS resume (
    id BIGINT NOT NULL AUTO_INCREMENT,
    file_name VARCHAR(255),
    content LONGTEXT,
    score INT NOT NULL,
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_resume_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    final_score INT NOT NULL,
    keyword_match_score INT NOT NULL,
    skill_match_score INT NOT NULL,
    completeness_score INT NOT NULL,
    status VARCHAR(255),
    insight VARCHAR(255),
    file_name VARCHAR(255),
    job_description TEXT,
    created_at DATETIME(6),
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_analysis_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS analysis_matched_skills (
    analysis_id BIGINT NOT NULL,
    skill VARCHAR(255),
    CONSTRAINT fk_matched_skills_analysis
        FOREIGN KEY (analysis_id) REFERENCES analyses(id)
);

CREATE TABLE IF NOT EXISTS analysis_missing_skills (
    analysis_id BIGINT NOT NULL,
    skill VARCHAR(255),
    CONSTRAINT fk_missing_skills_analysis
        FOREIGN KEY (analysis_id) REFERENCES analyses(id)
);

CREATE TABLE IF NOT EXISTS analysis_recommended_jobs (
    analysis_id BIGINT NOT NULL,
    job_role VARCHAR(255),
    CONSTRAINT fk_recommended_jobs_analysis
        FOREIGN KEY (analysis_id) REFERENCES analyses(id)
);
