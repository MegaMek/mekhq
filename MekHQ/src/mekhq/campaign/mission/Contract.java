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

import java.io.Serializable;
import java.util.Date;

import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;


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
	private int transportComp;
	
	private boolean mrbcFee;
	private int advancePct;
	private int signBonus;
	
	//actual amounts
	private long advanceAmount;
	private long signingAmount;
	private long transportAmount;
	private long overheadAmount;
	private long supportAmount;
	private long baseAmount;
	private long feeAmount;
	
	public Contract(String name, String employer) {
		super(name);
		this.employer = employer;
		
		this.nMonths = 12;
		this.paymentMultiplier = 5.0;
		this.commandRights = COM_HOUSE;
		this.overheadComp = OH_NONE;
		this.straightSupport = 50;
		this.battleLossComp = 50;
		this.transportComp = 100;
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
	
	public int getLength() {
		return nMonths;
	}
	
	public void setLength(int m) {
		nMonths = m;
	}
	
	public double getMultiplier() {
		return paymentMultiplier;
	}
	
	public void setMultiplier(double s) {
		paymentMultiplier = s;
	}
	
	public int getStraightSupport() {
		return straightSupport;
	}
	
	public void setStraightSupport(int s) {
		straightSupport = s;
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
		battleLossComp = s;
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
		return baseAmount + supportAmount + overheadAmount + transportAmount + signingAmount - feeAmount;
	}
	
	public long getTotalAmount() {
		return baseAmount + supportAmount + overheadAmount + transportAmount;
	}
	
	public long getAdvanceAmount() {
		return advanceAmount;
	}
	
	public long getFeeAmount() {
		return feeAmount;
	}

	public long getBaseAmount() {
		return baseAmount;
	}
	
	public long getOverheadAmount() {
		return overheadAmount;
	}
	
	public long getSupportAmount() {
		return supportAmount;
	}

	public long getTransportAmount() {
		return transportAmount;
	}
	
	public long getSigningBonusAmount() {
		return signingAmount;
	}
	
	public long getMonthlyPayOut() {
		return (getTotalAmountPlusFeesAndBonuses() - getTotalAdvanceMonies())/getLength();
	}
	
	public long getTotalAdvanceMonies() {
		return getAdvanceAmount() + getSigningBonusAmount();
	}
	
	public long getEstimatedTotalProfit(Campaign c) {
		long profit = getTotalAmountPlusFeesAndBonuses();
		profit -= c.getOverheadExpenses() * getLength();
		profit -= c.getMaintenanceCosts() * getLength();
		profit -= c.getPayRoll() * getLength();
		return profit;
	}
	
	/**
	 * Only do this at the time the contract is set up, otherwise amounts may change after
	 * the ink is signed, which is a no-no.
	 * @param c
	 */
	public void calculateContract(Campaign c) {
		
		//calculate base amount
		baseAmount = (long)(paymentMultiplier * getLength() * c.getPayRoll());
		
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
		
		supportAmount = (long)((straightSupport/100.0) * c.getSupportPayRoll() * getLength());
		
		signingAmount = (long)((signBonus/100.0) * (baseAmount + overheadAmount + transportAmount + supportAmount));
		
		//TODO: transport amount
		transportAmount = 0;
		
		advanceAmount = (long)((advancePct/100.0) * (baseAmount + overheadAmount + transportAmount + supportAmount));
		
		if(mrbcFee) {
			feeAmount =  (long)(0.05 * (baseAmount + overheadAmount + transportAmount + supportAmount));
		} else {
			feeAmount = 0;
		}
	}
	
}