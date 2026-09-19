package dev.morecommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.morecommands.Config;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MiscCommands {
	private static final Map<UUID, String> NICKNAMES = new HashMap<>();

	private MiscCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nick")
			.requires(source -> Config.get().allowNicknames || Perms.moderator(source))
			.then(Commands.argument("name", StringArgumentType.word())
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					String nick = StringArgumentType.getString(ctx, "name");
					NICKNAMES.put(player.getUUID(), nick);
					player.setCustomName(Component.literal(nick));
					player.setCustomNameVisible(true);
					ctx.getSource().sendSuccess(() -> Msg.ok("Your nickname is now " + nick + "."), false);
					return 1;
				}))
			.then(Commands.literal("off").executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				NICKNAMES.remove(player.getUUID());
				player.setCustomName(null);
				player.setCustomNameVisible(false);
				ctx.getSource().sendSuccess(() -> Msg.ok("Nickname removed."), false);
				return 1;
			}))
		);

		dispatcher.register(Commands.literal("realname")
			.requires(Perms::moderator)
			.then(Commands.argument("nick", StringArgumentType.word())
				.executes(ctx -> {
					String nick = StringArgumentType.getString(ctx, "nick");
					for (Map.Entry<UUID, String> entry : NICKNAMES.entrySet()) {
						if (entry.getValue().equalsIgnoreCase(nick)) {
							ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayer(entry.getKey());
							String real = target != null ? target.getScoreboardName() : entry.getKey().toString();
							ctx.getSource().sendSuccess(() -> Msg.ok("Nickname '" + nick + "' belongs to " + real + "."), false);
							return 1;
						}
					}
					throw Msg.fail("No one online has that nickname.");
				}))
		);

		dispatcher.register(Commands.literal("suicide")
			.executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				player.kill((ServerLevel) player.level());
				ctx.getSource().sendSuccess(() -> Msg.ok("Goodbye, cruel world."), false);
				return 1;
			})
		);

		dispatcher.register(Commands.literal("burn")
			.requires(Perms::gamemaster)
			.then(Commands.argument("target", EntityArgument.player())
				.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
					.executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
						int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
						target.setRemainingFireTicks(seconds * 20);
						ctx.getSource().sendSuccess(() -> Msg.ok("Burned " + target.getScoreboardName() + " for " + seconds + " seconds."), true);
						return 1;
					})))
		);

		dispatcher.register(Commands.literal("lightning")
			.requires(Perms::gamemaster)
			.executes(ctx -> lightning(ctx, List.of(ctx.getSource().getPlayerOrException())))
			.then(Commands.argument("targets", EntityArgument.players())
				.executes(ctx -> lightning(ctx, EntityArgument.getPlayers(ctx, "targets"))))
		);

		dispatcher.register(Commands.literal("day")
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource(), "time set day");
				return 1;
			})
		);
		dispatcher.register(Commands.literal("night")
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource(), "time set night");
				return 1;
			})
		);

		dispatcher.register(Commands.literal("sun")
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource(), "weather clear");
				return 1;
			})
		);
		dispatcher.register(Commands.literal("rain")
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource(), "weather rain");
				return 1;
			})
		);
		dispatcher.register(Commands.literal("storm")
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource(), "weather thunder");
				return 1;
			})
		);
	}

	private static int lightning(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			String cmd = String.format("summon lightning_bolt %f %f %f", player.getX(), player.getY(), player.getZ());
			ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource(), cmd);
		}
		ctx.getSource().sendSuccess(() -> Msg.ok("Struck " + Msg.describe(targets) + " with lightning."), true);
		return targets.size();
	}
}
