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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code RecruitmentBonusesPage} class builds and manages the Recruitment Bonuses leaf page of the Campaign Options
 * dialog. It owns the per-{@link PersonnelRole} bonus spinners, grouped by combat and support roles, and synchronises
 * them with a shared {@link AwardsAndRandomizationOptionsModel}.
 *
 * <p>This view is a sub-component of {@link AwardsAndRandomizationPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code AwardsAndRandomizationPages}, while this class is responsible only for constructing the
 * Recruitment Bonuses panel and copying bonus values to and from the model. The page is built lazily; until
 * {@link #createPanel(AwardsAndRandomizationOptionsModel)} is called,
 * {@link #readFromModel(AwardsAndRandomizationOptionsModel)} and {@link #writeToModel(AwardsAndRandomizationOptionsModel)}
 * are no-ops.</p>
 */
class RecruitmentBonusesPage {
    private static final int LABEL_COLUMN_WIDTH = 190;
    private static final int CONTROL_COLUMN_WIDTH = 90;
    private static final int RECRUITMENT_PAIRS_PER_ROW = 2;

    private JPanel pnlRecruitmentBonusesCombat;
    private JLabel[] lblRecruitmentBonusCombat;
    private JSpinner[] spnRecruitmentBonusCombat;

    private JPanel pnlRecruitmentBonusesSupport;
    private JLabel[] lblRecruitmentBonusSupport;
    private JSpinner[] spnRecruitmentBonusSupport;

    private boolean created;

    /**
     * Constructs and returns the panel containing recruitment bonus controls
     * grouped by combat and support roles.
     *
     * <p>
     * Includes the header and separately laid-out subpanels for combat and support
     * personnel roles.
     * </p>
     *
     * @param model the shared awards and randomization options model to populate the freshly built controls from
     *
     * @return the fully configured {@link JPanel} for recruitment bonus settings
     */
    @Nonnull JPanel createPanel(@Nullable AwardsAndRandomizationOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_calderon_protectorate.png";
        CampaignOptionsHeaderPanel recruitmentBonusesHeader = new CampaignOptionsHeaderPanel("RecruitmentBonusesPage",
                imageAddress);

        // Contents
        pnlRecruitmentBonusesCombat = createRecruitmentBonusesCombatPanel();
        pnlRecruitmentBonusesSupport = createRecruitmentBonusesSupportPanel();

        JPanel panel = CampaignOptionsPagePanel.builder("RecruitmentBonusesPage", "RecruitmentBonusesPage", imageAddress)
                .header(recruitmentBonusesHeader)
                .showDetailsPanel(false)
                .section("lblRecruitmentBonusesCombatPanel.text",
                        "lblRecruitmentBonusesCombatPanel.summary",
                        pnlRecruitmentBonusesCombat)
                .section("lblRecruitmentBonusesSupportPanel.text",
                        "lblRecruitmentBonusesSupportPanel.summary",
                        pnlRecruitmentBonusesSupport)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates and initializes a panel for setting recruitment bonuses for combat
     * personnel roles.
     *
     * <p>
     * This method arranges labels and spinner controls for all combat-specific
     * personnel roles
     * in a grid layout. Each row contains up to four roles, where each role is
     * represented by a label and a
     * corresponding numeric spinner control for input.
     * </p>
     *
     * @return a configured {@link JPanel} specifically for defining recruitment
     *         bonuses for combat roles
     */
    private @Nonnull JPanel createRecruitmentBonusesCombatPanel() {
        // Contents
        List<PersonnelRole> roles = PersonnelRole.getCombatRoles();
        lblRecruitmentBonusCombat = new JLabel[roles.size()];
        spnRecruitmentBonusCombat = new JSpinner[roles.size()];

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RecruitmentBonusesCombatPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);

        JComponent[] labelsAndControls = new JComponent[roles.size() * 2];
        for (int i = 0; i < roles.size(); i++) {
            lblRecruitmentBonusCombat[i] = new JLabel(roles.get(i).getLabel(false));
            spnRecruitmentBonusCombat[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            CampaignOptionsSpinner.installSelectAllOnFocus(spnRecruitmentBonusCombat[i]);
            labelsAndControls[i * 2] = lblRecruitmentBonusCombat[i];
            labelsAndControls[i * 2 + 1] = spnRecruitmentBonusCombat[i];
        }
        panel.addRowGrid(RECRUITMENT_PAIRS_PER_ROW, labelsAndControls);

        return panel;
    }

    /**
     * Creates and initializes a panel for setting recruitment bonuses for support
     * (non-combat) personnel roles.
     *
     * <p>
     * This method arranges labels and spinner controls for all support-specific
     * personnel roles
     * in a grid layout. Each row contains up to four roles, where each role is
     * represented by a label and a
     * corresponding numeric spinner control for input.
     * </p>
     *
     * @return a configured {@link JPanel} specifically for defining recruitment
     *         bonuses for support roles
     */
    private @Nonnull JPanel createRecruitmentBonusesSupportPanel() {
        // Contents
        List<PersonnelRole> roles = PersonnelRole.getSupportRoles();
        lblRecruitmentBonusSupport = new JLabel[roles.size()];
        spnRecruitmentBonusSupport = new JSpinner[roles.size()];

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RecruitmentBonusesSupportPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);

        JComponent[] labelsAndControls = new JComponent[roles.size() * 2];
        for (int i = 0; i < roles.size(); i++) {
            lblRecruitmentBonusSupport[i] = new JLabel(roles.get(i).getLabel(false));
            spnRecruitmentBonusSupport[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            CampaignOptionsSpinner.installSelectAllOnFocus(spnRecruitmentBonusSupport[i]);
            labelsAndControls[i * 2] = lblRecruitmentBonusSupport[i];
            labelsAndControls[i * 2 + 1] = spnRecruitmentBonusSupport[i];
        }
        panel.addRowGrid(RECRUITMENT_PAIRS_PER_ROW, labelsAndControls);

        return panel;
    }

    /**
     * Copies recruitment bonus values from the shared model into this page's controls. This is a no-op until the page
     * has been built.
     *
     * @param model the shared awards and randomization options model to read values from
     */
    void readFromModel(@Nullable AwardsAndRandomizationOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        final List<PersonnelRole> combatRoles = PersonnelRole.getCombatRoles();
        for (int i = 0; i < spnRecruitmentBonusCombat.length; i++) {
            PersonnelRole role = combatRoles.get(i);
            spnRecruitmentBonusCombat[i].setValue(model.recruitmentBonuses.getOrDefault(role, 0));
        }

        final List<PersonnelRole> supportRoles = PersonnelRole.getSupportRoles();
        for (int i = 0; i < spnRecruitmentBonusSupport.length; i++) {
            PersonnelRole role = supportRoles.get(i);
            spnRecruitmentBonusSupport[i].setValue(model.recruitmentBonuses.getOrDefault(role, 0));
        }
    }

    /**
     * Copies recruitment bonus values from this page's controls into the shared model. This is a no-op until the page
     * has been built.
     *
     * @param model the shared awards and randomization options model to write values into
     */
    void writeToModel(@Nullable AwardsAndRandomizationOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        final List<PersonnelRole> supportRoles = PersonnelRole.getSupportRoles();
        final List<PersonnelRole> combatRoles = PersonnelRole.getCombatRoles();

        for (int i = 0; i < spnRecruitmentBonusCombat.length; i++) {
            PersonnelRole role = combatRoles.get(i);
            model.recruitmentBonuses.put(role, (int) spnRecruitmentBonusCombat[i].getValue());
        }

        for (int i = 0; i < spnRecruitmentBonusSupport.length; i++) {
            PersonnelRole role = supportRoles.get(i);
            model.recruitmentBonuses.put(role, (int) spnRecruitmentBonusSupport[i].getValue());
        }
    }
}
