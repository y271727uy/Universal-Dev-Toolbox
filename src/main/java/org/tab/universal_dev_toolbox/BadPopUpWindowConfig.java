package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BadPopUpWindowConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BAD_POPUP_CONTENTS;
    
    static {
        BUILDER.push("Bad Pop-up Window Configuration");
        
        BAD_POPUP_CONTENTS = BUILDER
                .comment("List of strings that should be blocked in system chat messages",
                        "Any system message containing these strings will be suppressed",
                        "Format: \"string_to_block\"",
                        "Example: \"关注\"")
                .defineListAllowEmpty("bad_popup_contents", 
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