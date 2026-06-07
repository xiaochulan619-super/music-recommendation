-- ============================================
-- 音乐推荐系统 - 数据库建表脚本
-- 数据库名: music_recommend
-- ============================================

CREATE DATABASE IF NOT EXISTS music_recommend
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE music_recommend;

-- 1. 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    avatar_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 歌手表
CREATE TABLE artists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    image_url VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 歌曲表
CREATE TABLE songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    artist_id BIGINT,
    album VARCHAR(200),
    duration INT COMMENT '秒',
    cover_url VARCHAR(255),
    audio_url VARCHAR(255),
    play_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (artist_id) REFERENCES artists(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 收藏表
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_song (user_id, song_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (song_id) REFERENCES songs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 评分表
CREATE TABLE ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    score TINYINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_song (user_id, song_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (song_id) REFERENCES songs(id),
    CONSTRAINT chk_score CHECK (score >= 1 AND score <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 播放历史表
CREATE TABLE play_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    played_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (song_id) REFERENCES songs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 评论表（楼中楼）
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (song_id) REFERENCES songs(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 标签字典表
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL COMMENT 'genre|mood|artist|era'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 歌曲标签关联表
CREATE TABLE song_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_song_tag (song_id, tag_id),
    FOREIGN KEY (song_id) REFERENCES songs(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 训练任务表
CREATE TABLE train_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending|running|completed|failed',
    started_at DATETIME,
    finished_at DATETIME,
    message TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 推荐结果表
CREATE TABLE recommendation_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    score FLOAT COMMENT '推荐得分',
    source VARCHAR(20) COMMENT 'ncf|tag|fusion',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (song_id) REFERENCES songs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 热门榜单表
CREATE TABLE charts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL,
    chart_type VARCHAR(20) NOT NULL COMMENT 'daily|weekly|total',
    play_count INT DEFAULT 0,
    fav_count INT DEFAULT 0,
    hot_score FLOAT COMMENT '综合热度分',
    rank_date DATE NOT NULL,
    FOREIGN KEY (song_id) REFERENCES songs(id),
    UNIQUE KEY uk_song_type_date (song_id, chart_type, rank_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
