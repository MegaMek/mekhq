/*
 * Copyright (c) 2019 Vicente Cartas Espinel (vicente.cartas at outlook.com). All rights reserved.
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import java.util.Objects;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import mekhq.campaign.finances.Money;

/**
 * Control used when a field in the UI represents an editable money amount.
 */
public class JMoneyTextField extends JFormattedTextField implements FocusListener {
    private final NumberFormat format;

    public JMoneyTextField() {
        this.format = NumberFormat.getInstance();
        this.addFocusListener(this);
        this.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(this.format)));
    }

    public Money getMoney() {
        try {
            return Money.of(format.parse(getText()).doubleValue());
        } catch (Exception ignored) {
            return Money.zero();
        }
    }

    public void setMoney(Money money) {
        setText(Objects.requireNonNull(money).toAmountString());
    }

    @Override
    public void focusGained(FocusEvent evt) {
        SwingUtilities.invokeLater(this::selectAll);
    }

    @Override
    public void focusLost(FocusEvent evt) {

    }
}
