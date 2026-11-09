package dune.holtzman;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SpiceMotorBlockEntity extends GeneratingKineticBlockEntity {
    public static final int MAX_RPM = 128;
    public ScrollValueBehaviour generatedSpeed;
    public int fuel;

    public SpiceMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SpiceMotorBlockEntity(BlockPos pos, BlockState state) {
        this(CreateHeat.SPICE_MOTOR_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        generatedSpeed = new KineticScrollValueBehaviour(CreateLang.translateDirect("kinetics.creative_motor.rotation_speed"), this, new Box());
        generatedSpeed.between(-MAX_RPM, MAX_RPM);
        generatedSpeed.value = 16;
        generatedSpeed.withCallback(i -> this.updateGeneratedRotation());
        behaviours.add(generatedSpeed);
    }

    @Override
    public float getGeneratedSpeed() {
        if (fuel <= 0 || generatedSpeed == null) return 0;
        return convertToDirection(generatedSpeed.getValue(), getBlockState().getValue(SpiceMotorBlock.FACING));
    }

    public void addFuel(int ticks) {
        boolean was = fuel > 0;
        fuel = Math.min(fuel + ticks, SpiceMotorBlock.MAX_FUEL);
        if (!was) updateGeneratedRotation();
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || fuel <= 0) return;
        fuel = Math.max(0, fuel - burnRate());
        if (fuel == 0) {
            updateGeneratedRotation();
            setChanged();
            sendData();
        } else if (level.getGameTime() % 10 == 0 && level instanceof ServerLevel sl) {
            Direction up = Direction.UP;
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 0, 0.0, 1.0, 0.0, 0.06);
        }
    }

    /** Fuel ticks burned per game tick: 1 at 16 RPM, 8 at 128 RPM. */
    public int burnRate() {
        if (generatedSpeed == null) return 1;
        return Math.max(1, Math.abs(generatedSpeed.getValue()) / 16);
    }

    @Override
    public boolean addToGoggleTooltip(List<net.minecraft.network.chat.Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        int secs = fuel / burnRate() / 20;
        tooltip.add(net.minecraft.network.chat.Component.literal("    Spice Engine").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (fuel <= 0) {
            tooltip.add(net.minecraft.network.chat.Component.literal("    Out of fuel").withStyle(net.minecraft.ChatFormatting.RED));
        } else {
            String t = secs >= 60 ? (secs / 60) + "m " + (secs % 60) + "s" : secs + "s";
            tooltip.add(net.minecraft.network.chat.Component.literal("    Fuel remaining: " + t + " at " + Math.abs(generatedSpeed.getValue()) + " RPM").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        return true;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Fuel", fuel);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuel = tag.getInt("Fuel");
    }

    private static class Box extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction dir) {
            Direction facing = state.getValue(SpiceMotorBlock.FACING);
            if (facing.getAxis() == Direction.Axis.Y) return dir == Direction.NORTH;
            return dir == Direction.UP;
        }
    }
}
