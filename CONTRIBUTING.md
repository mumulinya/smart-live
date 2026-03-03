# Contributing to SmartLive

感谢你愿意为 **SmartLive（智评生活）** 做出贡献！

本项目欢迎以下类型的贡献：
- Bug 反馈与修复
- 新功能建议与实现
- 文档完善
- 性能优化与测试用例补充

## Before You Start

1. 先阅读项目根目录 `README.md`，了解架构、模块与启动方式。
2. 建议先创建 Issue 讨论较大改动，避免重复工作。
3. 确保你的改动与现有模块职责保持一致（gateway/common/modules/api 等）。

## Development Setup

```bash
# 安装依赖并编译
mvn clean install

# 运行测试
mvn test
```

如需单模块运行，可参考 `README.md` 中的快速开始章节。

## Pull Request Checklist

提交 PR 前请确认：

- [ ] 代码可编译通过
- [ ] 相关测试已通过（至少包含改动模块测试）
- [ ] 新增/变更行为已补充文档（如接口、配置项、脚本）
- [ ] 提交信息符合约定格式

## Commit Convention

建议使用：`type(scope): subject`

常见 `type`：
- `feat`：新功能
- `fix`：修复问题
- `docs`：文档改动
- `refactor`：重构
- `test`：测试
- `chore`：构建与工具

示例：

```text
docs(readme): add links to community health files
```

## Coding Guidelines

- 遵循现有 Java/Spring 编码风格与分层结构。
- 避免引入无必要的大规模重构。
- 保持接口兼容性；若存在 breaking change，请在 PR 中明确说明。

## Reporting Security Issues

请不要在公开 Issue 中直接披露安全漏洞。

请参考：[`SECURITY.md`](./SECURITY.md)

---

再次感谢你的时间与贡献 ❤️
