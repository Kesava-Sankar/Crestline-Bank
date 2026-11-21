package com.crestlinebank.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public final class Ui {
    private Ui() {}

    /** Apply the Crestline palette to Swing's native dialogs and common controls. */
    public static void installTheme() {
        Font body = Theme.font(Font.PLAIN, 14);
        Font buttonFont = Theme.font(Font.BOLD, 13);
        UIManager.put("OptionPane.background", Theme.CREAM);
        UIManager.put("Panel.background", Theme.CREAM);
        UIManager.put("OptionPane.messageForeground", Theme.NAVY);
        UIManager.put("OptionPane.messageFont", Theme.font(Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", buttonFont);
        UIManager.put("Button.font", buttonFont);
        UIManager.put("Button.foreground", Theme.WHITE);
        UIManager.put("Button.background", Theme.NAVY);
        UIManager.put("Button.select", Theme.NAVY_LIGHT);
        UIManager.put("Button.focus", Theme.GOLD);
        UIManager.put("ButtonUI", "javax.swing.plaf.basic.BasicButtonUI");
        UIManager.put("OptionPane.border", BorderFactory.createCompoundBorder(
                new LineBorder(Theme.GOLD, 1, true), new EmptyBorder(12, 12, 12, 12)));
        UIManager.put("TextField.font", Theme.font(Font.PLAIN, 15));
        UIManager.put("PasswordField.font", Theme.font(Font.PLAIN, 15));
        UIManager.put("Label.font", body);
        UIManager.put("OptionPane.yesButtonText", "Continue");
        UIManager.put("OptionPane.noButtonText", "Cancel");
    }

    /**
     * Creates a readable, themed button.
     * Text automatically switches between navy and white based on the
     * supplied background colour, preventing white-on-white buttons.
     */
    public static JButton button(String text, Color background) {
        Color base = background == null ? Theme.WHITE : background;
        Color foreground = readableForeground(base);

        JButton button = new JButton(text);
        // Use a predictable Swing button delegate instead of platform-specific
        // rendering, which can replace the foreground with white on Windows.
        button.setUI(new BasicButtonUI());
        button.setFont(Theme.font(Font.BOLD, 13));
        button.setForeground(foreground);
        button.setBackground(base);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setRolloverEnabled(true);
        button.setBorder(new CompoundBorder(
                new LineBorder(borderColor(base), 1, true),
                new EmptyBorder(10, 17, 10, 17)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(text);

        // Explicitly update colours for hover and pressed states.
        ChangeListener stateListener = event -> {
            ButtonModel model = button.getModel();

            if (model.isPressed()) {
                button.setBackground(pressedColor(base));
            } else if (model.isRollover()) {
                button.setBackground(hoverColor(base));
            } else {
                button.setBackground(base);
            }

            // Keep the text readable in every state.
            button.setForeground(foreground);
        };
        button.getModel().addChangeListener(stateListener);

        return button;
    }

    private static Color readableForeground(Color background) {
        double luminance = (0.299 * background.getRed())
                + (0.587 * background.getGreen())
                + (0.114 * background.getBlue());
        return luminance >= 150 ? Theme.NAVY : Theme.WHITE;
    }

    private static Color borderColor(Color background) {
        return readableForeground(background).equals(Theme.WHITE)
                ? background.darker()
                : Theme.BORDER;
    }

    private static Color hoverColor(Color color) {
        if (readableForeground(color).equals(Theme.WHITE)) {
            return mix(color, Color.WHITE, 0.12);
        }
        return mix(color, Theme.NAVY, 0.07);
    }

    private static Color pressedColor(Color color) {
        if (readableForeground(color).equals(Theme.WHITE)) {
            return mix(color, Color.BLACK, 0.15);
        }
        return mix(color, Theme.NAVY, 0.14);
    }

    private static Color mix(Color first, Color second, double amount) {
        double inverse = 1.0 - amount;
        return new Color(
                (int) Math.round(first.getRed() * inverse + second.getRed() * amount),
                (int) Math.round(first.getGreen() * inverse + second.getGreen() * amount),
                (int) Math.round(first.getBlue() * inverse + second.getBlue() * amount)
        );
    }

    public static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(Theme.font(Font.PLAIN, 16));
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.NAVY);
        field.setBackground(Theme.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(9, 10, 9, 10)
        ));
        field.setPreferredSize(new Dimension(Math.max(220, field.getPreferredSize().width), 46));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return field;
    }


    public static JPasswordField passwordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(Theme.font(Font.PLAIN, 16));
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.NAVY);
        field.setBackground(Theme.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(9, 10, 9, 10)));
        field.setPreferredSize(new Dimension(Math.max(220, field.getPreferredSize().width), 46));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return field;
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 12));
        label.setForeground(Theme.MUTED);
        return label;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Theme.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 233, 239)),
                new EmptyBorder(18, 18, 18, 18)
        ));
        return panel;
    }
}
