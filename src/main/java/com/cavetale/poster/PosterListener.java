package com.cavetale.poster;

import com.cavetale.poster.save.Poster;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

@RequiredArgsConstructor
public final class PosterListener implements Listener {
    protected final PosterPlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    protected void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
        onClick(event.getPlayer(), itemFrame);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame)) return;
        if (!(event.getDamager() instanceof Player)) return;
        onClick((Player) event.getDamager(), (ItemFrame) event.getEntity());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    protected void onPlayerInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK:
            break;
        default: return;
        }
        if (!event.hasBlock()) return;
        Block block = event.getClickedBlock().getRelative(event.getBlockFace());
        for (ItemFrame itemFrame : block.getWorld().getNearbyEntitiesByType(ItemFrame.class,
                                                                            block.getLocation().add(0.5, 0.5, 0.5),
                                                                            0.5, 0.5, 0.5)) {
            onClick(event.getPlayer(), itemFrame);
        }
    }

    protected void onClick(Player player, ItemFrame itemFrame) {
        ItemStack itemStack = itemFrame.getItem();
        if (itemStack == null || itemStack.getType() != Material.FILLED_MAP) return;
        MapMeta meta = (MapMeta) itemStack.getItemMeta();
        MapView mapView = meta.getMapView();
        if (mapView == null) return;
        int mapId = mapView.getId();
        Poster poster = plugin.findPosterWithMapId(mapId);
        if (poster == null) return;
        if (poster.getChat() != null) {
            for (String command : poster.getChat()) {
                player.chat(command);
            }
        }
    }
}
