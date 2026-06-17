package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        List<Map<String, Object>> favorites = favoriteMapper.selectUserFavoritesWithSong(userId);
        List<Map<String, Object>> ratings = ratingMapper.selectUserRatingsWithSong(userId);
        List<Map<String, Object>> history = historyMapper.selectUserHistoryWithCount(userId, 30);
        List<Map<String, Object>> topTags = tagMapper.selectUserTopTags(userId, 10);

        String behaviorHash = computeHash(userId, favorites, ratings, history);
        CacheEntry cached = cache.get(behaviorHash);
        if (cached != null && !cached.isExpired(props.getCacheTtlMinutes())) {
            log.info("DeepSeek cache hit for user {}", userId);
            return buildResult(cached.songIds, cached.reasons, "deepseek", false);
        }

        String prompt = buildPrompt(favorites, ratings, history, topTags);

        try {
            String response = callDeepSeekApi(prompt);
            List<DeepSeekPick> picks = parseResponse(response);
            List<Long> songIds = picks.stream().map(p -> p.id).collect(Collectors.toList());
            List<String> reasons = picks.stream().map(p -> p.reason).collect(Collectors.toList());

            cache.put(behaviorHash, new CacheEntry(songIds, reasons));

            saveResults(userId, songIds);

            return buildResult(songIds, reasons, "deepseek", false);

        } catch (Exception e) {
            log.warn("DeepSeek API failed, falling back to tag matching. Error: {}", e.getMessage());
            return fallbackRecommend(userId, topTags, favorites, history);
        }
    }

    // ========== 私有方法 ==========

    private String buildPrompt(List<Map<String, Object>> favorites,
                               List<Map<String, Object>> ratings,
                               List<Map<String, Object>> history,
                               List<Map<String, Object>> topTags) {

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个音乐推荐专家。根据以下用户画像，从歌曲库中选出最适合的")
          .append(props.getMaxRecommendations()).append("首歌。\n\n");

        String tagStr = topTags.stream()
                .map(t -> t.get("name").toString())
                .limit(8)
                .collect(Collectors.joining("、"));
        sb.append("【用户偏好】\n");
        sb.append("- 偏好标签: ").append(tagStr.isEmpty() ? "暂无" : tagStr).append("\n\n");

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

        sb.append("请以 JSON 数组返回推荐结果，格式严格如下：\n");
        sb.append("[{\"id\": 歌曲id, \"reason\": \"一句话推荐理由\"}, ...]\n");
        sb.append("只返回 JSON 数组，不要包含任何其他文字。");

        return sb.toString();
    }

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

    private List<DeepSeekPick> parseResponse(String content) {
        String json = content.trim();
        Pattern pattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            json = matcher.group(1).trim();
        }
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

    private Map<String, Object> fallbackRecommend(Long userId,
                                                   List<Map<String, Object>> topTags,
                                                   List<Map<String, Object>> favorites,
                                                   List<Map<String, Object>> history) {
        Set<Long> excludeIds = new HashSet<>();
        for (Map<String, Object> f : favorites) {
            excludeIds.add(((Number) f.get("song_id")).longValue());
        }
        for (Map<String, Object> h : history) {
            excludeIds.add(((Number) h.get("song_id")).longValue());
        }

        List<Long> tagIds = topTags.stream()
                .map(t -> ((Number) t.get("id")).longValue())
                .limit(10)
                .collect(Collectors.toList());

        List<Long> excludeList = new ArrayList<>(excludeIds);
        if (excludeList.isEmpty()) {
            excludeList.add(-1L);
        }
        if (tagIds.isEmpty()) {
            tagIds.add(-1L);
        }

        List<Map<String, Object>> songs = songMapper.selectSongsByTagIds(
                tagIds, excludeList, props.getMaxRecommendations());

        if (songs.size() < props.getMaxRecommendations()) {
            List<Map<String, Object>> allSongs = songMapper.selectLibrarySummary();
            for (Map<String, Object> s : allSongs) {
                if (songs.size() >= props.getMaxRecommendations()) break;
                Long sid = ((Number) s.get("id")).longValue();
                if (!excludeIds.contains(sid)) {
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

        saveResults(userId, songIds);

        return buildResult(songIds, reasons, "fallback", true);
    }

    private Map<String, Object> buildResult(List<Long> songIds, List<String> reasons,
                                             String source, boolean fallback) {
        List<Map<String, Object>> songs;
        if (songIds.isEmpty()) {
            songs = Collections.emptyList();
        } else {
            songs = songMapper.selectSongsByIds(songIds);
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

    private void saveResults(Long userId, List<Long> songIds) {
        recommendationMapper.delete(
                new LambdaQueryWrapper<RecommendationResult>()
                        .eq(RecommendationResult::getUserId, userId));

        for (int i = 0; i < songIds.size(); i++) {
            RecommendationResult r = new RecommendationResult();
            r.setUserId(userId);
            r.setSongId(songIds.get(i));
            r.setScore((float) (1.0 - i * 0.03));
            r.setSource("deepseek");
            r.setCreatedAt(LocalDateTime.now());
            recommendationMapper.insert(r);
        }
    }

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

    public static class DeepSeekPick {
        public Long id;
        public String reason;
    }

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
