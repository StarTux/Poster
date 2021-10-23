package com.cavetale.poster.save;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Save one poster to disk in the posters folder.  That is, remember
 * one image filename and a list of Map Ids which will display it
 * in-game.
 */
@Data @NoArgsConstructor
public final class Poster {
    /**
     * The name corresponds with the file name without suffix.
     */
    protected String name;
    /**
     * Image files reside in the images folder.
     */
    protected String imageFilename;
    protected List<Integer> mapIds;
    protected List<String> chat;

    public Poster(final String name, final String theImageFileName, final List<Integer> mapIds) {
        this.name = name;
        this.imageFilename = theImageFileName;
        this.mapIds = mapIds;
    }
}
