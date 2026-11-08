package dune.holtzman;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.List;

/** Eating Processed Melange: spice effects, title, sounds, overdose bookkeeping. Spice sight itself runs in KubeJS and reads dune_melange_until. */
@EventBusSubscriber(modid = CreateHeat.MODID)
public class SpiceEvents {
    static final long DOSE_WINDOW = 36000, EFFECT_TIME = 6000;

    @SubscribeEvent
    public static void onFinish(LivingEntityUseItemEvent.Finish e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(e.getItem().getItem());
            if (!id.toString().equals("minecraft:processed_melange")) return;
            long now = p.level().getGameTime();
            CompoundTag pd = p.getPersistentData();
            List<Long> times = new ArrayList<>();
            for (String s : pd.getString("dune_doses").split(",")) {
                if (s.isEmpty()) continue;
                try { long t = Long.parseLong(s); if (now - t < DOSE_WINDOW) times.add(t); } catch (NumberFormatException ignored) { }
            }
            times.add(now);
            StringBuilder sb = new StringBuilder();
            for (Long t : times) { if (sb.length() > 0) sb.append(','); sb.append(t); }
            pd.putString("dune_doses", sb.toString());
            pd.putLong("dune_melange_until", now + EFFECT_TIME);

            p.sendSystemMessage(Component.literal("The spice opens your eyes.").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE0873A))));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, (int) EFFECT_TIME, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) EFFECT_TIME, 1, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) EFFECT_TIME, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
            title(p, "✵", "The spice opens your eyes", 0xE0A030);
            sound(p, "dune.spice_wave", 1.0f, 1.0f);
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.8f);
            if (times.size() >= 3) {
                pd.putBoolean("dune_overdose", true);
                pd.putLong("dune_od_at", now);
                title(p, "☠", "Melange poisoning. Drink milk.", 0xAA0000);
                overdose(p);
            }
        } catch (Throwable t) {
            CreateHeat.LOGGER.error("melange eaten", t);
        }
    }

    static void overdose(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1200, 9, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 1, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 2, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, false, true));
    }

    static void title(ServerPlayer p, String main, String sub, int rgb) {
        Style st = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 40));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(st)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(main).withStyle(st)));
    }

    static void sound(ServerPlayer p, String path, float vol, float pitch) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(path));
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), ev, SoundSource.PLAYERS, vol, pitch);
    }
}
