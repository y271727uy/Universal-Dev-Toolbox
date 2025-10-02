package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NewPopUpWindowConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_POPUP_MESSAGES;
    
    static {
        BUILDER.push("New Pop-up Window Configuration");
        
        CUSTOM_POPUP_MESSAGES = BUILDER
                .comment("List of custom popup messages to display",
                        "These messages will be shown as system chat messages when the game starts",
                        "Format: \"message\"",
                        "Example: \"Welcome to our modpack!\"",
                        "支持中文消息 (Supports Chinese messages)")
                .defineListAllowEmpty("custom_popup_messages", 
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