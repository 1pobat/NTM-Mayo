package dev.hbmmayo.core;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * One adapter tank, one cumulative service clock. Fresh top-ups never reset age.
 * The clock advances only after a reactor accepted cooling, once per server tick.
 * Emptying fresh fluid also preserves age; after burnout, drain ALL burnt fluid
 * to reset. This prevents pumping fluid out and back in to evade burnout.
 */
public final class CoolantTank {
    private final int capacity;
    private int amount, usedTicks;
    private boolean burnt;
    public CoolantTank(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }
    public int capacity() { return capacity; }
    public int amount() { return amount; }
    public int usedTicks() { return usedTicks; }
    public boolean burnt() { return burnt; }
    public int fillIndustrial(int mb) {
        if (mb < 0) throw new IllegalArgumentException("mB");
        if (burnt) return 0;
        int accepted = Math.min(capacity - amount, mb); amount += accepted;
        return accepted;
    }
    public int drain(int mb) {
        if (mb < 0) throw new IllegalArgumentException("mB");
        int drained = Math.min(amount, mb); amount -= drained;
        if (amount == 0 && burnt) { burnt = false; usedTicks = 0; }
        return drained;
    }
    public boolean canCool(MayoConfig c) {
        return amount > 0 && !burnt && usedTicks < c.coolantLifetimeTicks();
    }
    /** Pass ONLY the actual accepted cooling result, not requested cooling. */
    public boolean afterReactorTick(boolean actuallyCooled, MayoConfig c) {
        if (amount > 0 && !burnt && (usedTicks >= c.coolantLifetimeTicks()
                || (actuallyCooled && ++usedTicks >= c.coolantLifetimeTicks()))) {
            burnt = true;
            return true; // request native SCRAM if supported; no explosion
        }
        return false;
    }
    public void write(DataOutput out) throws IOException {
        out.writeInt(1); out.writeInt(capacity); out.writeInt(amount);
        out.writeInt(usedTicks); out.writeBoolean(burnt);
    }
    public static CoolantTank read(DataInput in) throws IOException {
        if (in.readInt() != 1) throw new IOException("Unsupported tank schema");
        int capacity = in.readInt();
        if (capacity <= 0) throw new IOException("Invalid capacity");
        CoolantTank t = new CoolantTank(capacity);
        t.amount = in.readInt(); t.usedTicks = in.readInt(); t.burnt = in.readBoolean();
        if (t.amount < 0 || t.amount > capacity || t.usedTicks < 0 || (t.burnt && t.amount == 0))
            throw new IOException("Corrupt coolant state");
        return t;
    }
}
