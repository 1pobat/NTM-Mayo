package dev.hbmmayo.neoforge;
/** Side-neutral payload cache. Contains no client-only Minecraft type. */
public final class ClientData {
    private ClientData() {}
    private static volatile MayoPayload latest;
    public static MayoPayload latest() { return latest; }
    public static void clear() { latest=null; }
    public static void accept(MayoPayload payload) {
        if(!Double.isFinite(payload.instability()) || payload.instability()<0 || payload.instability()>100
                || payload.revision()<0) throw new IllegalArgumentException("Invalid server infusion snapshot");
        latest=payload; // Reliable ordered connection; login/dimension always sends a fresh full snapshot.
    }
}
