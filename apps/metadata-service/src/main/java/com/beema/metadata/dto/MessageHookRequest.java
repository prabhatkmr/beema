package com.beema.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating MessageHook.
 */
public class MessageHookRequest {

    @NotBlank(message = "Hook name is required")
    private String hookName;

    @NotBlank(message = "Message type is required")
    private String messageType;

    @NotBlank(message = "JEXL script is required")
    private String script;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled = true;

    private Integer priority = 0;

    private String description;

    // Constructors
    public MessageHookRequest() {
    }

    public MessageHookRequest(String hookName, String messageType, String script, Boolean enabled) {
        this.hookName = hookName;
        this.messageType = messageType;
        this.script = script;
        this.enabled = enabled;
    }

    // Getters and Setters
    public String getHookName() {
        return hookName;
    }

    public void setHookName(String hookName) {
        this.hookName = hookName;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
