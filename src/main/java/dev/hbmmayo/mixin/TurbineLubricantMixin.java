package dev.hbmmayo.mixin;

import com.hbm.blockentity.machine.BlockEntityMachineTurbineGas;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.hbmmayo.neoforge.*;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;

/** Only the native gas turbine has a lubricant tank in this inspected build. */
@Mixin(value=BlockEntityMachineTurbineGas.class,remap=false)
abstract class TurbineLubricantMixin {
    @WrapMethod(method="canReceiveFluid")
    private boolean hbmmayo$acceptsMayo(Fluid fluid,Operation<Boolean> original) {
        var tank=((BlockEntityMachineTurbineGas)(Object)this).tanks[1];
        if(fluid==MayoRegistry.fluid("industrial_mayonnaise") || fluid==NTMFluids.LUBRICANT)
            return tank.getFill()==0 || tank.getTankType()==fluid;
        if(fluid==MayoRegistry.fluid("burnt_mayonnaise")) return false;
        return original.call(fluid);
    }
    @WrapMethod(method="receiverFor")
    private FluidTankNTM hbmmayo$compatibleLubricant(Fluid fluid,Operation<FluidTankNTM> original) {
        var tank=((BlockEntityMachineTurbineGas)(Object)this).tanks[1];
        if(fluid==MayoRegistry.fluid("industrial_mayonnaise") || fluid==NTMFluids.LUBRICANT)
            return tank.getFill()==0 || tank.getTankType()==fluid?tank:null;
        if(fluid==MayoRegistry.fluid("burnt_mayonnaise")) return null;
        return original.call(fluid);
    }
    @WrapMethod(method="transferFluid")
    private long hbmmayo$commitFluid(Fluid fluid,int pressure,long amount,Operation<Long> original) {
        var tank=((BlockEntityMachineTurbineGas)(Object)this).tanks[1];
        if(amount>0 && pressure==tank.getPressure() && tank.getFill()==0
                && (fluid==MayoRegistry.fluid("industrial_mayonnaise") || fluid==NTMFluids.LUBRICANT))
            tank.setTankType(fluid); // Mutate only on an actual transfer, never on a network probe.
        return original.call(fluid,pressure,amount);
    }
    @WrapMethod(method="makePower")
    private void hbmmayo$efficiency(double fuel,int throttle,Operation<Void> original) {
        var machine=(BlockEntityMachineTurbineGas)(Object)this;
        var tank=machine.tanks[1];
        boolean mayonnaise=tank.getTankType()==MayoRegistry.fluid("industrial_mayonnaise");
        if(tank.getTankType()==MayoRegistry.fluid("burnt_mayonnaise")) {
            machine.hbm$syncSet$BlockEntityMachineTurbineGas$state(0);machine.markChanged();return;
        }
        if(mayonnaise && machine.temp>=HbmMayonnaise.config.lubricantMaxTemperature()) {
            int amount=tank.getFill();tank.setTankType(MayoRegistry.fluid("burnt_mayonnaise"));tank.setFill(amount);
            machine.hbm$syncSet$BlockEntityMachineTurbineGas$state(0);machine.markChanged();return;
        }
        long before=machine.getPower();
        original.call(fuel,throttle); // Native fuel, oil consumption and turbine behavior remain in control.
        if(mayonnaise && machine.getPower()>before)
            machine.setPower(before+(long)Math.floor((machine.getPower()-before)*HbmMayonnaise.config.lubricantEfficiency()));
    }
}
