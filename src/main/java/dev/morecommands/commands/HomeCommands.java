package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.morecommands.Config;
import dev.morecommands.HomeStore;
import dev.morecommands.HomeStore.Home;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import dev.morecommands.Teleports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** /sethome, /home, /delhome, /homes, /back, /top */
public final class HomeCommands {
	private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
	private static final String DEFAULT_NAME = "home";

	private static final SuggestionProvider<CommandSourceStack> HOME_NAMES = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			return builder.buildFuture();
		}
		return SharedSuggestionProvider.suggest(HomeStore.names(player.getUUID()), builder);
	};

	private HomeCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("sethome")
			.executes(ctx -> setHome(ctx, DEFAULT_NAME))
			.then(Commands.argument("name", StringArgumentType.word())
				.executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "name")))));

		dispatcher.register(Commands.literal("home")
			.executes(ctx -> home(ctx, DEFAULT_NAME))
			.then(Commands.argument("name", StringArgumentType.word())
				.suggests(HOME_NAMES)
				.executes(ctx -> home(ctx, StringArgumentType.getString(ctx, "name")))));

		dispatcher.register(Commands.literal("delhome")
			.executes(ctx -> delHome(ctx, DEFAULT_NAME))
			.then(Commands.argument("name", StringArgumentType.word())
				.suggests(HOME_NAMES)
				.executes(ctx -> delHome(ctx, StringArgumentType.getString(ctx, "name")))));

		dispatcher.register(Commands.literal("homes")
			.executes(HomeCommands::listHomes));

		dispatcher.register(Commands.literal("back")
			.executes(HomeCommands::back));

		dispatcher.register(Commands.literal("top")
			.requires(Perms::gamemaster)
			.executes(HomeCommands::top));
	}

	private static String cleanName(String raw) throws CommandSyntaxException {
		String name = raw.toLowerCase(Locale.ROOT);
		if (!VALID_NAME.matcher(name).matches()) {
			throw Msg.fail("Home names can use letters, numbers, '_' and '-' (up to 32 characters).");
		}
		return name;
	}

	private static int setHome(CommandContext<CommandSourceStack> ctx, String rawName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = cleanName(rawName);

		Map<String, Home> homes = HomeStore.homes(player.getUUID());
		int limit = Config.get().homeLimit;
		if (!homes.containsKey(name) && limit > 0 && homes.size() >= limit) {
			throw Msg.fail("You have reached the limit of " + limit + " homes. Use /delhome to free one up.");
		}

		homes.put(name, Teleports.locationOf(player));
		HomeStore.save();

		ctx.getSource().sendSuccess(() -> Msg.ok("Home '" + name + "' set."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int home(CommandContext<CommandSourceStack> ctx, String rawName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = cleanName(rawName);

		Home home = HomeStore.homes(player.getUUID()).get(name);
		if (home == null) {
			throw Msg.fail("You don't have a home called '" + name + "'. Use /homes to see yours.");
		}
		if (!Teleports.teleport(player, home)) {
			throw Msg.fail("The dimension for home '" + name + "' no longer exists.");
		}

		ctx.getSource().sendSuccess(() -> Msg.ok("Welcome home."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int delHome(CommandContext<CommandSourceStack> ctx, String rawName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = cleanName(rawName);

		if (HomeStore.homes(player.getUUID()).remove(name) == null) {
			throw Msg.fail("You don't have a home called '" + name + "'.");
		}
		HomeStore.save();

		ctx.getSource().sendSuccess(() -> Msg.ok("Home '" + name + "' deleted."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int listHomes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		List<String> names = HomeStore.names(player.getUUID());

		if (names.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Msg.ok("You have no homes yet. Use /sethome [name] to make one."), false);
			return 0;
		}

		int limit = Config.get().homeLimit;
		String count = limit > 0 ? names.size() + "/" + limit : String.valueOf(names.size());
		ctx.getSource().sendSuccess(() -> Msg.ok("Homes (" + count + "): " + String.join(", ", names)), false);
		return names.size();
	}

	private static int back(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();

		Home previous = Teleports.back(player);
		if (previous == null) {
			throw Msg.fail("There is nowhere to go back to yet.");
		}
		if (!Teleports.teleport(player, previous)) {
			throw Msg.fail("That location's dimension no longer exists.");
		}

		ctx.getSource().sendSuccess(() -> Msg.ok("Returned to your previous location."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int top(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();

		int x = player.getBlockX();
		int z = player.getBlockZ();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

		if (y <= player.getBlockY()) {
			throw Msg.fail("You are already at the top.");
		}

		Teleports.teleport(player, level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Msg.ok("Teleported to the surface above you."), false);
		return Command.SINGLE_SUCCESS;
	}
}
