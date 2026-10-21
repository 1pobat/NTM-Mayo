package dev.hbmmayo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.hbmmayo.neoforge.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
abstract class LivingDamageMixin {
    @WrapMethod(method="setHealth")
    private void hbmmayo$healthWrites(float value,Operation<Void> original) {
        if(!((Object)this instanceof ServerPlayer player)) { original.call(value);return; }
        float before=player.getHealth();
        original.call(value);
        MayoRuntime.healthChanged(player,before,player.getHealth());
    }
    @WrapMethod(method="hurtServer")
    private boolean hbmmayo$actualHealth(ServerLevel level,DamageSource source,float amount,Operation<Boolean> original) {
        if(source.getEntity() instanceof ServerPlayer attacker && MayoRuntime.state(attacker).active())
            amount*=(float)(1+HbmMayonnaise.config.attackBonus());
        if(!((Object)this instanceof ServerPlayer player)) return original.call(level,source,amount);
        var controller=MayoRuntime.controller(player);
        controller.beforeDamage(player.getHealth());
        boolean completed=false;
        try {
            boolean result=original.call(level,source,amount);
            completed=true;
            controller.afterDamage(player.getHealth(),player.getMaxHealth(),MayoRuntime.port(player),HbmMayonnaise.config);
            return result;
        } finally { if(!completed) controller.damageAborted(); }
    }
}
