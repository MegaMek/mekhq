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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code BiographyGeneralPage} class builds and manages the General leaf page of the Biography section of the
 * Campaign Options dialog. It owns the widgets for general biography options - gender distribution, familial
 * relationship display, anniversary announcements, life-event dialogs, and coming-of-age rewards - and synchronises
 * them with a shared {@link BiographyOptionsModel}.
 *
 * <p>This view is a sub-component of {@link BiographyPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code BiographyPages}, while this class is responsible only for constructing the General panel and
 * copying its values to and from the model. The page is built lazily; until {@link #createPanel(BiographyOptionsModel)}
 * is called, {@link #readFromModel(BiographyOptionsModel)} and {@link #writeToModel(BiographyOptionsModel)} are
 * no-ops.</p>
 */
class BiographyGeneralPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column because the Biography combo boxes need the extra room.
    private static final int CONTROL_COLUMN_WIDTH = 240;

    private CampaignOptionsHeaderPanel generalHeader;
    private JCheckBox chkUseDylansRandomXP;
    private JLabel lblGender;
    private JSpinner spnGender;
    private JLabel lblNonBinaryDiceSize;
    private JSpinner spnNonBinaryDiceSize;
    private JLabel lblFamilyDisplayLevel;
    private MMComboBox<FamilialRelationshipDisplayLevel> comboFamilyDisplayLevel;
    private JPanel pnlAnniversariesPanel;
    private JCheckBox chkAnnounceOfficersOnly;
    private JCheckBox chkAnnounceBirthdays;
    private JCheckBox chkAnnounceChildBirthdays;
    private JCheckBox chkAnnounceRecruitmentAnniversaries;
    private JCheckBox chkAnnounceRetireeDeath;
    private JCheckBox chkAnnounceRetireeDeathExpanded;
    private JPanel pnlLifeEvents;
    private JCheckBox chkShowLifeEventDialogBirths;
    private JCheckBox chkShowLifeEventDialogComingOfAge;
    private JCheckBox chkShowLifeEventDialogCelebrations;
    private JPanel pnlComingOfAge;
    private JCheckBox chkVeterancySPAs;
    private JCheckBox chkAwardRelevantVeterancySPAs;
    private JCheckBox chkComingOfAgeSPAs;
    private JCheckBox chkRewardComingOfAgeRPSkills;

    private boolean created;

    /**
     * Builds the General page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared biography options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the General Page
     */
    @Nonnull JPanel createPanel(@Nullable BiographyOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_blood_spirit.png";
        generalHeader = new CampaignOptionsHeaderPanel("BiographyGeneralPage", imageAddress);

        // Contents
        comboFamilyDisplayLevel = new MMComboBox<>("comboFamilyDisplayLevel",
                FamilialRelationshipDisplayLevel.values());

        chkUseDylansRandomXP = new CampaignOptionsCheckBox("UseDylansRandomXP");
        chkUseDylansRandomXP.addMouseListener(createTipPanelUpdater("UseDylansRandomXP"));

        lblGender = new CampaignOptionsLabel("Gender");
        lblGender.addMouseListener(createTipPanelUpdater("Gender"));
        spnGender = new CampaignOptionsSpinner("Gender", 50, 0, 100, 1);
        spnGender.addMouseListener(createTipPanelUpdater("Gender"));

        lblNonBinaryDiceSize = new CampaignOptionsLabel("NonBinaryDiceSize",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblNonBinaryDiceSize.addMouseListener(createTipPanelUpdater("NonBinaryDiceSize"));
        spnNonBinaryDiceSize = new CampaignOptionsSpinner("NonBinaryDiceSize", 60, 0, 100000, 1);
        spnNonBinaryDiceSize.addMouseListener(createTipPanelUpdater("NonBinaryDiceSize"));

        lblFamilyDisplayLevel = new CampaignOptionsLabel("FamilyDisplayLevel",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFamilyDisplayLevel.addMouseListener(createTipPanelUpdater("FamilyDisplayLevel"));
        comboFamilyDisplayLevel.addMouseListener(createTipPanelUpdater("FamilyDisplayLevel"));

        JPanel generalOptionsPanel = createBiographyGeneralOptionsPanel();
        pnlAnniversariesPanel = createAnniversariesPanel();
        pnlLifeEvents = createLifeEventsPanel();
        pnlComingOfAge = createComingOfAgePanel();
        JPanel panel = CampaignOptionsPagePanel.builder("BiographyGeneralPage", "BiographyGeneralPage", imageAddress)
            .header(generalHeader)
            .quote("biographyGeneralPage")
            .section("lblBiographyGeneralPage.text",
                "lblBiographyGeneralPage.summary",
                generalOptionsPanel,
                getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM))
            .section("lblAnniversariesPanel.text", "lblAnniversariesPanel.summary", pnlAnniversariesPanel)
            .section("lblLifeEventsPanel.text", "lblLifeEventsPanel.summary", pnlLifeEvents)
            .section("lblComingOfAgePanel.text", "lblComingOfAgePanel.summary", pnlComingOfAge)
            .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createBiographyGeneralOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("BiographyGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseDylansRandomXP);
        panel.addRow(lblGender, spnGender);
        panel.addRow(lblNonBinaryDiceSize, spnNonBinaryDiceSize);
        panel.addRow(lblFamilyDisplayLevel, comboFamilyDisplayLevel);

        return panel;
    }

    /**
     * Creates the Anniversaries panel within the General page for managing
     * announcement-related settings:
     * <p>
     * <li>Enabling birthday and recruitment anniversary announcements.</li>
     * <li>Specifying whether such announcements should be limited to officers.</li>
     * </p>
     *
     * @return A `JPanel` containing the UI components for defining
     *         anniversary-related settings.
     */
    private @Nonnull JPanel createAnniversariesPanel() {
        // Contents
        chkAnnounceBirthdays = new CampaignOptionsCheckBox("AnnounceBirthdays");
        chkAnnounceBirthdays.addMouseListener(createTipPanelUpdater("AnnounceBirthdays"));
        chkAnnounceRecruitmentAnniversaries = new CampaignOptionsCheckBox("AnnounceRecruitmentAnniversaries");
        chkAnnounceRecruitmentAnniversaries.addMouseListener(createTipPanelUpdater("AnnounceRecruitmentAnniversaries"));
        chkAnnounceRetireeDeath = new CampaignOptionsCheckBox("AnnounceRetireeDeath",
                getMetadata(new Version(0, 51, 0)));
        chkAnnounceRetireeDeath.addMouseListener(createTipPanelUpdater("AnnounceRetireeDeath"));
        chkAnnounceRetireeDeathExpanded = new CampaignOptionsCheckBox("AnnounceRetireeDeathExpanded",
                getMetadata(new Version(0, 51, 0)));
        chkAnnounceRetireeDeathExpanded.addMouseListener(createTipPanelUpdater("AnnounceRetireeDeathExpanded"));
        chkAnnounceOfficersOnly = new CampaignOptionsCheckBox("AnnounceOfficersOnly");
        chkAnnounceOfficersOnly.addMouseListener(createTipPanelUpdater("AnnounceOfficersOnly"));
        chkAnnounceChildBirthdays = new CampaignOptionsCheckBox("AnnounceChildBirthdays",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkAnnounceChildBirthdays.addMouseListener(createTipPanelUpdater("AnnounceChildBirthdays"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AnniversariesPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkAnnounceBirthdays,
                chkAnnounceRecruitmentAnniversaries,
                chkAnnounceOfficersOnly,
                chkAnnounceChildBirthdays,
                chkAnnounceRetireeDeath,
                chkAnnounceRetireeDeathExpanded);

        return panel;
    }

    private @Nonnull JPanel createLifeEventsPanel() {
        // Contents
        chkShowLifeEventDialogBirths = new CampaignOptionsCheckBox("ShowLifeEventDialogBirths");
        chkShowLifeEventDialogBirths.addMouseListener(createTipPanelUpdater("ShowLifeEventDialogBirths"));
        chkShowLifeEventDialogComingOfAge = new CampaignOptionsCheckBox("ShowLifeEventDialogComingOfAge");
        chkShowLifeEventDialogComingOfAge.addMouseListener(createTipPanelUpdater("ShowLifeEventDialogComingOfAge"));
        chkShowLifeEventDialogCelebrations = new CampaignOptionsCheckBox("ShowLifeEventDialogCelebrations");
        chkShowLifeEventDialogCelebrations.addMouseListener(createTipPanelUpdater("ShowLifeEventDialogCelebrations"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("LifeEventsPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkShowLifeEventDialogBirths,
                chkShowLifeEventDialogComingOfAge,
                chkShowLifeEventDialogCelebrations);

        return panel;
    }

    private @Nonnull JPanel createComingOfAgePanel() {
        // Contents
        chkVeterancySPAs = new CampaignOptionsCheckBox("VeterancySPAs",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        chkVeterancySPAs.addMouseListener(createTipPanelUpdater("VeterancySPAs"));

        chkAwardRelevantVeterancySPAs = new CampaignOptionsCheckBox("AwardRelevantVeterancySPAs",
                getMetadata(new Version(0, 51, 0), CampaignOptionFlag.IMPORTANT));
        chkAwardRelevantVeterancySPAs.addMouseListener(createTipPanelUpdater("AwardRelevantVeterancySPAs"));

        chkComingOfAgeSPAs = new CampaignOptionsCheckBox("ComingOfAgeAbilities",
                getMetadata(null, CampaignOptionFlag.RECOMMENDED));
        chkComingOfAgeSPAs.addMouseListener(createTipPanelUpdater("ComingOfAgeAbilities"));

        chkRewardComingOfAgeRPSkills = new CampaignOptionsCheckBox("ComingOfAgeRPSkills",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkRewardComingOfAgeRPSkills.addMouseListener(createTipPanelUpdater("ComingOfAgeRPSkills"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ComingOfAgePanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkVeterancySPAs,
                chkAwardRelevantVeterancySPAs,
                chkComingOfAgeSPAs,
                chkRewardComingOfAgeRPSkills);

        return panel;
    }

    /**
     * Copies general biography values from the shared model into this page's controls. This is a no-op until the page
     * has been built.
     *
     * @param model the shared biography options model to read values from
     */
    void readFromModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseDylansRandomXP.setSelected(model.useDylansRandomXP);
        spnGender.setValue(model.percentFemale);
        spnNonBinaryDiceSize.setValue(model.nonBinaryDiceSize);
        comboFamilyDisplayLevel.setSelectedItem(model.familyDisplayLevel);
        chkAnnounceOfficersOnly.setSelected(model.announceOfficersOnly);
        chkAnnounceBirthdays.setSelected(model.announceBirthdays);
        chkAnnounceChildBirthdays.setSelected(model.announceChildBirthdays);
        chkAnnounceRecruitmentAnniversaries.setSelected(model.announceRecruitmentAnniversaries);
        chkAnnounceRetireeDeath.setSelected(model.announceRetireeDeath);
        chkAnnounceRetireeDeathExpanded.setSelected(model.announceRetireeDeathExpanded);
        chkShowLifeEventDialogBirths.setSelected(model.showLifeEventDialogBirths);
        chkShowLifeEventDialogComingOfAge.setSelected(model.showLifeEventDialogComingOfAge);
        chkShowLifeEventDialogCelebrations.setSelected(model.showLifeEventDialogCelebrations);
        chkVeterancySPAs.setSelected(model.awardVeterancySPAs);
        chkAwardRelevantVeterancySPAs.setSelected(model.awardRelevantVeterancySPAs);
        chkComingOfAgeSPAs.setSelected(model.rewardComingOfAgeAbilities);
        chkRewardComingOfAgeRPSkills.setSelected(model.rewardComingOfAgeRPSkills);
    }

    /**
     * Copies general biography values from this page's controls into the shared model. This is a no-op until the page
     * has been built.
     *
     * @param model the shared biography options model to write values into
     */
    void writeToModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useDylansRandomXP = chkUseDylansRandomXP.isSelected();
        model.percentFemale = (int) spnGender.getValue();
        model.nonBinaryDiceSize = (int) spnNonBinaryDiceSize.getValue();
        model.familyDisplayLevel = comboFamilyDisplayLevel.getSelectedItem();
        model.announceOfficersOnly = chkAnnounceOfficersOnly.isSelected();
        model.announceBirthdays = chkAnnounceBirthdays.isSelected();
        model.announceChildBirthdays = chkAnnounceChildBirthdays.isSelected();
        model.announceRecruitmentAnniversaries = chkAnnounceRecruitmentAnniversaries.isSelected();
        model.announceRetireeDeath = chkAnnounceRetireeDeath.isSelected();
        model.announceRetireeDeathExpanded = chkAnnounceRetireeDeathExpanded.isSelected();
        model.showLifeEventDialogBirths = chkShowLifeEventDialogBirths.isSelected();
        model.showLifeEventDialogComingOfAge = chkShowLifeEventDialogComingOfAge.isSelected();
        model.showLifeEventDialogCelebrations = chkShowLifeEventDialogCelebrations.isSelected();
        model.awardVeterancySPAs = chkVeterancySPAs.isSelected();
        model.awardRelevantVeterancySPAs = chkAwardRelevantVeterancySPAs.isSelected();
        model.rewardComingOfAgeAbilities = chkComingOfAgeSPAs.isSelected();
        model.rewardComingOfAgeRPSkills = chkRewardComingOfAgeRPSkills.isSelected();
    }
}
