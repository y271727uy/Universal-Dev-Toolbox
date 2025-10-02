package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MCreatorWhitelistConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELISTED_MODS;
    
    static {
        BUILDER.push("MCreator Whitelist Configuration");
        
        WHITELISTED_MODS = BUILDER
                .comment("List of mod IDs that should be skipped during MCreator mod detection",
                        "Format: \"modid\"",
                        "Example: \"examplemod\"")
                .defineListAllowEmpty("whitelisted_mods", 
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