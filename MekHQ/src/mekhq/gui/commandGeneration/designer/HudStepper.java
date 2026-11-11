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
package mekhq.gui.commandGeneration.designer;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.Dimension;
import java.awt.Font;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mekhq.gui.baseComponents.hud.HudButton;

/**
 * A HUD number stepper: a value between a minus and a plus button, as the Oracle Console steps its Chaos Factor. The
 * value stays between a minimum and maximum, and changes by a fixed step.
 */
public final class HudStepper extends JPanel {
    private final int minimum;
    private final int maximum;
    private final IntFunction<String> format;
    private final IntConsumer onChange;
    private final HudButton less;
    private final HudButton more;
    private final JLabel valueLabel = new JLabel("", SwingConstants.CENTER);
    private int value;
    private boolean enabled = true;

    /**
     * @param minimum  the smallest value
     * @param maximum  the largest value
     * @param step     how far each press moves the value
     * @param format   the text shown for a value
     * @param width    the value's width, before GUI scaling
     * @param onChange told the new value when the player changes it, but not when it is set
     */
    public HudStepper(int minimum, int maximum, int step, IntFunction<String> format, int width,
          IntConsumer onChange) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.format = format;
        this.onChange = onChange;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        less = new HudButton(DesignerUi.text("Designer.symbol.minus"), false, true);
        more = new HudButton(DesignerUi.text("Designer.symbol.plus"), false, true);
        less.addActionListener(event -> change(-step));
        more.addActionListener(event -> change(step));
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        valueLabel.setOpaque(true);
        valueLabel.setBackground(SURFACE_DEEP);
        valueLabel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(6), scaleForGUI(3), scaleForGUI(6))));
        Dimension size = new Dimension(scaleForGUI(width), valueLabel.getPreferredSize().height + scaleForGUI(8));
        valueLabel.setPreferredSize(size);
        valueLabel.setMinimumSize(size);
        valueLabel.setMaximumSize(size);
        add(less);
        add(Box.createHorizontalStrut(scaleForGUI(4)));
        add(valueLabel);
        add(Box.createHorizontalStrut(scaleForGUI(4)));
        add(more);
        setMaximumSize(getPreferredSize());
        setValue(minimum);
    }

    private void change(int delta) {
        int next = Math.clamp((long) value + delta, minimum, maximum);
        if (next != value) {
            setValue(next);
            onChange.accept(next);
        }
    }

    /** @param value the value to show, kept within the stepper's range; the change listener is not told */
    public void setValue(int value) {
        this.value = Math.clamp(value, minimum, maximum);
        valueLabel.setText(format.apply(this.value));
        refreshArmed();
    }

    public int getValue() {
        return value;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
        valueLabel.setForeground(enabled ? TEXT : TEXT_FAINT);
        refreshArmed();
    }

    private void refreshArmed() {
        less.setArmed(enabled && (value > minimum));
        more.setArmed(enabled && (value < maximum));
    }
}
