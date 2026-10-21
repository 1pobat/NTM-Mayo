package dev.hbmmayo.core;

/** Server orchestration. PlayerPort is OUR contract, not an invented NTM API. */
public final class PlayerController {
    public interface PlayerPort {
        /** Must upsert the two stable attribute keys and native DT contribution. */
        void reconcileBonuses(InfusionBonuses bonuses);
        /** Must clamp healing to actual max HP. */
        void heal(double hp);
        /** Native chest armor module + HE state, resolved anew every call. */
        InfusionState.ChestModule chestModule();
        EnergyStore armorEnergy();
        /** Send only to the owning player's client. */
        void synchronize(InfusionState.Snapshot snapshot);
        /**
         * Must kill immediately, bypassing native DT/DR, armor, shields and totems;
         * emit local gold particles, dedicated sound and custom death message.
         * Must not use an explosion or area damage. No default implementation.
         */
        void annihilate();
    }
    private final InfusionState state;
    private final DamageLedger damage = new DamageLedger();
    private boolean annihilating;
    private long lastSent = -1;
    public PlayerController(InfusionState state) { this.state = state; }
    public InfusionState state() { return state; }
    public void beforeDamage(double hp) { damage.begin(hp); }
    public void healthChanged(double before,double after) { damage.healthChanged(before,after); }
    public void damageAborted() { damage.abort(); }
    public void afterDamage(double hp, double maxHp, PlayerPort player, MayoConfig c) {
        double loss = damage.end(hp);
        if (annihilating) return;
        var result = state.healthLost(loss, maxHp, c);
        if (result.annihilate()) annihilate(player,c);
        sync(player);
    }
    private void annihilate(PlayerPort player, MayoConfig c) {
        if (annihilating) return;
        annihilating = true;
        try {
            // Disable bonuses before any nested forced-death damage hooks.
            state.die();
            player.reconcileBonuses(InfusionBonuses.forState(state,c));
            player.annihilate();
        } finally { annihilating = false; }
    }
    /** Return true only if the item can now be consumed by the platform callback. */
    public boolean consumeFlashgoldJar(PlayerPort player, MayoConfig c) {
        if (!state.activate()) return false;
        player.reconcileBonuses(InfusionBonuses.forState(state,c)); sync(player); return true;
    }
    public boolean consumeInjector(PlayerPort player, MayoConfig c) {
        if (!state.inject(c)) return false;
        sync(player); return true;
    }
    public void tick(PlayerPort player, MayoConfig c) {
        if (state.active() && state.instability() >= 100) annihilate(player,c);
        var result = state.tick(player.chestModule(), player.armorEnergy(), c);
        player.reconcileBonuses(InfusionBonuses.forState(state,c));
        if (result.healHp() > 0) player.heal(result.healHp());
        sync(player);
    }
    public void death(PlayerPort player, MayoConfig c) {
        state.die(); player.reconcileBonuses(InfusionBonuses.forState(state,c)); sync(player);
    }
    public void connectedOrChangedDimension(PlayerPort player, MayoConfig c) {
        lastSent = -1;
        player.reconcileBonuses(InfusionBonuses.forState(state,c)); sync(player);
    }
    private void sync(PlayerPort player) {
        if (state.revision() != lastSent) {
            player.synchronize(state.snapshot()); lastSent = state.revision();
        }
    }
}
