# 歌曲数据多样化设计

## 背景

当前首页、发现页、榜单页的歌曲展示存在以下问题：
- 热门推荐和最新上架是同一批数据（按ID倒序），外语歌占主导
- 榜单数据 hotScore 随机生成，缺少温度感
- 同一专辑的歌曲连续出现，观感单调
- 整体呈现过于"AI 生成"，不够自然

## 目标

1. 三个板块的中文歌占比达到 60-70%
2. 排序多样化，同一专辑不连续出现
3. 首页"热门推荐"和"最新上架"合并为一个推荐流轮播
4. 发现页前几页优先展示热门中文歌，列表打散不要连着同专辑
5. 去除 AI 感，让数据和排序看起来像真实音乐平台

---

## 改动点

### 1. 首页 — 推荐流合并

**现状：** Hero → 🔥热门推荐(轮播10首) → 🆕最新上架(网格12首) → 🏆本周热榜(10首)

**改为：** Hero → 🎵 为你推荐(轮播~15首) → 🏆 本周热榜(10首)

**前端改动：** `HomeView.vue`
- 删除"最新上架"网格区块
- 将"热门推荐"改为"为你推荐"，数据源切换为新接口
- 轮播效果保留不变

### 2. 首页 + 发现页 — 推荐流排序算法

**后端新接口：** `GET /api/songs/recommend?limit=15`

排序逻辑：
```
1. 中文歌权重 +40%（通过 artist.nationality 判断）
2. 播放量归一化参与排序分计算
3. 排序分 = play_count_normalized * (is_chinese ? 1.4 : 1.0)
4. 按排序分 DESC 取出候选列表（2倍 limit）
5. 专辑打散后处理：
   - 扫描候选列表
   - 遇到连续同 album → 从后面找第一个不同 album 的歌交换
   - 同时避免交换后产生新的连续
6. 最终截取 limit 条
```

### 3. 发现页 — 搜索/浏览排序

**现状：** 默认 `ORDER BY s.id DESC`，同歌手/同专辑扎堆

**改为：** 默认排序增加专辑打散逻辑

**后端改动：** `SongService.java` → `listSongs()` 方法
- 无关键词时：按 `(is_chinese ? 1.4 : 1.0) * play_count` 排序
- 查询结果做专辑打散后处理
- 保留关键词搜索的原有逻辑

### 4. 榜单 — 数据重新生成

**现状：** 日榜/周榜/总榜各50条，hotScore 随机，外语歌霸榜 TOP5

**改为：** 重新生成三个榜的数据
- 日榜：TOP3 各不同，头条是一首热门中文歌
- 周榜：中文 60-70%，TOP5 各有特色（不同艺人）
- 总榜：经典中文歌占主导，TOP10 至少有 6 首中文
- 所有榜单做专辑打散处理

**后端改动：** `ChartService.java` → `getCharts()` 方法
- 在返回前做专辑打散后处理
- 不修改数据库，在查询结果上做后处理

### 5. 专辑打散算法（公共方法）

```java
/**
 * 打散列表，确保同一专辑不连续出现
 * 策略：从左到右扫描，发现连续同专辑就向后交换
 */
public static <T> void diversifyAlbums(List<T> items, 
    Function<T, String> albumGetter) {
    for (int i = 0; i < items.size() - 1; i++) {
        String cur = albumGetter.apply(items.get(i));
        String next = albumGetter.apply(items.get(i + 1));
        if (cur != null && cur.equals(next)) {
            // 向后找第一个不同专辑的元素交换
            for (int j = i + 2; j < items.size(); j++) {
                String alt = albumGetter.apply(items.get(j));
                if (alt != null && !alt.equals(cur)) {
                    Collections.swap(items, i + 1, j);
                    break;
                }
            }
        }
    }
}
```

---

## 涉及文件

| 文件 | 改动 |
|------|------|
| `frontend/src/views/HomeView.vue` | 合并热门+最新为推荐流，数据源切换 |
| `backend/.../controller/SongController.java` | 新增 `/api/songs/recommend` 端点 |
| `backend/.../service/SongService.java` | 新增推荐流方法 + 发现页排序改造 |
| `backend/.../service/ChartService.java` | 榜单查询增加专辑打散后处理 |
| `backend/.../service/SongDiversityUtil.java` | 新增：专辑打散公共方法 |

---

## 验收标准

- [ ] 首页推荐流 60-70% 是中文歌，轮播效果正常
- [ ] 发现页前 2 页（40首）以中文歌为主，列表内无连续同专辑
- [ ] 日榜/周榜/总榜 TOP10 中文歌 >= 6 首
- [ ] 所有列表不出现连续 2 首以上同专辑歌曲
- [ ] 数据观感自然，不像机器生成的排序
