package com.musicrec.service;

import java.util.*;
import java.util.function.Function;

/**
 * 歌曲列表多样化工具
 * - 专辑打散：确保同一专辑不连续出现
 * - 中文歌权重计算
 */
public class SongDiversityUtil {

    /** 华语歌手国籍关键词 */
    private static final java.util.regex.Pattern CHINESE_PATTERN =
            java.util.regex.Pattern.compile(".*(中国|台湾|香港|新加坡|马来西亚).*");

    /** 中文歌排序权重加成 */
    public static final double CHINESE_BOOST = 1.4;

    /**
     * 判断是否华语歌手
     */
    public static boolean isChinese(String nationality) {
        return nationality != null && CHINESE_PATTERN.matcher(nationality).matches();
    }

    /**
     * 贪心构建多样化列表：从候选池中逐首挑选，跳过与上一首同专辑或同歌手的歌曲。
     * 当无干净候选时放宽限制（仅跳过同专辑）。
     *
     * @param pool    候选列表（按分数降序）
     * @param limit   需要选取的数量
     * @return 多样化后的列表
     */
    public static <T> List<T> greedyDiverse(List<T> pool, int limit,
                                             Function<T, String> albumGetter,
                                             Function<T, String> artistGetter) {
        if (pool == null || pool.isEmpty()) return List.of();
        List<T> result = new ArrayList<>();
        List<T> remaining = new ArrayList<>(pool);

        while (result.size() < limit && !remaining.isEmpty()) {
            T prev = result.isEmpty() ? null : result.get(result.size() - 1);
            String prevAlbum = prev != null ? albumGetter.apply(prev) : null;
            String prevArtist = prev != null ? artistGetter.apply(prev) : null;

            // 找第一个不冲突的
            T picked = null;
            for (int i = 0; i < remaining.size(); i++) {
                T candidate = remaining.get(i);
                String candAlbum = albumGetter.apply(candidate);
                String candArtist = artistGetter.apply(candidate);

                boolean albumOk = prevAlbum == null || !prevAlbum.equals(candAlbum);
                boolean artistOk = prevArtist == null || !prevArtist.equals(candArtist);

                if (albumOk && artistOk) {
                    picked = remaining.remove(i);
                    break;
                }
            }

            // 严格模式找不到 → 放宽：只要求专辑不同
            if (picked == null) {
                for (int i = 0; i < remaining.size(); i++) {
                    T candidate = remaining.get(i);
                    String candAlbum = albumGetter.apply(candidate);
                    if (prevAlbum == null || !prevAlbum.equals(candAlbum)) {
                        picked = remaining.remove(i);
                        break;
                    }
                }
            }

            // 还找不到 → 直接取第一个
            if (picked == null) {
                picked = remaining.remove(0);
            }

            result.add(picked);
        }

        return result;
    }

    /**
     * 计算排序分数：播放量归一化后，中文歌 * 1.4
     */
    public static double computeScore(int playCount, boolean isChinese, int maxPlayCount) {
        double normalized = maxPlayCount > 0 ? (double) playCount / maxPlayCount : 0.0;
        return normalized * (isChinese ? CHINESE_BOOST : 1.0);
    }
}
