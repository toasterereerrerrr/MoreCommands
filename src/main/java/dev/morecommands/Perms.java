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

	/** Basic moderation tasks. Level 1 and up. */
	public static boolean moderator(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
	}

	/** Social commands (/msg, /me, /nick, etc). Open to everyone by default. */
	public static boolean social(CommandSourceStack source) {
		return true;
	}
}
