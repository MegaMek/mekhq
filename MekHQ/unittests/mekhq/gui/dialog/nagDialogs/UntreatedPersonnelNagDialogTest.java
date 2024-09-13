package mekhq.gui.dialog.nagDialogs;

import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.UntreatedPersonnelNagDialog.isUntreatedInjury;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UntreatedPersonnelNagDialogTest {
    Campaign campaign;
    Person person;

    @BeforeAll
    public static void setup() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        Ranks.initializeRankSystems();
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @BeforeEach
    public void init() {
        campaign = new Campaign();
        person = campaign.newPerson(PersonnelRole.MEKWARRIOR);
        person.setHits(1);
    }

    @Test
    public void isUntreatedInjuryIncludesNonPrisonersTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.FREE, false);
        campaign.importPerson(person);
        assertTrue(isUntreatedInjury(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesPrisonersTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.PRISONER, false);
        campaign.importPerson(person);
        assertFalse(isUntreatedInjury(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesPrisonerDefectorsTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.PRISONER_DEFECTOR, false);
        campaign.importPerson(person);
        assertFalse(isUntreatedInjury(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesBondsmenTest() {
        person.setPrisonerStatus(campaign, PrisonerStatus.BONDSMAN, false);
        campaign.importPerson(person);
        assertTrue(isUntreatedInjury(campaign));
    }

    @Test
    public void isUntreatedInjuryExcludesInactivePersonnelTest() {
        person.setStatus(PersonnelStatus.AWOL);
        campaign.importPerson(person);
        assertFalse(isUntreatedInjury(campaign));
    }
}