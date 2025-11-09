package mekhq.gui.dialog;

import static mekhq.campaign.personnel.medical.BodyLocation.LEFT_FOOT;
import static mekhq.campaign.personnel.medical.BodyLocation.LEFT_HAND;
import static mekhq.campaign.personnel.medical.BodyLocation.RIGHT_FOOT;
import static mekhq.campaign.personnel.medical.BodyLocation.RIGHT_HAND;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.COSMETIC_SURGERY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;

import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;

public class AdvancedReplacementLimbDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedReplacementLimbDialog";

    private final List<Person> levelTwoDoctors = new ArrayList<>();
    private final List<Person> levelFiveDoctors = new ArrayList<>();
    private final List<Injury> relevantInjuries = new ArrayList<>();
    private final Map<Injury, List<ProstheticType>> treatmentOptions = new HashMap<>();

    public AdvancedReplacementLimbDialog(List<Person> activePersonnel, Person patient) {
        gatherRelevantInjuries(patient.getPermanentInjuries());
        gatherSurgeons(activePersonnel);
        gatherTreatmentOptions();
    }

    private void gatherSurgeons(List<Person> activePersonnel) {
        for (Person person : activePersonnel) {
            if (person.isDoctor()) {
                Skill skill = person.getSkill(SkillType.S_SURGERY);
                if (skill != null) {
                    SkillModifierData skillModifierData = person.getSkillModifierData();
                    int skillLevel = skill.getTotalSkillLevel(skillModifierData);

                    if (skillLevel >= 5) {
                        levelFiveDoctors.add(person);
                    }

                    if (skillLevel >= 2) { // We want 5+ doctors to be in both lists
                        levelTwoDoctors.add(person);
                    }
                }
            }
        }
    }

    private void gatherRelevantInjuries(List<Injury> permanentInjuries) {
        for (Injury injury : permanentInjuries) {
            InjuryType injuryType = injury.getType();
            if (injuryType.getKey().contains("alt:")) { // Filters out any injury not from Alt Advanced Medical
                if (!injuryType.getInjurySubType().isProsthetic()) {
                    relevantInjuries.add(injury);
                }
            }
        }
    }

    private void gatherTreatmentOptions() {
        for (Injury injury : relevantInjuries) {
            InjuryType injuryType = injury.getType();
            if (injuryType.getInjurySubType().isBurn()) {
                treatmentOptions.put(injury, List.of(COSMETIC_SURGERY));
                continue;
            }

            BodyLocation injuryLocation = getBodyLocation(injury);
            List<ProstheticType> eligibleTreatments = getEligibleTreatments(injuryLocation);
            treatmentOptions.put(injury, eligibleTreatments);
        }
    }

    private static List<ProstheticType> getEligibleTreatments(BodyLocation injuryLocation) {
        List<ProstheticType> eligibleTreatments = new ArrayList<>();
        for (ProstheticType type : ProstheticType.values()) {
            if (type.getEligibleLocations().contains(injuryLocation)) {
                eligibleTreatments.add(type);
            }
        }
        return eligibleTreatments;
    }

    private static BodyLocation getBodyLocation(Injury injury) {
        BodyLocation injuryLocation = injury.getLocation();
        if (injuryLocation.isChildOf(LEFT_HAND)) {
            injuryLocation = LEFT_HAND;
        } else if (injuryLocation.isChildOf(RIGHT_HAND)) {
            injuryLocation = RIGHT_HAND;
        } else if (injuryLocation.isChildOf(LEFT_FOOT)) {
            injuryLocation = LEFT_FOOT;
        } else if (injuryLocation.isChildOf(RIGHT_FOOT)) {
            injuryLocation = RIGHT_FOOT;
        } else if (injuryLocation.Parent() != null) {
            injuryLocation = injuryLocation.Parent();
        }
        return injuryLocation;
    }
}


