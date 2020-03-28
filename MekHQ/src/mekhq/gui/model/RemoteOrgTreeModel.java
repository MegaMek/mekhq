/*
 * Copyright (c) 2020 The MegaMek Team.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import mekhq.online.forces.RemoteForce;
import mekhq.online.forces.RemoteTOE;
import mekhq.online.forces.RemoteUnit;

public class RemoteOrgTreeModel implements TreeModel {

    private RemoteForce rootForce;
    private Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();

    public RemoteOrgTreeModel() {
        rootForce = RemoteForce.emptyForce();
    }

    public RemoteOrgTreeModel(RemoteTOE toe) {
        rootForce = toe.getForces();
    }

    @Override
    public Object getChild(Object parent, int index) {
        if(parent instanceof RemoteForce) {
            return ((RemoteForce)parent).getAllChildren().get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if(parent instanceof RemoteForce) {
            return ((RemoteForce)parent).getAllChildren().size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if(parent instanceof RemoteForce) {
            return ((RemoteForce)parent).getAllChildren().indexOf(child);
        }
        return 0;
    }

    @Override
    public Object getRoot() {
        return rootForce;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof RemoteUnit
            || (node instanceof RemoteForce && ((RemoteForce)node).getAllChildren().size() == 0);
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
