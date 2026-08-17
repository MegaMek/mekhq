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
package mekhq.gui.baseComponents.immersiveDialogs;

import static megamek.client.ui.util.UIUtil.scaleForGUI;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

/** Shared visual language for immersive communication dialogs. */
final class ImmersiveDialogStyle {
    private static final String FLATLAF_STYLE_PROPERTY = "FlatLaf.style";
    private static final String TITLE_BAR_CAPTION_PROPERTY = "JComponent.titleBarCaption";
    private static final String WINDOW_BUTTONS_PLACEHOLDER_PROPERTY =
          "FlatLaf.fullWindowContent.buttonsPlaceholder";
    private static final Color DARK_THEME_SIGNAL = new Color(86, 208, 197);
    private static final Color LIGHT_THEME_SIGNAL = new Color(23, 112, 112);
    private static final Color DARK_THEME_INFORMATION = new Color(235, 177, 76);
    private static final Color LIGHT_THEME_INFORMATION = new Color(150, 92, 16);
    private static final int FRAME_PADDING = scaleForGUI(10);
    private static final int SECTION_GAP = scaleForGUI(6);
    private static final int CORNER_CUT = scaleForGUI(11);

    private ImmersiveDialogStyle() {
    }

    static JPanel createBackdropPanel() {
        JPanel panel = new TransmissionBackdropPanel();
        panel.setLayout(new BorderLayout(0, scaleForGUI(10)));
        return panel;
    }

    static JPanel createHeaderPanel(String title, String status, boolean useFullWindowContent) {
        JPanel panel = new TransmissionHeaderPanel();
        panel.setLayout(new BorderLayout(scaleForGUI(12), 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, scaleForGUI(2), 0, getSignalColor()));
        if (useFullWindowContent) {
            panel.putClientProperty(TITLE_BAR_CAPTION_PROPERTY, Boolean.TRUE);
        }

        JPanel contentPanel = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(9),
              scaleForGUI(12),
              scaleForGUI(9),
              useFullWindowContent ? 0 : scaleForGUI(12)));

        JLabel titleLabel = createTechnicalLabel(title, getSignalColor(), 2.0f);
        JLabel statusLabel = createTechnicalLabel("[ " + status + " ]", getSignalColor(), -1.0f);
        contentPanel.add(titleLabel, BorderLayout.WEST);
        contentPanel.add(statusLabel, BorderLayout.EAST);
        panel.add(contentPanel, BorderLayout.CENTER);

        if (useFullWindowContent) {
            JPanel windowButtonsPlaceholder = new JPanel();
            windowButtonsPlaceholder.setOpaque(false);
            windowButtonsPlaceholder.putClientProperty(WINDOW_BUTTONS_PLACEHOLDER_PROPERTY, "win horizontal");
            panel.add(windowButtonsPlaceholder, BorderLayout.EAST);
        }

        return panel;
    }

    static JPanel createAngularSurfacePanel() {
        JPanel panel = new AngularSurfacePanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(new AngularTransmissionBorder(),
              BorderFactory.createEmptyBorder(FRAME_PADDING, FRAME_PADDING, FRAME_PADDING, FRAME_PADDING)));
        return panel;
    }

    static JPanel createSourcePanel(String title, JPanel content) {
        JPanel panel = new JPanel(new BorderLayout(0, SECTION_GAP));
        panel.setOpaque(false);
        panel.add(createSectionHeader(title, getSignalColor()), BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    static JPanel createSectionHeader(String title, Color color) {
        JPanel panel = new JPanel(new BorderLayout(scaleForGUI(8), 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, SECTION_GAP, 0));

        JLabel label = createTechnicalLabel(title, color, -1.0f);
        JComponent separator = new CenteredRule(withAlpha(color, 150));
        panel.add(label, BorderLayout.WEST);
        panel.add(separator, BorderLayout.CENTER);
        return panel;
    }

    static JPanel createInformationPanel() {
        JPanel panel = new InformationPanel();
        panel.setLayout(new BorderLayout(0, scaleForGUI(3)));
        panel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(3), 0, 0, getInformationColor()),
              BorderFactory.createEmptyBorder(SECTION_GAP, scaleForGUI(10), SECTION_GAP, scaleForGUI(10))));
        return panel;
    }

    static Border createSectionSpacingBorder() {
          return BorderFactory.createEmptyBorder(SECTION_GAP, SECTION_GAP, SECTION_GAP, SECTION_GAP);
    }

    static void applyResponseButtonStyle(TransmissionResponseButton button) {
        button.applyFrameStyle();
    }

    static void applySupplementalControlStyle(Container container) {
        if (container.getLayout() instanceof FlowLayout flowLayout) {
            flowLayout.setAlignOnBaseline(true);
        }
        for (Component child : container.getComponents()) {
            if (child instanceof JSpinner || child instanceof JComboBox<?>) {
                Color signalColor = getSignalColor();
                Color subtleSignalColor = getSubtleSignalColor();
                Color interactiveSignalColor = getInteractiveSignalColor();
                ((JComponent) child).putClientProperty(FLATLAF_STYLE_PROPERTY, Map.of(
                      "focusColor", signalColor,
                      "focusedBorderColor", signalColor,
                      "buttonSeparatorColor", subtleSignalColor,
                      "buttonArrowColor", interactiveSignalColor,
                      "buttonHoverArrowColor", signalColor,
                      "buttonPressedArrowColor", signalColor));
            }
            if (child instanceof Container childContainer) {
                applySupplementalControlStyle(childContainer);
            }
        }
    }

    static Color getSignalColor() {
        return isDarkTheme() ? DARK_THEME_SIGNAL : LIGHT_THEME_SIGNAL;
    }

    static Color getInformationColor() {
        return isDarkTheme() ? DARK_THEME_INFORMATION : LIGHT_THEME_INFORMATION;
    }

    static ResponseButtonColors getResponseButtonColors() {
        Color panelColor = getPanelColor();
        Color surfaceColor = getSurfaceColor();
        Color signalColor = getSignalColor();
        Color labelColor = getLabelColor();
        return new ResponseButtonColors(
              new ResponseButtonStateColors(withAlpha(surfaceColor, 190),
                    mix(labelColor, signalColor, 0.18f),
                    getSubtleSignalColor()),
              new ResponseButtonStateColors(mix(surfaceColor, signalColor, 0.16f),
                    signalColor,
                    signalColor),
              new ResponseButtonStateColors(mix(surfaceColor, signalColor, 0.27f),
                    signalColor,
                    signalColor),
              new ResponseButtonStateColors(withAlpha(surfaceColor, 120),
                    mix(panelColor, labelColor, 0.36f),
                    mix(panelColor, signalColor, 0.20f)));
    }

    static ResponseButtonStateColors getTransmissionConfirmationColors() {
        return getResponseButtonColors().pressed();
    }

    private static JLabel createTechnicalLabel(String text, Color color, float sizeAdjustment) {
        JLabel label = new JLabel(text);
        Font baseFont = label.getFont();
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD,
              Math.max(1, Math.round(baseFont.getSize2D() + sizeAdjustment))));
        label.setForeground(color);
        return label;
    }

    private static Color getSurfaceColor() {
        return mix(getPanelColor(), getLabelColor(), isDarkTheme() ? 0.06f : 0.025f);
    }

    private static Color getSubtleSignalColor() {
        return mix(getPanelColor(), getSignalColor(), isDarkTheme() ? 0.42f : 0.30f);
    }

    private static Color getInteractiveSignalColor() {
        return mix(getLabelColor(), getSignalColor(), isDarkTheme() ? 0.62f : 0.72f);
    }

    private static Color getPanelColor() {
        Color color = UIManager.getColor("Panel.background");
        return (color == null) ? Color.DARK_GRAY : color;
    }

    private static Color getLabelColor() {
        Color color = UIManager.getColor("Label.foreground");
        return (color == null) ? Color.WHITE : color;
    }

    private static boolean isDarkTheme() {
        Color background = getPanelColor();
        double luminance = 0.2126 * background.getRed() +
                                 0.7152 * background.getGreen() +
                                 0.0722 * background.getBlue();
        return luminance < 128;
    }

    private static Color mix(Color firstColor, Color secondColor, float secondColorWeight) {
        float firstColorWeight = 1.0f - secondColorWeight;
        int red = Math.round(firstColor.getRed() * firstColorWeight + secondColor.getRed() * secondColorWeight);
        int green = Math.round(firstColor.getGreen() * firstColorWeight + secondColor.getGreen() * secondColorWeight);
        int blue = Math.round(firstColor.getBlue() * firstColorWeight + secondColor.getBlue() * secondColorWeight);
        return new Color(red, green, blue);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    record ResponseButtonStateColors(Color background, Color foreground, Color frame) {
    }

    record ResponseButtonColors(ResponseButtonStateColors idle, ResponseButtonStateColors active,
          ResponseButtonStateColors pressed, ResponseButtonStateColors disabled) {
    }

    private static Path2D createAngularFrame(float left, float top, float right, float bottom) {
        Path2D frame = new Path2D.Float();
        frame.moveTo(left, top);
        frame.lineTo(right - CORNER_CUT, top);
        frame.lineTo(right, top + CORNER_CUT);
        frame.lineTo(right, bottom);
        frame.lineTo(left + CORNER_CUT, bottom);
        frame.lineTo(left, bottom - CORNER_CUT);
        frame.closePath();
        return frame;
    }

    private static final class CenteredRule extends JComponent {
        private final Color color;

        private CenteredRule(Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setColor(color);
            int centerY = (getHeight() - 1) / 2;
            graphics2D.drawLine(0, centerY, getWidth(), centerY);
            graphics2D.dispose();
        }
    }

    private static final class AngularSurfacePanel extends JPanel {
        private AngularSurfacePanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(getSurfaceColor());
            graphics2D.fill(createAngularFrame(0, 0, getWidth(), getHeight()));
            graphics2D.dispose();
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.clip(createAngularFrame(0, 0, getWidth(), getHeight()));
            super.paintChildren(graphics2D);
            graphics2D.dispose();
        }
    }

    private static final class TransmissionBackdropPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setColor(withAlpha(getSignalColor(), isDarkTheme() ? 14 : 9));

            int gridSize = scaleForGUI(28);
            for (int x = 0; x < getWidth(); x += gridSize) {
                graphics2D.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += gridSize) {
                graphics2D.drawLine(0, y, getWidth(), y);
            }
            graphics2D.dispose();
        }
    }

    private static final class TransmissionHeaderPanel extends JPanel {
        private TransmissionHeaderPanel() {
            setBackground(getSurfaceColor());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setColor(withAlpha(getSignalColor(), isDarkTheme() ? 22 : 14));

            int stripeGap = scaleForGUI(18);
            int stripeStart = Math.max(0, getWidth() - scaleForGUI(220));
            for (int x = stripeStart; x < getWidth(); x += stripeGap) {
                graphics2D.drawLine(x, 0, x + scaleForGUI(30), getHeight());
            }
            graphics2D.dispose();
        }
    }

    private static final class InformationPanel extends JPanel {
        private InformationPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setColor(withAlpha(getInformationColor(), isDarkTheme() ? 18 : 12));
            graphics2D.fillRect(0, 0, getWidth(), getHeight());
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class AngularTransmissionBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component component) {
            int inset = scaleForGUI(1);
            return new Insets(inset, inset, inset, inset);
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int xPosition, int yPosition, int width,
              int height) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setStroke(new BasicStroke(scaleForGUI(1)));
            graphics2D.setColor(getSubtleSignalColor());

            float left = xPosition + 0.5f;
            float top = yPosition + 0.5f;
            float right = xPosition + width - 1.5f;
            float bottom = yPosition + height - 1.5f;
            Path2D frame = createAngularFrame(left, top, right, bottom);
            graphics2D.draw(frame);
            graphics2D.dispose();
        }
    }

}
