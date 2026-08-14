package com.bai.xuebai.factorysimulator.command;

import com.bai.xuebai.factorysimulator.config.PluginMessages;
import com.bai.xuebai.factorysimulator.FactorySimulator;
import com.bai.xuebai.factorysimulator.model.FactoryProfile;
import com.bai.xuebai.factorysimulator.service.FactoryService;
import com.bai.xuebai.factorysimulator.service.MachineRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FactoryCommand implements CommandExecutor, TabCompleter {
    private final FactorySimulator plugin;
    private final FactoryService service;
    private final PluginMessages messages;
    private final MachineRegistry registry;

    public FactoryCommand(FactorySimulator plugin, FactoryService service, PluginMessages messages, MachineRegistry registry) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }
        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hlep")) {
            sendHelp(sender, label);
            return true;
        }
        if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage(messages.get("version-info"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("factorysimulator.admin")) {
                sender.sendMessage(messages.get("no-permission"));
                return true;
            }
            plugin.reloadConfigs();
            sender.sendMessage(messages.get("reload-success"));
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) { sender.sendMessage(messages.get("only-player-needed")); return true; }
            Player player = (Player) sender;
            if (service.createFactory(player)) {
                sender.sendMessage(ChatColor.GREEN + "工厂创建成功，已发放初始设备、拆卸镐和教程书。使用 /fs enter 进入工厂。");
            } else sender.sendMessage(ChatColor.RED + "你已经创建过工厂了。");
            return true;
        }
        if (args[0].equalsIgnoreCase("enter")) {
            if (!(sender instanceof Player)) { sender.sendMessage(messages.get("only-player-needed")); return true; }
            FactoryProfile profile = service.getOrCreate((Player) sender);
            if (!profile.isCreated()) sender.sendMessage(ChatColor.YELLOW + "请先使用 /fs create 创建工厂。");
            else service.enterFactory((Player) sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player)) { sender.sendMessage(messages.get("only-player-needed")); return true; }
            sender.sendMessage(ChatColor.AQUA + "教程菜单请右键工厂模拟器教程书打开。");
            return true;
        }
        if (args[0].equalsIgnoreCase("rename") && args.length >= 2 && sender instanceof Player) {
            StringBuilder name = new StringBuilder();
            for (int i = 1; i < args.length; i++) { if (name.length() > 0) name.append(' '); name.append(args[i]); }
            sender.sendMessage(service.renameFactory((Player) sender, name.toString()) ? ChatColor.GREEN + "工厂已改名。" : ChatColor.RED + "工厂名称无效或工厂尚未创建。");
            return true;
        }
        if (args[0].equalsIgnoreCase("upgrade")) {
            if (!(sender instanceof Player)) { sender.sendMessage(messages.get("only-player-needed")); return true; }
            Player player = (Player) sender;
            FactoryProfile profile = service.getOrCreate(player);
            double cost = service.getPlotUpgradeCost(profile);
            if (profile.getPlotSize() >= pluginConfig().getMaxPlotSize()) {
                sender.sendMessage(ChatColor.YELLOW + "你的地皮已经达到最大尺寸。");
            } else if (service.upgradePlot(player)) {
                sender.sendMessage(ChatColor.GREEN + "地皮升级成功，当前尺寸：" + profile.getPlotSize() + "，花费：" + cost);
            } else {
                sender.sendMessage(ChatColor.RED + "地皮升级失败，可能是资金不足或工厂尚未创建。需要：" + cost);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("buy") && args.length >= 2 && sender instanceof Player) {
            Player player = (Player) sender; String id = args[1].toLowerCase();
            if (registry.get(id) == null) { sender.sendMessage(ChatColor.RED + "不存在的设备 ID。"); return true; }
            FactoryProfile profile = service.getOrCreate(player);
            if (!profile.isCreated()) { sender.sendMessage(ChatColor.RED + "请先创建工厂。"); return true; }
            if (profile.getLevel() < pluginConfig().getMachineUnlockLevel(id)) { sender.sendMessage(ChatColor.RED + "你的工厂等级不足，无法解锁该设备。"); return true; }
            double price = pluginConfig().getMachinePrice(id);
            if (profile.getMoney() < price) { sender.sendMessage(ChatColor.RED + "资金不足，需要 " + price + "。"); return true; }
            profile.setMoney(profile.getMoney() - price); service.save(profile);
            player.getInventory().addItem(registry.createItem(id, 1));
            sender.sendMessage(ChatColor.GREEN + "购买成功：" + id);
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(messages.get("only-player-needed"));
                    return true;
                }
                Player player = (Player) sender;
                FactoryProfile profile = service.getOrCreate(player);
                sender.sendMessage(service.formatOverview(profile));
                return true;
            }
            String target = args[1];
            if (target.equalsIgnoreCase("server")) {
                sender.sendMessage(service.formatServerStatus());
                return true;
            }
            FactoryProfile profile = service.getById(target);
            if (profile == null) {
                profile = service.getByPlayerName(target);
            }
            if (profile == null) {
                sender.sendMessage(messages.format("not-found", "target", target));
                return true;
            }
            sender.sendMessage(service.formatOverview(profile));
            return true;
        }
        if (args[0].equalsIgnoreCase("machine") && args.length >= 2 && args[1].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player) && args.length < 4) {
                sender.sendMessage(messages.get("only-player-needed"));
                return true;
            }
            Player target = sender instanceof Player ? (Player) sender : Bukkit.getPlayer(args[2]);
            String type = sender instanceof Player ? args[2] : args[3];
            int amountIndex = sender instanceof Player ? 3 : 4;
            int amount = args.length > amountIndex ? parseAmount(args[amountIndex]) : 1;
            if (target == null || registry.get(type) == null) {
                sender.sendMessage(messages.format("machine-not-found", "type", type));
                return true;
            }
            target.getInventory().addItem(registry.createItem(type, amount));
            sender.sendMessage(messages.format("machine-given", "type", type, "amount", amount));
            return true;
        }
        if (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("server")) {
            sender.sendMessage(service.formatServerStatus());
            return true;
        }
        sendHelp(sender, label);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        for (String line : messages.help(label)) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("help", "version", "create", "enter", "menu", "rename", "upgrade", "buy", "info", "status", "server", "machine", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("machine")) return filter(Collections.singletonList("give"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("machine") && args[1].equalsIgnoreCase("give")) return filter(new ArrayList<String>(registry.all().keySet()), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> options = new ArrayList<String>();
            options.add("server");
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
            return filter(options, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> input, String prefix) {
        List<String> result = new ArrayList<String>();
        for (String value : input) {
            if (value.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(value);
            }
        }
        return result;
    }

    private int parseAmount(String value) {
        try { return Math.max(1, Math.min(64, Integer.parseInt(value))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private com.bai.xuebai.factorysimulator.config.PluginConfig pluginConfig() {
        return com.bai.xuebai.factorysimulator.FactorySimulator.getInstance().getPluginConfig();
    }
}