package de.epiceric.shopchest.config;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import de.epiceric.shopchest.ShopChest;

public class HologramFormat {

    public enum Requirement {
        VENDOR, AMOUNT, ITEM_TYPE, ITEM_NAME, HAS_ENCHANTMENT, BUY_PRICE,
        SELL_PRICE, HAS_POTION_EFFECT, IS_MUSIC_DISC, IS_POTION_EXTENDED, IS_BANNER_PATTERN,
        IS_WRITTEN_BOOK, ADMIN_SHOP, NORMAL_SHOP, IN_STOCK, MAX_STACK, CHEST_SPACE, DURABILITY
    }

    // no "-" sign since no variable can be negative
    // e.g.: 100.0 >= 50.0
    private static final Pattern SIMPLE_NUMERIC_CONDITION = Pattern.compile("^(\\d+(?:\\.\\d+)?) ([<>][=]?|[=!]=) (\\d+(?:\\.\\d+)?)$");

    // e.g.: "STONE" == "DIAMOND_SWORD"
    private static final Pattern SIMPLE_STRING_CONDITION = Pattern.compile("^\"([^\"]*)\" ([=!]=) \"([^\"]*)\"$");

    private ShopChest plugin;
    private File configFile;
    private YamlConfiguration config;

    public HologramFormat(ShopChest plugin) {
        this.configFile = new File(plugin.getDataFolder(), "hologram-format.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.plugin = plugin;
    }

    /**
     * Get the format for the given line of the hologram
     * @param line Line of the hologram
     * @param reqMap Values of the requirements that might be needed by the format (contains {@code null} if not comparable)
     * @param plaMap Values of the placeholders that might be needed by the format
     * @return  The format of the first working option, or an empty String if no option is working
     *          because of not fulfilled requirements
     */
    public String getFormat(int line, Map<Requirement, Object> reqMap, Map<Placeholder, Object> plaMap) {
        ConfigurationSection options = config.getConfigurationSection("lines." + line + ".options");

        optionLoop:
        for (String key : options.getKeys(false)) {
            ConfigurationSection option = options.getConfigurationSection(key);
            List<String> requirements = option.getStringList("requirements");

            String format = option.getString("format");

            for (String sReq : requirements) {
                for (Requirement req : reqMap.keySet()) {
                    if (sReq.contains(req.toString())) {
                        if (!evalRequirement(sReq, reqMap)) {
                            continue optionLoop;
                        }
                    }
                }
            }

            return evalPlaceholder(format, plaMap);
        }

        return "";
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * @return Whether the hologram text has to change dynamically without reloading
     */
    public boolean isDynamic() {
        int count = getLineCount();
        for (int i = 0; i < count; i++) {
            ConfigurationSection options = config.getConfigurationSection("lines." + i + ".options");

            for (String key : options.getKeys(false)) {
                ConfigurationSection option = options.getConfigurationSection(key);

                String format = option.getString("format");
                if (format.contains(Placeholder.STOCK.toString()) || format.contains(Placeholder.CHEST_SPACE.toString())) {
                    return true;
                }

                for (String req : option.getStringList("requirements")) {
                    if (req.contains(Requirement.IN_STOCK.toString()) || req.contains(Requirement.CHEST_SPACE.toString())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * @return Amount of lines in a hologram
     */
    public int getLineCount() {
        return config.getConfigurationSection("lines").getKeys(false).size();
    }

    /** 
     * @return Configuration of the "hologram-format.yml" file
     */
    public YamlConfiguration getConfig() {
        return config;
    }

    /**
     * Parse and evaluate a condition
     * @param condition Condition to evaluate
     * @param values Values of the requirements
     * @return Result of the condition
     */
    public boolean evalRequirement(String condition, Map<Requirement, Object> values) {
        String cond = condition;

        for (HologramFormat.Requirement req : HologramFormat.Requirement.values()) {
            if (cond.contains(req.toString()) && values.containsKey(req)) {
                Object val = values.get(req);
                String sVal = String.valueOf(val);

                if (val instanceof String && !(sVal.startsWith("\"") && sVal.endsWith("\""))) {
                    sVal = String.format("\"%s\"", sVal);
                }

                cond = cond.replace(req.toString(), sVal);
            }
        }

        if (cond.equals("true")) {
            // e.g.: ADMIN_SHOP
            return true;
        } else if (cond.equals("false")) {
            return false;
        }

        return ExpressionEvaluator.evaluate(cond);
    }

    /**
     * Parse and evaluate a condition
     * @param string Message or hologram format whose containing scripts to execute
     * @param values Values of the placeholders
     * @return Result of the condition
     */
    public String evalPlaceholder(String string, Map<Placeholder, Object> values) {
        Matcher matcher = Pattern.compile("\\{([^}]+)}").matcher(string);
        String newString = string;

        while (matcher.find()) {
            String withBrackets = matcher.group();
            String script = withBrackets.substring(1, withBrackets.length() - 1);

            for (Placeholder placeholder : values.keySet()) {
                if (script.contains(placeholder.toString())) {
                    Object val = values.get(placeholder);
                    String sVal = String.valueOf(val);

                    if (val instanceof String && !(sVal.startsWith("\"") && sVal.endsWith("\""))) {
                        sVal = String.format("\"%s\"", sVal);
                    }

                    script = script.replace(placeholder.toString(), sVal);
                }
            }

            String result = ExpressionEvaluator.evaluateScript(script);
            newString = newString.replace(withBrackets, result);
        }

        return newString;
    }
}
