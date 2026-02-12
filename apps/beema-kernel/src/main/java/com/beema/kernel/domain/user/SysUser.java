package com.beema.kernel.domain.user;

import com.beema.kernel.util.JsonbConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sys_users")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Smart Routing Fields

    @Convert(converter = JsonbConverter.class)
    @Column(name = "skill_tags", columnDefinition = "jsonb")
    private List<String> skillTags = new ArrayList<>();

    @Column(name = "max_tasks")
    private Integer maxTasks = 10;

    @Column(name = "current_tasks")
    private Integer currentTasks = 0;

    @Column(name = "availability_status")
    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "location")
    private String location;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods

    public boolean hasSkill(String skill) {
        return skillTags != null && skillTags.contains(skill);
    }

    public boolean hasAllSkills(List<String> requiredSkills) {
        return skillTags != null && skillTags.containsAll(requiredSkills);
    }

    public boolean hasAnySkill(List<String> requiredSkills) {
        if (skillTags == null || requiredSkills == null) {
            return false;
        }
        return requiredSkills.stream().anyMatch(skillTags::contains);
    }

    public boolean isAvailable() {
        return availabilityStatus == AvailabilityStatus.AVAILABLE
                && currentTasks < maxTasks;
    }

    public double getCapacityUtilization() {
        if (maxTasks == null || maxTasks == 0) {
            return 0.0;
        }
        return (double) currentTasks / maxTasks;
    }

    public int getRemainingCapacity() {
        return Math.max(0, maxTasks - currentTasks);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getSkillTags() {
        return skillTags;
    }

    public void setSkillTags(List<String> skillTags) {
        this.skillTags = skillTags;
    }

    public Integer getMaxTasks() {
        return maxTasks;
    }

    public void setMaxTasks(Integer maxTasks) {
        this.maxTasks = maxTasks;
    }

    public Integer getCurrentTasks() {
        return currentTasks;
    }

    public void setCurrentTasks(Integer currentTasks) {
        this.currentTasks = currentTasks;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
