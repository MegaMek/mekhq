/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.optionChangeDialogs;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.MHQConstants.DISCORD_LINK;
import static mekhq.utilities.MHQInternationalization.getFormattedText;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.net.URI;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

public class StratConMaplessCampaignOptionsChangedConfirmationDialog {
    private static final MMLogger LOGGER = MMLogger.create(StratConMaplessCampaignOptionsChangedConfirmationDialog.class);

    private final static String ATB_RETIREMENT_LINK = "https://megamek.org/announcements/development/mekhq/2025/11/11/Retiring-the-Colors-Against-the-Bot.html";

    public StratConMaplessCampaignOptionsChangedConfirmationDialog(Campaign campaign) {
        new ImmersiveDialogCore(campaign,
              null,
              null,
              getFormattedText("StratConMaplessCampaignOptionsChangedConfirmationDialog.main",
                    MHQConstants.VERSION.toString()),
              createButton(),
              null,
              ImmersiveDialogWidth.LARGE.getWidth(),
              false,
              getPanel(),
              getBanner(),
              true);
    }

    private static ImageIcon getBanner() {
        ImageIcon banner = new ImageIcon("data/images/misc/megamek-splash.png");
        return ImageUtilities.scaleImageIcon(banner, scaleForGUI(400), true);
    }

    private static List<ImmersiveDialogCore.ButtonLabelTooltipPair> createButton() {
        return List.of(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Understood.text"), null));
    }

    private static JPanel getPanel() {
        JPanel pnlUpgrades = new JPanel();
        pnlUpgrades.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlUpgrades.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlUpgrades.getPreferredSize().height));


        JButton btnDiscord = new RoundedJButton("<html><b>" +
                                                      getText("MilestoneUpgradePathDialog.discord") +
                                                      "</b></html>");
        btnDiscord.setName("btnDiscord");
        buildUrlButton(pnlUpgrades, btnDiscord, DISCORD_LINK);

        JButton btnAnnouncement = new RoundedJButton("<html><b>" +
                                                           getText(
                                                                 "StratConMaplessCampaignOptionsChangedConfirmationDialog.announcement") +
                                                           "</b></html>");
        btnAnnouncement.setName("btnAnnouncement");
        buildUrlButton(pnlUpgrades, btnAnnouncement, ATB_RETIREMENT_LINK);

        return pnlUpgrades;
    }

    private static void buildUrlButton(JPanel pnlUpgrades, JButton btnDiscord, String discordLink) {
        btnDiscord.setToolTipText(discordLink);
        btnDiscord.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDiscord.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDiscord.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    URI uri = new URI(discordLink);
                    Desktop.getDesktop().browse(uri);
                } catch (Exception ex) {
                    LOGGER.error(ex, "Failed to open URL: {}", discordLink);
                }
            }
        });
        pnlUpgrades.add(btnDiscord);
    }
}
