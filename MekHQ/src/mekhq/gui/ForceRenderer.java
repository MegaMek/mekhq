/*
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.*;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class ForceRenderer extends DefaultTreeCellRenderer {
    //region Variable Declarations
    private static final long serialVersionUID = -553191867660269247L;
    //endregion Variable Declarations

    //region Constructors
    public ForceRenderer() {

    }
    //endregion Constructors

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                  boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setOpaque(false);

        if (value instanceof Unit) {
            String name = "<font color='red'>No Crew</font>";
            if (((Unit) value).getEntity() instanceof GunEmplacement) {
                name = "AutoTurret";
            }
            String uname;
            String c3network = "";
            StringBuilder transport = new StringBuilder();
            Unit u = (Unit) value;
            Person person = u.getCommander();
            if (person != null) {
                name = person.getFullTitle();
                name += " (" + u.getEntity().getCrew().getGunnery() + "/"
                        + u.getEntity().getCrew().getPiloting() + ")";
                if (person.needsFixing() || (u.getEntity().getCrew().getHits() > 0)) {
                    name = "<font color='red'>" + name + "</font>";
                }
            }
            uname = "<i>" + u.getName() + "</i>";
            if (u.isDamaged()) {
                uname = "<font color='red'>" + uname + "</font>";
            }

            Entity entity = u.getEntity();
            if (entity.hasNavalC3()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.NC3None");
                } else {
                    c3network += Messages
                            .getString("ChatLounge.NC3Network")
                            + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.NC3Nodes",
                                entity.calculateFreeC3Nodes());
                    }
                }
            } else if (entity.hasC3i()) {
                if (entity.calculateFreeC3Nodes() >= 5) {
                    c3network += Messages.getString("ChatLounge.C3iNone");
                } else {
                    c3network += Messages.getString("ChatLounge.C3iNetwork")
                            + entity.getC3NetId();
                    if (entity.calculateFreeC3Nodes() > 0) {
                        c3network += Messages.getString("ChatLounge.C3iNodes",
                                entity.calculateFreeC3Nodes());
                    }
                }
            } else if (entity.hasC3()) {
                if (entity.C3MasterIs(entity)) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    c3network += Messages.getString("ChatLounge.C3MNodes",
                            entity.calculateFreeC3MNodes());
                    if (entity.hasC3MM()) {
                        c3network += Messages.getString("ChatLounge.C3SNodes",
                                entity.calculateFreeC3Nodes());
                    }
                } else if (!entity.hasC3S()) {
                    c3network += Messages.getString("ChatLounge.C3Master");
                    c3network += Messages.getString("ChatLounge.C3SNodes",
                            entity.calculateFreeC3Nodes());
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

            if (u.hasTransportShipAssignment()) {
                transport.append("<br>Transported by: ")
                        .append(u.getTransportShipAssignment().getTransportShip().getName());
            }
            String text = name + ", " + uname + c3network + transport;
            setText("<html>" + text + "</html>");
            getAccessibleContext().setAccessibleName((u.isDeployed() ? "Deployed Unit: " : "Unit: ") + text);
            if (!sel && u.isDeployed()) {
                setForeground(MekHQ.getMekHQOptions().getDeployedForeground());
                setBackground(MekHQ.getMekHQOptions().getDeployedBackground());
                setOpaque(true);
            }
        } else if (value instanceof Force) {
            Force force = (Force) value;
            getAccessibleContext().setAccessibleName((force.isDeployed() ? "Deployed Force: " : "Force: ") + force.getFullName());
            if (!sel && force.isDeployed()) {
                setForeground(MekHQ.getMekHQOptions().getDeployedForeground());
                setBackground(MekHQ.getMekHQOptions().getDeployedBackground());
                setOpaque(true);
            }
        } else {
            MekHQ.getLogger().error("Attempted to render node with unknown node class of "
                    + ((value != null) ? value.getClass() : "null"));
        }

        setIcon(getIcon(value));

        return this;
    }

    protected Icon getIcon(Object node) {
        if (node instanceof Unit) {
            final Person person = ((Unit) node).getCommander();
            return (person == null) ? null : person.getPortrait().getImageIcon(58);
        } else if (node instanceof Force) {
            return getIconFrom((Force) node);
        } else {
            return null;
        }
    }

    protected Icon getIconFrom(Force force) {
        try {
            return new ImageIcon(MHQStaticDirectoryManager.buildForceIcon(force.getIconCategory(),
                    force.getIconFileName(), force.getIconMap())
                    .getScaledInstance(58, -1, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }
    }
}
