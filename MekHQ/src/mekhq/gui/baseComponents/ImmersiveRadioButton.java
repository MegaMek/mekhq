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
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;
import javax.swing.JRadioButton;

import megamek.client.ui.util.UIUtil;

public class ImmersiveRadioButton extends JRadioButton {
    private static final Icon RADIO_BUTTON_ICON = new RadioButtonIcon();

    public ImmersiveRadioButton(String text) {
        super(text);
        setOpaque(false);
        setForeground(ImmersiveControlStyle.TEXT);
        setFont(getFont().deriveFont(Font.PLAIN));
        setFocusPainted(false);
        setIcon(RADIO_BUTTON_ICON);
        setSelectedIcon(RADIO_BUTTON_ICON);
        setDisabledIcon(RADIO_BUTTON_ICON);
        setDisabledSelectedIcon(RADIO_BUTTON_ICON);
        setRolloverIcon(RADIO_BUTTON_ICON);
        setRolloverSelectedIcon(RADIO_BUTTON_ICON);
        setRolloverEnabled(true);
        setIconTextGap(UIUtil.scaleForGUI(8));
    }

    private static final class RadioButtonIcon implements Icon {
        private final int size = UIUtil.scaleForGUI(18);

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            JRadioButton radioButton = (JRadioButton) component;
            boolean enabled = radioButton.isEnabled();
            boolean selected = radioButton.isSelected();
            boolean highlighted = radioButton.getModel().isRollover() || radioButton.hasFocus();

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.translate(x, y);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setComposite(AlphaComposite.SrcOver.derive(enabled ? 1.0f : 0.45f));
            graphics2D.setColor(ImmersiveControlStyle.CONTROL_BACKGROUND);
            Ellipse2D.Double outerCircle = new Ellipse2D.Double(1, 1, size - 2, size - 2);
            graphics2D.fill(outerCircle);
            graphics2D.setColor(highlighted ? ImmersiveControlStyle.ACCENT : ImmersiveControlStyle.BORDER);
            graphics2D.setStroke(new BasicStroke(UIUtil.scaleForGUI(1.5f)));
            graphics2D.draw(outerCircle);
            if (selected) {
                double inset = UIUtil.scaleForGUI(5);
                graphics2D.setColor(ImmersiveControlStyle.SELECTED);
                graphics2D.fill(new Ellipse2D.Double(inset, inset, size - (inset * 2), size - (inset * 2)));
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
