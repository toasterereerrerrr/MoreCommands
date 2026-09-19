package dev.morecommands;

import dev.morecommands.commands.HomeCommands;
import dev.morecommands.commands.InfoCommands;
import dev.morecommands.commands.MenuCommands;
import dev.morecommands.commands.MiscCommands;
import dev.morecommands.commands.PlayerCommands;
import dev.morecommands.commands.SocialCommands;
import dev.morecommands.commands.TeleportExpansionCommands;
import dev.morecommands.commands.UtilityCommands;
import dev.morecommands.commands.WarpCommands;
import dev.morecommands.commands.WorldCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreCommands implements ModInitializer {
	public static final String MOD_ID = "morecommands";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Config.load();

		// Homes and warps are stored per world, so load them when a server (or integrated world) starts.
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			HomeStore.load(server);
			WarpStore.load(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			HomeStore.save();
			WarpStore.save();
		});

		// Remember where a player died so /back can take them there.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				Teleports.rememberBack(player);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			PlayerCommands.register(dispatcher);
			MenuCommands.register(dispatcher);
			HomeCommands.register(dispatcher);
			InfoCommands.register(dispatcher);
			WorldCommands.register(dispatcher);

			WarpCommands.register(dispatcher);
			TeleportExpansionCommands.register(dispatcher);
			UtilityCommands.register(dispatcher);
			SocialCommands.register(dispatcher);
			MiscCommands.register(dispatcher);
		});

		LOGGER.info("More Commands loaded.");
	}
}
