package tests;


import org.cef.browser.CefBrowser;
import org.cef.callback.CefNativeAdapter;
import tests.JBCefOsrHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;



@SuppressWarnings("NotNullFieldNotInitialized")
public
class JBCefOsrComponent extends JPanel {
    private volatile JBCefOsrHandler myRenderHandler;
    private volatile CefBrowser myBrowser;
    private final MyScale myScale = new MyScale();
    
    private Timer myTimer;
    
    public JBCefOsrComponent() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.CYAN);
        addPropertyChangeListener("graphicsConfiguration",
                e -> myRenderHandler.updateScale(myScale.update(myRenderHandler.getDeviceScaleFactor(myBrowser))));

        enableEvents(AWTEvent.KEY_EVENT_MASK |
                AWTEvent.MOUSE_EVENT_MASK |
                AWTEvent.MOUSE_WHEEL_EVENT_MASK |
                AWTEvent.MOUSE_MOTION_EVENT_MASK);

        setFocusable(true);
        setRequestFocusEnabled(true);
        // [tav] todo: so far the browser component can not be traversed out
        setFocusTraversalKeysEnabled(false);
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                myBrowser.setFocus(true);
            }
            @Override
            public void focusLost(FocusEvent e) {
                myBrowser.setFocus(false);
            }
        });
    }

    public void setBrowser(CefBrowser browser) {
        myBrowser = browser;
    }

    public void setRenderHandler(JBCefOsrHandler renderHandler) {
        myRenderHandler = renderHandler;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (((CefNativeAdapter)myBrowser).getNativeRef("CefBrowser") == 0) {
            myBrowser.createImmediately();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        myRenderHandler.paint((Graphics2D)g);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reshape(int x, int y, int w, int h) {
        super.reshape(x, y, w, h);
        if (myTimer != null) {
            myTimer.stop();
        }

        double scale = myScale.getInverted();
        myTimer = new Timer(100, e -> {
            myBrowser.wasResized((int) Math.ceil(w * scale), (int) Math.ceil(h * scale));
        });
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);

        double scale = myScale.getIdeBiased();
        myBrowser.sendMouseEvent(new MouseEvent(
                e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));

        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            requestFocusInWindow();
        }
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        super.processMouseWheelEvent(e);

        double val = e.getPreciseWheelRotation() * Integer.getInteger("ide.browser.jcef.osr.wheelRotation.factor", 1) * (-1);
        double scale = myScale.getIdeBiased();
        myBrowser.sendMouseWheelEvent(new MouseWheelEvent(
                e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getScrollType(),
                e.getScrollAmount(),
                (int)val,
                val));
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);

        double scale = myScale.getIdeBiased();
        myBrowser.sendMouseEvent(new MouseEvent(
                e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiersEx(),
                (int)Math.round(e.getX() / scale),
                (int)Math.round(e.getY() / scale),
                (int)Math.round(e.getXOnScreen() / scale),
                (int)Math.round(e.getYOnScreen() / scale),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        myBrowser.sendKeyEvent(e);
    }

    static class MyScale {
        private volatile double myScale = 1;
        private volatile double myInvertedScale = 1;

        public MyScale update(double scale) {
            myScale = scale;
            return this;
        }

        public MyScale update(MyScale scale) {
            myScale = scale.myScale;
            myInvertedScale = scale.myInvertedScale;
            return this;
        }

        public double get() {
            return myScale;
        }

        public double getInverted() {
            return myScale;
        }

        public double getIdeBiased() {
            // IDE-managed HiDPI
            return 1;
        }

        public double getJreBiased() {
            // JRE-managed HiDPI
            return myScale;
        }
    }
}