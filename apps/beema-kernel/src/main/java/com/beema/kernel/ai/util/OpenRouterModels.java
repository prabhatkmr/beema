package com.beema.kernel.ai.util;

/**
 * OpenRouter model identifiers for easy reference
 *
 * This utility class provides constants for all supported OpenRouter models,
 * making it easier to reference and switch between different LLM providers.
 */
public class OpenRouterModels {

    // OpenAI Models
    public static final String GPT_4_TURBO = "openai/gpt-4-turbo-preview";
    public static final String GPT_4O = "openai/gpt-4o";
    public static final String GPT_4O_MINI = "openai/gpt-4o-mini";
    public static final String GPT_3_5_TURBO = "openai/gpt-3.5-turbo";

    // Anthropic Models
    public static final String CLAUDE_3_OPUS = "anthropic/claude-3-opus-20240229";
    public static final String CLAUDE_3_SONNET = "anthropic/claude-3-sonnet-20240229";
    public static final String CLAUDE_3_HAIKU = "anthropic/claude-3-haiku-20240307";

    // Google Models
    public static final String GEMINI_PRO_1_5 = "google/gemini-pro-1.5";
    public static final String GEMINI_FLASH = "google/gemini-flash-1.5";

    // Meta Models
    public static final String LLAMA_3_1_70B = "meta-llama/llama-3.1-70b-instruct";
    public static final String LLAMA_3_1_405B = "meta-llama/llama-3.1-405b-instruct";

    // Perplexity Models (with internet access)
    public static final String SONAR_LARGE_ONLINE = "perplexity/llama-3.1-sonar-large-128k-online";

    // Recommended for insurance claims analysis
    public static final String RECOMMENDED_ACCURACY = CLAUDE_3_OPUS;
    public static final String RECOMMENDED_BALANCED = CLAUDE_3_SONNET;
    public static final String RECOMMENDED_SPEED = GPT_4O_MINI;
    public static final String RECOMMENDED_COST = LLAMA_3_1_70B;

    private OpenRouterModels() {
        // Utility class - prevent instantiation
    }
}
