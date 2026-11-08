package dune.holtzman;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HoltzmanMod.MODID)
public class HoltzmanMod {
    public static final String MODID = "holtzman";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredHolder<Item, HoltzmanShieldItem> SHIELD =
            ITEMS.register("holtzman_shield", () -> new HoltzmanShieldItem(new Item.Properties().durability(400)));

    public static final DeferredHolder<SoundEvent, SoundEvent> HIT = sound("shield_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIERCE = sound("shield_pierce");
    public static final DeferredHolder<SoundEvent, SoundEvent> ON = sound("shield_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> OFF = sound("shield_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUM = sound("shield_hum");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, name)));
    }

    public HoltzmanMod(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        SOUNDS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC, "holtzman-server.toml");
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.COMBAT) e.accept(SHIELD.get());
        });
    }
}
