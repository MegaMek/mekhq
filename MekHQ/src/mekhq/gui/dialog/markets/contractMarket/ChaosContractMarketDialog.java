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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.*;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ContractMarket;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractMarketAvailability;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.utilities.ContractAcceptance;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * The Chaos contract market: a "job board" of 0-3 available {@link AbstractContract} offers that expands into a full
 * briefing dossier for whichever offer the player selects.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosContractMarketDialog extends JDialog implements ContractMarketActions {
    private static final MMLogger LOGGER = MMLogger.create(ChaosContractMarketDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private final int PADDING = scaleForGUI(5);
    private static final Dimension MINIMUM_SIZE = scaleForGUI(760, 620);
    private static final Dimension DEFAULT_SIZE = scaleForGUI(880, 900);

    private static final int GENERATED_OFFERS_PER_BATCH = 1;

    private final transient Campaign campaign;
    private final JFrame parent;
    private final LocalDate currentDate;
    private final transient ContractMarket contractMarket;
    private final transient List<AbstractContract> contracts;
    private transient ContractSearchType searchType;

    private final transient List<ContractCardPanel> cards = new ArrayList<>();
    private JPanel content;
    private JPanel dossierContainer;
    private transient AbstractContract acceptedContract;

    /**
     * On-departure automation options the player toggles before accepting an offer. They are fields (not rebuilt with
     * the button bar) so their state persists across {@link #rebuildContent()} calls; read them via
     * {@link #isMothballOnDepartureSelected()} and {@link #isTravelToSystemSelected()} when committing the accepted
     * contract.
     */
    private final JCheckBox mothballOnDepartureCheckbox = buildOptionCheckbox("checkbox.contractMarket.mothball",
          "checkbox.contractMarket.mothball.tooltip");
    private final JCheckBox travelToSystemCheckbox = buildOptionCheckbox("checkbox.contractMarket.travel",
          "checkbox.contractMarket.travel.tooltip");
    /** Only shown (and only meaningful) when the campaign uses StratCon; unticked leaves the contract without one. */
    private final JCheckBox useStratConCheckbox = buildOptionCheckbox("checkbox.contractMarket.stratcon",
          "checkbox.contractMarket.stratcon.tooltip");

    /**
     * Constructs and shows the contract market backed by the player force's {@link ContractMarket}.
     *
     * <p>The board shows the offers stored under the campaign's default {@link ContractSearchType}; switching type in
     * the header swaps to that type's map (see {@link #loadOffersForSearchType()}).</p>
     *
     * @param campaign the active campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ChaosContractMarketDialog(Campaign campaign) {
        super(campaign.getGUI().getFrame(), true);
        this.campaign = campaign;
        this.parent = campaign.getGUI().getFrame();
        this.currentDate = campaign.getLocalDate();
        this.contractMarket = campaign.getPlayerForce().getContractMarket();
        this.searchType = defaultSearchType(campaign);
        this.contracts = new ArrayList<>();
        loadOffersForSearchType();

        initializeComponents();
    }

    /**
     * Returns the offer the player accepted, or {@code null} if they closed the market without accepting one.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public AbstractContract getAcceptedContract() {
        return acceptedContract;
    }

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "title.contractMarket"));

        content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        rebuildContent();

        getContentPane().add(content);

        setMinimumSize(MINIMUM_SIZE);
        pack();
        setSize(DEFAULT_SIZE); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(parent);
        setPreferences(this); // Must be before setVisible
        setVisible(true); // Should always be last
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "title.contractMarket"));
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() + 4f));
        header.add(title, gbc);

        gbc.gridy = 1;
        // getCurrentSystem() is null when the player force has no known location (e.g. in transit), so fall back to a
        // placeholder rather than dereferencing it.
        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        final String locationName = currentSystem != null
                                          ? currentSystem.getName(currentDate)
                                          : getTextAt(RESOURCE_BUNDLE, "header.contractMarket.location.unknown");
        JLabel subtitle = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "header.contractMarket.location",
              locationName,
              currentDate));
        header.add(subtitle, gbc);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel count = new JLabel(countMessage());
        count.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(count);
        JPanel searchTypeControl = buildSearchTypeControl();
        searchTypeControl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(searchTypeControl);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        header.add(right, gbc);

        return header;
    }

    private JPanel buildSearchTypeControl() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, PADDING, 0));
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "header.contractMarket.searchType")));

        List<ContractSearchType> allowed = allowedSearchTypes(campaign);
        MMComboBox<ContractSearchType> combo = new MMComboBox<>("contractSearchType");
        combo.setModel(new DefaultComboBoxModel<>(allowed.toArray(new ContractSearchType[0])));
        combo.setSelectedItem(searchType);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContractSearchType type) {
                    setText(searchTypeLabel(type));
                }
                return this;
            }
        });
        combo.setEnabled(allowed.size() > 1);
        combo.addActionListener(e -> {
            ContractSearchType selected = combo.getSelectedItem();
            if (selected != null && selected != searchType) {
                changeSearchType(selected);
            }
        });
        panel.add(combo);
        return panel;
    }

    private static String searchTypeLabel(ContractSearchType type) {
        return getTextAt(RESOURCE_BUNDLE, "searchType.contractMarket." + type.name());
    }

    /**
     * Switches the search type and shows that type's offers from the {@link ContractMarket}. Types whose generation is
     * not yet implemented (pirate, government, tournament) have empty maps, so the board falls to its empty state until
     * their generation lands.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void changeSearchType(ContractSearchType newType) {
        searchType = newType;
        loadOffersForSearchType();
        rebuildContent();
    }

    /**
     * Mirrors the active search type's {@link ContractMarket} map into the visible {@link #contracts} list. The market
     * is populated by the monthly refresh ({@link ChaosContractMarketAvailability#processNewMonth(Campaign)}), so the
     * dialog only displays whatever offers are currently available - an empty board is a valid state.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void loadOffersForSearchType() {
        contracts.clear();
        contracts.addAll(contractMarket.getContracts(searchType).values());
    }

    private String countMessage() {
        int contractCount = contracts.size();
        if (contractCount == 0) {
            return getTextAt(RESOURCE_BUNDLE, "header.contractMarket.count.none");
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, "header.contractMarket.count", contractCount);
    }

    /**
     * Rebuilds the whole content area (header, board or empty state, and button bar) so the dialog reflects the current
     * set of offers after one is deleted or all are cleared.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void rebuildContent() {
        content.removeAll();
        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(contracts.isEmpty() ? buildEmptyState() : buildBoard(), BorderLayout.CENTER);
        content.add(buildButtonBar(), BorderLayout.SOUTH);
        content.revalidate();
        content.repaint();
    }

    private JPanel buildBoard() {
        cards.clear();
        JPanel board = new JPanel(new BorderLayout(0, PADDING));

        // The cards stay on a single row (a plain FlowLayout keeps its one-row preferred width), and the
        // FastJScrollPane scrolls horizontally when they overflow the window width rather than wrapping to a new row.
        JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, PADDING));
        for (AbstractContract contract : contracts) {
            ContractCardPanel card = new ContractCardPanel(contract, currentDate, this::selectContract);
            cards.add(card);
            cardRow.add(card);
        }
        equalizeCardHeights();

        FastJScrollPane cardScroll = new FastJScrollPane(cardRow,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardScroll.setBorder(null);
        cardScroll.getHorizontalScrollBar().setUnitIncrement(scaleForGUI(16));
        int cardRowHeight = cards.getFirst().getPreferredSize().height + 2 * PADDING
                                  + cardScroll.getHorizontalScrollBar().getPreferredSize().height;
        cardScroll.setPreferredSize(new Dimension(scaleForGUI(1), cardRowHeight));
        board.add(cardScroll, BorderLayout.NORTH);

        // A width-tracking view lets the dossier shrink with the dialog: without it, the viewport keeps the panel at
        // its wider preferred width when the window is made smaller, so the content never reflows narrower.
        dossierContainer = new WidthTrackingPanel(new BorderLayout());
        FastJScrollPane dossierScroll = new FastJScrollPane(dossierContainer,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dossierScroll.setBorder(null);
        dossierScroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        board.add(dossierScroll, BorderLayout.CENTER);

        // Open on the first offer so the player never faces an empty detail pane.
        selectContract(contracts.getFirst());

        return board;
    }

    /**
     * Sets every card to the tallest card's height so a row of cards with differing name lengths lines up evenly.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void equalizeCardHeights() {
        int maxHeight = 0;
        for (ContractCardPanel card : cards) {
            maxHeight = Math.max(maxHeight, card.getPreferredSize().height);
        }
        for (ContractCardPanel card : cards) {
            card.setForcedHeight(maxHeight);
        }
    }


    private void selectContract(AbstractContract contract) {
        for (ContractCardPanel card : cards) {
            card.setSelected(card.getContract() == contract);
        }

        dossierContainer.removeAll();
        dossierContainer.add(new ContractDossierPanel(campaign, contract, currentDate, this),
              BorderLayout.NORTH);
        dossierContainer.revalidate();
        dossierContainer.repaint();
    }

    /**
     * Commits the offer to the campaign via {@link ContractAcceptance}, honoring the player's on-departure and StratCon
     * checkbox choices. If the player cancels at the confirmation nag the market stays open; otherwise the accepted
     * offer is recorded and the dialog closes.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void accept(AbstractContract contract) {
        boolean accepted = ContractAcceptance.accept(campaign,
              contract,
              searchType,
              isUseStratConSelected(),
              isMothballOnDepartureSelected(),
              isTravelToSystemSelected());
        if (!accepted) {
            return;
        }

        LOGGER.info("Contract accepted from market: {}", contract.getName());
        this.acceptedContract = contract;
        dispose();
    }

    /**
     * Opens the negotiation table for the offer. If the player confirms changes, the contract's terms, pay, and rented
     * facilities are updated in place, so the dossier is rebuilt to reflect them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void negotiate(AbstractContract contract) {
        ContractNegotiationDialog negotiation = new ContractNegotiationDialog(campaign, contract);
        if (negotiation.wasConfirmed()) {
            selectContract(contract);
        }
    }

    /**
     * Removes a single offer from the board and refreshes the dialog.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void delete(AbstractContract contract) {
        contractMarket.removeContract(searchType, contract);
        contracts.remove(contract);
        rebuildContent();
    }

    /**
     * Opens the GM editor for the offer. If the GM confirms changes, the contract is updated in place, so the dossier
     * is rebuilt to reflect them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void edit(AbstractContract contract) {
        ContractEditorDialog editor = new ContractEditorDialog(campaign, contract);
        if (editor.wasConfirmed()) {
            selectContract(contract);
        }
    }

    /**
     * Clears every offer from the board and refreshes the dialog to the empty state.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void deleteAllContracts() {
        contractMarket.getContracts(searchType).clear();
        contracts.clear();
        rebuildContent();
    }

    /**
     * Generates a fresh batch of offers and adds them to the board.
     *
     * <p>This is a GM tool, so it uses the same generator the market will eventually call, in GM mode, and simply
     * appends whatever valid contracts come back (a batch can legitimately be smaller than requested, or empty, if the
     * generator cannot place a contract). The first newly generated offer is selected so the result is visible.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void generateNewContracts() {
        List<AbstractContract> generated = generateOfferBatch();
        if (generated.isEmpty()) {
            LOGGER.info("Generate-new produced no valid contracts.");
            return;
        }

        Map<UUID, AbstractContract> marketOffers = contractMarket.getContracts(searchType);
        for (AbstractContract contract : generated) {
            marketOffers.put(contract.getId(), contract);
        }
        contracts.clear();
        contracts.addAll(marketOffers.values());
        rebuildContent();
        selectContract(generated.getFirst());
    }

    /**
     * Rolls up to {@link #GENERATED_OFFERS_PER_BATCH} contracts for the GM "Generate new" action.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private List<AbstractContract> generateOfferBatch() {
        return ChaosContractMarketAvailability.generateOffers(campaign, GENERATED_OFFERS_PER_BATCH, true, searchType);
    }

    /**
     * The search type a campaign defaults to: mercenary or pirate bands default to their own type, and every other
     * (government) campaign can only look for government contracts.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static ContractSearchType defaultSearchType(Campaign campaign) {
        Faction faction = campaign.getPlayerForce().getFaction();
        if (faction.isPirate()) {
            return ContractSearchType.PIRATE;
        }
        if (faction.isMercenary()) {
            return ContractSearchType.MERCENARY;
        }
        return ContractSearchType.GOVERNMENT;
    }

    /**
     * The search types a campaign may choose between. Mercenary and pirate bands may look for either mercenary or
     * pirate work; government campaigns are limited to government contracts. Tournament circuits are open to every
     * campaign.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static List<ContractSearchType> allowedSearchTypes(Campaign campaign) {
        Faction faction = campaign.getPlayerForce().getFaction();
        if (faction.isMercenary() || faction.isPirate()) {
            return List.of(ContractSearchType.MERCENARY, ContractSearchType.PIRATE, ContractSearchType.TOURNAMENT);
        }
        return List.of(ContractSearchType.GOVERNMENT, ContractSearchType.TOURNAMENT);
    }

    /**
     * Opens the GM editor on a fresh, fully-defaulted contract. If the GM confirms, the new offer is added to the
     * current search type's market and shown on the board.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void createNewContract() {
        AbstractContract contract = NewContractFactory.createBlank(campaign);
        ContractEditorDialog editor = new ContractEditorDialog(campaign, contract, searchType);
        if (!editor.wasConfirmed()) {
            return;
        }

        ContractSearchType bucket = editor.getSelectedSearchType();
        contractMarket.addContract(bucket, contract);

        // Switch the board to the bucket the GM chose so the new offer is visible.
        searchType = bucket;
        loadOffersForSearchType();
        rebuildContent();
        selectContract(contract);
    }

    private JPanel buildEmptyState() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(48), PADDING, scaleForGUI(48), PADDING));

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "empty.contractMarket.title"), SwingConstants.CENTER);
        title.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() + 2f));

        JLabel body = new JLabel("<html><body style='width:" +
                                       scaleForGUI(360)
                                       +
                                       "px; text-align:center'>" +
                                       getTextAt(RESOURCE_BUNDLE, "empty.contractMarket.body") +
                                       "</body></html>",
              SwingConstants.CENTER);
        body.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(PADDING));
        panel.add(body);
        return panel;
    }

    /**
     * Builds a persistent, initially-selected automation checkbox from its label and tooltip resource keys.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static JCheckBox buildOptionCheckbox(String labelKey, String tooltipKey) {
        JCheckBox checkbox = new JCheckBox(getTextAt(RESOURCE_BUNDLE, labelKey), true);
        checkbox.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, tooltipKey)));
        return checkbox;
    }

    /**
     * @return {@code true} if the player wants eligible units mothballed on departure when the accepted contract begins
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isMothballOnDepartureSelected() {
        return mothballOnDepartureCheckbox.isSelected();
    }

    /**
     * @return {@code true} if the player wants to automatically travel to the contract's system when the accepted
     *       contract begins
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isTravelToSystemSelected() {
        return travelToSystemCheckbox.isSelected();
    }

    /**
     * @return {@code true} if the accepted contract should use StratCon (only meaningful when the campaign uses
     *       StratCon; when {@code false} the contract keeps a {@code null} StratCon campaign state)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isUseStratConSelected() {
        return useStratConCheckbox.isSelected();
    }

    private JPanel buildButtonBar() {
        boolean isGM = campaign.isGM();

        // Options on one row, actions on the row beneath, so the bar does not run off the dialog's width.
        JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        optionRow.add(mothballOnDepartureCheckbox);
        optionRow.add(travelToSystemCheckbox);
        // The StratCon opt-out is only relevant when the campaign is running StratCon at all.
        if (campaign.getCampaignOptions().isUseStratCon()) {
            optionRow.add(useStratConCheckbox);
        }

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        RoundedJButton instructions = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.contractMarket.instructions"));
        instructions.addActionListener(e -> showInstructions());
        actionRow.add(instructions);

        RoundedJButton close = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.close"));
        close.addActionListener(e -> dispose());
        actionRow.add(close);

        RoundedJButton deleteAll = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.deleteAll"));
        deleteAll.addActionListener(e -> deleteAllContracts());
        deleteAll.setEnabled(!contracts.isEmpty());
        actionRow.add(deleteAll);

        RoundedJButton generateNew = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.contractMarket.generateNew"));
        generateNew.addActionListener(e -> generateNewContracts());
        generateNew.setEnabled(isGM);
        actionRow.add(generateNew);

        RoundedJButton createNew = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.createNew"));
        createNew.addActionListener(e -> createNewContract());
        createNew.setEnabled(isGM);
        actionRow.add(createNew);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(RoundedLineBorder.createRoundedLineBorder());
        bar.add(optionRow, BorderLayout.NORTH);
        bar.add(actionRow, BorderLayout.SOUTH);
        return bar;
    }

    /** Opens a modal, scrollable overview of the contract market board and what its controls do. */
    private void showInstructions() {
        JEditorPane pane = new JEditorPane("text/html",
              getTextAt(RESOURCE_BUNDLE, "instructions.contractMarket.board.body"));
        pane.setEditable(false);
        pane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(scaleForGUI(580), scaleForGUI(520)));
        JOptionPane.showMessageDialog(this, scroll,
              getTextAt(RESOURCE_BUNDLE, "instructions.contractMarket.board.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * A scroll-pane view that is forced to the viewport's width but keeps its own (taller) height. This lets the
     * dossier reflow narrower when the dialog shrinks while still scrolling vertically when its content is tall.
     */
    static class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return scaleForGUI(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChaosContractMarketDialog.class);
            dialog.setName("ChaosContractMarketDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
