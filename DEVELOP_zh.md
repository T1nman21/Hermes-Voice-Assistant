# 开发指南

> [English](DEVELOP.md) | 中文

## 项目结构

```
app/                        # Android 应用模块
  src/main/java/.../        # Kotlin 源码
    ui/screens/             # Jetpack Compose UI 界面
    providers/              # AI 服务商配置与客户端
    runtime/                # ROS 运行时核心
      tools/                # 30+ 原生桥接服务
      skills/               # 技能系统
      voice/                # 实时语音客户端
    models/                 # 数据模型
  src/main/python/          # Python 运行时脚本
  src/androidTest/          # 集成测试
  src/test/                 # 单元测试
scripts/                    # 构建与部署脚本
keystore/                   # 签名配置
store-assets/               # Play Store 素材
```

## 环境要求

- Android Studio Hedgehog+
- Android SDK 35（目标），SDK 28+（最低）
- Kotlin + Jetpack Compose
- JDK 11+

## 构建

```bash
# Debug 构建
./gradlew assembleStandardDebug

# Release 构建（需要先配置签名）
./gradlew bundleStandardRelease

# 运行测试
./gradlew testStandardDebugUnitTest
./gradlew connectedStandardDebugAndroidTest
```

## 部署到 Google Play

```bash
# 首次配置（密钥库 + 服务账户）
./scripts/deploy-playstore.sh --setup

# 部署到内部测试轨道
./scripts/deploy-playstore.sh

# 部署到 Beta 或正式版
./scripts/deploy-playstore.sh --track beta
./scripts/deploy-playstore.sh --track production
```

## 代码风格

- 4 空格缩进
- 类型使用 `PascalCase`，属性/方法使用 `camelCase`
- Jetpack Compose + Material 3 + MVVM 架构
- Kotlin 协程，async/await 模式

## 测试

使用 JUnit 和 Espresso 进行集成测试。测试覆盖服务商清单、工具注册、技能存储、角色系统和 UI 组件。
