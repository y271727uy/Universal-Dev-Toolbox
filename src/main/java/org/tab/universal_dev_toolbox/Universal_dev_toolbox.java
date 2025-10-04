package org.tab.universal_dev_toolbox;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Universal_dev_toolbox.MODID)
public class Universal_dev_toolbox {

    public static final String MODID = "universal_dev_toolbox";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Universal_dev_toolbox() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        
        // Register configs with unique filenames to prevent conflicts
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "universal_dev_toolbox/universal_dev_toolbox-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecipeNameConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-recipe_name.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MobBiomeConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-mob-biome.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BadMobBiomeConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-bad_mob-biome.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MCreatorBlacklistConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-mcreator-black_list.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MCreatorWhitelistConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-mcreator-white_list.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BadPopUpWindowConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-bad_pop-up-window.toml");
        // ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BadModIdConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NewPopUpWindowConfig.SPEC, "universal_dev_toolbox/universal_dev_toolbox-new_pop-up_window.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", Config.items);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        UDTCommand.register(event.getDispatcher());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}