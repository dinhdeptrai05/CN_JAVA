package vn.edu.donga.unischedule.ui.component;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class Avatar extends JComponent {
    private BufferedImage photo;
    private vn.edu.donga.unischedule.model.User user;
    private final java.beans.PropertyChangeListener listener = event -> javax.swing.SwingUtilities.invokeLater(() -> { loadPhoto(); repaint(); });
    public Avatar(vn.edu.donga.unischedule.model.User user, int size) {
        this.user=user;setPreferredSize(new Dimension(size,size));setToolTipText(user.getFullName());loadPhoto();
    }
    private void loadPhoto() {
        byte[] data=user.getAvatarData();photo=null;
        if(data!=null) try { photo=ImageIO.read(new java.io.ByteArrayInputStream(data)); } catch(java.io.IOException ignored) { photo=null; }
    }
    @Override public void addNotify() { super.addNotify();if(user!=null) {user.addAvatarListener(listener);loadPhoto();} }
    @Override public void removeNotify() { if(user!=null)user.removeAvatarListener(listener);super.removeNotify(); }
    public Avatar(long identity, String name, int size) {
        setPreferredSize(new Dimension(size, size));
        setToolTipText(name);
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
