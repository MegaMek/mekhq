/*
 * Copyright (c) 2011-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * A custom panel that gets filled in with goodies from a Force record
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ForceViewPanel extends JScrollablePanel {
    private Force force;
    private Campaign campaign;

    private javax.swing.JLabel lblIcon;
    private javax.swing.JPanel pnlStats;
    private javax.swing.JPanel pnlSubUnits;
    private javax.swing.JTextPane txtDesc;

    private javax.swing.JLabel lblType;
    private javax.swing.JLabel lblAssign1;
    private javax.swing.JLabel lblAssign2;
    private javax.swing.JLabel lblCommander1;
    private javax.swing.JLabel lblCommander2;
    private javax.swing.JLabel lblBV1;
    private javax.swing.JLabel lblBV2;
    private javax.swing.JLabel lblTonnage1;
    private javax.swing.JLabel lblTonnage2;
    private javax.swing.JLabel lblCost1;
    private javax.swing.JLabel lblCost2;
    private javax.swing.JLabel lblTech1;
    private javax.swing.JLabel lblTech2;

    public ForceViewPanel(Force f, Campaign c) {
        super();
        this.force = f;
        this.campaign = c;
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        getAccessibleContext().setAccessibleName("Selected Force: " + force.getFullName());

        lblIcon = new javax.swing.JLabel();
        pnlStats = new javax.swing.JPanel();
        pnlSubUnits = new javax.swing.JPanel();
        txtDesc = new javax.swing.JTextPane();

        setLayout(new java.awt.GridBagLayout());

        lblIcon.setIcon(force.getForceIcon().getImageIcon(150));
        lblIcon.setName("lblIcon");
        lblIcon.getAccessibleContext().setAccessibleName("Force Icon");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(10, 10, 0, 0);
        add(lblIcon, gridBagConstraints);

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(force.getName()));
        fillStats();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        pnlSubUnits.setName("pnlSubUnits");
        pnlSubUnits.getAccessibleContext().setAccessibleName("Force Composition");
        fillSubUnits();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlSubUnits, gridBagConstraints);

        if (null != force.getDescription() && !force.getDescription().isEmpty()) {
            txtDesc.setName("txtDesc");
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(force.getDescription()));
            txtDesc.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Description"),
                    BorderFactory.createEmptyBorder(0,2,2,2)));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            add(txtDesc, gridBagConstraints);
        }
    }

    private void fillStats() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ForceViewPanel",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());

        lblType = new javax.swing.JLabel();
        lblAssign1 = new javax.swing.JLabel();
        lblAssign2 = new javax.swing.JLabel();
        lblCommander1 = new javax.swing.JLabel();
        lblCommander2 = new javax.swing.JLabel();
        lblBV1 = new javax.swing.JLabel();
        lblBV2 = new javax.swing.JLabel();
        lblCost1 = new javax.swing.JLabel();
        lblCost2 = new javax.swing.JLabel();
        lblTonnage1 = new javax.swing.JLabel();
        lblTonnage2 = new javax.swing.JLabel();
        lblTech1 = new javax.swing.JLabel();
        lblTech2 = new javax.swing.JLabel();
        java.awt.GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new java.awt.GridBagLayout());

        pnlStats.getAccessibleContext().setAccessibleName("Force Statistics");

        long bv = 0;
        Money cost = Money.zero();
        double ton = 0;
        String commander = "";
        String lanceTech = "";
        String assigned = "";
        String type = null;
        ArrayList<Person> people = new ArrayList<>();
        for (UUID uid : force.getAllUnits(false)) {
            Unit u = campaign.getUnit(uid);
            if (null != u) {
                Person p = u.getCommander();
                bv += u.getEntity().calculateBattleValue(true, !u.hasPilot());
                cost = cost.plus(u.getEntity().getCost(true));
                ton += u.getEntity().getWeight();
                String utype = UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                if (null == type) {
                    type = utype;
                } else if (!utype.equals(type)) {
                    type = resourceMap.getString("mixed");
                }
                if (null != p) {
                    people.add(p);
                }
            }
        }

        // sort person vector by rank
        people.sort((p1, p2) -> ((Comparable<Integer>) p2.getRankNumeric()).compareTo(p1.getRankNumeric()));
        if (!people.isEmpty()) {
            commander = people.get(0).getFullTitle();
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
            lblType.setName("lblCommander2");
            String forceType = (force.isCombatForce() ? "" : "Non-Combat ") + type + " " + resourceMap.getString("unit");
            lblType.setText("<html><i>" + forceType + "</i></html>");
            lblType.getAccessibleContext().setAccessibleDescription("Force Type: " + forceType);
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

        if (!commander.isBlank()) {
            lblCommander1.setName("lblCommander1");
            lblCommander1.setText(resourceMap.getString("lblCommander1.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCommander1, gridBagConstraints);

            lblCommander2.setName("lblCommander2");
            lblCommander2.setText(commander);
            lblCommander1.setLabelFor(lblCommander2);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCommander2, gridBagConstraints);
            nexty++;
        }
        if (null != force.getTechID()) {
            if (!lanceTech.isBlank()) {
                lblTech1.setName("lblTech1");
                lblTech1.setText(resourceMap.getString("lblTech1.text"));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = nexty;
                gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                pnlStats.add(lblTech1, gridBagConstraints);

                lblTech2.setName("lblTech2");
                lblTech2.setText(lanceTech);
                lblTech1.setLabelFor(lblTech2);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = nexty;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                pnlStats.add(lblTech2, gridBagConstraints);
                nexty++;
                }
        }

        if (!assigned.isBlank()) {
            lblAssign1.setName("lblAssign1");
            lblAssign1.setText(resourceMap.getString("lblAssign1.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(lblAssign1, gridBagConstraints);

            lblAssign2.setName("lblAssign2");
            lblAssign2.setText(assigned);
            lblAssign1.setLabelFor(lblAssign2);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(lblAssign2, gridBagConstraints);
            nexty++;
        }

        lblBV1.setName("lblBV1");
        lblBV1.setText(resourceMap.getString("lblBV1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBV1, gridBagConstraints);

        lblBV2.setName("lblBV2");
        lblBV2.setText(DecimalFormat.getInstance().format(bv));
        lblBV1.setLabelFor(lblBV1);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBV2, gridBagConstraints);
        nexty++;

        lblTonnage1.setName("lblTonnage1");
        lblTonnage1.setText(resourceMap.getString("lblTonnage1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTonnage1, gridBagConstraints);

        lblTonnage2.setName("lblTonnage2");
        lblTonnage2.setText(DecimalFormat.getInstance().format(ton));
        lblTonnage1.setLabelFor(lblTonnage2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTonnage2, gridBagConstraints);
        nexty++;

        // if AtB is enabled, set tooltip to show lance weight breakdowns
        if (campaign.getCampaignOptions().getUseAtB()) {
            // see Lance.java for lance weight breakdowns
            lblTonnage1.setToolTipText(resourceMap.getString("tonnageToolTip.text"));
            lblTonnage2.setToolTipText(resourceMap.getString("tonnageToolTip.text"));
        }

        lblCost1.setName("lblCost1");
        lblCost1.setText(resourceMap.getString("lblCost1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCost1, gridBagConstraints);

        lblCost2.setName("lblCost2");
        lblCost2.setText(cost.toAmountAndSymbolString());
        lblCost1.setLabelFor(lblCost2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = nexty;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
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
            lblForce.setText(getSummaryFor(subForce));
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
        units.sort((u1, u2) -> ((Comparable<Integer>) u2.getCommander().getRankNumeric()).compareTo(u1.getCommander().getRankNumeric()));
        units.addAll(unmannedUnits);
        for (Unit unit : units) {
            Person p = unit.getCommander();
            lblPerson = new JLabel();
            lblUnit = new JLabel();
            if (null != p) {
                lblPerson.setText(getSummaryFor(p, unit));
                lblPerson.setIcon(p.getPortrait().getImageIcon());
            } else {
                lblPerson.getAccessibleContext().setAccessibleName("Unmanned Unit");
                  }
            nexty++;
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlSubUnits.add(lblPerson, gridBagConstraints);
            lblUnit.setText(getSummaryFor(unit));
            lblUnit.setIcon(new ImageIcon(unit.getImage(lblUnit)));
            lblPerson.setLabelFor(lblUnit);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = nexty;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlSubUnits.add(lblUnit, gridBagConstraints);
        }
    }

    public String getSummaryFor(Person person, Unit unit) {
        String toReturn = "<html><font size='2'><b>" + person.getFullTitle() + "</b><br/>";
        toReturn += person.getSkillSummary(campaign) + " " + person.getRoleDesc();
        if (null != unit && null != unit.getEntity()
                && null != unit.getEntity().getCrew() && unit.getEntity().getCrew().getHits() > 0) {
            toReturn += "<br><font color='red' size='2'>" + unit.getEntity().getCrew().getHits() + " hit(s)";
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    public String getSummaryFor(Unit unit) {
        String toReturn = "<html><font size='3'><b>" + unit.getName() + "</b></font><br/>";
        toReturn += "<font size='2'><b>BV:</b> " + unit.getEntity().calculateBattleValue(true, null == unit.getEntity().getCrew()) + "<br/>";
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
            toReturn += "<br><i>" + "Transported by: ";
            toReturn += unit.getTransportShipAssignment().getTransportShip().getName();
            toReturn += "</i>";
        }
        // If this is a transport ship, tell us what bay capacity is at
        if (!unit.getEntity().getTransportBays().isEmpty()) {
            int veeTotal = (int) (unit.getCurrentLightVehicleCapacity() + unit.getCurrentHeavyVehicleCapacity() + unit.getCurrentSuperHeavyVehicleCapacity());
            int aeroTotal = (int) (unit.getCurrentASFCapacity() + unit.getCurrentSmallCraftCapacity());
            if (unit.getCurrentMechCapacity() > 0) {
                toReturn += "<br><i>" + "Mech Bays: " + (int) unit.getCurrentMechCapacity() + " free.</i>";
            }
            if (veeTotal > 0) {
                toReturn += "<br><i>" + "Vehicle Bays: " + veeTotal + " free.</i>";
            }
            if (aeroTotal > 0) {
                toReturn += "<br><i>" + "ASF/SC Bays: " + aeroTotal + " free.</i>";
            }
            if (unit.getCurrentProtomechCapacity() > 0) {
                toReturn += "<br><i>" + "ProtoMech Bays: " + (int) unit.getCurrentProtomechCapacity() + " free.</i>";
            }
            if (unit.getCurrentBattleArmorCapacity() > 0) {
                toReturn += "<br><i>" + "Battle Armor Bays: " + (int) unit.getCurrentBattleArmorCapacity() + " free.</i>";
            }
            if (unit.getCurrentInfantryCapacity() > 0) {
                toReturn += "<br><i>" + "Infantry Bays: " + (int) unit.getCurrentInfantryCapacity() + " tons free.</i>";
            }
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    public String getSummaryFor(Force f) {
        // we are not going to use the campaign methods here because we can be more efficient
        // by only traversing once
        int bv = 0;
        Money cost = Money.zero();
        double ton = 0;
        int number = 0;
        String commander = "No personnel found";
        ArrayList<Person> people = new ArrayList<>();
        for (UUID uid : f.getAllUnits(false)) {
            Unit u = campaign.getUnit(uid);
            if (null != u) {
                Person p = u.getCommander();
                number++;
                if (p != null) {
                    bv += u.getEntity().calculateBattleValue(true, false);
                } else {
                    bv += u.getEntity().calculateBattleValue(true, true);
                }
                cost = cost.plus(u.getEntity().getCost(true));
                ton += u.getEntity().getWeight();
                if (p != null) {
                    people.add(p);
                }
            }
        }
        // sort person vector by rank
        people.sort((p1, p2) -> ((Comparable<Integer>) p2.getRankNumeric()).compareTo(p1.getRankNumeric()));
        if (!people.isEmpty()) {
            commander = people.get(0).getFullTitle();
        }
        String toReturn = "<html><font size='2'><b>" + f.getName() + "</b> (" + commander + ")<br/>";
        toReturn += "<b>Number of Units:</b> " + number + "<br/>";
        toReturn += bv + " BV, ";
        toReturn += DecimalFormat.getInstance().format(ton) + " tons, ";
        toReturn += cost.toAmountAndSymbolString();
        toReturn += "</font></html>";
        return toReturn;
    }
}
