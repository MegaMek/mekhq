package mekhq.gui.handler;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
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

public class OrgTreeTransferHandler extends TransferHandler {

    /**
     *
     */
    private static final long serialVersionUID = -1276891849078287710L;
    private CampaignGUI gui;

    public OrgTreeTransferHandler(CampaignGUI gui) {
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
            Object node = ((JTree)c).getLastSelectedPathComponent();
            if (node instanceof Unit) {
                MekHQ.triggerEvent(new OrganizationChangedEvent((Unit)node));
            } else if (node instanceof Force) {
                MekHQ.triggerEvent(new OrganizationChangedEvent((Force)node));
            }
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        Object node = tree.getLastSelectedPathComponent();
        if (node instanceof Unit) {
            return new StringSelection("UNIT|"
                    + ((Unit) node).getId().toString());
        } else if (node instanceof Force) {
            return new StringSelection("FORCE|"
                    + Integer.toString(((Force) node).getId()));
        }
        return null;
    }

    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false;
        }
        // Extract transfer data.
        @SuppressWarnings("unused")
        // FIXME
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
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Do not allow a drop on the drag source selections.
        JTree.DropLocation dl = (JTree.DropLocation) support
                .getDropLocation();
        JTree tree = (JTree) support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        for (int i = 0; i < selRows.length; i++) {
            if (selRows[i] == dropRow) {
                return false;
            }
        }
        TreePath dest = dl.getPath();
        Object parent = dest.getLastPathComponent();
        Force superForce = null;
        if (parent instanceof Force) {
            superForce = (Force) parent;
        } else if (parent instanceof Unit) {
            superForce = gui.getCampaign().getForce(
                    ((Unit) parent).getForceId());
        }
        if (null != force && null != superForce
                && force.isAncestorOf(superForce)) {
            return false;
        }

        return parent instanceof Force || parent instanceof Unit;
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        // Extract transfer data.
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
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support
                .getDropLocation();
        TreePath dest = dl.getPath();
        Force superForce = null;
        Object parent = dest.getLastPathComponent();
        if (parent instanceof Force) {
            superForce = (Force) parent;
        } else if (parent instanceof Unit) {
            superForce = gui.getCampaign().getForce(
                    ((Unit) parent).getForceId());
        }
        if (null != superForce) {
            if (null != unit) {
                gui.getCampaign().addUnitToForce(unit, superForce.getId());
                return true;
            }
            if (null != force) {
                gui.getCampaign().moveForce(force, superForce);
                return true;
            }
        }
        return false;
    }
}
