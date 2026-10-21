package dev.hbmmayo.neoforge;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ModArmorItem;
import dev.hbmmayo.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Server-only lifecycle and native integration. No client classes are referenced. */
public final class MayoRuntime {
    private MayoRuntime() {}
    private static final Map<ServerPlayer,PlayerController> CONTROLLERS=new WeakHashMap<>();
    private static final Map<LivingEntity,KillLedger.VictimLife> LIVES=new WeakHashMap<>();
    private static final Set<LivingEntity> FORCED=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final KillLedger KILLS=new KillLedger();
    public static final ResourceKey<DamageType> ANNIHILATION=ResourceKey.create(Registries.DAMAGE_TYPE,
        MayoRegistry.id("flashgold_annihilation"));
    public static InfusionState state(ServerPlayer player) { return player.getData(MayoRegistry.INFUSION.get()); }
    public static PlayerController controller(ServerPlayer player) {
        return CONTROLLERS.computeIfAbsent(player,p->new PlayerController(state(p)));
    }
    public static void healthChanged(ServerPlayer player,double before,double after) {
        var existing=CONTROLLERS.get(player);
        if(existing!=null) existing.healthChanged(before,after);
    }
    public static boolean forced(LivingEntity entity) { return FORCED.contains(entity); }
    public static InfusionState.ChestModule module(ServerPlayer player) {
        ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST);
        var installed=ArmorModHandler.modItem(chest,ArmorModHandler.PLATE_ONLY);
        if(installed==MayoRegistry.item("passive_flashgold_stabilizer")) return InfusionState.ChestModule.PASSIVE;
        if(installed==MayoRegistry.item("entropy_reclaimer")) return InfusionState.ChestModule.ENTROPY;
        return InfusionState.ChestModule.NONE;
    }
    public static EnergyStore armorEnergy(ServerPlayer player) {
        // Capture neither stack nor energy balance: equipment can change between calls.
        return cost->{
            ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST);
            if(cost==0) return !chest.isEmpty();
            if(chest.getItem() instanceof ModArmorItem armor && armor.isPowered() && armor.isEnabled(chest)) {
                long available=armor.getCharge(chest);
                if(available<cost) return false;
                armor.setCharge(chest,available-cost); return true;
            }
            if(chest.getItem() instanceof IBatteryItem battery) {
                long available=battery.getCharge(chest);
                if(available<cost) return false;
                battery.setCharge(chest,available-cost); return true;
            }
            return false;
        };
    }
    public static PlayerController.PlayerPort port(ServerPlayer player) {
        return new PlayerController.PlayerPort() {
            public void reconcileBonuses(InfusionBonuses bonuses) {
                var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);
                Identifier key=Identifier.parse(InfusionBonuses.SPEED_KEY);
                if(speed!=null) {
                    if(state(player).active()) speed.addOrUpdateTransientModifier(new AttributeModifier(key,
                        bonuses.movementMultiplier()-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    else speed.removeModifier(key);
                }
                // Outgoing damage is scaled once at hurtServer; no attack attribute double dip.
                // Native DT is contributed by DamageThresholdMixin, before DT pierce/DR.
            }
            public void heal(double hp) { if(player.isAlive()) player.heal((float)hp); }
            public InfusionState.ChestModule chestModule() { return module(player); }
            public EnergyStore armorEnergy() { return MayoRuntime.armorEnergy(player); }
            public void synchronize(InfusionState.Snapshot snapshot) {
                PacketDistributor.sendToPlayer(player,new MayoPayload(player.getUUID(),snapshot.active(),
                    snapshot.instability(),snapshot.revision()));
            }
            public void annihilate() {
                ServerLevel level=player.level();
                var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ANNIHILATION));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,player.getX(),player.getY()+1,player.getZ(),
                    70,.5,.8,.5,.15);
                level.playSound(null,player.blockPosition(),MayoRegistry.ANNIHILATION_SOUND.get(),SoundSource.PLAYERS,1,1);
                FORCED.add(player);
                try {
                    // Direct native death bypasses hurtServer/DT/DR/shields and totem interception.
                    player.setHealth(0);
                    player.die(damage);
                } finally { FORCED.remove(player); }
            }
        };
    }
    public static void levelTick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        for(ServerPlayer player:level.players()) if(player.isAlive())
            controller(player).tick(port(player),HbmMayonnaise.config);
    }
    private static void resync(net.minecraft.world.entity.player.Player player) {
        if(player instanceof ServerPlayer server)
            controller(server).connectedOrChangedDimension(port(server),HbmMayonnaise.config);
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { resync(event.getEntity()); }
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { resync(event.getEntity()); }
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { resync(event.getEntity()); }
    /** Registered at LOWEST after NTM's ordinary armor-revival listeners. */
    public static void death(LivingDeathEvent event) {
        var victim=event.getEntity();
        if(forced(victim)) event.setCanceled(false);
        if(event.isCanceled()) return;
        if(victim instanceof ServerPlayer player)
            controller(player).death(port(player),HbmMayonnaise.config);
        // DamageSource.getEntity is the causing entity, including projectile owner.
        if(event.getSource().getEntity() instanceof ServerPlayer killer) {
            var life=LIVES.computeIfAbsent(victim,e->new KillLedger.VictimLife());
            KILLS.death(life,state(killer),module(killer),armorEnergy(killer),HbmMayonnaise.config);
        }
    }
}
