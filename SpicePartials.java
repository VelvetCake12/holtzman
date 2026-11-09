package dune.holtzman;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/** Client-only: moving pistons of the Spice Engine. Loaded from ClientSetup's static initialiser so they register before models bake. */
public final class SpicePartials {
    public static final PartialModel PISTON_L = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateHeat.MODID, "block/spice_motor/piston_l"));
    public static final PartialModel PISTON_R = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateHeat.MODID, "block/spice_motor/piston_r"));

    private SpicePartials() {}

    public static void init() {}
}
