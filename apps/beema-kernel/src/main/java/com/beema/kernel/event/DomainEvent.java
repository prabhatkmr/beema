package com.beema.kernel.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all domain events published to Inngest
 */
public abstract class DomainEvent {

    @JsonProperty("id")
    private String eventId;

    @JsonProperty("name")
    private String eventName;

    @JsonProperty("ts")
    private long timestamp;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("user")
    private Map<String, String> user;

    @JsonProperty("v")
    private String version = "2024-11-01";

    protected DomainEvent(String eventName) {
        this.eventId = UUID.randomUUID().toString();
        this.eventName = eventName;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
        this.user = new HashMap<>();
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, String> getUser() {
        return user;
    }

    public void setUser(Map<String, String> user) {
        this.user = user;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Add data to the event payload
     */
    public DomainEvent withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * Set user context for the event
     */
    public DomainEvent withUser(String userId, String email) {
        this.user.put("id", userId);
        this.user.put("email", email);
        return this;
    }
}
