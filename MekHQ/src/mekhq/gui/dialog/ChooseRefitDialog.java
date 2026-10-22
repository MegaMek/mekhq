/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import jakarta.annotation.Nullable;
import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.StringUtility;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.RefitDesignSearch;
import mekhq.campaign.parts.RefitFacility;
import mekhq.campaign.parts.RefitPartOrders;
import mekhq.campaign.parts.RefitPartsStatus;
import mekhq.campaign.parts.RefitPurchaseCheck;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.PlanetaryAcquisitionModifiers;
import mekhq.campaign.work.WorkTimeEstimate;
import mekhq.campaign.work.WorkTimeFormatter;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * @author Taharqa
 */
public class ChooseRefitDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(ChooseRefitDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChooseRefitDialog";
    private static final String PARTS_RESOURCE_BUNDLE = "mekhq.resources.Parts";
    /** The order confirmation's button that closes the refit window as well */
    private static final int ORDERED_CONFIRM_CLOSE_WINDOW = 1;
    /** The smallest a panel can be dragged to, so none disappears behind a divider */
    private static final int MINIMUM_PANEL_SIZE = 100;

    // region Variable Declarations
    private final Campaign campaign;
    private final Unit unit;
    private RefitTableModel refitModel;
    private RefitDesignSearch.Result designSearch;
    private int failedRefitCheck;

    private JButton btnRefit;
    private JButton btnCustomize;
    private JButton btnOrderParts;
    private JCheckBox chkOnlySupportedRefits;
    private JTable refitTable;
    private TableRowSorter<RefitTableModel> refitSorter;
    private JScrollPane scrShoppingList;
    private JTextPane txtNewUnit;
    private JScrollPane scrOldUnit;
    private JScrollPane scrNewUnit;
    private JSplitPane mainSplit;
    private JSplitPane refitListSplit;
    private JSplitPane unitReadoutSplit;

    private boolean confirmed = false;
    private boolean customize = false;
    // endregion Variable Declarations

    // region Constructors

    /** Creates new form EditPersonnelLogDialog */
    public ChooseRefitDialog(JFrame parent, boolean modal, Campaign c, Unit unit) {
        super(parent, modal);
        campaign = c;
        this.unit = unit;
        populateRefits();
        initComponents();
        showWhyNoRefitsWhenEmpty();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    // endregion Constructors

    // region Initialization
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseRefitDialog",
              MekHQ.getMHQOptions().getLocale());

        setTitle(resourceMap.getString("title.text") + " " + unit.getName());

        getContentPane().setLayout(new GridBagLayout());

        refitTable = new JTable(refitModel);
        TableColumn column;
        for (int i = 0; i < RefitTableModel.N_COL; i++) {
            column = refitTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(refitModel.getColumnWidth(i));
            column.setCellRenderer(refitModel.getRenderer());
        }
        refitTable.setIntercellSpacing(new Dimension(0, 0));
        refitTable.setShowGrid(false);
        refitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refitTable.getSelectionModel().addListSelectionListener(evt -> refitTableValueChanged());
        refitSorter = new TableRowSorter<>(refitModel);
        refitSorter.setComparator(RefitTableModel.COL_CLASS, new ClassSorter());
        refitSorter.setComparator(RefitTableModel.COL_COST, new FormattedNumberSorter());
        refitTable.setRowSorter(refitSorter);
        JScrollPane scrRefitTable = new FastJScrollPane();
        scrRefitTable.setViewportView(refitTable);
        scrRefitTable.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("refitTable.title")));

        scrShoppingList = new FastJScrollPane();
        scrShoppingList.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "shoppingList.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrShoppingList.setMinimumSize(new Dimension(MINIMUM_PANEL_SIZE, MINIMUM_PANEL_SIZE));
        scrShoppingList.setPreferredSize(new Dimension(300, 200));

        JTextPane txtOldUnit = new JTextPane();
        txtOldUnit.setEditable(false);
        txtOldUnit.setContentType("text/html");
        txtOldUnit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "txtOldUnit.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        EntityReadout mv = EntityReadout.createReadout(unit.getEntity(), false, true, true);
        txtOldUnit.setText("<div style='font: 12pt monospaced'>" + mv.getFullReadout() + "</div>");
        scrOldUnit = new FastJScrollPane(txtOldUnit);
        scrOldUnit.setMinimumSize(new Dimension(MINIMUM_PANEL_SIZE, MINIMUM_PANEL_SIZE));
        scrOldUnit.setPreferredSize(new Dimension(300, 400));
        SwingUtilities.invokeLater(() -> scrOldUnit.getVerticalScrollBar().setValue(0));

        txtNewUnit = new JTextPane();
        txtNewUnit.setEditable(false);
        txtNewUnit.setContentType("text/html");
        txtNewUnit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "txtNewUnit.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrNewUnit = new FastJScrollPane(txtNewUnit);
        scrNewUnit.setMinimumSize(new Dimension(MINIMUM_PANEL_SIZE, MINIMUM_PANEL_SIZE));
        scrNewUnit.setPreferredSize(new Dimension(300, 400));

        // Each border between the panels can be dragged, and the dialog remembers where it was left
        refitListSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrRefitTable, scrShoppingList);
        refitListSplit.setName("refitListSplit");
        refitListSplit.setResizeWeight(0.7);
        unitReadoutSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrOldUnit, scrNewUnit);
        unitReadoutSplit.setName("unitReadoutSplit");
        unitReadoutSplit.setResizeWeight(0.5);
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, refitListSplit, unitReadoutSplit);
        mainSplit.setName("mainSplit");
        mainSplit.setResizeWeight(0.4);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(mainSplit, gridBagConstraints);

        JPanel panBtn = new JPanel(new GridBagLayout());

        // Hides the refits the unit's repair site cannot support (issue #3437); the dialog remembers the choice
        chkOnlySupportedRefits = new JCheckBox(getFormattedTextAt(RESOURCE_BUNDLE, "chkOnlySupportedRefits.text",
              Unit.getSiteName(unit.getSite())));
        chkOnlySupportedRefits.setName("chkOnlySupportedRefits");
        chkOnlySupportedRefits.setToolTipText(getTextAt(RESOURCE_BUNDLE, "chkOnlySupportedRefits.tooltip"));
        chkOnlySupportedRefits.addItemListener(event -> filterRefitsBySite());
        panBtn.add(chkOnlySupportedRefits, new GridBagConstraints());

        // Orders what the refit needs without starting it, so the unit stays in service while they arrive (#7848)
        btnOrderParts = new JButton(getTextAt(RESOURCE_BUNDLE, "btnOrderParts.text"));
        btnOrderParts.setEnabled(false);
        btnOrderParts.addActionListener(event -> orderParts());
        panBtn.add(btnOrderParts, new GridBagConstraints());

        btnRefit = new JButton(resourceMap.getString("btnRefit.text"));
        btnRefit.setEnabled(false);
        btnRefit.addActionListener(evt -> confirmRefit());
        panBtn.add(btnRefit, new GridBagConstraints());

        btnCustomize = new JButton(resourceMap.getString("btnCustomize.text"));
        btnCustomize.setEnabled(false);
        btnCustomize.addActionListener(evt -> confirmCustomize());
        panBtn.add(btnCustomize, new GridBagConstraints());

        JButton btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.setToolTipText(getTextAt(RESOURCE_BUNDLE, "btnClose.tooltip"));
        btnClose.addActionListener(evt -> cancel());
        panBtn.add(btnClose, new GridBagConstraints());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(panBtn, gridBagConstraints);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChooseRefitDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
            preferences.manage(new JSplitPanePreference(mainSplit));
            preferences.manage(new JSplitPanePreference(refitListSplit));
            preferences.manage(new JSplitPanePreference(unitReadoutSplit));
            preferences.manage(new JToggleButtonPreference(chkOnlySupportedRefits));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }
    // endregion Initialization

    private void confirmRefit() {
        confirmed = getSelectedRefit() != null;
        customize = false;
        setVisible(false);
    }

    private void confirmCustomize() {
        confirmed = getSelectedRefit() != null;
        customize = true;
        setVisible(false);
    }

    private void cancel() {
        setVisible(false);
    }

    /**
     * Shows only the refits the unit's repair site supports, while the checkbox is ticked.
     */
    private void filterRefitsBySite() {
        if (!chkOnlySupportedRefits.isSelected()) {
            refitSorter.setRowFilter(null);
            return;
        }
        refitSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends RefitTableModel, ? extends Integer> entry) {
                Refit refit = entry.getModel().getRefitAt(entry.getIdentifier());
                return RefitFacility.isSiteGoodEnough(refit, unit.getSite());
            }
        });
        logger.debug("[RefitFacility] {}: showing only refits {} supports, {} of {}", unit.getName(),
              Unit.getSiteName(unit.getSite()), refitTable.getRowCount(), refitModel.getRowCount());
    }

    /**
     * Orders everything the selected refit still needs, then shows the parts as on order and has the senior
     * administrator say what was ordered and what to do next.
     */
    private void orderParts() {
        Refit refit = getSelectedRefit();
        if (refit == null) {
            return;
        }
        RefitPartOrders.OrderResult result = RefitPartOrders.orderMissingParts(campaign, refit);
        // Other refits of this unit may share the parts just ordered
        refitModel.forgetPartsStatuses();
        refitTable.repaint();
        refitTableValueChanged();
        confirmOrders(refit, result);
    }

    /**
     * Tells the player what Order Parts did and what comes next: begin the refit now and it waits for the parts, or
     * close the window and come back once they arrive, while the Hangar shows the unit as Pending Refit. The player
     * can go back to the refit options or close the refit window from the confirmation.
     */
    private void confirmOrders(Refit refit, RefitPartOrders.OrderResult result) {
        String orderedKey = result.ordered().isEmpty() ? "orderedConfirm.labelled" : "orderedConfirm.ordered";
        StringBuilder message = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE, orderedKey,
              campaign.getCommanderAddress(), unit.getName(), refit.getNewEntity().getModel(),
              String.join(", ", result.ordered())));
        if (!result.cannotBeBought().isEmpty()) {
            message.append(' ').append(getFormattedTextAt(RESOURCE_BUNDLE, "orderedConfirm.cannotBeBought",
                  String.join(", ", result.cannotBeBought())));
        }
        String closeWindow = getTextAt(RESOURCE_BUNDLE, "orderedConfirm.button.closeWindow");
        String nextSteps = getFormattedTextAt(RESOURCE_BUNDLE, "orderedConfirm.nextSteps", btnRefit.getText(),
              btnCustomize.getText(), closeWindow);
        // Back is first, so closing the confirmation with X leaves the refit window open
        List<String> buttons = List.of(getTextAt(RESOURCE_BUNDLE, "orderedConfirm.button.back"), closeWindow);

        Person speaker = campaign.getPlayerForce()
                               .getHumanResources()
                               .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                     campaign.getPlayerForce().isClanForce(), campaign.getLocalDate());
        ImmersiveDialogSimple confirmation = new ImmersiveDialogSimple(campaign, speaker, null, message.toString(),
              buttons, nextSteps, null, false);
        if (confirmation.getDialogChoice() == ORDERED_CONFIRM_CLOSE_WINDOW) {
            logger.debug("[RefitParts] {}: refit window closed after ordering parts", unit.getName());
            cancel();
        }
    }

    /**
     * Selects the refit to the given model, if the list offers it, so a refit whose parts were ordered ahead opens
     * ready to begin. A refit hidden by the repair site filter is not selected.
     *
     * @param model the model to select, such as {@code UZL-3S}, or {@code null} to select nothing
     */
    public void selectRefitModel(@Nullable String model) {
        if (model == null) {
            return;
        }
        for (int row = 0; row < refitModel.getRowCount(); row++) {
            if (!model.equals(refitModel.getRefitAt(row).getNewEntity().getModel())) {
                continue;
            }
            int viewRow = refitTable.convertRowIndexToView(row);
            if (viewRow < 0) {
                logger.debug("[RefitParts] {}: the {} refit is hidden by the site filter", unit.getName(), model);
                return;
            }
            refitTable.setRowSelectionInterval(viewRow, viewRow);
            refitTable.scrollRectToVisible(refitTable.getCellRect(viewRow, 0, true));
            return;
        }
        logger.debug("[RefitParts] {}: no {} refit is offered to select", unit.getName(), model);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isCustomize() {
        return customize;
    }

    public Refit getSelectedRefit() {
        int selectedRow = refitTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return refitModel.getRefitAt(refitTable.convertRowIndexToModel(selectedRow));
    }

    private void refitTableValueChanged() {
        Refit r = getSelectedRefit();
        if (null == r) {
            scrShoppingList.setViewportView(null);
            scrShoppingList.setColumnHeaderView(null);
            txtNewUnit.setText("");
            btnRefit.setEnabled(false);
            btnCustomize.setEnabled(false);
            btnOrderParts.setEnabled(false);
            return;
        }

        boolean isAllowedDesign = !campaign.getCampaignOptions().get(CampaignOption.ALLOW_CANON_REFIT_ONLY)
              || r.getNewEntity().isCanon();
        boolean canBuyKit = !RefitPurchaseCheck.isKitWithPartsThatCannotBeBought(campaign, r);
        btnRefit.setEnabled(isAllowedDesign && canBuyKit);
        btnRefit.setToolTipText(canBuyKit ? null : getTextAt(RESOURCE_BUNDLE, "btnRefit.cannotBuy.tooltip"));
        btnCustomize.setEnabled(true);

        List<Refit.RefitNeed> needs = r.describeNeeds();
        boolean hasPartsToOrder = needs.stream().anyMatch(need -> need.supply() == Refit.Supply.TO_ORDER);
        // Orders placed some other way, or in an older save, are not yet labelled for this refit
        boolean hasUnlabelledOrders = !hasPartsToOrder && !unit.isPendingRefit() && needs.stream()
              .anyMatch(need -> (need.supply() == Refit.Supply.ON_ORDER) || (need.supply() == Refit.Supply.ON_ITS_WAY));
        btnOrderParts.setEnabled(hasPartsToOrder || hasUnlabelledOrders);
        String tooltipKey = hasPartsToOrder ? "btnOrderParts.tooltip"
              : (hasUnlabelledOrders ? "btnOrderParts.labelOrders.tooltip" : "btnOrderParts.nothingToOrder.tooltip");
        btnOrderParts.setToolTipText(getTextAt(RESOURCE_BUNDLE, tooltipKey));

        JList<String> lstShopping = new JList<>(listNeeds(needs).toArray(new String[0]));
        scrShoppingList.setViewportView(lstShopping);
        scrShoppingList.setColumnHeaderView(createPartsStatusLabel(refitModel.getPartsStatus(r)));
        EntityReadout mv = EntityReadout.createReadout(r.getNewEntity(), false, true);
        txtNewUnit.setText("<div style='font: 12pt monospaced'>" + mv.getFullReadout() + "</div>");
        SwingUtilities.invokeLater(() -> scrNewUnit.getVerticalScrollBar().setValue(0));
    }

    /**
     * @return a line above the parts list saying where the refit stands for parts, in the colour the refit list uses
     */
    private static JLabel createPartsStatusLabel(RefitPartsStatus partsStatus) {
        JLabel statusLabel = new JLabel("<html>" + partsStatus.describe() + "</html>");
        Color statusColor = getPartsStatusColor(partsStatus.state());
        if (statusColor != null) {
            statusLabel.setForeground(statusColor);
        }
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return statusLabel;
    }

    /**
     * @return what the refit needs, under a heading for what the warehouse holds, what is on its way, what is on
     *       order and what must still be ordered
     */
    private static List<String> listNeeds(List<Refit.RefitNeed> needs) {
        List<String> lines = new ArrayList<>();
        Refit.Supply lastSupply = null;
        // The needs come grouped by where they come from
        for (Refit.RefitNeed need : needs) {
            if (need.supply() != lastSupply) {
                lines.add(getTextAt(PARTS_RESOURCE_BUNDLE, getSupplyHeadingKey(need.supply())));
                lastSupply = need.supply();
            }
            lines.add("   " + need.description());
        }
        return lines;
    }

    private static String getSupplyHeadingKey(Refit.Supply supply) {
        return switch (supply) {
            case IN_STOCK -> "RefitPartsStatus.heading.inStock";
            case ON_ITS_WAY -> "RefitPartsStatus.heading.onItsWay";
            case ON_ORDER -> "RefitPartsStatus.heading.onOrder";
            case TO_ORDER -> "RefitPartsStatus.heading.toOrder";
        };
    }

    /**
     * @return green when the refit can start now or has nothing left to order, amber when the warehouse covers part
     *       of it, the normal text colour when everything must be ordered, and red when some parts cannot be bought
     */
    private static @Nullable Color getPartsStatusColor(RefitPartsStatus.State state) {
        return switch (state) {
            case READY, WAITING_FOR_DELIVERY -> MekHQ.getMHQOptions().getFontColorPositive();
            case PARTLY_IN_STOCK -> MekHQ.getMHQOptions().getFontColorWarning();
            case TO_ORDER -> null;
            case CANNOT_BUY -> MekHQ.getMHQOptions().getFontColorNegative();
        };
    }

    private void populateRefits() {
        List<Refit> refits = new ArrayList<>();
        Entity entity = unit.getEntity();
        String chassis = entity.getFullChassis();
        designSearch = RefitDesignSearch.findDesigns(entity, campaign);
        failedRefitCheck = 0;
        for (String model : designSearch.models()) {
            model = StringUtility.isNullOrBlank(model) ? "" : " " + model;
            try {
                MekSummary summary = Utilities.retrieveUnit(chassis + model);
                Entity refitEntity = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                if (null != refitEntity) {
                    Refit refit = new Refit(unit, refitEntity, false, false, false);
                    String refitProblem = refit.checkFixable();
                    if (null == refitProblem) {
                        refits.add(refit);
                    } else {
                        logger.trace("[RefitDesigns] {}: refused by the refit check: {}", summary.getName(),
                              refitProblem);
                        failedRefitCheck++;
                    }
                }
            } catch (EntityLoadingException exception) {
                logger.error("", exception);
            }
        }
        refitModel = new RefitTableModel(refits);
    }

    /** When no refit can be offered, tells the player why in the panel that would show the selected design. */
    private void showWhyNoRefitsWhenEmpty() {
        if (refitModel.getRowCount() > 0) {
            return;
        }
        logger.debug("[RefitDesigns] {}: no refits offered, {} refused by the refit check", unit.getName(),
              failedRefitCheck);
        txtNewUnit.setText(designSearch.describeEmptyList(unit.getName(), failedRefitCheck));
    }

    /**
     * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
     */
    public class RefitTableModel extends AbstractTableModel {
        protected String[] columnNames;
        protected List<Refit> data;

        public final static int COL_MODEL = 0;
        public final static int COL_CLASS = 1;
        public final static int COL_BV = 2;
        public final static int COL_TIME = 3;
        public final static int COL_NUM_PART = 4;
        public final static int COL_TARGET = 5;
        public final static int COL_COST = 6;
        public final static int COL_FACILITY = 7;
        public final static int N_COL = 8;

        // Worked out once for each refit, since the table repaints often
        private final Map<Refit, RefitPartsStatus> partsStatusByRefit = new HashMap<>();

        public RefitTableModel(List<Refit> refits) {
            data = refits;
        }

        /**
         * @return whether the refit needs parts ordered, or has some that cannot be bought
         */
        public RefitPartsStatus getPartsStatus(Refit refit) {
            return partsStatusByRefit.computeIfAbsent(refit, key -> RefitPartsStatus.of(campaign, key));
        }

        /**
         * Works the parts status out again next time, after parts have been ordered.
         */
        public void forgetPartsStatuses() {
            partsStatusByRefit.clear();
        }

        /**
         * @return {@code true} if the unit's repair site is too low for the refit's class
         */
        private boolean isSiteTooLow(Refit refit) {
            return !RefitFacility.isSiteGoodEnough(refit, unit.getSite());
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_MODEL -> "Model";
                case COL_CLASS -> "Class";
                case COL_BV -> "BV";
                case COL_TIME -> "Time";
                case COL_NUM_PART -> "# Parts";
                case COL_COST -> "Cost";
                case COL_TARGET -> "Kit TN";
                case COL_FACILITY -> getTextAt(RESOURCE_BUNDLE, "refitTable.facility.column");
                default -> "?";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (data.isEmpty()) {
                return "";
            }
            Refit r = data.get(row);

            if (col == COL_MODEL) {
                return r.getNewEntity().getModel();
            } else if (col == COL_CLASS) {
                return r.getRefitClassName();
            } else if (col == COL_BV) {
                return r.getNewEntity().calculateBattleValue(true, true);
            } else if (col == COL_TIME) {
                return new RefitTime(r.getTime(), WorkTimeFormatter.formatDuration(r.getTime(),
                      getRefitMinutesPerDay(), campaign.getLocalDate(), MekHQ.getMHQOptions().getWorkTimeDisplay()));
            } else if (col == COL_NUM_PART) {
                return r.getShoppingList().size();
            } else if (col == COL_COST) {
                return r.getCost().toAmountAndSymbolString();
            } else if (col == COL_TARGET) {
                return PlanetaryAcquisitionModifiers.addForCurrentSystem(campaign, r, campaign.checkAcquisition(r))
                             .getTargetNumber()
                             .getValueAsString();
            } else if (col == COL_FACILITY) {
                return RefitFacility.describeRequiredSite(r);
            } else {
                return "?";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Refit getRefitAt(int row) {
            return data.get(row);
        }

        public int getColumnWidth(int c) {
            return switch (c) {
                case COL_MODEL -> 75;
                case COL_CLASS -> 110;
                case COL_COST -> 40;
                case COL_FACILITY -> 90;
                default -> 10;
            };
        }

        public int getAlignment(int col) {
            return switch (col) {
                case COL_MODEL, COL_CLASS, COL_FACILITY -> SwingConstants.LEFT;
                default -> SwingConstants.RIGHT;
            };
        }

        public String getTooltip(int row, int col) {
            Refit r;
            if (data.isEmpty()) {
                return "";
            } else {
                r = data.get(row);
            }

            if (col == COL_TARGET) {
                return PlanetaryAcquisitionModifiers.addForCurrentSystem(campaign, r, campaign.checkAcquisition(r))
                             .getTargetNumber()
                             .getDesc();
            }
            if (col == COL_TIME) {
                return WorkTimeFormatter.describeWorkingDays(r.getTime(), getRefitMinutesPerDay());
            }
            if (col == COL_MODEL) {
                return getPartsStatus(r).describe();
            }
            if (col == COL_FACILITY) {
                String key = isSiteTooLow(r) ? "refitTable.facility.tooltip.tooLow" : "refitTable.facility.tooltip.ok";
                return getFormattedTextAt(RESOURCE_BUNDLE, key, RefitFacility.describeRequiredSite(r),
                      Unit.getSiteName(unit.getSite()));
            }

            return null;
        }

        /**
         * @return the minutes a tech puts into a refit each day; no tech is chosen yet, so a plain working day
         */
        private int getRefitMinutesPerDay() {
            return WorkTimeEstimate.getMinutesPerDay(campaign, null, false);
        }

        // fill table with values
        public void setData(ArrayList<Refit> refits) {
            data = refits;
            fireTableDataChanged();
        }

        public Renderer getRenderer() {
            return new Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));
                // The model name is coloured by whether the refit needs parts ordered
                Color foregroundColor = null;
                boolean isColouredByParts = (actualCol == COL_MODEL) && !isSelected && !data.isEmpty();
                if (isColouredByParts) {
                    foregroundColor = getPartsStatusColor(getPartsStatus(data.get(actualRow)).state());
                }
                // The facility is shown in red when the unit's site is too low for the refit
                boolean isFacilityTooLow = (actualCol == COL_FACILITY) && !isSelected && !data.isEmpty()
                      && isSiteTooLow(data.get(actualRow));
                if (isFacilityTooLow) {
                    foregroundColor = MekHQ.getMHQOptions().getFontColorNegative();
                }
                if (foregroundColor != null) {
                    setForeground(foregroundColor);
                } else {
                    setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                }

                return this;
            }
        }
    }

    /**
     * A refit's time as the table shows it, sorted by its minutes of work rather than by its text.
     *
     * @param minutes minutes of work
     * @param text    the time as the player's chosen display shows it
     */
    public record RefitTime(int minutes, String text) implements Comparable<RefitTime> {
        @Override
        public int compareTo(RefitTime other) {
            return Integer.compare(minutes, other.minutes);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     *
     * @author Jay Lawson
     */
    public static class FormattedNumberSorter implements Comparator<String> {
        @Override
        public int compare(String s0, String s1) {
            // let's find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            int l0 = 0;
            try {
                l0 = format.parse(s0).intValue();
            } catch (ParseException e) {
                logger.error("", e);
            }
            int l1 = 0;
            try {
                l1 = format.parse(s1).intValue();
            } catch (ParseException e) {
                logger.error("", e);
            }
            return ((Comparable<Integer>) l0).compareTo(l1);
        }
    }

    /**
     * A comparator for refit classes
     *
     * @author Jay Lawson
     */
    public static class ClassSorter implements Comparator<String> {
        @Override
        public int compare(String s0, String s1) {
            int r0 = Refit.NO_CHANGE;
            int r1 = Refit.NO_CHANGE;
            if (s0.contains("Omni")) {
                r0 = Refit.CLASS_OMNI;
            } else if (s0.contains("Class A")) {
                r0 = Refit.CLASS_A;
            } else if (s0.contains("Class B")) {
                r0 = Refit.CLASS_B;
            } else if (s0.contains("Class C")) {
                r0 = Refit.CLASS_C;
            } else if (s0.contains("Class D")) {
                r0 = Refit.CLASS_D;
            } else if (s0.contains("Class E")) {
                r0 = Refit.CLASS_E;
            } else if (s0.contains("Class F")) {
                r0 = Refit.CLASS_F;
            }

            if (s1.contains("Omni")) {
                r1 = Refit.CLASS_OMNI;
            } else if (s1.contains("Class A")) {
                r1 = Refit.CLASS_A;
            } else if (s1.contains("Class B")) {
                r1 = Refit.CLASS_B;
            } else if (s1.contains("Class C")) {
                r1 = Refit.CLASS_C;
            } else if (s1.contains("Class D")) {
                r1 = Refit.CLASS_D;
            } else if (s1.contains("Class E")) {
                r1 = Refit.CLASS_E;
            } else if (s1.contains("Class F")) {
                r1 = Refit.CLASS_F;
            }
            return ((Comparable<Integer>) r0).compareTo(r1);
        }
    }
}
