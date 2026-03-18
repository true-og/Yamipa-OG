package io.josemmo.bukkit.plugin.commands;

import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import io.josemmo.bukkit.plugin.renderer.ImageRenderer;
import io.josemmo.bukkit.plugin.renderer.ItemService;
import io.josemmo.bukkit.plugin.storage.ImageFile;
import io.josemmo.bukkit.plugin.utils.ActionBar;
import io.josemmo.bukkit.plugin.utils.Permissions;
import io.josemmo.bukkit.plugin.utils.SelectBlockTask;
import net.trueog.utilitiesog.UtilitiesOG;

public class ImageCommand {

    public static final int ITEMS_PER_PAGE = 9;

    private static void sendMessage(@NotNull CommandSender sender, @NotNull String message) {

        if (sender instanceof Player) {

            UtilitiesOG.trueogMessage((Player) sender, message);

        } else {

            UtilitiesOG.logToConsole(YamipaPlugin.getPrefix(), message);

        }

    }

    public static void showHelp(@NotNull CommandSender s, @NotNull String commandName) {

        final String cmd = "/" + commandName;
        sendMessage(s, "&l=== Yamipa-OG Plugin Help ===");
        sendMessage(s, "&b" + cmd + "&r - Show this help");
        if (s.hasPermission("yamipa.command.clear") || s.hasPermission("yamipa.clear")) {

            sendMessage(s, "&b" + cmd + " clear <x z w> <r> [<player>]&r - Remove placed images");

        }

        if (s.hasPermission("yamipa.command.describe") || s.hasPermission("yamipa.describe")) {

            sendMessage(s, "&b" + cmd + " describe&r - Describe placed image");

        }

        if (s.hasPermission("yamipa.command.download") || s.hasPermission("yamipa.download")) {

            sendMessage(s, "&b" + cmd + " download <url> <filename>&r - Download image");

        }

        if (s.hasPermission("yamipa.command.give") || s.hasPermission("yamipa.give")) {

            sendMessage(s, "&b" + cmd + " give <p> <filename> <#> <w> [<h>] [<f>]&r - Give items");

        }

        if (s.hasPermission("yamipa.command.list") || s.hasPermission("yamipa.list")) {

            sendMessage(s, "&b" + cmd + " list [<page>]&r - List all images");

        }

        if (s.hasPermission("yamipa.command.place") || s.hasPermission("yamipa.place")) {

            sendMessage(s, "&b" + cmd + " place <filename> <w> [<h>] [<f>]&r - Place image");

        }

        if (s.hasPermission("yamipa.command.remove.own") || s.hasPermission("yamipa.remove")) {

            sendMessage(s, "&b" + cmd + " remove&r - Remove a single placed image");

        }

        if (s.hasPermission("yamipa.command.top") || s.hasPermission("yamipa.top")) {

            sendMessage(s, "&b" + cmd + " top&r - List players with the most images");

        }

    }

    public static void listImages(@NotNull CommandSender sender, int page) {

        final String[] filenames = YamipaPlugin.getInstance().getStorage().getAllFilenames();
        final int numOfImages = filenames.length;

        // Are there any images available?
        if (numOfImages == 0) {

            sendMessage(sender, "&cNo images found in the images directory");
            return;

        }

        // Is the page number valid?
        final int firstImageIndex = Math.max(page - 1, 0) * ITEMS_PER_PAGE;
        if (firstImageIndex >= numOfImages) {

            sendMessage(sender, "&cPage " + page + " not found");
            return;

        }

        // Render list of images
        final int stopImageIndex = (page == 0) ? numOfImages : Math.min(numOfImages, firstImageIndex + ITEMS_PER_PAGE);
        if (page > 0) {

            final int maxPage = (int) Math.ceil((float) numOfImages / ITEMS_PER_PAGE);
            sendMessage(sender, "=== Page " + page + " out of " + maxPage + " ===");

        }

        for (int i = firstImageIndex; i < stopImageIndex; ++i) {

            sendMessage(sender, "&6" + filenames[i]);

        }

    }

    public static void downloadImage(@NotNull CommandSender sender, @NotNull String rawUrl, @NotNull String filename) {

        final YamipaPlugin plugin = YamipaPlugin.getInstance();

        // Validate destination file
        final Path basePath = Paths.get(plugin.getStorage().getBasePath());
        final Path destPath = basePath.resolve(filename);
        if (!destPath.getParent().equals(basePath)) {

            sendMessage(sender, "&cNot a valid destination filename");
            return;

        }

        if (destPath.toFile().exists()) {

            sendMessage(sender, "&cThere's already a file with that name");
            return;

        }

        // Validate and fix remote URL
        URL url;
        String referrer = null;
        try {

            url = new URL(rawUrl);
            // Giphy.com
            if ("giphy.com".equals(url.getHost())) {

                final String path = url.getPath();
                final String id = StringUtils.substring(path, path.lastIndexOf('-') + 1);
                url = new URL("https://media.giphy.com/media/" + id + "/giphy.gif");
                referrer = "https://giphy.com/";

            }

            // Imgur.com
            if ("imgur.com".equals(url.getHost())) {

                final String[] parts = url.getPath().replaceAll("^/|/$", "").split("/");
                if (parts.length == 2 && ("a".equals(parts[0]) || "gallery".equals(parts[0]))) {

                    url = new URL("https://imgur.com/a/" + parts[1] + "/zip");
                    referrer = "https://imgur.com/a/" + parts[1];

                } else {

                    url = new URL("https://imgur.com/download/" + parts[parts.length - 1] + "/");
                    referrer = "https://imgur.com/" + parts[parts.length - 1];

                }

            }

        } catch (MalformedURLException malformedURLException) {

            sendMessage(sender, "&cThe remote URL is not valid");
            return;

        }

        // Download and validate remote file
        final URL finalUrl = url;
        final String finalReferrer = referrer;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            try {

                final URLConnection conn = finalUrl.openConnection();
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("User-Agent",
                        plugin.getPluginMeta().getName() + "/" + plugin.getPluginMeta().getVersion());
                if (finalReferrer != null) {

                    conn.setRequestProperty("Referer", finalReferrer);

                }

                // Download file
                sendMessage(sender, "Downloading file...");
                Files.copy(conn.getInputStream(), destPath);

                // Validate downloaded file
                if (ImageIO.read(destPath.toFile()) == null) {

                    throw new IllegalArgumentException("The downloaded file is not a valid image");

                }

                // Notify sender
                sendMessage(sender, "&aDone!");

            } catch (IOException e) {

                sendMessage(sender, "&cAn error occurred trying to download the remote file");
                plugin.warning("Failed to download file from \"" + finalUrl + "\": " + e.getClass().getName());

            } catch (IllegalArgumentException e) {

                if (Files.exists(destPath) && !destPath.toFile().delete()) {

                    plugin.warning("Failed to delete corrupted file \"" + destPath + "\"");

                }

                sendMessage(sender, "&c" + e.getMessage());

            }

        });

    }

    public static void placeImage(@NotNull Player player, @NotNull ImageFile image, int width, int height, int flags) {

        // Get image size in blocks
        final Dimension sizeInPixels = image.getSize();
        if (sizeInPixels == null) {

            UtilitiesOG.trueogMessage(player, "&cThe requested file is not a valid image");
            return;

        }

        final int finalHeight = (height == 0) ? FakeImage.getProportionalHeight(sizeInPixels, width) : height;

        // Ask player where to place image
        final SelectBlockTask task = new SelectBlockTask(player);
        task.onSuccess((location, face) -> placeImage(player, image, width, finalHeight, flags, location, face));
        task.onFailure(() -> ActionBar.send(player, "&cImage placing canceled"));
        task.run("Right click a block to continue");

    }

    public static boolean placeImage(@NotNull Player player, @NotNull ImageFile image, int width, int height, int flags,
            @NotNull Location location, @NotNull BlockFace face)
    {

        final ImageRenderer renderer = YamipaPlugin.getInstance().getRenderer();

        // Create new fake image instance
        final Rotation rotation = FakeImage.getRotationFromPlayerEyesight(face, player.getEyeLocation());
        final FakeImage fakeImage = new FakeImage(image.getName(), location, face, rotation, width, height, new Date(),
                player, flags);

        // Make sure image can be placed
        for (Location loc : fakeImage.getAllLocations()) {

            if (!Permissions.canBuild(player, loc)) {

                ActionBar.send(player, "&cYou're not allowed to place an image here!");
                return false;

            }

            if (renderer.getImage(loc, face) != null) {

                ActionBar.send(player, "&cThere's already an image there!");
                return false;

            }

        }

        // Show loading status to player
        final ActionBar loadingActionBar = ActionBar.repeat(player, "&bLoading image...");
        fakeImage.setOnLoadedListener(loadingActionBar::clear);

        // Add fake image to renderer
        renderer.addImage(fakeImage);
        return true;

    }

    public static void removeImage(@NotNull Player player) {

        final SelectBlockTask task = new SelectBlockTask(player);
        task.onSuccess((location, face) -> {

            final FakeImage image = YamipaPlugin.getInstance().getRenderer().getImage(location, face);
            if (image == null) {

                ActionBar.send(player, "&cThat is not a valid image!");
                return;

            }

            // Check player's command permissions
            if (!player.getUniqueId().equals(image.getPlacedBy().getUniqueId())
                    && !player.hasPermission("yamipa.command.remove") && !player.hasPermission("yamipa.remove"))
            {

                ActionBar.send(player, "&cYou cannot remove images from other players!");
                return;

            }

            // Attempt to remove image
            removeImage(player, image);

        });

        task.onFailure(() -> ActionBar.send(player, "&cImage removing canceled"));
        task.run("Right click an image to continue");

    }

    public static boolean removeImage(@NotNull Player player, @NotNull FakeImage image) {

        // Check block permissions
        for (Location loc : image.getAllLocations()) {

            if (!Permissions.canDestroy(player, loc)) {

                ActionBar.send(player, "&cYou're not allowed to remove this image!");
                return false;

            }

        }

        // Trigger image removal
        YamipaPlugin.getInstance().getRenderer().removeImage(image);
        return true;

    }

    public static void clearImages(@NotNull CommandSender sender, @NotNull Location origin, int radius,
            @Nullable OfflinePlayer placedBy)
    {

        final ImageRenderer renderer = YamipaPlugin.getInstance().getRenderer();

        // Get images in area
        final Set<FakeImage> images = renderer.getImages(origin.getWorld(), origin.getBlockX() - radius + 1,
                origin.getBlockX() + radius - 1, origin.getBlockZ() - radius + 1, origin.getBlockZ() + radius - 1);

        // Filter out images not placed by targeted player
        if (placedBy != null) {

            final UUID target = placedBy.getUniqueId();
            images.removeIf(image -> !target.equals(image.getPlacedBy().getUniqueId()));

        }

        // Filter out images outside the permission scope of the sender
        if (sender instanceof Player senderAsPlayer) {

            images.removeIf(image -> {

                for (Location loc : image.getAllLocations()) {

                    if (!Permissions.canDestroy(senderAsPlayer, loc)) {

                        return true;

                    }

                }

                return false;

            });

        }

        // Remove found images
        images.forEach(renderer::removeImage);

        sendMessage(sender, "Removed " + images.size() + " placed image(s)");

    }

    public static void describeImage(@NotNull Player player) {

        final ImageRenderer renderer = YamipaPlugin.getInstance().getRenderer();

        // Ask user to select fake image
        final SelectBlockTask task = new SelectBlockTask(player);
        task.onSuccess((location, face) -> {

            final FakeImage image = renderer.getImage(location, face);
            if (image == null) {

                ActionBar.send(player, "&cThat is not a valid image!");
                return;

            }

            // Separate previous messages
            UtilitiesOG.trueogMessage(player, "");

            // Basic information
            UtilitiesOG.trueogMessage(player, "&6Filename: &r" + image.getFilename());
            UtilitiesOG.trueogMessage(player, "&6World: &r" + image.getLocation().getChunk().getWorld().getName());
            UtilitiesOG.trueogMessage(player, "&6Coordinates: &r" + image.getLocation().getBlockX() + ", "
                    + image.getLocation().getBlockY() + ", " + image.getLocation().getBlockZ());
            UtilitiesOG.trueogMessage(player, "&6Block Face: &r" + image.getBlockFace());
            UtilitiesOG.trueogMessage(player, "&6Rotation: &r" + image.getRotation());
            UtilitiesOG.trueogMessage(player,
                    "&6Dimensions: &r" + image.getWidth() + "x" + image.getHeight() + " blocks");

            // Speed
            final int delay = image.getDelay() * 50;
            final String delayStr = (delay > 0) ? delay + " ms per step" : "&7N/A";
            UtilitiesOG.trueogMessage(player, "&6Speed: &r" + delayStr);

            // Placed At
            final String dateStr = (image.getPlacedAt() == null) ? "&7Some point in time"
                    : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(image.getPlacedAt());
            UtilitiesOG.trueogMessage(player, "&6Placed At: &r" + dateStr);

            // Placed By
            final String playerStr;
            if (image.getPlacedBy().getUniqueId().equals(FakeImage.UNKNOWN_PLAYER_ID)) {

                playerStr = "&7Someone";

            } else if (image.getPlacedBy().getName() == null) {

                playerStr = "&3" + image.getPlacedBy().getUniqueId();

            } else {

                playerStr = image.getPlacedBy().getName();

            }

            UtilitiesOG.trueogMessage(player, "&6Placed By: &r" + playerStr);

            // Flags
            String flagsStr = "";
            if (image.hasFlag(FakeImage.FLAG_ANIMATABLE)) {

                flagsStr += "&bANIM ";

            }

            if (image.hasFlag(FakeImage.FLAG_REMOVABLE)) {

                flagsStr += "&cREMO ";

            }

            if (image.hasFlag(FakeImage.FLAG_DROPPABLE)) {

                flagsStr += "&dDROP ";

            }

            if (image.hasFlag(FakeImage.FLAG_GLOWING)) {

                flagsStr += "&aGLOW ";

            }

            if (StringUtils.isEmpty(flagsStr)) {

                flagsStr = "&7N/A";

            }

            UtilitiesOG.trueogMessage(player, "&6Flags: &r" + flagsStr);

        });
        task.onFailure(() -> ActionBar.send(player, "&cImage describing canceled"));
        task.run("Right click the image to describe");

    }

    public static void showTopPlayers(@NotNull CommandSender sender) {

        final UUID senderId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;
        final Map<OfflinePlayer, Integer> stats = YamipaPlugin.getInstance().getRenderer().getImagesCountByPlayer();

        // Render header
        sendMessage(sender, "=== Top players with the most placed images ===");
        if (stats.isEmpty()) {

            sendMessage(sender, "&cNo one on this server has placed a single image!");
            return;

        }

        int rank = 0;
        int printedLines = 0;
        boolean hasShownSender = (senderId == null); // Assume sender has already been shown if it's not a player
        for (Map.Entry<OfflinePlayer, Integer> item : stats.entrySet()) {

            final OfflinePlayer player = item.getKey();
            final int value = item.getValue();
            ++rank;

            // Skip line if irrelevant
            if (player.getUniqueId().equals(senderId)) {

                hasShownSender = true;

            } else if (!hasShownSender && printedLines == ITEMS_PER_PAGE - 1) {

                continue; // Leave last line empty for sender rank

            } else if (printedLines >= ITEMS_PER_PAGE) {

                break; // Stop printing players when chat is filled

            }

            // Prepare player name or UUID
            final String playerName = (player.getName() == null) ? "&6" + player.getUniqueId()
                    : "&a" + player.getName();

            // Render player line
            sendMessage(sender, "&l" + (rank > 1000 ? "1000+" : rank) + "&r. " + playerName + "&r&7 - " + value + " "
                    + (value == 1 ? "image" : "images"));
            ++printedLines;

        }

    }

    public static void giveImageItems(@NotNull CommandSender sender, @NotNull Player player, @NotNull ImageFile image,
            int amount, int width, int height, int flags)
    {

        // Get image size in blocks
        final Dimension sizeInPixels = image.getSize();
        if (sizeInPixels == null) {

            sendMessage(sender, "&cThe requested file is not a valid image");
            return;

        }

        if (height == 0) {

            height = FakeImage.getProportionalHeight(sizeInPixels, width);

        }

        // Create item stack
        final ItemStack itemStack = ItemService.getImageItem(image, amount, width, height, flags);

        // Add item stack to player's inventory
        player.getInventory().addItem(itemStack);
        sendMessage(sender, "&oAdded " + amount + " " + (amount == 1 ? "image item" : "image items") + " to "
                + player.getName() + "'s inventory");

    }

}