/*
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.handler;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

public class TOETransferHandler extends TransferHandler {
    private static final MMLogger LOGGER = MMLogger.create(TOETransferHandler.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.TOETransferHandler";

    private final CampaignGUI gui;

    public TOETransferHandler(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public void exportDone(JComponent c, Transferable t, int action) {
        if (action == MOVE) {
            Object node = ((JTree) c).getLastSelectedPathComponent();
            if (node instanceof Unit) {
                MekHQ.triggerEvent(new OrganizationChangedEvent((Unit) node));
            } else if (node instanceof Formation) {
                MekHQ.triggerEvent(new OrganizationChangedEvent((Formation) node));
            }
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        Object node = tree.getLastSelectedPathComponent();
        if (node instanceof Unit) {
            // Carry every selected unit, not just the lead selection, so multi-unit drags move the whole selection
            StringBuilder transferData = new StringBuilder("UNIT");
            for (Unit unit : getSelectedUnits(tree)) {
                if (unit.isDeployed()) {
                    showCannotMoveDialog(c, unit.getEntity().getShortName());
                    return null;
                }
                transferData.append('|').append(unit.getId());
            }
            return new StringSelection(transferData.toString());
        } else if (node instanceof Formation formation) {
            if (formation.isDeployed()) {
                showCannotMoveDialog(c, formation.toString());
                return null;
            }
            return new StringSelection("FORCE|" + formation.getId());
        } else {
            return null;
        }
    }

    private static List<Unit> getSelectedUnits(JTree tree) {
        List<Unit> units = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof Unit unit) {
                    units.add(unit);
                }
            }
        }
        return units;
    }

    /**
     * A method that displays a dialog for units or formations that cannot be moved in the
     * TO&E because they are already deployed.
     *
     */
    private void showCannotMoveDialog(JComponent parent, String unit_formation_string) {
        JOptionPane.showMessageDialog(
            parent,
            getFormattedTextAt(RESOURCE_BUNDLE, "popup.TOETransferHandler.cannotMove", unit_formation_string),
            getFormattedTextAt(RESOURCE_BUNDLE, "popup.TOETransferHandler.cannotMoveTitle", unit_formation_string),
            JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        if (dl.getPath() == null) {
            return false;
        }

        // Do not allow a drop on the drag source selections.
        JTree tree = (JTree) support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        if (selRows != null) {
            for (int selRow : selRows) {
                if (selRow == dropRow) {
                    return false;
                }
            }
        }

        Object parent = dl.getPath().getLastPathComponent();
        Formation superFormation = null;
        if (parent instanceof Formation) {
            superFormation = (Formation) parent;
        } else if (parent instanceof Unit) {
            Campaign campaign = gui.getCampaign();
            int id = ((Unit) parent).getFormationId();
            superFormation = campaign.getPlayerForce().getFormation(id);
        }

        // Extract transfer data.
        // FIXME : Unit does not work
        @SuppressWarnings(value = "unused")
        Unit unit = null;
        Formation formation = null;
        Transferable t = support.getTransferable();
        try {
            StringTokenizer st = new StringTokenizer(
                  (String) t.getTransferData(DataFlavor.stringFlavor),
                  "|");
            String type = st.nextToken();
            String id = st.nextToken();
            if (type.equals("UNIT")) {
                unit = gui.getCampaign().getUnit(UUID.fromString(id));
            }
            if (type.equals("FORCE")) {
                Campaign campaign = gui.getCampaign();
                formation = campaign.getPlayerForce().getFormation(Integer.parseInt(id));
            }
        } catch (UnsupportedFlavorException ufe) {
            LOGGER.error("UnsupportedFlavor: {}", ufe.getMessage());
        } catch (IOException ioe) {
            LOGGER.error("I/O error: {}", ioe.getMessage());
        }

        if ((formation != null) && (superFormation != null) &&
                  (formation.isAncestorOf(superFormation) || formation.equals(superFormation))) {
            return false;
        }

        return (parent instanceof Formation) || (parent instanceof Unit);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        List<Unit> units = new ArrayList<>();
        Formation formation = null;
        Transferable t = support.getTransferable();
        try {
            StringTokenizer st = new StringTokenizer((String) t.getTransferData(DataFlavor.stringFlavor), "|");
            String type = st.nextToken();
            if (type.equals("UNIT")) {
                // Validate the whole selection before moving anything, so a bad drop doesn't leave it half-moved
                while (st.hasMoreTokens()) {
                    Unit unit = gui.getCampaign().getUnit(UUID.fromString(st.nextToken()));
                    if (unit == null || unit.isDeployed()) {
                        return false;
                    }
                    units.add(unit);
                }
            }
            if (type.equals("FORCE")) {
                Campaign campaign = gui.getCampaign();
                formation = campaign.getPlayerForce().getFormation(Integer.parseInt(st.nextToken()));
                if (formation == null || formation.isDeployed()) {
                    return false;
                }
            }
        } catch (UnsupportedFlavorException ufe) {
            LOGGER.error("UnsupportedFlavor: {}", ufe.getMessage());
        } catch (IOException ioe) {
            LOGGER.error("I/O error: {}", ioe.getMessage());
        }

        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        Formation superFormation = null;
        Object parent = dest.getLastPathComponent();

        if (parent instanceof Formation) {
            superFormation = (Formation) parent;
        } else if (parent instanceof Unit) {
            Campaign campaign = gui.getCampaign();
            int id = ((Unit) parent).getFormationId();
            superFormation = campaign.getPlayerForce().getFormation(id);
        }

        if (superFormation != null) {
            if (!units.isEmpty()) {
                Campaign campaign = gui.getCampaign();
                int id = superFormation.getId();
                for (Unit unit : units) {
                    campaign.getPlayerForce().addUnitToFormation(unit, id, campaign);
                }
                return true;
            }
            if (formation != null) {
                mekhq.campaign.Campaign campaign = gui.getCampaign();
                campaign.getPlayerForce().moveFormation(formation, superFormation, campaign);
                return true;
            }
        }
        return false;
    }
}
