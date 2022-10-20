package gst;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Swing component for displaying video from a GStreamer pipeline.
 */
public class MyNewVideoComponent extends javax.swing.JComponent {

    private final Lock bufferLock = new ReentrantLock();
    private final AppSink videosink;
    private final boolean useVolatile;

    private BufferedImage currentImage = null;
    private RenderComponent renderComponent = new RenderComponent();
    private boolean keepAspect = true;
    private Timer resourceTimer;
    private VolatileImage volatileImage;
    private boolean frameRendered = false;
    private volatile boolean updatePending = false;

    /**
     * Create a GstVideoComponent. A new AppSink element will be created that
     * can be accessed using {@link #getElement()} and added to a pipeline.
     */
    public MyNewVideoComponent() {
        this(new AppSink("GstVideoComponent"));
    }

    /**
     * Create a GstVideoComponent wrapping the provided AppSink element.
     */
    public MyNewVideoComponent(AppSink appsink) {
        this.videosink = appsink;
        videosink.set("emit-signals", true);
        AppSinkListener listener = new AppSinkListener();
        videosink.connect((AppSink.NEW_SAMPLE) listener);
        videosink.connect((AppSink.NEW_PREROLL) listener);
        StringBuilder caps = new StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,");
        //StringBuilder caps = new StringBuilder("video/x-h264,colorimetry=bt709,stream-format=(string)byte-stream,alignment=(string)au,");
        // JNA creates ByteBuffer using native byte order, set masks according to that.
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            caps.append("format=BGRx");
        } else {
            caps.append("format=xRGB");
        }
        videosink.setCaps(new Caps(caps.toString()));

        useVolatile = true;

        // Kick off a timer to free up the volatile image if there have been no recent updates
        // (e.g. the player is paused)
        //
        resourceTimer = new Timer(250, resourceReaper);

        //
        // Don't use a layout manager - the output component will positioned within this
        // component according to the aspect ratio and scaling mode
        //
        setLayout(null);
        add(renderComponent);

        //
        // Listen for the child changing its preferred size to the size of the
        // video stream.
        //
        renderComponent.addPropertyChangeListener("preferredSize", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                setPreferredSize(renderComponent.getPreferredSize());
                scaleVideoOutput();
            }
        });
        //
        // Scale the video output in response to this component being resized
        //
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent arg0) {
                scaleVideoOutput();
            }

        });
        renderComponent.setBounds(getBounds());
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    /**
     * Scales the video output component according to its aspect ratio
     */
    private void scaleVideoOutput() {
        final Component child = renderComponent;
        final Dimension childSize = child.getPreferredSize();
        final int width = getWidth(), height = getHeight();
        // Figure out the aspect ratio
        double aspect = keepAspect ? (double) childSize.width / (double) childSize.height : 1.0f;

        //
        // Now scale and position the videoChild component to be in the correct position
        // to keep the aspect ratio correct.
        //
        int scaledHeight = (int) ((double) width / aspect);
        if (!keepAspect) {
            //
            // Just make the child match the parent
            //
            child.setBounds(0, 0, width, height);
        } else if (scaledHeight < height) {
            //
            // Output window is taller than the image is when scaled, so move the
            // video component to sit vertically in the centre of the VideoComponent.
            //
            final int y = (height - scaledHeight) / 2;
            child.setBounds(0, y, width, scaledHeight);
        } else {
            final int scaledWidth = (int) ((double) height * aspect);
            final int x = (width - scaledWidth) / 2;
            child.setBounds(x, 0, scaledWidth, height);
        }
    }
    private ActionListener resourceReaper = new ActionListener() {
        public void actionPerformed(ActionEvent arg0) {
            if (!frameRendered) {
                if (volatileImage != null) {
                    volatileImage.flush();
                    volatileImage = null;
                }

                // Stop the timer so we don't wakeup needlessly
                resourceTimer.stop();
            }
            frameRendered = false;
        }
    };

    /**
     * Get the wrapped AppSink element.
     *
     * @return sink element
     */
    public Element getElement() {
        return videosink;
    }

    /**
     * Set whether to respect the aspect ratio of the video when scaling.
     * Defaults to true.
     *
     * @param keepAspect respect aspect ratio
     */
    public void setKeepAspect(boolean keepAspect) {
        this.keepAspect = keepAspect;
    }

    @Override
    public boolean isLightweight() {
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
        }
    }

    private class RenderComponent extends javax.swing.JComponent {

        private static final long serialVersionUID = -4736605073704494268L;

        @Override
        protected void paintComponent(Graphics g) {
            int width = getWidth(), height = getHeight();
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (currentImage != null) {
                GraphicsConfiguration gc = getGraphicsConfiguration();
                render(g2d, 0, 0, width, height);
            } else {
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, width, height);
            }
            g2d.dispose();
        }

        @Override
        public boolean isOpaque() {
            return MyNewVideoComponent.this.isOpaque();
        }

        @Override
        public boolean isLightweight() {
            return true;
        }
    }

    private void renderVolatileImage(BufferedImage bufferedImage) {
        do {
            int w = bufferedImage.getWidth(), h = bufferedImage.getHeight();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (volatileImage == null || volatileImage.getWidth() != w
                    || volatileImage.getHeight() != h
                    || volatileImage.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                if (volatileImage != null) {
                    volatileImage.flush();
                }
                volatileImage = gc.createCompatibleVolatileImage(w, h);
                volatileImage.setAccelerationPriority(1.0f);
            }
            //
            // Now paint the BufferedImage into the accelerated image
            //
            Graphics2D g = volatileImage.createGraphics();
            g.drawImage(bufferedImage, 0, 0, null);
            g.dispose();
        } while (volatileImage.contentsLost());
    }

    /**
     * Renders to a volatile image, and then paints that to the screen. This
     * helps with scaling performance on accelerated surfaces (e.g. OpenGL)
     *
     * @param g the graphics to paint the image to
     * @param x the left coordinate to start painting at.
     * @param y the top coordinate to start painting at.
     * @param w the width of the paint area
     * @param h the height of the paint area
     */
    private void volatileRender(Graphics g, int x, int y, int w, int h) {
        do {
            if (updatePending || volatileImage == null
                    || volatileImage.validate(getGraphicsConfiguration()) != VolatileImage.IMAGE_OK) {
                bufferLock.lock();
                try {
                    updatePending = false;
                    renderVolatileImage(currentImage);
                } finally {
                    bufferLock.unlock();
                }
            }
            g.drawImage(volatileImage, x, y, w, h, null);
        } while (volatileImage.contentsLost());
    }

    /**
     * Renders directly to the given <tt>Graphics</tt>. This is only really
     * useful on MacOS where swing graphics are unaccelerated so using a
     * volatile just incurs an extra memcpy().
     *
     * @param g the graphics to paint the image to
     * @param x the left coordinate to start painting at.
     * @param y the top coordinate to start painting at.
     * @param w the width of the paint area
     * @param h the height of the paint area
     */
    private void heapRender(Graphics g, int x, int y, int w, int h) {
        bufferLock.lock();
        try {
            updatePending = false;
            g.drawImage(currentImage, x, y, w, h, null);
        } finally {
            bufferLock.unlock();
        }
    }

    /**
     * Renders the current frame to the given <tt>Graphics</tt>.
     *
     * @param g the graphics to paint the image to
     * @param x the left coordinate to start painting at.
     * @param y the top coordinate to start painting at.
     * @param w the width of the paint area
     * @param h the height of the paint area
     */
    private void render(Graphics g, int x, int y, int w, int h) {
        if (useVolatile) {
            volatileRender(g, x, y, w, h);
        } else {
            heapRender(g, x, y, w, h);
        }
        //
        // Restart the resource reaper timer if neccessary
        //
        if (!frameRendered) {
            frameRendered = true;
            if (!resourceTimer.isRunning()) {
                resourceTimer.restart();
            }
        }
    }

    private int imgWidth = 0, imgHeight = 0;

    private final void update(final int width, final int height) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                //
                // If the image changed size, resize the component to fit
                //
                if (width != imgWidth || height != imgHeight) {
                    renderComponent.setPreferredSize(new Dimension(width, height));
                    imgWidth = width;
                    imgHeight = height;
                }

                if (renderComponent.isVisible()) {
                    renderComponent.paintImmediately(0, 0,
                            renderComponent.getWidth(), renderComponent.getHeight());
                }
            }
        });
    }

    private BufferedImage getBufferedImage(int width, int height) {
        if (currentImage != null && currentImage.getWidth() == width
                && currentImage.getHeight() == height) {
            return currentImage;
        }
        if (currentImage != null) {
            currentImage.flush();
        }
        currentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        currentImage.setAccelerationPriority(0.0f);
        return currentImage;
    }

    private class AppSinkListener implements AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {

        @Override
        public FlowReturn newSample(AppSink elem) {
            Sample sample = elem.pullSample();
            Structure capsStruct = sample.getCaps().getStructure(0);
            int w = capsStruct.getInteger("width");
            int h = capsStruct.getInteger("height");
            Buffer buffer = sample.getBuffer();
            ByteBuffer bb = buffer.map(false);
            if (bb != null) {
                rgbFrame(false, w, h, bb.asIntBuffer());
                buffer.unmap();
            }
            sample.dispose();
            return FlowReturn.OK;
        }

        @Override
        public FlowReturn newPreroll(AppSink elem) {
            Sample sample = elem.pullPreroll();
            Structure capsStruct = sample.getCaps().getStructure(0);
            int w = capsStruct.getInteger("width");
            int h = capsStruct.getInteger("height");
            Buffer buffer = sample.getBuffer();
            ByteBuffer bb = buffer.map(false);
            if (bb != null) {
                rgbFrame(true, w, h, bb.asIntBuffer());
                buffer.unmap();
            }
            sample.dispose();
            return FlowReturn.OK;
        }

        private void rgbFrame(boolean isPrerollFrame, int width, int height, IntBuffer rgb) {
            // If the EDT is still copying data from the buffer, just drop this frame
            //
            if (!bufferLock.tryLock()) {
                return;
            }

            //
            // If there is already a swing update pending, also drop this frame.
            //
            if (updatePending && !isPrerollFrame) {
                bufferLock.unlock();
                return;
            }
            try {
                final BufferedImage renderImage = getBufferedImage(width, height);
                int[] pixels = ((DataBufferInt) renderImage.getRaster().getDataBuffer()).getData();
                rgb.get(pixels, 0, width * height);
                updatePending = true;
            } finally {
                bufferLock.unlock();
            }

//            int scaledWidth = currentImage.getWidth();
//            if (keepAspect) {
//                // Scale width according to pixel aspect ratio.
//                Caps videoCaps = videoPad.getNegotiatedCaps();
//                Structure capsStruct = videoCaps.getStructure(0);
//                if (capsStruct.hasField("pixel-aspect-ratio")) {
//                    Fraction pixelAspectRatio = capsStruct.getFraction("pixel-aspect-ratio");
//                    scaledWidth = scaledWidth * pixelAspectRatio.getNumerator() / pixelAspectRatio.getDenominator();
//                }
//            }
            // Tell swing to use the new buffer
            update(currentImage.getWidth(), currentImage.getHeight());
        }

    }
}
