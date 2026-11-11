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
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

import mekhq.gui.baseComponents.hud.HudButton;

/**
 * A HUD number stepper: a value between a minus and a plus button, as the Oracle Console steps its Chaos Factor. The
 * value stays between a minimum and maximum, and changes by a fixed step. The value can also be typed: Enter or leaving
 * the field applies it, Escape puts the shown value back.
 */
public final class HudStepper extends JPanel {
    /**
     * A whole number with optional grouping separators, each followed by exactly three digits ("1,500,000",
     * "1.500.000", "60%"), then an optional decimal part ("1.5", "1,5") that is rounded away.
     */
    private static final Pattern WHOLE_NUMBER = Pattern.compile(
          "(\\d+(?:[,.'\\s\\u00A0\\u202F]\\d{3}(?!\\d))*)(?:[.,](\\d+))?");
    /** A number that may have a decimal part, written with either a point or a comma: "1.5" or "1,5". */
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("(\\d*)(?:[.,](\\d+))?");

    private final int minimum;
    private final int maximum;
    private final IntFunction<String> format;
    private final IntConsumer onChange;
    private final HudButton less;
    private final HudButton more;
    private final JTextField valueField = new JTextField();
    private int value;
    private int typedValueDivisor = 1;
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
        buildValueField(width);
        add(less);
        add(Box.createHorizontalStrut(scaleForGUI(4)));
        add(valueField);
        add(Box.createHorizontalStrut(scaleForGUI(4)));
        add(more);
        setMaximumSize(getPreferredSize());
        setValue(minimum);
    }

    /**
     * Styles the value field and wires typing: Enter or leaving the field applies the typed value, Escape restores the
     * shown one, and entering the field selects its text so typing replaces it.
     *
     * @param width the field's width, before GUI scaling
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void buildValueField(int width) {
        valueField.setHorizontalAlignment(SwingConstants.CENTER);
        valueField.setForeground(TEXT);
        valueField.setDisabledTextColor(TEXT_FAINT);
        valueField.setCaretColor(TEXT);
        valueField.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        valueField.setOpaque(true);
        valueField.setBackground(SURFACE_DEEP);
        valueField.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(6), scaleForGUI(3), scaleForGUI(6))));
        Dimension size = new Dimension(scaleForGUI(width), valueField.getPreferredSize().height + scaleForGUI(8));
        valueField.setPreferredSize(size);
        valueField.setMinimumSize(size);
        valueField.setMaximumSize(size);
        valueField.addActionListener(event -> applyTypedValue());
        valueField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                SwingUtilities.invokeLater(valueField::selectAll);
            }

            @Override
            public void focusLost(FocusEvent event) {
                applyTypedValue();
            }
        });
        valueField.getInputMap(JComponent.WHEN_FOCUSED)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "hudStepperRevert");
        valueField.getActionMap().put("hudStepperRevert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showValue();
                valueField.selectAll();
            }
        });
    }

    /**
     * Sets how a typed number maps onto the stepper's value, for a stepper that shows its value divided down: a stepper
     * holding tenths and showing "1.5" for 15 takes a divisor of 10, so typing "1.5" sets 15. Whole-number steppers
     * keep the default of 1.
     *
     * @param typedValueDivisor what the shown value is divided by; at least 1
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setTypedValueDivisor(int typedValueDivisor) {
        this.typedValueDivisor = Math.max(1, typedValueDivisor);
    }

    /**
     * Applies whatever has been typed into the field, clamped to the stepper's range, and tells the change listener if
     * the value moved. Text that holds no number, or the shown text left untouched, just puts the shown value back.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void applyTypedValue() {
        String typed = valueField.getText();
        if (typed.equals(format.apply(value))) {
            return;
        }
        Long parsed = parseTyped(typed, typedValueDivisor);
        if (parsed == null) {
            showValue();
            return;
        }
        int next = Math.clamp(parsed, minimum, maximum);
        boolean moved = next != value;
        setValue(next);
        if (moved) {
            onChange.accept(next);
        }
    }

    /**
     * Reads the first number in typed text, ignoring units and symbols around it ("60%", "2,000 ly", "3d6"), and scales
     * it by the typed value divisor. With a divisor of 1, a point or comma counts as a grouping separator only when
     * exactly three digits follow it; otherwise it starts a decimal part, which is rounded ("1.5" is 2, not 15).
     *
     * @param typed             the field's text
     * @param typedValueDivisor how many stepper units make one typed unit; 1 for a whole-number stepper
     *
     * @return the stepper value the text asks for, or {@code null} when it holds no number
     *
     * @author Illiani
     * @since 0.51.01
     */
    static Long parseTyped(String typed, int typedValueDivisor) {
        try {
            if (typedValueDivisor == 1) {
                Matcher matcher = WHOLE_NUMBER.matcher(typed);
                if (!matcher.find()) {
                    return null;
                }
                String digits = matcher.group(1).replaceAll("\\D", "");
                // Longer than a long can hold: past any stepper's maximum anyway.
                if (digits.length() > 18) {
                    return Long.MAX_VALUE;
                }
                long wholeNumber = Long.parseLong(digits);
                String fractionPart = matcher.group(2);
                boolean roundsUp = (fractionPart != null) && (fractionPart.charAt(0) >= '5');
                return roundsUp ? (wholeNumber + 1) : wholeNumber;
            }
            Matcher matcher = DECIMAL_NUMBER.matcher(typed);
            while (matcher.find()) {
                String wholePart = matcher.group(1);
                String fractionPart = matcher.group(2);
                if (wholePart.isEmpty() && (fractionPart == null)) {
                    continue;
                }
                double number = Double.parseDouble((wholePart.isEmpty() ? "0" : wholePart)
                      + ((fractionPart == null) ? "" : ("." + fractionPart)));
                return Math.round(number * typedValueDivisor);
            }
            return null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void showValue() {
        valueField.setText(format.apply(value));
    }

    private void change(int delta) {
        // A value typed but not yet applied counts before the buttons step from it.
        applyTypedValue();
        int next = Math.clamp((long) value + delta, minimum, maximum);
        if (next != value) {
            setValue(next);
            onChange.accept(next);
        }
    }

    /** @param value the value to show, kept within the stepper's range; the change listener is not told */
    public void setValue(int value) {
        this.value = Math.clamp(value, minimum, maximum);
        showValue();
        refreshArmed();
    }

    public int getValue() {
        return value;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
        valueField.setEnabled(enabled);
        refreshArmed();
    }

    private void refreshArmed() {
        less.setArmed(enabled && (value > minimum));
        more.setArmed(enabled && (value < maximum));
    }
}
