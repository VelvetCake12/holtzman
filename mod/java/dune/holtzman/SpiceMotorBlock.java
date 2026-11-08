package dune.holtzman;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Burns Melange to turn a shaft. Same body as the Creative Motor; speed is capped at 128 RPM and it needs fuel. */
public class SpiceMotorBlock extends DirectionalKineticBlock implements IBE<SpiceMotorBlockEntity> {
    public static final int MELANGE_TICKS = 6000, COMPACTED_TICKS = 60000, MAX_FUEL = 144000;

    public SpiceMotorBlock(Properties props) {
        super(props);
    }

    public static int fuelValue(ItemStack s) {
        ResourceLocation k = BuiltInRegistries.ITEM.getKey(s.getItem());
        if (!k.getNamespace().equals("minecraft")) return 0;
        return switch (k.getPath()) {
            case "melange" -> MELANGE_TICKS;
            case "compacted_melange" -> COMPACTED_TICKS;
            default -> 0;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return AllShapes.MOTOR_BLOCK.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? facing : facing.getOpposite());
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int add = fuelValue(stack);
        if (add <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        SpiceMotorBlockEntity be = getBlockEntity(level, pos);
        if (be == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (be.fuel + add > MAX_FUEL) {
            player.displayClientMessage(Component.literal("The motor is full."), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        be.addFuel(add);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.9f, 0.7f);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.6f);
        player.displayClientMessage(Component.literal("Spice Motor: " + be.fuel / 1200 + " min of fuel"), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        SpiceMotorBlockEntity be = getBlockEntity(level, pos);
        if (be == null) return InteractionResult.PASS;
        player.displayClientMessage(Component.literal(be.fuel > 0 ? "Spice Motor: " + be.fuel / 1200 + " min of fuel" : "Spice Motor: out of fuel. Feed it Melange."), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public Class<SpiceMotorBlockEntity> getBlockEntityClass() {
        return SpiceMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SpiceMotorBlockEntity> getBlockEntityType() {
        return CreateHeat.SPICE_MOTOR_BE.get();
    }
}
