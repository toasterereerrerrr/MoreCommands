package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.morecommands.HomeStore.Home;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import dev.morecommands.Teleports;
import dev.morecommands.WarpStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** /setwarp, /warp, /delwarp, /warps */
public final class WarpCommands {
	private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");

	private static final SuggestionProvider<CommandSourceStack> WARP_NAMES = (ctx, builder) -> {
		return SharedSuggestionProvider.suggest(WarpStore.names(), builder);
	};

	private WarpCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("setwarp")
			.requires(Perms::moderator)
			.then(Commands.argument("name", StringArgumentType.word())
				.executes(ctx -> setWarp(ctx, StringArgumentType.getString(ctx, "name")))));

		dispatcher.register(Commands.literal("warp")
			.requires(Perms::utility)
			.then(Commands.argument("name", StringArgumentType.word())
				.suggests(WARP_NAMES)
				.executes(ctx -> warp(ctx, StringArgumentType.getString(ctx, "name")))));

		dispatcher.register(Commands.literal("delwarp")
			.requires(Perms::moderator)
			.then(Commands.argument("name", StringArgumentType.word())
				.suggests(WARP_NAMES)
				.executes(ctx -> delWarp(ctx, StringArgumentType.getString(ctx, "name")))));

		dispatcher.register(Commands.literal("warps")
			.requires(Perms::utility)
			.executes(WarpCommands::listWarps));
	}

	private static String cleanName(String raw) throws CommandSyntaxException {
		String name = raw.toLowerCase(Locale.ROOT);
		if (!VALID_NAME.matcher(name).matches()) {
			throw Msg.fail("Warp names can use letters, numbers, '_' and '-' (up to 32 characters).");
		}
		return name;
	}

	private static int setWarp(CommandContext<CommandSourceStack> ctx, String rawName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = cleanName(rawName);

		WarpStore.warps().put(name, Teleports.locationOf(player));
		WarpStore.save();

		ctx.getSource().sendSuccess(() -> Msg.ok("Warp '" + name + "' set."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int warp(CommandContext<CommandSourceStack> ctx, String rawName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = cleanName(rawName);

		Home warp = WarpStore.warps().get(name);
		if (warp == null) {
			throw Msg.fail("There is no warp called '" + name + "'. Use /warps to see available ones.");
		}
		if (!Teleports.teleport(player, warp)) {
			throw Msg.fail("The dimension for warp '" + name + "' no longer exists.");
		}

		ctx.getSource().sendSuccess(() -> Msg.ok("Teleported to warp '" + name + "'."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int delWarp(CommandContext<CommandSourceStack> ctx, String rawName) throws CommandSyntaxException {
		String name = cleanName(rawName);

		if (WarpStore.warps().remove(name) == null) {
			throw Msg.fail("There is no warp called '" + name + "'.");
		}
		WarpStore.save();

		ctx.getSource().sendSuccess(() -> Msg.ok("Warp '" + name + "' deleted."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int listWarps(CommandContext<CommandSourceStack> ctx) {
		List<String> names = WarpStore.names();

		if (names.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Msg.ok("There are no warps set yet."), false);
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Msg.ok("Warps (" + names.size() + "): " + String.join(", ", names)), false);
		return names.size();
	}
}
