#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql
echo "begin copy sql "
cp ../sql/ry_20250523.sql ./mysql/db
cp ../sql/ry_config_20250224.sql ./mysql/db

# copy html
echo "begin copy html "
cp -r ../smartLive-ui/dist/** ./nginx/html/dist


# copy jar
echo "begin copy smartLive-gateway "
cp ../smartLive-gateway/target/smartLive-gateway.jar ./smartLive/gateway/jar

echo "begin copy smartLive-auth "
cp ../smartLive-auth/target/smartLive-auth.jar ./smartLive/auth/jar

echo "begin copy smartLive-visual "
cp ../smartLive-visual/smartLive-monitor/target/smartLive-visual-monitor.jar  ./smartLive/visual/monitor/jar

echo "begin copy smartLive-modules-system "
cp ../smartLive-modules/smartLive-system/target/smartLive-modules-system.jar ./smartLive/modules/system/jar

echo "begin copy smartLive-modules-file "
cp ../smartLive-modules/smartLive-file/target/smartLive-modules-file.jar ./smartLive/modules/file/jar

echo "begin copy smartLive-modules-job "
cp ../smartLive-modules/smartLive-job/target/smartLive-modules-job.jar ./smartLive/modules/job/jar

echo "begin copy smartLive-modules-gen "
cp ../smartLive-modules/smartLive-gen/target/smartLive-modules-gen.jar ./smartLive/modules/gen/jar

