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
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.hex;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.assessmentDifficulty;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.compactMoney;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.marketName;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ContractMarket;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractMarketAvailability;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.utilities.ContractAcceptance;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudStyle;

/**
 * The Chaos contract market, drawn as a heads-up display in the style of the Oracle Console. A one-row command bar
 * holds the market's identity, the page selector and the player's reputation and funds; the pages are
 * {@link MarketOffersPage Offers} (a sortable board of offers beside the selected offer's dossier) and
 * {@link MarketComparePage Compare} (the offers side by side, with the strongest value in each row marked). The footer
 * holds the on-departure options and the Accept button, which act on the selected offer from either page. A guide
 * slides over the window, opened by its button or by itself the first time the market is opened.
 *
 * <p>The dialog keeps the market's state - the search type, the selected offer, the board's sort and which offers
 * are compared - and the pages read it through package-private accessors, rebuilding from it on {@link #refresh}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosContractMarketDialog extends JDialog implements ContractMarketActions {
    private static final MMLogger LOGGER = MMLogger.create(ChaosContractMarketDialog.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final Dimension MINIMUM_SIZE = scaleForGUI(1040, 600);
    private static final Dimension DEFAULT_SIZE = scaleForGUI(1180, 700);
    private static final int GENERATED_OFFERS_PER_BATCH = 1;
    private static final int SHORTCUT_OFFERS = 9;

    /** The market's pages, in the order the page selector shows them. */
    enum MarketPage {
        OFFERS, COMPARE;

        String label() {
            return getTextAt(RESOURCE_BUNDLE, "page.contractMarket." + name());
        }

        /** @return the label of the page's guide button, such as "How the board works" */
        String guideLabel() {
            return getTextAt(RESOURCE_BUNDLE, "page.contractMarket." + name() + ".guide");
        }

        /** @return the resource-key prefix the page's guide is read from */
        String guidePrefix() {
            return "guide.contractMarket." + name().toLowerCase(Locale.ROOT) + ".";
        }
    }

    /** The orders the board can be sorted in. */
    enum MarketSort {
        BOARD, NET_PER_MONTH, PAY, DANGER, NEAREST;

        String label() {
            return getTextAt(RESOURCE_BUNDLE, "sort.contractMarket." + name());
        }

        String toolTip() {
            return getTextAt(RESOURCE_BUNDLE, "sort.contractMarket." + name() + ".tooltip");
        }
    }

    private final transient Campaign campaign;
    private final JFrame parent;
    private final LocalDate currentDate;
    private final transient ContractMarket contractMarket;
    private final transient List<AbstractContract> contracts = new ArrayList<>();
    private final transient Map<UUID, ContractProfitEstimate> estimates = new HashMap<>();
    private final Set<UUID> excludedFromComparison = new HashSet<>();
    private final boolean hideContractType;
    private transient ContractSearchType searchType;
    private transient AbstractContract selected;
    private MarketSort sort = MarketSort.BOARD;
    private MarketPage page = MarketPage.OFFERS;
    private transient AbstractContract acceptedContract;

    /**
     * On-departure automation options the player toggles before accepting an offer. They persist across refreshes; the
     * mothball and travel options remember the player's last choice in preferences (see {@link #setPreferences()}).
     */
    private final HudCheckBox mothballOnDepartureCheckbox = buildOptionCheckbox("checkbox.contractMarket.mothball",
          "checkbox.contractMarket.mothball.tooltip");
    private final HudCheckBox travelToSystemCheckbox = buildOptionCheckbox("checkbox.contractMarket.travel",
          "checkbox.contractMarket.travel.tooltip");
    /** Only shown (and only meaningful) when the campaign uses StratCon; unticked leaves the contract without one. */
    private final HudCheckBox useStratConCheckbox = buildOptionCheckbox("checkbox.contractMarket.stratcon",
          "checkbox.contractMarket.stratcon.tooltip");

    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageLayout);
    private HudModeSelector<MarketPage> pageSelector;
    private MarketOffersPage offersPage;
    private MarketComparePage comparePage;
    private JLabel selectedLabel;
    private HudButton acceptButton;
    private MarketGuide guide;
    private HudButton guideButton;

    /**
     * Constructs and shows the contract market backed by the player force's {@link ContractMarket}.
     *
     * <p>The board shows the offers stored under the campaign's default {@link ContractSearchType}; switching type on
     * the board swaps to that type's offers (see {@link #loadOffersForSearchType()}).</p>
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
        this.hideContractType = campaign.getCampaignOptions().get(CampaignOption.HIDE_CONTRACT_TYPE)
                                      && campaign.getCampaignOptions().get(CampaignOption.USE_OPERATION_CODENAMES);
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

    // region Layout

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "title.contractMarket"));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        offersPage = new MarketOffersPage(this);
        comparePage = new MarketComparePage(this);
        pageHolder.setOpaque(true);
        pageHolder.setBackground(GROUND);
        pageHolder.add(offersPage.getComponent(), MarketPage.OFFERS.name());
        pageHolder.add(comparePage.getComponent(), MarketPage.COMPARE.name());

        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(pageHolder, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        guide = new MarketGuide(this);
        installKeyBindings(content);
        refresh(true);

        setMinimumSize(MINIMUM_SIZE);
        pack();
        setSize(DEFAULT_SIZE); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(parent);
        setPreferences(); // Must be before setVisible

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                maybeOpenGuide();
            }
        });
        setVisible(true); // Should always be last
    }

    private JPanel buildCommandBar() {
        JPanel bar = new JPanel(new BorderLayout(scaleForGUI(20), 0));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        // getCurrentSystem() is null when the player force has no known location (e.g. in transit), so fall back to a
        // placeholder rather than dereferencing it.
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        String locationName = currentSystem != null
                                    ? currentSystem.getName(currentDate)
                                    : getTextAt(RESOURCE_BUNDLE, "header.contractMarket.location.unknown");
        JPanel identity = MarketHud.column();
        identity.add(HudStyle.leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE,
              "header.contractMarket.eyebrow",
              locationName,
              MekHQ.getMHQOptions().getDisplayFormattedDate(currentDate)))));
        identity.add(Box.createVerticalStrut(scaleForGUI(2)));
        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "title.contractMarket").toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.2f, 0.16f));
        identity.add(HudStyle.leftAligned(title));

        pageSelector = new HudModeSelector<>(MarketPage.class, MarketPage::label, this::showPage);
        pageSelector.setSelected(MarketPage.OFFERS);
        JPanel selectorHolder = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        selectorHolder.add(pageSelector);

        boolean useChaosReputation = campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_REPUTATION);
        int reputation = campaign.getPlayerForce().getReputationRating(useChaosReputation);
        JPanel reputationReadout = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "readout.contractMarket.reputation"),
              Integer.toString(reputation), AMBER, SURFACE_DEEP);
        reputationReadout.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
              "readout.contractMarket.reputation.tooltip")));
        Money balance = campaign.getPlayerForce().getFinances().getBalance();
        JPanel fundsReadout = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "readout.contractMarket.funds"),
              compactMoney(balance), TEXT, SURFACE_DEEP);
        fundsReadout.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "readout.contractMarket.funds.tooltip",
              balance.toAmountAndSymbolString()));

        guideButton = new HudButton("", false, true);
        guideButton.addActionListener(event -> openGuide());
        refreshGuideButton();

        JPanel readouts = Hud.transparentPanel(null);
        readouts.setLayout(new BoxLayout(readouts, BoxLayout.X_AXIS));
        readouts.add(reputationReadout);
        readouts.add(Box.createHorizontalStrut(scaleForGUI(6)));
        readouts.add(fundsReadout);
        readouts.add(Box.createHorizontalStrut(scaleForGUI(10)));
        readouts.add(guideButton);

        bar.add(identity, BorderLayout.WEST);
        bar.add(selectorHolder, BorderLayout.CENTER);
        bar.add(readouts, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        JPanel options = Hud.transparentPanel(null);
        options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
        options.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "footer.contractMarket.options")
                                            .toUpperCase(Locale.ROOT)));
        options.add(Box.createHorizontalStrut(scaleForGUI(12)));
        options.add(mothballOnDepartureCheckbox);
        options.add(Box.createHorizontalStrut(scaleForGUI(14)));
        options.add(travelToSystemCheckbox);
        // The StratCon opt-out is only relevant when the campaign is running StratCon at all.
        if (campaign.getCampaignOptions().isUseStratCon()) {
            options.add(Box.createHorizontalStrut(scaleForGUI(14)));
            options.add(useStratConCheckbox);
        }

        selectedLabel = Hud.hint("");
        acceptButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.accept")
                                           .toUpperCase(Locale.ROOT), true);
        acceptButton.addActionListener(event -> {
            if (selected != null) {
                accept(selected);
            }
        });
        HudButton close = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.close")
                                              .toUpperCase(Locale.ROOT), false);
        close.addActionListener(event -> dispose());

        JPanel actions = Hud.transparentPanel(null);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(selectedLabel);
        actions.add(Box.createHorizontalStrut(scaleForGUI(10)));
        actions.add(acceptButton);
        actions.add(Box.createHorizontalStrut(scaleForGUI(8)));
        actions.add(close);

        footer.add(options, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void installKeyBindings(JComponent root) {
        // Number keys pick an offer in the board's current order; C switches between the board and the comparison.
        for (int number = 1; number <= SHORTCUT_OFFERS; number++) {
            final int index = number - 1;
            String name = "offer" + number;
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((char) ('0' + number)),
                  name);
            root.getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    List<AbstractContract> ordered = sortedContracts();
                    if (!guide.isOpen() && index < ordered.size()) {
                        select(ordered.get(index));
                    }
                }
            });
        }
        AbstractAction togglePage = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!guide.isOpen()) {
                    showPage(page == MarketPage.OFFERS ? MarketPage.COMPARE : MarketPage.OFFERS);
                }
            }
        };
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('c'), "togglePage");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('C'), "togglePage");
        root.getActionMap().put("togglePage", togglePage);

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "closeGuide");
        root.getActionMap().put("closeGuide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                closeGuide();
            }
        });
    }

    // endregion Layout

    // region State and refresh

    /**
     * Brings every part of the window up to date with the market's state. Only the page on screen is rebuilt; the
     * other is rebuilt when it is next shown.
     *
     * @param rebuildDossier {@code true} to rebuild the selected offer's dossier even if the selection is unchanged, as
     *                       after its terms were negotiated or edited
     */
    void refresh(boolean rebuildDossier) {
        List<AbstractContract> ordered = sortedContracts();
        if ((selected == null || !contracts.contains(selected)) && !ordered.isEmpty()) {
            selected = ordered.getFirst();
        } else if (ordered.isEmpty()) {
            selected = null;
        }
        excludedFromComparison.removeIf(id -> contracts.stream().noneMatch(contract -> contract.getId().equals(id)));

        pageSelector.setModeLabel(MarketPage.OFFERS, pageLabel(MarketPage.OFFERS, contracts.size()));
        pageSelector.setModeLabel(MarketPage.COMPARE, pageLabel(MarketPage.COMPARE, comparedContracts().size()));

        selectedLabel.setText(selected == null
                                    ? getTextAt(RESOURCE_BUNDLE, "footer.contractMarket.selected.none")
                                    : getFormattedTextAt(RESOURCE_BUNDLE, "footer.contractMarket.selected",
                                          marketName(selected)));
        acceptButton.setArmed(selected != null);

        if (page == MarketPage.OFFERS) {
            offersPage.refresh(rebuildDossier);
        } else {
            comparePage.refresh();
        }
    }

    private static String pageLabel(MarketPage marketPage, int count) {
        return "<html>" + MarketHud.escape(marketPage.label().toUpperCase(Locale.ROOT))
                     + "&nbsp;&nbsp;<span style='color:" + hex(ACCENT_BRIGHT) + "'>" + count + "</span></html>";
    }

    void showPage(MarketPage newPage) {
        closeGuide();
        page = newPage;
        pageSelector.setSelected(newPage);
        pageLayout.show(pageHolder, newPage.name());
        refreshGuideButton();
        refresh(newPage == MarketPage.OFFERS);
        maybeOpenGuide();
    }

    Campaign campaign() {
        return campaign;
    }

    LocalDate currentDate() {
        return currentDate;
    }

    boolean isContractTypeHidden() {
        return hideContractType;
    }

    boolean isGM() {
        return campaign.isGM();
    }

    boolean isMarketDisabled() {
        return campaign.getCampaignOptions().get(CampaignOption.CONTRACT_MARKET_METHOD).isNone();
    }

    @Nullable AbstractContract selected() {
        return selected;
    }

    /**
     * Selects an offer: its dossier is shown on the board, its column is marked on the comparison, and the footer's
     * Accept button acts on it.
     */
    void select(AbstractContract contract) {
        if (contract == selected) {
            return;
        }
        selected = contract;
        refresh(false);
    }

    ContractSearchType searchType() {
        return searchType;
    }

    List<ContractSearchType> allowedSearchTypes() {
        return allowedSearchTypes(campaign);
    }

    /**
     * Switches the search type and shows that type's offers from the {@link ContractMarket}. Types whose generation is
     * not yet implemented (pirate, government, tournament) have empty maps, so the board falls to its empty state until
     * their generation lands.
     *
     * @author Illiani
     * @since 0.51.01
     */
    void changeSearchType(ContractSearchType newType) {
        if (newType == searchType) {
            return;
        }
        searchType = newType;
        selected = null;
        loadOffersForSearchType();
        refresh(true);
    }

    MarketSort sort() {
        return sort;
    }

    void setSort(MarketSort newSort) {
        sort = newSort;
        refresh(false);
    }

    /**
     * @return the offers in the board's current order; ties keep the market's own order
     */
    List<AbstractContract> sortedContracts() {
        List<AbstractContract> ordered = new ArrayList<>(contracts);
        Comparator<AbstractContract> comparator = switch (sort) {
            case BOARD -> null;
            case NET_PER_MONTH -> Comparator.comparing((AbstractContract contract) ->
                                                             estimate(contract).getNetPerMonth()).reversed();
            case PAY -> Comparator.comparing(AbstractContract::getTotalPay).reversed();
            case DANGER -> Comparator.comparingInt(ContractMarketFormatting::assessmentDifficulty);
            case NEAREST -> Comparator.comparingInt(contract -> estimate(contract).transitDays());
        };
        if (comparator != null) {
            ordered.sort(comparator);
        }
        return ordered;
    }

    /** @return the offers turned on for comparison, in the board's current order */
    List<AbstractContract> comparedContracts() {
        return sortedContracts().stream().filter(this::isCompared).toList();
    }

    boolean isCompared(AbstractContract contract) {
        return !excludedFromComparison.contains(contract.getId());
    }

    void setCompared(AbstractContract contract, boolean compared) {
        if (compared) {
            excludedFromComparison.remove(contract.getId());
        } else {
            excludedFromComparison.add(contract.getId());
        }
        refresh(false);
    }

    /**
     * Returns the offer's profit estimate, computed once and kept until the offer's terms change. Estimating plots the
     * jump path to the target, so it is too slow to repeat every time a row or cell is drawn.
     */
    ContractProfitEstimate estimate(AbstractContract contract) {
        return estimates.computeIfAbsent(contract.getId(),
              id -> ContractProfitEstimate.estimate(campaign, contract));
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

    // endregion State and refresh

    // region Actions

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
     * facilities are updated in place, so its estimate and dossier are rebuilt to reflect them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void negotiate(AbstractContract contract) {
        ContractNegotiationDialog negotiation = new ContractNegotiationDialog(campaign, contract);
        if (negotiation.wasConfirmed()) {
            estimates.remove(contract.getId());
            refresh(true);
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
        estimates.remove(contract.getId());
        if (contract == selected) {
            selected = null;
        }
        refresh(true);
    }

    /**
     * Opens the GM editor for the offer. If the GM confirms changes, the contract is updated in place, so its estimate
     * and dossier are rebuilt to reflect them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void edit(AbstractContract contract) {
        ContractEditorDialog editor = new ContractEditorDialog(campaign, contract);
        if (editor.wasConfirmed()) {
            estimates.remove(contract.getId());
            refresh(true);
        }
    }

    /**
     * Clears every offer from the board and refreshes the dialog to the empty state.
     *
     * @author Illiani
     * @since 0.51.01
     */
    void deleteAllContracts() {
        contractMarket.getContracts(searchType).clear();
        contracts.clear();
        estimates.clear();
        selected = null;
        refresh(true);
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
    void generateNewContracts() {
        List<AbstractContract> generated = ChaosContractMarketAvailability.generateOffers(campaign,
              GENERATED_OFFERS_PER_BATCH, true, searchType);
        if (generated.isEmpty()) {
            LOGGER.info("Generate-new produced no valid contracts.");
            return;
        }

        Map<UUID, AbstractContract> marketOffers = contractMarket.getContracts(searchType);
        for (AbstractContract contract : generated) {
            marketOffers.put(contract.getId(), contract);
        }
        loadOffersForSearchType();
        selected = generated.getFirst();
        refresh(true);
    }

    /**
     * Opens the GM editor on a fresh, fully-defaulted contract. If the GM confirms, the new offer is added to the
     * search type's market the GM chose and shown on the board.
     *
     * @author Illiani
     * @since 0.51.01
     */
    void createNewContract() {
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
        selected = contract;
        refresh(true);
    }

    // endregion Actions

    // region Search types

    /**
     * The search type a campaign defaults to: mercenary or pirate bands default to their own type, and every other
     * (government) campaign can only look for government contracts.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static ContractSearchType defaultSearchType(Campaign campaign) {
        Faction faction = campaign.getPlayerForce().getFaction();
        if (faction.getShortName().equals(PIRATE_FACTION_CODE)) {
            return ContractSearchType.PIRATE;
        }
        if (faction.isMercenary()) {
            return ContractSearchType.MERCENARY;
        }
        return ContractSearchType.GOVERNMENT;
    }

    /**
     * The search types a campaign may choose between. Acts of piracy ({@link ContractSearchType#PIRATE}) are reserved
     * for pirate bands. Mercenary commands may look for mercenary work or the tournament circuit; every other
     * (government) campaign is limited to government orders.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static List<ContractSearchType> allowedSearchTypes(Campaign campaign) {
        Faction faction = campaign.getPlayerForce().getFaction();
        if (faction.getShortName().equals(PIRATE_FACTION_CODE)) {
            return List.of(ContractSearchType.PIRATE);
        }
        if (faction.isMercenary()) {
            return List.of(ContractSearchType.MERCENARY, ContractSearchType.TOURNAMENT);
        }
        return List.of(ContractSearchType.GOVERNMENT);
    }

    static String searchTypeLabel(ContractSearchType type) {
        return getTextAt(RESOURCE_BUNDLE, "searchType.contractMarket." + type.name());
    }

    // endregion Search types

    // region Guide

    /** Opens the guide for the page on screen, marking it seen so it does not open by itself again. */
    private void openGuide() {
        ContractMarketGuidePreferences.getInstance().setGuideSeen(page.guidePrefix(), true);
        guide.open(page.guidePrefix());
    }

    /** Opens the page's guide by itself the first time the player sees the page. */
    private void maybeOpenGuide() {
        if (isShowing() && !ContractMarketGuidePreferences.getInstance().isGuideSeen(page.guidePrefix())) {
            openGuide();
        }
    }

    private void refreshGuideButton() {
        guideButton.setText(getFormattedTextAt(RESOURCE_BUNDLE, "button.contractMarket.guide", page.guideLabel())
                                  .toUpperCase(Locale.ROOT));
    }

    private void closeGuide() {
        if (guide != null) {
            guide.close();
        }
    }

    // endregion Guide

    // region Departure options

    /**
     * Builds a persistent automation checkbox from its label and tooltip resource keys. It starts ticked; saved
     * preferences then restore the player's last choice.
     *
     * @param labelKey   resource key of the checkbox label
     * @param tooltipKey resource key of the checkbox tooltip
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static HudCheckBox buildOptionCheckbox(String labelKey, String tooltipKey) {
        HudCheckBox checkbox = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, labelKey));
        checkbox.setSelected(true);
        checkbox.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, tooltipKey)));
        checkbox.setAlignmentY(Component.CENTER_ALIGNMENT);
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

    // endregion Departure options

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek. Besides the
     * window bounds, it remembers the mothball and travel on-departure checkboxes between opens.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChaosContractMarketDialog.class);
            setName("ChaosContractMarketDialog");
            preferences.manage(new JWindowPreference(this));

            mothballOnDepartureCheckbox.setName("mothballOnDepartureCheckbox");
            preferences.manage(new JToggleButtonPreference(mothballOnDepartureCheckbox));
            travelToSystemCheckbox.setName("travelToSystemCheckbox");
            preferences.manage(new JToggleButtonPreference(travelToSystemCheckbox));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    /** @return the difficulty of an offer, for tooltips that explain its ring colour */
    static String difficultyToolTip(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "board.contractMarket.difficulty.tooltip",
              ContractMarketFormatting.assessmentLabel(contract), assessmentDifficulty(contract));
    }
}
