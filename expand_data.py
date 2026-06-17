# -*- coding: utf-8 -*-
"""音乐推荐系统 - 数据扩展脚本 (1000+ songs, 100 artists)"""
import pymysql
import random
random.seed(42)

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root',
                        password='wmr20056354', database='music_recommend', charset='utf8mb4')
cur = conn.cursor()

# ==================== 1. Fix artists 33-50 ====================
fixes = [
    (33,'梁静茹','马来西亚'),(34,'莫文蔚','中国'),(35,'陶喆','中国'),
    (36,'方大同','中国'),(37,'王力宏','中国'),(38,'潘玮柏','中国'),
    (39,'罗大佑','中国'),(40,'李宗盛','中国'),(41,'张国荣','中国'),
    (42,'Beyond','中国'),(43,'苏打绿','中国'),(44,'新裤子','中国'),
    (45,'万能青年旅店','中国'),(46,'Imagine Dragons','美国'),
    (47,'Maroon 5','美国'),(48,'OneRepublic','美国'),
    (49,'Linkin Park','美国'),(50,'Queen','英国'),
]
for id_, orig, nat in fixes:
    cur.execute("UPDATE artists SET original_name=%s, nationality=%s WHERE id=%s", (orig, nat, id_))
print(f"Fixed {len(fixes)} artists")

# ==================== 2. Add 50 new artists ====================
new_artists = [
    ('Na Ying','那英','中国'),('Liu Huan','刘欢','中国'),('Han Hong','韩红','中国'),
    ('Li Jian','李健','中国'),('Xu Song','许嵩','中国'),('Wang Feng','汪峰','中国'),
    ('Zhang Bichen','张碧晨','中国'),('Tanya Chua','蔡健雅','新加坡'),
    ('A-Lin','黄丽玲','中国'),('Lala Hsu','徐佳莹','中国'),('Yisa Yu','郁可唯','中国'),
    ('Zhou Shen','周深','中国'),('GAI','周延','中国'),('Ice Paper','魏然','中国'),
    ('Lexie Liu','刘柏辛','中国'),('Tia Ray','袁娅维','中国'),('Jony J','肖佳','中国'),
    ('Higher Brothers','更高兄弟','中国'),('WayV','威神V','中国'),
    ('IU','IU','韩国'),('BTS','BTS','韩国'),('BLACKPINK','BLACKPINK','韩国'),
    ('YOASOBI','YOASOBI','日本'),('RADWIMPS','RADWIMPS','日本'),
    ('Kenshi Yonezu','米津玄师','日本'),
    ('Ariana Grande','Ariana Grande','美国'),('The Weeknd','The Weeknd','加拿大'),
    ('Post Malone','Post Malone','美国'),('Harry Styles','Harry Styles','英国'),
    ('Olivia Rodrigo','Olivia Rodrigo','美国'),('Doja Cat','Doja Cat','美国'),
    ('SZA','SZA','美国'),('Lana Del Rey','Lana Del Rey','美国'),
    ('Sam Smith','Sam Smith','英国'),('Charlie Puth','Charlie Puth','美国'),
    ('Arctic Monkeys','Arctic Monkeys','英国'),('Radiohead','Radiohead','英国'),
    ('Nirvana','Nirvana','美国'),('Green Day','Green Day','美国'),
    ('The Beatles','The Beatles','英国'),('Oasis','Oasis','英国'),
    ('Muse','Muse','英国'),('Foo Fighters','Foo Fighters','美国'),
    ('Avicii','Avicii','瑞典'),('Marshmello','Marshmello','美国'),
    ('The Chainsmokers','The Chainsmokers','美国'),('Tame Impala','Tame Impala','澳大利亚'),
    ('Joji','Joji','日本'),('FKJ','FKJ','法国'),('Lauv','Lauv','美国'),
]
artist_ids = {}
for name, orig, nat in new_artists:
    cur.execute("INSERT INTO artists (name, original_name, nationality, description) VALUES (%s,%s,%s,%s)",
                (name, orig, nat, '...'))
    artist_ids[name] = cur.lastrowid
print(f"Added {len(new_artists)} new artists (IDs {min(artist_ids.values())}-{max(artist_ids.values())})")

# ==================== 3. Song data ====================
# title templates by genre + language
T = {
    # genre_id -> (CN_titles, EN_titles)
    1: (  # Pop
        ['微光','追光者','星辰大海','起风了','年少有为','明天你好','最美的期待','错过','奔跑','时光','后来的我们','岁月神偷','无名的人','梦的光点','告白','星光','遇见','答案','温柔','等风来'],
        ['Starlight','Golden Hour','Heartbeat','Bloom','Rise','Infinity','Wonderland','Daylight','Phoenix','Horizon','Echo','Wings','Glow','Illusion','Shine','Destiny','Paradise','Forever','Remember','Dreamer']
    ),
    2: (  # Rock
        ['燃烧','狂野','自由','呐喊','破晓','逆光','挣脱','风暴','觉醒','冲锋','在路上','撕裂','重来','无地自容','向阳而生','闪电','怒吼','热血','征途','不朽'],
        ['Fire','Storm','Revolution','Rebel','Scream','Thunder','Inferno','Break Free','Overdrive','Voltage','Fury','Ignite','Unbroken','Warrior','Rampage','Rise','Blaze','Surge','Wreckage','Savior']
    ),
    3: (  # Folk
        ['南方','北方','故乡','远方','旅人','时光','来信','晚安','温暖','从前慢','春风十里','安河桥','南山南','理想','生活','旧时光','路口','归途','春分','秋意'],
        ['Home','River','Wanderer','Autumn','Letter','Story','Old Town','Whiskey','Fireplace','Rain','Traveler','Roots','Memoir','Lanterns','Campfire','Path','Horizon','Gone','Alone','Fields']
    ),
    4: (  # HipHop
        ['态度','征程','自由式','王冠','逆袭','出身','街头','顶峰','光芒','破格','不凡','致敬','重生','天命','传奇'],
        ['Crown','Hustle','Savage','Drip','Beast Mode','No Cap','Top Down','Flex','Grind','Stack','Zone','Ace','Run It','Blessed','Real Talk']
    ),
    5: (  # R&B
        ['午夜','沉溺','温柔','心动','呼吸','秘密','倒影','温度','循环','幻想','迷离','浅笑','暗香','微醺','涟漪'],
        ['Velvet','Moonlight','Silk','Honey','Waves','Desire','Gravity','Satin','Candy','Slowly','Touch','Deep','Lush','Bliss','Sway']
    ),
    6: (  # Electronic
        ['电流','霓虹','频率','脉冲','幻境','波长','光圈','潮汐','数字','程序','信号','极光','维度','量子','光年'],
        ['Pixel','Neon','Vortex','Quantum','Circuit','Hologram','Synth','Binary','Laser','Pulse','Digital','Cyber','Waves','Phase','Signal']
    ),
    7: (  # Jazz
        ['夜色','咖啡','雨夜','慵懒','旧梦','摇摆','琴键','蓝调','萨克斯','迷醉','黄昏','月光','独奏','爵士','浪漫'],
        ['Blue','Jazz Night','Velvet','Smoke','Whiskey','Piano','Swing','Moon','Misty','Satin Doll','Autumn','Rain','Midnight','Solo','Blues']
    ),
    8: (  # Classical
        ['序曲','交响','夜曲集','赋格','奏鸣','圆舞','狂想','协奏','变奏','安魂','晨曲','咏叹','间奏','终章','回旋'],
        ['Overture','Symphony','Sonata','Concerto','Waltz','Rhapsody','Fugue','Nocturne','Prelude','Adagio','Aria','Suite','Etude','March','Elegy']
    ),
    9: (  # Metal
        ['烈焰','铁拳','轰鸣','雷霆','战歌','冲锋','怒吼','黑暗','决裂','重生','狂暴','钢铁','末日','天启','怒焰'],
        ['Iron','Steel','Thunder','War','Fury','Metal','Rage','Rise','Storm','Blood','Doom','Chaos','Venom','Oblivion','Savage']
    ),
    10: ( # Indie
        ['岛屿','清晨','独白','雾中','荒原','飞行','坠落','呼吸','零度','暗涌','深海','极夜','蜃楼','潮汐','孤岛'],
        ['Void','Haze','Coast','Float','Shade','Tide','Dawn','Flux','Silt','Drift','Mist','Waves','Luna','Fade','Hollow']
    ),
}

# Era tags: 26=2000s 27=2010s 28=1990s 29=2020s 30=1980s
# Albums per top artist
ALBUMS = {
    1: ['Jay','范特西','八度空间','叶惠美','七里香','十一月的肖邦','依然范特西','我很忙','魔杰座','跨时代','十二新作','周杰伦的床边故事','最伟大的作品'],
    2: ['乐行者','第二天堂','编号89757','曹操','西界','JJ陆','100天','她说','学不会','因你而在','新地球','伟大的渺小','幸存者','重拾_快乐'],
    3: ['打得火热','Shall We Dance','The Line-Up','U87','Life Continues','What\'s Going On','认了吧','H3M','Time Flies','Stranger Under My Skin','The Key','准备中','L.O.V.E.'],
    10: ['Taylor Swift','Fearless','Speak Now','Red','1989','Reputation','Lover','Folklore','Evermore','Midnights'],
    9: ['Parachutes','A Rush of Blood','X&Y','Viva la Vida','Mylo Xyloto','Ghost Stories','A Head Full of Dreams','Everyday Life','Music of the Spheres'],
    49: ['Hybrid Theory','Meteora','Minutes to Midnight','A Thousand Suns','Living Things','The Hunting Party','One More Light'],
    50: ['Queen','Queen II','A Night at the Opera','A Day at the Races','News of the World','Jazz','The Game','The Works','A Kind of Magic','The Miracle','Innuendo'],
}

def is_cn(nat):
    return bool(nat and any(k in str(nat) for k in ['中国','台湾','香港','新加坡','马来西亚']))

def gen_songs(aid, name, nat, count):
    cn = is_cn(nat)
    # Genre distribution per artist type
    if aid in [1,2,3,4,5]:        glist = [1,1,1,2,5,3]    # Mandopop kings
    elif aid in [9,42,49,50]:     glist = [2,2,2,1,9,6]    # Rock bands
    elif aid == 10:               glist = [1,1,3,1,1,2]    # Taylor
    elif aid in range(51,100) and cn: glist = [1,1,2,3,5,1]   # Chinese
    elif aid in range(51,100):    glist = [1,2,1,6,5,2]    # Western
    else:                         glist = [1,2,3,5,10,1]

    # Era distribution
    if aid <= 8 or aid in [21,22,23,24,25,39,40,41,42]:
        eralist = [26,26,27,27,28,28]
    elif aid <= 32: eralist = [27,27,27,29,29,26]
    elif aid >= 51: eralist = [27,27,29,29,27,29]
    else: eralist = [27,27,26,27,29,29]

    used = set()
    songs = []
    for i in range(count):
        gid = random.choice(glist)
        eid = random.choice(eralist)
        cn_pool, en_pool = T[gid]
        pool = cn_pool if cn else en_pool
        title = random.choice(pool)
        # Avoid duplicates
        if title in used:
            sfx = ['Acoustic','Live','Remix','Demo','Piano Ver.','Orchestral','Studio','Deluxe'][i%8]
            title = title + ' (' + sfx + ')'
        used.add(title)

        # Album
        if aid in ALBUMS:
            album = random.choice(ALBUMS[aid])
        else:
            words_cn = ['时光','梦想','旅程','光芒','自由','星空','大海','回忆','未来']
            words_en = ['Echoes','Horizons','Reflections','Origins','Phases','Odyssey','Legacy','Elements','Atlas']
            w = random.choice(words_cn if cn else words_en)
            album = f'{w} Vol.{random.randint(1,4)}'

        dur = random.randint(185, 315)
        pc = random.randint(800, 3500) if aid in [1,2,3,9,10,49,50] else random.randint(100, 1500)
        songs.append((title, aid, album, dur, pc, gid, eid))
    return songs

# ==================== 4. Predefined hits for top artists ====================
PREDEF = {
    1: [  # Jay Chou
        ('告白气球','周杰伦的床边故事',208,1,27),('听妈妈的话','依然范特西',263,1,26),
        ('龙拳','八度空间',266,1,26),('双截棍','范特西',218,1,26),
        ('青花瓷','我很忙',237,1,27),('简单爱','范特西',280,1,26),
        ('以父之名','叶惠美',334,1,26),('东风破','叶惠美',313,1,26),
        ('发如雪','十一月的肖邦',312,1,26),('珊瑚海','十一月的肖邦',259,1,26),
        ('说好的幸福呢','魔杰座',270,1,27),('给我一首歌的时间','魔杰座',258,1,27),
        ('烟花易冷','跨时代',254,1,27),('Mojito','最伟大的作品',186,1,29),
        ('等你下课','等你下课',271,1,27),('不爱我就拉倒','不爱我就拉倒',242,1,27),
    ],
    2: [  # JJ Lin
        ('一千年以后','编号89757',234,1,26),('曹操','曹操',261,2,26),
        ('小酒窝','JJ陆',217,1,27),('她说','她说',231,1,27),
        ('学不会','学不会',298,1,27),('背对背拥抱','100天',251,1,27),
        ('当你','乐行者',257,1,26),('冻结','乐行者',264,1,26),
        ('杀手','西界',244,1,27),('伟大的渺小','伟大的渺小',246,1,27),
        ('黑夜问白天','伟大的渺小',278,1,27),('交换余生','幸存者',243,1,29),
        ('愿与愁','重拾_快乐',262,1,29),('新地球','新地球',232,1,27),
        ('浪漫血液','新地球',261,1,27),('一时的选择','重拾_快乐',248,1,29),
    ],
    3: [  # Eason Chan
        ('K歌之王','打得火热',227,1,26),('你的背包','Special Thanks',239,1,26),
        ('富士山下','What\'s Going On',258,1,26),('最佳损友','Life Continues',231,5,26),
        ('单车','Shall We Dance',203,1,26),('陀飞轮','Time Flies',275,1,27),
        ('苦瓜','Stranger Under My Skin',277,1,27),('淘汰','认了吧',239,1,27),
        ('红玫瑰','认了吧',235,1,27),('孤勇者','孤勇者',248,2,29),
        ('明年今日','The Line-Up',218,1,26),('岁月如歌','Live For Today',247,1,26),
        ('一丝不挂','Time Flies',269,1,27),('任我行','The Key',292,1,27),
        ('我们','我们',229,1,27),('白玫瑰','What\'s Going On',237,1,26),
    ],
    4: [  # G.E.M.
        ('多远都要在一起','新的心跳',218,1,27),('我的秘密','My Secret',245,1,27),
        ('A.I.N.Y.','18...',228,5,27),('后会无期','后会无期',221,3,27),
        ('睡皇后','睡皇后',241,1,27),('岩石里的花','岩石里的花',278,1,27),
        ('摩天动物园','摩天动物园',254,1,29),('透明','摩天动物园',212,6,29),
        ('差不多姑娘','差不多姑娘',244,4,29),('超能力','超能力',199,1,29),
        ('句号','句号',237,1,29),
    ],
    5: [  # Joker Xue
        ('天外来物','天外来物',260,1,29),('像风一样','渡',272,1,27),
        ('暧昧','渡',248,1,27),('刚刚好','初学者',242,1,27),
        ('我好像在哪见过你','初学者',255,1,27),('你还要我怎样','意外',278,3,27),
        ('方圆几里','意外',268,3,27),('怪咖','怪咖',253,1,27),
        ('动物世界','渡',248,2,27),('陪你去流浪','尘',295,3,29),
        ('慢半拍','尘',248,1,29),
    ],
    10: [  # Taylor Swift
        ('Love Story','Fearless',235,1,26),('You Belong With Me','Fearless',232,1,26),
        ('Shake It Off','1989',219,1,27),('Blank Space','1989',231,1,27),
        ('Bad Blood','1989',211,1,27),('Style','1989',231,1,27),
        ('Delicate','Reputation',232,1,27),('Cardigan','Folklore',239,3,29),
        ('Anti-Hero','Midnights',200,1,29),('Cruel Summer','Lover',178,1,29),
        ('Lover','Lover',221,1,29),('All Too Well','Red',329,1,27),
        ('Wildest Dreams','1989',221,1,27),('Enchanted','Speak Now',320,1,27),
        ('Willow','Evermore',214,3,29),('Look What You Made Me Do','Reputation',217,1,27),
    ],
    9: [  # Coldplay
        ('Yellow','Parachutes',269,2,26),('The Scientist','A Rush of Blood',309,2,26),
        ('Clocks','A Rush of Blood',309,2,26),('Fix You','X&Y',295,2,26),
        ('Viva la Vida','Viva la Vida',241,2,27),('Paradise','Mylo Xyloto',279,2,27),
        ('A Sky Full of Stars','Ghost Stories',268,6,27),('Hymn for the Weekend','A Head Full of Dreams',258,1,27),
        ('Adventure of a Lifetime','A Head Full of Dreams',264,2,27),('Something Just Like This','Kaleidoscope EP',247,6,27),
        ('My Universe','Music of the Spheres',227,1,29),('Higher Power','Music of the Spheres',233,1,29),
        ('Orphans','Everyday Life',237,2,29),('Everglow','A Head Full of Dreams',233,2,27),
        ('Magic','Ghost Stories',285,2,27),('In My Place','A Rush of Blood',235,2,26),
    ],
    49: [  # Linkin Park
        ('In the End','Hybrid Theory',216,2,26),('Numb','Meteora',187,2,26),
        ('What I\'ve Done','Minutes to Midnight',205,2,27),('New Divide','New Divide',269,2,27),
        ('Burn It Down','Living Things',230,2,27),('Castle of Glass','Living Things',205,2,27),
        ('One Step Closer','Hybrid Theory',155,9,26),('Faint','Meteora',162,2,26),
        ('Breaking the Habit','Meteora',196,2,26),('Somewhere I Belong','Meteora',214,2,26),
        ('Crawling','Hybrid Theory',209,9,26),('Papercut','Hybrid Theory',185,9,26),
        ('Leave Out All the Rest','Minutes to Midnight',209,2,27),('Shadow of the Day','Minutes to Midnight',289,2,27),
        ('Waiting for the End','A Thousand Suns',231,2,27),('Iridescent','A Thousand Suns',239,2,27),
    ],
    50: [  # Queen
        ('Bohemian Rhapsody','A Night at the Opera',355,2,30),('We Will Rock You','News of the World',122,2,30),
        ('We Are the Champions','News of the World',180,2,30),('Don\'t Stop Me Now','Jazz',209,2,30),
        ('Somebody to Love','A Day at the Races',296,2,30),('Radio Ga Ga','The Works',348,1,30),
        ('I Want to Break Free','The Works',260,2,30),('Under Pressure','Hot Space',245,2,30),
        ('Another One Bites the Dust','The Game',216,2,30),('Crazy Little Thing Called Love','The Game',162,2,30),
        ('Killer Queen','Sheer Heart Attack',180,2,30),('Love of My Life','A Night at the Opera',219,2,30),
        ('The Show Must Go On','Innuendo',271,2,30),('Who Wants to Live Forever','A Kind of Magic',317,2,30),
        ('I Want It All','The Miracle',241,2,30),('Hammer to Fall','The Works',220,2,30),
    ],
}

# Artist tag mapping
ARTIST_TAGS = {1:19, 2:20, 3:21, 10:22, 11:23, 42:24, 49:25}

# ==================== 5. Generate all songs ====================
cur.execute("SELECT id, name, nationality FROM artists ORDER BY id")
all_artists = cur.fetchall()

# Get existing song titles for dedup
cur.execute("SELECT title, artist_id FROM songs")
existing = set(cur.fetchall())

all_songs = []    # (title, artist_id, album, duration, play_count)
all_tags = []     # list of [tag_id, tag_id, ...] per song, aligned with all_songs

for aid, name, nat in all_artists:
    # Predefined first
    songs_this = []
    if aid in PREDEF:
        for title, album, dur, gid, eid in PREDEF[aid]:
            if (title, aid) not in existing:
                songs_this.append((title, aid, album, dur, random.randint(1000,4000)))
                all_tags.append([gid, eid] + ([ARTIST_TAGS[aid]] if aid in ARTIST_TAGS else []))
                existing.add((title, aid))

    # Target count per artist
    if aid in [1,2,3,9,10,49,50]: target = 20
    elif aid <= 32: target = 15
    elif aid <= 50: target = 10
    else: target = 8

    extra = target - len(songs_this)
    if extra > 0:
        gen = gen_songs(aid, name, nat, extra)
        for title, art_id, album, dur, pc, gid, eid in gen:
            if (title, aid) not in existing:
                songs_this.append((title, aid, album, dur, pc))
                all_tags.append([gid, eid] + ([ARTIST_TAGS[aid]] if aid in ARTIST_TAGS else []))
                existing.add((title, aid))

    all_songs.extend(songs_this)

print(f"New songs to insert: {len(all_songs)}")

# ==================== 6. Insert songs ====================
if all_songs:
    # Batch insert
    batch_size = 100
    for i in range(0, len(all_songs), batch_size):
        batch = all_songs[i:i+batch_size]
        cur.executemany(
            "INSERT INTO songs (title, artist_id, album, duration, play_count) VALUES (%s,%s,%s,%s,%s)",
            batch)
    conn.commit()
    print(f"Inserted {len(all_songs)} songs")

# ==================== 7. Insert song_tags ====================
# Get IDs of newly inserted songs
cur.execute("SELECT MAX(id) FROM songs")
max_id = cur.fetchone()[0]
# Original 200 songs
new_start = max_id - len(all_songs) + 1

cur.execute("SELECT id FROM songs WHERE id >= %s ORDER BY id", (new_start,))
new_ids = [r[0] for r in cur.fetchall()]

assert len(new_ids) == len(all_tags), f"ID count {len(new_ids)} != tag count {len(all_tags)}"

tag_values = []
for i, song_id in enumerate(new_ids):
    for tag_id in all_tags[i]:
        tag_values.append((song_id, tag_id))

cur.executemany("INSERT INTO song_tags (song_id, tag_id) VALUES (%s,%s)", tag_values)
conn.commit()
print(f"Inserted {len(tag_values)} song_tags")

# ==================== 8. Rebuild charts ====================
cur.execute("DELETE FROM charts")
cur.execute("SELECT id, play_count FROM songs ORDER BY play_count DESC")
ranked = cur.fetchall()

def fill_chart(ctype, cnt):
    pool = list(ranked)
    random.shuffle(pool)
    for sid, pc in pool[:cnt]:
        adj_play = pc + random.randint(-300, 300)
        adj_fav = max(10, pc // 10 + random.randint(-50, 50))
        cur.execute(
            "INSERT INTO charts (song_id, chart_type, play_count, fav_count, hot_score, rank_date) VALUES (%s,%s,%s,%s,%s,CURDATE())",
            (sid, ctype, max(0, adj_play), adj_fav, int(adj_play * 0.4 + adj_fav * 0.6)))

fill_chart('daily', 50)
fill_chart('weekly', 50)
fill_chart('total', 50)
conn.commit()
print("Charts rebuilt")

# ==================== 9. Report ====================
cur.execute("SELECT COUNT(*) FROM songs")
ns = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM artists")
na = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM song_tags")
nt = cur.fetchone()[0]
cur.execute("SELECT a.name, COUNT(s.id) FROM artists a LEFT JOIN songs s ON a.id=s.artist_id GROUP BY a.id ORDER BY COUNT(s.id) DESC LIMIT 5")
top = cur.fetchall()

print(f"\n=== Done ===")
print(f"Artists: {na} | Songs: {ns} | Tags: {nt}")
print(f"Top artists: {[(n,c) for n,c in top]}")

cur.close()
conn.close()
