package dev.morecommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Per-world home storage, saved as morecommands_homes.json inside the world folder. */
public final class HomeStore {
	/** A saved position. Also used for /back. */
	public static final class Home {
		public String dimension;
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE = new TypeToken<Map<String, Map<String, Home>>>() {}.getType();

	/** player uuid -> (home name -> home) */
	private static Map<String, Map<String, Home>> data = new HashMap<>();
	private static Path file;

	private HomeStore() {}

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("morecommands_homes.json");
		data = new HashMap<>();

		if (!Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file)) {
			Map<String, Map<String, Home>> loaded = GSON.fromJson(reader, TYPE);
			if (loaded != null) {
				data = loaded;
			}
		} catch (IOException | JsonParseException e) {
			MoreCommands.LOGGER.error("Could not read homes from {}", file, e);
		}
	}

	public static void save() {
		if (file == null) {
			return;
		}

		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(data, TYPE, writer);
			}
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			MoreCommands.LOGGER.error("Could not save homes to {}", file, e);
		}
	}

	/** Mutable map of the player's homes. Creates an empty one if needed. */
	public static Map<String, Home> homes(UUID player) {
		return data.computeIfAbsent(player.toString(), key -> new TreeMap<>());
	}

	/** Sorted home names, without creating anything. */
	public static List<String> names(UUID player) {
		Map<String, Home> homes = data.get(player.toString());
		if (homes == null) {
			return List.of();
		}
		return new ArrayList<>(new TreeSet<>(homes.keySet()));
	}
}
