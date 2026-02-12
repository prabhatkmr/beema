package com.beema.kernel.service.router;

public enum TaskComplexity {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    TaskComplexity(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
