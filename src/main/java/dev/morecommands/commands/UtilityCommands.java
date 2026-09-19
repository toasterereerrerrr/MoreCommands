package dev.morecommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.GameType;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class UtilityCommands {
	private UtilityCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("fly")
			.requires(Perms::moderator)
			.executes(ctx -> toggleFly(ctx, List.of(ctx.getSource().getPlayerOrException())))
			.then(Commands.argument("targets", EntityArgument.players())
				.executes(ctx -> toggleFly(ctx, EntityArgument.getPlayers(ctx, "targets"))))
		);

		dispatcher.register(Commands.literal("speed")
			.requires(Perms::moderator)
			.then(Commands.argument("value", FloatArgumentType.floatArg(0, 10))
				.executes(ctx -> setSpeed(ctx, List.of(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "value")))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(ctx -> setSpeed(ctx, EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "value")))))
		);

		dispatcher.register(Commands.literal("invsee")
			.requires(Perms::gamemaster)
			.then(Commands.argument("target", EntityArgument.player())
				.executes(ctx -> {
					ServerPlayer source = ctx.getSource().getPlayerOrException();
					ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
					source.openMenu(new SimpleMenuProvider(
						(id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x5, id, inv, target.getInventory(), 5),
						target.getDisplayName()
					));
					return 1;
				}))
		);

		dispatcher.register(Commands.literal("sudo")
			.requires(Perms::gamemaster)
			.then(Commands.argument("target", EntityArgument.player())
				.then(Commands.argument("command", StringArgumentType.greedyString())
					.executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
						String command = StringArgumentType.getString(ctx, "command");
						ctx.getSource().getServer().getCommands().performPrefixedCommand(target.createCommandSourceStack(), command);
						return 1;
					})))
		);

		dispatcher.register(Commands.literal("extinguish")
			.requires(Perms::moderator)
			.executes(ctx -> extinguish(ctx, List.of(ctx.getSource().getPlayerOrException())))
			.then(Commands.argument("targets", EntityArgument.players())
				.executes(ctx -> extinguish(ctx, EntityArgument.getPlayers(ctx, "targets"))))
		);

		dispatcher.register(Commands.literal("gms")
			.requires(Perms::gamemaster)
			.executes(ctx -> setGameMode(ctx, GameType.SURVIVAL))
		);
		dispatcher.register(Commands.literal("gmc")
			.requires(Perms::gamemaster)
			.executes(ctx -> setGameMode(ctx, GameType.CREATIVE))
		);
		dispatcher.register(Commands.literal("gma")
			.requires(Perms::gamemaster)
			.executes(ctx -> setGameMode(ctx, GameType.ADVENTURE))
		);
		dispatcher.register(Commands.literal("gmsp")
			.requires(Perms::gamemaster)
			.executes(ctx -> setGameMode(ctx, GameType.SPECTATOR))
		);

		dispatcher.register(Commands.literal("near")
			.requires(Perms::utility)
			.executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				ServerLevel level = (ServerLevel) player.level();
				List<ServerPlayer> near = ctx.getSource().getServer().getPlayerList().getPlayers().stream()
					.filter(p -> p != player && p.level() == level && p.distanceToSqr(player) < 100 * 100)
					.toList();
				if (near.isEmpty()) {
					ctx.getSource().sendSuccess(() -> Msg.ok("No players nearby."), false);
				} else {
					ctx.getSource().sendSuccess(() -> Msg.ok("Nearby players: " + near.stream().map(ServerPlayer::getScoreboardName).collect(Collectors.joining(", "))), false);
				}
				return 1;
			})
		);

		dispatcher.register(Commands.literal("whois")
			.requires(Perms::moderator)
			.then(Commands.argument("target", EntityArgument.player())
				.executes(ctx -> {
					ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
					ctx.getSource().sendSuccess(() -> Component.literal("--- Player Info: " + target.getScoreboardName() + " ---")
						.append("\nUUID: " + target.getUUID())
						.append("\nIP: " + target.getIpAddress())
						.append("\nGamemode: " + target.gameMode.getGameModeForPlayer().getName())
						.append("\nHealth: " + (int)target.getHealth() + "/" + (int)target.getMaxHealth())
						.append("\nLocation: " + target.level().dimension().identifier().toString() + " " + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ()), false);
					return 1;
				}))
		);

		dispatcher.register(Commands.literal("list")
			.requires(Perms::utility)
			.executes(ctx -> {
				Collection<ServerPlayer> players = ctx.getSource().getServer().getPlayerList().getPlayers();
				ctx.getSource().sendSuccess(() -> Msg.ok("Online players (" + players.size() + "): " +
					players.stream().map(ServerPlayer::getScoreboardName).collect(Collectors.joining(", "))), false);
				return 1;
			})
		);
	}

	private static int toggleFly(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			boolean flying = !player.getAbilities().mayfly;
			player.getAbilities().mayfly = flying;
			if (!flying) player.getAbilities().flying = false;
			player.onUpdateAbilities();
			player.sendSystemMessage(Msg.ok("Flight " + (flying ? "enabled" : "disabled") + "."));
		}
		ctx.getSource().sendSuccess(() -> Msg.ok("Toggled flight for " + Msg.describe(targets) + "."), true);
		return targets.size();
	}

	private static int setSpeed(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, float speed) {
		for (ServerPlayer player : targets) {
			float mcSpeed = speed / 10f;
			if (player.getAbilities().flying) {
				player.getAbilities().setFlyingSpeed(mcSpeed);
			} else {
				player.getAbilities().setWalkingSpeed(mcSpeed);
			}
			player.onUpdateAbilities();
			player.sendSystemMessage(Msg.ok("Speed set to " + speed + "."));
		}
		ctx.getSource().sendSuccess(() -> Msg.ok("Set speed for " + Msg.describe(targets) + "."), true);
		return targets.size();
	}

	private static int extinguish(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			player.clearFire();
			player.sendSystemMessage(Msg.ok("You have been extinguished."));
		}
		ctx.getSource().sendSuccess(() -> Msg.ok("Extinguished " + Msg.describe(targets) + "."), true);
		return targets.size();
	}

	private static int setGameMode(CommandContext<CommandSourceStack> ctx, GameType type) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		player.setGameMode(type);
		ctx.getSource().sendSuccess(() -> Msg.ok("Gamemode set to " + type.getName() + "."), true);
		return 1;
	}
}
