package me.dw1e.kbm.command;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class KBMCommand implements TabExecutor {

    private static final List<String> TYPES = Arrays.asList(
            "horizontal.ground", "horizontal.air", "horizontal.sprint_extra",
            "vertical.ground", "vertical.air", "vertical.sprint_extra",
            "projectile.enabled", "projectile.horizontal_multiplier", "projectile.vertical_multiplier", "projectile.direction_override",
            "packet.misplace.enabled", "packet.misplace.distance", "packet.delay.enabled", "packet.delay.ticks",
            "stop_sprint", "y_limit", "hit_delay",
            "potion.enabled", "potion.horizontal_multiplier", "potion.vertical_multiplier", "potion.compensation_multiplier",
            "modern.cooldown_affects_kb", "modern.netherite_kb_resistance"
    );

    private static final List<String> COMMANDS = Arrays.asList(
            "create", "delete", "list", "edit", "view", "reload", "gui",
            "getprofile", "setprofile", "setexcluded", "help", "debug"
    );

    private final KnockbackManager plugin;

    public KBMCommand(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("kbm.use")) return Collections.emptyList();

        if (args.length == 1) {
            return filterStartingWith(COMMANDS, args[0]);
        }

        List<String> fileNames = new ArrayList<>(plugin.getKBLoader().getKbMap().keySet());
        List<String> onlinePlayers = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "delete":
            case "edit":
            case "view": {
                if (args.length == 2) {
                    return filterStartingWith(fileNames, args[1]);

                } else if (args.length == 3 && subCmd.equals("edit")) {
                    return filterStartingWith(TYPES, args[2]);

                } else if (args.length == 4) {
                    switch (args[2].toLowerCase()) {
                        case "packet.misplace.enabled":
                        case "packet.delay.enabled":
                        case "stop_sprint":
                        case "projectile.enabled":
                        case "projectile.direction_override":
                        case "potion.enabled":
                        case "modern.cooldown_affects_kb":
                        case "modern.netherite_kb_resistance":
                            return filterStartingWith(Arrays.asList("true", "false"), args[3]);
                    }
                }
                break;
            }
            case "reload": {
                if (args.length == 2) {
                    return filterStartingWith(Arrays.asList("config", "kb"), args[1]);
                } else if (args.length == 3 && args[1].equalsIgnoreCase("kb")) {
                    return filterStartingWith(fileNames, args[2]);
                }
            }
            case "setexcluded":
            case "getprofile":
            case "setprofile": {
                if (args.length == 2) {
                    return filterStartingWith(onlinePlayers, args[1]);

                } else if (args.length == 3) {
                    if (subCmd.equals("setexcluded")) {
                        return filterStartingWith(Arrays.asList("true", "false"), args[2]);

                    } else if (subCmd.equals("setprofile")) {
                        return filterStartingWith(fileNames, args[2]);
                    }
                }
                break;
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterStartingWith(List<String> options, String input) {
        return options.stream().filter(option -> option.toLowerCase()
                .startsWith(input.toLowerCase())).collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = KnockbackManager.PREFIX;

        if (!sender.hasPermission("kbm.use")) {
            sender.sendMessage(prefix + " §c你没有此命令的使用权限!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(prefix + " §7使用 §f/" + label + " help §7查看帮助!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help": {
                String same = "§f  /" + label;

                sender.sendMessage(prefix + " §7可用命令:");

                sender.sendMessage(same + " create§7: 创建KB文件");
                sender.sendMessage(same + " delete§7: 删除KB文件");
                sender.sendMessage(same + " list§7: 查看已读取的KB文件");
                sender.sendMessage(same + " edit§7: 编辑KB文件的数值");
                sender.sendMessage(same + " view§7: 查看KB文件的数值");
                sender.sendMessage(same + " reload§7: 重新加载KB文件");
                sender.sendMessage(same + " gui§7: 打开GUI界面");
                sender.sendMessage(same + " getprofile§7: 查看玩家使用的KB文件");
                sender.sendMessage(same + " setprofile§7: 设置玩家使用的KB文件");
                sender.sendMessage(same + " setexcluded§7: 设置玩家是否该被排除修改");
                sender.sendMessage(same + " debug§7: 开启/关闭调试模式");

                sender.sendMessage("§a使用教程&参考配置: 请见群(673765463)文件");

                return true;
            }
            case "create": {
                if (args.length != 2) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " create <KB文件名>");
                    return true;
                }

                plugin.getKBLoader().create(args[1], sender);

                return true;
            }
            case "delete": {
                if (args.length != 2) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " delete <KB文件名>");
                    return true;
                }

                plugin.getKBLoader().delete(args[1], sender);

                return true;
            }
            case "list": {
                sender.sendMessage(prefix + " §7已读取的KB文件:");

                plugin.getKBLoader().getKbMap().keySet().forEach(filename -> sender.sendMessage("  §f" + filename));

                return true;
            }
            case "edit": {
                if (args.length != 4) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " edit <KB文件名> <类型> <数值>");
                    return true;
                }

                FileConfiguration config = plugin.getKBLoader().getConfig(args[1]);

                if (config == null) {
                    sender.sendMessage(prefix + " §cKB文件 " + args[1] + " 不存在!");
                    return true;
                }

                boolean sameValue = false;

                String type = args[2].toLowerCase();

                if (!TYPES.contains(type)) {
                    sender.sendMessage(prefix + " §c无效的类型: " + args[2]);
                    return true;
                }

                switch (type) {
                    case "packet.misplace.enabled":
                    case "packet.delay.enabled":
                    case "stop_sprint":
                    case "projectile.enabled":
                    case "projectile.direction_override":
                    case "potion.enabled":
                    case "modern.cooldown_affects_kb":
                    case "modern.netherite_kb_resistance": {
                        boolean value;

                        String input = args[3].toLowerCase();

                        if (input.equals("true") || input.equals("false")) value = Boolean.parseBoolean(input);
                        else {
                            sender.sendMessage(prefix + " §c无效的布尔值: " + args[3]);
                            return true;
                        }

                        if (config.get(type).equals(value)) {
                            sameValue = true;
                            break;
                        }

                        config.set(type, value);

                        break;
                    }
                    case "packet.delay.ticks":
                    case "hit_delay": {
                        int value;

                        try {
                            value = Integer.parseInt(args[3]);
                        } catch (NumberFormatException ignore) {
                            sender.sendMessage(prefix + " §c无效的整数: " + args[3]);
                            return true;
                        }

                        if (config.get(type).equals(value)) {
                            sameValue = true;
                            break;
                        }

                        if (type.equals("packet.delay.ticks") && (value < 1 || value > 5)) {
                            sender.sendMessage(prefix + " §c不在范围(1~5)内的数值: " + value);
                            return true;
                        }

                        config.set(type, value);

                        break;
                    }
                    default: {
                        double value;

                        try {
                            value = Double.parseDouble(args[3]);
                        } catch (NumberFormatException ignore) {
                            sender.sendMessage(prefix + " §c无效的浮点数: " + args[3]);
                            return true;
                        }

                        if (config.get(type).equals(value)) {
                            sameValue = true;
                            break;
                        }

                        if (type.equals("packet.misplace.distance") && (value < 0.0 || value > 1.0)) {
                            sender.sendMessage(prefix + " §c不在范围(0.0~1.0)内的数值: " + value);
                            return true;
                        } else if (type.equals("potion.horizontal_multiplier") && (value < -8.0 || value > 8.0)) {
                            sender.sendMessage(prefix + " §c不在范围(-8.0~8.0)内的数值: " + value);
                            return true;
                        } else if (type.equals("potion.vertical_multiplier") && (value < -7.0 || value > 7.0)) {
                            sender.sendMessage(prefix + " §c不在范围(-7.0~7.0)内的数值: " + value);
                            return true;
                        }

                        config.set(type, value);

                        break;
                    }
                }

                sender.sendMessage(prefix + (sameValue
                        ? " §c无变化. " + args[1] + " 中的 " + type + " 原已设置为 " + config.get(type)
                        : " §7已将 §f" + args[1] + " §7中的 §f" + type + " §7设置为 §f" + config.get(type)));

                plugin.getKBLoader().save(args[1], sender);

                return true;
            }
            case "view": {
                if (args.length != 2) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " view <KB文件名>");
                    return true;
                }

                if (!plugin.getKBLoader().getKbMap().containsKey(args[1])) {
                    sender.sendMessage(prefix + " §cKB文件 " + args[1] + " 不存在!");
                    return true;
                }

                sender.sendMessage(prefix + " §7查看KB文件 §f" + args[1] + " §7的数值:");

                StringBuilder message = new StringBuilder();

                formatConfigSection(message, plugin.getKBLoader().getConfig(args[1]), 1);

                for (String line : message.toString().split("\n"))
                    sender.sendMessage("§e" + line);

                return true;
            }
            case "reload": {
                if (args.length < 2) {
                    sender.sendMessage(prefix + " §c请选择要重载的部分:");
                    sender.sendMessage("  §7主配置: §f/" + label + " reload config");
                    sender.sendMessage("  §7KB配置: §f/" + label + " reload kb <KB文件名|*>");
                    return true;
                }

                String input = args[1].toLowerCase();

                if (input.equals("config")) {
                    plugin.reload();
                    sender.sendMessage(KnockbackManager.PREFIX + " §a主配置文件已重载!");
                    return true;

                } else if (input.equals("kb")) {
                    if (args.length < 3) {
                        sender.sendMessage(prefix + " §c用法: /" + label + " reload kb <KB文件名|*>");
                        return true;
                    }

                    plugin.getKBLoader().reload(args[2], sender);
                    plugin.getGuiManager().disable();
                    plugin.getGuiManager().enable();
                    return true;

                } else {
                    sender.sendMessage(KnockbackManager.PREFIX + " §c未知部分: " + args[1]);
                    return true;
                }
            }
            case "gui": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(KnockbackManager.PREFIX + " §c此命令仅玩家可使用!");
                    return false;
                }

                plugin.getGuiManager().getMainGui().openGui((Player) sender);
                return true;
            }
            case "getprofile": {
                if (args.length != 2) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " getprofile <玩家>");
                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[1]);

                if (player == null) {
                    sender.sendMessage(prefix + " §c没有找到名为 " + args[1] + " 的玩家!");
                    return true;
                }

                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());

                sender.sendMessage(prefix + (data.isExcluded() ? " §c" + player.getName() + " 当前位于过滤名单中!"
                        : " §f" + player.getName() + " §7当前使用的KB为: §f" + data.getProfile()));

                return true;
            }
            case "setprofile": {
                if (args.length != 3) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " setprofile <玩家|*> <KB文件名>");
                    return true;
                }

                if (!plugin.getKBLoader().getKbMap().containsKey(args[2])) {
                    sender.sendMessage(prefix + " §cKB文件 " + args[2] + " 不存在!");
                    return true;
                }

                if (args[1].equals("*")) {
                    plugin.getDataManager().getAllData().forEach(data -> data.setProfile(args[2]));

                    sender.sendMessage(prefix + " §7已将所有玩家使用的KB设置为 §f" + args[2]);

                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[1]);

                if (player == null) {
                    sender.sendMessage(prefix + " §c没有找到名为 " + args[1] + " 的玩家!");
                    return true;
                }

                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());

                if (data.getProfile().equals(args[2])) {
                    sender.sendMessage(prefix + " §c无变化. " + player.getName() + " 原已使用 " + args[2]);
                    return true;
                }

                data.setProfile(args[2]);

                sender.sendMessage(prefix + " §7已将 §f" + player.getName() + " §7使用的KB设置为 §f" + args[2]);

                return true;
            }
            case "setexcluded": {
                if (args.length != 3) {
                    sender.sendMessage(prefix + " §c用法: /" + label + " setexcluded <玩家> <true/false>");
                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[1]);

                if (player == null) {
                    sender.sendMessage(prefix + " §c没有找到名为 " + args[1] + " 的玩家!");
                    return true;
                }

                boolean value;

                String input = args[2].toLowerCase();

                if (input.equals("true") || input.equals("false")) value = Boolean.parseBoolean(input);
                else {
                    sender.sendMessage(prefix + " §c无效的布尔值: " + args[2]);
                    return true;
                }

                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());

                if (data.isExcluded() == value) {
                    sender.sendMessage(prefix + " §c无变化. " + player.getName() + " 原已" + (value ? "加入" : "移出") + "排除名单!");
                    return true;
                }

                data.setExcluded(value);

                sender.sendMessage(prefix + " §7已将 §f" + player.getName() + " " + (value ? "§a加入" : "§c移出") + " §f排除名单!");

                return true;
            }
            case "debug": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(prefix + " §c此命令仅玩家可使用!");
                    return true;
                }

                Player player = (Player) sender;

                PlayerData data = plugin.getDataManager().getData(player.getUniqueId());

                data.setDebugging(!data.isDebugging());

                sender.sendMessage(prefix + " §f调试模式现已" + (data.isDebugging() ? "§a开启" : "§c关闭") + "§f!");

                return true;
            }
            default: {
                sender.sendMessage(prefix + " §c未知子命令: " + args[0]);
                return true;
            }
        }
    }

    private void formatConfigSection(StringBuilder builder, ConfigurationSection section, int indentLV) {
        StringBuilder indent = new StringBuilder();

        for (int i = 0; i < indentLV; i++) indent.append("  ");

        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                builder.append("§e").append(indent).append(key).append("§f");

                if (key.equals("packet") && plugin.getProtocolManager() == null) {
                    builder.append(": §c(未检测到ProtocolLib 5+)\n");
                } else if (key.equals("misplace") && ConfigValue.SSHD_ENABLED) {
                    builder.append(": §c(与服务器端命中检测不兼容)\n");
                } else if (key.equals("modern") && !plugin.isAtLeast1_16()) {
                    builder.append(": §c(1.16+)\n");
                } else {
                    builder.append(":\n");
                }

                formatConfigSection(builder, section.getConfigurationSection(key), indentLV + 1);

            } else {
                builder.append("§7").append(indent).append(key)
                        .append("§f").append(": ").append(section.get(key))
                        .append("\n");
            }
        }
    }
}
