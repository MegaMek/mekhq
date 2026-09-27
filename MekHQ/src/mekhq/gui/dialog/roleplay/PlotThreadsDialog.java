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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import mekhq.campaign.roleplay.Concepts.Concept;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.campaign.roleplay.PlotThreadLength;
import mekhq.campaign.roleplay.PlotThreadStep;
import mekhq.campaign.roleplay.RandomOracleGenerator;
import mekhq.campaign.roleplay.Roleplay;

/**
 * Lets the player create, rename and delete {@link PlotThread}s, and reveal each thread's secretly rolled steps one
 * at a time.
 */
public class PlotThreadsDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";
    private static final int CELLS_PER_ROW = 5;

    private final Roleplay roleplay;

    private DefaultListModel<PlotThread> threadModel;
    private JList<PlotThread> lstThreads;
    private JLabel lblProgress;
    private JPanel pnlTrack;
    private JButton btnReveal;
    private JEditorPane txtRevealed;

    public PlotThreadsDialog(final JDialog owner, final Roleplay roleplay) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.title"), true);
        this.roleplay = roleplay;
        initialize();
        refreshThreads(roleplay.getPlotThreads().isEmpty() ? -1 : 0);
        setSize(new Dimension(800, 560));
        setLocationRelativeTo(owner);
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createThreadListPanel(), BorderLayout.WEST);
        getContentPane().add(createTrackPanel(), BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.close"));
        btnClose.addActionListener(event -> dispose());
        pnlButtons.add(btnClose);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
    }

    // region Thread list
    private JPanel createThreadListPanel() {
        JPanel pnlThreads = new JPanel(new BorderLayout(5, 5));
        pnlThreads.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5),
              BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.threads.title"))));
        pnlThreads.setPreferredSize(new Dimension(230, 0));

        threadModel = new DefaultListModel<>();
        lstThreads = new JList<>(threadModel);
        lstThreads.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstThreads.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshTrack();
            }
        });
        pnlThreads.add(new JScrollPane(lstThreads), BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel(new GridLayout(1, 3, 5, 0));
        pnlButtons.add(createButton("PlotThreadsDialog.new", this::newThread));
        pnlButtons.add(createButton("PlotThreadsDialog.rename", this::renameThread));
        pnlButtons.add(createButton("PlotThreadsDialog.delete", this::deleteThread));
        pnlThreads.add(pnlButtons, BorderLayout.SOUTH);
        return pnlThreads;
    }

    private JButton createButton(final String key, final Runnable action) {
        JButton button = new JButton(getTextAt(RESOURCE_BUNDLE, key));
        button.addActionListener(event -> action.run());
        return button;
    }

    private void refreshThreads(final int selectedIndex) {
        threadModel.clear();
        threadModel.addAll(roleplay.getPlotThreads());
        if (selectedIndex >= 0 && selectedIndex < threadModel.size()) {
            lstThreads.setSelectedIndex(selectedIndex);
        }
        refreshTrack();
    }

    private void newThread() {
        JTextField txtName = new JTextField(20);
        JComboBox<PlotThreadLength> cboLength = new JComboBox<>(PlotThreadLength.values());
        JPanel pnlForm = new JPanel(new GridLayout(2, 2, 5, 5));
        pnlForm.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.new.name")));
        pnlForm.add(txtName);
        pnlForm.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.new.length")));
        pnlForm.add(cboLength);

        int choice = JOptionPane.showConfirmDialog(this, pnlForm, getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.new"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION || txtName.getText().isBlank()) {
            return;
        }

        PlotThreadLength length = (PlotThreadLength) cboLength.getSelectedItem();
        roleplay.getPlotThreads().add(PlotThread.create(txtName.getText().trim(),
              length == null ? PlotThreadLength.SHORT : length, RandomOracleGenerator.getInstance()));
        refreshThreads(roleplay.getPlotThreads().size() - 1);
    }

    private void renameThread() {
        PlotThread thread = lstThreads.getSelectedValue();
        if (thread == null) {
            return;
        }

        Object input = JOptionPane.showInputDialog(this, getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.rename.prompt"),
              getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.rename"), JOptionPane.PLAIN_MESSAGE, null, null,
              thread.getName());
        if (input != null && !input.toString().isBlank()) {
            thread.setName(input.toString().trim());
            refreshThreads(lstThreads.getSelectedIndex());
        }
    }

    private void deleteThread() {
        int index = lstThreads.getSelectedIndex();
        if (index < 0) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
              getFormattedTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.delete.confirm",
                    roleplay.getPlotThreads().get(index).getName()),
              getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.delete"), JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            roleplay.getPlotThreads().remove(index);
            refreshThreads(Math.min(index, roleplay.getPlotThreads().size() - 1));
        }
    }
    // endregion Thread list

    // region Track
    private JPanel createTrackPanel() {
        JPanel pnlMain = new JPanel(new BorderLayout(5, 5));
        pnlMain.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10),
              BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.track.title"))));

        JPanel pnlTop = new JPanel(new BorderLayout(5, 5));
        lblProgress = new JLabel();
        btnReveal = createButton("PlotThreadsDialog.reveal", this::revealNextStep);
        JPanel pnlHeader = new JPanel(new BorderLayout(10, 0));
        pnlHeader.add(lblProgress, BorderLayout.CENTER);
        pnlHeader.add(btnReveal, BorderLayout.EAST);
        pnlTop.add(pnlHeader, BorderLayout.NORTH);

        pnlTrack = new JPanel(new GridLayout(0, CELLS_PER_ROW, 3, 3));
        pnlTop.add(pnlTrack, BorderLayout.CENTER);
        pnlMain.add(pnlTop, BorderLayout.NORTH);

        txtRevealed = new JEditorPane("text/html", "");
        txtRevealed.setEditable(false);
        txtRevealed.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pnlMain.add(new JScrollPane(txtRevealed), BorderLayout.CENTER);
        return pnlMain;
    }

    private void revealNextStep() {
        PlotThread thread = lstThreads.getSelectedValue();
        if (thread != null) {
            thread.revealNextStep();
            refreshTrack();
        }
    }

    private void refreshTrack() {
        PlotThread thread = lstThreads.getSelectedValue();
        pnlTrack.removeAll();

        if (thread == null) {
            lblProgress.setText(getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.noThread"));
            btnReveal.setEnabled(false);
            txtRevealed.setText("");
        } else {
            lblProgress.setText(thread.isComplete()
                                      ? getFormattedTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.complete",
                  escapeHtml(thread.getName()))
                                      : getFormattedTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.progress",
                                            escapeHtml(thread.getName()), thread.getRevealedSteps(),
                                            thread.getSteps().size()));
            btnReveal.setEnabled(!thread.isComplete());
            for (PlotThreadStep step : thread.getSteps()) {
                pnlTrack.add(createCell(step, step.number() <= thread.getRevealedSteps()));
            }
            txtRevealed.setText(describeRevealed(thread.getRevealed()));
            txtRevealed.setCaretPosition(0);
        }

        pnlTrack.revalidate();
        pnlTrack.repaint();
    }

    private JLabel createCell(final PlotThreadStep step, final boolean revealed) {
        String subtitle = "";
        if (step.conclusion()) {
            subtitle = getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.cell.conclusion");
        } else if (step.majorRevelation()) {
            subtitle = getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.cell.flashpoint");
        }

        JLabel cell = new JLabel("<html><center><b>" + step.number() + "</b><br><font size=\"-2\">" + subtitle
                                       + "</font></center></html>", SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")));
        cell.setPreferredSize(new Dimension(0, 44));

        Color background = UIManager.getColor("Panel.background");
        Color foreground = UIManager.getColor("Label.foreground");
        if (revealed) {
            background = new Color(40, 45, 110);
            foreground = Color.WHITE;
        } else if (step.majorRevelation() || step.conclusion()) {
            background = background.darker();
        }
        cell.setBackground(background);
        cell.setForeground(foreground);
        return cell;
    }

    private String describeRevealed(final List<PlotThreadStep> revealed) {
        if (revealed.isEmpty()) {
            return "<html><i>" + getTextAt(RESOURCE_BUNDLE, "PlotThreadsDialog.nothingRevealed") + "</i></html>";
        }

        StringBuilder text = new StringBuilder("<html>");
        // Newest first, so the step just revealed is at the top.
        for (int i = revealed.size() - 1; i >= 0; i--) {
            PlotThreadStep step = revealed.get(i);
            String key = step.conclusion() ? "PlotThreadsDialog.step.conclusion"
                               : step.majorRevelation() ? "PlotThreadsDialog.step.majorRevelation"
                                       : "PlotThreadsDialog.step";
            text.append("<p><b>").append(getFormattedTextAt(RESOURCE_BUNDLE, key, step.number())).append("</b><br>");
            for (Concept concept : step.concepts()) {
                String meaning = concept.meaning() == null
                                       ? getTextAt(RESOURCE_BUNDLE, "OracleDialog.concepts.empty")
                                       : escapeHtml(concept.meaning());
                text.append(getFormattedTextAt(RESOURCE_BUNDLE, "OracleDialog.concepts.result",
                      concept.table().getLabel(), meaning)).append("<br>");
            }
            text.append("</p>");
        }
        return text.append("</html>").toString();
    }
    // endregion Track

    private static String escapeHtml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
