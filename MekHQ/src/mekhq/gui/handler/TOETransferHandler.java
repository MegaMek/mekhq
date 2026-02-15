/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.UUID;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

public class TOETransferHandler extends TransferHandler {
    private static final MMLogger LOGGER = MMLogger.create(TOETransferHandler.class);

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
            return new StringSelection("UNIT|" + ((Unit) node).getId().toString());
        } else if (node instanceof Formation) {
            return new StringSelection("FORCE|" + ((Formation) node).getId());
        } else {
            return null;
        }
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
            superFormation = gui.getCampaign().getFormation(((Unit) parent).getFormationId());
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
                formation = gui.getCampaign().getFormation(Integer.parseInt(id));
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
        Unit unit = null;
        Formation formation = null;
        Transferable t = support.getTransferable();
        try {
            StringTokenizer st = new StringTokenizer((String) t.getTransferData(DataFlavor.stringFlavor), "|");
            String type = st.nextToken();
            String id = st.nextToken();
            if (type.equals("UNIT")) {
                unit = gui.getCampaign().getUnit(UUID.fromString(id));
                if (unit == null || unit.isDeployed()) {
                    return false;
                }
            }
            if (type.equals("FORCE")) {
                formation = gui.getCampaign().getFormation(Integer.parseInt(id));
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
            superFormation = gui.getCampaign().getFormation(((Unit) parent).getFormationId());
        }

        if (superFormation != null) {
            if (unit != null) {
                gui.getCampaign().addUnitToFormation(unit, superFormation.getId());
                return true;
            }
            if (formation != null) {
                gui.getCampaign().moveFormation(formation, superFormation);
                return true;
            }
        }
        return false;
    }
}
