# Poster

Make map walls from image files.

## Files

In the plugin data folder, there are two folders:

- `images` for image files
- `posters` to save created posters

## Commands
- `/poster list` List all posters
- `/poster reload` Reload posters
- `/poster create <imageFile>` Create a poster from a file in the images folder
- `/poster give <name> [player]` Give all poster maps to a player

## Poster

Posters hold the following data:

- `imageFilename` The image file name inside the images folder
- `mapIds` The list of map ids, only valid on this server
- `chat` (Optional) Chat to run when a player clicks the poster

## Known Issues

This plugin uses one or more methods marked as Deprecated by Spigot or
Paper, particularly:

- `/Bukkit::getMap(int)` which ahs been deprecated, to my knowledge without a replacement