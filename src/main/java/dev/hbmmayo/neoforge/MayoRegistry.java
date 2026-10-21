package dev.hbmmayo.neoforge;

import com.hbm.capability.FluidContainerRows;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.*;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.platform.NeoForgeAttachments;
import com.hbm.registration.*;
import com.mojang.serialization.Codec;
import dev.hbmmayo.core.InfusionState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import java.util.*;
import java.util.function.Supplier;

/** Uses the addon registration API verified in 1.0.3+1b8c6c2d. */
public final class MayoRegistry {
    private MayoRegistry() {}
    public static final String[] FLUIDS={"industrial_mayonnaise","flashgold_mayonnaise","mayonnaise_emulsion","flashgold_colloid","burnt_mayonnaise"};
    public static final Map<String,RegistryHandle<? extends Item>> ITEMS=new LinkedHashMap<>();
    public static Reg.Handle<MayoBlock> CHAMBER, ADAPTER;
    public static Reg.BeHandle<MayoBlockEntity> CHAMBER_ENTITY, ADAPTER_ENTITY;
    public static RegistryHandle<DataComponentType<String>> MACHINE_STATE;
    public static RegistryHandle<SoundEvent> ANNIHILATION_SOUND;
    public static Supplier<AttachmentType<InfusionState>> INFUSION;
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(HbmMayonnaise.ID,path); }
    public static Item item(String name) { return Objects.requireNonNull(ITEMS.get(name),name).get(); }
    public static Fluid fluid(String name) { return NTMFluids.byName(name.toUpperCase(Locale.ROOT)); }
    public static void register() {
        var reg=Reg.in(HbmMayonnaise.ID);
        int[] colors={0xebe089,0xf5bd28,0xf6e9bf,0xffcc18,0x472716};
        for(int i=0;i<FLUIDS.length;i++) {
            String name=FLUIDS[i].toUpperCase(Locale.ROOT);
            NTMFluids.register(HbmMayonnaise.ID,name,"EGG");
            new FluidBuilder(name,colors[i],0,0,0,EnumSymbol.NONE)
                .addTraits(FluidTraitSimple.FT_Liquid.INSTANCE,FluidTraitSimple.FT_Viscous.INSTANCE).declare();
        }
        for(String name:new String[]{"empty_mayonnaise_jar","shielded_mayonnaise_jar","empty_capsule",
                "flashgold_colloid_capsule","empty_injector","machine_frame"}) {
            ITEMS.put(name,reg.item(name,Item::new,Item.Properties::new));
        }
        ITEMS.put("industrial_mayonnaise_jar",reg.item("industrial_mayonnaise_jar",Item::new,Item.Properties::new));
        ITEMS.put("flashgold_mayonnaise_jar",reg.item("flashgold_mayonnaise_jar",FlashgoldJar::new,
            ()->new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(0)
                .saturationModifier(0).alwaysEdible().build(),Consumables.DEFAULT_DRINK)));
        ITEMS.put("flashgold_stabilizer_injector",reg.item("flashgold_stabilizer_injector",StabilizerInjector::new,
            ()->new Item.Properties().stacksTo(16)));
        // Both use the SAME native chest-only socket; armor table excludes simultaneous installation.
        ITEMS.put("passive_flashgold_stabilizer",reg.item("passive_flashgold_stabilizer",MayoArmorModule::new,
            ()->new Item.Properties().stacksTo(1)));
        ITEMS.put("entropy_reclaimer",reg.item("entropy_reclaimer",MayoArmorModule::new,
            ()->new Item.Properties().stacksTo(1)));
        INFUSION=NeoForgeAttachments.register(id("infusion"),InfusionState::new,
            Codec.STRING.xmap(StateCodec::decode,StateCodec::encode).fieldOf("state"),false);
        MACHINE_STATE=NeoForgeRegistrar.registerDataComponent("hbmmayo:machine_state",
            DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        ANNIHILATION_SOUND=NeoForgeRegistrar.registerSound("hbmmayo:flashgold_annihilation");
        CHAMBER=reg.blockItem("flashgold_stabilization_chamber",p->new MayoBlock(p,false),
            ()->BlockBehaviour.Properties.of().strength(4));
        ADAPTER=reg.blockItem("mayonnaise_coolant_adapter",p->new MayoBlock(p,true),
            ()->BlockBehaviour.Properties.of().strength(4));
        CHAMBER_ENTITY=reg.<MayoBlockEntity>be("flashgold_stabilization_chamber",()->new BlockEntityType<>(
            (pos,state)->new MayoBlockEntity(pos,state,false),Set.of(CHAMBER.get()))).ticksServer(MayoBlockEntity::tickServer);
        ADAPTER_ENTITY=reg.<MayoBlockEntity>be("mayonnaise_coolant_adapter",()->new BlockEntityType<>(
            (pos,state)->new MayoBlockEntity(pos,state,true),Set.of(ADAPTER.get()))).ticksServer(MayoBlockEntity::tickServer);
        FluidContainerRows.addon(new FluidContainerRows.Row(()->item("industrial_mayonnaise_jar"),null,
            ()->item("empty_mayonnaise_jar"),()->fluid("industrial_mayonnaise"),()->250));
        FluidContainerRows.addon(new FluidContainerRows.Row(()->item("flashgold_mayonnaise_jar"),null,
            ()->item("shielded_mayonnaise_jar"),()->fluid("flashgold_mayonnaise"),()->250));
        FluidContainerRows.addon(new FluidContainerRows.Row(()->item("flashgold_colloid_capsule"),null,
            ()->item("empty_capsule"),()->fluid("flashgold_colloid"),()->100));
        NeoForgeRegistrar.registerCreativeModeTab("hbmmayo:main",builder->builder
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.hbmmayo"))
            .icon(()->new ItemStack(item("flashgold_mayonnaise_jar")))
            .displayItems((parameters,output)->{
                ITEMS.values().forEach(handle->output.accept(new ItemStack(handle.get())));
                output.accept(new ItemStack(CHAMBER.asItem()));output.accept(new ItemStack(ADAPTER.asItem()));
            }));
    }
}
