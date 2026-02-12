package com.beema.kernel.service.router;

public enum TaskPriority {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    URGENT(4);

    private final int weight;

    TaskPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
