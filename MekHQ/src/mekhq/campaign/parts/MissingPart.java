/*
 * MissingPart.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.UUID;

import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;

/**
 * A missing part is a placeholder on a unit to indicate that a replacement
 * task needs to be performed
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class MissingPart extends Part implements Serializable, MekHqXmlSerializable, IPartWork, IAcquisitionWork {

	/**
	 *
	 */
	private static final long serialVersionUID = 300672661487966982L;

	public MissingPart(int tonnage, Campaign c) {
	    super(tonnage, false, c);
	}

	public MissingPart(int tonnage, boolean isOmniPodded, Campaign c) {
		super(tonnage, isOmniPodded, c);
	}

	public MissingPart clone() {
		//should never be called
		return null;
	}

	@Override
	public long getStickerPrice() {
		//missing parts aren't worth a thing
		return 0;
	}

	@Override
	public long getBuyCost() {
	    return getNewPart().getStickerPrice();
	}

	@Override
	public boolean isSalvaging() {
		return false;
	}

	@Override
	public String getStatus() {
		return "Destroyed";
	}

	@Override
	public boolean isSamePartType(Part part) {
		//missing parts should always return false
		return false;
	}

	public String getDesc() {
		String bonus = getAllMods(null).getValueAsString();
		if (getAllMods(null).getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getTeamId() != null) {
			scheduled = " (scheduled) ";
		}

		//if (this instanceof ReplacementItem
		//		&& !((ReplacementItem) this).hasPart()) {
		//	toReturn += " color='white'";
		//}
		toReturn += ">";
		toReturn += "<b>Replace " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		if(getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='red'>Impossible</font>";
        } else {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
            if(!getCampaign().getCampaignOptions().isDestroyByMargin()) {
                toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
            }
            toReturn += " " + bonus;
            if (getMode() != WorkTime.NORMAL) {
                toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
            }
        }
		toReturn += "</font></html>";
		return toReturn;
	}

	@Override
	public String succeed() {
		fix();
		return " <font color='green'><b> replaced.</b></font>";
	}

	@Override
	public void fix() {
		Part replacement = findReplacement(false);
		if(null != replacement) {
			Part actualReplacement = replacement.clone();
			unit.addPart(actualReplacement);
			campaign.addPart(actualReplacement, 0);
			replacement.decrementQuantity();
			remove(false);
			//assign the replacement part to the unit
			actualReplacement.updateConditionFromPart();
		}
	}

	@Override
	public void remove(boolean salvage) {
		campaign.removePart(this);
		if(null != unit) {
			unit.removePart(this);
		}
		setUnit(null);
	}

	public abstract boolean isAcceptableReplacement(Part part, boolean refit);

	public Part findReplacement(boolean refit) {
		Part bestPart = null;

		//check to see if we already have a replacement assigned
		if(replacementId > -1) {
			bestPart = campaign.getPart(replacementId);
			if(null != bestPart) {
				return bestPart;
			}
		}
		// don't just return with the first part if it is damaged
		for(Part part : campaign.getSpareParts()) {
			if(part.isReservedForRefit() || part.isBeingWorkedOn() || part.isReservedForReplacement() || !part.isPresent() || part.hasParentPart()) {
				continue;
			}

			if(isAcceptableReplacement(part, refit)) {
				if(null == bestPart) {
					bestPart = part;
				} else if(bestPart.needsFixing() && !part.needsFixing()) {
					bestPart = part;
				}
			}
		}
		return bestPart;
	}

	public boolean isReplacementAvailable() {
		return null != findReplacement(false);
	}

	@Override
    public String getDetails() {
	    String[] inventories = campaign.getPartInventory(getNewPart());
		if(isReplacementAvailable()) {
			return "Replacement part available";
		} else {
			return "<font color='red'>No replacement (" + inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
		}
    }

	@Override
	public boolean needsFixing() {
		//missing parts always need fixing
		if(null != unit) {
			return (!unit.isSalvage() || null != getTeamId()) && unit.isRepairable();
		}
		return false;
	}

	@Override
	public MissingPart getMissingPart() {
		//do nothing - this should never be accessed
		return null;
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		//do nothing
	}

	@Override
	public String fail(int rating) {
		skillMin = ++rating;
		timeSpent = 0;
		shorthandedMod = 0;
		if(skillMin > SkillType.EXP_ELITE) {
			Part part = findReplacement(false);
			if(null != part) {
				part.decrementQuantity();
				skillMin = SkillType.EXP_GREEN;
			}
			return " <font color='red'><b> failed and part destroyed.</b></font>";
		} else {
			return " <font color='red'><b> failed.</b></font>";
		}
	}

	@Override
	public boolean canChangeWorkMode() {
	    return !isOmniPodded();
	}

	@Override
	public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        if(getTechBase() == T_CLAN && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        }
        else if(getTechBase() == T_IS && campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        else if(getTechBase() == T_BOTH) {
            int penalty = Math.min(campaign.getCampaignOptions().getClanAcquisitionPenalty(), campaign.getCampaignOptions().getIsAcquisitionPenalty());
            if(penalty > 0) {
                target.addModifier(penalty, "tech limit");
            }
        }
        //availability mod
        int avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");

        return target;
    }

	@Override
	public String getAcquisitionDesc() {
		String toReturn = "<html><font size='2'";

		toReturn += ">";
		toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
		String[] inventories = campaign.getPartInventory(getNewPart());
		toReturn += inventories[1] + " in transit, " + inventories[2] + " on order";
		if (!isOmniPodded()) {
		    Part newPart = getAcquisitionPart();
		    newPart.setOmniPodded(true);
		    inventories = campaign.getPartInventory(newPart);
		    if (Integer.parseInt(inventories[0]) > 0) {
		        toReturn += ", " + inventories[0] + " OmniPod";
		    }
		}
		toReturn += "<br/>";
		toReturn += Utilities.getCurrencyString(getBuyCost()) + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
	}

    @Override
    public String getAcquisitionDisplayName() {
    	return getAcquisitionName();
    }

	@Override
	public String getAcquisitionExtraDesc() {
		return "";
	}

	@Override
    public String getAcquisitionBonus() {
		String bonus = getAllAcquisitionMods().getValueAsString();
		if(getAllAcquisitionMods().getValue() > -1) {
			bonus = "+" + bonus;
		}

		return "(" + bonus + ")";
    }

	@Override
	public Part getAcquisitionPart() {
		return getNewPart();
	}

	@Override
	public String find(int transitDays) {
		Part newPart = getNewPart();
		newPart.setBrandNew(true);
		newPart.setDaysToArrival(transitDays);
		if(campaign.buyPart(newPart, transitDays)) {
		    return "<font color='green'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
		} else {
		    return "<font color='red'><b> You cannot afford this part. Transaction cancelled</b>.</font>";
		}
	}

	@Override
	public Object getNewEquipment() {
	    return getNewPart();
	}

	public abstract Part getNewPart();

	@Override
	public String failToFind() {
	    resetDaysToWait();
		return "<font color='red'><b> part not found</b>.</font>";
	}

	@Override
	public void writeToXmlBegin(PrintWriter pw1, int indent) {
		super.writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<daysToWait>"
				+daysToWait
				+"</daysToWait>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<replacementId>"
				+replacementId
				+"</replacementId>");
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	public String checkScrappable() {
		if(!isReplacementAvailable()) {
			return "Nothing to scrap";
		}
		return null;
	}

	@Override
	public String scrap() {
		Part replace = findReplacement(false);
		if(null != replace) {
			replace.decrementQuantity();
		}
		skillMin = SkillType.EXP_GREEN;
		return replace.getName() + " scrapped.";
	}

	@Override
	public String getAcquisitionName() {
	    String details = getNewPart().getDetails();
	    details = details.replaceFirst("\\d+\\shit\\(s\\)", "");
		return getPartName() + " " + details;
	}

	@Override
	public int getTechLevel() {
		return getNewPart().getTechLevel();
	}

	@Override
	public void reservePart() {
		//this is being set as an overnight repair, so
		//we also need to reserve the replacement. If the
		//quantity of the replacement is more than one, we will
		//also need to split off a separate one
		//shouldn't be null, but it never hurts to check
		Part replacement = findReplacement(false);
		UUID teamId = getTeamId();
		if(null != replacement && null != teamId) {
			if(replacement.getQuantity() > 1) {
				Part actualReplacement = replacement.clone();
				actualReplacement.setReserveId(teamId);
				campaign.addPart(actualReplacement, 0);
				replacementId = actualReplacement.getId();
				replacement.decrementQuantity();
			} else {
				replacement.setReserveId(teamId);
				replacementId = replacement.getId();
			}
		}
	}

	@Override
	public void cancelReservation() {
		Part replacement = findReplacement(false);
		if(replacementId > -1 && null != replacement) {
			replacementId = -1;
			replacement.setReserveId(null);
			if(replacement.isSpare()) {
				Part spare = campaign.checkForExistingSparePart(replacement);
				if(null != spare) {
					spare.incrementQuantity();
					campaign.removePart(replacement);
				}
			}
		}
	}

	@Override
	public boolean needsMaintenance() {
        return false;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}
