package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.morecommands.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;

/** Portable crafting table, anvil, ender chest and friends. */
public final class MenuCommands {
	private MenuCommands() {}

	@FunctionalInterface
	private interface MenuFactory {
		AbstractContainerMenu create(int containerId, Inventory inventory, ServerPlayer player);
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// ContainerLevelAccess.NULL means "not tied to a block", so the menus stay open anywhere.
		menu(dispatcher, "container.crafting",
			(id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.NULL), "craft", "workbench");
		menu(dispatcher, "container.repair",
			(id, inv, p) -> new AnvilMenu(id, inv, ContainerLevelAccess.NULL), "anvil");
		menu(dispatcher, "container.grindstone_title",
			(id, inv, p) -> new GrindstoneMenu(id, inv, ContainerLevelAccess.NULL), "grindstone");
		menu(dispatcher, "container.stonecutter",
			(id, inv, p) -> new StonecutterMenu(id, inv, ContainerLevelAccess.NULL), "stonecutter");
		menu(dispatcher, "container.loom",
			(id, inv, p) -> new LoomMenu(id, inv, ContainerLevelAccess.NULL), "loom");
		menu(dispatcher, "container.cartography_table",
			(id, inv, p) -> new CartographyTableMenu(id, inv, ContainerLevelAccess.NULL), "cartography");
		menu(dispatcher, "container.upgrade",
			(id, inv, p) -> new SmithingMenu(id, inv, ContainerLevelAccess.NULL), "smithing");
		menu(dispatcher, "container.enderchest",
			(id, inv, p) -> ChestMenu.threeRows(id, inv, p.getEnderChestInventory()), "enderchest", "ec");
	}

	private static void menu(CommandDispatcher<CommandSourceStack> dispatcher, String titleKey, MenuFactory factory, String... names) {
		for (String name : names) {
			dispatcher.register(Commands.literal(name)
				.requires(Perms::utility)
				.executes(ctx -> open(ctx, titleKey, factory)));
		}
	}

	private static int open(CommandContext<CommandSourceStack> ctx, String titleKey, MenuFactory factory) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		player.openMenu(new SimpleMenuProvider(
			(id, inventory, ignored) -> factory.create(id, inventory, player),
			Component.translatable(titleKey)));
		return Command.SINGLE_SUCCESS;
	}
}
