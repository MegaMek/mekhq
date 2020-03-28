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
package mekhq.gui;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import mekhq.online.forces.RemoteForce;
import mekhq.online.forces.RemoteUnit;

public class RemoteForceRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);
        setBackground(UIManager.getColor("Tree.background"));
        setForeground(UIManager.getColor("Tree.textForeground"));
        if (sel) {
            setBackground(UIManager.getColor("Tree.selectionBackground"));
            setForeground(UIManager.getColor("Tree.selectionForeground"));
        }

        if (value instanceof RemoteUnit) {
            RemoteUnit u = (RemoteUnit)value;
            String name = u.getCommander();
            if (name == null) {
                name = "<font color='red'>No Crew</font>";
            }
            String uname = "<i>" + u.getName() + "</i>";

            setText("<html>" + name + ", " + uname + "</html>");
        }
        if (value instanceof RemoteForce) {
            RemoteForce f = (RemoteForce)value;

            setText("<html>" + f.getName() + "</html>");
        }

        return this;
    }
}
