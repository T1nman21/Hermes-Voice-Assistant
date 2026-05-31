//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderKind(
    val displayName: String,
    val summary: String,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val apiKeyPlaceholder: String,
    val guideUrl: String,
    val baseUrl: String
) {
    OPENAI(
        displayName = "OpenAI",
        summary = "GPT-5, GPT-5 mini, GPT-4o",
        defaultModel = "gpt-5",
        suggestedModels = listOf("gpt-5", "gpt-5-mini", "gpt-4o", "gpt-4o-mini", "o3-mini"),
        apiKeyPlaceholder = "sk-...",
        guideUrl = "https://platform.openai.com/api-keys",
        baseUrl = "https://api.openai.com/v1/"
    ),
    AZURE_OPENAI(
        displayName = "Azure OpenAI",
        summary = "Azure-hosted OpenAI models",
        defaultModel = "gpt-4o",
        suggestedModels = listOf("gpt-4o", "gpt-4o-mini", "gpt-4"),
        apiKeyPlaceholder = "Azure API Key",
        guideUrl = "https://portal.azure.com",
        baseUrl = ""
    ),
    ANTHROPIC(
        displayName = "Anthropic",
        summary = "Claude 3.7 Sonnet, Claude 4",
        defaultModel = "claude-sonnet-4-20250514",
        suggestedModels = listOf("claude-sonnet-4-20250514", "claude-3-7-sonnet-20250219", "claude-3-5-haiku-20241022"),
        apiKeyPlaceholder = "sk-ant-...",
        guideUrl = "https://console.anthropic.com/settings/keys",
        baseUrl = "https://api.anthropic.com/v1/"
    ),
    GEMINI(
        displayName = "Gemini",
        summary = "Gemini 2.5 Pro, Flash",
        defaultModel = "gemini-2.5-flash",
        suggestedModels = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash"),
        apiKeyPlaceholder = "AI...",
        guideUrl = "https://aistudio.google.com/apikey",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/"
    ),
    GROQ(
        displayName = "Groq",
        summary = "Llama 3.3 70B, Mixtral",
        defaultModel = "llama-3.3-70b-versatile",
        suggestedModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"),
        apiKeyPlaceholder = "gsk_...",
        guideUrl = "https://console.groq.com/keys",
        baseUrl = "https://api.groq.com/openai/v1/"
    ),
    XAI(
        displayName = "xAI",
        summary = "Grok-3 Beta",
        defaultModel = "grok-3-beta",
        suggestedModels = listOf("grok-3-beta", "grok-3-mini-beta"),
        apiKeyPlaceholder = "xai-...",
        guideUrl = "https://console.x.ai",
        baseUrl = "https://api.x.ai/v1/"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        summary = "Multi-model gateway",
        defaultModel = "anthropic/claude-sonnet-4.5",
        suggestedModels = listOf("anthropic/claude-sonnet-4.5", "deepseek/deepseek-r1", "openai/gpt-4o"),
        apiKeyPlaceholder = "sk-or-...",
        guideUrl = "https://openrouter.ai/keys",
        baseUrl = "https://openrouter.ai/api/v1/"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        summary = "DeepSeek Chat & Reasoner",
        defaultModel = "deepseek-chat",
        suggestedModels = listOf("deepseek-chat", "deepseek-reasoner"),
        apiKeyPlaceholder = "sk-...",
        guideUrl = "https://platform.deepseek.com/api_keys",
        baseUrl = "https://api.deepseek.com/v1/"
    ),
    VOLCENGINE(
        displayName = "Doubao (Volcengine)",
        summary = "Doubao Seed 1.8, Doubao 1.5 Pro",
        defaultModel = "doubao-seed-1-8-251228",
        suggestedModels = listOf("doubao-seed-1-8-251228", "doubao-1.5-pro-256k-250115", "doubao-1.5-thinking-pro-250415"),
        apiKeyPlaceholder = "API Key",
        guideUrl = "https://console.volcengine.com/ark",
        baseUrl = "https://ark.cn-beijing.volces.com/api/v3/"
    ),
    ZHIPU_AI(
        displayName = "Zhipu AI (GLM)",
        summary = "Zhipu AI GLM OpenAI-compatible endpoint",
        defaultModel = "glm-4.7",
        suggestedModels = listOf("glm-4.7", "glm-5", "glm-5-turbo", "glm-5.1", "glm-4.5"),
        apiKeyPlaceholder = "sk-...",
        guideUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4/"
    ),
    BAILIAN(
        displayName = "Bailian Coding Plan",
        summary = "Alibaba Cloud Bailian Coding Plan. OpenAI-compatible endpoint.",
        defaultModel = "qwen-plus",
        suggestedModels = listOf("qwen-plus", "qwen-max", "qwen-turbo", "qwen3-235b-a22b"),
        apiKeyPlaceholder = "sk-sp-...",
        guideUrl = "https://bailian.console.aliyun.com/cn-beijing/?tab=model#/efm/coding_plan",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/"
    ),
    AIPROXY(
        displayName = "AIProxy",
        summary = "AIProxy-backed OpenAI traffic using partial key plus service URL",
        defaultModel = "gpt-5",
        suggestedModels = listOf("gpt-5", "gpt-5-mini", "gpt-4o"),
        apiKeyPlaceholder = "Partial Key",
        guideUrl = "https://www.aiproxy.pro",
        baseUrl = ""
    ),
    HERMES(
        displayName = "Hermes",
        summary = "Local Hermes agent on your desktop",
        defaultModel = "hermes",
        suggestedModels = listOf("hermes"),
        apiKeyPlaceholder = "hermes-local",
        guideUrl = "http://localhost:8642/v1",
        baseUrl = "http://localhost:8642/v1/"
    );
}
