package org.baicaizhale.litemotto;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LiteMotto命令的Tab补全实现类
 * 提供命令参数的自动补全功能
 */
public class LiteMottoTabCompleter implements TabCompleter {

    private static final List<String> COMMANDS = Arrays.asList("gen", "debug");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 只在第一个参数时提供补全
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            // 过滤出以用户输入开头的命令
            for (String cmd : COMMANDS) {
                if (cmd.startsWith(partialCommand)) {
                    completions.add(cmd);
                }
            }
        }
        
        return completions;
    }
}