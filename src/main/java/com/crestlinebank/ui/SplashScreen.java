package com.crestlinebank.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Animated Crestline startup screen inspired by the supplied banking-app video.
 * The animation is rendered with Java2D and needs no external assets.
 */
public final class SplashScreen extends JFrame {
    private final Runnable onComplete;
    private final SplashPanel panel = new SplashPanel();
    private final Timer timer;
    private double progress = 0;
    private boolean completed = false;

    public SplashScreen(Runnable onComplete) {
        this.onComplete = onComplete;
        setUndecorated(true);
        setSize(760, 440);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(panel);

        // About five seconds of intro animation, then open the login screen.
        timer = new Timer(33, e -> {
            progress = Math.min(100.0, progress + 0.66);
            panel.repaint();
            if (progress >= 100 && !completed) {
                completed = true;
                ((Timer) e.getSource()).stop();
                Timer finishTimer = new Timer(350, event -> {
                    ((Timer) event.getSource()).stop();
                    dispose();
                    onComplete.run();
                });
                finishTimer.setRepeats(false);
                finishTimer.start();
            }
        });
    }

    public void showSplash() {
        setVisible(true);
        timer.start();
    }

    private final class SplashPanel extends JPanel {
        private final long startedAt = System.currentTimeMillis();

        SplashPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            double elapsed = (System.currentTimeMillis() - startedAt) / 1000.0;

            // Rich midnight-navy backdrop, retaining Crestline's gold identity.
            g.setPaint(new GradientPaint(0, 0, new Color(22, 18, 55),
                    w, h, new Color(9, 22, 43)));
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 30, 30));
            g.setColor(new Color(120, 91, 160, 22));
            g.fillOval(w / 2 - 245, 18, 490, 490);
            g.setColor(new Color(205, 166, 91, 24));
            g.fillOval(w / 2 - 160, 52, 320, 320);

            g.setColor(new Color(205, 166, 91, 100));
            g.setStroke(new BasicStroke(1.1f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 28, 28);

            int cx = w / 2;
            int logoY = 112;
            double pulse = 1.0 + 0.035 * Math.sin(elapsed * 3.0);
            int halo = (int) (112 * pulse);
            g.setColor(new Color(205, 166, 91, 30));
            g.fillOval(cx - halo / 2, logoY - halo / 2, halo, halo);
            drawCompassStar(g, cx, logoY, (int) (43 * pulse));

            // Brand reveal.
            float reveal = (float) Math.min(1.0, progress / 24.0);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, reveal));
            g.setFont(Theme.font(Font.BOLD, 31));
            g.setColor(Theme.GOLD);
            drawCentered(g, "CRESTLINE", cx, 202);
            g.setFont(Theme.font(Font.BOLD, 11));
            g.setColor(new Color(246, 241, 231));
            drawCentered(g, "PRIVATE BANKING", cx, 226);
            g.setComposite(AlphaComposite.SrcOver);

            // Rotating gold loading arc, similar to the reference's "Please Wait" spinner.
            int spinnerX = cx - 13;
            int spinnerY = 275;
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 35));
            g.drawOval(spinnerX, spinnerY, 26, 26);
            g.setColor(new Color(218, 179, 105));
            int angle = (int) ((elapsed * 260) % 360);
            g.drawArc(spinnerX, spinnerY, 26, 26, angle, 245);

            g.setFont(Theme.font(Font.BOLD, 12));
            g.setColor(new Color(246, 241, 231));
            drawCentered(g, "Please wait...", cx, 329);

            g.setFont(Theme.font(Font.PLAIN, 13));
            g.setColor(new Color(206, 211, 224));
            String setup = "Setting up your private banking experience";
            drawCentered(g, setup + ".".repeat((int) (elapsed * 1.5) % 4), cx, 382);

            g.setFont(Theme.font(Font.BOLD, 9));
            g.setColor(new Color(205, 166, 91, 185));
            drawCentered(g, "A MORE PERSONAL WAY TO BANK", cx, 410);
            g.dispose();
        }

        private void drawCompassStar(Graphics2D g, int cx, int cy, int radius) {
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 16; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 8;
                double r = (i % 2 == 0) ? radius : radius * 0.30;
                double x = cx + Math.cos(angle) * r;
                double y = cy + Math.sin(angle) * r;
                if (i == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.closePath();
            g.setColor(Theme.GOLD);
            g.fill(star);
            g.setColor(new Color(255, 231, 178));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(star);
        }

        private void drawCentered(Graphics2D g, String text, int centerX, int y) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, centerX - fm.stringWidth(text) / 2, y);
        }
    }
}
