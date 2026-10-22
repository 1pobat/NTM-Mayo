package dev.hbmmayo.neoforge;

import com.hbm.capability.*;
import com.hbm.registration.Reg;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MayoBlock extends Block implements EntityBlock,ICapabilityBlock {
    private final boolean adapter;
    public MayoBlock(Properties properties,boolean adapter) { super(properties);this.adapter=adapter; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new MayoBlockEntity(pos,state,adapter);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return Reg.ticker(type,level.isClientSide());
    }
    @Override public MachineCaps caps() {
        var caps=MachineCaps.of(adapter?MayoRegistry.ADAPTER_ENTITY:MayoRegistry.CHAMBER_ENTITY).items();
        return adapter?caps.fluidIn().fluidOut():caps.powerIn();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player user,BlockHitResult hit) {
        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof MayoBlockEntity machine) {
            if(user.isSecondaryUseActive()) user.sendSystemMessage(Component.translatable(
                "message.hbmmayo.machine_status",machine.getPower(),machine.coolantAmount(),machine.serviceTicks()));
            else user.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }
}
