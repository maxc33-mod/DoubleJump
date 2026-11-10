package fr.barton.doublejump.client;

import fr.barton.doublejump.client.zone.ClientSafeZones;
import fr.barton.doublejump.network.SafeZoneNetwork;
import fr.barton.doublejump.zone.SafeZone;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * L'écran ouvert en cliquant sur une Magnétite dans la carte :
 * propose de se téléporter à côté d'elle.
 */
public class TeleportScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;

    private final Screen parent; // la carte, pour y revenir avec "Annuler"
    private final BlockPos pos;  // position de la Magnétite choisie
    private SafeZone zone;       // dernière version connue de sa zone

    private Button teleportButton;

    public TeleportScreen(Screen parent, SafeZone zone) {
        super(Component.translatable("screen.doublejump.teleport"));
        this.parent = parent;
        this.pos = zone.pos();
        this.zone = zone;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_WIDTH / 2;
        int top = this.height / 2 - 40;

        teleportButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.doublejump.teleport.go"),
                                button -> teleport())
                        .bounds(left, top + 52, BUTTON_WIDTH, 20).build());
        this.addRenderableWidget(
                Button.builder(Component.translatable("screen.doublejump.waypoint.cancel"),
                                button -> onClose())
                        .bounds(left, top + 76, BUTTON_WIDTH, 20).build());

        updateButton();
    }

    // Envoie la demande au serveur, puis ferme tous les écrans pour voir l'arrivée
    private void teleport() {
        ClientPlayNetworking.send(new SafeZoneNetwork.TeleportPayload(pos));
        this.minecraft.gui.setScreen(null);
    }

    // Le jeu continue pendant que l'écran est ouvert (sinon, en solo, le serveur serait en pause)
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // "Annuler" ou Échap : retour à la carte
    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    // Vingt fois par seconde, on tient compte des changements (zone détruite, joueur qui bouge...)
    @Override
    public void tick() {
        SafeZone latest = ClientSafeZones.get(pos);
        if (latest == null) {
            onClose(); // la Magnétite a été détruite entre-temps
            return;
        }
        zone = latest;
        updateButton();
    }

    // Le bouton n'est cliquable que si le joueur se trouve dans une safe zone
    private void updateButton() {
        teleportButton.active = isPlayerInSafeZone();
    }

    private boolean isPlayerInSafeZone() {
        LocalPlayer player = this.minecraft.player;
        return player != null && ClientSafeZones.isProtected(player.blockPosition());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta); // fond + boutons

        int top = this.height / 2 - 40;
        drawCentered(graphics, this.title.getString(), top, 0xFFFFFFFF);
        drawCentered(graphics, Component.translatable("screen.doublejump.waypoint.coords",
                pos.getX(), pos.getY(), pos.getZ()).getString(), top + 14, 0xFFAAAAAA);
        drawCentered(graphics, Component.translatable("screen.doublejump.lodestone.level",
                zone.level(), SafeZone.MAX_LEVEL, zone.side()).getString(), top + 26, 0xFFAAAAAA);

        // Explique pourquoi le bouton est grisé
        if (!isPlayerInSafeZone()) {
            drawCentered(graphics, Component.translatable("screen.doublejump.teleport.not_in_zone").getString(),
                    top + 38, 0xFFFF8080);
        }
    }

    private void drawCentered(GuiGraphicsExtractor graphics, String text, int y, int color) {
        graphics.text(this.font, text, this.width / 2 - this.font.width(text) / 2, y, color, true);
    }
}
