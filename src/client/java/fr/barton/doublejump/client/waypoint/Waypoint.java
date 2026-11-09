package fr.barton.doublejump.client.waypoint;

public class Waypoint {

    // Couleurs proposées (format 0xRRGGBB)
    public static final int[] PALETTE = {
            0xE02020, // rouge
            0xF08020, // orange
            0xF0D020, // jaune
            0x30C030, // vert
            0x20C0C0, // cyan
            0x3060F0, // bleu
            0x9040D0, // violet
            0xF060B0, // rose
            0xF0F0F0, // blanc
            0x808080  // gris
    };

    public String name = "";
    public int x;
    public int y;
    public int z;
    public int color = PALETTE[0];

    public Waypoint() {
    }

    public Waypoint(String name, int x, int y, int z, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }
}