/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.MEK;
import static megamek.common.units.UnitType.TANK;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.getContractDefinition;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.force.CombatTeam.getStandardFormationSize;
import static mekhq.campaign.force.FormationLevel.BATTALION;
import static mekhq.campaign.force.FormationLevel.COMPANY;
import static mekhq.campaign.mission.ContractDifficulty.calculateContractDifficulty;
import static mekhq.campaign.mission.RandomFactionCamouflage.pickRandomCamouflage;
import static mekhq.campaign.mission.enums.ContractMoraleLevel.OVERWHELMING;
import static mekhq.campaign.randomEvents.prisoners.PrisonerStatus.FREE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.ContractMoraleLevel;
import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.other.MercenaryAuction;
import mekhq.campaign.randomEvents.other.RoninOffer;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contract class for use with Against the Bot rules
 *
 * @author Neoancient
 */
@Deprecated(since = "0.51.01", forRemoval = true)
public class AtBContract extends Contract {
    private static final MMLogger logger = MMLogger.create(AtBContract.class);

    /* null unless subcontract */
    protected AtBContract parentContract;
    /* hired by another mercenary unit on contract to a third-party employer */
    boolean mercSubcontract;

    protected int extensionLength;

    protected int playerMinorBreaches;
    protected int employerMinorBreaches;
    protected int contractScoreArbitraryModifier;

    protected int moraleMod = 0;

    /* lasts for a month, then removed at next events roll */
    protected boolean priorLogisticsFailure;
    /**
     * If the date is non-null, there will be a special scenario or big battle on that date, but the scenario is not
     * generated until the other battle rolls for the week.
     */
    protected LocalDate specialEventScenarioDate;
    protected int specialEventScenarioType;
    /* Lasts until end of contract */
    protected int battleTypeMod;
    /* Only applies to next week */
    protected int nextWeekBattleTypeMod;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBContract";

    protected AtBContract() {
        this(null);
    }

    public AtBContract(String name) {
        setName(name);
        setContractDifficulty(Integer.MIN_VALUE);

        parentContract = null;
        setContractTypeAndName(ContractObjectiveType.GARRISON_DUTY);

        extensionLength = 0;

        setSharesPercent(0);
        priorLogisticsFailure = false;
        specialEventScenarioDate = null;
        battleTypeMod = 0;
        nextWeekBattleTypeMod = 0;
    }

    public void initContractDetails(Campaign campaign) {
        int companySize = getStandardFormationSize(campaign.getFaction(), COMPANY.getDepth());
        int battalionSize = getStandardFormationSize(campaign.getFaction(), BATTALION.getDepth());

        if (ContractUtilities.getEffectiveNumUnits(campaign) <= companySize) {
            setOverheadCompensation(OH_FULL);
        } else if (ContractUtilities.getEffectiveNumUnits(campaign) <= battalionSize) {
            setOverheadCompensation(OH_HALF);
        } else {
            setOverheadCompensation(OH_NONE);
        }

        int currentYear = campaign.getGameYear();
        setAllyBotName(getEmployerName(currentYear));
        setAllyCamouflage(pickRandomCamouflage(currentYear, getEmployerCode()));

        setEnemyBotName(generateEnemyName(currentYear));
        setEnemyCamouflage(pickRandomCamouflage(currentYear, getEnemyCode()));
    }

    public void calculateLength(final boolean variable) {
        setLengthInMonths(getObjectiveType().getChaosObjectiveType().calculateLength(variable));
    }


    /**
     * @return the total available support points, or 0 if StratCon is not enabled for this contract
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getCurrentSupportPoints() {
        if (getStratConCampaignState() == null) {
            return 0;
        }

        return getStratConCampaignState().getSupportPoints();
    }

    public int getContractScoreArbitraryModifier() {
        return contractScoreArbitraryModifier;
    }

    /**
     * Performs a bonus roll to determine and execute a random campaign bonus. The roll is simulated using 1d6, and the
     * outcome triggers different bonus effects based on the roll value. The effects may involve recruiting dependents,
     * adding new units, or other benefits as determined by the campaign options and roll outcome.
     *
     * @param campaign       the current {@link Campaign} instance.
     * @param isPostScenario a {@code boolean} indicating if this roll occurs post-scenario (used to determine specific
     *                       behaviors for roll = 3).
     *
     * @return {@code true} if specific post-scenario behavior is triggered (roll = 3), otherwise {@code false}.
     *
     * @throws IllegalStateException if an unexpected roll value is encountered.
     */
    public boolean doBonusRoll(Campaign campaign, boolean isPostScenario) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        int number;
        int roll = d6();

        return switch (roll) {
            case 1 -> { /* 1d6 dependents */
                if (campaignOptions.isUseRandomDependentAddition()) {
                    number = d6();
                    campaign.addReport(GENERAL, "Bonus: " + number + " dependent" + ((number > 1) ? "s" : ""));

                    for (int i = 0; i < number; i++) {
                        Person person = campaign.getPlayerForce()
                                              .getHumanResources()
                                              .newDependent(campaign, megamek.common.enums.Gender.RANDOMIZE);
                        campaign.recruitPerson(person, FREE, true, false, false);
                    }
                } else {
                    campaign.addReport(GENERAL, "Bonus: Ronin");
                    new RoninOffer(campaign, getStratConCampaignState(), getRequiredCombatElements());
                }
                yield false;
            }
            case 2 -> {
                campaign.addReport(GENERAL, "Bonus: Ronin");
                new RoninOffer(campaign, getStratConCampaignState(), getRequiredCombatElements());
                yield false;
            }
            case 3 -> { // Resupply
                if (!campaignOptions.isUseStratCon()) {
                    campaign.addReport(GENERAL, "Bonus: Ronin");
                    new RoninOffer(campaign, getStratConCampaignState(), getRequiredCombatElements());
                    yield false;
                } else {
                    if (isPostScenario) {
                        yield true;
                    } else {
                        campaign.addReport(GENERAL, "Bonus: Support Point");
                        getStratConCampaignState().changeSupportPoints(1);
                        yield false;
                    }
                }
            }
            case 4 -> {
                new MercenaryAuction(campaign, getRequiredCombatElements(), getStratConCampaignState(), TANK);
                yield false;
            }
            case 5 -> {
                new MercenaryAuction(campaign,
                      getRequiredCombatElements(),
                      getStratConCampaignState(),
                      AEROSPACE_FIGHTER);
                yield false;
            }
            case 6 -> {
                new MercenaryAuction(campaign, getRequiredCombatElements(), getStratConCampaignState(), MEK);
                yield false;
            }
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/mission/AtBContract.java/doBonusRoll: " + roll);
        };
    }

    public boolean isSubcontract() {
        return parentContract != null;
    }

    public AtBContract getParentContract() {
        return parentContract;
    }

    public void setParentContract(AtBContract parent) {
        parentContract = parent;
    }

    public boolean isMercSubcontract() {
        return mercSubcontract;
    }

    public void setMercSubcontract(boolean sub) {
        mercSubcontract = sub;
    }

    public void checkEvents(Campaign campaign) {
        if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            nextWeekBattleTypeMod = 0;
        }

        boolean isUseStratCon = campaign.getCampaignOptions().isUseStratCon();

        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            if (priorLogisticsFailure) {
                changePartsAvailabilityLevel(-1);
                priorLogisticsFailure = false;
            }

            String text;
            switch (getObjectiveType().generateEventType(campaign)) {
                case BONUS_ROLL:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> ");
                    doBonusRoll(campaign, false);
                    break;
                case SPECIAL_SCENARIO:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> Special scenario this month");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = getObjectiveType().generateSpecialScenarioType(campaign);
                    break;
                case CIVIL_DISTURBANCE:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Civil disturbance<br />Next enemy morale roll gets +1 modifier");
                    moraleMod++;
                    break;
                case SPORADIC_UPRISINGS:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Sporadic uprisings<br />+2 to next enemy morale roll");
                    moraleMod += 2;
                    break;
                case REBELLION:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> Rebellion<br />+2 to next enemy morale roll");
                    moraleMod += 2;

                    if (!isUseStratCon) {
                        specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                        specialEventScenarioType = AtBScenario.CIVILIAN_RIOT;
                    }
                    break;
                case BETRAYAL:
                    text = "<b>Special Event:</b> Betrayal (employer minor breach)<br />";
                    switch (d6()) {
                        case 1:
                            text += "Major logistics problem: parts availability level for the rest of the contract becomes one level lower.";
                            changePartsAvailabilityLevel(1);
                            break;
                        case 2:
                            text += "Transport: Player is abandoned in the field by employer transports; if he loses a Base Attack battle he loses all Meks on repair.";
                            break;
                        case 3:
                            text += "Diversion: All Battle Type rolls for the rest of the contract get a -5 modifier.";
                            battleTypeMod -= 5;
                            break;
                        case 4:
                            text += "False Intelligence: Next week Battle Type rolls get a -10 modifier.";
                            nextWeekBattleTypeMod -= 10;
                            break;
                        case 5:
                            text += "The Company Store: All equipment/supply prices are increased by 100% until the end of the contract.";
                            break;
                        case 6:
                            text += "False Alarm: No betrayal, but the employer still gets a minor breach.";
                            break;
                    }
                    employerMinorBreaches++;
                    campaign.addReport(GENERAL, text);
                    break;
                case TREACHERY:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Treachery<br />Bad information from employer. Next Enemy Morale roll gets +1. Employer minor breach.");
                    moraleMod++;
                    employerMinorBreaches++;
                    break;
                case LOGISTICS_FAILURE:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Logistics Failure<br />Parts availability for the next month are one level lower.");
                    changePartsAvailabilityLevel(1);
                    priorLogisticsFailure = true;
                    break;
                case REINFORCEMENTS:
                    campaign.addReport(GENERAL,
                          "<b>Special Event:</b> Reinforcements<br />The next Enemy Morale roll gets a -1.");
                    moraleMod--;
                    break;
                case SPECIAL_EVENTS:
                    text = "<b>Special Event:</b> ";
                    switch (d6()) {
                        case 1:
                            text += "Change of Alliance: Next Enemy Morale roll gets a +1 modifier.";
                            moraleMod++;
                            break;
                        case 2:
                            text += "Internal Dissension";
                            if (!isUseStratCon) {
                                specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                                specialEventScenarioType = AtBScenario.AMBUSH;
                            } else {
                                StratConCampaignState campaignState = getStratConCampaignState();

                                if (campaignState != null) {
                                    text += ": -1 Support Point";
                                    campaignState.changeSupportPoints(-1);
                                }
                            }
                            break;
                        case 3:
                            text += "ComStar Interdict: Base availability level decreases one level for the rest of the contract.";
                            changePartsAvailabilityLevel(1);
                            break;
                        case 4:
                            text += "Defectors: Next Enemy Morale roll gets a -1 modifier.";
                            moraleMod--;
                            break;
                        case 5:
                            text += "Free Trader: Base availability level increases one level for the rest of the contract.";
                            changePartsAvailabilityLevel(-1);
                            break;
                        case 6:
                            final String unitName = campaign.getUnitMarket()
                                                          .addSingleUnit(campaign,
                                                                UnitMarketType.EMPLOYER,
                                                                MEK,
                                                                getEmployerFaction(),
                                                                DragoonRating.DRAGOON_F.getRating(),
                                                                50);
                            if (unitName != null) {
                                text += String.format(
                                      "Surplus Sale: %s offered by employer on the <a href='UNIT_MARKET'>unit market</a>",
                                      unitName);
                            }
                            break;
                    }
                    campaign.addReport(GENERAL, text);
                    break;
                case BIG_BATTLE:
                    campaign.addReport(GENERAL, "<b>Special Event:</b> Big battle this month");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = getObjectiveType().generateBigBattleType();
                    break;
            }
        }

        /*
         * If the campaign somehow gets past the scheduled date (such as by changing the date in the campaign
         * options), ignore it rather than generating a new scenario in the past. The event will still be available
         * (if the campaign date is restored) until another special scenario or big battle event is rolled.
         */
        if ((specialEventScenarioDate != null) && !specialEventScenarioDate.isBefore(campaign.getLocalDate())) {
            LocalDate nextMonday = campaign.getLocalDate()
                                         .plusDays(8 - campaign.getLocalDate().getDayOfWeek().getValue());

            if (specialEventScenarioDate.isBefore(nextMonday)) {
                AtBScenario atBScenario = AtBScenarioFactory.createScenario(campaign,
                      null,
                      specialEventScenarioType,
                      false,
                      specialEventScenarioDate);

                if (atBScenario != null) {
                    campaign.addScenario(atBScenario, this);

                    if (campaign.getCampaignOptions().isUsePlanetaryConditions()) {
                        atBScenario.setPlanetaryConditions(this, campaign);
                    }

                    atBScenario.setForces(campaign);
                }

                specialEventScenarioDate = null;
            }
        }
    }

    public LocalDate getRandomDayOfMonth(LocalDate today) {
        return LocalDate.of(today.getYear(),
              today.getMonth(),
              randomInt(today.getMonth().length(today.isLeapYear())) + 1);
    }

    public boolean contractExtended(final Campaign campaign) {
        if (getObjectiveType().isPirateHunting() || getObjectiveType().isRiotDuty()) {
            return false;
        }

        final String warName = RandomFactionGenerator.getInstance()
                                     .getFactionHints()
                                     .getCurrentWar(getEmployerFaction(), getEnemy(), campaign.getLocalDate());
        if (warName == null) {
            return false;
        }

        final int extension;
        int roll = d6();

        if (roll == 1) {
            extension = max(1, getLengthInMonths() / 2);
        } else if (roll == 2) {
            extension = 1;
        } else {
            return false;
        }

        campaign.addReport(GENERAL, String.format(
              "Due to the %s crisis your employer has invoked the emergency clause and extended the contract %d %s",
              warName,
              extension,
              ((extension == 1) ? " month" : " months")));
        setEndingDate(getEndingDate().plusMonths(extension));
        extensionLength += extension;

        // We spike morale to create a jump in contract difficulty - essentially the reason why the employer is using
        // the emergency clause.
        int moraleOrdinal = getMoraleLevel().ordinal();
        roll = d6(2) / 2;

        // we need to reset routEnd to null otherwise we'll attempt to rally
        if (getRoutEndDate() != null) {
            setRoutEndDate(null);
        }

        moraleOrdinal = min(moraleOrdinal + roll, OVERWHELMING.ordinal());
        setMoraleLevel(ContractMoraleLevel.values()[moraleOrdinal]);

        campaign.addReport(GENERAL, getMoraleLevel().getToolTipText());

        MekHQ.triggerEvent(new MissionChangedEvent(this));
        return true;
    }

    @Override
    public Money getMonthlyPayOut() {
        if (extensionLength == 0) {
            return super.getMonthlyPayOut();
        }
        /*
         * The transport clause and the advance monies have already been accounted for over the original length of
         * the contract. The extension uses the base monthly amounts for support and overhead, with a 50% bonus to
         * the base amount.
         */

        if (getLengthInMonths() <= 0) {
            return Money.zero();
        }

        return getBaseAmount().multipliedBy(1.5)
                     .plus(getSupportAmount())
                     .plus(getOverheadAmount())
                     .dividedBy(getLengthInMonths());
    }

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter printWriter, int indent) {
        // AbstractMission.writeToXMLBegin writes all shared fields (including those declared on AbstractMission
        // but historically serialized here). This override adds only the fields private to AtBContract.
        indent = super.writeToXMLBegin(campaign, printWriter, indent);

        if (parentContract != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "parentContractId", parentContract.getId());
        }

        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "extensionLength", extensionLength);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "playerMinorBreaches", playerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "employerMinorBreaches", employerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(printWriter,
              indent,
              "contractScoreArbitraryModifier",
              contractScoreArbitraryModifier);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "priorLogisticsFailure", priorLogisticsFailure);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "battleTypeMod", battleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "nextWeekBattleTypeMod", nextWeekBattleTypeMod);

        if (specialEventScenarioDate != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "specialEventScenarioDate", specialEventScenarioDate);
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "specialEventScenarioType", specialEventScenarioType);
        }

        return indent;
    }

    @Override
    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node node) throws ParseException {
        // AbstractMission.loadFieldsFromXmlNode handles all shared fields. This override adds only the fields
        // private to AtBContract.
        super.loadFieldsFromXmlNode(campaign, version, node);

        NodeList childNodes = node.getChildNodes();
        for (int x = 0; x < childNodes.getLength(); x++) {
            Node item = childNodes.item(x);

            try {
                if (item.getNodeName().equalsIgnoreCase("extensionLength")) {
                    extensionLength = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("playerMinorBreaches")) {
                    playerMinorBreaches = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("employerMinorBreaches")) {
                    employerMinorBreaches = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("contractScoreArbitraryModifier")) {
                    contractScoreArbitraryModifier = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("priorLogisticsFailure")) {
                    priorLogisticsFailure = Boolean.parseBoolean(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("battleTypeMod")) {
                    battleTypeMod = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("nextWeekBattleTypeMod")) {
                    nextWeekBattleTypeMod = Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equalsIgnoreCase("parentContractId")) {
                    parentContract = new AtBContractRef(Integer.parseInt(item.getTextContent()));
                } else if (item.getNodeName().equalsIgnoreCase("specialEventScenarioDate")) {
                    specialEventScenarioDate = MHQXMLUtility.parseDate(item.getTextContent().trim());
                } else if (item.getNodeName().equalsIgnoreCase("specialEventScenarioType")) {
                    specialEventScenarioType = Integer.parseInt(item.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        // Wire up the StratCon campaign state to this contract now that we have a typed reference.
        if (getStratConCampaignState() != null) {
            getStratConCampaignState().setContract(this);
        }

        // Create NPCs if they were not present in the save (e.g. older saves, or first load after feature addition).
        if (getEmployerLiaison() == null) {
            createEmployerLiaison(campaign);
        }
        if (getClanOpponent() == null && getEnemy().isClan()) {
            createClanOpponent(campaign);
        }
    }

    /**
     * Restores any references to other contracts.
     *
     * @param c The Campaign which holds this contract.
     */
    public void restore(Campaign c) {
        //        if (parentContract != null) {
        //            Mission m = c.getMission(parentContract.getId());
        //            if (m != null) {
        //                if (m instanceof AtBContract) {
        //                    setParentContract((AtBContract) m);
        //                } else {
        //                    logger.warn("Parent Contract reference #{} is not an AtBContract for contract {}",
        //                          parentContract.getId(),
        //                          getName());
        //                    setParentContract(null);
        //                }
        //            } else {
        //                logger.warn("Parent Contract #{} reference was not found for contract {}",
        //                      parentContract.getId(),
        //                      getName());
        //                setParentContract(null);
        //            }
        //        }
    }

    @Override
    public String getEmployerName(int year) {
        return isMercSubcontract() ?
                     "Mercenary (" + getEmployerFaction().getFullName(year) + ')' :
                     getEmployerFaction().getFullName(year);
    }

    public void addPlayerMinorBreach() {
        playerMinorBreaches++;
    }

    public void addPlayerMinorBreaches(int num) {
        playerMinorBreaches += num;
    }

    public void setContractScoreArbitraryModifier(int newModifier) {
        contractScoreArbitraryModifier = newModifier;
    }

    public int getBattleTypeMod() {
        return battleTypeMod + nextWeekBattleTypeMod;
    }

    @Override
    public void acceptContract(Campaign campaign) {
        if (campaign.getCampaignOptions().isUseStratCon()) {
            StratConContractDefinition stratconContractDefinition = getContractDefinition(getObjectiveType());
            if (stratconContractDefinition != null) {
                StratConContractInitializer.initializeCampaignState(this, campaign, stratconContractDefinition);
            }
        }

        // Announce that the contract is now fully initialized (its StratCon state, if any, now exists). addMission
        // already fired MissionNewEvent earlier, but that happens before this initialization, so listeners such as the
        // StratCon tab need this second signal to pick the contract up immediately rather than only after the next day.
        MekHQ.triggerEvent(new MissionChangedEvent(this));
    }

    public AtBContract(Contract contract, Campaign campaign) {
        this(contract.getName());

        setContractTypeName(contract.getContractTypeName());
        setSystemId(contract.getSystemId());
        setDescription(contract.getDescription());
        setStatus(contract.getStatus());
        for (Scenario s : contract.getScenarios()) {
            addScenario(s);
        }
        setId(contract.getId());
        setLengthInMonths(contract.getLengthInMonths());
        setStartDate(contract.getStartDate());
        /*
         * Set ending date; the other calculated values will be replaced
         * from the original contract
         */
        calculateContract(campaign);
        setPaymentMultiplier(contract.getPaymentMultiplier());
        setTransportCompensation(contract.getTransportCompensation());
        setStraightSupport(contract.getStraightSupport());
        setOverheadCompensation(contract.getOverheadCompensation());
        setCommandRights(contract.getCommandRights());
        setBattleLossCompensation(contract.getBattleLossCompensation());
        setSalvagePercent(contract.getSalvagePercent());
        setSalvageExchange(contract.isSalvageExchange());
        setSalvagedByUnit(contract.getSalvagedByUnit());
        setSalvagedByEmployer(contract.getSalvagedByEmployer());
        setSigningBonus(contract.getSigningBonus());
        setAdvancePercent(contract.getAdvancePercent());
        setPaidMRBCFee(contract.isPaidMRBCFee());
        setAdvanceAmount(contract.getAdvanceAmount());
        setFeeAmount(contract.getFeeAmount());
        setBaseAmount(contract.getBaseAmount());
        setOverheadAmount(contract.getOverheadAmount());
        setSupportAmount(contract.getSupportAmount());
        setTransportAmount(contract.getTransportAmount());
        setSigningBonusAmount(contract.getSigningBonusAmount());

        /* Guess at AtBContract values */
        ContractObjectiveType contractType = getContractObjectiveType(contract);
        setContractTypeAndName(contractType);

        Faction f = Factions.getInstance()
                          .getFactionFromFullNameAndYear(contract.getEmployerName(), campaign.getGameYear());
        if (null == f) {
            setEmployerCode("IND");
        } else {
            setEmployerCode(f.getShortName());
        }

        if (getObjectiveType().isPirateHunting()) {
            Faction employer = getEmployerFaction();
            setEnemyCode(employer.isClan() ? "BAN" : PIRATE_FACTION_CODE);
        } else if (getObjectiveType().isRiotDuty()) {
            setEnemyCode("REB");
        }

        setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign,
              contractType.isCadreDuty(), true, 1.0));
        setRequiredCombatElements(ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(campaign));

        setPartsAvailabilityLevel(getObjectiveType().calculatePartsAvailabilityLevel());

        int currentYear = campaign.getGameYear();
        setAllyBotName(getEmployerName(currentYear));
        setAllyCamouflage(pickRandomCamouflage(currentYear, getEmployerCode()));

        setEnemyBotName(generateEnemyName(currentYear));
        setEnemyCamouflage(pickRandomCamouflage(currentYear, getEnemyCode()));

        setContractDifficulty(calculateContractDifficulty(contract, contract.getStartDate().getYear(),
              true, campaign.getAllCombatEntities()));

        if (campaign.getCampaignOptions().isLimitClanTech()) {
            clanTechSalvageOverride();
        }
    }

    private static ContractObjectiveType getContractObjectiveType(Contract contract) {
        ContractObjectiveType contractType = null;
        for (final ContractObjectiveType type : ContractObjectiveType.values()) {
            if (type.toString().equalsIgnoreCase(contract.getContractTypeName())) {
                contractType = type;
                break;
            }
        }
        /* Make a rough guess */
        if (contractType == null) {
            if (contract.getLengthInMonths() <= 3) {
                contractType = ContractObjectiveType.OBJECTIVE_RAID;
            } else if (contract.getLengthInMonths() < 12) {
                contractType = ContractObjectiveType.GARRISON_DUTY;
            } else {
                contractType = ContractObjectiveType.PLANETARY_ASSAULT;
            }
        }
        return contractType;
    }

    /**
     * Applies a salvage override rule for Clan technology based on the contract timeline and faction involvement. This
     * method checks the factions of both the enemy and employer and determines if a salvage exchange should be forced
     * based on whether the battle occurs before the Battle of Tukayyid.
     *
     * <p>
     * This rule was implemented to better match canon employer behavior during this period.
     * </p>
     */
    public void clanTechSalvageOverride() {
        if (getEnemy().isClan() && !getEmployerFaction().isClan()) {
            if (getStartDate().isBefore(BATTLE_OF_TUKAYYID)) {
                setSalvageExchange(true);
            }
        }
    }

    /**
     * Represents a reference to another AtBContract.
     */
    protected static class AtBContractRef extends AtBContract {
        public AtBContractRef(int id) {
            setId(id);
        }
    }
}
