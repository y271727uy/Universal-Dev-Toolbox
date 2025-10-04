package org.tab.universal_dev_toolbox;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class UDTCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("udt")
                        .then(Commands.literal("recipe")
                                .executes(UDTCommand::handleRecipePrint))
                        .then(Commands.literal("tag")
                                .executes(UDTCommand::handleTagPrint))
                        .then(Commands.literal("mcreator")
                                .executes(UDTCommand::handleMCreator)
                                .then(Commands.literal("print")
                                        .executes(UDTCommand::handleMCreatorPrint)))
                        .then(Commands.literal("wiki")
                                .then(Commands.literal("kubejs")
                                        .executes(UDTCommand::handleWikiKubeJS))
                                .then(Commands.literal("crafttweaker")
                                        .executes(UDTCommand::handleWikiCraftTweaker)))
        );
    }

    private static int handleMCreator(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 获取mods文件夹路径
            File modsDir = new File("mods");
            if (!modsDir.exists() || !modsDir.isDirectory()) {
                source.sendFailure(Component.literal("Mods folder not found"));
                return 0;
            }
            
            // 获取所有jar文件
            File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                source.sendSuccess(() -> Component.literal("No jar files found in mods folder"), false);
                return 1;
            }
            
            // 存储检测结果
            Map<String, Set<String>> confirmedMCreatorMods = new HashMap<>();
            Map<String, Set<String>> suspectedMCreatorMods = new HashMap<>();
            
            // 获取白名单和黑名单
            List<? extends String> whitelistedMods = MCreatorWhitelistConfig.WHITELISTED_MODS.get();
            List<? extends String> blacklistedMods = MCreatorBlacklistConfig.BLACKLISTED_MODS.get();
            
            // 分析每个jar文件
            for (File jarFile : jarFiles) {
                String modFileName = jarFile.getName();
                String modId = extractModId(modFileName);
                
                // 检查白名单
                if (whitelistedMods.contains(modId)) {
                    // 跳过白名单中的模组
                    continue;
                }
                
                // 检查黑名单
                if (blacklistedMods.contains(modId)) {
                    // 黑名单中的模组直接归类为@mcreator mod
                    confirmedMCreatorMods.computeIfAbsent(modFileName, k -> new HashSet<>()).add("Blacklisted mod");
                    continue;
                }
                
                ModAnalysisResult result = analyzeJarFile(jarFile);
                
                // 根据规则判断分类
                if ((result.condition1 && result.condition2 && result.condition3 && result.condition4) || // 全部满足
                    (result.condition1 && result.condition4) || // 条件1和4各自满足一次
                    (result.condition2 && result.condition3) || // 条件2和3满足
                    result.condition5) { // 满足条件5
                    confirmedMCreatorMods.computeIfAbsent(modFileName, k -> new HashSet<>()).addAll(result.reasons);
                } else if (result.condition1 || result.condition4) { // 只满足条件1或4
                    suspectedMCreatorMods.computeIfAbsent(modFileName, k -> new HashSet<>()).addAll(result.reasons);
                }
            }
            
            // 输出结果
            source.sendSuccess(() -> Component.literal("MCreator Mod Detection Results:"), false);
            
            // 输出确认的MCreator模组
            if (!confirmedMCreatorMods.isEmpty()) {
                source.sendSuccess(() -> Component.literal("@mcreator mod"), false);
                confirmedMCreatorMods.forEach((modName, reasons) -> {
                    MutableComponent component = Component.literal(" ▪ " + modName);
                    Style style = Style.EMPTY
                            .withColor(TextColor.parseColor("#55FF55"))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, modName))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")));
                    component.setStyle(style);
                    source.sendSuccess(() -> component, false);
                });
            }
            
            // 输出疑似MCreator模组
            if (!suspectedMCreatorMods.isEmpty()) {
                source.sendSuccess(() -> Component.literal("@suspected mcreator mod"), false);
                suspectedMCreatorMods.forEach((modName, reasons) -> {
                    MutableComponent component = Component.literal(" ▪ " + modName);
                    Style style = Style.EMPTY
                            .withColor(TextColor.parseColor("#55FF55"))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, modName))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")));
                    component.setStyle(style);
                    source.sendSuccess(() -> component, false);
                });
            }
            
            if (confirmedMCreatorMods.isEmpty() && suspectedMCreatorMods.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No MCreator or suspected MCreator mods found."), false);
            }
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt mcreator command", e);
            source.sendFailure(Component.literal("Error occurred while scanning mods: " + e.getMessage()));
            return 0;
        }
    }

    private static int handleMCreatorPrint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 获取mods文件夹路径
            File modsDir = new File("mods");
            if (!modsDir.exists() || !modsDir.isDirectory()) {
                source.sendFailure(Component.literal("Mods folder not found"));
                return 0;
            }
            
            // 获取所有jar文件
            File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                source.sendSuccess(() -> Component.literal("No jar files found in mods folder"), false);
                return 1;
            }
            
            // 存储检测结果
            Map<String, Set<String>> confirmedMCreatorMods = new HashMap<>();
            Map<String, Set<String>> suspectedMCreatorMods = new HashMap<>();
            
            // 获取白名单和黑名单
            List<? extends String> whitelistedMods = MCreatorWhitelistConfig.WHITELISTED_MODS.get();
            List<? extends String> blacklistedMods = MCreatorBlacklistConfig.BLACKLISTED_MODS.get();
            
            // 分析每个jar文件
            for (File jarFile : jarFiles) {
                String modFileName = jarFile.getName();
                String modId = extractModId(modFileName);
                
                // 检查白名单
                if (whitelistedMods.contains(modId)) {
                    // 跳过白名单中的模组
                    continue;
                }
                
                // 检查黑名单
                if (blacklistedMods.contains(modId)) {
                    // 黑名单中的模组直接归类为@mcreator mod
                    confirmedMCreatorMods.computeIfAbsent(modFileName, k -> new HashSet<>()).add("Blacklisted mod");
                    continue;
                }
                
                ModAnalysisResult result = analyzeJarFile(jarFile);
                
                // 根据规则判断分类
                if ((result.condition1 && result.condition2 && result.condition3 && result.condition4) || // 全部满足
                    (result.condition1 && result.condition4) || // 条件1和4各自满足一次
                    (result.condition2 && result.condition3) || // 条件2和3满足
                    result.condition5) { // 满足条件5
                    confirmedMCreatorMods.computeIfAbsent(modFileName, k -> new HashSet<>()).addAll(result.reasons);
                } else if (result.condition1 || result.condition4) { // 只满足条件1或4
                    suspectedMCreatorMods.computeIfAbsent(modFileName, k -> new HashSet<>()).addAll(result.reasons);
                }
            }
            
            // 创建配置文件夹
            File configDir = new File("config/universal_dev_toolbox");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // 写入文件
            File outputFile = new File(configDir, "mcreator_mod_list.txt");
            try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
                writer.write("MCreator Mod Detection Results:\n\n");
                
                // 写入确认的MCreator模组
                if (!confirmedMCreatorMods.isEmpty()) {
                    writer.write("@mcreator mod\n");
                    for (String modName : confirmedMCreatorMods.keySet()) {
                        writer.write(" ▪ " + modName + "\n");
                    }
                    writer.write("\n");
                }
                
                // 写入疑似MCreator模组
                if (!suspectedMCreatorMods.isEmpty()) {
                    writer.write("@suspected mcreator mod\n");
                    for (String modName : suspectedMCreatorMods.keySet()) {
                        writer.write(" ▪ " + modName + "\n");
                    }
                    writer.write("\n");
                }
                
                if (confirmedMCreatorMods.isEmpty() && suspectedMCreatorMods.isEmpty()) {
                    writer.write("No MCreator or suspected MCreator mods found.\n");
                }
            }
            
            source.sendSuccess(() -> Component.literal("MCreator mod list has been saved to: " + outputFile.getAbsolutePath()), false);
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt mcreator print command", e);
            source.sendFailure(Component.literal("Error occurred while scanning mods: " + e.getMessage()));
            return 0;
        }
    }

    private static int handleWikiKubeJS(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 发送标题
            source.sendSuccess(() -> Component.literal("kubejs wiki"), false);
            
            // 发送可点击访问的链接
            String url = "https://kubejs.com/";
            MutableComponent component = Component.literal(" ▪ " + url);
            Style style = Style.EMPTY
                    .withColor(TextColor.parseColor("#55FF55"))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open")));
            component.setStyle(style);
            source.sendSuccess(() -> component, false);
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt wiki kubejs command", e);
            source.sendFailure(Component.literal("Error occurred while processing command: " + e.getMessage()));
            return 0;
        }
    }

    private static int handleWikiCraftTweaker(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 发送标题
            source.sendSuccess(() -> Component.literal("crafttweaker wiki"), false);
            
            // 发送可点击访问的链接
            String url = "https://docs.blamejared.com/";
            MutableComponent component = Component.literal(" ▪ " + url);
            Style style = Style.EMPTY
                    .withColor(TextColor.parseColor("#55FF55"))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open")));
            component.setStyle(style);
            source.sendSuccess(() -> component, false);
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt wiki crafttweaker command", e);
            source.sendFailure(Component.literal("Error occurred while processing command: " + e.getMessage()));
            return 0;
        }
    }

    private static class ModAnalysisResult {
        boolean condition1 = false; // 存在mcreator或procedures文件夹
        boolean condition2 = false; // mods.toml或fabric.mod.json中出现mcreator
        boolean condition3 = false; // java包名出现mcreator
        boolean condition4 = false; // 出现模板化名字(XxxMod.java等)
        boolean condition5 = false; // 存在Procedure类
        Set<String> reasons = new HashSet<>();
    }
    
    private static ModAnalysisResult analyzeJarFile(File jarFile) {
        ModAnalysisResult result = new ModAnalysisResult();
        
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // 条件1: 检查是否存在mcreator或procedures文件夹
                if (entryName.startsWith("mcreator/") || entryName.contains("/mcreator/") || 
                    entryName.startsWith("procedures/") || entryName.contains("/procedures/")) {
                    result.condition1 = true;
                    result.reasons.add("Contains mcreator or procedures folders");
                }
                
                // 条件3: 检查Java包名是否包含mcreator
                if ((entryName.endsWith(".class") || entryName.endsWith(".java")) && 
                    (entryName.contains("mcreator") || entryName.contains("MCreator"))) {
                    result.condition3 = true;
                    result.reasons.add("Contains mcreator in package names");
                }
                
                // 条件4: 检查是否有模板化的类名
                if (entryName.endsWith(".class") || entryName.endsWith(".java")) {
                    String className = new File(entryName).getName();
                    // 检查类似XxxMod.java, XxxModItems等模板化命名
                    if (Pattern.compile(".*[Mm]od.*\\.(class|java)").matcher(className).matches() ||
                        Pattern.compile(".*[Mm]od[Ii]tems.*\\.(class|java)").matcher(className).matches() ||
                        Pattern.compile(".*[Mm]od[Bb]locks.*\\.(class|java)").matcher(className).matches() ||
                        Pattern.compile(".*[Mm]od[Ee]ntities.*\\.(class|java)").matcher(className).matches()) {
                        result.condition4 = true;
                        result.reasons.add("Contains template-like class names");
                    }
                }
                
                // 条件5: 检查是否存在Procedure类
                if ((entryName.endsWith(".class") || entryName.endsWith(".java")) && 
                    new File(entryName).getName().equals("Procedure.java")) {
                    result.condition5 = true;
                    result.reasons.add("Contains Procedure class");
                }
                
                // 条件2: 检查mod配置文件
                if (entryName.equals("META-INF/mods.toml") || entryName.equals("fabric.mod.json")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.toLowerCase().contains("mcreator")) {
                                result.condition2 = true;
                                result.reasons.add("Contains mcreator in mod metadata");
                                break;
                            }
                        }
                    } catch (IOException e) {
                        LogUtils.getLogger().warn("Could not read mod metadata file: " + entryName, e);
                    }
                }
            }
            
        } catch (IOException e) {
            LogUtils.getLogger().error("Error reading jar file: " + jarFile.getName(), e);
        }
        
        return result;
    }
    
    /**
     * 从文件名中提取modid
     * @param fileName jar文件名
     * @return 提取的modid
     */
    private static String extractModId(String fileName) {
        // 移除.jar扩展名
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        // 移除版本号（假设版本号以数字开头）
        // 例如: "modname-1.0.0" -> "modname"
        String[] parts = fileName.split("-");
        StringBuilder modId = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            // 如果部分以数字开头，认为是版本号，停止处理
            if (parts[i].length() > 0 && Character.isDigit(parts[i].charAt(0))) {
                break;
            }
            
            if (i > 0) {
                modId.append("-");
            }
            modId.append(parts[i]);
        }
        
        return modId.toString();
    }
    
    private static int handleRecipePrint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ItemStack heldItem = source.getPlayerOrException().getMainHandItem();
            
            if (heldItem.isEmpty()) {
                source.sendFailure(Component.translatable("commands.udt.recipe_print.no_item"));
                return 0;
            }
            
            RecipeManager recipeManager = source.getLevel().getRecipeManager();
            
            // 根据输出物品筛选配方
            Map<String, StringBuilder> recipeMap = new HashMap<>();
            
            // 获取所有配方
            Collection<Recipe<?>> recipes = recipeManager.getRecipes();
            for (Recipe<?> recipe : recipes) {
                ItemStack output = recipe.getResultItem(source.getLevel().registryAccess());
                if (ItemStack.isSameItemSameTags(output, heldItem)) {
                    ResourceLocation recipeId = recipe.getId();
                    String modId = recipeId.getNamespace();
                    recipeMap.computeIfAbsent(modId, k -> new StringBuilder())
                            .append(modId)
                            .append(":")
                            .append(recipeId.getPath())
                            .append("\n");
                }
            }
            
            // 发送结果给玩家
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(heldItem.getItem());
            source.sendSuccess(() -> Component.translatable("commands.udt.recipe_print.title", itemId.toString()), false);
            
            if (recipeMap.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.udt.recipe_print.no_recipes"), false);
            } else {
                recipeMap.forEach((modId, recipesBuilder) -> {
                    // 移除末尾的换行符
                    if (recipesBuilder.length() > 0 && recipesBuilder.charAt(recipesBuilder.length() - 1) == '\n') {
                        recipesBuilder.setLength(recipesBuilder.length() - 1);
                    }
                    
                    // 分割配方列表
                    String[] lines = recipesBuilder.toString().split("\n");
                    
                    // 显示模组标题
                    source.sendSuccess(() -> Component.literal("@" + modId), false);
                    
                    // 为每个配方创建可点击复制的绿色文本
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            // 根据配置决定是否添加引号
                            String copyText = line;
                            if (RecipeNameConfig.ADD_QUOTES.get()) {
                                copyText = "'" + line + "'";
                            }
                            
                            MutableComponent component = Component.literal(" ▪ " + line);
                            // 设置绿色文本样式
                            Style style = Style.EMPTY
                                    .withColor(TextColor.parseColor("#55FF55"))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyText))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")));
                            component.setStyle(style);
                            source.sendSuccess(() -> component, false);
                        }
                    }
                });
            }
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt rp command", e);
            source.sendFailure(Component.translatable("commands.udt.recipe_print.error"));
            return 0;
        }
    }
    
    private static int handleTagPrint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ItemStack mainHandItem = source.getPlayerOrException().getMainHandItem();
            ItemStack offHandItem = source.getPlayerOrException().getOffhandItem();
            
            // 检查是否有物品
            if (mainHandItem.isEmpty() && offHandItem.isEmpty()) {
                source.sendFailure(Component.translatable("commands.udt.recipe_print.no_item"));
                return 0;
            }
            
            // 如果只有主手有物品
            if (!mainHandItem.isEmpty() && offHandItem.isEmpty()) {
                return printItemTags(source, mainHandItem);
            }
            
            // 如果只有副手有物品
            if (mainHandItem.isEmpty() && !offHandItem.isEmpty()) {
                return printItemTags(source, offHandItem);
            }
            
            // 如果双手都有物品，比较标签
            return compareItemTags(source, mainHandItem, offHandItem);
            
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt tag command", e);
            source.sendFailure(Component.translatable("commands.udt.recipe_print.error"));
            return 0;
        }
    }
    
    private static int printItemTags(CommandSourceStack source, ItemStack item) {
        try {
            // 获取物品的标签
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item.getItem());
            Collection<TagKey<Item>> tags = item.getTags().toList();
            
            // 发送结果给玩家
            String itemDisplayName = itemId.toString();
            source.sendSuccess(() -> Component.translatable("commands.udt.tag_print.title", itemDisplayName), false);
            
            if (tags.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.udt.tag_print.no_tags"), false);
            } else {
                Map<String, StringBuilder> tagMap = new HashMap<>();
                
                // 按模组分组标签
                for (TagKey<Item> tag : tags) {
                    ResourceLocation tagId = tag.location();
                    String modId = tagId.getNamespace();
                    tagMap.computeIfAbsent(modId, k -> new StringBuilder())
                            .append("'")
                            .append(tagId.getNamespace())
                            .append(":")
                            .append(tagId.getPath())
                            .append("'\n");
                }
                
                // 显示标签信息
                tagMap.forEach((modId, tagsBuilder) -> {
                    // 移除末尾的换行符
                    if (tagsBuilder.length() > 0 && tagsBuilder.charAt(tagsBuilder.length() - 1) == '\n') {
                        tagsBuilder.setLength(tagsBuilder.length() - 1);
                    }
                    
                    // 分割标签列表
                    String[] lines = tagsBuilder.toString().split("\n");
                    
                    // 显示模组标题
                    source.sendSuccess(() -> Component.literal("@" + modId), false);
                    
                    // 为每个标签创建可点击复制的绿色文本
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            MutableComponent component = Component.literal(" ▪ " + line);
                            // 设置绿色文本样式
                            Style style = Style.EMPTY
                                    .withColor(TextColor.parseColor("#55FF55"))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, line))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")));
                            component.setStyle(style);
                            source.sendSuccess(() -> component, false);
                        }
                    }
                });
            }
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error printing item tags", e);
            source.sendFailure(Component.translatable("commands.udt.recipe_print.error"));
            return 0;
        }
    }
    
    private static int compareItemTags(CommandSourceStack source, ItemStack mainHandItem, ItemStack offHandItem) {
        try {
            // 获取两个物品的标签
            Collection<TagKey<Item>> mainHandTags = mainHandItem.getTags().toList();
            Collection<TagKey<Item>> offHandTags = offHandItem.getTags().toList();
            
            // 获取物品ID
            ResourceLocation mainHandItemId = ForgeRegistries.ITEMS.getKey(mainHandItem.getItem());
            ResourceLocation offHandItemId = ForgeRegistries.ITEMS.getKey(offHandItem.getItem());
            
            // 发送结果给玩家
            source.sendSuccess(() -> Component.translatable("commands.udt.tag_print.compare_title", mainHandItemId.toString(), offHandItemId.toString()), false);
            
            // 找到相同的标签
            Set<TagKey<Item>> commonTags = new HashSet<>(mainHandTags);
            commonTags.retainAll(offHandTags);
            
            if (commonTags.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.udt.tag_print.no_common_tags"), false);
            } else {
                Map<String, StringBuilder> tagMap = new HashMap<>();
                
                // 按模组分组相同标签
                for (TagKey<Item> tag : commonTags) {
                    ResourceLocation tagId = tag.location();
                    String modId = tagId.getNamespace();
                    tagMap.computeIfAbsent(modId, k -> new StringBuilder())
                            .append("'")
                            .append(tagId.getNamespace())
                            .append(":")
                            .append(tagId.getPath())
                            .append("'\n");
                }
                
                // 显示相同标签信息
                source.sendSuccess(() -> Component.translatable("commands.udt.tag_print.same_tags"), false);
                tagMap.forEach((modId, tagsBuilder) -> {
                    // 移除末尾的换行符
                    if (tagsBuilder.length() > 0 && tagsBuilder.charAt(tagsBuilder.length() - 1) == '\n') {
                        tagsBuilder.setLength(tagsBuilder.length() - 1);
                    }
                    
                    // 分割标签列表
                    String[] lines = tagsBuilder.toString().split("\n");
                    
                    // 为每个标签创建可点击复制的绿色文本
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            MutableComponent component = Component.literal(" ▪ " + line);
                            // 设置绿色文本样式
                            Style style = Style.EMPTY
                                    .withColor(TextColor.parseColor("#55FF55"))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, line))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")));
                            component.setStyle(style);
                            source.sendSuccess(() -> component, false);
                        }
                    }
                });
            }
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error comparing item tags", e);
            source.sendFailure(Component.translatable("commands.udt.recipe_print.error"));
            return 0;
        }
    }
}