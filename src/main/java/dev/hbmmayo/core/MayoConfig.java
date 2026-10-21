package dev.hbmmayo.core;

/** Server-owned configuration, values use HP, ticks, HE and percentage points. */
public record MayoConfig(
    double lubricantEfficiency, double coolantEfficiency, int coolantLifetimeTicks,
    double damageThresholdHp, double attackBonus, double speedBonus,
    double regenHp, int regenIntervalTicks, double damageGraceHearts,
    double instabilityScale, double maxHealthOffsetHearts,
    int passiveDelayTicks, double passivePointsPerSecond,
    double killPoints, long passiveHePerSecond, long killHe,
    double injectorPoints, int injectorCooldownTicks,
    double chamberRadius, double chamberPointsPerSecond, long chamberHePerSecond,
    double lubricantMaxTemperature) {
    public MayoConfig {
        double[] positive = {lubricantEfficiency, coolantEfficiency, instabilityScale,
            maxHealthOffsetHearts, chamberRadius, lubricantMaxTemperature};
        for (double n : positive) if (!Double.isFinite(n) || n <= 0)
            throw new IllegalArgumentException("Expected a finite positive config value");
        double[] nonnegative = {damageThresholdHp, attackBonus, speedBonus, regenHp,
            damageGraceHearts, passivePointsPerSecond, killPoints, injectorPoints,
            chamberPointsPerSecond};
        for (double n : nonnegative) if (!Double.isFinite(n) || n < 0)
            throw new IllegalArgumentException("Expected a finite nonnegative config value");
        if (coolantLifetimeTicks <= 0 || regenIntervalTicks <= 0 || passiveDelayTicks < 0
            || injectorCooldownTicks < 0 || passiveHePerSecond < 0 || killHe < 0
            || chamberHePerSecond < 0) throw new IllegalArgumentException("Invalid ticks/energy");
    }
    public static MayoConfig defaults() {
        return new MayoConfig(.85, .60, 600, 3, .30, .20, 1, 100,
            .5, 100, 10, 160, .5, 7, 100, 1000, 25, 1200, 3, 1, 1000, 800);
    }
}
