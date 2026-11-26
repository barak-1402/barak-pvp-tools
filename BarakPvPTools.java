package dev.barak.pvptools;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BarakPvPTools implements ModInitializer {
	public static final String MOD_ID = "barakpvptools";
	public static final String DISCORD_URL = "https://discord.gg/cKGD334gED";

	// This logger is used to write text to the console and the log file.
	public static final Logger LOGGER = LoggerFactory.getLogger("Barak PvP Tools");

	@Override
	public void onInitialize() {
		LOGGER.info("Barak PvP Tools geladen.");
	}
}
