package dev.hbmmayo.neoforge;

import com.hbm.api.energymk2.*;
import com.hbm.api.fluidmk2.*;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.machine.rbmk.*;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.hbmmayo.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.io.*;
import java.util.*;

/** Two simple blocks share native item/HE/fluid machinery, not a new reactor. */
public final class MayoBlockEntity extends BlockEntityMachineBase implements IEnergyHandlerMK2,FluidTankEndpoint,MenuProvider {
    private final boolean adapter;
    private long power;
    private final FluidTankNTM coolant;
    private CoolantTank service=new CoolantTank(16000);
    private final Chamber chamber=new Chamber();
    private final FluidFlushOutputs flush=new FluidFlushOutputs();
    public MayoBlockEntity(BlockPos pos,BlockState state,boolean adapter) {
        super((adapter?MayoRegistry.ADAPTER_ENTITY:MayoRegistry.CHAMBER_ENTITY).get(),pos,state,9);
        this.adapter=adapter;
        coolant=new FluidTankNTM(MayoRegistry.fluid("industrial_mayonnaise"),16000);
        coolant.bindOwner(this);
    }
    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power=Math.clamp(value,0,getMaxPower());markChanged(); }
    @Override public long getMaxPower() { return 1000000; }
    public int coolantAmount() { return adapter?coolant.getFill():0; }
    public int serviceTicks() { return adapter?service.usedTicks():0; }
    private boolean consume(long cost) {
        if(power<cost) return false;
        if(cost>0) { power-=cost;markChanged(); }
        return true;
    }
    @Override public void tickServer() {
        if(!(level instanceof ServerLevel server)) return;
        long charge=adapter?0:ItemEnergyTransfer.extract(this,0,getMaxPower()-power,false);
        if(charge>0) { power+=charge;markChanged(); }
        if(!adapter) {
            var targets=new ArrayList<Chamber.Target>();
            for(var player:server.players()) targets.add(new Chamber.Target(MayoRuntime.state(player),
                player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)));
            if(chamber.tick(targets,this::consume,HbmMayonnaise.config)>0) markChanged();
            return;
        }
        coolant.loadTank(1,2,inventory);
        coolant.unloadTank(1,2,inventory);
        reconcileService();
        boolean actuallyCooled=false;
        if(service.canCool(HbmMayonnaise.config)) {
            // Native RBMK Cooler scans a 5x5 square and removes 200 heat per column.
            // Less than 50 mB scales capacity down; topping up does not reset service age.
            double cooling=200*HbmMayonnaise.config.coolantEfficiency()*Math.min(1,coolant.getFill()/50.0);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                BlockPos at=worldPosition.offset(x,0,z);
                if(!server.hasChunkAt(at)) continue;
                if(server.getBlockEntity(at) instanceof BlockEntityRBMKBase column && column.heat>20) {
                    column.hbm$syncSet$BlockEntityRBMKBase$heat(Math.max(20,column.heat-cooling));
                    column.markChanged();actuallyCooled=true;
                }
            }
        }
        if(service.afterReactorTick(actuallyCooled,HbmMayonnaise.config)) {
            syncNativeTank();
            // Native setTarget(0) inserts nearby control rods; it does not change reactor physics.
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                BlockPos at=worldPosition.offset(x,0,z);
                if(server.hasChunkAt(at) && server.getBlockEntity(at) instanceof BlockEntityRBMKControl rods)
                    rods.setTarget(0);
            }
        }
        if(actuallyCooled) markChanged();
        flush.provide(server,this);
    }
    private void reconcileService() {
        int actual=coolant.getFill(),tracked=service.amount();
        if(actual<tracked) service.drain(tracked-actual);
        else if(actual>tracked && service.fillIndustrial(actual-tracked)!=actual-tracked)
            throw new IllegalStateException("Unexpected fresh coolant in burnt tank");
        if(actual==0 && coolant.getTankType()!=MayoRegistry.fluid("industrial_mayonnaise")) syncNativeTank();
    }
    private void syncNativeTank() {
        var type=MayoRegistry.fluid(service.burnt()?"burnt_mayonnaise":"industrial_mayonnaise");
        if(coolant.getTankType()!=type) coolant.setTankType(type);
        coolant.setFill(service.amount());markChanged();
    }
    @Override public FluidTankNTM[] getReceivingTanks() {
        return adapter && !service.burnt()?new FluidTankNTM[]{coolant}:new FluidTankNTM[0];
    }
    @Override public FluidTankNTM[] getSendingTanks() {
        return adapter && service.burnt()?new FluidTankNTM[]{coolant}:new FluidTankNTM[0];
    }
    @Override protected Component getDefaultName() {
        return Component.translatable(adapter?"block.hbmmayo.mayonnaise_coolant_adapter":"block.hbmmayo.flashgold_stabilization_chamber");
    }
    @Override public Component getDisplayName() { return getName(); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory player,Player user) {
        return new ChestMenu(MenuType.GENERIC_9x1,id,player,this,1);
    }
    @Override public boolean canPlaceItem(int slot,net.minecraft.world.item.ItemStack stack) {
        if(slot==0) return !adapter && IBatteryItem.isBattery(stack);
        return adapter && slot==1 && FluidTankNTM.isFluidContainer(stack);
    }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return adapter?new int[]{0,1,2}:new int[]{0}; }
    @Override public boolean canTakeItemThroughFace(int slot,net.minecraft.world.item.ItemStack stack,net.minecraft.core.Direction side) {
        return slot==2 || slot==0;
    }
    private String snapshot() {
        if(adapter) reconcileService();
        return MachineStateCodec.encode(power,service);
    }
    private void restore(String encoded) {
        var saved=MachineStateCodec.decode(encoded);power=saved.power();service=saved.coolant();
        if(adapter) syncNativeTank();
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);output.putString("mayoState",snapshot());
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);input.getString("mayoState").ifPresent(this::restore);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);builder.set(MayoRegistry.MACHINE_STATE.get(),snapshot());
    }
    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        String saved=components.get(MayoRegistry.MACHINE_STATE.get());if(saved!=null) restore(saved);
    }
    @Override public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);output.discard("mayoState");
    }
}
