package com.xnu.rocky.providers

/**
 * Hermes provider — uses the existing OpenAI-compatible ChatClient
 * with a custom host pointing to the Hermes API server.
 *
 * No API key is required for localhost connections. For remote access,
 * the Remote Pi relay (WebSocket) is used instead of direct HTTP.
 */
object HermesProviderConfig {
    /** Default Hermes API base URL on the desktop */
    const val DEFAULT_HOST = "http://localhost:8642/v1"

    /** Default model name to use with Hermes */
    const val DEFAULT_MODEL = "hermes"

    /**
     * Build a ProviderConfiguration for Hermes.
     *
     * @param host Base URL of the Hermes API server (e.g., "http://192.168.1.100:8642/v1")
     * @param model Model name (default "hermes")
     * @param apiKey API key if needed (blank for localhost)
     */
    fun configuration(
        host: String = DEFAULT_HOST,
        model: String = DEFAULT_MODEL,
        apiKey: String = "hermes-local"
    ): ProviderConfiguration = ProviderConfiguration(
        provider = ProviderKind.HERMES,
        modelID = model,
        credential = apiKey,
        customHost = host
    )
}
