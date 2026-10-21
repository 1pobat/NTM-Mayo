package dev.hbmmayo.neoforge;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ItemArmorMod;
import net.minecraft.world.item.Item;

public final class MayoArmorModule extends ItemArmorMod {
    public MayoArmorModule(Item.Properties properties) {
        super(properties,ArmorModHandler.PLATE_ONLY,false,true,false,false);
    }
}
