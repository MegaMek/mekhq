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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.gui.roleplay.AskPage.column;
import static mekhq.gui.roleplay.AskPage.rightButtonRow;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.gui.roleplay.OracleConsole.escape;
import static mekhq.gui.roleplay.OracleConsole.formatDate;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;

/**
 * A small prompt, shown when a contract ends, offering to open the Oracle Console's journal on a new note describing
 * the contract. A "don't show again" check box sets the {@link MHQConstants#NAG_CONTRACT_JOURNAL_PROMPT} client option,
 * which can be turned back on from the Reminders page of the MekHQ Options.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractJournalPrompt extends JDialog {
    private final HudCheckBox dontShowAgain;
    private boolean writeEntry;

    /**
     * Asks the player whether they want to write a journal entry for a contract that has just ended, and opens the
     * journal on a new note if they do. Does nothing if the player has hidden the prompt.
     *
     * @param frame    the MekHQ main window
     * @param campaign the campaign
     * @param contract the contract that ended
     * @param status   how the contract ended
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void offer(final JFrame frame, final Campaign campaign, final AbstractContract contract,
          final MissionStatus status) {
        if (MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_CONTRACT_JOURNAL_PROMPT)) {
            return;
        }

        ContractJournalPrompt prompt = new ContractJournalPrompt(frame, contract, status);
        prompt.setVisible(true);

        if (prompt.dontShowAgain.isSelected()) {
            MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_CONTRACT_JOURNAL_PROMPT, true);
        }
        if (prompt.writeEntry) {
            OracleConsole.showNewNote(frame, campaign, noteText(campaign, contract, status));
        }
    }

    private ContractJournalPrompt(final JFrame frame, final AbstractContract contract, final MissionStatus status) {
        super(frame, getTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.windowTitle"), true);

        dontShowAgain = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.dontShowAgain"));
        dontShowAgain.setToolTipText(getTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.dontShowAgain.toolTipText"));

        setContentPane(buildContent(contract, status));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(frame);
    }

    private JPanel buildContent(final AbstractContract contract, final MissionStatus status) {
        JLabel eyebrow = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleConsole.title").toUpperCase(Locale.ROOT));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.eyebrow")
                                        .toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));

        JPanel header = column();
        header.setOpaque(true);
        header.setBackground(GROUND);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        header.add(leftAligned(eyebrow));
        header.add(Box.createVerticalStrut(scaleForGUI(3)));
        header.add(leftAligned(title));

        JLabel body = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.body",
              escape(contract.getName()), status));
        body.setForeground(TEXT);
        body.setFont(hudFont(Font.PLAIN, 1.0f, 0.0f));
        // Wraps the text to a comfortable reading width.
        body.setText(body.getText().replace("<html>", "<html><div style='width:" + scaleForGUI(320) + "px'>")
                           .replace("</html>", "</div></html>"));
        JPanel bodyPanel = new JPanel(new BorderLayout());
        bodyPanel.setOpaque(true);
        bodyPanel.setBackground(SURFACE_DEEP);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(16), scaleForGUI(16), scaleForGUI(16),
              scaleForGUI(16)));
        bodyPanel.add(body, BorderLayout.CENTER);

        HudButton notNow = new HudButton(getTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.notNow")
                                               .toUpperCase(Locale.ROOT), false, true);
        notNow.addActionListener(event -> close(false));
        HudButton write = new HudButton(getTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.write")
                                              .toUpperCase(Locale.ROOT), true, true);
        write.addActionListener(event -> close(true));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(true);
        footer.setBackground(GROUND);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        footer.add(rightButtonRow(null, notNow, write), BorderLayout.CENTER);
        JPanel checkBoxRow = Hud.transparentPanel(new BorderLayout());
        checkBoxRow.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), 0, 0, 0));
        checkBoxRow.add(dontShowAgain, BorderLayout.EAST);
        footer.add(checkBoxRow, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(header, BorderLayout.NORTH);
        content.add(bodyPanel, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private void close(final boolean write) {
        writeEntry = write;
        dispose();
    }

    /**
     * The new note's opening text: the contract's name as a heading, then how it ended and the dates it ran.
     */
    private static String noteText(final Campaign campaign, final AbstractContract contract,
          final MissionStatus status) {
        LocalDate start = contract.getStartDate();
        LocalDate end = campaign.getLocalDate();
        String dates = (start == null) ? formatDate(end) : formatDate(start) + " – " + formatDate(end);
        return getFormattedTextAt(RESOURCE_BUNDLE, "ContractJournalPrompt.noteText", escape(contract.getName()),
              escape(status.toString()), escape(dates));
    }
}
