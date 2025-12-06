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
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.MHQConstants.DISCORD_LINK;
import static mekhq.utilities.MHQInternationalization.getFormattedText;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import megamek.Version;
import megamek.common.util.milestoneReleaseInformation.MilestoneData;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * Dialog responsible for displaying the upgrade path for a {@link Campaign} based on the current campaign version.
 *
 * <p>If upgrades are necessary, it presents a dialog listing the required upgrade milestones as interactive buttons
 * that provide information and links. After acknowledgment, the application exits.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class MilestoneUpgradePathDialog {
    private static final MMLogger LOGGER = MMLogger.create(MilestoneUpgradePathDialog.class);

    /**
     * Displays the milestone upgrade path dialog if upgrades are needed for the supplied campaign.
     *
     * <p>If the dialog is shown, once it has been acknowledged, the application will exit.</p>
     *
     * @param campaign               the {@link Campaign} for which to check upgrade requirements
     * @param currentCampaignVersion the current {@link Version} of the campaign
     *
     * @author Illiani
     * @since 0.50.07
     */
    public MilestoneUpgradePathDialog(Campaign campaign, Version currentCampaignVersion) {
        // Are any upgrades necessary?
        final List<MilestoneData> upgradePath = getUpgradePath(currentCampaignVersion);
        if (upgradePath.isEmpty()) {
            LOGGER.info("No upgrade path found for campaign {}", campaign.getName());
            return;
        }

        // If upgrades are necessary, display the upgrade dialog
        new ImmersiveDialogCore(campaign,
              null,
              null,
              getFormattedText("MilestoneUpgradePathDialog.main", MHQConstants.VERSION.toString()),
              createButton(),
              getText("MilestoneUpgradePathDialog.secondary"),
              ImmersiveDialogWidth.LARGE.getWidth(),
              false,
              getPanel(upgradePath),
              getBanner(),
              true);

        // If upgrades were necessary, exit the app once the dialog has been confirmed
        campaign.getApp().exit(false);
    }

    /**
     * Determines the list of milestone releases that the given version must be upgraded through.
     *
     * @param currentVersion the current {@link Version} of the campaign
     *
     * @return a list of {@link MilestoneData} objects representing required upgrades
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static List<MilestoneData> getUpgradePath(Version currentVersion) {
        List<MilestoneData> allMilestoneReleases = MHQConstants.ALL_MILESTONE_RELEASES;

        List<MilestoneData> upgradePath = new ArrayList<>();
        for (MilestoneData milestone : allMilestoneReleases) {
            // If the milestone and the current version are identical, the campaign can always load in
            if (milestone.version().equals(MHQConstants.VERSION)) {
                continue;
            }

            if (currentVersion.isLowerThan(milestone.version())) {
                upgradePath.add(milestone);
            }
        }
        return upgradePath;
    }

    /**
     * Loads and scales the banner image used in the upgrade path dialog.
     *
     * @return a scaled {@link ImageIcon} instance for the banner
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static ImageIcon getBanner() {
        ImageIcon banner = new ImageIcon("data/images/misc/megamek-splash.png");
        return ImageUtilities.scaleImageIcon(banner, scaleForGUI(400), true);
    }

    /**
     * Creates the acknowledgement button.
     *
     * @return a list of {@link ImmersiveDialogCore.ButtonLabelTooltipPair} for the dialog buttons
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static List<ImmersiveDialogCore.ButtonLabelTooltipPair> createButton() {
        return List.of(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Understood.text"), null));
    }

    /**
     * Builds a panel containing an interactive list of upgrade milestones, where each milestone is represented as a
     * button. Buttons display the milestone label and open associated documentation URLs when clicked.
     *
     * @param upgradePath the list of {@link MilestoneData} milestones to display
     *
     * @return a {@link JPanel} containing the arranged milestone buttons
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static JPanel getPanel(List<MilestoneData> upgradePath) {
        JPanel pnlUpgrades = new JPanel();
        pnlUpgrades.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlUpgrades.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlUpgrades.getPreferredSize().height));


        JButton btnDiscord = new RoundedJButton("<html><b>" +
                                                      getText("MilestoneUpgradePathDialog.discord") +
                                                      "</b></html>");
        btnDiscord.setName("btnDiscord");
        buildUrlButton(pnlUpgrades, btnDiscord, DISCORD_LINK);

        // Create a batch of labels, one per upgrade, that both look and act like hyperlinks
        for (MilestoneData milestone : upgradePath) {
            String milestoneLabel = milestone.label();
            String milestoneUrl = milestone.getMekHQUrl();

            JButton btnUpgrade = new RoundedJButton("<html><b>" + milestoneLabel + "</b></html>");
            btnUpgrade.setName("upgrade" + milestoneLabel);
            buildUrlButton(pnlUpgrades, btnUpgrade, milestoneUrl);
        }
        return pnlUpgrades;
    }

    /**
     * Adds a button to a specified panel that, when clicked, attempts to open the given URL in the user's default web
     * browser.
     *
     * <p>This method sets the button's tooltip to the provided URL, changes its cursor to a hand when hovered,
     * centers its alignment, and attaches a mouse click listener. On click, if the current platform supports desktop
     * browsing, it will attempt to open the URL. If an error occurs during this process, the exception is logged.</p>
     *
     * @param pnlUpgrades the {@link JPanel} to which the button will be added
     * @param btnDiscord  the {@link JButton} that will serve as the clickable link
     * @param discordLink the URL to be opened when the button is clicked
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void buildUrlButton(JPanel pnlUpgrades, JButton btnDiscord, String discordLink) {
        btnDiscord.setToolTipText(discordLink);
        btnDiscord.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDiscord.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDiscord.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        URI uri = new URI(discordLink);
                        Desktop.getDesktop().browse(uri);
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Failed to open URL: {}", discordLink);
                    }
                }
            }
        });
        pnlUpgrades.add(btnDiscord);
    }
}
