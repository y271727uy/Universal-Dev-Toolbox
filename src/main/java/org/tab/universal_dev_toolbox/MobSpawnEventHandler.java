package org.tab.universal_dev_toolbox;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = Universal_dev_toolbox.MODID)
public class MobSpawnEventHandler {
    
    // 缓存解析后的黑名单配置
    private static final Map<String, Set<String>> blacklistedMobBiomePairs = new HashMap<>();
    
    // 缓存解析后的白名单配置
    private static final Map<String, Set<String>> whitelistedMobBiomePairs = new HashMap<>();
    
    /**
     * 处理实体加入世界事件，根据配置阻止或允许特定生物在特定生物群系生成
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // 只处理生物实体
        Entity entity = event.getEntity();
        EntityType<?> entityType = entity.getType();
        if (entityType.getCategory() == MobCategory.MISC) {
            return;
        }
        
        // 获取实体和生物群系的资源位置
        ResourceLocation entityLocation = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        ResourceLocation biomeLocation = event.getLevel().getBiome(entity.blockPosition()).unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        
        if (entityLocation == null || biomeLocation == null) {
            return;
        }
        
        String entityKey = entityLocation.toString();
        String biomeKey = biomeLocation.toString();
        
        // 检查黑名单配置 - 如果在黑名单中则阻止生成
        if (isBlacklisted(entityKey, biomeKey)) {
            event.setCanceled(true);
            
            // 如果是玩家放置的生物，发送提示消息
            if (entity instanceof Player player) {
                sendSpawnBlockedMessage(player);
            }
            
            return;
        }
        
        // 检查白名单配置 - 如果白名单不为空且生物不在允许的生物群系中则阻止生成
        if (!whitelistedMobBiomePairs.isEmpty() && !isWhitelisted(entityKey, biomeKey)) {
            event.setCanceled(true);
            
            // 如果是玩家放置的生物，发送提示消息
            if (entity instanceof Player player) {
                sendSpawnBlockedMessage(player);
            }
        }
    }
    
    /**
     * 向玩家发送生物生成被拦截的提示消息
     */
    private static void sendSpawnBlockedMessage(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 发送红色文本提示消息
            serverPlayer.sendSystemMessage(
                Component.literal("生物已经被拦截")
                    .withStyle(style -> style.withColor(TextColor.parseColor("#FF5555")))
            );
        }
    }
    
    /**
     * 检查指定的生物-生物群系组合是否在黑名单中
     */
    private static boolean isBlacklisted(String entityKey, String biomeKey) {
        return blacklistedMobBiomePairs.containsKey(entityKey) && 
               blacklistedMobBiomePairs.get(entityKey).contains(biomeKey);
    }
    
    /**
     * 检查指定的生物是否可以在指定的生物群系中生成（基于白名单）
     */
    private static boolean isWhitelisted(String entityKey, String biomeKey) {
        // 如果没有对该生物设置白名单限制，则允许在任何生物群系生成
        if (!whitelistedMobBiomePairs.containsKey(entityKey)) {
            return true;
        }
        
        // 如果设置了白名单，则只允许在指定的生物群系中生成
        return whitelistedMobBiomePairs.get(entityKey).contains(biomeKey);
    }
    
    /**
     * 解析配置并更新缓存
     */
    public static void parseConfigs() {
        // 清空旧的缓存
        blacklistedMobBiomePairs.clear();
        whitelistedMobBiomePairs.clear();
        
        // 解析黑名单配置
        List<? extends String> blacklistEntries = BadMobBiomeConfig.BLACKLISTED_MOB_BIOME_PAIRS.get();
        for (String entry : blacklistEntries) {
            String[] parts = entry.split(";");
            if (parts.length == 2) {
                String entityKey = parts[0];
                String biomeKey = parts[1];
                
                blacklistedMobBiomePairs.computeIfAbsent(entityKey, k -> new HashSet<>()).add(biomeKey);
            }
        }
        
        // 解析白名单配置
        List<? extends String> whitelistEntries = MobBiomeConfig.WHITELISTED_MOB_BIOME_PAIRS.get();
        for (String entry : whitelistEntries) {
            String[] parts = entry.split(";");
            if (parts.length == 2) {
                String entityKey = parts[0];
                String biomeKey = parts[1];
                
                whitelistedMobBiomePairs.computeIfAbsent(entityKey, k -> new HashSet<>()).add(biomeKey);
            }
        }
    }
}