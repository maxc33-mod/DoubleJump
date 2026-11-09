package fr.barton.doublejump.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.barton.doublejump.client.waypoint.Waypoint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class WaypointEditScreen extends Screen {

    private static final int SWATCH = 18; // taille d'une pastille de couleur
    private static final int GAP = 4;     // espace entre deux pastilles

    private final Screen parent;       // l'écran de la carte, pour y retourner
    private final Waypoint waypoint;
    private final boolean isNew;

    private EditBox nameBox;
    private int selectedColor = 0;
    private int paletteX;
    private int paletteY;

    public WaypointEditScreen(Screen parent, Waypoint waypoint, boolean isNew) {
        super(Component.translatable(isNew
                ? "screen.doublejump.waypoint.new"
                : "screen.doublejump.waypoint.edit"));
        this.parent = parent;
        this.waypoint = waypoint;
        this.isNew = isNew;

        // On présélectionne la couleur actuelle du waypoint
        for (int i = 0; i < Waypoint.PALETTE.length; i++) {
            if (Waypoint.PALETTE[i] == waypoint.color) {
                selectedColor = i;
            }
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 70;

        // Champ de texte pour le nom
        nameBox = new EditBox(this.font, centerX - 100, top + 28, 200, 20,
                Component.translatable("screen.doublejump.waypoint.name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(waypoint.name);
        this.addRenderableWidget(nameBox);
        this.setInitialFocus(nameBox);

        // Position de la rangée de pastilles de couleur
        int paletteWidth = Waypoint.PALETTE.length * (SWATCH + GAP) - GAP;
        paletteX = centerX - paletteWidth / 2;
        paletteY = top + 68;

        // Boutons
        int buttonsY = top + 108;
        this.addRenderableWidget(
                Button.builder(Component.translatable("screen.doublejump.waypoint.save"),
                                button -> save())
                        .bounds(centerX - 100, buttonsY, 96, 20).build());
        this.addRenderableWidget(
                Button.builder(Component.translatable("screen.doublejump.waypoint.cancel"),
                                button -> onClose())
                        .bounds(centerX + 4, buttonsY, 96, 20).build());

        if (!isNew) {
            this.addRenderableWidget(
                    Button.builder(Component.translatable("screen.doublejump.waypoint.delete"),
                                    button -> delete())
                            .bounds(centerX - 100, buttonsY + 24, 200, 20).build());
        }
    }

    private void save() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            name = Component.translatable("screen.doublejump.waypoint.default_name").getString();
        }
        waypoint.name = name;
        waypoint.color = Waypoint.PALETTE[selectedColor];

        if (isNew) {
            DoubleJumpClient.WAYPOINTS.add(waypoint);
        } else {
            DoubleJumpClient.WAYPOINTS.save();
        }
        onClose();
    }

    private void delete() {
        DoubleJumpClient.WAYPOINTS.remove(waypoint);
        onClose();
    }

    // Échap, Annuler, Enregistrer et Supprimer : on retourne à la carte
    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    // Clic sur une pastille de couleur
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < Waypoint.PALETTE.length; i++) {
                int x = paletteX + i * (SWATCH + GAP);
                if (event.x() >= x && event.x() < x + SWATCH
                        && event.y() >= paletteY && event.y() < paletteY + SWATCH) {
                    selectedColor = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta); // fond + champ + boutons

        int centerX = this.width / 2;
        int top = this.height / 2 - 70;

        // Titre centré
        String title = this.title.getString();
        graphics.text(this.font, title, centerX - this.font.width(title) / 2, top, 0xFFFFFFFF, true);

        // Libellés
        graphics.text(this.font,
                Component.translatable("screen.doublejump.waypoint.name").getString(),
                centerX - 100, top + 16, 0xFFAAAAAA, true);
        graphics.text(this.font,
                Component.translatable("screen.doublejump.waypoint.color").getString(),
                centerX - 100, top + 56, 0xFFAAAAAA, true);

        // Pastilles de couleur
        for (int i = 0; i < Waypoint.PALETTE.length; i++) {
            int x = paletteX + i * (SWATCH + GAP);
            int border = (i == selectedColor) ? 0xFFFFFFFF : 0xFF000000;
            int borderSize = (i == selectedColor) ? 2 : 1;
            graphics.fill(x - borderSize, paletteY - borderSize,
                    x + SWATCH + borderSize, paletteY + SWATCH + borderSize, border);
            graphics.fill(x, paletteY, x + SWATCH, paletteY + SWATCH,
                    0xFF000000 | Waypoint.PALETTE[i]);
        }

        // Coordonnées du waypoint
        String coords = Component.translatable("screen.doublejump.waypoint.coords",
                waypoint.x, waypoint.y, waypoint.z).getString();
        graphics.text(this.font, coords, centerX - this.font.width(coords) / 2,
                top + 92, 0xFFAAAAAA, true);
    }
}