/*
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.getEffectiveFatigue;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.utilities.ReportingUtilities;

/**
 * A custom panel that gets filled in with goodies from a Force record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ForceViewPanel extends JScrollablePanel {
    private final Force force;
    private final Campaign campaign;

    private JPanel pnlStats;
    private JPanel pnlSubUnits;

    public ForceViewPanel(Force f, Campaign c) {
        super();
        this.force = f;
        this.campaign = c;
        this.setBorder(null);
        initComponents();
    }

    private void initComponents() {
        getAccessibleContext().setAccessibleName("Selected Force: " + force.getFullName());

        JLabel lblIcon = new JLabel();
        pnlStats = new JPanel();
        pnlSubUnits = new JPanel();
        JTextPane txtDesc = new JTextPane();

        setLayout(new GridBagLayout());

        lblIcon.setIcon(force.getForceIcon().getImageIcon(150));
        lblIcon.setName("lblIcon");
        lblIcon.getAccessibleContext().setAccessibleName("Force Icon");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(10, 10, 0, 0);
        add(lblIcon, gridBagConstraints);

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(RoundedLineBorder.createRoundedLineBorder(force.getName()));
        fillStats();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        pnlSubUnits.setName("pnlSubUnits");
        pnlSubUnits.getAccessibleContext().setAccessibleName("Force Composition");
        fillSubUnits();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlSubUnits, gridBagConstraints);

        if (null != force.getDescription() && !force.getDescription().isEmpty()) {
            txtDesc.setName("txtDesc");
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(force.getDescription()));
            txtDesc.setBorder(RoundedLineBorder.createRoundedLineBorder("Description"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, gridBagConstraints);
        }
    }

    private void fillStats() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ForceViewPanel",
              MekHQ.getMHQOptions().getLocale());

        JLabel lblType = new JLabel();
        JLabel lblAssign1 = new JLabel();
        JLabel lblAssign2 = new JLabel();
        JLabel lblFormationType1 = new JLabel();
        JLabel lblFormationType2 = new JLabel();
        JLabel lblCommander1 = new JLabel();
        JLabel lblCommander2 = new JLabel();
        JLabel lblBV1 = new JLabel();
        JLabel lblBV2 = new JLabel();
        JLabel lblCost1 = new JLabel();
        JLabel lblCost2 = new JLabel();
        JLabel lblTonnage1 = new JLabel();
        JLabel lblTonnage2 = new JLabel();
        JLabel lblTech1 = new JLabel();
        JLabel lblTech2 = new JLabel();
        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        pnlStats.getAccessibleContext().setAccessibleName("Force Statistics");

        long bv = 0;
        Money cost = Money.zero();
        double ton = 0;
        String lanceTech = "";
        String assigned = "";
        String type = null;

        Person commanderPerson = campaign.getPerson(force.getForceCommanderID());

        if (force.getId() == 0) {
            commanderPerson = campaign.getCommander();
        }
        String commanderName = commanderPerson != null ? commanderPerson.getFullTitle() : "";

        for (UUID uid : force.getAllUnits(false)) {
            Unit unit = campaign.getUnit(uid);
            if (null != unit) {
                // Never factor in C3 in this check. It will cause the TO&E to lock up for large campaigns.
                bv += unit.getEntity().calculateBattleValue(true, !unit.hasPilot());
                cost = cost.plus(unit.getEntity().getCost(true));
                ton += unit.getEntity().getWeight();
                String unitTypeName = UnitType.getTypeDisplayableName(unit.getEntity().getUnitType());
                if (null == type) {
                    type = unitTypeName;
                } else if (!unitTypeName.equals(type)) {
                    type = resourceMap.getString("mixed");
                }
            }
        }

        if (force.getTechID() != null) {
            final Person person = campaign.getPerson(force.getTechID());
            if (person != null) {
                lanceTech = person.getFullName();
            }
        }

        if (null != force.getParentForce()) {
            assigned = force.getParentForce().getName();
        }

        int nexty = 0;

        if (null != type) {
            lblType.setName("lblType");

            ForceType forceType = force.getForceType();

            String forceLabel;
            if (forceType.isStandard()) {
                forceLabel = force.getFormationLevel().toString();
            } else {
                forceLabel = forceType.getDisplayName() + ' ' + force.getFormationLevel().toString();
            }

            lblType.setText("<html><i>" + forceLabel + "</i></html>");
            lblType.getAccessibleContext().setAccessibleDescription("Force Type: " + forceLabel);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblType, gridBagConstraints);
            nexty++;
        }

        if (!commanderName.isBlank()) {
            lblCommander1.setName("lblCommander1");
            lblCommander1.setText(resourceMap.getString("lblCommander1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCommander1, gridBagConstraints);

            lblCommander2.setName("lblCommander2");
            lblCommander2.setText(commanderName);
            lblCommander1.setLabelFor(lblCommander2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCommander2, gridBagConstraints);
            nexty++;
        }

        lblFormationType1.setName("lblFormationType1");
        lblFormationType1.setText(resourceMap.getString("lblFormationType1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblFormationType1, gridBagConstraints);

        lblFormationType2.setName("lblFormationType2");
        lblFormationType2.setText(force.getFormationLevel().toString());
        lblFormationType1.setLabelFor(lblFormationType2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblFormationType2, gridBagConstraints);
        nexty++;

        if (null != force.getTechID()) {
            if (!lanceTech.isBlank()) {
                lblTech1.setName("lblTech1");
                lblTech1.setText(resourceMap.getString("lblTech1.text"));
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = nexty;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblTech1, gridBagConstraints);

                lblTech2.setName("lblTech2");
                lblTech2.setText(lanceTech);
                lblTech1.setLabelFor(lblTech2);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = nexty;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new Insets(0, 10, 0, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                pnlStats.add(lblTech2, gridBagConstraints);
                nexty++;
            }
        }

        if (!assigned.isBlank()) {
            lblAssign1.setName("lblAssign1");
            lblAssign1.setText(resourceMap.getString("lblAssign1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblAssign1, gridBagConstraints);

            lblAssign2.setName("lblAssign2");
            lblAssign2.setText(assigned);
            lblAssign1.setLabelFor(lblAssign2);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblAssign2, gridBagConstraints);
            nexty++;
        }

        lblBV1.setName("lblBV1");
        lblBV1.setText(resourceMap.getString("lblBV1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBV1, gridBagConstraints);

        lblBV2.setName("lblBV2");
        lblBV2.setText(DecimalFormat.getInstance().format(bv));
        lblBV1.setLabelFor(lblBV1);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBV2, gridBagConstraints);
        nexty++;

        lblTonnage1.setName("lblTonnage1");
        lblTonnage1.setText(resourceMap.getString("lblTonnage1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTonnage1, gridBagConstraints);

        lblTonnage2.setName("lblTonnage2");
        lblTonnage2.setText(DecimalFormat.getInstance().format(ton));
        lblTonnage1.setLabelFor(lblTonnage2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTonnage2, gridBagConstraints);
        nexty++;

        // if AtB is enabled, set tooltip to show lance weight breakdowns
        if (campaign.getCampaignOptions().isUseAtB()) {
            // see Lance.java for lance weight breakdowns
            lblTonnage1.setToolTipText(resourceMap.getString("tonnageToolTip.text"));
            lblTonnage2.setToolTipText(resourceMap.getString("tonnageToolTip.text"));
        }

        lblCost1.setName("lblCost1");
        lblCost1.setText(resourceMap.getString("lblCost1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCost1, gridBagConstraints);

        lblCost2.setName("lblCost2");
        lblCost2.setText(cost.toAmountAndSymbolString());
        lblCost1.setLabelFor(lblCost2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCost2, gridBagConstraints);
        nexty++;

        //BV
        //Tonnage?
        //Cost?
        //Number of units?
        //Assigned to
    }

    private void fillSubUnits() {
        GridBagConstraints gridBagConstraints;

        pnlSubUnits.setLayout(new GridBagLayout());

        JLabel lblForce;

        int nexty = 0;
        for (Force subForce : force.getSubForces()) {
            lblForce = new JLabel();
            lblForce.setText(getForceSummary(subForce));
            lblForce.setIcon(subForce.getForceIcon().getImageIcon(72));
            nexty++;
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSubUnits.add(lblForce, gridBagConstraints);
        }

        JLabel lblPerson;
        JLabel lblUnit;
        ArrayList<Unit> units = new ArrayList<>();
        ArrayList<Unit> unmannedUnits = new ArrayList<>();
        for (UUID uid : force.getUnits()) {
            Unit u = campaign.getUnit(uid);
            if (null == u) {
                continue;
            }
            if (null == u.getCommander()) {
                unmannedUnits.add(u);
            } else {
                units.add(u);
            }
        }
        //sort person vector by rank
        units.sort((u1, u2) -> ((Comparable<Integer>) u2.getCommander().getRankNumeric()).compareTo(u1.getCommander()
                                                                                                          .getRankNumeric()));
        units.addAll(unmannedUnits);
        for (Unit unit : units) {
            Person p = unit.getCommander();
            lblPerson = new JLabel();
            lblUnit = new JLabel();
            if (null != p) {
                lblPerson.setText(getForceSummary(p, unit));
                lblPerson.setIcon(p.getPortrait().getImageIcon());
            } else {
                lblPerson.getAccessibleContext().setAccessibleName("Unmanned Unit");
            }
            nexty++;
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSubUnits.add(lblPerson, gridBagConstraints);
            lblUnit.setText(getForceSummary(unit));
            lblUnit.setIcon(new ImageIcon(unit.getImage(lblUnit)));
            lblPerson.setLabelFor(lblUnit);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlSubUnits.add(lblUnit, gridBagConstraints);
        }
    }

    public String getForceSummary(Person person, Unit unit) {
        if (null == person) {
            return "";
        }

        StringBuilder toReturn = new StringBuilder();
        toReturn.append("<html><nobr><font size='3'><b>")
              .append(person.getFullTitle())
              .append("</b><br/><b>")
              .append(SkillType.getColoredExperienceLevelName(person.getSkillLevel(campaign, false)))
              .append("</b> ")
              .append(person.getRoleDesc());

        toReturn.append("<br>");

        boolean isInjured = false;
        boolean isFatigued = false;

        if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
            if (person.hasInjuries(true)) {
                isInjured = true;
                int injuryCount = person.getInjuries().size();

                String injuriesMessage = " " + injuryCount + (injuryCount == 1 ? " injury" : " injuries");

                toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                      ReportingUtilities.getNegativeColor(), injuriesMessage));
            }

        } else {
            if (null != unit && null != unit.getEntity() && null != unit.getEntity().getCrew()
                      && unit.getEntity().getCrew().getHits() > 0) {
                isInjured = true;
                int hitCount = unit.getEntity().getCrew().getHits();

                String hitsMessage = " " + hitCount + (hitCount == 1 ? " hit" : " hits");

                toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                      ReportingUtilities.getNegativeColor(), hitsMessage));
            }
        }

        int effectiveFatigue = getEffectiveFatigue(person.getFatigue(), person.isClanPersonnel(),
              person.getSkillLevel(campaign, false));
        if (campaign.getCampaignOptions().isUseFatigue() && (effectiveFatigue > 0)) {
            isFatigued = true;
            if (isInjured) {
                toReturn.append(',');
            }
            toReturn.append(' ');

            String fatigueMessage = effectiveFatigue + " fatigue";

            toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getWarningColor(), fatigueMessage));
        }

        if (!(isInjured || isFatigued)) {
            toReturn.append("&nbsp;");
        }

        toReturn.append("</font></nobr></html>");
        return toReturn.toString();
    }

    public String getForceSummary(Unit unit) {
        String toReturn = "<html><font size='4'><b>" + unit.getName() + "</b></font><br/>";

        // Never factor in C3 in this check. It will cause the TO&E to lock up for large campaigns.
        toReturn += "<font><b>BV:</b> " +
                          unit.getEntity().calculateBattleValue(true, null == unit.getEntity().getCrew()) +
                          "<br/>";
        toReturn += unit.getStatus();
        Entity entity = unit.getEntity();
        if (entity.hasNavalC3()) {
            toReturn += "<br><i>";
            if (entity.calculateFreeC3Nodes() >= 5) {
                toReturn += Messages.getString("ChatLounge.NC3None");
            } else {
                toReturn += Messages.getString("ChatLounge.NC3Network") + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                    toReturn += Messages.getString("ChatLounge.NC3Nodes", entity.calculateFreeC3Nodes());
                }
            }
            toReturn += "</i>";
        } else if (entity.hasC3i()) {
            toReturn += "<br><i>";
            if (entity.calculateFreeC3Nodes() >= 5) {
                toReturn += Messages.getString("ChatLounge.C3iNone");
            } else {
                toReturn += Messages
                                  .getString("ChatLounge.C3iNetwork")
                                  + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                    toReturn += Messages.getString("ChatLounge.C3Nodes", entity.calculateFreeC3Nodes());
                }
            }
            toReturn += "</i>";
        }
        if (unit.hasTransportShipAssignment()) {
            toReturn += "<br><i>" + "Transported (Ship) by: ";
            toReturn += unit.getTransportShipAssignment().getTransportShip().getName();
            toReturn += "</i>";
        }
        // If this is a transport ship, tell us what bay capacity is at
        if (!unit.getEntity().getTransportBays().isEmpty()) {
            int veeTotal = (int) (unit.getCurrentLightVehicleCapacity() +
                                        unit.getCurrentHeavyVehicleCapacity() +
                                        unit.getCurrentSuperHeavyVehicleCapacity());
            int aeroTotal = (int) (unit.getCurrentASFCapacity() + unit.getCurrentSmallCraftCapacity());
            if (unit.getCurrentMekCapacity() > 0) {
                toReturn += "<br><i>" + "Mek Bays: " + (int) unit.getCurrentMekCapacity() + " free.</i>";
            }
            if (veeTotal > 0) {
                toReturn += "<br><i>" + "Vehicle Bays: " + veeTotal + " free.</i>";
            }
            if (aeroTotal > 0) {
                toReturn += "<br><i>" + "ASF/SC Bays: " + aeroTotal + " free.</i>";
            }
            if (unit.getCurrentProtoMekCapacity() > 0) {
                toReturn += "<br><i>" + "ProtoMek Bays: " + (int) unit.getCurrentProtoMekCapacity() + " free.</i>";
            }
            if (unit.getCurrentBattleArmorCapacity() > 0) {
                toReturn += "<br><i>" +
                                  "Battle Armor Bays: " +
                                  (int) unit.getCurrentBattleArmorCapacity() +
                                  " free.</i>";
            }
            if (unit.getCurrentInfantryCapacity() > 0) {
                toReturn += "<br><i>" + "Infantry Bays: " + (int) unit.getCurrentInfantryCapacity() + " tons free.</i>";
            }
        }
        //Let's get preferred transport too
        if (unit.hasTacticalTransportAssignment()) {
            toReturn += "<br><i>" + "Transported (Tactical) by: ";
            toReturn += unit.getTacticalTransportAssignment().getTransport().getName();
            toReturn += "</i>";
        }

        //Can't forget what's towing this unit!
        if (unit.hasTransportAssignment(CampaignTransportType.TOW_TRANSPORT)) {
            toReturn += "<br><i>" + "Towed by: ";
            toReturn += unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT).getTransport().getName();
            toReturn += "</i>";
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    /**
     * Returns a summary of the given Force in HTML format.
     *
     * @param force the Force to generate the summary for
     *
     * @return a summary of the Force in HTML format
     */
    public String getForceSummary(Force force) {
        int battleValue = 0;
        Money cost = Money.zero();
        double tonnage = 0;
        int number = 0;
        String commander = "No personnel found";

        for (UUID uid : force.getAllUnits(false)) {
            Unit unit = campaign.getUnit(uid);
            if (null != unit) {
                boolean crewExists = unit.getCommander() != null;
                battleValue += unit.getEntity().calculateBattleValue(true, !crewExists);
                cost = cost.plus(unit.getEntity().getCost(true));
                tonnage += unit.getEntity().getWeight();
                number++;
            }
        }

        if (force.getForceCommanderID() != null) {
            Person forceCommander = campaign.getPerson(force.getForceCommanderID());

            if (forceCommander != null) {
                commander = forceCommander.getFullTitle();
            } else {
                commander = "No Commander";
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("<html><font size='4'><b>")
              .append(force.getName())
              .append("</b> (")
              .append(commander)
              .append(")</font><br/>");
        summary.append("<font>");
        appendSummary(summary, "Number of Units", number);
        appendSummary(summary, "BV", battleValue);
        appendSummary(summary, "Tonnage", DecimalFormat.getInstance().format(tonnage));
        appendSummary(summary, "Value", cost.toAmountAndSymbolString());
        summary.append("</font></html>");

        return summary.toString();
    }

    /**
     * Appends a summary line to the provided StringBuilder.
     *
     * @param string    the StringBuilder to append the summary line to
     * @param attribute the attribute name to display in bold
     * @param value     the value associated with the attribute
     */
    private void appendSummary(StringBuilder string, String attribute, Object value) {
        string.append("<b>").append(attribute).append(":</b> ").append(value).append("<br/>");
    }
}
