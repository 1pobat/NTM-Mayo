package dev.hbmmayo.core;
import java.util.Locale;

/** No Minecraft client imports; a future client HUD adapter renders this model. */
public final class ClientSnapshot {
    private InfusionState.Snapshot current;
    public void resetConnection() { current = null; }
    public boolean accept(InfusionState.Snapshot incoming) {
        if (incoming == null || incoming.revision() < 0 || !Double.isFinite(incoming.instability())
                || incoming.instability() < 0 || incoming.instability() > 100)
            throw new IllegalArgumentException("Invalid snapshot");
        if (current != null && incoming.revision() < current.revision()) return false;
        current = incoming; return true;
    }
    public boolean visible() { return current != null && current.active(); }
    public boolean warning() { return visible() && current.instability() >= 75; }
    public String label(boolean russian) {
        return String.format(Locale.ROOT, "%s: %.1f%%", russian ? "НЕСТАБИЛЬНОСТЬ FLASHGOLD" : "FLASHGOLD INSTABILITY",
            current == null ? 0 : current.instability());
    }
}
