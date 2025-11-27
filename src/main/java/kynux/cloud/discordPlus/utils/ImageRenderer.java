package kynux.cloud.discordPlus.utils;

import kynux.cloud.discordPlus.data.PlayerData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ImageRenderer {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    
    private static final Color COLOR_BG_START = new Color(15, 15, 20);
    private static final Color COLOR_BG_END = new Color(25, 28, 35);
    
    private static final Color COLOR_PANEL_GLASS = new Color(40, 40, 50, 200);
    private static final Color COLOR_PANEL_BORDER = new Color(255, 255, 255, 30);
    
    private static final Color COLOR_ACCENT = new Color(88, 101, 242);
    private static final Color COLOR_ACCENT_GLOW = new Color(88, 101, 242, 100);
    
    private static final Color COLOR_TEXT_TITLE = new Color(255, 255, 255);
    private static final Color COLOR_TEXT_LABEL = new Color(170, 170, 180);
    private static final Color COLOR_TEXT_VALUE = new Color(240, 240, 250);
    
    private static final Color COLOR_ONLINE = new Color(67, 181, 129);
    private static final Color COLOR_OFFLINE = new Color(116, 127, 141);

    private static final Font FONT_NAME = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_VALUE = new Font("Segoe UI", Font.PLAIN, 26);
    private static final Font FONT_FOOTER = new Font("Segoe UI", Font.ITALIC, 12);

    public static InputStream createProfileImage(PlayerData playerData, String playerName, boolean isOnline) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        drawBackground(g2d);

        int sidePanelWidth = 280;
        int padding = 25;
        
        g2d.setColor(new Color(0,0,0, 100));
        g2d.fillRoundRect(padding+5, padding+5, sidePanelWidth, HEIGHT - (padding*2), 30, 30);
        
        g2d.setColor(COLOR_PANEL_GLASS);
        g2d.fillRoundRect(padding, padding, sidePanelWidth, HEIGHT - (padding*2), 30, 30);
        
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(COLOR_PANEL_BORDER);
        g2d.drawRoundRect(padding, padding, sidePanelWidth, HEIGHT - (padding*2), 30, 30);

        drawSkin(g2d, playerData.getUuid(), padding, sidePanelWidth);

        int infoStartX = sidePanelWidth + (padding * 2);
        
        drawHeader(g2d, playerName, isOnline, infoStartX, 80);

        int gridY = 160;
        int colGap = 240;
        int rowGap = 100;

        drawStatCard(g2d, "OYUN SÜRESİ", TimeUtil.formatDuration(playerData.getTotalPlaytime() * 1000L), infoStartX, gridY);
        drawStatCard(g2d, "K/D ORANI", String.format("%.2f", playerData.getKDRatio()), infoStartX + colGap, gridY);
        drawStatCard(g2d, "ÖLDÜRME", String.valueOf(playerData.getKillCount()), infoStartX, gridY + rowGap);
        drawStatCard(g2d, "ÖLME", String.valueOf(playerData.getDeathCount()), infoStartX + colGap, gridY + rowGap);

        drawFooter(g2d, infoStartX);

        g2d.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return new ByteArrayInputStream(os.toByteArray());
    }

    private static void drawBackground(Graphics2D g2d) {
        GradientPaint bgGradient = new GradientPaint(0, 0, COLOR_BG_START, WIDTH, HEIGHT, COLOR_BG_END);
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(new Color(255, 255, 255, 8));
        int gridSize = 40;
        for (int x = 0; x < WIDTH; x += gridSize) {
            for (int y = 0; y < HEIGHT; y += gridSize) {
                if ((x + y) % (gridSize * 2) == 0) {
                    g2d.fillRect(x, y, 2, 2);
                }
            }
        }
        
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(88, 101, 242, 30));
        g2d.drawLine(WIDTH - 150, 0, WIDTH, 150);
        g2d.drawLine(WIDTH - 100, 0, WIDTH, 100);
    }

    private static void drawSkin(Graphics2D g2d, java.util.UUID uuid, int panelX, int panelWidth) {
        try {
            URL url = new URL("https://crafatar.com/renders/body/" + uuid + "?overlay");
            BufferedImage skin = ImageIO.read(url);
            if (skin != null) {
                int skinHeight = 320;
                int skinWidth = (int) ((double) skin.getWidth() / skin.getHeight() * skinHeight);
                int x = panelX + (panelWidth - skinWidth) / 2;
                int y = (HEIGHT - skinHeight) / 2;
                
                g2d.setColor(new Color(0,0,0,80));
                g2d.fillOval(x + 10, y + skinHeight - 20, skinWidth - 20, 15);
                
                g2d.drawImage(skin, x, y, skinWidth, skinHeight, null);
            }
        } catch (IOException e) {
            g2d.setColor(Color.GRAY);
            g2d.fillOval(panelX + panelWidth/2 - 40, HEIGHT/2 - 40, 80, 80);
        }
    }

    private static void drawHeader(Graphics2D g2d, String name, boolean isOnline, int x, int y) {
        g2d.setColor(COLOR_TEXT_TITLE);
        g2d.setFont(FONT_NAME);
        g2d.drawString(name, x, y);

        int nameWidth = g2d.getFontMetrics().stringWidth(name);
        int badgeX = x + nameWidth + 20;
        int badgeY = y - 30;
        
        Color statusColor = isOnline ? COLOR_ONLINE : COLOR_OFFLINE;
        String statusText = isOnline ? "ONLINE" : "OFFLINE";
        
        g2d.setColor(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 40));
        g2d.fillRoundRect(badgeX, badgeY, 80, 32, 20, 20);
        
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(statusColor);
        g2d.drawRoundRect(badgeX, badgeY, 80, 32, 20, 20);
        
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2d.setColor(statusColor);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textX = badgeX + (80 - fm.stringWidth(statusText)) / 2;
        int textY = badgeY + ((32 - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(statusText, textX, textY);
    }

    private static void drawStatCard(Graphics2D g2d, String label, String value, int x, int y) {
        g2d.setPaint(new GradientPaint(x, y, COLOR_ACCENT_GLOW, x, y+50, new Color(0,0,0,0)));
        g2d.fillRect(x, y, 3, 50);
        
        g2d.setColor(COLOR_ACCENT);
        g2d.fillRect(x, y, 3, 25);

        g2d.setColor(COLOR_TEXT_LABEL);
        g2d.setFont(FONT_LABEL);
        g2d.drawString(label, x + 15, y + 15);

        g2d.setColor(COLOR_TEXT_VALUE);
        g2d.setFont(FONT_VALUE);
        g2d.drawString(value, x + 15, y + 45);
    }
    
    private static void drawFooter(Graphics2D g2d, int startX) {
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawLine(startX, HEIGHT - 50, WIDTH - 40, HEIGHT - 50);
        
        g2d.setColor(COLOR_TEXT_LABEL);
        g2d.setFont(FONT_FOOTER);
        g2d.drawString("DiscordPlus System", startX, HEIGHT - 25);
        
        String date = java.time.LocalDate.now().toString();
        g2d.drawString(date, WIDTH - 100, HEIGHT - 25);
    }
}
