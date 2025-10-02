package org.tab.universal_dev_toolbox;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID, value = Dist.CLIENT)
public class SystemChatEventHandler {
    
    @SubscribeEvent
    public static void onSystemChatReceived(ClientChatReceivedEvent.System event) {
        // 获取消息内容
        Component messageComponent = event.getMessage();
        String messageText = messageComponent.getString();
        
        // 获取配置中的屏蔽词列表
        List<? extends String> badContents = BadPopUpWindowConfig.BAD_POPUP_CONTENTS.get();
        
        // 检查消息是否包含任何屏蔽词
        for (String badContent : badContents) {
            if (!badContent.isEmpty() && messageText.contains(badContent)) {
                // 如果包含屏蔽词，则取消事件，阻止消息显示
                event.setCanceled(true);
                return;
            }
        }
        
        // 注释掉基于Mod ID的拦截逻辑
        // 由于之前已完全禁用BadModIdConfig功能，此处相关代码已移除
    }
}