package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.morecommands.Msg;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** /ping, /pos */
public final class InfoCommands {
	private InfoCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ping").executes(InfoCommands::ping));
		dispatcher.register(Commands.literal("pos").executes(InfoCommands::pos));
	}

	private static int ping(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		int latency = player.connection.latency();

		ctx.getSource().sendSuccess(() -> Msg.ok("Your ping is " + latency + " ms."), false);
		return latency;
	}

	private static int pos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();

		int x = player.getBlockX();
		int y = player.getBlockY();
		int z = player.getBlockZ();
		String dimension = level.dimension().identifier().toString();

		String text = "X " + x + ", Y " + y + ", Z " + z + " in " + dimension
			+ " (chunk " + (x >> 4) + ", " + (z >> 4) + ")";
		ctx.getSource().sendSuccess(() -> Msg.ok(text), false);
		return Command.SINGLE_SUCCESS;
	}
}
