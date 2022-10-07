package tests.junittests;

import org.cef.misc.CefLog;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tests.OsrSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SwingComponentsTest {
    public static CountDownLatch latch;

    static TestStage testStage;

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private Robot robot;
    private JFrame testFrame;

    @Test
    public void testRobot() throws InvocationTargetException, InterruptedException {
        // debug helper for JBR-4649
        CefLog.Info("Start SwingComponentsTest.testRobot");
        try {
            TestFrame.addGlobalMouseListener();
            SwingUtilities.invokeAndWait(()->{
                testFrame = new JFrame("TestRobot");
                CefLog.Info("Created test frame: %s", testFrame);
                testFrame.setResizable(false);
                testFrame.setSize(400, 300);
                testFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                testFrame.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        latch.countDown();
                        super.mousePressed(e);
                    }
                });

                testFrame.setVisible(true);
            });

            Robot r = new Robot();
            r.waitForIdle();
            final long delayMs = 500;
            Thread.sleep(delayMs);

            PointerInfo pi0 = MouseInfo.getPointerInfo();
            CefLog.Debug("p0: %s", pi0.getLocation());

            Point frameCenter = new Point(testFrame.getLocationOnScreen().x + testFrame.getWidth() / 2,
                    testFrame.getLocationOnScreen().y + testFrame.getHeight() / 2);
            r.mouseMove(frameCenter.x, frameCenter.y);
            r.waitForIdle();
            Thread.sleep(delayMs);

            PointerInfo pi1 = MouseInfo.getPointerInfo();
            CefLog.Debug("p1: %s", pi1.getLocation());

            int delte = 10;
            r.mouseMove(frameCenter.x + delte, frameCenter.y + delte);
            r.waitForIdle();
            Thread.sleep(delayMs);

            PointerInfo pi2 = MouseInfo.getPointerInfo();
            CefLog.Debug("p2: %s", pi2.getLocation());

            Assert.assertEquals(pi2.getLocation().x - pi1.getLocation().x, delte);
            Assert.assertEquals(pi2.getLocation().y - pi1.getLocation().y, delte);

            CefLog.Debug("Moving mouse works correctly, now test listener for mouse_pressed event");

            latch = new CountDownLatch(1);
            r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            latch.await(2, TimeUnit.SECONDS);
            Assert.assertEquals(0, latch.getCount());

            CefLog.Info("Test PASSED");
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeAndWait(testFrame::dispose);
            TestFrame.removeGlobalMouseListener();
        }
    }

    @Test
    public void testMouseListener() throws InvocationTargetException, InterruptedException {
        // reproducer for JBR-4884
        CefLog.Info("Start SwingComponentsTest.testMouseListener, mode = %s", OsrSupport.isEnabled() ? "OSR" : "Window");
        try {
            robot = new Robot();
            SwingUtilities.invokeAndWait(()->{
                testFrame = new TestFrame(WIDTH, HEIGHT, null, OsrSupport.isEnabled());
            });
            robot.waitForIdle();
            doMouseActions();
            System.err.println("Test PASSED");
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeAndWait(testFrame::dispose);
        }
    }

    @Test
    public void testMouseListenerWithHideAndShow() throws InvocationTargetException, InterruptedException {
        // reproducer for JBR-4884
        CefLog.Info("Start SwingComponentsTest.testMouseListenerWithHideAndShow, mode = %s", OsrSupport.isEnabled() ? "OSR" : "Window");
        try {
            robot = new Robot();
            SwingUtilities.invokeAndWait(()->{
                testFrame = new TestFrame(WIDTH, HEIGHT, null, OsrSupport.isEnabled());
            });
            SwingUtilities.invokeLater(()-> {
                ((TestFrame)testFrame).addremove();
            });
            robot.waitForIdle();
            doMouseActions();
            System.err.println("Test PASSED");
        } catch (AWTException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeAndWait(testFrame::dispose);
        }
    }

    private void doMouseActions() throws InterruptedException {
        Point frameCenter = new Point(testFrame.getLocationOnScreen().x + testFrame.getWidth() / 2,
                testFrame.getLocationOnScreen().y + testFrame.getHeight() / 2);
        robot.mouseMove(frameCenter.x, frameCenter.y);

        testStage = TestStage.MOUSE_PRESSED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_DRAGGED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        // Empiric observation: robot.mouseMove with small shifts (1-3 pixels) doesn't produce real moves
        // So we must use quite large shifts
        robot.mouseMove(frameCenter.x + testFrame.getWidth() / 4, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_RELEASED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_CLICKED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_MOVED;
        System.err.println("Stage: " + testStage.name());
        latch = new CountDownLatch(1);
        robot.mouseMove(frameCenter.x + 2, frameCenter.y);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");

        testStage = TestStage.MOUSE_WHEEL_MOVED;
        latch = new CountDownLatch(1);
        robot.mouseWheel(1);
        latch.await(2, TimeUnit.SECONDS);
        if (latch.getCount() > 0) throw new RuntimeException("ERROR: " + testStage.name() + " action was not handled.");
    }

    enum TestStage {
        MOUSE_ENTERED,
        MOUSE_EXITED,
        MOUSE_MOVED,
        MOUSE_DRAGGED,
        MOUSE_CLICKED,
        MOUSE_PRESSED,
        MOUSE_RELEASED,
        MOUSE_WHEEL_MOVED
    }

    @Test
    @Disabled("Reproducer for JBR-4833")
    public void testPaintOccured() {
        for (int c = 0; c < 10; ++c) {
            try {
                CountDownLatch paintLatch = new CountDownLatch(1);
                SwingUtilities.invokeAndWait(()->{
                    testFrame = new TestFrame(WIDTH, HEIGHT, paintLatch, false);
                });
                if(!paintLatch.await(5, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Paint wasn't occured in 5 seconds");
                }
                Assert.assertTrue(paintLatch.getCount() < 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                SwingUtilities.invokeLater(testFrame::dispose);
            }
        }
    }

    // Test component with the same hierarchy structure as CefBrowserWr
    static class TestComponentWr extends JPanel {
        private Canvas canvas_ = null;
        private final CountDownLatch paintLatch;

        private MouseListener mouseListener;
        private MouseWheelListener mouseWheelListener;
        private MouseMotionListener mouseMotionListener;

        @SuppressWarnings("DuplicatedCode")
        @Override
        protected void processMouseEvent(MouseEvent e) {
            System.err.println("processMouseEvent: " + e);
            super.processMouseEvent(e);
        }

        @Override
        public void addMouseListener(MouseListener l) {
            mouseListener = l;
            if (canvas_ != null)
                canvas_.addMouseListener(l);
            // NOTE: should also add listener into parent JPanel
            // otherwise testMouseListenerWithHideAndShow starts fail
            super.addMouseListener(l);
        }
        @Override
        public void addMouseWheelListener(MouseWheelListener l) {
            mouseWheelListener = l;
            if (canvas_ != null)
                canvas_.addMouseWheelListener(l);
            super.addMouseWheelListener(l);
        }
        @Override
        public void addMouseMotionListener(MouseMotionListener l) {
            mouseMotionListener = l;
            if (canvas_ != null)
                canvas_.addMouseMotionListener(l);
            super.addMouseMotionListener(l);
        }

        @Override
        public void removeMouseListener(MouseListener l) {
            mouseListener = null;
            if (canvas_ != null)
                canvas_.removeMouseListener(l);
            super.removeMouseListener(l);
        }

        @Override
        public void removeMouseMotionListener(MouseMotionListener l) {
            mouseMotionListener = null;
            if (canvas_ != null)
                canvas_.removeMouseMotionListener(l);
            super.removeMouseMotionListener(l);
        }

        @Override
        public void removeMouseWheelListener(MouseWheelListener l) {
            mouseWheelListener = null;
            if (canvas_ != null)
                canvas_.removeMouseWheelListener(l);
            super.removeMouseWheelListener(l);
        }

        public TestComponentWr(CountDownLatch paintLatch) {
            super(new BorderLayout());
            this.paintLatch = paintLatch;
        }

        private void addCanvas() {
            canvas_ = new Canvas();
            if (mouseListener != null) canvas_.addMouseListener(mouseListener);
            if (mouseWheelListener != null) canvas_.addMouseWheelListener(mouseWheelListener);
            if (mouseMotionListener != null) canvas_.addMouseMotionListener(mouseMotionListener);
            this.add(canvas_, BorderLayout.CENTER);
        }
        private void removeCanvas() {
            if (canvas_ == null) return;
            if (mouseListener != null) canvas_.removeMouseListener(mouseListener);
            if (mouseWheelListener != null) canvas_.removeMouseWheelListener(mouseWheelListener);
            if (mouseMotionListener != null) canvas_.removeMouseMotionListener(mouseMotionListener);
            this.remove(canvas_);
            canvas_ = null;
        }
        @Override
        public void addNotify() {
            // NOTE: generally it's a bad idea to add components inside addNotify
            // It'd be better to add component before super.addNotify (because it perfroms some processing with
            // all children - send notifications, set listeners etc)
            addCanvas();
            super.addNotify();
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            removeCanvas();
        }

        @Override
        public void paint(Graphics g) {
            if (paintLatch != null) {
                paintLatch.countDown();
                Window frame = SwingUtilities.getWindowAncestor(this);
                if (frame != null)
                    SwingUtilities.invokeLater(()->frame.dispose());
            }
            super.paint(g);
        }
    }

    // Test component with the same hierarchy structure as CefBrowserOsrWithHandler
    static class TestComponentOsr extends JPanel {
            public TestComponentOsr() {
                setPreferredSize(new Dimension(800, 600));
                setBackground(Color.CYAN);

                enableEvents(AWTEvent.KEY_EVENT_MASK |
                        AWTEvent.MOUSE_EVENT_MASK |
                        AWTEvent.MOUSE_WHEEL_EVENT_MASK |
                        AWTEvent.MOUSE_MOTION_EVENT_MASK);

                setFocusable(true);
                setRequestFocusEnabled(true);
                setFocusTraversalKeysEnabled(false);
            }

            @SuppressWarnings("deprecation")
            @Override
            public void reshape(int x, int y, int w, int h) {
                super.reshape(x, y, w, h);
            }

            @SuppressWarnings("DuplicatedCode")
            @Override
            protected void processMouseEvent(MouseEvent e) {
                super.processMouseEvent(e);
                System.err.println("processMouseEvent: " + e);
                //latch.countDown();
                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    requestFocusInWindow();
                }
            }

            @Override
            protected void processMouseWheelEvent(MouseWheelEvent e) {
                System.err.println("processMouseWheelEvent: " + e);
                //latch.countDown();
                super.processMouseWheelEvent(e);
            }

            @SuppressWarnings("DuplicatedCode")
            @Override
            protected void processMouseMotionEvent(MouseEvent e) {
                System.err.println("processMouseMotionEvent: " + e);
                //latch.countDown();
                super.processMouseMotionEvent(e);
            }

            @Override
            protected void processKeyEvent(KeyEvent e) {
                System.err.println("processKeyEvent: " + e);
                //latch.countDown();
                super.processKeyEvent(e);
            }
    }

    static class TestFrame extends JFrame {
        private final Component testComponent;
        private static AWTEventListener awtListener;

        public static void addGlobalMouseListener() {
            CefLog.Debug("Add global AWT mouse event listener");
            awtListener = new AWTEventListener() {
                @Override
                public void eventDispatched(AWTEvent event) {
                    CefLog.Debug("awt event: %s, src: %s", event, event.getSource());
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }

        public static void removeGlobalMouseListener() {
            if (awtListener != null) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
            }
        }

        private MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                System.err.println("mouseDragged: " + e);
                if (testStage == TestStage.MOUSE_DRAGGED) {
                    latch.countDown();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                System.err.println("mouseMoved: " + e);
                if (testStage == TestStage.MOUSE_MOVED) {
                    latch.countDown();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                System.err.println("mouseWheelMoved: " + e);
                if (testStage == TestStage.MOUSE_WHEEL_MOVED) {
                    latch.countDown();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                System.err.println("mouseClicked: " + e);
                if (testStage == TestStage.MOUSE_CLICKED) {
                    latch.countDown();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                System.err.println("mousePressed: " + e);
                if (testStage == TestStage.MOUSE_PRESSED) {
                    latch.countDown();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                System.err.println("mouseReleased: " + e);
                if (testStage == TestStage.MOUSE_RELEASED) {
                    latch.countDown();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (testStage == TestStage.MOUSE_ENTERED) {
                    System.err.println("mouseEntered");
                    latch.countDown();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (testStage == TestStage.MOUSE_EXITED) {
                    System.err.println("mouseExited");
                    latch.countDown();
                }
            }
        };

        TestFrame(int width, int height, CountDownLatch paintLatch/* Wr mode only*/, boolean isOsr) {
            super("TestFrame");

            // Init UI
            testComponent = isOsr ? new TestComponentOsr() : new TestComponentWr(paintLatch);
            testComponent.addMouseMotionListener(mouseAdapter);
            testComponent.addMouseListener(mouseAdapter);
            testComponent.addMouseWheelListener(mouseAdapter);

            setResizable(false);
            getContentPane().add(testComponent);
            pack();
            setSize(width, height);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setVisible(true);

            addGlobalMouseListener();
        }

        @Override
        public void dispose() {
            removeGlobalMouseListener();
            super.dispose();
        }

        void addremove() {
            Container parent = testComponent.getParent();
            parent.remove(testComponent);
            parent.add(testComponent);
        }
    }
}