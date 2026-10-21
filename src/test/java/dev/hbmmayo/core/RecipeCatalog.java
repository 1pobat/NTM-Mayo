package dev.hbmmayo.core;
import java.util.List;
import java.util.Map;
/** Generated headless transaction fixtures from the real native recipe resources. */
public final class RecipeCatalog {
private RecipeCatalog() {}
public static final List<Production.Recipe> ALL = List.of(
new Production.Recipe("hbmmayo:colloid_capsule", Production.Machine.CHEMICAL_PLANT, Map.of("item:hbmmayo:empty_capsule", 1L, "fluid:hbmmayo:flashgold_colloid", 100L), Map.of("item:hbmmayo:flashgold_colloid_capsule", 1L), 80, 50L),
new Production.Recipe("hbmmayo:flashgold_colloid", Production.Machine.PUREX, Map.of("item:hbm:billet_balefire_gold", 1L, "item:hbm:pellet_charged", 1L, "fluid:hbm:salient", 500L), Map.of("fluid:hbmmayo:flashgold_colloid", 500L), 400, 1000L),
new Production.Recipe("hbmmayo:flashgold_filling", Production.Machine.CHEMICAL_PLANT, Map.of("item:hbmmayo:shielded_mayonnaise_jar", 1L, "fluid:hbmmayo:flashgold_mayonnaise", 250L), Map.of("item:hbmmayo:flashgold_mayonnaise_jar", 1L), 80, 50L),
new Production.Recipe("hbmmayo:flashgold_mayonnaise", Production.Machine.INDUSTRIAL_MIXER, Map.of("fluid:hbmmayo:mayonnaise_emulsion", 1000L, "fluid:hbmmayo:flashgold_colloid", 500L), Map.of("fluid:hbmmayo:flashgold_mayonnaise", 1500L), 160, 50L),
new Production.Recipe("hbmmayo:industrial_filling", Production.Machine.CHEMICAL_PLANT, Map.of("item:hbmmayo:empty_mayonnaise_jar", 4L, "fluid:hbmmayo:industrial_mayonnaise", 1000L), Map.of("item:hbmmayo:industrial_mayonnaise_jar", 4L), 80, 50L),
new Production.Recipe("hbmmayo:industrial_mayonnaise", Production.Machine.CHEMICAL_PLANT, Map.of("item:hbm:sulfur", 1L, "fluid:hbmmayo:mayonnaise_emulsion", 1000L, "fluid:hbm:salient", 100L), Map.of("fluid:hbmmayo:industrial_mayonnaise", 1000L), 200, 125L),
new Production.Recipe("hbmmayo:mayonnaise_emulsion", Production.Machine.INDUSTRIAL_MIXER, Map.of("item:hbm:niter", 1L, "fluid:hbm:sunfloweroil", 700L, "fluid:hbm:egg", 300L), Map.of("fluid:hbmmayo:mayonnaise_emulsion", 1000L), 160, 50L),
new Production.Recipe("hbmmayo:stabilizer_injector", Production.Machine.CHEMICAL_PLANT, Map.of("item:hbmmayo:empty_injector", 1L, "item:hbm:cm_circuit_aluminium", 1L, "fluid:hbmmayo:flashgold_colloid", 100L), Map.of("item:hbmmayo:flashgold_stabilizer_injector", 1L), 200, 125L)
);
public static Production.Recipe get(String id) { return ALL.stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow(); }
}
