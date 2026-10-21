package dev.hbmmayo.core;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic recipe transaction model; a platform adapter resolves native symbols. */
public final class Production {
    private Production() {}
    public enum Machine { INDUSTRIAL_MIXER, CHEMICAL_PLANT, PUREX, PACKAGER }
    public record Recipe(String id, Machine machine, Map<String, Long> inputs,
                         Map<String, Long> outputs, int ticks, long hePerTick) {
        public Recipe {
            inputs = Map.copyOf(inputs); outputs = Map.copyOf(outputs);
            if (ticks <= 0 || hePerTick < 0 || inputs.isEmpty() || outputs.isEmpty())
                throw new IllegalArgumentException("Recipe");
            for (long v : inputs.values()) if (v <= 0) throw new IllegalArgumentException("Input");
            for (long v : outputs.values()) if (v <= 0) throw new IllegalArgumentException("Output");
        }
    }
    public interface Inventory {
        /** Validate presence AND output space, without changing any inventory. */
        boolean canExchange(Map<String, Long> inputs, Map<String, Long> outputs);
        /** Atomic revalidation + consumption + insertion. False must make NO changes. */
        boolean exchange(Map<String, Long> inputs, Map<String, Long> outputs);
    }
    public static final class Process {
        private String recipeId = "";
        private int progress;
        public int progress() { return progress; }
        public String recipeId() { return recipeId; }
        public void restore(String id, int ticks, Recipe recipe) {
            if (!recipe.id().equals(id) || ticks < 0 || ticks >= recipe.ticks())
                throw new IllegalArgumentException("Invalid saved recipe progress");
            recipeId = id; progress = ticks;
        }
        /** Do not switch recipes while progress is nonzero; use cancel explicitly. */
        public boolean tick(Recipe r, Inventory inventory, EnergyStore energy) {
            if (!recipeId.isEmpty() && !recipeId.equals(r.id()))
                throw new IllegalStateException("Cancel current recipe before switching");
            recipeId = r.id();
            if (!inventory.canExchange(r.inputs(), r.outputs()) || !energy.consume(r.hePerTick()))
                return false;
            if (progress + 1 < r.ticks()) { progress++; return false; }
            if (!inventory.exchange(r.inputs(), r.outputs())) return false;
            progress = 0; recipeId = ""; return true;
        }
        public void cancel() { progress = 0; recipeId = ""; }
    }
    /** In-memory inventory for simulations and headless checks, not a Minecraft capability. */
    public static final class Stock implements Inventory {
        private final Map<String, Long> quantities = new LinkedHashMap<>();
        private final Map<String, Long> limits;
        public Stock(Map<String, Long> limits) {
            this.limits = Map.copyOf(limits);
            if (limits.values().stream().anyMatch(n -> n < 0)) throw new IllegalArgumentException("limits");
        }
        public long get(String id) { return quantities.getOrDefault(id, 0L); }
        public void set(String id, long value) {
            if (value < 0 || value > limits.getOrDefault(id, 0L)) throw new IllegalArgumentException(id);
            quantities.put(id, value);
        }
        private Map<String, Long> result(Map<String, Long> in, Map<String, Long> out) {
            var next = new LinkedHashMap<>(quantities);
            for (var e : in.entrySet()) {
                long have = next.getOrDefault(e.getKey(), 0L);
                if (e.getValue() <= 0 || have < e.getValue()) return null;
                next.put(e.getKey(), have - e.getValue());
            }
            for (var e : out.entrySet()) {
                long have = next.getOrDefault(e.getKey(), 0L), max = limits.getOrDefault(e.getKey(), 0L);
                if (e.getValue() <= 0 || e.getValue() > max - have) return null;
                next.put(e.getKey(), have + e.getValue());
            }
            return next;
        }
        public synchronized boolean canExchange(Map<String, Long> in, Map<String, Long> out) {
            return result(in, out) != null;
        }
        public synchronized boolean exchange(Map<String, Long> in, Map<String, Long> out) {
            var next = result(in, out);
            if (next == null) return false;
            quantities.clear(); quantities.putAll(next); return true;
        }
    }
}
