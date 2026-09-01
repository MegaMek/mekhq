/*
 * Copyright (C) 2011-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.TableColumn;

import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.TechConstants;
import megamek.common.options.IOption;
import megamek.common.preference.PreferenceManager;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.units.Entity;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.model.PersonnelEventLogModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.utilities.ImgLabel;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.gui.utilities.MultiLineTooltip;

/**
 * A custom panel that gets filled in with goodies from a unit record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class UnitViewPanel extends JScrollablePanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.UnitViewPanel";

    private final Unit unit;
    private final Entity entity;
    private final Campaign campaign;

    private JPanel pnlStats;
    private JPanel pnlCrew;

    public UnitViewPanel(Unit u, Campaign c) {
        super();
        unit = u;
        entity = u.getEntity();
        campaign = c;
        initComponents();
    }

    private void initComponents() {
        pnlStats = new JPanel();
        pnlCrew = new JPanel();

        setLayout(new GridBagLayout());

        // Unit image (top left)
        JLabel lblImage = buildImageLabel();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(lblImage, gridBagConstraints);

        // Unit stats (top right)
        pnlStats.setName("pnlBasic");
        pnlStats.setBorder(RoundedLineBorder.createRoundedLineBorder(unit.getName()));
        fillStats();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        // Crew needs (spans full width, below the header)
        pnlCrew.setName("pnlCrew");
        pnlCrew.setLayout(new BorderLayout());
        pnlCrew.setBorder(RoundedLineBorder.createRoundedLineBorder(getTextAt(RESOURCE_BUNDLE, "lblCrew.text")));
        JLabel lblCrew = new JLabel(UnitTableModel.getCrewTooltip(unit));
        pnlCrew.add(lblCrew, BorderLayout.WEST);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlCrew, gridBagConstraints);

        // Location (spans full width, below crew needs)
        LocationSummaryPanel pnlLocation = new LocationSummaryPanel(unit, campaign);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlLocation, gridBagConstraints);

        // Tabs: Technical Readout and Unit History
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(tabbedPane, gridBagConstraints);

        JPanel pnlReadoutTab = new JPanel(new GridBagLayout());
        pnlReadoutTab.setName("pnlReadoutTab");
        fillReadoutTab(pnlReadoutTab);
        tabbedPane.addTab(getTextAt(RESOURCE_BUNDLE, "pnlReadoutTab.title"), pnlReadoutTab);

        JPanel pnlHistoryTab = new JPanel(new GridBagLayout());
        pnlHistoryTab.setName("pnlHistoryTab");
        fillHistoryTab(pnlHistoryTab);
        tabbedPane.addTab(getTextAt(RESOURCE_BUNDLE, "pnlHistoryTab.title"), pnlHistoryTab);
    }

    /**
     * Builds the label holding the unit's image, preferring the fluff image and falling back to the top-down sprite.
     *
     * @return a label displaying the unit's image
     */
    private JLabel buildImageLabel() {
        boolean isSpritesOnly = PreferenceManager.getClientPreferences().getSpritesOnly();
        Image image = isSpritesOnly ? null : FluffImageHelper.getFluffImage(entity);

        if (null != image) {
            // fluff image exists so use custom ImgLabel to get full mek porn; cap it so it stays prominent without
            // crowding out the stats panel beside it (the image label is laid out to its preferred size).
            return new ImgLabel(image, UIUtil.scaleForGUI(240), UIUtil.scaleForGUI(360));
        }

        // no fluff image, so just use image icon from top-down view
        JLabel lblImage = new JLabel();
        image = unit.getImage(lblImage);
        if (null != image) {
            ImageIcon icon = new ImageIcon(image);
            icon = ImageUtilities.scaleImageIcon(icon, UIUtil.scaleForGUI(150), true);
            lblImage.setIcon(icon);
        }
        return lblImage;
    }

    /**
     * Builds the Technical Readout tab.
     *
     * @param readoutTab the panel to populate
     */
    private void fillReadoutTab(JPanel readoutTab) {
        JTextPane txtReadout = new JTextPane();

        EntityReadout entityReadout = EntityReadout.createReadout(entity, false, true);
        txtReadout.setName("txtReadout");
        txtReadout.setContentType(getTextAt(RESOURCE_BUNDLE, "txtReadout.contentType"));
        txtReadout.setEditable(false);
        txtReadout.setFont(Font.decode(getTextAt(RESOURCE_BUNDLE, "txtReadout.font")));
        txtReadout.setText("<div style='font: 12pt monospaced'>" +
                                 entityReadout.getBasicSection(ViewFormatting.HTML) +
                                 "<br>" +
                                 entityReadout.getLoadoutSection(ViewFormatting.HTML) +
                                 "</div>");
        txtReadout.setBorder(RoundedLineBorder.createRoundedLineBorder(getTextAt(RESOURCE_BUNDLE,
              "technicalReadout.title")));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        readoutTab.add(txtReadout, gridBagConstraints);
    }

    /**
     * Builds the Unit History tab, showing the unit's written history at the top and its log below it.
     *
     * @param historyTab the panel to populate
     */
    private void fillHistoryTab(JPanel historyTab) {
        int y = 0;

        if (!unit.getHistory().isBlank()) {
            JTextPane txtFluff = new JTextPane();
            txtFluff.setName("txtFluff");
            txtFluff.setEditable(false);
            txtFluff.setContentType("text/html");
            txtFluff.setText(MarkdownRenderer.getRenderedHtml(unit.getHistory()));
            txtFluff.setBorder(RoundedLineBorder.createRoundedLineBorder(getTextAt(RESOURCE_BUNDLE,
                  "unitHistory.title")));
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            historyTab.add(txtFluff, gridBagConstraints);
            y++;
        }

        y = addLogSection(historyTab, y, getTextAt(RESOURCE_BUNDLE, "unitLog.title"), unit.getUnitLog(),
              MekHQ.getMHQOptions().getDisplayUnitLog());
        y = addLogSection(historyTab, y, getTextAt(RESOURCE_BUNDLE, "killLog.title"), unit.getKillLog(),
              MekHQ.getMHQOptions().getDisplayUnitKillLog());
        y = addLogSection(historyTab, y, getTextAt(RESOURCE_BUNDLE, "crewLog.title"), unit.getCrewLog(),
              MekHQ.getMHQOptions().getDisplayUnitCrewLog());
        y = addLogSection(historyTab, y, getTextAt(RESOURCE_BUNDLE, "deploymentLog.title"), unit.getDeploymentLog(),
              MekHQ.getMHQOptions().getDisplayUnitDeploymentLog());
        y = addLogSection(historyTab, y, getTextAt(RESOURCE_BUNDLE, "repairLog.title"), unit.getRepairLog(),
              MekHQ.getMHQOptions().getDisplayUnitRepairLog());

        // glue to soak up remaining vertical space so the panels sit at the top at their preferred height
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        historyTab.add(Box.createGlue(), gridBagConstraints);
    }

    /**
     * Adds a collapsible log section to the given panel. The section is always added, even when the log is empty. It is
     * shown expanded by default; clicking the section title collapses it to a titled bar, and clicking a collapsed bar
     * expands it again, mirroring the personnel record on {@code PersonViewPanel}.
     *
     * @param panel         the panel to add the section to
     * @param gridY         the grid row to place the section at
     * @param title         the title for the section
     * @param logs          the log entries to display
     * @param startExpanded whether the section is shown expanded initially
     *
     * @return the next available grid row
     */
    private int addLogSection(JPanel panel, int gridY, String title, List<LogEntry> logs, boolean startExpanded) {
        // collapsed state - just the titled bar, labelled to show the log when clicked
        JPanel header = new JPanel();
        header.setBorder(RoundedLineBorder.createRoundedLineBorder(getFormattedTextAt(RESOURCE_BUNDLE,
              "logSection.show.title", title)));
        header.setVisible(!startExpanded);

        // expanded state - the titled bar plus the table of entries, labelled to hide the log when clicked
        JPanel content = fillLogTable(logs, title);
        content.setBorder(RoundedLineBorder.createRoundedLineBorder(getFormattedTextAt(RESOURCE_BUNDLE,
              "logSection.hide.title", title)));
        content.setVisible(startExpanded);

        header.addMouseListener(getSwitchListener(header, content));
        content.addMouseListener(getSwitchListener(content, header));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridY;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(header, gridBagConstraints);
        panel.add(content, gridBagConstraints);

        return gridY + 1;
    }

    /**
     * Returns a mouse listener that hides {@code current} and shows {@code switchTo} when {@code current} is clicked,
     * driving the expand/collapse behaviour of the log sections.
     *
     * @param current  the panel that is currently visible
     * @param switchTo the panel to show in its place
     *
     * @return the switch mouse listener
     */
    private MouseListener getSwitchListener(JPanel current, JPanel switchTo) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (current.isVisible()) {
                    current.setVisible(false);
                    switchTo.setVisible(true);
                }
            }
        };
    }

    /**
     * Builds a panel containing a table of the given log entries, most recent first.
     *
     * @param logs           the log entries to display
     * @param accessibleName a short name describing the log, used for the table's accessible name
     *
     * @return a panel with a table of the log entries
     */
    private JPanel fillLogTable(List<LogEntry> logs, String accessibleName) {
        List<LogEntry> orderedLogs = new ArrayList<>(logs);
        Collections.reverse(orderedLogs);

        JPanel pnlLog = new JPanel(new GridBagLayout());

        PersonnelEventLogModel eventModel = new PersonnelEventLogModel();
        eventModel.setData(orderedLogs);
        JTable eventTable = new JTable(eventModel);
        eventTable.getAccessibleContext().setAccessibleName(accessibleName + " for " + unit.getName());
        eventTable.setRowSelectionAllowed(false);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumn column;
        for (int i = 0; i < eventModel.getColumnCount(); ++i) {
            column = eventTable.getColumnModel().getColumn(i);
            column.setCellRenderer(eventModel.getRenderer());
            column.setPreferredWidth(eventModel.getPreferredWidth(i));
            if (eventModel.hasConstantWidth(i)) {
                column.setMinWidth(eventModel.getPreferredWidth(i));
                column.setMaxWidth(eventModel.getPreferredWidth(i));
            }
        }
        eventTable.setIntercellSpacing(new Dimension(0, 0));
        eventTable.setShowGrid(false);
        eventTable.setTableHeader(null);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLog.add(eventTable, gridBagConstraints);

        return pnlLog;
    }

    private void fillStats() {
        pnlStats.setLayout(new GridBagLayout());

        JLabel lblType = new JLabel();
        lblType.setName("lblType");
        lblType.setText("<html><i>" + unit.getTypeDisplayableNameWithOmni() + "</i></html>");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblType, gridBagConstraints);

        // Constraints for the left column, containing labels
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        labelConstraints.fill = GridBagConstraints.NONE;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;

        // Constraints for the right column, containing values
        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = 1;
        valueConstraints.weightx = 0.5;
        valueConstraints.insets = new Insets(0, 10, 0, 0);
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.NORTHWEST;

        JLabel lblTech = new JLabel();
        lblTech.setName("lblTech1");
        lblTech.setText(getTextAt(RESOURCE_BUNDLE, "lblTech1.text"));
        pnlStats.add(lblTech, labelConstraints);

        JLabel txtTech = new JLabel();
        txtTech.setName("lblTech2");
        txtTech.setText(TechConstants.getLevelDisplayableName(entity.getTechLevel()));
        pnlStats.add(txtTech, valueConstraints);

        labelConstraints.gridy++;
        valueConstraints.gridy++;

        JLabel lblTonnage = new JLabel();
        lblTonnage.setName("lblTonnage1");
        lblTonnage.setText(getTextAt(RESOURCE_BUNDLE, "lblTonnage1.text"));
        pnlStats.add(lblTonnage, labelConstraints);

        JLabel txtTonnage = new JLabel();
        txtTonnage.setName("lblTonnage2");
        txtTonnage.setText(Double.toString(entity.getWeight()));
        pnlStats.add(txtTonnage, valueConstraints);

        labelConstraints.gridy++;
        valueConstraints.gridy++;

        JLabel lblBV = new JLabel();
        lblBV.setName("lblBV1");
        lblBV.setText(getTextAt(RESOURCE_BUNDLE, "lblBV1.text"));
        pnlStats.add(lblBV, labelConstraints);

        JLabel txtBV = new JLabel();
        txtBV.setName("lblBV2");
        txtBV.setText(Integer.toString(entity.calculateBattleValue(true, true)));
        pnlStats.add(txtBV, valueConstraints);

        labelConstraints.gridy++;
        valueConstraints.gridy++;

        JLabel lblCost = new JLabel();
        lblCost.setName("lblCost1");
        lblCost.setText(getTextAt(RESOURCE_BUNDLE, "lblCost1.text"));
        pnlStats.add(lblCost, labelConstraints);

        JLabel txtCost = new JLabel();
        txtCost.setName("lblCost2");
        txtCost.setText(unit.getSellValue().toAmountAndSymbolString());
        pnlStats.add(txtCost, valueConstraints);

        labelConstraints.gridy++;
        valueConstraints.gridy++;

        if (campaign.getCampaignOptions().get(CampaignOption.USE_QUIRKS) && (entity.countQuirks() > 0)) {
            JLabel lblQuirk = new JLabel();
            lblQuirk.setName("lblQuirk1");
            lblQuirk.setText(getTextAt(RESOURCE_BUNDLE, "lblQuirk1.text"));
            pnlStats.add(lblQuirk, labelConstraints);

            for (IOption quirk : unit.getQuirks()) {
                JLabel label = new JLabel(quirk.getDisplayableNameWithValue());
                label.setToolTipText(MultiLineTooltip.splitToolTip(quirk.getDescription()));
                label.setName("quirk" + quirk.getName());
                pnlStats.add(label, valueConstraints);

                labelConstraints.gridy++;
                valueConstraints.gridy++;
            }
        }

        // Add a dummy element at the end of the panel with a non-zero weighty
        // to soak up any additional vertical space: Otherwise, the components
        // will clump in the center of the panel.
        GridBagConstraints dummyConstraints = new GridBagConstraints();
        dummyConstraints.gridwidth = 2;
        dummyConstraints.weighty = 1.0;
        pnlStats.add(new Panel(), dummyConstraints);
    }
}
