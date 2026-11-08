package dune.holtzman;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** Client side: keeps the field state of nearby players and paints a translucent coloured shell over their model. */
@EventBusSubscriber(modid = CreateHeat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientFx {
    static final class State { int flashRgb; long flashStart; }
    static final Map<Integer, State> STATES = new HashMap<>();
    static final int STEPS = 10, FADE_TICKS = 26;
    static final float MIN_ALPHA = 0.11f, PEAK_ALPHA = 0.6f;
    static final ResourceLocation[] FIELD = new ResourceLocation[STEPS];
    static { for (int i = 0; i < STEPS; i++) FIELD[i] = ResourceLocation.fromNamespaceAndPath(CreateHeat.MODID, "textures/misc/field_" + i + ".png"); }

    /** Only flashes are drawn: blocked (blue, flickers and shakes), pierced (red), equip / unequip (blue). Idle is invisible. */
    public static void handle(FxPayload m, IPayloadContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || m.flashRgb() == 0) return;
        State s = STATES.computeIfAbsent(m.entityId(), k -> new State());
        s.flashRgb = m.flashRgb();
        s.flashStart = mc.level.getGameTime();
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        for (var skin : e.getSkins()) {
            var r = e.getSkin(skin);
            if (r instanceof PlayerRenderer pr) pr.addLayer(new FieldLayer(pr));
        }
    }

    static final class FieldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        FieldLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p) { super(p); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buf, int light, AbstractClientPlayer pl, float ls, float lsa, float pt, float age, float yaw, float pitch) {
            State s = STATES.get(pl.getId());
            if (s == null) return;
            if (pl.isInvisible() || pl.isSpectator()) return;
            float t = (pl.level().getGameTime() - s.flashStart + pt) / FADE_TICKS;
            if (t >= 1f) { STATES.remove(pl.getId()); return; }
            float k = (1f - t) * (1f - t);
            var rnd = pl.getRandom();
            float alpha = PEAK_ALPHA * k * (0.45f + 0.55f * rnd.nextFloat());
            if (alpha < MIN_ALPHA) return;
            int step = Math.max(0, Math.min(STEPS - 1, Math.round((alpha - MIN_ALPHA) / (PEAK_ALPHA - MIN_ALPHA) * (STEPS - 1))));
            int argb = 0xFF000000 | (s.flashRgb & 0xFFFFFF);
            pose.pushPose();
            float j = 0.03f * k;
            pose.translate((rnd.nextFloat() - 0.5f) * j, (rnd.nextFloat() - 0.5f) * j, (rnd.nextFloat() - 0.5f) * j);
            var vc = buf.getBuffer(RenderType.entityTranslucent(FIELD[step]));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
            pose.popPose();
        }

        private static int lerp(int a, int b, float t) {
            int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
            return (r << 16) | (g << 8) | bl;
        }
    }
}
