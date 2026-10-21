package dev.hbmmayo.core;
public final class Lubrication {
    private Lubrication() {}
    /** Preserve native consumption; apply this factor only to accepted mayo lubricant. */
    public static double efficiency(boolean industrialMayonnaise, double temperature, MayoConfig c) {
        if (!Double.isFinite(temperature)) throw new IllegalArgumentException("temperature");
        return industrialMayonnaise && temperature < c.lubricantMaxTemperature()
            ? c.lubricantEfficiency() : 0;
    }
}
