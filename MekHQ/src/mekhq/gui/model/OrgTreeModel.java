package mekhq.gui.model;

import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;

public class OrgTreeModel implements TreeModel {

    private Force rootForce;
    private Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();
    private Campaign campaign;

    public OrgTreeModel(Campaign c) {
        campaign = c;
        rootForce = campaign.getForces();
    }

    @Override
    public Object getChild(Object parent, int index) {
        if(parent instanceof Force) {
            return ((Force)parent).getAllChildren(campaign).get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if(parent instanceof Force) {
            return ((Force)parent).getAllChildren(campaign).size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if(parent instanceof Force) {
            return ((Force)parent).getAllChildren(campaign).indexOf(child);
        }
        return 0;
    }

    @Override
    public Object getRoot() {
        return rootForce;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof Unit || (node instanceof Force && ((Force)node).getAllChildren(campaign).size() == 0);
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    public void addTreeModelListener( TreeModelListener listener ) {
        if ( listener != null && !listeners.contains( listener ) ) {
            listeners.addElement( listener );
        }
    }

    public void removeTreeModelListener( TreeModelListener listener ) {
        if ( listener != null ) {
            listeners.removeElement( listener );
        }
    }
}
