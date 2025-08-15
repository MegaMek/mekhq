/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import static mekhq.campaign.force.Force.COMBAT_TEAM_OVERRIDE_NONE;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

public class ForceRenderer extends DefaultTreeCellRenderer {
    private static final MMLogger logger = MMLogger.create(ForceRenderer.class);

    // region Constructors
    public ForceRenderer() {

    }
    // endregion Constructors

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
          boolean expanded, boolean leaf, int row,
          boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setOpaque(false);

        if (value instanceof Unit unit) {
            String name = ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getNegativeColor(), "No Crew");
            if (unit.getEntity() instanceof GunEmplacement) {
                name = "AutoTurret";
            }
            String c3network = "";
            StringBuilder transport = new StringBuilder();
            Person person = unit.getCommander();
            if (person != null) {
                name = person.getFullTitle();
                name += " (" + unit.getEntity().getCrew().getGunnery() + '/'
                              + unit.getEntity().getCrew().getPiloting() + ')';
                if (person.needsFixing() || (unit.getEntity().getCrew().getHits() > 0)) {
                    name = ReportingUtilities.messageSurroundedBySpanWithColor(
                          ReportingUtilities.getNegativeColor(), name);
                }
            }
            String unitName = "<i>" + unit.getName() + "</i>";
            if (unit.isDamaged()) {
                unitName = ReportingUtilities.messageSurroundedBySpanWithColor(
                      ReportingUtilities.getNegativeColor(), unitName);
            }

            Entity entity = unit.getEntity();
            if (entity.hasNavalC3()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.NC3None");
                } else {
                    c3network += Messages.getString("ChatLounge.NC3Network") + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.NC3Nodes",
                              entity.calculateFreeC3Nodes());
                    }
                }
            } else if (entity.hasC3i()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.C3iNone");
                } else {
                    c3network += Messages.getString("ChatLounge.C3iNetwork") + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.C3iNodes",
                              entity.calculateFreeC3Nodes());
                    }
                }
            } else if (entity.hasC3()) {
                if (entity.C3MasterIs(entity)) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    c3network += Messages.getString("ChatLounge.C3MNodes", entity.calculateFreeC3MNodes());
                    if (entity.hasC3MM()) {
                        c3network += Messages.getString("ChatLounge.C3SNodes",
                              entity.calculateFreeC3Nodes());
                    }
                } else if (!entity.hasC3S()) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    c3network += Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes());
                    // an independent master might also be a slave to a company master
                    if (entity.getC3Master() != null) {
                        c3network += "<br>" + Messages.getString("ChatLounge.C3Slave")
                                           + entity.getC3Master().getShortName();
                    }
                } else if (entity.getC3Master() != null) {
                    c3network += Messages.getString("ChatLounge.C3Slave")
                                       + entity.getC3Master().getShortName();
                } else {
                    c3network += Messages.getString("ChatLounge.C3None");
                }
            }

            if (!c3network.isEmpty()) {
                c3network = "<br><i>" + c3network + "</i>";
            }

            if (unit.hasTransportShipAssignment()) {
                transport.append("<br>Transported (Ship) by: ")
                      .append(unit.getTransportShipAssignment().getTransportShip().getName());
            }
            String tacticalTransport = "";
            if (unit.hasTacticalTransportAssignment()) {
                transport.append("<br>Transported (Tactical) by: ")
                      .append(unit.getTacticalTransportAssignment().getTransport().getName());
            }
            String towTransport = "";
            if (unit.hasTransportAssignment(CampaignTransportType.TOW_TRANSPORT)) {
                transport.append("<br>Towed by: ")
                      .append(unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT)
                                    .getTransport()
                                    .getName());
            }

            String text = name + ", " + unitName + c3network + transport + tacticalTransport + towTransport;

            Force force = unit.getCampaign().getForce(unit.getForceId());
            if ((null != person) && (null != force) && (person.getId().equals(force.getForceCommanderID()))) {
                text = "<b>" + text + "</b>";
            }
            setText("<html>" + text + "</html>");
            getAccessibleContext().setAccessibleName((unit.isDeployed() ? "Deployed Unit: " : "Unit: ") + text);
            if (!sel && unit.isDeployed()) {
                setForeground(MekHQ.getMHQOptions().getDeployedForeground());
                setBackground(MekHQ.getMHQOptions().getDeployedBackground());
                setOpaque(true);
            }
        } else if (value instanceof Force force) {
            getAccessibleContext().setAccessibleName((
                  force.isDeployed() ? "Deployed Force: " : "Force: ") + force.getFullName());
            if (!sel && force.isDeployed()) {
                setForeground(MekHQ.getMHQOptions().getDeployedForeground());
                setBackground(MekHQ.getMHQOptions().getDeployedBackground());
                setOpaque(true);
            }

            ForceType forceType = force.getForceType();
            String typeKey = forceType.getSymbol();

            String formattedForceName = String.format("<html>%s%s%s%s%s%s%s</html>",
                  force.isCombatTeam() ? "<b>" : "",
                  force.getOverrideCombatTeam() != COMBAT_TEAM_OVERRIDE_NONE ? "<u>" : "",
                  force.getName(),
                  force.isCombatTeam() ? "</b>" : "",
                  force.getOverrideCombatTeam() != COMBAT_TEAM_OVERRIDE_NONE ? "</u>" : "",
                  force.isCombatTeam() ? " <s>c</s>" : "",
                  typeKey);

            setText(formattedForceName);
        } else {
            logger.error("Attempted to render node with unknown node class of "
                               + ((value != null) ? value.getClass() : "null"));
        }

        setIcon(getIcon(value));

        return this;
    }

    protected Icon getIcon(Object node) {
        if (node instanceof Unit) {
            if (MekHQ.getMHQOptions().getShowUnitPicturesOnTOE()) {
                return new ImageIcon(((Unit) node).getImage(this));
            } else {
                final Person person = ((Unit) node).getCommander();
                return (person == null) ? null : person.getPortrait().getImageIcon(58);
            }
        } else if (node instanceof Force) {
            return ((Force) node).getForceIcon().getImageIcon(58);
        } else {
            return null;
        }
    }
}
