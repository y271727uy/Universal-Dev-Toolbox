package org.tab.universal_dev_toolbox;

import net.minecraftforge.common.ForgeConfigSpec;

public class RecipeNameConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.BooleanValue ADD_QUOTES;
    
    static {
        BUILDER.push("Recipe Name Configuration");
        
        ADD_QUOTES = BUILDER.comment("Add single quotes around recipe names")
                           .define("add_quotes", false);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}