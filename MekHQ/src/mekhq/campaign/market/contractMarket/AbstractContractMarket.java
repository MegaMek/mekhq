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
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.common.Compute.d6;
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
     * @param campaign the current campaign
     * @param contract the relevant contract
     * @param bypassVariance if {@code true} requirements will not be semi-randomized.
     * @return The number of lances required to be deployed.
     */
    public int calculateRequiredLances(Campaign campaign, AtBContract contract, boolean bypassVariance) {
        int maxDeployedLances = max(calculateMaxDeployedLances(campaign), 1);
        if (contract.isSubcontract()) {
            return 1;
        } else {
            int formationSize = getStandardForceSize(campaign.getFaction());
            int availableForces = max(getEffectiveNumUnits(campaign) / formationSize, 1);

            // We allow for one reserve force per 3 depth 0 forces (lances, etc)
            availableForces -= max((int) floor((double) availableForces / 3), 1);

            if (!bypassVariance) {
                int roll = d6(2);

                if (roll == 2) {
                    availableForces = (int) round((double) availableForces * 0.25);
                } else if (roll == 3) {
                    availableForces = (int) round((double) availableForces * 0.5);
                } else if (roll < 5) {
                    availableForces = (int) round((double) availableForces * 0.75);
                } else if (roll == 12) {
                    availableForces = (int) round((double) availableForces * 1.75);
                } else if (roll == 11) {
                    availableForces = (int) round((double) availableForces * 1.5);
                } else if (roll > 9) {
                    availableForces = (int) round((double) availableForces * 1.25);
                }
            }

            return MathUtility.clamp(availableForces, 1, maxDeployedLances);
        }
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
            return SkillLevel.VETERAN;
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
        int roll = Math.min(d6(2) + mod, 13);
        if (roll < 2) {
            contract.setSalvagePct(0);
        } else if (roll < 4) {
            contract.setSalvageExchange(true);
            int r;
            do {
                r = d6(2);
            } while (r < 4);
            contract.setSalvagePct(Math.min((r - 3) * 10, contractMaxSalvagePercentage));
        } else {
            contract.setSalvagePct(Math.min((roll - 3) * 10, contractMaxSalvagePercentage));
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
            contract.setBattleLossComp(Math.min((roll - 8) * 20, 100));
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

    protected void setAllyRating(AtBContract contract, int year) {
        int mod = 0;
        if (contract.getEnemy().isRebelOrPirate()) {
            mod -= 1;
        }

        if (contract.getContractType().isGuerrillaWarfare() || contract.getContractType().isCadreDuty()) {
            mod -= 3;
        } else if (contract.getContractType().isGarrisonDuty() || contract.getContractType().isSecurityDuty()) {
            mod -= 2;
        }

        if (AtBContract.isMinorPower(contract.getEmployerCode())) {
            mod -= 1;
        }

        if (contract.getEnemy().isIndependent()) {
            mod -= 2;
        }

        if (contract.getContractType().isPlanetaryAssault()) {
            mod += 1;
        }

        if (Factions.getInstance().getFaction(contract.getEmployerCode()).isClan() && !contract.isAttacker()) {
            // facing front-line units
            mod += 1;
        }
        contract.setAllySkill(getSkillRating(d6(2) + mod));
        if (year > 2950 && year < 3039 &&
            !Factions.getInstance().getFaction(contract.getEmployerCode()).isClan()) {
            mod -= 1;
        }
        contract.setAllyQuality(getQualityRating(d6(2) + mod));
    }

    protected void setEnemyRating(AtBContract contract, int year) {
        int mod = 0;
        if (contract.getEnemy().isRebelOrPirate()) {
            mod -= 2;
        }
        if (contract.getContractType().isGuerrillaWarfare()) {
            mod += 2;
        }
        if (contract.getContractType().isPlanetaryAssault()) {
            mod += 1;
        }
        if (AtBContract.isMinorPower(contract.getEmployerCode())) {
            mod -= 1;
        }
        if (Factions.getInstance().getFaction(contract.getEmployerCode()).isClan()) {
            mod += contract.isAttacker() ? 2 : 4;
        }
        contract.setEnemySkill(getSkillRating(d6(2) + mod));
        if (year > 2950 && year < 3039 &&
            !Factions.getInstance().getFaction(contract.getEnemyCode()).isClan()) {
            mod -= 1;
        }
        contract.setEnemyQuality(getQualityRating(d6(2) + mod));
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
