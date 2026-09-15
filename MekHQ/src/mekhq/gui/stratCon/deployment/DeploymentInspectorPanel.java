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
package mekhq.gui.stratCon.deployment;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.gui.stratCon.ScenarioWizardLanceRenderer;

/**
 * The right-hand panel of the StratCon deployment wizard: a dossier for whichever force the player is currently
 * looking at, the running list of forces staged for deployment, and the stage / commit / cancel controls.
 *
 * <p>This panel is a view. It renders what the wizard tells it to and exposes its buttons; the wizard owns the
 * {@link mekhq.campaign.digitalGM.stratCon.deployment.DeploymentContext} and decides what a click means.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DeploymentInspectorPanel extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final Campaign campaign;

    private final JLabel dossierLabel = new JLabel();
    private final JLabel stagedTitle = new JLabel();
    private final DefaultListModel<Formation> stagedModel = new DefaultListModel<>();
    private final JList<Formation> stagedList = new JList<>(stagedModel);

    private final JButton stageButton = new JButton();
    private final JButton commitButton = new JButton(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.commit"));
    private final JButton cancelButton = new JButton(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.cancel"));

    public DeploymentInspectorPanel(Campaign campaign) {
        super(new BorderLayout(0, 8));
        this.campaign = campaign;

        dossierLabel.setVerticalAlignment(JLabel.TOP);
        dossierLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane dossierScroll = new JScrollPane(dossierLabel);
        dossierScroll.setBorder(BorderFactory.createEmptyBorder());

        stagedList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        JPanel stagedPanel = new JPanel(new BorderLayout(0, 4));
        stagedPanel.add(stagedTitle, BorderLayout.NORTH);
        stagedPanel.add(new JScrollPane(stagedList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(stageButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(commitButton);

        add(dossierScroll, BorderLayout.NORTH);
        add(stagedPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setStageButtonEnabled(false);
        showEmpty();
        setStaged(List.of());
    }

    /**
     * Renders the dossier for the force the player is focused on: name, readiness, battle value, and unit count.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showFormation(Formation formation) {
        StringBuilder dossier = new StringBuilder("<html>");
        dossier.append("<b>").append(formation.getName()).append("</b><br/>");
        dossier.append(formation.getFullName()).append("<br/><br/>");

        List<OperationalStatus> statuses = formation.updateFormationIconOperationalStatus(campaign);
        if (!statuses.isEmpty()) {
            dossier.append(readinessSpan(statuses.get(0))).append("<br/>");
        }

        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.inspector.battleValue",
              formation.getTotalBV(campaign, true))).append("<br/>");
        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.inspector.units",
              formation.getAllUnits(true).size()));
        dossier.append("</html>");

        dossierLabel.setText(dossier.toString());
    }

    /**
     * Clears the dossier back to the "nothing focused" prompt.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showEmpty() {
        dossierLabel.setText("<html>" + getTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.empty") + "</html>");
    }

    /**
     * Replaces the staged-forces tray and updates its running count.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStaged(List<Formation> stagedFormations) {
        stagedModel.clear();
        for (Formation formation : stagedFormations) {
            stagedModel.addElement(formation);
        }
        stagedTitle.setText(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.staged.title",
              stagedFormations.size()));
    }

    /**
     * Sets the stage button's label to "Stage" or "Unstage" depending on whether the focused force is already staged.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStageButtonStaged(boolean staged) {
        stageButton.setText(getTextAt(RESOURCE_BUNDLE, staged ? "deploymentWizard.unstage" : "deploymentWizard.stage"));
    }

    public void setStageButtonEnabled(boolean enabled) {
        stageButton.setEnabled(enabled);
    }

    public JButton getStageButton() {
        return stageButton;
    }

    public JButton getCommitButton() {
        return commitButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    private static String readinessSpan(OperationalStatus status) {
        String color = switch (status) {
            case FULLY_OPERATIONAL, FACTORY_FRESH -> getPositiveColor();
            case SUBSTANTIALLY_OPERATIONAL -> getWarningColor();
            case MARGINALLY_OPERATIONAL, NOT_OPERATIONAL -> getNegativeColor();
        };
        String label = getTextAt(RESOURCE_BUNDLE, "deploymentWizard.readiness." + status.name());
        return spanOpeningWithCustomColor(color) + label + CLOSING_SPAN_TAG;
    }
}
