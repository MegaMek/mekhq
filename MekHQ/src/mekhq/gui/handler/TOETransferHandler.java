/*
 * Copyright (c) 2018-2020 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

public class TOETransferHandler extends TransferHandler {
    private static final long serialVersionUID = -1276891849078287710L;
    private CampaignGUI gui;

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
            MekHQ.getLogger().error("UnsupportedFlavor: " + ufe.getMessage());
        } catch (IOException ioe) {
            MekHQ.getLogger().error("I/O error: " + ioe.getMessage());
        }

        if ((force != null) && (superForce != null) && force.isAncestorOf(superForce)) {
            return false;
        }

        return (parent instanceof Force) || (parent instanceof Unit);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
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
            }
            if (type.equals("FORCE")) {
                force = gui.getCampaign().getForce(Integer.parseInt(id));
            }
        } catch (UnsupportedFlavorException ufe) {
            MekHQ.getLogger().error("UnsupportedFlavor: " + ufe.getMessage());
        } catch (IOException ioe) {
            MekHQ.getLogger().error("I/O error: " + ioe.getMessage());
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
