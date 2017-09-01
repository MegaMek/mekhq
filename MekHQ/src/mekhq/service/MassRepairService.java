package mekhq.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import megamek.common.Aero;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignOptions.MassRepairOption;
import mekhq.campaign.force.Force;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.MassRepairSalvageDialog;
import mekhq.gui.sorter.UnitStatusSorter;

public class MassRepairService {
	private MassRepairService() {
	}

	public static boolean isValidMRMSUnit(Unit unit) {
		if (unit.isSelfCrewed() || !unit.isAvailable()) {
			return false;
		}

		if (unit.isDeployed()) {
			return false;
		}

		return (unit.getEntity() instanceof Tank) || (unit.getEntity() instanceof Aero)
				|| (unit.getEntity() instanceof Mech);
	}

	private static List<MassRepairOption> createActiveMROsFromConfiguration(CampaignGUI campaignGUI) {
		List<MassRepairOption> activeMROs = new ArrayList<MassRepairOption>();
		List<MassRepairOption> mroList = campaignGUI.getCampaign().getCampaignOptions().getMassRepairOptions();

		if (null != mroList) {
			for (int i = 0; i < mroList.size(); i++) {
				MassRepairOption mro = mroList.get(i);

				if (mro.isActive()) {
					activeMROs.add(mro);
				}
			}
		}

		return activeMROs;
	}

	public static int performWarehouseMassRepair(List<IPartWork> selectedParts, List<MassRepairOption> mroList,
			MassRepairConfiguredOptions configuredOptions, CampaignGUI campaignGUI) {
		Campaign campaign = campaignGUI.getCampaign();

		campaign.addReport("Beginning mass warehouse repair.");

		ArrayList<Person> techs = campaign.getTechs(true);

		int totalActionsPerformed = 0;

		if (techs.isEmpty()) {
			campaign.addReport("No available techs to repairs parts.");
		} else {
			Map<Integer, MassRepairOption> mroByTypeMap = new HashMap<Integer, MassRepairOption>();

			for (int i = 0; i < mroList.size(); i++) {
				mroByTypeMap.put(mroList.get(i).getType(), mroList.get(i));
			}

			/*
			 * Filter our parts list to only those that aren't being worked on
			 * or those that meet our criteria as defined in the campaign
			 * configurations
			 */
			List<IPartWork> parts = filterParts(selectedParts, mroByTypeMap);

			if (parts.isEmpty()) {
				return totalActionsPerformed;
			}

			for (IPartWork partWork : parts) {
				((Part) partWork).resetModeToNormal();
				
				Part part = (Part) partWork;
				
				List<Person> validTechs = filterTechs(partWork, techs, mroByTypeMap, true, campaignGUI);

				if (validTechs.isEmpty()) {
					campaign.addReport(
							"<font color='red'>Unable to repair any more parts because there are no available techs.</font>");
					continue;
				}

				int originalQuantity = part.getQuantity();

				for (int i = 0; i < originalQuantity; i++) {
					if (repairPart(campaignGUI, part, null, validTechs, mroByTypeMap, configuredOptions, true)) {
						totalActionsPerformed++;
					}
				}
			}
		}

		return totalActionsPerformed;
	}

	public static void performSingleUnitMassRepairOrSalvage(CampaignGUI campaignGUI, Unit unit) {
		CampaignOptions options = campaignGUI.getCampaign().getCampaignOptions();
		List<MassRepairOption> activeMROs = createActiveMROsFromConfiguration(campaignGUI);
		String msg = "";

		MassRepairConfiguredOptions configuredOptions = new MassRepairConfiguredOptions();
		configuredOptions.setup(options);

		int repairsCompleted = performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(), activeMROs,
				configuredOptions);

		if (repairsCompleted == 1) {
			msg = "Mass Repair/Salvage complete. There was 1 repair completed or scheduled.";
		} else {
			msg = String.format("Mass Repair/Salvage complete. There were %d repairs completed or scheduled.",
					repairsCompleted);
		}

		JOptionPane.showMessageDialog(campaignGUI.getFrame(), msg, "Complete", JOptionPane.INFORMATION_MESSAGE);

		campaignGUI.getCampaign().addReport(msg);
	}

	public static void massRepairSalvageAllUnits(CampaignGUI campaignGUI) {
		CampaignOptions options = campaignGUI.getCampaign().getCampaignOptions();
		List<MassRepairOption> activeMROs = createActiveMROsFromConfiguration(campaignGUI);
		String msg = "";
		int repairsCompleted = 0;

		List<Unit> units = new ArrayList<>();

		for (int i = 0; i < campaignGUI.getCampaign().getServiceableUnits().size(); i++) {
			Unit unit = campaignGUI.getCampaign().getServiceableUnits().get(i);

			if (!isValidMRMSUnit(unit)) {
				continue;
			}

			units.add(unit);
		}

		// Sort the list status fixing the least damaged first
		Collections.sort(units, new Comparator<Unit>() {
			@Override
			public int compare(Unit o1, Unit o2) {
				int damageIdx1 = UnitStatusSorter.getDamageStateIndex(Unit.getDamageStateName(o1.getDamageState()));
				int damageIdx2 = UnitStatusSorter.getDamageStateIndex(Unit.getDamageStateName(o2.getDamageState()));

				if (damageIdx2 == damageIdx1) {
					return 0;
				} else if (damageIdx2 < damageIdx1) {
					return -1;
				}

				return 1;
			}
		});

		MassRepairConfiguredOptions configuredOptions = new MassRepairConfiguredOptions();
		configuredOptions.setup(options);

		for (Unit unit : units) {
			repairsCompleted += performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(), activeMROs,
					configuredOptions);
		}

		if (repairsCompleted == 1) {
			msg = "Mass Repair/Salvage complete. There was 1 repair completed or scheduled.";
		} else {
			msg = String.format("Mass Repair/Salvage complete. There were %d repairs completed or scheduled.",
					repairsCompleted);
		}

		JOptionPane.showMessageDialog(campaignGUI.getFrame(), msg, "Complete", JOptionPane.INFORMATION_MESSAGE);

		campaignGUI.getCampaign().addReport(msg);
	}

	public static int performUnitMassRepairOrSalvage(CampaignGUI campaignGUI, Unit unit, boolean isSalvage,
			List<MassRepairOption> mroList, MassRepairConfiguredOptions configuredOptions) {
		String actionDescriptor = isSalvage ? "salvage" : "repair";
		Campaign campaign = campaignGUI.getCampaign();

		campaign.addReport(String.format("Beginning mass %s on %s.", actionDescriptor, unit.getName()));

		ArrayList<Person> techs = campaign.getTechs(true);

		int totalActionsPerformed = 0;

		if (techs.isEmpty()) {
			campaign.addReport(String.format("<font color='red'>No available techs to %s parts %s %s.</font>",
					actionDescriptor, isSalvage ? "from" : "on", unit.getName()));
		} else {
			// Filter our tech list to only our techs that can work on this unit
			for (int i = techs.size() - 1; i >= 0; i--) {
				Person tech = techs.get(i);

				if (!tech.canTech(unit.getEntity())) {
					techs.remove(i);
				}
			}

			Map<Integer, MassRepairOption> mroByTypeMap = new HashMap<Integer, MassRepairOption>();

			for (int i = 0; i < mroList.size(); i++) {
				mroByTypeMap.put(mroList.get(i).getType(), mroList.get(i));
			}

			/*
			 * Possibly call this multiple times. Sometimes some actions are
			 * first dependent upon others being finished, also failed actions
			 * can be performed again by a tech with a higher skill.
			 */
			int actionsPerformed = 1;

			while (actionsPerformed > 0) {
				actionsPerformed = performUnitMassTechAction(campaignGUI, unit, techs, mroByTypeMap, isSalvage,
						configuredOptions);
				totalActionsPerformed += actionsPerformed;
			}

			campaign.addReport(String.format("Mass %s complete on %s. %d total actions performed.", actionDescriptor,
					unit.getName(), totalActionsPerformed));
		}

		return totalActionsPerformed;
	}

	private static int performUnitMassTechAction(CampaignGUI campaignGUI, Unit unit, List<Person> techs,
			Map<Integer, MassRepairOption> mroByTypeMap, boolean salvaging,
			MassRepairConfiguredOptions configuredOptions) {
		Campaign campaign = campaignGUI.getCampaign();
		int totalActionsPerformed = 0;
		String actionDescriptor = salvaging ? "salvage" : "repair";

		List<IPartWork> parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());

		/*
		 * If we're performing an action on a unit and we allow auto-scrapping
		 * of parts that can't be fixed by an elite tech, let's first get rid of
		 * those parts and start with a cleaner slate
		 */
		if (configuredOptions.isScrapImpossible()) {
			boolean refreshParts = false;

			for (IPartWork partWork : parts) {
				if (partWork instanceof Part && partWork.getSkillMin() > SkillType.EXP_ELITE) {
					campaign.addReport(((Part) partWork).scrap());
					refreshParts = true;
				}
			}

			if (refreshParts) {
				parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());
			}
		}

		if (unit.getEntity().isOmni() && !unit.isSalvage()) {
			for (PodSpace ps : unit.getPodSpace()) {
				ps.setRepairInPlace(!configuredOptions.isReplacePodParts());
			}
			
			/*
				// This needs to be looked at by Neoancient since he put this in. 
				// I don't think this accomplishes the desire effect.
			
			// If we're replacing damaged parts, we want to remove any that have
			// an available replacement
			// from the list since the pod space repair will cover it.

			List<IPartWork> temp = new ArrayList<>();
			
			for (IPartWork p : parts) {
				if ((p instanceof Part) && ((Part)p).isOmniPodded()) {					
					MissingPart m = ((Part) p).getMissingPart();
					
					if (m != null && m.isReplacementAvailable()) {
						continue;
					}
				}
				
				temp.add(p);
			}
			
			parts = temp;
			*/
		}

		if (techs.isEmpty()) {
			campaign.addReport(String.format(
					"<font color='red'>Unable to %s any more parts from %s because there are no available techs.</font>",
					actionDescriptor, unit.getName()));
			return totalActionsPerformed;
		}

		/*
		 * If we're a mek and we have a limb with a bad shoulder/hip, we're
		 * going to try to flip it to salvageable and remove all the parts so
		 * that we can nuke the limb. If we do this, when we're finally done we
		 * need to flip the mek back to repairable so that we don't accidentally
		 * strip everything off it.
		 */
		boolean scrappingLimbMode = false;

		/*
		 * Pre checking for hips/shoulders on repairable meks. If we have a bad
		 * hip or shoulder, we're not going to do anything until we get those
		 * parts out of the location and scrap it. Once we're at a happy place,
		 * we'll proceed.
		 */

		if ((unit.getEntity() instanceof Mech)) {
			Map<Integer, Part> locationMap = new HashMap<Integer, Part>();

			for (IPartWork partWork : parts) {
				if ((partWork instanceof MekLocation) && ((MekLocation) partWork).onBadHipOrShoulder()) {
					locationMap.put(((MekLocation) partWork).getLoc(), (MekLocation) partWork);
				} else if (partWork instanceof MissingMekLocation) {
					locationMap.put(partWork.getLocation(), (MissingMekLocation) partWork);
				}
			}

			if (!locationMap.isEmpty()) {
				MassRepairOption mro = mroByTypeMap.get(Part.REPAIR_PART_TYPE.GENERAL_LOCATION);

				if ((null == mro) || !mro.isActive()) {
					campaign.addReport(
							"Unable to proceed with repairs because this mek has an unfixable limb but configured settings do not allow location repairs.");

					return 0;
				}

				/*
				 * Find our parts in our bad locations. If we don't actually
				 * have, just scrap the limbs and move on with our normal work
				 */

				scrappingLimbMode = true;

				if (!salvaging) {
					unit.setSalvage(true);
				}

				List<IPartWork> partsTemp = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());
				List<IPartWork> partsToBeRemoved = new ArrayList<IPartWork>();
				Map<Integer, Integer> countOfPartsPerLocation = new HashMap<Integer, Integer>();

				for (IPartWork partWork : partsTemp) {
					if (!(partWork instanceof MekLocation) && !(partWork instanceof MissingMekLocation)
							&& locationMap.containsKey(partWork.getLocation()) && partWork.isSalvaging()) {
						campaign.addReport(String.format("Planning to remove a %s due to a bad location.",
								partWork.getPartName()));

						partsToBeRemoved.add(partWork);

						int count = 0;

						if (countOfPartsPerLocation.containsKey(partWork.getLocation())) {
							count = countOfPartsPerLocation.get(partWork.getLocation());
						}

						count++;

						countOfPartsPerLocation.put(partWork.getLocation(), count);
					}
				}

				if (partsToBeRemoved.isEmpty()) {
					/*
					 * We have no parts left on our unfixable locations, so
					 * we'll just scrap those locations and rebuild the parts
					 * list and reset back our normal repair mode
					 */

					for (Part part : locationMap.values()) {
						if (part instanceof MekLocation) {
							campaign.addReport(part.scrap());
						}
					}

					scrappingLimbMode = false;

					if (!salvaging) {
						unit.setSalvage(false);
					}

					parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());
				} else {
					for (int locId : countOfPartsPerLocation.keySet()) {
						boolean unfixable = false;
						Part loc = null;

						if (locationMap.containsKey(locId)) {
							loc = locationMap.get(locId);
							unfixable = (loc instanceof MekLocation);
						}

						if (unfixable) {
							campaign.addReport(String.format(
									"<font color='orange'>Found an unfixable limb - %s which contains %s parts. Going to remove all parts and scrap the limb before proceeding with other repairs.</font>",
									loc.getName(), countOfPartsPerLocation.get(locId)));
						} else {
							campaign.addReport(String.format(
									"<font color='orange'>Found missing location - %s which contains %s parts. Going to remove all parts before proceeding with other repairs.</font>",
									loc.getName(), countOfPartsPerLocation.get(locId)));
						}
					}

					parts = partsToBeRemoved;
				}
			}
		}

		/*
		 * If we're scrapping limbs, we don't want salvage repairs to go into a
		 * new day otherwise it can be confusing when trying to figure why a
		 * unit can't be repaired because 'salvage' repairs don't show up on the
		 * task list as scheduled if we're in 'repair' mode.
		 */
		if (scrappingLimbMode) {
			configuredOptions.setAllowCarryover(false);
		}

		/*
		 * Filter our parts list to only those that aren't being worked on or
		 * those that meet our criteria as defined in the campaign
		 * configurations
		 */
		parts = filterParts(parts, mroByTypeMap);

		if (parts.isEmpty()) {
			campaign.addReport(
					String.format("Unable to %s any more parts from %s because there are no valid parts left to %s.",
							actionDescriptor, unit.getName(), actionDescriptor));

			if (scrappingLimbMode) {
				unit.setSalvage(false);
			}

			return totalActionsPerformed;
		}

		for (IPartWork partWork : parts) {
			((Part) partWork).resetModeToNormal();
			
			List<Person> validTechs = filterTechs(partWork, techs, mroByTypeMap, false, campaignGUI);

			if (validTechs.isEmpty()) {
				campaign.addReport(String.format(
						"<font color='orange'>Unable to %s a %s because there are no valid available techs.</font>",
						actionDescriptor, partWork.getPartName()));
				continue;
			}

			// Search the list of techs each time for a variety of checks. We'll
			// create a temporary truncated list of techs
			if (repairPart(campaignGUI, partWork, unit, validTechs, mroByTypeMap, configuredOptions, false)) {
				totalActionsPerformed++;
			}
		}

		if (scrappingLimbMode) {
			unit.setSalvage(false);
		}

		return totalActionsPerformed;
	}

	private static boolean repairPart(CampaignGUI campaignGUI, IPartWork partWork, Unit unit, List<Person> techs,
			Map<Integer, MassRepairOption> mroByTypeMap, MassRepairConfiguredOptions configuredOptions,
			boolean warehouseMode) {

		// We were doing this check for every tech, that's unnecessary as it
		// doesn't change from tech to tech
		MassRepairOption mro = mroByTypeMap.get(IPartWork.findCorrectMassRepairType(partWork));

		if (null == mro) {
			return false;
		}

		Map<String, WorkTime> techToWorktimeMap = new HashMap<String, WorkTime>();
		Campaign campaign = campaignGUI.getCampaign();
		List<Person> sameDayTechs = new ArrayList<Person>();
		List<Person> overflowDayTechs = new ArrayList<Person>();
		List<Person> sameDayAssignedTechs = new ArrayList<Person>();
		List<Person> overflowDayAssignedTechs = new ArrayList<Person>();

		int highestAvailableTechSkill = -1;

		for (int i = techs.size() - 1; i >= 0; i--) {
			Person tech = techs.get(i);
			Skill skill = tech.getSkillForWorkingOn(partWork);

			if (skill.getExperienceLevel() > highestAvailableTechSkill) {
				highestAvailableTechSkill = skill.getExperienceLevel();
			}

			if (highestAvailableTechSkill == SkillType.EXP_ELITE) {
				break;
			}
		}

		WorkTime normalWorktime = WorkTime.NORMAL;
		
		for (int i = techs.size() - 1; i >= 0; i--) {
			Person tech = techs.get(i);
			TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);
			WorkTime selectedWorktime = normalWorktime;
			
			// Check if we need to increase the time to meet the min BTH
			if (targetRoll.getValue() > mro.getBthMin()) {
				if (!configuredOptions.isUseExtraTime()) {
					continue;
				}

				WorkTimeCalculation workTimeCalc = calculateNewMassRepairWorktime(partWork, tech, mro, campaign, true,
						highestAvailableTechSkill);

				if (null == workTimeCalc.getWorkTime()) {
					if (workTimeCalc.isReachedMaxSkill()) {
						campaign.addReport(String.format(
								"<font color='orange'>Unable to act on %s because it is not possible for the best currently available technician (%s) to achieve the configured BTH of %s.</font>",
								partWork.getPartName(), SkillType.getExperienceLevelName(highestAvailableTechSkill),
								mro.getBthMin()));

						return false;
					} else {
						continue;
					}
				}

				selectedWorktime = workTimeCalc.getWorkTime();
			} else if (targetRoll.getValue() < mro.getBthMax()) {
				// Or decrease the time to meet the max BTH
				if (configuredOptions.isUseRushJob()) {
					WorkTimeCalculation workTimeCalc = calculateNewMassRepairWorktime(partWork, tech, mro, campaign,
							false, highestAvailableTechSkill);

					// This should never happen, but...
					if (null != workTimeCalc.getWorkTime()) {
						selectedWorktime = workTimeCalc.getWorkTime();
					}
				}
			}

			boolean assigned = false;

			if ((null != unit) && configuredOptions.isUseAssignedTechsFirst()) {
				Force force = campaign.getForce(unit.getForceId());

				if ((null != force) && (null != force.getTechID())) {
					assigned = force.getTechID().toString().equals(tech.getId().toString());
				}

				if (!assigned && (null != tech.getTechUnitIDs()) && !tech.getTechUnitIDs().isEmpty()) {
					assigned = tech.getTechUnitIDs().contains(unit.getId());
				}
			}

			boolean isSameDayTech = false;

			if ((tech.getMinutesLeft() < partWork.getActualTime())) {
				if (!configuredOptions.isAllowCarryover()) {
					continue;
				}

				if (configuredOptions.isOptimizeToCompleteToday()) {
					isSameDayTech = false;
				} else {
					isSameDayTech = true;
				}
			} else {
				isSameDayTech = true;
			}

			if (isSameDayTech) {
				if (assigned) {
					sameDayAssignedTechs.add(tech);
				} else {
					sameDayTechs.add(tech);
				}
			} else {
				if (assigned) {
					overflowDayAssignedTechs.add(tech);
				} else {
					overflowDayTechs.add(tech);
				}
			}

			techToWorktimeMap.put(tech.getId().toString(), selectedWorktime);
			((Part) partWork).resetModeToNormal();
		}

		if (overflowDayTechs.isEmpty() && sameDayTechs.isEmpty()) {
			return false;
		}

		TechSorter sorter = new TechSorter(partWork);

		if (!overflowDayTechs.isEmpty()) {
			Collections.sort(overflowDayTechs, sorter);
		}

		if (!sameDayTechs.isEmpty()) {
			Collections.sort(sameDayTechs, sorter);
		}

		if (!overflowDayAssignedTechs.isEmpty()) {
			Collections.sort(overflowDayAssignedTechs, sorter);
		}

		if (!sameDayAssignedTechs.isEmpty()) {
			Collections.sort(sameDayAssignedTechs, sorter);
		}

		List<Person> validTechs = new ArrayList<Person>();
		validTechs.addAll(sameDayAssignedTechs);
		validTechs.addAll(sameDayTechs);
		validTechs.addAll(overflowDayAssignedTechs);
		validTechs.addAll(overflowDayTechs);

		Person tech = validTechs.get(0);

		if (partWork instanceof Part) {
			WorkTime wt = techToWorktimeMap.get(tech.getId().toString());

			((Part) partWork).setMode(wt);
		}

		if (warehouseMode) {
			campaign.fixWarehousePart((Part) partWork, tech);
		} else {
			campaign.fixPart(partWork, tech);
		}

		// If this tech has no time left, filter them out so we don't
		// spend cycles on them in the future
		if (tech.getMinutesLeft() <= 0) {
			techs.remove(tech);
		}

		Thread.yield();

		return true;
	}

	private static List<IPartWork> filterParts(List<IPartWork> parts, Map<Integer, MassRepairOption> mroByTypeMap) {
		List<IPartWork> newParts = new ArrayList<IPartWork>();

		for (IPartWork partWork : parts) {
			if (partWork.isBeingWorkedOn()) {
				continue;
			}
			
			if (partWork instanceof PodSpace) {
				continue;
			}
			
			if (partWork instanceof MissingPart && !((MissingPart) partWork).isReplacementAvailable()) {
				continue;
			}

			int repairType = IPartWork.findCorrectMassRepairType(partWork);

			MassRepairOption mro = mroByTypeMap.get(repairType);

			if ((null != mro) && mro.isActive()) {
				if (!checkArmorSupply(partWork)) {
					continue;
				}

				newParts.add(partWork);
			}
		}

		return newParts;
	}

	private static List<Person> filterTechs(IPartWork partWork, List<Person> techs,
			Map<Integer, MassRepairOption> mroByTypeMap, boolean warehouseMode, CampaignGUI campaignGUI) {
		List<Person> validTechs = new ArrayList<Person>();

		if (techs.isEmpty()) {
			return validTechs;
		}

		MassRepairOption mro = mroByTypeMap.get(IPartWork.findCorrectMassRepairType(partWork));

		if (null == mro) {
			return validTechs;
		}

		int modePenalty = partWork.getMode().expReduction;
		Campaign campaign = campaignGUI.getCampaign();

		for (int i = techs.size() - 1; i >= 0; i--) {
			Person tech = techs.get(i);

			if (warehouseMode && !tech.isRightTechTypeFor(partWork)) {
				continue;
			}

			Skill skill = tech.getSkillForWorkingOn(partWork);

			if (null == skill) {
				continue;
			}

			if (mro.getSkillMin() > skill.getExperienceLevel()) {
				continue;
			}

			if (mro.getSkillMax() < skill.getExperienceLevel()) {
				continue;
			}

			if (partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
				continue;
			}

			if (tech.getMinutesLeft() <= 0) {
				continue;
			}

			// Check if we can actually even repair this part
			TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);

			if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
					|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
				continue;
			}

			validTechs.add(tech);
		}

		return validTechs;
	}

	private static boolean checkArmorSupply(IPartWork part) {
		if (part.isSalvaging()) {
			return true;
		}

		if ((part instanceof Armor) && !((Armor) part).isInSupply()) {
			return false;
		}

		return true;
	}

	private static WorkTimeCalculation calculateNewMassRepairWorktime(IPartWork partWork, Person tech, MassRepairOption mro,
			Campaign campaign, boolean increaseTime, int highestAvailableTechSkill) {
		WorkTime newWorkTime = partWork.getMode();
		WorkTime previousNewWorkTime = newWorkTime;
		TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);
		Skill skill = tech.getSkillForWorkingOn(partWork);
		
		while (null != newWorkTime) {
			previousNewWorkTime = newWorkTime;
			newWorkTime = newWorkTime.moveTimeToNextLevel(increaseTime);

			//If we're trying to a rush a job, our effective skill goes down
			//Let's make sure we don't put it so high that we can't fix it anymore
			if (!increaseTime) {
				int modePenalty = partWork.getMode().expReduction;
				
				if (partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
					return new WorkTimeCalculation(previousNewWorkTime);
				}
			}
			
			if (null == newWorkTime) {
				if (!increaseTime) {
					return new WorkTimeCalculation(previousNewWorkTime);
				} else {
					WorkTimeCalculation wtc = new WorkTimeCalculation();

					if (skill.getExperienceLevel() >= highestAvailableTechSkill) {
						wtc.setReachedMaxSkill(true);
					}

					return wtc;
				}
			}

			if (partWork instanceof Part) {
				((Part) partWork).setMode(newWorkTime);
			}

			targetRoll = campaign.getTargetFor(partWork, tech);

			if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
					|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
				continue;
			}

			if (increaseTime) {
				if (targetRoll.getValue() <= mro.getBthMin()) {
					return new WorkTimeCalculation(newWorkTime);
				}
			} else {
				if (targetRoll.getValue() > mro.getBthMax()) {
					return new WorkTimeCalculation(previousNewWorkTime);
				}

				return new WorkTimeCalculation(newWorkTime);
			}
		}

		return new WorkTimeCalculation();
	}

	private static class WorkTimeCalculation {
		private WorkTime workTime;
		private boolean reachedMaxSkill = false;

		public WorkTimeCalculation() {

		}

		public WorkTimeCalculation(WorkTime workTime) {
			this.workTime = workTime;
		}

		public WorkTime getWorkTime() {
			return workTime;
		}

		public boolean isReachedMaxSkill() {
			return reachedMaxSkill;
		}

		public void setReachedMaxSkill(boolean reachedMaxSkill) {
			this.reachedMaxSkill = reachedMaxSkill;
		}
	}

	private static class TechSorter implements Comparator<Person> {
		private IPartWork partWork = null;

		public TechSorter(IPartWork _part) {
			this.partWork = _part;
		}

		@Override
		public int compare(Person tech1, Person tech2) {
			/*
			 * Sort the valid techs by applicable skill. Let's start with the
			 * least experienced and work our way up until we find someone who
			 * can perform the work. If we have two techs with the same skill,
			 * put the one with the lesser XP in the front.
			 */

			Skill skill1 = tech1.getSkillForWorkingOn(partWork);
			Skill skill2 = tech2.getSkillForWorkingOn(partWork);

			if (skill1.getExperienceLevel() == skill2.getExperienceLevel()) {
				if (tech1.getXp() == tech2.getXp()) {
					return 0;
				}

				return tech1.getXp() < tech2.getXp() ? -1 : 1;
			}

			return skill1.getExperienceLevel() < skill2.getExperienceLevel() ? -1 : 1;
		}
	}

	public static class MassRepairConfiguredOptions {
		private boolean useExtraTime;
		private boolean useRushJob;
		private boolean allowCarryover;
		private boolean optimizeToCompleteToday;
		private boolean useAssignedTechsFirst;
		private boolean scrapImpossible;
		private boolean replacePodParts;

		public boolean isUseExtraTime() {
			return useExtraTime;
		}

		public void setUseExtraTime(boolean useExtraTime) {
			this.useExtraTime = useExtraTime;
		}

		public boolean isUseRushJob() {
			return useRushJob;
		}

		public void setUseRushJob(boolean useRushJob) {
			this.useRushJob = useRushJob;
		}

		public boolean isAllowCarryover() {
			return allowCarryover;
		}

		public void setAllowCarryover(boolean allowCarryover) {
			this.allowCarryover = allowCarryover;
		}

		public boolean isOptimizeToCompleteToday() {
			return optimizeToCompleteToday;
		}

		public void setOptimizeToCompleteToday(boolean optimizeToCompleteToday) {
			this.optimizeToCompleteToday = optimizeToCompleteToday;
		}

		public boolean isUseAssignedTechsFirst() {
			return useAssignedTechsFirst;
		}

		public void setUseAssignedTechsFirst(boolean useAssignedTechsFirst) {
			this.useAssignedTechsFirst = useAssignedTechsFirst;
		}

		public boolean isScrapImpossible() {
			return scrapImpossible;
		}

		public void setScrapImpossible(boolean scrapImpossible) {
			this.scrapImpossible = scrapImpossible;
		}

		public boolean isReplacePodParts() {
			return replacePodParts;
		}

		public void setReplacePodParts(boolean replacePodParts) {
			this.replacePodParts = replacePodParts;
		}

		public void setup(CampaignOptions options) {
			setUseExtraTime(options.massRepairUseExtraTime());
			setUseRushJob(options.massRepairUseRushJob());
			setAllowCarryover(options.massRepairAllowCarryover());
			setOptimizeToCompleteToday(options.massRepairOptimizeToCompleteToday());
			setScrapImpossible(options.massRepairScrapImpossible());
			setUseAssignedTechsFirst(options.massRepairUseAssignedTechsFirst());
			setReplacePodParts(options.massRepairReplacePod());
		}

		public void setup(MassRepairSalvageDialog dlg) {
			setUseExtraTime(dlg.getUseExtraTimeBox().isSelected());
			setUseRushJob(dlg.getUseRushJobBox().isSelected());
			setAllowCarryover(dlg.getAllowCarryoverBox().isSelected());
			setOptimizeToCompleteToday(dlg.getOptimizeToCompleteTodayBox().isSelected());

			if (null != dlg.getScrapImpossibleBox()) {
				setScrapImpossible(dlg.getScrapImpossibleBox().isSelected());
			}

			if (null != dlg.getUseAssignedTechsFirstBox()) {
				setUseAssignedTechsFirst(dlg.getUseAssignedTechsFirstBox().isSelected());
			}

			if (null != dlg.getReplacePodPartsBox()) {
				setReplacePodParts(dlg.getReplacePodPartsBox().isSelected());
			}
		}
	}
}
