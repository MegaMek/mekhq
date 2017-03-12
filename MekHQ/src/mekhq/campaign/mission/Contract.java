/*
 * Contract.java
 *
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.text.CharacterPredicate;
import org.apache.commons.text.RandomStringGenerator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.BattleArmor;
import megamek.common.Infantry;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planets;


/**
 * Contracts - we need to track static amounts here because changes in the underlying
 * campaign don't change the figures once the ink is dry
 *
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Contract extends Mission implements Serializable, MekHqXmlSerializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4606932545119410453L;

	public final static int OH_NONE = 0;
	public final static int OH_HALF = 1;
	public final static int OH_FULL = 2;
	public final static int OH_NUM = 3;

	public final static int COM_INTEGRATED = 0;
	public final static int COM_HOUSE      = 1;
	public final static int COM_LIAISON    = 2;
	public final static int COM_INDEP      = 3;
	public final static int COM_NUM        = 4;

	private Date startDate;
	private Date endDate;
	private int nMonths;

	private String employer;

	private double paymentMultiplier;
	private int commandRights;
	private int overheadComp;
	private int straightSupport;
	private int battleLossComp;
	private int salvagePct;
	private boolean salvageExchange;
	private int transportComp;

	private boolean mrbcFee;
	private int advancePct;
	private int signBonus;

	//need to keep track of total value salvaged for salvage rights
	private long salvagedByUnit = 0;
	private long salvagedByEmployer = 0;

	//actual amounts
	private long advanceAmount;
	private long signingAmount;
	private long transportAmount;
	private long transitAmount;
	private long overheadAmount;
	private long supportAmount;
	private long baseAmount;
	private long feeAmount;

	public Contract() {
		this(null,null);
	}

	public Contract(String name, String employer) {
		super(name);
		this.employer = employer;

		this.nMonths = 12;
		this.paymentMultiplier = 2.0;
		this.commandRights = COM_HOUSE;
		this.overheadComp = OH_NONE;
		this.straightSupport = 50;
		this.battleLossComp = 50;
		this.salvagePct = 50;
		this.salvageExchange = false;
		this.transportComp = 50;
		this.mrbcFee = true;
		this.advancePct = 25;
		this.signBonus = 0;

	}

	public static String getOverheadCompName(int i) {
		switch(i) {
		case OH_NONE:
			return "None";
		case OH_HALF:
			return "Half";
		case OH_FULL:
			return "Full";
		default:
			return "?";
		}
	}

	public static String getCommandRightsName(int i) {
		switch(i) {
		case COM_INTEGRATED:
			return "Integrated";
		case COM_HOUSE:
			return "House";
		case COM_LIAISON:
			return "Liaison";
		case COM_INDEP:
			return "Independent";
		default:
			return "?";
		}
	}

	public String getEmployer() {
		return employer;
	}

	public void setEmployer(String s) {
		this.employer = s;
	}

	public int getLength() {
		return nMonths;
	}

	public void setLength(int m) {
		nMonths = m;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date d) {
		startDate = d;
	}

	public Date getEndingDate() {
		return endDate;
	}

	public double getMultiplier() {
		return paymentMultiplier;
	}

	public void setMultiplier(double s) {
		paymentMultiplier = s;
	}

	public int getTransportComp() {
		return transportComp;
	}

	public void setTransportComp(int s) {
		transportComp = s;
	}

	public int getStraightSupport() {
		return straightSupport;
	}

	public void setStraightSupport(int s) {
		straightSupport = Math.max(0, Math.min(100, s));
	}

	public int getOverheadComp() {
		return overheadComp;
	}

	public void setOverheadComp(int s) {
		overheadComp = s;
	}

	public int getCommandRights() {
		return commandRights;
	}

	public void setCommandRights(int s) {
		commandRights = s;
	}

	public int getBattleLossComp() {
		return battleLossComp;
	}

	public void setBattleLossComp(int s) {
		battleLossComp = Math.max(0, Math.min(100, s));
	}

	public int getSalvagePct() {
		return salvagePct;
	}

	public void setSalvagePct(int s) {
		salvagePct = s;
	}

	public boolean isSalvageExchange() {
		return salvageExchange;
	}

	public void setSalvageExchange(boolean b) {
		salvageExchange = b;
	}

	public boolean canSalvage() {
		return salvagePct > 0;
	}

	public long getSalvagedByUnit() {
		return salvagedByUnit;
	}

	public void setSalvagedByUnit(long l) {
		this.salvagedByUnit = l;
	}

	public void addSalvageByUnit(long l) {
		salvagedByUnit += l;
	}

	public long getSalvagedByEmployer() {
		return salvagedByEmployer;
	}

	public void setSalvagedByEmployer(long l) {
		this.salvagedByEmployer = l;
	}

	public void addSalvageByEmployer(long l) {
		salvagedByEmployer += l;
	}

	public int getSigningBonusPct() {
		return signBonus;
	}

	public void setSigningBonusPct(int s) {
		signBonus = s;
	}

	public int getAdvancePct() {
		return advancePct;
	}

	public void setAdvancePct(int s) {
		advancePct = s;
	}

	public boolean payMRBCFee() {
		return mrbcFee;
	}

	public void setMRBCFee(boolean b) {
		mrbcFee = b;
	}

	public long getTotalAmountPlusFeesAndBonuses() {
		return baseAmount + supportAmount + overheadAmount + transportAmount + transitAmount + signingAmount - feeAmount;
	}

	public long getTotalAmount() {
		return baseAmount + supportAmount + overheadAmount + transportAmount + transitAmount;
	}

	public long getAdvanceAmount() {
		return advanceAmount;
	}

	protected void setAdvanceAmount(long amount) {
		advanceAmount = amount;
	}

	public long getFeeAmount() {
		return feeAmount;
	}

	protected void setFeeAmount(long amount) {
		feeAmount = amount;
	}

	public long getBaseAmount() {
		return baseAmount;
	}

	protected void setBaseAmount(long amount) {
		baseAmount = amount;
	}

	public long getOverheadAmount() {
		return overheadAmount;
	}

	protected void setOverheadAmount(long amount) {
		overheadAmount = amount;
	}

	public long getSupportAmount() {
		return supportAmount;
	}

	protected void setSupportAmount(long amount) {
		supportAmount = amount;
	}
	
	public long getTransitAmount() {
        return transitAmount;
    }

    protected void setTransitAmount(long amount) {
        transitAmount = amount;
    }
	public long getTransportAmount() {
		return transportAmount;
	}

	protected void setTransportAmount(long amount) {
		transportAmount = amount;
	}

	public long getSigningBonusAmount() {
		return signingAmount;
	}

	protected void setSigningBonusAmount (long amount) {
		signingAmount = amount;
	}

	public long getMonthlyPayOut() {
		return (getTotalAmountPlusFeesAndBonuses() - getTotalAdvanceMonies())/getLength();
	}

	public long getTotalAdvanceMonies() {
		return getAdvanceAmount() + getSigningBonusAmount();
	}
	
	public static String generateRandomContractName() {
		RandomStringGenerator generator = new RandomStringGenerator.Builder()
			     .withinRange('0', 'Z').filteredBy(UpperCaseAndDigits.UPPERANDDIGITS).build();
		return generator.generate(15);
	}

	public static enum UpperCaseAndDigits implements CharacterPredicate {
		UPPERANDDIGITS {
			@Override
			public boolean test(int codePoint) {
				return (Character.isDigit(codePoint) || Character.isUpperCase(codePoint));
			}
		}
	}

	public long getEstimatedTotalProfit(Campaign c) {
		long profit = getTotalAmountPlusFeesAndBonuses();
		profit -= c.getOverheadExpenses() * getLength();
		profit -= c.getMaintenanceCosts() * getLength();
		if (c.getCampaignOptions().usePeacetimeCost()) {
            profit -= c.getPeacetimeCost() * getLength();
        } else {
            profit -= c.getPayRoll() * getLength();
        }
		if(null != c.getPlanet(planetName) && c.getCampaignOptions().payForTransport()) {
			profit -= 2 * c.calculateCostPerJump(true) * c.calculateJumpPath(c.getCurrentPlanet(), getPlanet()).getJumps();
		}
		return profit;
	}

	public int getMonthsLeft(Date date) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		cal.add(Calendar.MONTH, 1);
		date = cal.getTime();
		int monthsLeft = 0;
		while(date.before(endDate) || date.equals(endDate)) {
			monthsLeft++;
			cal.add(Calendar.MONTH, 1);
			date = cal.getTime();
		}
		return monthsLeft;
	}

	/**
	 * Only do this at the time the contract is set up, otherwise amounts may change after
	 * the ink is signed, which is a no-no.
	 * @param c
	 */
	public void calculateContract(Campaign c) {

		//calculate base amount
		baseAmount = (long)(paymentMultiplier * getLength() * c.getContractBase());

		//calculate overhead
		switch(overheadComp) {
		case OH_HALF:
			overheadAmount = (long)(0.5 * c.getOverheadExpenses() * getLength());
			break;
		case OH_FULL:
			overheadAmount = (long)(1 * c.getOverheadExpenses() * getLength());
			break;
		default:
			overheadAmount = 0;
		}

		//calculate support amount
		if (c.getCampaignOptions().usePeacetimeCost()
		        && c.getCampaignOptions().getUnitRatingMethod().equals(mekhq.campaign.rating.UnitRatingMethod.CAMPAIGN_OPS)) {
		    supportAmount = (long)((straightSupport/100.0) * c.getPeacetimeCost() * getLength());
		} else {
		    long maintCosts = 0;
	        for (Unit u : c.getUnits()) {
	            if (u.getEntity() instanceof Infantry && !(u.getEntity() instanceof BattleArmor)) {
	                continue;
	            }
	            maintCosts += u.getWeeklyMaintenanceCost();
	        }
	        maintCosts *= 4;
	        supportAmount = (long)((straightSupport/100.0) * maintCosts * getLength());
		}

		//calculate transportation costs
		if(null != Planets.getInstance().getPlanetById(planetName)) {
	        transportAmount = (long)((transportComp/100.0) * 2 * c.calculateCostPerJump(false) * c.calculateJumpPath(c.getCurrentPlanet(), getPlanet()).getJumps());
		}

		//calculate transit amount for CO
        if (c.getCampaignOptions().usePeacetimeCost()
                && c.getCampaignOptions().getUnitRatingMethod().equals(mekhq.campaign.rating.UnitRatingMethod.CAMPAIGN_OPS)) {
            //contract base * transport period * reputation * employer modifier
            transitAmount = (long)(c.getContractBase() * (((c.calculateJumpPath(c.getCurrentPlanet(), getPlanet()).getJumps()) * 2) / 4) * (c.getUnitRatingMod() * .2 + .5) * 1.2);
        } else {
            transitAmount = 0;
        }

		signingAmount = (long)((signBonus/100.0) * (baseAmount + overheadAmount + transportAmount + transitAmount + supportAmount));

		advanceAmount = (long)((advancePct/100.0) * (baseAmount + overheadAmount + transportAmount + transitAmount + supportAmount));

		if(mrbcFee) {
			feeAmount =  (long)(0.05 * (baseAmount + overheadAmount + transportAmount + transitAmount + supportAmount));
		} else {
			feeAmount = 0;
		}

		//only adjust the start date for travel if the start date is currently null
		boolean adjustStartDate = false;
		if(null == startDate) {
			startDate = c.getCalendar().getTime();
			adjustStartDate = true;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(startDate);
		if(adjustStartDate && null != c.getPlanet(planetName)) {
		    int days = (int)Math.ceil(c.calculateJumpPath(c.getCurrentPlanet(), getPlanet()).getTotalTime(Utilities.getDateTimeDay(cal), c.getLocation().getTransitTime()));
		    while(days > 0) {
		        cal.add(Calendar.DAY_OF_YEAR, 1);
		        days--;
			}
			startDate = cal.getTime();
		}
		int months = getLength();
		while(months > 0) {
			cal.add(Calendar.MONTH, 1);
			months--;
		}
		endDate = cal.getTime();
	}

	protected void writeToXmlBegin(PrintWriter pw1, int indent) {
		super.writeToXmlBegin(pw1, indent);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<nMonths>"
				+nMonths
				+"</nMonths>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<startDate>"
				+df.format(startDate)
				+"</startDate>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<endDate>"
				+df.format(endDate)
				+"</endDate>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<employer>"
				+MekHqXmlUtil.escape(employer)
				+"</employer>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<paymentMultiplier>"
				+paymentMultiplier
				+"</paymentMultiplier>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<commandRights>"
				+commandRights
				+"</commandRights>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<overheadComp>"
				+overheadComp
				+"</overheadComp>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<salvagePct>"
				+salvagePct
				+"</salvagePct>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<salvageExchange>"
				+salvageExchange
				+"</salvageExchange>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<straightSupport>"
				+straightSupport
				+"</straightSupport>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<battleLossComp>"
				+battleLossComp
				+"</battleLossComp>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<transportComp>"
				+transportComp
				+"</transportComp>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<mrbcFee>"
				+mrbcFee
				+"</mrbcFee>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<advancePct>"
				+advancePct
				+"</advancePct>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<signBonus>"
				+signBonus
				+"</signBonus>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<advanceAmount>"
				+advanceAmount
				+"</advanceAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<signingAmount>"
				+signingAmount
				+"</signingAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<transportAmount>"
				+transportAmount
				+"</transportAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<overheadAmount>"
				+overheadAmount
				+"</overheadAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<supportAmount>"
				+supportAmount
				+"</supportAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<baseAmount>"
				+baseAmount
				+"</baseAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<feeAmount>"
				+feeAmount
				+"</feeAmount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<salvagedByUnit>"
				+salvagedByUnit
				+"</salvagedByUnit>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<salvagedByEmployer>"
				+salvagedByEmployer
				+"</salvagedByEmployer>");
	}

	public void loadFieldsFromXmlNode(Node wn) throws ParseException {
		// Okay, now load mission-specific fields!
		NodeList nl = wn.getChildNodes();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);

			if (wn2.getNodeName().equalsIgnoreCase("employer")) {
				employer = wn2.getTextContent();
			}
			else if (wn2.getNodeName().equalsIgnoreCase("startDate")) {
				startDate = df.parse(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("endDate")) {
				endDate = df.parse(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("nMonths")) {
				nMonths = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("paymentMultiplier")) {
				paymentMultiplier = Double.parseDouble(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("commandRights")) {
				commandRights = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("overheadComp")) {
				overheadComp = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("salvagePct")) {
				salvagePct = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("salvageExchange")) {
				if (wn2.getTextContent().trim().equals("true"))
					salvageExchange = true;
				else
					salvageExchange = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("straightSupport")) {
				straightSupport = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("battleLossComp")) {
				battleLossComp = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("salvagePct")) {
				salvagePct = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("transportComp")) {
				transportComp = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("advancePct")) {
				advancePct = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("signBonus")) {
				signBonus = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("mrbcFee")) {
				if (wn2.getTextContent().trim().equals("true"))
					mrbcFee = true;
				else
					mrbcFee = false;
			}
			else if (wn2.getNodeName().equalsIgnoreCase("advanceAmount")) {
				advanceAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("signingAmount")) {
				signingAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("transportAmount")) {
				transportAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("overheadAmount")) {
				overheadAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("supportAmount")) {
				supportAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("baseAmount")) {
				baseAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("feeAmount")) {
				feeAmount = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("salvagedByUnit")) {
				salvagedByUnit = Long.parseLong(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("salvagedByEmployer")) {
				salvagedByEmployer = Long.parseLong(wn2.getTextContent().trim());
			}
		}
	}
}