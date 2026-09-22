package vn.edu.donga.unischedule;

import org.junit.jupiter.api.Test;
import vn.edu.donga.unischedule.ui.component.UiTasks;

import javax.swing.JRootPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UiTasksTest {
    @Test void busyOverlayRunsWorkOffEdtAndRestoresUiBeforeReportingSuccess() throws Exception {
        JRootPane root = new JRootPane();
        JPanel panel = new JPanel();
        root.getContentPane().add(panel);
        Component previous = root.getGlassPane();
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean workerOffEdt = new AtomicBoolean();
        AtomicBoolean overlayShown = new AtomicBoolean();
        AtomicBoolean callbackOnEdt = new AtomicBoolean();
        AtomicBoolean restored = new AtomicBoolean();
        AtomicReference<Integer> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            UiTasks.run(panel, "Đang kiểm tra…", () -> {
                workerOffEdt.set(!SwingUtilities.isEventDispatchThread());
                return 42;
            }, value -> {
                callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
                result.set(value);
                restored.set(previous == root.getGlassPane());
                finished.countDown();
            }, error -> { failure.set(error); finished.countDown(); });
            overlayShown.set(root.getGlassPane() != previous && root.getGlassPane().isVisible());
        });
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertNull(failure.get());
        assertTrue(workerOffEdt.get());
        assertTrue(overlayShown.get());
        assertTrue(callbackOnEdt.get());
        assertTrue(restored.get());
        assertEquals(42, result.get());
    }

    @Test void failedWorkReportsErrorOnEdtAndRemovesOverlay() throws Exception {
        JRootPane root = new JRootPane();
        JPanel panel = new JPanel();
        root.getContentPane().add(panel);
        Component previous = root.getGlassPane();
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> reported = new AtomicReference<>();
        AtomicBoolean callbackOnEdt = new AtomicBoolean();
        AtomicBoolean restored = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> UiTasks.run(panel, "Đang lưu…",
                () -> { throw new IllegalArgumentException("Dữ liệu không hợp lệ"); },
                ignored -> finished.countDown(), error -> {
                    reported.set(error);
                    callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
                    restored.set(previous == root.getGlassPane());
                    finished.countDown();
                }));
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalArgumentException.class, reported.get());
        assertEquals("Dữ liệu không hợp lệ", UiTasks.message(reported.get()));
        assertTrue(callbackOnEdt.get());
        assertTrue(restored.get());
    }
}
