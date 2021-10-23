package com.cavetale.poster;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.poster.save.Poster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PosterCommand extends AbstractCommand<PosterPlugin> {
    protected PosterCommand(final PosterPlugin plugin) {
        super(plugin, "poster");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("list").denyTabCompletion()
            .description("List posters")
            .senderCaller(this::list);
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload posters")
            .senderCaller(this::reload);
        rootNode.addChild("create").arguments("<imageFile>")
            .completers(CommandArgCompleter.supplyList(() -> List.of(plugin.imagesFolder.list())))
            .description("Create a poster")
            .senderCaller(this::create);
        CommandArgCompleter posterNameCompleter = CommandArgCompleter.supplyList(() -> {
                List<String> result = new ArrayList<>(plugin.posterList.size());
                for (Poster poster : plugin.posterList) {
                    result.add(poster.getName());
                }
                return result;
            });
        rootNode.addChild("give").arguments("<poster> [target]")
            .completers(posterNameCompleter,
                        CommandArgCompleter.NULL)
            .description("Give poster items to player")
            .senderCaller(this::give);
    }

    protected boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage(Component.text("" + plugin.posterList.size() + " posters loaded",
                                          NamedTextColor.YELLOW));
        for (Poster poster : plugin.posterList) {
            sender.sendMessage(Component.text(Json.serialize(poster),
                                              NamedTextColor.YELLOW));
        }
        return true;
    }

    protected boolean reload(CommandSender sender, String[] args) {
        plugin.unloadPosters();
        plugin.loadPosters();
        sender.sendMessage(Component.text("" + plugin.posterList.size() + " posters reloaded",
                                          NamedTextColor.YELLOW));
        return true;
    }

    protected boolean create(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        File imageFile = new File(plugin.imagesFolder, args[0]);
        Poster poster = plugin.createPoster(imageFile);
        if (poster == null) {
            throw new CommandWarn("Poster creation failed. See console");
        }
        sender.sendMessage(Component.text("Poster created: " + Json.serialize(poster),
                                          NamedTextColor.YELLOW));
        return true;
    }

    protected boolean give(CommandSender sender, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        Poster poster = plugin.findPosterNamed(args[0]);
        if (poster == null) throw new CommandWarn("Poster not found: " + args[0]);
        Player target;
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                throw new CommandWarn("[poster:give] player expected");
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                throw new CommandWarn("Player not found: " + args[1]);
            }
        }
        plugin.givePosterMaps(target, poster);
        sender.sendMessage(Component.text("Maps of poster " + poster.getName()
                                          + "given to " + target.getName(),
                                          NamedTextColor.YELLOW));
        return true;
    }
}
