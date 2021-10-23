package com.cavetale.poster;

import java.awt.image.BufferedImage;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Render one BufferedImage onto one MapCanvas.  This class does not
 * know about the MapView, Map ID, or the Poster, at all.
 */
final class PosterRenderer extends MapRenderer {
    private final PosterPlugin plugin;
    @Setter protected BufferedImage image;
    @Setter protected boolean drawn = false;

    PosterRenderer(final PosterPlugin plugin, final BufferedImage image) {
        super(false);
        this.plugin = plugin;
        this.image = image;
    }

    @Override
    public void initialize(MapView map) { }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (drawn || image == null) return;
        canvas.drawImage(0, 0, image);
        drawn = true;
    }
}
