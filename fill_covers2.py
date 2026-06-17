# -*- coding: utf-8 -*-
"""封面填充第二轮：更大延迟 + 中英文混合搜索"""
import pymysql, urllib.request, urllib.parse, json, time

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root',
                        password='wmr20056354', database='music_recommend', charset='utf8mb4')
cur = conn.cursor()

cur.execute("""SELECT DISTINCT a.id, a.name, a.original_name, a.nationality
    FROM artists a JOIN songs s ON a.id = s.artist_id
    WHERE s.cover_url IS NULL OR s.cover_url = ''""")
remaining = cur.fetchall()
print(f"Remaining artists with uncovered songs: {len(remaining)}")

def search(term):
    try:
        encoded = urllib.parse.quote(term)
        url = f'https://itunes.apple.com/search?term={encoded}&entity=song&limit=5'
        req = urllib.request.Request(url, headers={'User-Agent': 'MusicRec/1.0'})
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read())
            for item in data.get('results', []):
                img = item.get('artworkUrl100', '')
                if img:
                    return img.replace('100x100bb', '600x600bb')
    except:
        pass
    return None

updated = 0
matched = 0
for aid, name, orig, nat in remaining:
    is_cn = nat and any(k in str(nat) for k in ['中国','台湾','香港','新加坡','马来西亚'])
    terms = []
    if is_cn and orig:
        terms.append(orig)           # "周杰伦"
        terms.append(orig + ' ' + name)  # "周杰伦 Jay Chou"
    terms.append(name)               # "Jay Chou"

    cover = None
    for t in terms:
        cover = search(t)
        if cover:
            break
        time.sleep(0.6)  # iTunes rate limit

    if cover:
        cur.execute("""UPDATE songs SET cover_url=%s
            WHERE artist_id=%s AND (cover_url IS NULL OR cover_url='')""", (cover, aid))
        cnt = cur.rowcount
        updated += cnt
        matched += 1
        print(f"  [{aid}] {name}: {cnt} songs -> {cover[:55]}...")
    else:
        print(f"  [{aid}] {name}: NO MATCH")

    time.sleep(0.4)

conn.commit()
cur.execute("SELECT COUNT(*) FROM songs WHERE cover_url IS NOT NULL AND cover_url != ''")
done = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM songs")
total = cur.fetchone()[0]
print(f"\nDone: {done}/{total} ({100*done//total}%), matched {matched}/{len(remaining)} artists")
cur.close()
conn.close()
