/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */
package mekhq.gui.baseComponents;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JCheckBox;

import megamek.client.ui.util.UIUtil;

public class ImmersiveCheckBox extends JCheckBox {
    private static final Icon CHECK_BOX_ICON = new CheckBoxIcon();

    public ImmersiveCheckBox(String text) {
        this(text, false);
    }

    public ImmersiveCheckBox(String text, boolean selected) {
        super(text, selected);
        setOpaque(false);
        setForeground(ImmersiveControlStyle.TEXT);
        setFont(getFont().deriveFont(Font.PLAIN));
        setFocusPainted(false);
        setIcon(CHECK_BOX_ICON);
        setSelectedIcon(CHECK_BOX_ICON);
        setDisabledIcon(CHECK_BOX_ICON);
        setDisabledSelectedIcon(CHECK_BOX_ICON);
        setRolloverIcon(CHECK_BOX_ICON);
        setRolloverSelectedIcon(CHECK_BOX_ICON);
        setRolloverEnabled(true);
        setIconTextGap(UIUtil.scaleForGUI(8));
    }

    private static final class CheckBoxIcon implements Icon {
        private final int size = UIUtil.scaleForGUI(18);

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            JCheckBox checkBox = (JCheckBox) component;
            boolean enabled = checkBox.isEnabled();
            boolean selected = checkBox.isSelected();
            boolean highlighted = checkBox.getModel().isRollover() || checkBox.hasFocus();

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.translate(x, y);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setComposite(AlphaComposite.SrcOver.derive(enabled ? 1.0f : 0.45f));
            graphics2D.setColor(selected
                  ? ImmersiveControlStyle.SELECTED : ImmersiveControlStyle.CONTROL_BACKGROUND);
            graphics2D.fillRect(1, 1, size - 2, size - 2);
            graphics2D.setColor(selected
                  ? ImmersiveControlStyle.SELECTED.brighter()
                  : highlighted ? ImmersiveControlStyle.ACCENT : ImmersiveControlStyle.BORDER);
            graphics2D.setStroke(new BasicStroke(UIUtil.scaleForGUI(1.5f)));
            graphics2D.drawRect(1, 1, size - 3, size - 3);

            if (selected) {
                graphics2D.setColor(ImmersiveControlStyle.CONTROL_BACKGROUND);
                graphics2D.setStroke(new BasicStroke(UIUtil.scaleForGUI(2.2f),
                      BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics2D.drawLine(size / 4, size / 2, size * 2 / 5, size * 2 / 3);
                graphics2D.drawLine(size * 2 / 5, size * 2 / 3, size * 3 / 4, size / 3);
            }
            graphics2D.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
