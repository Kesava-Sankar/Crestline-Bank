package com.crestlinebank.ui;

import com.crestlinebank.dao.AdminDao;
import com.crestlinebank.model.Admin;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class AdminAccessDialog extends JDialog {
    private final AdminDao dao = new AdminDao();
    private final JTextField name = Ui.field(22), email = Ui.field(22);
    private final JPasswordField password = Ui.passwordField(22);
    private final JLabel heading = new JLabel("Administrator sign in");
    private final JButton action = Ui.button("Sign in securely", Theme.NAVY);
    private boolean setup;

    public AdminAccessDialog(JFrame owner) {
        super(owner, "Crestline | Admin access", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        build(); checkSetup(); setLocationRelativeTo(owner);
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Theme.CREAM);
        JPanel banner = new JPanel(new BorderLayout()); banner.setBackground(Theme.NAVY);
        banner.setBorder(new EmptyBorder(15, 24, 15, 24));
        JLabel brand = new JLabel("CRESTLINE  /  SECURE ADMINISTRATION");
        brand.setForeground(Theme.GOLD); brand.setFont(Theme.font(Font.BOLD, 13)); banner.add(brand);
        JPanel form = new JPanel(); form.setBackground(Theme.CREAM);
        form.setBorder(new EmptyBorder(24, 30, 26, 30)); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        heading.setFont(Theme.font(Font.BOLD, 23)); heading.setForeground(Theme.NAVY); heading.setAlignmentX(Component.LEFT_ALIGNMENT); form.add(heading);
        JLabel note = new JLabel("Restricted access for bank administrators"); note.setForeground(Theme.MUTED); note.setFont(Theme.font(Font.PLAIN, 13)); note.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(7)); form.add(note);
        addField(form, "FULL NAME (FIRST ADMIN ONLY)", name);
        addField(form, "EMAIL ADDRESS", email);
        addField(form, "PASSWORD", password);
        form.add(Box.createVerticalStrut(20)); action.setFont(Theme.font(Font.BOLD, 14));
        action.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); action.setPreferredSize(new Dimension(380, 48)); action.setAlignmentX(Component.LEFT_ALIGNMENT);
        action.addActionListener(e -> submit()); form.add(action);
        root.add(banner, BorderLayout.NORTH); JScrollPane scroll = new JScrollPane(form); scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(18); root.add(scroll, BorderLayout.CENTER); setContentPane(root);
        getRootPane().setDefaultButton(action);
    }

    private void addField(JPanel panel, String label, JComponent field) {
        panel.add(Box.createVerticalStrut(14)); JLabel title = Ui.label(label); title.setAlignmentX(Component.LEFT_ALIGNMENT); panel.add(title);
        panel.add(Box.createVerticalStrut(6)); field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        field.setMinimumSize(new Dimension(100, 49)); field.setPreferredSize(new Dimension(380, 49));
        field.setAlignmentX(Component.LEFT_ALIGNMENT); panel.add(field);
    }

    private void checkSetup() {
        try { setup = !dao.hasAdmins(); name.setVisible(setup); heading.setText(setup ? "Create first administrator" : "Administrator sign in");
            action.setText(setup ? "Create administrator" : "Sign in securely"); setSize(510, Math.min(setup ? 465 : 395, Math.max(340, getOwner().getHeight() - 80)));
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Could not check admin setup: " + e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); dispose(); }
    }

    private void submit() {
        String em = email.getText().trim(), pw = new String(password.getPassword());
        if (!em.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$") || pw.length() < 8) {
            JOptionPane.showMessageDialog(this, "Enter a valid email and a password of at least 8 characters.", "Check details", JOptionPane.WARNING_MESSAGE); return;
        }
        try {
            if (setup) {
                if (name.getText().trim().length() < 2) throw new IllegalArgumentException("Enter the administrator's full name.");
                dao.createFirstAdmin(name.getText(), em, pw);
                JOptionPane.showMessageDialog(this, "First administrator created. Please sign in.", "Crestline Bank", JOptionPane.INFORMATION_MESSAGE);
                setup = false; name.setVisible(false); heading.setText("Administrator sign in"); action.setText("Sign in securely"); setSize(510, Math.min(395, Math.max(340, getOwner().getHeight() - 80))); revalidate(); return;
            }
            Optional<Admin> admin = dao.authenticate(em, pw);
            if (admin.isEmpty()) throw new IllegalArgumentException("Incorrect admin email or password.");
            new AdminFrame(admin.get()).setVisible(true); dispose(); getOwner().dispose();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "Admin access failed", JOptionPane.ERROR_MESSAGE); }
    }
}
