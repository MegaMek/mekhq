/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.contractMarket;

import static java.lang.Math.max;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.compute.Compute.d6;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_NETWORKER;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.randomEvents.GrayMonday.isGrayMonday;
import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Contract offers that are generated monthly under AtB rules.
 * <p>
 * Based on PersonnelMarket
 *
 * @author Neoancient
 */
public class AtbMonthlyContractMarket extends AbstractContractMarket {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtbMonthlyContractMarket";
    private static final MMLogger logger = MMLogger.create(AtbMonthlyContractMarket.class);

    private static final int COMSTAR_CO_OPT_CHANCE = 200;
    private static final int WOB_CO_OPT_CHANCE = 10;

    public AtbMonthlyContractMarket() {
        super(ContractMarketMethod.ATB_MONTHLY);
    }

    @Override
    public AtBContract addAtBContract(Campaign campaign) {
        AtBContract c = generateAtBContract(campaign, campaign.getAtBUnitRatingMod());
        if (c != null) {
            contracts.add(c);
        }
        return c;
    }

    @Override
    public void generateContractOffers(Campaign campaign, boolean newCampaign) {
        boolean isGrayMonday = isGrayMonday(campaign.getLocalDate(),
              campaign.getCampaignOptions().isSimulateGrayMonday());
        boolean hasActiveContract = campaign.hasActiveContract() || campaign.hasActiveAtBContract(true);

        if (((campaign.getLocalDate().getDayOfMonth() == 1)) || newCampaign) {
            // need to copy to prevent concurrent modification errors
            new ArrayList<>(contracts).forEach(this::removeContract);

            Person campaignCommander = campaign.getCommander();
            if (campaignCommander != null && !newCampaign) {
                if (campaignCommander.getConnections() > 0) {
                    campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                          "AtbMonthlyContractMarket.connectionsReport.normal"));
                } else {
                    campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                          "AtbMonthlyContractMarket.connectionsReport.none"));
                }
            }

            int unitRatingMod = campaign.getAtBUnitRatingMod();

            if (newCampaign) {
                // At this point in a campaign the unit rating would be NONE, however, we want the player to start
                // with access to plenty of contracts, so we temporarily bump them to REGULAR.
                unitRatingMod = REGULAR.getExperienceLevel();
            }

            for (AtBContract contract : campaign.getActiveAtBContracts()) {
                checkForSubcontracts(campaign, contract, unitRatingMod);

                if (!contracts.isEmpty() && hasActiveContract) {
                    updateReport(campaign);
                }
            }

            // If the player has an active contract, they will not be offered new contracts,
            // as MekHQ doesn't support multiple contracts (outside of subcontracts).
            if (hasActiveContract) {
                return;
            }

            Person negotiator = campaign.getSeniorAdminPerson(COMMAND);
            int negotiatorModifier = 0;
            if (negotiator != null) {
                PersonnelOptions options = negotiator.getOptions();
                if (options.booleanOption(ADMIN_NETWORKER)) {
                    negotiatorModifier++;
                }
            }

            int numContracts = d6() - 4 + unitRatingMod + negotiatorModifier;

            if (newCampaign) {
                // For a similar reason as previously stated, we want the user to be able to jump into the action off
                // the bat, so we give them extra contracts to start off.
                numContracts = d6() + (unitRatingMod * 2);
            }

            if (isGrayMonday) {
                for (int i = 0; i < numContracts; i++) {
                    if (d6() <= 2) {
                        numContracts--;
                    }
                }
            }

            if (numContracts == 0) {
                return;
            }

            Set<Faction> currentFactions = campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate());
            final boolean inMinorFaction = currentFactions.stream()
                                                 .noneMatch(faction -> faction.isISMajorOrSuperPower() ||
                                                                             faction.isClan());
            if (inMinorFaction) {
                numContracts--;
            }

            boolean inBackwater = true;
            if (currentFactions.size() > 1) {
                // More than one faction, if any is *not* periphery, we're not in backwater either
                for (Faction f : currentFactions) {
                    if (!f.isPeriphery()) {
                        inBackwater = false;
                    }
                }
            } else if (!currentFactions.isEmpty()) {
                // Just one faction. Are there any others nearby?
                Faction onlyFaction = currentFactions.iterator().next();
                if (!onlyFaction.isPeriphery()) {
                    for (PlanetarySystem key : Systems.getInstance()
                                                     .getNearbySystems(campaign.getCurrentSystem(), 30)) {
                        for (Faction f : key.getFactionSet(campaign.getLocalDate())) {
                            if (!onlyFaction.equals(f)) {
                                inBackwater = false;
                                break;
                            }
                        }
                        if (!inBackwater) {
                            break;
                        }
                    }
                }
            } else {
                logger.warn("Unable to find any factions around {} on {}",
                      campaign.getCurrentSystem().getName(campaign.getLocalDate()),
                      campaign.getLocalDate());
            }

            if (inBackwater) {
                numContracts--;
            }

            if (campaign.getFaction().isMercenary() || campaign.getFaction().isPirate()) {
                if (campaign.getCurrentSystem().isHiringHall(campaign.getLocalDate())) {
                    numContracts++;
                    /* Though the rules do not state these modifiers are mutually exclusive, the fact that the
                     * distance of Galatea from a border means that it has no advantage for Mercs over border
                     * worlds. Common sense dictates that worlds with hiring halls should not be
                     * subject to the -1 for backwater/interior.
                     */
                    if (inBackwater) {
                        numContracts++;
                    }
                }
            } else {
                /* Per IOps Beta, government units determine number of contracts as on a system with a great hall */
                numContracts++;
            }

            /*
             * If located on a faction's capital (interpreted as the starting planet for that faction),
             * generate one contract offer for that faction.
             */
            if (!campaign.isPirateCampaign()) {
                for (Faction f : campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate())) {
                    try {
                        if (f.getStartingPlanet(campaign.getLocalDate()).equals(campaign.getCurrentSystem().getId()) &&
                                  RandomFactionGenerator.getInstance().getEmployerSet().contains(f.getShortName())) {
                            AtBContract c = generateAtBContract(campaign, f.getShortName(), unitRatingMod);
                            if (c != null) {
                                contracts.add(c);
                                break;
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // no starting planet in current era; continue to next faction
                    }
                }
            }

            if (newCampaign) {
                numContracts = max(numContracts, 2);
            }

            for (int i = 0; i < numContracts; i++) {
                AtBContract c = generateAtBContract(campaign, unitRatingMod);
                if (c != null) {
                    contracts.add(c);
                }
            }
            updateReport(campaign);
        }
    }

    private void checkForSubcontracts(Campaign campaign, AtBContract contract, int unitRatingMod) {
        if (contract.getContractType().isGarrisonDuty()) {
            int numSubcontracts = 0;
            for (AtBContract c : campaign.getAtBContracts()) {
                if (contract.equals(c.getParentContract())) {
                    numSubcontracts++;
                }
            }
            for (int i = numSubcontracts; i < unitRatingMod - 1; i++) {
                int roll = d6(2);
                if (roll >= 10) {
                    AtBContract sub = generateAtBSubcontract(campaign, contract, unitRatingMod);
                    if (sub.getEndingDate().isBefore(contract.getEndingDate())) {
                        contracts.add(sub);
                    }
                }
            }
        }
    }

    /*
     * If no suitable planet can be found or no jump path to the planet can be
     * calculated after the indicated number of retries, this will return null.
     */
    private @Nullable AtBContract generateAtBContract(Campaign campaign, int unitRatingMod) {
        if (campaign.getFaction().isMercenary()) {
            if (null == campaign.getRetainerEmployerCode()) {
                int retries = MAXIMUM_GENERATION_RETRIES;
                AtBContract contract = null;
                while ((retries > 0) && (contract == null)) {
                    Faction employer = RandomFactionGenerator.getInstance().getEmployerFaction();
                    if (employer == null) {
                        retries--;
                        continue;
                    }

                    String employerCode = employer.getShortName();
                    // Send only 1 retry down because we're handling retries in our loop
                    contract = generateAtBContract(campaign, employerCode, unitRatingMod, 1);

                    // This try-catch is specifically implemented to make testing easier. Otherwise, we would need to
                    // define the player's TO&E, their Ally's unit availability, and their Enemy's unit availability,
                    // a RAT generator instance and a whole other pile of stuff. So instead, we let it fail, and if we
                    // need to specifically define difficulty in a unit test, we can do so by using
                    // contract.setDifficulty().
                    try {
                        if (contract != null) {
                            checkForEmployerOverride(campaign.getLocalDate(), contract, employerCode);
                            contract.setDifficulty(contract.calculateContractDifficulty(
                                  contract.getStartDate().getYear(),
                                  true,
                                  campaign.getAllCombatEntities()));
                        }
                    } catch (Exception e) {
                        contract.setDifficulty(5);
                        logger.error(e, "Unable to calculate difficulty for AtB contract {}", contract);
                    }
                    retries--;
                }
                return contract;
            } else {
                return generateAtBContract(campaign, campaign.getRetainerEmployerCode(), unitRatingMod);
            }
        } else {
            return generateAtBContract(campaign, campaign.getFaction().getShortName(), unitRatingMod);
        }
    }

    /**
     * Checks for special employer overrides on a contract.
     *
     * <p>There is a small randomized chance that specific factions (ComStar or Word of Blake) may co-opt a contract
     * and become its employer, depending on the date and current employer.</p>
     * <ul>
     *   <li>If ComStar is active and the chance check passes, ComStar immediately becomes the employer.</li>
     *   <li>If during the Jihad era, the Word of Blake is active, and not already the employer,
     *       there is a randomized chance that Word of Blake becomes the employer.</li>
     *   <li>If neither condition is met, the employer remains unchanged.</li>
     * </ul>
     *
     * @param today        the current date in the campaign
     * @param contract     the contract that may be overridden
     * @param employerCode the faction code of the current employer
     */
    private static void checkForEmployerOverride(LocalDate today, AtBContract contract, String employerCode) {
        // 1. ComStar co-opting check
        Faction comStar = Factions.getInstance().getFaction(COMSTAR_FACTION_CODE);
        if (comStar.validIn(today) && Compute.randomInt(COMSTAR_CO_OPT_CHANCE) == 0) {
            contract.setEmployerCode(COMSTAR_FACTION_CODE, today.getYear());
            return;
        }

        // 2. Word of Blake co-opting during Jihad period
        Faction wordOfBlake = Factions.getInstance().getFaction("WOB");
        boolean isDuringJihad = !today.isBefore(MHQConstants.JIHAD_START) &&
                                      !today.isAfter(MHQConstants.NOMINAL_JIHAD_END);

        if (isDuringJihad
                  && wordOfBlake.validIn(today)
                  && !Objects.equals("WOB", employerCode)
                  && Compute.randomInt(WOB_CO_OPT_CHANCE) == 0) {
            contract.setEmployerCode("WOB", today.getYear());
        }
    }

    private @Nullable AtBContract generateAtBContract(Campaign campaign, @Nullable String employer, int unitRatingMod) {
        return generateAtBContract(campaign, employer, unitRatingMod, MAXIMUM_GENERATION_RETRIES);
    }

    private @Nullable AtBContract generateAtBContract(Campaign campaign, @Nullable String employer, int unitRatingMod,
          int retries) {
        if (employer == null) {
            logger.warn("Could not generate an AtB Contract because there was no employer!");
            return null;
        } else if (retries <= 0) {
            logger.warn("Could not generate an AtB Contract because we ran out of retries!");
            return null;
        }

        AtBContract contract = new AtBContract("UnnamedContract");
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        if (Factions.getInstance().getFaction(employer).isMercenary()) {
            contract.setMercSubcontract(true);
            for (int attempts = 0; attempts < MAXIMUM_ATTEMPTS_TO_FIND_NON_MERC_EMPLOYER; ++attempts) {
                employer = RandomFactionGenerator.getInstance().getEmployerFaction().getShortName();
                if ((employer != null) && !Factions.getInstance().getFaction(employer).isMercenary()) {
                    break;
                }
            }

            if ((employer == null) || Factions.getInstance().getFaction(employer).isMercenary()) {
                logger.warn("Could not generate an AtB Contract because we could not find a non-MERC employer!");
                return null;
            }
        }
        contract.setEmployerCode(employer, campaign.getGameYear());

        getContractType(campaign, contract);

        setEnemyCode(contract);
        setIsRiotDuty(contract);

        /*
         * Addition to AtB rules: factions which are generally neutral
         * (ComStar, Mercs not under contract) are more likely to have garrison-type
         * contracts and less likely to have battle-type contracts unless at war.
         */
        if (RandomFactionGenerator.getInstance()
                  .getFactionHints()
                  .isNeutral(Factions.getInstance().getFaction(employer)) &&
                  !RandomFactionGenerator.getInstance()
                         .getFactionHints()
                         .isAtWarWith(Factions.getInstance().getFaction(employer),
                               Factions.getInstance().getFaction(contract.getEnemyCode()),
                               campaign.getLocalDate())) {
            if (contract.getContractType().isPlanetaryAssault()) {
                contract.setContractType(AtBContractType.GARRISON_DUTY);
            } else if (contract.getContractType().isReliefDuty()) {
                contract.setContractType(AtBContractType.SECURITY_DUTY);
            }
        }
        setAttacker(contract);
        try {
            setSystemId(contract);
        } catch (NoContractLocationFoundException ex) {
            return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
        }
        JumpPath jp = null;
        try {
            jp = contract.getJumpPath(campaign);
        } catch (NullPointerException ex) {
            // could not calculate jump path; leave jp null
            logger.warn("Could not calculate jump path to contract location: {}",
                  contract.getSystem().getName(campaign.getLocalDate()));
        }

        if (jp == null) {
            return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
        }

        final ReputationController reputation = campaign.getReputation();
        final SkillLevel campaignSkillLevel = reputation == null ? REGULAR : reputation.getAverageSkillLevel();
        final boolean useDynamicDifficulty = campaign.getCampaignOptions().isUseDynamicDifficulty();
        setAllyRating(contract, campaign.getGameYear(), useDynamicDifficulty ? campaignSkillLevel : REGULAR);
        setEnemyRating(contract, campaign.getGameYear(), useDynamicDifficulty ? campaignSkillLevel : REGULAR);

        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }

        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        setContractClauses(contract, unitRatingMod, campaign);

        double varianceFactor = ContractUtilities.calculateVarianceFactor();
        contract.setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign,
              contract.getContractType().isCadreDuty(), false, varianceFactor));
        contract.setRequiredCombatElements(calculateRequiredCombatElements(campaign, contract, false, varianceFactor));
        contract.setMultiplier(calculatePaymentMultiplier(campaign, contract));

        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());

        contract.initContractDetails(campaign);
        contract.calculateContract(campaign);

        contract.setName(String.format("%s - %s - %s %s",
              contract.getStartDate()
                    .format(DateTimeFormatter.ofPattern("yyyy").withLocale(MekHQ.getMHQOptions().getDateLocale())),
              employer,
              contract.getSystem().getName(contract.getStartDate()),
              contract.getContractType()));

        contract.clanTechSalvageOverride();

        return contract;
    }

    protected AtBContract generateAtBSubcontract(Campaign campaign, AtBContract parent, int unitRatingMod) {
        AtBContract contract = new AtBContract("New Subcontract");
        contract.setEmployerCode(parent.getEmployerCode(), campaign.getGameYear());
        getContractType(campaign, contract);

        if (contract.getContractType().isPirateHunting()) {
            Faction employer = contract.getEmployerFaction();
            contract.setEnemyCode(employer.isClan() ? "BAN" : PIRATE_FACTION_CODE);
        } else if (contract.getContractType().isRiotDuty()) {
            contract.setEnemyCode("REB");
        } else {
            contract.setEnemyCode(RandomFactionGenerator.getInstance()
                                        .getEnemy(contract.getEmployerCode(),
                                              contract.getContractType().isGarrisonType()));
        }
        if (contract.getContractType().isGarrisonDuty() && contract.getEnemy().isRebel()) {
            contract.setContractType(AtBContractType.RIOT_DUTY);
        }

        contract.setParentContract(parent);
        contract.initContractDetails(campaign);
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        /*
         * The AtB rules say to roll the enemy, but also that the subcontract
         * takes place in the same planet/sector. Rebels and pirates can
         * appear anywhere, but others should be limited to what's within a
         * jump.
         */

        // TODO : When MekHQ gets the capability of splitting the unit to different
        // locations, this restriction can be lessened or lifted.
        if (!contract.getEnemy().isRebelOrPirate()) {
            boolean factionValid = false;
            for (PlanetarySystem p : Systems.getInstance().getNearbySystems(campaign.getCurrentSystem(), 30)) {
                if (factionValid) {
                    break;
                }

                for (Faction f : p.getFactionSet(campaign.getLocalDate())) {
                    if (f.getShortName().equals(contract.getEnemyCode())) {
                        factionValid = true;
                        break;
                    }
                }
            }
            if (!factionValid) {
                contract.setEnemyCode(parent.getEnemyCode());
            }
        }

        setAttacker(contract);
        contract.setSystemId(parent.getSystemId());
        setAllyRating(contract, campaign.getGameYear(), campaign.getReputation().getAverageSkillLevel());
        setEnemyRating(contract, campaign.getGameYear(), campaign.getReputation().getAverageSkillLevel());

        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }
        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());

        contract.setCommandRights(ContractCommandRights.values()[max(parent.getCommandRights().ordinal() - 1, 0)]);
        contract.setSalvageExchange(parent.isSalvageExchange());
        contract.setSalvagePct(max(parent.getSalvagePct() - 10, 0));
        contract.setStraightSupport(max(parent.getStraightSupport() - 20, 0));
        if (parent.getBattleLossComp() <= 10) {
            contract.setBattleLossComp(0);
        } else if (parent.getBattleLossComp() <= 20) {
            contract.setBattleLossComp(10);
        } else {
            contract.setBattleLossComp(parent.getBattleLossComp() - 20);
        }
        contract.setTransportComp(100);

        double varianceFactor = ContractUtilities.calculateVarianceFactor();
        contract.setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign,
              contract.getContractType().isCadreDuty(), false, varianceFactor));
        contract.setRequiredCombatElements(calculateRequiredCombatElements(campaign, contract, false, varianceFactor));

        contract.setMultiplier(calculatePaymentMultiplier(campaign, contract));
        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());
        contract.calculateContract(campaign);

        contract.setName(String.format("%s - %s - %s Subcontract %s",
              contract.getStartDate()
                    .format(DateTimeFormatter.ofPattern("yyyy").withLocale(MekHQ.getMHQOptions().getDateLocale())),
              contract.getEmployer(),
              contract.getSystem().getName(parent.getStartDate()),
              contract.getContractType()));

        contract.clanTechSalvageOverride();

        return contract;
    }

    /**
     * Determines and sets the contract type for a new AtB contract through negotiation.
     *
     * <p>This method performs a negotiation skill check using the campaign commander's negotiation skill and
     * connections. The margin of success from this check, combined with the commander's connections rating, influences
     * which contract types are available from the employer. The negotiation results are added to the campaign
     * report.</p>
     *
     * @param campaign the current campaign
     * @param contract the AtB contract to assign a type to
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void getContractType(Campaign campaign, AtBContract contract) {
        Person campaignCommander = campaign.getFlaggedCommander();

        int connections = campaignCommander != null ? campaignCommander.getConnections() : 0;

        boolean isUseAgingEffects = campaign.getCampaignOptions().isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();
        SkillCheckUtility checkUtility = new SkillCheckUtility(campaignCommander, S_NEGOTIATION, null,
              0, false, true, isUseAgingEffects, isClanCampaign, campaign.getLocalDate());

        campaign.addReport(checkUtility.getResultsText());
        contract.setContractType(ContractTypePicker.findMissionType(contract.getEmployerFaction(), connections,
              max(0, checkUtility.getMarginOfSuccess())));
    }

    /**
     * Creates and adds a follow-up contract based on the just concluded contract.
     * <p>
     * This method generates a new contract (`AtBContract`) as a follow-up to the provided `contract`. Certain
     * properties of the original contract, such as employer, enemy, skill, system location, and other details, are
     * carried over or modified as necessary based on the contract type. The method ensures that the follow-up contract
     * contains all necessary details and is correctly initialized.
     * </p>
     *
     * <p>
     * <b>Order Dependency:</b> The operations in this method must match the order specified in
     * `generateAtBContract` to maintain compatibility and consistency.
     * </p>
     *
     * @param campaign the {@link Campaign} to which the follow-up contract belongs. This is used for retrieving
     *                 campaign-wide settings and applying modifiers.
     * @param contract the {@link AtBContract} that serves as the base for generating the follow-up contract. Key
     *                 details from this contract are reused or adapted for the follow-up.
     */
    private void addFollowup(Campaign campaign, AtBContract contract) {
        if (followupContracts.containsValue(contract.getId())) {
            return;
        }

        // The order in this method needs to match generateAtBContract

        AtBContract followup = new AtBContract("Followup Contract");
        lastId++;
        followup.setId(lastId);
        contractIds.put(lastId, followup);

        followup.setEmployerCode(contract.getEmployerCode(), campaign.getGameYear());
        switch (contract.getContractType()) {
            case DIVERSIONARY_RAID:
                followup.setContractType(AtBContractType.OBJECTIVE_RAID);
                break;
            case RECON_RAID:
                followup.setContractType(AtBContractType.PLANETARY_ASSAULT);
                break;
            case RIOT_DUTY:
                followup.setContractType(AtBContractType.GARRISON_DUTY);
                break;
            default:
                break;
        }

        followup.setEnemyCode(contract.getEnemyCode());
        followup.setEnemySkill(contract.getEnemySkill());
        followup.setEnemyQuality(contract.getEnemyQuality());
        setAttacker(followup);
        followup.setSystemId(contract.getSystemId());
        followup.setAllySkill(contract.getAllySkill());
        followup.setAllyQuality(contract.getAllyQuality());
        followup.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        setContractClauses(followup, campaign.getAtBUnitRatingMod(), campaign);

        double varianceFactor = ContractUtilities.calculateVarianceFactor();
        followup.setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign,
              followup.getContractType().isCadreDuty(), false, varianceFactor));
        followup.setRequiredCombatElements(calculateRequiredCombatElements(campaign, followup, false, varianceFactor));

        followup.setMultiplier(calculatePaymentMultiplier(campaign, followup));
        followup.setPartsAvailabilityLevel(followup.getContractType().calculatePartsAvailabilityLevel());
        followup.initContractDetails(campaign);
        followup.calculateContract(campaign);

        contract.clanTechSalvageOverride();

        contracts.add(followup);
        followupContracts.put(followup.getId(), contract.getId());
    }

    @Override
    public double calculatePaymentMultiplier(Campaign campaign, AtBContract contract) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        double multiplier = 1.0;

        // Operations tempo
        multiplier *= contract.getContractType().getOperationsTempoMultiplier();

        // Employer multiplier
        final Faction employer = Factions.getInstance().getFaction(contract.getEmployerCode());
        final Faction enemy = contract.getEnemy();
        if (employer.isISMajorOrSuperPower() || employer.isClan()) {
            multiplier *= 1.2;
        } else if (enemy.isIndependent()) {
            multiplier *= 1.0;
        } else {
            multiplier *= 1.1;
        }

        // Reputation multiplier
        if (campaignOptions.getUnitRatingMethod().isCampaignOperations()) {
            double reputationFactor = campaign.getReputation().getReputationFactor();

            if (campaignOptions.isClampReputationPayMultiplier()) {
                reputationFactor = clamp(reputationFactor, 0.5, 2.0);
            }

            multiplier *= reputationFactor;
        } else {
            int unitRatingMod = campaign.getAtBUnitRatingMod();
            if (unitRatingMod >= IUnitRating.DRAGOON_A) {
                multiplier *= 2.0;
            } else if (unitRatingMod == IUnitRating.DRAGOON_B) {
                multiplier *= 1.5;
            } else if (unitRatingMod == IUnitRating.DRAGOON_D) {
                multiplier *= 0.8;
            } else if (unitRatingMod == IUnitRating.DRAGOON_F) {
                multiplier *= 0.5;
            }
        }

        if (campaignOptions.isUseFactionStandingContractPaySafe()) {
            FactionStandings factionStandings = campaign.getFactionStandings();
            double regard = factionStandings.getRegardForFaction(employer.getShortName(), true);
            multiplier *= FactionStandingUtilities.getContractPayMultiplier(regard);
        }

        // FG3 Difficulty Multiplier
        if (campaign.getLocalDate().isBefore(BATTLE_OF_TUKAYYID)
                  && !employer.isClan()
                  && enemy.isClan()) {
            multiplier *= 0.5;
        } else if (campaignOptions.isUseGenericBattleValue()) {
            int contractDifficulty = contract.getDifficulty();
            if (contractDifficulty != Integer.MIN_VALUE && contractDifficulty <= 2) {
                multiplier /= 0.5;
            } else if (contractDifficulty >= 8) {
                multiplier *= 0.5;
            } else if (contractDifficulty >= 6) {
                multiplier *= 0.25;
            }
        }

        // This should always be last
        if (isGrayMonday(campaign.getLocalDate(), campaign.getCampaignOptions().isSimulateGrayMonday())) {
            multiplier *= 0.25;
        }

        return multiplier;
    }

    @Override
    public void checkForFollowup(Campaign campaign, AtBContract contract) {
        AtBContractType type = contract.getContractType();
        if (type.isDiversionaryRaid() || type.isReconRaid() || type.isRiotDuty()) {
            int roll = d6();
            if (roll == 6) {
                addFollowup(campaign, contract);
                campaign.addReport(
                      "Your employer has offered a follow-up contract (available on the <a href=\"CONTRACT_MARKET\">contract market</a>).");
            }
        }
    }

    /**
     * Computes and applies modifiers to the clauses of a contract based on various campaign, contract, and personnel
     * factors.
     *
     * <p>This method sets up and calculates the negotiation modifiers for each contract clause, such as command,
     * salvage, transport, and support. It takes into account the experience level of the best administrators in
     * relevant roles, campaign options (such as age effects, faction type, unit rating, and size limitations), contract
     * specifics, enemy faction characteristics, mission type, and employer details. Clause modifiers are further
     * adjusted for special circumstances like government or retainer contracts, high-performing units, and employer
     * faction type.</p>
     *
     * <p>After all modifiers are applied for the contract, the method triggers the resolution of each contract clause
     * (command, salvage, support, transport) using the final calculated modifiers.</p>
     *
     * @param contract      the {@link AtBContract} for which clauses and modifiers are being set
     * @param unitRatingMod the current unit rating modifier to be used in clause calculations
     * @param campaign      the {@link Campaign} in which the contract negotiation is taking place
     */
    private void setContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {
        ClauseMods mods = new ClauseMods();
        clauseMods.put(contract.getId(), mods);

        boolean isPirateCampaign = campaign.isPirateCampaign();
        if (isPirateCampaign) {
            contract.setCommandRights(ContractCommandRights.INDEPENDENT);
            contract.setSalvageExchange(false);
            contract.setSalvagePct(100);
            contract.setTransportComp(0);
            contract.setStraightSupport(0);
            contract.setBattleLossComp(0);
            return;
        }

        /*
         * AtB rules seem to indicate one admin in each role (though this
         * is not explicitly stated that I have seen) but MekHQ allows
         * assignment of multiple admins to each role. Therefore, we go
         * through all the admins and for each role select the one with
         * the highest admin skill, or higher negotiation if the admin
         * skills are equal.
         */
        Person adminCommand = campaign.getSeniorAdminPerson(COMMAND);
        Person adminTransport = campaign.getSeniorAdminPerson(TRANSPORT);
        Person adminLogistics = campaign.getSeniorAdminPerson(LOGISTICS);

        boolean isUseAgeEffects = campaign.getCampaignOptions().isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();

        int adminCommandExp = SkillType.EXP_NONE;
        if (adminCommand != null) {
            Skill skill = adminCommand.getSkill(S_NEGOTIATION);
            if (skill != null) {
                SkillModifierData skillModifierData = adminCommand.getSkillModifierData(isUseAgeEffects,
                      isClanCampaign, today);
                adminCommandExp = skill.getExperienceLevel(skillModifierData);
            }
        }
        int adminTransportExp = SkillType.EXP_NONE;
        if (adminTransport != null) {
            Skill skill = adminTransport.getSkill(S_NEGOTIATION);
            if (skill != null) {
                SkillModifierData skillModifierData = adminTransport.getSkillModifierData(isUseAgeEffects,
                      isClanCampaign, today);

                adminTransportExp = skill.getExperienceLevel(skillModifierData);
            }
        }
        int adminLogisticsExp = SkillType.EXP_NONE;
        if (adminLogistics != null) {
            Skill skill = adminLogistics.getSkill(S_NEGOTIATION);
            if (skill != null) {
                SkillModifierData skillModifierData = adminLogistics.getSkillModifierData(isUseAgeEffects,
                      isClanCampaign, today);

                adminLogisticsExp = skill.getExperienceLevel(skillModifierData);
            }
        }

        /* Treat government units like merc units that have a retainer contract */
        if ((!campaign.getFaction().isMercenary() && !campaign.getFaction().isPirate()) ||
                  (null != campaign.getRetainerEmployerCode())) {
            for (int i = 0; i < CLAUSE_NUM; i++) {
                mods.mods[i]++;
            }
        }

        if (campaign.getCampaignOptions().isMercSizeLimited() && campaign.getFaction().isMercenary()) {
            int max = (unitRatingMod + 1) * 12;
            int numMods = (ContractUtilities.getEffectiveNumUnits(campaign) - max) / 2;
            while (numMods > 0) {
                mods.mods[Compute.randomInt(4)]--;
                numMods--;
            }
        }

        mods.mods[CLAUSE_COMMAND] = adminCommandExp - SkillType.EXP_REGULAR;
        mods.mods[CLAUSE_SALVAGE] = adminLogisticsExp - SkillType.EXP_REGULAR;
        mods.mods[CLAUSE_TRANSPORT] = adminTransportExp - SkillType.EXP_REGULAR;
        mods.mods[CLAUSE_SUPPORT] = adminLogisticsExp - SkillType.EXP_REGULAR;
        if (unitRatingMod >= IUnitRating.DRAGOON_A) {
            mods.mods[Compute.randomInt(4)] += 2;
            mods.mods[Compute.randomInt(4)] += 2;
        } else if (unitRatingMod == IUnitRating.DRAGOON_B) {
            mods.mods[Compute.randomInt(4)] += 1;
            mods.mods[Compute.randomInt(4)] += 1;
        } else if (unitRatingMod == IUnitRating.DRAGOON_C) {
            mods.mods[Compute.randomInt(4)] += 1;
        } else if (unitRatingMod <= IUnitRating.DRAGOON_F) {
            mods.mods[Compute.randomInt(4)] -= 1;
        }

        if (Factions.getInstance().getFaction(contract.getEnemyCode()).isClan() &&
                  !Factions.getInstance().getFaction(contract.getEmployerCode()).isClan()) {
            for (int i = 0; i < mods.mods.length; i++) {
                if (i == CLAUSE_SALVAGE) {
                    mods.mods[i] -= 2;
                } else {
                    mods.mods[i] += 1;
                }
            }
        } else {
            if (contract.getEnemySkill().isVeteranOrGreater()) {
                mods.mods[Compute.randomInt(4)] += 1;
            }

            if (contract.getEnemySkill().isEliteOrGreater()) {
                mods.mods[Compute.randomInt(4)] += 1;
            }
        }

        if (campaign.getCampaignOptions().isUseFactionStandingNegotiationSafe()) {
            FactionStandings standings = campaign.getFactionStandings();
            double regard = standings.getRegardForFaction(contract.getEmployerCode(), true);
            int negotiationModifier = FactionStandingUtilities.getNegotiationModifier(regard);
            for (int i = 0; i < mods.mods.length; i++) {
                mods.mods[i] += negotiationModifier;
            }
        }

        int[][] missionMods = { { 1, 0, 1, 0 }, { 0, 1, -1, -3 }, { -3, 0, 2, 1 }, { -2, 1, -1, -1 }, { -2, 0, 2, 3 },
                                { -1, 1, 1, 1 }, { -2, 3, -2, -1 }, { 2, 2, -1, -1 }, { 0, 2, 2, 1 }, { -1, 0, 1, 2 },
                                { -1, -2, 1, -1 }, { -1, -1, 2, 1 }, { 2, 1, -1, -3 }, { -1, 4, -3, -2 },
                                { -3, 0, 2, 1 }, { -1, -2, 1, -1 }, { -2, 0, 2, 1 }, { -1, 4, -3, -2 },
                                { 2, 1, -1, -1 } };
        for (int i = 0; i < mods.mods.length; i++) {
            mods.mods[i] += missionMods[contract.getContractType().ordinal()][i];
        }

        Faction employerFaction = contract.getEmployerFaction();

        if (employerFaction.isISMajorOrSuperPower()) {
            mods.mods[CLAUSE_SALVAGE] -= 1;
            mods.mods[CLAUSE_TRANSPORT] += 1;
        } else if (employerFaction.isMinorPower()) {
            mods.mods[CLAUSE_SALVAGE] -= 2;
        } else if (employerFaction.isMercenary()) {
            mods.mods[CLAUSE_COMMAND] -= 1;
            mods.mods[CLAUSE_SALVAGE] += 2;
            mods.mods[CLAUSE_SUPPORT] += 1;
            mods.mods[CLAUSE_TRANSPORT] += 1;
        } else if (employerFaction.getShortName().equals("IND")) {
            mods.mods[CLAUSE_SALVAGE] -= 1;
            mods.mods[CLAUSE_SUPPORT] -= 1;
        }

        int modifier = getEmployerNegotiatorModifier(employerFaction);
        mods.mods[CLAUSE_COMMAND] -= modifier;
        mods.mods[CLAUSE_SALVAGE] -= modifier;
        mods.mods[CLAUSE_SUPPORT] -= modifier;
        mods.mods[CLAUSE_TRANSPORT] -= modifier;

        rollCommandClause(contract, mods.mods[CLAUSE_COMMAND], campaign.getFaction().isMercenary());
        rollSalvageClause(contract,
              mods.mods[CLAUSE_SALVAGE],
              campaign.getCampaignOptions().getContractMaxSalvagePercentage());
        rollSupportClause(contract, mods.mods[CLAUSE_SUPPORT]);
        rollTransportClause(contract, mods.mods[CLAUSE_TRANSPORT]);
    }

    /**
     * Calculates the negotiation modifier for a contract based on the employer's faction type. The modifier reflects
     * the negotiation capabilities of the employer, making it harder to achieve favorable results with more influential
     * or powerful employers.
     *
     * <p>The negotiation modifier is determined as follows:
     * <ul>
     *   <li>Default: A "Green" modifier is used for most employers.</li>
     *   <li>Major or superpower faction: A "Regular" modifier is applied.</li>
     *   <li>Clan faction: A "Veteran" modifier is applied.</li>
     *   <li>ComStar or Word of Blake (WoB): An "Elite" modifier is applied.</li>
     * </ul>
     *
     * @param employerFaction The {@link Faction} that is performing the negotiation.
     *
     * @return An integer representing the negotiation modifier corresponding to the employer's capabilities.
     */
    private static int getEmployerNegotiatorModifier(Faction employerFaction) {
        if (employerFaction.isMajorOrSuperPower()) {
            return REGULAR.getExperienceLevel();
        } else if (employerFaction.isClan()) {
            return VETERAN.getExperienceLevel();
        } else if (employerFaction.isComStarOrWoB()) {
            return ELITE.getExperienceLevel();
        }

        return GREEN.getExperienceLevel();
    }
}
