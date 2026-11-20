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
package mekhq.gui.baseComponents.hud;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * A label for plain text that wraps to the width its layout gives it and grows tall enough for every line, like
 * {@link HudWrappingLabel}, but keeps the font's own attributes. HTML text drops attributes such as letter spacing
 * ({@link java.awt.font.TextAttribute#TRACKING}), which the HUD's eyebrows and titles use, so those wrap with this
 * instead. Text stays on one line while it fits, breaks between words when it does not, and breaks inside a word only
 * when the word alone is too wide. A leading icon sits beside the first line, and the lines wrap beside it.
 *
 * <p>Its preferred width never exceeds the width it was last given, and it can shrink to nothing, so it never widens
 * the row it sits in. In a vertical {@code BoxLayout} it stretches to the column's width, so it grows again when the
 * window widens.</p>
 *
 * <p>Give it plain text; HTML is drawn as written.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudWrappingTextLabel extends JLabel {
    /**
     * @param text the label's plain text
     */
    public HudWrappingTextLabel(String text) {
        super(text);
    }

    /**
     * A wrapping copy of a one-line label: its text, font, colour, icon and tooltip.
     *
     * @param label the label to copy, such as one from {@link Hud#eyebrow(String)}
     *
     * @return the wrapping label
     */
    public static HudWrappingTextLabel copyOf(JLabel label) {
        HudWrappingTextLabel copy = new HudWrappingTextLabel(label.getText());
        copy.setFont(label.getFont());
        copy.setForeground(label.getForeground());
        copy.setIcon(label.getIcon());
        copy.setIconTextGap(label.getIconTextGap());
        copy.setToolTipText(label.getToolTipText());
        copy.setAlignmentX(label.getAlignmentX());
        return copy;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int lead = leadWidth();
        int natural = insets.left + insets.right + lead + (int) Math.ceil(naturalTextWidth());
        int width = getWidth();
        int shown = (width <= 0 || natural <= width) ? natural : width;
        int textWidth = Math.max(1, shown - insets.left - insets.right - lead);
        return new Dimension(shown, insets.top + insets.bottom + contentHeight(textWidth));
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return new Dimension(0, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean resized = width != getWidth();
        super.setBounds(x, y, width, height);
        if (resized) {
            // Its height depends on its width, so lay out again now the width is known.
            SwingUtilities.invokeLater(this::revalidate);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (isOpaque()) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (hints instanceof Map<?, ?> desktopHints) {
                graphics2D.addRenderingHints(desktopHints);
            } else {
                graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }

            Insets insets = getInsets();
            int lead = leadWidth();
            int textWidth = Math.max(1, getWidth() - insets.left - insets.right - lead);
            List<TextLayout> lines = lines(textWidth, graphics2D.getFontRenderContext());

            Icon icon = getIcon();
            if (icon != null) {
                int lineHeight = lines.isEmpty() ? icon.getIconHeight() : (int) Math.ceil(lineHeight(lines.get(0)));
                int iconY = insets.top + Math.max(0, (lineHeight - icon.getIconHeight()) / 2);
                icon.paintIcon(this, graphics2D, insets.left, iconY);
            }

            graphics2D.setColor(isEnabled() ? getForeground() : getBackground().darker());
            float x = insets.left + lead;
            float y = insets.top;
            for (TextLayout line : lines) {
                y += line.getAscent();
                line.draw(graphics2D, x, y);
                y += line.getDescent() + line.getLeading();
            }
        } finally {
            graphics2D.dispose();
        }
    }

    /** @return the width the icon and its gap take before the text, or 0 without an icon */
    private int leadWidth() {
        Icon icon = getIcon();
        return icon == null ? 0 : icon.getIconWidth() + getIconTextGap();
    }

    /** @return the text's width on a single line */
    private float naturalTextWidth() {
        String text = getText();
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return new TextLayout(attributed(text).getIterator(), measuringContext()).getAdvance();
    }

    /** @return the height of the text wrapped to a width, or of the icon if that is taller than one line */
    private int contentHeight(int textWidth) {
        List<TextLayout> lines = lines(textWidth, measuringContext());
        float height = 0;
        for (TextLayout line : lines) {
            height += lineHeight(line);
        }
        int textHeight = lines.isEmpty() ? getFontMetrics(getFont()).getHeight() : (int) Math.ceil(height);
        Icon icon = getIcon();
        return icon == null ? textHeight : Math.max(textHeight, icon.getIconHeight());
    }

    /** @return the text broken into lines that each fit the width */
    private List<TextLayout> lines(int textWidth, FontRenderContext context) {
        List<TextLayout> lines = new ArrayList<>();
        String text = getText();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        LineBreakMeasurer measurer = new LineBreakMeasurer(attributed(text).getIterator(), context);
        while (measurer.getPosition() < text.length()) {
            lines.add(measurer.nextLayout(textWidth));
        }
        return lines;
    }

    private AttributedString attributed(String text) {
        return new AttributedString(text, getFont().getAttributes());
    }

    private FontRenderContext measuringContext() {
        return getFontMetrics(getFont()).getFontRenderContext();
    }

    private static float lineHeight(TextLayout line) {
        return line.getAscent() + line.getDescent() + line.getLeading();
    }
}
