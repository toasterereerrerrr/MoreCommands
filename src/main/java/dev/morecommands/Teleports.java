package dev.morecommands;

import dev.morecommands.HomeStore.Home;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Teleport helper that also tracks the last position for /back. */
public final class Teleports {
	private static final Map<UUID, Home> BACK = new HashMap<>();

	private Teleports() {}

	public static Home locationOf(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		Home home = new Home();
		home.dimension = level.dimension().identifier().toString();
		home.x = player.getX();
		home.y = player.getY();
		home.z = player.getZ();
		home.yaw = player.getYRot();
		home.pitch = player.getXRot();
		return home;
	}

	public static void rememberBack(ServerPlayer player) {
		BACK.put(player.getUUID(), locationOf(player));
	}

	public static Home back(ServerPlayer player) {
		return BACK.get(player.getUUID());
	}

	/** Teleports to a saved position. Returns false if its dimension no longer exists. */
	public static boolean teleport(ServerPlayer player, Home destination) {
		ServerLevel here = (ServerLevel) player.level();
		MinecraftServer server = here.getServer();

		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(destination.dimension));
		ServerLevel target = server.getLevel(key);
		if (target == null) {
			return false;
		}

		teleport(player, target, destination.x, destination.y, destination.z, destination.yaw, destination.pitch);
		return true;
	}

	public static void teleport(ServerPlayer player, ServerLevel target, double x, double y, double z, float yaw, float pitch) {
		Home previous = locationOf(player);
		player.teleportTo(target, x, y, z, Set.of(), yaw, pitch, true);
		BACK.put(player.getUUID(), previous);
	}
}
