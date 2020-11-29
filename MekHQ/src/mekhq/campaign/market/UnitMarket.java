/*
 * UnitMarket.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.enums.UnitMarketType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.Compute;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.UnitGeneratorParameters;

/**
 * Generates units available for sale.
 *
 * @author Neoancient
 */
public class UnitMarket implements Serializable {
    private static final long serialVersionUID = -2085002038852079114L;

    public static class MarketOffer {
        public UnitMarketType market;
        public int unitType;
        public int unitWeight;
        public MechSummary unit;
        public int pct;

        public MarketOffer(UnitMarketType market, int t, int w, MechSummary u, int p) {
            this.market = market;
            unitType = t;
            unitWeight = w;
            unit = u;
            pct = p;
        }

        public MarketOffer() {

        }
    }

    private UnitMarketMethod method = UnitMarketMethod.ATB_MONTHLY;

    //master list
    private List<MarketOffer> offers;

    public UnitMarket() {
        offers = new ArrayList<>();
    }

    public List<MarketOffer> getOffers() {
        return offers;
    }

    public void removeOffer(MarketOffer o) {
        offers.remove(o);
    }

    public void generateUnitOffers(Campaign campaign) {
        if ((method == UnitMarketMethod.ATB_MONTHLY) && (campaign.getLocalDate().getDayOfMonth() == 1)) {
            offers.clear();

            AtBContract contract = null;
            for (Mission m : campaign.getMissions()) {
                if (m.isActive() && m instanceof AtBContract) {
                    contract = (AtBContract)m;
                    break;
                }
            }

            addOffers(campaign, Compute.d6() - 2, UnitMarketType.OPEN, UnitType.MEK,
                    null, IUnitRating.DRAGOON_F, 7);
            addOffers(campaign, Compute.d6() - 1, UnitMarketType.OPEN, UnitType.TANK,
                    null, IUnitRating.DRAGOON_F, 7);
            addOffers(campaign, Compute.d6() - 2, UnitMarketType.OPEN, UnitType.AERO,
                    null, IUnitRating.DRAGOON_F, 7);

            if (contract != null) {
                addOffers(campaign, Compute.d6() - 3, UnitMarketType.EMPLOYER, UnitType.MEK,
                        contract.getEmployerCode(), IUnitRating.DRAGOON_D, 7);
                addOffers(campaign, Compute.d6() - 2, UnitMarketType.EMPLOYER,
                        UnitType.TANK, contract.getEmployerCode(),
                        IUnitRating.DRAGOON_D, 7);
                addOffers(campaign, Compute.d6() - 3, UnitMarketType.EMPLOYER, UnitType.AERO,
                        contract.getEmployerCode(), IUnitRating.DRAGOON_D, 7);
            }

            if (!campaign.getFaction().isClan()) {
                addOffers(campaign, Compute.d6(3) - 9, UnitMarketType.MERCENARY, UnitType.MEK,
                        "MERC", IUnitRating.DRAGOON_C, 5);
                addOffers(campaign, Compute.d6(3) - 6, UnitMarketType.MERCENARY, UnitType.TANK,
                        "MERC", IUnitRating.DRAGOON_C, 5);
                addOffers(campaign, Compute.d6(3) - 9, UnitMarketType.MERCENARY, UnitType.AERO,
                        "MERC", IUnitRating.DRAGOON_C, 5);
            }

            if (campaign.getUnitRatingMod() >= IUnitRating.DRAGOON_B) {
                Set<Faction> factions = campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate());
                String faction = Utilities.getRandomItem(factions).getShortName();
                if (campaign.getFaction().isClan() || !Faction.getFaction(faction).isClan()) {
                    addOffers(campaign, Compute.d6() - 3, UnitMarketType.FACTORY, UnitType.MEK,
                            faction, IUnitRating.DRAGOON_A, 6);
                    addOffers(campaign, Compute.d6() - 2, UnitMarketType.FACTORY, UnitType.TANK,
                            faction, IUnitRating.DRAGOON_A, 6);
                    addOffers(campaign, Compute.d6() - 3, UnitMarketType.FACTORY, UnitType.AERO,
                            faction, IUnitRating.DRAGOON_A, 6);
                }
            }

            if (!campaign.getFaction().isClan()) {
                addOffers(campaign, Compute.d6(2) - 6, UnitMarketType.BLACK_MARKET, UnitType.MEK,
                        null, IUnitRating.DRAGOON_C, 6);
                addOffers(campaign, Compute.d6(2) - 4, UnitMarketType.BLACK_MARKET, UnitType.TANK,
                        null, IUnitRating.DRAGOON_C, 6);
                addOffers(campaign, Compute.d6(2) - 6, UnitMarketType.BLACK_MARKET, UnitType.AERO,
                        null, IUnitRating.DRAGOON_C, 6);
            }

            if (campaign.getCampaignOptions().getUnitMarketReportRefresh()) {
                campaign.addReport("<a href='UNIT_MARKET'>Unit market updated</a>");
            }
        }
    }

    private void addOffers(Campaign campaign, int num, UnitMarketType market, int unitType,
                           String faction, int quality, int priceTarget) {
        if (faction == null) {
            faction = RandomFactionGenerator.getInstance().getEmployer();
        }
        if (faction == null) {
            faction = campaign.getFactionCode();
            market = UnitMarketType.EMPLOYER;
        }

        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(faction);
        params.setYear(campaign.getGameYear());
        params.setUnitType(unitType);
        params.setQuality(quality);

        for (int i = 0; i < num; i++) {
            params.setWeightClass(getRandomWeight(unitType, faction,
                    campaign.getCampaignOptions().getRegionalMechVariations()));
            params.clearMissionRoles();

            MechSummary ms;
            if (unitType == UnitType.TANK) {
                params.setMovementModes(IUnitGenerator.MIXED_TANK_VTOL);
                params.addMissionRole(MissionRole.MIXED_ARTILLERY);

            } else {
                params.clearMovementModes();
            }
            ms = campaign.getUnitGenerator().generate(params);
            if (ms != null) {
                if (campaign.getCampaignOptions().limitByYear()
                        && (campaign.getGameYear() < ms.getYear())) {
                    continue;
                }
                if ((campaign.getCampaignOptions().allowClanPurchases() && ms.isClan())
                        || (campaign.getCampaignOptions().allowISPurchases() && !ms.isClan())) {
                    int pct = 100 - (Compute.d6(2) - priceTarget) * 5;
                    /*Some RATs, particularly ASF, group multiple weight classes together
                     * so we need to get the actual weight class from the generated unit
                     * (-1 because EntityWeightClass starts with ultra-light).*/
                    offers.add(new MarketOffer(market, unitType, ms.getWeightClass(), ms, pct));
                }
            }
        }
    }

    /* Used by special event */
    public String addSingleUnit(Campaign campaign, UnitMarketType market, int unitType,
                                String faction, int quality, int pricePct) {
        int weight = getRandomWeight(unitType, faction,
                campaign.getCampaignOptions().getRegionalMechVariations());
        MechSummary ms = campaign.getUnitGenerator().generate(faction, unitType, weight,
                campaign.getGameYear(), quality);
        if (ms == null) {
            return null;
        } else {
            offers.add(new MarketOffer(market, unitType, weight,
                    ms, pricePct));
            return ms.getName();
        }

    }

    public static int getRandomWeight(int unitType, String faction, boolean regionalVariations) {
        if (unitType == UnitType.AERO) {
            return getRandomAeroWeight();
        } else if ((unitType == UnitType.MEK) && regionalVariations) {
            return getRegionalMechWeight(faction);
        } else {
            return getRandomMechWeight();
        }
    }

    public static int getRandomMechWeight() {
        int roll = Compute.randomInt(10);
        if (roll <= 2) {
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (roll <= 6) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else if (roll <= 8) {
            return EntityWeightClass.WEIGHT_HEAVY;
        } else {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
    }

    public static int getRegionalMechWeight(String faction) {
        int roll = Compute.randomInt(100);
        switch (faction) {
            case "DC":
                if (roll < 40) return EntityWeightClass.WEIGHT_LIGHT;
                if (roll < 60) return EntityWeightClass.WEIGHT_MEDIUM;
                if (roll < 90) return EntityWeightClass.WEIGHT_HEAVY;
                return EntityWeightClass.WEIGHT_ASSAULT;
            case "LA":
                if (roll < 20) return EntityWeightClass.WEIGHT_LIGHT;
                if (roll < 50) return EntityWeightClass.WEIGHT_MEDIUM;
                if (roll < 85) return EntityWeightClass.WEIGHT_HEAVY;
                return EntityWeightClass.WEIGHT_ASSAULT;
            case "FWL":
                if (roll < 30) return EntityWeightClass.WEIGHT_LIGHT;
                if (roll < 70) return EntityWeightClass.WEIGHT_MEDIUM;
                if (roll < 92) return EntityWeightClass.WEIGHT_HEAVY;
                return EntityWeightClass.WEIGHT_ASSAULT;
        }
        if (roll < 30) return EntityWeightClass.WEIGHT_LIGHT;
        if (roll < 70) return EntityWeightClass.WEIGHT_MEDIUM;
        if (roll < 90) return EntityWeightClass.WEIGHT_HEAVY;
        return EntityWeightClass.WEIGHT_ASSAULT;
    }

    public static int getRandomAeroWeight() {
        int roll = Compute.randomInt(8);
        if (roll <= 2) return EntityWeightClass.WEIGHT_LIGHT;
        if (roll <= 6) return EntityWeightClass.WEIGHT_MEDIUM;
        return EntityWeightClass.WEIGHT_HEAVY;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "unitMarket");
        for (MarketOffer o : offers) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "offer");
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "market", o.market.name());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "unitType", o.unitType);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "unitWeight", o.unitWeight);
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "unit", o.unit.getName());
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "pct", o.pct);
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "offer");
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "unitMarket");
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
                    MekHQ.getLogger().error("Unknown node type not loaded in offer nodes: " + wn2.getNodeName());
                    continue;
                }

                MarketOffer o = new MarketOffer();
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn3 = nl2.item(i);
                    if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (wn3.getNodeName().equalsIgnoreCase("market")) {
                        o.market = UnitMarketType.parseFromString(wn3.getTextContent().trim());
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

                retVal.offers.add(o);
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }


}
