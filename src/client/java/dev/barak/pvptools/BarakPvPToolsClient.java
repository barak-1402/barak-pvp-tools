package dev.barak.pvptools;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BarakPvPToolsClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        BarakConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BarakPvPTools.MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.barakpvptools.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                client.setScreen(new BarakScreen());
            }
            AuctionState.tick();
            GiveawayState.tick();
        });

        HudRenderCallback.EVENT.register(BarakHud::render);

        // Server-Nachrichten (z.B. "[HugoSMP] Du hast $10 von Name erhalten.")
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            Minecraft.getInstance().execute(() -> handleLine(text));
        });

        // normale Spieler-Chatnachrichten
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String text = params.name().getString() + ": " + message.getString();
            Minecraft.getInstance().execute(() -> handleLine(text));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (BarakConfig.showJoinMessage && client.player != null) {
                AuctionState.localMessage("[Barak PvP Tools] Join my Discord: " + BarakPvPTools.DISCORD_URL);
            }
        }));
    }

    private static void handleLine(String line) {
        AuctionState.onLine(line);
        GiveawayState.onLine(line);
    }
}
