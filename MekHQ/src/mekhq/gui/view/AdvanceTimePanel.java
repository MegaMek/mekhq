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

package mekhq.gui.view;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.events.NewDayEvent;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.ScalingWidthConstrainedPanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.utilities.MHQInternationalization;

/**
 * Provides UI controls for advancing the campaign timeline, shows the current date.
 * <p>
 * This panel subscribes to the global event bus updates to stay synchronized with the current date changes.
 * </p>
 */
public class AdvanceTimePanel extends ScalingWidthConstrainedPanel {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvanceTime";

    private static final String ADVANCE_N_DAYS_ICON = "data/images/widgets/advance_n_days.png";
    private static final String ADVANCE_DAY_ICON = "data/images/widgets/advance_day.png";

    /**
     * Constructs a new {@code AdvanceTimePanel}.
     *
     * @param minWidth               the minimum enforced width of the panel in pixels
     * @param maxWidth               the maximum enforced width of the panel in pixels
     * @param date                   the initial campaign date to display
     * @param advanceDay             a {@link Runnable} that advances a single day
     * @param openAdvanceNDaysDialog a {@link Runnable} that triggers the Advance Multiple Days dialog
     */
    public AdvanceTimePanel(int minWidth, int maxWidth, LocalDate date,
          Runnable advanceDay, Runnable openAdvanceNDaysDialog) {
        super(minWidth, maxWidth);

        // Advance Multiple Days button

        RoundedJButton btnAdvanceNDays = new RoundedJButton();
        ImageIcon icon = new ImageIcon(ADVANCE_N_DAYS_ICON);
        if (icon.getIconHeight() > 0) {
            btnAdvanceNDays.setIcon(icon);
        } else {
            btnAdvanceNDays.setText("..."); // fallback to text if the icon file is missing or not yet merged
        }
        btnAdvanceNDays.setToolTipText(getTextAt("advanceNDays.toolTip"));
        btnAdvanceNDays.addActionListener(e -> openAdvanceNDaysDialog.run());
        int advanceNDaysWidth = icon.getIconWidth() + CampaignGUI.MEDIUM_GAP * 3;
        int advanceNDaysHeight = icon.getIconHeight() + CampaignGUI.MEDIUM_GAP * 2;
        btnAdvanceNDays.setMinimumSize(new Dimension(advanceNDaysWidth, advanceNDaysHeight));
        btnAdvanceNDays.setMaximumSize(new Dimension(advanceNDaysWidth, Integer.MAX_VALUE));

        // Advance Day button

        RoundedJButton btnAdvanceDay = new RoundedJButton(getTextAt("advanceDay.label"));
        btnAdvanceDay.setIcon(new ImageIcon(ADVANCE_DAY_ICON));
        // This button uses a mnemonic that is unique and listed in the initMenu JavaDoc
        btnAdvanceDay.setMnemonic(KeyEvent.VK_A);
        btnAdvanceDay.setToolTipText(getTextAt("advanceDay.toolTip"));
        btnAdvanceDay.setIconTextGap(CampaignGUI.MEDIUM_GAP);
        btnAdvanceDay.addActionListener(evt -> {
            // We disable the button here, as we don't want the user to be able to advance
            // day  again, until after Advance Day has completed.
            btnAdvanceDay.setEnabled(false);
            btnAdvanceNDays.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    advanceDay.run();
                } finally {
                    btnAdvanceDay.setEnabled(true);
                    btnAdvanceNDays.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        });
        btnAdvanceDay.setMnemonic(KeyEvent.VK_A);
        Insets insets = RoundedLineBorder.createRoundedLineBorder().getBorderInsets(this);
        int reservedStaticWidth = CampaignGUI.SMALL_GAP + advanceNDaysWidth + insets.left + insets.right;
        btnAdvanceDay.setMinimumSize(new Dimension(minWidth - reservedStaticWidth, advanceNDaysHeight));
        btnAdvanceDay.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Panel layout

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(btnAdvanceDay);
        add(Box.createHorizontalStrut(CampaignGUI.SMALL_GAP));
        add(btnAdvanceNDays);

        refresh(date);
        MekHQ.registerHandler(this);
    }

    /**
     * Updates the panel's border to display the current campaign date.
     *
     * @param date the new local date to display
     */
    private void refresh(LocalDate date) {
        String formattedDate = MekHQ.getMHQOptions().getLongDisplayFormattedDate(date);
        Border innerPadding = BorderFactory.createEmptyBorder(CampaignGUI.THIN_GAP, 0, 0, 0);
        TitledBorder rounded = RoundedLineBorder.createRoundedLineBorder(formattedDate);
        setBorder(BorderFactory.createCompoundBorder(rounded, innerPadding));
    }

    /**
     * Retrieves localized text from the panel's resource bundle.
     */
    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    // ======================================
    // Event handlers for UI synchronization
    // ======================================

    @Subscribe
    public void handle(NewDayEvent event) {
        refresh(event.getCampaign().getLocalDate());
    }

}
