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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicSpinnerUI;

import megamek.client.ui.util.UIUtil;

public class ImmersiveSpinner extends JSpinner {
    public ImmersiveSpinner(SpinnerModel model) {
        super(model);
        setUI(new ImmersiveSpinnerUI());
        setBorder(BorderFactory.createLineBorder(ImmersiveControlStyle.BORDER));
        styleEditor(getEditor());
        normalizeHeight();
    }

    @Override
    public void setEditor(JComponent editor) {
        super.setEditor(editor);
        styleEditor(editor);
        normalizeHeight();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateEditorColors();
    }

    private void styleEditor(JComponent editor) {
        if (!(editor instanceof DefaultEditor defaultEditor)) {
            return;
        }
        JFormattedTextField textField = defaultEditor.getTextField();
        textField.setOpaque(true);
        textField.setCaretColor(ImmersiveControlStyle.ACCENT);
        textField.setHorizontalAlignment(SwingConstants.TRAILING);
        textField.setBorder(BorderFactory.createEmptyBorder(0, UIUtil.scaleForGUI(8), 0,
              UIUtil.scaleForGUI(8)));
        updateEditorColors();
        textField.addFocusListener(new FocusAdapter() {
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

    private void updateEditorColors() {
        if (getEditor() instanceof DefaultEditor editor) {
            JFormattedTextField textField = editor.getTextField();
            textField.setBackground(isEnabled()
                  ? ImmersiveControlStyle.INPUT_BACKGROUND : ImmersiveControlStyle.DISABLED_BACKGROUND);
            textField.setForeground(isEnabled()
                  ? ImmersiveControlStyle.TEXT : ImmersiveControlStyle.MUTED_TEXT);
            textField.setDisabledTextColor(ImmersiveControlStyle.MUTED_TEXT);
        }
    }

    private void normalizeHeight() {
        Dimension preferredSize = getPreferredSize();
        Dimension controlSize = new Dimension(preferredSize.width, ImmersiveControlStyle.CONTROL_HEIGHT);
        setPreferredSize(controlSize);
        setMinimumSize(controlSize);
    }

    private static final class ImmersiveSpinnerUI extends BasicSpinnerUI {
        @Override
        protected Component createNextButton() {
            JButton button = new ImmersiveControlStyle.ArrowButton(true, true);
            installNextButtonListeners(button);
            return button;
        }

        @Override
        protected Component createPreviousButton() {
            JButton button = new ImmersiveControlStyle.ArrowButton(false, true);
            installPreviousButtonListeners(button);
            return button;
        }
    }
}
