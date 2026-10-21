package dev.hbmmayo.core;
import java.util.List;

/** Server tick controller; energy is charged once per active chamber operation. */
public final class Chamber {
    private int ticks;
    public record Target(InfusionState state, double distanceSquared) {}
    public int tick(List<Target> nearby, EnergyStore energy, MayoConfig c) {
        if (++ticks < 20) return 0;
        ticks = 0;
        double r2 = c.chamberRadius() * c.chamberRadius();
        var targets = nearby.stream().filter(t -> Double.isFinite(t.distanceSquared())
            && t.distanceSquared() >= 0 && t.distanceSquared() <= r2
            && t.state().active() && t.state().instability() > 0)
            .map(Target::state).distinct().toList();
        if (targets.isEmpty() || !energy.consume(c.chamberHePerSecond())) return 0;
        targets.forEach(s -> s.stabilize(c.chamberPointsPerSecond()));
        return targets.size();
    }
}
