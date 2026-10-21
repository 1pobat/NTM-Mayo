package dev.hbmmayo.core;
import java.util.ArrayDeque;
import java.util.Deque;

/** Per-player invocation stack prevents counting nested health loss twice. */
public final class DamageLedger {
    private final Deque<Frame> stack = new ArrayDeque<>();
    private static final class Frame {
        final double before;
        double nestedLoss, recordedLoss;
        boolean observedWrite;
        Frame(double hp) { before = hp; }
    }
    public void begin(double ordinaryHp) {
        if (!Double.isFinite(ordinaryHp)) throw new IllegalArgumentException("HP");
        stack.push(new Frame(ordinaryHp));
    }
    /** Called around the real ordinary-health setter, only while a damage frame exists. */
    public void healthChanged(double before,double after) {
        if(!Double.isFinite(before) || !Double.isFinite(after)) throw new IllegalArgumentException("HP");
        if(stack.isEmpty()) return;
        Frame f=stack.peek();f.observedWrite=true;f.recordedLoss+=Math.max(0,before-after);
    }
    public double end(double ordinaryHp) {
        if (!Double.isFinite(ordinaryHp)) throw new IllegalArgumentException("HP");
        if (stack.isEmpty()) throw new IllegalStateException("Unpaired damage hook");
        Frame f = stack.pop();
        double total = Math.max(0, f.before - ordinaryHp);
        double own = f.observedWrite?f.recordedLoss:Math.max(0, total - f.nestedLoss);
        if (!stack.isEmpty()) stack.peek().nestedLoss += total;
        return own;
    }
    /** Use when a damage invocation throws: it must not leave a stale frame. */
    public void abort() {
        if (stack.isEmpty()) throw new IllegalStateException("Unpaired damage hook");
        stack.pop();
    }
}
