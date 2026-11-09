package dune.holtzman;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

/** Spice sight: after eating Processed Melange the consumer sees people (red), creatures (green) and spice sand (orange). Particles go only to that player. */
@EventBusSubscriber(modid = CreateHeat.MODID)
public class SpiceSight {
    static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 2.5f);
    static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.2f, 1.0f, 0.2f), 2.5f);
    static final DustParticleOptions ORANGE = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.05f), 2.5f);
    static final Random RNG = new Random();

    static void beam(ServerLevel lv, ServerPlayer p, DustParticleOptions o, double x, double y, double z, int height, int step) {
        for (int h = 0; h < height; h += step) lv.sendParticles(p, o, true, x, y + h, z, 2, 0.1, 0.3, 0.1, 0.0);
    }

    static void sound(ServerPlayer p, String path, float vol) {
        p.playNotifySound(SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(path)), SoundSource.PLAYERS, vol, 1.0f);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
            CompoundTag pd = p.getPersistentData();
            ServerLevel lv = p.serverLevel();
            if (lv.getGameTime() >= pd.getLong("dune_melange_until")) return;

            int people = 0, beasts = 0;
            List<LivingEntity> near = lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(96, 60, 96), en -> en != p);
            for (LivingEntity en : near) {
                if (en instanceof Player) { people++; beam(lv, p, RED, en.getX(), en.getY() + 1, en.getZ(), 40, 2); }
                else if (beasts < 40) { beasts++; beam(lv, p, GREEN, en.getX(), en.getY() + 1, en.getZ(), 16, 2); }
            }
            if (p.tickCount % 20 == 0) {
                int found = 0;
                BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
                for (int k = 0; k < 900 && found < 40; k++) {
                    mp.set(Math.floor(p.getX() + (RNG.nextDouble() - 0.5) * 128), Math.floor(p.getY() + (RNG.nextDouble() - 0.5) * 50), Math.floor(p.getZ() + (RNG.nextDouble() - 0.5) * 128));
                    if (!lv.isLoaded(mp)) continue;
                    if (lv.getBlockState(mp).is(SpiceContent.SPICE_SAND.get())) { found++; beam(lv, p, ORANGE, mp.getX() + 0.5, mp.getY() + 1.2, mp.getZ() + 0.5, 14, 2); }
                }
                HitResult hr = p.pick(8.0, 0.0f, false);
                String msg;
                if (hr instanceof BlockHitResult bhr && lv.getBlockState(bhr.getBlockPos()).getBlock() instanceof CropBlock crop) {
                    BlockState st = lv.getBlockState(bhr.getBlockPos());
                    int age = crop.getAge(st), max = crop.getMaxAge();
                    msg = "Growth " + age + "/" + max + (age >= max ? " (ripe)" : "");
                    p.displayClientMessage(Component.literal(msg).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9AD06A))), true);
                } else if (people + beasts > 0) {
                    p.displayClientMessage(Component.literal(people + " people  " + beasts + " creatures nearby").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE0873A))), true);
                }
            }
            if (p.tickCount % 200 == 0) sound(p, "dune.spice_hum", 0.6f);
            if (p.tickCount % 600 == 0) sound(p, "dune.spice_whisper", 0.5f);
        } catch (Throwable t) {
            CreateHeat.LOGGER.error("spice sight", t);
        }
    }
}
