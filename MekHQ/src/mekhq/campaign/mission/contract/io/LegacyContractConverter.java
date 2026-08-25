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
package mekhq.campaign.mission.contract.io;

import static java.lang.Math.max;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.MathUtility;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Converts a legacy {@code <mission>} XML node - a pre-migration {@code AtBContract}/{@code Contract}/{@code Mission} -
 * into a new {@link AbstractContract}, so old saves keep a record of their contracts once the legacy classes are
 * removed.
 *
 * <p>Old-format contracts cannot run under the new system, so a converted contract that was <b>still active</b> is
 * closed out on load: force-completed as a {@link MissionStatus#SUCCESS complete success}, any explicitly-outstanding
 * payout (the legacy routed payout) settled to the player, and the player advised. A contract that had already
 * concluded keeps the outcome it finished with, and is converted silently. The conversion is otherwise best-effort and
 * lossy -
 * fields with a clean equivalent are carried over, and fields that are normally never {@code null} in the new model
 * (faction codes, display names, target system, and the NPC personnel) are filled with placeholders so the resulting
 * contract is always well-formed. The negotiable terms have no step-table equivalent and are set to a neutral step, and
 * the legacy integer id is replaced with a fresh {@link UUID}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class LegacyContractConverter {
    private static final MMLogger LOGGER = MMLogger.create(LegacyContractConverter.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractAutomation";

    /** Neutral mid-table step used for every term the legacy percentage model cannot map. */
    private static final ChaosContractStepsTable DEFAULT_STEP = ChaosContractStepsTable.STEP_SEVEN;
    /** Placeholder objective when the legacy save has none. */
    private static final ContractObjectiveType DEFAULT_OBJECTIVE = ContractObjectiveType.GARRISON_DUTY;

    private LegacyContractConverter() {}

    /**
     * Converts and closes out one legacy {@code <mission>} element.
     *
     * @param missionNode the legacy {@code <mission>} element
     * @param campaign    the owning campaign (used for placeholders, scenarios, and the payout settlement)
     * @param version     the save file version
     *
     * @return the converted, closed-out contract, never {@code null}
     */
    public static AbstractContract convert(final Node missionNode, final Campaign campaign, final Version version) {
        final int year = campaign.getGameYear();

        // Accumulators, seeded with the defaults a fresh contract uses.
        String name = "";
        String description = "";
        String systemId = null;
        String legacySystemName = "";
        String legacyParentContractId = "";
        MissionStatus status = MissionStatus.ACTIVE;

        LocalDate startDate = null;
        LocalDate endDate = null;
        int lengthInMonths = 0;

        String employerCode = "";
        String employerName = "";
        String enemyCode = "";
        String enemyName = "";
        SkillLevel allySkill = SkillLevel.REGULAR;
        SkillLevel enemySkill = SkillLevel.REGULAR;
        int allyQuality = 0;
        int enemyQuality = 0;
        PlayerColour allyColour = PlayerColour.BLUE;
        PlayerColour enemyColour = PlayerColour.RED;
        String allyCamoCategory = null;
        String allyCamoFileName = null;
        String enemyCamoCategory = null;
        String enemyCamoFileName = null;

        ContractObjectiveType objective = DEFAULT_OBJECTIVE;

        Money transportAmount = Money.zero();
        Money baseAmount = Money.zero();
        // Legacy contracts almost never persist their resolved pay amounts (they recomputed them live from the
        // campaign's contract base); the amount fields above read as zero, so the base pay is reconstructed from this
        // multiplier and the contract base in a post-load pass.
        double paymentMultiplier = 1.0;

        int hospitalBeds = 0;
        int kitchens = 0;
        int holdingCells = 0;

        int scale = 1;
        int requiredCombatElements = 0;
        int difficulty = 0;

        ContractMoraleLevel moraleLevel = ContractMoraleLevel.STALEMATE;
        LocalDate routEndDate = null;
        Money routedPayout = Money.zero();

        // Matches the legacy default: a contract that never involved a Batchall counts as accepted.
        boolean batchallAccepted = true;
        int sharesPercent = AbstractContract.DEFAULT_SHARES_PERCENT;

        // The legacy format stores these two NPCs in full; the new model's third NPC (the employer's negotiator) has
        // no legacy equivalent and is always a placeholder.
        Person legacyLiaison = null;
        Person legacyOpposingCommander = null;

        final ChaosContract contract = new ChaosContract();

        final NodeList children = missionNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = child.getNodeName();
            final String value = child.getTextContent().trim();
            try {
                switch (tag) {
                    case "name" -> name = value;
                    case "desc" -> description = value;
                    // "planetId" is what the oldest saves called the system id; "planetName" holds the system's
                    // name when the save has no id at all - the two are written as alternatives, never together.
                    case "systemId", "planetId" -> systemId = value;
                    case "planetName" -> legacySystemName = value;
                    case "status" -> status = MissionStatus.parseFromString(value);
                    case "startDate" -> startDate = MHQXMLUtility.parseDate(value);
                    case "endDate" -> endDate = MHQXMLUtility.parseDate(value);
                    case "nMonths" -> lengthInMonths = MathUtility.parseInt(value);
                    case "employerCode" -> employerCode = value;
                    case "employer" -> employerName = value;
                    case "enemyCode" -> enemyCode = value;
                    case "enemyBotName" -> enemyName = value;
                    case "allySkill" -> allySkill = SkillLevel.valueOf(value);
                    case "enemySkill" -> enemySkill = SkillLevel.valueOf(value);
                    case "allyQuality" -> allyQuality = MathUtility.parseInt(value);
                    case "enemyQuality" -> enemyQuality = MathUtility.parseInt(value);
                    case "allyColour" -> allyColour = PlayerColour.valueOf(value);
                    case "enemyColour" -> enemyColour = PlayerColour.valueOf(value);
                    case "allyCamoCategory" -> allyCamoCategory = value;
                    case "allyCamoFileName" -> allyCamoFileName = value;
                    case "enemyCamoCategory" -> enemyCamoCategory = value;
                    case "enemyCamoFileName" -> enemyCamoFileName = value;
                    case "contractType" -> objective = ContractObjectiveType.parseFromString(value);
                    case "transportAmount" -> transportAmount = Money.fromXmlString(value);
                    case "baseAmount" -> baseAmount = Money.fromXmlString(value);
                    case "paymentMultiplier" -> paymentMultiplier = MathUtility.parseDouble(value);
                    case "hospitalBedsRented" -> hospitalBeds = MathUtility.parseInt(value);
                    case "kitchensRented" -> kitchens = MathUtility.parseInt(value);
                    case "holdingCellsRented" -> holdingCells = MathUtility.parseInt(value);
                    case "requiredCombatTeams" -> scale = MathUtility.parseInt(value);
                    case "requiredCombatElements" -> requiredCombatElements = MathUtility.parseInt(value);
                    case "difficulty" -> difficulty = MathUtility.parseInt(value);
                    case "moraleLevel" -> moraleLevel = ContractMoraleLevel.valueOf(value);
                    case "routEnd" -> routEndDate = MHQXMLUtility.parseDate(value);
                    case "routedPayout" -> routedPayout = Money.fromXmlString(value);
                    case "batchallAccepted" -> batchallAccepted = Boolean.parseBoolean(value);
                    case "sharesPct" -> sharesPercent = MathUtility.parseInt(value);
                    case "employerLiaison" -> legacyLiaison = legacyPerson(child, campaign, version);
                    case "clanOpponent" -> legacyOpposingCommander = legacyPerson(child, campaign, version);
                    case "parentContractId" -> legacyParentContractId = value;
                    case "scenarios" -> convertScenarios(child, campaign, version, contract);
                    default -> {
                        if (StratConCampaignState.ROOT_XML_ELEMENT_NAME.equals(tag)) {
                            contract.setStratConCampaignState(StratConCampaignState.Deserialize(child));
                        }
                        // Everything else is a legacy-only field with no new-model equivalent; skip it.
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "Error converting legacy contract element: {}", tag);
            }
        }

        // Fill placeholders for values that must never be null in the new model.
        employerCode = codeOrPlaceholder(employerCode, campaign.getPlayerForce().getFaction().getShortName());
        enemyCode = codeOrPlaceholder(enemyCode, PIRATE_FACTION_CODE);
        employerName = nameOrPlaceholder(employerName, employerCode, year);
        enemyName = nameOrPlaceholder(enemyName, enemyCode, year);
        systemId = systemIdOrPlaceholder(systemId, legacySystemName, campaign);
        if (name.isBlank()) {
            name = getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.defaultName", employerName);
        }

        // The new contract model has no parent/child linkage, so a subcontract converts as a standalone contract.
        // Logged rather than dropped silently, so the loss shows up in the migration log alongside everything else.
        if (!legacyParentContractId.isBlank()) {
            LOGGER.warn("Legacy contract '{}' was a subcontract of mission {}. The new contract model has no"
                              + " subcontract linkage, so it converts as a standalone contract.",
                  name, legacyParentContractId);
        }

        // Keep the NPCs the save actually recorded; only invent one where the legacy format had none. A liaison is
        // absent unless the contract had one, and a clan opponent only exists for Clan enemies.
        final Person negotiator = placeholderPerson(campaign, employerCode);
        final Person liaison = (legacyLiaison != null) ? legacyLiaison : placeholderPerson(campaign, employerCode);
        final Person opposingCommander = (legacyOpposingCommander != null) ?
                                               legacyOpposingCommander :
                                               placeholderPerson(campaign, enemyCode);

        contract.setContractId(UUID.randomUUID());
        contract.setContractName(name);
        contract.setDescription(description);
        // Old-format contracts cannot run in the new system, so an active one is closed out as a complete success.
        // A contract that had already concluded keeps the outcome it finished with - there is nothing to close out.
        final boolean wasActive = status.isActive();
        contract.setStatus(wasActive ? MissionStatus.SUCCESS : status);
        contract.setScale(max(1, scale));
        contract.setSharesPercent(sharesPercent);
        contract.setRequiredCombatElements(requiredCombatElements);
        contract.setCachedContractDifficulty(difficulty);

        contract.setScheduleData(new ContractScheduleData(startDate, endDate, lengthInMonths));
        contract.setSystemsTargetData(new SystemsTargetData(systemId, null));

        contract.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_SYSTEM_OWNER, employerCode, employerCode,
              null, employerName, negotiator, liaison, allySkill, allyQuality,
              camouflage(allyCamoCategory, allyCamoFileName), allyColour));
        contract.setEnemyData(new EnemyData(enemyCode, null, enemyName, enemySkill, enemyQuality, opposingCommander,
              camouflage(enemyCamoCategory, enemyCamoFileName), enemyColour, batchallAccepted));

        contract.setContractTerms(new ContractTermsData(DEFAULT_STEP, DEFAULT_STEP, DEFAULT_STEP, DEFAULT_STEP,
              DEFAULT_STEP));
        contract.setObjectiveData(new ContractObjectiveData(objective, ContractObjectiveType.UNDEFINED));
        contract.setContractFinanceData(new ContractFinanceData(transportAmount, baseAmount, Money.zero()));
        contract.setRentedFacilitiesData(new RentedFacilitiesData(hospitalBeds, kitchens, holdingCells));
        contract.setMoraleData(new MoraleData(moraleLevel, routEndDate, routedPayout));

        // Point any restored StratCon state back at its new contract.
        if (contract.getStratConCampaignState() != null) {
            contract.getStratConCampaignState().setContract(contract);
        }

        // Only a contract that was still running needs settling and an advisory; one that had already concluded was
        // settled when it ended.
        if (wasActive) {
            campaign.addReport(GENERAL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.report", contract.getName()));
            if (routedPayout.isPositive()) {
                // A routed contract's outstanding payout is an explicit lump sum known here (it needs no force data),
                // so settle it immediately - mirroring how completeMission pays a routed contract.
                settle(campaign, contract, routedPayout);
            } else {
                // Otherwise the remaining balance is the leftover monthly pay, reconstructed from the campaign's
                // contract base. That is unavailable this early in the load (forces are parsed after missions), so the
                // settlement is deferred to a post-load pass via a pending marker.
                contract.setPendingLegacySettlementMultiplier(paymentMultiplier);
            }
        }
        return contract;
    }

    /** Credits a closed-out legacy contract's outstanding lump sum (its routed payout) to the player. */
    private static void settle(final Campaign campaign, final AbstractContract contract, final Money settlementAmount) {
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.CONTRACT_PAYMENT,
                    campaign.getLocalDate(),
                    settlementAmount,
                    getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.settlement", contract.getName()));
    }

    /**
     * Post-load pass that settles the remaining balance of every legacy contract closed out on load. Runs once the whole
     * save is read, so the force is populated and the campaign's contract base can be computed. For each contract still
     * carrying a pending-settlement marker, the monthly base pay is reconstructed as
     * {@code contractBase * paymentMultiplier}, written onto the contract's finance record (the legacy amounts were
     * never persisted, so it reads zero without this), and its remaining months are paid to the player as a lump sum.
     *
     * @param campaign the loaded campaign whose converted legacy contracts are settled
     */
    public static void settlePendingLegacyContracts(final Campaign campaign) {
        final Money contractBase = campaign.getAccountant().getContractBase();

        for (final AbstractContract contract : campaign.getContractHistoryAsMap().values()) {
            final Double paymentMultiplier = contract.getPendingLegacySettlementMultiplier();
            if (paymentMultiplier == null) {
                continue;
            }
            contract.setPendingLegacySettlementMultiplier(null);

            final Money monthlyBase = contractBase.multipliedBy(paymentMultiplier);
            // Fix the finance record, which the conversion left at the save's (zero) base amount.
            contract.updateMonthlyPay(monthlyBase);

            final LocalDate endDate = contract.getEndingDate();
            final long monthsRemaining = max(1,
                  endDate == null ? 0 : ChronoUnit.MONTHS.between(campaign.getLocalDate(), endDate));
            final Money settlementAmount = monthlyBase.multipliedBy(monthsRemaining);
            if (settlementAmount.isPositive()) {
                campaign.getPlayerForce()
                      .getFinances()
                      .credit(TransactionType.CONTRACT_PAYMENT,
                            campaign.getLocalDate(),
                            settlementAmount,
                            getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.settlement", contract.getName()));
            }
        }
    }

    private static void convertScenarios(final Node scenariosNode, final Campaign campaign, final Version version,
          final ChaosContract contract) {
        final NodeList children = scenariosNode.getChildNodes();
        int skipped = 0;
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE || !"scenario".equalsIgnoreCase(child.getNodeName())) {
                continue;
            }
            final Scenario scenario = Scenario.generateInstanceFromXML(child, campaign, version);
            if (scenario == null) {
                // Expected for legacy AtB scenarios, whose per-type classes were retired along with legacy AtB
                // scenario generation. The contract itself still converts; it just arrives without them.
                skipped++;
                continue;
            }
            contract.getScenarios().add(scenario);
        }

        if (skipped > 0) {
            LOGGER.info("Dropped {} scenario(s) of a legacy contract that can no longer be loaded.", skipped);
        }
    }

    /**
     * Reads an NPC the legacy format stored inline under its own wrapper tag.
     *
     * <p>Legacy saves write the person's fields directly into the wrapper (there is no nested {@code <person>}
     * element), so the wrapper node itself is what gets parsed.</p>
     *
     * @param personNode the wrapper element holding the person's fields
     * @param campaign   the campaign the person belongs to
     * @param version    the save file version
     *
     * @return the parsed NPC, or {@code null} if it could not be read
     */
    private static @Nullable Person legacyPerson(final Node personNode, final Campaign campaign,
          final Version version) {
        try {
            return Person.generateInstanceFromXML(personNode, campaign, version);
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to read a legacy contract's NPC; a placeholder will stand in.");
            return null;
        }
    }

    /**
     * Creates a lightweight placeholder NPC of the given faction.
     *
     * <p>These fill the new model's NPC slots (the employer's negotiator and liaison, and the opposing commander),
     * which the legacy format has no equivalent for. Generation can fail on an unrecognized legacy faction code, so a
     * bare {@link Person} is used as a last resort rather than returning {@code null}: those slots are not nullable,
     * and an unnamed placeholder on a defunct contract is better than aborting the conversion and losing the
     * contract.</p>
     *
     * @param campaign    the campaign the placeholder belongs to
     * @param factionCode the faction the placeholder should originate from
     *
     * @return a placeholder NPC, never {@code null}
     */
    private static Person placeholderPerson(final Campaign campaign, final String factionCode) {
        try {
            final Person person = campaign.getPlayerForce()
                                        .getHumanResources()
                                        .newPerson(campaign, PersonnelRole.MEKWARRIOR, factionCode, Gender.RANDOMIZE);
            if (person != null) {
                return person;
            }
            LOGGER.warn("Placeholder personnel generation returned null for faction {}; using an unnamed placeholder.",
                  factionCode);
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to create placeholder personnel for a legacy contract; using an unnamed"
                                   + " placeholder.");
        }
        // Falls back to the player's own faction, which is always valid - the legacy code may be what failed above.
        return new Person(campaign);
    }

    private static String codeOrPlaceholder(final String code, final String placeholder) {
        return code.isBlank() ? placeholder : code;
    }

    private static String nameOrPlaceholder(final String displayName, final String factionCode, final int year) {
        if (!displayName.isBlank()) {
            return displayName;
        }
        final Faction faction = Factions.getInstance().getFaction(factionCode);
        return faction == null ? factionCode : faction.getFullName(year);
    }

    /**
     * Resolves the system a legacy contract was fought in, in the same order the legacy loader used: an explicit id
     * first, then the recorded system name, and finally the campaign's current system as a stand-in.
     *
     * <p>A save that predates system ids records its location as a name instead, so that name is resolved here rather
     * than discarded - otherwise such a contract would adopt whatever system the campaign happens to be sitting in.</p>
     *
     * <p>Returns {@code null} when none of the three yields a system: an unrecognized name (the universe data may no
     * longer carry it) and no current system, which during a load is possible because the force's location may not be
     * restored yet. {@link SystemsTargetData} treats a missing system id as unknown rather than invalid, so a defunct
     * contract simply displays no system.</p>
     *
     * @param systemId         the system id read from the legacy save, if any
     * @param legacySystemName the system's name, recorded by saves written before system ids existed
     * @param campaign         the campaign used to resolve the name, and whose current system stands in as a last
     *                         resort
     *
     * @return the resolved system id, or {@code null} when none can be determined
     */
    private static @Nullable String systemIdOrPlaceholder(final @Nullable String systemId,
          final String legacySystemName, final Campaign campaign) {
        if ((systemId != null) && !systemId.isBlank()) {
            return systemId;
        }

        if (!legacySystemName.isBlank()) {
            final PlanetarySystem namedSystem = campaign.getSystemByName(legacySystemName);
            if (namedSystem != null) {
                return namedSystem.getId();
            }
            LOGGER.warn("A legacy contract names an unknown system ({}); falling back to the current system.",
                  legacySystemName);
        }

        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        return currentSystem == null ? systemId : currentSystem.getId();
    }

    /** Rebuilds a camouflage from its legacy category/filename tags, or a default when neither is present. */
    private static Camouflage camouflage(final @Nullable String category, final @Nullable String filename) {
        if ((category == null) && (filename == null)) {
            return new Camouflage();
        }
        return new Camouflage(category, filename);
    }
}
