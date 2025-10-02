package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BadMobBiomeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MOB_BIOME_PAIRS;
    
    static {
        BUILDER.push("Bad Mob-Biome Configuration");
        
        BLACKLISTED_MOB_BIOME_PAIRS = BUILDER
                .comment("List of mob-biome pairs where the mob generation is disabled",
                        "Format: \"modid:entity_name;modid:biome_name\"",
                        "Example: \"minecraft:zombie;minecraft:plains\"")
                .defineListAllowEmpty("blacklisted_mob_biome_pairs", 
                        List.of(), // 默认为空列表
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