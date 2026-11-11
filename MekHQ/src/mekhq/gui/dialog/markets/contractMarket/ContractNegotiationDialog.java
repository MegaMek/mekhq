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
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MAXIMUM_STEP_VALUE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MINIMUM_STEP_VALUE;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.DISASTROUS;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.compute.Compute;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ActiveNegotiationData;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import mekhq.campaign.mission.contract.contractData.NegotiationData;
import mekhq.campaign.mission.contract.contractData.NonNegotiableTermsData;
import mekhq.campaign.mission.contract.contractData.RentedFacilitiesData;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractDeterminationPay;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationScale;
import mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.TermFunding;
import mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath;
import mekhq.campaign.mission.contract.utilities.ContractCharacteristics;
import mekhq.campaign.mission.contract.utilities.NegotiationStepMath;
import mekhq.campaign.mission.contract.utilities.NegotiationStepMath.Term;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;

/**
 * The contract negotiation table, drawn as a heads-up display in the style of the contract market. Lets the player
 * improve a contract's terms - spending reputation or sacrificing steps between terms - and rent support facilities,
 * before accepting the offer.
 *
 * <p>Rules (Hot Spots: Draconis Reach): each of the five terms may be raised at most {@code Scale} steps above its
 * baseline; reputation raises one term one step per point, up to {@code 2 x Scale} points total. Lowering a term skips
 * straight to the next step whose value differs (whole plateaus on the Contract Steps Table are crossed in one move),
 * banking one sacrifice step per raw step crossed; at most two distinct terms may be sacrificed, four raw steps in
 * total. Two banked steps fund a one-step raise of another term (a "swap"), at most twice.</p>
 *
 * <p>Raising a sacrificed term reverses its lowers one at a time, returning it to the step each lower started from and
 * reclaiming those banked steps. Raising a term above its baseline is single-step, and such a step may land on a
 * plateau that leaves the value unchanged - surfaced as "no change yet" so the player sees they must spend more to
 * cross a band.</p>
 *
 * <p>Each term is a track of seventeen steps showing the employer's offer, how every raised step was paid for, and
 * the steps given up; when a term cannot move, the reason is written beneath it. The negotiators, the one-time
 * re-negotiation and the impact on pay sit in a column on the right.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractNegotiationDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ContractNegotiationDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";
    static final String GUIDE_PREFIX = "guide.contractNegotiation.";

    private static final int SACRIFICE_STEPS_PER_SWAP = 2;
    private static final int MAXIMUM_SWAPS = 2;
    private static final int MAXIMUM_FACILITY_UNITS = 100;
    private static final int SIDE_WIDTH = 330;
    private static final int LABEL_WIDTH = 130;
    private static final int PORTRAIT_SIZE = 48;

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final transient AbstractLocation currentLocation;
    private transient NonNegotiableTermsData nonNegotiableTerms;

    private final int scale;
    private final int reputationPool;
    private final boolean activeNegotiators;

    private int capPerTerm;
    private int maxSacrificedTerms;
    private int maxSacrificedSteps;

    private int reputationUsed;
    private int swapsUsed;
    private int sacrificeBank;

    /** Clause order matching {@link NegotiationData} and {@link ContractTermsData} for persistence. */
    private static final Clause[] CANONICAL_ORDER =
          { Clause.PAY, Clause.SUPPORT, Clause.TRANSPORT, Clause.SALVAGE, Clause.COMMAND };

    private final int[] originalStep = new int[Clause.values().length];
    private final int[] currentStep = new int[Clause.values().length];
    private final transient List<List<TermFunding>> funding = new ArrayList<>();
    /**
     * Each negotiator's portrait, scaled once: the negotiator cards are rebuilt on every click at the table, and
     * re-reading and smoothly scaling the images each time made the steppers sluggish.
     */
    private final transient Map<Person, Icon> portraits = new HashMap<>();

    private final TermRow[] termRows = new TermRow[Clause.values().length];

    private final int[] facilityUnitCost = new int[3];
    private final int[] facilityQuantity = new int[3];
    private final int[] initialFacilityQuantity = new int[3];
    private final JLabel[] facilityQuantityLabels = new JLabel[3];
    private final JLabel[] facilityTotalLabels = new JLabel[3];
    private final HudButton[] facilityLowerButtons = new HudButton[3];

    private final Readout reputationReadout = new Readout(ACCENT);
    private final Readout swapsReadout = new Readout(AMBER);
    private final JLabel bankValue = new JLabel();
    private final JPanel negotiatorHolder = Hud.transparentPanel(new BorderLayout());
    private final JPanel employerHolder = Hud.transparentPanel(new BorderLayout());
    /** Says why re-negotiation is closed while the terms differ from the employer's offer. */
    private JTextArea termsChangedNote;
    private final JPanel impactHolder = Hud.transparentPanel(new BorderLayout());
    private final JLabel attemptsLabel = HudStyle.keyLabel("");
    private final HudVerdictBanner resultBanner = new HudVerdictBanner();
    private JLabel limitsLabel;
    private HudButton resetButton;
    private HudButton confirmButton;
    private HudConfirmButton haggleButton;
    private HudConfirmButton exceptionButton;
    private MarketGuide guide;
    private JScrollPane mainScroll;
    private JScrollPane sideScroll;

    private boolean confirmed;

    /** The five negotiable clauses, in display order. */
    private enum Clause {
        COMMAND("negotiate.contractMarket.term.command", Term.COMMAND_RIGHTS),
        PAY("negotiate.contractMarket.term.pay", Term.BASE_PAY),
        SUPPORT("negotiate.contractMarket.term.support", Term.SUPPORT),
        TRANSPORT("negotiate.contractMarket.term.transport", Term.TRANSPORT),
        SALVAGE("negotiate.contractMarket.term.salvage", Term.SALVAGE);

        private final String labelKey;
        private final Term term;

        Clause(String labelKey, Term term) {
            this.labelKey = labelKey;
            this.term = term;
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
        if (contract.getPlayerNegotiator() == null) {
            contract.setPlayerNegotiator(ContractNegotiatorPickerDialog.leadNegotiator(campaign));
        }
        recomputeNegotiatorCaps();
        boolean useChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);
        int reputation = campaign.getPlayerForce().getReputationRating(useChaosReputation);
        this.reputationPool = Math.clamp(reputation, 0, 2 * scale);

        NonNegotiableTermsData lockedTerms = contract.getNonNegotiableTermsData();
        this.nonNegotiableTerms = lockedTerms != null ? lockedTerms : NonNegotiableTermsData.none();
        this.activeNegotiators = campaignOptions.get(CampaignOption.USE_ACTIVE_NEGOTIATORS);

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
        System.arraycopy(facilityQuantity, 0, initialFacilityQuantity, 0, facilityQuantity.length);

        initializeComponents();
    }

    /** @return whether the player confirmed the negotiated terms. */
    public boolean wasConfirmed() {
        return confirmed;
    }

    // region Layout

    private void initializeComponents() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "title.contractMarket.negotiate"));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(SURFACE_DEEP);
        mainScroll = MarketHud.scroll(buildMain(), SURFACE_DEEP);
        body.add(mainScroll, BorderLayout.CENTER);
        sideScroll = MarketHud.scroll(buildSide(), GROUND);
        JComponent side = sideScroll;
        side.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        side.setPreferredSize(new Dimension(scaleForGUI(SIDE_WIDTH), 0));
        body.add(side, BorderLayout.EAST);
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        guide = new MarketGuide(this);
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              "closeGuide");
        content.getActionMap().put("closeGuide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                guide.close();
            }
        });

        refresh();

        setMinimumSize(scaleForGUI(960, 600));
        pack();
        setSize(scaleForGUI(1120, 720)); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(getParent());
        setPreferences(); // Must be before setVisible
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                // Both columns open at their top, whatever their contents scrolled into view while being laid out.
                SwingUtilities.invokeLater(() -> {
                    mainScroll.getVerticalScrollBar().setValue(0);
                    sideScroll.getVerticalScrollBar().setValue(0);
                });
                // The guide opens by itself the first time the player sits at the table.
                if (ContractMarketGuidePreferences.getInstance().shouldAutoOpen(GUIDE_PREFIX)) {
                    openGuide();
                }
            }
        });
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

    private void openGuide() {
        ContractMarketGuidePreferences.getInstance().setGuideSeen(GUIDE_PREFIX, true);
        guide.open(GUIDE_PREFIX);
    }

    /** The market-style command bar: the contract, and the reputation, swaps and banked steps left to spend. */
    private JPanel buildCommandBar() {
        JPanel bar = new JPanel(new BorderLayout(scaleForGUI(20), 0));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JPanel identity = MarketHud.column();
        String scaleText = ChaosContractDeterminationScale.formatScale(contract.getExactScale(),
              campaign.getCampaignOptions().get(CampaignOption.USE_FRACTIONAL_SCALE));
        identity.add(leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.eyebrow",
              contract.getEmployerMarketDisplayName(), scaleText))));
        identity.add(Box.createVerticalStrut(scaleForGUI(2)));
        JLabel title = new JLabel(ContractMarketFormatting.marketName(contract).toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.12f, 0.12f));
        identity.add(leftAligned(title));

        JPanel readouts = Hud.transparentPanel(null);
        readouts.setLayout(new BoxLayout(readouts, BoxLayout.X_AXIS));
        readouts.add(reputationReadout.build(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.reputation"),
              getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.reputation.tooltip")));
        readouts.add(Box.createHorizontalStrut(scaleForGUI(6)));
        readouts.add(swapsReadout.build(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.swaps"),
              getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.swaps.tooltip")));
        readouts.add(Box.createHorizontalStrut(scaleForGUI(6)));
        JPanel bank = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.bank"), "", AMBER,
              SURFACE_DEEP);
        bank.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.budget.bank.tooltip")));
        bank.remove(1);
        bankValue.setForeground(AMBER);
        bankValue.setFont(hudFont(Font.BOLD, 1.08f, 0.0f));
        bankValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        bank.add(bankValue);
        readouts.add(bank);
        readouts.add(Box.createHorizontalStrut(scaleForGUI(10)));
        HudButton guideButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.guide")
                                                    .toUpperCase(Locale.ROOT), false, true);
        guideButton.addActionListener(event -> openGuide());
        readouts.add(guideButton);

        bar.add(identity, BorderLayout.CENTER);
        bar.add(readouts, BorderLayout.EAST);
        return bar;
    }

    /** The terms as step tracks with a legend, then the rented facilities. */
    private JPanel buildMain() {
        JPanel main = MarketHud.column();
        main.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(18), scaleForGUI(18),
              scaleForGUI(18)));
        main.add(leftAligned(heading("negotiate.contractMarket.section.terms")));
        for (Clause clause : Clause.values()) {
            termRows[clause.ordinal()] = new TermRow(clause);
            main.add(leftAligned(termRows[clause.ordinal()].panel));
        }
        main.add(leftAligned(buildLegend()));

        if (!isPirateContract()) {
            main.add(leftAligned(heading("negotiate.contractMarket.section.facilities")));
            String[] facilityKeys = { "negotiate.contractMarket.facility.hospitalBeds",
                                      "negotiate.contractMarket.facility.kitchens",
                                      "negotiate.contractMarket.facility.holdingCells" };
            for (int index = 0; index < facilityKeys.length; index++) {
                main.add(leftAligned(buildFacilityRow(index, facilityKeys[index])));
            }
        }
        return main;
    }

    private static JComponent heading(String key) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), 0, scaleForGUI(4), 0));
        holder.add(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, key)), BorderLayout.CENTER);
        return holder;
    }

    /** What each colour on the tracks means. */
    private JComponent buildLegend() {
        JPanel legend = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(14),
              scaleForGUI(2)));
        legend.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(4), 0, 0));
        legend.add(legendItem(CellStyle.OFFER, "negotiate.contractMarket.legend.offer"));
        legend.add(legendItem(CellStyle.REPUTATION, "negotiate.contractMarket.legend.reputation"));
        legend.add(legendItem(CellStyle.SWAP, "negotiate.contractMarket.legend.swap"));
        legend.add(legendItem(CellStyle.GIVEN_UP, "negotiate.contractMarket.legend.givenUp"));
        return legend;
    }

    private static JLabel legendItem(CellStyle style, String key) {
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, key));
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        label.setIcon(new Icon() {
            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                style.paint((Graphics2D) graphics, x, y, getIconWidth(), getIconHeight());
            }

            @Override
            public int getIconWidth() {
                return scaleForGUI(12);
            }

            @Override
            public int getIconHeight() {
                return scaleForGUI(10);
            }
        });
        label.setIconTextGap(scaleForGUI(6));
        return label;
    }

    private JPanel buildFacilityRow(int index, String labelKey) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(9), scaleForGUI(6), 0)));

        String costEach = getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.facility.each",
              Money.of(facilityUnitCost[index]).toAmountAndSymbolString());
        String benefit = getTextAt(RESOURCE_BUNDLE, labelKey + ".benefit");
        JLabel name = new JLabel("<html>" + MarketHud.escape(getTextAt(RESOURCE_BUNDLE, labelKey))
                                       + "<br><span style='color:" + hex(TEXT_FAINT) + "'>"
                                       + MarketHud.escape(costEach) + " &middot; " + MarketHud.escape(benefit)
                                       + "</span></html>");
        name.setForeground(TEXT);
        name.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        row.add(name, BorderLayout.CENTER);

        facilityLowerButtons[index] = stepButton("negotiate.contractMarket.stepper.decrease",
              () -> adjustFacility(index, -1));
        HudButton more = stepButton("negotiate.contractMarket.stepper.increase", () -> adjustFacility(index, 1));
        facilityQuantityLabels[index] = new JLabel("", SwingConstants.CENTER);
        facilityQuantityLabels[index].setForeground(TEXT);
        facilityQuantityLabels[index].setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        facilityQuantityLabels[index].setPreferredSize(new Dimension(scaleForGUI(30),
              facilityQuantityLabels[index].getPreferredSize().height + scaleForGUI(4)));
        facilityTotalLabels[index] = new JLabel("", SwingConstants.RIGHT);
        facilityTotalLabels[index].setForeground(TEXT_MUTED);
        facilityTotalLabels[index].setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        facilityTotalLabels[index].setPreferredSize(new Dimension(scaleForGUI(130),
              facilityTotalLabels[index].getPreferredSize().height + scaleForGUI(4)));

        JPanel controls = Hud.transparentPanel(null);
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.add(facilityLowerButtons[index]);
        controls.add(Box.createHorizontalStrut(scaleForGUI(4)));
        controls.add(facilityQuantityLabels[index]);
        controls.add(Box.createHorizontalStrut(scaleForGUI(4)));
        controls.add(more);
        controls.add(Box.createHorizontalStrut(scaleForGUI(12)));
        controls.add(facilityTotalLabels[index]);
        row.add(controls, BorderLayout.EAST);
        return row;
    }

    /** The negotiators, the re-negotiation, and the impact on pay. */
    private JPanel buildSide() {
        JPanel side = MarketHud.column();
        side.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(16),
              scaleForGUI(14)));

        side.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.negotiator.player.title"))));
        side.add(Box.createVerticalStrut(scaleForGUI(5)));
        side.add(leftAligned(negotiatorHolder));
        side.add(Box.createVerticalStrut(scaleForGUI(14)));

        Person employerNegotiator = contract.getEmployerNegotiator();
        if (employerNegotiator != null) {
            side.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.negotiator.employer.title"))));
            side.add(Box.createVerticalStrut(scaleForGUI(5)));
            side.add(leftAligned(employerHolder));
            side.add(Box.createVerticalStrut(scaleForGUI(14)));
        }

        // The re-negotiation only exists when the campaign uses active negotiators.
        if (activeNegotiators) {
            JPanel headingRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            headingRow.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.section.renegotiate")),
                  BorderLayout.WEST);
            attemptsLabel.setForeground(ACCENT_BRIGHT);
            headingRow.add(attemptsLabel, BorderLayout.EAST);
            side.add(leftAligned(headingRow));
            side.add(Box.createVerticalStrut(scaleForGUI(5)));
            side.add(leftAligned(buildRenegotiationCard()));
            side.add(Box.createVerticalStrut(scaleForGUI(14)));
        }

        side.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.section.impact"))));
        side.add(Box.createVerticalStrut(scaleForGUI(5)));
        side.add(leftAligned(impactHolder));
        return side;
    }

    /**
     * The re-negotiation: what it does, and one confirm-twice button for each way to press the case. The result is
     * shown in place once an attempt has been made.
     */
    private JPanel buildRenegotiationCard() {
        JPanel content = MarketHud.column();
        content.add(leftAligned(MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.renegotiate.explain"), TEXT_FAINT, Font.PLAIN, 0.84f)));
        content.add(Box.createVerticalStrut(scaleForGUI(8)));
        termsChangedNote = MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.renegotiate.termsChanged"), AMBER, Font.PLAIN, 0.84f);
        termsChangedNote.setBorder(BorderFactory.createEmptyBorder(0, 0, scaleForGUI(8), 0));
        content.add(leftAligned(termsChangedNote));

        haggleButton = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.renegotiate.haggle")
                                                  .toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.renegotiate.confirm").toUpperCase(Locale.ROOT),
              true);
        haggleButton.addActionListener(event -> renegotiate(true));
        exceptionButton = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.renegotiate.exception").toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.renegotiate.confirm").toUpperCase(Locale.ROOT),
              true);
        exceptionButton.addActionListener(event -> renegotiate(false));

        content.add(leftAligned(option(haggleButton, "negotiate.contractMarket.renegotiate.haggle.detail")));
        content.add(Box.createVerticalStrut(scaleForGUI(6)));
        content.add(leftAligned(option(exceptionButton, "negotiate.contractMarket.renegotiate.exception.detail")));
        content.add(Box.createVerticalStrut(scaleForGUI(8)));
        resultBanner.setVisible(false);
        content.add(leftAligned(resultBanner));
        return card(content);
    }

    private static JPanel option(HudConfirmButton button, String detailKey) {
        JPanel option = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(2)));
        option.add(button, BorderLayout.NORTH);
        option.add(MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE, detailKey), TEXT_FAINT, Font.PLAIN, 0.8f),
              BorderLayout.CENTER);
        return option;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));

        limitsLabel = Hud.hint("");
        footer.add(limitsLabel, BorderLayout.CENTER);

        resetButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.reset")
                                          .toUpperCase(Locale.ROOT), false);
        resetButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.reset.tooltip"));
        resetButton.addActionListener(event -> resetAll());
        HudButton cancel = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.cancel")
                                               .toUpperCase(Locale.ROOT), false);
        cancel.addActionListener(event -> dispose());
        confirmButton = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.confirm")
                                            .toUpperCase(Locale.ROOT), true);
        confirmButton.addActionListener(event -> confirmAction());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(resetButton);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(confirmButton);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    // endregion Layout

    // region Negotiators

    /**
     * Rebuilds the "your negotiator" card: their portrait, Negotiation skill and Edge, and each negotiation trait with
     * what it does at this table; or, with no negotiator chosen, a prompt to choose one.
     */
    private void refreshNegotiatorCard() {
        Person negotiator = contract.getPlayerNegotiator();
        JPanel content = MarketHud.column();
        if (negotiator == null) {
            JLabel none = new JLabel(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.negotiator.none"));
            none.setForeground(AMBER);
            none.setFont(hudFont(Font.BOLD, 0.92f, 0.0f));
            content.add(leftAligned(none));
            content.add(Box.createVerticalStrut(scaleForGUI(4)));
            content.add(leftAligned(MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.negotiator.player.empty"), TEXT_FAINT, Font.PLAIN, 0.84f)));
        } else {
            content.add(leftAligned(personSummary(negotiator)));
            List<JComponent> traits = traitRows(negotiator);
            if (!traits.isEmpty()) {
                content.add(Box.createVerticalStrut(scaleForGUI(8)));
                traits.forEach(trait -> {
                    content.add(leftAligned(trait));
                    content.add(Box.createVerticalStrut(scaleForGUI(6)));
                });
            }
        }
        content.add(Box.createVerticalStrut(scaleForGUI(8)));
        String changeKey = negotiator == null
                                 ? "negotiate.contractMarket.negotiator.choose"
                                 : "negotiate.contractMarket.negotiator.change";
        HudButton change = new HudButton(getTextAt(RESOURCE_BUNDLE, changeKey).toUpperCase(Locale.ROOT),
              negotiator == null, true);
        change.addActionListener(event -> openNegotiatorPicker());
        if (isNegotiatorLocked()) {
            change.setArmed(false);
            change.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.negotiator.change.locked")));
        }
        JPanel changeRow = Hud.transparentPanel(new BorderLayout());
        changeRow.add(change, BorderLayout.WEST);
        content.add(leftAligned(changeRow));

        negotiatorHolder.removeAll();
        negotiatorHolder.add(card(content), BorderLayout.CENTER);
        negotiatorHolder.revalidate();
        negotiatorHolder.repaint();
    }

    /**
     * The employer's negotiator, with the player's chances against them when the campaign uses re-negotiation. The
     * chances follow the player's negotiator, so the card is rebuilt with every refresh.
     */
    private void refreshEmployerCard() {
        Person employerNegotiator = contract.getEmployerNegotiator();
        if (employerNegotiator == null) {
            return;
        }
        JPanel content = MarketHud.column();
        content.add(leftAligned(personSummary(employerNegotiator)));
        if (activeNegotiators) {
            content.add(Box.createVerticalStrut(scaleForGUI(10)));
            content.add(leftAligned(renegotiationOdds(employerNegotiator)));
        }
        employerHolder.removeAll();
        employerHolder.add(card(content), BorderLayout.CENTER);
        employerHolder.revalidate();
        employerHolder.repaint();
    }

    /**
     * The chance the player's negotiator comes out ahead of the employer's on each re-negotiation, worked out exactly
     * from both target numbers, natural aptitude, Edge rerolls and the player's traits.
     */
    private JPanel renegotiationOdds(Person employerNegotiator) {
        JPanel block = MarketHud.column();
        JLabel heading = new JLabel(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.odds.heading")
                                          .toUpperCase(Locale.ROOT));
        heading.setForeground(TEXT_FAINT);
        heading.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        heading.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.odds.tooltip")));
        block.add(leftAligned(heading));
        block.add(Box.createVerticalStrut(scaleForGUI(4)));

        Person playerNegotiator = contract.getPlayerNegotiator();
        if (playerNegotiator == null) {
            block.add(leftAligned(MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.odds.none"), TEXT_FAINT, Font.PLAIN, 0.8f)));
            return block;
        }
        NegotiatorProfile.TableOdds odds = NegotiatorProfile.odds(campaign, playerNegotiator, employerNegotiator,
              scale);
        block.add(leftAligned(oddsRow("negotiate.contractMarket.odds.haggle", odds.haggle())));
        block.add(Box.createVerticalStrut(scaleForGUI(2)));
        block.add(leftAligned(oddsRow("negotiate.contractMarket.odds.exception", odds.exception())));
        return block;
    }

    /** One re-negotiation's chances: its name, and how often the player prevails, coloured by who is favoured. */
    private static JPanel oddsRow(String labelKey, ActiveNegotiationMath.Odds odds) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        JLabel name = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        name.setForeground(TEXT_MUTED);
        name.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        row.add(name, BorderLayout.WEST);
        JLabel value = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.odds.value",
              percentOf(odds.win()), percentOf(odds.stalemate())));
        value.setForeground(odds.win() >= odds.loss() ? READY : DANGER);
        value.setFont(hudFont(Font.BOLD, 0.84f, 0.0f));
        row.add(value, BorderLayout.EAST);
        row.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.odds.row.tooltip",
              percentOf(odds.win()), percentOf(odds.stalemate()), percentOf(odds.loss())));
        return row;
    }

    private static long percentOf(double chance) {
        return Math.round(chance * 100);
    }

    /** A person's portrait beside their name and Negotiation skill (and Edge, when the campaign uses it). */
    private JPanel personSummary(Person person) {
        JPanel summary = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        JLabel portrait = MarketHud.portrait(null, scaleForGUI(PORTRAIT_SIZE));
        portrait.setIcon(portraits.computeIfAbsent(person,
              key -> MarketHud.portrait(key, scaleForGUI(PORTRAIT_SIZE)).getIcon()));
        summary.add(MarketHud.topAligned(portrait), BorderLayout.WEST);
        JPanel text = MarketHud.column();
        JLabel name = new JLabel(person.getFullTitle());
        name.setForeground(TEXT);
        name.setFont(hudFont(Font.BOLD, 0.92f, 0.0f));
        text.add(leftAligned(name));
        JLabel skill = new JLabel(skillSummary(person));
        skill.setForeground(TEXT_MUTED);
        skill.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        text.add(leftAligned(skill));
        summary.add(text, BorderLayout.CENTER);
        return summary;
    }

    /** "Negotiation 4 · needs 7+ · Edge 2", or "Untrained" when the person has no Negotiation skill. */
    private String skillSummary(Person person) {
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        String text;
        if (skill == null) {
            text = getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.negotiator.untrained");
        } else {
            SkillModifierData modifiers = person.getSkillModifierData();
            text = getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.negotiator.skill",
                  skill.getTotalSkillLevel(modifiers), skill.getSkillTargetNumber(modifiers, 0));
        }
        if (campaign.getCampaignOptions().get(CampaignOption.USE_EDGE)) {
            // The Edge left to spend, as the picker shows it and the odds assume, rather than the person's maximum.
            text = getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.negotiator.edge", text,
                  person.getCurrentEdge());
        }
        return text;
    }

    /** Each negotiation trait the person carries, named in green or red, with what it does at this table. */
    private List<JComponent> traitRows(Person person) {
        boolean useEdge = campaign.getCampaignOptions().get(CampaignOption.USE_EDGE);
        List<JComponent> rows = new ArrayList<>();
        for (String trait : ContractNegotiatorPickerDialog.RELEVANT_TRAITS) {
            // The Edge negotiation trigger has no effect when Edge is disabled, so it is not listed then.
            if ((trait.equals(PersonnelOptions.EDGE_COMMANDER_NEGOTIATION) && !useEdge)
                      || !person.getOptions().booleanOption(trait)) {
                continue;
            }
            IOption option = person.getOptions().getOption(trait);
            if (option == null) {
                continue;
            }
            String name = option.getDisplayableName().replaceAll("\\s*\\([^)]*\\)", "");
            // The trait's name over its effect, so both have the card's full width.
            JPanel row = MarketHud.column();
            JLabel nameLabel = new JLabel(name);
            nameLabel.setForeground(NegotiatorProfile.isHelpfulTrait(trait) ? READY : DANGER);
            nameLabel.setFont(hudFont(Font.BOLD, 0.82f, 0.0f));
            row.add(leftAligned(nameLabel));
            JTextArea effect = MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE,
                  "negotiate.contractMarket.trait." + trait), TEXT_FAINT, Font.PLAIN, 0.8f);
            row.add(leftAligned(effect));
            if (option.getDescription() != null) {
                String toolTip = MarketHud.wrapToolTip(option.getDescription());
                row.setToolTipText(toolTip);
                nameLabel.setToolTipText(toolTip);
                effect.setToolTipText(toolTip);
            }
            rows.add(row);
        }
        return rows;
    }


    /**
     * Whether the negotiator is fixed for this contract: once a re-negotiation has been made, the negotiator who made
     * it stays at the table. Otherwise the attempts left would follow whoever sat down next, and swapping in a
     * Relentless Bargainer after another negotiator's attempt would win a roll neither had alone.
     */
    private boolean isNegotiatorLocked() {
        return contract.getPlayerNegotiator() != null && attemptsSoFar() > 0;
    }

    private void openNegotiatorPicker() {
        if (isNegotiatorLocked()) {
            return;
        }
        ContractNegotiatorPickerDialog picker = new ContractNegotiatorPickerDialog(campaign, contract,
              contract.getPlayerNegotiator());
        if (picker.wasConfirmed()) {
            contract.setPlayerNegotiator(picker.getSelectedNegotiator());
            recomputeNegotiatorCaps();
            // Terms one negotiator was allowed may be beyond the next one's limits; rather than mix the two
            // negotiators' traits, the manual haggling starts over within the new limits.
            if (exceedsNegotiatorLimits()) {
                resetTerms();
            }
            refresh();
        }
    }

    /**
     * Recomputes the manual-haggle caps from the chosen negotiator's contract SPAs and Flaws: Hard Bargainer / Pushover
     * shift the per-term raise cap, Shrewd Trader / Inflexible shift the sacrifice budget (both its distinct-term and
     * its total-step limits). With no negotiator the base values apply. Called at construction and whenever the
     * negotiator changes.
     */
    private void recomputeNegotiatorCaps() {
        NegotiatorProfile profile = NegotiatorProfile.of(contract.getPlayerNegotiator(), scale);
        capPerTerm = profile.raiseCap();
        maxSacrificedTerms = profile.sacrificeTerms();
        maxSacrificedSteps = profile.sacrificeSteps();
    }

    // endregion Negotiators

    // region Negotiation logic

    /** Whether this contract is a pirate raid, which cannot rent support facilities. */
    private boolean isPirateContract() {
        return contract.getObjectiveType().getChaosObjectiveType() == ChaosObjectiveType.PIRATE_RAID;
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
        if (currentStep[index] >= CHAOS_CONTRACT_MAXIMUM_STEP_VALUE) {
            return;
        }

        if (currentStep[index] < originalStep[index]) {
            int target = restoreTarget(clause);
            int gap = target - currentStep[index];
            if (gap > sacrificeBank) {
                return;
            }
            sacrificeBank -= gap;
            currentStep[index] = target;
            refresh();
            return;
        }

        if ((currentStep[index] - originalStep[index]) >= capPerTerm) {
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

        if (currentStep[index] > originalStep[index]) {
            TermFunding paidWith = funding.get(index).removeLast();
            if (paidWith == TermFunding.REPUTATION) {
                reputationUsed--;
            } else {
                swapsUsed--;
                sacrificeBank += SACRIFICE_STEPS_PER_SWAP;
            }
            currentStep[index]--;
            refresh();
            return;
        }

        int target = NegotiationStepMath.nextLowerDifferentStep(clause.term, currentStep[index]);
        if (target < CHAOS_CONTRACT_MINIMUM_STEP_VALUE) {
            return;
        }
        int gap = currentStep[index] - target;
        if (!canSacrifice(clause, gap)) {
            return;
        }

        currentStep[index] = target;
        sacrificeBank += gap;
        refresh();
    }

    /**
     * Whether {@code gap} more raw steps may be sacrificed from this clause, honoring the two negotiator-adjusted caps:
     * at most {@link #maxSacrificedTerms} distinct terms sacrificed, and at most {@link #maxSacrificedSteps} raw steps
     * in total across them.
     */
    private boolean canSacrifice(Clause clause, int gap) {
        boolean alreadySacrificed = currentStep[clause.ordinal()] < originalStep[clause.ordinal()];
        return NegotiationStepMath.sacrificeAllowed(gap, alreadySacrificed,
              NegotiationStepMath.distinctTermsSacrificed(originalStep, currentStep),
              NegotiationStepMath.totalStepsSacrificed(originalStep, currentStep),
              maxSacrificedTerms, maxSacrificedSteps);
    }

    /** Whether the employer has locked this clause as non-negotiable - it can be neither raised nor lowered. */
    private boolean isLocked(Clause clause) {
        return isPermanentlyLocked(clause) || nonNegotiableTerms.isLocked(clause.term);
    }

    /**
     * Whether this clause is permanently fixed for this contract and can never be re-negotiated, lowered, raised, or
     * waived. Unlike an ordinary employer's non-negotiable lock - which the "exception" active negotiation can waive -
     * these are dictated outright by a pirate raid: command rights and salvage on every pirate raid, plus support and
     * transport on a self-directed (non-covert) raid (a secretly-bankrolled covert raid leaves those to its hidden
     * sponsor).
     */
    private boolean isPermanentlyLocked(Clause clause) {
        if (!isPirateContract()) {
            return false;
        }
        return switch (clause) {
            case COMMAND, SALVAGE -> true;
            case SUPPORT, TRANSPORT -> !contract.isCovertOperation();
            case PAY -> false;
        };
    }

    /**
     * Why the term cannot be raised right now, or {@code null} when it can. The order follows {@link #raise}, so the
     * reason given is the one actually stopping it.
     */
    private String raiseBlockReason(Clause clause) {
        if (isPermanentlyLocked(clause)) {
            return getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.fixed");
        }
        if (isLocked(clause)) {
            return getTextAt(RESOURCE_BUNDLE, activeNegotiators
                                                    ? "negotiate.contractMarket.why.locked.exception"
                                                    : "negotiate.contractMarket.why.locked");
        }
        int index = clause.ordinal();
        if (currentStep[index] >= CHAOS_CONTRACT_MAXIMUM_STEP_VALUE) {
            return getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.top");
        }
        if (currentStep[index] < originalStep[index]) {
            int gap = restoreTarget(clause) - currentStep[index];
            return gap <= sacrificeBank ? null
                         : getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.takeBack", gap);
        }
        if ((currentStep[index] - originalStep[index]) >= capPerTerm) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.cap", capPerTerm);
        }
        if (reputationUsed < reputationPool) {
            return null;
        }
        if (swapsUsed >= MAXIMUM_SWAPS) {
            return getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.noSwaps");
        }
        if (sacrificeBank < SACRIFICE_STEPS_PER_SWAP) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.needBank",
                  SACRIFICE_STEPS_PER_SWAP - sacrificeBank);
        }
        return null;
    }

    /** Why the term cannot be lowered right now, or {@code null} when it can. */
    private String lowerBlockReason(Clause clause) {
        if (isPermanentlyLocked(clause)) {
            return getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.fixed");
        }
        if (isLocked(clause)) {
            return getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.locked");
        }
        int index = clause.ordinal();
        if (currentStep[index] > originalStep[index]) {
            return null;
        }
        int target = NegotiationStepMath.nextLowerDifferentStep(clause.term, currentStep[index]);
        if (target < CHAOS_CONTRACT_MINIMUM_STEP_VALUE) {
            return getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.bottom");
        }
        if (canSacrifice(clause, currentStep[index] - target)) {
            return null;
        }
        boolean alreadySacrificed = currentStep[index] < originalStep[index];
        boolean termLimitReached = !alreadySacrificed
                                         && NegotiationStepMath.distinctTermsSacrificed(originalStep, currentStep)
                                                  >= maxSacrificedTerms;
        return termLimitReached
                     ? getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.termLimit",
                           maxSacrificedTerms)
                     : getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.why.stepLimit",
                           maxSacrificedSteps);
    }

    private void adjustFacility(int index, int delta) {
        facilityQuantity[index] = Math.clamp(facilityQuantity[index] + delta, 0, MAXIMUM_FACILITY_UNITS);
        refresh();
    }

    private void resetAll() {
        resetTerms();
        facilityQuantity[0] = contract.getRentedHospitalBeds();
        facilityQuantity[1] = contract.getRentedKitchens();
        facilityQuantity[2] = contract.getRentedHoldingCells();
        refresh();
    }

    /** Puts every term back to the baseline and refunds everything spent on them. */
    private void resetTerms() {
        System.arraycopy(originalStep, 0, currentStep, 0, originalStep.length);
        clearManualHaggling();
    }

    /**
     * Whether the terms on screen go past what the chosen negotiator may do: a term raised beyond their raise cap, or
     * more terms or steps given up than they may give up. Raises and sacrifices are only checked as they are made, so
     * this catches a negotiator swapped in after them.
     */
    private boolean exceedsNegotiatorLimits() {
        for (Clause clause : Clause.values()) {
            if (currentStep[clause.ordinal()] - originalStep[clause.ordinal()] > capPerTerm) {
                return true;
            }
        }
        return NegotiationStepMath.distinctTermsSacrificed(originalStep, currentStep) > maxSacrificedTerms
                     || NegotiationStepMath.totalStepsSacrificed(originalStep, currentStep) > maxSacrificedSteps;
    }

    private void confirmAction() {
        commitTermsAndPay();

        if (isPirateContract()) {
            contract.setRentedFacilitiesData(new RentedFacilitiesData(0, 0, 0));
        } else {
            contract.setRentedFacilitiesData(new RentedFacilitiesData(facilityQuantity[0],
                  facilityQuantity[1], facilityQuantity[2]));
        }

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

    /**
     * Writes the current proposed term steps onto the contract and recomputes its pay from them. The pay and combat pay
     * characteristics are baked back on, as generation did, since the recomputed pay starts from the scheme's base.
     */
    private void commitTermsAndPay() {
        contract.setContractTerms(new ContractTermsData(step(Clause.PAY), step(Clause.SUPPORT), step(Clause.TRANSPORT),
              step(Clause.SALVAGE), step(Clause.COMMAND)));
        AbstractContractDeterminationPay payScheme = AbstractContractDeterminationPay.forCampaign(campaign);
        contract.updateMonthlyPay(ContractCharacteristics.bakeMonthlyPay(
              payScheme.getContractMonthlyPay(campaign, contract), contract));
        contract.updateCombatPay(ContractCharacteristics.bakeCombatPay(
              payScheme.getContractCombatPay(campaign, contract), contract));
        contract.updateTransportPay(payScheme.getTransportPay(campaign,
              campaign.getLocalDate(), contract, currentLocation));
    }

    // endregion Negotiation logic

    // region Active negotiation

    /**
     * Runs one active-negotiation attempt: an opposed Negotiation check between the player's and the employer's
     * negotiators. {@code haggle} presses for a better offer - each margin-of-success level the player wins improves a
     * random term one meaningful step, each level lost lowers one, and the outcome becomes a fresh baseline. Otherwise
     * it asks for an exception - the stakes are the non-negotiable flags. Either way the
     * result is committed immediately and cannot be undone. It is only open while every term matches the employer's
     * offer (see {@link #termsChanged()}). The confirm-twice buttons stand in for the old prompt, so a stray click
     * never spends the attempt.
     */
    private void renegotiate(boolean haggle) {
        if (contract.getPlayerNegotiator() == null || attemptsSoFar() >= maxRenegotiationAttempts()
                  || termsChanged()) {
            return;
        }
        if (haggle) {
            performHaggle();
        } else {
            performException();
        }
    }

    /**
     * The net margin of the opposed Negotiation check between the player's and the employer's negotiators (positive
     * favors the player). A missing employer negotiator counts as a neutral result.
     */
    private int rollNetMargin() {
        Person playerNegotiator = contract.getPlayerNegotiator();
        if (playerNegotiator == null) {
            // This shouldn't happen, so we go ahead and log it.
            LOGGER.warn("Contract {} has no player negotiator", contract.getName());
            return ActiveNegotiationMath.netMargin(DISASTROUS, MarginOfSuccess.BARELY_MADE_IT);
        }

        boolean isUseEdge = campaign.getCampaignOptions().get(CampaignOption.USE_EDGE);
        boolean playerUsesEdge = isUseEdge &&
                                       playerNegotiator.getOptions()
                                             .booleanOption(PersonnelOptions.EDGE_COMMANDER_NEGOTIATION);
        MarginOfSuccess playerMargin = reportedNegotiationCheck(playerNegotiator,
              "negotiate.contractMarket.renegotiate.roll.player",
              playerUsesEdge);
        Person employerNegotiator = contract.getEmployerNegotiator();
        MarginOfSuccess employerMargin = employerNegotiator != null ?
                                               reportedNegotiationCheck(employerNegotiator,
                                                     "negotiate.contractMarket.renegotiate.roll.employer",
                                                     isUseEdge) :
                                               MarginOfSuccess.BARELY_MADE_IT;
        return ActiveNegotiationMath.netMargin(playerMargin, employerMargin)
                     + NegotiatorProfile.of(playerNegotiator, scale).generalModifier();
    }

    /**
     * Resolves a Negotiation check for the given person, posts the result to the daily report's skill-checks tab, and
     * returns its margin of success.
     */
    private MarginOfSuccess reportedNegotiationCheck(Person negotiator, String reasonKey, boolean useEdge) {
        ActionCheckResult result = negotiator.checkSkill(SkillType.S_NEGOTIATION, campaign)
                                         .resolve(useEdge, getTextAt(RESOURCE_BUNDLE, reasonKey));
        campaign.addReport(DailyReportType.SKILL_CHECKS, result.getReport());
        return result.getReportMargin();
    }

    /** Runs the opposed check and rewrites the terms as a fresh baseline. Spends one attempt. */
    private void performHaggle() {
        int net = rollNetMargin() + NegotiatorProfile.of(contract.getPlayerNegotiator(), scale).haggleModifier();
        int[] moveCounts = applyActiveNegotiation(net);
        commitAsNewBaseline();
        contract.setActiveNegotiationData(ActiveNegotiationData.haggle(attemptsSoFar() + 1, net,
              moveCounts[Clause.PAY.ordinal()], moveCounts[Clause.SUPPORT.ordinal()],
              moveCounts[Clause.TRANSPORT.ordinal()], moveCounts[Clause.SALVAGE.ordinal()],
              moveCounts[Clause.COMMAND.ordinal()]));

        LOGGER.info("Active negotiation (haggle) for contract {}: net margin {}", contract.getName(), net);
        refresh();
    }

    /**
     * Runs the same opposed check, but the stakes are the non-negotiable flags: each margin of success waives a random
     * locked term, each margin of failure locks a random unlocked one. Spends one attempt.
     */
    private void performException() {
        int net = rollNetMargin() + NegotiatorProfile.of(contract.getPlayerNegotiator(), scale).exceptionModifier();
        int[] lockChanges = applyLockChanges(net);
        // The terms on screen match the employer's offer, but the contract may still hold an earlier confirmed
        // negotiation that the player has only reset here. Committing them keeps a cancelled table from leaving a
        // raised term behind under a lock this attempt just set.
        commitAsNewBaseline();
        contract.setNonNegotiableTermsData(nonNegotiableTerms);
        contract.setActiveNegotiationData(ActiveNegotiationData.exception(attemptsSoFar() + 1, net,
              lockChanges[Clause.PAY.ordinal()], lockChanges[Clause.SUPPORT.ordinal()],
              lockChanges[Clause.TRANSPORT.ordinal()], lockChanges[Clause.SALVAGE.ordinal()],
              lockChanges[Clause.COMMAND.ordinal()]));

        LOGGER.info("Active negotiation (exception) for contract {}: net margin {}", contract.getName(), net);
        refresh();
    }

    /**
     * Makes the terms on screen the new baseline after a re-negotiation: manual haggling starts fresh from them, and
     * they are written to the contract at once, as the re-negotiation itself is.
     */
    private void commitAsNewBaseline() {
        clearManualHaggling();
        commitTermsAndPay();
        contract.setNegotiationData(null);
    }

    /** Spends nothing: no reputation, swaps or banked steps, and no raise paid for. */
    private void clearManualHaggling() {
        reputationUsed = 0;
        swapsUsed = 0;
        sacrificeBank = 0;
        for (Clause clause : Clause.values()) {
            funding.get(clause.ordinal()).clear();
        }
    }

    /**
     * Applies {@code net} lock toggles: each step (net &gt; 0) waives a randomly chosen still-locked term, or (net &lt;
     * 0) locks a randomly chosen still-unlocked one, with the pool shrinking as it goes; when none remain the rest are
     * dropped. Returns the signed change per term (+1 waived, -1 locked, indexed by clause ordinal).
     */
    private int[] applyLockChanges(int net) {
        int[] changes = new int[Clause.values().length];
        boolean waive = net > 0;
        int count = Math.abs(net);
        for (int move = 0; move < count; move++) {
            List<Clause> eligible = new ArrayList<>();
            for (Clause clause : Clause.values()) {
                // A permanently-locked term (a pirate raid's dictated terms) can never be waived or toggled.
                if (!isPermanentlyLocked(clause) && (nonNegotiableTerms.isLocked(clause.term) == waive)) {
                    eligible.add(clause);
                }
            }
            if (eligible.isEmpty()) {
                break;
            }
            Clause chosen = eligible.get(Compute.randomInt(eligible.size()));
            nonNegotiableTerms = waive
                                       ? nonNegotiableTerms.withUnlocked(chosen.term)
                                       : nonNegotiableTerms.withLocked(chosen.term);
            changes[chosen.ordinal()] += waive ? 1 : -1;
        }
        return changes;
    }

    /**
     * Applies {@code net} meaningful steps to the term baseline: each step improves (net &gt; 0) or lowers (net &lt; 0)
     * a randomly chosen eligible term, with repeats. Non-negotiable terms, and terms already at the end of the table in
     * that direction, are skipped; when none remain eligible the remaining steps are dropped. Sets both the baseline
     * and the current step to the outcome, and returns the signed count of meaningful steps applied to each term
     * (indexed by clause ordinal).
     */
    private int[] applyActiveNegotiation(int net) {
        int[] moveCounts = new int[Clause.values().length];
        boolean improve = net > 0;
        int moves = Math.abs(net);
        for (int move = 0; move < moves; move++) {
            List<Clause> eligible = new ArrayList<>();
            for (Clause clause : Clause.values()) {
                if (!isLocked(clause) && meaningfulStep(clause, improve) >= CHAOS_CONTRACT_MINIMUM_STEP_VALUE) {
                    eligible.add(clause);
                }
            }
            if (eligible.isEmpty()) {
                break;
            }
            Clause chosen = eligible.get(Compute.randomInt(eligible.size()));
            originalStep[chosen.ordinal()] = meaningfulStep(chosen, improve);
            moveCounts[chosen.ordinal()] += improve ? 1 : -1;
        }
        System.arraycopy(originalStep, 0, currentStep, 0, originalStep.length);
        return moveCounts;
    }

    /** The step a term's baseline moves to on one meaningful improve/lower, or -1 if there is none that way. */
    private int meaningfulStep(Clause clause, boolean improve) {
        int from = originalStep[clause.ordinal()];
        return improve
                     ? NegotiationStepMath.nextHigherDifferentStep(clause.term, from)
                     : NegotiationStepMath.nextLowerDifferentStep(clause.term, from);
    }

    /**
     * Whether any term differs from the employer's offer. Re-negotiation is only open while none do, so an attempt
     * never has to settle what becomes of steps the player has already raised or given up.
     */
    private boolean termsChanged() {
        for (Clause clause : Clause.values()) {
            if (currentStep[clause.ordinal()] != originalStep[clause.ordinal()]) {
                return true;
            }
        }
        return false;
    }

    /** How many re-negotiation attempts have already been spent on this contract. */
    private int attemptsSoFar() {
        ActiveNegotiationData data = contract.getActiveNegotiationData();
        return data == null ? 0 : data.attempts();
    }

    /**
     * How many re-negotiation attempts this contract allows: one, plus one more when the chosen negotiator carries the
     * Relentless Bargainer SPA.
     */
    private int maxRenegotiationAttempts() {
        Person negotiator = contract.getPlayerNegotiator();
        return NegotiatorProfile.of(negotiator, scale).attempts();
    }

    /** Arms the re-negotiation buttons only when a negotiator is set and an attempt remains, saying why otherwise. */
    private void refreshRenegotiation() {
        if (haggleButton == null) {
            return;
        }
        boolean hasNegotiator = contract.getPlayerNegotiator() != null;
        int left = max(0, maxRenegotiationAttempts() - attemptsSoFar());
        boolean termsChanged = termsChanged();
        boolean available = hasNegotiator && left > 0 && !termsChanged;
        String tooltipKey;
        if (!hasNegotiator) {
            tooltipKey = "negotiate.contractMarket.renegotiate.tooltip.noNegotiator";
        } else if (left == 0) {
            tooltipKey = "negotiate.contractMarket.renegotiate.tooltip.spent";
        } else if (termsChanged) {
            tooltipKey = "negotiate.contractMarket.renegotiate.tooltip.termsChanged";
        } else {
            tooltipKey = "negotiate.contractMarket.renegotiate.tooltip";
        }
        // The note only matters while an attempt could otherwise be made.
        termsChangedNote.setVisible(hasNegotiator && left > 0 && termsChanged);
        for (HudConfirmButton button : List.of(haggleButton, exceptionButton)) {
            button.reset();
            button.setArmed(available);
            button.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE, tooltipKey)));
        }
        attemptsLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.renegotiate.left", left)
                                    .toUpperCase(Locale.ROOT));
        renderResults();
    }

    /** Shows the last re-negotiation's outcome as a verdict banner, or nothing until an attempt has been made. */
    private void renderResults() {
        ActiveNegotiationData data = contract.getActiveNegotiationData();
        if (data == null) {
            resultBanner.setVisible(false);
            return;
        }

        int net = data.netMargin();
        String headlineKey = net > 0 ? "negotiate.contractMarket.results.won"
                                   : net < 0 ? "negotiate.contractMarket.results.lost"
                                           : "negotiate.contractMarket.results.stalemate";
        Color accent = net > 0 ? READY : net < 0 ? DANGER : TEXT_FAINT;
        List<String> lines = new ArrayList<>();
        for (Clause clause : Clause.values()) {
            int change = data.deltaFor(clause.term);
            if (change == 0) {
                continue;
            }
            String termLabel = getTextAt(RESOURCE_BUNDLE, clause.labelKey);
            String line = switch (data.kind()) {
                case HAGGLE -> getFormattedTextAt(RESOURCE_BUNDLE,
                      change > 0 ? "negotiate.contractMarket.results.termImproved"
                            : "negotiate.contractMarket.results.termWorsened",
                      termLabel, Math.abs(change));
                case EXCEPTION -> getFormattedTextAt(RESOURCE_BUNDLE,
                      change > 0 ? "negotiate.contractMarket.results.termUnlocked"
                            : "negotiate.contractMarket.results.termLocked",
                      termLabel);
            };
            lines.add("<span style='color:" + hex(change > 0 ? READY : DANGER) + "'>" + MarketHud.escape(line)
                            + "</span>");
        }
        if (lines.isEmpty() && net != 0) {
            // A win or loss that changed nothing (no eligible term left in that direction).
            lines.add(MarketHud.escape(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.results.noTerms")));
        }
        resultBanner.setVerdict(getFormattedTextAt(RESOURCE_BUNDLE, headlineKey, Math.abs(net)),
              "<html>" + String.join("<br>", lines) + "</html>",
              getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.results.badge", net), accent);
        resultBanner.setVisible(true);
    }

    // endregion Active negotiation

    // region Rendering

    private void refresh() {
        Runnable restoreMain = holdScrollPosition(mainScroll);
        Runnable restoreSide = holdScrollPosition(sideScroll);
        refreshContent();
        // Rebuilt rows change height for a moment; put each column back where the player had it once laid out.
        SwingUtilities.invokeLater(() -> {
            restoreMain.run();
            restoreSide.run();
        });
    }

    /**
     * Notes where a column is scrolled to, returning how to put it back: at the bottom if it was at the bottom, so
     * rows changed there stay in view, and otherwise at the same offset.
     */
    private static Runnable holdScrollPosition(@Nullable JScrollPane scroll) {
        if (scroll == null) {
            return () -> {};
        }
        JScrollBar bar = scroll.getVerticalScrollBar();
        int value = bar.getValue();
        boolean atBottom = value > 0 && value + bar.getVisibleAmount() >= bar.getMaximum() - scaleForGUI(2);
        return () -> {
            scroll.getViewport().validate();
            bar.setValue(atBottom ? bar.getMaximum() - bar.getVisibleAmount() : value);
        };
    }

    private void refreshContent() {
        reputationReadout.set(reputationUsed, reputationPool, getFormattedTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.budget.left", reputationPool - reputationUsed));
        swapsReadout.set(swapsUsed, MAXIMUM_SWAPS,
              getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.budget.left", MAXIMUM_SWAPS - swapsUsed));
        bankValue.setText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.bank.value", sacrificeBank));

        for (TermRow row : termRows) {
            row.refresh();
        }

        int changedFacilities = 0;
        if (!isPirateContract()) {
            for (int i = 0; i < facilityQuantity.length; i++) {
                Money lineTotal = Money.of((double) facilityQuantity[i] * facilityUnitCost[i]);
                facilityQuantityLabels[i].setText(Integer.toString(facilityQuantity[i]));
                facilityTotalLabels[i].setText(lineTotal.toAmountAndSymbolString());
                facilityLowerButtons[i].setArmed(facilityQuantity[i] > 0);
                if (facilityQuantity[i] != initialFacilityQuantity[i]) {
                    changedFacilities++;
                }
            }
        }

        int changedTerms = 0;
        for (Clause clause : Clause.values()) {
            if (currentStep[clause.ordinal()] != originalStep[clause.ordinal()]) {
                changedTerms++;
            }
        }
        int changes = changedTerms + changedFacilities;
        resetButton.setArmed(changes > 0);
        String confirmText = changes == 0
                                   ? getTextAt(RESOURCE_BUNDLE, "button.contractMarket.negotiate.confirm")
                                   : getFormattedTextAt(RESOURCE_BUNDLE,
                                         "button.contractMarket.negotiate.confirmChanges", changes);
        confirmButton.setText(confirmText.toUpperCase(Locale.ROOT));

        limitsLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.summary",
              NegotiationStepMath.totalStepsSacrificed(originalStep, currentStep), maxSacrificedSteps,
              NegotiationStepMath.distinctTermsSacrificed(originalStep, currentStep), maxSacrificedTerms, capPerTerm));

        refreshNegotiatorCard();
        refreshEmployerCard();
        refreshRenegotiation();
        refreshImpact();
    }

    /**
     * The impact on pay: monthly pay at the proposed base-pay step against the employer's offer, the total payout,
     * the facility rental, and the overall change against the offer. Support, transport and salvage pay out during the
     * contract, so they are not counted here.
     */
    private void refreshImpact() {
        double originalMultiplier = ChaosContractStepsTable.fromStepValue(
              originalStep[Clause.PAY.ordinal()]).getBasePayMultiplier();
        double proposedMultiplier = ChaosContractStepsTable.fromStepValue(
              currentStep[Clause.PAY.ordinal()]).getBasePayMultiplier();
        Money baseMonthly = contract.getMonthlyPayOut();
        Money projectedMonthly = originalMultiplier == 0 ? baseMonthly
                                       : baseMonthly.multipliedBy(proposedMultiplier / originalMultiplier);
        Money payChange = projectedMonthly.minus(baseMonthly).multipliedBy(contract.getLengthInMonths());
        Money totalPay = contract.getTotalPay().plus(payChange);

        Money rental = Money.zero();
        Money initialRental = Money.zero();
        if (!isPirateContract()) {
            for (int i = 0; i < facilityQuantity.length; i++) {
                rental = rental.plus(Money.of((double) facilityQuantity[i] * facilityUnitCost[i]));
                initialRental = initialRental.plus(Money.of((double) initialFacilityQuantity[i] * facilityUnitCost[i]));
            }
        }
        Money change = payChange.minus(rental.minus(initialRental));

        JPanel impact = MarketHud.section(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.section.impact.money"));
        String monthly = baseMonthly.toAmountString();
        if (proposedMultiplier != originalMultiplier) {
            monthly = "<html>" + MarketHud.escape(baseMonthly.toAmountString()) + " &rarr; <b style='color:"
                            + hex(proposedMultiplier > originalMultiplier ? READY : DANGER) + "'>"
                            + MarketHud.escape(projectedMonthly.toAmountString()) + "</b></html>";
        }
        MarketHud.addRow(impact, getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.pay"), monthly, null, null,
              false);
        MarketHud.addRow(impact, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.total"),
              totalPay.toAmountString(), null, totalPay.toAmountAndSymbolString(), false);
        if (!isPirateContract()) {
            MarketHud.addRow(impact, getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.impact.rental"),
                  ContractMarketFormatting.costAmount(rental),
                  rental.isZero() ? null : DANGER, null, false);
        }
        MarketHud.addRow(impact, getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.impact.change"),
              ContractMarketFormatting.signedAmount(change),
              change.isNegative() ? DANGER : READY, null, true);
        MarketHud.addSpanning(impact, MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE,
              "negotiate.contractMarket.impact.note"), TEXT_FAINT, Font.PLAIN, 0.78f), 6);

        impactHolder.removeAll();
        impactHolder.add(card(impact), BorderLayout.CENTER);
        impactHolder.revalidate();
        impactHolder.repaint();
    }

    /** A bordered deep-surface card around a side-column block. */
    private static JPanel card(JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(SURFACE_DEEP);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(10), scaleForGUI(10), scaleForGUI(10))));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static HudButton stepButton(String labelKey, Runnable action) {
        HudButton button = new HudButton(getTextAt(RESOURCE_BUNDLE, labelKey), false, true);
        button.setPreferredSize(new Dimension(scaleForGUI(30), button.getPreferredSize().height));
        button.setMaximumSize(button.getPreferredSize());
        button.addActionListener(event -> action.run());
        return button;
    }

    /**
     * The step a below-baseline (sacrificed) term rises to on a single raise: the sacrifice boundary directly above
     * where it now sits, i.e. the exact step the most recent lower started from. One raise mirrors one lower, and the
     * term never rests on a mid-plateau step that shares the baseline's value.
     */
    private int restoreTarget(Clause clause) {
        int index = clause.ordinal();
        return NegotiationStepMath.restoreStep(clause.term, originalStep[index], currentStep[index]);
    }

    // endregion Rendering

    // region Term rows

    /** How one cell of a term's track is drawn. */
    private enum CellStyle {
        /** A step beyond the term, not part of the deal. */
        EMPTY,
        /** A step within the employer's offer. */
        OFFER,
        /** A raised step paid for with reputation. */
        REPUTATION,
        /** A raised step paid for with a swap of banked steps. */
        SWAP,
        /** A step of the offer the player has given up. */
        GIVEN_UP;

        private void paint(Graphics2D canvas, int x, int y, int width, int height) {
            switch (this) {
                case EMPTY -> {
                    canvas.setColor(SURFACE_HIGHLIGHT);
                    canvas.fillRect(x, y, width, height);
                }
                case OFFER -> {
                    canvas.setColor(translucent(ACCENT, 56));
                    canvas.fillRect(x, y, width, height);
                }
                case REPUTATION -> {
                    canvas.setColor(ACCENT);
                    canvas.fillRect(x, y, width, height);
                }
                case SWAP -> {
                    canvas.setColor(AMBER);
                    canvas.fillRect(x, y, width, height);
                }
                case GIVEN_UP -> {
                    canvas.setColor(translucent(DANGER, 64));
                    canvas.fillRect(x, y, width, height);
                    // Hatching marks the given-up steps apart from colour alone.
                    Graphics2D hatch = (Graphics2D) canvas.create(x, y, width, height);
                    try {
                        hatch.setColor(translucent(DANGER, 190));
                        hatch.setStroke(new BasicStroke(scaleForGUI(2)));
                        int spacing = scaleForGUI(5);
                        for (int offset = -height; offset < width; offset += spacing) {
                            hatch.drawLine(offset, height, offset + height, 0);
                        }
                    } finally {
                        hatch.dispose();
                    }
                }
            }
        }
    }

    /**
     * One term: its name (and the employer's leaning on it), its track, its value from the offer to the proposal with
     * how it was paid for, the lower and raise buttons (or a lock badge), and beneath it why the term cannot be raised
     * when it cannot.
     */
    private final class TermRow {
        private final Clause clause;
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final Track track;
        private final JLabel value = new MarketHud.WrappingLabel();
        private final JLabel why = new MarketHud.WrappingLabel();
        private final HudButton lowerButton;
        private final HudButton raiseButton;
        private final JLabel lockBadge = new JLabel();
        private final JPanel controls = Hud.transparentPanel(null);

        private TermRow(Clause clause) {
            this.clause = clause;
            this.track = new Track(clause);
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(9), scaleForGUI(5), 0)));

            JPanel label = MarketHud.column();
            JLabel name = new JLabel(getTextAt(RESOURCE_BUNDLE, clause.labelKey));
            name.setForeground(TEXT_MUTED);
            name.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
            label.add(leftAligned(name));
            String marker = factionModifierMarker(clause);
            if (marker != null) {
                JLabel markerLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                      "negotiate.contractMarket.factionModifier.employer", marker));
                markerLabel.setForeground(TEXT_FAINT);
                markerLabel.setFont(hudFont(Font.ITALIC, 0.82f, 0.0f));
                label.add(leftAligned(markerLabel));
            }
            label.setPreferredSize(new Dimension(scaleForGUI(LABEL_WIDTH), label.getPreferredSize().height));
            // When the window narrows, the track gives way; the name and value keep their width.
            label.setMinimumSize(label.getPreferredSize());

            value.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            value.setForeground(TEXT);

            lowerButton = stepButton("negotiate.contractMarket.stepper.decrease", () -> lower(clause));
            raiseButton = stepButton("negotiate.contractMarket.stepper.increase", () -> raise(clause));
            lockBadge.setFont(hudFont(Font.BOLD, 0.66f, 0.12f));
            lockBadge.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(7), scaleForGUI(4),
                  scaleForGUI(7)));
            controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));

            why.setForeground(TEXT_FAINT);
            why.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridx = 0;
            panel.add(label, constraints);
            constraints.gridx = 1;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(0, scaleForGUI(12), 0, scaleForGUI(12));
            panel.add(track, constraints);
            constraints.gridx = 2;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(0, 0, 0, 0);
            constraints.anchor = GridBagConstraints.EAST;
            panel.add(controls, constraints);
            // The term's effect and, when it will not move, the reason sit under the track.
            constraints.gridx = 1;
            constraints.gridwidth = 2;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridy = 1;
            constraints.insets = new Insets(scaleForGUI(4), scaleForGUI(12), 0, 0);
            panel.add(value, constraints);
            constraints.gridy = 2;
            constraints.insets = new Insets(scaleForGUI(2), scaleForGUI(12), 0, 0);
            panel.add(why, constraints);
        }

        private void refresh() {
            int index = clause.ordinal();
            String originalValue = valueOf(clause, ChaosContractStepsTable.fromStepValue(originalStep[index]));
            String proposedValue = valueOf(clause, ChaosContractStepsTable.fromStepValue(currentStep[index]));
            int delta = currentStep[index] - originalStep[index];

            StringBuilder html = new StringBuilder("<html><b>").append(MarketHud.escape(originalValue));
            if (delta != 0) {
                boolean valueChanged = !originalValue.equals(proposedValue);
                Color color = !valueChanged ? TEXT_MUTED : (delta > 0 ? READY : DANGER);
                html.append(" &rarr; <span style='color:").append(hex(color)).append("'>")
                      .append(MarketHud.escape(proposedValue)).append("</span>");
            }
            html.append("</b>");
            List<String> chips = new ArrayList<>();
            long reputation = funding.get(index).stream().filter(paid -> paid == TermFunding.REPUTATION).count();
            long swaps = funding.get(index).size() - reputation;
            if (reputation > 0) {
                chips.add(chip(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.fund.rep", reputation),
                      ACCENT));
            }
            if (swaps > 0) {
                chips.add(chip(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.fund.swap", swaps),
                      AMBER));
            }
            if (delta < 0) {
                chips.add(chip(getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.fund.banked", -delta),
                      DANGER));
            }
            if (delta != 0 && originalValue.equals(proposedValue)) {
                chips.add(chip(getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.plateau"), TEXT_FAINT));
            }
            if (!chips.isEmpty()) {
                html.append("&nbsp;&nbsp;&nbsp;<span>")
                      .append(String.join("&nbsp;&nbsp;", chips))
                      .append("</span>");
            }
            value.setText(html.append("</html>").toString());
            if (delta != 0 && originalValue.equals(proposedValue)) {
                value.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
                      "negotiate.contractMarket.plateau.tooltip")));
            } else {
                value.setToolTipText(null);
            }

            controls.removeAll();
            if (isLocked(clause)) {
                boolean fixed = isPermanentlyLocked(clause);
                lockBadge.setText(getTextAt(RESOURCE_BUNDLE, fixed
                                                                   ? "negotiate.contractMarket.locked.permanent"
                                                                   : "negotiate.contractMarket.locked")
                                        .toUpperCase(Locale.ROOT));
                Color color = fixed ? TEXT_FAINT : AMBER;
                lockBadge.setForeground(color);
                lockBadge.setBorder(BorderFactory.createCompoundBorder(
                      BorderFactory.createLineBorder(fixed ? BORDER : AMBER, scaleForGUI(1)),
                      BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(7), scaleForGUI(4),
                            scaleForGUI(7))));
                lockBadge.setToolTipText(fixed ? MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
                      "negotiate.contractMarket.locked.permanent.tooltip")) : null);
                controls.add(lockBadge);
            } else {
                String lowerReason = lowerBlockReason(clause);
                String raiseReason = raiseBlockReason(clause);
                lowerButton.setArmed(lowerReason == null);
                lowerButton.setToolTipText(lowerReason == null
                                                 ? getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.lower.tooltip")
                                                 : lowerReason);
                raiseButton.setArmed(raiseReason == null);
                raiseButton.setToolTipText(raiseReason == null
                                                 ? getTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.raise.tooltip")
                                                 : raiseReason);
                controls.add(lowerButton);
                controls.add(Box.createHorizontalStrut(scaleForGUI(4)));
                controls.add(raiseButton);
            }
            String reason = raiseBlockReason(clause);
            why.setText(reason == null ? " " : "<html>" + MarketHud.escape(reason) + "</html>");
            track.repaint();
            controls.revalidate();
            controls.repaint();
        }

        private String chip(String text, Color color) {
            return "<b style='color:" + hex(color) + "'>" + MarketHud.escape(text.toUpperCase(Locale.ROOT)) + "</b>";
        }
    }

    /**
     * A term's seventeen steps: the employer's offer shaded, the steps raised coloured by how they were paid for, the
     * steps given up hatched, a rule beneath the step the employer started from, and an outline on the furthest step
     * the raise cap allows. Hovering a step shows its value.
     */
    private final class Track extends JComponent {
        private final Clause clause;

        private Track(Clause clause) {
            this.clause = clause;
            setToolTipText("");
            setPreferredSize(new Dimension(scaleForGUI(17 * 12), scaleForGUI(24)));
            setMinimumSize(new Dimension(scaleForGUI(17 * 6), scaleForGUI(24)));
        }

        private int stepAt(int x) {
            return Math.clamp((long) Math.floor((double) x * CHAOS_CONTRACT_MAXIMUM_STEP_VALUE / max(1, getWidth())),
                  0, CHAOS_CONTRACT_MAXIMUM_STEP_VALUE - 1) + 1;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int step = stepAt(event.getX());
            return getFormattedTextAt(RESOURCE_BUNDLE, "negotiate.contractMarket.track.tooltip", step,
                  valueOf(clause, ChaosContractStepsTable.fromStepValue(step)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                int index = clause.ordinal();
                int origin = originalStep[index];
                int current = currentStep[index];
                List<TermFunding> paid = funding.get(index);
                boolean locked = isLocked(clause);
                int steps = CHAOS_CONTRACT_MAXIMUM_STEP_VALUE;
                int gap = scaleForGUI(2);
                int cellHeight = scaleForGUI(18);
                double cell = (getWidth() - gap * (steps - 1)) / (double) steps;
                if (locked) {
                    canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                }
                for (int step = 1; step <= steps; step++) {
                    int x = (int) Math.round((step - 1) * (cell + gap));
                    int width = (int) Math.round(step * (cell + gap) - gap) - x;
                    CellStyle style;
                    if (step <= Math.min(origin, current)) {
                        style = CellStyle.OFFER;
                    } else if (step > origin && step <= current) {
                        int paidIndex = step - origin - 1;
                        style = (paidIndex < paid.size() && paid.get(paidIndex) == TermFunding.SACRIFICE)
                                      ? CellStyle.SWAP : CellStyle.REPUTATION;
                    } else if (step > current && step <= origin) {
                        style = CellStyle.GIVEN_UP;
                    } else {
                        style = CellStyle.EMPTY;
                    }
                    style.paint(canvas, x, 0, width, cellHeight);
                    if (step == origin) {
                        canvas.setColor(TEXT);
                        canvas.fillRect(x, cellHeight + scaleForGUI(3), width, scaleForGUI(2));
                    }
                    if (!locked && step == Math.min(steps, origin + capPerTerm)) {
                        canvas.setColor(BORDER_CYAN);
                        canvas.drawRect(x, 0, width - 1, cellHeight - 1);
                    }
                }
            } finally {
                canvas.dispose();
            }
        }
    }

    // endregion Term rows

    /** A budget readout for the command bar: a key over a row of pips and how many are left. */
    private static final class Readout {
        private final Color color;
        private final Pips pips = new Pips();
        private final JLabel text = new JLabel();

        private Readout(Color color) {
            this.color = color;
        }

        private JPanel build(String key, String toolTip) {
            JPanel readout = new JPanel();
            readout.setLayout(new BoxLayout(readout, BoxLayout.Y_AXIS));
            readout.setOpaque(true);
            readout.setBackground(SURFACE_DEEP);
            readout.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(10), scaleForGUI(5),
                  scaleForGUI(10)));
            readout.setToolTipText(MarketHud.wrapToolTip(toolTip));
            JLabel keyLabel = HudStyle.keyLabel(key.toUpperCase(Locale.ROOT));
            keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel value = Hud.transparentPanel(null);
            value.setLayout(new BoxLayout(value, BoxLayout.X_AXIS));
            value.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.setForeground(TEXT);
            text.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
            value.add(pips);
            value.add(Box.createHorizontalStrut(scaleForGUI(6)));
            value.add(text);
            readout.add(keyLabel);
            readout.add(Box.createVerticalStrut(scaleForGUI(2)));
            readout.add(value);
            return readout;
        }

        private void set(int used, int total, String left) {
            pips.used = used;
            pips.total = total;
            pips.color = color;
            pips.revalidate();
            pips.repaint();
            text.setText(left);
        }

        /** A row of small squares, the spent ones filled. */
        private static final class Pips extends JComponent {
            private int used;
            private int total;
            private Color color = ACCENT;

            @Override
            public Dimension getPreferredSize() {
                int size = scaleForGUI(9);
                int gap = scaleForGUI(3);
                return new Dimension(max(1, total) * (size + gap), size);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                int size = scaleForGUI(9);
                int gap = scaleForGUI(3);
                int top = (getHeight() - size) / 2;
                for (int index = 0; index < total; index++) {
                    int x = index * (size + gap);
                    graphics.setColor(index < used ? color : SURFACE_HIGHLIGHT);
                    graphics.fillRect(x, top, size, size);
                    graphics.setColor(index < used ? color : BORDER);
                    graphics.drawRect(x, top, size - 1, size - 1);
                }
            }
        }
    }

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
}
