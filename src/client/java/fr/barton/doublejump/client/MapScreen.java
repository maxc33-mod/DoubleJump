package fr.barton.doublejump.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.barton.doublejump.client.map.MapRenderer;
import fr.barton.doublejump.client.map.WorldMapData;
import fr.barton.doublejump.client.waypoint.Waypoint;
import fr.barton.doublejump.client.zone.ClientSafeZones;
import fr.barton.doublejump.zone.SafeZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapScreen extends Screen {

    private static final double MIN_SCALE = 0.25;
    private static final double MAX_SCALE = 8.0;

    private static final int SPAWN_COLOR = 0xFF30C030;        // vert
    private static final int OTHER_PLAYER_COLOR = 0xFF30A0FF; // bleu clair
    private static final int WAYPOINT_HIT_RADIUS = 7;         // zone cliquable d'un waypoint, en pixels
    private static final int ZONE_FILL_COLOR = 0x5540A0FF;    // bleu translucide
    private static final int ZONE_BORDER_COLOR = 0xFF80D0FF;  // bleu clair opaque
    private static final int ICON_HALF_SIZE = 8;              // moitié de la taille d'une icône d'objet

    private final MapRenderer mapRenderer = new MapRenderer();
    private final ItemStack lodestoneIcon = new ItemStack(Items.LODESTONE);

    // Vue : coordonnées du monde au centre de l'écran, et pixels par bloc
    private double centerX = 0;
    private double centerZ = 0;
    private double scale = 1.0;

    // Vrai si le dernier clic a été capté par un bouton (on ne déplace alors pas la carte)
    private boolean clickStartedOnWidget = false;

    public MapScreen() {
        super(Component.translatable("screen.doublejump.map"));
        recenter();

        // On charge les waypoints du monde et de la dimension actuels
        DoubleJumpClient.WAYPOINTS.useKey(DoubleJumpClient.SCANNER.currentKey());
    }

    // Replace le centre de la carte sur le joueur
    private void recenter() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            centerX = player.getX();
            centerZ = player.getZ();
        }
    }

    // Conversions : coordonnées du monde -> position à l'écran
    private int screenX(double worldX) {
        return (int) Math.round(this.width / 2.0 + (worldX - centerX) * scale);
    }

    private int screenY(double worldZ) {
        return (int) Math.round(this.height / 2.0 + (worldZ - centerZ) * scale);
    }

    // Le jeu continue pendant que la carte est ouverte
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        mapRenderer.init(this.minecraft, this.width, this.height);

        // Bouton de recentrage, en haut à droite
        this.addRenderableWidget(
                Button.builder(Component.translatable("screen.doublejump.map.recenter"),
                                button -> recenter())
                        .bounds(this.width - 108, 8, 100, 20)
                        .build());
    }

    // Molette : zoom, centré sur le curseur
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }

        double newScale = scrollY > 0 ? scale * 2 : scale / 2;
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        if (newScale != scale) {
            // Point du monde sous le curseur avant le zoom
            double worldX = centerX + (mouseX - this.width / 2.0) / scale;
            double worldZ = centerZ + (mouseY - this.height / 2.0) / scale;

            scale = newScale;

            // On recale le centre pour que ce point reste sous le curseur
            centerX = worldX - (mouseX - this.width / 2.0) / scale;
            centerZ = worldZ - (mouseY - this.height / 2.0) / scale;
        }
        return true;
    }

    // Clic maintenu + déplacement : la carte suit la souris
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (clickStartedOnWidget) {
            return super.mouseDragged(event, dragX, dragY);
        }
        centerX -= dragX / scale;
        centerZ -= dragY / scale;
        return true;
    }

    // Clic droit : créer ou modifier un waypoint
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // On laisse d'abord les boutons (Me recentrer) réagir
        clickStartedOnWidget = super.mouseClicked(event, doubleClick);
        if (clickStartedOnWidget) {
            return true;
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            openWaypointEditor(event.x(), event.y());
            return true;
        }
        return false;
    }

    private void openWaypointEditor(double mouseX, double mouseY) {
        Waypoint existing = findWaypointAt(mouseX, mouseY);
        if (existing != null) {
            this.minecraft.gui.setScreen(new WaypointEditScreen(this, existing, false));
            return;
        }

        // Nouveau waypoint à l'endroit cliqué
        int worldX = (int) Math.floor(centerX + (mouseX - this.width / 2.0) / scale);
        int worldZ = (int) Math.floor(centerZ + (mouseY - this.height / 2.0) / scale);

        // Hauteur : celle de la carte si on la connaît, sinon celle du joueur
        int y;
        int knownHeight = DoubleJumpClient.SCANNER.data().getHeight(worldX, worldZ);
        if (knownHeight != WorldMapData.UNKNOWN_HEIGHT) {
            y = knownHeight + 1;
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            y = (player != null) ? player.getBlockY() : 64;
        }

        Waypoint created = new Waypoint("", worldX, y, worldZ, Waypoint.PALETTE[0]);
        this.minecraft.gui.setScreen(new WaypointEditScreen(this, created, true));
    }

    // Waypoint le plus proche de la souris (dans la zone cliquable), ou null
    private Waypoint findWaypointAt(double mouseX, double mouseY) {
        Waypoint best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Waypoint waypoint : DoubleJumpClient.WAYPOINTS.all()) {
            double dx = mouseX - screenX(waypoint.x + 0.5);
            double dy = mouseY - screenY(waypoint.z + 0.5);
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= WAYPOINT_HIT_RADIUS && distance < bestDistance) {
                best = waypoint;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        WorldMapData data = DoubleJumpClient.SCANNER.data();
        mapRenderer.update(data, centerX, centerZ, scale);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // La carte, sur tout l'écran
        graphics.blit(RenderPipelines.GUI_TEXTURED, mapRenderer.textureId(),
                0, 0, 0f, 0f,
                this.width, this.height,
                this.width, this.height,
                this.width, this.height);

        // Du plus "en dessous" au plus "au dessus" : zones, waypoints, spawn, autres joueurs, moi
        drawSafeZones(graphics);
        drawWaypoints(graphics, mouseX, mouseY);
        drawSpawnMarker(graphics);
        drawOtherPlayers(graphics);
        drawPlayerMarker(graphics);
        drawInfo(graphics, data, mouseX, mouseY);
    }

    // Les safe zones de la dimension : un carré bleu translucide, avec la Magnétite au centre
    private void drawSafeZones(GuiGraphicsExtractor graphics) {
        // Les surfaces d'abord, pour qu'une zone voisine ne recouvre pas l'icône d'une autre
        for (SafeZone zone : ClientSafeZones.zones()) {
            int left = screenX(zone.pos().getX() - zone.radius());
            int right = screenX(zone.pos().getX() + zone.radius() + 1);
            int top = screenY(zone.pos().getZ() - zone.radius());
            int bottom = screenY(zone.pos().getZ() + zone.radius() + 1);

            // Hors de l'écran : inutile de dessiner
            if (right < 0 || left > this.width || bottom < 0 || top > this.height) {
                continue;
            }

            graphics.fill(left, top, right, bottom, ZONE_FILL_COLOR);
            graphics.outline(left, top, right - left, bottom - top, ZONE_BORDER_COLOR);
        }

        for (SafeZone zone : ClientSafeZones.zones()) {
            graphics.item(lodestoneIcon,
                    screenX(zone.pos().getX() + 0.5) - ICON_HALF_SIZE,
                    screenY(zone.pos().getZ() + 0.5) - ICON_HALF_SIZE);
        }
    }

    // Dessine un losange de la couleur donnée, avec un contour blanc
    private void drawDiamond(GuiGraphicsExtractor graphics, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(x - half, y + dy, x + half + 1, y + dy + 1, color);
        }
    }

    private void drawWaypoints(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Waypoint waypoint : DoubleJumpClient.WAYPOINTS.all()) {
            int x = screenX(waypoint.x + 0.5);
            int y = screenY(waypoint.z + 0.5);

            // Hors de l'écran : inutile de dessiner
            if (x < -80 || x > this.width + 80 || y < -20 || y > this.height + 20) {
                continue;
            }

            drawDiamond(graphics, x, y, 5, 0xFFFFFFFF);
            drawDiamond(graphics, x, y, 4, 0xFF000000 | waypoint.color);
            graphics.text(this.font, waypoint.name, x + 9, y - 4, 0xFFFFFFFF, true);

            // Au survol : on affiche aussi ses coordonnées
            if (Math.abs(mouseX - x) <= WAYPOINT_HIT_RADIUS && Math.abs(mouseY - y) <= WAYPOINT_HIT_RADIUS) {
                String coords = Component.translatable("screen.doublejump.waypoint.coords",
                        waypoint.x, waypoint.y, waypoint.z).getString();
                graphics.text(this.font, coords, x + 9, y + 6, 0xFFDDDDDD, true);
            }
        }
    }

    // Dessine un carré coloré (contour blanc) à une position du monde, avec un nom optionnel
    private void drawMarker(GuiGraphicsExtractor graphics, double worldX, double worldZ,
                            int color, String label) {
        int x = screenX(worldX);
        int y = screenY(worldZ);

        // Hors de l'écran : inutile de dessiner
        if (x < -60 || x > this.width + 60 || y < -20 || y > this.height + 20) {
            return;
        }

        graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFFFFFFFF);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, color);

        if (label != null) {
            graphics.text(this.font, label, x + 7, y - 4, 0xFFFFFFFF, true);
        }
    }

    // Point d'apparition du monde (affiché dans la dimension "Surface" uniquement)
    private void drawSpawnMarker(GuiGraphicsExtractor graphics) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !Level.OVERWORLD.equals(level.dimension())) {
            return;
        }

        BlockPos spawn = level.getRespawnData().pos();
        drawMarker(graphics, spawn.getX() + 0.5, spawn.getZ() + 0.5, SPAWN_COLOR,
                Component.translatable("screen.doublejump.map.spawn").getString());
    }

    // Les autres joueurs que ton jeu connaît (ceux qui sont proches)
    private void drawOtherPlayers(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        for (Player other : level.players()) {
            if (other == client.player || other.isInvisible()) {
                continue; // pas moi, et pas les joueurs invisibles
            }
            drawMarker(graphics, other.getX(), other.getZ(), OTHER_PLAYER_COLOR,
                    other.getName().getString());
        }
    }

    private void drawPlayerMarker(GuiGraphicsExtractor graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int x = screenX(player.getX());
        int y = screenY(player.getZ());

        // Direction du regard : en Minecraft, yaw 0 = sud, yaw 90 = ouest
        double yaw = Math.toRadians(player.getYRot());
        double dirX = -Math.sin(yaw);
        double dirZ = Math.cos(yaw);
        for (int i = 4; i <= 12; i += 2) {
            int dx = (int) Math.round(x + dirX * i);
            int dy = (int) Math.round(y + dirZ * i);
            graphics.fill(dx, dy, dx + 2, dy + 2, 0xFFFFFF00);
        }

        // Le joueur : un carré rouge avec un contour blanc
        graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFFFFFFFF);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFE02020);
    }

    // Nom de la dimension dans laquelle on se trouve
    private Component dimensionName() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            if (Level.OVERWORLD.equals(client.level.dimension())) {
                return Component.translatable("screen.doublejump.map.dim.overworld");
            }
            if (Level.NETHER.equals(client.level.dimension())) {
                return Component.translatable("screen.doublejump.map.dim.nether");
            }
            if (Level.END.equals(client.level.dimension())) {
                return Component.translatable("screen.doublejump.map.dim.end");
            }
        }
        return Component.translatable("screen.doublejump.map.dim.other");
    }

    private void drawInfo(GuiGraphicsExtractor graphics, WorldMapData data, int mouseX, int mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Coordonnées du monde sous le curseur
        int cursorX = (int) Math.floor(centerX + (mouseX - this.width / 2.0) / scale);
        int cursorZ = (int) Math.floor(centerZ + (mouseY - this.height / 2.0) / scale);

        // Zoom affiché proprement : "4" ou "0.5" plutôt que "4.0"
        String zoomText = scale >= 1 ? String.valueOf((int) scale) : String.valueOf(scale);

        graphics.fill(8, 8, 290, 96, 0xA0000000); // fond semi-transparent

        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.dimension",
                        dimensionName().getString()).getString(),
                14, 12, 0xFFFFFFFF, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.player",
                        player.getBlockX(), player.getBlockZ()).getString(),
                14, 24, 0xFFFFFFFF, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.cursor",
                        cursorX, cursorZ).getString(),
                14, 36, 0xFFFFFFFF, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.chunks",
                        data.chunkCount()).getString(),
                14, 48, 0xFFFFFFFF, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.zoom", zoomText).getString(),
                14, 60, 0xFFFFFFFF, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.hint").getString(),
                14, 72, 0xFFAAAAAA, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.map.hint2").getString(),
                14, 84, 0xFFAAAAAA, true);
    }
}