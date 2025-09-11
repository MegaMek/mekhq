/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

public class TOETransferHandler extends TransferHandler {
    private static final MMLogger logger = MMLogger.create(TOETransferHandler.class);

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
            } else if (node instanceof Force) {
                MekHQ.triggerEvent(new OrganizationChangedEvent((Force) node));
            }
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        Object node = tree.getLastSelectedPathComponent();
        if (node instanceof Unit) {
            return new StringSelection("UNIT|" + ((Unit) node).getId().toString());
        } else if (node instanceof Force) {
            return new StringSelection("FORCE|" + ((Force) node).getId());
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
        Force superForce = null;
        if (parent instanceof Force) {
            superForce = (Force) parent;
        } else if (parent instanceof Unit) {
            superForce = gui.getCampaign().getForce(((Unit) parent).getForceId());
        }

        // Extract transfer data.
        // FIXME : Unit does not work
        @SuppressWarnings(value = "unused")
        Unit unit = null;
        Force force = null;
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
                force = gui.getCampaign().getForce(Integer.parseInt(id));
            }
        } catch (UnsupportedFlavorException ufe) {
            logger.error("UnsupportedFlavor: " + ufe.getMessage());
        } catch (IOException ioe) {
            logger.error("I/O error: " + ioe.getMessage());
        }

        if ((force != null) && (superForce != null) && force.isAncestorOf(superForce)) {
            return false;
        }

        return (parent instanceof Force) || (parent instanceof Unit);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        Unit unit = null;
        Force force = null;
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
                force = gui.getCampaign().getForce(Integer.parseInt(id));
                if (force == null || force.isDeployed()) {
                    return false;
                }
            }
        } catch (UnsupportedFlavorException ufe) {
            logger.error("UnsupportedFlavor: " + ufe.getMessage());
        } catch (IOException ioe) {
            logger.error("I/O error: " + ioe.getMessage());
        }

        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        Force superForce = null;
        Object parent = dest.getLastPathComponent();

        if (parent instanceof Force) {
            superForce = (Force) parent;
        } else if (parent instanceof Unit) {
            superForce = gui.getCampaign().getForce(((Unit) parent).getForceId());
        }

        if (superForce != null) {
            if (unit != null) {
                gui.getCampaign().addUnitToForce(unit, superForce.getId());
                return true;
            }
            if (force != null) {
                gui.getCampaign().moveForce(force, superForce);
                return true;
            }
        }
        return false;
    }
}
