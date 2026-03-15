import random
import datetime
import os

# --- Configuration ---
NUM_USERS = 200
NUM_SHOPS = 500
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

# --- Random Text Pools ---
PRODUCT_SUBTITLES = ['优质商品', '火爆热销', '新品上市', '限时特惠', '店长推荐', '超值划算', '人气爆款', '精选好物', '品质保证', '镇店之宝']
PRODUCT_NAMES = ['双人豪华甄选套餐', '招牌浓郁骨汤拉面', '经典美式黑咖啡', '特级安格斯肉眼牛排', '夏日缤纷水果茶', '至尊海鲜自助餐', '单人全身体验SPA', '人气网红奶茶', '家庭欢聚多人餐', '招牌秘制烤鱼', '精选生鲜大礼包', '法式焦糖烤布蕾', '深海三文鱼刺身', '滋补养生炖汤', '芝士爆浆披萨', '手捣香水柠檬茶', '金奖红烧肉', '脆皮烤鸭半套', '沉浸式密室逃脱单人券', '高级定制剪发设计']

USER_NAMES_P1 = ['快乐的', '奔跑的', '安静的', '无敌的', '神秘的', '可爱的', '帅气的', '温柔的', '发财的', '幸运的', '干饭的', '迷茫的', '佛系的']
USER_NAMES_P2 = ['二哈', '橘猫', '程序猿', '打工人', '干饭人', '夜猫子', '小仙女', '老司机', '吃货', '胖丁', '布偶猫', '小黄人', '闪电侠']

BLOG_TITLES = ['探店达人这波没白来', '周末好去处分享', '绝绝子推荐', '发现隐藏的宝藏店铺', '打卡圣地不容错过', '吃货必看指南', '这店绝了绝了', '不能错过的美食', '氛围感拉满的一天', '神仙店铺种草']
BLOG_CONTENTS = [
    '这家店真的很惊艳！强烈推荐给大家。',
    '环境超级好，味道也在线，下次还来！',
    '终于拔草了，没有踩雷，放心冲。',
    '服务态度没话说，菜品颜值巨高，爱了爱了。',
    '盲点都不出错的一家店，大家一定要来试试。',
    '性价比很高，学生党打工人首选。',
    '分量超足，老板太良心了吧！',
    '随便拍都很出片，朋友圈点赞收割机。',
    '宝藏小店被我发现了，就是排队太久。',
    '本地人从小吃到大的味道，绝对正宗。'
]

REVIEW_CONTENTS = [
    '味道很棒，服务热情！',
    '菜品新鲜，体验特别好，给个好评。',
    '一般般吧，没有想象中好吃，但也还行。',
    '上菜速度很快，环境也很干净。',
    '全家都觉得好吃，推荐！',
    '稍微有点贵，不过一分钱一分货。',
    '服务员小哥哥小姐姐态度特别好，很贴心。',
    '老顾客了，一如既往的稳定输出。',
    '非常惊艳，创意十足，值得二刷！',
    '无可挑剔，全五星好评！'
]

# --- Helper Functions ---
def get_now():
    return datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')

def random_date(start_year=2026, start_month=1, start_day=15, end_year=2026, end_month=3, end_day=15):
    start = datetime.datetime(start_year, start_month, start_day)
    end = datetime.datetime(end_year, end_month, end_day)
    return (start + datetime.timedelta(seconds=random.randint(0, int((end - start).total_seconds())))).strftime('%Y-%m-%d %H:%M:%S')

def get_biased_shop_id():
    # 50% chance to be in the top 50 shops, 50% chance for the remaining 450 shops
    return random.randint(1, 50) if random.random() < 0.5 else random.randint(51, NUM_SHOPS)

def get_audit_status():
    """
    Returns (status, audit_status)
    70% Approved: (1, 1)
    20% Pending: (0, 0)
    10% Rejected: (2, 2)
    """
    r = random.random()
    if r < 0.7:
        return 1, 1
    elif r < 0.9:
        return 0, 0
    else:
        return 2, 2

# --- Data Generation Stacks ---
db_sql = {
    'smart-live_user': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_user`;", "TRUNCATE TABLE `user`;", "TRUNCATE TABLE `user_info`;"],
    'smart-live_shop': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_shop`;", "TRUNCATE TABLE `shop_type`;", "TRUNCATE TABLE `shop`;"],
    'smart-live_system': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_system`;", "DELETE FROM `sys_user` WHERE user_id != 1;", "DELETE FROM `sys_user_role` WHERE user_id != 1;", "DELETE FROM `sys_user_shop` WHERE user_id != 1;"],
    'smart-live_product': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_product`;", "TRUNCATE TABLE `product`;"],
    'smart-live_blog': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_blog`;", "TRUNCATE TABLE `blog`;"],
    'smart-live_order': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_order`;", "TRUNCATE TABLE `order`;"],
    'smart-live_interaction': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_interaction`;", "TRUNCATE TABLE `review`;", "TRUNCATE TABLE `follow`;", "TRUNCATE TABLE `like_record`;", "TRUNCATE TABLE `star`;", "TRUNCATE TABLE `comment`;"],
    'smart-live_points': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_points`;", "TRUNCATE TABLE `user_points_wallet`;"],
    'smart-live_audit': ["SET FOREIGN_KEY_CHECKS = 0;", "USE `smart-live_audit`;", "TRUNCATE TABLE `audit_task`;"]
}

# 1. Users (App Users)
BCRYPT_PASSWORD = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'
user_ids = []
for i in range(1, NUM_USERS + 1):
    phone = f"13{random.randint(0, 9):01d}{random.randint(10000000, 99999999)}"
    nick = random.choice(USER_NAMES_P1) + random.choice(USER_NAMES_P2) + str(random.randint(10, 99))
    user_ids.append(i)
    db_sql['smart-live_user'].append(f"INSERT INTO `user` (`id`, `phone`, `password`, `nick_name`, `icon`) VALUES ({i}, '{phone}', '{BCRYPT_PASSWORD}', '{nick}', 'https://api.dicebear.com/7.x/avataaars/svg?seed={nick}');")
    city = random.choice(['上海', '北京', '深圳', '广州', '杭州', '成都', '佛山', '东莞', '中山'])
    db_sql['smart-live_user'].append(f"INSERT INTO `user_info` (`user_id`, `city`, `introduce`, `fans`, `followee`, `liked`, `gender`, `birthday`, `credits`, `level`, `audit_status`) VALUES ({i}, '{city}', 'Hello, I am {nick}', {random.randint(0, 500)}, {random.randint(0, 200)}, {random.randint(0, 1000)}, {random.randint(0, 1)}, '1990-01-01', {random.randint(0, 5000)}, {random.randint(0, 9)}, 1);")

def add_audit_task(biz_id, biz_type, submitter_id, status, content_name):
    """
    biz_type: 2-Shop, 3-Blog, 4-Voucher (Prod Cat 1), 5-Review, 6-GroupBuy (Prod Cat 2)
    """
    reason = "'内容违规'" if status == 2 else "NULL"
    content = f'{{"name": "{content_name}", "note": "Generated by script"}}'
    db_sql['smart-live_audit'].append(f"INSERT INTO `audit_task` (`biz_id`, `biz_type`, `submitter_id`, `status`, `reason`, `audit_content`, `create_time`) VALUES ({biz_id}, {biz_type}, {submitter_id}, {status}, {reason}, '{content}', '{get_now()}');")

# First, assign shops to products to ensure relational integrity
product_shop_mapping = {}
for i in range(1, NUM_PRODUCTS + 1):
    product_shop_mapping[i] = get_biased_shop_id()

# --- First Pass: Generate Orders & Reviews in Memory ---
orders_data = [] # (i, u_id, prod_id, shop_id, order_status, use_dt, amount, use_time, create_time_str)
product_sold = {i: 0 for i in range(1, NUM_PRODUCTS + 1)}
shop_sold = {i: 0 for i in range(1, NUM_SHOPS + 1)}

for i in range(1, NUM_ORDERS + 1):
    u_id = random.randint(1, NUM_USERS)
    prod_id = random.randint(1, NUM_PRODUCTS)
    shop_id = product_shop_mapping[prod_id] # Fetch the correct shop for the chosen product
    order_status = random.choice([2, 3]) 
    amount = random.randint(1, 3)
    create_time_str = random_date()
    use_time = "NULL"
    use_dt = None
    
    # 记录销量（付款即算销量）
    product_sold[prod_id] += amount
    shop_sold[shop_id] += amount
    
    if order_status == 3:
        create_dt = datetime.datetime.strptime(create_time_str, '%Y-%m-%d %H:%M:%S')
        end_dt = datetime.datetime(2026, 3, 15)
        if create_dt < end_dt:
            max_sec = int((end_dt - create_dt).total_seconds())
            if max_sec <= 0:
                use_dt = create_dt + datetime.timedelta(seconds=1)
            elif max_sec < 3600:
                use_dt = create_dt + datetime.timedelta(seconds=random.randint(1, max_sec))
            else:
                use_dt = create_dt + datetime.timedelta(seconds=random.randint(3600, max_sec))
            use_time = f"'{use_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
        else:
            use_dt = create_dt
            use_time = f"'{create_time_str}'"
            
    orders_data.append((i, u_id, prod_id, shop_id, order_status, use_dt, amount, use_time, create_time_str))

# Create tracking for Review Ratings
shop_reviews_count = {i: 0 for i in range(1, NUM_SHOPS + 1)}
shop_reviews_score_sum = {i: 0 for i in range(1, NUM_SHOPS + 1)}

reviews_data = []
completed_orders = [o for o in orders_data if o[4] == 3 and o[5] is not None]

for i in range(1, NUM_REVIEWS + 1):
    review_type = random.choice(['shop', 'product'])
    
    if review_type == 'product' and completed_orders:
        order = random.choice(completed_orders)
        order_id = order[0]
        u_id = order[1]
        prod_id = order[2]
        s_id = order[3]
        use_dt = order[5]
        source_type = 4  # PRODUCT_CODE
        source_id = prod_id
        
        max_sec = int((datetime.datetime(2026, 3, 15) - use_dt).total_seconds())
        if max_sec > 0:
            review_time = (use_dt + datetime.timedelta(seconds=random.randint(1, max_sec))).strftime('%Y-%m-%d %H:%M:%S')
        else:
            review_time = use_dt.strftime('%Y-%m-%d %H:%M:%S')
    else:
        # Shop review
        order_id = "NULL"
        u_id = random.randint(1, NUM_USERS)
        s_id = get_biased_shop_id()
        source_type = 2
        source_id = s_id
        review_time = random_date()
        
    status, audit_status = get_audit_status()
    content = random.choice(REVIEW_CONTENTS)
    score = random.randint(1, 5)
    taste_score = min(5, max(1, score + random.choice([-1, 0, 1])))
    env_score = min(5, max(1, score + random.choice([-1, 0, 1])))
    service_score = min(5, max(1, score + random.choice([-1, 0, 1])))
    
    # Track shop total score for average calculation later
    shop_reviews_count[s_id] += 1
    shop_reviews_score_sum[s_id] += score
    
    reviews_data.append((i, u_id, s_id, order_id, source_type, source_id, content, score, taste_score, env_score, service_score, status, audit_status, review_time))

# 2. Shops
shop_types = ["精致西餐", "地道中餐", "甜品饮品", "休闲娱乐", "酒店住宿", "美容美发", "运动健身", "火锅烧烤", "小吃快餐", "便利超市"]
for i, name in enumerate(shop_types, 1):
    db_sql['smart-live_shop'].append(f"INSERT INTO `shop_type` (`id`, `name`, `icon`, `sort`) VALUES ({i}, '{name}', 'type_{i}.png', {i});")

foshan_areas = ['禅城区', '南海区', '顺德区', '三水区', '高明区']
foshan_streets = ['祖庙', '桂城', '大良', '西南', '荷城', '石湾', '狮山', '容桂', '北滘', '乐从', '九江', '西樵', '丹灶', '里水']
for i in range(1, NUM_SHOPS + 1):
    t_id = random.randint(1, len(shop_types))
    area = random.choice(foshan_areas)
    street = random.choice(foshan_streets)
    name_suffix = random.choice(['大酒店', '饭店', '火锅店', '私房菜', '饮品店', '健身中心', '造型屋', '超市', '快餐店'])
    name = f"{street}{name_suffix}"
    x = 113.0 + random.random() * 0.3
    y = 23.0 + random.random() * 0.2
    status, audit_status = get_audit_status()
    # Calculate actual average score, defaults to 50 if no reviews (5.0 * 10)
    avg_score = 50
    if shop_reviews_count[i] > 0:
        avg_score = int((shop_reviews_score_sum[i] / shop_reviews_count[i]) * 10)
        
    db_sql['smart-live_shop'].append(f"INSERT INTO `shop` (`id`, `name`, `type_id`, `images`, `area`, `address`, `x`, `y`, `avg_price`, `score`, `reviews`, `sold`, `open_hours`, `status`, `audit_status`) VALUES ({i}, '{name}', {t_id}, 'https://picsum.photos/seed/shop{i}/400/300', '{area}', '广东省佛山市{area}{street}街道{random.randint(1, 999)}号', {x:.6f}, {y:.6f}, {random.randint(20, 500)}, {avg_score}, {shop_reviews_count[i]}, {shop_sold[i]}, '09:00-22:00', {status}, {audit_status});")
    # Audit Task for Shop (biz_type 2)
    add_audit_task(i, 2, 1, status, name)

# 3. System Users & Merchant Linkage
MERCHANT_ROLE_ID = 100
for i in range(1, NUM_SHOPS + 1):
    sys_user_id = 1000 + i
    user_name = f"merchant_{i}"
    db_sql['smart-live_system'].append(f"INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `password`, `status`, `create_time`) VALUES ({sys_user_id}, 100, '{user_name}', '商家店长', '{BCRYPT_PASSWORD}', '0', '{get_now()}');")
    db_sql['smart-live_system'].append(f"INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES ({sys_user_id}, {MERCHANT_ROLE_ID});")
    db_sql['smart-live_system'].append(f"INSERT INTO `sys_user_shop` (`user_id`, `shop_id`, `create_time`) VALUES ({sys_user_id}, {i}, '{get_now()}');")

# 4. Products
for i in range(1, NUM_PRODUCTS + 1):
    s_id = product_shop_mapping[i]
    price = random.randint(10, 1000)
    category = random.randint(1, 2)
    status, audit_status = get_audit_status()
    sub_title = random.choice(PRODUCT_SUBTITLES)
    prod_name = random.choice(PRODUCT_NAMES)
    
    # 30% chance for seckill product
    activity_type = 1 if random.random() < 0.3 else 0
    begin_time = "NULL"
    end_time = "NULL"
    
    if activity_type == 1:
        # Seckill time generation
        base_time = datetime.datetime.strptime(random_date(), '%Y-%m-%d %H:%M:%S')
        begin_dt = base_time.replace(hour=10, minute=0, second=0)
        end_dt = begin_dt + datetime.timedelta(days=random.randint(1, 7))
        begin_time = f"'{begin_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
        end_time = f"'{end_dt.strftime('%Y-%m-%d %H:%M:%S')}'"

    # Validity period generation
    validity_type = random.choice([1, 2])
    valid_days = "NULL"
    use_start_time = "NULL"
    use_end_time = "NULL"
    
    if validity_type == 1:
        # Fixed duration
        start_dt = datetime.datetime.strptime(random_date(), '%Y-%m-%d %H:%M:%S')
        end_dt = start_dt + datetime.timedelta(days=random.randint(15, 90))
        use_start_time = f"'{start_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
        use_end_time = f"'{end_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
    else:
        # Dynamic duration
        valid_days = random.randint(7, 365)

    prod_sold = product_sold[i]
    db_sql['smart-live_product'].append(f"INSERT INTO `product` (`id`, `shop_id`, `name`, `sub_title`, `price`, `original_price`, `category`, `activity_type`, `status`, `audit_status`, `stock`, `sold`, `validity_type`, `valid_days`, `use_start_time`, `use_end_time`, `begin_time`, `end_time`, `cover_img`, `create_time`) VALUES ({i}, '{s_id}', '{prod_name}', '{sub_title}', {price}.00, {price+20}.00, {category}, {activity_type}, {status}, {audit_status}, {max(10, prod_sold + random.randint(10, 500))}, {prod_sold}, {validity_type}, {valid_days}, {use_start_time}, {use_end_time}, {begin_time}, {end_time}, 'https://picsum.photos/seed/prod{i}/400/400', '{random_date()}');")
    # Audit Task (4-Voucher, 6-GroupBuy)
    biz_type = 4 if category == 1 else 6
    add_audit_task(i, biz_type, 1000 + s_id, status, prod_name)

# 5. Blogs
for i in range(1, NUM_BLOGS + 1):
    u_id = random.randint(1, NUM_USERS)
    s_id = get_biased_shop_id()
    status, audit_status = get_audit_status()
    title = random.choice(BLOG_TITLES)
    content = random.choice(BLOG_CONTENTS)
    db_sql['smart-live_blog'].append(f"INSERT INTO `blog` (`id`, `shop_id`, `type_id`, `user_id`, `title`, `images`, `content`, `liked`, `stared`, `comments`, `status`, `audit_status`, `create_time`) VALUES ({i}, {s_id}, 1, {u_id}, '{title}', 'https://picsum.photos/seed/blog{i}/600/400', '{content}', {random.randint(0, 1000)}, {random.randint(0, 200)}, {random.randint(0, 50)}, {status}, {audit_status}, '{random_date()}');")
    # Audit Task (3-Blog)
    add_audit_task(i, 3, u_id, status, title)

# 6. Push Orders to SQL
for o in orders_data:
    i, u_id, prod_id, shop_id, order_status, use_dt, amount, use_time, create_time_str = o
    db_sql['smart-live_order'].append(f"INSERT INTO `order` (`id`, `user_id`, `source_type`, `source_id`, `pay_amount`, `amount`, `status`, `shop_id`, `use_time`, `create_time`) VALUES ({i}, {u_id}, 1, {prod_id}, {random.randint(1000, 50000)}, {amount}, {order_status}, '{shop_id}', {use_time}, '{create_time_str}');")

# 7. Push Interaction (Reviews, Follows, Likes)
for rev in reviews_data:
    i, u_id, s_id, order_id, source_type, source_id, content, score, taste_score, env_score, service_score, status, audit_status, review_time = rev
    db_sql['smart-live_interaction'].append(f"INSERT INTO `review` (`id`, `user_id`, `shop_id`, `order_id`, `source_type`, `source_id`, `content`, `score`, `taste_score`, `env_score`, `service_score`, `status`, `audit_status`, `create_time`) VALUES ({i}, {u_id}, {s_id}, {order_id}, {source_type}, {source_id}, '{content}', {score}, {taste_score}, {env_score}, {service_score}, {status}, {audit_status}, '{review_time}');")
    add_audit_task(i, 5, u_id, status, f"Review_{i}")

for i in range(1, NUM_FOLLOWS + 1):
    u1, u2 = random.sample(range(1, NUM_USERS+1), 2)
    # Follow a user: source_type = 1 (USER_CODE)
    db_sql['smart-live_interaction'].append(f"INSERT INTO `follow` (`user_id`, `source_id`, `source_type`, `create_time`) VALUES ({u1}, {u2}, 1, '{random_date()}');")

for i in range(1, NUM_LIKES + 1):
    # Likes for: 2(Shop), 3(Blog), 7(Review)
    stype = random.choice([2, 3, 7])
    if stype == 2:
        sid = get_biased_shop_id()
    elif stype == 3:
        sid = random.randint(1, NUM_BLOGS)
    else:
        sid = random.randint(1, NUM_REVIEWS)
    db_sql['smart-live_interaction'].append(f"INSERT INTO `like_record` (`source_type`, `source_id`, `user_id`, `create_time`) VALUES ('{stype}', {sid}, {random.randint(1, NUM_USERS)}, '{random_date()}');")

for i in range(1, NUM_STARS + 1):
    # Stars (Collections) for: 2(Shop), 3(Blog), 4(Product)
    stype = random.choice([2, 3, 4])
    if stype == 2:
        sid = get_biased_shop_id()
    elif stype == 3:
        sid = random.randint(1, NUM_BLOGS)
    else:
        sid = random.randint(1, NUM_PRODUCTS)
    db_sql['smart-live_interaction'].append(f"INSERT INTO `star` (`source_type`, `source_id`, `user_id`, `create_time`) VALUES ('{stype}', {sid}, {random.randint(1, NUM_USERS)}, '{random_date()}');")

for i in range(1, NUM_COMMENTS + 1):
    # Comments ONLY for: 3(Blog), 7(Review)
    stype = random.choice([3, 7]) 
    if stype == 3:
        sid = random.randint(1, NUM_BLOGS) 
    else:
        sid = random.randint(1, NUM_REVIEWS)
        
    content = random.choice(REVIEW_CONTENTS)
    status, audit_status = get_audit_status()
    db_sql['smart-live_interaction'].append(f"INSERT INTO `comment` (`user_id`, `source_type`, `source_id`, `parent_id`, `content`, `liked`, `reply_count`, `status`, `audit_status`, `create_time`) VALUES ({random.randint(1, NUM_USERS)}, {stype}, {sid}, 0, '{content}', {random.randint(0, 50)}, {random.randint(0, 10)}, {status}, {audit_status}, '{random_date()}');")

# 8. Points
for i in range(1, NUM_USERS + 1):
    db_sql['smart-live_points'].append(f"INSERT INTO `user_points_wallet` (`user_id`, `balance`, `total_earned`, `consecutive_days`, `version`) VALUES ({i}, {random.randint(100, 5000)}, {random.randint(500, 10000)}, {random.randint(0, 15)}, 0);")

# --- Write Files ---
for db, lines in db_sql.items():
    lines.append("\nSET FOREIGN_KEY_CHECKS = 1;")
    filename = f"{OUTPUT_DIR}/{db}.sql"
    with open(filename, 'w', encoding='utf-8') as f:
        f.write("\n".join(lines))

print(f"Generated {len(db_sql)} SQL files in {OUTPUT_DIR}/ (Total Records Expanded with Audit Tasks and TRUNCATE statements)")
