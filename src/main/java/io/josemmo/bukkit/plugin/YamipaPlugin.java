package io.josemmo.bukkit.plugin;

import java.awt.Color;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.josemmo.bukkit.plugin.commands.ImageCommandBridge;
import io.josemmo.bukkit.plugin.renderer.FakeEntity;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import io.josemmo.bukkit.plugin.renderer.FakeMap;
import io.josemmo.bukkit.plugin.renderer.ImageRenderer;
import io.josemmo.bukkit.plugin.renderer.ItemService;
import io.josemmo.bukkit.plugin.storage.ImageStorage;
import net.trueog.utilitiesog.UtilitiesOG;

public class YamipaPlugin extends JavaPlugin {

	private static YamipaPlugin instance;
	private boolean verbose;
	private ImageStorage storage;
	private ImageRenderer renderer;
	private ItemService itemService;
	private ScheduledExecutorService scheduler;

	/**
	 * Get plugin instance
	 * 
	 * @return Plugin instance
	 */
	public static @NotNull YamipaPlugin getInstance() {

		return instance;

	}

	public static String getPrefix() {

		return "&7[&eYamipa&f-&4OG&7] ";

	}

	/**
	 * Get image storage instance
	 * 
	 * @return Image storage instance
	 */
	public @NotNull ImageStorage getStorage() {

		return storage;

	}

	/**
	 * Get image renderer instance
	 * 
	 * @return Image renderer instance
	 */
	public @NotNull ImageRenderer getRenderer() {

		return renderer;

	}

	/**
	 * Get internal tasks scheduler
	 * 
	 * @return Tasks scheduler
	 */
	public @NotNull ScheduledExecutorService getScheduler() {

		return scheduler;

	}

	@Override
	public void onLoad() {

		instance = this;

	}

	@Override
	public void onEnable() {

		// Initialize logger
		verbose = getConfig().getBoolean("verbose", false);
		if (verbose) {

			info("Running on VERBOSE mode");

		}

		// Register plugin commands
		ImageCommandBridge.register(this);

		// Read plugin configuration paths
		final Path basePath = getDataFolder().toPath();
		final String imagesPath = getConfig().getString("images-path", "images");
		final String cachePath = getConfig().getString("cache-path", "cache");
		final String dataPath = getConfig().getString("data-path", "images.dat");

		// Create image storage
		storage = new ImageStorage(basePath.resolve(imagesPath).toString(), basePath.resolve(cachePath).toString());
		try {

			storage.start();

		} catch (Exception e) {

			log(Level.SEVERE, "Failed to initialize image storage", e);

		}

		// Create image renderer
		final boolean animateImages = getConfig().getBoolean("animate-images", true);
		FakeImage.configure(animateImages);
		info(animateImages ? "Enabled image animation support" : "Image animation support is disabled");
		renderer = new ImageRenderer(basePath.resolve(dataPath).toString());
		renderer.start();

		// Create image item service
		itemService = new ItemService();
		itemService.start();

		// Create thread pool
		scheduler = Executors.newScheduledThreadPool(6);

		// Warm-up plugin dependencies
		fine("Waiting for ProtocolLib to be ready...");
		scheduler.execute(() -> {

			FakeEntity.waitForProtocolLib();
			fine("ProtocolLib is now ready");

		});
		fine("Triggered map color cache warm-up");
		FakeMap.pixelToIndex(Color.RED.getRGB()); // Ask for a color index to force cache generation

	}

	@Override
	public void onDisable() {

		// Stop plugin components
		storage.stop();
		renderer.stop();
		itemService.stop();
		storage = null;
		renderer = null;
		itemService = null;

		// Stop internal scheduler
		scheduler.shutdownNow();
		scheduler = null;

		// Remove Bukkit listeners and tasks
		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);

	}

	/**
	 * Log message
	 * 
	 * @param level   Record level
	 * @param message Message
	 * @param e       Throwable instance, NULL to ignore
	 */
	public void log(@NotNull Level level, @NotNull String message, @Nullable Throwable e) {

		// Fix log level
		if (level.intValue() < Level.INFO.intValue()) {

			if (!verbose) {
				return;
			}
			level = Level.INFO;

		}

		// Proxy record to real logger
		if (e == null) {

			UtilitiesOG.logToConsole(getPrefix(), message);

		} else {

			UtilitiesOG.logToConsole(getPrefix(), message);

			e.printStackTrace();

		}

	}

	/**
	 * Log message
	 * 
	 * @param level   Record level
	 * @param message Message
	 */
	public void log(@NotNull Level level, @NotNull String message) {

		log(level, message, null);

	}

	/**
	 * Log warning message
	 * 
	 * @param message Message
	 */
	public void warning(@NotNull String message) {

		log(Level.WARNING, message);

	}

	/**
	 * Log info message
	 * 
	 * @param message Message
	 */
	public void info(@NotNull String message) {

		log(Level.INFO, message);

	}

	/**
	 * Log fine message
	 * 
	 * @param message Message
	 */
	public void fine(@NotNull String message) {

		log(Level.FINE, message);

	}

}