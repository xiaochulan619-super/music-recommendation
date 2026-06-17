# -*- coding: utf-8 -*-
"""智能封面填充：按歌手搜 iTunes，同歌手歌曲共享封面 + 批量填充歌手图片"""
import pymysql
import urllib.request
import urllib.parse
import json
import time

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root',
                        password='wmr20056354', database='music_recommend', charset='utf8mb4')
cur = conn.cursor()

def itunes_search(term, entity='song', limit=3):
    """搜索 iTunes，返回结果列表"""
    try:
        encoded = urllib.parse.quote(term)
        url = f'https://itunes.apple.com/search?term={encoded}&entity={entity}&limit={limit}'
        req = urllib.request.Request(url, headers={'User-Agent': 'MusicRec/1.0'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read())
            return data.get('results', [])
    except Exception as e:
        return []

def get_cover_url(results):
    """从 iTunes 结果提取 600x600 封面 URL"""
    for r in results:
        img = r.get('artworkUrl100', '')
        if img:
            return img.replace('100x100bb', '600x600bb')
    return None

def get_artist_image(results):
    """从 iTunes 结果提取艺术家图片（用专辑封面代替）"""
    return get_cover_url(results)

# ===== 1. 获取所有需要封面的歌手 =====
cur.execute("""
    SELECT a.id, a.name, a.original_name, a.nationality
    FROM artists a
    WHERE a.image_url IS NULL OR a.image_url = ''
    ORDER BY a.id
""")
artists_need_img = cur.fetchall()
print(f"Artists needing image: {len(artists_need_img)}")

# ===== 2. 获取所有需要封面的歌曲（按歌手分组）=====
cur.execute("""
    SELECT s.artist_id, COUNT(*) as cnt
    FROM songs s
    WHERE s.cover_url IS NULL OR s.cover_url = ''
    GROUP BY s.artist_id
    ORDER BY cnt DESC
""")
artist_song_counts = {r[0]: r[1] for r in cur.fetchall()}
print(f"Artists with songs needing covers: {len(artist_song_counts)}")

# ===== 3. 合并所有需要处理的歌手 =====
all_artist_ids = set(r[0] for r in artists_need_img) | set(artist_song_counts.keys())
cur.execute("SELECT id, name, original_name, nationality FROM artists WHERE id IN (%s)" %
             ','.join(str(x) for x in all_artist_ids))
artist_info = {r[0]: r for r in cur.fetchall()}

processed = 0
songs_updated = 0
artists_updated = 0

for aid in sorted(all_artist_ids):
    info = artist_info.get(aid)
    if not info:
        continue
    name, orig_name, nat = info[1], info[2], info[3]

    # 搜索策略：中文歌手用中文名搜索，英文歌手用英文名
    is_cn = nat and any(k in str(nat) for k in ['中国','台湾','香港','新加坡','马来西亚'])
    search_terms = []
    if is_cn and orig_name:
        search_terms.append(orig_name)  # e.g. "周杰伦"
    search_terms.append(name)  # e.g. "Jay Chou"

    cover_url = None
    artist_img = None

    for term in search_terms:
        if cover_url and artist_img:
            break
        # 搜歌曲（获取封面）
        if not cover_url:
            results = itunes_search(term, entity='song', limit=5)
            cover_url = get_cover_url(results)
        # 搜专辑（获取歌手图）
        if not artist_img:
            results = itunes_search(term, entity='album', limit=3)
            artist_img = get_artist_image(results)
        time.sleep(0.15)  # 避免限速

    # 更新歌曲封面
    if cover_url and aid in artist_song_counts:
        cur.execute("""
            UPDATE songs SET cover_url = %s
            WHERE artist_id = %s AND (cover_url IS NULL OR cover_url = '')
        """, (cover_url, aid))
        cnt = cur.rowcount
        if cnt > 0:
            songs_updated += cnt
            print(f"  [{aid}] {name}: {cnt} songs -> {cover_url[:50]}...")

    # 更新歌手图片
    if artist_img and aid in [r[0] for r in artists_need_img]:
        cur.execute("UPDATE artists SET image_url = %s WHERE id = %s", (artist_img, aid))
        artists_updated += 1
        print(f"  [{aid}] {name}: artist image updated")

    processed += 1
    if processed % 20 == 0:
        conn.commit()
        cur.execute("SELECT COUNT(*) FROM songs WHERE cover_url IS NULL OR cover_url = ''")
        remaining_songs = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM artists WHERE image_url IS NULL OR image_url = ''")
        remaining_artists = cur.fetchone()[0]
        print(f"  ... {processed}/{len(all_artist_ids)} done, {remaining_songs} songs + {remaining_artists} artists remaining")

conn.commit()

# ===== 4. 最终统计 =====
cur.execute("SELECT COUNT(*) FROM songs WHERE cover_url IS NOT NULL AND cover_url != ''")
done_songs = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM songs")
total_songs = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM artists WHERE image_url IS NOT NULL AND image_url != ''")
done_artists = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM artists")
total_artists = cur.fetchone()[0]

print(f"\n=== Done ===")
print(f"Song covers: {done_songs}/{total_songs} ({100*done_songs//total_songs}%)")
print(f"Artist images: {done_artists}/{total_artists} ({100*done_artists//total_artists}%)")

cur.close()
conn.close()
