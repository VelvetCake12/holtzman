package dune.holtzman;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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

@EventBusSubscriber(modid = HoltzmanMod.MODID)
public class ShieldEvents {
    private static final String RED_UNTIL = "holtzman_red_until", ON_KEY = "holtzman_on";

    private static boolean held(Player p) {
        return p.getOffhandItem().is(HoltzmanMod.SHIELD.get());
    }

    private static DustParticleOptions dust(String hex, double scale) {
        float[] c = Config.rgb(hex);
        return new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), (float) scale);
    }

    private static void sound(ServerLevel l, Player p, SoundEvent s, float vol, float pitch) {
        l.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, (float) (vol * Config.VOLUME.get()), pitch);
    }

    private static void burst(ServerLevel l, Player p, String hex) {
        DustParticleOptions o = dust(hex, Config.PARTICLE_SCALE.get() * 1.3);
        int n = Config.GLIMMER_PARTICLES.get();
        for (int i = 0; i < n; i++) {
            double a = l.random.nextDouble() * Math.PI * 2, h = l.random.nextDouble() * 1.9, r = 0.7;
            l.sendParticles(o, p.getX() + Math.cos(a) * r, p.getY() + 0.05 + h, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), n / 3, 0.5, 0.8, 0.5, 0.05);
    }

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
                sound(l, p, HoltzmanMod.PIERCE.get(), 1f, 1f);
                p.getPersistentData().putLong(RED_UNTIL, l.getGameTime() + Config.PIERCE_FLASH_TICKS.get());
                return;
            }
            if (!blocked) return;
            ItemStack s = p.getOffhandItem();
            if (Config.DURABILITY_PER_BLOCK.get() > 0) s.hurtAndBreak(Config.DURABILITY_PER_BLOCK.get(), p, EquipmentSlot.OFFHAND);
            burst(l, p, Config.COLOR_GLIMMER.get());
            sound(l, p, HoltzmanMod.HIT.get(), 1f, 1f);
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
                burst(l, p, Config.COLOR_GLIMMER.get());
                sound(l, p, on ? HoltzmanMod.ON.get() : HoltzmanMod.OFF.get(), 1f, 1f);
                if (Config.STATUS_MESSAGE.get()) p.displayClientMessage(Component.literal(on ? "Holtzman field engaged" : "Holtzman field down"), true);
                return;
            }
            if (!on) return;
            if (Config.OUTLINE.get()) {
                boolean red = l.getGameTime() < p.getPersistentData().getLong(RED_UNTIL);
                DustParticleOptions o = dust(red ? Config.COLOR_PIERCED.get() : Config.COLOR_FIELD.get(), Config.PARTICLE_SCALE.get());
                double ph = p.tickCount * 0.25;
                for (int i = 0; i < 4; i++) {
                    double a = ph + i * Math.PI / 2;
                    for (double h = 0.1; h < 1.9; h += 0.9)
                        l.sendParticles(o, p.getX() + Math.cos(a) * 0.55, p.getY() + h, p.getZ() + Math.sin(a) * 0.55, 1, 0, 0, 0, 0);
                }
            }
            if (Config.HUM.get() && p.tickCount % 40 == 0) sound(l, p, HoltzmanMod.HUM.get(), 0.35f, 1f);
        } catch (Throwable t) {
            // ignore
        }
    }
}
