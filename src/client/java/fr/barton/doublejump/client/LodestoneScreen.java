package fr.barton.doublejump.client;

import fr.barton.doublejump.client.zone.ClientSafeZones;
import fr.barton.doublejump.network.SafeZoneNetwork;
import fr.barton.doublejump.zone.SafeZone;
import fr.barton.doublejump.zone.SafeZoneActions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * L'écran ouvert par un clic droit sur une Magnétite :
 * associer la boussole tenue en main, ou agrandir la zone avec du quartz.
 */
public class LodestoneScreen extends Screen {

    private static final int BUTTON_WIDTH = 260;

    private final BlockPos pos;
    private SafeZone zone; // dernière version connue de la zone

    private Button compassButton;
    private Button upgradeButton;

    public LodestoneScreen(BlockPos pos) {
        super(Component.translatable("screen.doublejump.lodestone"));
        this.pos = pos;
        this.zone = ClientSafeZones.get(pos);
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_WIDTH / 2;
        int top = this.height / 2 - 40;

        compassButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.doublejump.lodestone.compass"),
                                button -> linkCompass())
                        .bounds(left, top + 40, BUTTON_WIDTH, 20).build());
        upgradeButton = this.addRenderableWidget(
                Button.builder(Component.empty(), button -> upgrade())
                        .bounds(left, top + 64, BUTTON_WIDTH, 20).build());

        updateButtons();
    }

    private void linkCompass() {
        ClientPlayNetworking.send(new SafeZoneNetwork.LinkCompassPayload(pos));
        onClose();
    }

    private void upgrade() {
        // L'écran reste ouvert : il se mettra à jour dès la réponse du serveur.
        ClientPlayNetworking.send(new SafeZoneNetwork.UpgradeZonePayload(pos, zone.level()));
    }

    // Le jeu continue pendant que l'écran est ouvert. En solo, sans cela,
    // le serveur serait en pause et ne traiterait pas les demandes.
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Vingt fois par seconde, on tient compte des changements (quartz, niveau...)
    @Override
    public void tick() {
        SafeZone latest = ClientSafeZones.get(pos);
        if (latest == null) {
            onClose(); // la Magnétite a été détruite, par exemple par un autre joueur
            return;
        }
        zone = latest;
        updateButtons();
    }

    private void updateButtons() {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }

        compassButton.active = SafeZoneActions.findCompassHand(player) != null;

        if (zone.isMaxLevel()) {
            upgradeButton.setMessage(Component.translatable("screen.doublejump.lodestone.max"));
            upgradeButton.active = false;
        } else {
            SafeZone next = zone.nextLevel();
            upgradeButton.setMessage(Component.translatable("screen.doublejump.lodestone.upgrade",
                    next.level(), next.side(), zone.upgradeCost()));
            upgradeButton.active = SafeZoneActions.canPay(player, zone.upgradeCost());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta); // fond + boutons

        int top = this.height / 2 - 40;
        drawCentered(graphics, this.title.getString(), top, 0xFFFFFFFF);
        drawCentered(graphics, Component.translatable("screen.doublejump.lodestone.level",
                zone.level(), SafeZone.MAX_LEVEL, zone.side()).getString(), top + 14, 0xFFAAAAAA);

        if (this.minecraft.player != null) {
            int quartz = this.minecraft.player.getInventory().countItem(Items.QUARTZ);
            drawCentered(graphics, Component.translatable("screen.doublejump.lodestone.quartz", quartz).getString(),
                    top + 26, 0xFFAAAAAA);
        }
    }

    private void drawCentered(GuiGraphicsExtractor graphics, String text, int y, int color) {
        graphics.text(this.font, text, this.width / 2 - this.font.width(text) / 2, y, color, true);
    }
}
