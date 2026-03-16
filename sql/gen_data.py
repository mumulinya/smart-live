import random
import datetime
import os

# --- Configuration ---
NUM_USERS = 200
NUM_SHOPS = 200
NUM_PRODUCTS = 2000
NUM_BLOGS = 2000
NUM_REVIEWS = 15000
NUM_ORDERS = 20000
NUM_FOLLOWS = 2000
NUM_LIKES = 5000
NUM_STARS = 4000
NUM_COMMENTS = 4000

OUTPUT_DIR = 'd:/smart-live/smart-live-Cloud/sql/test_data'
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

# --- Categorized Data Pools ---
TYPE_DATA = {
    1: { # 美食
        'products': [('招牌浓郁骨汤拉面', 'ramen'), ('特级安格斯肉眼牛排', 'steak'), ('单人超值午餐套票', 'lunch'), ('夏日缤纷水果茶', 'tea'), ('至尊海鲜双人餐', 'seafood'), ('网红黑糖珍珠奶茶', 'milktea')],
        'reviews': ['味道挺正宗的，份量也足，推荐！', '环境很有格调，上菜速度也快。', '团购价格很划算，性价比之王。', '味道一般，排队太久了，体验不太好。', '服务员态度很好，菜品颜值巨高。'],
        'blog_titles': ['吃货必看！佛山隐藏最深的宝藏餐厅', '这家火锅店我能吃一辈子！', '周末探店：颜值与实力并存的排队王', '不到100块就能吃到撑的良心小店'],
        'blog_contents': ['今天来打卡这家心选小店，味道真的惊艳到我了！肉质鲜美，汤头浓郁，强烈推荐。', '环境超级好，随便一拍都很出片。味道也在线，没有踩雷，放心冲！'],
        'img_kws': ['food', 'restaurant', 'meal']
    },
    2: { # KTV
        'products': [('下午场欢唱3小时', 'ktv'), ('黄金时段大包房套餐', 'ktv'), ('单人欢唱团购券', 'ktv'), ('深夜狂欢酒水套餐', 'party')],
        'reviews': ['音响效果不错，曲库也很全。', '房间挺干净的，没有异味。', '服务态度很好，送餐也很准时。', '性价比很高，周末聚会首选。', '稍微有点贵，但设备确实是顶级的。'],
        'blog_titles': ['周末聚会哪里去？这家KTV氛围感拉满', '佛山神仙KTV推荐，曲库超全不踩雷', '和死党唱到失声！这家店真的很嗨'],
        'blog_contents': ['这家KTV的环境真的没话说，灯光效果也酷炫。最重要的是音响真的顶，唱起来一点也不费力！', '周末和朋友来聚会，包间很大，服务也贴心。大家玩得很开心，下次还来。'],
        'img_kws': ['ktv', 'karaoke', 'party']
    },
    3: { # 丽人·美发
        'products': [('首席理发师单人剪发', 'haircut'), ('深层滋养头皮护理', 'haircare'), ('时尚资深烫发套餐', 'hair'), ('潮流男士快剪', 'menhair')],
        'reviews': ['剪得很有层次感，发型师很有耐心。', '洗头的小哥手法很专业，很舒服。', '环境干净明亮，没有推销，好评。', '虽然有点贵，但一分钱一分货。', '效果很满意，朋友都说好。'],
        'blog_titles': ['换个发型换个心情！这家店的理发师太会了', '佛山口碑理发店测评，拒绝无效理发', '夏日清爽短发分享，修饰脸型满分'],
        'blog_contents': ['终于找到了适合自己的发型师！剪出来的效果完全符合预期，细节处理得非常好。', '这家店的环境很高级，服务也非常专业。洗护产品都是大牌，用着很安心。'],
        'img_kws': ['haircut', 'salon', 'hair']
    },
    4: { # 健身运动
        'products': [('专业私人教练1对1课程', 'fitness'), ('健身月卡不限次使用', 'gym'), ('单人力量训练体验课', 'workout'), ('游泳健身单次通票', 'swim')],
        'reviews': ['教练很专业，动作纠正很到位。', '器材很全，环境也挺好的。', '浴室很干净，热水也足。', '人不是很多，不用排队练。', '氛围很好，很有健身动力。'],
        'blog_titles': ['自律给我自由！坚持打卡第100天', '佛山最强健身房推荐，器材多到哭', '夏季减脂攻略，这家店的教练太狠了'],
        'blog_contents': ['这里的健身氛围真的超棒！每个人都在努力流汗。器材维护得很好，教练也很乐于助人。', '为了夏天的腹肌，拼了！这家健身房离家近，设施全，是我坚持下来的动力。'],
        'img_kws': ['gym', 'fitness', 'workout']
    },
    5: { # 按摩·足疗
        'products': [('招牌足底按摩60分钟', 'massage'), ('全身经络推拿套餐', 'bodycare'), ('中式舒压按摩套餐', 'relax'), ('泰式古法按摩体验', 'thai')],
        'reviews': ['技师手法很专业，力道刚好。', '按完感觉整个人都轻松了，舒服。', '环境很安静，私密性很好。', '免费零食和饮料不错，贴心。', '性价比很高，下次带家人来。'],
        'blog_titles': ['打工人续命指南：这家按摩店太解压了', '佛山足疗老店分享，手法地道不踩雷', '周末放松好去处，感觉整个人都轻了'],
        'blog_contents': ['上一周班太累了，来这里按一下真的全身舒坦。技师手法很有力量感，按到了痛点。', '环境很温馨，灯光幽暗很助眠。服务也很周到，还有热毛巾和姜茶，好评！'],
        'img_kws': ['massage', 'spa', 'relax']
    },
    6: { # 美容SPA
        'products': [('面部深层净透护理', 'beauty'), ('全身体验SPA芳疗', 'spa'), ('补水提亮焕活面部套餐', 'skincare'), ('单人肩颈舒压SPA', 'neck')],
        'reviews': ['做完感觉脸亮了一个度，好评。', '服务非常细心，整个过程很享受。', '精油的味道很好闻，很放松。', '环境很高端，仪式感满满。', '皮肤状况改善了很多，长期回购。'],
        'blog_titles': ['沉浸式SPA体验，做一个精致的猪猪女孩', '佛山高端美容院测评，皮肤变好的秘密', '悦己时光：这家SPA店的氛围我能待一天'],
        'blog_contents': ['今天来做个全能SPA，真的太治愈了。小姐姐手法轻柔，全程无推销，真的可以完全放松。', '做完护理感觉毛孔都呼吸顺畅了。这里的环境真的很私密，细节处处体现高级感。'],
        'img_kws': ['spa', 'beauty', 'skincare']
    },
    7: { # 亲子游乐
        'products': [('双人全天无限畅玩票', 'playground'), ('亲子烘焙DIY体验课', 'kids'), ('单人益智游乐园门票', 'toy'), ('少儿体能素质体验课', 'play')],
        'reviews': ['孩子玩得不想回家，环境挺安全的。', '玩具种类很多，都很干净。', '服务人员对小孩很有耐心。', '周末人有点多，排队时间长。', '性价比不错，办卡更划算。'],
        'blog_titles': ['周末遛娃神器！这家游乐园我给满分', '佛山亲子好去处分享，解放双手不是梦', '带娃打卡室内乐园，这配置绝了'],
        'blog_contents': ['这家乐园的设施真的非常丰富，适合各个年龄段的孩子。最重要的是卫生搞得很好，放心。', '周末带孩子来这里放电，我可以坐在一旁喝咖啡，真的太轻松了。孩子玩得很开心，我也很满意。'],
        'img_kws': ['playground', 'kids', 'toy']
    },
    8: { # 酒吧
        'products': [('单人特调鸡尾酒', 'cocktail'), ('精酿啤酒畅饮双人套餐', 'beer'), ('氛围感威士忌套餐', 'whisky'), ('深夜食堂人气小吃拼盘', 'snack')],
        'reviews': ['调酒师技术很好，酒的味道很有层次感。', '音乐很棒，氛围感拉满。', '适合约会，灯光非常有感觉。', '酒类品种很多，挑花了眼。', '价格适中，还会带朋友来。'],
        'blog_titles': ['微醺之夜：佛山藏在巷子里的神仙清吧', '拒绝夜店喧嚣！佛山氛围感酒吧推荐', '来一杯马天尼，治愈这一整天的疲惫'],
        'blog_contents': ['这家酒吧的环境真的很适合三五好友闲聊。调酒师会根据你的口味定制酒水。', '氛围非常有格调，音乐不吵闹，刚好可以说话。这里的特调真的很绝，一定要试一试。'],
        'img_kws': ['bar', 'cocktail', 'whisky']
    },
    9: { # 轰趴馆
        'products': [('全天包场团建套餐', 'party'), ('超值双人观影桌游包间', 'game'), ('单人狂欢畅想通票', 'hobby'), ('生日派对定制布置套餐', 'party')],
        'reviews': ['设备很齐全，桌游、ktv都有。', '老板人很好，还帮我们布置房间。', '环境很宽敞，二三十个人都没问题。', '适合同学聚会，玩得特别high。', '性价比超高，大家分摊下来不贵。'],
        'blog_titles': ['团建聚会看过来！佛山最全轰趴馆推荐', '和朋友连住2天不重样！这家轰趴房太飒了', '毕业聚会首选！这家店承包了我们所有的快乐'],
        'blog_contents': ['这家轰趴馆简直是聚会天堂！台球、麻将、电影房应有尽有。厨房很大，我们还自己煮了火锅。', '老板非常热情，设备都很新。空间足够大，大家各玩各的完全不会互相干扰，太爽了。'],
        'img_kws': ['party', 'room', 'game']
    },
    10: { # 美睫·美甲
        'products': [('精致纯色美甲套餐', 'nail'), ('自然浓密假睫毛嫁接', 'eyelash'), ('定制手绘美甲艺术', 'nailart'), ('日系清新美睫美甲双套餐', 'beauty')],
        'reviews': ['美甲师做得非常细心，颜色很正。', '睫毛接得很自然，一点也不扎眼。', '款式很多可以选，审美在线。', '服务态度很好，还给倒了花茶。', '持久度很高，一个月了都没掉。'],
        'blog_titles': ['指尖的艺术！这家美甲店的审美我爱了', '佛山神仙美睫推荐，眼睛瞬间变大一倍', '夏日冰透美甲分享，真的太显白了'],
        'blog_contents': ['终于做到了心心念念的美甲款式！美甲师的手艺真的没话说，线条勾勒得非常精准。', '第一次接睫毛，效果非常惊艳，完全没有异物感。店里的环境也很温馨，让人很放松。'],
        'img_kws': ['nail', 'eyelash', 'manicure']
    }
}

NICE_SHOP_PREFIX = ['浮生', '锦瑟', '拾光', '悦己', '初见', '半闲', '归园', '青未', '朝暮', '云栖', '桃源', '溪山', '隐逸', '花间', '墨染', '筑梦', '遇见', '心享', '逸境', '尚品']
PRODUCT_SUBTITLES = ['新品上市', '限时特惠', '店长推荐', '超值划算', '人气爆款', '精选好物', '品质保证', '镇店之宝']
USER_NAMES_P1 = ['快乐的', '奔跑的', '安静的', '无敌的', '神秘的', '可爱的', '帅气的', '温柔的', '发财的', '幸运的', '干饭的', '迷茫的', '佛系的']
USER_NAMES_P2 = ['二哈', '橘猫', '程序猿', '打工人', '干饭人', '夜猫子', '小仙女', '老司机', '吃货', '胖丁', '布偶猫', '小黄人', '闪电侠']


NICE_SHOP_SUFFIX = {
    1: ['食苑', '雅厨', '火锅', '私房菜', '馆', '小聚', '味道'], # 美食
    2: ['唱响', '悦量', 'KTV', '麦浪', '空间'], # KTV
    3: ['造型', '美发', '工坊', '丝语', '视觉'], # 丽人
    4: ['能量', '健身', '中心', '工作室', '汗水'], # 健身
    5: ['足道', '养神', '舒压', '工坊', '空间'], # 按摩
    6: ['悦肤', '美学', 'SPA', '私享', '空间'], # 美容SPA
    7: ['营地', '乐园', '时光', '天地', '堡垒'], # 亲子
    8: ['微醺', '清吧', '实验室', '角落', '夜色'], # 酒吧
    9: ['派对', '空间', '盒子', '公馆', '潮玩'], # 轰趴
    10: ['指尖', '艺术', '美甲', '工坊', '悦色'] # 美睫
}

# --- Helper Functions ---
def get_now():
    return datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')

def random_date(start_year=2026, start_month=1, start_day=15, end_year=2026, end_month=3, end_day=15):
    start = datetime.datetime(start_year, start_month, start_day)
    end = datetime.datetime(end_year, end_month, end_day)
    return (start + datetime.timedelta(seconds=random.randint(0, int((end - start).total_seconds())))).strftime('%Y-%m-%d %H:%M:%S')

def get_weighted_score():
    r = random.random()
    if r < 0.15: # High
        return 5
    elif r < 0.30: # Low
        return random.randint(1, 2)
    else: # Medium
        return random.randint(3, 4)

def get_audit_status():
    r = random.random()
    if r < 0.7:
        return 1, 1 # Approved
    elif r < 0.9:
        return 0, 0 # Pending
    else:
        return 2, 2 # Rejected

# --- Data Generation Stacks ---
def get_db_setup(db_name):
    return [
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        f"USE `{db_name}`;",
    ]

db_sql = {
    'smart-live_user': get_db_setup('smart-live_user') + ["TRUNCATE TABLE `user`;", "TRUNCATE TABLE `user_info`;"],
    'smart-live_shop': get_db_setup('smart-live_shop') + ["TRUNCATE TABLE `shop_type`;", "TRUNCATE TABLE `shop`;"],
    'smart-live_system': get_db_setup('smart-live_system') + ["DELETE FROM `sys_user` WHERE user_id != 1;", "DELETE FROM `sys_user_role` WHERE user_id != 1;", "DELETE FROM `sys_user_shop` WHERE user_id != 1;"],
    'smart-live_product': get_db_setup('smart-live_product') + ["TRUNCATE TABLE `product`;"],
    'smart-live_blog': get_db_setup('smart-live_blog') + ["TRUNCATE TABLE `blog`;"],
    'smart-live_order': get_db_setup('smart-live_order') + ["TRUNCATE TABLE `order`;"],
    'smart-live_interaction': get_db_setup('smart-live_interaction') + ["TRUNCATE TABLE `review`;", "TRUNCATE TABLE `follow`;", "TRUNCATE TABLE `like_record`;", "TRUNCATE TABLE `star`;", "TRUNCATE TABLE `comment`;"],
    'smart-live_points': get_db_setup('smart-live_points') + ["TRUNCATE TABLE `user_points_wallet`;"],
    'smart-live_audit': get_db_setup('smart-live_audit') + ["TRUNCATE TABLE `audit_task`;"]
}

def add_audit_task(biz_id, biz_type, submitter_id, status, content_name):
    reason = "'内容违规'" if status == 2 else "NULL"
    content = f'{{"name": "{content_name}", "note": "Generated by script"}}'
    db_sql['smart-live_audit'].append(f"INSERT INTO `audit_task` (`biz_id`, `biz_type`, `submitter_id`, `status`, `reason`, `audit_content`, `create_time`) VALUES ({biz_id}, {biz_type}, {submitter_id}, {status}, {reason}, '{content}', '{get_now()}');")

# 1. Users
BCRYPT_PASSWORD = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'
for i in range(1, NUM_USERS + 1):
    phone = f"13{random.randint(0, 9):01d}{random.randint(10000000, 99999999)}"
    nick = random.choice(USER_NAMES_P1) + random.choice(USER_NAMES_P2) + str(random.randint(10, 99))
    db_sql['smart-live_user'].append(f"INSERT INTO `user` (`id`, `phone`, `password`, `nick_name`, `icon`) VALUES ({i}, '{phone}', '{BCRYPT_PASSWORD}', '{nick}', 'https://api.dicebear.com/7.x/avataaars/svg?seed={nick}');")
    city = random.choice(['上海', '北京', '深圳', '广州', '杭州', '成都', '佛山', '东莞', '中山'])
    db_sql['smart-live_user'].append(f"INSERT INTO `user_info` (`user_id`, `city`, `introduce`, `fans`, `followee`, `liked`, `gender`, `birthday`, `credits`, `level`, `audit_status`) VALUES ({i}, '{city}', 'Hello, I am {nick}', {random.randint(0, 500)}, {random.randint(0, 200)}, {random.randint(0, 1000)}, {random.randint(0, 1)}, '1990-01-01', {random.randint(0, 5000)}, {random.randint(0, 9)}, 1);")

# 2. Shops Pre-determination
# Updated shop types with keywords for images
shop_types = [
    (1, '美食', '/types/ms.png', 1, 'restaurant'),
    (2, 'KTV', '/types/KTV.png', 2, 'ktv'),
    (3, '丽人·美发', '/types/lrmf.png', 3, 'haircut'),
    (4, '健身运动', '/types/jsyd.png', 10, 'gym'),
    (5, '按摩·足疗', '/types/amzl.png', 5, 'massage'),
    (6, '美容SPA', '/types/spa.png', 6, 'spa'),
    (7, '亲子游乐', '/types/qzyl.png', 7, 'playground'),
    (8, '酒吧', '/types/jiuba.png', 8, 'bar'),
    (9, '轰趴馆', '/types/hpg.png', 9, 'party'),
    (10, '美睫·美甲', '/types/mjmj.png', 4, 'nail')
]

for t_id, name, icon, sort, kw in shop_types:
    db_sql['smart-live_shop'].append(f"INSERT INTO `shop_type` (`id`, `name`, `icon`, `sort`) VALUES ({t_id}, '{name}', '{icon}', {sort});")

def random_seckill_dates():
    # Base date March 16th, 2026
    base = datetime.datetime(2026, 3, 16)
    # Random start within 30 days before/after base
    days_offset = random.randint(-30, 15) # Start can be in the past or near future
    start_dt = base + datetime.timedelta(days=days_offset, hours=random.randint(0, 23))
    # End is 1-14 days after start
    end_dt = start_dt + datetime.timedelta(days=random.randint(1, 14))
    return f"'{start_dt.strftime('%Y-%m-%d %H:%M:%S')}'", f"'{end_dt.strftime('%Y-%m-%d %H:%M:%S')}'"

foshan_areas = ['禅城区', '南海区', '顺德区', '三水区', '高明区']
foshan_streets = ['祖庙', '桂城', '大良', '西南', '荷城', '石湾', '狮山', '容桂', '北舅', '乐从', '九江', '西樵', '丹灶', '里水']

approved_shop_ids = []
shops_metadata = {} # id -> (status, audit_status, name, type_info)
for i in range(1, NUM_SHOPS + 1):
    t_info = random.choice(shop_types)
    t_id = t_info[0]
    status, audit_status = get_audit_status()
    name = f"{random.choice(NICE_SHOP_PREFIX)}{random.choice(NICE_SHOP_SUFFIX[t_id])}"
    shops_metadata[i] = (status, audit_status, name, t_info)
    if audit_status == 1:
        approved_shop_ids.append(i)

# 3. Products & Blogs (Only for Approved Shops)
approved_product_ids = []
product_metadata = {} # id -> (shop_ids_str, name, audit_status, price, img_kw)
for i in range(1, NUM_PRODUCTS + 1):
    # Support 1-3 shops per product
    num_s = random.choice([1, 1, 1, 2, 3])
    s_ids = random.sample(approved_shop_ids, min(num_s, len(approved_shop_ids)))
    shop_ids_str = ",".join(map(str, s_ids))
    
    # Pick the first shop's type for product name consistency
    first_shop_id = s_ids[0]
    first_shop_type = shops_metadata[first_shop_id][3][0]
    
    status, audit_status = get_audit_status()
    p_info = random.choice(TYPE_DATA[first_shop_type]['products'])
    prod_name, img_kw = p_info
    price = random.randint(10, 500)
    product_metadata[i] = (shop_ids_str, prod_name, audit_status, price, img_kw, first_shop_type)
    if audit_status == 1:
        approved_product_ids.append(i)

approved_blog_ids = []
blog_metadata = {} # id -> (shop_id, user_id, audit_status, type_id)
for i in range(1, NUM_BLOGS + 1):
    s_id = random.choice(approved_shop_ids)
    s_type = shops_metadata[s_id][3][0]
    u_id = random.randint(1, NUM_USERS)
    status, audit_status = get_audit_status()
    blog_metadata[i] = (s_id, u_id, audit_status, s_type)
    if audit_status == 1:
        approved_blog_ids.append(i)

# 4. Orders (Only for Approved Products)
orders_data = [] # (id, u_id, prod_id, shop_ids_str, status, amount, create_time, use_time, verify_shop_id)
product_sold = {i: 0 for i in range(1, NUM_PRODUCTS + 1)}
shop_sold = {i: 0 for i in range(1, NUM_SHOPS + 1)}

for i in range(1, NUM_ORDERS + 1):
    u_id = random.randint(1, NUM_USERS)
    prod_id = random.choice(approved_product_ids)
    shop_ids_str, _, _, _, _, _ = product_metadata[prod_id]
    s_id_list = shop_ids_str.split(',')
    
    order_status = random.choice([2, 3]) # 2-Paid, 3-Completed
    amount = 1 
    create_time_str = random_date()
    use_time = "NULL"
    verify_shop_id = "NULL"
    
    product_sold[prod_id] += amount
    
    if order_status == 3:
        create_dt = datetime.datetime.strptime(create_time_str, '%Y-%m-%d %H:%M:%S')
        use_dt = create_dt + datetime.timedelta(minutes=random.randint(30, 1440))
        use_time = f"'{use_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
        v_shop_id = random.choice(s_id_list)
        verify_shop_id = v_shop_id
        shop_sold[int(v_shop_id)] += amount
            
    orders_data.append((i, u_id, prod_id, shop_ids_str, order_status, amount, create_time_str, use_time, verify_shop_id))

# 5. Reviews (Only for Approved Shops/Products/Orders)
reviews_data = []
approved_review_ids = []
shop_scores = {i: [] for i in range(1, NUM_SHOPS + 1)}
completed_orders = [o for o in orders_data if o[4] == 3 and o[8] != "NULL"]

for i in range(1, NUM_REVIEWS + 1):
    review_type = random.choice(['shop', 'product'])
    status, audit_status = get_audit_status()
    score = get_weighted_score()
    
    if review_type == 'product' and completed_orders:
        order = random.choice(completed_orders)
        u_id, prod_id, s_id, order_id = order[1], order[2], int(order[8]), order[0]
        source_type, source_id = 4, prod_id
        use_time_str = order[7].strip("'")
        use_dt = datetime.datetime.strptime(use_time_str, '%Y-%m-%d %H:%M:%S')
        review_time = (use_dt + datetime.timedelta(hours=random.randint(1, 48))).strftime('%Y-%m-%d %H:%M:%S')
        s_type = product_metadata[prod_id][5]
    else:
        u_id = random.randint(1, NUM_USERS)
        s_id = random.choice(approved_shop_ids)
        source_type, source_id, order_id = 2, s_id, "NULL"
        review_time = random_date()
        s_type = shops_metadata[s_id][3][0]
        
    if audit_status == 1:
        approved_review_ids.append(i)
        shop_scores[s_id].append(score)
    
    content = random.choice(TYPE_DATA[s_type]['reviews'])
    reviews_data.append((i, u_id, s_id, order_id, source_type, source_id, score, status, audit_status, review_time, content))

# --- Final SQL Generation ---

# Shops
for i in range(1, NUM_SHOPS + 1):
    status, audit_status, name, t_info = shops_metadata[i]
    t_id, _, _, _, kw = t_info
    area = random.choice(foshan_areas)
    street = random.choice(foshan_streets)
    x, y = 113.0 + random.random() * 0.3, 23.0 + random.random() * 0.2
    scores = shop_scores[i]
    avg_score = int((sum(scores) / len(scores)) * 10) if scores else 45
    shop_img = f"https://loremflickr.com/400/300/{kw},shop?lock={i}"
    shop_logo = f"https://api.dicebear.com/7.x/identicon/svg?seed={name}"
    db_sql['smart-live_shop'].append(f"INSERT INTO `shop` (`id`, `name`, `shop_logo`, `type_id`, `images`, `area`, `address`, `x`, `y`, `avg_price`, `score`, `reviews`, `sold`, `open_hours`, `status`, `audit_status`) VALUES ({i}, '{name}', '{shop_logo}', {t_id}, '{shop_img}', '{area}', '广东省佛山市{area}{street}街道{random.randint(1, 999)}号', {x:.6f}, {y:.6f}, {random.randint(20, 500)}, {avg_score}, {len(scores)}, {shop_sold[i]}, '09:00-22:00', {status}, {audit_status});")
    add_audit_task(i, 2, 1, audit_status, name)
    sys_user_id = 1000 + i
    sys_status = '0' if audit_status == 1 else '1'
    db_sql['smart-live_system'].append(f"INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `password`, `status`, `create_time`) VALUES ({sys_user_id}, 100, 'merchant_{i}', '商家店长', '{BCRYPT_PASSWORD}', '{sys_status}', '{get_now()}');")
    db_sql['smart-live_system'].append(f"INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES ({sys_user_id}, 100);")
    db_sql['smart-live_system'].append(f"INSERT INTO `sys_user_shop` (`user_id`, `shop_id`, `create_time`) VALUES ({sys_user_id}, {i}, '{get_now()}');")

# Products
for i in range(1, NUM_PRODUCTS + 1):
    if i not in product_metadata: continue
    shop_ids_str, name, audit_status, price, img_kw, _ = product_metadata[i]
    category = random.randint(1, 2)
    activity_type = 1 if random.random() < 0.3 else 0
    status = 1 if audit_status == 1 else (0 if audit_status == 0 else 2)
    sold = product_sold[i]
    b_time, e_time = ("NULL", "NULL")
    if activity_type == 1:
        b_time, e_time = random_seckill_dates()
    prod_img = f"https://loremflickr.com/400/400/{img_kw}?lock={i}"
    db_sql['smart-live_product'].append(f"INSERT INTO `product` (`id`, `shop_id`, `name`, `sub_title`, `price`, `original_price`, `category`, `activity_type`, `status`, `audit_status`, `stock`, `sold`, `validity_type`, `valid_days`, `cover_img`, `begin_time`, `end_time`, `create_time`) VALUES ({i}, '{shop_ids_str}', '{name}', '{random.choice(PRODUCT_SUBTITLES)}', {price}.00, {price+20}.00, {category}, {activity_type}, {status}, {audit_status}, {sold + random.randint(10, 100)}, {sold}, 2, 90, '{prod_img}', {b_time}, {e_time}, '{random_date()}');")
    first_shop_id = int(shop_ids_str.split(',')[0])
    add_audit_task(i, 4 if category == 1 else 6, 1000 + first_shop_id, audit_status, name)

# Blogs
for i in range(1, NUM_BLOGS + 1):
    if i not in blog_metadata: continue
    s_id, u_id, audit_status, s_type = blog_metadata[i]
    status = 1 if audit_status == 1 else (0 if audit_status == 0 else 2)
    title = random.choice(TYPE_DATA[s_type]['blog_titles'])
    content = random.choice(TYPE_DATA[s_type]['blog_contents'])
    kw = random.choice(TYPE_DATA[s_type]['img_kws'])
    blog_img = f"https://loremflickr.com/600/400/{kw}?lock={i}"
    db_sql['smart-live_blog'].append(f"INSERT INTO `blog` (`id`, `shop_id`, `type_id`, `user_id`, `title`, `images`, `content`, `liked`, `stared`, `comments`, `status`, `audit_status`, `create_time`) VALUES ({i}, {s_id}, 1, {u_id}, '{title}', '{blog_img}', '{content}', {random.randint(0, 500)}, {random.randint(0, 100)}, 0, {status}, {audit_status}, '{random_date()}');")
    add_audit_task(i, 3, u_id, audit_status, title)

# Orders
for o in orders_data:
    i, u_id, prod_id, shop_ids_str, status, amount, create_time, use_time, verify_shop_id = o
    # Get product price from metadata (it's random but we can re-derive it or store it)
    # For simplicity and realism, let's use the price we'll generate later or store it now
    base_price = int(product_metadata[prod_id][3])
    # Pay amount is base_price * amount (amount is 1)
    pay_amount = base_price 
    db_sql['smart-live_order'].append(f"INSERT INTO `order` (`id`, `user_id`, `source_type`, `source_id`, `pay_amount`, `amount`, `status`, `shop_id`, `verify_shop_id`, `use_time`, `create_time`) VALUES ({i}, {u_id}, 1, {prod_id}, {pay_amount}, {amount}, {status}, '{shop_ids_str}', {verify_shop_id}, {use_time}, '{create_time}');")

# Reviews
for rev in reviews_data:
    i, u_id, s_id, order_id, source_type, source_id, score, status, audit_status, review_time, content = rev
    ts, es, ss = [min(5, max(1, score + random.choice([-1, 0, 1]))) for _ in range(3)]
    s_type = shops_metadata[s_id][3][0]
    kw = random.choice(TYPE_DATA[s_type]['img_kws'])
    rev_img = f"'https://loremflickr.com/400/300/{kw}?lock={i}'" if random.random() < 0.3 else "NULL"
    db_sql['smart-live_interaction'].append(f"INSERT INTO `review` (`id`, `user_id`, `shop_id`, `order_id`, `source_type`, `source_id`, `content`, `images`, `score`, `taste_score`, `env_score`, `service_score`, `status`, `audit_status`, `create_time`) VALUES ({i}, {u_id}, {s_id}, {order_id}, {source_type}, {source_id}, '{content}', {rev_img}, {score}, {ts}, {es}, {ss}, {status}, {audit_status}, '{review_time}');")
    add_audit_task(i, 5, u_id, audit_status, f"Review_{i}")

# Comments
for i in range(1, NUM_COMMENTS + 1):
    stype = random.choice([3, 7]) 
    target_pool = approved_blog_ids if stype == 3 else approved_review_ids
    if not target_pool: continue
    sid = random.choice(target_pool)
    status, audit_status = get_audit_status()
    
    # Match comment content to target type if possible
    if stype == 3: # Blog
        target_type = blog_metadata[sid][3]
        content = random.choice(TYPE_DATA[target_type]['reviews']) # Use reviews as comment source
    else: # Review
        target_s_id = reviews_data[sid-1][2]
        target_type = shops_metadata[target_s_id][3][0]
        content = random.choice(TYPE_DATA[target_type]['reviews'])
        
    com_img = f"'https://loremflickr.com/400/300/lifestyle?lock={i}'" if random.random() < 0.1 else "NULL"
    db_sql['smart-live_interaction'].append(f"INSERT INTO `comment` (`user_id`, `source_type`, `source_id`, `parent_id`, `content`, `images`, `liked`, `reply_count`, `status`, `audit_status`, `create_time`) VALUES ({random.randint(1, NUM_USERS)}, {stype}, {sid}, 0, '{content}', {com_img}, {random.randint(0, 20)}, 0, {status}, {audit_status}, '{random_date()}');")

# Interactions (Likes/Stars)
for _ in range(NUM_LIKES):
    stype = random.choice([2, 3, 7]) 
    pool = approved_shop_ids if stype == 2 else (approved_blog_ids if stype == 3 else approved_review_ids)
    if pool: db_sql['smart-live_interaction'].append(f"INSERT INTO `like_record` (`source_type`, `source_id`, `user_id`, `create_time`) VALUES ('{stype}', {random.choice(pool)}, {random.randint(1, NUM_USERS)}, '{random_date()}');")

for _ in range(NUM_STARS):
    stype = random.choice([2, 3, 4]) 
    pool = approved_shop_ids if stype == 2 else (approved_blog_ids if stype == 3 else approved_product_ids)
    if pool: db_sql['smart-live_interaction'].append(f"INSERT INTO `star` (`source_type`, `source_id`, `user_id`, `create_time`) VALUES ('{stype}', {random.choice(pool)}, {random.randint(1, NUM_USERS)}, '{random_date()}');")

# Points & Follows
for i in range(1, NUM_USERS + 1):
    db_sql['smart-live_points'].append(f"INSERT INTO `user_points_wallet` (`user_id`, `balance`, `total_earned`, `consecutive_days`, `version`) VALUES ({i}, {random.randint(100, 5000)}, {random.randint(500, 10000)}, {random.randint(0, 15)}, 0);")
for _ in range(NUM_FOLLOWS):
    u1, u2 = random.sample(range(1, NUM_USERS+1), 2)
    db_sql['smart-live_interaction'].append(f"INSERT INTO `follow` (`user_id`, `source_id`, `source_type`, `create_time`) VALUES ({u1}, {u2}, 1, '{random_date()}');")

# --- Write Files ---
all_sql_lines = ["SET NAMES utf8mb4;", "SET FOREIGN_KEY_CHECKS = 0;", "\n"]
for db, lines in db_sql.items():
    lines.append("\nSET FOREIGN_KEY_CHECKS = 1;")
    with open(f"{OUTPUT_DIR}/{db}.sql", 'w', encoding='utf-8') as f:
        f.write("\n".join(lines))
    all_sql_lines.append(f"-- ##################################################")
    all_sql_lines.append(f"-- Database: {db}")
    all_sql_lines.append(f"-- ##################################################")
    all_sql_lines.extend(lines)
    all_sql_lines.append("\n")

with open(f"{OUTPUT_DIR}/all_test_data.sql", 'w', encoding='utf-8') as f:
    f.write("\n".join(all_sql_lines))

print(f"Generated clean SQL files with multi-shop and correct verification logic in {OUTPUT_DIR}/")
