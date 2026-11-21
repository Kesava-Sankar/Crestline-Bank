package com.crestlinebank.ui;

import com.crestlinebank.model.Customer;
import com.crestlinebank.service.BankingService;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Optional;

public class LoginFrame extends JFrame {
    private final BankingService service = new BankingService();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private boolean passwordVisible;

    private static final Color CREAM = new Color(249, 246, 240);
    private static final Color FIELD_BORDER = new Color(218, 213, 204);

    public LoginFrame() {
        setTitle("Crestline Bank | Private Banking");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CREAM);

        BrandPanel brand = new BrandPanel();
        brand.setPreferredSize(new Dimension(238, 0));
        ArchitecturePanel architecture = new ArchitecturePanel();
        architecture.setPreferredSize(new Dimension(360, 0));
        JPanel form = buildFormPanel();
        form.setPreferredSize(new Dimension(510, 0));

        root.add(brand, BorderLayout.WEST);
        root.add(architecture, BorderLayout.CENTER);
        root.add(form, BorderLayout.EAST);
        setContentPane(root);
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(CREAM);
        outer.setBorder(new EmptyBorder(35, 48, 30, 48));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setMaximumSize(new Dimension(410, 620));

        JLabel eyebrow = label("WELCOME TO CRESTLINE", 10, Font.BOLD, Theme.GOLD_DARK);
        JLabel heading = label("Sign in to Crestline", 30, Font.BOLD, Theme.NAVY);
        JLabel intro = label("Secure access to your private banking account.", 13, Font.PLAIN, Theme.MUTED);
        form.add(eyebrow);
        form.add(Box.createVerticalStrut(12));
        form.add(heading);
        form.add(Box.createVerticalStrut(9));
        form.add(intro);
        form.add(Box.createVerticalStrut(42));

        form.add(fieldLabel("EMAIL ADDRESS"));
        form.add(Box.createVerticalStrut(9));
        styleField(emailField);
        emailField.setToolTipText("Enter your registered email address");
        form.add(emailField);

        form.add(Box.createVerticalStrut(25));
        form.add(fieldLabel("PASSWORD"));
        form.add(Box.createVerticalStrut(9));

        JPanel passwordShell = new JPanel(new BorderLayout());
        passwordShell.setBackground(Color.WHITE);
        passwordShell.setBorder(new CompoundBorder(
                new LineBorder(FIELD_BORDER, 1, true), new EmptyBorder(0, 13, 0, 8)));
        passwordShell.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        passwordShell.setPreferredSize(new Dimension(400, 49));
        passwordField.setFont(Theme.font(Font.PLAIN, 16));
        passwordField.setForeground(Theme.TEXT);
        passwordField.setBackground(Color.WHITE);
        passwordField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        passwordField.setPreferredSize(new Dimension(100, 49));
        passwordField.setEchoChar('\u2022');

        JButton eye = new JButton("Show");
        eye.setFont(Theme.font(Font.BOLD, 11));
        eye.setForeground(Theme.NAVY);
        eye.setBackground(Color.WHITE);
        eye.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 5));
        eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.addActionListener(e -> {
            passwordVisible = !passwordVisible;
            passwordField.setEchoChar(passwordVisible ? (char) 0 : '\u2022');
            eye.setText(passwordVisible ? "Hide" : "Show");
        });
        passwordShell.add(passwordField, BorderLayout.CENTER);
        passwordShell.add(eye, BorderLayout.EAST);
        passwordShell.setMinimumSize(new Dimension(100, 49));
        passwordShell.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passwordShell);

        JPanel forgotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotRow.setOpaque(false);
        forgotRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        forgotRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JButton forgot = textButton("Forgot password?");
        forgot.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Password recovery is not configured in this demo. Please contact your bank administrator.",
                "Password assistance", JOptionPane.INFORMATION_MESSAGE));
        forgotRow.add(forgot);
        form.add(forgotRow);

        form.add(Box.createVerticalStrut(17));
        JButton signIn = primaryButton("Sign in");
        signIn.addActionListener(e -> login());
        form.add(signIn);
        form.add(Box.createVerticalStrut(25));
        form.add(createDivider());
        form.add(Box.createVerticalStrut(20));

        JButton passkey = outlineButton("Sign in with a passkey");
        passkey.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Passkey authentication is not enabled in this demo.",
                "Passkey sign-in", JOptionPane.INFORMATION_MESSAGE));
        form.add(passkey);

        form.add(Box.createVerticalStrut(12));
        JButton staffAccess = textButton("Staff sign in  ›");
        staffAccess.setForeground(Theme.NAVY);
        staffAccess.setAlignmentX(Component.LEFT_ALIGNMENT);
        staffAccess.addActionListener(e -> new StaffLoginDialog(this).setVisible(true));
        form.add(staffAccess);
        form.add(Box.createVerticalStrut(8));
        JButton childAccess = textButton("Child UPI sign in  ›");
        childAccess.setForeground(Theme.GREEN);
        childAccess.setAlignmentX(Component.LEFT_ALIGNMENT);
        childAccess.addActionListener(e -> new ChildLoginDialog(this).setVisible(true));
        form.add(childAccess);
        form.add(Box.createVerticalStrut(8));

        JButton adminAccess = textButton("Administrator access  ›");
        adminAccess.setForeground(Theme.GOLD_DARK);
        adminAccess.setAlignmentX(Component.LEFT_ALIGNMENT);
        adminAccess.addActionListener(e -> new AdminAccessDialog(this).setVisible(true));
        form.add(adminAccess);

        form.add(Box.createVerticalStrut(15));
        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        registerRow.setOpaque(false);
        registerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel newCustomer = label("New to Crestline?  ", 12, Font.PLAIN, Theme.MUTED);
        JButton register = textButton("Get started  ›");
        register.setForeground(Theme.GOLD_DARK);
        register.addActionListener(e -> new RegisterDialog(this, service).setVisible(true));
        registerRow.add(newCustomer);
        registerRow.add(register);
        form.add(registerRow);

        form.add(Box.createVerticalStrut(35));
        JLabel security = new JLabel("<html><b>◆ &nbsp; Secure demo access</b><br>"
                + "<span style='color:#777777'>Your account data is managed by the Crestline demo application.</span></html>");
        security.setFont(Theme.font(Font.PLAIN, 10));
        security.setForeground(Theme.MUTED);
        security.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(security);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.CENTER;
        outer.add(form, c);
        getRootPane().setDefaultButton(signIn);
        return outer;
    }

    private JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel fieldLabel(String text) {
        return label(text, 11, Font.BOLD, new Color(105, 111, 119));
    }

    private void styleField(JTextField field) {
        field.setFont(Theme.font(Font.PLAIN, 16));
        field.setForeground(Theme.TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(Theme.NAVY);
        field.setBorder(new CompoundBorder(new LineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(0, 13, 0, 13)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        field.setMinimumSize(new Dimension(100, 49));
        field.setPreferredSize(new Dimension(400, 49));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton primaryButton(String text) {
        JButton b = Ui.button(text, Theme.NAVY);
        b.setFont(Theme.font(Font.BOLD, 15));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        b.setPreferredSize(new Dimension(400, 52));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    private JButton outlineButton(String text) {
        JButton b = Ui.button(text, Color.WHITE);
        b.setFont(Theme.font(Font.BOLD, 14));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        b.setPreferredSize(new Dimension(400, 50));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    private JButton textButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFont(Theme.font(Font.BOLD, 11));
        b.setForeground(Theme.NAVY);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel createDivider() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        p.add(new JSeparator(), c);
        c.weightx = 0; c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 13, 0, 13);
        JLabel or = label("OR", 10, Font.PLAIN, Theme.MUTED);
        p.add(or, c);
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(new JSeparator(), c);
        return p;
    }

    private void login() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (email.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter your email address and password.",
                    "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Optional<Customer> customer = service.login(email, password);
            if (customer.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Incorrect email or password, or the account is inactive.",
                        "Sign in failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new DashboardFrame(customer.get()).setVisible(true);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to the Crestline database.\n\n" + ex.getMessage()
                            + "\n\nCheck that MySQL is running and follow SETUP.md.",
                    "Database connection error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class BrandPanel extends JPanel {
        BrandPanel() {
            setBackground(Theme.NAVY);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(42, 27, 28, 20));
            StarLogo star = new StarLogo();
            star.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(star);
            add(Box.createVerticalStrut(23));
            add(labelStatic("CRESTLINE", 23, Font.BOLD, Theme.GOLD));
            add(Box.createVerticalStrut(5));
            add(labelStatic("PRIVATE BANKING", 9, Font.BOLD, new Color(235, 228, 212)));
            add(Box.createVerticalStrut(24));
            JSeparator line = new JSeparator();
            line.setForeground(new Color(119, 108, 86));
            line.setMaximumSize(new Dimension(75, 1));
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(line);
            add(Box.createVerticalStrut(24));
            JLabel tagline = new JLabel("<html>Guiding your legacy<br>with confidence.</html>");
            tagline.setFont(new Font("Serif", Font.ITALIC, 17));
            tagline.setForeground(new Color(218, 184, 119));
            tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(tagline);
            add(Box.createVerticalGlue());
            add(labelStatic("<html>CRESTLINE BANK<br>PRIVATE BANKING  •  DEMO</html>",
                    9, Font.PLAIN, new Color(157, 169, 185)));
        }
        private static JLabel labelStatic(String s, int size, int style, Color color) {
            JLabel l = new JLabel(s);
            l.setFont(Theme.font(style, size));
            l.setForeground(color);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            return l;
        }
    }

    private static class StarLogo extends JComponent {
        StarLogo() {
            setPreferredSize(new Dimension(76, 76));
            setMaximumSize(new Dimension(76, 76));
            setMinimumSize(new Dimension(76, 76));
        }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 16; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 8;
                double r = (i % 2 == 0) ? 34 : 10;
                double x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.closePath();
            g.setColor(Theme.GOLD); g.fill(star);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(245, 224, 177)); g.draw(star);
            g.dispose();
        }
    }

    private static class ArchitecturePanel extends JPanel {
        private final BufferedImage hero;
        private float glow = 0.0f;
        private boolean forward = true;
        private final Timer animation;

        ArchitecturePanel() {
            setBackground(new Color(32, 42, 58));
            BufferedImage loaded = null;
            try (InputStream in = LoginFrame.class.getResourceAsStream("/crestline-bank-hero.jpg")) {
                if (in != null) loaded = ImageIO.read(in);
            } catch (Exception ignored) { }
            hero = loaded;
            animation = new Timer(70, e -> {
                glow += forward ? 0.006f : -0.006f;
                if (glow >= 0.18f) forward = false;
                if (glow <= 0.0f) forward = true;
                repaint();
            });
            animation.start();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (hero != null) {
                double scale = Math.max((double) w / hero.getWidth(), (double) h / hero.getHeight());
                int dw = (int) (hero.getWidth() * scale), dh = (int) (hero.getHeight() * scale);
                g.drawImage(hero, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                g.setPaint(new GradientPaint(0, 0, new Color(190,177,155), w, h, Theme.NAVY));
                g.fillRect(0, 0, w, h);
            }
            g.setColor(new Color(9, 20, 38, 70));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, 0, new Color(225, 184, 105, (int)(glow * 150)),
                    w, h, new Color(225, 184, 105, 0)));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

}
