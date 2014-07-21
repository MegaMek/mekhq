/**
 * 
 */
package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.TargetRoll;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;

/**
 * @author Neoancient
 * 
 * Against the Bot
 * Utitlity class that handles retirement/defection rolls and final payments
 * to personnel who retire/defect/get sacked and families of those killed
 * in battle.
 *
 */
public class RetirementDefectionTracker implements Serializable, MekHqXmlSerializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7245317499458320654L;
	
	/* In case the dialog is closed after making the retirement rolls
	 * and determining payouts but before the retirees have been paid,
	 * we store those results to avoid making the rolls again.
	 */
	private HashSet<Integer> rollRequired;
	private HashMap<Integer, HashSet<UUID>> unresolvedPersonnel;
	private HashMap<UUID, Payout> payouts;
	private GregorianCalendar lastRetirementRoll;
	
	public RetirementDefectionTracker() {
		rollRequired = new HashSet<Integer>();
		unresolvedPersonnel = new HashMap<Integer, HashSet<UUID>>();
		payouts = new HashMap<UUID, Payout>();
		lastRetirementRoll = new GregorianCalendar();
	}

	public static long getShareValue(Campaign campaign) {
		if (!campaign.getCampaignOptions().getUseShareSystem()) {
			return 0;
		}
		String financialReport = campaign.getFinancialReport();
		long netWorth = 0;
		try {
			Pattern p = Pattern.compile("Net Worth\\D*(.*)");
			Matcher m = p.matcher(financialReport);
			m.find();
			netWorth = (Long)(new DecimalFormat().parse(m.group(1)));
		} catch (Exception e) {
			MekHQ.logError("Error parsing net worth in financial report");
			MekHQ.logError(e);
		}
		int totalShares = 0;
		for (Person p : campaign.getPersonnel()) {
			totalShares += p.getNumShares(campaign.getCampaignOptions().getSharesForAll());
		}
		return netWorth / totalShares;
	}

    public HashMap<UUID, TargetRoll> calculateTargetNumbers(AtBContract contract,
    		Campaign campaign) {
    	HashMap <UUID, TargetRoll> targets = new HashMap<UUID, TargetRoll>();
    	int combatLeadershipMod = 0;
    	int supportLeadershipMod = 0;
    	
    	if (null != contract) {
    		rollRequired.add(contract.getId());
    	}
    	
    	if (campaign.getCampaignOptions().getUseLeadership()) {
    		int combat = 0;
    		int proto = 0;
    		int support = 0;
    		for (Person p : campaign.getPersonnel()) {
    			if (!p.isActive() || p.getPrimaryRole() == Person.T_NONE ||
    					p.isDependent()) {
    				continue;
    			}
    			if (p.getPrimaryRole() >= Person.T_MECH_TECH) {
    				support++;
    			} else if (null == p.getUnitId() ||
    					campaign.getUnit(p.getUnitId()).isCommander(p)) {
    				/* The AtB rules do not state that crews count as a 
    				 * single person for leadership purposes, but to do otherwise
    				 * would tax all but the most exceptional commanders of
    				 * vehicle or infantry units.  
    				 */
    				if (p.getPrimaryRole() == Person.T_PROTO_PILOT) {
    					proto++;
    				} else {
    					combat++;
    				}
    			}
    		}
    		combat += proto / 5;
     		int max = 12;
    		if (null != campaign.getFlaggedCommander() &&
    				null != campaign.getFlaggedCommander().getSkill(SkillType.S_LEADER)) {
    			max += 6 * campaign.getFlaggedCommander().getSkill(SkillType.S_LEADER).getLevel();
    		}
    		if (combat > 2 * max) {
    			combatLeadershipMod = 2;
    		} else if (combat > max) {
    			combatLeadershipMod = 1;
    		}
    		if (support > 2 * max) {
    			supportLeadershipMod = 2;
    		} else if (support > max) {
    			supportLeadershipMod = 1;
    		}
    	}

    	for (Person p : campaign.getPersonnel()) {
    		if (!p.isActive() || p.isDependent()) {
    			continue;
    		}
    		/* Infantry units retire or defect by platoon */
    		if (null != p.getUnitId() && campaign.getUnit(p.getUnitId()).usesSoldiers() &&
    				!campaign.getUnit(p.getUnitId()).isCommander(p)) {
    			continue;
    		}
    		TargetRoll target = new TargetRoll(5, "Target");
    		target.addModifier(p.getExperienceLevel(false) - campaign.getUnitRatingMod(),
    				"Experience");
    		/* Retirement rolls are made before the contract status is set */
    		if (null != contract && (
    				contract.getStatus() == Mission.S_FAILED ||
    				contract.getStatus() == Mission.S_BREACH)) {
    			target.addModifier(1, "Failed mission");
    		}
    		if (campaign.getFactionCode().equals("PIR")) {
    			target.addModifier(1, "Pirate");
    		}
    		if (p.getRank().isOfficer()) {
    			target.addModifier(-1, "Officer");
    		} else {
    	        for (Enumeration<IOption> i = p.getOptions(PilotOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
    	            IOption ability = i.nextElement();
    	            if (ability.booleanValue()) {
    	            	if (ability.getName().equals("tactical_genius")) {
    	            		target.addModifier(1, "Non-officer tactical genius");
    	            		break;
    	            	}
    	            }
    	        }
    		}
    		if (p.getAge(campaign.getCalendar()) >= 50) {
    			target.addModifier(1, "Over 50");
    		}
    		if (campaign.getCampaignOptions().getUseShareSystem()) {
    			/* If this retirement roll is not being made at the end
    			 * of a contract (e.g. >12 months since last roll), the
    			 * share percentage should still apply. In the case of multiple
    			 * active contracts, pick the one with the best percentage.
    			 */
    			AtBContract c = contract;
    			if (null == c) {
    				for (Mission m : campaign.getMissions()) {
    					if (m.isActive() && m instanceof AtBContract &&
    							(null == c || c.getSharesPct() < ((AtBContract)m).getSharesPct())) {
    						c = (AtBContract)m;
    					}
    				}
    			}
    			if (null != c && c.getSharesPct() > 20) {
    				target.addModifier(-((c.getSharesPct() - 20) / 10), "Shares");
        		}
    		} else {
    			//Bonus payments handled by dialog
    		}
    		if (p.getPrimaryRole() == Person.T_INFANTRY) {
    			target.addModifier(-1, "Infantry Platoon");
    		}
    		int injuryMod = 0;
    		for (Injury i : p.getInjuries()) {
    			if (i.getPermanent()) {
    				injuryMod++;
    			}
    		}
    		if (injuryMod > 0) {
    			target.addModifier(injuryMod, "Permanent injuries");
    		}
    		if (combatLeadershipMod != 0 && p.getPrimaryRole() < Person.T_MECH_TECH) {
    			target.addModifier(combatLeadershipMod, "Leadership");
    		}
    		if (supportLeadershipMod != 0 && p.getPrimaryRole() >= Person.T_MECH_TECH) {
    			target.addModifier(supportLeadershipMod, "Leadership");
    		}
    		
    		targets.put(p.getId(), target);
    	}
    	return targets;
    }
    
    public void rollRetirement(AtBContract contract,
    		HashMap<UUID, TargetRoll> targets, long shareValue, Campaign c) {
    	if (null != contract && !unresolvedPersonnel.keySet().contains(contract.getId())) {
    		unresolvedPersonnel.put(contract.getId(), new HashSet<UUID>());
    	}
    	for (UUID id : targets.keySet()) {
    		if (Compute.d6(2) < targets.get(id).getValue()) {
    			if (null != contract) {
    				unresolvedPersonnel.get(contract.getId()).add(id);
    			}
    			payouts.put(id, new Payout(c.getPerson(id),
    					shareValue, false, c.getCampaignOptions().getSharesForAll()));
    		}
    	}
    	if (null != contract) {
    		rollRequired.remove(contract.getId());
    	}
    	lastRetirementRoll.setTime(c.getDate());
    }
    
    public GregorianCalendar getLastRetirementRoll() {
    	return lastRetirementRoll;
    }
    
    public void setLastRetirementRoll(GregorianCalendar cal) {
    	lastRetirementRoll.setTime(cal.getTime());
    }
    
    public void removeFromCampaign(Person person, boolean killed,
    		int shares, Campaign campaign, AtBContract contract) {
    	payouts.put(person.getId(), new Payout(person, getShareValue(campaign),
    			killed, campaign.getCampaignOptions().getSharesForAll()));
    	if (null != contract) {
    		if (null == unresolvedPersonnel.get(contract.getId())) {
    			unresolvedPersonnel.put(contract.getId(), new HashSet<UUID>());
    		}
    		unresolvedPersonnel.get(contract.getId()).add(person.getId());
    	}
    }
    
    public void removePayout(Person person) {
    	payouts.remove(person.getId());
    }
    
    public boolean isOutstanding(AtBContract contract) {
    	return isOutstanding(contract.getId());
    }
    
    public boolean isOutstanding(int id) {
    	return unresolvedPersonnel.keySet().contains(id);
    }
    
    /* Called by when all payouts have been resolved for the contract.
     * If contract is null, the dialog has been invoked without a
     * specific contract and all outstanding payouts have been resolved.
     */
    public void resolveAllContracts() {
    	resolveContract(null);
    	payouts.clear();
    }
    
    public void resolveContract(AtBContract contract) {
    	if (null == contract) {
    		for (int id : unresolvedPersonnel.keySet()) {
    			resolveContract(id);
    		}
    	} else {
    		resolveContract(contract.getId());
    	}
    }
    
    public void resolveContract(int contractId) {
    	if (null != unresolvedPersonnel.get(contractId)) {
    		for (UUID pid : unresolvedPersonnel.get(contractId)) {
    			payouts.remove(pid);
    		}
    		unresolvedPersonnel.remove(contractId);
    	}
    	rollRequired.remove(contractId);
    }
    
    public Set<UUID> getRetirees() {
    	return getRetirees(null);
    }
    
    public Set<UUID> getRetirees(AtBContract contract) {
    	if (null != contract) {
    		return unresolvedPersonnel.get(contract.getId());
    	} else {
    		return payouts.keySet();
    	}
    }
    
    public Payout getPayout(UUID id) {
    	return payouts.get(id);
    }

	public static long getBonusCost(Person p) {
		switch (p.getExperienceLevel(false)) {
		case SkillType.EXP_ELITE:
			return (p.getProfession() == Ranks.RPROF_MW)?300000:150000;
		case SkillType.EXP_VETERAN:
			return (p.getProfession() == Ranks.RPROF_MW)?150000:50000;
		case SkillType.EXP_REGULAR:
			return (p.getProfession() == Ranks.RPROF_MW)?50000:20000;
		case SkillType.EXP_GREEN:
		default:
			return (p.getProfession() == Ranks.RPROF_MW)?20000:10000;
		}
	}
	
	public class Payout {
		int weightClass = 0;
		int dependents = 0;
		long cbills = 0;
		boolean recruit = false;
		int recruitType = Person.T_NONE;
		boolean heir = false;
		boolean stolenUnit = false;
		UUID stolenUnitId = null;
		
		public Payout() {}
		
		public Payout(Person p, long shareValue, boolean killed, boolean sharesForAll) {
			calculatePayout(p, killed, shareValue > 0);
			if (shareValue > 0) {
				cbills += shareValue * p.getNumShares(sharesForAll);
			}
			if (killed) {
				switch (Compute.d6()) {
				case 1:
					/* No effects */
					break;
				case 2:
					dependents = 1;
					break;
				case 3:
					dependents = Compute.d6();
					break;
				case 4:
				case 5:
					recruit = true;
					break;
				case 6:
					heir = true;
					break;
				}
			}
		}

		private void calculatePayout(Person p, boolean killed,
				boolean shareSystem) {
			int roll;
			if (killed) {
				roll = Utilities.dice(1, 5);
			} else {
				roll = Compute.d6() + Math.max(-1, p.getExperienceLevel(false) - 2);
				if (p.getRank().isOfficer()) {
					roll += 1;
				}
			}
			if (roll >= 6 && (p.getPrimaryRole() == Person.T_AERO_PILOT ||
					p.getPrimaryRole() == Person.T_AERO_PILOT)) {
				stolenUnit = true;
			} else {
				if (p.getProfession() == Ranks.RPROF_INF) {
					cbills = 50000;
				} else {
					cbills = getBonusCost(p);
					if (p.getRank().isOfficer()) {
						cbills *= 2;
					}
				}
				if (!shareSystem &&
						(p.getProfession() == Ranks.RPROF_MW ||
						p.getProfession() == Ranks.RPROF_ASF) &&
						p.getOriginalUnitWeight() > 0) {
					weightClass = p.getOriginalUnitWeight() +
							p.getOriginalUnitTech();
					if (roll <= 1) {
						weightClass--;
					}
					if (roll >= 5) {
						weightClass++;
					}
				}
			}
		}
		
		public int getWeightClass() {
			return weightClass;
		}
		
		public void setWeightClass(int weight) {
			weightClass = weight;
		}

		public int getDependents() {
			return dependents;
		}
		
		public void setDependents(int d) {
			dependents = d;
		}

		public long getCbills() {
			return cbills;
		}

		public void setCbills(long cbills) {
			this.cbills = cbills;
		}

		public boolean hasRecruit() {
			return recruit;
		}
		
		public void setRecruit(boolean r) {
			recruit = r;
		}
		
		public int getRecruitType() {
			return recruitType;
		}
		
		public void setRecruitType(int type) {
			recruitType = type;
		}

		public boolean hasHeir() {
			return heir;
		}
		
		public void setHeir(boolean h) {
			heir = h;
		}

		public boolean hasStolenUnit() {
			return stolenUnit;
		}
		
		public void setStolenUnit(boolean stolen) {
			stolenUnit = stolen;
		}
		
		public UUID getStolenUnitId() {
			return stolenUnitId;
		}
		
		public void setStolenUnitId(UUID id) {
			stolenUnitId = id;
		}
	}

	private String createCsv(Collection<? extends Object> coll) {
		String retVal = "";
		if (coll.size() > 0) {
			for (Object o : coll) {
				retVal += o.toString() + ",";
			}
			return retVal.substring(0, retVal.length() - 1);
		}
		return "";
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<retirementDefectionTracker>");
        
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1,
        		"rollRequired",
        		createCsv(rollRequired));

        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
        		+ "<unresolvedPersonnel>");
        for (Integer i : unresolvedPersonnel.keySet()) {
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2)
        			+ "<contract id=\"" + i + "\">"
        			+ createCsv(unresolvedPersonnel.get(i))
        			+ "</contract>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
        		+ "</unresolvedPersonnel>");

        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
        		+ "<payouts>");
        for (UUID pid : payouts.keySet()) {
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2)
        			+ "<payout id=\"" + pid.toString() + "\">");
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"weightClass", payouts.get(pid).getWeightClass());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"dependents", payouts.get(pid).getDependents());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"cbills", payouts.get(pid).getCbills());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"recruit", payouts.get(pid).hasRecruit());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"heir", payouts.get(pid).hasHeir());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"stolenUnit", payouts.get(pid).hasStolenUnit());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3,
        			"stolenUnitId", payouts.get(pid).getStolenUnitId().toString());
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2)
        			+ "</payout>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
        		+ "</payouts>");

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1,
        		"lastRetirementRoll", df.format(lastRetirementRoll.getTime()));
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</retirementDefectionTracker>");		
	}

    public static RetirementDefectionTracker generateInstanceFromXML(Node wn, Campaign c) {
        RetirementDefectionTracker retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new RetirementDefectionTracker();

            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            // Loop through the nodes and load our contract offers
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (wn2.getNodeName().equalsIgnoreCase("rollRequired")) {
                	if (wn2.getTextContent().trim().length() > 0) {
	                	String [] ids = wn2.getTextContent().split(",");
	                	for (String id : ids) {
	                		retVal.rollRequired.add(Integer.parseInt(id));
	                	}
                	}
                } else if (wn2.getNodeName().equalsIgnoreCase("unresolvedPersonnel")) {
                	NodeList nl2 = wn2.getChildNodes();
                	for (int y = 0; y < nl2.getLength(); y++) {
                		Node wn3 = nl2.item(y);
                		if (wn3.getNodeType() != Node.ELEMENT_NODE){
                			continue;
                		}
                		if (wn3.getNodeName().equalsIgnoreCase("contract")) {
                			int id = Integer.parseInt(wn3.getAttributes().getNamedItem("id").getTextContent());
                			HashSet<UUID> pids = new HashSet<UUID>();
                			String [] ids = wn3.getTextContent().split(",");
                			for (String s : ids) {
                				pids.add(UUID.fromString(s));
                			}
                			retVal.unresolvedPersonnel.put(id, pids);
                		}
                	}
                } else if (wn2.getNodeName().equalsIgnoreCase("payouts")) {
                	NodeList nl2 = wn2.getChildNodes();
                	for (int y = 0; y < nl2.getLength(); y++) {
                		Node wn3 = nl2.item(y);
                		if (wn3.getNodeType() != Node.ELEMENT_NODE){
                			continue;
                		}
                		if (wn3.getNodeName().equalsIgnoreCase("payout")) {
                			UUID pid = UUID.fromString(wn3.getAttributes().getNamedItem("id").getTextContent());
                			Payout payout = retVal.new Payout();
                			NodeList nl3 = wn3.getChildNodes();
                			for (int z = 0; z < nl3.getLength(); z++) {
                				Node wn4 = nl3.item(z);
                				if (wn4.getNodeType() != Node.ELEMENT_NODE) {
                					continue;
                				}
                				if (wn4.getNodeName().equalsIgnoreCase("weightClass")) {
                					payout.setWeightClass(Integer.parseInt(wn4.getTextContent()));
                				} else if (wn4.getNodeName().equalsIgnoreCase("dependents")) {
                					payout.setDependents(Integer.parseInt(wn4.getTextContent()));
                				} else if (wn4.getNodeName().equalsIgnoreCase("cbills")) {
                					payout.setCbills(Long.parseLong(wn4.getTextContent()));
                				} else if (wn4.getNodeName().equalsIgnoreCase("recruit")) {
                					payout.setRecruit(Boolean.parseBoolean(wn4.getTextContent()));
                				} else if (wn4.getNodeName().equalsIgnoreCase("heir")) {
                					payout.setHeir(Boolean.parseBoolean(wn4.getTextContent()));
                            	} else if (wn4.getNodeName().equalsIgnoreCase("stolenUnit")) {
                					payout.setStolenUnit(Boolean.parseBoolean(wn4.getTextContent()));
                            	} else if (wn4.getNodeName().equalsIgnoreCase("stolenUnitId")) {
                					payout.setStolenUnitId(UUID.fromString(wn4.getTextContent()));
                				}
                			}
                			retVal.payouts.put(pid, payout);
                		}
                	}
                } else if (wn2.getNodeName().equalsIgnoreCase("lastRetirementRoll")) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    retVal.lastRetirementRoll.setTime(df.parse(wn2.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }

        return retVal;
    }
}
