/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.gui.model.FilterableComboBoxModel;

/**
 * @author Ralgith
 */
public class EditInjuryEntryDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(EditInjuryEntryDialog.class);

    private Injury injury;

    private JButton                       btnClose;
    private JButton                       btnOK;
    private JTextArea                     txtDays;
    private JComboBox<BodyLocationChoice> ddLocation;
    private JComboBox<InjuryTypeChoice>   ddType;
    private JTextArea                     txtFluff;
    private JTextArea                     txtHits;
    private JComboBox<String>             ddPermanent;
    private JComboBox<String>             ddWorkedOn;
    private JComboBox<String>             ddExtended;
    private JPanel                        panBtn;
    private JPanel                        panMain;

    private BodyLocationChoice[]                      locations;
    private InjuryTypeChoice[]                        types;
    private FilterableComboBoxModel<InjuryTypeChoice> ddTypeModel;

    public EditInjuryEntryDialog(final JFrame frame, final boolean modal, final Injury injury) {
        super(frame, modal);
        this.injury = injury;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        locations = new BodyLocationChoice[BodyLocation.values().length];
        int i = 0;
        for (BodyLocation loc : BodyLocation.values()) {
            locations[i] = new BodyLocationChoice(loc);
            ++i;
        }

        ddLocation = new JComboBox<>(locations);
        for (BodyLocationChoice choice : locations) {
            if (injury.getLocation() == choice.loc) {
                ddLocation.setSelectedItem(choice);
                break;
            }
        }

        types = InjuryType.getAllTypes().stream().map(InjuryTypeChoice::new).toArray(InjuryTypeChoice[]::new);

        ddType      = new JComboBox<>(types);
        ddTypeModel = new FilterableComboBoxModel<>(ddType.getModel());
        ddTypeModel.setFilter(it -> {
            BodyLocation loc = ((BodyLocationChoice) Objects.requireNonNull(ddLocation.getSelectedItem())).loc;
            return it.type.isValidInLocation(loc);
        });
        ddType.setModel(ddTypeModel);

        for (InjuryTypeChoice choice : types) {
            if (injury.getType() == choice.type) {
                ddType.setSelectedItem(choice);
                break;
            }
        }

        txtDays  = new JTextArea();
        txtFluff = new JTextArea();
        txtHits  = new JTextArea();
        String[] tf = { "True", "False" };
        ddPermanent = new JComboBox<>(tf);
        ddWorkedOn  = new JComboBox<>(tf);
        ddExtended  = new JComboBox<>(tf);
        btnOK       = new JButton();
        btnClose    = new JButton();
        panBtn      = new JPanel();
        panMain     = new JPanel();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditInjuryEntryDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new BorderLayout());
        panBtn.setLayout(new GridLayout(0, 2));
        panMain.setLayout(new GridBagLayout());

        txtDays.setText(Integer.toString(injury.getTime()));
        txtDays.setName("txtDays");
        txtDays.setEditable(true);
        txtDays.setLineWrap(true);
        txtDays.setWrapStyleWord(true);
        txtDays.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Days Remaining"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtDays.setPreferredSize(new Dimension(250, 75));
        txtDays.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 0;
        gridBagConstraints.gridy   = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(txtDays, gridBagConstraints);

        ddLocation.setName("ddLocation");
        ddLocation.setEditable(false);
        ddLocation.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Location on Body"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        ddLocation.setPreferredSize(new Dimension(250, 75));
        ddLocation.setMinimumSize(new Dimension(250, 75));
        ddLocation.addActionListener(evt -> {
            ddTypeModel.updateFilter();

            BodyLocation loc  = ((BodyLocationChoice) Objects.requireNonNull(ddLocation.getSelectedItem())).loc;
            InjuryType   type = ((InjuryTypeChoice) Objects.requireNonNull(ddType.getSelectedItem())).type;
            if (!type.isValidInLocation(loc)) {
                ddType.setSelectedItem(ddTypeModel.getElementAt(0));
            }
        });

        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 1;
        gridBagConstraints.gridy   = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(ddLocation, gridBagConstraints);

        ddType.setName("ddType");
        ddType.setEditable(false);
        ddType.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Type of Injury"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        ddType.setPreferredSize(new Dimension(250, 75));
        ddType.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 2;
        gridBagConstraints.gridy   = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(ddType, gridBagConstraints);

        txtFluff.setText(injury.getFluff());
        txtFluff.setName("txtFluff");
        txtFluff.setEditable(true);
        txtFluff.setLineWrap(true);
        txtFluff.setWrapStyleWord(true);
        txtFluff.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Fluff Message"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtFluff.setPreferredSize(new Dimension(250, 75));
        txtFluff.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 3;
        gridBagConstraints.gridy   = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(txtFluff, gridBagConstraints);

        txtHits.setText(Integer.toString(injury.getHits()));
        txtHits.setName("txtHits");
        txtHits.setEditable(true);
        txtHits.setLineWrap(true);
        txtHits.setWrapStyleWord(true);
        txtHits.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Number of Hits"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtHits.setPreferredSize(new Dimension(250, 75));
        txtHits.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 0;
        gridBagConstraints.gridy   = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(txtHits, gridBagConstraints);

        ddPermanent.setSelectedIndex(injury.isPermanent() ? 0 : 1);
        ddPermanent.setName("ddPermanent");
        ddPermanent.setEditable(false);
        ddPermanent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Is Permanent"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        ddPermanent.setPreferredSize(new Dimension(250, 75));
        ddPermanent.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 1;
        gridBagConstraints.gridy   = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(ddPermanent, gridBagConstraints);

        ddWorkedOn.setSelectedIndex(injury.isWorkedOn() ? 0 : 1);
        ddWorkedOn.setName("ddWorkedOn");
        ddWorkedOn.setEditable(false);
        ddWorkedOn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Doctor Has Worked On"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        ddWorkedOn.setPreferredSize(new Dimension(250, 75));
        ddWorkedOn.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 2;
        gridBagConstraints.gridy   = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(ddWorkedOn, gridBagConstraints);

        ddExtended.setSelectedIndex(injury.getExtended() ? 0 : 1);
        ddExtended.setName("ddExtended");
        ddExtended.setEditable(true);
        ddExtended.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Was Extended Time"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        ddExtended.setPreferredSize(new Dimension(250, 75));
        ddExtended.setMinimumSize(new Dimension(250, 75));
        gridBagConstraints         = new GridBagConstraints();
        gridBagConstraints.gridx   = 3;
        gridBagConstraints.gridy   = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill    = GridBagConstraints.BOTH;
        gridBagConstraints.anchor  = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets  = new Insets(5, 5, 5, 5);
        panMain.add(ddExtended, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOkay.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditInjuryEntryDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        injury.setTime(Integer.parseInt(txtDays.getText()));
        injury.setHits(Integer.parseInt(txtHits.getText()));
        injury.setFluff(txtFluff.getText());
        injury.setLocation(((BodyLocationChoice) Objects.requireNonNull(ddLocation.getSelectedItem())).loc);
        injury.setType(((InjuryTypeChoice) Objects.requireNonNull(ddType.getSelectedItem())).type);
        injury.setPermanent(ddPermanent.getSelectedIndex() == 0);
        injury.setWorkedOn(ddWorkedOn.getSelectedIndex() == 0);
        injury.setExtended(ddExtended.getSelectedIndex() == 0);
        injury.setUUID(UUID.randomUUID());
        this.setVisible(false);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        injury = null;
        this.setVisible(false);
    }

    public Injury getEntry() {
        return injury;
    }

    private record BodyLocationChoice(BodyLocation loc) {

        @Override
        public String toString() {
            return loc.locationName();
        }
    }

    private record InjuryTypeChoice(InjuryType type) {

        @Override
        public String toString() {
            return type.getName(BodyLocation.GENERIC, 1);
        }
    }
}
