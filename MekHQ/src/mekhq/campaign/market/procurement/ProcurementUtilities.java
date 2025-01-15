package mekhq.campaign.market.procurement;

import megamek.common.EquipmentType;
import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.work.IAcquisitionWork;

import java.util.List;

public class ProcurementUtilities {
    /**
     * Finds the best-suited character to handle acquisitions in the current campaign.
     *
     * <p>This method evaluates the active personnel in the campaign and selects a character
     * based on the acquisition skill set in the campaign options. If a suitable character with
     * the highest skill level is found, they are returned. Otherwise, {@code null} is returned.
     * The selection process considers optional constraints such as support staff restriction
     * and a limit on the maximum number of acquisitions a character can perform.</p>
     *
     * @param campaign the {@link Campaign} instance containing the campaign data, including
     *                 active personnel and configuration options.
     * @return the {@link Person} best suited for acquisitions, or {@code null} if no suitable person is found
     *         or if auto-acquisition is enabled ({@link CampaignOptions#S_AUTO}).
     */
    public static @Nullable Person getAcquisitionsCharacter(Campaign campaign) {
        final CampaignOptions options = campaign.getCampaignOptions();
        final List<Person> activePersonnel = campaign.getActivePersonnel();

        // Return early if S_AUTO is selected (automatic acquisition)
        String acquisitionSkill = options.getAcquisitionSkill();
        if (CampaignOptions.S_AUTO.equals(acquisitionSkill)) {
            return null;
        }

        int bestSkill = -1;
        int maxAcquisitions = options.getMaxAcquisitions();
        Person acquisitionsCharacter = null;

        // Iterate over active personnel to find the best suited character
        for (Person person : activePersonnel) {
            // Skip if only support staff are allowed but the person is not support staff
            if (options.isAcquisitionSupportStaffOnly() && !person.hasSupportRole(true)) {
                continue;
            }

            // Skip if the person has already reached the maximum acquisitions limit
            if (maxAcquisitions > 0 && person.getAcquisitions() >= maxAcquisitions) {
                continue;
            }

            // Determine skill level based on the acquisition skill type
            int skillLevel = 0;
            if (CampaignOptions.S_TECH.equals(acquisitionSkill)) {
                if (person.getBestTechSkill() != null) {
                    skillLevel = person.getBestTechSkill().getLevel();
                }
            } else if (person.hasSkill(acquisitionSkill)) {
                skillLevel = person.getSkill(acquisitionSkill).getLevel();
            }

            // Update if this person has the highest skill level seen so far
            if (skillLevel > bestSkill) {
                acquisitionsCharacter = person;
                bestSkill = skillLevel;
            }
        }

        return acquisitionsCharacter;
    }



    public static TargetRoll getTargetForAcquisition(final Campaign campaign, final IAcquisitionWork acquisition) {
        return getTargetForAcquisition(campaign, acquisition, getAcquisitionsCharacter(campaign));
    }

    public static TargetRoll getTargetForAcquisition(final Campaign campaign, final IAcquisitionWork acquisition,
                                              final @Nullable Person person) {
        return getTargetForAcquisition(campaign, acquisition, person, false);
    }

    public static TargetRoll getTargetForAcquisition(final Campaign campaign, final IAcquisitionWork acquisition,
                                              final @Nullable Person person, final boolean checkDaysToWait) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final int currentYear = campaign.getLocalDate().getYear();
        final ShoppingList shoppingList = campaign.getShoppingList();
        final boolean useClanTechBase = campaign.useClanTechBase();
        final int techFaction = campaign.getTechFaction();
        final Faction faction = campaign.getFaction();

        if (campaignOptions.getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "Automatic Success");
        }

        if (null == person) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                "No one on your force is capable of acquiring parts");
        }
        final Skill skill = person.getSkillForWorkingOn(campaignOptions.getAcquisitionSkill());
        if (null != shoppingList.getShoppingItem(
            acquisition.getNewEquipment())
            && checkDaysToWait) {
            return new TargetRoll(
                TargetRoll.AUTOMATIC_FAIL,
                "You must wait until the new cycle to check for this part. Further attempts will be added to the shopping list.");
        }
        if (acquisition.getTechBase() == Part.T_CLAN
            && !campaignOptions.isAllowClanPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                "You cannot acquire clan parts");
        }
        if (acquisition.getTechBase() == Part.T_IS
            && !campaignOptions.isAllowISPurchases()) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                "You cannot acquire inner sphere parts");
        }
        if (campaignOptions.getTechLevel() < Utilities
            .getSimpleTechLevel(acquisition.getTechLevel())) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                "You cannot acquire parts of this tech level");
        }
        if (campaignOptions.isLimitByYear()
            && !acquisition.isIntroducedBy(currentYear, useClanTechBase, techFaction)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                "It has not been invented yet!");
        }
        if (campaignOptions.isDisallowExtinctStuff() &&
            (acquisition.isExtinctIn(currentYear, useClanTechBase, techFaction)
                || acquisition.getAvailability() == EquipmentType.RATING_X)) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE,
                "It is extinct!");
        }

        int targetNumber = skill.getFinalSkillValue();
        Procurement procurement = new Procurement(targetNumber, currentYear, faction);

        TargetRoll target = new TargetRoll(targetNumber, skill.getSkillLevel().toString());
        if (acquisition instanceof Part) {
            target = procurement.getProcurementTargetNumber((Part) acquisition,
                false, false);
        } else {
            target.append(acquisition.getAllAcquisitionMods());
        }

        if (campaignOptions.isUseAtB() && campaignOptions.isRestrictPartsByMission()) {
            int contractAvailability = getContractPartsAvailability(campaign);
            target.addModifier(contractAvailability, "Contract Availability");
        }

        return target;
    }

    /**
     * Determines the parts availability level for active contracts in the current campaign.
     *
     * <p>This method evaluates all active AtB (Against the Bot) contracts and calculates the
     * most permissive parts availability level. It ensures that parts availability is not
     * restricted by contracts that have not yet started. The availability level corresponds to
     * the least restrictive contract, allowing the greatest access to parts.</p>
     *
     * @return the highest parts availability level among all active AtB contracts, as an integer.
     */
    public static int getContractPartsAvailability(Campaign campaign) {
        int contractAvailability = 0;

        for (final AtBContract contract : campaign.getActiveAtBContracts()) {
            int partsAvailability = contract.getPartsAvailabilityLevel();

            if (partsAvailability > contractAvailability) {
                contractAvailability = partsAvailability;
            }
        }

        return contractAvailability;
    }
}
