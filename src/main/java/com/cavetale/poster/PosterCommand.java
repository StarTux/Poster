package com.cavetale.poster;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.util.Json;
import com.cavetale.poster.save.Poster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public final class PosterCommand extends AbstractCommand<PosterPlugin> {
    protected final Map<UUID, List<GlowItemFrame>> placedEntities = new HashMap<>();

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
        rootNode.addChild("place").arguments("<poster>")
            .completers(posterNameCompleter)
            .description("Place the poster")
            .playerCaller(this::place);
        rootNode.addChild("undo").denyTabCompletion()
            .description("Undo poster placement")
            .playerCaller(this::undo);
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

    protected boolean place(Player player, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        Poster poster = plugin.findPosterNamed(args[0]);
        if (poster == null) throw new CommandWarn("Poster not found: " + args[0]);
        RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
        if (rayTraceResult == null) {
            throw new CommandWarn("Not looking at block!");
        }
        Block block = rayTraceResult.getHitBlock();
        BlockFace face = rayTraceResult.getHitBlockFace();
        final BlockFace right;
        final BlockFace down;
        final Rotation rotation;
        switch (face) {
        case SOUTH:
        case NORTH:
        case EAST:
        case WEST:
            right = ccw(face);
            down = BlockFace.DOWN;
            rotation = Rotation.NONE;
            break;
        case DOWN:
            down = yawFacing(player.getLocation()).getOppositeFace();
            right = cw(down);
            rotation = rotDown(down);
            break;
        case UP:
            down = yawFacing(player.getLocation()).getOppositeFace();
            right = ccw(down);
            rotation = rotDown(down);
            break;
        default: throw new IllegalStateException(face.name());
        }
        int count = 0;
        Block origin = block.getRelative(face);
        int mapIndex = 0;
        List<GlowItemFrame> entityList = new ArrayList<>();
        placedEntities.put(player.getUniqueId(), entityList);
        for (int y = 0; y < poster.getHeight(); y += 1) {
            for (int x = 0; x < poster.getWidth(); x += 1) {
                Block mapBlock = origin.getRelative(right, x).getRelative(down, y);
                Location location = mapBlock.getLocation().add(0.5, 0.5, 0.5);
                int mapId = poster.getMapIds().get(mapIndex++);
                GlowItemFrame glowItemFrame = location.getWorld().spawn(location, GlowItemFrame.class, e -> {
                        entityList.add(e);
                        e.setPersistent(true);
                        e.setFixed(true);
                        e.setVisible(false);
                        e.setItem(plugin.createPosterMapItem(mapId));
                        e.setFacingDirection(face);
                        e.setRotation(rotation);
                    });
                if (glowItemFrame == null || glowItemFrame.isDead()) {
                    player.sendMessage(Component.text("Could not spawn frame at"
                                                      + " " + mapBlock.getX()
                                                      + " " + mapBlock.getY()
                                                      + " " + mapBlock.getZ(),
                                                      NamedTextColor.RED));
                } else {
                    count += 1;
                }
            }
        }
        player.sendMessage(Component.text(count + " maps of poster " + poster.getName()
                                          + " placed against "
                                          + " " + block.getX()
                                          + " " + block.getY()
                                          + " " + block.getZ(),
                                          NamedTextColor.YELLOW));
        return true;
    }

    protected boolean undo(Player player, String[] args) {
        List<GlowItemFrame> entityList = placedEntities.remove(player.getUniqueId());
        if (entityList == null || entityList.isEmpty()) {
            throw new CommandWarn("You didn't place a poster");
        }
        for (GlowItemFrame entity : entityList) {
            entity.remove();
        }
        player.sendMessage(Component.text(entityList.size() + " poster entities removed",
                                          NamedTextColor.YELLOW));
        return true;
    }

    protected static BlockFace cw(BlockFace in) {
        switch (in) {
        case NORTH: return BlockFace.EAST;
        case EAST: return BlockFace.SOUTH;
        case SOUTH: return BlockFace.WEST;
        case WEST: return BlockFace.NORTH;
        default: throw new IllegalArgumentException(in.name());
        }
    }

    protected static BlockFace ccw(BlockFace in) {
        switch (in) {
        case NORTH: return BlockFace.WEST;
        case EAST: return BlockFace.NORTH;
        case SOUTH: return BlockFace.EAST;
        case WEST: return BlockFace.SOUTH;
        default: throw new IllegalArgumentException(in.name());
        }
    }

    protected static BlockFace yawFacing(Location location) {
        float yaw = location.getYaw();
        while (yaw < 0) yaw += 360.0;
        while (yaw > 360.0) yaw -= 360.0;
        if (yaw < 45.0f) return BlockFace.SOUTH;
        // 90
        if (yaw < 135.0f) return BlockFace.WEST;
        // 180
        if (yaw < 225.0f) return BlockFace.NORTH;
        // 270
        if (yaw < 315.0f) return BlockFace.EAST;
        // 360
        return BlockFace.SOUTH;
    }

    protected static Rotation rotDown(BlockFace down) {
        switch (down) {
        case SOUTH: return Rotation.NONE;
        case WEST: return Rotation.CLOCKWISE_45; // ???
        case NORTH: return Rotation.CLOCKWISE;
        case EAST: return Rotation.COUNTER_CLOCKWISE_45; // ???
        default: throw new IllegalArgumentException(down.name());
        }
    }
}
