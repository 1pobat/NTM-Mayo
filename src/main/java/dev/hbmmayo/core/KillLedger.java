package dev.hbmmayo.core;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Use one fresh identity token per victim LIFE, retained on that victim. Never
 * use a reused UUID as token. Invoke only from an authoritative server death
 * hook after native kill attribution has identified the player (incl. projectiles).
 * This class has no victim category or cooldown checks.
 */
public final class KillLedger {
    public static final class VictimLife {}
    private final Set<VictimLife> processed = Collections.newSetFromMap(new WeakHashMap<>());
    public boolean death(VictimLife victimLife, InfusionState killer,
                         InfusionState.ChestModule module, EnergyStore energy, MayoConfig c) {
        if (victimLife == null) throw new IllegalArgumentException("victimLife");
        if (!processed.add(victimLife)) return false;
        return killer != null && killer.kill(module, energy, c);
    }
}
