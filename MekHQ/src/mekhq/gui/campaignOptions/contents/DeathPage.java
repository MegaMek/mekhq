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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code DeathPage} class builds and manages the Death leaf page of the Biography section of the Campaign Options
 * dialog. It owns the widgets for random death configuration - the death multiplier, suicide-cause toggle, and the
 * per-age-group enablement checkboxes - and synchronises them with a shared {@link BiographyOptionsModel}.
 *
 * <p>This view is a sub-component of {@link BiographyPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code BiographyPages}, while this class is responsible only for constructing the Death panel and
 * copying its values to and from the model. The page is built lazily; until {@link #createPanel(BiographyOptionsModel)}
 * is called, {@link #readFromModel(BiographyOptionsModel)} and {@link #writeToModel(BiographyOptionsModel)} are
 * no-ops.</p>
 */
class DeathPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column because the Biography combo boxes need the extra room.
    private static final int CONTROL_COLUMN_WIDTH = 240;

    private CampaignOptionsHeaderPanel deathHeader;
    private JCheckBox chkUseRandomDeathSuicideCause;
    private JLabel lblRandomDeathMultiplier;
    private JSpinner spnRandomDeathMultiplier;
    private JPanel pnlDeathAgeGroup;
    private final Map<AgeGroup, JCheckBox> chkEnabledRandomDeathAgeGroups = new HashMap<>();

    private boolean created;

    /**
     * Builds the Death page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared biography options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Death Page
     */
    @Nonnull JPanel createPanel(@Nullable BiographyOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_fire_mandrills.png";
        deathHeader = new CampaignOptionsHeaderPanel("DeathPage", imageAddress);

        // Contents
        lblRandomDeathMultiplier = new CampaignOptionsLabel("RandomDeathMultiplier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.DOCUMENTED, CampaignOptionFlag.IMPORTANT));
        lblRandomDeathMultiplier.addMouseListener(createTipPanelUpdater("RandomDeathMultiplier"));
        spnRandomDeathMultiplier = new CampaignOptionsSpinner("RandomDeathMultiplier", 1.0, 0, 100.0, 0.01);
        spnRandomDeathMultiplier.addMouseListener(createTipPanelUpdater("RandomDeathMultiplier"));

        chkUseRandomDeathSuicideCause = new CampaignOptionsCheckBox("UseRandomDeathSuicideCause");
        chkUseRandomDeathSuicideCause.addMouseListener(createTipPanelUpdater("UseRandomDeathSuicideCause"));

        JPanel deathOptionsPanel = createDeathOptionsPanel();
        pnlDeathAgeGroup = createDeathAgeGroupsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("DeathPage", "DeathPage", imageAddress)
            .header(deathHeader)
            .quote("deathPage")
            .section("lblDeathPage.text", "lblDeathPage.summary", deathOptionsPanel)
            .section("lblDeathAgeGroupsPanel.text", "lblDeathAgeGroupsPanel.summary", pnlDeathAgeGroup)
            .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createDeathOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DeathOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRandomDeathMultiplier, spnRandomDeathMultiplier);
        panel.addCheckBox(chkUseRandomDeathSuicideCause);

        return panel;
    }

    /**
     * Configures and creates a panel where users can enable or disable random death
     * probabilities for specific age
     * groups.
     *
     * @return A `JPanel` containing the random death age group options.
     */
    private @Nonnull JPanel createDeathAgeGroupsPanel() {
        final AgeGroup[] ageGroups = AgeGroup.values();

        // Contents
        JCheckBox[] ageGroupCheckBoxes = new JCheckBox[ageGroups.length];
        for (final AgeGroup ageGroup : ageGroups) {
            final JCheckBox checkBox = new JCheckBox(ageGroup.toString());
            checkBox.setToolTipText(ageGroup.getToolTipText());
            checkBox.setName("chk" + ageGroup);
            checkBox.addMouseListener(createTipPanelUpdater(null, ageGroup.getToolTipText()));

            ageGroupCheckBoxes[ageGroup.ordinal()] = checkBox;
            chkEnabledRandomDeathAgeGroups.put(ageGroup, checkBox);
        }

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DeathAgeGroupsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2, ageGroupCheckBoxes);

        return panel;
    }

    /**
     * Copies death values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared biography options model to read values from
     */
    void readFromModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseRandomDeathSuicideCause.setSelected(model.useRandomDeathSuicideCause);
        spnRandomDeathMultiplier.setValue(model.randomDeathMultiplier);
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            JCheckBox ageGroupCheckBox = chkEnabledRandomDeathAgeGroups.get(ageGroup);
            if (ageGroupCheckBox != null) {
                ageGroupCheckBox.setSelected(Boolean.TRUE.equals(model.enabledRandomDeathAgeGroups.get(ageGroup)));
            }
        }
    }

    /**
     * Copies death values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared biography options model to write values into
     */
    void writeToModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useRandomDeathSuicideCause = chkUseRandomDeathSuicideCause.isSelected();
        model.randomDeathMultiplier = (double) spnRandomDeathMultiplier.getValue();
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            JCheckBox ageGroupCheckBox = chkEnabledRandomDeathAgeGroups.get(ageGroup);
            if (ageGroupCheckBox != null) {
                model.enabledRandomDeathAgeGroups.put(ageGroup, ageGroupCheckBox.isSelected());
            }
        }
    }
}
