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
package mekhq.campaign.mission.newContract.io;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
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
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.ContractMoraleLevel;
import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.mission.newContract.ChaosContract;
import mekhq.campaign.mission.newContract.contractData.*;
import mekhq.campaign.mission.newContract.contractGeneration.ChaosEmployerType;
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
 * <p>Old-format contracts cannot run under the new system, so each converted contract is <b>closed out on load</b>:
 * force-completed as a {@link MissionStatus#SUCCESS complete success}, any explicitly-outstanding payout (the legacy
 * routed payout) settled to the player, and the player advised. The conversion is otherwise best-effort and lossy -
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

        int hospitalBeds = 0;
        int kitchens = 0;
        int holdingCells = 0;

        int scale = 1;
        int requiredCombatElements = 0;
        int difficulty = 0;

        ContractMoraleLevel moraleLevel = ContractMoraleLevel.STALEMATE;
        LocalDate routEndDate = null;
        Money routedPayout = Money.zero();

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
                    case "systemId" -> systemId = value;
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
                    case "hospitalBedsRented" -> hospitalBeds = MathUtility.parseInt(value);
                    case "kitchensRented" -> kitchens = MathUtility.parseInt(value);
                    case "holdingCellsRented" -> holdingCells = MathUtility.parseInt(value);
                    case "requiredCombatTeams" -> scale = MathUtility.parseInt(value);
                    case "requiredCombatElements" -> requiredCombatElements = MathUtility.parseInt(value);
                    case "difficulty" -> difficulty = MathUtility.parseInt(value);
                    case "moraleLevel" -> moraleLevel = ContractMoraleLevel.valueOf(value);
                    case "routEnd" -> routEndDate = MHQXMLUtility.parseDate(value);
                    case "routedPayout" -> routedPayout = Money.fromXmlString(value);
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
        employerCode = codeOrPlaceholder(employerCode, campaign.getFaction().getShortName());
        enemyCode = codeOrPlaceholder(enemyCode, PIRATE_FACTION_CODE);
        employerName = nameOrPlaceholder(employerName, employerCode, year);
        enemyName = nameOrPlaceholder(enemyName, enemyCode, year);
        systemId = systemIdOrPlaceholder(systemId, campaign);
        if (name.isBlank()) {
            name = getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.defaultName", employerName);
        }

        final Person negotiator = placeholderPerson(campaign, employerCode);
        final Person liaison = placeholderPerson(campaign, employerCode);
        final Person opposingCommander = placeholderPerson(campaign, enemyCode);

        contract.setContractId(UUID.randomUUID());
        contract.setContractName(name);
        contract.setDescription(description);
        // Old-format contracts cannot run in the new system: close each one out as a complete success.
        contract.setStatus(MissionStatus.SUCCESS);
        contract.setScale(Math.max(1, scale));
        contract.setRequiredCombatElements(requiredCombatElements);
        contract.setCachedContractDifficulty(difficulty);

        contract.setScheduleData(new ContractScheduleData(startDate, endDate, lengthInMonths));
        contract.setSystemsTargetData(new SystemsTargetData(systemId, null));

        contract.setEmployerData(new EmployerData(ChaosEmployerType.LOCAL_SYSTEM_OWNER, employerCode, employerCode,
              null, employerName, negotiator, liaison, allySkill, allyQuality,
              camouflage(allyCamoCategory, allyCamoFileName), allyColour));
        contract.setEnemyData(new EnemyData(enemyCode, null, enemyName, enemySkill, enemyQuality, opposingCommander,
              camouflage(enemyCamoCategory, enemyCamoFileName), enemyColour, true));

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

        settle(campaign, contract, routedPayout);
        return contract;
    }

    /**
     * Settles a closed-out legacy contract: pays any explicitly-outstanding amount (the routed payout) and advises the
     * player that the old-format contract has been ended.
     */
    private static void settle(final Campaign campaign, final AbstractContract contract, final Money routedPayout) {
        if ((routedPayout != null) && routedPayout.isPositive()) {
            campaign.getPlayerForce()
                  .getFinances()
                  .credit(TransactionType.CONTRACT_PAYMENT,
                        campaign.getLocalDate(),
                        routedPayout,
                        getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.settlement", contract.getName()));
        }

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "legacyContract.report", contract.getName()));
    }

    private static void convertScenarios(final Node scenariosNode, final Campaign campaign, final Version version,
          final ChaosContract contract) {
        final NodeList children = scenariosNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE || !"scenario".equalsIgnoreCase(child.getNodeName())) {
                continue;
            }
            final Scenario scenario = Scenario.generateInstanceFromXML(child, campaign, version);
            if (scenario != null) {
                contract.getScenarios().add(scenario);
            }
        }
    }

    /** Creates a lightweight placeholder NPC of the given faction, or {@code null} if one cannot be made. */
    private static @Nullable Person placeholderPerson(final Campaign campaign, final String factionCode) {
        try {
            return campaign.getPlayerForce()
                         .getHumanResources()
                         .newPerson(campaign, PersonnelRole.MEKWARRIOR, factionCode, Gender.RANDOMIZE);
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to create placeholder personnel for a legacy contract");
            return null;
        }
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

    private static @Nullable String systemIdOrPlaceholder(final @Nullable String systemId, final Campaign campaign) {
        if ((systemId != null) && !systemId.isBlank()) {
            return systemId;
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
