package dev.morecommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config stored at config/morecommands.json. */
public final class Config {
	/** Maximum homes per player. 0 means unlimited. */
	public int homeLimit = 5;

	/** If true, portable menus (/craft, /anvil, /ec, ...) and /hat need operator (gamemaster) permission. */
	public boolean utilityCommandsRequireOp = true;

	private static Config instance = new Config();

	public static Config get() {
		return instance;
	}

	public static void load() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("morecommands.json");

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				Config loaded = gson.fromJson(reader, Config.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonParseException e) {
				MoreCommands.LOGGER.warn("Could not read {}, using defaults.", file, e);
			}
		}

		if (instance.homeLimit < 0) {
			instance.homeLimit = 0;
		}

		// Write the file back so new options show up for existing installs.
		try (Writer writer = Files.newBufferedWriter(file)) {
			gson.toJson(instance, writer);
		} catch (IOException e) {
			MoreCommands.LOGGER.warn("Could not write {}.", file, e);
		}
	}
}
