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

import static java.lang.Math.max;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MAXIMUM_STEP_VALUE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MINIMUM_STEP_VALUE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import mekhq.campaign.mission.contract.contractData.NegotiationData;
import mekhq.campaign.mission.contract.contractData.RentedFacilitiesData;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractPayDetermination;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.TermFunding;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * The contract negotiation table. Lets the player improve a contract's terms - spending reputation or sacrificing steps
 * between terms - and rent support facilities, before accepting the offer.
 *
 * <p>Rules (Hot Spots: Draconis Reach): each of the five terms may be raised at most {@code Scale} steps; reputation
 * raises one term one step per point, up to {@code 2 x Scale} points total; and a term may be lowered two steps to
 * raise another one step, at most twice. Steps that leave a term's value unchanged (the em-dash plateaus on the
 * Contract Steps Table) are surfaced as "no change" so the player can see they must spend more to cross a band.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractNegotiationDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ContractNegotiationDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int SACRIFICE_STEPS_PER_SWAP = 2;
    private static final int MAXIMUM_SWAPS = 2;
    private static final int MAXIMUM_FACILITY_UNITS = 100;

    private static final String REPUTATION_HEX = "#1d5fa5";
    private static final String SACRIFICE_HEX = "#993c1d";
    private static final String MUTED_HEX = "#888888";

    private final int PADDING = scaleForGUI(6);

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final transient AbstractLocation currentLocation;

    private final int scale;
    private final int capPerTerm;
    private final int reputationPool;

    private int reputationUsed;
    private int swapsUsed;
    private int sacrificeBank;

    /** Clause order matching {@link NegotiationData} and {@link ContractTermsData} for persistence. */
    private static final Clause[] CANONICAL_ORDER =
          { Clause.PAY, Clause.SUPPORT, Clause.TRANSPORT, Clause.SALVAGE, Clause.COMMAND };

    private final int[] originalStep = new int[Clause.values().length];
    private final int[] currentStep = new int[Clause.values().length];
    private final transient List<List<TermFunding>> funding = new ArrayList<>();

    private final JLabel[] termValueLabels = new JLabel[Clause.values().length];
    private final JLabel[] termCapLabels = new JLabel[Clause.values().length];
    private final JButton[] termRaiseButtons = new JButton[Clause.values().length];
    private final JButton[] termLowerButtons = new JButton[Clause.values().length];

    private final int[] facilityUnitCost = new int[3];
    private final int[] facilityQuantity = new int[3];
    private final JLabel[] facilityQuantityLabels = new JLabel[3];
    private final JLabel[] facilityTotalLabels = new JLabel[3];

    private JButton negotiatorButton;
    private JLabel negotiatorLabel;
    private JLabel reputationBudgetLabel;
    private JLabel swapsBudgetLabel;
    private JLabel bankBudgetLabel;
    private JLabel payImpactLabel;
    private JLabel summaryLabel;

    private boolean confirmed;

    /** The five negotiable clauses, in display order. */
    private enum Clause {
        COMMAND("negotiate.contractMarket.term.command"),
        PAY("negotiate.contractMarket.term.pay"),
        SUPPORT("negotiate.contractMarket.term.support"),
        TRANSPORT("negotiate.contractMarket.term.transport"),
        SALVAGE("negotiate.contractMarket.term.salvage");

        private final String labelKey;

        Clause(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    /**
     * Opens the modal negotiation dialog for the given offer. On confirmation the contract's terms, pay, and rented
     * facilities are updated in place; query {@link #wasConfirmed()} afterward.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractNegotiationDialog(Campaign campaign, AbstractContract contract) {
        super(campaign.getGUI().getFrame(), true);
        this.campaign = campaign;
        this.contract = contract;

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        this.currentLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();

        this.scale = max(1, contract.getScale());
        this.capPerTerm = scale;
        boolean useChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);
        int reputation = campaign.getPlayerForce().getReputationRating(useChaosReputation);
        this.reputationPool = Math.clamp(reputation, 0, 2 * scale);

        for (Clause clause : Clause.values()) {
            currentStep[clause.ordinal()] = initialStep(clause);
            funding.add(new ArrayList<>());
        }
        restoreNegotiationState();

        facilityUnitCost[0] = campaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS);
        facilityUnitCost[1] = campaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_KITCHENS);
        facilityUnitCost[2] = campaignOptions.get(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS);
        facilityQuantity[0] = contract.getRentedHospitalBeds();
        facilityQuantity[1] = contract.getRentedKitchens();
        facilityQuantity[2] = contract.getRentedHoldingCells();

        initializeComponents();
    }

    /** @return whether the player confirmed the negotiated terms. */
    public boolean wasConfirmed() {
        return confirmed;
    }

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "title.contractMarket.negotiate"));

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildBody(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        getContentPane().add(content);

        refresh();

        pack();
        setSize(scaleForGUI(540, 640)); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(getParent());
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Tracks this dialog's window size and position in MekHQ's preferences so it reopens where the player left it.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ContractNegotiationDialog.class);
            setName("ContractNegotiationDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        header.add(buildNegotiatorControl(), BorderLayout.WEST);

        JLabel heading = new JLabel("<html><b style='font-size:larger'>"
                                          +
                                          getTextAt(RESOURCE_BUNDLE, "title.contractMarket.negotiate") +
                                          "</b></html>");
        heading.setVerticalAlignment(SwingConstants.TOP);

        JLabel subtitle = new JLabel("<html><span style='color:" +
                                           MUTED_HEX +
                                           "'>"
                                           +
                                           escape(getFormattedTextAt(RESOURCE_BUNDLE,
                                                 "negotiate.contractMarket.subtitle",
                                                 contract.getName(),
                                                 contract.getEmployerMarketDisplayName(),
                                                 scale)) +
                                           "</span></html>");

        JPanel budgets = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(16), 0));
        reputationBudgetLabel = new JLabel();
        swapsBudgetLabel = new JLabel();
        bankBudgetLabel = new JLabel();
        budgets.add(reputationBudgetLabel);
        budgets.add(swapsBudgetLabel);
        budgets.add(bankBudgetLabel);

        // Top line: the heading on the left, the budgets on the right. The contract name sits below both.
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(heading, BorderLayout.WEST);
        topRow.add(budgets, BorderLayout.EAST);

        JPanel titleArea = new JPanel(new BorderLayout());
        titleArea.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(10), 0, 0));
        titleArea.add(topRow, BorderLayout.NORTH);
        titleArea.add(subtitle, BorderLayout.CENTER);
        header.add(titleArea, BorderLayout.CENTER);

        return header;
    }

    /**
     * Builds the negotiator control: a portrait button, showing the chosen negotiator or a warning default when none
     * has been picked, above a label naming them. Clicking the button opens the negotiator picker.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildNegotiatorControl() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        negotiatorButton = new JButton();
        negotiatorButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        negotiatorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        negotiatorButton.addActionListener(e -> openNegotiatorPicker());
        panel.add(negotiatorButton);

        negotiatorLabel = new JLabel();
        negotiatorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        negotiatorLabel.setFont(negotiatorLabel.getFont().deriveFont(negotiatorLabel.getFont().getSize2D() - 2f));
        panel.add(negotiatorLabel);

        updateNegotiatorControl();
        return panel;
    }

    private void updateNegotiatorControl() {
        int size = scaleForGUI(56);
        Person negotiator = contract.getPlayerNegotiator();

        if (negotiator != null) {
            ImageIcon icon = negotiator.getPortraitImageIconWithFallback(true);
            if (icon != null) {
                negotiatorButton.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size,
                      Image.SCALE_SMOOTH)));
            }
            negotiatorButton.setBorder(BorderFactory.createLineBorder(muted(), scaleForGUI(1)));
            negotiatorButton.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.negotiator.tooltip")));
            negotiatorLabel.setText(escape(negotiator.getFullTitle()));
            negotiatorLabel.setForeground(null);
        } else {
            ImageIcon icon = new Portrait().getImageIcon(size);
            if (icon != null) {
                negotiatorButton.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size,
                      Image.SCALE_SMOOTH)));
            }
            negotiatorButton.setBorder(BorderFactory.createLineBorder(warning(), scaleForGUI(2)));
            negotiatorButton.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.negotiator.tooltip")));
            negotiatorLabel.setText(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.negotiator.none"));
            negotiatorLabel.setForeground(warning());
        }
    }

    private void openNegotiatorPicker() {
        ContractNegotiatorPickerDialog picker = new ContractNegotiatorPickerDialog(campaign,
              contract.getPlayerNegotiator());
        if (picker.wasConfirmed()) {
            contract.setPlayerNegotiator(picker.getSelectedNegotiator());
            updateNegotiatorControl();
        }
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        body.add(buildTermsCard());
        body.add(javax.swing.Box.createVerticalStrut(PADDING));
        body.add(buildFacilitiesCard());

        return body;
    }

    private JPanel buildTermsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));

        JPanel head = new JPanel(new BorderLayout());
        JLabel section = new JLabel(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.section.terms").toUpperCase());
        section.setFont(section.getFont().deriveFont(section.getFont().getSize2D() - 2f));
        section.setForeground(muted());
        head.add(section, BorderLayout.WEST);
        JLabel hint = new JLabel(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.section.terms.hint"));
        hint.setForeground(muted());
        hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
        head.add(hint, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));
        for (Clause clause : Clause.values()) {
            rows.add(buildTermRow(clause));
        }

        payImpactLabel = new JLabel();
        payImpactLabel.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));

        JPanel south = new JPanel(new BorderLayout());
        south.add(payImpactLabel, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        center.add(rows, BorderLayout.NORTH);
        center.add(south, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildTermRow(Clause clause) {
        int index = clause.ordinal();

        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), 0, scaleForGUI(4), 0));

        JLabel name = new JLabel(getTextAt(RESOURCE_BUNDLE, clause.labelKey));
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(6), 0));
        namePanel.setPreferredSize(new Dimension(scaleForGUI(210), name.getPreferredSize().height));
        namePanel.add(name);
        // Mark a term the employer's faction disposition shifts (e.g. "Stingy" next to Base Pay), when the campaign
        // uses faction modifiers.
        String factionMarker = factionModifierMarker(clause);
        if (factionMarker != null) {
            JLabel marker = new JLabel(factionMarker);
            marker.setForeground(muted());
            marker.setFont(marker.getFont().deriveFont(Font.ITALIC, marker.getFont().getSize2D() - 1f));
            namePanel.add(marker);
        }
        row.add(namePanel, BorderLayout.WEST);

        termValueLabels[index] = new JLabel();
        row.add(termValueLabels[index], BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(6), 0));
        termCapLabels[index] = new JLabel();
        termCapLabels[index].setForeground(muted());
        termCapLabels[index].setFont(termCapLabels[index].getFont().deriveFont(
              termCapLabels[index].getFont().getSize2D() - 1f));
        controls.add(termCapLabels[index]);

        termLowerButtons[index] = stepperButton(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.stepper.decrease"), () -> lower(clause));
        controls.add(termLowerButtons[index]);
        termRaiseButtons[index] = stepperButton(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.stepper.increase"), () -> raise(clause));
        controls.add(termRaiseButtons[index]);

        row.add(controls, BorderLayout.EAST);
        return row;
    }

    /**
     * The faction-disposition marker to show beside a term, or {@code null} when none applies. Only returned when the
     * campaign uses faction modifiers and the employer's faction carries the relevant tag: Base Pay is marked
     * Generous/Stingy, Command Rights is marked Lenient/Controlling.
     */
    private String factionModifierMarker(Clause clause) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_CONTRACT_FACTION_MODIFIERS)) {
            return null;
        }
        Faction employerFaction = contract.getEmployerFaction();
        if (employerFaction == null) {
            return null;
        }
        return switch (clause) {
            case PAY -> {
                if (employerFaction.isGenerous()) {
                    yield getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.factionModifier.generous");
                }
                if (employerFaction.isStingy()) {
                    yield getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.factionModifier.stingy");
                }
                yield null;
            }
            case COMMAND -> {
                if (employerFaction.isLenient()) {
                    yield getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.factionModifier.lenient");
                }
                if (employerFaction.isControlling()) {
                    yield getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.factionModifier.controlling");
                }
                yield null;
            }
            default -> null;
        };
    }

    private JPanel buildFacilitiesCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(RoundedLineBorder.createSubtleRoundedLineBorder(),
              BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));

        JLabel section = new JLabel(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.section.facilities").toUpperCase());
        section.setFont(section.getFont().deriveFont(section.getFont().getSize2D() - 2f));
        section.setForeground(muted());
        card.add(section, BorderLayout.NORTH);

        String[] facilityKeys = { "negotiate.contractMarket.facility.hospitalBeds",
                                  "negotiate.contractMarket.facility.kitchens",
                                  "negotiate.contractMarket.facility.holdingCells" };

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));
        for (int i = 0; i < facilityKeys.length; i++) {
            rows.add(buildFacilityRow(i, facilityKeys[i]));
        }
        card.add(rows, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFacilityRow(int index, String labelKey) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), 0, scaleForGUI(4), 0));

        String costEach = getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.facility.each",
              Money.of(facilityUnitCost[index]).toAmountAndSymbolString());
        String benefit = getTextAt(RESOURCE_BUNDLE, labelKey + ".benefit");
        JLabel name = new JLabel("<html>" +
                                       escape(getTextAt(RESOURCE_BUNDLE, labelKey)) +
                                       "<br><span style='color:"
                                       +
                                       MUTED_HEX +
                                       "; font-size:smaller'>" +
                                       escape(costEach) +
                                       " &middot; " +
                                       escape(benefit)
                                       +
                                       "</span></html>");
        row.add(name, BorderLayout.WEST);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(10), 0));

        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(6), 0));
        stepper.add(stepperButton(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.stepper.decrease"),
              () -> adjustFacility(index, -1)));
        // Seed with real text so the label has a non-zero preferred height before we pin its width.
        facilityQuantityLabels[index] = new JLabel(Integer.toString(facilityQuantity[index]), SwingConstants.CENTER);
        facilityQuantityLabels[index].setPreferredSize(new Dimension(scaleForGUI(28),
              facilityQuantityLabels[index].getPreferredSize().height));
        stepper.add(facilityQuantityLabels[index]);
        stepper.add(stepperButton(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.stepper.increase"),
              () -> adjustFacility(index, 1)));
        east.add(stepper);

        Money initialTotal = Money.of((double) facilityQuantity[index] * facilityUnitCost[index]);
        facilityTotalLabels[index] = new JLabel(initialTotal.toAmountAndSymbolString(), SwingConstants.RIGHT);
        facilityTotalLabels[index].setPreferredSize(new Dimension(scaleForGUI(120),
              facilityTotalLabels[index].getPreferredSize().height));
        east.add(facilityTotalLabels[index]);

        row.add(east, BorderLayout.EAST);
        return row;
    }

    private JPanel buildFooter() {
        // Summary on its own line (left), buttons on the line below (right).
        JPanel footer = new JPanel(new BorderLayout(0, PADDING));
        footer.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));

        summaryLabel = new JLabel();
        summaryLabel.setForeground(muted());
        footer.add(summaryLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, PADDING, 0));
        RoundedJButton explain = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.contractMarket.negotiate.explain"));
        explain.addActionListener(e -> explainAction());
        buttons.add(explain);
        RoundedJButton reset = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.reset"));
        reset.addActionListener(e -> resetAll());
        buttons.add(reset);
        RoundedJButton cancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.contractMarket.negotiate.cancel"));
        cancel.addActionListener(e -> dispose());
        buttons.add(cancel);
        RoundedJButton confirm = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.contractMarket.negotiate.confirm"));
        confirm.addActionListener(e -> confirmAction());
        buttons.add(confirm);
        footer.add(buttons, BorderLayout.SOUTH);

        return footer;
    }

    private JButton stepperButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setPreferredSize(scaleForGUI(30, 28));
        button.addActionListener(e -> action.run());
        return button;
    }

    // region Negotiation logic

    /**
     * Restores a previous negotiation from the contract, if one was stored, so reopening the dialog resumes where it
     * left off. Otherwise the original baseline is the contract's current terms and no budget has been spent.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void restoreNegotiationState() {
        NegotiationData data = contract.getNegotiationData();
        if (data == null) {
            for (Clause clause : Clause.values()) {
                originalStep[clause.ordinal()] = currentStep[clause.ordinal()];
            }
            return;
        }

        originalStep[Clause.PAY.ordinal()] = data.originalPayStep();
        originalStep[Clause.SUPPORT.ordinal()] = data.originalSupportStep();
        originalStep[Clause.TRANSPORT.ordinal()] = data.originalTransportStep();
        originalStep[Clause.SALVAGE.ordinal()] = data.originalSalvageStep();
        originalStep[Clause.COMMAND.ordinal()] = data.originalCommandStep();

        reputationUsed = data.reputationUsed();
        swapsUsed = data.swapsUsed();
        sacrificeBank = data.sacrificeBank();

        for (int i = 0; i < CANONICAL_ORDER.length; i++) {
            funding.get(CANONICAL_ORDER[i].ordinal()).addAll(data.funding().get(i));
        }
    }

    private void raise(Clause clause) {
        int index = clause.ordinal();
        if ((currentStep[index] - originalStep[index]) >= capPerTerm
                  || currentStep[index] >= CHAOS_CONTRACT_MAXIMUM_STEP_VALUE) {
            return;
        }

        if (reputationUsed < reputationPool) {
            reputationUsed++;
            funding.get(index).add(TermFunding.REPUTATION);
        } else if (sacrificeBank >= SACRIFICE_STEPS_PER_SWAP && swapsUsed < MAXIMUM_SWAPS) {
            sacrificeBank -= SACRIFICE_STEPS_PER_SWAP;
            swapsUsed++;
            funding.get(index).add(TermFunding.SACRIFICE);
        } else {
            return;
        }

        currentStep[index]++;
        refresh();
    }

    private void lower(Clause clause) {
        int index = clause.ordinal();
        if (currentStep[index] <= CHAOS_CONTRACT_MINIMUM_STEP_VALUE) {
            return;
        }

        if (currentStep[index] > originalStep[index]) {
            // Undo a raise, refunding whatever paid for it.
            TermFunding paidWith = funding.get(index).removeLast();
            if (paidWith == TermFunding.REPUTATION) {
                reputationUsed--;
            } else {
                swapsUsed--;
                sacrificeBank += SACRIFICE_STEPS_PER_SWAP;
            }
        } else {
            // Drop below the original, banking a sacrifice step.
            sacrificeBank++;
        }

        currentStep[index]--;
        refresh();
    }

    private void adjustFacility(int index, int delta) {
        facilityQuantity[index] = Math.clamp(facilityQuantity[index] + delta, 0, MAXIMUM_FACILITY_UNITS);
        refresh();
    }

    private void explainAction() {
        String body = getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.explain.body", scaleForGUI(360));
        JOptionPane.showMessageDialog(this, new JLabel(body),
              getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.explain.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetAll() {
        for (Clause clause : Clause.values()) {
            currentStep[clause.ordinal()] = originalStep[clause.ordinal()];
            funding.get(clause.ordinal()).clear();
        }
        reputationUsed = 0;
        swapsUsed = 0;
        sacrificeBank = 0;
        facilityQuantity[0] = contract.getRentedHospitalBeds();
        facilityQuantity[1] = contract.getRentedKitchens();
        facilityQuantity[2] = contract.getRentedHoldingCells();
        refresh();
    }

    private void confirmAction() {
        ChaosContractStepsTable payRate = step(Clause.PAY);
        ChaosContractStepsTable support = step(Clause.SUPPORT);
        ChaosContractStepsTable transport = step(Clause.TRANSPORT);
        ChaosContractStepsTable salvage = step(Clause.SALVAGE);
        ChaosContractStepsTable command = step(Clause.COMMAND);

        contract.setContractTerms(new ContractTermsData(payRate, support, transport, salvage, command));
        contract.updateMonthlyPay(ChaosContractPayDetermination.getMonthlyPay(campaign, contract));
        contract.updateTransportPay(ChaosContractPayDetermination.getTransportPay(campaign,
              campaign.getLocalDate(), contract, currentLocation));

        contract.setRentedFacilitiesData(new RentedFacilitiesData(facilityQuantity[0],
              facilityQuantity[1], facilityQuantity[2]));

        List<List<TermFunding>> fundingByClause = new ArrayList<>();
        for (Clause clause : CANONICAL_ORDER) {
            fundingByClause.add(new ArrayList<>(funding.get(clause.ordinal())));
        }
        contract.setNegotiationData(new NegotiationData(originalStep[Clause.PAY.ordinal()],
              originalStep[Clause.SUPPORT.ordinal()], originalStep[Clause.TRANSPORT.ordinal()],
              originalStep[Clause.SALVAGE.ordinal()], originalStep[Clause.COMMAND.ordinal()],
              reputationUsed, swapsUsed, sacrificeBank, fundingByClause));

        LOGGER.info("Negotiated terms confirmed for contract: {}", contract.getName());
        confirmed = true;
        dispose();
    }

    // endregion Negotiation logic

    // region Rendering

    private void refresh() {
        reputationBudgetLabel.setText(budgetHtml(getTextAt(RESOURCE_BUNDLE,
                    "negotiate.contractMarket.budget.reputation"),
              pips(reputationPool - reputationUsed, reputationPool, REPUTATION_HEX)
                    + " " + (reputationPool - reputationUsed) + "/" + reputationPool));
        swapsBudgetLabel.setText(budgetHtml(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.swaps"),
              pips(swapsUsed, MAXIMUM_SWAPS, SACRIFICE_HEX) + " " + swapsUsed + "/" + MAXIMUM_SWAPS));
        bankBudgetLabel.setText(budgetHtml(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.bank"),
              getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.bank.value", sacrificeBank)));

        for (Clause clause : Clause.values()) {
            int index = clause.ordinal();
            termValueLabels[index].setText(termValueHtml(clause));
            termCapLabels[index].setText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.cap",
                  max(0, currentStep[index] - originalStep[index]), capPerTerm));
            termRaiseButtons[index].setEnabled(canRaise(clause));
            termLowerButtons[index].setEnabled(currentStep[index] > CHAOS_CONTRACT_MINIMUM_STEP_VALUE);
        }

        payImpactLabel.setText(payImpactHtml());

        Money rentalTotal = Money.zero();
        for (int i = 0; i < facilityQuantity.length; i++) {
            Money lineTotal = Money.of((double) facilityQuantity[i] * facilityUnitCost[i]);
            rentalTotal = rentalTotal.plus(lineTotal);
            facilityQuantityLabels[i].setText(Integer.toString(facilityQuantity[i]));
            facilityTotalLabels[i].setText(lineTotal.toAmountAndSymbolString());
        }

        summaryLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.summary",
              rentalTotal.toAmountAndSymbolString(), reputationPool - reputationUsed, sacrificeBank));
    }

    private boolean canRaise(Clause clause) {
        int index = clause.ordinal();
        if ((currentStep[index] - originalStep[index]) >= capPerTerm
                  || currentStep[index] >= CHAOS_CONTRACT_MAXIMUM_STEP_VALUE) {
            return false;
        }
        return reputationUsed < reputationPool
                     || (sacrificeBank >= SACRIFICE_STEPS_PER_SWAP && swapsUsed < MAXIMUM_SWAPS);
    }

    private String termValueHtml(Clause clause) {
        int index = clause.ordinal();
        String originalValue = valueOf(clause, ChaosContractStepsTable.fromStepValue(originalStep[index]));
        String proposedValue = valueOf(clause, ChaosContractStepsTable.fromStepValue(currentStep[index]));
        int delta = currentStep[index] - originalStep[index];

        StringBuilder html = new StringBuilder("<html>").append(escape(originalValue));
        if (delta != 0) {
            boolean valueChanged = !originalValue.equals(proposedValue);
            String color = delta > 0 ? hex(positiveHex()) : hex(negativeHex());
            html.append(" &rarr; <b style='color:").append(valueChanged ? color : MUTED_HEX).append("'>")
                  .append(escape(proposedValue)).append("</b>");

            for (TermFunding paidWith : funding.get(index)) {
                String chipColor = paidWith == TermFunding.REPUTATION ? REPUTATION_HEX : SACRIFICE_HEX;
                String chipText = getTextAt(RESOURCE_BUNDLE, paidWith == TermFunding.REPUTATION
                                                                   ?
                                                                   "negotiate.contractMarket.fund.rep" :
                                                                   "negotiate.contractMarket.fund.swap");
                html.append(" <span style='color:").append(chipColor).append("'>[").append(escape(chipText))
                      .append("]</span>");
            }

            if (!valueChanged) {
                html.append(" <span style='color:").append(MUTED_HEX).append("'>&mdash; ")
                      .append(escape(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.plateau"))).append("</span>");
            }
        }
        return html.append("</html>").toString();
    }

    private String payImpactHtml() {
        // Show the current monthly pay against what it would become at the proposed base-pay step.
        double originalMultiplier = ChaosContractStepsTable.fromStepValue(
              originalStep[Clause.PAY.ordinal()]).getBasePayMultiplier();
        double proposedMultiplier = ChaosContractStepsTable.fromStepValue(
              currentStep[Clause.PAY.ordinal()]).getBasePayMultiplier();
        Money baseMonthly = contract.getMonthlyPayOut();
        Money projectedMonthly = originalMultiplier == 0 ? baseMonthly
                                       : baseMonthly.multipliedBy(proposedMultiplier / originalMultiplier);

        StringBuilder html = new StringBuilder("<html><span style='color:").append(MUTED_HEX)
                                   .append("'>")
                                   .append(escape(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.pay")))
                                   .append("</span> ")
                                   .append(escape(baseMonthly.toAmountAndSymbolString()));
        if (proposedMultiplier != originalMultiplier) {
            String color = proposedMultiplier > originalMultiplier ? hex(positiveHex()) : hex(negativeHex());
            html.append(" &rarr; <b style='color:").append(color).append("'>")
                  .append(escape(projectedMonthly.toAmountAndSymbolString())).append("</b>");
        }
        return html.append("</html>").toString();
    }

    private String budgetHtml(String label, String value) {
        return "<html><div style='text-align:right'><span style='color:" + MUTED_HEX + "; font-size:smaller'>"
                     + escape(label) + "</span><br>" + value + "</div></html>";
    }

    private static String pips(int filled, int total, String filledHex) {
        StringBuilder pips = new StringBuilder();
        for (int i = 0; i < total; i++) {
            pips.append("<span style='color:").append(i < filled ? filledHex : MUTED_HEX).append("'>&#9679;</span>");
        }
        return pips.toString();
    }

    // endregion Rendering

    // region Clause helpers

    private ChaosContractStepsTable step(Clause clause) {
        return ChaosContractStepsTable.fromStepValue(currentStep[clause.ordinal()]);
    }

    private int initialStep(Clause clause) {
        return switch (clause) {
            case COMMAND -> contract.getCommandRightsStep().stepValue();
            case PAY -> contract.getBasePayRateStep().stepValue();
            case SUPPORT -> contract.getSupportStep().stepValue();
            case TRANSPORT -> contract.getTransportStep().stepValue();
            case SALVAGE -> contract.getSalvageRightsStep().stepValue();
        };
    }

    private String valueOf(Clause clause, ChaosContractStepsTable step) {
        // Show the effective term value: the step multiplier scaled by the campaign's configured per-term multiplier,
        // so the negotiation screen matches the pay/salvage the contract will actually deliver.
        return switch (clause) {
            case COMMAND -> step.getContractCommandRights().toString();
            case PAY -> percent(step.getBasePayMultiplier() * termOption(CampaignOption.CONTRACT_BASE_PAY_MULTIPLIER));
            case SUPPORT -> getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.value.support",
                  (int) Math.round(step.getStraightSupportMultiplier()
                                         * termOption(CampaignOption.CONTRACT_STRAIGHT_SUPPORT_MULTIPLIER) * 100),
                  (int) Math.round(step.getBattlefieldLossMultiplier()
                                         * termOption(CampaignOption.CONTRACT_BATTLEFIELD_LOSS_MULTIPLIER) * 100));
            case TRANSPORT ->
                  percent(step.getTransportMultiplier() * termOption(CampaignOption.CONTRACT_TRANSPORT_MULTIPLIER));
            case SALVAGE -> step.isExchangeSalvage()
                                  ? getTextAt(RESOURCE_BUNDLE, "value.contractMarket.salvage.exchange")
                                  : percent(step.getSalvageMultiplier()
                                                  * termOption(CampaignOption.CONTRACT_SALVAGE_MULTIPLIER));
        };
    }

    /** The campaign's configured multiplier for a contract term (base pay, support, transport, salvage, etc.). */
    private double termOption(CampaignOption<Double> option) {
        return campaign.getCampaignOptions().get(option);
    }

    private String percent(double multiplier) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.percent",
              (int) Math.round(multiplier * 100));
    }

    // endregion Clause helpers

    private java.awt.Color muted() {
        return decodeHex(MUTED_HEX);
    }

    private static java.awt.Color warning() {
        return decodeHex(MekHQ.getMHQOptions().getFontColorWarningHexColor());
    }

    private static String positiveHex() {
        return MekHQ.getMHQOptions().getFontColorPositiveHexColor();
    }

    private static String negativeHex() {
        return MekHQ.getMHQOptions().getFontColorNegativeHexColor();
    }

    private static String hex(String value) {
        return value.startsWith("#") ? value : "#" + value;
    }

    private static Color decodeHex(String value) {
        try {
            return Color.decode(hex(value));
        } catch (NumberFormatException ex) {
            return Color.GRAY;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
