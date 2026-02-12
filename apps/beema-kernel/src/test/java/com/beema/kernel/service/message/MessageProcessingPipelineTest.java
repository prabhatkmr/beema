package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.MessageHook;
import com.beema.kernel.domain.message.MessageHookRepository;
import com.beema.kernel.domain.message.MessageProcessingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageProcessingPipeline
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageProcessingPipeline Tests")
class MessageProcessingPipelineTest {

    @Mock
    private MessageProcessingService processingService;

    @Mock
    private MessageHookRepository hookRepository;

    private MessageProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new MessageProcessingPipeline(processingService, hookRepository);
    }

    @Test
    @DisplayName("Should build pipeline with builder pattern")
    void shouldBuildPipelineWithBuilder() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        MessageHook hook = createTestHook();
        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(processingService.executePreProcessing(any(), eq(hook))).thenReturn(expectedContext);
        when(processingService.executeTransformation(any(), eq(hook))).thenReturn(expectedContext);
        when(processingService.executePostProcessing(any(), eq(hook))).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hook(hook)
                .execute();

        // Then
        assertThat(result).isNotNull();
        verify(processingService).executePreProcessing(any(), eq(hook));
        verify(processingService).executeTransformation(any(), eq(hook));
        verify(processingService).executePostProcessing(any(), eq(hook));
    }

    @Test
    @DisplayName("Should auto-load hooks based on message type and source system")
    void shouldAutoLoadHooks() {
        // Given
        String messageType = "policy.created";
        String sourceSystem = "retail_system";
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        List<MessageHook> hooks = Arrays.asList(
                createTestHook("hook1"),
                createTestHook("hook2")
        );

        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(hookRepository.findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem))
                .thenReturn(hooks);
        when(processingService.executePreProcessing(any(), any())).thenReturn(expectedContext);
        when(processingService.executeTransformation(any(), any())).thenReturn(expectedContext);
        when(processingService.executePostProcessing(any(), any())).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType(messageType)
                .sourceSystem(sourceSystem)
                .autoLoadHooks()
                .execute();

        // Then
        assertThat(result).isNotNull();
        verify(hookRepository).findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem);
        verify(processingService, times(2)).executePreProcessing(any(), any());
        verify(processingService, times(2)).executeTransformation(any(), any());
        verify(processingService, times(2)).executePostProcessing(any(), any());
    }

    @Test
    @DisplayName("Should skip pre-processing when configured")
    void shouldSkipPreProcessing() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        MessageHook hook = createTestHook();
        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(processingService.executeTransformation(any(), eq(hook))).thenReturn(expectedContext);
        when(processingService.executePostProcessing(any(), eq(hook))).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hook(hook)
                .skipPreProcess()
                .execute();

        // Then
        assertThat(result).isNotNull();
        verify(processingService, never()).executePreProcessing(any(), any());
        verify(processingService).executeTransformation(any(), eq(hook));
        verify(processingService).executePostProcessing(any(), eq(hook));
    }

    @Test
    @DisplayName("Should skip transformation when configured")
    void shouldSkipTransformation() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        MessageHook hook = createTestHook();
        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(processingService.executePreProcessing(any(), eq(hook))).thenReturn(expectedContext);
        when(processingService.executePostProcessing(any(), eq(hook))).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hook(hook)
                .skipTransform()
                .execute();

        // Then
        assertThat(result).isNotNull();
        verify(processingService).executePreProcessing(any(), eq(hook));
        verify(processingService, never()).executeTransformation(any(), any());
        verify(processingService).executePostProcessing(any(), eq(hook));
    }

    @Test
    @DisplayName("Should skip post-processing when configured")
    void shouldSkipPostProcessing() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        MessageHook hook = createTestHook();
        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(processingService.executePreProcessing(any(), eq(hook))).thenReturn(expectedContext);
        when(processingService.executeTransformation(any(), eq(hook))).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hook(hook)
                .skipPostProcess()
                .execute();

        // Then
        assertThat(result).isNotNull();
        verify(processingService).executePreProcessing(any(), eq(hook));
        verify(processingService).executeTransformation(any(), eq(hook));
        verify(processingService, never()).executePostProcessing(any(), any());
    }

    @Test
    @DisplayName("Should handle errors based on error handling strategy")
    void shouldHandleErrorsBasedOnStrategy() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        MessageHook hook = createTestHook();
        hook.setErrorHandlingStrategy("fail_fast");

        MessageProcessingContext contextWithError = new MessageProcessingContext(message);
        contextWithError.recordError("Test error", new RuntimeException("Test error"));

        when(processingService.executePreProcessing(any(), eq(hook))).thenReturn(contextWithError);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hook(hook)
                .execute();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isHasErrors()).isTrue();
        verify(processingService).executePreProcessing(any(), eq(hook));
        // Should not proceed to transformation with fail_fast
        verify(processingService, never()).executeTransformation(any(), any());
    }

    @Test
    @DisplayName("Should process with quick execution method")
    void shouldProcessWithQuickMethod() {
        // Given
        String messageType = "policy.created";
        String sourceSystem = "retail_system";
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        List<MessageHook> hooks = Arrays.asList(createTestHook());
        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(hookRepository.findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem))
                .thenReturn(hooks);
        when(processingService.executePreProcessing(any(), any())).thenReturn(expectedContext);
        when(processingService.executeTransformation(any(), any())).thenReturn(expectedContext);
        when(processingService.executePostProcessing(any(), any())).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.process(messageType, sourceSystem, message);

        // Then
        assertThat(result).isNotNull();
        verify(hookRepository).findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem);
    }

    @Test
    @DisplayName("Should throw exception when context not set")
    void shouldThrowExceptionWhenContextNotSet() {
        // When/Then
        assertThatThrownBy(() -> pipeline.builder().execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Context must be set");
    }

    @Test
    @DisplayName("Should throw exception when auto-loading without message type")
    void shouldThrowExceptionWhenAutoLoadingWithoutMessageType() {
        // When/Then
        assertThatThrownBy(() -> pipeline.builder()
                .message(new HashMap<>())
                .autoLoadHooks())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("messageType and sourceSystem must be set");
    }

    @Test
    @DisplayName("Should warn when no hooks configured")
    void shouldWarnWhenNoHooksConfigured() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .execute();

        // Then
        assertThat(result).isNotNull();
        verifyNoInteractions(processingService);
    }

    @Test
    @DisplayName("Should handle multiple hooks with different stages")
    void shouldHandleMultipleHooksWithDifferentStages() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "ABC123");

        MessageHook hook1 = createTestHook("hook1");
        hook1.setPreprocessingJexl("validation script");
        hook1.setPostprocessingJexl(null); // No post-processing

        MessageHook hook2 = createTestHook("hook2");
        hook2.setPreprocessingJexl(null); // No pre-processing
        hook2.setPostprocessingJexl("audit script");

        MessageProcessingContext expectedContext = new MessageProcessingContext(message);

        when(processingService.executePreProcessing(any(), any())).thenReturn(expectedContext);
        when(processingService.executeTransformation(any(), any())).thenReturn(expectedContext);
        when(processingService.executePostProcessing(any(), any())).thenReturn(expectedContext);

        // When
        MessageProcessingContext result = pipeline.builder()
                .message(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hooks(Arrays.asList(hook1, hook2))
                .execute();

        // Then
        assertThat(result).isNotNull();
        verify(processingService, times(1)).executePreProcessing(any(), eq(hook1));
        verify(processingService, times(2)).executeTransformation(any(), any());
        verify(processingService, times(1)).executePostProcessing(any(), eq(hook2));
    }

    // Helper methods

    private MessageHook createTestHook() {
        return createTestHook("test_hook");
    }

    private MessageHook createTestHook(String name) {
        MessageHook hook = new MessageHook();
        hook.setHookId(1L);
        hook.setHookName(name);
        hook.setMessageType("policy.created");
        hook.setSourceSystem("retail_system");
        hook.setPreprocessingJexl("message.validated = true;");
        hook.setTransformationJexl("message");
        hook.setPostprocessingJexl("result.processed = true;");
        hook.setErrorHandlingStrategy("fail_fast");
        hook.setEnabled(true);
        return hook;
    }
}
