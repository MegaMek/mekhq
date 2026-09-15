package mekhq.gui.stratCon.deployment;

import static mekhq.gui.stratCon.deployment.HudStyle.ACCENT;
import static mekhq.gui.stratCon.deployment.HudStyle.BORDER;
import static mekhq.gui.stratCon.deployment.HudStyle.SURFACE;
import static mekhq.gui.stratCon.deployment.HudStyle.SURFACE_DEEP;
import static mekhq.gui.stratCon.deployment.HudStyle.SURFACE_HIGHLIGHT;
import static mekhq.gui.stratCon.deployment.HudStyle.TEXT;
import static mekhq.gui.stratCon.deployment.HudStyle.TEXT_FAINT;
import static mekhq.gui.stratCon.deployment.HudStyle.TEXT_MUTED;
import static mekhq.gui.stratCon.deployment.HudStyle.hudFont;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentMode;

/**
 * The wizard's segmented mode selector: one cell per {@link DeploymentMode}, with the interstellar-map tab's glowing
 * accent underline on the selected cell. Choosing a cell notifies the wizard, which switches pages.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudModeSelector extends JPanel {
    private final transient Map<DeploymentMode, Cell> cells = new EnumMap<>(DeploymentMode.class);
    private final transient Consumer<DeploymentMode> onSelect;

    public HudModeSelector(Consumer<DeploymentMode> onSelect) {
        this.onSelect = onSelect;

        DeploymentMode[] modes = DeploymentMode.values();
        setLayout(new GridLayout(1, modes.length, 0, 0));
        setOpaque(true);
        setBackground(SURFACE_DEEP);
        setBorder(BorderFactory.createLineBorder(BORDER, UIUtil.scaleForGUI(1)));

        for (int index = 0; index < modes.length; index++) {
            Cell cell = new Cell(modes[index], index < modes.length - 1);
            cells.put(modes[index], cell);
            add(cell);
        }
    }

    /** Highlights the given mode's cell without firing the selection callback. */
    public void setSelected(DeploymentMode mode) {
        for (Map.Entry<DeploymentMode, Cell> entry : cells.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == mode);
        }
    }

    /**
     * Locks or unlocks a mode's cell. A locked cell is dimmed and ignores clicks and keys, so the page cannot be
     * selected.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setModeEnabled(DeploymentMode mode, boolean enabled) {
        Cell cell = cells.get(mode);
        if (cell != null) {
            cell.setCellEnabled(enabled);
        }
    }

    private final class Cell extends JPanel {
        private final transient DeploymentMode mode;
        private final boolean rightBorder;
        private final JLabel title;
        private boolean selected;
        private boolean hovered;
        private boolean cellEnabled = true;

        private Cell(DeploymentMode mode, boolean rightBorder) {
            this.mode = mode;
            this.rightBorder = rightBorder;
            setOpaque(false);
            setFocusable(true);
            setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10), UIUtil.scaleForGUI(6),
                  UIUtil.scaleForGUI(10), UIUtil.scaleForGUI(6)));

            title = new JLabel(mode.getLabel(), SwingConstants.CENTER);
            title.setForeground(TEXT_MUTED);
            title.setFont(hudFont(Font.BOLD, 0.85f, 0.06f));
            setLayout(new java.awt.GridBagLayout());
            add(title);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        choose();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    if (!cellEnabled) {
                        return;
                    }
                    hovered = true;
                    refreshTitleColor();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    refreshTitleColor();
                    repaint();
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    if ((event.getKeyCode() == KeyEvent.VK_SPACE) || (event.getKeyCode() == KeyEvent.VK_ENTER)) {
                        choose();
                    }
                }
            });
        }

        private void choose() {
            if (!cellEnabled) {
                return;
            }
            requestFocusInWindow();
            onSelect.accept(mode);
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
            refreshTitleColor();
            repaint();
        }

        private void setCellEnabled(boolean cellEnabled) {
            this.cellEnabled = cellEnabled;
            if (!cellEnabled) {
                hovered = false;
            }
            refreshTitleColor();
            repaint();
        }

        private void refreshTitleColor() {
            if (!cellEnabled) {
                title.setForeground(TEXT_FAINT);
                return;
            }
            title.setForeground((selected || hovered) ? TEXT : TEXT_MUTED);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                g2.setColor(selected ? SURFACE : (hovered ? SURFACE_HIGHLIGHT : SURFACE_DEEP));
                g2.fillRect(0, 0, width, height);

                if (rightBorder) {
                    g2.setColor(BORDER);
                    g2.fillRect(width - UIUtil.scaleForGUI(1), 0, UIUtil.scaleForGUI(1), height);
                }

                if (selected) {
                    int underline = UIUtil.scaleForGUI(2);
                    g2.setColor(ACCENT);
                    g2.fillRect(0, height - underline, width, underline);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
