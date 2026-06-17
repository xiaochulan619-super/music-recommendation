#!/usr/bin/env python3
"""
重新生成榜单数据：中文歌 60-70%，TOP5 各有特色，数据自然化
"""
import mysql.connector
import random
import sys

random.seed(42)

conn = mysql.connector.connect(
    host='127.0.0.1', port=3306, user='root',
    password='wmr20056354', database='music_recommend',
    charset='utf8mb4'
)
cur = conn.cursor()

# 获取所有歌曲及国籍
cur.execute("""
    SELECT s.id, s.title, s.album, s.artist_id, s.play_count,
           a.name AS artist_name, a.nationality
    FROM songs s
    LEFT JOIN artists a ON s.artist_id = a.id
""")
rows = cur.fetchall()

songs = []
for r in rows:
    songs.append({
        'id': r[0], 'title': r[1], 'album': r[2],
        'artist_id': r[3], 'play_count': r[4] or 0,
        'artist_name': r[5], 'nationality': r[6] or ''
    })

print(f"Total songs: {len(songs)}")

# 分离中文歌和其他
def is_chinese(nat):
    if not nat:
        return False
    for kw in ['中国', '台湾', '香港', '新加坡', '马来西亚']:
        if kw in nat:
            return True
    return False

cn_songs = [s for s in songs if is_chinese(s['nationality'])]
other_songs = [s for s in songs if not is_chinese(s['nationality'])]
print(f"Chinese songs: {len(cn_songs)}, Other: {len(other_songs)}")

# 确保同一专辑不连续出现
def diversify(song_list):
    """打散同专辑连续"""
    for i in range(len(song_list) - 1):
        if song_list[i]['album'] == song_list[i+1]['album']:
            for j in range(i+2, len(song_list)):
                if song_list[j]['album'] != song_list[i]['album']:
                    # 检查交换后前后也不冲突
                    song_list[i+1], song_list[j] = song_list[j], song_list[i+1]
                    break
    return song_list

# 生成自然感的播放量（有热门有冷门，符合长尾分布）
def gen_natural_counts(base, rank):
    """排行榜递减 + 随机波动"""
    decay = 1.0 / (1.0 + rank * 0.03)
    noise = random.uniform(0.7, 1.3)
    hot = int(base * decay * noise)
    fav = int(hot * random.uniform(0.05, 0.3))
    # 确保有变化：前5差距大，后面差距小
    if rank < 5:
        hot = int(base * (1.0 - rank * 0.12) * noise)
    elif rank < 15:
        hot = int(base * (0.5 - (rank-5) * 0.02) * noise)
    else:
        hot = int(base * (0.2 - (rank-15) * 0.005) * noise)
    hot = max(hot, random.randint(50, 300))
    return hot, fav

def gen_chart(chart_type, cn_pct=0.65):
    """
    生成一张榜单
    - cn_pct: 中文歌占比
    - TOP1 中文，TOP5 各不同艺人
    - 每个歌手最多出现 3 次
    """
    total = 50
    cn_count = int(total * cn_pct)

    cn_sorted = sorted(cn_songs, key=lambda x: x['play_count'], reverse=True)
    other_sorted = sorted(other_songs, key=lambda x: x['play_count'], reverse=True)

    pick = []
    used_ids = set()
    artist_count = {}  # 每位歌手已选数量，上限3

    def can_pick(s):
        if s['id'] in used_ids:
            return False
        return artist_count.get(s['artist_name'], 0) < 3

    def do_pick(s):
        pick.append(s)
        used_ids.add(s['id'])
        artist_count[s['artist_name']] = artist_count.get(s['artist_name'], 0) + 1

    # TOP1: 必须是中文
    for s in cn_sorted:
        if can_pick(s):
            do_pick(s)
            break

    # TOP2-5: 不同艺人，混合中外
    for s in cn_sorted:
        if len(pick) >= 5:
            break
        if can_pick(s) and s['artist_name'] not in [p['artist_name'] for p in pick]:
            do_pick(s)

    if len(pick) < 5:
        for s in other_sorted:
            if len(pick) >= 5:
                break
            if can_pick(s) and s['artist_name'] not in [p['artist_name'] for p in pick]:
                do_pick(s)

    # 填充中文歌（从热门往下，同歌手不超过3首）
    for s in cn_sorted:
        if len([x for x in pick if is_chinese(x['nationality'])]) >= cn_count:
            break
        if can_pick(s):
            do_pick(s)

    # 填充外语歌
    for s in other_sorted:
        if len(pick) >= total:
            break
        if can_pick(s):
            do_pick(s)

    pick = pick[:total]

    # 专辑打散
    pick = diversify(pick)

    # 生成榜单数据
    charts = []
    base_play = random.randint(80000, 200000)
    base_fav_base = random.randint(5000, 20000)

    for i, s in enumerate(pick):
        play, fav = gen_natural_counts(base_play, i)
        # hotScore = 播放量*0.6 + 收藏*0.4 + 少量随机
        hot_score = play * 0.6 + fav * 0.4 + random.uniform(-50, 50)
        charts.append({
            'song_id': s['id'],
            'chart_type': chart_type,
            'play_count': play,
            'fav_count': fav,
            'hot_score': round(hot_score, 1)
        })

    # 按 hot_score 降序
    charts.sort(key=lambda x: x['hot_score'], reverse=True)
    return charts

# 生成三种榜单
daily = gen_chart('daily', 0.65)
weekly = gen_chart('weekly', 0.65)
total = gen_chart('total', 0.70)

# 写入数据库
cur.execute("DELETE FROM charts")

for chart_list in [daily, weekly, total]:
    for c in chart_list:
        cur.execute("""
            INSERT INTO charts (song_id, chart_type, play_count, fav_count, hot_score, rank_date)
            VALUES (%s, %s, %s, %s, %s, CURDATE())
        """, (c['song_id'], c['chart_type'], c['play_count'],
              c['fav_count'], c['hot_score']))

conn.commit()

# 验证
cur.execute("""
    SELECT chart_type, COUNT(*) as cnt,
           COUNT(DISTINCT song_id) as uniq_songs
    FROM charts GROUP BY chart_type
""")
for r in cur.fetchall():
    print(f"Chart {r[0]}: {r[1]} entries, {r[2]} unique songs")

# 检查TOP5
for ct in ['daily', 'weekly', 'total']:
    cur.execute("""
        SELECT c.hot_score, s.title, a.name, a.nationality
        FROM charts c
        JOIN songs s ON c.song_id = s.id
        JOIN artists a ON s.artist_id = a.id
        WHERE c.chart_type = %s
        ORDER BY c.hot_score DESC LIMIT 5
    """, (ct,))
    print(f"\n{ct.upper()} TOP5:")
    for r in cur.fetchall():
        tag = "🇨🇳" if is_chinese(r[3] or '') else "🌐"
        print(f"  {tag} {r[1]:20s} - {r[2]:18s} | hot={r[0]:.0f}")

cur.close()
conn.close()
print("\n✅ Done!")
