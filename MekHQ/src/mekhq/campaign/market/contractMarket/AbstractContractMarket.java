package mekhq.campaign.market.contractMarket;

import megamek.Version;
import megamek.common.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.rating.IUnitRating;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

public abstract class AbstractContractMarket {
    public static final int CLAUSE_COMMAND = 0;
    public static final int CLAUSE_SALVAGE = 1;
    public static final int CLAUSE_SUPPORT = 2;
    public static final int CLAUSE_TRANSPORT = 3;
    public static final int CLAUSE_NUM = 4;

    protected static final MMLogger logger = MMLogger.create(AbstractContractMarket.class);
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


    abstract public AtBContract addAtBContract(Campaign campaign);
    abstract public void generateContractOffers(Campaign campaign, boolean newCampaign);
    abstract public void addFollowup(Campaign campaign, AtBContract contract);
    abstract protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign);

    protected AbstractContractMarket(final ContractMarketMethod method) {
        this.method = method;
    }

    public ContractMarketMethod getMethod() {
        return method;
    }

    public void removeContract(Contract c) {
        contracts.remove(c);
        contractIds.remove(c.getId());
        clauseMods.remove(c.getId());
        followupContracts.remove(c.getId());
    }

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

    public int getRerollsUsed(Contract c, int clause) {
        if (null != clauseMods.get(c.getId())) {
            return clauseMods.get(c.getId()).rerollsUsed[clause];
        }
        return 0;
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void generateContractOffers(Campaign campaign) {
        generateContractOffers(campaign, false);
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
        final int roll = Compute.d6(2) + modifier;
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
        int roll = Math.min(Compute.d6(2) + mod, 13);
        if (roll < 2) {
            contract.setSalvagePct(0);
        } else if (roll < 4) {
            contract.setSalvageExchange(true);
            int r;
            do {
                r = Compute.d6(2);
            } while (r < 4);
            contract.setSalvagePct(Math.min((r - 3) * 10, contractMaxSalvagePercentage));
        } else {
            contract.setSalvagePct(Math.min((roll - 3) * 10, contractMaxSalvagePercentage));
        }
    }

    protected void rollSupportClause(AtBContract contract, int mod) {
        int roll = Compute.d6(2) + mod;
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
        int roll = Compute.d6(2) + mod;
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
                if (contract instanceof AtBContract) {
                    final AtBContract atbContract = (AtBContract) contract;
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
}
