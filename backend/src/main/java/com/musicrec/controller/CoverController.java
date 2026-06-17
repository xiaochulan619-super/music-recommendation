package com.musicrec.controller;

import com.musicrec.common.Result;
import com.musicrec.service.CoverService;
import org.springframework.web.bind.annotation.*;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/covers")
public class CoverController {

    private final CoverService coverService;
    public CoverController(CoverService coverService) { this.coverService = coverService; }

    /**
     * 搜索专辑封面
     */
    @GetMapping("/search")
    public Result<List<Map<String, String>>> searchCover(
            @RequestParam String term,
            @RequestParam(defaultValue = "3") int limit) {
        try {
            String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String apiUrl = "https://itunes.apple.com/search?term=" + encoded
                    + "&entity=song&limit=" + limit;

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return Result.success(Collections.emptyList());
            }

            StringBuilder sb = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buf = new char[1024];
                int n;
                while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
            }

            return Result.success(parseResults(sb.toString()));
        } catch (Exception e) {
            return Result.error("封面搜索失败");
        }
    }

    /**
     * 批量更新数据库中缺失的封面
     */
    @PostMapping("/batch-update")
    public Result<Map<String, Object>> batchUpdate(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(coverService.batchUpdateCovers(limit));
    }

    private List<Map<String, String>> parseResults(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            int resultsIdx = json.indexOf("\"results\"");
            if (resultsIdx < 0) return list;
            int bracketStart = json.indexOf('[', resultsIdx);
            if (bracketStart < 0) return list;

            int pos = bracketStart;
            while (pos < json.length()) {
                int objStart = json.indexOf('{', pos);
                if (objStart < 0 || objStart > json.indexOf(']', bracketStart)) break;
                int objEnd = json.indexOf('}', objStart);
                if (objEnd < 0) break;
                String obj = json.substring(objStart, objEnd + 1);

                Map<String, String> item = new HashMap<>();
                item.put("artworkUrl100", extractString(obj, "artworkUrl100"));
                item.put("artworkUrl600", extractString(obj, "artworkUrl100")
                        .replace("100x100bb", "600x600bb"));
                item.put("trackName", extractString(obj, "trackName"));
                item.put("artistName", extractString(obj, "artistName"));
                item.put("collectionName", extractString(obj, "collectionName"));
                item.put("previewUrl", extractString(obj, "previewUrl"));

                if (item.get("artworkUrl100") != null && !item.get("artworkUrl100").isEmpty()) {
                    list.add(item);
                }
                pos = objEnd + 1;
            }
        } catch (Exception e) {}
        return list;
    }

    private String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx);
        if (colonIdx < 0) return null;
        int valStart = json.indexOf('"', colonIdx);
        if (valStart < 0) return null;
        int valEnd = json.indexOf('"', valStart + 1);
        if (valEnd < 0) return null;
        return json.substring(valStart + 1, valEnd);
    }
}
