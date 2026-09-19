package dev.morecommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SocialCommands {
	private static final WeakHashMap<ServerPlayer, ServerPlayer> REPLIES = new WeakHashMap<>();
	private static final Set<UUID> SOCIAL_SPY = new HashSet<>();

	private SocialCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("msg")
			.then(Commands.argument("target", EntityArgument.player())
				.then(Commands.argument("message", StringArgumentType.greedyString())
					.executes(ctx -> message(ctx, EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "message")))))
		);
		dispatcher.register(Commands.literal("w")
			.then(Commands.argument("target", EntityArgument.player())
				.then(Commands.argument("message", StringArgumentType.greedyString())
					.executes(ctx -> message(ctx, EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "message")))))
		);
		dispatcher.register(Commands.literal("tell")
			.then(Commands.argument("target", EntityArgument.player())
				.then(Commands.argument("message", StringArgumentType.greedyString())
					.executes(ctx -> message(ctx, EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "message")))))
		);

		dispatcher.register(Commands.literal("r")
			.then(Commands.argument("message", StringArgumentType.greedyString())
				.executes(ctx -> {
					ServerPlayer source = ctx.getSource().getPlayerOrException();
					ServerPlayer target = REPLIES.get(source);
					if (target == null || target.isRemoved()) {
						throw Msg.fail("You have no one to reply to.");
					}
					return message(ctx, target, StringArgumentType.getString(ctx, "message"));
				}))
		);

		dispatcher.register(Commands.literal("socialspy")
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				if (SOCIAL_SPY.contains(player.getUUID())) {
					SOCIAL_SPY.remove(player.getUUID());
					ctx.getSource().sendSuccess(() -> Msg.ok("Social Spy disabled."), false);
				} else {
					SOCIAL_SPY.add(player.getUUID());
					ctx.getSource().sendSuccess(() -> Msg.ok("Social Spy enabled."), false);
				}
				return 1;
			})
		);

		dispatcher.register(Commands.literal("broadcast")
			.requires(Perms::gamemaster)
			.then(Commands.argument("message", StringArgumentType.greedyString())
				.executes(ctx -> {
					String msg = StringArgumentType.getString(ctx, "message");
					Component broadcast = Component.literal("[Broadcast] ").withStyle(s -> s.withColor(0xFF5555))
						.append(Component.literal(msg).withStyle(s -> s.withColor(0xFFFFFF)));
					ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(broadcast, false);
					return 1;
				}))
		);
	}

	private static int message(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String message) {
		ServerPlayer source = null;
		try {
			source = ctx.getSource().getPlayerOrException();
		} catch (Exception ignored) {}

		String sourceName = source != null ? source.getScoreboardName() : "Server";
		String targetName = target.getScoreboardName();

		Component toSource = Component.literal("To " + targetName + ": " + message).withStyle(s -> s.withItalic(true).withColor(0xAAAAAA));
		Component toTarget = Component.literal("From " + sourceName + ": " + message).withStyle(s -> s.withItalic(true).withColor(0xAAAAAA));

		ctx.getSource().sendSuccess(() -> toSource, false);
		target.sendSystemMessage(toTarget);

		if (source != null) {
			REPLIES.put(source, target);
			REPLIES.put(target, source);
		}

		// Social Spy
		Component spyMsg = Component.literal("[Spy] " + sourceName + " -> " + targetName + ": " + message).withStyle(s -> s.withColor(0x55FFFF));
		for (ServerPlayer spy : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			if (SOCIAL_SPY.contains(spy.getUUID()) && spy != source && spy != target) {
				spy.sendSystemMessage(spyMsg);
			}
		}

		return 1;
	}
}
