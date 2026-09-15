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

import static mekhq.gui.stratCon.deployment.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementRoll;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.unit.Unit;

/**
 * The right-hand HUD panel of the StratCon deployment wizard: a dossier for whichever force the player is looking at,
 * the running list of forces staged for deployment, a budget line, and the stage / commit / cancel controls. Styled to
 * match the interstellar-map chrome via {@link HudStyle}.
 *
 * <p>This panel is a view. It renders what the wizard tells it to and exposes its buttons; the wizard owns the staged
 * state and decides what a click means.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DeploymentInspectorPanel extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final transient Campaign campaign;

    private final JLabel dossierLabel = new JLabel();
    private final JLabel budgetLabel = new JLabel();
    private final JLabel stagedTitle = new JLabel();
    private final DefaultListModel<Object> stagedModel = new DefaultListModel<>();
    private final JList<Object> stagedList = new JList<>(stagedModel);

    private final HudButton stageButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.stage"), false);
    private final HudButton cancelButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.cancel"), false);
    private final HudButton commitButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.commit"), true);

    public DeploymentInspectorPanel(Campaign campaign) {
        super(new BorderLayout(0, UIUtil.scaleForGUI(10)));
        this.campaign = campaign;

        setOpaque(true);
        setBackground(GROUND);
        int pad = UIUtil.scaleForGUI(12);
        setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, UIUtil.scaleForGUI(1), 0, 0, BORDER),
              BorderFactory.createEmptyBorder(pad, pad, pad, pad)));

        add(buildDossierSection(), BorderLayout.NORTH);
        add(buildStagedSection(), BorderLayout.CENTER);
        add(buildButtonRow(), BorderLayout.SOUTH);

        setStageButtonEnabled(false);
        showEmpty();
        setStaged(List.of());
        clearBudget();
    }

    private JPanel buildDossierSection() {
        dossierLabel.setVerticalAlignment(JLabel.TOP);
        dossierLabel.setForeground(TEXT);
        dossierLabel.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        int inset = UIUtil.scaleForGUI(10);
        dossierLabel.setBorder(BorderFactory.createEmptyBorder(inset, inset, inset, inset));

        JScrollPane dossierScroll = new JScrollPane(dossierLabel);
        dossierScroll.setBorder(BorderFactory.createLineBorder(BORDER, UIUtil.scaleForGUI(1)));
        dossierScroll.getViewport().setBackground(SURFACE_DEEP);
        dossierScroll.setPreferredSize(new java.awt.Dimension(UIUtil.scaleForGUI(320), UIUtil.scaleForGUI(220)));

        JPanel section = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        section.setOpaque(false);
        section.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.dossier.title")), BorderLayout.NORTH);
        section.add(dossierScroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildStagedSection() {
        budgetLabel.setForeground(ACCENT);
        budgetLabel.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));

        stagedList.setCellRenderer(new DeploymentItemRenderer(campaign));
        stagedList.setBackground(SURFACE_DEEP);
        stagedList.setBorder(null);

        JScrollPane stagedScroll = new JScrollPane(stagedList);
        stagedScroll.setBorder(BorderFactory.createLineBorder(BORDER, UIUtil.scaleForGUI(1)));
        stagedScroll.getViewport().setBackground(SURFACE_DEEP);

        JPanel header = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        header.setOpaque(false);
        header.add(budgetLabel, BorderLayout.NORTH);
        header.add(stagedTitle, BorderLayout.SOUTH);

        JPanel section = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        section.setOpaque(false);
        section.add(header, BorderLayout.NORTH);
        section.add(stagedScroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIUtil.scaleForGUI(8), 0));
        row.setOpaque(false);
        row.add(stageButton);
        row.add(cancelButton);
        row.add(commitButton);
        return row;
    }

    /**
     * Renders the dossier for the force the player is focused on: name, readiness, battle value, and unit count.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showFormation(Formation formation) {
        setDossier("<html>" + formationSummaryHtml(formation) + "</html>");
    }

    /**
     * Renders the dossier for a focused individual unit (auxiliaries and utility pages): name, status, and battle
     * value.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showUnit(Unit unit) {
        StringBuilder dossier = new StringBuilder("<html>");
        dossier.append("<b>").append(unit.getName()).append("</b><br/><br/>");
        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.inspector.unitStatus",
              unit.getStatus())).append("<br/>");
        if (unit.getEntity() != null) {
            dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "deploymentWizard.inspector.battleValue",
                  unit.getEntity().calculateBattleValue(true, true)));
        }
        dossier.append("</html>");
        setDossier(dossier.toString());
    }

    /**
     * Sets the budget line above the staged tray (leadership battle value or minefields remaining, depending on the
     * page). Pass {@code null} to hide it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setBudget(String html) {
        budgetLabel.setText(html == null ? " " : "<html>" + html + "</html>");
    }

    public void clearBudget() {
        budgetLabel.setText(" ");
    }

    /**
     * Renders the reinforcement dossier for a focused force: the shared formation summary plus how it is eligible to
     * reinforce, its roll and odds, and its per-force support-point cost.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showReinforcementFormation(Formation formation, ReinforcementEligibilityType eligibility,
          ReinforcementRoll roll, int perForceSupportPoints) {
        StringBuilder dossier = new StringBuilder("<html>");
        dossier.append(formationSummaryHtml(formation)).append("<br/><br/>");
        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.eligibility",
              eligibilityLabel(eligibility))).append("<br/>");
        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.targetNumber",
              roll.finalTargetNumber())).append("<br/>");
        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.odds",
              (int) Math.round(roll.successProbability() * 100))).append("<br/>");
        dossier.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.cost",
              perForceSupportPoints));
        dossier.append("</html>");

        setDossier(dossier.toString());
    }

    private String formationSummaryHtml(Formation formation) {
        StringBuilder summary = new StringBuilder();
        summary.append("<b>").append(formation.getName()).append("</b><br/>");
        summary.append("<font color='").append(hex(HudStyle.TEXT_MUTED)).append("'>")
              .append(formation.getFullName()).append("</font><br/><br/>");

        List<OperationalStatus> statuses = formation.updateFormationIconOperationalStatus(campaign);
        if (!statuses.isEmpty()) {
            summary.append(readinessSpan(statuses.get(0))).append("<br/>");
        }

        summary.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.inspector.battleValue",
              formation.getTotalBV(campaign, true))).append("<br/>");
        summary.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.inspector.units",
              formation.getAllUnits(true).size()));
        return summary.toString();
    }

    private static String eligibilityLabel(ReinforcementEligibilityType eligibility) {
        return switch (eligibility) {
            case REGULAR -> getTextAt(RESOURCE_BUNDLE, "regular.text");
            case AUXILIARY -> getTextAt(RESOURCE_BUNDLE, "auxiliary.text");
            case CHAINED_SCENARIO -> getTextAt(RESOURCE_BUNDLE, "fromChainedScenario.text");
            case NONE -> getTextAt(RESOURCE_BUNDLE, "deploymentWizard.reinforce.ineligible");
        };
    }

    /**
     * Sets the dossier area to arbitrary HTML. Used by pages (such as Reinforce) that show more than the shared
     * formation summary.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setDossier(String html) {
        dossierLabel.setText(html);
    }

    /**
     * Clears the dossier back to the "nothing focused" prompt.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showEmpty() {
        setDossier("<html><font color='" + hex(HudStyle.TEXT_MUTED) + "'>" +
                         getTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.empty") + "</font></html>");
    }

    /**
     * Replaces the staged-forces tray and updates its running count.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStaged(List<?> stagedItems) {
        stagedModel.clear();
        for (Object item : stagedItems) {
            stagedModel.addElement(item);
        }
        stagedTitle.setForeground(HudStyle.TEXT_FAINT);
        stagedTitle.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        stagedTitle.setText(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.staged.title",
              stagedItems.size()).toUpperCase(java.util.Locale.ROOT));
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
        stageButton.setArmed(enabled);
    }

    public HudButton getStageButton() {
        return stageButton;
    }

    public HudButton getCommitButton() {
        return commitButton;
    }

    public HudButton getCancelButton() {
        return cancelButton;
    }

    private static String readinessSpan(OperationalStatus status) {
        Color color = switch (status) {
            case FULLY_OPERATIONAL, FACTORY_FRESH -> READY;
            case SUBSTANTIALLY_OPERATIONAL -> CAUTION;
            case MARGINALLY_OPERATIONAL, NOT_OPERATIONAL -> DANGER;
        };
        String label = getTextAt(RESOURCE_BUNDLE, "deploymentWizard.readiness." + status.name());
        return spanOpeningWithCustomColor(hex(color)) + label + CLOSING_SPAN_TAG;
    }
}
