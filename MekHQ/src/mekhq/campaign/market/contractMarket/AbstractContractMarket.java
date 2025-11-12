/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import static java.lang.Math.min;
import static megamek.common.compute.Compute.d6;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.HEROIC;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.Version;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for various Contract Market types in AtB/StratCon. Responsible for generation and initialization
 * of AtBContracts.
 */
public abstract class AbstractContractMarket {
    public static final int CLAUSE_COMMAND = 0;
    public static final int CLAUSE_SALVAGE = 1;
    public static final int CLAUSE_SUPPORT = 2;
    public static final int CLAUSE_TRANSPORT = 3;
    public static final int CLAUSE_NUM = 4;

    // Command Rights thresholds
    private static final int MERCENARY_THRESHOLD_INTEGRATED = 3;
    private static final int MERCENARY_THRESHOLD_HOUSE = 8;
    private static final int MERCENARY_THRESHOLD_LIAISON = 12;
    private static final int NON_MERCENARY_THRESHOLD = 12;


    protected List<Contract> contracts = new ArrayList<>();
    protected int lastId = 0;
    protected Map<Integer, Contract> contractIds = new HashMap<>();
    protected Map<Integer, ClauseMods> clauseMods = new HashMap<>();

    /**
     * An arbitrary maximum number of attempts to generate a contract.
     */
    protected static final int MAXIMUM_GENERATION_RETRIES = 3;

    /* It is possible to call addFollowup more than once for the
     * same contract by canceling the dialog and running it again;
     * this is the easiest place to track it to prevent
     * multiple followup contracts.
     * key: followup id
     * value: main contract id
     */
    protected HashMap<Integer, Integer> followupContracts = new HashMap<>();

    /**
     * An arbitrary maximum number of attempts to find a random employer faction that is not a Mercenary.
     */
    protected static final int MAXIMUM_ATTEMPTS_TO_FIND_NON_MERC_EMPLOYER = 20;

    private final ContractMarketMethod method;
    private static final MMLogger logger = MMLogger.create(AbstractContractMarket.class);


    /**
     * Generate a new contract and add it to the market.
     *
     * @return The newly generated contract
     */
    public abstract AtBContract addAtBContract(Campaign campaign);

    /**
     * Generate available contract offers for the player's force.
     *
     * @param newCampaign Boolean indicating whether this is a fresh campaign.
     */
    public abstract void generateContractOffers(Campaign campaign, boolean newCampaign);

    /**
     * Generate followup contracts and add them to the market if the currently selected market type supports them.
     *
     * @param campaign The current campaign.
     * @param contract The AtBContract being completed and used as a basis for followup missions
     */
    public abstract void checkForFollowup(Campaign campaign, AtBContract contract);

    /**
     * Calculate the total payment modifier for the contract based on the configured market method (e.g., CAM_OPS,
     * ATB_MONTHLY).
     *
     * @return a double representing the total payment multiplier.
     */
    public abstract double calculatePaymentMultiplier(Campaign campaign, AtBContract contract);

    protected AbstractContractMarket(final ContractMarketMethod method) {
        this.method = method;
    }

    /**
     * @return the Method (e.g., CAM_OPS, ATB_MONTHLY) associated with the Contract Market instance
     */
    public ContractMarketMethod getMethod() {
        return method;
    }

    /**
     * Empty an available contract from the market.
     *
     * @param c contract to remove
     */
    public void removeContract(Contract c) {
        contracts.remove(c);
        contractIds.remove(c.getId());
        clauseMods.remove(c.getId());
        followupContracts.remove(c.getId());
    }

    /**
     * Rerolls a specific clause in a contract, typically as part of a negotiation process. This method adjusts the
     * clause based on the provided clause type and associated modifiers, ensuring the contract reflects updated terms.
     *
     * <p>The recalculated clause values can affect aspects such as command, salvage, transport,
     * or support terms. Special rules, such as overrides for Clan technology salvage, may also be applied when
     * rerolling specific clauses.
     *
     * @param contract the contract being negotiated, which will have its terms modified
     * @param clause   the type of clause to be rerolled (e.g., command, salvage, transport, or support)
     * @param campaign the active campaign context, used to access campaign-specific options and rules
     */
    public void rerollClause(AtBContract contract, int clause, Campaign campaign) {
        final Faction faction = campaign.getFaction();
        final boolean isMercenary = faction.isMercenary();
        if (null != clauseMods.get(contract.getId())) {
            switch (clause) {
                case CLAUSE_COMMAND ->
                      rollCommandClause(contract, clauseMods.get(contract.getId()).mods[clause], isMercenary);
                case CLAUSE_SALVAGE -> {
                    rollSalvageClause(contract,
                          clauseMods.get(contract.getId()).mods[clause],
                          campaign.getCampaignOptions().getContractMaxSalvagePercentage());

                    contract.clanTechSalvageOverride();
                }
                case CLAUSE_TRANSPORT -> rollTransportClause(contract, clauseMods.get(contract.getId()).mods[clause]);
                case CLAUSE_SUPPORT -> rollSupportClause(contract, clauseMods.get(contract.getId()).mods[clause]);
            }
            clauseMods.get(contract.getId()).rerollsUsed[clause]++;
            contract.calculateContract(campaign);
        }
    }

    /**
     * Returns the number of rerolls used so far for a specific clause.
     *
     * @param clause ID representing the type of clause.
     *
     */
    public int getRerollsUsed(Contract contract, int clause) {
        if (null != clauseMods.get(contract.getId())) {
            return clauseMods.get(contract.getId()).rerollsUsed[clause];
        }
        return 0;
    }

    /**
     * @return a list of currently active contracts on the market
     */
    public List<Contract> getContracts() {
        return contracts;
    }

    /**
     * Empties the market and generates a new batch of contract offers for an existing campaign.
     *
     */
    public void generateContractOffers(Campaign campaign) {
        generateContractOffers(campaign, false);
    }

    protected void updateReport(Campaign campaign) {
        if (campaign.getCampaignOptions().isContractMarketReportRefresh()) {
            campaign.addReport("<a href='CONTRACT_MARKET'>Contract market updated</a>");
        }
    }

    /**
     * Calculates the required number of combat elements for a contract based on campaign options, contract details, and
     * variance factors.
     *
     * <p>
     * This method determines the number of combat elements needed to deploy, taking into account factors such as:
     * <ul>
     *   <li>Whether the contract is a subcontract (returns 1 as a base case).</li>
     *   <li>The effective unit forces.</li>
     *   <li>Whether variance bypass is enabled, applying a flat reduction to available forces.</li>
     *   <li>Variance adjustments applied through a die roll, affecting the availability of forces.</li>
     * </ul>
     * The method ensures values are clamped to maintain a minimum deployment of at least 1 combat
     * element while not exceeding the maximum deployable combat elements.
     *
     * @param campaign       the campaign containing relevant options and faction information
     * @param contract       the contract that specifies details such as subcontract status
     * @param bypassVariance a flag indicating whether variance adjustments should be bypassed
     * @param varianceFactor the degree of variance to apply to required combat elements
     *
     * @return the calculated number of required units in combat teams, ensuring it meets game rules and constraints
     */
    public int calculateRequiredCombatElements(Campaign campaign, AtBContract contract, boolean bypassVariance,
          double varianceFactor) {
        // Return 1 combat team if the contract is a subcontract
        if (contract.isSubcontract()) {
            return 1;
        }

        // Calculate base formation size and effective unit force
        int effectiveForces = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(campaign);

        // If bypassing variance, apply flat reduction (reduce force by 1/3)
        if (bypassVariance) {
            return max(effectiveForces - calculateBypassVarianceReduction(effectiveForces), 1);
        }

        // Adjust available forces based on variance, ensuring minimum clamping
        int adjustedForces = (int) Math.floor((double) effectiveForces * varianceFactor);

        if (adjustedForces < 1) {
            adjustedForces = 1;
        }

        // Return the clamped value, ensuring it does not exceed max-deployable forces
        return Math.min(adjustedForces, effectiveForces);
    }

    /**
     * Calculates the required number of combat teams (intensity) for a contract based on campaign options, contract
     * details, and variance factors.
     *
     * <p>This method determines the number of combat elements needed to deploy, taking into account factors such
     * as:</p>
     * <ul>
     *   <li>Whether the contract is a subcontract (returns 1 as a base case).</li>
     *   <li>The effective unit forces.</li>
     *   <li>Whether variance bypass is enabled, applying a flat reduction to available forces.</li>
     *   <li>Variance adjustments applied through a die roll, affecting the availability of forces.</li>
     * </ul>
     *
     * <p>The method ensures values are clamped to maintain a minimum deployment of at least 1 combat element while
     * not exceeding the maximum deployable combat elements.</p>
     *
     * @param campaign       the campaign containing relevant options and faction information
     * @param contract       the contract that specifies details such as subcontract status
     * @param bypassVariance a flag indicating whether variance adjustments should be bypassed
     * @param varianceFactor the degree of variance to apply to required combat elements
     *
     * @return the calculated number of required units in combat teams, ensuring it meets game rules and constraints
     *
     * @since 0.50.10
     */
    public int calculateRequiredCombatTeams(Campaign campaign, AtBContract contract, boolean bypassVariance,
          double varianceFactor) {
        // Return 1 combat team if the contract is a subcontract
        if (contract.isSubcontract()) {
            return 1;
        }

        // Calculate base formation size and effective unit force
        int effectCombatTeams = 0;
        for (Map.Entry<Integer, CombatTeam> combatTeam : campaign.getCombatTeamsAsMap().entrySet()) {
            Force force = campaign.getForce(combatTeam.getKey());
            if (force != null) {
                CombatRole combatRoleInMemory = force.getCombatRoleInMemory();
                if (combatRoleInMemory != CombatRole.TRAINING) {
                    effectCombatTeams++;
                }
            }
        }

        // If bypassing variance, apply flat reduction (reduce force by 1/3)
        if (bypassVariance) {
            return max(effectCombatTeams - calculateBypassVarianceReduction(effectCombatTeams), 1);
        }

        // Adjust available forces based on variance, ensuring minimum clamping
        int adjustedCombatTeams = (int) Math.floor((double) effectCombatTeams * varianceFactor);
        adjustedCombatTeams = max(adjustedCombatTeams, 1);

        // Return the clamped value, ensuring it does not exceed max-deployable forces
        return Math.min(adjustedCombatTeams, effectCombatTeams);
    }

    /**
     * Calculates the bypass variance reduction based on the available forces.
     *
     * <p>
     * The reduction is calculated by dividing the available forces by a fixed factor of 3 and rounding down to the
     * nearest whole number. This value is used in scenarios where variance adjustments are bypassed.
     * </p>
     *
     * @param availableForces the total number of forces available
     *
     * @return the bypass variance reduction as an integer
     */
    private int calculateBypassVarianceReduction(int availableForces) {
        return (int) Math.floor((double) availableForces / 3);
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int calculateMaxDeployableCombatTeams(Campaign campaign) {
        CampaignOptions options = campaign.getCampaignOptions();
        int baseStrategyDeployment = options.getBaseStrategyDeployment();
        int additionalStrategyDeployment = options.getAdditionalStrategyDeployment();
        int commanderStrategy = campaign.getCommanderStrategy();

        return baseStrategyDeployment + additionalStrategyDeployment * commanderStrategy;
    }

    /**
     * Determines the {@link SkillLevel} corresponding to a given roll result (TW pg 273), optionally applying the
     * Bolster Contract skill adjustment.
     *
     * <p>This method maps a numerical {@code roll} value to a {@link SkillLevel} according to fixed thresholds. If
     * {@code isUseBolsterContractSkill} is {@code true}, the resulting level is shifted one tier higher to reflect the
     * benefit of the Bolster Contract skill.</p>
     *
     * @param roll                      the numeric roll determining the base skill rating
     * @param isUseBolsterContractSkill {@code true} to apply the Bolster Contract bonus, {@code false} for the standard
     *                                  progression
     *
     * @return the {@link SkillLevel} corresponding to the roll and modifier
     */
    protected SkillLevel getSkillRating(int roll, boolean isUseBolsterContractSkill) {
        if (roll <= 5) {
            return isUseBolsterContractSkill ? REGULAR : GREEN;
        } else if (roll <= 9) {
            return isUseBolsterContractSkill ? VETERAN : REGULAR;
        } else if (roll <= 11) {
            return isUseBolsterContractSkill ? ELITE : VETERAN;
        } else {
            return isUseBolsterContractSkill ? HEROIC : ELITE;
        }
    }

    protected int getQualityRating(int roll) {
        if (roll <= 5) {
            return IUnitRating.DRAGOON_F;
        } else if (roll <= 8) {
            return IUnitRating.DRAGOON_D;
        } else if (roll <= 10) {
            return IUnitRating.DRAGOON_C;
        } else if (roll == 11) {
            return IUnitRating.DRAGOON_B;
        } else {
            return IUnitRating.DRAGOON_A;
        }
    }

    /**
     * Calculates and sets the command rights clause for a contract based on a roll and modifier.
     *
     * <p>This method determines the appropriate {@link ContractCommandRights} for the given {@link Contract},
     * using the result of a die roll (with modifiers). The logic differentiates between mercenary and non-mercenary
     * contracts, as these have different thresholds for command rights determination.</p>
     *
     * <ul>
     *   <li>For mercenaries, the command rights are determined using multiple thresholds, defined by constants,
     *       and can be one of the following:
     *       <ul>
     *         <li>{@link ContractCommandRights#INTEGRATED}</li>
     *         <li>{@link ContractCommandRights#HOUSE}</li>
     *         <li>{@link ContractCommandRights#LIAISON}</li>
     *         <li>{@link ContractCommandRights#INDEPENDENT}</li>
     *       </ul>
     *   </li>
     *   <li>For non-mercenaries, only two outcomes are possible:
     *       <ul>
     *         <li>{@link ContractCommandRights#INTEGRATED}</li>
     *         <li>{@link ContractCommandRights#HOUSE}</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param contract    The {@link Contract} whose command rights will be set based on the roll outcome.
     * @param modifier    The numeric modifier applied to the dice roll value.
     * @param isMercenary Indicates whether the contract applies to a mercenary, which affects the thresholds used for
     *                    determining command rights.
     */
    protected void rollCommandClause(final Contract contract, final int modifier, boolean isMercenary) {
        final int roll = d6(2) + modifier;

        if (isMercenary) {
            // Handle mercenary thresholds
            contract.setCommandRights(determineMercenaryCommandRights(roll));
        } else {
            // Handle non-mercenary thresholds
            contract.setCommandRights(roll < NON_MERCENARY_THRESHOLD ?
                                            ContractCommandRights.INTEGRATED :
                                            ContractCommandRights.HOUSE);
        }
    }

    /**
     * Determines the command rights for a mercenary contract based on a roll.
     *
     * <p>This method evaluates the roll against predefined thresholds to determine and return the appropriate
     * {@link ContractCommandRights} for mercenaries</p>
     *
     * <ul>
     *   <li>Results:
     *       <ul>
     *         <li>Less than {@code MERCENARY_THRESHOLD_INTEGRATED}: {@link ContractCommandRights#INTEGRATED}</li>
     *         <li>Between {@code MERCENARY_THRESHOLD_INTEGRATED} (inclusive) and {@code MERCENARY_THRESHOLD_HOUSE}:
     *             {@link ContractCommandRights#HOUSE}</li>
     *         <li>Between {@code MERCENARY_THRESHOLD_HOUSE} (inclusive) and {@code MERCENARY_THRESHOLD_LIAISON}:
     *             {@link ContractCommandRights#LIAISON}</li>
     *         <li>Greater than or equal to {@code MERCENARY_THRESHOLD_LIAISON}: {@link ContractCommandRights#INDEPENDENT}</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param roll The total value of a die roll (with modifiers) used to determine command rights.
     *
     * @return The {@link ContractCommandRights} determined based on the roll value.
     */
    ContractCommandRights determineMercenaryCommandRights(int roll) {
        if (roll < MERCENARY_THRESHOLD_INTEGRATED) {
            return ContractCommandRights.INTEGRATED;
        } else if (roll < MERCENARY_THRESHOLD_HOUSE) {
            return ContractCommandRights.HOUSE;
        } else if (roll < MERCENARY_THRESHOLD_LIAISON) {
            return ContractCommandRights.LIAISON;
        } else {
            return ContractCommandRights.INDEPENDENT;
        }
    }

    protected void rollSalvageClause(AtBContract contract, int mod, int contractMaxSalvagePercentage) {
        contract.setSalvageExchange(false);
        int roll = min(d6(2) + mod, 13);
        if (roll < 2) {
            contract.setSalvagePct(0);
        } else if (roll < 4) {
            contract.setSalvageExchange(true);
            int r;
            do {
                r = d6(2);
            } while (r < 4);
            contract.setSalvagePct(min((r - 3) * 10, contractMaxSalvagePercentage));
        } else {
            contract.setSalvagePct(min((roll - 3) * 10, contractMaxSalvagePercentage));
        }
    }

    protected void rollSupportClause(AtBContract contract, int mod) {
        int roll = d6(2) + mod;
        contract.setStraightSupport(0);
        contract.setBattleLossComp(0);
        if (roll < 3) {
            contract.setStraightSupport(0);
        } else if (roll < 8) {
            contract.setStraightSupport((roll - 2) * 20);
        } else if (roll == 8) {
            contract.setBattleLossComp(10);
        } else {
            contract.setBattleLossComp(min((roll - 8) * 20, 100));
        }
    }

    protected void rollTransportClause(AtBContract contract, int mod) {
        int roll = d6(2) + mod;
        if (roll < 2) {
            contract.setTransportComp(0);
        } else if (roll < 6) {
            contract.setTransportComp((20 + (roll - 2) * 5));
        } else if (roll < 10) {
            contract.setTransportComp((45 + (roll - 6) * 5));
        } else {
            contract.setTransportComp(100);
        }
    }

    protected void setEnemyCode(AtBContract contract) {
        if (contract.getContractType().isPirateHunting()) {
            Faction employer = contract.getEmployerFaction();
            contract.setEnemyCode(employer.isClan() ? "BAN" : PIRATE_FACTION_CODE);
        } else if (contract.getContractType().isRiotDuty()) {
            contract.setEnemyCode("REB");
        } else if (contract.getEmployerCode().equals(PIRATE_FACTION_CODE)) {
            RandomFactionGenerator factionGenerator = RandomFactionGenerator.getInstance();
            Set<String> localFactions = new HashSet<>(factionGenerator.getCurrentFactions());
            String enemyCode = ObjectUtility.getRandomItem(localFactions);
            contract.setEnemyCode(enemyCode);
        } else {
            contract.setEnemyCode(RandomFactionGenerator.getInstance()
                                        .getEnemy(contract.getEmployerCode(),
                                              contract.getContractType().isGarrisonType()));
        }
    }

    protected void setAttacker(AtBContract contract) {
        boolean isAttacker = !contract.getContractType().isGarrisonType() ||
                                   (contract.getContractType().isReliefDuty() && (d6() < 4)) ||
                                   contract.getEnemy().isRebel();
        contract.setAttacker(isAttacker);
    }

    protected void setSystemId(AtBContract contract) throws NoContractLocationFoundException {
        if (contract.isAttacker()) {
            contract.setSystemId(RandomFactionGenerator.getInstance()
                                       .getMissionTarget(contract.getEmployerCode(), contract.getEnemyCode()));
        } else {
            contract.setSystemId(RandomFactionGenerator.getInstance()
                                       .getMissionTarget(contract.getEnemyCode(), contract.getEmployerCode()));
        }
        if (contract.getSystem() == null) {
            String errorMsg = "Could not find contract location for " +
                                    contract.getEmployerCode() +
                                    " vs. " +
                                    contract.getEnemyCode();
            logger.warn(errorMsg);
            throw new NoContractLocationFoundException(errorMsg);
        }
    }

    protected void setIsRiotDuty(AtBContract contract) {
        if (contract.getContractType().isGarrisonDuty() && contract.getEnemy().isRebel()) {
            contract.setContractType(AtBContractType.RIOT_DUTY);
        }
    }

    /**
     * Calculates and sets the ally skill and quality ratings for the given contract.
     *
     * <p>The ally rating is influenced by multiple factors:</p>
     * <ul>
     *     <li>The employer faction's specific modifiers.</li>
     *     <li>Modifiers based on the contract type (e.g., attacking vs. defending roles).</li>
     *     <li>Historical context derived from the year parameter.</li>
     *     <li>The player's average skill level, used to adjust contract difficulty.</li>
     *     <li>A random roll for variability in calculations.</li>
     * </ul>
     *
     * <p>Special considerations are made for specific factions:</p>
     * <ul>
     *     <li><b>Clan Factions</b>: Enforce minimum ally skill levels based on attacking or defending roles.</li>
     *     <li><b>ComStar or Word of Blake</b>: Apply additional historical modifier adjustments.</li>
     * </ul>
     *
     * <p>For non-Clan and non-ComStar/Word of Blake factions, the ally ratings are further adjusted based
     * on the player's average skill level, making the contract easier if the player's skill level is lower.</p>
     *
     * <p>After all the calculations, the resulting ally skill and quality ratings are assigned to the contract.</p>
     *
     * @param contract                  the {@link AtBContract} instance for which the ally ratings are being calculated
     *                                  and assigned.
     * @param year                      the year of the contract, used for applying historical context modifiers.
     * @param averageSkillLevel         the average skill level of the player, used to adjust contract difficulty.
     * @param isUseBolsterContractSkill {@code true} to increase ally skill
     */
    protected void setAllyRating(AtBContract contract, int year, SkillLevel averageSkillLevel,
          boolean isUseBolsterContractSkill) {
        final Faction employerFaction = contract.getEmployerFaction();

        int mod = calculateFactionModifiers(contract.getEmployerFaction());
        mod += calculateContractTypeModifiers(contract.getContractType(), contract.isAttacker());

        // The less skilled the player, the easier their contract.
        mod += REGULAR.getExperienceLevel() - averageSkillLevel.getExperienceLevel();

        // Assign ally skill rating
        contract.setAllySkill(getSkillRating(d6(2) + mod, isUseBolsterContractSkill));

        // Apply faction modifiers
        if (employerFaction.isClan()) {
            // Apply Clan clamping
            if (contract.isAttacker()) {
                if (contract.getAllySkill().ordinal() < VETERAN.ordinal()) {
                    contract.setAllySkill(VETERAN);
                }
            } else {
                if (contract.getAllySkill().ordinal() < REGULAR.ordinal()) {
                    contract.setAllySkill(SkillLevel.REGULAR);
                }
            }
        } else {
            mod += calculateHistoricalModifiers(year);

            if (employerFaction.isComStarOrWoB()) {
                mod += 2;
            }
        }

        // Assign ally quality rating
        contract.setAllyQuality(getQualityRating(d6(2) + mod));
    }

    /**
     * Calculates and sets the enemy skill and quality ratings for the given contract.
     *
     * <p>The enemy rating is influenced by various factors:</p>
     * <ul>
     *     <li>Modifiers based on the enemy faction's attributes.</li>
     *     <li>The enemy faction's role in the contract (e.g., attacking or defending).</li>
     *     <li>Historical context derived from the year parameter.</li>
     *     <li>The player's average skill level, used to adjust the enemy difficulty.</li>
     *     <li>A random roll to introduce variability into the calculations.</li>
     * </ul>
     *
     * <p>Special adjustments are made for specific factions:</p>
     * <ul>
     *     <li><b>Clan Factions</b>: Enforce minimum enemy skill levels based on their roles as attackers or defenders.</li>
     *     <li><b>ComStar or Word of Blake</b>: Apply additional historical modifier adjustments.</li>
     * </ul>
     *
     * <p>For non-Clan and non-ComStar/Word of Blake factions, the enemy ratings are further adjusted
     * based on the player's average skill level, making contracts more difficult against weaker factions
     * or easier when the enemy's overall experience level is lower.</p>
     *
     * <p>After the calculations, the resulting enemy skill and quality ratings are applied to the contract.</p>
     *  @param contract          the {@link AtBContract} instance for which the enemy ratings are being calculated and
     *                          assigned.
     *
     * @param year                      the year of the contract, used for applying historical context modifiers.
     * @param averageSkillLevel         the average skill level of the player, used to adjust the enemy contract
     *                                  difficulty.
     * @param isUseBolsterContractSkill {@code true} to increase ally skill
     */
    protected void setEnemyRating(AtBContract contract, int year, SkillLevel averageSkillLevel,
          boolean isUseBolsterContractSkill) {
        Faction enemyFaction = Factions.getInstance().getFaction(contract.getEnemyCode());
        int mod = calculateFactionModifiers(enemyFaction);

        // Adjust modifiers based on attack/defense roles
        if (!contract.isAttacker()) {
            mod += 1;
        }

        // The less skilled the player, the easier their contract.
        mod += averageSkillLevel.getExperienceLevel() - REGULAR.getExperienceLevel();

        // Assign enemy skill rating
        contract.setEnemySkill(getSkillRating(d6(2) + mod, isUseBolsterContractSkill));

        // Apply faction modifiers
        if (enemyFaction.isClan()) {
            // Apply Clan clamping
            if (!contract.isAttacker()) {
                if (contract.getEnemySkill().ordinal() < VETERAN.ordinal()) {
                    contract.setEnemySkill(VETERAN);
                }
            } else {
                if (contract.getEnemySkill().ordinal() < REGULAR.ordinal()) {
                    contract.setEnemySkill(SkillLevel.REGULAR);
                }
            }
        } else {
            mod += calculateHistoricalModifiers(year);

            if (enemyFaction.isComStarOrWoB()) {
                mod += 2;
            }
        }

        // Assign enemy quality rating
        contract.setEnemyQuality(getQualityRating(d6(2) + mod));
    }

    /**
     * Calculates the modifiers for a faction based on its attributes, such as whether it is: a rebel, pirate,
     * independent, a minor power, or a Clan faction.
     *
     * <p>Faction modifiers are determined as follows:</p>
     * <ul>
     *   <li>Rebel or Pirate factions receive a penalty of -3.</li>
     *   <li>Independent factions receive a penalty of -2.</li>
     *   <li>Minor powers receive a penalty of -1.</li>
     *   <li>Clan factions receive a bonus of +4.</li>
     * </ul>
     *
     * @param faction the faction for which the modifiers are being calculated.
     *
     * @return the calculated modifier for the faction.
     */
    private int calculateFactionModifiers(Faction faction) {
        int mod = 0;

        if (faction.isRebelOrPirate()) {
            mod -= 3;
        }

        if (faction.isIndependent()) {
            mod -= 2;
        }

        if (faction.isMinorPower()) {
            mod -= 1;
        }

        if (faction.isClan()) {
            mod += 4;
        }

        return mod;
    }

    /**
     * Calculates the modifiers for a contract based on its type and whether the faction is in an attacker role or
     * defender role.
     *
     * <p>Contract type modifiers are determined as follows:</p>
     * <ul>
     *   <li>Guerrilla Warfare or Cadre Duty incurs a penalty of -3.</li>
     *   <li>Garrison Duty or Security Duty incurs a penalty of -2.</li>
     *   <li>An attacking faction receives a bonus of +1.</li>
     * </ul>
     *
     * @param contractType the type of the contract (e.g., Guerrilla Warfare, Cadre Duty, etc.).
     * @param isAttacker   a boolean indicating whether the faction is in an attacker role.
     *
     * @return the calculated modifier for the contract type.
     */
    private int calculateContractTypeModifiers(AtBContractType contractType, boolean isAttacker) {
        int mod = 0;

        if (contractType.isGuerrillaType() || contractType.isCadreDuty()) {
            mod -= 3;
        } else if (contractType.isGarrisonDuty() || contractType.isSecurityDuty()) {
            mod -= 2;
        }

        if (isAttacker) {
            mod += 1;
        }

        return mod;
    }

    /**
     * Calculates modifiers based on the historical period in which the given year falls. Modifiers are applied to
     * non-Clan factions based on the progressive degradation or recovery of combat capabilities during the Succession
     * Wars and Renaissance periods.
     *
     * <p>The modifiers are determined as follows:</p>
     * <ul>
     *   <li>The Second Succession War (2830-2865): a penalty of -1.</li>
     *   <li>The Third Succession War (2866-3038): a penalty of -2.</li>
     *   <li>The Renaissance start period (3039-3049): a penalty of -1.</li>
     * </ul>
     *
     * @param year the year of the contract, which determines the historical period.
     *
     * @return the calculated historical modifier to be applied.
     */
    private int calculateHistoricalModifiers(int year) {
        final int SECOND_SUCCESSION_WAR_START = 2830;
        final int THIRD_SUCCESSION_WAR_START = 2866;
        final int RENAISSANCE_START = 3039;
        final int RENAISSANCE_END = 3049;

        int mod = 0;

        if ((year >= SECOND_SUCCESSION_WAR_START) && (year < THIRD_SUCCESSION_WAR_START)) {
            mod -= 1;
        } else if ((year >= THIRD_SUCCESSION_WAR_START) && (year < RENAISSANCE_START)) {
            mod -= 2;
        } else if (year >= RENAISSANCE_START) {
            if (year < RENAISSANCE_END) {
                mod -= 1;
            }
        }

        return mod;
    }

    /**
     * @deprecated use {@link #writeToXML(Campaign, PrintWriter, int)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void writeToXML(final PrintWriter pw, int indent) {
    }

    public void writeToXML(Campaign campaign, final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "contractMarket");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "method", method.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastId", lastId);
        for (final Contract contract : contracts) {
            contract.writeToXML(campaign, pw, indent);
        }

        for (final Integer key : clauseMods.keySet()) {
            if (!contractIds.containsKey(key)) {
                continue;
            }

            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "clauseMods", "id", key);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mods", clauseMods.get(key).mods);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rerollsUsed", clauseMods.get(key).rerollsUsed);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "clauseMods");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "contractMarket");
    }

    public static AbstractContractMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
        AbstractContractMarket retVal = null;

        try {
            retVal = parseMarketMethod(wn);
            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            // Loop through the nodes and load our contract offers
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (wn2.getNodeName().equalsIgnoreCase("lastId")) {
                    retVal.lastId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mission")) {
                    Mission m = Mission.generateInstanceFromXML(wn2, c, version);

                    if (m instanceof Contract) {
                        retVal.contracts.add((Contract) m);
                        retVal.contractIds.put(m.getId(), (Contract) m);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("clauseMods")) {
                    int key = Integer.parseInt(wn2.getAttributes().getNamedItem("id").getTextContent());
                    ClauseMods cm = new ClauseMods();
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("mods")) {
                            String[] s = wn3.getTextContent().split(",");
                            for (int j = 0; j < s.length; j++) {
                                cm.mods[j] = Integer.parseInt(s[j]);
                            }
                        } else if (wn3.getNodeName().equalsIgnoreCase("rerollsUsed")) {
                            String[] s = wn3.getTextContent().split(",");
                            for (int j = 0; j < s.length; j++) {
                                cm.rerollsUsed[j] = Integer.parseInt(s[j]);
                            }
                        }
                    }
                    retVal.clauseMods.put(key, cm);
                }
            }

            // Restore any parent contract references
            for (Contract contract : retVal.contracts) {
                if (contract instanceof AtBContract atbContract) {
                    atbContract.restore(c);
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            logger.error("", ex);
        }

        return retVal;
    }

    private static AbstractContractMarket parseMarketMethod(Node xmlNode) {
        AbstractContractMarket market = null;
        NodeList nodeList = xmlNode.getChildNodes();
        for (int x = 0; x < nodeList.getLength(); x++) {
            Node childNode = nodeList.item(x);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (childNode.getNodeName().equalsIgnoreCase("method")) {
                String name = childNode.getTextContent();
                if (Objects.equals(name, ContractMarketMethod.CAM_OPS.toString())) {
                    market = new CamOpsContractMarket();
                    break;
                } else if (Objects.equals(name, ContractMarketMethod.NONE.toString())) {
                    market = new DisabledContractMarket();
                    break;
                } else {
                    market = new AtbMonthlyContractMarket();
                    break;
                }
            }
        }
        if (market == null) {
            logger.warn("No Contract Market method found in XML...falling back to AtB_Monthly");
            market = new AtbMonthlyContractMarket();
        }
        return market;
    }

    /* Keep track of how many rerolls remain for each contract clause
     * based on the admin's negotiation skill. Also track bonuses, as
     * the random clause bonuses should be persistent.
     */
    protected static class ClauseMods {
        public int[] rerollsUsed = { 0, 0, 0, 0 };
        public int[] mods = { 0, 0, 0, 0 };
    }

    /**
     * Exception indicating that no valid location was generated for a contract and that the contract is invalid.
     */
    public static class NoContractLocationFoundException extends RuntimeException {
        public NoContractLocationFoundException(String message) {
            super(message);
        }
    }
}
