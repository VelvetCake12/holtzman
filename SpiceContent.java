package dune.holtzman;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Spice items and the spice sand block. Registered in the minecraft namespace so ids stay identical to the baked worlds and old recipes. */
public class SpiceContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("minecraft");
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("minecraft");

    public static final DeferredHolder<Item, Item> MELANGE = ITEMS.registerSimpleItem("melange", new Item.Properties());
    public static final DeferredHolder<Item, Item> COMPACTED_MELANGE = ITEMS.registerSimpleItem("compacted_melange", new Item.Properties());
    public static final DeferredHolder<Item, Item> PROCESSED_MELANGE = ITEMS.registerSimpleItem("processed_melange",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible().build()));
    public static final DeferredHolder<Item, Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot", new Item.Properties());
    public static final DeferredHolder<Item, Item> PLASTEEL_INGOT = ITEMS.registerSimpleItem("plasteel_ingot", new Item.Properties());
    public static final DeferredHolder<Item, Item> SPICE_BREAD = ITEMS.registerSimpleItem("spice_bread",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build()));

    public static final DeferredHolder<Block, SpiceSandBlock> SPICE_SAND = BLOCKS.registerBlock("spice_sand", SpiceSandBlock::new,
            BlockBehaviour.Properties.of().strength(0.5f, 0.5f).sound(SoundType.SAND));
    public static final DeferredHolder<Item, BlockItem> SPICE_SAND_ITEM = ITEMS.registerSimpleBlockItem("spice_sand", SPICE_SAND);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
    }
}
