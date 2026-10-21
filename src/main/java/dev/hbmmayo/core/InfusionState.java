package dev.hbmmayo.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** One instance per server player. All calls occur on the server thread. */
public final class InfusionState {
    private boolean active;
    private double instability;
    private int quietTicks, regenTicks, passiveTicks, injectorCooldown;
    private long revision;

    public boolean active() { return active; }
    public double instability() { return instability; }
    public int quietTicks() { return quietTicks; }
    public int injectorCooldown() { return injectorCooldown; }
    public long revision() { return revision; }
    public Snapshot snapshot() { return new Snapshot(active, instability, revision); }
    public record Snapshot(boolean active, double instability, long revision) {}
    public record DamageResult(double lostHp, double addedPoints, boolean annihilate) {}
    public record TickResult(double healHp, boolean attributesRequired) {}
    public enum ChestModule { NONE, PASSIVE, ENTROPY }

    /** Call BEFORE removing a jar. False means no item should be consumed. */
    public boolean activate() {
        if (active) return false;
        active = true; instability = 0; quietTicks = 0; regenTicks = 0; passiveTicks = 0;
        revision++;
        return true;
    }

    /**
     * Pair the HP immediately before and after ONE damage invocation, after that
     * invocation returns. HP is ordinary health, never incoming damage or shield HP.
     * A platform hook must call this exactly once per invocation, including nested
     * damage calls; see DamageLedger. Do not also subscribe a second damage event.
     */
    public DamageResult healthLost(double lostHp, double maxHp, MayoConfig c) {
        if (!Double.isFinite(lostHp) || !Double.isFinite(maxHp) || maxHp <= 0)
            throw new IllegalArgumentException("Invalid health");
        double loss = Math.max(0, lostHp);
        if (!active || loss == 0) return new DamageResult(loss, 0, false);
        quietTicks = 0; passiveTicks = 0;
        double delta = Math.max(0, loss / 2 - c.damageGraceHearts())
            * c.instabilityScale() / (maxHp / 2 + c.maxHealthOffsetHearts());
        double old = instability;
        instability = Math.min(100, instability + delta);
        if (instability != old) revision++;
        return new DamageResult(loss, instability - old, instability >= 100);
    }

    public TickResult tick(ChestModule module, EnergyStore energy, MayoConfig c) {
        if (injectorCooldown > 0) injectorCooldown--;
        if (!active) return new TickResult(0, false);
        if (quietTicks < Integer.MAX_VALUE) quietTicks++;
        double heal = 0;
        if (++regenTicks >= c.regenIntervalTicks()) { regenTicks = 0; heal = c.regenHp(); }
        if (module == ChestModule.PASSIVE && quietTicks > c.passiveDelayTicks()) {
            if (++passiveTicks >= 20) {
                passiveTicks = 0;
                if (instability > 0 && energy.consume(c.passiveHePerSecond()))
                    stabilize(c.passivePointsPerSecond());
            }
        } else passiveTicks = 0;
        return new TickResult(heal, true);
    }

    /** Called by one authoritative attributed-death handler, once per victim life. */
    public boolean kill(ChestModule module, EnergyStore energy, MayoConfig c) {
        if (!active || module != ChestModule.ENTROPY || instability == 0) return false;
        if (!energy.consume(c.killHe())) return false;
        stabilize(c.killPoints()); // absolute points: 42 -> 35, never 42 * .93
        return true;
    }
    public boolean inject(MayoConfig c) {
        if (!active || injectorCooldown != 0 || instability == 0) return false;
        stabilize(c.injectorPoints()); injectorCooldown = c.injectorCooldownTicks();
        return true; // only now consume injector
    }
    public void stabilize(double points) {
        if (!Double.isFinite(points) || points < 0) throw new IllegalArgumentException("points");
        if (!active) return;
        double next = Math.max(0, instability - points);
        if (next != instability) { instability = next; revision++; }
    }
    public void die() {
        active = false; instability = 0; quietTicks = 0; regenTicks = 0; passiveTicks = 0;
        injectorCooldown = 0; revision++;
    }
    /** Schema version is explicit; malformed persisted data fails loudly. */
    public void write(DataOutput out) throws IOException {
        out.writeInt(1); out.writeBoolean(active); out.writeDouble(instability);
        out.writeInt(quietTicks); out.writeInt(regenTicks); out.writeInt(passiveTicks);
        out.writeInt(injectorCooldown); out.writeLong(revision);
    }
    public static InfusionState read(DataInput in) throws IOException {
        if (in.readInt() != 1) throw new IOException("Unsupported infusion schema");
        InfusionState s = new InfusionState();
        s.active = in.readBoolean(); s.instability = in.readDouble();
        s.quietTicks = in.readInt(); s.regenTicks = in.readInt(); s.passiveTicks = in.readInt();
        s.injectorCooldown = in.readInt(); s.revision = in.readLong();
        if (!Double.isFinite(s.instability) || s.instability < 0 || s.instability > 100
            || s.quietTicks < 0 || s.regenTicks < 0 || s.passiveTicks < 0
            || s.injectorCooldown < 0 || s.revision < 0) throw new IOException("Corrupt infusion state");
        if (!s.active) { s.instability = 0; s.quietTicks = 0; s.regenTicks = 0; s.passiveTicks = 0; }
        return s;
    }
}
