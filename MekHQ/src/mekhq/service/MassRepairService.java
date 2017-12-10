package mekhq.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
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
import mekhq.campaign.parts.equipment.AmmoBin;
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
				|| (unit.getEntity() instanceof Mech) || (unit.getEntity() instanceof BattleArmor);
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

	public static MassRepairPartSet performWarehouseMassRepair(List<IPartWork> selectedParts,
			List<MassRepairOption> mroList, MassRepairConfiguredOptions configuredOptions, CampaignGUI campaignGUI) {
		Campaign campaign = campaignGUI.getCampaign();

		campaign.addReport("Beginning mass warehouse repair.");

		List<Person> techs = campaign.getTechs(true);

		MassRepairPartSet partSet = new MassRepairPartSet();

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
			List<IPartWork> parts = filterParts(selectedParts, mroByTypeMap, techs, campaign);

			if (!parts.isEmpty()) {
				for (IPartWork partWork : parts) {
					Part part = (Part) partWork;
					part.resetModeToNormal();

					List<Person> validTechs = filterTechs(partWork, techs, mroByTypeMap, true, campaignGUI);

					if (validTechs.isEmpty()) {
						continue;
					}

					int originalQuantity = part.getQuantity();

					for (int i = 0; i < originalQuantity; i++) {
						partSet.addPartAction(
								repairPart(campaignGUI, part, null, validTechs, mroByTypeMap, configuredOptions, true));
					}
				}
			}
		}

		return partSet;
	}

	public static void performSingleUnitMassRepairOrSalvage(CampaignGUI campaignGUI, Unit unit) {
		CampaignOptions options = campaignGUI.getCampaign().getCampaignOptions();
		List<MassRepairOption> activeMROs = createActiveMROsFromConfiguration(campaignGUI);

		MassRepairConfiguredOptions configuredOptions = new MassRepairConfiguredOptions();
		configuredOptions.setup(options);

		MassRepairUnitAction unitAction = performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(),
				activeMROs, configuredOptions);

		String actionDescriptor = unit.isSalvage() ? "Salvage" : "Repair";
		String msg = String.format("<font color='green'>Mass %s complete on %s.</font>", actionDescriptor,
				unit.getName());

		switch (unitAction.getStatus()) {
		case ACTIONS_PERFORMED:
			int count = unitAction.getPartSet().countRepairs();
			msg += String.format(" There were %s action%s performed.", count, (count == 1 ? "" : "s"));
			break;

		case NO_PARTS:
			msg += " No actions were performed because there are currently no valid parts.";
			break;

		case ALL_PARTS_IN_PROCESS:
			msg += " No actions were performed because all parts are being worked on.";
			break;

		case NO_TECHS:
			msg += " No actions were performed because there are currently no valid techs.";
			break;

		case UNFIXABLE_LIMB:
			msg += " No actions were performed because there is at least one unfixable limb and configured settings do not allow location repairs.";
			break;

		case NO_ACTIONS:
		default:
			break;
		}

		campaignGUI.getCampaign().addReport(msg);

		List<Person> techs = campaignGUI.getCampaign().getTechs(false);

		if (!techs.isEmpty()) {
			List<IPartWork> parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId(), true);
			parts = filterParts(parts, null, techs, campaignGUI.getCampaign());

			if (!parts.isEmpty()) {
				if (parts.size() == 1) {
					campaignGUI.getCampaign()
							.addReport("<font color='red'>There in still 1 part that in not being worked on.</font>");
				} else {
					campaignGUI.getCampaign()
							.addReport(String.format(
									"<font color='red'>There are still %s parts that are not being worked on.</font>",
									parts.size()));
				}
			}
		}

		JOptionPane.showMessageDialog(campaignGUI.getFrame(),
				String.format("Mass %s complete on %s.", actionDescriptor, unit.getName()), "Complete",
				JOptionPane.INFORMATION_MESSAGE);
	}

	public static void massRepairSalvageAllUnits(CampaignGUI campaignGUI) {
		List<Unit> units = new ArrayList<Unit>();

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

		massRepairSalvageUnits(campaignGUI, units);
	}

	public static void massRepairSalvageUnits(CampaignGUI campaignGUI, List<Unit> units) {
		CampaignOptions options = campaignGUI.getCampaign().getCampaignOptions();
		List<MassRepairOption> activeMROs = createActiveMROsFromConfiguration(campaignGUI);

		Map<MassRepairUnitAction.STATUS, List<MassRepairUnitAction>> unitActionsByStatus = new HashMap<MassRepairUnitAction.STATUS, List<MassRepairUnitAction>>();

		MassRepairConfiguredOptions configuredOptions = new MassRepairConfiguredOptions();
		configuredOptions.setup(options);

		for (Unit unit : units) {
			MassRepairUnitAction unitAction = performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(),
					activeMROs, configuredOptions);

			List<MassRepairUnitAction> list = unitActionsByStatus.get(unitAction.getStatus());

			if (null == list) {
				list = new ArrayList<MassRepairUnitAction>();
				unitActionsByStatus.put(unitAction.getStatus(), list);
			}

			list.add(unitAction);
		}

		if (unitActionsByStatus.isEmpty()) {
			campaignGUI.getCampaign().addReport("Mass Repair/Salvage complete. There were no units worked on.");
		} else {
			int totalCount = 0;
			int actionsPerformed = 0;

			for (MassRepairUnitAction.STATUS key : unitActionsByStatus.keySet()) {
				if (key == MassRepairUnitAction.STATUS.ALL_PARTS_IN_PROCESS) {
					continue;
				}

				totalCount += unitActionsByStatus.get(key).size();
			}

			if (unitActionsByStatus.containsKey(MassRepairUnitAction.STATUS.ACTIONS_PERFORMED)) {
				List<MassRepairUnitAction> unitsByStatus = unitActionsByStatus
						.get(MassRepairUnitAction.STATUS.ACTIONS_PERFORMED);

				for (MassRepairUnitAction mrua : unitsByStatus) {
					actionsPerformed += mrua.getPartSet().countRepairs();
				}
			}

			StringBuilder sb = new StringBuilder(
					String.format("<font color='green'>Mass Repair/Salvage complete for %s units.</font>", totalCount));

			if (actionsPerformed > 0) {
				sb.append(String.format(" %s repair/salvage action%s performed.", actionsPerformed,
						(actionsPerformed == 1 ? "" : "s")));
			}

			sb.append(generateUnitRepairSummary("<br/>- %s unit%s had repairs/parts salvaged.", unitActionsByStatus,
					MassRepairUnitAction.STATUS.ACTIONS_PERFORMED));
			sb.append(generateUnitRepairSummary(
					"<br/>- %s unit%s had no actions performed because there were no valid parts.", unitActionsByStatus,
					MassRepairUnitAction.STATUS.NO_PARTS));
			sb.append(generateUnitRepairSummary(
					"<br/>- %s unit%s had no actions performed because there were no valid techs.", unitActionsByStatus,
					MassRepairUnitAction.STATUS.NO_TECHS));
			sb.append(generateUnitRepairSummary(
					"<br/>- %s unit%s had no actions performed because there were unfixable limbs and configured settings do not allow location repairs.",
					unitActionsByStatus, MassRepairUnitAction.STATUS.UNFIXABLE_LIMB));

			campaignGUI.getCampaign().addReport(sb.toString());
		}

		generateCampaignLogForUnitStatus(unitActionsByStatus, MassRepairUnitAction.STATUS.NO_PARTS,
				"Units with no valid parts:", campaignGUI);
		generateCampaignLogForUnitStatus(unitActionsByStatus, MassRepairUnitAction.STATUS.NO_TECHS,
				"Units with no valid techs:", campaignGUI);
		generateCampaignLogForUnitStatus(unitActionsByStatus, MassRepairUnitAction.STATUS.UNFIXABLE_LIMB,
				"Units with unfixable limbs:", campaignGUI);

		if (!unitActionsByStatus.isEmpty()) {
			List<Person> techs = campaignGUI.getCampaign().getTechs(false);

			if (!techs.isEmpty()) {
				int count = 0;
				int unitCount = 0;

				for (List<MassRepairUnitAction> list : unitActionsByStatus.values()) {
					for (MassRepairUnitAction mrua : list) {
						List<IPartWork> parts = campaignGUI.getCampaign()
								.getPartsNeedingServiceFor(mrua.getUnit().getId(), true);
						int tempCount = filterParts(parts, null, techs, campaignGUI.getCampaign()).size();

						if (tempCount > 0) {
							unitCount++;
							count += tempCount;
						}
					}
				}

				if (count > 0) {
					if (count == 1) {
						campaignGUI.getCampaign().addReport(
								"<font color='red'>There in still 1 part that in not being worked on.</font>");
					} else {
						campaignGUI.getCampaign()
								.addReport(String.format(
										"<font color='red'>There are still %s parts that are not being worked on %s unit%s.</font>",
										count, unitCount, (unitCount == 1 ? "" : "s")));
					}
				}
			}
		}

		JOptionPane.showMessageDialog(campaignGUI.getFrame(), "Mass Repair/Salvage complete.", "Complete",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private static String generateUnitRepairSummary(String baseDescription,
			Map<MassRepairUnitAction.STATUS, List<MassRepairUnitAction>> unitActionsByStatus,
			MassRepairUnitAction.STATUS status) {

		if (!unitActionsByStatus.containsKey(status)) {
			return "";
		}

		int count = unitActionsByStatus.get(status).size();

		return String.format(baseDescription, count, count == 1 ? "" : "s");
	}

	private static void generateCampaignLogForUnitStatus(
			Map<MassRepairUnitAction.STATUS, List<MassRepairUnitAction>> unitActionsByStatus,
			MassRepairUnitAction.STATUS status, String statusDesc, CampaignGUI campaignGUI) {
		if (!unitActionsByStatus.containsKey(status) || unitActionsByStatus.get(status).isEmpty()) {
			return;
		}

		StringBuilder sbMsg = new StringBuilder();
		sbMsg.append(statusDesc);

		List<MassRepairUnitAction> unitsByStatus = unitActionsByStatus.get(status);

		for (MassRepairUnitAction mrua : unitsByStatus) {
			sbMsg.append("<br/>- " + mrua.getUnit().getName());
		}

		campaignGUI.getCampaign().addReport(sbMsg.toString());
	}

	public static MassRepairUnitAction performUnitMassRepairOrSalvage(CampaignGUI campaignGUI, Unit unit,
			boolean isSalvage, List<MassRepairOption> mroList, MassRepairConfiguredOptions configuredOptions) {
		Campaign campaign = campaignGUI.getCampaign();

		List<Person> techs = campaign.getTechs(true);

		if (techs.isEmpty()) {
			return new MassRepairUnitAction(unit, isSalvage, MassRepairUnitAction.STATUS.NO_TECHS);
		}

		MassRepairUnitAction unitAction = new MassRepairUnitAction(unit, isSalvage,
				MassRepairUnitAction.STATUS.NO_ACTIONS);

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
		 * Possibly call this multiple times. Sometimes some actions are first
		 * dependent upon others being finished, also failed actions can be
		 * performed again by a tech with a higher skill.
		 */
		boolean performMoreRepairs = true;

		long time = System.nanoTime();

		while (performMoreRepairs) {
			MassRepairUnitAction currentUnitAction = performUnitMassTechAction(campaignGUI, unit, techs, mroByTypeMap,
					isSalvage, configuredOptions);

			performMoreRepairs = currentUnitAction.getPartSet().isHasRepairs();
			unitAction.merge(currentUnitAction);

			if (unitAction.isStatusNoActions()) {
				unitAction.setStatus(currentUnitAction.getStatus());
			}
		}

		debugLog("Finished fixing %s in %s ns", "performUnitMassRepairOrSalvage", unit.getName(), System.nanoTime() - time);

		return unitAction;
	}

	private static MassRepairUnitAction performUnitMassTechAction(CampaignGUI campaignGUI, Unit unit,
			List<Person> techs, Map<Integer, MassRepairOption> mroByTypeMap, boolean salvaging,
			MassRepairConfiguredOptions configuredOptions) {
		Campaign campaign = campaignGUI.getCampaign();

		List<IPartWork> parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId(), true);

		if (parts.isEmpty()) {
			parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId(), false);

			if (!parts.isEmpty()) {
				return new MassRepairUnitAction(unit, salvaging, MassRepairUnitAction.STATUS.ALL_PARTS_IN_PROCESS);
			}
			
			return new MassRepairUnitAction(unit, salvaging, MassRepairUnitAction.STATUS.NO_PARTS);
		}
		
		for (IPartWork partWork : parts) {
			if (partWork instanceof Part) {
				Part part = (Part) partWork;
				part.resetModeToNormal();
			}
		}

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
				parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId(), true);
			}
		}

		if (unit.getEntity().isOmni() && !unit.isSalvage()) {
			for (PodSpace ps : unit.getPodSpace()) {
				ps.setRepairInPlace(!configuredOptions.isReplacePodParts());
			}

			// If we're replacing damaged parts, we want to remove any that have
			// an available replacement
			// from the list since the pod space repair will cover it.

			List<IPartWork> temp = new ArrayList<>();

			for (IPartWork p : parts) {
				if ((p instanceof Part) && ((Part) p).isOmniPodded()) {
					if (!(p instanceof AmmoBin) || ((p instanceof AmmoBin) && salvaging)) {
						MissingPart m = ((Part) p).getMissingPart();
	
						if (m != null && m.isReplacementAvailable()) {
							continue;
						}
					}
				}

				temp.add(p);
			}

			parts = temp;
		}

		if (techs.isEmpty()) {
			return new MassRepairUnitAction(unit, salvaging, MassRepairUnitAction.STATUS.NO_TECHS);
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
					return new MassRepairUnitAction(unit, salvaging, MassRepairUnitAction.STATUS.UNFIXABLE_LIMB);
				}

				/*
				 * Find our parts in our bad locations. If we don't actually
				 * have, just scrap the limbs and move on with our normal work
				 */

				scrappingLimbMode = true;

				if (!salvaging) {
					unit.setSalvage(true);
				}

				List<IPartWork> partsTemp = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId(), true);
				List<IPartWork> partsToBeRemoved = new ArrayList<IPartWork>();
				Map<Integer, Integer> countOfPartsPerLocation = new HashMap<Integer, Integer>();

				for (IPartWork partWork : partsTemp) {
					if (!(partWork instanceof MekLocation) && !(partWork instanceof MissingMekLocation)
							&& locationMap.containsKey(partWork.getLocation()) && partWork.isSalvaging()) {
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

					parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId(), true);
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
									"<font color='orange'>Found an unfixable limb (%s) on %s which contains %s parts. Going to remove all parts and scrap the limb before proceeding with other repairs.</font>",
									loc.getName(), unit.getName(), countOfPartsPerLocation.get(locId)));
						} else {
							campaign.addReport(String.format(
									"<font color='orange'>Found missing location (%s) on %s which contains %s parts. Going to remove all parts before proceeding with other repairs.</font>",
									loc.getName(), unit.getName(), countOfPartsPerLocation.get(locId)));
						}
					}

					parts = partsToBeRemoved;
				}
			}
		}

		boolean originalAllowCarryover = configuredOptions.isAllowCarryover();

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
		parts = filterParts(parts, mroByTypeMap, techs, campaignGUI.getCampaign());

		if (parts.isEmpty()) {
			if (scrappingLimbMode) {
				unit.setSalvage(false);
			}

			return new MassRepairUnitAction(unit, salvaging, MassRepairUnitAction.STATUS.NO_PARTS);
		}

		MassRepairUnitAction unitAction = new MassRepairUnitAction(unit, salvaging,
				MassRepairUnitAction.STATUS.ACTIONS_PERFORMED);

		for (IPartWork partWork : parts) {
			if (partWork instanceof Part) {
				((Part) partWork).resetModeToNormal();
			}

			List<Person> validTechs = filterTechs(partWork, techs, mroByTypeMap, false, campaignGUI);

			if (validTechs.isEmpty()) {
				MassRepairPartAction mrpa = MassRepairPartAction.createNoTechs(partWork);
				unitAction.addPartAction(mrpa);
				continue;
			}

			MassRepairPartAction mrpa = repairPart(campaignGUI, partWork, unit, validTechs, mroByTypeMap,
					configuredOptions, false);
			unitAction.addPartAction(mrpa);
		}

		if (scrappingLimbMode) {
			unit.setSalvage(false);
			configuredOptions.setAllowCarryover(originalAllowCarryover);
		}

		if (unitAction.getPartSet().isOnlyNoTechs()) {
			unitAction.resetPartSet();
			unitAction.setStatus(MassRepairUnitAction.STATUS.NO_TECHS);
		}

		return unitAction;
	}

	private static MassRepairPartAction repairPart(CampaignGUI campaignGUI, IPartWork partWork, Unit unit,
			List<Person> techs, Map<Integer, MassRepairOption> mroByTypeMap,
			MassRepairConfiguredOptions configuredOptions, boolean warehouseMode) {

		// We were doing this check for every tech, that's unnecessary as it
		// doesn't change from tech to tech
		MassRepairOption mro = mroByTypeMap.get(IPartWork.findCorrectMassRepairType(partWork));

		if (null == mro) {
			return MassRepairPartAction.createOptionDisabled(partWork);
		}

		long repairPartTime = System.nanoTime();

		Campaign campaign = campaignGUI.getCampaign();
		TechSorter sorter = new TechSorter(partWork);
		Map<String, WorkTime> techSkillToWorktimeMap = new HashMap<String, WorkTime>();
		List<Person> sameDayTechs = new ArrayList<Person>();
		List<Person> overflowDayTechs = new ArrayList<Person>();
		List<Person> sameDayAssignedTechs = new ArrayList<Person>();
		List<Person> overflowDayAssignedTechs = new ArrayList<Person>();
		int highestAvailableTechSkill = -1;

		for (Person tech : techs) {
			Skill skill = tech.getSkillForWorkingOn(partWork);

			if (skill.getExperienceLevel() > highestAvailableTechSkill) {
				highestAvailableTechSkill = skill.getExperienceLevel();
			}

			if (highestAvailableTechSkill == SkillType.EXP_ELITE) {
				break;
			}
		}

		debugLog("Starting with %s techs on %s", "repairPart", techs.size(), partWork.getPartName());

		boolean canChangeWorkTime = (partWork instanceof Part) && ((Part) partWork).canChangeWorkMode();

		for (int i = techs.size() - 1; i >= 0; i--) {
			long time = System.nanoTime();

			Person tech = techs.get(i);

			debugLog("Checking tech %s", "repairPart", tech.getName());

			Skill skill = tech.getSkillForWorkingOn(partWork);

			// We really only have to check one tech of each skill level
			if (!techSkillToWorktimeMap.containsKey(skill.getType().getName() + "-" + skill.getLevel())) {
				TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);
				WorkTime selectedWorktime = null;

				// Check if we need to increase the time to meet the min BTH
				if (targetRoll.getValue() > mro.getBthMin()) {
					if (!configuredOptions.isUseExtraTime()) {
						debugLog("... can't increase time to reach BTH due to configuration", "repairPart");
						continue;
					} else if (!canChangeWorkTime) {
						debugLog("... can't increase time because this part can not have it's workMode changed", "repairPart");
						continue;
					}

					WorkTimeCalculation workTimeCalc = calculateNewMassRepairWorktime(partWork, tech, mro, campaign,
							true, highestAvailableTechSkill);

					if (null == workTimeCalc.getWorkTime()) {
						if (workTimeCalc.isReachedMaxSkill()) {
							debugLog("... can't increase time enough to reach BTH with max available tech", "repairPart");

							return MassRepairPartAction.createMaxSkillReached(partWork, highestAvailableTechSkill,
									mro.getBthMin());
						} else {
							debugLog("... can't increase time enough to reach BTH", "repairPart");

							continue;
						}
					}

					selectedWorktime = workTimeCalc.getWorkTime();
				} else if (targetRoll.getValue() < mro.getBthMax()) {
					// Or decrease the time to meet the max BTH
					if (configuredOptions.isUseRushJob() && canChangeWorkTime) {
						WorkTimeCalculation workTimeCalc = calculateNewMassRepairWorktime(partWork, tech, mro, campaign,
								false, highestAvailableTechSkill);

						if (null == workTimeCalc.getWorkTime()) {
							selectedWorktime = WorkTime.NORMAL;
						} else {
							selectedWorktime = workTimeCalc.getWorkTime();
						}
					}
				}

				techSkillToWorktimeMap.put(skill.getType().getName() + "-" + skill.getLevel(), selectedWorktime);

				if (partWork instanceof Part) {
					((Part) partWork).resetModeToNormal();
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
					debugLog("... would carry over day and configuration doesn't allow", "repairPart");

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

			debugLog("... time to check tech: %s ns", "repairPart", (System.nanoTime() - time));
		}

		List<Person> validTechs = new ArrayList<Person>();

		if (!sameDayAssignedTechs.isEmpty()) {
			Collections.sort(sameDayAssignedTechs, sorter);
			validTechs.addAll(sameDayAssignedTechs);
		}

		if (!sameDayTechs.isEmpty()) {
			Collections.sort(sameDayTechs, sorter);
			validTechs.addAll(sameDayTechs);
		}

		if (!overflowDayAssignedTechs.isEmpty()) {
			Collections.sort(overflowDayAssignedTechs, sorter);
			validTechs.addAll(overflowDayAssignedTechs);
		}

		if (!overflowDayTechs.isEmpty()) {
			Collections.sort(overflowDayTechs, sorter);
			validTechs.addAll(overflowDayTechs);
		}

		if (validTechs.isEmpty()) {
			debugLog("Ending because there are no techs", "repairPart");

			return MassRepairPartAction.createNoTechs(partWork);
		}

		Person tech = validTechs.get(0);

		if (partWork instanceof Part) {
			Skill skill = tech.getSkillForWorkingOn(partWork);
			WorkTime wt = techSkillToWorktimeMap.get(skill.getType().getName() + "-" + skill.getLevel());

			if (null == wt) {
				debugLog("[ERROR] Null work-time from techToWorktimeMap for %s", "repairPart", tech.getName());
				wt = WorkTime.NORMAL;
			}

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

		debugLog("Ending after %s ns", "repairPart", System.nanoTime() - repairPartTime);

		return MassRepairPartAction.createRepaired(partWork);
	}

	private static List<IPartWork> filterParts(List<IPartWork> parts, Map<Integer, MassRepairOption> mroByTypeMap,
			List<Person> techs, Campaign campaign) {
		List<IPartWork> newParts = new ArrayList<IPartWork>();

		if (techs.isEmpty() || parts.isEmpty()) {
			return newParts;
		}

		Map<String, Person> techCache = new HashMap<String, Person>();

		for (IPartWork partWork : parts) {
			if (partWork.isBeingWorkedOn()) {
				continue;
			}

			if (partWork instanceof MissingPart && !((MissingPart) partWork).isReplacementAvailable()) {
				continue;
			}

			if (null != mroByTypeMap) {
				int repairType = IPartWork.findCorrectMassRepairType(partWork);

				MassRepairOption mro = mroByTypeMap.get(repairType);

				if ((null == mro) || !mro.isActive()) {
					continue;
				}
			}

			if (!checkArmorSupply(partWork)) {
				continue;
			}

			// See if this part is blocked or can be dealt with
			// Find an appropriate tech and get their skill then create an
			// elite tech with the same skill
			Skill partSkill = null;

			for (Person techExisting : techs) {
				partSkill = techExisting.getSkillForWorkingOn(partWork);

				if (null != partSkill) {
					break;
				}
			}

			if (null == partSkill) {
				continue;
			}

			String skillName = partSkill.getType().getName();

			// Find a tech in our placeholder cache
			Person tech = techCache.get(skillName);

			if (null == tech) {
				// Create a dummy elite tech with the proper skill and 1
				// minute and put it in our cache for later use

				tech = new Person(String.format("Temp Tech (%s)", skillName), campaign);
				tech.addSkill(skillName, partSkill.getType().getEliteLevel(), 1);
				tech.setMinutesLeft(1);

				techCache.put(skillName, tech);
			}

			TargetRoll roll = campaign.getTargetFor(partWork, tech);

			if ((roll.getValue() == TargetRoll.IMPOSSIBLE) || (roll.getValue() == TargetRoll.AUTOMATIC_FAIL)
					|| (roll.getValue() == TargetRoll.CHECK_FALSE)) {
				continue;
			}

			newParts.add(partWork);
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

		Campaign campaign = campaignGUI.getCampaign();

		for (int i = techs.size() - 1; i >= 0; i--) {
			Person tech = techs.get(i);

			if (tech.getMinutesLeft() <= 0) {
				continue;
			}

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

			if (partWork.getSkillMin() > skill.getExperienceLevel()) {
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

	private static WorkTimeCalculation calculateNewMassRepairWorktime(IPartWork partWork, Person tech,
			MassRepairOption mro, Campaign campaign, boolean increaseTime, int highestAvailableTechSkill) {
		long time = System.nanoTime();

		debugLog("...... starting calculateNewMassRepairWorktime", "calculateNewMassRepairWorktime");

		if (partWork instanceof Part) {
			((Part) partWork).resetModeToNormal();
		}

		TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);

		if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
				|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
			debugLog("...... ending calculateNewMassRepairWorktime due to impossible role - %s ns", "calculateNewMassRepairWorktime",
					System.nanoTime() - time);

			return new WorkTimeCalculation();
		}

		WorkTime newWorkTime = partWork.getMode();
		WorkTime previousNewWorkTime = newWorkTime;

		Skill skill = tech.getSkillForWorkingOn(partWork);

		while (null != newWorkTime) {
			previousNewWorkTime = newWorkTime;
			newWorkTime = newWorkTime.moveTimeToNextLevel(increaseTime);

			debugLog("...... looping workTime check. NewWorkTime: %s, PreviousWorkTime: %s", "calculateNewMassRepairWorktime",
					(null == newWorkTime ? "NULL" : newWorkTime.name()), previousNewWorkTime.name());

			// If we're trying to a rush a job, our effective skill goes down
			// Let's make sure we don't put it so high that we can't fix it
			// anymore
			if (!increaseTime) {
				int modePenalty = partWork.getMode().expReduction;

				if (partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
					debugLog(
							"...... ending calculateNewMassRepairWorktime with previousWorkTime due time reduction skill mod now being less that required skill - %s ns", "calculateNewMassRepairWorktime",
							System.nanoTime() - time);

					return new WorkTimeCalculation(previousNewWorkTime);
				}
			}

			// If we have a null newWorkTime, we're done. Use the previous one.
			if (null == newWorkTime) {
				debugLog("...... ending calculateNewMassRepairWorktime because newWorkTime is null - %s ns", "calculateNewMassRepairWorktime",
						System.nanoTime() - time);

				if (!increaseTime) {
					return new WorkTimeCalculation(previousNewWorkTime);
				}

				WorkTimeCalculation wtc = new WorkTimeCalculation();

				if (skill.getExperienceLevel() >= highestAvailableTechSkill) {
					wtc.setReachedMaxSkill(true);
				}

				return wtc;
			}

			// Set our new workTime and calculate the new targetRoll
			if (partWork instanceof Part) {
				((Part) partWork).setMode(newWorkTime);
			}

			targetRoll = campaign.getTargetFor(partWork, tech);

			// If our roll is impossible, revert to the previous one
			if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
					|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
				debugLog("...... ending calculateNewMassRepairWorktime due to impossible role - %s ns", "calculateNewMassRepairWorktime",
						System.nanoTime() - time);

				return new WorkTimeCalculation(previousNewWorkTime);
			}

			if (increaseTime) {
				// If we've reached our BTH, kick out. Otherwise we'll loop
				// around again
				if (targetRoll.getValue() <= mro.getBthMin()) {
					debugLog(
							"...... ending calculateNewMassRepairWorktime because we have reached our BTH goal - %s ns", "calculateNewMassRepairWorktime",
							System.nanoTime() - time);

					return new WorkTimeCalculation(newWorkTime);
				}
			} else {
				if (targetRoll.getValue() > mro.getBthMax()) {
					debugLog(
							"...... ending calculateNewMassRepairWorktime because we have reached our BTH goal - %s ns", "calculateNewMassRepairWorktime",
							System.nanoTime() - time);

					return new WorkTimeCalculation(previousNewWorkTime);
				} else if (targetRoll.getValue() > mro.getBthMax()) {
					debugLog(
							"...... ending calculateNewMassRepairWorktime because we have reached our BTH goal - %s ns", "calculateNewMassRepairWorktime",
							System.nanoTime() - time);

					return new WorkTimeCalculation(newWorkTime);
				}
			}
		}

		return new WorkTimeCalculation();
	}

	private static void debugLog(String msg, String methodName, Object... replacements) {
		if ((null != replacements) && (replacements.length > 0)) {
			msg = String.format(msg, replacements);
		}

		MekHQ.getLogger().log(MassRepairService.class, methodName, LogLevel.DEBUG, msg);
	}

	private static class WorkTimeCalculation {
		private WorkTime workTime = WorkTime.NORMAL;
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
			 * put the one with the lesser XP in the front. If we have techs
			 * with the same XP, put the one with the more time ahead.
			 */

			Skill skill1 = tech1.getSkillForWorkingOn(partWork);
			Skill skill2 = tech2.getSkillForWorkingOn(partWork);

			if (skill1.getExperienceLevel() == skill2.getExperienceLevel()) {
				if ((tech1.getXp() == tech2.getXp()) || (skill1.getLevel() == SkillType.EXP_ELITE)) {
					return tech1.getMinutesLeft() - tech2.getMinutesLeft();
				}

				return tech1.getXp() < tech2.getXp() ? -1 : 1;
			}

			return skill1.getExperienceLevel() < skill2.getExperienceLevel() ? -1 : 1;
		}
	}

	public static class MassRepairPartAction {
		public enum STATUS {
			REPAIRED, MAX_SKILL_REACHED, MRO_DISABLED, NO_TECHS
		}

		private IPartWork partWork;
		private STATUS status;
		private int maxTechSkill;
		private int configuredBTHMin;

		public MassRepairPartAction() {

		}

		public MassRepairPartAction(IPartWork partWork) {
			this.partWork = partWork;
		}

		public MassRepairPartAction(IPartWork partWork, STATUS status) {
			this(partWork);

			this.status = status;
		}

		public IPartWork getPartWork() {
			return partWork;
		}

		public void setPartWork(IPartWork partWork) {
			this.partWork = partWork;
		}

		public STATUS getStatus() {
			return status;
		}

		public void setStatus(STATUS status) {
			this.status = status;
		}

		public boolean isStatusRepaired() {
			return status == STATUS.REPAIRED;
		}

		public boolean isStatusMaxSkillReached() {
			return status == STATUS.MAX_SKILL_REACHED;
		}

		public boolean isStatusOptionDisabled() {
			return status == STATUS.MRO_DISABLED;
		}

		public boolean isStatusNoTechs() {
			return status == STATUS.NO_TECHS;
		}

		public int getMaxTechSkill() {
			return maxTechSkill;
		}

		public void setMaxTechSkill(int maxTechSkill) {
			this.maxTechSkill = maxTechSkill;
		}

		public int getConfiguredBTHMin() {
			return configuredBTHMin;
		}

		public void setConfiguredBTHMin(int configuredBTHMin) {
			this.configuredBTHMin = configuredBTHMin;
		}

		public static MassRepairPartAction createRepaired(IPartWork partWork) {
			return new MassRepairPartAction(partWork, STATUS.REPAIRED);
		}

		public static MassRepairPartAction createMaxSkillReached(IPartWork partWork, int maxSkill, int bthMin) {
			MassRepairPartAction mrpa = new MassRepairPartAction(partWork, STATUS.MAX_SKILL_REACHED);
			mrpa.setMaxTechSkill(maxSkill);
			mrpa.setConfiguredBTHMin(bthMin);

			return mrpa;
		}

		public static MassRepairPartAction createOptionDisabled(IPartWork partWork) {
			return new MassRepairPartAction(partWork, STATUS.MRO_DISABLED);
		}

		public static MassRepairPartAction createNoTechs(IPartWork partWork) {
			return new MassRepairPartAction(partWork, STATUS.NO_TECHS);
		}
	}

	public static class MassRepairPartSet {
		private Map<MassRepairPartAction.STATUS, List<MassRepairPartAction>> partActionsByStatus = new HashMap<MassRepairPartAction.STATUS, List<MassRepairPartAction>>();

		public void addPartAction(MassRepairPartAction partAction) {
			if (null == partAction) {
				return;
			}

			List<MassRepairPartAction> list = partActionsByStatus.get(partAction.getStatus());

			if (null == list) {
				list = new ArrayList<MassRepairPartAction>();
				partActionsByStatus.put(partAction.getStatus(), list);
			}

			list.add(partAction);
		}

		public Map<MassRepairPartAction.STATUS, List<MassRepairPartAction>> getPartActions() {
			return partActionsByStatus;
		}

		public boolean isHasRepairs() {
			return partActionsByStatus.containsKey(MassRepairPartAction.STATUS.REPAIRED);
		}

		public int countRepairs() {
			if (!isHasRepairs()) {
				return 0;
			}

			return partActionsByStatus.get(MassRepairPartAction.STATUS.REPAIRED).size();
		}

		public boolean isOnlyNoTechs() {
			if (!partActionsByStatus.containsKey(MassRepairPartAction.STATUS.NO_TECHS)) {
				return false;
			}

			if (partActionsByStatus.size() > 1) {
				return false;
			}

			return true;
		}
	}

	public static class MassRepairUnitAction {
		public enum STATUS {
			NO_ACTIONS, ACTIONS_PERFORMED, NO_TECHS, UNFIXABLE_LIMB, NO_PARTS, ALL_PARTS_IN_PROCESS
		}

		private Unit unit;
		private MassRepairPartSet partSet = new MassRepairPartSet();
		private STATUS status;
		private boolean salvaging;

		public MassRepairUnitAction() {

		}

		public MassRepairUnitAction(Unit unit, boolean salvaging, STATUS status) {
			this.unit = unit;
			this.salvaging = salvaging;
			this.status = status;
		}

		public Unit getUnit() {
			return unit;
		}

		public void setUnit(Unit unit) {
			this.unit = unit;
		}

		public MassRepairPartSet getPartSet() {
			return partSet;
		}

		public void setPartSet(MassRepairPartSet partSet) {
			this.partSet = partSet;
		}

		public STATUS getStatus() {
			return status;
		}

		public void setStatus(STATUS status) {
			this.status = status;
		}

		public boolean isSalvaging() {
			return salvaging;
		}

		public void setSalvaging(boolean salvaging) {
			this.salvaging = salvaging;
		}

		public boolean isStatusNoActions() {
			return status == STATUS.NO_ACTIONS;
		}

		public boolean isStatusActionsPerformed() {
			return status == STATUS.ACTIONS_PERFORMED;
		}

		public boolean isStatusNoTechs() {
			return status == STATUS.NO_TECHS;
		}

		public boolean isStatusUnfixableLimb() {
			return status == STATUS.UNFIXABLE_LIMB;
		}

		public boolean isStatusNoParts() {
			return status == STATUS.NO_PARTS;
		}

		public void addPartAction(MassRepairPartAction partAction) {
			partSet.addPartAction(partAction);
		}

		public void resetPartSet() {
			partSet = new MassRepairPartSet();
		}

		public void merge(MassRepairUnitAction currentUnitAction) {
			for (List<MassRepairPartAction> partActionList : currentUnitAction.getPartSet().getPartActions().values()) {
				for (MassRepairPartAction partAction : partActionList) {
					getPartSet().addPartAction(partAction);
				}
			}
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
