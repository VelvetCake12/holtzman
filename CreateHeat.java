package dune.holtzman;

import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mod(CreateHeat.MODID)
public class CreateHeat {
    public static final String MODID = "createheat";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<Item, HoltzmanShieldItem> SHIELD =
            ITEMS.register("holtzman_shield", () -> new HoltzmanShieldItem(new Item.Properties().durability(400)));

    public static final DeferredHolder<Block, SpiceMotorBlock> SPICE_MOTOR =
            BLOCKS.register("spice_motor", () -> new SpiceMotorBlock(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().strength(4.0f, 8.0f)));
    public static final DeferredHolder<Item, BlockItem> SPICE_MOTOR_ITEM = ITEMS.registerSimpleBlockItem("spice_motor", SPICE_MOTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiceMotorBlockEntity>> SPICE_MOTOR_BE =
            BLOCK_ENTITIES.register("spice_motor", () -> BlockEntityType.Builder.of(SpiceMotorBlockEntity::new, SPICE_MOTOR.get()).build(null));

    public static final DeferredHolder<Block, SpiceBurnerBlock> SPICE_BURNER =
            BLOCKS.register("spice_burner", () -> new SpiceBurnerBlock(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5f, 6.0f)
                    .lightLevel(s -> s.getValue(SpiceBurnerBlock.HEAT) == BlazeBurnerBlock.HeatLevel.NONE ? 0 : 13)));
    public static final DeferredHolder<Item, BlockItem> SPICE_BURNER_ITEM = ITEMS.registerSimpleBlockItem("spice_burner", SPICE_BURNER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiceBurnerBlockEntity>> SPICE_BURNER_BE =
            BLOCK_ENTITIES.register("spice_burner", () -> BlockEntityType.Builder.of(SpiceBurnerBlockEntity::new, SPICE_BURNER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SpiceBurnerMenu>> SPICE_BURNER_MENU =
            MENUS.register("spice_burner", () -> new MenuType<>(SpiceBurnerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Every item of every metal, in creative-tab order. */
    public static final List<Supplier<? extends Item>> METAL_ITEMS = new java.util.ArrayList<>();

    private record MetalDef(String id, int toolUses, float toolSpeed, float toolDamage, int ench, int armorMult,
                            int boots, int legs, int chest, int helmet, float toughness, float knockback,
                            net.minecraft.tags.TagKey<Block> incorrect) {}

    private static final MetalDef[] METALS = {
            new MetalDef("platinum", 900, 7.0f, 2.5f, 18, 24, 3, 6, 8, 3, 1.0f, 0.0f, BlockTags.INCORRECT_FOR_IRON_TOOL),
            new MetalDef("titanium", 2000, 8.0f, 3.5f, 12, 40, 3, 6, 8, 3, 3.0f, 0.05f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
            new MetalDef("rosegold", 450, 11.0f, 1.5f, 28, 14, 2, 5, 6, 2, 0.0f, 0.0f, BlockTags.INCORRECT_FOR_IRON_TOOL),
    };

    static {
        for (MetalDef m : METALS) registerMetal(m);
    }

    private static void registerMetal(MetalDef m) {
        DeferredHolder<Item, Item> ingot = ITEMS.register(m.id() + "_ingot", () -> new Item(new Item.Properties()));
        DeferredHolder<Item, Item> nugget = ITEMS.register(m.id() + "_nugget", () -> new Item(new Item.Properties()));
        DeferredHolder<Block, Block> block = BLOCKS.register(m.id() + "_block", () -> new Block(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
        DeferredHolder<Item, BlockItem> blockItem = ITEMS.registerSimpleBlockItem(m.id() + "_block", block);
        Supplier<Ingredient> repair = () -> Ingredient.of(ingot.get());
        Tier tier = new SimpleTier(m.incorrect(), m.toolUses(), m.toolSpeed(), m.toolDamage(), m.ench(), repair);

        Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
        def.put(ArmorItem.Type.BOOTS, m.boots()); def.put(ArmorItem.Type.LEGGINGS, m.legs());
        def.put(ArmorItem.Type.CHESTPLATE, m.chest()); def.put(ArmorItem.Type.HELMET, m.helmet());
        DeferredHolder<ArmorMaterial, ArmorMaterial> mat = ARMOR.register(m.id(), () -> new ArmorMaterial(def, m.ench(),
                SoundEvents.ARMOR_EQUIP_IRON, repair, List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, m.id()))),
                m.toughness(), m.knockback()));

        METAL_ITEMS.add(ingot); METAL_ITEMS.add(nugget); METAL_ITEMS.add(blockItem);
        METAL_ITEMS.add(ITEMS.register(m.id() + "_sword", () -> new SwordItem(tier, new Item.Properties().attributes(SwordItem.createAttributes(tier, 3, -2.4f)))));
        METAL_ITEMS.add(ITEMS.register(m.id() + "_pickaxe", () -> new PickaxeItem(tier, new Item.Properties().attributes(PickaxeItem.createAttributes(tier, 1.0f, -2.8f)))));
        METAL_ITEMS.add(ITEMS.register(m.id() + "_axe", () -> new AxeItem(tier, new Item.Properties().attributes(AxeItem.createAttributes(tier, 6.0f, -3.1f)))));
        METAL_ITEMS.add(ITEMS.register(m.id() + "_shovel", () -> new ShovelItem(tier, new Item.Properties().attributes(ShovelItem.createAttributes(tier, 1.5f, -3.0f)))));
        METAL_ITEMS.add(ITEMS.register(m.id() + "_hoe", () -> new HoeItem(tier, new Item.Properties().attributes(HoeItem.createAttributes(tier, -2.0f, -1.0f)))));
        for (ArmorItem.Type t : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            METAL_ITEMS.add(ITEMS.register(m.id() + "_" + t.getName(), () -> new ArmorItem(mat, t, new Item.Properties().durability(t.getDurability(m.armorMult())))));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .icon(() -> new ItemStack(SPICE_MOTOR_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(SHIELD.get());
                out.accept(SPICE_MOTOR_ITEM.get());
                out.accept(SPICE_BURNER_ITEM.get());
                for (Supplier<? extends Item> i : METAL_ITEMS) out.accept(i.get());
            }).build());

    public CreateHeat(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ARMOR.register(bus);
        MENUS.register(bus);
        bus.addListener((RegisterCapabilitiesEvent e) -> e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SPICE_BURNER_BE.get(), (be, side) -> be.inv));
        TABS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC, "createheat-server.toml");
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            BlockStressValues.CAPACITIES.register(SPICE_MOTOR.get(), () -> 128.0);
            BlockStressValues.RPM.register(SPICE_MOTOR.get(), new BlockStressValues.GeneratedRpm(128, false));
        }));
        bus.addListener((RegisterPayloadHandlersEvent e) ->
                e.registrar("1").playToClient(FxPayload.TYPE, FxPayload.CODEC, (p, c) -> ClientFx.handle(p, c)));
    }
}
