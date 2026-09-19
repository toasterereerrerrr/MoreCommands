package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/** /heal, /feed, /god, /repair, /hat */
public final class PlayerCommands {
	private PlayerCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("heal")
			.requires(Perms::gamemaster)
			.executes(ctx -> heal(ctx, List.of(ctx.getSource().getPlayerOrException())))
			.then(Commands.argument("targets", EntityArgument.players())
				.executes(ctx -> heal(ctx, EntityArgument.getPlayers(ctx, "targets")))));

		dispatcher.register(Commands.literal("feed")
			.requires(Perms::gamemaster)
			.executes(ctx -> feed(ctx, List.of(ctx.getSource().getPlayerOrException())))
			.then(Commands.argument("targets", EntityArgument.players())
				.executes(ctx -> feed(ctx, EntityArgument.getPlayers(ctx, "targets")))));

		dispatcher.register(Commands.literal("god")
			.requires(Perms::gamemaster)
			.executes(ctx -> god(ctx, List.of(ctx.getSource().getPlayerOrException())))
			.then(Commands.argument("targets", EntityArgument.players())
				.executes(ctx -> god(ctx, EntityArgument.getPlayers(ctx, "targets")))));

		dispatcher.register(Commands.literal("repair")
			.requires(Perms::gamemaster)
			.executes(PlayerCommands::repairHand)
			.then(Commands.literal("all").executes(PlayerCommands::repairAll)));

		dispatcher.register(Commands.literal("hat")
			.requires(Perms::utility)
			.executes(PlayerCommands::hat));
	}

	private static int heal(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			player.setHealth(player.getMaxHealth());
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(20.0F);
			player.clearFire();
			player.setAirSupply(player.getMaxAirSupply());
		}

		String who = Msg.describe(targets);
		ctx.getSource().sendSuccess(() -> Msg.ok("Healed " + who + "."), true);
		return targets.size();
	}

	private static int feed(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(20.0F);
		}

		String who = Msg.describe(targets);
		ctx.getSource().sendSuccess(() -> Msg.ok("Fed " + who + "."), true);
		return targets.size();
	}

	private static int god(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
		boolean lastState = false;
		for (ServerPlayer player : targets) {
			lastState = !player.isInvulnerable();
			player.setInvulnerable(lastState);
		}

		String who = Msg.describe(targets);
		String message = targets.size() == 1
			? "God mode " + (lastState ? "enabled" : "disabled") + " for " + who + "."
			: "Toggled god mode for " + who + ".";
		ctx.getSource().sendSuccess(() -> Msg.ok(message), true);
		return targets.size();
	}

	private static int repairHand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ItemStack held = player.getMainHandItem();

		if (!held.isDamageableItem() || !held.isDamaged()) {
			throw Msg.fail("The item in your main hand has nothing to repair.");
		}

		held.setDamageValue(0);
		ctx.getSource().sendSuccess(() -> Msg.ok("Repaired the item in your main hand."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int repairAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		Inventory inventory = player.getInventory();

		int repaired = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
				stack.setDamageValue(0);
				repaired++;
			}
		}

		if (repaired == 0) {
			throw Msg.fail("Nothing in your inventory needs repairing.");
		}

		int count = repaired;
		ctx.getSource().sendSuccess(() -> Msg.ok("Repaired " + count + (count == 1 ? " item." : " items.")), false);
		return repaired;
	}

	private static int hat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ItemStack held = player.getMainHandItem().copy();

		if (held.isEmpty()) {
			throw Msg.fail("Hold an item in your main hand to wear it as a hat.");
		}

		ItemStack oldHat = player.getItemBySlot(EquipmentSlot.HEAD).copy();
		player.setItemSlot(EquipmentSlot.HEAD, held);
		player.setItemSlot(EquipmentSlot.MAINHAND, oldHat);

		ctx.getSource().sendSuccess(() -> Msg.ok("Enjoy your new hat."), false);
		return Command.SINGLE_SUCCESS;
	}
}
