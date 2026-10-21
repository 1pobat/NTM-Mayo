package dev.hbmmayo.neoforge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class FlashgoldJar extends Item {
    public FlashgoldJar(Properties p) { super(p); }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server && MayoRuntime.state(server).active()) return InteractionResult.FAIL;
        return super.use(level,player,hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user) {
        if(!(user instanceof ServerPlayer player)) return stack;
        var controller=MayoRuntime.controller(player);
        if(!controller.consumeFlashgoldJar(MayoRuntime.port(player),HbmMayonnaise.config)) return stack;
        // Do not invoke super: a second consumption callback must not eat another jar.
        if(!player.getAbilities().instabuild) {
            stack.shrink(1);
            var empty=new ItemStack(MayoRegistry.item("shielded_mayonnaise_jar"));
            if(stack.isEmpty()) return empty;
            player.getInventory().placeItemBackInInventory(empty);
        }
        return stack;
    }
}
