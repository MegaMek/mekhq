/*
 * Copyright (c) 2013-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

import static mekhq.campaign.force.Force.COMBAT_TEAM_OVERRIDE_NONE;

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
                    MekHQ.getMHQOptions().getFontColorNegativeHexColor(), "No Crew");
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
                            MekHQ.getMHQOptions().getFontColorNegativeHexColor(), name);
                }
            }
            String unitName = "<i>" + unit.getName() + "</i>";
            if (unit.isDamaged()) {
                unitName = ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorNegativeHexColor(), unitName);
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
                transport.append("<br>Transported by: ")
                        .append(unit.getTransportShipAssignment().getTransportShip().getName());
            }
            String text = name + ", " + unitName + c3network + transport;

            Force force = unit.getCampaign().getForce(unit.getForceId());
            if((null != person) && (null != force) && (person.getId() == force.getForceCommanderID())) {
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

            String formattedForceName = String.format("<html>%s%s%s%s%s%s</html>",
                force.isCombatTeam() ? "<b>" : "",
                force.getOverrideCombatTeam() != COMBAT_TEAM_OVERRIDE_NONE ? "<u>" : "",
                force.getName(),
                force.isCombatTeam() ? "</b>" : "",
                force.getOverrideCombatTeam() != COMBAT_TEAM_OVERRIDE_NONE ? "</u>" : "",
                force.isConvoyForce() ? " &#926;" : force.isCombatForce() ? "" : " &#8709;");

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
