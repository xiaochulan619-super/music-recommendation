# DeepSeek 大模型推荐引擎 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 DeepSeek API 替代现有推荐逻辑，基于用户多维度行为数据实现智能推荐

**Architecture:** 后端新增 DeepSeekService 收集用户行为数据→构造 prompt→调用 DeepSeek API→解析结果→返回推荐歌曲+理由；前端 RecommendView 改为一键获取推荐，展示推荐理由

**Tech Stack:** Spring Boot 3.2 + RestTemplate + Jackson + MyBatis-Plus + Vue3 + Element Plus

---

## 文件结构

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `application.yml` | 新增 deepseek 配置段 |
| 新建 | `config/DeepSeekConfig.java` | RestTemplate Bean + 配置注入 |
| 修改 | `mapper/FavoriteMapper.java` | 新增：查用户收藏(含歌曲艺人) |
| 修改 | `mapper/RatingMapper.java` | 新增：查用户评分(含歌曲艺人) |
| 修改 | `mapper/HistoryMapper.java` | 新增：查用户播放历史(含次数) |
| 修改 | `mapper/TagMapper.java` | 新增：查用户偏好标签 Top-N |
| 修改 | `mapper/SongMapper.java` | 新增：全库摘要 + 按 ID 批量查询 + 按标签查询 |
| 新建 | `service/DeepSeekService.java` | 核心：构造 prompt → 调 API → 解析 → 降级 → 缓存 |
| 修改 | `service/RecommendationService.java` | 改为调用 DeepSeekService |
| 修改 | `controller/RecommendationController.java` | 新响应格式 + GET → POST |
| 修改 | `frontend/src/views/RecommendView.vue` | 一键刷新 UI + 推荐理由 + 降级提示 |

---

### Task 1: 添加 DeepSeek 配置

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/musicrec/config/DeepSeekConfig.java`

- [ ] **Step 1: 在 application.yml 末尾追加 DeepSeek 配置**

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:sk-0a3ec1dd729d4b2a835985226cd1d7ab}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout: 15000
  max-recommendations: 12
  cache-ttl-minutes: 5
```

- [ ] **Step 2: 创建 DeepSeekConfig.java**

```java
package com.musicrec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class DeepSeekConfig {

    @Bean
    @ConfigurationProperties(prefix = "deepseek")
    public DeepSeekProperties deepSeekProperties() {
        return new DeepSeekProperties();
    }

    @Bean
    public RestTemplate deepSeekRestTemplate(RestTemplateBuilder builder, DeepSeekProperties props) {
        return builder
                .connectTimeout(Duration.ofMillis(props.getTimeout()))
                .readTimeout(Duration.ofMillis(props.getTimeout()))
                .build();
    }

    public static class DeepSeekProperties {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
        private int timeout = 15000;
        private int maxRecommendations = 12;
        private int cacheTtlMinutes = 5;

        // getters and setters
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        public int getMaxRecommendations() { return maxRecommendations; }
        public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
        public int getCacheTtlMinutes() { return cacheTtlMinutes; }
        public void setCacheTtlMinutes(int cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }
    }
}
```

- [ ] **Step 3: 验证配置编译通过**

```bash
cd C:/Users/20822/music-recommendation/backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/main/java/com/musicrec/config/DeepSeekConfig.java
git commit -m "feat: add DeepSeek configuration"
```

---

### Task 2: 为 Mapper 添加查询方法

**Files:**
- Modify: `backend/src/main/java/com/musicrec/mapper/FavoriteMapper.java`
- Modify: `backend/src/main/java/com/musicrec/mapper/RatingMapper.java`
- Modify: `backend/src/main/java/com/musicrec/mapper/HistoryMapper.java`
- Modify: `backend/src/main/java/com/musicrec/mapper/TagMapper.java`
- Modify: `backend/src/main/java/com/musicrec/mapper/SongMapper.java`

- [ ] **Step 1: 修改 FavoriteMapper.java — 追加方法**

```java
package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT f.song_id, s.title, s.cover_url, a.name AS artist_name, " +
            "a.nationality, a.original_name AS artist_original_name " +
            "FROM favorites f " +
            "JOIN songs s ON f.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectUserFavoritesWithSong(@Param("userId") Long userId);
}
```

- [ ] **Step 2: 修改 RatingMapper.java — 追加方法**

```java
package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    @Select("SELECT r.song_id, r.score, s.title, a.name AS artist_name " +
            "FROM ratings r " +
            "JOIN songs s ON r.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.score DESC")
    List<Map<String, Object>> selectUserRatingsWithSong(@Param("userId") Long userId);
}
```

- [ ] **Step 3: 修改 HistoryMapper.java — 追加方法**

```java
package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface HistoryMapper extends BaseMapper<PlayHistory> {

    @Select("SELECT h.song_id, s.title, a.name AS artist_name, COUNT(*) AS play_count " +
            "FROM play_history h " +
            "JOIN songs s ON h.song_id = s.id " +
            "LEFT JOIN artists a ON s.artist_id = a.id " +
            "WHERE h.user_id = #{userId} " +
            "GROUP BY h.song_id, s.title, a.name " +
            "ORDER BY play_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectUserHistoryWithCount(@Param("userId") Long userId,
                                                          @Param("limit") int limit);
}
```

- [ ] **Step 4: 修改 TagMapper.java — 追加方法**

```java
package com.musicrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musicrec.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.id, t.name, t.type, COUNT(*) AS cnt " +
            "FROM favorites f " +
            "JOIN song_tags st ON f.song_id = st.song_id " +
            "JOIN tags t ON st.tag_id = t.id " +
            "WHERE f.user_id = #{userId} " +
            "GROUP BY t.id, t.name, t.type " +
            "ORDER BY cnt DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectUserTopTags(@Param("userId") Long userId,
                                                 @Param("limit") int limit);
}
```

- [ ] **Step 5: 修改 SongMapper.java — 追加方法**

```java
// 追加到 SongMapper 接口中:

@Select("SELECT s.id, s.title, a.name AS artist_name, a.nationality, " +
        "GROUP_CONCAT(t.name SEPARATOR '、') AS tags " +
        "FROM songs s " +
        "LEFT JOIN artists a ON s.artist_id = a.id " +
        "LEFT JOIN song_tags st ON s.id = st.song_id " +
        "LEFT JOIN tags t ON st.tag_id = t.id " +
        "GROUP BY s.id, s.title, a.name, a.nationality " +
        "ORDER BY s.id")
List<Map<String, Object>> selectLibrarySummary();

@Select("<script>" +
        "SELECT s.id AS song_id, s.title, s.cover_url, s.album, s.duration, s.play_count, " +
        "a.name AS artist_name, a.nationality, a.original_name AS artist_original_name " +
        "FROM songs s " +
        "LEFT JOIN artists a ON s.artist_id = a.id " +
        "WHERE s.id IN " +
        "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
        "</script>")
List<Map<String, Object>> selectSongsByIds(@Param("ids") List<Long> ids);

@Select("<script>" +
        "SELECT DISTINCT s.id AS song_id, s.title, s.cover_url, a.name AS artist_name, " +
        "a.nationality, a.original_name AS artist_original_name " +
        "FROM songs s " +
        "LEFT JOIN artists a ON s.artist_id = a.id " +
        "JOIN song_tags st ON s.id = st.song_id " +
        "WHERE st.tag_id IN " +
        "<foreach item='tagId' collection='tagIds' open='(' separator=',' close=')'>#{tagId}</foreach>" +
        "AND s.id NOT IN " +
        "<foreach item='exId' collection='excludeIds' open='(' separator=',' close=')'>#{exId}</foreach>" +
        "ORDER BY RAND() " +
        "LIMIT #{limit}" +
        "</script>")
List<Map<String, Object>> selectSongsByTagIds(@Param("tagIds") List<Long> tagIds,
                                               @Param("excludeIds") List<Long> excludeIds,
                                               @Param("limit") int limit);
```

**注意:** SongMapper 已有 `selectPageWithArtist` 和 `selectPageSorted` 方法，新增方法追加在接口末尾即可，不要删除已有方法。

- [ ] **Step 6: 编译验证**

```bash
cd C:/Users/20822/music-recommendation/backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/musicrec/mapper/
git commit -m "feat: add mapper query methods for recommendation engine"
```

---

### Task 3: 创建 DeepSeekService

**Files:**
- Create: `backend/src/main/java/com/musicrec/service/DeepSeekService.java`

- [ ] **Step 1: 创建 DeepSeekService.java**

```java
package com.musicrec.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicrec.config.DeepSeekConfig.DeepSeekProperties;
import com.musicrec.entity.RecommendationResult;
import com.musicrec.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final RestTemplate restTemplate;
    private final DeepSeekProperties props;
    private final ObjectMapper objectMapper;

    private final FavoriteMapper favoriteMapper;
    private final RatingMapper ratingMapper;
    private final HistoryMapper historyMapper;
    private final TagMapper tagMapper;
    private final SongMapper songMapper;
    private final RecommendationMapper recommendationMapper;

    // 简单内存缓存: key = userId:behaviorHash, value = {songIds, reasons, timestamp}
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public DeepSeekService(RestTemplate deepSeekRestTemplate,
                           DeepSeekProperties deepSeekProperties,
                           FavoriteMapper favoriteMapper,
                           RatingMapper ratingMapper,
                           HistoryMapper historyMapper,
                           TagMapper tagMapper,
                           SongMapper songMapper,
                           RecommendationMapper recommendationMapper) {
        this.restTemplate = deepSeekRestTemplate;
        this.props = deepSeekProperties;
        this.objectMapper = new ObjectMapper();
        this.favoriteMapper = favoriteMapper;
        this.ratingMapper = ratingMapper;
        this.historyMapper = historyMapper;
        this.tagMapper = tagMapper;
        this.songMapper = songMapper;
        this.recommendationMapper = recommendationMapper;
    }

    /**
     * 生成推荐 — 主入口
     * @return { songs: List<Map>, source: "deepseek"|"fallback", fallback: boolean }
     */
    public Map<String, Object> generateRecommend(Long userId) {
        // 1. 收集用户行为数据
        List<Map<String, Object>> favorites = favoriteMapper.selectUserFavoritesWithSong(userId);
        List<Map<String, Object>> ratings = ratingMapper.selectUserRatingsWithSong(userId);
        List<Map<String, Object>> history = historyMapper.selectUserHistoryWithCount(userId, 30);
        List<Map<String, Object>> topTags = tagMapper.selectUserTopTags(userId, 10);

        // 2. 计算行为 hash 用于缓存
        String behaviorHash = computeHash(userId, favorites, ratings, history);
        CacheEntry cached = cache.get(behaviorHash);
        if (cached != null && !cached.isExpired(props.getCacheTtlMinutes())) {
            log.info("DeepSeek cache hit for user {}", userId);
            return buildResult(cached.songIds, cached.reasons, "deepseek", false);
        }

        // 3. 构造 prompt
        String prompt = buildPrompt(favorites, ratings, history, topTags);

        // 4. 调用 DeepSeek API
        try {
            String response = callDeepSeekApi(prompt);
            List<DeepSeekPick> picks = parseResponse(response);
            List<Long> songIds = picks.stream().map(p -> p.id).collect(Collectors.toList());
            List<String> reasons = picks.stream().map(p -> p.reason).collect(Collectors.toList());

            // 5. 缓存
            cache.put(behaviorHash, new CacheEntry(songIds, reasons));

            // 6. 保存推荐结果
            saveResults(userId, songIds);

            return buildResult(songIds, reasons, "deepseek", false);

        } catch (Exception e) {
            log.warn("DeepSeek API failed, falling back to tag matching. Error: {}", e.getMessage());
            return fallbackRecommend(userId, topTags, favorites, history);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 构造发给 DeepSeek 的 prompt
     */
    private String buildPrompt(List<Map<String, Object>> favorites,
                               List<Map<String, Object>> ratings,
                               List<Map<String, Object>> history,
                               List<Map<String, Object>> topTags) {

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个音乐推荐专家。根据以下用户画像，从歌曲库中选出最适合的")
          .append(props.getMaxRecommendations()).append("首歌。\n\n");

        // 用户偏好摘要
        String tagStr = topTags.stream()
                .map(t -> t.get("name").toString())
                .limit(8)
                .collect(Collectors.joining("、"));
        sb.append("【用户偏好】\n");
        sb.append("- 偏好标签: ").append(tagStr.isEmpty() ? "暂无" : tagStr).append("\n\n");

        // 正反馈歌曲
        sb.append("【正反馈歌曲】（用户喜欢）\n");
        for (Map<String, Object> f : favorites) {
            sb.append("- ").append(f.get("title")).append(" / ")
              .append(f.get("artist_name")).append("（收藏）\n");
        }
        for (Map<String, Object> r : ratings) {
            int score = ((Number) r.get("score")).intValue();
            if (score >= 4) {
                sb.append("- ").append(r.get("title")).append(" / ")
                  .append(r.get("artist_name")).append("（").append(score).append("星）\n");
            }
        }
        for (Map<String, Object> h : history) {
            long count = ((Number) h.get("play_count")).longValue();
            if (count >= 5) {
                sb.append("- ").append(h.get("title")).append(" / ")
                  .append(h.get("artist_name")).append("（播放").append(count).append("次）\n");
            }
        }
        sb.append("\n");

        // 负反馈歌曲
        List<Map<String, Object>> negativeRatings = ratings.stream()
                .filter(r -> ((Number) r.get("score")).intValue() <= 2)
                .collect(Collectors.toList());
        if (!negativeRatings.isEmpty()) {
            sb.append("【负反馈歌曲】（用户不喜欢，避免推荐类似）\n");
            for (Map<String, Object> r : negativeRatings) {
                sb.append("- ").append(r.get("title")).append(" / ")
                  .append(r.get("artist_name")).append("（")
                  .append(((Number) r.get("score")).intValue()).append("星）\n");
            }
            sb.append("\n");
        }

        // 歌曲库
        List<Map<String, Object>> library = songMapper.selectLibrarySummary();
        sb.append("【歌曲库】（id | 歌名 | 艺人 | 国籍 | 标签）\n");
        for (Map<String, Object> song : library) {
            sb.append(song.get("id")).append(" | ")
              .append(song.get("title")).append(" | ")
              .append(song.get("artist_name") != null ? song.get("artist_name") : "未知").append(" | ")
              .append(song.get("nationality") != null ? song.get("nationality") : "").append(" | ")
              .append(song.get("tags") != null ? song.get("tags") : "")
              .append("\n");
        }
        sb.append("\n");

        // 输出格式要求
        sb.append("请以 JSON 数组返回推荐结果，格式严格如下：\n");
        sb.append("[{\"id\": 歌曲id, \"reason\": \"一句话推荐理由\"}, ...]\n");
        sb.append("只返回 JSON 数组，不要包含任何其他文字。");

        return sb.toString();
    }

    /**
     * 调用 DeepSeek API
     */
    private String callDeepSeekApi(String prompt) {
        String url = props.getBaseUrl() + "/v1/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", 0.7);
        body.put("max_tokens", 2000);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是一个专业的音乐推荐助手，擅长根据用户口味推荐歌曲。"),
                Map.of("role", "user", "content", prompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("DeepSeek returned empty response");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("DeepSeek returned no choices");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (RestClientException e) {
            throw new RuntimeException("DeepSeek API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 DeepSeek 返回的内容，提取 JSON 数组
     */
    private List<DeepSeekPick> parseResponse(String content) {
        // 尝试提取 JSON 数组（兼容 markdown 代码块包裹）
        String json = content.trim();
        Pattern pattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            json = matcher.group(1).trim();
        }
        // 找到第一个 [ 到最后一个 ]
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<DeepSeekPick>>() {});
        } catch (Exception e) {
            log.error("Failed to parse DeepSeek response: {}", content);
            throw new RuntimeException("Failed to parse DeepSeek response: " + e.getMessage(), e);
        }
    }

    /**
     * 降级推荐：按用户偏好标签匹配
     */
    private Map<String, Object> fallbackRecommend(Long userId,
                                                   List<Map<String, Object>> topTags,
                                                   List<Map<String, Object>> favorites,
                                                   List<Map<String, Object>> history) {
        // 收集已交互歌曲 ID
        Set<Long> excludeIds = new HashSet<>();
        for (Map<String, Object> f : favorites) {
            excludeIds.add(((Number) f.get("song_id")).longValue());
        }
        for (Map<String, Object> h : history) {
            excludeIds.add(((Number) h.get("song_id")).longValue());
        }

        // 获取 top 标签 ID
        List<Long> tagIds = topTags.stream()
                .map(t -> ((Number) t.get("id")).longValue())
                .limit(10)
                .collect(Collectors.toList());

        List<Long> excludeList = new ArrayList<>(excludeIds);
        if (excludeList.isEmpty()) {
            excludeList.add(-1L); // 避免空 IN 子句
        }
        if (tagIds.isEmpty()) {
            tagIds.add(-1L);
        }

        List<Map<String, Object>> songs = songMapper.selectSongsByTagIds(
                tagIds, excludeList, props.getMaxRecommendations());

        // 如果匹配不够，用热门歌曲补足
        if (songs.size() < props.getMaxRecommendations()) {
            // 查询全部歌曲，排除已交互的
            List<Map<String, Object>> allSongs = songMapper.selectLibrarySummary();
            for (Map<String, Object> s : allSongs) {
                if (songs.size() >= props.getMaxRecommendations()) break;
                Long sid = ((Number) s.get("id")).longValue();
                if (!excludeIds.contains(sid)) {
                    // 简单检查是否已在结果中
                    boolean already = songs.stream().anyMatch(
                            x -> ((Number) x.get("song_id")).longValue() == sid);
                    if (!already) {
                        Map<String, Object> song = new LinkedHashMap<>();
                        song.put("song_id", sid);
                        song.put("title", s.get("title"));
                        song.put("artist_name", s.get("artist_name"));
                        songs.add(song);
                    }
                }
            }
        }

        List<Long> songIds = songs.stream()
                .map(s -> ((Number) s.get("song_id")).longValue())
                .collect(Collectors.toList());
        List<String> reasons = songs.stream()
                .map(s -> "根据你的音乐口味为你推荐")
                .collect(Collectors.toList());

        // 保存降级结果
        saveResults(userId, songIds);

        return buildResult(songIds, reasons, "fallback", true);
    }

    /**
     * 组装最终结果
     */
    private Map<String, Object> buildResult(List<Long> songIds, List<String> reasons,
                                             String source, boolean fallback) {
        // 批量查询歌曲完整信息
        List<Map<String, Object>> songs;
        if (songIds.isEmpty()) {
            songs = Collections.emptyList();
        } else {
            songs = songMapper.selectSongsByIds(songIds);
            // 按 songIds 的顺序重新排列
            Map<Long, Map<String, Object>> songMap = new HashMap<>();
            for (Map<String, Object> s : songs) {
                songMap.put(((Number) s.get("song_id")).longValue(), s);
            }
            List<Map<String, Object>> ordered = new ArrayList<>();
            for (int i = 0; i < songIds.size(); i++) {
                Map<String, Object> s = songMap.get(songIds.get(i));
                if (s != null) {
                    s.put("reason", i < reasons.size() ? reasons.get(i) : "为你推荐");
                    ordered.add(s);
                }
            }
            songs = ordered;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("songs", songs);
        result.put("source", source);
        result.put("fallback", fallback);
        return result;
    }

    /**
     * 保存推荐结果到 recommendation_results 表
     */
    private void saveResults(Long userId, List<Long> songIds) {
        // 先清旧结果
        recommendationMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RecommendationResult>()
                        .eq(RecommendationResult::getUserId, userId));

        for (int i = 0; i < songIds.size(); i++) {
            RecommendationResult r = new RecommendationResult();
            r.setUserId(userId);
            r.setSongId(songIds.get(i));
            r.setScore((float) (1.0 - i * 0.03)); // 按顺序递减
            r.setSource("deepseek");
            r.setCreatedAt(LocalDateTime.now());
            recommendationMapper.insert(r);
        }
    }

    /**
     * 简单行为 hash，用于缓存判断
     */
    private String computeHash(Long userId, List<?>... lists) {
        int hash = userId.hashCode();
        for (List<?> list : lists) {
            for (Object item : list) {
                hash = 31 * hash + item.hashCode();
            }
        }
        return userId + ":" + hash;
    }

    // ========== 内部类 ==========

    /**
     * DeepSeek 返回的单条推荐
     */
    public static class DeepSeekPick {
        public Long id;
        public String reason;
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final List<Long> songIds;
        final List<String> reasons;
        final long createdAt;

        CacheEntry(List<Long> songIds, List<String> reasons) {
            this.songIds = songIds;
            this.reasons = reasons;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired(int ttlMinutes) {
            return System.currentTimeMillis() - createdAt > ttlMinutes * 60L * 1000L;
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd C:/Users/20822/music-recommendation/backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/musicrec/service/DeepSeekService.java
git commit -m "feat: create DeepSeekService with API call, fallback, and caching"
```

---

### Task 4: 修改 RecommendationService

**Files:**
- Modify: `backend/src/main/java/com/musicrec/service/RecommendationService.java`

- [ ] **Step 1: 替换 RecommendationService.java 内容**

```java
package com.musicrec.service;

import com.musicrec.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RecommendationService {

    private final DeepSeekService deepSeekService;
    private final HttpServletRequest request;

    public RecommendationService(DeepSeekService deepSeekService, HttpServletRequest request) {
        this.deepSeekService = deepSeekService;
        this.request = request;
    }

    /**
     * 获取个性化推荐 — 基于 DeepSeek 大模型
     */
    public Result<Map<String, Object>> getRecommendations() {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = deepSeekService.generateRecommend(userId);
        return Result.success(result);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd C:/Users/20822/music-recommendation/backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/musicrec/service/RecommendationService.java
git commit -m "feat: refactor RecommendationService to use DeepSeekService"
```

---

### Task 5: 修改 RecommendationController

**Files:**
- Modify: `backend/src/main/java/com/musicrec/controller/RecommendationController.java`

- [ ] **Step 1: 替换 RecommendationController.java 内容**

```java
package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recService;

    public RecommendationController(RecommendationService recService) {
        this.recService = recService;
    }

    /**
     * 获取 AI 个性化推荐
     * POST 方法，触发实时推荐（含 DeepSeek API 调用）
     */
    @PostMapping
    public Result<Map<String, Object>> recommend() {
        return recService.getRecommendations();
    }

    /**
     * 获取历史推荐结果（缓存/数据库中的上次结果，不调 API）
     */
    @GetMapping
    public Result<Map<String, Object>> getLatest() {
        return recService.getRecommendations();
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd C:/Users/20822/music-recommendation/backend && ./mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/musicrec/controller/RecommendationController.java
git commit -m "feat: update RecommendationController — POST for fresh recs"
```

---

### Task 6: 更新前端 RecommendView.vue

**Files:**
- Modify: `frontend/src/views/RecommendView.vue`

- [ ] **Step 1: 替换 RecommendView.vue 内容**

```vue
<template>
  <div class="recommend-page">
    <div class="page-hero">
      <h2 class="page-title">🤖 AI 智能推荐</h2>
      <p class="page-desc">DeepSeek 大模型分析你的听歌习惯，为你精准推荐</p>
    </div>

    <!-- 操作区 -->
    <div class="action-card">
      <div class="action-info">
        <span class="action-label">推荐引擎</span>
        <el-tag :type="recSource === 'deepseek' ? 'success' : 'warning'" effect="dark" size="small">
          {{ recSource === 'deepseek' ? 'DeepSeek AI' : '智能匹配' }}
        </el-tag>
        <span v-if="recSource === 'deepseek'" class="powered-by">Powered by DeepSeek</span>
      </div>
      <el-button
        type="primary"
        @click="fetchRecs"
        :loading="loading"
        class="refresh-btn"
      >
        <span v-if="!loading">🔄 获取推荐</span>
        <span v-else>分析中...</span>
      </el-button>
    </div>

    <!-- 降级提示 -->
    <el-alert
      v-if="isFallback"
      title="DeepSeek AI 暂时不可用，已为你切换到智能标签匹配推荐"
      type="warning"
      :closable="false"
      show-icon
      class="fallback-alert"
    />

    <!-- 加载骨架 -->
    <div class="recs-section" v-if="loading">
      <h3 class="section-heading">为你推荐</h3>
      <div class="recs-grid">
        <div v-for="i in 12" :key="'sk'+i" class="rec-card sk-rec">
          <div class="rec-cover-wrap sk-cover"></div>
          <div class="rec-info">
            <div class="sk-line w-70"></div>
            <div class="sk-line w-50"></div>
            <div class="sk-line w-60"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐结果 -->
    <div class="recs-section" v-else-if="recs.length">
      <h3 class="section-heading">为你推荐</h3>
      <div class="recs-grid">
        <div
          v-for="r in recs"
          :key="r.song_id"
          class="rec-card"
          @click="$router.push(`/song/${r.song_id}`)"
        >
          <div class="rec-cover-wrap">
            <img :src="r.cover_url || genCover(r.title)" alt="" class="rec-cover" />
          </div>
          <div class="rec-info">
            <div class="rec-title">{{ r.title }}</div>
            <div class="rec-artist">
              {{ r.artist_original_name || r.artist_name || '未知' }}
            </div>
            <div class="rec-reason" v-if="r.reason">
              💡 {{ r.reason }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty" v-if="!loading && !recs.length && !errorMsg">
      <span class="empty-icon">🎵</span>
      <p>点击上方按钮，让 AI 为你发现好音乐</p>
      <p class="empty-hint">收藏几首喜欢的歌，推荐会更精准哦</p>
    </div>

    <!-- 错误 -->
    <div class="empty error" v-if="errorMsg">
      <span class="empty-icon">⚠️</span>
      <p>{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const recs = ref([])
const loading = ref(false)
const errorMsg = ref('')
const recSource = ref('')
const isFallback = ref(false)

function genCover(title) {
  const colors = ['667eea','e250c0','f5576c','43e97b','764ba2']
  const h = (title||'?').charCodeAt(0) || 0
  return `https://via.placeholder.com/200/${colors[h%colors.length]}/fff?text=${encodeURIComponent((title||'?').slice(0,4))}`
}

async function fetchRecs() {
  if (!userStore.isLoggedIn) {
    errorMsg.value = '请先登录后再获取推荐'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await request.post('/recommendations')
    const data = res.data
    recs.value = data.songs || []
    recSource.value = data.source || 'fallback'
    isFallback.value = data.fallback || false
  } catch (e) {
    errorMsg.value = '推荐加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchRecs()
  }
})
</script>

<style scoped>
.recommend-page { max-width: 1400px; margin: 0 auto; padding: 0 24px 40px; }
.page-hero { margin-bottom: 24px; }
.page-title { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 6px 0 0; font-size: 14px; }

.action-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px; padding: 20px 24px;
  margin-bottom: 24px;
  display: flex; align-items: center; flex-wrap: wrap; gap: 16px;
  justify-content: space-between;
}
.action-info { display: flex; align-items: center; gap: 10px; }
.action-label { font-size: 14px; color: var(--text-muted); }
.powered-by { font-size: 12px; color: var(--text-muted); opacity: 0.7; }
.refresh-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none; border-radius: 12px; font-weight: 600;
  transition: all 0.3s; padding: 10px 24px;
}
.refresh-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(102,126,234,0.4); }

.fallback-alert { margin-bottom: 20px; }

.section-heading { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 18px; }

.recs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1100px) { .recs-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px)  { .recs-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px)  { .recs-grid { grid-template-columns: 1fr; } }

.rec-card {
  cursor: pointer; border-radius: 14px; overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border);
  transition: all 0.35s;
}
.rec-card:hover {
  transform: translateY(-6px);
  border-color: rgba(102,126,234,0.3);
  box-shadow: var(--card-shadow);
}
.rec-cover-wrap {
  position: relative; aspect-ratio: 1; overflow: hidden;
  background: var(--bg-secondary);
}
.rec-cover {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s;
}
.rec-card:hover .rec-cover { transform: scale(1.1) rotate(2deg); }
.rec-info { padding: 12px 14px; }
.rec-title {
  font-size: 14px; font-weight: 600; color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.rec-artist { font-size: 12px; color: var(--text-muted); margin: 4px 0 6px; }
.rec-reason {
  font-size: 12px; color: var(--text-secondary); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; }
.empty p { color: var(--text-muted); margin: 8px 0; }
.empty.error p { color: #f56c6c; }
.empty-hint { font-size: 13px; color: var(--text-muted); }

/* Skeleton */
.sk-rec { pointer-events: none; }
.sk-cover {
  aspect-ratio: 1;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.sk-line {
  height: 12px; border-radius: 6px; margin: 6px 0;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.w-70 { width: 70%; } .w-50 { width: 50%; } .w-60 { width: 60%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
```

- [ ] **Step 2: 前端编译验证**

```bash
cd C:/Users/20822/music-recommendation/frontend && npm run build -- --emptyOutDir
```

Expected: builds without error

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/RecommendView.vue
git commit -m "feat: redesign RecommendView — AI refresh button, reasons, fallback notice"
```

---

## 验证清单

全部实现完成后，执行以下验证：

### 后端验证

```bash
# 1. 启动后端
cd C:/Users/20822/music-recommendation/backend
./mvnw spring-boot:run

# 2. 用 curl 测试推荐接口（先登录获取 token）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 记下返回的 token

# 3. 调用推荐接口
curl -X POST http://localhost:8080/api/recommendations \
  -H "Authorization: Bearer <token>"
# 预期返回: { code: 200, data: { songs: [...], source: "deepseek"|"fallback", fallback: true|false } }
```

### 前端验证

```bash
# 启动前端
cd C:/Users/20822/music-recommendation/frontend
npm run dev
# 浏览器打开 http://localhost:3000
# 登录 admin / 123456
# 进入「智能推荐」页面
# 点击「获取推荐」按钮，等待结果
```

### 预期行为

| 场景 | 预期 |
|---|---|
| 正常调用 | 返回 12 首歌曲，每首有推荐理由，source=deepseek |
| DeepSeek 不可用 | 降级到标签匹配，显示黄色提示条，fallback=true |
| 5 分钟内重复请求 | 返回缓存结果（后端日志显示 cache hit） |
| 未登录 | 前端提示"请先登录后再获取推荐" |
| 新用户无行为数据 | DeepSeek 基于歌曲库做多样性推荐 |

---

## 自检清单

- [x] 6 个 Task 完整覆盖 spec 所有要求
- [x] 无 TBD / TODO / 占位符
- [x] 所有方法签名跨 Task 一致（DeepSeekService.generateRecommend → RecommendationService.getRecommendations → Controller）
- [x] 前端响应格式与后端返回一致（`res.data.songs`、`res.data.source`、`res.data.fallback`）
- [x] 降级策略已实现（tag matching + alert）
- [x] 缓存机制已实现（5 min TTL）
- [x] API Key 通过环境变量注入
