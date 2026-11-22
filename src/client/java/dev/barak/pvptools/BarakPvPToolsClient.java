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
import net.minecraft.world.entity.item.ItemEntity;
import org.lwjgl.glfw.GLFW;

public class BarakPvPToolsClient implements ClientModInitializer {
    private static KeyMapping openKey;
    private static boolean glowWasOn = false;

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
            if (client.player != null && client.player.tickCount % 10 == 0) {
                updateGlow(client);
            }
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

    /** Lässt fallengelassene Items mit eingestellter Größe leuchten, damit man sie leichter findet. */
    private static void updateGlow(Minecraft mc) {
        boolean glow = BarakConfig.glowDropped;
        if (!glow && !glowWasOn) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            return;
        }
        for (ItemEntity entity : mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(64.0))) {
            boolean want = glow && BarakConfig.scaleFor(entity.getItem()) != 1.0f;
            if (entity.hasGlowingTag() != want) {
                entity.setGlowingTag(want);
            }
        }
        glowWasOn = glow;
    }
}
