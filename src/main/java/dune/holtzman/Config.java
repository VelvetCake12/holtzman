package dune.holtzman;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Every number and colour of the shield lives here: config/holtzman-server.toml (world/serverconfig on a server). */
public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BLOCK_PROJECTILES = B.comment("Turn aside arrows, tridents, fireballs and other projectiles").define("blockProjectiles", true);
    public static final ModConfigSpec.BooleanValue BLOCK_MELEE = B.comment("Turn aside fast melee blows").define("blockMelee", true);
    public static final ModConfigSpec.DoubleValue FAST_BLOW = B.comment("Player attack charge below this value counts as a fast blow (turned aside). At or above it the blow pierces.")
            .defineInRange("fastBlowThreshold", 0.75, 0.0, 1.0);
    public static final ModConfigSpec.IntValue DURABILITY_PER_BLOCK = B.comment("Durability lost per absorbed hit").defineInRange("durabilityPerBlock", 1, 0, 400);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED = B.comment("Damage type paths the shield never affects (e.g. explosion, fall, magic)")
            .defineListAllowEmpty("ignoredDamageTypes", List.of("explosion", "player_explosion", "fall", "fire", "in_fire", "on_fire", "lava", "magic", "wither", "starve", "drown", "out_of_world", "generic_kill", "freeze", "cactus", "hot_floor", "indirect_magic"), () -> "x", o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue OUTLINE = B.comment("Show the rotating outline around the holder").define("outline", true);
    public static final ModConfigSpec.ConfigValue<String> COLOR_FIELD = B.comment("Outline colour (hex)").define("fieldColor", "#4099FF");
    public static final ModConfigSpec.ConfigValue<String> COLOR_PIERCED = B.comment("Outline colour while pierced (hex)").define("piercedColor", "#FF0D0D");
    public static final ModConfigSpec.ConfigValue<String> COLOR_GLIMMER = B.comment("Colour of the burst on hit / equip (hex)").define("glimmerColor", "#B3F2FF");
    public static final ModConfigSpec.IntValue GLIMMER_PARTICLES = B.comment("Particles in a glimmer burst").defineInRange("glimmerParticles", 60, 0, 400);
    public static final ModConfigSpec.DoubleValue PARTICLE_SCALE = B.defineInRange("particleScale", 1.2, 0.2, 4.0);
    public static final ModConfigSpec.DoubleValue VOLUME = B.comment("Master volume of shield sounds").defineInRange("volume", 1.0, 0.0, 2.0);
    public static final ModConfigSpec.BooleanValue HUM = B.comment("Play the quiet hum while the field is up").define("hum", true);
    public static final ModConfigSpec.BooleanValue STATUS_MESSAGE = B.comment("Action-bar message on equip / unequip").define("statusMessage", true);
    public static final ModConfigSpec.IntValue PIERCE_FLASH_TICKS = B.defineInRange("pierceFlashTicks", 14, 1, 200);

    public static final ModConfigSpec SPEC = B.build();

    public static float[] rgb(String hex) {
        try {
            int v = Integer.parseInt(hex.replace("#", ""), 16);
            return new float[]{((v >> 16) & 255) / 255f, ((v >> 8) & 255) / 255f, (v & 255) / 255f};
        } catch (Exception e) {
            return new float[]{0.25f, 0.6f, 1f};
        }
    }
}
