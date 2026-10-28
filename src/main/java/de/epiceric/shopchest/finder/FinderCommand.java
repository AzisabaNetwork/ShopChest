package de.epiceric.shopchest.finder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * {@code /findshop [page | search <keyword> | settings | config ...]}
 */
class FinderCommand implements CommandExecutor, TabCompleter {
    private static final String USAGE = "§c使用方法: /findshop [ページ番号 | search <キーワード> | settings | config ...]";
    private static final String CONFIG_USAGE = "§c使用方法: /findshop config <group-materials|exclude-materials|exclude-vanilla-materials> <add|remove> <Material>";

    private final ShopFinder finder;

    FinderCommand(ShopFinder finder) {
        this.finder = finder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!finder.isEnabled()) {
            sender.sendMessage("§cショップ検索は無効になっています。");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("config")) {
            changeConfig(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (args.length == 0) {
            finder.getMenu().openItems(player, 1, null);
        } else if (args[0].equalsIgnoreCase("settings")) {
            finder.getMenu().openSettings(player);
        } else if (args[0].equalsIgnoreCase("search")) {
            if (args.length > 1) {
                String keyword = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                finder.getMenu().openItems(player, 1, keyword);
            } else {
                player.sendMessage("§c使用方法: /findshop search <キーワード>");
            }
        } else {
            try {
                finder.getMenu().openItems(player, Integer.parseInt(args[0]), null);
            } catch (NumberFormatException e) {
                player.sendMessage(USAGE);
            }
        }
        return true;
    }

    private void changeConfig(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません。");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(CONFIG_USAGE);
            return;
        }

        String listName = args[1].toLowerCase(Locale.ROOT);
        if (!ShopFinder.MATERIAL_LISTS.contains(listName)) {
            sender.sendMessage("§c無効なリスト名です。");
            return;
        }

        Material material = Material.matchMaterial(args[3]);
        if (material == null) {
            sender.sendMessage("§c無効なマテリアル名です: " + args[3]);
            return;
        }
        String materialName = material.name();

        List<String> list = new ArrayList<>(finder.getMaterialList(listName));
        // Entries may have been written by hand in a different case
        boolean contains = list.stream().anyMatch(materialName::equalsIgnoreCase);

        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (contains) {
                    sender.sendMessage("§c既にリストに含まれています。");
                    return;
                }
                list.add(materialName);
                finder.setMaterialList(listName, list);
                sender.sendMessage("§a" + listName + " に " + materialName + " を追加しました。");
            }
            case "remove" -> {
                if (!contains) {
                    sender.sendMessage("§cリストに含まれていません。");
                    return;
                }
                list.removeIf(materialName::equalsIgnoreCase);
                finder.setMaterialList(listName, list);
                sender.sendMessage("§a" + listName + " から " + materialName + " を削除しました。");
            }
            default -> sender.sendMessage(CONFIG_USAGE);
        }
    }

    private static boolean isAdmin(CommandSender sender) {
        return !(sender instanceof Player player) || ShopFinder.isAdmin(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean config = args.length > 1 && args[0].equalsIgnoreCase("config") && isAdmin(sender);

        if (args.length == 1) {
            addMatching(completions, args[0], "search", "settings");
            if (isAdmin(sender)) addMatching(completions, args[0], "config");
        } else if (config && args.length == 2) {
            addMatching(completions, args[1], ShopFinder.MATERIAL_LISTS.toArray(new String[0]));
        } else if (config && args.length == 3) {
            addMatching(completions, args[2], "add", "remove");
        } else if (config && args.length == 4) {
            String prefix = args[3].toUpperCase(Locale.ROOT);
            for (Material material : Material.values()) {
                if (material.isItem() && !material.isLegacy() && material.name().startsWith(prefix)) {
                    completions.add(material.name());
                }
            }
        }
        return completions;
    }

    private static void addMatching(List<String> completions, String input, String... options) {
        String lower = input.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.startsWith(lower)) completions.add(option);
        }
    }
}
