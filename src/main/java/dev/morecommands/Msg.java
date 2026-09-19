package dev.morecommands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class Msg {
	private static final int GREEN = 0x55FF55;

	private Msg() {}

	/** Green success text. */
	public static MutableComponent ok(String text) {
		return Component.literal(text).withStyle(style -> style.withColor(GREEN));
	}

	/** An exception Brigadier shows to the player as a red error message. */
	public static CommandSyntaxException fail(String text) {
		return new SimpleCommandExceptionType(Component.literal(text)).create();
	}

	/** "Steve" for one player, "3 players" for several. */
	public static String describe(Collection<ServerPlayer> players) {
		if (players.size() == 1) {
			return players.iterator().next().getName().getString();
		}
		return players.size() + " players";
	}
}
