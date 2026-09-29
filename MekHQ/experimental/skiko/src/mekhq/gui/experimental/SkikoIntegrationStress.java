/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.utilities.MHQInternationalization;
import org.jetbrains.skia.Canvas;
import org.jetbrains.skia.Font;
import org.jetbrains.skia.FontMgr;
import org.jetbrains.skia.FontStyle;
import org.jetbrains.skia.Paint;
import org.jetbrains.skia.PixelGeometry;
import org.jetbrains.skia.Typeface;
import org.jetbrains.skiko.SkiaLayer;
import org.jetbrains.skiko.SkiaLayerAnalytics;
import org.jetbrains.skiko.SkiaLayerProperties;

public final class SkikoIntegrationStress {
    private static final String BUNDLE = "mekhq.resources.SkikoIntegrationStress";
    private static final int BACKGROUND = 0xFF142523;
    private static final int STAR_COUNT = 700;
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private final JFrame frame = new JFrame(text("title"));
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = new JLabel(text("starting"));
    private final JCheckBox animate = new JCheckBox(text("animate"), true);
    private final JCheckBox surfaceVisible = new JCheckBox(text("surface"), true);
    private final JCheckBox overlayVisible = new JCheckBox(text("overlay"), true);
    private final JCheckBox heavyweightPopup = new JCheckBox(text("heavyweight"));
    private final JTextField overlayInput = new JTextField(12);
    private final JPanel overlay = new JPanel(new FlowLayout());
    private final JLayeredPane layers = new JLayeredPane() {
        @Override
        public void doLayout() {
            if (surface != null) {
                surface.setBounds(0, 0, getWidth(), getHeight());
            }
            overlay.setBounds(scaled(24), scaled(24), scaled(310), scaled(76));
        }
    };
    private final JScrollPane scroll;
    private final Timer animationTimer;
    private final Timer statusTimer;
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private SkiaLayer surface;
    private Paint paint;
    private Font font;
    private Typeface typeface;
    private JPopupMenu popup;
    private Timer cycleTimer;
    private long frames;
    private long previousFrame;
    private long largestGap;
    private long previousStatusFrames;
    private long previousStatusTime = System.nanoTime();
    private String callbackThread = "-";
    private int generation;
    private int clicks;
    private double phase;
    private boolean closed;

    private SkikoIntegrationStress() {
        Random random = new Random(8969);
        for (int index = 0; index < STAR_COUNT; index++) {
            starX[index] = random.nextFloat();
            starY[index] = random.nextFloat();
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        overlay.setBackground(new Color(238, 224, 179));
        overlay.add(new JLabel(text("callsign")));
        overlayInput.setName("overlayInput");
        overlayInput.setToolTipText(text("inputTip"));
        overlay.add(overlayInput);
        JButton overlayAction = button("contact", () -> {
            clicks++;
            System.out.println("OVERLAY_ACTION " + clicks);
        });
        overlayAction.setToolTipText(text("contactTip"));
        overlay.add(overlayAction);
        layers.add(overlay, JLayeredPane.PALETTE_LAYER);
        layers.setPreferredSize(new Dimension(scaled(820), scaled(1100)));
        JScrollablePanel scrollable = new JScrollablePanel();
        scrollable.setLayout(new BorderLayout());
        scrollable.add(layers);
        scroll = new JScrollPane(scrollable);
        scroll.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
        tabs.addTab(text("map"), scroll);
        JTable table = new JTable(new Object[][] { { "Sol", "0, 0" }, { "Terra", "1, 1" } },
              new Object[] { text("system"), text("coordinates") });
        tabs.addTab(text("roster"), new JScrollPane(table));
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(scaled(12), scaled(12), scaled(12), scaled(12)));
        controls.add(animate);
        controls.add(surfaceVisible);
        controls.add(overlayVisible);
        controls.add(heavyweightPopup);
        JComboBox<String> sectors = new JComboBox<>(new String[] { text("sector"), "Sol", "Terra" });
        sectors.setMaximumSize(sectors.getPreferredSize());
        controls.add(sectors);
        controls.add(button("modal", () -> showModal(false)));
        controls.add(button("recreate", this::recreateSurface));
        controls.add(button("cycle", () -> startCycles(false)));
        controls.add(button("reopen", () -> {
            close();
            new SkikoIntegrationStress().show();
        }));
        controls.add(Box.createVerticalGlue());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, tabs);
        split.setResizeWeight(0);
        split.setDividerLocation(scaled(240));
        frame.add(split, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
        Rectangle desktop = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(scaled(1100), desktop.width), Math.min(scaled(760), desktop.height));
        frame.setLocationByPlatform(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                close();
            }
        });
        surfaceVisible.addActionListener(event -> surface.setVisible(surfaceVisible.isSelected()));
        overlayVisible.addActionListener(event -> overlay.setVisible(overlayVisible.isSelected()));
        recreateSurface();
        animationTimer = new Timer(16, event -> {
            if (animate.isSelected() && surface.isShowing()) {
                phase = System.nanoTime() / 1_000_000_000.0;
                surface.needRender(true);
            }
        });
        statusTimer = new Timer(1000, event -> updateStatus());
    }

    private void recreateSurface() {
        requireEdt();
        if (popup != null) {
            popup.setVisible(false);
        }
        releaseSurface();
          surface = new SkiaLayer(null, new SkiaLayerProperties(), SkiaLayerAnalytics.Companion.getEmpty(),
              PixelGeometry.UNKNOWN);
        paint = new Paint();
          typeface = Objects.requireNonNull(FontMgr.Companion.getDefault().matchFamilyStyle(
              UIManager.getFont("Label.font").getFamily(), FontStyle.Companion.getNORMAL()),
              "Skia could not resolve the Swing label font");
        font = new Font(typeface, scaled(14));
        generation++;
        surface.setRenderDelegate(this::render);
        surface.setFocusable(true);
        surface.getAccessibleContext().setAccessibleName(text("map"));
        surface.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                surface.requestFocusInWindow();
                showPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                showPopup(event);
            }
        });
        surface.setVisible(surfaceVisible.isSelected());
        layers.add(surface, JLayeredPane.DEFAULT_LAYER);
        layers.revalidate();
        layers.repaint();
        previousFrame = 0;
        System.out.println("SURFACE_CREATED " + generation);
    }

    private void releaseSurface() {
        if (surface == null) {
            return;
        }
        surface.dispose();
        layers.remove(surface);
        font.close();
        typeface.close();
        paint.close();
        surface = null;
    }

    private void render(Canvas canvas, int width, int height, long nanoTime) {
        requireEdt();
        callbackThread = Thread.currentThread().getName();
        if (previousFrame != 0) {
            largestGap = Math.max(largestGap, nanoTime - previousFrame);
        }
        previousFrame = nanoTime;
        frames++;
        float scale = surface.getContentScale();
        canvas.scale(scale, scale);
        float logicalWidth = width / scale;
        float logicalHeight = height / scale;
        canvas.clear(BACKGROUND);
        paint.setColor(0xFF45675C);
        paint.setStrokeWidth(scaled(1));
        for (int index = 1; index < STAR_COUNT; index++) {
            if (index % 4 == 0) {
                canvas.drawLine(starX[index - 1] * logicalWidth, starY[index - 1] * logicalHeight,
                      starX[index] * logicalWidth, starY[index] * logicalHeight, paint);
            }
        }
        for (int index = 0; index < STAR_COUNT; index++) {
            float positionX = starX[index] * logicalWidth;
            float positionY = starY[index] * logicalHeight;
            paint.setColor(index % 3 == 0 ? 0xFFF2CC78 : 0xFF8EDBD2);
            canvas.drawCircle(positionX, positionY, scaled(index % 3 + 1), paint);
            if (index % 23 == 0) {
                canvas.drawString("SYS " + index, positionX + scaled(6), positionY, font, paint);
            }
        }
        float movingX = (float) (logicalWidth * (0.5 + 0.35 * Math.sin(phase)));
        float movingY = (float) (scaled(190) + scaled(70) * Math.cos(phase * 0.7));
        paint.setColor(0xFFFF745E);
        canvas.drawCircle(movingX, movingY, scaled(12), paint);
        paint.setColor(0xFFE3EEE8);
        canvas.drawString(text("transit"), movingX + scaled(18), movingY, font, paint);
    }

    private void showPopup(MouseEvent event) {
        if (event.isPopupTrigger()) {
            openPopup(event.getX(), event.getY());
        }
    }

    private void openPopup(int positionX, int positionY) {
        popup = new JPopupMenu();
        popup.setLightWeightPopupEnabled(!heavyweightPopup.isSelected());
        JMenuItem inspect = new JMenuItem(text("inspect"));
        inspect.addActionListener(event -> showModal(false));
        popup.add(inspect);
        popup.add(new JMenuItem(text("coordinates")));
        popup.show(surface.getCanvas(), positionX, positionY);
    }

    private void showModal(boolean automatic) {
        JDialog dialog = new JDialog(frame, text("modal"), true);
        JPanel content = new JPanel(new FlowLayout());
        content.add(new JLabel(text("callsign")));
        content.add(new JTextField(18));
        content.add(button("close", dialog::dispose));
        dialog.add(content);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        if (automatic) {
            later(600, dialog::dispose);
        }
        dialog.setVisible(true);
    }

    private void updateStatus() {
        long now = System.nanoTime();
        double rate = (frames - previousStatusFrames) * 1_000_000_000.0 / (now - previousStatusTime);
        status.setText(MHQInternationalization.getFormattedTextAt(BUNDLE, "status", surface.getRenderApi(),
              surface.getContentScale(), Math.round(rate), generation, FAILURES.get()));
        System.out.printf("RENDER backend=%s scale=%.2f callbacks/s=%.1f callbackThread=%s maxGapMs=%.1f frames=%d%n",
              surface.getRenderApi(), surface.getContentScale(), rate, callbackThread,
              largestGap / 1_000_000.0, frames);
        previousStatusFrames = frames;
        previousStatusTime = now;
        largestGap = 0;
    }

    private void startCycles(boolean automatic) {
        if (cycleTimer != null && cycleTimer.isRunning()) {
            return;
        }
        frame.setAlwaysOnTop(automatic);
        frame.toFront();
        animate.setSelected(true);
        surfaceVisible.setSelected(true);
        overlayVisible.setSelected(true);
        overlay.setVisible(true);
        surface.setVisible(true);
        tabs.setSelectedIndex(0);
        int[] step = { 0 };
        long[] lastFrames = { frames };
        cycleTimer = new Timer(400, event -> {
            try {
                int stage = step[0]++ % 10;
                switch (stage) {
                    case 0 -> {
                        check(frames > lastFrames[0], "visible surface is producing frames");
                        lastFrames[0] = frames;
                        frame.setSize(scaled(960 + step[0] % 3 * 90), scaled(700));
                    }
                    case 1 -> openPopup(scaled(160), scaled(140));
                    case 2 -> {
                        capture("popup-" + step[0]);
                        popup.setVisible(false);
                        showModal(true);
                    }
                    case 3 -> tabs.setSelectedIndex(1);
                    case 4 -> tabs.setSelectedIndex(0);
                    case 5 -> scroll.getViewport().setViewPosition(new Point(0, scaled(260)));
                    case 6 -> {
                        capture("scroll-" + step[0]);
                        surface.setVisible(false);
                    }
                    case 7 -> {
                        surface.setVisible(true);
                        scroll.getViewport().setViewPosition(new Point());
                    }
                    case 8 -> recreateSurface();
                    case 9 -> {
                        check(frames > lastFrames[0], "animation resumes after lifecycle transitions");
                        capture("surface-" + step[0]);
                        heavyweightPopup.setSelected(!heavyweightPopup.isSelected());
                    }
                    default -> throw new IllegalStateException("Unknown stage");
                }
                if (step[0] >= 30) {
                    cycleTimer.stop();
                    System.out.println("CYCLES_COMPLETE failures=" + FAILURES.get());
                    if (automatic) {
                        close();
                        SkikoIntegrationStress reopened = new SkikoIntegrationStress();
                        reopened.frame.setAlwaysOnTop(true);
                        reopened.show();
                        later(1800, () -> {
                            try {
                                check(reopened.frames > 0, "new window renders after disposal");
                                reopened.capture("reopened");
                                System.out.println("SMOKE_COMPLETE failures=" + FAILURES.get());
                            } catch (Exception failure) {
                                recordFailure(failure);
                            } finally {
                                reopened.close();
                                System.exit(FAILURES.get() == 0 ? 0 : 1);
                            }
                        });
                    }
                }
            } catch (Exception failure) {
                recordFailure(failure);
                cycleTimer.stop();
                if (automatic) {
                    close();
                    System.exit(1);
                }
            }
        });
        cycleTimer.start();
    }

    private void capture(String name) throws Exception {
        Path output = Path.of("build", "skiko-stress");
        Files.createDirectories(output);
        Rectangle bounds = new Rectangle(frame.getLocationOnScreen(), frame.getSize());
        BufferedImage image = new Robot().createScreenCapture(bounds);
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
        int backgroundPixels = 0;
        int starPixels = 0;
        for (int positionY = 0; positionY < image.getHeight(); positionY += 3) {
            for (int positionX = 0; positionX < image.getWidth(); positionX += 3) {
                int pixel = image.getRGB(positionX, positionY);
                if (pixel == BACKGROUND) {
                    backgroundPixels++;
                } else if (pixel == 0xFFF2CC78 || pixel == 0xFF8EDBD2) {
                    starPixels++;
                }
            }
        }
        check(backgroundPixels > 1000 && starPixels > 20, "desktop capture must contain the rendered map: " + name);
        System.out.println("CAPTURE " + name + " background=" + backgroundPixels + " stars=" + starPixels);
    }

    private void show() {
        frame.setVisible(true);
        animationTimer.start();
        statusTimer.start();
        System.out.println("RENDER_INFO " + surface.getRenderInfo());
    }

    private void close() {
        if (closed) {
            return;
        }
        closed = true;
        animationTimer.stop();
        statusTimer.stop();
        if (cycleTimer != null) {
            cycleTimer.stop();
        }
        if (popup != null) {
            popup.setVisible(false);
        }
        releaseSurface();
        frame.dispose();
        System.out.println("WINDOW_DISPOSED generations=" + generation + " frames=" + frames);
    }

    private static JButton button(String key, Runnable action) {
        JButton button = new JButton(text(key));
        button.addActionListener(event -> action.run());
        return button;
    }

    private static String text(String key) {
        return MHQInternationalization.getTextAt(BUNDLE, key);
    }

    private static int scaled(int value) {
        return UIUtil.scaleForGUI(value);
    }

    private static void later(int delay, Runnable action) {
        Timer timer = new Timer(delay, event -> action.run());
        timer.setRepeats(false);
        timer.start();
    }

    private static void requireEdt() {
        check(SwingUtilities.isEventDispatchThread(), "callback and lifecycle are EDT-confined");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void recordFailure(Throwable failure) {
        FAILURES.incrementAndGet();
        failure.printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("The Skiko stress harness needs an interactive desktop");
        }
        boolean automatic = Arrays.asList(args).contains("--smoke");
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            recordFailure(failure);
            if (automatic) {
                System.exit(1);
            }
        });
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(() -> {
            SkikoIntegrationStress harness = new SkikoIntegrationStress();
            harness.show();
            if (automatic) {
                later(1500, () -> harness.startCycles(true));
            }
        });
    }
}
