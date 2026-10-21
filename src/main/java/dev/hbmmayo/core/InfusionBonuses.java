package dev.hbmmayo.core;
/** Stable keys must be upserted/removed, never appended on login or equip. */
public record InfusionBonuses(double damageThresholdHp, double attackMultiplier, double movementMultiplier) {
    public static final String ATTACK_KEY = "hbmmayo:flashgold_attack";
    public static final String SPEED_KEY = "hbmmayo:flashgold_speed";
    public static final String DT_KEY = "hbmmayo:flashgold_dt";
    public static InfusionBonuses forState(InfusionState state, MayoConfig c) {
        return state.active() ? new InfusionBonuses(c.damageThresholdHp(), 1 + c.attackBonus(), 1 + c.speedBonus())
            : new InfusionBonuses(0, 1, 1);
    }
}
