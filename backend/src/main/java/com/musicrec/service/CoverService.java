package com.musicrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musicrec.entity.Artist;
import com.musicrec.entity.Song;
import com.musicrec.mapper.ArtistMapper;
import com.musicrec.mapper.SongMapper;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CoverService {

    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;

    public CoverService(SongMapper songMapper, ArtistMapper artistMapper) {
        this.songMapper = songMapper;
        this.artistMapper = artistMapper;
    }

    /**
     * 批量搜索并更新歌曲封面（每次最多处理 limit 首无封面的歌曲）
     */
    public Map<String, Object> batchUpdateCovers(int limit) {
        // 查找没有封面的歌曲
        List<Song> songs = songMapper.selectList(
            new LambdaQueryWrapper<Song>()
                .isNull(Song::getCoverUrl).or().eq(Song::getCoverUrl, "")
                .last("LIMIT " + limit)
        );

        int updated = 0;
        int failed = 0;
        for (Song song : songs) {
            String artistName = song.getArtistName();
            if (artistName == null && song.getArtistId() != null) {
                Artist artist = artistMapper.selectById(song.getArtistId());
                if (artist != null) artistName = artist.getName();
            }
            String term = song.getTitle() + " " + (artistName != null ? artistName : "");
            String coverUrl = searchCover(term);
            // fallback: 只搜歌手名
            if (coverUrl == null && artistName != null) {
                coverUrl = searchCover(artistName);
            }
            if (coverUrl != null) {
                song.setCoverUrl(coverUrl);
                songMapper.updateById(song);
                updated++;
            } else {
                failed++;
            }
            // iTunes API 限速，稍微延迟
            try { Thread.sleep(200); } catch (InterruptedException e) { break; }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", songs.size());
        result.put("updated", updated);
        result.put("failed", failed);
        return result;
    }

    private String searchCover(String term) {
        try {
            String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String apiUrl = "https://itunes.apple.com/search?term=" + encoded
                    + "&entity=song&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                char[] buf = new char[1024];
                int n;
                while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
            }

            String json = sb.toString();
            // 提取 artworkUrl100 并替换为 600x600 大图
            String artwork = extractString(json, "artworkUrl100");
            if (artwork != null) {
                return artwork.replace("100x100bb", "600x600bb");
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = json.indexOf('"', colon);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}
