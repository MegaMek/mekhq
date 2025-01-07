package mekhq.campaign.market.contractMarket;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.common.Compute.d6;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.force.CombatTeam.getStandardForceSize;
import static mekhq.campaign.mission.AtBContract.getEffectiveNumUnits;

/**
 * Abstract base class for various Contract Market types in AtB/Stratcon. Responsible for generation
 * and initialization of AtBContracts.
 */
public abstract class AbstractContractMarket {
    public static final int CLAUSE_COMMAND = 0;
    public static final int CLAUSE_SALVAGE = 1;
    public static final int CLAUSE_SUPPORT = 2;
    public static final int CLAUSE_TRANSPORT = 3;
    public static final int CLAUSE_NUM = 4;


    protected List<Contract> contracts = new ArrayList<>();
    protected int lastId = 0;
    protected Map<Integer, Contract> contractIds = new HashMap<>();
    protected Map<Integer, ClauseMods> clauseMods = new HashMap<>();

    /**
     * An arbitrary maximum number of attempts to generate a contract.
     */
    protected final static int MAXIMUM_GENERATION_RETRIES = 3;

    /* It is possible to call addFollowup more than once for the
     * same contract by canceling the dialog and running it again;
     * this is the easiest place to track it to prevent
     * multiple followup contracts.
     * key: followup id
     * value: main contract id
     */
    protected HashMap<Integer, Integer> followupContracts = new HashMap<>();

    /**
     * An arbitrary maximum number of attempts to find a random employer faction that
     * is not a Mercenary.
     */
    protected final static int MAXIMUM_ATTEMPTS_TO_FIND_NON_MERC_EMPLOYER = 20;

    private final ContractMarketMethod method;
    private static final MMLogger logger = MMLogger.create(AbstractContractMarket.class);


    /**
     * Generate a new contract and add it to the market.
     * @param campaign
     * @return The newly generated contract
     */
    abstract public AtBContract addAtBContract(Campaign campaign);

    /**
     * Generate available contract offers for the player's force.
     * @param campaign
     * @param newCampaign Boolean indicating whether this is a fresh campaign.
     */
    abstract public void generateContractOffers(Campaign campaign, boolean newCampaign);

    /**
     * Generate followup contracts and add them to the market if the currently selected market type
     * supports them.
     *
     * @param campaign The current campaign.
     * @param contract The AtBContract being completed and used as a basis for followup missions
     */
    abstract public void checkForFollowup(Campaign campaign, AtBContract contract);

    /**
     * Calculate the total payment modifier for the contract based on the configured market method
     * (e.g., CAM_OPS, ATB_MONTHLY).
     * @param campaign
     * @param contract
     * @return a double representing the total payment multiplier.
     */
    abstract public double calculatePaymentMultiplier(Campaign campaign, AtBContract contract);

    protected AbstractContractMarket(final ContractMarketMethod method) {
        this.method = method;
    }

    /**
     *
     * @return the Method (e.g., CAM_OPS, ATB_MONTHLY) associated with the Contract Market instance
     */
    public ContractMarketMethod getMethod() {
        return method;
    }

    /**
     * Empty an available contract from the market.
     * @param c contract to remove
     */
    public void removeContract(Contract c) {
        contracts.remove(c);
        contractIds.remove(c.getId());
        clauseMods.remove(c.getId());
        followupContracts.remove(c.getId());
    }

    /**
     * Rerolls a specific clause in a contract, usually via negotiation.
     * @param c the contract being negotiated
     * @param clause ID representing the type of clause.
     * @param campaign
     */
    public void rerollClause(AtBContract c, int clause, Campaign campaign) {
        if (null != clauseMods.get(c.getId())) {
            switch (clause) {
                case CLAUSE_COMMAND -> rollCommandClause(c, clauseMods.get(c.getId()).mods[clause]);
                case CLAUSE_SALVAGE ->
                    rollSalvageClause(c, clauseMods.get(c.getId()).mods[clause], campaign.getCampaignOptions().getContractMaxSalvagePercentage());
                case CLAUSE_TRANSPORT -> rollTransportClause(c, clauseMods.get(c.getId()).mods[clause]);
                case CLAUSE_SUPPORT -> rollSupportClause(c, clauseMods.get(c.getId()).mods[clause]);
            }
            clauseMods.get(c.getId()).rerollsUsed[clause]++;
            c.calculateContract(campaign);
        }
    }

    /**
     * Returns the number of rerolls used so far for a specific clause.
     * @param c
     * @param clause ID representing the type of clause.
     * @return
     */
    public int getRerollsUsed(Contract c, int clause) {
        if (null != clauseMods.get(c.getId())) {
            return clauseMods.get(c.getId()).rerollsUsed[clause];
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
     * @param campaign
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
     * Determines the number of required lances to be deployed for a contract. For Mercenary subcontracts
     * this defaults to 1; otherwise, the number is based on the number of combat units in the
     * campaign. Modified by a 2d6 roll if {@code bypassVariance} is {@code false}.
     *
     * @param campaign the current campaign
     * @param contract the relevant contract
     * @param bypassVariance if {@code true} requirements will not be semi-randomized.
     * @return The number of lances required to be deployed.
     */
    public int calculateRequiredLances(Campaign campaign, AtBContract contract, boolean bypassVariance) {
        if (contract.isSubcontract()) {
            return 1;
        }

        int formationSize = getStandardForceSize(campaign.getFaction());
        int availableForces = max(getEffectiveNumUnits(campaign) / formationSize, 1);
        int maxDeployedLances = availableForces;

        if (campaign.getCampaignOptions().isUseStrategy()) {
            maxDeployedLances = max(calculateMaxDeployedLances(campaign), 1);
        }

        availableForces = min(availableForces, maxDeployedLances);

        // If we're bypassing variance, we can early exit here
        if (bypassVariance) {
            availableForces -= (int) floor((double) availableForces / 3);

            return max(availableForces, 1);
        }

        // Otherwise, we roll to determine the amount we divide availableForces by
        int roll = d6(2);
        double varianceFactor = switch (roll) {
            case 2 -> 4.5;
            case 3 -> 4;
            case 4 -> 3.5;
            case 10 -> 2.5;
            case 11 -> 2;
            case 12 -> 1.5;
            default -> 3;
        };

        availableForces -= (int) floor((double) availableForces / varianceFactor);

        return max(availableForces, 1);
    }

    /**
     * Determine the maximum number of lances the force can deploy. The result is based on the
     * commander's Strategy skill and various campaign options.
     * @param campaign
     * @return the maximum number of lances that can be deployed on the contract.
     */
    public int calculateMaxDeployedLances(Campaign campaign) {
        return campaign.getCampaignOptions().getBaseStrategyDeployment() +
            campaign.getCampaignOptions().getAdditionalStrategyDeployment() *
                campaign.getCommanderStrategy();
    }

    protected SkillLevel getSkillRating(int roll) {
        if (roll <= 5) {
            return SkillLevel.GREEN;
        } else if (roll <= 9) {
            return SkillLevel.REGULAR;
        } else if (roll <= 11) {
            return VETERAN;
        } else {
            return SkillLevel.ELITE;
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

    protected void rollCommandClause(final Contract contract, final int modifier) {
        final int roll = d6(2) + modifier;
        if (roll < 3) {
            contract.setCommandRights(ContractCommandRights.INTEGRATED);
        } else if (roll < 8) {
            contract.setCommandRights(ContractCommandRights.HOUSE);
        } else if (roll < 12) {
            contract.setCommandRights(ContractCommandRights.LIAISON);
        } else {
            contract.setCommandRights(ContractCommandRights.INDEPENDENT);
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

    protected AtBContractType findMissionType(int unitRatingMod, boolean majorPower) {
        final AtBContractType[][] table = {
            // col 0: IS Houses
            { AtBContractType.GUERRILLA_WARFARE, AtBContractType.RECON_RAID, AtBContractType.PIRATE_HUNTING,
                AtBContractType.PLANETARY_ASSAULT, AtBContractType.OBJECTIVE_RAID,
                AtBContractType.OBJECTIVE_RAID,
                AtBContractType.EXTRACTION_RAID, AtBContractType.RECON_RAID, AtBContractType.GARRISON_DUTY,
                AtBContractType.CADRE_DUTY, AtBContractType.RELIEF_DUTY },
            // col 1: Others
            { AtBContractType.GUERRILLA_WARFARE, AtBContractType.RECON_RAID, AtBContractType.PLANETARY_ASSAULT,
                AtBContractType.OBJECTIVE_RAID, AtBContractType.EXTRACTION_RAID, AtBContractType.PIRATE_HUNTING,
                AtBContractType.SECURITY_DUTY, AtBContractType.OBJECTIVE_RAID, AtBContractType.GARRISON_DUTY,
                AtBContractType.CADRE_DUTY, AtBContractType.DIVERSIONARY_RAID }
        };
        int roll = MathUtility.clamp(d6(2) + unitRatingMod - IUnitRating.DRAGOON_C, 2, 12);
        return table[majorPower ? 0 : 1][roll - 2];
    }

    protected void setEnemyCode(AtBContract contract) {
        if (contract.getContractType().isPirateHunting()) {
            contract.setEnemyCode("PIR");
        } else if (contract.getContractType().isRiotDuty()) {
            contract.setEnemyCode("REB");
        } else {
            contract.setEnemyCode(RandomFactionGenerator.getInstance().getEnemy(contract.getEmployerCode(),
                contract.getContractType().isGarrisonType()));
        }
    }

    protected void setAttacker(AtBContract contract) {
        boolean isAttacker = !contract.getContractType().isGarrisonType()
            || (contract.getContractType().isReliefDuty() && (d6() < 4))
            || contract.getEnemy().isRebel();
        contract.setAttacker(isAttacker);
    }

    protected void setSystemId(AtBContract contract) throws NoContractLocationFoundException {
        // FIXME : Windchild : I don't work properly
        if (contract.isAttacker()) {
            contract.setSystemId(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEmployerCode(),
                contract.getEnemyCode()));
        } else {
            contract.setSystemId(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEnemyCode(),
                contract.getEmployerCode()));
        }
        if (contract.getSystem() == null) {
            String errorMsg = "Could not find contract location for "
                + contract.getEmployerCode() + " vs. " + contract.getEnemyCode();
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
     * Sets the ally rating (skill and quality) for the contract.
     * The ally rating is determined by modifiers influenced by the employer faction,
     * contract type, historical context, and a random roll.
     *
     * <p>The calculated ally skill and quality ratings are assigned to the contract.</p>
     *
     * @param contract the contract for which the ally rating is being set.
     * @param year     the year of the contract, used for calculating historical modifiers.
     */
    protected void setAllyRating(AtBContract contract, int year) {
        int mod = calculateFactionModifiers(contract.getEmployerFaction());
        mod += calculateContractTypeModifiers(contract.getContractType(), contract.isAttacker());

        // Assign ally skill rating
        contract.setAllySkill(getSkillRating(d6(2) + mod));

        // Apply historical modifiers
        if (!contract.getEmployerFaction().isClan()) {
            mod += calculateHistoricalModifiers(year);
        } else {
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
        }

        // Assign ally quality rating
        contract.setAllyQuality(getQualityRating(d6(2) + mod));
    }

    /**
     * Sets the enemy rating (skill and quality) for the contract.
     * The enemy rating is determined by modifiers based on the enemy faction,
     * whether the faction is attacking or defending, historical context, and a random roll.
     *
     * <p>The calculated enemy skill and quality ratings are assigned to the contract.</p>
     *
     * @param contract the contract for which the enemy rating is being set.
     * @param year     the year of the contract, used for calculating historical modifiers.
     */
    protected void setEnemyRating(AtBContract contract, int year) {
        Faction enemyFaction = Factions.getInstance().getFaction(contract.getEnemyCode());
        int mod = calculateFactionModifiers(enemyFaction);

        // Adjust modifiers based on attack/defense roles
        if (!contract.isAttacker()) {
            mod += 1;
        }

        // Assign enemy skill rating
        contract.setEnemySkill(getSkillRating(d6(2) + mod));

        // Apply historical modifiers
        if (!enemyFaction.isClan()) {
            mod += calculateHistoricalModifiers(year);
        } else {
            // Apply Clan clamping
            if (!contract.isAttacker()) {
                if (contract.getAllySkill().ordinal() < VETERAN.ordinal()) {
                    contract.setAllySkill(VETERAN);
                }
            } else {
                if (contract.getAllySkill().ordinal() < REGULAR.ordinal()) {
                    contract.setAllySkill(SkillLevel.REGULAR);
                }
            }
        }

        // Assign enemy quality rating
        contract.setEnemyQuality(getQualityRating(d6(2) + mod));
    }

    /**
     * Calculates the modifiers for a faction based on its attributes, such as whether it is:
     * a rebel, pirate, independent, a minor power, or a Clan faction.
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
     * Calculates the modifiers for a contract based on its type and whether the faction
     * is in an attacker role or defender role.
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
     * @return the calculated modifier for the contract type.
     */
    private int calculateContractTypeModifiers(AtBContractType contractType, boolean isAttacker) {
        int mod = 0;

        if (contractType.isGuerrillaWarfare() || contractType.isCadreDuty()) {
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
     * Calculates modifiers based on the historical period in which the given year falls.
     * Modifiers are applied to non-Clan factions based on the progressive degradation or
     * recovery of combat capabilities during the Succession Wars and Renaissance periods.
     *
     * <p>The modifiers are determined as follows:</p>
     * <ul>
     *   <li>The Second Succession War (2830-2865): a penalty of -1.</li>
     *   <li>The Third Succession War (2866-3038): a penalty of -2.</li>
     *   <li>The Renaissance start period (3039-3049): a penalty of -1.</li>
     * </ul>
     *
     * @param year the year of the contract, which determines the historical period.
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

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "contractMarket");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "method", method.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastId", lastId);
        for (final Contract contract : contracts) {
            contract.writeToXML(pw, indent);
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
                            String [] s = wn3.getTextContent().split(",");
                            for (int j = 0; j < s.length; j++) {
                                cm.mods[j] = Integer.parseInt(s[j]);
                            }
                        } else if (wn3.getNodeName().equalsIgnoreCase("rerollsUsed")) {
                            String [] s = wn3.getTextContent().split(",");
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
        public int[] rerollsUsed = {0, 0, 0, 0};
        public int[] mods = {0, 0, 0, 0};
    }

    /**
     * Exception indicating that no valid location was generated for a contract and that the contract
     * is invalid.
     */
    public static class NoContractLocationFoundException extends RuntimeException {
        public NoContractLocationFoundException(String message) {
            super(message);
        }
    }
}
