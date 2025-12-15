#!/bin/sh

# ====================================================
# 函数定义：复制文件并自动创建目标目录
# 参数1: 源文件路径
# 参数2: 目标文件夹路径
# ====================================================
copy_jar() {
    src="$1"
    dest="$2"

    # 1. 检查源文件是否存在
    if [ ! -f "${src}" ]; then
        echo "❌ [ERROR] 文件未找到: ${src}"
        return
    fi

    # 2. 检查目标目录是否存在，不存在则创建
    if [ ! -d "${dest}" ]; then
        mkdir -p "${dest}"
    fi

    # 3. 执行复制
    cp "${src}" "${dest}"
    echo "✅ [OK] 复制成功: ${src} -> ${dest}"
}

# ====================================================
# 1. 复制 SQL 脚本
# ====================================================
echo "=== 开始复制 SQL 脚本 ==="
# 确保目标目录存在
mkdir -p ./mysql/db
# 尝试复制，屏蔽错误信息（以防没有sql文件时报错）
cp ../sql/*.sql ./mysql/db 2>/dev/null || echo "⚠️  提示: ../sql/ 目录下没有找到 .sql 文件"


# ====================================================
# 2. 复制 前端资源
# ====================================================
echo "=== 开始复制 HTML 资源 ==="
if [ -d "../smartLive-ui/dist" ]; then
    mkdir -p ./nginx/html/dist
    cp -r ../smartLive-ui/dist/* ./nginx/html/dist
    echo "✅ [OK] 前端ui资源复制完成"
else
    echo "⚠️  跳过: 未找到前端 dist 目录 (../smartLive-ui/dist)"
fi
if [ -d "../smartLive-html/html" ]; then
    mkdir -p ./nginx/html/html
    cp -r ../smartLive-html/html/* ./nginx/html/html
    echo "✅ [OK] 前端html资源复制完成"
else
    echo "⚠️  跳过: 未找到前端 html 目录 (../smartLive-html/html)"
fi

# ====================================================
# 3. 复制 后端 JAR 包
# ====================================================
echo "=== 开始复制 JAR 包 ==="

# --- 基础服务 ---
copy_jar "../smartLive-gateway/target/smartLive-gateway.jar" "./smartLive/gateway/jar"
copy_jar "../smartLive-auth/target/smartLive-auth.jar" "./smartLive/auth/jar"
copy_jar "../smartLive-visual/smartLive-monitor/target/smartLive-visual-monitor.jar" "./smartLive/visual/monitor/jar"

# --- 核心业务模块 (已根据你的 docker-compose.yml 补全) ---

# 系统与用户
copy_jar "../smartLive-modules/smartLive-system/target/smartLive-modules-system.jar" "./smartLive/modules/system/jar"
copy_jar "../smartLive-modules/smartLive-user/target/smartLive-modules-user.jar" "./smartLive/modules/user/jar"

# 业务功能
copy_jar "../smartLive-modules/smartLive-file/target/smartLive-modules-file.jar" "./smartLive/modules/file/jar"
copy_jar "../smartLive-modules/smartLive-job/target/smartLive-modules-job.jar" "./smartLive/modules/job/jar"
copy_jar "../smartLive-modules/smartLive-gen/target/smartLive-modules-gen.jar" "./smartLive/modules/gen/jar"
copy_jar "../smartLive-modules/smartLive-ai/target/smartLive-modules-ai.jar" "./smartLive/modules/ai/jar"
copy_jar "../smartLive-modules/smartLive-blog/target/smartLive-modules-blog.jar" "./smartLive/modules/blog/jar"
copy_jar "../smartLive-modules/smartLive-chat/target/smartLive-modules-chat.jar" "./smartLive/modules/chat/jar"
copy_jar "../smartLive-modules/smartLive-comment/target/smartLive-modules-comment.jar" "./smartLive/modules/comment/jar"
copy_jar "../smartLive-modules/smartLive-follow/target/smartLive-modules-follow.jar" "./smartLive/modules/follow/jar"
copy_jar "../smartLive-modules/smartLive-index/target/smartLive-modules-index.jar" "./smartLive/modules/index/jar"
copy_jar "../smartLive-modules/smartLive-map/target/smartLive-modules-map.jar" "./smartLive/modules/map/jar"
copy_jar "../smartLive-modules/smartLive-marketing/target/smartLive-modules-marketing.jar" "./smartLive/modules/marketing/jar"
copy_jar "../smartLive-modules/smartLive-order/target/smartLive-modules-order.jar" "./smartLive/modules/order/jar"
copy_jar "../smartLive-modules/smartLive-search/target/smartLive-modules-search.jar" "./smartLive/modules/search/jar"
copy_jar "../smartLive-modules/smartLive-shop/target/smartLive-modules-shop.jar" "./smartLive/modules/shop/jar"

echo "🎉 所有复制任务执行完毕！"