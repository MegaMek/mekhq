/*
 * JMoneyTextField.java
 *
 * Copyright (c) 2019 Vicente Cartas Espinel <vicente.cartas at outlook.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.utilities;

import mekhq.campaign.finances.Money;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.text.NumberFormat;

/*
 * Control used when a field in the UI represents an editable money amount.
 */
public class JMoneyTextField extends JFormattedTextField implements FocusListener {
    private NumberFormat format;
    private mekhq.Action whenEnterPressed;

    public JMoneyTextField() {
        this.format = NumberFormat.getInstance();
        this.addFocusListener(this);
        this.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(this.format)));
    }

    public JMoneyTextField(mekhq.Action whenEnterPressed) {
        this();

        assert whenEnterPressed != null;

        this.whenEnterPressed = whenEnterPressed;
        this.addActionListener(x -> whenEnterPressed.invoke());
    }

    public Money getMoney() {
        try {
            return Money.of(this.format.parse(this.getText()).doubleValue());
        } catch (Exception ignored) {
            return Money.zero();
        }
    }

    public void setMoney(Money money) {
        assert money != null;
        this.setText(money.toAmountString());
    }

    @Override
    public void focusGained(FocusEvent e) {
        SwingUtilities.invokeLater(() -> this.selectAll());
    }

    @Override
    public void focusLost(FocusEvent e) {
        // Not used
    }
}
