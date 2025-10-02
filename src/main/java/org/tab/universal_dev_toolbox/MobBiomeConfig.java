package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobBiomeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.BooleanValue ENABLE_WHITELIST_CLEANUP;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELISTED_MOB_BIOME_PAIRS;
    
    static {
        BUILDER.push("Mob-Biome Whitelist Configuration");
        
        ENABLE_WHITELIST_CLEANUP = BUILDER
                .comment("Enable automatic cleanup of mobs that are not in their allowed biomes")
                .define("enable_whitelist_cleanup", true);
        
        WHITELISTED_MOB_BIOME_PAIRS = BUILDER
                .comment("List of mob-biome pairs where the mob generation is exclusively allowed",
                        "Format: \"modid:entity_name;modid:biome_name\"",
                        "Example: \"minecraft:skeleton;minecraft:nether_wastes\"")
                .defineListAllowEmpty("whitelisted_mob_biome_pairs", 
                        List.of(),
                        obj -> obj instanceof String);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            MobSpawnEventHandler.parseConfigs();
        }
    }
}