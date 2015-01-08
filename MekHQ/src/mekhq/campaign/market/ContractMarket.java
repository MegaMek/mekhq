/*
 * ContractMarket.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.market;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import megamek.client.RandomSkillsGenerator;
import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planets;
import mekhq.campaign.universe.RandomFactionGenerator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contract offers that are generated monthly under AtB rules.
 *
 * Based on PersonnelMarket
 *
 * @author Neoancient
 *
 */
public class ContractMarket implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1303462872220110093L;

	public static int TYPE_ATBMONTHLY = 0;
	//TODO: Implement a method that rolls each day to see whether a new contract appears or an offer disappears

	public final static int CLAUSE_COMMAND = 0;
	public final static int CLAUSE_SALVAGE = 1;
	public final static int CLAUSE_SUPPORT = 2;
	public final static int CLAUSE_TRANSPORT = 3;
	public final static int CLAUSE_NUM = 4;

	private int method = TYPE_ATBMONTHLY;

	private ArrayList<Contract> contracts;
	private int lastId = 0;
	private HashMap<Integer, Contract> contractIds;
	private HashMap<Integer, ClauseMods> clauseMods;

	/* It is possible to call addFollowup more than once for the
	 * same contract by canceling the dialog and running it again;
	 * this is the easiest place to track it to prevent
	 * multiple followup contracts.
	 * key: followup id
	 * value: main contract id
	 */
	private HashMap<Integer, Integer> followupContracts;

	public ContractMarket() {
		contracts = new ArrayList<Contract>();
		contractIds = new HashMap<Integer, Contract>();
		clauseMods = new HashMap<Integer, ClauseMods>();
		followupContracts = new HashMap<Integer, Integer>();
	}

	public ArrayList<Contract> getContracts() {
		return contracts;
	}

	public void removeContract(Contract c) {
		contracts.remove(c);
		contractIds.remove(c.getId());
		clauseMods.remove(c.getId());
		followupContracts.remove(c.getId());
	}

	public AtBContract addAtBContract(Campaign campaign) {
		AtBContract c = generateAtBContract(campaign, campaign.getUnitRatingMod());
		if (c != null) {
			contracts.add(c);
		}
		return c;
	}

	public int getRerollsUsed(Contract c, int clause) {
		if (null != clauseMods.get(c.getId())) {
			return clauseMods.get(c.getId()).rerollsUsed[clause];
		}
		return 0;
	}

	public void rerollClause(AtBContract c, int clause, Campaign campaign) {
		if (null != clauseMods.get(c.getId())) {
			switch (clause) {
			case CLAUSE_COMMAND:
				rollCommandClause(c, clauseMods.get(c.getId()).mods[clause]);
				break;
			case CLAUSE_SALVAGE:
				rollSalvageClause(c, clauseMods.get(c.getId()).mods[clause]);
				break;
			case CLAUSE_TRANSPORT:
				rollTransportClause(c, clauseMods.get(c.getId()).mods[clause]);
				break;
			case CLAUSE_SUPPORT:
				rollSupportClause(c, clauseMods.get(c.getId()).mods[clause]);
				break;
			}
			clauseMods.get(c.getId()).rerollsUsed[clause]++;
			c.calculateContract(campaign);
		}
	}

	public void generateContractOffers(Campaign campaign) {
		generateContractOffers(campaign, false);
	}

	public void generateContractOffers(Campaign campaign, boolean newCampaign) {
		if ((method == TYPE_ATBMONTHLY && campaign.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) ||
				newCampaign) {
			Contract[] list = contracts.toArray(new Contract[contracts.size()]);
			for (Contract c : list) {
				removeContract(c);
			}

			int unitRatingMod = campaign.getUnitRatingMod();

			for (Mission m : campaign.getMissions()) {
				if (m instanceof AtBContract && m.isActive()) {
					checkForSubcontracts(campaign, (AtBContract)m,
							unitRatingMod);
				}
			}

			int numContracts = Compute.d6() - 4 + unitRatingMod;

			ArrayList<Faction> currentFactions =
					campaign.getCurrentPlanet().getCurrentFactions(campaign.getDate());
			boolean inMinorFaction = true;
			for (Faction f : currentFactions) {
				if (RandomFactionGenerator.getInstance().isMajorPower(f) ||
						f.isClan()) {
					inMinorFaction = false;
					break;
				}
			}
			if (inMinorFaction) {
				numContracts--;
			}

			boolean inBackwater = true;
			if (!currentFactions.get(0).isPeriphery()) {
				if (currentFactions.size() > 1) {
					inBackwater = false;
				} else {
					for (String key : Planets.getNearbyPlanets(campaign.getCurrentPlanet(), 30)) {
						for (Faction f : Planets.getInstance().getPlanets().get(key).getCurrentFactions(campaign.getDate())) {
							if (!f.getShortName().equals(currentFactions.get(0).getShortName())) {
								inBackwater = false;
								break;
							}
						}
						if (!inBackwater) break;
					}
				}
			}
			if (inBackwater) {
				numContracts--;
			}

			if (campaign.getFactionCode().equals("MERC") || campaign.getFactionCode().equals("PIR")) {
				if (campaign.getAtBConfig().isHiringHall(campaign.getCurrentPlanet().getName(), campaign.getDate())) {
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

			for (Faction f : Faction.factions.values()) {
				try {
					if (f.getStartingPlanet(campaign.getEra()).equals(campaign.getCurrentPlanet().getName())) {
						for (Faction currentFaction : campaign.getCurrentPlanet().getCurrentFactions(campaign.getDate())) {
							if (f.equals(currentFaction) && RandomFactionGenerator.getInstance().getEmployerSet().contains(currentFaction)) {
								AtBContract c = generateAtBContract(campaign, f.getShortName(), unitRatingMod);
								if (c != null) {
									contracts.add(c);
								}
								break;
							}
						}
						break;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					//no starting planet in current era; continue to next planet
				}
			}

			if (newCampaign) {
				numContracts = Math.max(numContracts, 2);
			}

			for (int i = 0; i < numContracts; i++) {
				AtBContract c = generateAtBContract(campaign, unitRatingMod);
				if (c != null) {
					contracts.add(c);
				}
			}
	        if (campaign.getCampaignOptions().getContractMarketReportRefresh()) {
	            campaign.addReport("<a href='CONTRACT_MARKET'>Contract market updated</a>");
	        }
		}
	}

	private void checkForSubcontracts(Campaign campaign,
			AtBContract contract, int unitRatingMod) {
		if (contract.getMissionType() == AtBContract.MT_GARRISONDUTY) {
			int numSubcontracts = 0;
			for (Mission m : campaign.getMissions()) {
				if (m instanceof AtBContract &&
						((AtBContract)m).getParentContract() == contract) {
					numSubcontracts++;
				}
			}
			if (numSubcontracts >= unitRatingMod - 1) {
				return;
			}
			if (Compute.d6(2) >= 10) {
				AtBContract sub = generateAtBSubcontract(campaign, contract, unitRatingMod);
				if (sub.getEndingDate().before(contract.getEndingDate())) {
					contracts.add(sub);
				}
			}
		}
	}

	/* If no suitable planet can be found or no jump path to the planet can be calculated after
	 * the indicated number of retries, this will return null.
	 */
	private AtBContract generateAtBContract(Campaign campaign, int unitRatingMod) {
		if (campaign.getFactionCode().equals("MERC")) {
			if (null == campaign.getRetainerEmployerCode()) {
				int retries = 3;
				AtBContract retVal = null;
				while (retries > 0 && retVal == null) {
					retVal = generateAtBContract(campaign,
							RandomFactionGenerator.getInstance().getEmployer(),
							unitRatingMod, 0);
					retries--;
				}
				return retVal;
			} else {
				return generateAtBContract(campaign,
						campaign.getRetainerEmployerCode(), unitRatingMod, 3);
			}
		} else {
			return generateAtBContract(campaign,
					campaign.getFactionCode(), unitRatingMod, 3);
		}
	}

	private AtBContract generateAtBContract(Campaign campaign,
			String employer, int unitRatingMod) {
		return generateAtBContract(campaign, employer, unitRatingMod, 3);
	}

	private AtBContract generateAtBContract(Campaign campaign,
			String employer, int unitRatingMod, int retries) {
		AtBContract contract = new AtBContract("New Contract");
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);

        if (employer.equals("MERC")) {
        	contract.setMercSubcontract(true);
        	while (employer.equals("MERC")) {
        		employer = RandomFactionGenerator.getInstance().getEmployer();
        	}
        }
		contract.setEmployerCode(employer, campaign.getEra());
		contract.setMissionType(findAtBMissionType(unitRatingMod,
				RandomFactionGenerator.getInstance().isISMajorPower(contract.getEmployerCode())));

		if (contract.getMissionType() == AtBContract.MT_PIRATEHUNTING)
			contract.setEnemyCode("PIR");
		else if (contract.getMissionType() == AtBContract.MT_RIOTDUTY)
			contract.setEnemyCode("REB");
		else {
			boolean rebsAllowed = contract.getMissionType() <= AtBContract.MT_RIOTDUTY;
			contract.setEnemyCode(RandomFactionGenerator.getInstance().getEnemy(contract.getEmployerCode(), rebsAllowed));
		}
		if (contract.getMissionType() == AtBContract.MT_GARRISONDUTY && contract.getEnemyCode().equals("REB")) {
			contract.setMissionType(AtBContract.MT_RIOTDUTY);
		}

		/* Addition to AtB rules: factions which are generally neutral
		 * (ComStar, Mercs not under contract) are more likely to have garrison-type
		 * contracts and less likely to have battle-type contracts unless at war.
		 */
		if (RandomFactionGenerator.getInstance().isNeutral(employer) &&
				!RandomFactionGenerator.getInstance().isAtWarWith(employer,
						contract.getEnemyCode(), campaign.getDate())) {
			if (contract.getMissionType() == AtBContract.MT_PLANETARYASSAULT) {
				contract.setMissionType(AtBContract.MT_GARRISONDUTY);
			} else if (contract.getMissionType() == AtBContract.MT_RELIEFDUTY) {
				contract.setMissionType(AtBContract.MT_SECURITYDUTY);
			}
		}

		boolean isAttacker = (contract.getMissionType() == AtBContract.MT_PLANETARYASSAULT ||
				contract.getMissionType() >= AtBContract.MT_PLANETARYASSAULT ||
				(contract.getMissionType() == AtBContract.MT_RELIEFDUTY && Compute.d6() < 4) ||
				contract.getEnemyCode().equals("REB"));
		if (isAttacker) {
			contract.setPlanetName(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEmployerCode(), contract.getEnemyCode(), campaign.getDate()));
		} else {
			contract.setPlanetName(RandomFactionGenerator.getInstance().getMissionTarget(contract.getEnemyCode(), contract.getEmployerCode(), campaign.getDate()));
		}
		if (contract.getPlanetName() == null) {
			MekHQ.logError("Could not find contract location for " +
					contract.getEmployerCode() + " vs. " + contract.getEnemyCode());
			if (retries > 0) {
				return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
			} else {
				return null;
			}
		}
		JumpPath jp = null;
		try {
			jp = campaign.calculateJumpPath(campaign.getCurrentPlanetName(), contract.getPlanetName());
		} catch (NullPointerException ex) {
			// could not calculate jump path; leave jp null
		}
		if (jp == null) {
			if (retries > 0) {
				return generateAtBContract(campaign, employer, unitRatingMod, retries - 1);
			} else {
				return null;
			}			
		}

		setAllyRating(contract, isAttacker, campaign.getCalendar().get(Calendar.YEAR));
		setEnemyRating(contract, isAttacker, campaign.getCalendar().get(Calendar.YEAR));

		if (contract.getMissionType() == AtBContract.MT_CADREDUTY) {
			contract.setAllySkill(RandomSkillsGenerator.L_GREEN);
			contract.setAllyQuality(IUnitRating.DRAGOON_F);
		}

		contract.calculateLength(campaign.getCampaignOptions().getVariableContractLength());
		setAtBContractClauses(contract, unitRatingMod, campaign);

		contract.calculatePaymentMultiplier(campaign);

		contract.calculatePartsAvailabilityLevel(campaign);

        contract.initContractDetails(campaign);
        contract.calculateContract(campaign);
		return contract;
	}

	protected AtBContract generateAtBSubcontract(Campaign campaign,
			AtBContract parent, int unitRatingMod) {
		AtBContract contract = new AtBContract("New Subcontract");
		contract.setEmployerCode(parent.getEmployerCode(), campaign.getEra());
		contract.setMissionType(findAtBMissionType(unitRatingMod,
				RandomFactionGenerator.getInstance().isISMajorPower(contract.getEmployerCode())));

		if (contract.getMissionType() == AtBContract.MT_PIRATEHUNTING)
			contract.setEnemyCode("PIR");
		else if (contract.getMissionType() == AtBContract.MT_RIOTDUTY)
			contract.setEnemyCode("REB");
		else {
			boolean rebsAllowed = contract.getMissionType() <= AtBContract.MT_RIOTDUTY;
			contract.setEnemyCode(RandomFactionGenerator.getInstance().getEnemy(contract.getEmployerCode(), rebsAllowed));
		}
		if (contract.getMissionType() == AtBContract.MT_GARRISONDUTY && contract.getEnemyCode().equals("REB")) {
			contract.setMissionType(AtBContract.MT_RIOTDUTY);
		}

		contract.setParentContract(parent);
        contract.initContractDetails(campaign);
		lastId++;
		contract.setId(lastId);
		contractIds.put(lastId, contract);

        /* The AtB rules say to roll the enemy, but also that the subcontract
         * takes place in the same planet/sector. Rebels and pirates can
         * appear anywhere, but others should be limited to what's within a
         * jump. */

        /*TODO: When MekHQ gets the capability of splitting the unit to
         * different locations, this restriction can be lessened or lifted.
         */
        if (!contract.getEnemyCode().equals("REB") &&
        		!contract.getEnemyCode().equals("PIR")) {
        	boolean factionValid = false;
        	for (String p : Planets.getNearbyPlanets(campaign.getCurrentPlanet(), 30)) {
        		if (factionValid) break;
        		for (Faction f : Planets.getInstance().getPlanets().get(p).getCurrentFactions(campaign.getDate())) {
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
		boolean isAttacker = (contract.getMissionType() == AtBContract.MT_PLANETARYASSAULT ||
				contract.getMissionType() >= AtBContract.MT_PLANETARYASSAULT ||
				(contract.getMissionType() == AtBContract.MT_RELIEFDUTY && Compute.d6() < 4) ||
				contract.getEnemyCode().equals("REB"));
        contract.setPlanetName(parent.getPlanetName());
		setAllyRating(contract, isAttacker, campaign.getCalendar().get(Calendar.YEAR));
		setEnemyRating(contract, isAttacker, campaign.getCalendar().get(Calendar.YEAR));

		if (contract.getMissionType() == AtBContract.MT_CADREDUTY) {
			contract.setAllySkill(RandomSkillsGenerator.L_GREEN);
			contract.setAllyQuality(IUnitRating.DRAGOON_F);
		}
		contract.calculateLength(campaign.getCampaignOptions().getVariableContractLength());

        contract.setCommandRights(Math.max(parent.getCommandRights() - 1,
        		Contract.COM_INTEGRATED));
        contract.setSalvageExchange(parent.isSalvageExchange());
        contract.setSalvagePct(Math.max(parent.getSalvagePct() - 10, 0));
        contract.setStraightSupport(Math.max(parent.getStraightSupport() - 20,
        		0));
        if (parent.getBattleLossComp() <= 10) {
        	contract.setBattleLossComp(0);
        } else if (parent.getBattleLossComp() <= 20) {
        	contract.setBattleLossComp(10);
        } else {
        	contract.setBattleLossComp(parent.getBattleLossComp() - 20);
		}
        contract.setTransportComp(100);

		contract.calculatePaymentMultiplier(campaign);
		contract.calculatePartsAvailabilityLevel(campaign);
        contract.calculateContract(campaign);

		return contract;
	}

	public void addFollowup(Campaign campaign,
			AtBContract contract) {
		if (followupContracts.values().contains(contract.getId())) {
			return;
		}
		AtBContract followup = new AtBContract("Followup Contract");
		contract.setEmployerCode(contract.getEmployerCode(), campaign.getEra());
		followup.setEnemyCode(contract.getEnemyCode());
		followup.setPlanetName(contract.getPlanetName());
		switch (contract.getMissionType()) {
		case AtBContract.MT_DIVERSIONARYRAID:
			followup.setMissionType(AtBContract.MT_OBJECTIVERAID);
			break;
		case AtBContract.MT_RECONRAID:
			followup.setMissionType(AtBContract.MT_PLANETARYASSAULT);
			break;
		case AtBContract.MT_RIOTDUTY:
			followup.setMissionType(AtBContract.MT_GARRISONDUTY);
			break;
		}
		followup.setAllySkill(contract.getAllySkill());
		followup.setAllyQuality(contract.getAllyQuality());
		followup.setEnemySkill(contract.getEnemySkill());
		followup.setEnemyQuality(contract.getEnemyQuality());
		followup.calculateLength(campaign.getCampaignOptions().getVariableContractLength());
		setAtBContractClauses(followup, campaign.getUnitRatingMod(), campaign);

		followup.calculatePaymentMultiplier(campaign);

		followup.calculatePartsAvailabilityLevel(campaign);

        followup.initContractDetails(campaign);
        followup.calculateContract(campaign);
		lastId++;
		followup.setId(lastId);
		contractIds.put(lastId, followup);

		contracts.add(followup);
		followupContracts.put(followup.getId(), contract.getId());
	}

	protected int findAtBMissionType(int unitRatingMod, boolean majorPower) {
		final int[][] table = {
			//col 0: IS Houses
			{AtBContract.MT_GUERRILLAWARFARE, AtBContract.MT_RECONRAID, AtBContract.MT_PIRATEHUNTING,
				AtBContract.MT_PLANETARYASSAULT, AtBContract.MT_OBJECTIVERAID, AtBContract.MT_OBJECTIVERAID,
				AtBContract.MT_EXTRACTIONRAID, AtBContract.MT_RECONRAID, AtBContract.MT_GARRISONDUTY,
				AtBContract.MT_CADREDUTY, AtBContract.MT_RELIEFDUTY},
			//col 1: Others
				{AtBContract.MT_GUERRILLAWARFARE, AtBContract.MT_RECONRAID, AtBContract.MT_PLANETARYASSAULT,
					AtBContract.MT_OBJECTIVERAID, AtBContract.MT_EXTRACTIONRAID, AtBContract.MT_PIRATEHUNTING,
					AtBContract.MT_SECURITYDUTY, AtBContract.MT_OBJECTIVERAID, AtBContract.MT_GARRISONDUTY,
					AtBContract.MT_CADREDUTY, AtBContract.MT_DIVERSIONARYRAID}
		};
		int roll = Compute.d6(2) + unitRatingMod - IUnitRating.DRAGOON_C;
		if (roll > 12) {
			roll = 12;
		}
		if (roll < 2) {
			roll = 2;
		}
		return table[majorPower?0:1][roll - 2];
	}

	public void setAllyRating(AtBContract contract, boolean isAttacker, int year) {
		int mod = 0;
		if (contract.getEnemyCode().equals("REB") ||
				contract.getEnemyCode().equals("PIR")) {
			mod -= 1;
		}
		if (contract.getMissionType() == AtBContract.MT_GUERRILLAWARFARE ||
				contract.getMissionType() == AtBContract.MT_CADREDUTY) {
			mod -= 3;
		}
		if (contract.getMissionType() == AtBContract.MT_GARRISONDUTY ||
				contract.getMissionType() == AtBContract.MT_SECURITYDUTY) {
			mod -= 2;
		}
		if (AtBContract.isMinorPower(contract.getEmployerCode())) {
			mod -= 1;
		}
		if (contract.getEnemyCode().equals("IND") ||
				contract.getEnemyCode().equals("PIND")) {
			mod -= 2;
		}
		if (contract.getMissionType() == AtBContract.MT_PLANETARYASSAULT) {
			mod += 1;
		}
		if (Faction.getFaction(contract.getEmployerCode()).isClan() && !isAttacker) {
			//facing front-line units
			mod += 1;
		}
		contract.setAllySkill(getSkillRating(Compute.d6(2) + mod));
		if (year > 2950 && year < 3039 &&
				!Faction.getFaction(contract.getEmployerCode()).isClan()) {
			mod -= 1;
		}
		contract.setAllyQuality(getQualityRating(Compute.d6(2) + mod));
	}

	public void setEnemyRating(AtBContract contract, boolean isAttacker, int year) {
		int mod = 0;
		if (contract.getEnemyCode().equals("REB") ||
				contract.getEnemyCode().equals("PIR")) {
			mod -= 2;
		}
		if (contract.getMissionType() == AtBContract.MT_GUERRILLAWARFARE) {
			mod += 2;
		}
		if (contract.getMissionType() == AtBContract.MT_PLANETARYASSAULT) {
			mod += 1;
		}
		if (AtBContract.isMinorPower(contract.getEmployerCode())) {
			mod -= 1;
		}
		if (Faction.getFaction(contract.getEmployerCode()).isClan()) {
			mod += isAttacker?2:4;
		}
		contract.setEnemySkill(getSkillRating(Compute.d6(2) + mod));
		if (year > 2950 && year < 3039 &&
				!Faction.getFaction(contract.getEnemyCode()).isClan()) {
			mod -= 1;
		}
		contract.setEnemyQuality(getQualityRating(Compute.d6(2) + mod));
	}

	protected int getSkillRating(int roll) {
		if (roll <= 5) return RandomSkillsGenerator.L_GREEN;
		if (roll <= 9) return RandomSkillsGenerator.L_REG;
		if (roll <= 11) return RandomSkillsGenerator.L_VET;
		return RandomSkillsGenerator.L_ELITE;
	}

	protected int getQualityRating(int roll) {
		if (roll <= 5) return IUnitRating.DRAGOON_F;
		if (roll <= 8) return IUnitRating.DRAGOON_D;
		if (roll <= 10) return IUnitRating.DRAGOON_C;
		if (roll == 11) return IUnitRating.DRAGOON_B;
		return IUnitRating.DRAGOON_A;
	}
	
	protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {
		ClauseMods mods = new ClauseMods();
		clauseMods.put(contract.getId(), mods);

		/* AtB rules seem to indicate one admin in each role (though this
		 * is not explicitly stated that I have seen) but MekHQ allows
		 * assignment of multiple admins to each role. Therefore we go
		 * through all the admins and for each role select the one with
		 * the highest admin skill, or higher negotiation if the admin
		 * skills are equal.
		 */
		Person adminCommand = campaign.findBestInRole(Person.T_ADMIN_COM, SkillType.S_ADMIN, SkillType.S_NEG);
		Person adminTransport = campaign.findBestInRole(Person.T_ADMIN_TRA, SkillType.S_ADMIN, SkillType.S_NEG);
		Person adminLogistics = campaign.findBestInRole(Person.T_ADMIN_LOG, SkillType.S_ADMIN, SkillType.S_NEG);
		int adminCommandExp = (adminCommand == null)?SkillType.EXP_ULTRA_GREEN:adminCommand.getSkill(SkillType.S_ADMIN).getExperienceLevel();
		int adminTransportExp = (adminTransport == null)?SkillType.EXP_ULTRA_GREEN:adminTransport.getSkill(SkillType.S_ADMIN).getExperienceLevel();
		int adminLogisticsExp = (adminLogistics == null)?SkillType.EXP_ULTRA_GREEN:adminLogistics.getSkill(SkillType.S_ADMIN).getExperienceLevel();

		/* Treat government units like merc units that have a retainer contract */
		if ((!campaign.getFactionCode().equals("MERC") &&
				!campaign.getFactionCode().equals("PIR")) ||
				null != campaign.getRetainerEmployerCode()) {
			for (int i = 0; i < CLAUSE_NUM; i++) {
				mods.mods[i]++;
			}
		}

		if (campaign.getCampaignOptions().isMercSizeLimited() &&
				campaign.getFactionCode().equals("MERC")) {
			int max = (unitRatingMod + 1) * 12;
			int numMods = (AtBContract.getEffectiveNumUnits(campaign) - max) / 2;
			while (numMods > 0) {
				mods.mods[Compute.randomInt(4)]--;
				numMods--;
			}
		}

		mods.mods[CLAUSE_COMMAND] = adminCommandExp - SkillType.EXP_REGULAR;
		mods.mods[CLAUSE_SALVAGE] = 0;
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

		if (Faction.getFaction(contract.getEnemyCode()).isClan() &&
				!Faction.getFaction(contract.getEmployerCode()).isClan()) {
			for (int i = 0; i < 4; i++)
				if (i == CLAUSE_SALVAGE) mods.mods[i] -= 2;
				else mods.mods[i] += 1;
		} else {
			if (contract.getEnemySkill() >= SkillType.EXP_VETERAN)
				mods.mods[Compute.randomInt(4)] += 1;
			if (contract.getEnemySkill() == SkillType.EXP_ELITE)
				mods.mods[Compute.randomInt(4)] += 1;
		}

		int[][] missionMods = {
			{1, 0, 1, 0}, {0, 1, -1, -3}, {-3, 0, 2, 1}, {-2, 1, -1, -1},
			{-2, 0, 2, 3}, {-1, 1, 1, 1}, {-2, 3, -2, -1}, {2, 2, -1, -1},
			{0, 2, 2, 1}, {-1, 0, 1, 2}, {-1, -2, 1, -1}, {-1, -1, 2, 1}
		};
		for (int i = 0; i < 4; i++) {
			mods.mods[i] += missionMods[contract.getMissionType()][i];
		}

		if (RandomFactionGenerator.getInstance().isISMajorPower(contract.getEmployerCode())) {
			mods.mods[CLAUSE_SALVAGE] += -1;
			mods.mods[CLAUSE_TRANSPORT] += 1;
		}
		if (AtBContract.isMinorPower(contract.getEmployerCode())) {
			mods.mods[CLAUSE_SALVAGE] += -2;
		}
		if (contract.getEmployerCode().equals("MERC")) {
			mods.mods[CLAUSE_COMMAND] += -1;
			mods.mods[CLAUSE_SALVAGE] += 2;
			mods.mods[CLAUSE_SUPPORT] += 1;
			mods.mods[CLAUSE_TRANSPORT] += 1;
		}
		if (contract.getEmployerCode().equals("IND")) {
			mods.mods[CLAUSE_COMMAND] += 0;
			mods.mods[CLAUSE_SALVAGE] += -1;
			mods.mods[CLAUSE_SUPPORT] += -1;
			mods.mods[CLAUSE_TRANSPORT] += 0;
		}

		if (campaign.getFactionCode().equals("MERC")) {
			rollCommandClause(contract, mods.mods[CLAUSE_COMMAND]);
		} else {
			contract.setCommandRights(Contract.COM_INTEGRATED);
		}
		rollSalvageClause(contract, mods.mods[CLAUSE_SALVAGE]);
		rollSupportClause(contract, mods.mods[CLAUSE_SUPPORT]);
		rollTransportClause(contract, mods.mods[CLAUSE_TRANSPORT]);
	}

	private void rollCommandClause(AtBContract contract, int mod) {
		int roll = Compute.d6(2) + mod;
		if (roll < 3) contract.setCommandRights(Contract.COM_INTEGRATED);
		else if (roll < 8) contract.setCommandRights(Contract.COM_HOUSE);
		else if (roll < 12) contract.setCommandRights(Contract.COM_LIAISON);
		else contract.setCommandRights(Contract.COM_INDEP);
	}

	private void rollSalvageClause(AtBContract contract, int mod) {
		contract.setSalvageExchange(false);
		int roll = Compute.d6(2) + mod;
		if (roll < 4) contract.setSalvagePct(0);
		else if (roll < 4) {
			contract.setSalvageExchange(true);
			int r;
			do {
				r = Compute.d6(2);
			} while (r < 4);
			contract.setSalvagePct((roll - 3) * 10);
		} else {
			contract.setSalvagePct((roll - 3) * 10);
		}
	}

	private void rollSupportClause(AtBContract contract, int mod) {
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
		    contract.setBattleLossComp((roll - 8) * 20);
		}
	}

	private void rollTransportClause(AtBContract contract, int mod) {
		int roll = Compute.d6(2) + mod;
		if (roll < 2) contract.setTransportComp(0);
		else if (roll < 6) contract.setTransportComp((20 + (roll - 2) * 5));
		else if (roll < 10) contract.setTransportComp((45 + (roll - 6) * 5));
		else contract.setTransportComp(100);
	}

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<contractMarket>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "lastId", lastId);
        for (Contract c : contracts) {
            c.writeToXml(pw1, indent + 1);
        }
        for (Integer key : clauseMods.keySet()) {
        	pw1.println(MekHqXmlUtil.indentStr(indent+1)
        			+ "<clauseMods id=\"" + key + "\">");
        	String rerolls = "";
        	String mods = "";
        	for (int i = 0; i < CLAUSE_NUM; i++) {
        		rerolls += clauseMods.get(key).rerollsUsed[i] + ((i < CLAUSE_NUM - 1)?",":"");
        		mods += clauseMods.get(key).mods[i] + ((i < CLAUSE_NUM - 1)?",":"");
        	}
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "mods", mods);
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "rerollsUsed", rerolls);
        	pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</clauseMods>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</contractMarket>");
    }

    public static ContractMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
        ContractMarket retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new ContractMarket();

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

                	if (m != null && m instanceof Contract) {
                		retVal.contracts.add((Contract)m);
                		retVal.contractIds.put(m.getId(), (Contract)m);
                	}
                } else if (wn2.getNodeName().equalsIgnoreCase("clauseMods")) {
                	int key = Integer.parseInt(wn2.getAttributes().getNamedItem("id").getTextContent());
                	ClauseMods cm = retVal.new ClauseMods();
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
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }

        return retVal;
    }

    /* Keep track of how many rerolls remain for each contract clause
     * based on the admin's negotiation skill. Also track bonuses, as
     * the random clause bonuses should be persistent.
     */
    public class ClauseMods {
    	public int[] rerollsUsed = {0, 0, 0, 0};
    	public int[] mods = {0, 0, 0, 0};
    }
}