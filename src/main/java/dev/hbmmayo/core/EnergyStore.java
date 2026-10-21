package dev.hbmmayo.core;

/** The implementation must atomically check and extract the exact HE requested. */
@FunctionalInterface
public interface EnergyStore {
    boolean consume(long he);
}
