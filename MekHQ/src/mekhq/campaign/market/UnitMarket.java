/*
 * UniMarket.java
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
import java.util.Set;

import megamek.client.RandomUnitGenerator;
import megamek.common.Compute;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.UnitTableData;
import mekhq.campaign.universe.UnitTableData.FactionTables;

import org.joda.time.DateTime;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Generates units available for sale.
 * 
 * @author Neoancient
 *
 */
public class UnitMarket implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2085002038852079114L;

	public class MarketOffer {
		public int market;
		public int unitType;
		public int unitWeight;
		public MechSummary unit;
		public int pct;
		
		public MarketOffer(int m, int t, int w, MechSummary u, int p) {
			market = m;
			unitType = t;
			unitWeight = w;
			unit = u;
			pct = p;
		}

		public MarketOffer() {
		}
	}
	
	public static int TYPE_ATBMONTHLY = 0;
	//TODO: Implement a method that rolls each day and adds or removes units
	
	public static int MARKET_OPEN = 0;
	public static int MARKET_EMPLOYER = 1;
	public static int MARKET_MERCENARY = 2;
	public static int MARKET_FACTORY = 3;
	public static int MARKET_BLACK = 4;
	public static int MARKET_NUM = 5;
	public static String [] marketNames = {
		"Open Market", "Employer Market", "Mercenary Auction",
		"Factory Line", "Black Market"
	};
	
	private int method = TYPE_ATBMONTHLY;
	
	//master list
	private ArrayList<MarketOffer> offers;

	public UnitMarket() {
		offers = new ArrayList<MarketOffer>();
	}
	
	public ArrayList<MarketOffer> getOffers() {
		return offers;
	}
	
	public void removeOffer(MarketOffer o) {
		offers.remove(o);
	}
	
	public void generateUnitOffers(Campaign campaign) {
		if (method == TYPE_ATBMONTHLY && campaign.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) {
			offers.clear();
			
			AtBContract contract = null;
			for (Mission m : campaign.getMissions()) {
				if (m.isActive() && m instanceof AtBContract) {
					contract = (AtBContract)m;
					break;
				}
			}

			addOffers(campaign, Compute.d6() - 2, MARKET_OPEN,
					UnitTableData.UNIT_MECH, null,
					UnitTableData.QUALITY_F, 7);
			addOffers(campaign, Compute.d6() - 1, MARKET_OPEN,
					UnitTableData.UNIT_VEHICLE, null,
					UnitTableData.QUALITY_F, 7);
			addOffers(campaign, Compute.d6() - 2, MARKET_OPEN,
					UnitTableData.UNIT_AERO, null,
					UnitTableData.QUALITY_F, 7);

			if (contract != null) {
				addOffers(campaign, Compute.d6() - 3,
						MARKET_EMPLOYER,
						UnitTableData.UNIT_MECH,
						contract.getEmployerCode(),
						UnitTableData.QUALITY_D, 7);
				addOffers(campaign, Compute.d6() - 2,
						MARKET_EMPLOYER,
						UnitTableData.UNIT_VEHICLE,
						contract.getEmployerCode(),
						UnitTableData.QUALITY_D, 7);
				addOffers(campaign, Compute.d6() - 3,
						MARKET_EMPLOYER,
						UnitTableData.UNIT_AERO,
						contract.getEmployerCode(),
						UnitTableData.QUALITY_D, 7);
			}

			if (!campaign.getFaction().isClan()) {
				addOffers(campaign, Compute.d6(3) - 9,
						MARKET_MERCENARY,
						UnitTableData.UNIT_MECH, "MERC",
						UnitTableData.QUALITY_C, 5);
				addOffers(campaign, Compute.d6(3) - 6,
						MARKET_MERCENARY,
						UnitTableData.UNIT_VEHICLE, "MERC",
						UnitTableData.QUALITY_C, 5);
				addOffers(campaign, Compute.d6(3) - 9,
						MARKET_MERCENARY,
						UnitTableData.UNIT_AERO, "MERC",
						UnitTableData.QUALITY_C, 5);
			}

			if (campaign.getUnitRatingMod() >= IUnitRating.DRAGOON_B) {
				Set<Faction> factions = campaign.getCurrentPlanet().getFactionSet(new DateTime(campaign.getCalendar()));
				String faction = Utilities.getRandomItem(factions).getShortName();
				if (campaign.getFaction().isClan() ||
						!Faction.getFaction(faction).isClan()) {
					addOffers(campaign, Compute.d6() - 3,
							MARKET_FACTORY,
							UnitTableData.UNIT_MECH, faction,
							UnitTableData.QUALITY_A, 6);
					addOffers(campaign, Compute.d6() - 2,
							MARKET_FACTORY,
							UnitTableData.UNIT_VEHICLE, faction,
							UnitTableData.QUALITY_A, 6);
					addOffers(campaign, Compute.d6() - 3,
							MARKET_FACTORY,
							UnitTableData.UNIT_AERO, faction,
							UnitTableData.QUALITY_A, 6);
				}
			}

			if (!campaign.getFaction().isClan()) {
				addOffers(campaign, Compute.d6(2) - 6,
						MARKET_BLACK,
						UnitTableData.UNIT_MECH, null,
						UnitTableData.QUALITY_C, 6);
				addOffers(campaign, Compute.d6(2) - 4,
						MARKET_BLACK,
						UnitTableData.UNIT_VEHICLE, null,
						UnitTableData.QUALITY_C, 6);
				addOffers(campaign, Compute.d6(2) - 6,
						MARKET_BLACK,
						UnitTableData.UNIT_AERO, null,
						UnitTableData.QUALITY_C, 6);
			}

			if (campaign.getCampaignOptions().getUnitMarketReportRefresh()) {
				campaign.addReport("<a href='UNIT_MARKET'>Unit market updated</a>");
			}
		}
	}	

	private void addOffers(Campaign campaign, int num, int market,
			int unitType, String faction, int quality, int priceTarget) {
		if (faction == null) {
			faction = RandomFactionGenerator.getInstance().getEmployer();
		}
		if (faction == null) {
			faction = campaign.getFactionCode();
			market = MARKET_EMPLOYER;
		}
		FactionTables ft = UnitTableData.getInstance().getBestRAT(campaign.getCampaignOptions().getRATs(),
				campaign.getCalendar().get(Calendar.YEAR),
				faction, unitType);
		if (ft == null) {
			MekHQ.logMessage("Unit market could not locate appropriate RAT");
			return;
		}
		for (int i = 0; i < num; i++) {
			int weight = getRandomWeight(unitType, faction,
					campaign.getCampaignOptions().getRegionalMechVariations());
			String rat = ft.getTable(unitType, weight, quality);
			if (rat == null) {
				continue;
			}
			MechSummary ms = null;
			RandomUnitGenerator.getInstance().setChosenRAT(rat);
			ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
			if (msl.size() > 0) {
				ms = msl.get(0);
				if (campaign.getCampaignOptions().limitByYear() &&
						campaign.getCalendar().get(Calendar.YEAR) < ms.getYear()) {
					continue;
				}
				if ((campaign.getCampaignOptions().allowClanPurchases() && ms.isClan())
						|| (campaign.getCampaignOptions().allowISPurchases() && !ms.isClan())) {
					int pct = 100 - (Compute.d6(2) - priceTarget) * 5;
					/*Some RATs, particularly ASF, group multiple weight classes together
					 * so we need to get the actual weight class from the generated unit
					 * (-1 because EntityWeightClass starts with ultra-light).*/
					offers.add(new MarketOffer(market, unitType, ms.getWeightClass(),
							ms, pct));
				}
			}
		}
	}

	/* Used by special event */
	public String addSingleUnit(Campaign campaign, int market, int unitType,
			String faction, int quality, int pricePct) {
		FactionTables ft = UnitTableData.getInstance().getBestRAT(campaign.getCampaignOptions().getRATs(),
				campaign.getCalendar().get(Calendar.YEAR),
				faction, unitType);
		int weight = getRandomWeight(unitType, faction,
				campaign.getCampaignOptions().getRegionalMechVariations());
		String rat = ft.getTable(unitType, weight, quality);
		MechSummary ms = null;
		if (null != rat) {
			RandomUnitGenerator.getInstance().setChosenRAT(rat);
			ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
			if (msl.size() > 0) {
				ms = msl.get(0);
			}
		}
		offers.add(new MarketOffer(market, unitType, weight,
				ms, pricePct));
		return ms.getName();
	}
	
	public static int getRandomWeight(int unitType, String faction,
			boolean regionalVariations) {
		if (unitType == UnitTableData.UNIT_AERO) {
			return getRandomAeroWeight();
		}
		if (unitType == UnitTableData.UNIT_MECH && regionalVariations) {
			return getRegionalMechWeight(faction);
		}
		return getRandomMechWeight();
	}
	
	public static int getRandomMechWeight() {
		int roll = Compute.randomInt(10);
		if (roll <= 2) return UnitTableData.WT_LIGHT;
		if (roll <= 6) return UnitTableData.WT_MEDIUM;
		if (roll <= 8) return UnitTableData.WT_HEAVY;
		return UnitTableData.WT_ASSAULT;
	}
	
	public static int getRegionalMechWeight(String faction) {
		int roll = Compute.randomInt(100);
		if (faction.equals("DC")) {
			if (roll < 40) return UnitTableData.WT_LIGHT;
			if (roll < 60) return UnitTableData.WT_MEDIUM;
			if (roll < 90) return UnitTableData.WT_HEAVY;
			return UnitTableData.WT_ASSAULT;
		} else if (faction.equals("LA")) {
			if (roll < 20) return UnitTableData.WT_LIGHT;
			if (roll < 50) return UnitTableData.WT_MEDIUM;
			if (roll < 85) return UnitTableData.WT_HEAVY;
			return UnitTableData.WT_ASSAULT;
		} else if (faction.equals("FWL")) {
			if (roll < 30) return UnitTableData.WT_LIGHT;
			if (roll < 70) return UnitTableData.WT_MEDIUM;
			if (roll < 92) return UnitTableData.WT_HEAVY;
			return UnitTableData.WT_ASSAULT;
		}
		if (roll < 30) return UnitTableData.WT_LIGHT;
		if (roll < 70) return UnitTableData.WT_MEDIUM;
		if (roll < 90) return UnitTableData.WT_HEAVY;
		return UnitTableData.WT_ASSAULT;
	}
	
	public static int getRandomAeroWeight() {
		int roll = Compute.randomInt(8);
		if (roll <= 2) return UnitTableData.WT_LIGHT;
		if (roll <= 6) return UnitTableData.WT_MEDIUM;
		return UnitTableData.WT_HEAVY;
	}

   public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<unitMarket>");
        for (MarketOffer o : offers) {
        	pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<offer>");
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) +
        			"<market>" + o.market + "</market>");
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) +
        			"<unitType>" + o.unitType + "</unitType>");
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) +
        			"<unitWeight>" + o.unitWeight + "</unitWeight>");
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) +
        			"<unit>" + o.unit.getName() + "</unit>");
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) +
        			"<pct>" + o.pct + "</pct>");
        	pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</offer>");
        }
    	
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</unitMarket>");
    }

    public static UnitMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
        UnitMarket retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new UnitMarket();

            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            // Loop through the nodes and load our contract offers
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("offer")) {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    MekHQ.logMessage("Unknown node type not loaded in Mission nodes: "
                                     + wn2.getNodeName());

                    continue;
                }

                MarketOffer o = retVal.new MarketOffer();
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                	Node wn3 = nl2.item(i);
                    if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (wn3.getNodeName().equalsIgnoreCase("market")) {
                    	o.market = Integer.parseInt(wn3.getTextContent().trim());
                    } else if (wn3.getNodeName().equalsIgnoreCase("unitType")) {
                    	o.unitType = Integer.parseInt(wn3.getTextContent().trim());
                    } else if (wn3.getNodeName().equalsIgnoreCase("unitWeight")) {
                    	o.unitWeight = Integer.parseInt(wn3.getTextContent().trim());
                    } else if (wn3.getNodeName().equalsIgnoreCase("unit")) {
                    	o.unit = MechSummaryCache.getInstance().getMech(wn3.getTextContent().trim());
                    } else if (wn3.getNodeName().equalsIgnoreCase("pct")) {
                    	o.pct = Integer.parseInt(wn3.getTextContent().trim());
                    }
              }

                if (o != null) {
                    retVal.offers.add(o);
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
