package vn.edu.donga.unischedule.ui.component;

import vn.edu.donga.unischedule.config.AppConfig;
import vn.edu.donga.unischedule.util.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Runs blocking work off the Swing event thread with a visible, input-blocking progress overlay. */
public final class UiTasks {
    private UiTasks() { }

    public static void run(Component parent, String waitingMessage, Runnable work, Runnable success) {
        run(parent, waitingMessage, () -> { work.run(); return null; }, ignored -> success.run(),
                error -> Dialogs.error(parent, message(error)));
    }

    public static <T> void run(Component parent, String waitingMessage, Callable<T> work,
                               Consumer<T> success, Consumer<Throwable> failure) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("UiTasks must be started on the Swing event thread.");
        JRootPane root = SwingUtilities.getRootPane(parent);
        Component previous = root == null ? null : root.getGlassPane();
        boolean previousVisible = previous != null && previous.isVisible();
        if (previous instanceof BusyPane busy && busy.isVisible()) return;
        BusyPane overlay = root == null ? null : new BusyPane(waitingMessage);
        if (overlay != null) {
            root.setGlassPane(overlay);
            overlay.setVisible(true);
            overlay.requestFocusInWindow();
        }
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                if (root != null && root.getGlassPane() == overlay) {
                    overlay.setVisible(false);
                    root.setGlassPane(previous);
                    previous.setVisible(previousVisible);
                }
                T value;
                try { value = get(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); failure.accept(ex); return; }
                catch (ExecutionException ex) { failure.accept(ex.getCause() == null ? ex : ex.getCause()); return; }
                try { success.accept(value); }
                catch (RuntimeException ex) {
                    Dialogs.warning(parent, "Thao tác đã hoàn tất nhưng không tải lại được giao diện: " + message(ex));
                }
            }
        }.execute();
    }

    public static String message(Throwable error) {
        String text = error.getMessage();
        return text == null || text.isBlank() ? "Thao tác không thành công. Vui lòng thử lại." : text;
    }

    private static final class BusyPane extends JPanel {
        private BusyPane(String message) {
            super(new GridBagLayout());
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            addMouseListener(new MouseAdapter() { });
            addMouseMotionListener(new MouseAdapter() { });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent event) { event.consume(); }
            });
            JPanel card = new JPanel(new java.awt.BorderLayout(0, 12));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 236)),
                    BorderFactory.createEmptyBorder(18, 24, 18, 24)));
            JLabel label = new JLabel(message == null || message.isBlank() ? "Đang xử lý…" : message);
            label.setForeground(AppConfig.TEXT);
            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setForeground(AppConfig.PRIMARY);
            card.add(label, java.awt.BorderLayout.NORTH);
            card.add(progress, java.awt.BorderLayout.SOUTH);
            add(card);
        }

        @Override protected void paintComponent(java.awt.Graphics graphics) {
            graphics.setColor(new Color(15, 23, 42, 90));
            graphics.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(graphics);
        }
    }
}
