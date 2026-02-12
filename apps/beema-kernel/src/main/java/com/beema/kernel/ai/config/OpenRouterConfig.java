package com.beema.kernel.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenRouter Configuration for Spring AI
 *
 * Configures Spring AI to use OpenRouter endpoint instead of OpenAI directly.
 * OpenRouter provides access to multiple LLM providers (OpenAI, Anthropic, Google, etc.)
 * through a single unified API.
 */
@Configuration
public class OpenRouterConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:openai/gpt-4-turbo-preview}")
    private String model;

    @Value("${beema.ai.openrouter.site-url:https://beema.io}")
    private String siteUrl;

    @Value("${beema.ai.openrouter.app-name:Beema Insurance Platform}")
    private String appName;

    @Bean
    public OpenAiApi openRouterApi() {
        // Create custom headers for OpenRouter
        MultiValueMap<String, String> customHeaders = new LinkedMultiValueMap<>();
        customHeaders.add("HTTP-Referer", siteUrl);
        customHeaders.add("X-Title", appName);

        // Create RestClient with custom headers
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("HTTP-Referer", siteUrl)
            .defaultHeader("X-Title", appName);

        // Create WebClient.Builder
        WebClient.Builder webClientBuilder = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("HTTP-Referer", siteUrl)
            .defaultHeader("X-Title", appName);

        return new OpenAiApi(
            baseUrl,
            apiKey,
            customHeaders,
            "",  // organizationId - not used with OpenRouter
            "",  // projectId - not used with OpenRouter
            restClientBuilder,
            webClientBuilder,
            null // responseErrorHandler - use default
        );
    }

    @Bean
    public OpenAiChatModel openRouterChatModel(OpenAiApi openRouterApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .withModel(model)
            .withTemperature(0.3)
            .withMaxTokens(2000)
            .build();

        return new OpenAiChatModel(openRouterApi, options);
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel openRouterChatModel) {
        return ChatClient.builder(openRouterChatModel);
    }
}
