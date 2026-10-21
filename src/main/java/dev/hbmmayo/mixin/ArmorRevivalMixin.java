package dev.hbmmayo.mixin;
import com.hbm.handler.ArmorModHandler;
import dev.hbmmayo.neoforge.MayoRuntime;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ArmorModHandler.class,remap=false)
abstract class ArmorRevivalMixin {
    @Inject(method="tryRevive",at=@At("HEAD"),cancellable=true)
    private static void hbmmayo$noAnnihilationRevival(LivingEntity entity,CallbackInfoReturnable<Boolean> cir) {
        if(MayoRuntime.forced(entity)) cir.setReturnValue(false);
    }
}
