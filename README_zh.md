# OpenRocky Android

**语音优先的 Android AI Agent — 不是聊天壳子。**

[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen)](LICENSE)
[![Android](https://img.shields.io/badge/Android-Internal%20Testing-3DDC84?logo=android&logoColor=white)](https://play.google.com/apps/testing/com.xnu.rocky)
[![Website](https://img.shields.io/badge/Website-openrocky.org-blue)](https://openrocky.org/)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/SvvsaDA4nE)
[![Telegram](https://img.shields.io/badge/Telegram-@openrocky-26A5E4?logo=telegram&logoColor=white)](https://t.me/openrocky)

> [English](README.md) | 中文

Rocky 将语音交互、任务执行、系统桥接和结果回顾组织成一个原生的 Android Agent 体验。

## 截图

<table>
  <tr>
    <td><img src="screenshots/screenshot_android1.png" width="240" alt="天气查询"></td>
    <td><img src="screenshots/screenshot_android2.png" width="240" alt="能力列表"></td>
    <td><img src="screenshots/screenshot_android3.png" width="240" alt="设置"></td>
  </tr>
</table>

## 特性

- **语音优先** — 语音是主要交互方式，而非聊天列表
- **30+ 原生 Android 工具** — 通讯录、日历、天气、定位、提醒事项、闹钟、相机、浏览器、加密等
- **多 AI 服务商** — OpenAI、Anthropic、Gemini、Azure、Groq、xAI、OpenRouter、DeepSeek、豆包、aiProxy
- **实时语音** — 通过 OpenAI、Gemini、豆包的实时 API 进行语音对话
- **自定义技能** — 内置技能 + 用户可导入的自定义技能
- **本地执行** — 通过 Chaquopy 在设备上运行 Shell 和 Python 3.11
- **角色与灵魂** — 可配置的 AI 人格和声音
- **记忆** — 跨会话的持久化上下文

## 架构

```
语音输入 → 语音引擎 → AI 服务商 → ROS 运行时 → 执行层 → 结果 → UI + 语音
```

Rocky 围绕 **ROS (Rocky OS) 运行时** 构建，管理会话、工具、技能、语音桥接、角色和记忆。执行分为三层：

| 层级 | 说明 |
|---|---|
| **Android 原生桥接** | Kotlin 代码调用系统 API（通讯录、日历、定位等） |
| **AI 工具层** | 通过 AI 服务商 API 分发的操作 |
| **本地执行** | 通过 Chaquopy 在沙盒中运行 Shell/Python |

服务商采用三层抽象：**服务商 (Provider)** → **账户 (Account)** → **模型 (Model)**。

## 快速开始

```bash
git clone https://github.com/openrocky/openrocky_android.git
cd openrocky_android
./gradlew assembleStandardDebug
```

完整的构建说明、项目结构和代码风格，请参见 **[开发指南](DEVELOP_zh.md)**。

## 参与贡献

欢迎贡献！请随时提交 [Pull Request](https://github.com/openrocky/openrocky_android/pulls)。

- [报告 Bug](https://github.com/openrocky/openrocky_android/issues/new)
- [功能建议](https://github.com/openrocky/openrocky_android/issues/new)

## 社区

- [官网](https://openrocky.org/) — 项目主页
- [Discord](https://discord.gg/SvvsaDA4nE) — 社区交流
- [Telegram](https://t.me/openrocky) — @openrocky
- [iOS 版本](https://github.com/openrocky/openrocky) — OpenRocky for iOS

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=openrocky/openrocky_android&type=Date)](https://star-history.com/#openrocky/openrocky_android&Date)

## 致谢

由 [@everettjf](https://github.com/everettjf) 借助 [Claude Code](https://claude.ai/code) 和 [Codex](https://openai.com/codex) 构建。

## 许可证

[Apache License 2.0](LICENSE)
