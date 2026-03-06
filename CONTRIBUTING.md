# Contributing

感谢你愿意为 SmartLive 贡献代码或文档。

## 提交流程

1. Fork 本仓库并基于最新代码创建分支。
2. 在本地完成开发、联调和必要验证。
3. 自查通过后提交 Pull Request，并写清楚改动背景、影响范围和验证方式。

## 分支与提交信息

- 分支名建议使用 feature/*、fix/*、docs/* 这类清晰前缀。
- 提交信息建议保持 type(scope): subject。
- 常用 type：feat、fix、docs、refactor、test、chore。

## 文档与编码约束

- 文档、配置和源码统一使用 UTF-8 编码。
- 修改 README、docs/ 或 YAML 配置时，不要引入乱码、控制字符或混乱的行尾格式。
- 涉及端口、模块名、依赖关系、Nacos dataId 变更时，请同步更新 README.md 与 docs/OPEN_SOURCE.md。

## 提交前自查

- 我确认没有提交真实 API key、密码、令牌或生产地址。
- 我确认新增文档能够被正常渲染，中文没有乱码。
- 我确认变更说明里写明了影响模块、是否需要改 Nacos、是否需要导入新 SQL。
- 我确认没有把无关生成物、日志和本地缓存一并提交。

## Pull Request 建议内容

- 改动目的
- 影响模块
- 配置变更
- 数据库或 Nacos 变更
- 验证方式
- 风险与回滚点

## 安全相关变更

如果改动涉及鉴权、支付、消息投递、文件上传、AI 密钥或外部回调，请同时阅读 SECURITY.md，并在 PR 描述中明确说明风险边界。