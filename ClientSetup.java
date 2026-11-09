package dune.holtzman;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid = CreateHeat.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    static { SpicePartials.init(); }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(CreateHeat.SPICE_MOTOR_BE.get(), SpiceMotorRenderer::new);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(CreateHeat.SPICE_BURNER_MENU.get(), SpiceBurnerScreen::new);
    }

    /** The shield is invisible and must not raise the holding arm: report the empty-hand pose. */
    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent e) {
        e.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.EMPTY;
            }
        }, CreateHeat.SHIELD.get());
    }
}
