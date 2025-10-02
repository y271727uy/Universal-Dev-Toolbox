package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MCreatorBlacklistConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MODS;
    
    static {
        BUILDER.push("MCreator Blacklist Configuration");
        
        BLACKLISTED_MODS = BUILDER
                .comment("List of mod IDs that should always be treated as MCreator mods",
                        "Format: \"modid\"",
                        "Example: \"examplemod\"")
                .defineListAllowEmpty("blacklisted_mods", 
                        List.of(),
                        obj -> obj instanceof String);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            // Configuration loaded
        }
    }
}