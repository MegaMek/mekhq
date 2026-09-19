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
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementRoll;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ScoutingSkills;
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
    // The scenario's hex and environment block, prepended to every dossier so it is always visible. Set once per open.
    private String environmentHtml = "";
    private final JCheckBox offBoardCheckBox =
          new JCheckBox(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.offBoard.label"));
    private final JLabel budgetLabel = new JLabel();
    private final JLabel stagedTitle = new JLabel();
    private final DefaultListModel<Object> stagedModel = new DefaultListModel<>();
    private final JList<Object> stagedList = new JList<>(stagedModel);
    private DeploymentItemRenderer stagedRenderer;

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

        FastJScrollPane dossierScroll = new FastJScrollPane(dossierLabel);
        dossierScroll.setBorder(BorderFactory.createLineBorder(BORDER, UIUtil.scaleForGUI(1)));
        dossierScroll.getViewport().setBackground(SURFACE_DEEP);
        dossierScroll.setPreferredSize(new java.awt.Dimension(UIUtil.scaleForGUI(320), UIUtil.scaleForGUI(280)));

        offBoardCheckBox.setOpaque(false);
        offBoardCheckBox.setForeground(TEXT);
        offBoardCheckBox.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
        offBoardCheckBox.setToolTipText(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.offBoard.tooltip"));
        offBoardCheckBox.setVisible(false);
        offBoardCheckBox.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), 0, 0, 0));

        JPanel section = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        section.setOpaque(false);
        section.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.dossier.title")), BorderLayout.NORTH);
        section.add(dossierScroll, BorderLayout.CENTER);
        section.add(offBoardCheckBox, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildStagedSection() {
        budgetLabel.setForeground(ACCENT);
        budgetLabel.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));

        stagedRenderer = new DeploymentItemRenderer(campaign);
        stagedList.setCellRenderer(stagedRenderer);
        stagedList.setBackground(SURFACE_DEEP);
        stagedList.setBorder(null);
        stagedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
        renderDossier(formationSummaryHtml(formation) + formationRosterHtml(formation));
    }

    /**
     * Renders the dossier for a focused individual unit (auxiliaries and utility pages): name, status, battle value,
     * crew, and repair state.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showUnit(Unit unit) {
        StringBuilder body = new StringBuilder();
        body.append("<b>").append(unit.getName()).append("</b><br/>");
        body.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.inspector.unitStatus",
              unit.getStatus())).append("<br/>");
        if (unit.getEntity() != null) {
            body.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "deploymentWizard.inspector.battleValue",
                  unit.getEntity().calculateBattleValue(true, true))).append("<br/>");
        }
        body.append(unitCrewAndRepairHtml(unit));
        renderDossier(body.toString());
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
          ReinforcementRoll roll, int perForceSupportPoints, int arrivalTurn) {
        StringBuilder body = new StringBuilder();
        body.append(formationSummaryHtml(formation)).append("<br/><br/>");
        body.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.eligibility",
              eligibilityLabel(eligibility))).append("<br/>");
        body.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.targetNumber",
              roll.finalTargetNumber())).append("<br/>");
        body.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.odds",
              (int) Math.round(roll.successProbability() * 100))).append("<br/>");
        body.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.cost",
              perForceSupportPoints)).append("<br/>");
        body.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.reinforce.arrival",
              arrivalTurn));
        body.append(formationRosterHtml(formation));

        renderDossier(body.toString());
    }

    /**
     * Sets the persistent hex-and-environment block shown at the top of every dossier. Pass {@code null} scenario data
     * to clear it (for example, a bare-hex deployment with no scenario yet).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setEnvironment(@Nullable AtBDynamicScenario backingScenario, @Nullable StratConTrackState track,
          @Nullable StratConCoords coords) {
        environmentHtml = buildEnvironmentHtml(backingScenario, track, coords);
    }

    private static String buildEnvironmentHtml(@Nullable AtBDynamicScenario backingScenario,
          @Nullable StratConTrackState track, @Nullable StratConCoords coords) {
        if ((backingScenario == null) && (coords == null)) {
            return "";
        }
        StringBuilder environment = new StringBuilder();

        environment.append(faintHeading("deploymentWizard.inspector.hex.title")).append("<br/>");
        if (coords != null) {
            environment.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.hex.coords",
                  coords.toBTString())).append("<br/>");
            if (track != null) {
                String terrain = track.getTerrainTile(coords);
                if (!terrain.isBlank()) {
                    environment.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.hex.terrain",
                          terrain)).append("<br/>");
                }
            }
        }

        if (backingScenario != null) {
            environment.append("<br/>").append(faintHeading("deploymentWizard.inspector.env.title")).append("<br/>");
            environment.append(envLine("deploymentWizard.inspector.env.atmosphere", backingScenario.getAtmosphere()));
            environment.append(envLine("deploymentWizard.inspector.env.taint", backingScenario.getAtmosphericTaint()));
            environment.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.env.temperature",
                  backingScenario.getTemperature())).append("<br/>");
            environment.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.env.gravity",
                  backingScenario.getGravity())).append("<br/>");
            environment.append(envLine("deploymentWizard.inspector.env.light", backingScenario.getLight()));
            environment.append(envLine("deploymentWizard.inspector.env.weather", backingScenario.getWeather()));
            environment.append(envLine("deploymentWizard.inspector.env.wind", backingScenario.getWind()));
            environment.append(envLine("deploymentWizard.inspector.env.fog", backingScenario.getFog()));
        }

        environment.append("<hr>");
        return environment.toString();
    }

    private static String faintHeading(String key) {
        return "<b><font color='" + hex(HudStyle.TEXT_FAINT) + "'>" + getTextAt(RESOURCE_BUNDLE, key) + "</font></b>";
    }

    private static String envLine(String key, Object value) {
        return getFormattedTextAt(RESOURCE_BUNDLE, key, value) + "<br/>";
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

    /**
     * Builds the per-unit roster for a formation: every unit's name, crew, and repair state, so the player can see what
     * shape the force is in before committing it.
     */
    private String formationRosterHtml(Formation formation) {
        StringBuilder roster = new StringBuilder();
        roster.append("<br/><br/>").append(faintHeading("deploymentWizard.inspector.roster")).append("<br/>");
        for (UUID unitId : formation.getAllUnits(true)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }
            roster.append("<br/><b>").append(unit.getName()).append("</b><br/>");
            roster.append(unitCrewAndRepairHtml(unit));
        }
        return roster.toString();
    }

    /**
     * Builds the crew and repair-state lines for a single unit: the commander and crew skills, the overall condition,
     * and the count of parts awaiting repair.
     */
    private String unitCrewAndRepairHtml(Unit unit) {
        StringBuilder details = new StringBuilder();

        Person commander = unit.getCommander();
        if (commander != null) {
            details.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.crew.commander",
                  commander.getFullTitle())).append("<br/>");
        }

        Entity entity = unit.getEntity();
        if (entity != null && entity.getCrew() != null) {
                details.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.crew.skills",
                      entity.getCrew().getGunnery(), entity.getCrew().getPiloting()));

                if (campaign.getCampaignOptions().get(CampaignOption.USE_ADVANCED_SCOUTING)) {
                    appendScoutingSkill(unit, details);
                }
            }

        details.append("<br/>");

        int crewSize = unit.getActiveCrew().size();
        if (crewSize > 1) {
            details.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.crew.size", crewSize))
                  .append("<br/>");
        }

        if (entity != null) {
            // Label the movement profile in the unit type's own terms: Safe/Max Thrust for aerospace, Cruise/Flank for
            // vehicles, Walk/Run (plus Jump when present) for everything ground-pounding.
            String movementKey;
            boolean canJump;
            if (entity.isAero()) {
                movementKey = "deploymentWizard.inspector.movement.aero";
                canJump = false;
            } else if (entity.isVehicle()) {
                movementKey = "deploymentWizard.inspector.movement.vehicle";
                canJump = true;
            } else {
                movementKey = "deploymentWizard.inspector.movement.ground";
                canJump = true;
            }

            StringBuilder movement = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE, movementKey,
                  entity.getWalkMP(), entity.getRunMP()));
            int jumpMP = entity.getJumpMP();
            if (canJump && (jumpMP > 0)) {
                movement.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.movement.jump", jumpMP));
            }
            details.append(movement).append("<br/>");
        }

        Color conditionColor = unit.isDamaged() ? AMBER : READY;
        String condition = spanOpeningWithCustomColor(hex(conditionColor))
                                 + Unit.getDamageStateName(unit.getDamageState()) + CLOSING_SPAN_TAG;
        details.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.repair.state", condition))
              .append("<br/>");

        int partsNeedingFixing = unit.getPartsNeedingFixing().size();
        if (partsNeedingFixing > 0) {
            details.append(spanOpeningWithCustomColor(hex(AMBER)))
                  .append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.repair.parts",
                        partsNeedingFixing))
                  .append(CLOSING_SPAN_TAG).append("<br/>");
        }

        return details.toString();
    }

    private static void appendScoutingSkill(Unit unit, StringBuilder details) {
        int bestScoutingSkillValue = Integer.MAX_VALUE;

        for (Person person : unit.getActiveCrew()) {
            String bestScoutingSkill = ScoutingSkills.getBestScoutingSkill(person);
            if (bestScoutingSkill != null && person.hasSkill(bestScoutingSkill)) {
                int scoutingValue = person.getSkill(bestScoutingSkill)
                                          .getFinalSkillValue(person.getSkillModifierData());
                if (scoutingValue < bestScoutingSkillValue) {
                    bestScoutingSkillValue = scoutingValue;
                }
            }
        }

        if (bestScoutingSkillValue != Integer.MAX_VALUE) {
            details.append(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.crew.scouting",
                  bestScoutingSkillValue));
        }
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
     * Wraps a dossier body fragment with the persistent hex-and-environment block and sets it into the scrollable
     * dossier area.
     */
    private void renderDossier(String bodyHtml) {
        // Constrain the width so the richer roster wraps and scrolls vertically instead of overflowing sideways.
        dossierLabel.setText("<html><body style='width:" + UIUtil.scaleForGUI(300) + "px'>"
                                   + environmentHtml + bodyHtml + "</body></html>");
    }

    /**
     * Clears the per-force dossier back to the "nothing focused" prompt, keeping the hex-and-environment block visible.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void showEmpty() {
        renderDossier("<font color='" + hex(HudStyle.TEXT_MUTED) + "'>" +
                            getTextAt(RESOURCE_BUNDLE, "deploymentWizard.inspector.empty") + "</font>");
    }
    /**
     * Flags which staged forces are deploying off-board, so the staged tray shows an off-board indicator on those rows.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStagedOffBoardForceIds(Set<Integer> offBoardForceIds) {
        stagedRenderer.setOffBoardForceIds(offBoardForceIds);
        stagedList.repaint();
    }

    /**
     * Flags which staged forces are already committed to the scenario, so the staged tray marks those rows "deployed".
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStagedLockedForceIds(Set<Integer> lockedForceIds) {
        stagedRenderer.setLockedForceIds(lockedForceIds);
        stagedList.repaint();
    }

    /**
     * Flags which staged loose units are already committed to the scenario, so the staged tray marks those rows
     * "deployed".
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStagedLockedUnitIds(Set<UUID> lockedUnitIds) {
        stagedRenderer.setLockedUnitIds(lockedUnitIds);
        stagedList.repaint();
    }

    /**
     * Replaces the staged-forces tray and updates its running count.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStaged(List<?> stagedItems) {
        stagedModel.clear();
        int count = 0;
        for (Object item : stagedItems) {
            stagedModel.addElement(item);
            if (!(item instanceof DeploymentItemRenderer.SectionHeader)) {
                count++;
            }
        }
        stagedTitle.setForeground(HudStyle.TEXT_FAINT);
        stagedTitle.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        stagedTitle.setText(getFormattedTextAt(RESOURCE_BUNDLE,
              "deploymentWizard.staged.title",
              count).toUpperCase(java.util.Locale.ROOT));
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

    /**
     * Registers a listener for selections in the staged tray, so the wizard can offer to unstage the focused item.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void addStagedSelectionListener(ListSelectionListener listener) {
        stagedList.addListSelectionListener(listener);
    }

    /**
     * @return the item currently selected in the staged tray (a {@link Formation}, a {@link Unit}, or a
     *       {@link DeploymentItemRenderer.SectionHeader} divider), or {@code null} if nothing is selected
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Object getSelectedStagedItem() {
        return stagedList.getSelectedValue();
    }

    public void clearStagedSelection() {
        stagedList.clearSelection();
    }

    /**
     * The "deploy off-board" checkbox, shown only for artillery-bearing formations when the client option is enabled.
     * The wizard wires its action and owns the off-board selection state.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public JCheckBox getOffBoardCheckBox() {
        return offBoardCheckBox;
    }

    public void setOffBoardOptionVisible(boolean visible) {
        offBoardCheckBox.setVisible(visible);
    }

    public void setOffBoardOptionSelected(boolean selected) {
        offBoardCheckBox.setSelected(selected);
    }

    public boolean isOffBoardOptionSelected() {
        return offBoardCheckBox.isSelected();
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
