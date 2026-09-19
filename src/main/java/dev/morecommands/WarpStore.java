package dev.morecommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.morecommands.HomeStore.Home;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Global warp storage, saved as morecommands_warps.json inside the world folder. */
public final class WarpStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE = new TypeToken<Map<String, Home>>() {}.getType();

	private static Map<String, Home> data = new TreeMap<>();
	private static Path file;

	private WarpStore() {}

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("morecommands_warps.json");
		data = new TreeMap<>();

		if (!Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file)) {
			Map<String, Home> loaded = GSON.fromJson(reader, TYPE);
			if (loaded != null) {
				data = new TreeMap<>(loaded);
			}
		} catch (IOException | JsonParseException e) {
			MoreCommands.LOGGER.error("Could not read warps from {}", file, e);
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
			MoreCommands.LOGGER.error("Could not save warps to {}", file, e);
		}
	}

	public static Map<String, Home> warps() {
		return data;
	}

	public static List<String> names() {
		return new ArrayList<>(new TreeSet<>(data.keySet()));
	}
}
