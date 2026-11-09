package dune.holtzman;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpiceSandBlock extends FallingBlock {
    public static final MapCodec<SpiceSandBlock> CODEC = simpleCodec(SpiceSandBlock::new);

    public SpiceSandBlock(Properties p) { super(p); }

    @Override
    protected MapCodec<? extends FallingBlock> codec() { return CODEC; }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) { return 0xD98A3A; }
}
