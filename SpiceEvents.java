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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
            if (id.toString().equals("minecraft:milk_bucket")) {
                CompoundTag mpd = p.getPersistentData();
                if (mpd.getBoolean("dune_overdose")) {
                    clearOverdose(p);
                    p.removeAllEffects();
                    p.sendSystemMessage(Component.literal("The milk settles the spice in your blood.").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE8E0C8))));
                }
                return;
            }
            if (id.toString().equals("minecraft:spice_bread")) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 0, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0, false, true));
                return;
            }
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
                if (!pd.getBoolean("dune_overdose")) pd.putLong("dune_od_at", now);
                pd.putBoolean("dune_overdose", true);
                title(p, "☠", "Melange poisoning. Drink milk.", 0xAA0000);
                overdose(p);
            }
        } catch (Throwable t) {
            CreateHeat.LOGGER.error("melange eaten", t);
        }
    }

    static final int INF = MobEffectInstance.INFINITE_DURATION;
    static final long DEATH_TICKS = 2400;   // 2 minutes after the fatal dose

    static void clearOverdose(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        pd.putBoolean("dune_overdose", false);
        pd.putString("dune_doses", "");
        pd.putLong("dune_od_at", 0L);
    }

    /** Effects never expire on their own: only milk or death removes them. */
    static void overdose(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, INF, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, INF, 9, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, INF, 2, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, INF, 1, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, INF, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, INF, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.POISON, INF, 1, false, true));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
            CompoundTag pd = p.getPersistentData();
            if (!pd.getBoolean("dune_overdose") || !p.isAlive()) return;
            long now = p.level().getGameTime();
            long left = DEATH_TICKS - (now - pd.getLong("dune_od_at"));
            if (left <= 0) {
                clearOverdose(p);
                p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
                return;
            }
            if (!p.hasEffect(MobEffects.WITHER)) overdose(p);   // re-apply if something removed them
            p.displayClientMessage(Component.literal("Melange poisoning. Drink milk. " + (left / 20) + "s").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAA0000))), true);
        } catch (Throwable t) {
            CreateHeat.LOGGER.error("melange tick", t);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) clearOverdose(p);
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
