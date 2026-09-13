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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;

import megamek.client.ui.util.UIUtil;

public class ImmersiveComboBox<E> extends JComboBox<E> {
    public ImmersiveComboBox(E[] items) {
        super(items);
        setUI(new ImmersiveComboBoxUI());
        setRenderer(new ImmersiveComboBoxRenderer(this));
        setBackground(ImmersiveControlStyle.INPUT_BACKGROUND);
        setForeground(ImmersiveControlStyle.TEXT);
        setBorder(BorderFactory.createLineBorder(ImmersiveControlStyle.BORDER));
        normalizeHeight();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                setBorder(BorderFactory.createLineBorder(ImmersiveControlStyle.ACCENT));
            }

            @Override
            public void focusLost(FocusEvent event) {
                setBorder(BorderFactory.createLineBorder(ImmersiveControlStyle.BORDER));
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setBackground(enabled
              ? ImmersiveControlStyle.INPUT_BACKGROUND : ImmersiveControlStyle.DISABLED_BACKGROUND);
        repaint();
    }

    private void normalizeHeight() {
        Dimension preferredSize = getPreferredSize();
        Dimension controlSize = new Dimension(preferredSize.width, ImmersiveControlStyle.CONTROL_HEIGHT);
        setPreferredSize(controlSize);
        setMinimumSize(controlSize);
    }

    private static final class ImmersiveComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            return new ImmersiveControlStyle.ArrowButton(false);
        }

        @Override
        public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) {
            graphics.setColor(comboBox.isEnabled()
                  ? ImmersiveControlStyle.INPUT_BACKGROUND : ImmersiveControlStyle.DISABLED_BACKGROUND);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private static final class ImmersiveComboBoxRenderer extends DefaultListCellRenderer {
        private final JComboBox<?> owner;

        private ImmersiveComboBoxRenderer(JComboBox<?> owner) {
            this.owner = owner;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                  isSelected, cellHasFocus);
            boolean enabled = owner.isEnabled();
            boolean popupSelection = index >= 0 && isSelected;
            label.setOpaque(index >= 0);
            label.setBackground(enabled
                ? popupSelection ? ImmersiveControlStyle.ACTIVE_BACKGROUND : ImmersiveControlStyle.INPUT_BACKGROUND
                  : ImmersiveControlStyle.DISABLED_BACKGROUND);
            label.setForeground(enabled
                ? popupSelection ? ImmersiveControlStyle.ACCENT : ImmersiveControlStyle.TEXT
                  : ImmersiveControlStyle.MUTED_TEXT);
            label.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(10),
                  UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(10)));
            return label;
        }
    }
}
