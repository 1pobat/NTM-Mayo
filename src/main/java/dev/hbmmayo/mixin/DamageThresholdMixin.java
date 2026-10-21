package dev.hbmmayo.mixin;
import com.hbm.util.DamageResistanceHandler;
import dev.hbmmayo.neoforge.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=DamageResistanceHandler.class,remap=false)
abstract class DamageThresholdMixin {
    @Inject(method="getDTDR",at=@At("RETURN"))
    private static void hbmmayo$nativeThreshold(LivingEntity entity,DamageSource source,float amount,
            float pierceDT,float pierceDR,CallbackInfoReturnable<float[]> cir) {
        if(entity instanceof ServerPlayer player && MayoRuntime.state(player).active())
            cir.getReturnValue()[0]+=(float)HbmMayonnaise.config.damageThresholdHp();
    }
}
