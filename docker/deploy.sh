#!/bin/sh

# 使用说明
usage() {
 echo "Usage: sh deploy.sh [port|base|heavy|modules|stop|rm]"
 echo "  port    : 开放防火墙端口"
 echo "  base    : 启动基础中间件 (MySQL, Redis, Nacos, RabbitMQ)"
 echo "  heavy   : 启动重型中间件 (ES, Kibana, MinIO, Milvus, Etcd)"
 echo "  modules : 启动所有 Java 微服务模块、网关和 Nginx"
 echo "  stop    : 停止所有服务"
 echo "  rm      : 删除所有容器"
 exit 1
}

# 开启所需端口 (已根据 YAML 补全)
port(){
 # 基础 + 网关
 firewall-cmd --add-port=80/tcp --permanent
 firewall-cmd --add-port=8080/tcp --permanent
 # Nacos
 firewall-cmd --add-port=8848/tcp --permanent
 firewall-cmd --add-port=9848/tcp --permanent
 firewall-cmd --add-port=9849/tcp --permanent
 # DB & Cache
 firewall-cmd --add-port=6379/tcp --permanent
 firewall-cmd --add-port=3306/tcp --permanent
 # RabbitMQ (关键补全)
 firewall-cmd --add-port=5672/tcp --permanent
 firewall-cmd --add-port=15672/tcp --permanent
 # ES & Kibana
 firewall-cmd --add-port=9200/tcp --permanent
 firewall-cmd --add-port=9300/tcp --permanent
 firewall-cmd --add-port=5601/tcp --permanent
 # MinIO
 firewall-cmd --add-port=9000/tcp --permanent
 firewall-cmd --add-port=9001/tcp --permanent
 # Milvus
 firewall-cmd --add-port=19530/tcp --permanent
 firewall-cmd --add-port=9091/tcp --permanent
 # 监控
 firewall-cmd --add-port=9100/tcp --permanent
 firewall-cmd --add-port=8718/tcp --permanent # Sentinel

 # === 业务模块端口 (已补全) ===
 firewall-cmd --add-port=9900/tcp --permanent # Auth
 firewall-cmd --add-port=9201/tcp --permanent # User
 firewall-cmd --add-port=9202/tcp --permanent # System
 firewall-cmd --add-port=9203/tcp --permanent # Shop
 firewall-cmd --add-port=9204/tcp --permanent # Search
 firewall-cmd --add-port=9205/tcp --permanent # Order
 firewall-cmd --add-port=9206/tcp --permanent # Marketing
 firewall-cmd --add-port=9207/tcp --permanent # Map
 firewall-cmd --add-port=9208/tcp --permanent # Job
 firewall-cmd --add-port=9209/tcp --permanent # Gen
 firewall-cmd --add-port=9210/tcp --permanent # Follow
 firewall-cmd --add-port=9211/tcp --permanent # File
 firewall-cmd --add-port=9212/tcp --permanent # Comment
 firewall-cmd --add-port=9213/tcp --permanent # Chat
 firewall-cmd --add-port=9214/tcp --permanent # Blog
 firewall-cmd --add-port=9215/tcp --permanent # Index
 firewall-cmd --add-port=9216/tcp --permanent # AI (之前改成这个了)

 service firewalld reload
 echo "端口开放完成！"
}

# 1. 启动轻量级基础环境（必须优先启动）
base(){
 echo "正在启动基础中间件..."
 docker-compose up -d smartLive-mysql smartLive-redis smartLive-nacos smartLive-rabbitmq
 echo "基础中间件启动完毕，请等待 Nacos 完全就绪后再启动模块。"
}

# 2. 启动重型组件（按需启动，吃内存大户）
heavy(){
 echo "正在启动重型中间件 (ES, MinIO, Milvus)..."
 docker-compose up -d smartLive-elasticsearch smartLive-kibana smartLive-minio smartLive-etcd smartLive-milvus
 echo "重型中间件启动完毕。"
}

# 3. 启动所有业务模块（依赖 base 和 heavy）
modules(){
 echo "正在启动业务微服务..."
 # 先启动核心：网关、认证、系统
 docker-compose up -d smartLive-gateway smartLive-auth smartLive-modules-system

 # 稍微停顿一下，防止并发太高卡死数据库
 sleep 5

 # 启动其他所有模块 (使用通配符不太好控制顺序，建议明确列出或直接 up -d)
 # 这里列出所有 smartLive-modules- 开头的服务
 docker-compose up -d smartLive-modules-user smartLive-modules-shop smartLive-modules-search smartLive-modules-order \
                      smartLive-modules-marketing smartLive-modules-map smartLive-modules-job smartLive-modules-gen \
                      smartLive-modules-follow smartLive-modules-file smartLive-modules-comment smartLive-modules-chat \
                      smartLive-modules-blog smartLive-modules-index smartLive-modules-ai

 # 最后启动前端和监控
 docker-compose up -d smartLive-nginx smartLive-sentinel smartLive-visual-monitor
 echo "所有模块已发送启动命令！"
}

# 4.启动基础业务模块
baseModules(){
  echo "正在启动业务微服务..."
   # 先启动核心：网关、认证、系统
  docker-compose up -d smartLive-gateway smartLive-auth smartLive-modules-system
  sleep 5

  docker-compose up -d  smartLive-modules-user smartLive-modules-shop smartLive-modules-search \
                        smartLive-modules-marketing smartLive-modules-map  \
                         smartLive-modules-chat  smartLive-modules-blog    \
  docker-compose up -d smartLive-nginx
}

# 关闭所有环境/模块
stop(){
 docker-compose stop
}

# 删除所有环境/模块
rm(){
 docker-compose rm
}

# 根据输入参数选择执行
case "$1" in
"port")
 port
;;
"base")
 base
;;
"heavy")
 heavy
;;
"modules")
 modules
;;
"stop")
 stop
;;
"rm")
 rm
;;
*)
 usage
;;
esac