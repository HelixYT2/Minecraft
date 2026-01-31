package com.example.hierarchicalbots.core;

import java.util.Random;

public record GeneticTraits(double intelligence, double speed, int generation) {
    private static final double MUTATION_RANGE = 0.10;

    public static GeneticTraits random(Random random) {
        double intelligence = 0.5 + random.nextDouble() * 0.5;
        double speed = 0.4 + random.nextDouble() * 0.6;
        return new GeneticTraits(intelligence, speed, 0);
    }

    public static GeneticTraits fromParents(GeneticTraits parentA, GeneticTraits parentB, Random random) {
        double baseIntelligence = (parentA.intelligence + parentB.intelligence) / 2.0;
        double baseSpeed = (parentA.speed + parentB.speed) / 2.0;
        double mutatedIntelligence = mutate(baseIntelligence, random);
        double mutatedSpeed = mutate(baseSpeed, random);
        int generation = Math.max(parentA.generation, parentB.generation) + 1;
        return new GeneticTraits(mutatedIntelligence, mutatedSpeed, generation);
    }

    private static double mutate(double value, Random random) {
        double mutation = 1.0 + (random.nextDouble() * 2.0 - 1.0) * MUTATION_RANGE;
        return Math.max(0.1, Math.min(1.5, value * mutation));
    }
}
