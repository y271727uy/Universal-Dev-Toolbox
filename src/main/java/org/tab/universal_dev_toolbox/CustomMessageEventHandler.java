package org.tab.universal_dev_toolbox;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, value = Dist.CLIENT)
public class CustomMessageEventHandler {
    private static boolean messagesSent = false;
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 确保只在游戏初始化后发送一次消息
        if (!messagesSent && event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null) {
            sendCustomMessages();
            messagesSent = true;
        }
    }
    
    private static void sendCustomMessages() {
        // 获取配置中的自定义消息列表
        List<? extends String> customMessages = NewPopUpWindowConfig.CUSTOM_POPUP_MESSAGES.get();
        
        // 发送每条消息
        for (String message : customMessages) {
            if (!message.isEmpty()) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
            }
        }
    }
}