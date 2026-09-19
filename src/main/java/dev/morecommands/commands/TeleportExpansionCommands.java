package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.morecommands.Config;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import dev.morecommands.TeleportRequest;
import dev.morecommands.Teleports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/** /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, /tpall, /rtp */
public final class TeleportExpansionCommands {
	private TeleportExpansionCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tpa")
			.then(Commands.argument("target", EntityArgument.player())
				.executes(ctx -> tpa(ctx, EntityArgument.getPlayer(ctx, "target"), TeleportRequest.Type.TPA))));

		dispatcher.register(Commands.literal("tpahere")
			.then(Commands.argument("target", EntityArgument.player())
				.executes(ctx -> tpa(ctx, EntityArgument.getPlayer(ctx, "target"), TeleportRequest.Type.TPAHERE))));

		dispatcher.register(Commands.literal("tpaccept")
			.executes(TeleportExpansionCommands::tpAccept));

		dispatcher.register(Commands.literal("tpdeny")
			.executes(TeleportExpansionCommands::tpDeny));

		dispatcher.register(Commands.literal("tpcancel")
			.executes(TeleportExpansionCommands::tpCancel));

		dispatcher.register(Commands.literal("tpall")
			.requires(Perms::gamemaster)
			.executes(TeleportExpansionCommands::tpAll));

		dispatcher.register(Commands.literal("rtp")
			.executes(TeleportExpansionCommands::rtp));
	}

	private static int tpa(CommandContext<CommandSourceStack> ctx, ServerPlayer target, TeleportRequest.Type type) throws CommandSyntaxException {
		ServerPlayer sender = ctx.getSource().getPlayerOrException();
		if (sender.equals(target)) {
			throw Msg.fail("You cannot teleport to yourself.");
		}

		TeleportRequest.addRequest(new TeleportRequest(sender.getUUID(), target.getUUID(), type));

		if (type == TeleportRequest.Type.TPA) {
			ctx.getSource().sendSuccess(() -> Msg.ok("Teleport request sent to " + target.getName().getString() + "."), false);
			target.sendSystemMessage(Msg.ok(sender.getName().getString() + " wants to teleport to you. Use /tpaccept or /tpdeny."));
		} else {
			ctx.getSource().sendSuccess(() -> Msg.ok("Teleport-here request sent to " + target.getName().getString() + "."), false);
			target.sendSystemMessage(Msg.ok(sender.getName().getString() + " wants you to teleport to them. Use /tpaccept or /tpdeny."));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int tpAccept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TeleportRequest req = TeleportRequest.getLatestIncoming(player.getUUID());

		if (req == null) {
			throw Msg.fail("You have no pending teleport requests.");
		}

		ServerPlayer other = ctx.getSource().getServer().getPlayerList().getPlayer(req.getSender());
		if (other == null) {
			TeleportRequest.removeRequest(req);
			throw Msg.fail("The player who sent the request is no longer online.");
		}

		if (req.getType() == TeleportRequest.Type.TPA) {
			Teleports.teleport(other, (ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
			player.sendSystemMessage(Msg.ok("Accepted teleport request from " + other.getName().getString() + "."));
			other.sendSystemMessage(Msg.ok(player.getName().getString() + " accepted your teleport request."));
		} else {
			Teleports.teleport(player, (ServerLevel) other.level(), other.getX(), other.getY(), other.getZ(), other.getYRot(), other.getXRot());
			player.sendSystemMessage(Msg.ok("Accepted teleport-here request from " + other.getName().getString() + "."));
			other.sendSystemMessage(Msg.ok(player.getName().getString() + " accepted your teleport-here request."));
		}

		TeleportRequest.removeRequest(req);
		return Command.SINGLE_SUCCESS;
	}

	private static int tpDeny(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TeleportRequest req = TeleportRequest.getLatestIncoming(player.getUUID());

		if (req == null) {
			throw Msg.fail("You have no pending teleport requests.");
		}

		TeleportRequest.removeRequest(req);
		ctx.getSource().sendSuccess(() -> Msg.ok("Teleport request denied."), false);

		ServerPlayer other = ctx.getSource().getServer().getPlayerList().getPlayer(req.getSender());
		if (other != null) {
			other.sendSystemMessage(Msg.ok(player.getName().getString() + " denied your teleport request."));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int tpCancel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TeleportRequest req = TeleportRequest.getLatestOutgoing(player.getUUID());

		if (req == null) {
			throw Msg.fail("You have no outgoing teleport requests.");
		}

		TeleportRequest.removeRequest(req);
		ctx.getSource().sendSuccess(() -> Msg.ok("Teleport request cancelled."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int tpAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) target.level();
		int count = 0;

		for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			if (player.equals(target)) continue;
			Teleports.teleport(player, level, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
			count++;
		}

		final int finalCount = count;
		ctx.getSource().sendSuccess(() -> Msg.ok("Teleported " + finalCount + " players to you."), false);
		return count;
	}

	private static int rtp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();
		int range = Config.get().rtpRange;

		double x = (Math.random() * range * 2) - range;
		double z = (Math.random() * range * 2) - range;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);

		Teleports.teleport(player, level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Msg.ok("Teleported to a random location."), false);
		return Command.SINGLE_SUCCESS;
	}
}
