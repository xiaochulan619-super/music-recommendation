# DeepSeek 大模型推荐引擎 — 设计文档

**日期**: 2026-06-17  
**状态**: 已确认  
**项目**: 音乐推荐系统 (Music Recommendation System)

---

## 1. 目标

用 DeepSeek 大模型替代自训练的 PyTorch NCF 推荐引擎。将用户的多维度行为数据（收藏、播放历史、评分、标签偏好）作为上下文发给 DeepSeek，由模型从歌曲库中直接挑选推荐歌曲，每首附带推荐理由。

## 2. 架构

```
前端 RecommendView          后端                                    外部
  │                          │
  ├─ POST /api/recommend ──→ RecommendationController
  │                          │ → 收集用户行为数据
  │                          │   (播放记录/收藏/评分/标签偏好)
  │                          ├→ 查询歌曲库摘要
  │                          │   (id+歌名+艺人+流派+语种+标签)
  │                          │
  │                          ├→ DeepSeekService.generateRecommend()
  │                          │   → POST https://api.deepseek.com/v1/chat/completions
  │                          │   → OpenAI 兼容格式
  │                          │   → 返回 { songs: [...], reasons: [...] }
  │                          │
  │                          ├→ 按 ID 查询完整歌曲数据
  │                          │   (封面/艺人/播放量等)
  │                          │
  │   ← { songs, reasons } ←─┤
  │                          │
  │  渲染推荐卡片 + 推荐理由    │
```

### 关键决策

| 项 | 决定 |
|---|---|
| 通信方式 | HTTP + OpenAI 兼容格式 (`/v1/chat/completions`) |
| SDK | 不引入额外 SDK，用已有的 HTTP 客户端 |
| 模型 | `deepseek-chat` |
| API Key | 环境变量 `DEEPSEEK_API_KEY` 注入 `application.yml` |
| 超时 | 15 秒，超时降级到标签匹配 |
| 每次推荐数 | 12 首（前端 3 行 × 4 列） |
| 缓存 | 用户行为数据 5 分钟内不重复调用（省 token） |
| 降级 | API 不可用时标签匹配 + 标记「智能推荐暂不可用」 |
| 去重 | 自动排除用户已收藏/播放历史中的歌曲 |

## 3. 用户画像数据

发给 DeepSeek 的 prompt 包含以下维度的用户数据：

| 数据源 | 表 | 含义 | 权重 |
|---|---|---|---|
| 收藏列表 | `favorites` | 用户明确喜欢的歌曲 | ⭐⭐⭐ 正向 |
| 播放历史 | `play_history` | 最近 N 首播放、播放次数 | ⭐⭐ 隐性偏好 |
| 评分记录 | `ratings` | 1-5 星，高分正反馈 + 低分负反馈 | ⭐⭐⭐/❌ 双向 |
| 标签偏好 | `song_tags` + `favorites` | 收藏/高评分歌曲的高频标签 | ⭐⭐ 风格指向 |

## 4. Prompt 结构

```
你是一个音乐推荐专家。根据以下用户画像，从歌曲库中选出最适合的12首歌。

【用户偏好】
- 常听流派: {genres}
- 偏好语种: {languages}
- 喜欢标签: {tags}
- 最近在听: {recent_songs}

【正反馈歌曲】
{每首: 歌名 / 艺人 / 流派 / 行为类型(收藏|5星|高频播放)}

【负反馈歌曲】
{每首: 歌名 / 艺人 / 行为类型(低评分|跳过)}

【歌曲库】
{id | 歌名 | 艺人 | 流派 | 语种 | 标签}

请以 JSON 数组返回推荐结果，格式：
[{"id": 123, "reason": "一句话推荐理由"}, ...]

只返回 JSON，不要其他内容。
```

## 5. 数据流

### 5.1 正常流程

1. 前端点击"获取推荐" / 页面加载
2. `POST /api/recommend` 请求（带 JWT token）
3. `RecommendationController` 调用 `DeepSeekService.generateRecommend(userId)`
4. `DeepSeekService` 查询用户行为（favorite/history/rating/tag 四个 mapper）
5. 构造 prompt，调用 DeepSeek API
6. 解析返回的 JSON，拿到歌曲 ID 列表
7. 批量查询 `song` 表获取完整信息 + 艺人 + 封面
8. 组装 `{ songs, reasons }` 返回前端
9. 保存推荐结果到 `recommendation_results` 表

### 5.2 降级流程

1. API 调用超时或返回错误
2. 根据用户 Top 标签，在数据库中按标签匹配随机选 12 首
3. 去重（排除已收藏/已播放）
4. 标记 `fallback: true`，前端显示提示信息

### 5.3 缓存流程

1. 生成缓存 key = `{userId}:{behaviorHash}`
2. 5 分钟内相同 key 直接返回上次结果
3. 用户新增收藏/评分后，缓存自动失效

## 6. 后端新增文件

| 文件 | 说明 |
|---|---|
| `service/DeepSeekService.java` | 核心：构造 prompt、调用 API、解析结果、降级、缓存 |
| `controller/RecommendationController.java` | 需修改：调用 DeepSeekService 替代旧逻辑 |
| `config/DeepSeekConfig.java` | API Key / base URL / timeout 配置类 |

## 7. 前端修改

| 文件 | 修改 |
|---|---|
| `views/RecommendView.vue` | 展示推荐理由、加载动画、降级提示 |

## 8. 配置

`application.yml`:
```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  timeout: 15000
  max-recommendations: 12
  cache-ttl-minutes: 5
```

## 9. 安全

- API Key 通过环境变量注入，不硬编码
- 不暴露在 git 历史中（`.gitignore` 已忽略 `application.yml` 或使用 `application-local.yml`）
- 用户数据仅用于构造 prompt，不发送到其他服务
