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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

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
import java.util.Optional;
import java.util.ArrayList;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
                        .then(Commands.literal("ModCheck")
                                .then(Commands.literal("minrust")
                                        .executes(UDTCommand::handleMinRust))
                                .then(Commands.literal("RustforMod")
                                        .executes(UDTCommand::handleRustforMod)))
        );
    }

    private static int handleRustforMod(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 发送开始检查消息
            source.sendSuccess(() -> Component.literal("Starting Rust mod health check..."), false);
            
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
            
            // 存储检查结果
            Map<String, ModCheckResult> normalMods = new HashMap<>();
            Map<String, ModCheckResult> partialMods = new HashMap<>();
            Map<String, ModCheckResult> abnormalMods = new HashMap<>();
            
            // 计数依赖minrust的模组
            int minrustDependentModsCount = 0;
            
            // 分析每个jar文件
            for (File jarFile : jarFiles) {
                ModInfo modInfo = extractModInfo(jarFile);
                if (modInfo != null && modInfo.dependencies.contains("minrust")) {
                    minrustDependentModsCount++;
                    ModCheckResult result = checkModHealth(jarFile, modInfo);
                    
                    // 根据检查结果分类
                    switch (result.status) {
                        case "正常":
                            normalMods.put(modInfo.name, result);
                            break;
                        case "部分正常":
                            partialMods.put(modInfo.name, result);
                            break;
                        case "异常":
                            abnormalMods.put(modInfo.name, result);
                            break;
                    }
                }
            }
            
            // 输出结果
            source.sendSuccess(() -> Component.literal("Rust Mod Health Check Results:"), false);
            
            // 输出异常模组（红色）
            if (!abnormalMods.isEmpty()) {
                MutableComponent abnormalHeader = Component.literal("异常");
                Style redStyle = Style.EMPTY.withColor(TextColor.parseColor("#FF5555"));
                abnormalHeader.setStyle(redStyle);
                source.sendSuccess(() -> abnormalHeader, false);
                
                abnormalMods.forEach((modName, result) -> {
                    MutableComponent component = Component.literal(" ▪ " + modName + " - " + result.message);
                    component.setStyle(redStyle);
                    source.sendSuccess(() -> component, false);
                });
            }
            
            // 输出部分正常模组（黄色）
            if (!partialMods.isEmpty()) {
                MutableComponent partialHeader = Component.literal("部分正常");
                Style yellowStyle = Style.EMPTY.withColor(TextColor.parseColor("#FFFF55"));
                partialHeader.setStyle(yellowStyle);
                source.sendSuccess(() -> partialHeader, false);
                
                partialMods.forEach((modName, result) -> {
                    MutableComponent component = Component.literal(" ▪ " + modName + " - " + result.message);
                    component.setStyle(yellowStyle);
                    source.sendSuccess(() -> component, false);
                });
            }
            
            // 输出正常模组（绿色）
            if (!normalMods.isEmpty()) {
                MutableComponent normalHeader = Component.literal("正常");
                Style greenStyle = Style.EMPTY.withColor(TextColor.parseColor("#55FF55"));
                normalHeader.setStyle(greenStyle);
                source.sendSuccess(() -> normalHeader, false);
                
                normalMods.forEach((modName, result) -> {
                    MutableComponent component = Component.literal(" ▪ " + modName + " - " + result.message);
                    component.setStyle(greenStyle);
                    source.sendSuccess(() -> component, false);
                });
            }
            
            // 如果没有发现任何依赖minrust的模组
            if (minrustDependentModsCount == 0) {
                source.sendSuccess(() -> Component.literal("未发现有依赖rust的mod"), false);
            }
            // 如果没有发现任何异常，显示绿色提示
            else if (abnormalMods.isEmpty() && partialMods.isEmpty() && !normalMods.isEmpty()) {
                MutableComponent noIssuesComponent = Component.literal("未发现依赖Rust的mod有异常");
                Style greenStyle = Style.EMPTY.withColor(TextColor.parseColor("#55FF55"));
                noIssuesComponent.setStyle(greenStyle);
                source.sendSuccess(() -> noIssuesComponent, false);
            }
            
            return 1;
        } catch (Exception e) {
            LogUtils.getLogger().error("Error processing /udt ModCheck RustforMod command", e);
            source.sendFailure(Component.literal("Error occurred while checking mods: " + e.getMessage()));
            return 0;
        }
    }

    private static int handleMinRust(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // 检查是否安装了minrust模组
        Optional<? extends IModInfo> minrustMod = ModList.get().getMods().stream()
                .filter(mod -> "minrust".equals(mod.getModId()))
                .findFirst();
        
        if (!minrustMod.isPresent()) {
            source.sendFailure(Component.literal("MinRust mod is not installed"));
            return 0;
        }
        
        // 打印标题
        source.sendSuccess(() -> Component.literal("MinRust Mod Information:"), false);
        
        // 打印版本和modid
        IModInfo modInfo = minrustMod.get();
        source.sendSuccess(() -> Component.literal("Mod ID: " + modInfo.getModId()), false);
        source.sendSuccess(() -> Component.literal("Version: " + modInfo.getVersion()), false);
        
        // 打印Rust相关信息
        source.sendSuccess(() -> Component.literal("Rust Version: 2021 edition or later"), false);
        source.sendSuccess(() -> Component.literal("Rust Project Name: minrust-native"), false);
        source.sendSuccess(() -> Component.literal("Rust Project Version: 0.1.0"), false);
        source.sendSuccess(() -> Component.literal("Main Dependency: jni = \"0.21\""), false);
        source.sendSuccess(() -> Component.literal("Library Type: cdylib (C Dynamic Library)"), false);
        
        // 红色警告字体
        MutableComponent warning = Component.literal("Please check your rust code");
        Style warningStyle = Style.EMPTY.withColor(TextColor.parseColor("#FF5555"));
        warning.setStyle(warningStyle);
        source.sendSuccess(() -> warning, false);
        
        return 1;
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
    
    private static class ModInfo {
        String name;
        String version;
        Set<String> dependencies = new HashSet<>();
        
        ModInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
    
    private static class ModCheckResult {
        String status;
        String message;
        
        ModCheckResult(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
    
    private static ModInfo extractModInfo(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            // 查找mods.toml
            JarEntry modsTomlEntry = jar.getJarEntry("META-INF/mods.toml");
            if (modsTomlEntry != null) {
                return extractFromModsToml(jar, modsTomlEntry);
            }
            
            // 查找fabric.mod.json
            JarEntry fabricModJsonEntry = jar.getJarEntry("fabric.mod.json");
            if (fabricModJsonEntry != null) {
                return extractFromFabricModJson(jar, fabricModJsonEntry);
            }
        } catch (IOException e) {
            LogUtils.getLogger().warn("Could not read jar file: " + jarFile.getName(), e);
        }
        return null;
    }
    
    private static ModInfo extractFromModsToml(JarFile jar, JarEntry modsTomlEntry) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(modsTomlEntry), StandardCharsets.UTF_8))) {
            ModInfo modInfo = null;
            String line;
            String currentModId = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 查找modId
                if (line.startsWith("modId")) {
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex != -1) {
                        currentModId = line.substring(equalsIndex + 1).trim().replaceAll("\"", "");
                        if (modInfo == null) {
                            modInfo = new ModInfo(currentModId, "unknown");
                        }
                    }
                }
                
                // 查找version
                if (line.startsWith("version") && modInfo != null) {
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex != -1) {
                        String version = line.substring(equalsIndex + 1).trim().replaceAll("\"", "");
                        modInfo.version = version;
                    }
                }
                
                // 查找依赖
                if (line.startsWith("[[dependencies.")) {
                    // 继续读取依赖信息
                    String dependencyLine;
                    while ((dependencyLine = reader.readLine()) != null) {
                        dependencyLine = dependencyLine.trim();
                        if (dependencyLine.startsWith("modId")) {
                            int equalsIndex = dependencyLine.indexOf('=');
                            if (equalsIndex != -1) {
                                String depModId = dependencyLine.substring(equalsIndex + 1).trim().replaceAll("\"", "");
                                if (modInfo != null) {
                                    modInfo.dependencies.add(depModId);
                                }
                            }
                        }
                        // 如果遇到下一个段落则停止
                        if (dependencyLine.startsWith("[") || dependencyLine.startsWith("#")) {
                            break;
                        }
                    }
                }
            }
            
            return modInfo;
        } catch (IOException e) {
            LogUtils.getLogger().warn("Could not read mods.toml", e);
        }
        return null;
    }
    
    private static ModInfo extractFromFabricModJson(JarFile jar, JarEntry fabricModJsonEntry) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(fabricModJsonEntry), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            
            // 简单解析JSON（实际项目中应该使用JSON库）
            String json = content.toString();
            ModInfo modInfo = null;
            
            // 提取mod id
            int idIndex = json.indexOf("\"id\"");
            if (idIndex != -1) {
                int colonIndex = json.indexOf(":", idIndex);
                if (colonIndex != -1) {
                    int startIndex = json.indexOf("\"", colonIndex);
                    if (startIndex != -1) {
                        int endIndex = json.indexOf("\"", startIndex + 1);
                        if (endIndex != -1) {
                            String modId = json.substring(startIndex + 1, endIndex);
                            modInfo = new ModInfo(modId, "unknown");
                        }
                    }
                }
            }
            
            // 提取version
            int versionIndex = json.indexOf("\"version\"");
            if (versionIndex != -1 && modInfo != null) {
                int colonIndex = json.indexOf(":", versionIndex);
                if (colonIndex != -1) {
                    int startIndex = json.indexOf("\"", colonIndex);
                    if (startIndex != -1) {
                        int endIndex = json.indexOf("\"", startIndex + 1);
                        if (endIndex != -1) {
                            String version = json.substring(startIndex + 1, endIndex);
                            modInfo.version = version;
                        }
                    }
                }
            }
            
            // 提取依赖（简单处理）
            int dependsIndex = json.indexOf("\"depends\"");
            if (dependsIndex != -1 && modInfo != null) {
                int startIndex = json.indexOf("{", dependsIndex);
                if (startIndex != -1) {
                    int endIndex = json.indexOf("}", startIndex);
                    if (endIndex != -1) {
                        String dependsSection = json.substring(startIndex + 1, endIndex);
                        // 简单提取依赖项
                        if (dependsSection.contains("\"minrust\"")) {
                            modInfo.dependencies.add("minrust");
                        }
                    }
                }
            }
            
            return modInfo;
        } catch (IOException e) {
            LogUtils.getLogger().warn("Could not read fabric.mod.json", e);
        }
        return null;
    }
    
    private static ModCheckResult checkModHealth(File jarFile, ModInfo modInfo) {
        try {
            // 检查模组是否加载
            Optional<? extends IModInfo> loadedMod = ModList.get().getMods().stream()
                    .filter(mod -> mod.getModId().equals(modInfo.name))
                    .findFirst();
            
            if (!loadedMod.isPresent()) {
                return new ModCheckResult("异常", "Mod not loaded in current environment");
            }
            
            // 检查基本功能（这里只是示例，实际检查需要根据具体模组实现）
            boolean basicFunctionality = checkBasicFunctionality(jarFile, modInfo);
            boolean exceptionHandling = checkExceptionHandling(jarFile, modInfo);
            
            if (basicFunctionality && exceptionHandling) {
                return new ModCheckResult("正常", "All checks passed");
            } else if (basicFunctionality || exceptionHandling) {
                return new ModCheckResult("部分正常", "Some checks failed");
            } else {
                return new ModCheckResult("异常", "Critical functionality failed");
            }
        } catch (Exception e) {
            return new ModCheckResult("异常", "Exception during health check: " + e.getMessage());
        }
    }
    
    private static boolean checkBasicFunctionality(File jarFile, ModInfo modInfo) {
        // 这里应该执行一些基本功能测试
        // 由于这是一个通用工具箱，我们只能做一些通用检查
        try (JarFile jar = new JarFile(jarFile)) {
            // 检查是否存在主要的类文件
            Enumeration<JarEntry> entries = jar.entries();
            int classCount = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    classCount++;
                }
            }
            
            // 如果没有类文件，可能有问题
            return classCount > 0;
        } catch (IOException e) {
            return false;
        }
    }
    
    private static boolean checkExceptionHandling(File jarFile, ModInfo modInfo) {
        // 这里应该检查异常处理机制
        // 由于这是一个通用工具箱，我们只能做一些通用检查
        return true; // 简化处理
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