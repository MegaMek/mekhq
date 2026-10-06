/*
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.force.Formation.COMBAT_TEAM_OVERRIDE_NONE;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.C3NetworkBadge;
import mekhq.gui.utilities.EnhancedImagingBadge;
import mekhq.utilities.ReportingUtilities;

public class ForceRenderer extends DefaultTreeCellRenderer {
    private static final MMLogger LOGGER = MMLogger.create(ForceRenderer.class);

    /**
     * Corner of the branch drawn before a carried or towed unit, matching the MegaMek lobby's
     * display of trains and loaded units. Escaped to keep the source plain ASCII.
     */
    private static final String BRANCH_CORNER = "\u2514";

    /** One length of branch. Repeated once per level, so a deeper load reads as a longer arm. */
    private static final String BRANCH_ARM = "\u2500";

    /** Stops a malformed transport chain from spinning while counting how deep a unit sits. */
    private static final int MAX_CARRIER_DEPTH = 16;

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
                        c3network += "<br>" + Messages.getString("ChatLounge.C3Slave") + " "
                                           + entity.getC3Master().getShortName();
                    }
                } else if (entity.getC3Master() != null) {
                    c3network += Messages.getString("ChatLounge.C3Slave") + " "
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
            if (unit.hasTacticalTransportAssignment()) {
                transport.append("<br>Transported (Tactical) by: ")
                      .append(unit.getTacticalTransportAssignment().getTransport().getName());
            }
            if (unit.hasTransportAssignment(CampaignTransportType.TOW_TRANSPORT)) {
                transport.append("<br>Towed by: ")
                      .append(unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT)
                                    .getTransport()
                                    .getName());
            }

            // The network is already named under the unit; the badge puts the same information where
            // it can be taken in without reading, so a formation sharing one network looks like one.
            String networkBadge = C3NetworkBadge.forEntity(entity);
            // Who is implanted is otherwise only visible by opening each warrior in turn, and EI
            // warriors come in whole formations, so the question is which formations are EI units.
            String imagingBadge = EnhancedImagingBadge.forUnit(unit);
            String text = networkBadge + imagingBadge + carriedBranch(unit) + name + ", "
                                + unitName + c3network + transport;

            mekhq.campaign.Campaign campaign = unit.getCampaign();
            int id = unit.getFormationId();
            Formation formation = campaign.getPlayerForce().getFormation(id);
            if ((null != person) &&
                      (null != formation) &&
                      (person.getId().equals(formation.getFormationCommanderID()))) {
                text = "<b>" + text + "</b>";
            }
            setText("<html>" + text + "</html>");
            getAccessibleContext().setAccessibleName((unit.isDeployed() ? "Deployed Unit: " : "Unit: ") + text);
            if (!sel && unit.isDeployed()) {
                setForeground(MekHQ.getMHQOptions().getDeployedForeground());
                setBackground(MekHQ.getMHQOptions().getDeployedBackground());
                setOpaque(true);
            }
        } else if (value instanceof Formation formation) {
            getAccessibleContext().setAccessibleName((
                  formation.isDeployed() ? "Deployed Force: " : "Force: ") + formation.getFullName());
            if (!sel && formation.isDeployed()) {
                setForeground(MekHQ.getMHQOptions().getDeployedForeground());
                setBackground(MekHQ.getMHQOptions().getDeployedBackground());
                setOpaque(true);
            }

            String formattedForceName = getFormattedForceName(formation);

            setText(formattedForceName);
        } else {
            LOGGER.error("Attempted to render node with unknown node class of {}",
                  (value != null) ? value.getClass() : "null");
        }

        setIcon(getIcon(value));

        return this;
    }

    /**
     * Returns the gray branch drawn in front of a unit that rides on another - carried in a bay or
     * towed behind a tractor - indented and lengthened by how deeply it sits, matching the MegaMek
     * lobby's train display. The second trailer of a train is drawn further in than the first, so a
     * train reads as a tree rather than a flat run of identical rows. Empty when the unit rides on
     * nothing.
     */
    private static String carriedBranch(Unit unit) {
        int depth = carriedDepth(unit);
        if (depth <= 0) {
            return "";
        }

        return UIUtil.fontHTML(UIUtil.uiGray())
                     + "&nbsp;".repeat(depth) + BRANCH_CORNER + BRANCH_ARM.repeat(depth) + "&nbsp;</font>";
    }

    /**
     * How many transports sit above this unit in the campaign's transport assignments: 1 for a
     * trailer hitched to a tractor, 2 for the second trailer in that train or for a trailer whose
     * tractor rides a transport itself.
     */
    private static int carriedDepth(Unit unit) {
        int depth = 0;
        Unit current = unit;

        while ((current != null) && (depth < MAX_CARRIER_DEPTH)) {
            Unit carrier = carrierOf(current);

            if ((carrier == null) || carrier.equals(current)) {
                break;
            }
            depth++;
            current = carrier;
        }

        return depth;
    }

    /** The unit this one rides on - transport bay, tactical carrier, or tow hitch - or {@code null} when none. */
    private static @Nullable Unit carrierOf(Unit unit) {
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            ITransportAssignment assignment = unit.getTransportAssignment(campaignTransportType);
            if ((assignment != null) && (assignment.getTransport() != null)) {
                return assignment.getTransport();
            }
        }

        return null;
    }

    private static String getFormattedForceName(Formation formation) {
        FormationType formationType = formation.getFormationType();
        String typeKey = formationType.getSymbol();

        return String.format("<html>%s%s%s%s%s%s%s</html>",
              formation.isCombatTeam() ? "<b>" : "",
              formation.getOverrideCombatTeam() != COMBAT_TEAM_OVERRIDE_NONE ? "<u>" : "",
              formation.getName(),
              formation.isCombatTeam() ? "</b>" : "",
              formation.getOverrideCombatTeam() != COMBAT_TEAM_OVERRIDE_NONE ? "</u>" : "",
              formation.isCombatTeam() ? " <s>c</s>" : "",
              typeKey);
    }

    protected Icon getIcon(Object node) {
        if (node instanceof Unit) {
            if (MekHQ.getMHQOptions().getShowUnitPicturesOnTOE()) {
                return new ImageIcon(((Unit) node).getImage(this));
            } else {
                final Person person = ((Unit) node).getCommander();
                return (person == null) ? null : person.getPortraitImageIconWithFallback(true, 58);
            }
        } else if (node instanceof Formation) {
            return ((Formation) node).getFormationIcon().getImageIcon(58);
        } else {
            return null;
        }
    }
}
