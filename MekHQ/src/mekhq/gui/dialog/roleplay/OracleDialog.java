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
package mekhq.gui.dialog.roleplay;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import mekhq.campaign.Campaign;
import mekhq.campaign.roleplay.FateChart;
import mekhq.campaign.roleplay.FateChartOdds;
import mekhq.campaign.roleplay.FateChartResult;
import mekhq.campaign.roleplay.RandomEventFocus;
import mekhq.campaign.roleplay.Roleplay;

/**
 * Lets the player ask the {@link FateChart} a yes/no question. The player chooses the odds, adjusts the campaign's
 * chaos factor (stored in the campaign's {@link Roleplay}), and rolls. The dialog also manages the Oracle's character
 * list, from which a character is drawn whenever a random event involves an NPC.
 */
public class OracleDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final Roleplay roleplay;

    private JComboBox<FateChartOdds> cboOdds;
    private JLabel lblChaosValue;
    private JButton btnDecreaseChaos;
    private JButton btnIncreaseChaos;
    private JLabel lblResult;

    private DefaultListModel<String> characterModel;
    private JList<String> lstCharacters;
    private JTextField txtNewCharacter;

    public OracleDialog(final JFrame frame, final Campaign campaign) {
        super(frame, getTextAt(RESOURCE_BUNDLE, "OracleDialog.title"), true);
        this.roleplay = campaign.getRoleplay();
        initialize();
        pack();
        setLocationRelativeTo(frame);
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel pnlMain = new JPanel(new GridBagLayout());
        pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // Odds
        JLabel lblOdds = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleDialog.odds.label"));
        cboOdds = new JComboBox<>(FateChartOdds.values());
        cboOdds.setSelectedItem(FateChartOdds.FIFTY_FIFTY);
        cboOdds.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.odds.toolTipText"));
        lblOdds.setLabelFor(cboOdds);
        constraints.gridx = 0;
        constraints.gridy = 0;
        pnlMain.add(lblOdds, constraints);
        constraints.gridx = 1;
        pnlMain.add(cboOdds, constraints);

        // Chaos factor
        JLabel lblChaos = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.label"));
        lblChaos.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.toolTipText"));
        constraints.gridx = 0;
        constraints.gridy = 1;
        pnlMain.add(lblChaos, constraints);
        constraints.gridx = 1;
        pnlMain.add(createChaosPanel(), constraints);

        // Result
        lblResult = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleDialog.prompt"), SwingConstants.CENTER);
        lblResult.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        pnlMain.add(lblResult, constraints);

        // Buttons
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnRoll = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.roll"));
        btnRoll.addActionListener(event -> roll());
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.close"));
        btnClose.addActionListener(event -> dispose());
        pnlButtons.add(btnRoll);
        pnlButtons.add(btnClose);
        getRootPane().setDefaultButton(btnRoll);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pnlMain, BorderLayout.CENTER);
        getContentPane().add(createCharacterPanel(), BorderLayout.EAST);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
    }

    private JPanel createCharacterPanel() {
        JPanel pnlCharacters = new JPanel(new BorderLayout(5, 5));
        pnlCharacters.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10),
              BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "OracleDialog.characters.title"))));

        // Add row
        txtNewCharacter = new JTextField(15);
        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.characters.add"));
        btnAdd.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.characters.add.toolTipText"));
        btnAdd.addActionListener(event -> addCharacter());
        txtNewCharacter.addActionListener(event -> addCharacter());
        JPanel pnlAdd = new JPanel(new BorderLayout(5, 0));
        pnlAdd.add(txtNewCharacter, BorderLayout.CENTER);
        pnlAdd.add(btnAdd, BorderLayout.EAST);
        pnlCharacters.add(pnlAdd, BorderLayout.NORTH);

        // List
        characterModel = new DefaultListModel<>();
        lstCharacters = new JList<>(characterModel);
        lstCharacters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCharacters.setVisibleRowCount(10);
        lstCharacters.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    editCharacter();
                }
            }
        });
        pnlCharacters.add(new JScrollPane(lstCharacters), BorderLayout.CENTER);

        // Edit controls
        JPanel pnlEdit = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 5, 0);
        pnlEdit.add(createListButton("OracleDialog.characters.edit", this::editCharacter), constraints);
        pnlEdit.add(createListButton("OracleDialog.characters.remove", this::removeCharacter), constraints);
        pnlEdit.add(createListButton("OracleDialog.characters.moveUp", () -> moveCharacter(-1)), constraints);
        pnlEdit.add(createListButton("OracleDialog.characters.moveDown", () -> moveCharacter(1)), constraints);
        JPanel pnlEditWrapper = new JPanel(new BorderLayout());
        pnlEditWrapper.add(pnlEdit, BorderLayout.NORTH);
        pnlCharacters.add(pnlEditWrapper, BorderLayout.EAST);

        refreshCharacters(-1);
        return pnlCharacters;
    }

    private JButton createListButton(final String key, final Runnable action) {
        JButton button = new JButton(getTextAt(RESOURCE_BUNDLE, key));
        button.addActionListener(event -> action.run());
        return button;
    }

    /**
     * Rebuilds the list view from the campaign's character list, then selects the given index if it is valid.
     */
    private void refreshCharacters(final int selectedIndex) {
        characterModel.clear();
        characterModel.addAll(roleplay.getCharacters());
        if (selectedIndex >= 0 && selectedIndex < characterModel.size()) {
            lstCharacters.setSelectedIndex(selectedIndex);
            lstCharacters.ensureIndexIsVisible(selectedIndex);
        }
    }

    private void addCharacter() {
        if (roleplay.addCharacter(txtNewCharacter.getText())) {
            txtNewCharacter.setText("");
            refreshCharacters(roleplay.getCharacters().size() - 1);
        }
        txtNewCharacter.requestFocusInWindow();
    }

    private void editCharacter() {
        int index = lstCharacters.getSelectedIndex();
        if (index < 0) {
            return;
        }

        List<String> characters = roleplay.getCharacters();
        Object input = JOptionPane.showInputDialog(this,
              getTextAt(RESOURCE_BUNDLE, "OracleDialog.characters.edit.prompt"),
              getTextAt(RESOURCE_BUNDLE, "OracleDialog.characters.edit"), JOptionPane.PLAIN_MESSAGE, null, null,
              characters.get(index));
        if (input == null || input.toString().isBlank()) {
            return;
        }

        String name = input.toString().trim();
        if (!name.equals(characters.get(index)) && characters.contains(name)) {
            return;
        }
        characters.set(index, name);
        refreshCharacters(index);
    }

    private void removeCharacter() {
        int index = lstCharacters.getSelectedIndex();
        if (index < 0) {
            return;
        }

        roleplay.getCharacters().remove(index);
        refreshCharacters(Math.min(index, roleplay.getCharacters().size() - 1));
    }

    private void moveCharacter(final int offset) {
        int index = lstCharacters.getSelectedIndex();
        int target = index + offset;
        if (index < 0 || target < 0 || target >= roleplay.getCharacters().size()) {
            return;
        }

        Collections.swap(roleplay.getCharacters(), index, target);
        refreshCharacters(target);
    }

    private JPanel createChaosPanel() {
        JPanel pnlChaos = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        btnDecreaseChaos = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.decrease"));
        btnDecreaseChaos.addActionListener(event -> {
            roleplay.decreaseChaosFactor();
            refreshChaos();
        });

        lblChaosValue = new JLabel();
        lblChaosValue.setFont(lblChaosValue.getFont().deriveFont(Font.BOLD));
        lblChaosValue.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.toolTipText"));

        btnIncreaseChaos = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.increase"));
        btnIncreaseChaos.addActionListener(event -> {
            roleplay.increaseChaosFactor();
            refreshChaos();
        });

        pnlChaos.add(btnDecreaseChaos);
        pnlChaos.add(lblChaosValue);
        pnlChaos.add(btnIncreaseChaos);
        refreshChaos();
        return pnlChaos;
    }

    private void refreshChaos() {
        int chaosFactor = roleplay.getChaosFactor();
        lblChaosValue.setText(String.valueOf(chaosFactor));
        btnDecreaseChaos.setEnabled(chaosFactor > FateChart.MINIMUM_CHAOS_FACTOR);
        btnIncreaseChaos.setEnabled(chaosFactor < FateChart.MAXIMUM_CHAOS_FACTOR);
    }

    private void roll() {
        FateChartOdds odds = (FateChartOdds) cboOdds.getSelectedItem();
        if (odds == null) {
            return;
        }

        int chaosFactor = roleplay.getChaosFactor();
        FateChartResult result = FateChart.consult(odds, chaosFactor);

        if (result.hasRandomEvent()) {
            RandomEventFocus focus = result.randomEventFocus();
            lblResult.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleDialog.result.randomEvent",
                  result.answer().getLabel(), result.roll(), odds.getLabel(), chaosFactor,
                  focus.getLabel(), result.randomEventRoll(), focus.getDescription(), getCharacterLine(focus)));
        } else {
            lblResult.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleDialog.result",
                  result.answer().getLabel(), result.roll(), odds.getLabel(), chaosFactor));
        }
        pack();
    }

    /**
     * @return the line naming a randomly chosen Oracle character if the focus involves an NPC, otherwise an empty
     *       string
     */
    private String getCharacterLine(final RandomEventFocus focus) {
        if (!focus.involvesNPC()) {
            return "";
        }

        String character = roleplay.pickRandomCharacter();
        if (character == null) {
            return getTextAt(RESOURCE_BUNDLE, "OracleDialog.result.noCharacter");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "OracleDialog.result.character", escapeHtml(character));
    }

    private static String escapeHtml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
