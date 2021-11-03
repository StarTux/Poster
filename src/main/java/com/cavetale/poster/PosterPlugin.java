package com.cavetale.poster;

import com.cavetale.core.util.Json;
import com.cavetale.poster.save.Poster;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

public final class PosterPlugin extends JavaPlugin {
    protected PosterCommand posterCommand = new PosterCommand(this);
    protected PosterListener posterListener = new PosterListener(this);
    protected File postersFolder;
    protected File imagesFolder;
    protected final List<Poster> posterList = new ArrayList<>();
    protected final List<MapView> mapViewList = new ArrayList<>();

    @Override
    public void onEnable() {
        postersFolder = new File(getDataFolder(), "posters");
        imagesFolder = new File(getDataFolder(), "images");
        postersFolder.mkdirs();
        imagesFolder.mkdirs();
        posterCommand.enable();
        posterListener.enable();
        loadPosters();
    }

    @Override
    public void onDisable() {
        unloadPosters();
    }

    protected void loadPosters() {
        for (File file : postersFolder.listFiles()) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            Poster poster = Json.load(file, Poster.class);
            if (poster == null) {
                getLogger().warning("Invalid poster file: " + file);
                continue;
            }
            poster.setName(name);
            posterList.add(poster);
        }
        for (Poster poster : posterList) {
            enablePoster(poster);
        }
    }

    protected boolean enablePoster(Poster poster) {
        File file = new File(imagesFolder, poster.getImageFilename());
        if (!file.isFile()) {
            getLogger().warning(poster.getName() + ": Image file not found: " + file);
            return false;
        }
        final BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException ioe) {
            getLogger().log(Level.SEVERE, "Reading image file: " + file, ioe);
            return false;
        }
        if (poster.getWidth() == 0 || poster.getHeight() == 0) {
            int width = (image.getWidth() - 1) / 128 + 1;
            int height = (image.getWidth() - 1) / 128 + 1;
            poster.setWidth(width);
            poster.setHeight(height);
            savePoster(poster);
        }
        OUTER:
        for (int y = 0; y < poster.getHeight(); y += 1) {
            for (int x = 0; x < poster.getWidth(); x += 1) {
                int index = x + y * poster.getWidth();
                if (index >= poster.getMapIds().size()) {
                    getLogger().warning(poster.getName() + ": Map Id list too short!");
                    break OUTER;
                }
                int mapId = poster.getMapIds().get(index);
                MapView mapView = Bukkit.getMap(mapId);
                if (mapView == null) {
                    getLogger().warning(poster.getName() + ": Map View not found: " + mapId);
                    continue;
                }
                BufferedImage imagePart = new BufferedImage(128, 128, image.getType());
                Graphics graphics = imagePart.getGraphics();
                graphics.drawImage(image,
                                   0, 0, 128, 128,
                                   x * 128, y * 128, x * 128 + 128, y * 128 + 128,
                                   null);
                PosterRenderer posterRenderer = new PosterRenderer(this, imagePart);
                mapView.setCenterX(0x7fffffff);
                mapView.setCenterZ(0x7fffffff);
                mapView.setScale(MapView.Scale.FARTHEST);
                for (MapRenderer renderer : new ArrayList<>(mapView.getRenderers())) {
                    mapView.removeRenderer(renderer);
                }
                mapView.addRenderer(posterRenderer);
                mapViewList.add(mapView);
            }
        }
        return true;
    }

    protected Poster createPoster(File imageFile) {
        if (!imageFile.isFile()) {
            getLogger().warning("Image file not found: " + imageFile);
            return null;
        }
        final BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException ioe) {
            getLogger().log(Level.SEVERE, "Reading image file: " + imageFile, ioe);
            return null;
        }
        int width = (image.getWidth() - 1) / 128 + 1;
        int height = (image.getWidth() - 1) / 128 + 1;
        List<Integer> mapIds = new ArrayList<>();
        OUTER:
        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                MapView mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
                mapIds.add(mapView.getId());
            }
        }
        String name = imageFile.getName();
        name = name.substring(0, name.lastIndexOf(".")); // cut off suffix
        Poster poster = new Poster(name, imageFile.getName(), mapIds, width, height);
        if (!enablePoster(poster)) return null;
        savePoster(poster);
        posterList.add(poster);
        return poster;
    }

    protected void savePoster(Poster poster) {
        File saveFile = new File(postersFolder, poster.getName() + ".json");
        Json.save(saveFile, poster, true);
    }

    protected void unloadPosters() {
        posterList.clear();
        for (MapView mapView : mapViewList) {
            for (MapRenderer renderer : new ArrayList<>(mapView.getRenderers())) {
                mapView.removeRenderer(renderer);
            }
        }
        mapViewList.clear();
    }

    public Poster findPosterNamed(String name) {
        for (Poster poster : posterList) {
            if (name.equals(poster.getName())) return poster;
        }
        return null;
    }

    public Poster findPosterWithMapId(int mapId) {
        for (Poster poster : posterList) {
            for (int posterMapId : poster.getMapIds()) {
                if (posterMapId == mapId) return poster;
            }
        }
        return null;
    }

    public ItemStack createPosterMapItem(int mapId) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapView mapView = Bukkit.getMap(mapId);
        if (mapView == null) {
            getLogger().warning("Map View not found: " + mapId);
            return item;
        }
        item.editMeta(m -> {
                MapMeta meta = (MapMeta) m;
                meta.setScaling(false);
                meta.setMapView(mapView);
            });
        return item;
    }

    public void givePosterMaps(Player player, Poster poster) {
        for (int mapId : poster.getMapIds()) {
            ItemStack item = createPosterMapItem(mapId);
            if (!player.getInventory().addItem(item).values().isEmpty()) {
                getLogger().warning(poster.getName() + ": Player inventory is full!");
            }
        }
    }
}
