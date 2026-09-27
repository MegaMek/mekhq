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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.AskPage.column;
import static mekhq.gui.roleplay.AskPage.heading;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.gui.roleplay.OracleConsole.escape;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mekhq.campaign.roleplay.DiceExpression;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudChip;

/**
 * The console's generic dice roller: an expression field, quick-roll chips, and the last result. Each page that
 * shows it gets its own instance; every roll is logged to the journal.
 */
class DiceRoller {
    private static final List<String> QUICK_DICE = List.of("d6", "2d6", "d10", "d20", "d100");

    private final OracleConsole console;
    private final JTextField diceField = new JTextField();
    private final JLabel diceResult = new JLabel();
    private final JPanel panel = column();

    DiceRoller(final OracleConsole console) {
        this.console = console;
        panel.add(heading("OracleConsole.dice", "OracleConsole.dice.help"));
        panel.add(Box.createVerticalStrut(scaleForGUI(6)));
        panel.add(leftAligned(buildDiceRow()));
        panel.add(Box.createVerticalStrut(scaleForGUI(6)));
        panel.add(leftAligned(buildQuickDice()));
        panel.add(Box.createVerticalStrut(scaleForGUI(8)));
        panel.add(leftAligned(diceResult));
    }

    /**
     * @return the dice section, heading included
     */
    JPanel getPanel() {
        return panel;
    }

    private JPanel buildDiceRow() {
        Hud.styleField(diceField);
        diceField.setText("2d6");
        diceField.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.field.toolTipText"));
        diceField.addActionListener(event -> rollDice(diceField.getText()));
        HudButton roll = new HudButton(getTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.roll").toUpperCase(Locale.ROOT),
              true, true);
        roll.addActionListener(event -> rollDice(diceField.getText()));
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
        row.add(diceField, BorderLayout.CENTER);
        row.add(roll, BorderLayout.EAST);
        diceResult.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        diceResult.setVisible(false);
        return row;
    }

    private JPanel buildQuickDice() {
        JPanel row = Hud.transparentPanel(null);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        for (String dice : QUICK_DICE) {
            if (row.getComponentCount() > 0) {
                row.add(Box.createHorizontalStrut(scaleForGUI(4)));
            }
            HudChip chip = new HudChip(dice, () -> {
                diceField.setText(dice);
                rollDice(dice);
            });
            chip.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.quick.toolTipText", dice));
            row.add(chip);
        }
        return row;
    }

    private void rollDice(final String expression) {
        diceResult.setVisible(true);
        if (!DiceExpression.isValid(expression)) {
            diceResult.setForeground(DANGER);
            diceResult.setText(wrappedHtml(getTextAt(RESOURCE_BUNDLE, "OracleConsole.dice.invalid")));
            return;
        }
        DiceExpression.Roll roll = console.actions().rollDice(expression);
        diceResult.setForeground(TEXT_MUTED);
        diceResult.setText("<html><b><font color='" + hex(ACCENT_BRIGHT) + "' size='+2'>" + roll.total()
                                 + "</font></b>&nbsp;&nbsp;" + escape(OracleConsole.joined(roll.expression(), roll.describeWorking())) + "</html>");
        console.changed();
    }

    private static String wrappedHtml(final String text) {
        return "<html><div style='width:" + scaleForGUI(300) + "px'>" + escape(text) + "</div></html>";
    }

}
