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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.mission.utilities.RandomFactionCamouflage.pickRandomCamouflage;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.utilities.ContractCharacteristics;
import mekhq.campaign.mission.contract.utilities.MHQMorale;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class AbstractContractGeneration {
    private static final MMLogger LOGGER = MMLogger.create(AbstractContractGeneration.class);
    private static final String RESOURCE_BUNDLES = "mekhq.resources.AbstractMission";

    /** How many times {@link #pickEnemy} will redraw to avoid an employer-versus-itself contract before giving up. */
    private static final int MAX_ENEMY_REDRAWS = 5;

    /** Auto intel obfuscation hides each obfuscatable field with a 1-in-this chance (so ~1 field hidden on average). */
    private static final int INTEL_OBFUSCATION_ODDS = 4;
    /** Each contract term independently has a 1-in-this chance of being locked as non-negotiable at generation. */
    private static final int NON_NEGOTIABLE_TERM_ODDS = 4;
    /** A covert-candidate objective has a 1-in-this chance of the contract actually being run as a covert operation. */
    private static final int COVERT_CONTRACT_ODDS = 6;
    /** A covert contract has a 1-in-this chance of being a false flag operation, run under a cover-story front. */
    private static final int FALSE_FLAG_ODDS = 6;
    /**
     * The steeper false flag odds (1-in-this) used when a covert contract's true backer is ComStar or the Word of
     * Blake, for whom deniable false flag operations are a signature tactic.
     */
    private static final int COMSTAR_WOB_FALSE_FLAG_ODDS = 2;

    public static @Nullable AbstractContract createContract(Campaign campaign, CampaignOptions campaignOptions,
          LocalDate currentDate, Detachment detachment, int contractGenerationModifier, ContractSearchType searchType,
          FactionStandings factionStandings, boolean overridingCommandCircuitRequirements, boolean isGM,
          boolean provingGround) {
        final ChaosContract contract = new ChaosContract();
        // Inject the options so the contract's term getters apply the configured per-term multipliers even while the
        // offer is still in the market (before it is accepted and registered on the campaign).
        contract.setCampaignOptions(campaignOptions);
        if (provingGround) {
            contract.setNature(ContractNature.PROVING_GROUND);
        }

        // Step 1: Type. Resolved before the employer so the employer step knows whether the contract is covert-viable
        ContractObjectiveData objectiveData = pickObjective(contractGenerationModifier, contract);
        ChaosObjectiveType chaosObjectiveType = objectiveData.playerObjectiveType().getChaosObjectiveType();
        boolean isDefensiveObjective = !chaosObjectiveType.isAttacker();

        // Step 2: Employer
        AbstractLocation currentLocation = detachment.getCurrentLocation();
        EmployerData employerData = pickEmployer(campaign, currentDate, currentLocation, searchType,
              chaosObjectiveType.isCovertCandidate(), contract);
        if (employerData == null) {
            return null;
        }

        // Covert status - needs both the objective and the now-resolved employer/sponsor (its odds depend on the true
        // backer). Must be set before the enemy is picked, which draws under covert rules when this is true.
        determineCovertStatus(chaosObjectiveType, contract);

        // Step 3: Scale & Intensity
        setAncillaryValues(campaign, detachment.getHangar(), contract);

        // Step 3: Enemy (situated against the employer's territorial anchor faction, so a landless flavor employer -
        // rebels, a mercenary command, a corporation - still yields a sensible nearby belligerent)
        EnemyData enemyData = pickEnemy(campaign, currentDate, currentLocation, employerData, objectiveData, contract);

        // Step 4: Location
        SystemsTargetData systemsTargetData = pickTargetLocation(currentLocation,
              currentDate,
              objectiveData,
              employerData,
              enemyData,
              contract,
              isDefensiveObjective,
              employerData.type().isCurrentSystemEmployer());
        if (systemsTargetData == null) {
            // No valid target planet means nowhere to situate the contract.
            return null;
        }

        // Step 5: Force Ratings (skill and equipment of both sides)
        PlayerForce playerForce = campaign.getPlayerForce();
        PlanetarySystem targetSystem = Systems.getInstance().getSystemById(systemsTargetData.systemId());
        Planet targetPlanet = targetSystem == null ? null : targetSystem.getPlanetById(systemsTargetData.planetId());
        setForceRatings(playerForce,
              campaignOptions,
              currentDate,
              detachment,
              targetPlanet,
              employerData,
              enemyData,
              objectiveData,
              contract,
              provingGround);

        // Step 6: Length
        boolean useVariableContractLength = campaignOptions.get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
        determineSchedule(campaign,
              useVariableContractLength,
              currentDate,
              currentLocation.getCurrentSystem(),
              targetPlanet,
              factionStandings,
              overridingCommandCircuitRequirements,
              isGM,
              targetSystem,
              employerData.factionCode(),
              chaosObjectiveType,
              contract);

        // Step 7: Initial Terms
        boolean useFactionModifiers = campaignOptions.get(CampaignOption.USE_CONTRACT_FACTION_MODIFIERS);
        setContractTerms(chaosObjectiveType, employerData.type(), employerData.getFaction(), useFactionModifiers,
              contract);
        if (campaignOptions.get(CampaignOption.USE_NON_NEGOTIABLE_TERMS)) {
            lockNonNegotiableTerms(contract);
        }

        // Step 9: Final Tasks
        performFinalTasks(campaign, currentDate, contract, currentLocation);

        // Rolled now that pay, length and objectives are set, so the generation-time effects bake onto their final
        // base values. Lifecycle characteristics (negotiator, bonuses, standing) are stored and applied later at
        // their own hooks.
        ContractCharacteristics.rollAndApply(contract, campaign);

        // Step 10: Difficulty estimate (cached; enemy force and skill are finalized by this point)
        contract.setCachedContractDifficulty(
              ChaosContractDeterminationDifficulty.calculateContractDifficulty(campaign, contract));

        // Step 11: Return the Contract
        return contract;
    }

    private static void performFinalTasks(Campaign campaign, LocalDate currentDate, ChaosContract contract,
          AbstractLocation currentLocation) {
        // Basic Information
        UUID contractId = UUID.randomUUID();
        contract.setContractId(contractId);
        contract.setStatus(null);

        // A false flag operation is run under a cover story: swap the visible employer to a front faction and preserve
        // the real employer as the covert sponsor. Done now that the real employer and enemy are known, so everything
        // downstream - the name, the market, the briefing, the allied forces - sees the front, while Faction Standing
        // still accrues to the real employer through the sponsor.
        if (contract.isFalseFlag()) {
            applyFalseFlagCover(contract, currentLocation, currentDate);
        }

        // Contract Details. A plain covert operation keeps the true employer out of the title (getEmployerMarketDisplayName
        // returns the "Undisclosed Employer" placeholder); a false flag already shows its front as the visible employer.
        String employerName = contract.isCovert()
                                    ? contract.getEmployerMarketDisplayName()
                                    : contract.getEmployerDisplayName();
        String contractName;
        if (campaign.getCampaignOptions().get(CampaignOption.USE_OPERATION_CODENAMES)) {
            contractName = getFormattedTextAt(RESOURCE_BUNDLES,
                  "AbstractContractGeneration.operationContractName",
                  contract.getStartDate(),
                  OperationNameController.generateOperationName(contract.getObjectiveType()));
        } else {
            contractName = getFormattedTextAt(RESOURCE_BUNDLES,
                  "AbstractContractGeneration.contractName",
                  contract.getStartDate(),
                  contract.getObjectiveType().toString(),
                  employerName,
                  contract.getEnemyDisplayName());
        }
        contract.setContractName(contractName);

        // Morale
        MHQMorale.determineStartingMorale(contract, campaign);

        // Rented Facilities
        RentedFacilitiesData rentedFacilitiesData = new RentedFacilitiesData(0, 0, 0);
        contract.setRentedFacilitiesData(rentedFacilitiesData);

        // StratCon
        StratConCampaignState stratConCampaignState = new StratConCampaignState();
        contract.setStratConCampaignState(stratConCampaignState);

        // Pay - the default Chaos Campaign scheme, or the CamOps force-value scheme when the campaign opts into it.
        AbstractContractDeterminationPay.forCampaign(campaign)
              .determineContractPay(campaign, currentDate, contract, currentLocation);

        // Intel Obfuscation - optionally hide some market-offer intel from the player.
        applyIntelObfuscation(campaign, contract);
    }

    /**
     * When the campaign opts into intel obfuscation, independently hides each obfuscatable intel field of a freshly
     * generated offer with a fixed per-field chance, so the player must sometimes commit with incomplete information.
     * The GM can still adjust the result by hand in the contract editor. Assessment is never obfuscated (it is not an
     * {@link ObfuscatableIntel} field).
     */
    private static void applyIntelObfuscation(Campaign campaign, ChaosContract contract) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_INTEL_OBFUSCATION)) {
            return;
        }
        for (ObfuscatableIntel field : ObfuscatableIntel.values()) {
            if (Compute.randomInt(INTEL_OBFUSCATION_ODDS) == 0) {
                contract.setIntelObfuscated(field, true);
            }
        }
    }

    /**
     * Decides whether a freshly generated contract is a covert operation. Only objectives flagged as covert candidates
     * are eligible, and even those turn covert only on a {@link #COVERT_CONTRACT_ODDS} chance roll; every other
     * contract is left as-is. A contract that already has a special designation (a Proving Ground) is never overridden
     * - the designations are mutually exclusive and the deliberate market top-up takes priority over the random covert
     * roll. A portion of the covert contracts become false flag operations instead, run under a cover-story front
     * (applied later, once the enemy is known - see {@link #applyFalseFlagCover}). That portion is much larger when the
     * contract's true backer is ComStar or the Word of Blake ({@link #COMSTAR_WOB_FALSE_FLAG_ODDS} rather than
     * {@link #FALSE_FLAG_ODDS}), since deniable false flags are their signature. Either way the enemy is later drawn
     * under covert rules (see {@link ChaosContractDeterminationEnemy#generateEnemyFactionForObjective}), where even the
     * employer's allies can become rare targets.
     */
    private static void determineCovertStatus(ChaosObjectiveType chaosObjectiveType, ChaosContract contract) {
        if (contract.getNature() != ContractNature.NORMAL) {
            return;
        }
        if (chaosObjectiveType.isCovertCandidate() && Compute.randomInt(COVERT_CONTRACT_ODDS) == 0) {
            int falseFlagOdds = contract.getStandingEmployerFaction().isComStarOrWoB()
                                      ? COMSTAR_WOB_FALSE_FLAG_ODDS
                                      : FALSE_FLAG_ODDS;
            boolean isFalseFlag = Compute.randomInt(falseFlagOdds) == 0;
            contract.setNature(isFalseFlag ? ContractNature.FALSE_FLAG : ContractNature.COVERT);
        }
    }

    /**
     * Dresses a false flag operation in its cover story by making a front faction the visible employer while preserving
     * the real employer as the covert sponsor. The front is a regionally plausible employer faction near the contract -
     * drawn the same way a real employer would be - that is neither the true employer nor the enemy, so the cover holds
     * up. The rest of the employer record (type, territorial anchor, negotiator, force ratings, camouflage, color) is
     * carried over unchanged, so only the presented identity changes; the real employer survives in the sponsor slot,
     * where {@link AbstractContract#getStandingEmployerFaction()} still routes Faction Standing to it.
     *
     * <p>If no plausible front can be found the contract falls back to a plain covert operation, concealing the
     * employer
     * as "Undisclosed Employer" instead.</p>
     *
     * @param contract the false flag contract to dress in a cover story
     * @param location the contract's current location, used to find regionally present factions
     * @param date     the current date, used for faction availability and the front's era-correct name
     */
    private static void applyFalseFlagCover(ChaosContract contract, ILocation location, LocalDate date) {
        EmployerData realEmployer = contract.getEmployerData();
        Faction realEmployerFaction = contract.getEmployerFaction();
        Faction enemy = contract.getEnemyFaction();
        Predicate<Faction> notAPrincipal = faction -> !faction.equals(realEmployerFaction) && !faction.equals(enemy);

        Faction front = RandomFactionGenerator.getInstance().getRandomEmployerFaction(location, date, true,
              notAPrincipal);
        if (front == null) {
            // No plausible front; degrade to a plain covert operation, which conceals the employer outright.
            contract.setNature(ContractNature.COVERT);
            return;
        }

        // Preserve the party that bears the standing consequences: the real employer's existing sponsor if it has one,
        // otherwise the real employer itself.
        String sponsorFactionCode = (realEmployer.sponsorFactionCode() != null)
                                          ? realEmployer.sponsorFactionCode()
                                          : realEmployer.factionCode();

        // Re-roll the camouflage for the front so allied units in battle wear the cover faction's colours, not the real
        // employer's - otherwise the front's faction camo would give the operation away. Force ratings are kept: the
        // troops are still the real employer's, only their presented identity changes. The player colour is left as the
        // generated default, which is uniform across all employers and so reveals nothing.
        EmployerData cover = new EmployerData(realEmployer.type(),
              front.getShortName(),
              realEmployer.anchorFactionCode(),
              sponsorFactionCode,
              front.getFullName(date.getYear()),
              realEmployer.negotiator(),
              realEmployer.liaison(),
              realEmployer.forceSkill(),
              realEmployer.equipmentRating(),
              pickRandomCamouflage(date.getYear(), front.getShortName()),
              realEmployer.color());
        contract.setEmployerData(cover);
    }

    private static void setAncillaryValues(Campaign campaign, LocalHangar detachmentHangar, ChaosContract contract) {
        // Scale must be set before the required victory points, which are derived from it.
        contract.setScale(determineScale(campaign, campaign.getPlayerForce(), detachmentHangar, contract));
        contract.setRequiredCombatElements(determineRequiredCombatElements(campaign));
        contract.setTrackCount(determineTrackCount(contract));
        contract.setRequiredVictoryPoints(determineRequiredVictoryPoints(contract));
    }

    /**
     * Determines a contract's scale from the units the player force commits to it. Exposed as public so the GM contract
     * editor's "Automatic" scale option follows exactly the same rule generation uses.
     *
     * @param campaign         the campaign, for the support-point-to-Battle-Value scale conversion option
     * @param playerForce      the player force whose committed units set the scale
     * @param detachmentHangar the hangar whose units are weighed
     * @param contract         the contract being sized (its objective decides whether cadre units count)
     *
     * @return the automatically determined scale
     */
    public static int determineScale(Campaign campaign, PlayerForce playerForce, LocalHangar detachmentHangar,
          AbstractContract contract) {
        boolean convertSupportPointsToBattleValue = campaign.getCampaignOptions()
                                                          .get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION);
        return ChaosContractDeterminationScale.generateScaleForDetachment(playerForce, detachmentHangar,
              contract.getObjectiveType().isCadreDuty(), convertSupportPointsToBattleValue);
    }

    /**
     * Determines a contract's required combat elements the way generation does. Note this includes a random variance
     * factor, so repeated calls will not agree.
     *
     * @return the automatically determined required combat elements
     */
    public static int determineRequiredCombatElements(Campaign campaign) {
        return RequiredCombatElements.calculateRequiredCombatElements(campaign, false,
              ContractUtilities.calculateVarianceFactor());
    }

    /**
     * Determines a contract's StratCon track count from its objective, the way generation does.
     *
     * @return the automatically determined track count
     */
    public static int determineTrackCount(AbstractContract contract) {
        return ChaosContractDeterminationIntensity.determineTrackCount(contract.getObjectiveType()
                                                                               .getChaosObjectiveType());
    }

    /**
     * Determines a contract's required victory points from its current state (scale, command rights, schedule, and
     * StratCon tracks), the way generation does. Determine the scale first if it too is being regenerated.
     *
     * @return the automatically determined required victory points
     */
    public static int determineRequiredVictoryPoints(AbstractContract contract) {
        return ChaosContractDeterminationRequiredVictoryPoints.getRequiredVictoryPoints(contract);
    }

    /**
     * Determines a contract's length in months the way generation does: from the objective type, honoring the campaign's
     * variable-contract-length option. Exposed as public so the GM contract editor's "Automatic" length follows the same
     * rule.
     *
     * @return the automatically determined contract length in months
     */
    public static int determineLength(Campaign campaign, AbstractContract contract) {
        boolean useVariableContractLength = campaign.getCampaignOptions().get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
        return contract.getObjectiveType().getChaosObjectiveType().calculateLength(useVariableContractLength);
    }

    /**
     * Determines which planet within a target system a contract is fought over, the way generation does: a weighted
     * random draw over the system's worlds by strategic value, in the direction the contract's objective sets. Exposed
     * as public so the GM contract editor's "Automatic" planet option follows the same rule.
     *
     * @param contract     the contract whose objective sets the weighting direction
     * @param targetSystem the already-chosen target system, or {@code null}
     * @param currentDate  the date the worlds are read for
     *
     * @return the automatically chosen planet, or {@code null} when there is no system or it has no eligible worlds
     */
    public static @Nullable Planet determineTargetPlanet(AbstractContract contract,
          @Nullable PlanetarySystem targetSystem, LocalDate currentDate) {
        if (targetSystem == null) {
            return null;
        }
        return ChaosPlanetSelector.selectTargetPlanet(targetSystem.getPlanets(),
              contract.getObjectiveType().getChaosObjectiveType(), currentDate);
    }

    /**
     * Determines a contract's monthly pay the way generation does. Exposed as public so the GM contract editor's
     * "Automatic" monthly pay follows the same rule, tracking the campaign's chosen pay scheme: the CamOps force value
     * when {@link CampaignOption#USE_LEGACY_CONTRACT_PAY} is set, otherwise the Chaos scale-and-base-pay scheme.
     */
    public static Money determineMonthlyPay(Campaign campaign, AbstractContract contract) {
        return AbstractContractDeterminationPay.forCampaign(campaign).getMonthlyPay(campaign, contract);
    }

    /**
     * Determines a contract's combat pay the way generation does, tracking the campaign's chosen pay scheme: CamOps
     * folds combat compensation into the monthly retainer (zero here), while the Chaos scheme pays a separate combat
     * bonus derived from scale.
     */
    public static Money determineCombatPay(Campaign campaign, AbstractContract contract) {
        return AbstractContractDeterminationPay.forCampaign(campaign).getCombatPay(campaign, contract);
    }

    /**
     * Determines a contract's transport pay the way generation does (from its scale, transport terms, and the journey
     * to the target from the player's current location).
     */
    public static Money determineTransportPay(Campaign campaign, AbstractContract contract) {
        return AbstractContractDeterminationPay.forCampaign(campaign).getTransportPay(campaign,
              campaign.getLocalDate(), contract,
              campaign.getPlayerForce().getForceDetachment().getCurrentLocation());
    }

    /**
     * Determines a contract's start date from the travel time to its target, the way generation does: the player
     * journeys from their current system to the contract's target world and the contract begins on arrival. The
     * computed jump path is cached on the contract as a side effect, exactly as generation does. Exposed as public so
     * the GM contract editor's "Automatic" start date follows the same rule.
     *
     * @return the automatically determined start date, or the campaign date when no route can be measured
     */
    public static LocalDate determineStartDate(Campaign campaign, AbstractContract contract) {
        PlayerForce playerForce = campaign.getPlayerForce();
        return determineStartDate(campaign, campaign.getLocalDate(), campaign.getCurrentSystem(),
              contract.getTargetSystem(), contract.getTargetPlanet(), playerForce.getFactionStandings(),
              playerForce.isOverridingCommandCircuitRequirements(), campaign.isGM(),
              contract.getEmployerFactionCode(), contract);
    }

    private static LocalDate determineStartDate(Campaign campaign, LocalDate currentDate,
          @Nullable PlanetarySystem currentSystem, @Nullable PlanetarySystem targetSystem,
          @Nullable Planet targetPlanet, FactionStandings factionStandings,
          boolean overridingCommandCircuitRequirements, boolean isGM, String employerFactionCode,
          AbstractContract contract) {
        if (currentSystem == null || targetSystem == null) {
            // No route to measure - the contract begins immediately.
            return currentDate;
        }

        // Jump Path, terminating at the specific target planet so the journey time ends at the world the contract is
        // fought over rather than the system's primary world.
        JumpPath jumpPath = campaign.calculateJumpPath(currentSystem, targetSystem);
        jumpPath.setTargetPlanet(targetPlanet);
        contract.setCachedJumpPath(jumpPath);

        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(overridingCommandCircuitRequirements,
              isGM, factionStandings, employerFactionCode);
        int journeyTimeInDays = (int) ceil(jumpPath.getTotalTime(currentDate, 0, isUseCommandCircuit));
        return currentDate.plusDays(journeyTimeInDays);
    }

    /**
     * Rates the skill and equipment both sides commit to the contract, then rebuilds the employer and enemy data with
     * those ratings in place of the placeholder defaults.
     *
     * <p>The rating is grounded in the galaxy: it synthesizes the contract's {@link ContractImportance} from who is
     * hiring, what for, and the strategic value of the world being fought over, then hands that &mdash; alongside each
     * side's faction, role, and the era &mdash; to {@link ChaosEmployerForceRating}. When the campaign has opted into
     * dynamic difficulty, the ratings are additionally scaled toward the player's own skill.</p>
     *
     * @param campaignOptions the campaign options
     * @param currentDate     the current date, for era context and reading the target world's data
     * @param targetPlanet    the target planet
     * @param employerData    the employer data to re-rate
     * @param enemyData       the enemy data to re-rate
     * @param objectiveData   the contract's objectives, for the player objective's strategic scope
     * @param contract        the contract being built
     */
    private static void setForceRatings(PlayerForce playerForce, CampaignOptions campaignOptions, LocalDate currentDate,
          Detachment detachment, Planet targetPlanet, EmployerData employerData, EnemyData enemyData,
          ContractObjectiveData objectiveData, ChaosContract contract, boolean provingGround) {
        int planetStrategicValue = (targetPlanet == null) ?
                                         0 :
                                         ChaosPlanetStrategicValue.calculate(targetPlanet, currentDate);

        ChaosObjectiveType objectiveType = objectiveData.playerObjectiveType().getChaosObjectiveType();
        ContractImportance importance = ContractImportance.from(employerData.type(), objectiveType,
              planetStrategicValue);

        boolean isPlayerAttacker = objectiveType.isAttacker();
        int year = currentDate.getYear();
        boolean scaleToPlayer = campaignOptions.get(CampaignOption.USE_DYNAMIC_DIFFICULTY);

        boolean isClanForce = playerForce.isClanForce();
        SkillLevel playerAverageSkill = getAverageSkill(playerForce,
              isClanForce,
              campaignOptions,
              currentDate,
              detachment);

        ChaosEmployerForceRating.ForceRating employerRating = ChaosEmployerForceRating.determine(employerData.getFaction(),
              isPlayerAttacker,
              true,
              year,
              importance,
              scaleToPlayer,
              playerAverageSkill);

        ChaosEmployerForceRating.ForceRating enemyRating = ChaosEmployerForceRating.determine(enemyData.getFaction(),
              !isPlayerAttacker,
              false,
              year,
              importance,
              scaleToPlayer,
              playerAverageSkill);

        // Proving Ground (pity) contracts are deliberately easy: a veteran ally against a green enemy. The rest of the
        // rating - equipment, factions, everything else - is left as generated.
        SkillLevel employerSkill = provingGround ? SkillLevel.VETERAN : employerRating.forceSkill();
        SkillLevel enemySkill = provingGround ? SkillLevel.GREEN : enemyRating.forceSkill();

        // "Bolster Contract Skill": an unofficial option that makes both allied and enemy forces one level tougher than
        // the random tables give, to offset how skilled MekHQ crews get. Proving-ground (pity) contracts stay easy.
        if (!provingGround && campaignOptions.get(CampaignOption.USE_BOLSTER_CONTRACT_SKILL)) {
            employerSkill = SkillLevel.changeByDelta(employerSkill, 1);
            enemySkill = SkillLevel.changeByDelta(enemySkill, 1);
        }

        EmployerData updatedEmployerData = new EmployerData(employerData,
              employerSkill,
              employerRating.equipmentRating());
        contract.setEmployerData(updatedEmployerData);

        EnemyData updatedEnemyData = new EnemyData(enemyData, enemySkill, enemyRating.equipmentRating());
        contract.setEnemyData(updatedEnemyData);
    }

    private static SkillLevel getAverageSkill(PlayerForce playerForce, boolean isClanForce,
          CampaignOptions campaignOptions, LocalDate currentDate, Detachment detachment) {
        SkillLevel averageSkill;
        boolean isUseChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);

        if (isUseChaosReputation) {
            List<Person> personnel = detachment.getPersonnel().values().stream()
                                           .filter(person -> !person.getStatus().isDepartedUnit())
                                           .toList();
            averageSkill = ChaosReputation.getAverageSkillLevel(campaignOptions,
                  isClanForce,
                  currentDate,
                  personnel);
        } else {
            averageSkill = playerForce.getCamOpsReputation().getAverageSkillLevel();
        }

        return averageSkill;
    }

    private static void setContractTerms(ChaosObjectiveType objectiveType, ChaosEmployerType employerType,
          Faction employerFaction, boolean useFactionModifiers, ChaosContract contract) {
        ContractTermsData initialContractTerms = ChaosContractDeterminationTerms.determineInitialTerms(objectiveType,
              employerType, employerFaction, useFactionModifiers);
        contract.setContractTerms(initialContractTerms);
    }

    /**
     * Rolls, independently for each of the five terms, a 1-in-4 chance that the employer locks it as non-negotiable.
     * Locked terms are fixed at their generated value: the player can neither improve nor sacrifice them in the
     * negotiation dialog.
     */
    private static void lockNonNegotiableTerms(ChaosContract contract) {
        contract.setNonNegotiableTermsData(new NonNegotiableTermsData(rollNonNegotiable(), rollNonNegotiable(),
              rollNonNegotiable(), rollNonNegotiable(), rollNonNegotiable()));
    }

    private static boolean rollNonNegotiable() {
        return Compute.randomInt(NON_NEGOTIABLE_TERM_ODDS) == 0;
    }

    private static void determineSchedule(Campaign campaign, boolean useVariableContractLength, LocalDate currentDate,
          PlanetarySystem currentSystem, Planet targetPlanet, FactionStandings factionStandings,
          boolean overridingCommandCircuitRequirements, boolean isGM, PlanetarySystem targetSystem,
          String employerFactionCode, ChaosObjectiveType objectiveType, ChaosContract contract) {
        // Contract schedule - the player journeys from their current system to the target world, arriving on the
        // start date (this also caches the jump path on the contract).
        LocalDate startDate = determineStartDate(campaign, currentDate, currentSystem, targetSystem, targetPlanet,
              factionStandings, overridingCommandCircuitRequirements, isGM, employerFactionCode, contract);

        int monthsLength = objectiveType.calculateLength(useVariableContractLength);
        LocalDate endDate = startDate.plusMonths(monthsLength);

        ContractScheduleData contractScheduleData = new ContractScheduleData(startDate, endDate, monthsLength);
        contract.setScheduleData(contractScheduleData);

        // Scenario schedule - spread the (already determined) track count across the contract's months via the Track
        // Intensity Tables. Nothing consumes this yet; it is generated and stored for a later feature.
        //
        // Hot Spots Draconis Reach sizes each scenario by the contract's scale. When "Multiply Track Intensity by
        // Scale" is set (the default MekHQ behavior), we instead roll the intensity table once per point of scale and
        // sum the results, so the contract's combat volume grows with scale while each scenario keeps its rolled size -
        // letting a player deploy smaller formations (e.g. lances). Otherwise, a single roll is made.
        int rollCount = campaign.getCampaignOptions().get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE)
                              ? contract.getScale()
                              : 1;
        contract.setScenarioSchedule(TrackIntensityTable.rollSchedule(monthsLength, contract.getTrackCount(),
              rollCount));
    }

    private static @Nonnull EnemyData pickEnemy(Campaign campaign, LocalDate currentDate, ILocation currentLocation,
          EmployerData employerData, ContractObjectiveData objectiveData, ChaosContract contract) {
        EnemyData enemyData;
        if (employerData.type() == ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS) {
            // A rebellion is fought against the rebels' own ruling power, not a randomly drawn belligerent, so the
            // enemy is fixed to the employer's territorial anchor (the local government the rebels rise against).
            enemyData = ChaosContractDeterminationEnemy.generateEnemyForFaction(campaign,
                  employerData.getAnchorFaction(),
                  currentDate);
        } else {
            // The randomly drawn belligerent must not be the employer itself - an "employer versus itself" contract is
            // nonsensical outside of a rebellion (handled above). Redraw a few times, then accept whatever we get
            // rather than loop forever if the pool genuinely cannot offer an alternative.
            String employerFactionCode = employerData.factionCode();
            int attempts = 0;
            do {
                enemyData = ChaosContractDeterminationEnemy.generateEnemyFactionForObjective(campaign,
                        currentLocation,
                        currentDate,
                        employerData.getAnchorFaction(),
                      objectiveData.playerObjectiveType(),
                      contract.isCovertOperation());
                attempts++;
            } while (enemyData.factionCode().equals(employerFactionCode) && attempts < MAX_ENEMY_REDRAWS);
        }
        contract.setEnemyData(enemyData);
        return enemyData;
    }

    private static @Nullable SystemsTargetData pickTargetLocation(ILocation currentLocation, LocalDate currentDate,
          ContractObjectiveData objectiveData, EmployerData employerData, EnemyData enemyData,
          ChaosContract contract, boolean isDefensiveObjective, boolean currentSystemEmployer) {
        // A rebellion happens where the rebels are - here, against the local ruling power - so it is always situated in
        // the current system rather than drawn from the employer/enemy border.
        boolean isRebellion = employerData.type() == ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS;
        boolean shouldAlwaysPickCurrentSystem = (isDefensiveObjective && currentSystemEmployer) || isRebellion;
        String currentSystemId = currentLocation.getCurrentSystem().getId();

        String targetSystemId = shouldAlwaysPickCurrentSystem ?
                                      currentSystemId :
                                      ChaosContractDeterminationLocation.determineContractLocation(objectiveData.playerObjectiveType(),
                                            true,
                                            employerData.anchorFactionCode(),
                                            enemyData.factionCode(),
                                            currentLocation);
        PlanetarySystem targetSystem = Systems.getInstance().getSystemById(targetSystemId);
        if (targetSystem == null) {
            LOGGER.warn("Target system {} could not be resolved. Contract generation failed.", targetSystemId);
            return null;
        }

        // Weight the pick toward (or, for pirate hunts, away from) strategically valuable worlds, the same way the
        // target system itself is chosen.
        Planet targetPlanet = determineTargetPlanet(contract, targetSystem, currentDate);
        if (targetPlanet == null) {
            LOGGER.warn("Target system {} has no planets to situate a contract on. Contract generation failed.",
                  targetSystemId);
            return null;
        }

        SystemsTargetData systemsTargetData = new SystemsTargetData(targetSystemId, targetPlanet.getId());
        contract.setSystemsTargetData(systemsTargetData);
        return systemsTargetData;
    }

    private static @Nonnull ContractObjectiveData pickObjective(int contractGenerationModifier,
          ChaosContract contract) {
        ContractObjectiveData objectiveData = ChaosContractDeterminationObjective.determineContractObjectiveType(
              contractGenerationModifier);
        contract.setObjectiveData(objectiveData);
        return objectiveData;
    }

    private static @Nullable EmployerData pickEmployer(Campaign campaign, LocalDate currentDate,
          ILocation currentLocation, ContractSearchType searchType, boolean covertViable, ChaosContract contract) {
        EmployerData employerData = ChaosContractDeterminationEmployer.getEmployerGenerationData(currentDate,
              currentLocation,
              campaign,
              searchType,
              covertViable);
        if (employerData == null) {
            // No employer means no contract
            return null;
        }

        contract.setEmployerData(employerData);
        return employerData;
    }
}
