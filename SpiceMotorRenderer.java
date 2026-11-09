package dune.holtzman;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SpiceMotorRenderer extends KineticBlockEntityRenderer<SpiceMotorBlockEntity> {
    private static final float PX = 1f / 16f;

    public SpiceMotorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(SpiceMotorBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
    }

    @Override
    protected void renderSafe(SpiceMotorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        if (be.getLevel() == null) return;
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(SpiceMotorBlock.FACING);
        float angle = getAngleForBe(be, be.getBlockPos(), facing.getAxis());
        float p = (1f + (float) Math.sin(angle)) * 0.5f;
        int lt = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        // left: A in phase p, B opposite; right: mirrored phase. Travel is 1 pixel inward.
        piston(PartialModelHolder.L, state, facing, +p * PX, 0f, lt, ms, vb);
        piston(PartialModelHolder.L, state, facing, +(1f - p) * PX, 7f * PX, lt, ms, vb);
        piston(PartialModelHolder.R, state, facing, -(1f - p) * PX, 0f, lt, ms, vb);
        piston(PartialModelHolder.R, state, facing, -p * PX, 7f * PX, lt, ms, vb);
    }

    private static void piston(PartialModel model, BlockState state, Direction facing, float dx, float dz, int light, PoseStack ms, VertexConsumer vb) {
        SuperByteBuffer b = CachedBuffers.partial(model, state);
        b.translate(0.5f, 0.5f, 0.5f);
        b.rotateToFace(facing);
        b.translate(dx, 0f, dz);
        b.translate(-0.5f, -0.5f, -0.5f);
        b.light(light).renderInto(ms, vb);
    }

    private static final class PartialModelHolder {
        static final PartialModel L = SpicePartials.PISTON_L, R = SpicePartials.PISTON_R;
    }
}
