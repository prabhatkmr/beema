package com.beema.kernel.service.router;

import com.beema.kernel.domain.user.AvailabilityStatus;
import com.beema.kernel.domain.user.SysUser;
import com.beema.kernel.repository.user.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Smart Work Routing Service
 *
 * Routes tasks to users based on:
 * - Skill match (exact skills required)
 * - Availability (current_tasks < max_tasks)
 * - Location match (optional)
 * - Capacity utilization (prefer users with lower load)
 *
 * Scoring Algorithm:
 * - Skill Match Score (0-100): % of required skills user has
 * - Availability Score (0-100): (1 - capacity_utilization) * 100
 * - Location Match Bonus: +20 points if location matches
 * - Total Score: Skill Match + Availability + Location Bonus
 */
@Service
public class WorkRouterService {

    private static final Logger log = LoggerFactory.getLogger(WorkRouterService.class);

    private final SysUserRepository userRepository;

    public WorkRouterService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Route a task to the best available user.
     *
     * @param requirements Task requirements (skills, location, complexity, priority)
     * @return Routing result with assigned user and match score
     */
    @Transactional
    public RoutingResult routeTask(TaskRequirements requirements) {
        log.info("Routing task with requirements: {}", requirements);

        // Find eligible users
        List<SysUser> eligibleUsers = findEligibleUsers(requirements);

        if (eligibleUsers.isEmpty()) {
            log.warn("No eligible users found for requirements: {}", requirements);
            return RoutingResult.noMatch();
        }

        // Score and rank users
        SysUser bestUser = eligibleUsers.stream()
                .map(user -> new ScoredUser(user, calculateScore(user, requirements)))
                .peek(scoredUser -> log.debug("User {} scored {}: {}",
                        scoredUser.user().getEmail(),
                        scoredUser.score(),
                        scoredUser.reasoning()))
                .max(Comparator.comparingDouble(ScoredUser::score))
                .map(ScoredUser::user)
                .orElse(null);

        if (bestUser == null) {
            return RoutingResult.noMatch();
        }

        // Assign task to user (increment current_tasks)
        bestUser.setCurrentTasks(bestUser.getCurrentTasks() + 1);
        userRepository.save(bestUser);

        double score = calculateScore(bestUser, requirements).score;
        String reasoning = calculateScore(bestUser, requirements).reasoning;

        log.info("Task routed to user: {} (score: {}, reason: {})",
                bestUser.getEmail(), score, reasoning);

        return new RoutingResult(bestUser, score, reasoning);
    }

    /**
     * Release a task from a user (decrement current_tasks).
     *
     * @param userId User ID
     */
    @Transactional
    public void releaseTask(UUID userId) {
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getCurrentTasks() > 0) {
            user.setCurrentTasks(user.getCurrentTasks() - 1);
            userRepository.save(user);
            log.info("Task released from user: {} (current_tasks: {})",
                    user.getEmail(), user.getCurrentTasks());
        }
    }

    /**
     * Find users eligible for task assignment.
     *
     * Criteria:
     * - Is active
     * - Availability status is AVAILABLE
     * - current_tasks < max_tasks
     * - Has at least one required skill (if skills specified)
     */
    private List<SysUser> findEligibleUsers(TaskRequirements requirements) {
        List<SysUser> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(SysUser::getIsActive)
                .filter(user -> user.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                .filter(SysUser::isAvailable)
                .filter(user -> hasRequiredSkills(user, requirements.requiredSkills()))
                .toList();
    }

    /**
     * Check if user has required skills.
     */
    private boolean hasRequiredSkills(SysUser user, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return true;  // No skill requirement
        }
        return user.hasAnySkill(requiredSkills);
    }

    /**
     * Calculate routing score for a user.
     *
     * Score = Skill Match (0-100) + Availability (0-100) + Location Bonus (0-20)
     */
    private ScoredResult calculateScore(SysUser user, TaskRequirements requirements) {
        StringBuilder reasoning = new StringBuilder();

        // Skill Match Score (0-100)
        double skillScore = calculateSkillScore(user, requirements.requiredSkills());
        reasoning.append(String.format("Skill: %.0f", skillScore));

        // Availability Score (0-100) - prefer users with lower load
        double availabilityScore = (1.0 - user.getCapacityUtilization()) * 100;
        reasoning.append(String.format(", Availability: %.0f (load: %d/%d)",
                availabilityScore, user.getCurrentTasks(), user.getMaxTasks()));

        // Location Match Bonus (0-20)
        double locationBonus = 0;
        if (requirements.location() != null
                && requirements.location().equals(user.getLocation())) {
            locationBonus = 20;
            reasoning.append(", Location: +20");
        }

        double totalScore = skillScore + availabilityScore + locationBonus;

        return new ScoredResult(totalScore, reasoning.toString());
    }

    /**
     * Calculate skill match score.
     *
     * Returns percentage of required skills that user has.
     */
    private double calculateSkillScore(SysUser user, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 100;  // No skill requirement = perfect match
        }

        long matchedSkills = requiredSkills.stream()
                .filter(user::hasSkill)
                .count();

        return (double) matchedSkills / requiredSkills.size() * 100;
    }

    private record ScoredUser(SysUser user, ScoredResult scoredResult) {
        double score() {
            return scoredResult.score;
        }

        String reasoning() {
            return scoredResult.reasoning;
        }
    }

    private record ScoredResult(double score, String reasoning) {
    }
}
