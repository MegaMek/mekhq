/*
 * Copyright (c) 2016 MegaMek team
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

import javax.swing.ComboBoxModel;

public class FilterableComboBoxModel<E> extends FilterableListModel<E> implements ComboBoxModel<E> {
    private static final long serialVersionUID = -8079476186183401700L;

    public FilterableComboBoxModel(ComboBoxModel<E> model) {
        super(model);
    }

    public ComboBoxModel<E> getComboBoxModel() {
        return (ComboBoxModel<E>) getModel();
    }

    @Override
    public void setSelectedItem(Object anItem) {
        getComboBoxModel().setSelectedItem(anItem);
    }

    @Override
    public Object getSelectedItem() {
        return getComboBoxModel().getSelectedItem();
    }

}
