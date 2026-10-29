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

import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic;
import mekhq.campaign.mission.contract.contractData.ObfuscatableIntel;
import mekhq.campaign.personnel.Person;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

/**
 * A plain-text reading of an offer's dossier, for the exports that leave MekHQ: the PDF of the board and the Discord
 * post. Both render this one model, so they always say what the dossier says - and, like the dossier, they show only
 * what the player is meant to know: intel the offer hides reads as "No Intel".
 *
 * @param employerLine the employer's name, as the dossier's eyebrow gives it
 * @param title        the offer's market name
 * @param statusLine   the objective, destination, term and transit, as one line
 * @param assessment   the assessment's word and score, such as "Even Fight 5/10"
 * @param badges       the offer's badges (batchall, proving ground, covert, special rules)
 * @param speaker      who is making the pitch, or {@code null} when the offer has its own description instead
 * @param briefing     the pitch, or the offer's own description as plain text
 * @param sections     the Terms, Intel and Money sections, in that order
 * @param caveat       the note that the money is an estimate
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ContractReadout(String employerLine, String title, String statusLine, String assessment,
      List<String> badges, @Nullable String speaker, String briefing, List<Section> sections, String caveat) {
    private static final String RESOURCE_BUNDLE = ChaosContractMarketDialog.RESOURCE_BUNDLE;

    /**
     * One section of the dossier.
     *
     * @param heading the section's heading
     * @param rows    its label/value rows
     * @param notes   free-standing lines beneath the rows, such as characteristics and advisories
     */
    public record Section(String heading, List<Row> rows, List<String> notes) {}

    /**
     * One labelled value.
     *
     * @param label     the label
     * @param value     the value
     * @param highlight whether the value is a headline figure (the totals and nets)
     */
    public record Row(String label, String value, boolean highlight) {}

    /**
     * Reads an offer's dossier.
     *
     * @param campaign         the campaign holding the offer
     * @param contract         the offer
     * @param estimate         the offer's profit estimate
     * @param currentDate      the campaign date, for time-sensitive names
     * @param hideContractType {@code true} if the campaign hides contract types behind operation codenames
     *
     * @return the readout
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ContractReadout of(Campaign campaign, AbstractContract contract, ContractProfitEstimate estimate,
          LocalDate currentDate, boolean hideContractType) {
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

        String assessment = assessmentLabel(contract) + ' '
                                  + getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.assessment.score",
              assessmentDifficulty(contract));

        List<String> badges = ContractMarketFormatting.badges(contract).stream().map(MarketBadge::text).toList();

        String speaker = null;
        String briefing;
        if (StringUtils.isBlank(contract.getDescription())) {
            Person negotiator = contract.getEmployerNegotiator();
            speaker = negotiator == null ? contract.getEmployerMarketDisplayName() : negotiator.getFullTitle();
            briefing = ContractContactStatement.forContract(campaign, contract);
        } else {
            briefing = markdownToPlainText(contract.getDescription());
        }

        return new ContractReadout(contract.getEmployerMarketDisplayName(),
              marketName(contract),
              String.join(separator(), status),
              assessment,
              badges,
              speaker,
              briefing,
              List.of(terms(contract, estimate), intel(contract, currentDate), money(contract, estimate)),
              getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.caveat"));
    }

    /** @return the separator the dossier places between the parts of a line, such as " · " */
    static String separator() {
        return getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator");
    }

    private static Section terms(AbstractContract contract, ContractProfitEstimate estimate) {
        List<Row> rows = new ArrayList<>();
        rows.add(row("dossier.contractMarket.terms.command", contract.getCommandRights().toString()));
        rows.add(row("dossier.contractMarket.terms.salvage", salvage(contract)));
        rows.add(row("dossier.contractMarket.terms.support", support(contract)));
        rows.add(row("dossier.contractMarket.terms.transport", transport(contract)));
        rows.add(row("dossier.contractMarket.terms.term",
              getFormattedTextAt(RESOURCE_BUNDLE, "card.contractMarket.months", contract.getLengthInMonths())));
        rows.add(row("dossier.contractMarket.terms.transit", transit(estimate)));

        List<String> notes = new ArrayList<>();
        if (!contract.getCharacteristics().isEmpty()) {
            List<ContractCharacteristic> ordered = new ArrayList<>(contract.getCharacteristics());
            ordered.sort(Comparator.comparing(ContractCharacteristic::getName));
            notes.add(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.characteristics") + ": "
                            + String.join(", ", ordered.stream().map(ContractCharacteristic::getName).toList()));
        }
        return new Section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.terms"), rows, notes);
    }

    private static Section intel(AbstractContract contract, LocalDate currentDate) {
        List<Row> rows = new ArrayList<>();
        rows.add(row("dossier.contractMarket.intel.alliedCommand",
              ContractMarketFormatting.intel(contract, ObfuscatableIntel.ALLIED_COMMAND, alliedCommand(contract))));
        rows.add(row("dossier.contractMarket.intel.opposition",
              ContractMarketFormatting.intel(contract, ObfuscatableIntel.OPPOSITION,
                    contract.getEnemyDisplayNameIncludingFaction(currentDate.getYear()))));
        rows.add(row("dossier.contractMarket.intel.threat",
              ContractMarketFormatting.intel(contract, ObfuscatableIntel.THREAT, threat(contract))));
        rows.add(row("dossier.contractMarket.intel.morale",
              ContractMarketFormatting.intel(contract, ObfuscatableIntel.MORALE,
                    contract.getMoraleLevel().toString())));
        rows.add(row("dossier.contractMarket.intel.intensity", String.valueOf(contract.getTrackCount())));

        // The same one-line advisory the dossier shows for a contract with a special nature.
        List<String> notes = new ArrayList<>();
        String advisoryKey = null;
        if (contract.isFalseFlag()) {
            advisoryKey = "dossier.contractMarket.intel.advisory.falseFlag";
        } else if (contract.isCovert()) {
            advisoryKey = "dossier.contractMarket.intel.advisory.covert";
        } else if (contract.isProvingGround()) {
            advisoryKey = "dossier.contractMarket.intel.advisory.provingGround";
        }
        if (advisoryKey != null) {
            notes.add(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.intel.advisory") + ": "
                            + getTextAt(RESOURCE_BUNDLE, advisoryKey));
        }
        return new Section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.intel"), rows, notes);
    }

    private static Section money(AbstractContract contract, ContractProfitEstimate estimate) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.total"),
              contract.getTotalPay().toAmountAndSymbolString(), true));
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.netPerMonth"),
              signedAmount(estimate.getNetPerMonth()), true));
        rows.add(new Row(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.money.monthly",
              contract.getLengthInMonths()), contract.getMonthlyPayOut().toAmountString(), false));
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.metric.transport"),
              contract.getTransportPayment().toAmountString(), false));
        rows.add(new Row(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.expenses",
              contract.getLengthInMonths(), Math.max(0, estimate.transitDays())),
              costAmount(estimate.operatingCosts()), false));
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.net"),
              signedAmount(estimate.estimatedNet()), true));

        if (estimate.potentialCombatPay().isPositive()) {
            String bonusCount = new DecimalFormat("0.#").format(estimate.combatBonus().bonusCount());
            rows.add(new Row(getFormattedTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.combatPay",
                  bonusCount), signedAmount(estimate.potentialCombatPay()), false));
            Money netWithCombatPay = estimate.getNetWithCombatPay();
            rows.add(new Row(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.profit.netWithCombatPay"),
                  signedAmount(netWithCombatPay), true));
        }
        return new Section(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.money"), rows, List.of());
    }

    private static Row row(String labelKey, String value) {
        return new Row(getTextAt(RESOURCE_BUNDLE, labelKey), value, false);
    }

    /**
     * Flattens an offer's Markdown description to plain text, as neither a PDF page nor a Discord code block can show
     * the formatting.
     */
    static String markdownToPlainText(String markdown) {
        return TextContentRenderer.builder()
                     .build()
                     .render(Parser.builder().build().parse(markdown))
                     .strip();
    }
}
