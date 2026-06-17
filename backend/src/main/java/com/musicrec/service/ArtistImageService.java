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
public class ArtistImageService {

    private final ArtistMapper artistMapper;
    private final SongMapper songMapper;

    public ArtistImageService(ArtistMapper artistMapper, SongMapper songMapper) {
        this.artistMapper = artistMapper;
        this.songMapper = songMapper;
    }

    public Map<String, Object> batchUpdateImages(int limit) {
        List<Artist> artists = artistMapper.selectList(
            new LambdaQueryWrapper<Artist>()
                .isNull(Artist::getImageUrl).or().eq(Artist::getImageUrl, "")
                .last("LIMIT " + limit)
        );

        int updated = 0, failed = 0;
        for (Artist artist : artists) {
            String imageUrl = null;

            // 1. 尝试从歌曲封面中取一张作为歌手图片
            imageUrl = getSongCover(artist.getId());

            // 2. 通过iTunes搜索艺人/专辑
            if (imageUrl == null) {
                imageUrl = searchiTunesArtist(artist.getName());
            }

            if (imageUrl != null) {
                artist.setImageUrl(imageUrl);
                artistMapper.updateById(artist);
                updated++;
            } else {
                failed++;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", artists.size());
        result.put("updated", updated);
        result.put("failed", failed);
        return result;
    }

    private String getSongCover(Long artistId) {
        List<Song> songs = songMapper.selectList(
            new LambdaQueryWrapper<Song>()
                .eq(Song::getArtistId, artistId)
                .isNotNull(Song::getCoverUrl)
                .ne(Song::getCoverUrl, "")
                .last("LIMIT 1")
        );
        return songs.isEmpty() ? null : songs.get(0).getCoverUrl();
    }

    private String searchiTunesArtist(String name) {
        try {
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
            String apiUrl = "https://itunes.apple.com/search?term=" + encoded
                    + "&entity=album&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (InputStreamReader r = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buf = new char[1024]; int n;
                while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            }

            String json = sb.toString();
            String img = extract(json, "artworkUrl100");
            if (img != null) return img.replace("100x100bb", "600x600bb");
        } catch (Exception e) {}
        return null;
    }

    private String extract(String json, String key) {
        String sk = "\"" + key + "\"";
        int idx = json.indexOf(sk);
        if (idx < 0) return null;
        int c = json.indexOf(':', idx);
        if (c < 0) return null;
        int s = json.indexOf('"', c);
        if (s < 0) return null;
        int e = json.indexOf('"', s + 1);
        if (e < 0) return null;
        return json.substring(s + 1, e);
    }
}
