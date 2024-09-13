/*
 * Copyright (C) 2016 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

public class ChooseFactionsDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(ChooseFactionsDialog.class);

    private LocalDate date;

    private JList<Faction> factionList;
    private List<String> result;
    private boolean changed;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
            "mekhq.resources.ChooseFactionsDialog",
            MekHQ.getMHQOptions().getLocale());

    public ChooseFactionsDialog(final JFrame frame, final LocalDate date,
            final List<String> defaults) {
        this(frame, true, date, defaults);
    }

    public ChooseFactionsDialog(final JFrame frame, final boolean modal, final LocalDate date,
            final List<String> defaults) {
        super(frame, modal);
        this.date = Objects.requireNonNull(date);
        this.result = defaults;
        this.changed = false;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    protected void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("form");
        setTitle(resourceMap.getString("Form.title"));
        setPreferredSize(new Dimension(400, 500));

        final Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane();
        factionList = new JList<>(new FactionListModel(date));
        factionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                DefaultListCellRenderer result = (DefaultListCellRenderer) super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);
                if (value instanceof Faction) {
                    result.setText(((Faction) value).getFullName(date.getYear()));
                }
                return result;
            }

        });
        scrollPane.setViewportView(factionList);
        content.add(scrollPane, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        content.add(new JButton(new AbstractAction(resourceMap.getString("ok.label")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = new ArrayList<>();
                for (Faction faction : factionList.getSelectedValuesList()) {
                    result.add(faction.getShortName());
                }
                changed = true;
                setVisible(false);
            }
        }), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("cancel.label")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }), gbc);
        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChooseFactionsDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    public List<String> getResult() {
        return result;
    }

    public boolean isChanged() {
        return changed;
    }

    private static class FactionListModel extends AbstractListModel<Faction> {
        private TreeMap<String, Faction> factionMap = new TreeMap<>();
        private List<String> names;

        public FactionListModel(LocalDate date) {
            for (Faction faction : Factions.getInstance().getFactions()) {
                factionMap.put(faction.getFullName(date.getYear()), faction);
            }
            names = new ArrayList<>(factionMap.navigableKeySet());
        }

        @Override
        public int getSize() {
            return names.size();
        }

        @Override
        public Faction getElementAt(int index) {
            return factionMap.get(names.get(index));
        }
    }
}
