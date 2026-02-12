package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.MessageHook;
import com.beema.kernel.domain.message.MessageHookDTO;
import com.beema.kernel.domain.message.MessageHookRepository;
import com.beema.kernel.service.expression.JexlExpressionEngine;
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
 * Unit tests for MessageHookService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageHookService Tests")
class MessageHookServiceTest {

    @Mock
    private MessageHookRepository hookRepository;

    @Mock
    private JexlExpressionEngine jexlEngine;

    @Mock
    private MessageProcessingService processingService;

    private MessageHookService hookService;

    @BeforeEach
    void setUp() {
        hookService = new MessageHookServiceImpl(hookRepository, jexlEngine, processingService);
    }

    @Test
    @DisplayName("Should find hooks by message type and source system")
    void shouldFindHooksByMessageTypeAndSourceSystem() {
        // Given
        String messageType = "policy.created";
        String sourceSystem = "retail_system";

        MessageHook hook1 = createSampleHook(1L, "hook1", messageType, sourceSystem);
        MessageHook hook2 = createSampleHook(2L, "hook2", messageType, sourceSystem);

        when(hookRepository.findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem))
                .thenReturn(Arrays.asList(hook1, hook2));

        // When
        List<MessageHook> result = hookService.findHooksByMessageType(messageType, sourceSystem);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(hook1, hook2);
        verify(hookRepository).findByMessageTypeAndSourceSystemAndEnabledTrue(messageType, sourceSystem);
    }

    @Test
    @DisplayName("Should throw exception when message type or source system is null")
    void shouldThrowExceptionWhenParametersNull() {
        // When/Then
        assertThatThrownBy(() -> hookService.findHooksByMessageType(null, "system"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageType and sourceSystem are required");

        assertThatThrownBy(() -> hookService.findHooksByMessageType("type", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageType and sourceSystem are required");
    }

    @Test
    @DisplayName("Should create new hook successfully")
    void shouldCreateNewHook() {
        // Given
        MessageHookDTO dto = createSampleDTO();
        MessageHook entity = dto.toEntity();

        when(hookRepository.existsByHookName(dto.getHookName())).thenReturn(false);
        when(jexlEngine.isValidSyntax(anyString())).thenReturn(true);
        when(hookRepository.save(any(MessageHook.class))).thenReturn(entity);

        // When
        MessageHook result = hookService.createHook(dto);

        // Then
        assertThat(result).isNotNull();
        verify(hookRepository).existsByHookName(dto.getHookName());
        verify(hookRepository).save(any(MessageHook.class));
    }

    @Test
    @DisplayName("Should throw exception when creating hook with duplicate name")
    void shouldThrowExceptionWhenDuplicateName() {
        // Given
        MessageHookDTO dto = createSampleDTO();

        when(hookRepository.existsByHookName(dto.getHookName())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> hookService.createHook(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should validate hook with valid JEXL scripts")
    void shouldValidateHookSuccessfully() {
        // Given
        MessageHook hook = createSampleHook(1L, "test_hook", "policy.created", "retail_system");

        when(jexlEngine.isValidSyntax(anyString())).thenReturn(true);

        // When
        MessageHookService.ValidationResult result = hookService.validateHook(hook);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation with invalid JEXL syntax")
    void shouldFailValidationWithInvalidJexl() {
        // Given
        MessageHook hook = createSampleHook(1L, "test_hook", "policy.created", "retail_system");

        when(jexlEngine.isValidSyntax(hook.getTransformationJexl())).thenReturn(false);

        // When
        MessageHookService.ValidationResult result = hookService.validateHook(hook);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0)).contains("Invalid JEXL syntax");
    }

    @Test
    @DisplayName("Should fail validation with missing required fields")
    void shouldFailValidationWithMissingFields() {
        // Given
        MessageHook hook = new MessageHook();
        hook.setHookName("test");
        // Missing messageType, sourceSystem, transformationJexl

        // When
        MessageHookService.ValidationResult result = hookService.validateHook(hook);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(3);
    }

    @Test
    @DisplayName("Should update existing hook")
    void shouldUpdateHook() {
        // Given
        Long hookId = 1L;
        MessageHook existing = createSampleHook(hookId, "old_name", "policy.created", "retail_system");
        MessageHookDTO dto = createSampleDTO();
        dto.setHookName("new_name");

        when(hookRepository.findById(hookId)).thenReturn(Optional.of(existing));
        when(hookRepository.existsByHookName(dto.getHookName())).thenReturn(false);
        when(jexlEngine.isValidSyntax(anyString())).thenReturn(true);
        when(hookRepository.save(any(MessageHook.class))).thenReturn(existing);

        // When
        MessageHook result = hookService.updateHook(hookId, dto);

        // Then
        assertThat(result).isNotNull();
        verify(hookRepository).findById(hookId);
        verify(hookRepository).save(any(MessageHook.class));
    }

    @Test
    @DisplayName("Should delete hook")
    void shouldDeleteHook() {
        // Given
        Long hookId = 1L;

        when(hookRepository.existsById(hookId)).thenReturn(true);

        // When
        hookService.deleteHook(hookId);

        // Then
        verify(hookRepository).existsById(hookId);
        verify(hookRepository).deleteById(hookId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent hook")
    void shouldThrowExceptionWhenDeletingNonExistentHook() {
        // Given
        Long hookId = 999L;

        when(hookRepository.existsById(hookId)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> hookService.deleteHook(hookId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should enable/disable hook")
    void shouldEnableDisableHook() {
        // Given
        Long hookId = 1L;
        MessageHook hook = createSampleHook(hookId, "test_hook", "policy.created", "retail_system");

        when(hookRepository.findById(hookId)).thenReturn(Optional.of(hook));
        when(hookRepository.save(any(MessageHook.class))).thenReturn(hook);

        // When
        MessageHook result = hookService.setHookEnabled(hookId, false);

        // Then
        assertThat(result).isNotNull();
        verify(hookRepository).findById(hookId);
        verify(hookRepository).save(any(MessageHook.class));
    }

    // Helper methods

    private MessageHook createSampleHook(Long id, String name, String messageType, String sourceSystem) {
        MessageHook hook = new MessageHook();
        hook.setHookId(id);
        hook.setHookName(name);
        hook.setMessageType(messageType);
        hook.setSourceSystem(sourceSystem);
        hook.setTransformationJexl("message");
        hook.setErrorHandlingStrategy("fail_fast");
        hook.setEnabled(true);
        return hook;
    }

    private MessageHookDTO createSampleDTO() {
        MessageHookDTO dto = new MessageHookDTO();
        dto.setHookName("test_hook");
        dto.setMessageType("policy.created");
        dto.setSourceSystem("retail_system");
        dto.setTransformationJexl("message");
        dto.setErrorHandlingStrategy("fail_fast");
        dto.setEnabled(true);
        return dto;
    }
}
