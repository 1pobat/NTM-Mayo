package dev.hbmmayo.neoforge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class StabilizerInjector extends Item {
    public StabilizerInjector(Properties p) { super(p); }
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand) {
        if(!(user instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if(!MayoRuntime.controller(player).consumeInjector(MayoRuntime.port(player),HbmMayonnaise.config))
            return InteractionResult.FAIL;
        if(!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }
}
