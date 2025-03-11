/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
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
    private Vector<TreeModelListener> listeners = new Vector<>();
    private Campaign campaign;

    public OrgTreeModel(Campaign c) {
        campaign = c;
        rootForce = campaign.getForces();
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof Force) {
            return ((Force) parent).getAllChildren(campaign).get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof Force) {
            return ((Force) parent).getAllChildren(campaign).size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof Force) {
            return ((Force) parent).getAllChildren(campaign).indexOf(child);
        }
        return 0;
    }

    @Override
    public Object getRoot() {
        return rootForce;
    }

    @Override
    public boolean isLeaf(Object node) {
        return (node instanceof Unit)
                || ((node instanceof Force) && ((Force) node).getAllChildren(campaign).isEmpty());
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {

    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        if ((listener != null) && !listeners.contains(listener)) {
            listeners.addElement(listener);
        }
    }

    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        if (listener != null) {
            listeners.removeElement(listener);
        }
    }
}
