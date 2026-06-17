-- 添加歌手国籍字段
ALTER TABLE artists ADD COLUMN nationality VARCHAR(100) COMMENT '国籍，如 中国/美国/英国/日本/韩国 等';
ALTER TABLE artists ADD COLUMN original_name VARCHAR(200) COMMENT '原始外文名';

-- 为现有歌手更新国籍和原始名（示例数据）
-- 后续可通过管理后台编辑
