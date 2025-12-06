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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.utilities.EasyBugReport;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * A dialog that assists players in preparing and submitting bug reports.
 *
 * <p>This immersive UI component provides quick access to:</p>
 * <ul>
 *     <li>Links for reporting issues across MegaMek ecosystem repositories</li>
 *     <li>A one-click method for packaging the player's campaign into a bug report-ready archive</li>
 *     <li>A direct link to the MegaMek Discord for discussion or clarification</li>
 * </ul>
 *
 * <p>The UI layout provides two button rows:</p>
 * <ol>
 *     <li>Discord + campaign packaging</li>
 *     <li>Direct links to specific GitHub issue templates</li>
 * </ol>
 *
 * @author Illiani
 * @since 0.50.11
 */
public class EasyBugReportDialog extends ImmersiveDialogCore {
    private static final MMLogger LOGGER = MMLogger.create(EasyBugReportDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EasyBugReport";

    private static final String REPORT_LINK_MM = "https://github.com/MegaMek/megamek/issues/new/choose";
    private static final String REPORT_LINK_MML = "https://github.com/MegaMek/megameklab/issues/new/choose";
    private static final String REPORT_LINK_MHQ = "https://github.com/MegaMek/mekhq/issues/new/choose";
    private static final String REPORT_LINK_MM_DATA = "https://github.com/MegaMek/mm-data/issues/new";

    private static final Map<String, String> URI_LABELS = new LinkedHashMap<>();

    static {
        URI_LABELS.put(getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.mm"), REPORT_LINK_MM);
        URI_LABELS.put(getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.mml"), REPORT_LINK_MML);
        URI_LABELS.put(getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.mhq"), REPORT_LINK_MHQ);
        URI_LABELS.put(getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.mmData"), REPORT_LINK_MM_DATA);
    }


    /**
     * Constructs a new Easy Bug Report dialog configured for the supplied campaign and parent frame.
     *
     * <p>The dialog initializes localized text, predefined GitHub reporting links, a Discord jump button, and a
     * button that invokes {@link EasyBugReport#saveCampaignForBugReport(JFrame, Campaign)} to produce a report-ready
     * archive.</p>
     *
     * @param frame    the parent window for dialog placement and modality
     * @param campaign the current campaign whose state may be archived for a bug report
     *
     * @author Illiani
     * @since 0.50.11
     */
    public EasyBugReportDialog(JFrame frame, Campaign campaign) {
        super(campaign,
              null,
              null,
              getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.message.main"),
              getButtons(),
              getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.message.supplementary"),
              ImmersiveDialogWidth.LARGE.getWidth(),
              false,
              getSupplementalPanel(frame, campaign),
              null,
              true);
    }

    /**
     * Builds the single default button for the dialog: a localized Cancel button.
     *
     * @return an immutable list containing a single {@link ImmersiveDialogCore.ButtonLabelTooltipPair}
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons() {
        String label = getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.cancel");
        return List.of(new ImmersiveDialogCore.ButtonLabelTooltipPair(label, null));
    }

    /**
     * Creates the supplemental panel containing service buttons.
     * <p>Row 1 contains:</p>
     * <ul>
     *     <li>A Discord link button</li>
     *     <li>A "Save Campaign for Bug Report" button</li>
     * </ul>
     *
     * <p>Row 2 contains dynamically generated repository issue buttons based on {@link #URI_LABELS}.</p>
     *
     * @param frame    the parent UI frame, required for bug-report packaging
     * @param campaign the active campaign to extract packaged data from
     *
     * @return a fully constructed panel containing supplemental UI actions
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static JPanel getSupplementalPanel(JFrame frame, Campaign campaign) {
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel row1 = new JPanel();
        row1.setAlignmentX(Component.CENTER_ALIGNMENT);

        String lblDiscord = getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.discord");
        RoundedJButton btnDiscord = new RoundedJButton(lblDiscord);
        btnDiscord.setName(lblDiscord);
        buildUrlButton(btnDiscord, DISCORD_LINK);
        row1.add(btnDiscord);

        String lblPackageBundle = getTextAt(RESOURCE_BUNDLE, "EasyBugReport.dialog.button.build");
        RoundedJButton btnPackage = new RoundedJButton(lblPackageBundle);
        btnPackage.setName(lblPackageBundle);
        btnPackage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPackage.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPackage.addActionListener(evt -> EasyBugReport.saveCampaignForBugReport(frame, campaign));
        row1.add(btnPackage);

        JPanel row2 = new JPanel();
        row2.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (Map.Entry<String, String> entry : URI_LABELS.entrySet()) {
            RoundedJButton btnURL = new RoundedJButton(entry.getKey());
            btnURL.setName(entry.getKey());
            buildUrlButton(btnURL, entry.getValue());
            row2.add(btnURL);
        }

        rootPanel.add(row1);
        rootPanel.add(Box.createVerticalStrut(scaleForGUI(5)));
        rootPanel.add(row2);

        return rootPanel;
    }

    /**
     * Configures a button so that clicking it opens a URL in the user's system browser.
     *
     * <p>This assigns:</p>
     * <ul>
     *     <li>a tooltip displaying the destination</li>
     *     <li>a pointer cursor to reinforce clickability</li>
     *     <li>a mouse listener that attempts to launch the browser</li>
     * </ul>
     *
     * <p>Any exceptions occurring during URL construction or browser invocation are caught and logged.</p>
     *
     * @param btnURL  the Swing button to update
     * @param address the URL string this button should open
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static void buildUrlButton(JButton btnURL, String address) {
        btnURL.setToolTipText(address);
        btnURL.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnURL.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnURL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        URI uri = new URI(address);
                        Desktop.getDesktop().browse(uri);
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Failed to open URL: {}", address);
                    }
                }
            }
        });
    }
}
