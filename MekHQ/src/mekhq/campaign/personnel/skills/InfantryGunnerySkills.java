package mekhq.campaign.personnel.skills;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;

public class InfantryGunnerySkills {
    public static final List<String> INFANTRY_GUNNERY_SKILLS = List.of(SkillType.S_ARCHERY, SkillType.S_SMALL_ARMS,
          SkillType.S_DEMOLITIONS, SkillType.S_MARTIAL_ARTS, SkillType.S_MELEE_WEAPONS, SkillType.S_THROWN_WEAPONS,
          SkillType.S_SUPPORT_WEAPONS);

    public static @Nullable String getBestInfantryGunnerySkill(Person person) {
        String bestSkill = null;
        int highestLevel = -1;

        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();
        for (String skillName : INFANTRY_GUNNERY_SKILLS) {
            if (person.hasSkill(skillName)) {
                int skillLevel = person.getSkill(skillName).getTotalSkillLevel(options, attributes);

                if (skillLevel > highestLevel) {
                    highestLevel = skillLevel;
                    bestSkill = skillName;
                }
            }
        }

        return bestSkill;
    }
}
