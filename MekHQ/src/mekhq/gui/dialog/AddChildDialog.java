/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.personnel.procreation.AbstractProcreation.MAXIMUM_ARRIVING_CHILD_AGE;
import static mekhq.campaign.personnel.procreation.AbstractProcreation.getEarliestChildDateOfBirth;
import static mekhq.campaign.personnel.procreation.AbstractProcreation.getMaximumBiologicalChildAge;
import static mekhq.campaign.personnel.procreation.AbstractProcreation.getRandomDateOfBirth;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

import jakarta.annotation.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.procreation.ChildArrivalType;

/**
 * A GM dialog for adding a new child to a person's family, either as a biological child they already had or as an
 * adopted child. The child is generated at the chosen age and joins the force as a dependent.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class AddChildDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AddChildDialog";

    private final Campaign campaign;
    private final Person person;
    private final LocalDate today;

    /** The possible other parents. The first entry is {@code null}, standing for no other parent. */
    private final List<Person> potentialCoParents = new ArrayList<>();

    private JComboBox<ChildArrivalType> comboType;
    private JComboBox<String> comboCoParent;
    private JSpinner spnAge;
    private JLabel lblWarning;
    private JButton btnConfirm;

    /**
     * Creates the dialog. Call {@link #setVisible(boolean)} to show it.
     *
     * @param parent   the frame the dialog belongs to
     * @param campaign the current campaign
     * @param person   the person the child is joining
     */
    public AddChildDialog(final JFrame parent, final Campaign campaign, final Person person) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "AddChildDialog.title"), true);
        this.campaign = campaign;
        this.person = person;
        this.today = campaign.getLocalDate();

        buildPotentialCoParents();
        initComponents();
        updateAgeLimit();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Lists the adults who could be the child's other parent, with the person's spouse (if any) first.
     */
    private void buildPotentialCoParents() {
        potentialCoParents.add(null);

        final Person spouse = person.getGenealogy().getSpouse();
        if (spouse != null) {
            potentialCoParents.add(spouse);
        }

        campaign.getPlayerForce()
              .getHumanResources()
              .getActivePersonnel(false, true)
              .stream()
              .filter(candidate -> !candidate.equals(person) && !candidate.equals(spouse))
              .filter(candidate -> !candidate.isChild(today, true))
              .sorted(Comparator.comparing(Person::getFullName))
              .forEach(potentialCoParents::add);
    }

    private void initComponents() {
        getContentPane().setLayout(new BorderLayout(0, scaleForGUI(5)));
        final int padding = scaleForGUI(10);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));

        getContentPane().add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "AddChildDialog.description",
              person.getFullName())), BorderLayout.NORTH);

        // Someone too young to have any biological child can still adopt
        comboType = new JComboBox<>();
        if (getMaximumBiologicalChildAge(today, person) >= 0) {
            comboType.addItem(ChildArrivalType.BIOLOGICAL);
        }
        comboType.addItem(ChildArrivalType.ADOPTION);
        comboType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChildArrivalType arrivalType) {
                    setText(getTextAt(RESOURCE_BUNDLE, "AddChildDialog.type." + arrivalType.name()));
                }
                return this;
            }
        });
        comboType.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE,
              "AddChildDialog.type.tooltip",
              MAXIMUM_ARRIVING_CHILD_AGE));
        comboType.addActionListener(evt -> updateAgeLimit());

        comboCoParent = new JComboBox<>();
        for (Person coParent : potentialCoParents) {
            comboCoParent.addItem(coParent == null ?
                                        getTextAt(RESOURCE_BUNDLE, "AddChildDialog.coParent.none") :
                                        coParent.getFullTitle());
        }
        // Default to the spouse, as they are the most likely other parent
        if (person.getGenealogy().hasSpouse()) {
            comboCoParent.setSelectedIndex(1);
        }
        comboCoParent.addActionListener(evt -> updateAgeLimit());

        spnAge = new JSpinner(new SpinnerNumberModel(0, 0, MAXIMUM_ARRIVING_CHILD_AGE, 1));

        final JPanel fieldsPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        final int fieldPadding = scaleForGUI(5);
        gbc.insets = new Insets(fieldPadding, fieldPadding, fieldPadding, fieldPadding);
        gbc.anchor = GridBagConstraints.WEST;
        addRow(fieldsPanel, gbc, 0, "AddChildDialog.type", comboType);
        addRow(fieldsPanel, gbc, 1, "AddChildDialog.coParent", comboCoParent);
        addRow(fieldsPanel, gbc, 2, "AddChildDialog.age", spnAge);

        lblWarning = new JLabel(getTextAt(RESOURCE_BUNDLE, "AddChildDialog.tooYoung"));
        lblWarning.setVisible(false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        fieldsPanel.add(lblWarning, gbc);

        getContentPane().add(fieldsPanel, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel();
        btnConfirm = new JButton(getTextAt(RESOURCE_BUNDLE, "AddChildDialog.confirm"));
        btnConfirm.addActionListener(evt -> {
            addChild();
            dispose();
        });
        final JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "AddChildDialog.cancel"));
        btnCancel.addActionListener(evt -> dispose());
        buttonPanel.add(btnConfirm);
        buttonPanel.add(btnCancel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private static void addRow(final JPanel panel, final GridBagConstraints gbc, final int row,
          final String labelKey, final Component field) {
        gbc.gridy = row;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey)), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private ChildArrivalType getSelectedType() {
        return (ChildArrivalType) comboType.getSelectedItem();
    }

    private @Nullable Person getSelectedCoParent() {
        final int index = comboCoParent.getSelectedIndex();
        return (index < 0) ? null : potentialCoParents.get(index);
    }

    /**
     * Limits the child's age to what the selected parents allow. A biological child can't be older than either
     * parent's adult life; if that rules out every age, the child can't be added until the selection changes.
     */
    private void updateAgeLimit() {
        int maximumAge = MAXIMUM_ARRIVING_CHILD_AGE;

        if (getSelectedType().isBiological()) {
            maximumAge = getMaximumBiologicalChildAge(today, person);

            final Person coParent = getSelectedCoParent();
            if (coParent != null) {
                maximumAge = Math.min(maximumAge, getMaximumBiologicalChildAge(today, coParent));
            }
        }

        final boolean isPossible = maximumAge >= 0;
        lblWarning.setVisible(!isPossible);
        btnConfirm.setEnabled(isPossible);

        final SpinnerNumberModel ageModel = (SpinnerNumberModel) spnAge.getModel();
        final int clampedMaximum = Math.max(maximumAge, 0);
        ageModel.setMaximum(clampedMaximum);
        if ((int) ageModel.getValue() > clampedMaximum) {
            ageModel.setValue(clampedMaximum);
        }

        pack();
    }

    private void addChild() {
        final ChildArrivalType arrivalType = getSelectedType();
        final Person coParent = getSelectedCoParent();

        // A biological child can't have been born before either parent was old enough
        LocalDate earliestDateOfBirth = null;
        if (arrivalType.isBiological()) {
            earliestDateOfBirth = getEarliestChildDateOfBirth(person);
            if (coParent != null) {
                final LocalDate coParentEarliestDateOfBirth = getEarliestChildDateOfBirth(coParent);
                if (coParentEarliestDateOfBirth.isAfter(earliestDateOfBirth)) {
                    earliestDateOfBirth = coParentEarliestDateOfBirth;
                }
            }
        }

        final LocalDate dateOfBirth = getRandomDateOfBirth(today, (int) spnAge.getValue(), earliestDateOfBirth);

        campaign.getPlayerForce()
              .getHumanResources()
              .getProcreation()
              .addChild(campaign, today, person, coParent, dateOfBirth, arrivalType);

        MekHQ.triggerEvent(new PersonChangedEvent(person));
        if (coParent != null) {
            MekHQ.triggerEvent(new PersonChangedEvent(coParent));
        }
    }
}
