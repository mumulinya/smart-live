#!/bin/sh

# 使用说明
usage() {
 echo "Usage: sh deploy.sh [port|infra|base|heavy|java|modules|stop|rm]"
 echo "  port    : 开放防火墙端口"
 echo "  infra   : 启动中间件与基础设施 (MySQL/Redis/Nacos/RabbitMQ/ES/Milvus/Nginx)"
 echo "  base    : 启动基础中间件 (MySQL, Redis, Nacos, RabbitMQ)"
 echo "  heavy   : 启动重型中间件 (ES, Kibana, MinIO, Milvus, Etcd)"
 echo "  java    : 启动所有 Java 微服务模块、网关与监控"
 echo "  modules : 启动所有 Java 微服务模块、网关与监控"
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
 # XXL-JOB
 firewall-cmd --add-port=9080/tcp --permanent
 # Milvus
 firewall-cmd --add-port=19530/tcp --permanent
 firewall-cmd --add-port=9091/tcp --permanent
 # 监控
 firewall-cmd --add-port=9100/tcp --permanent
 firewall-cmd --add-port=8718/tcp --permanent # Sentinel

 # === 业务模块端口 ===
 firewall-cmd --add-port=9900/tcp --permanent # Auth
 firewall-cmd --add-port=9201/tcp --permanent # User
 firewall-cmd --add-port=9202/tcp --permanent # System
 firewall-cmd --add-port=9203/tcp --permanent # Shop
 firewall-cmd --add-port=9204/tcp --permanent # Search
 firewall-cmd --add-port=9205/tcp --permanent # Order
 firewall-cmd --add-port=9206/tcp --permanent # Product
 firewall-cmd --add-port=9207/tcp --permanent # Interaction
 firewall-cmd --add-port=9208/tcp --permanent # Index
 firewall-cmd --add-port=9209/tcp --permanent # File
 firewall-cmd --add-port=9210/tcp --permanent # Chat
 firewall-cmd --add-port=9211/tcp --permanent # Blog
 firewall-cmd --add-port=9212/tcp --permanent # Audit
 firewall-cmd --add-port=9213/tcp --permanent # AI
 firewall-cmd --add-port=9214/tcp --permanent # IM
 firewall-cmd --add-port=9215/tcp --permanent # Points
 firewall-cmd --add-port=9216/tcp --permanent # Wallet
 firewall-cmd --add-port=8888/tcp --permanent # IM Netty

 service firewalld reload
 echo "端口开放完成！"
}

# 1. 启动轻量级基础环境（必须优先启动）
base(){
 echo "正在启动基础中间件..."
 docker compose -f docker-compose-infra.yml up -d smartLive-mysql smartLive-redis smartLive-nacos smartLive-rabbitmq
 echo "基础中间件启动完毕，请等待 Nacos 完全就绪后再启动模块。"
}

# 2. 启动重型组件（按需启动，吃内存大户）
heavy(){
 echo "正在启动重型中间件 (ES, MinIO, Milvus)..."
 docker compose -f docker-compose-infra.yml up -d smartLive-elasticsearch smartLive-kibana smartLive-minio smartLive-etcd smartLive-milvus
 echo "重型中间件启动完毕。"
}

# 2. 启动全部中间件与基础设施
infra(){
 echo "正在启动中间件与基础设施..."
 docker compose -f docker-compose-infra.yml up -d
 echo "基础设施启动完毕。"
}

# 3. 启动所有业务模块（依赖 base 和 heavy）
modules(){
 echo "正在启动业务微服务..."
 docker compose -f docker-compose-java.yml up -d
 echo "所有模块已发送启动命令！"
}

# 4.启动基础业务模块
baseModules(){
  echo "正在启动业务微服务..."
  docker compose -f docker-compose-java.yml up -d
}

# 关闭所有环境/模块
stop(){
 docker compose -f docker-compose-infra.yml stop
 docker compose -f docker-compose-java.yml stop
}

# 删除所有环境/模块
rm(){
 docker compose -f docker-compose-infra.yml rm
 docker compose -f docker-compose-java.yml rm
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
"infra")
 infra
;;
"java")
 modules
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
