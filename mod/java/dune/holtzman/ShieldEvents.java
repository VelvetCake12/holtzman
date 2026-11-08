package dune.holtzman;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateHeat.MODID)
public class ShieldEvents {
    private static final String RED_UNTIL = "holtzman_red_until", ON_KEY = "holtzman_on", GLOW_UNTIL = "holtzman_glow_until";

    private static int rgbInt(String hex) {
        float[] c = Config.rgb(hex);
        return ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
    }

    private static void fx(ServerPlayer p, boolean active, String flashHex) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FxPayload(p.getId(), active, rgbInt(Config.COLOR_FIELD.get()),
                flashHex == null ? 0 : rgbInt(flashHex), Config.GLOW_TICKS.get()));
    }

    private static boolean held(Player p) {
        return p.getOffhandItem().is(CreateHeat.SHIELD.get());
    }

    private static DustParticleOptions dust(String hex, double scale) {
        float[] c = Config.rgb(hex);
        return new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), (float) scale);
    }

    private static void sound(ServerLevel l, Player p, SoundEvent s, float vol, float pitch) {
        l.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, (float) (vol * Config.VOLUME.get()), pitch);
    }

    private static void burst(ServerLevel l, Player p, String hex) { /* outline only: no particles */ }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !held(p)) return;
            DamageSource src = e.getSource();
            String id = src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (Config.IGNORED.get().contains(id) || src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FALL)
                    || src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_DROWNING) || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            Entity att = src.getEntity(), imm = src.getDirectEntity();
            boolean blocked = false, pierced = false;
            if (imm instanceof Projectile || (imm != null && imm != att)) blocked = Config.BLOCK_PROJECTILES.get();
            else if (att != null && imm == att && Config.BLOCK_MELEE.get()) {
                float cd = att instanceof Player pl ? pl.getAttackStrengthScale(0.5f) : 1f;
                if (cd < Config.FAST_BLOW.get()) blocked = true; else pierced = true;
            }
            ServerLevel l = p.serverLevel();
            if (pierced) {
                burst(l, p, Config.COLOR_PIERCED.get());
                sound(l, p, SoundEvents.BEACON_DEACTIVATE, 1f, Config.PITCH_PIERCE.get().floatValue());
                fx(p, true, Config.COLOR_PIERCED.get());
                return;
            }
            if (!blocked) return;
            ItemStack s = p.getOffhandItem();
            if (Config.DURABILITY_PER_BLOCK.get() > 0) s.hurtAndBreak(Config.DURABILITY_PER_BLOCK.get(), p, EquipmentSlot.OFFHAND);
            fx(p, true, Config.COLOR_FIELD.get());
            burst(l, p, Config.COLOR_GLIMMER.get());
            sound(l, p, SoundEvents.BEACON_POWER_SELECT, 1f, Config.PITCH_HIT.get().floatValue());
            e.setCanceled(true);
        } catch (Throwable t) {
            // never let the shield take the server down
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 2 != 0) return;
            ServerLevel l = p.serverLevel();
            boolean on = held(p), was = p.getPersistentData().getBoolean(ON_KEY);
            if (on != was) {
                p.getPersistentData().putBoolean(ON_KEY, on);
                fx(p, on, Config.COLOR_GLIMMER.get());
                burst(l, p, Config.COLOR_GLIMMER.get());
                sound(l, p, on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 1f, (on ? Config.PITCH_ON : Config.PITCH_OFF).get().floatValue());
                if (Config.STATUS_MESSAGE.get()) p.displayClientMessage(Component.literal(on ? "Holtzman field engaged" : "Holtzman field down"), true);
                return;
            }
            if (!on) return;
            if (p.tickCount % 40 == 0) fx(p, true, null);
            if (Config.HUM.get() && p.tickCount % 40 == 0) sound(l, p, SoundEvents.BEACON_AMBIENT, 0.35f, Config.PITCH_HUM.get().floatValue());
        } catch (Throwable t) {
            // ignore
        }
    }
}
