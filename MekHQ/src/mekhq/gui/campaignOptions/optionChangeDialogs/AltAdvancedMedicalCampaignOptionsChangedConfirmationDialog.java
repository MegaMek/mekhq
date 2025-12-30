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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.round;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.personnel.medical.BodyLocation.GENERIC;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateImplants;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private static final Map<BodyLocation, InjuryType> SEVERED_LIMB_TRANSLATION_MAP = Map.of(
          BodyLocation.LEFT_ARM, AlternateInjuries.SEVERED_ARM,
          BodyLocation.RIGHT_ARM, AlternateInjuries.SEVERED_ARM,
          BodyLocation.LEFT_HAND, AlternateInjuries.SEVERED_HAND,
          BodyLocation.RIGHT_HAND, AlternateInjuries.SEVERED_HAND,
          BodyLocation.LEFT_LEG, AlternateInjuries.SEVERED_LEG,
          BodyLocation.RIGHT_LEG, AlternateInjuries.SEVERED_LEG,
          BodyLocation.LEFT_FOOT, AlternateInjuries.SEVERED_FOOT,
          BodyLocation.RIGHT_FOOT, AlternateInjuries.SEVERED_FOOT
    );

    private ImageIcon campaignIcon;
    private final Campaign campaign;

    private JCheckBox chkInjuryTransferral;
    private JCheckBox chkProtoMekPilots;

    public AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog(Campaign campaign) {
        this.campaignIcon = campaign.getCampaignFactionIcon();
        this.campaign = campaign;

        populateDialog();
        initializeDialog();
    }

    void initializeDialog() {
        setTitle(getText("accessingTerminal.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for campaign icon
        JPanel pnlLeft = buildLeftPanel();
        pnlLeft.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 1;
        mainPanel.add(pnlLeft, constraints);
        gridx++;

        // Center box for the message
        JPanel pnlCenter = populateCenterPanel();
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 2;
        constraints.weighty = 2;
        mainPanel.add(pnlCenter, constraints);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildLeftPanel() {
        JPanel pnlCampaign = new JPanel();
        pnlCampaign.setLayout(new BoxLayout(pnlCampaign, BoxLayout.Y_AXIS));
        pnlCampaign.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCampaign.setMaximumSize(new Dimension(IMAGE_WIDTH, scaleForGUI(MAX_VALUE)));

        campaignIcon = scaleImageIcon(campaignIcon, IMAGE_WIDTH, true);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(campaignIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlCampaign.add(imageLabel);

        return pnlCampaign;
    }

    private JPanel populateCenterPanel() {
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);

        String description = getFormattedTextAt(RESOURCE_BUNDLE,
              "AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog.description",
              spanOpeningWithCustomColor(getWarningColor()),
              CLOSING_SPAN_TAG);
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);

        FastJScrollPane scrollPane = new FastJScrollPane(editorPane);
        scrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollPane.setPreferredSize(new Dimension((int) round(CENTER_WIDTH * 1.2), scaleForGUI(600)));
        editorPane.setCaretPosition(0); // Start scrolled at the top, not bottom
        pnlCenter.add(scrollPane);

        pnlCenter.add(Box.createVerticalStrut(PADDING));

        chkInjuryTransferral = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
              "AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog.checkbox.injuries"));
        chkInjuryTransferral.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkInjuryTransferral.setSelected(true);

        chkProtoMekPilots = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
              "AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog.checkbox.enhancedImaging"));
        chkProtoMekPilots.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkProtoMekPilots.setSelected(true);

        pnlCenter.add(chkInjuryTransferral);
        pnlCenter.add(chkProtoMekPilots);

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(createButtonPanel());

        return pnlCenter;
    }

    private JPanel createButtonPanel() {
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedJButton btnConfirm = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog.confirm"));
        btnConfirm.addActionListener(evt -> {
            if (chkInjuryTransferral.isSelected()) {
                processInjuryTransferral(campaign);
            }
            if (chkProtoMekPilots.isSelected()) {
                processFreeEnhancedImaging(campaign);
            }
            dispose();
        });

        pnlButtons.add(btnConfirm);

        return pnlButtons;
    }

    public static void processFreeEnhancedImaging(Campaign campaign) {
        if (!campaign.getCampaignOptions().isUseImplants()) {
            return;
        }

        List<Person> personnel = campaign.getPersonnelFilteringOutDeparted();
        for (Person person : personnel) {
            if (!person.getPrimaryRole().isProtoMekPilot() && !person.getSecondaryRole().isProtoMekPilot()) {
                continue;
            }

            AdvancedMedicalAlternateImplants.giveEIImplant(campaign, person);

            campaign.personUpdated(person);
        }
    }

    public static void processInjuryTransferral(Campaign campaign) {
        List<Person> personnel = campaign.getPersonnelFilteringOutDeparted();
        for (Person person : personnel) {
            // First, Total Warfare-scale 'Hits'
            int hits = person.getHits();
            if (hits > 0) {
                for (int i = 0; i < hits; i++) {
                    Injury newInjury = AlternateInjuries.OLD_WOUND.newInjury(campaign, person, GENERIC, 1);
                    person.addInjury(newInjury);
                }
                person.setHits(0);
            }

            // Second, vanilla Advanced Medical 'Injuries'
            List<Injury> injuries = person.getInjuries();
            for (Injury oldInjury : injuries) {
                InjuryType injuryType = oldInjury.getType();

                // If the injury is an Alternate Advanced Medical injury, no transfer is required
                if (injuryType.getKey().contains("alt:")) {
                    continue;
                }

                boolean isMissingLocation = injuryType.impliesMissingLocation();

                // Missing locations have a special handler. In the event translation fails - most likely due to an
                // unexpected location - we're going to process the injury as if it were a normal permanent injury.
                if (isMissingLocation) {
                    BodyLocation oldInjuryLocation = oldInjury.getLocation();
                    InjuryType newInjuryType = SEVERED_LIMB_TRANSLATION_MAP.get(oldInjuryLocation);
                    if (newInjuryType != null) {
                        person.removeInjury(oldInjury);

                        Injury newInjury = newInjuryType.newInjury(campaign, person, oldInjuryLocation, 1);
                        if (newInjury != null) { // This will happen if there is an unexpected location match-up
                            person.addInjury(newInjury);
                            continue;
                        }
                    }

                    // Deliberate fall-through
                }

                // Handler for non-missing locations
                person.removeInjury(oldInjury);

                Injury newInjury = AlternateInjuries.OLD_WOUND.newInjury(campaign, person, GENERIC, 1);
                newInjury.setOriginalTime(oldInjury.getOriginalTime());
                newInjury.setTime(oldInjury.getTime());
                newInjury.setPermanent(oldInjury.isPermanent());
                person.addInjury(newInjury);
            }
        }
    }
}
