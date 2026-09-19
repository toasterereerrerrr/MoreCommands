package dev.morecommands.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.morecommands.Msg;
import dev.morecommands.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Time and weather shortcuts, /butcher and /clearitems. */
public final class WorldCommands {
	private static final int DEFAULT_RADIUS = 64;
	private static final int MAX_RADIUS = 512;

	private WorldCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// These simply run the vanilla command as the person who typed them, so vanilla feedback and
		// permission checks still apply.
		alias(dispatcher, "noon", "time set noon");
		alias(dispatcher, "midnight", "time set midnight");

		dispatcher.register(Commands.literal("butcher")
			.requires(Perms::gamemaster)
			.executes(ctx -> butcher(ctx, DEFAULT_RADIUS))
			.then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
				.executes(ctx -> butcher(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));

		dispatcher.register(Commands.literal("clearitems")
			.requires(Perms::gamemaster)
			.executes(ctx -> clearItems(ctx, DEFAULT_RADIUS))
			.then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
				.executes(ctx -> clearItems(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));
	}

	private static void alias(CommandDispatcher<CommandSourceStack> dispatcher, String name, String vanillaCommand) {
		dispatcher.register(Commands.literal(name)
			.requires(Perms::gamemaster)
			.executes(ctx -> {
				CommandSourceStack source = ctx.getSource();
				source.getServer().getCommands().performPrefixedCommand(source, vanillaCommand);
				return Command.SINGLE_SUCCESS;
			}));
	}

	private static int butcher(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();
		AABB area = player.getBoundingBox().inflate(radius);

		// Monsters only. Named mobs are left alone so renamed pets and mascots are safe.
		List<Entity> targets = level.getEntities((Entity) null, area,
			entity -> entity.getType().getCategory() == MobCategory.MONSTER && !entity.hasCustomName());

		for (Entity entity : targets) {
			entity.kill(level);
		}

		int count = targets.size();
		ctx.getSource().sendSuccess(() -> Msg.ok("Killed " + count + (count == 1 ? " monster" : " monsters")
			+ " within " + radius + " blocks."), true);
		return count;
	}

	private static int clearItems(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();
		AABB area = player.getBoundingBox().inflate(radius);

		List<Entity> targets = level.getEntities((Entity) null, area, entity -> entity instanceof ItemEntity);

		for (Entity entity : targets) {
			entity.discard();
		}

		int count = targets.size();
		ctx.getSource().sendSuccess(() -> Msg.ok("Removed " + count + (count == 1 ? " item" : " items")
			+ " within " + radius + " blocks."), true);
		return count;
	}
}
