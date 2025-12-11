开发时的完整工作流
第一步：启动 SkyWalking 后端
# 终端1 - 启动 OAP
cd apache-skywalking-apm-bin/bin
./oapService.sh

# 终端2 - 启动 UI
./webappService.sh
第二步：在 IDE 中配置并启动项目
IntelliJ 的 VM options:  -javaagent:D:/smart-live/smart-live-Cloud/skywalking/skywalking-agent/skywalking-agent.jar
IntelliJ 的 环境变量: SW_AGENT_NAME=smar
tLive-follow;SW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800