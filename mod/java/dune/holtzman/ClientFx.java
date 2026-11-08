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
    static final class State { boolean active; int fieldRgb; int flashRgb; long flashStart; int flashTicks; }
    static final Map<Integer, State> STATES = new HashMap<>();
    static final float FAINT_ALPHA = 0.16f, PEAK_ALPHA = 0.7f;
    static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public static void handle(FxPayload m, IPayloadContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!m.active() && m.flashRgb() == 0) { STATES.remove(m.entityId()); return; }
        State s = STATES.computeIfAbsent(m.entityId(), k -> new State());
        s.active = m.active(); s.fieldRgb = m.fieldRgb();
        if (m.flashRgb() != 0) { s.flashRgb = m.flashRgb(); s.flashStart = mc.level.getGameTime(); s.flashTicks = Math.max(1, m.flashTicks()); }
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
            float t = 1f;
            if (s.flashRgb != 0) t = Math.min(1f, (pl.level().getGameTime() - s.flashStart + pt) / s.flashTicks);
            if (t >= 1f) s.flashRgb = 0;
            float base = s.active ? FAINT_ALPHA : 0f;
            float alpha = base + (PEAK_ALPHA - base) * (1f - t) * (1f - t);
            int rgb = s.fieldRgb;
            if (s.flashRgb != 0) rgb = lerp(s.flashRgb, s.fieldRgb, t);
            if (alpha <= 0.01f) { if (!s.active && s.flashRgb == 0) STATES.remove(pl.getId()); return; }
            int argb = ((int) (alpha * 255) << 24) | (rgb & 0xFFFFFF);
            var vc = buf.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
        }

        private static int lerp(int a, int b, float t) {
            int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
            return (r << 16) | (g << 8) | bl;
        }
    }
}
