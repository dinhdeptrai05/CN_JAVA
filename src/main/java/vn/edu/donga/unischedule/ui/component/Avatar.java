package vn.edu.donga.unischedule.ui.component;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class Avatar extends JComponent {
    private BufferedImage photo;
    public Avatar(long identity, String name, int size) {
        setPreferredSize(new Dimension(size, size));
        setToolTipText(name + " (ảnh minh họa)");
        try (var stream = Avatar.class.getResourceAsStream("/images/reference-avatar-" + Math.floorMod(identity, 3) + ".png")) {
            if (stream != null) photo = ImageIO.read(stream);
        } catch (java.io.IOException ex) { photo = null; }
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
        if (photo != null) g.drawImage(photo, 0, 0, getWidth(), getHeight(), null);
        else HeroIcons.of("user-circle", getWidth(), Color.decode("#5850EC")).paintIcon(this, g, 0, 0);
        g.dispose();
    }
}
