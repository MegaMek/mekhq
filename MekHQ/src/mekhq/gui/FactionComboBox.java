/*
 * FactionComboBox.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import mekhq.campaign.universe.Faction;
import mekhq.gui.model.SortedComboBoxModel;

/**
 * Combo box for choosing a faction by full name that accounts for
 * the fact that full names are not always unique within the faction's
 * era.
 * 
 * @author Neoancient
 *
 */
public class FactionComboBox extends JComboBox<Map.Entry<String, String>> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1352706316707722054L;

	@SuppressWarnings("serial")
	public FactionComboBox() {
		Comparator<Map.Entry<String, String>> comp = new Comparator<Map.Entry<String, String>>() {

			@Override
			public int compare(Entry<String, String> arg0,
					Entry<String, String> arg1) {
				return arg0.getValue().compareTo(arg1.getValue());
			}
			
		};
		setModel(new SortedComboBoxModel<Map.Entry<String, String>>(comp));
		setRenderer(new DefaultListCellRenderer() {
			@Override
			public  Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value != null) {
					setText((String)((Map.Entry<?, ?>)value).getValue());
				}
				return this;
			}			
		});
	}
	
	public void addFactionEntries(Collection<String> list, int year) {
		HashMap<String, String> map = new HashMap<String, String>();
		HashSet<String> collisions = new HashSet<String>();
		for (String key : list) {
			String fullName = Faction.getFaction(key).getFullName(year);
			if (map.containsValue(fullName)) {
				collisions.add(fullName);
			}
			map.put(key, fullName);
		}
		for (String key : map.keySet()) {
			if (collisions.contains(map.get(key))) {
				map.put(key, map.get(key) + " (" + key + ")");
			}
		}
		for (Map.Entry<String, String> entry : map.entrySet()) {
			addItem(entry);
		}
	}

	public String getSelectedItemKey() {
		if (getSelectedItem() == null) {
			return null;
		}
		return (String)((Map.Entry<?, ?>)getSelectedItem()).getKey();
	}

	public String getSelectedItemValue() {
		if (getSelectedItem() == null) {
			return null;
		}
		return (String)((Map.Entry<?, ?>)getSelectedItem()).getValue();
	}
	
	public void setSelectedItemByKey(String key) {
		for (int i = 0; i < getModel().getSize(); i++) {
			if (key.equals(((Map.Entry<String, String>)(getModel().getElementAt(i))).getKey())) {
				setSelectedIndex(i);
				return;
			}
		}
	}
}
