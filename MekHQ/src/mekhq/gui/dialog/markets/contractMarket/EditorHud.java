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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javax.swing.*;

import jakarta.annotation.Nullable;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.gui.baseComponents.hud.Hud;

/**
 * The HUD controls the GM contract editor is built from, in the style of the contract market: pill toggles for
 * "automatic", "hidden from the player" and "non-negotiable", a 17-step track for a contract term, chips for the
 * one-per-category characteristics, a form row that marks itself when its value has changed, and stylers that bring
 * stock Swing inputs onto the HUD's dark surface.
 *
 * <p>The toggles and chips extend {@link JCheckBox}, so the editor reads and sets them with the familiar
 * {@code isSelected()} and {@code setSelected()} and listens to them like any check box.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class EditorHud {
    /** How many steps the Contract Steps Table runs to. */
    static final int STEP_COUNT = ChaosContractStepsTable.values().length;

    private EditorHud() {
    }

    // region Pill toggle

    /** What a {@link Pill} stands for, which sets its colours and the small glyph drawn before its text. */
    enum PillKind {
        /** On: the generator works the value out. Off: the GM sets it by hand. */
        AUTO,
        /** On: the employer has locked the term against negotiation. */
        LOCK,
        /** On: the intel is hidden from the player in the market. */
        HIDE
    }

    /**
     * A small two-state toggle drawn as a HUD pill, its label switching with its state (for example "Shown" and
     * "Hidden"). A disabled pill has a dashed outline and dim text, so it cannot be mistaken for one that is off.
     */
    static final class Pill extends JCheckBox {
        private final PillKind kind;
        private final String onText;
        private final String offText;
        private boolean hovered;

        Pill(PillKind kind, String onText, String offText, boolean selected) {
            super("", selected);
            this.kind = kind;
            this.onText = onText.toUpperCase(Locale.ROOT);
            this.offText = offText.toUpperCase(Locale.ROOT);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(hudFont(Font.BOLD, 0.66f, 0.12f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            int textWidth = Math.max(metrics.stringWidth(onText), metrics.stringWidth(offText));
            int glyph = (kind == PillKind.AUTO) ? 0 : scaleForGUI(14);
            return new Dimension(textWidth + glyph + scaleForGUI(16), metrics.getHeight() + scaleForGUI(8));
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int width = getWidth();
                int height = getHeight();
                boolean on = isSelected();
                Color accent = (kind == PillKind.AUTO) ? ACCENT_BRIGHT : AMBER;
                Color text;
                if (!isEnabled()) {
                    text = translucent(TEXT_FAINT, 150);
                    canvas.setColor(BORDER);
                    canvas.setStroke(dashed());
                } else {
                    canvas.setColor(on && kind == PillKind.AUTO ? SURFACE : SURFACE_DEEP);
                    canvas.fillRect(0, 0, width, height);
                    canvas.setColor(on ? (kind == PillKind.AUTO ? BORDER_CYAN : accent) : BORDER);
                    text = on ? accent : (hovered ? TEXT : TEXT_FAINT);
                }
                canvas.drawRect(0, 0, width - 1, height - 1);

                int x = scaleForGUI(8);
                int middle = height / 2;
                canvas.setColor(text);
                canvas.setStroke(new BasicStroke(scaleForGUI(1)));
                if (kind == PillKind.LOCK) {
                    drawLock(canvas, x, middle, on);
                    x += scaleForGUI(14);
                } else if (kind == PillKind.HIDE) {
                    drawEye(canvas, x, middle, on);
                    x += scaleForGUI(14);
                }
                canvas.setFont(getFont());
                FontMetrics metrics = canvas.getFontMetrics();
                canvas.drawString(on ? onText : offText, x,
                      middle + (metrics.getAscent() - metrics.getDescent()) / 2);
                if (isFocusOwner()) {
                    canvas.setColor(ACCENT_BRIGHT);
                    canvas.setStroke(dashed());
                    canvas.drawRect(1, 1, width - 3, height - 3);
                }
            } finally {
                canvas.dispose();
            }
        }

        private static void drawLock(Graphics2D canvas, int x, int middle, boolean locked) {
            int size = scaleForGUI(8);
            int top = middle - size / 2 + scaleForGUI(1);
            canvas.drawRect(x, top + size / 3, size, size * 2 / 3);
            // An open shackle reads as "negotiable", a closed one as "locked".
            int shackleX = locked ? x + scaleForGUI(2) : x + scaleForGUI(4);
            canvas.drawArc(shackleX, top - scaleForGUI(2), size - scaleForGUI(4), size * 2 / 3, 0, 180);
        }

        private static void drawEye(Graphics2D canvas, int x, int middle, boolean hidden) {
            int width = scaleForGUI(10);
            int height = scaleForGUI(6);
            canvas.drawOval(x, middle - height / 2, width, height);
            canvas.fillOval(x + width / 2 - scaleForGUI(2), middle - scaleForGUI(2), scaleForGUI(4), scaleForGUI(4));
            if (hidden) {
                int slash = height / 2 + scaleForGUI(1);
                canvas.drawLine(x, middle + slash, x + width, middle - slash);
            }
        }
    }

    // endregion Pill toggle

    // region Chip

    /**
     * One characteristic as a chip: tracked capitals with a hairline border, raised and drawn in its own colour when
     * chosen. The editor keeps one chosen per category.
     */
    static final class Chip extends JCheckBox {
        private final Color activeColor;
        private boolean hovered;

        Chip(String text, Color activeColor, boolean selected) {
            super(text.toUpperCase(Locale.ROOT), selected);
            this.activeColor = activeColor;
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(hudFont(Font.BOLD, 0.7f, 0.08f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            return new Dimension(metrics.stringWidth(getText()) + scaleForGUI(16),
                  metrics.getHeight() + scaleForGUI(8));
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean on = isSelected();
                canvas.setColor(on ? SURFACE : SURFACE_DEEP);
                canvas.fillRect(0, 0, getWidth(), getHeight());
                canvas.setColor(on ? activeColor : BORDER);
                canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                canvas.setFont(getFont());
                FontMetrics metrics = canvas.getFontMetrics();
                canvas.setColor(on ? activeColor : (hovered ? TEXT : TEXT_FAINT));
                canvas.drawString(getText(), scaleForGUI(8),
                      getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2);
                if (isFocusOwner()) {
                    canvas.setColor(ACCENT_BRIGHT);
                    canvas.setStroke(dashed());
                    canvas.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                }
            } finally {
                canvas.dispose();
            }
        }
    }

    // endregion Chip

    // region Step track

    /**
     * A contract term on the Contract Steps Table, drawn as a row of seventeen cells: the steps below the chosen one
     * filled, the chosen one lit. Click or drag to choose a step, or use the arrow keys. Hovering a cell shows its
     * effect, so the GM can read the table without leaving the editor.
     */
    static final class StepTrack extends JComponent {
        private final transient Function<ChaosContractStepsTable, String> describe;
        private final List<Runnable> listeners = new ArrayList<>();
        private ChaosContractStepsTable selected;

        /**
         * @param selected the step to start on
         * @param describe what a step does for this term, for the cells' tooltips
         */
        StepTrack(ChaosContractStepsTable selected, Function<ChaosContractStepsTable, String> describe) {
            this.selected = selected;
            this.describe = describe;
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("");
            MouseAdapter chooser = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    choose(stepAt(event.getX()));
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    choose(stepAt(event.getX()));
                }
            };
            addMouseListener(chooser);
            addMouseMotionListener(chooser);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    int index = selected.ordinal();
                    if (event.getKeyCode() == KeyEvent.VK_LEFT || event.getKeyCode() == KeyEvent.VK_DOWN) {
                        choose(ChaosContractStepsTable.values()[Math.max(0, index - 1)]);
                    } else if (event.getKeyCode() == KeyEvent.VK_RIGHT || event.getKeyCode() == KeyEvent.VK_UP) {
                        choose(ChaosContractStepsTable.values()[Math.min(STEP_COUNT - 1, index + 1)]);
                    }
                }
            });
        }

        ChaosContractStepsTable getSelected() {
            return selected;
        }

        /** Adds a listener told whenever the GM chooses a different step. */
        void addChangeListener(Runnable listener) {
            listeners.add(listener);
        }

        private void choose(@Nullable ChaosContractStepsTable step) {
            if (!isEnabled() || step == null || step == selected) {
                return;
            }
            selected = step;
            repaint();
            listeners.forEach(Runnable::run);
        }

        private @Nullable ChaosContractStepsTable stepAt(int x) {
            if (getWidth() <= 0) {
                return null;
            }
            int index = Math.clamp((long) Math.floor((double) x * STEP_COUNT / getWidth()), 0, STEP_COUNT - 1);
            return ChaosContractStepsTable.values()[index];
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            ChaosContractStepsTable step = stepAt(event.getX());
            return step == null ? null : describe.apply(step);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(scaleForGUI(STEP_COUNT * 13), scaleForGUI(18));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(scaleForGUI(STEP_COUNT * 6), scaleForGUI(18));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int gap = scaleForGUI(2);
            double cell = (getWidth() - gap * (STEP_COUNT - 1)) / (double) STEP_COUNT;
            int chosen = selected.ordinal();
            for (int index = 0; index < STEP_COUNT; index++) {
                int x = (int) Math.round(index * (cell + gap));
                int width = (int) Math.round((index + 1) * (cell + gap) - gap) - x;
                Color color;
                if (index == chosen) {
                    color = isEnabled() ? ACCENT : TEXT_FAINT;
                } else if (index < chosen) {
                    color = translucent(isEnabled() ? ACCENT : TEXT_FAINT, 97);
                } else {
                    color = SURFACE_HIGHLIGHT;
                }
                graphics.setColor(color);
                graphics.fillRect(x, 0, width, getHeight());
            }
            if (isFocusOwner()) {
                graphics.setColor(ACCENT_BRIGHT);
                graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        }
    }

    // endregion Step track

    // region Form row

    /**
     * One labelled field of the form: a faint label on the left, the control beside it, and an optional faint note
     * beneath (say, why the field cannot be changed). A row whose value differs from the one the editor opened with
     * draws an amber mark down its left edge.
     */
    static final class Row extends JPanel {
        private final JLabel noteLabel = new JLabel();
        private boolean changed;

        Row(String label, @Nullable String toolTip, JComponent control, @Nullable String note, int labelWidth) {
            super(new BorderLayout(scaleForGUI(10), scaleForGUI(2)));
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(9), scaleForGUI(5), 0)));

            JLabel labelComponent = new JLabel(label);
            labelComponent.setForeground(TEXT_FAINT);
            labelComponent.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            labelComponent.setVerticalAlignment(SwingConstants.TOP);
            labelComponent.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), 0, 0, 0));
            labelComponent.setPreferredSize(new Dimension(scaleForGUI(labelWidth),
                  labelComponent.getPreferredSize().height + scaleForGUI(5)));
            if (toolTip != null) {
                labelComponent.setToolTipText(toolTip);
            }
            add(labelComponent, BorderLayout.WEST);
            add(control, BorderLayout.CENTER);
            noteLabel.setForeground(TEXT_FAINT);
            noteLabel.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
            noteLabel.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(labelWidth + 10), 0, 0));
            add(noteLabel, BorderLayout.SOUTH);
            setNote(note);
        }

        /** Shows a faint note beneath the control, such as why it cannot be changed, or hides it when {@code null}. */
        void setNote(@Nullable String note) {
            noteLabel.setText(note == null ? "" : note);
            noteLabel.setVisible(note != null);
        }

        void setChanged(boolean changed) {
            if (this.changed != changed) {
                this.changed = changed;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (changed) {
                graphics.setColor(AMBER);
                graphics.fillRect(0, scaleForGUI(6), scaleForGUI(2), getHeight() - scaleForGUI(13));
            }
        }
    }

    /**
     * Lays a control out with pills (or other small controls) to its right, the control taking the spare width.
     *
     * @param control the main control, or {@code null} for a row of small controls only
     * @param extras  the pills and buttons to place after it
     */
    static JPanel line(@Nullable JComponent control, JComponent... extras) {
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
        if (control != null) {
            line.add(control, BorderLayout.CENTER);
        }
        if (extras.length > 0) {
            JPanel right = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(6), 0));
            for (JComponent extra : extras) {
                right.add(extra);
            }
            line.add(right, control == null ? BorderLayout.WEST : BorderLayout.EAST);
        }
        return line;
    }

    // endregion Form row

    // region Stylers

    /**
     * Styles a combo box for the HUD with a renderer that names each item through {@code labeler}, keeping any
     * per-item text the editor needs (an enum's player-facing label, "None" for an empty planet, and so on).
     */
    static <E> void styleCombo(JComboBox<E> combo, Function<Object, String> labeler) {
        combo.setBackground(GROUND);
        combo.setForeground(TEXT);
        combo.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        combo.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, labeler.apply(value), index, isSelected, cellHasFocus);
                setOpaque(true);
                setBackground(isSelected ? SURFACE : SURFACE_DEEP);
                setForeground(TEXT);
                setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(8), scaleForGUI(3),
                      scaleForGUI(8)));
                return this;
            }
        });
    }

    /** Styles a spinner's text box for the HUD; the arrow buttons keep the look and feel's own. */
    static void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        spinner.setBackground(GROUND);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JFormattedTextField field = editor.getTextField();
            field.setBackground(GROUND);
            field.setForeground(TEXT);
            field.setCaretColor(ACCENT);
            field.setDisabledTextColor(TEXT_FAINT);
            field.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
            field.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(6), scaleForGUI(3),
                  scaleForGUI(6)));
        }
    }

    /** Styles a text field for the HUD, with a faint colour for disabled text. */
    static void styleField(JTextField field) {
        Hud.styleField(field);
        field.setBackground(GROUND);
        field.setDisabledTextColor(TEXT_FAINT);
    }

    /** Styles a wrapping text area for the HUD, as the briefing's Markdown source. */
    static void styleTextArea(JTextArea area) {
        area.setBackground(GROUND);
        area.setForeground(TEXT);
        area.setCaretColor(ACCENT);
        area.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(6),
              scaleForGUI(8)));
    }

    /** Styles an icon button (camouflage, portrait) as a flat HUD tile. */
    static void styleIconButton(JButton button) {
        button.setBackground(SURFACE_DEEP);
        button.setForeground(TEXT_MUTED);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static BasicStroke dashed() {
        float dash = scaleForGUI(3);
        return new BasicStroke(scaleForGUI(1), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
              new float[] { dash, dash }, 0f);
    }

    // endregion Stylers
}
