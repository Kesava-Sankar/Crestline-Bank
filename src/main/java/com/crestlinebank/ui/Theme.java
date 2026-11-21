package com.crestlinebank.ui;

import java.awt.*;

public final class Theme {
    public static final Color NAVY = new Color(17, 31, 54);
    public static final Color NAVY_LIGHT = new Color(28, 47, 76);
    public static final Color GOLD = new Color(205, 166, 91);
    public static final Color GOLD_DARK = new Color(151, 119, 65);
    public static final Color BACKGROUND = new Color(245, 247, 250);
    public static final Color CREAM = new Color(249, 246, 240);
    public static final Color WHITE = Color.WHITE;
    public static final Color BORDER = new Color(218, 223, 230);
    public static final Color TEXT = new Color(35, 45, 60);
    public static final Color MUTED = new Color(112, 123, 139);
    public static final Color GREEN = new Color(27, 139, 94);
    public static final Color RED = new Color(190, 63, 63);

    private Theme() {}

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }
}
