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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.digitalGM.stratCon.StratConCombatBonusProjection;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic;
import mekhq.campaign.mission.contract.contractData.ObfuscatableIntel;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.gui.utilities.WrapLayout;
import mekhq.gui.view.PlanetViewPanel;
import org.apache.commons.lang3.StringUtils;

/**
 * The briefing dossier for the selected {@link AbstractContract}, drawn in the HUD style of the Oracle Console.
 *
 * <p>The header names the offer and its employer, rates the fight with a compact assessment and holds the offer's
 * actions; Accept lives in the market's footer instead, so it can act on the selection from either page. Beneath it,
 * one scrolling page runs from the briefing (the contact's pitch and the contract's description, beside a preview of
 * the route) through the terms, intel and money side by side, to the target planet. One page rather than tabs leaves
 * no empty space under a short section.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractDossierPanel extends JPanel {
    private static final String RESOURCE_BUNDLE = ChaosContractMarketDialog.RESOURCE_BUNDLE;

    private static final int PORTRAIT_SIZE = 48;

    private final transient Campaign campaign;
    private final transient AbstractContract contract;
    private final transient ContractProfitEstimate estimate;
    private final LocalDate currentDate;
    private final boolean hideContractType;

    /**
     * Builds a dossier for the given offer.
     *
     * @param campaign         the active campaign
     * @param contract         the selected offer
     * @param currentDate      campaign date, for time-sensitive names
     * @param estimate         the offer's profit estimate
     * @param hideContractType {@code true} if the campaign hides contract types behind operation codenames
     * @param actions          the per-offer actions the header buttons invoke
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ContractDossierPanel(Campaign campaign, AbstractContract contract, LocalDate currentDate,
          ContractProfitEstimate estimate, boolean hideContractType, ContractMarketActions actions) {
        super(new BorderLayout());
        this.campaign = campaign;
        this.contract = contract;
        this.estimate = estimate;
        this.currentDate = currentDate;
        this.hideContractType = hideContractType;
        setOpaque(true);
        setBackground(SURFACE_DEEP);

        // The header stays put while the page beneath it scrolls.
        JPanel top = Hud.transparentPanel(new BorderLayout());
        top.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));
        top.add(buildHeader(actions), BorderLayout.CENTER);

        JPanel page = MarketHud.column();
        page.add(leftAligned(buildOverview()));
        page.add(Box.createVerticalStrut(scaleForGUI(18)));
        page.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "tab.contractMarket.targetPlanet"))));
        page.add(Box.createVerticalStrut(scaleForGUI(6)));
        page.add(leftAligned(buildTargetPlanet()));

        add(top, BorderLayout.NORTH);
        add(MarketHud.scroll(padded(page), SURFACE_DEEP), BorderLayout.CENTER);
    }

    // region Header

    private JPanel buildHeader(ContractMarketActions actions) {
        JPanel header = Hud.transparentPanel(new BorderLayout(scaleForGUI(16), 0));
        header.add(buildIdentity(), BorderLayout.CENTER);

        JPanel side = Hud.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.EAST;

        JPanel markers = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(6), 0));
        for (MarketBadge badge : ContractMarketFormatting.badges(contract)) {
            markers.add(MarketHud.badge(badge.text(), badge.color(), badge.toolTip()));
        }
        markers.add(new AssessmentChip());
        constraints.gridy = 0;
        side.add(markers, constraints);

        HudButton edit = new HudButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.edit")
                                             .toUpperCase(Locale.ROOT), false, true);
        edit.setArmed(campaign.isGM());
        edit.setToolTipText(MarketOffersPage.gmToolTip(campaign.isGM(), "button.contractMarket.edit.tooltip"));
        edit.addActionListener(event -> actions.edit(contract));
        HudConfirmButton delete = new HudConfirmButton(getTextAt(RESOURCE_BUNDLE, "button.contractMarket.delete")
                                                             .toUpperCase(Locale.ROOT),
              getTextAt(RESOURCE_BUNDLE, "button.contractMarket.delete.confirm").toUpperCase(Locale.ROOT), true);
        delete.addActionListener(event -> actions.delete(contract));

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(edit);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(5)));
        buttons.add(delete);
        constraints.gridy = 1;
        constraints.insets = new Insets(scaleForGUI(8), 0, 0, 0);
        side.add(buttons, constraints);

        header.add(side, BorderLayout.EAST);
        return header;
    }

    private JPanel buildIdentity() {
        JPanel identity = MarketHud.column();

        JLabel eyebrow = Hud.eyebrow(joined(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.eyebrow"),
              contract.getEmployerMarketDisplayName()));
        eyebrow.setIcon(new MarketHud.SwatchIcon(contract.getEmployerColor().getColour()));
        eyebrow.setIconTextGap(scaleForGUI(6));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));

        JLabel title = new JLabel(marketName(contract).toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.12f, 0.12f));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(2)));

        List<String> status = new ArrayList<>();
        if (!hideContractType) {
            status.add(objective(contract, false));
        }
        status.add(destination(contract, currentDate));
        status.add(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.status.term",
              contract.getLengthInMonths()));
        status.add(estimate.hasRoute()
                         ? getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.status.transit",
                               estimate.transitDays())
                         : getTextAt(RESOURCE_BUNDLE, "value.contractMarket.noRoute"));
        JLabel statusLabel = new JLabel(joined(status));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        identity.add(leftAligned(statusLabel));
        return identity;
    }

    /**
     * The compact assessment: the difficulty's word and score over a row of ten pips, on a surface tinted with the
     * severity colour and a stripe down its left, as the HUD's verdict banner marks a result.
     */
    private final class AssessmentChip extends JPanel {
        private final Color severity;

        private AssessmentChip() {
            super(new GridBagLayout());
            int difficulty = assessmentDifficulty(contract);
            severity = severityColor(difficulty);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(11), scaleForGUI(5),
                  scaleForGUI(9)));

            JLabel verdict = new JLabel(assessmentLabel(contract).toUpperCase(Locale.ROOT));
            verdict.setForeground(severity);
            verdict.setFont(hudFont(Font.BOLD, 0.82f, 0.12f));
            JLabel score = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.assessment.score",
                  difficulty));
            score.setForeground(severity);
            score.setFont(hudFont(Font.BOLD, 0.72f, 0.1f));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.WEST;
            add(verdict, constraints);
            constraints.gridx = 1;
            constraints.weightx = 1.0;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = new Insets(0, scaleForGUI(10), 0, 0);
            add(score, constraints);
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 2;
            constraints.weightx = 0;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(scaleForGUI(3), 0, 0, 0);
            add(new MarketHud.PipMeter(difficulty, severity), constraints);

            String threat = intelValue(ObfuscatableIntel.THREAT, threat(contract));
            setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.assessment.tooltip", threat));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                canvas.setColor(SURFACE);
                canvas.fillRect(0, 0, width, height);
                canvas.setPaint(new GradientPaint(0, 0, translucent(severity, 34), width * 0.8f, 0, SURFACE));
                canvas.fillRect(0, 0, width, height);
                canvas.setColor(BORDER);
                canvas.drawRect(0, 0, width - 1, height - 1);
                canvas.setColor(severity);
                canvas.fillRect(0, 0, scaleForGUI(3), height);
            } finally {
                canvas.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    // endregion Header

    // region Overview

    /**
     * The briefing first, then terms, intel and money in three columns, so the numbers that decide an offer sit
     * together.
     */
    private JPanel buildOverview() {
        JPanel columns = Hud.transparentPanel(new GridLayout(1, 3, scaleForGUI(16), 0));
        columns.add(MarketHud.topAligned(buildTerms()));
        columns.add(MarketHud.topAligned(buildIntel()));
        columns.add(MarketHud.topAligned(buildMoney()));

        // The route preview sits beside the briefing, in the space the pitch leaves, rather than adding height.
        JPanel top = Hud.transparentPanel(new BorderLayout(scaleForGUI(18), 0));
        top.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(0, 0, scaleForGUI(12), 0)));
        top.add(buildBriefing(), BorderLayout.CENTER);
        top.add(MarketHud.topAligned(buildRoute()), BorderLayout.EAST);

        JPanel overview = MarketHud.column();
        overview.add(leftAligned(top));
        overview.add(Box.createVerticalStrut(scaleForGUI(14)));
        overview.add(leftAligned(columns));
        return overview;
    }

    /**
     * The route: a snapshot of the jump path in the interstellar map's style, a line summing up the trip, and a link
     * that closes the market and shows the target on the full map.
     */
    private JPanel buildRoute() {
        JumpPath path = ContractUtilities.getJumpPath(campaign, contract,
              campaign.getPlayerForce().getForceDetachment().getCurrentLocation());
        ContractRoutePreview preview = new ContractRoutePreview(path, currentDate,
              getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.route.none"));

        JPanel heading = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        heading.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.route")), BorderLayout.WEST);
        PlanetarySystem target = contract.getTargetSystem();
        if (target != null) {
            JLabel openMap = Hud.link(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.route.openMap"),
                  () -> showOnMap(target));
            openMap.setToolTipText(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.route.openMap.tooltip"));
            heading.add(openMap, BorderLayout.EAST);
        }

        JPanel route = MarketHud.column();
        route.add(leftAligned(heading));
        route.add(Box.createVerticalStrut(scaleForGUI(5)));
        route.add(leftAligned(preview));
        if (preview.hasRoute()) {
            PlanetarySystem origin = path.getFirstSystem();
            String from = origin == null ? "" : origin.getName(currentDate);
            route.add(Box.createVerticalStrut(scaleForGUI(4)));
            route.add(leftAligned(Hud.hint(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.route.summary",
                  from, path.getJumps(), estimate.transitDays()))));
        }
        Dimension size = preview.getPreferredSize();
        route.setPreferredSize(new Dimension(size.width, route.getPreferredSize().height));
        return route;
    }

    /**
     * Closes the market and shows the target system on the interstellar map. The market is modal, so the map behind it
     * could not be used while it stayed open.
     */
    private void showOnMap(PlanetarySystem target) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        campaign.getGUI().focusOnSystem(target);
    }

    private JPanel buildTerms() {
        JPanel terms = MarketHud.section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.terms"));
        addRow(terms, "dossier.contractMarket.terms.command", contract.getCommandRights().toString());
        addRow(terms, "dossier.contractMarket.terms.salvage", salvage(contract));
        addRow(terms, "dossier.contractMarket.terms.support", support(contract));
        addRow(terms, "dossier.contractMarket.terms.transport", transport(contract));
        addRow(terms, "dossier.contractMarket.terms.term",
              getFormattedTextAt(RESOURCE_BUNDLE, "card.contractMarket.months", contract.getLengthInMonths()));
        addRow(terms, "dossier.contractMarket.terms.transit", transit(estimate));

        // The contract's random characteristics ride along at the foot of the Terms column.
        if (!contract.getCharacteristics().isEmpty()) {
            JPanel flow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(5), scaleForGUI(5)));
            List<ContractCharacteristic> ordered = new ArrayList<>(contract.getCharacteristics());
            ordered.sort(Comparator.comparing(ContractCharacteristic::getName));
            for (ContractCharacteristic characteristic : ordered) {
                flow.add(MarketHud.badge(characteristic.getName(), polarityColor(characteristic),
                      characteristic.getToolTipText()));
            }
            MarketHud.addSpanning(terms, flow, 4);
        }
        return terms;
    }

    private JPanel buildIntel() {
        JPanel intel = MarketHud.section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.intel"));
        addRow(intel, "dossier.contractMarket.intel.alliedCommand",
              intelValue(ObfuscatableIntel.ALLIED_COMMAND, alliedCommand(contract)));
        addRow(intel, "dossier.contractMarket.intel.opposition",
              intelValue(ObfuscatableIntel.OPPOSITION,
                    contract.getEnemyDisplayNameIncludingFaction(currentDate.getYear())));
        addRow(intel, "dossier.contractMarket.intel.threat", intelValue(ObfuscatableIntel.THREAT, threat(contract)));
        addRow(intel, "dossier.contractMarket.intel.morale",
              intelValue(ObfuscatableIntel.MORALE, contract.getMoraleLevel().toString()));
        addRow(intel, "dossier.contractMarket.intel.intensity", String.valueOf(contract.getTrackCount()));

        // A muted, one-line advisory for a contract with a special nature. The natures are mutually exclusive, so at
        // most one shows. For a false flag it is an understated, ambiguously-worded paper-trail tell that (with the
        // deniability pay premium) an attentive player can learn to read; for a covert operation or a Proving Ground -
        // both already badged - it is a plain-language note explaining what the badge means.
        String advisoryKey = null;
        if (contract.isFalseFlag()) {
            advisoryKey = "dossier.contractMarket.intel.advisory.falseFlag";
        } else if (contract.isCovert()) {
            advisoryKey = "dossier.contractMarket.intel.advisory.covert";
        } else if (contract.isProvingGround()) {
            advisoryKey = "dossier.contractMarket.intel.advisory.provingGround";
        }
        if (advisoryKey != null) {
            MarketHud.addRow(intel, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.intel.advisory"),
                  getTextAt(RESOURCE_BUNDLE, advisoryKey), TEXT_FAINT, null, false);
        }
        return intel;
    }

    /**
     * The money column: total payout and net per month up top, then how the estimate is reached. Combat pay is kept
     * apart from the guaranteed figures, because it has to be won.
     */
    private JPanel buildMoney() {
        JPanel money = MarketHud.section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.money"));

        Money netPerMonth = estimate.getNetPerMonth();
        JPanel readouts = Hud.transparentPanel(new GridLayout(1, 2, scaleForGUI(6), 0));
        JPanel total = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.total"),
              compactMoney(contract.getTotalPay()), ACCENT_BRIGHT, GROUND);
        total.setToolTipText(contract.getTotalPay().toAmountAndSymbolString());
        readouts.add(total);
        JPanel perMonth = MarketHud.readout(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.netPerMonth"),
              signedCompactMoney(netPerMonth), netPerMonth.isNegative() ? DANGER : READY, GROUND);
        perMonth.setToolTipText(MarketHud.wrapToolTip(getTextAt(RESOURCE_BUNDLE,
              "dossier.contractMarket.metric.netPerMonth.tooltip")));
        readouts.add(perMonth);
        readouts.setBorder(BorderFactory.createEmptyBorder(0, 0, scaleForGUI(4), 0));
        MarketHud.addSpanning(money, readouts, 0);

        MarketHud.addRow(money, getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.money.monthly",
              contract.getLengthInMonths()), contract.getMonthlyPayOut().toAmountString(), null, null, false);
        MarketHud.addRow(money, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.transport"),
              contract.getTransportPayment().toAmountString(), null, null, false);

        String expensesBreakdown = buildExpensesBreakdown(estimate);
        MarketHud.addRow(money, getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.expenses",
                    contract.getLengthInMonths(), Math.max(0, estimate.transitDays())),
              costAmount(estimate.operatingCosts()), DANGER, expensesBreakdown, false);
        // The third segment is every running cost besides payroll, maintenance and food - not the operating costs as a
        // whole, which already include the other three.
        Money otherCosts = estimate.overhead()
                                 .plus(estimate.hotSpotsUpkeep())
                                 .plus(estimate.spareParts())
                                 .plus(estimate.trainingMunitions())
                                 .plus(estimate.fuel());
        MarketHud.SegmentBar costBar = new MarketHud.SegmentBar(
              List.of(amount(estimate.salaries()), amount(estimate.maintenance()), amount(otherCosts),
                    amount(estimate.foodAndHousing())),
              List.of(ACCENT, translucent(ACCENT, 150), translucent(ACCENT, 90), TEXT_FAINT));
        costBar.setToolTipText(expensesBreakdown);
        MarketHud.addSpanning(money, costBar, 3);

        Money net = estimate.estimatedNet();
        MarketHud.addRow(money, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.net"),
              signedAmount(net), net.isNegative() ? DANGER : READY, null, true);
        addCombatPayRows(money);

        JTextArea caveat = MarketHud.wrappedText(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.caveat"),
              TEXT_FAINT, Font.PLAIN, 0.78f);
        MarketHud.addSpanning(money, caveat, 6);
        return money;
    }

    /**
     * Lists each expense that makes up the operating costs. Expenses the campaign does not charge are left out, as on
     * the Finances tab.
     *
     * @return the breakdown as an HTML tooltip, or {@code null} when there are no expenses
     */
    private static @Nullable String buildExpensesBreakdown(ContractProfitEstimate estimate) {
        List<String> lines = new ArrayList<>();
        addExpenseLine(lines, "salaries", estimate.salaries());
        addExpenseLine(lines, "maintenance", estimate.maintenance());
        addExpenseLine(lines, "overhead", estimate.overhead());
        addExpenseLine(lines, "hotSpotsUpkeep", estimate.hotSpotsUpkeep());
        addExpenseLine(lines, "spareParts", estimate.spareParts());
        addExpenseLine(lines, "trainingMunitions", estimate.trainingMunitions());
        addExpenseLine(lines, "fuel", estimate.fuel());
        addExpenseLine(lines, "foodAndHousing", estimate.foodAndHousing());

        if (lines.isEmpty()) {
            return null;
        }
        return "<html>" + String.join("<br>", lines) + "</html>";
    }

    private static void addExpenseLine(List<String> lines, String expenseKey, Money amount) {
        if (amount.isZero()) {
            return;
        }
        lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.expenses." + expenseKey,
              amount.toAmountAndSymbolString()));
    }

    /**
     * Adds the combat pay the contract can bring if every chance at the bonus is won, and the net with it. Nothing is
     * added when the contract pays no combat bonus.
     */
    private void addCombatPayRows(JPanel money) {
        if (!estimate.potentialCombatPay().isPositive()) {
            return;
        }
        StratConCombatBonusProjection combatBonus = estimate.combatBonus();
        String bonusCount = decimalFormat("0.#").format(combatBonus.bonusCount());
        String combatPayTooltip = getFormattedTextAt(RESOURCE_BUNDLE,
              combatBonus.isFromNavPoint()
                    ? "dossier.contractMarket.profit.combatPay.navPoint.tooltip"
                    : "dossier.contractMarket.profit.combatPay.essential.tooltip",
              bonusCount,
              contract.getContractFinanceData().combatPay().toAmountAndSymbolString());
        MarketHud.addRow(money, getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.combatPay",
              bonusCount), signedAmount(estimate.potentialCombatPay()), READY, combatPayTooltip, false);

        Money netWithCombatPay = estimate.getNetWithCombatPay();
        MarketHud.addRow(money, getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.netWithCombatPay"),
              signedAmount(netWithCombatPay), netWithCombatPay.isNegative() ? DANGER : READY, null, true);
    }

    // endregion Overview

    // region Briefing

    /**
     * The briefing that opens the Overview: the contact's portrait and pitch, then the contract's own (Markdown)
     * description when it has one.
     */
    private JPanel buildBriefing() {
        Person negotiator = contract.getEmployerNegotiator();
        JPanel briefing = Hud.transparentPanel(new BorderLayout(scaleForGUI(14), 0));

        JPanel portraitHolder = Hud.transparentPanel(new BorderLayout());
        portraitHolder.add(MarketHud.portrait(negotiator, scaleForGUI(PORTRAIT_SIZE)), BorderLayout.NORTH);
        briefing.add(portraitHolder, BorderLayout.WEST);

        JPanel text = MarketHud.column();
        String name = negotiator == null ? contract.getEmployerMarketDisplayName() : negotiator.getFullTitle();
        JPanel nameLine = Hud.transparentPanel(null);
        nameLine.setLayout(new BoxLayout(nameLine, BoxLayout.X_AXIS));
        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT);
        nameLabel.setFont(hudFont(Font.BOLD, 1.02f, 0.0f));
        nameLine.add(nameLabel);
        if (negotiator != null) {
            JLabel role = new JLabel(getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator")
                                           + getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.contact.role"));
            role.setForeground(TEXT_MUTED);
            role.setFont(hudFont(Font.PLAIN, 0.96f, 0.0f));
            nameLine.add(role);
        }
        text.add(leftAligned(nameLine));
        text.add(Box.createVerticalStrut(scaleForGUI(6)));

        // Set larger than the figures below, so the pitch fills the height the route preview beside it takes.
        JTextArea quote = MarketHud.wrappedText(ContractContactStatement.forContract(campaign, contract), TEXT_MUTED,
              Font.ITALIC, 1.06f);
        quote.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, BORDER_CYAN),
              BorderFactory.createEmptyBorder(0, scaleForGUI(10), 0, 0)));
        text.add(leftAligned(quote));

        String description = contract.getDescription();
        if (!StringUtils.isBlank(description)) {
            text.add(Box.createVerticalStrut(scaleForGUI(12)));
            text.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE,
                  "dossier.contractMarket.section.briefing"))));
            text.add(Box.createVerticalStrut(scaleForGUI(4)));
            JEditorPane descriptionPane = new JEditorPane("text/html", MarkdownRenderer.getRenderedHtml(description));
            descriptionPane.setEditable(false);
            descriptionPane.setFocusable(false);
            descriptionPane.setOpaque(false);
            descriptionPane.setBorder(null);
            // Honoring the display properties makes the HTML take the HUD's font and colour instead of the default
            // dark text, which would vanish on the dark surface.
            descriptionPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            descriptionPane.setForeground(TEXT_MUTED);
            descriptionPane.setFont(hudFont(Font.PLAIN, 1.0f, 0.0f));
            text.add(leftAligned(descriptionPane));
        }
        // Pinned to the top: stretched to the route preview's height, the quote's rule would run on below the text.
        briefing.add(MarketHud.topAligned(text), BorderLayout.CENTER);
        return briefing;
    }

    // endregion Briefing

    // region Target planet

    /**
     * The target system's details, closing the page. The page's own scroll pane scrolls them with everything above.
     */
    private JComponent buildTargetPlanet() {
        PlanetarySystem system = contract.getTargetSystem();
        if (system == null) {
            JLabel empty = Hud.hint(getTextAt(RESOURCE_BUNDLE, "tab.contractMarket.targetPlanet.none"));
            empty.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), 0, 0, 0));
            return empty;
        }
        Planet planet = contract.getTargetPlanet();
        Integer position = planet == null ? null : planet.getSystemPosition();
        // The planet view already draws itself in the HUD palette, so it sits in the dossier unchanged.
        return new PlanetViewPanel(system, campaign, position == null ? 0 : position);
    }

    // endregion Target planet

    // region Helpers

    private void addRow(JPanel section, String labelKey, String value) {
        MarketHud.addRow(section, getTextAt(RESOURCE_BUNDLE, labelKey), value, null, null, false);
    }

    /**
     * The intel value to display: the real {@code value}, or a "No Intel" placeholder when the contract hides that
     * field from the player in the market.
     */
    private String intelValue(ObfuscatableIntel field, String value) {
        return ContractMarketFormatting.intel(contract, field, value);
    }

    private static Color polarityColor(ContractCharacteristic characteristic) {
        return switch (characteristic.getPolarity()) {
            case BENEFICIAL -> READY;
            case ADVERSE -> DANGER;
            case NEUTRAL -> AMBER;
        };
    }

    private static double amount(Money money) {
        return money.getAmount().doubleValue();
    }

    private static String joined(String... parts) {
        return joined(List.of(parts));
    }

    private static String joined(List<String> parts) {
        return String.join(getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator"), parts);
    }

    /** Pads a tab's content away from the tab strip and the window's edges. */
    private static JComponent padded(JComponent content) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(14),
              scaleForGUI(14)));
        holder.add(content, BorderLayout.CENTER);
        return holder;
    }

    // endregion Helpers
}
