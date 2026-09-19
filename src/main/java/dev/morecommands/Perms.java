package dev.morecommands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

public final class Perms {
	private Perms() {}

	/** Operator level 2 and up, plus the console and command blocks. */
	public static boolean gamemaster(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Portable menus and similar conveniences. Open to everyone unless the config says otherwise. */
	public static boolean utility(CommandSourceStack source) {
		return !Config.get().utilityCommandsRequireOp || gamemaster(source);
	}
}
