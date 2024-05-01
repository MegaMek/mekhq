package mekhq.campaign.personnel.education;

import mekhq.campaign.personnel.SkillType;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademyTests {
    @InjectMocks
    Academy academy = new Academy();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsMilitary() {
        Boolean result = academy.isMilitary();
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testSetIsMilitary() {
        academy.setIsMilitary(true);
    }

    @Test
    void testIsClan() {
        Boolean result = academy.isClan();
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testSetIsClan() {
        academy.setIsClan(true);
    }

    @Test
    void testIsPrepSchool() {
        Boolean result = academy.isPrepSchool();
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testSetIsPrepSchool() {
        academy.setIsPrepSchool(true);
    }

    @Test
    void testIsLocal() {
        Boolean result = academy.isLocal();
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testSetIsLocal() {
        academy.setIsLocal(true);
    }

    @Test
    void testIsFactionRestricted() {
        Boolean result = academy.isFactionRestricted();
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testSetIsFactionRestricted() {
        academy.setIsFactionRestricted(true);
    }

    @Test
    void testGetTuitionAdjusted() {
        Integer result = academy.getTuitionAdjusted(0, 0);
        Assertions.assertEquals(Integer.valueOf(0), result);
    }

    @Test
    public void testSkillParser_ValidSkill() {
        assertEquals(SkillType.S_PILOT_MECH, Academy.skillParser("piloting/mech"));
        assertEquals(SkillType.S_GUN_MECH, Academy.skillParser("gunnery/mech"));
        assertEquals(SkillType.S_GUN_MECH, Academy.skillParser("gunnery/mech"));
        assertEquals(SkillType.S_PILOT_AERO, Academy.skillParser("piloting/aerospace"));
        assertEquals(SkillType.S_GUN_AERO, Academy.skillParser("gunnery/aerospace"));
        assertEquals(SkillType.S_PILOT_GVEE, Academy.skillParser("piloting/groundvehicle"));
        assertEquals(SkillType.S_PILOT_VTOL, Academy.skillParser("piloting/vtol"));
        assertEquals(SkillType.S_PILOT_NVEE, Academy.skillParser("piloting/naval"));
        assertEquals(SkillType.S_GUN_VEE, Academy.skillParser("gunnery/vehicle"));
        assertEquals(SkillType.S_PILOT_JET, Academy.skillParser("piloting/aircraft"));
        assertEquals(SkillType.S_GUN_JET, Academy.skillParser("gunnery/aircraft"));
        assertEquals(SkillType.S_PILOT_SPACE, Academy.skillParser("piloting/spacecraft"));
        assertEquals(SkillType.S_GUN_SPACE, Academy.skillParser("gunnery/spacecraft"));
        assertEquals(SkillType.S_ARTILLERY, Academy.skillParser("artillery"));
        assertEquals(SkillType.S_GUN_BA, Academy.skillParser("gunnery/battlesuit"));
        assertEquals(SkillType.S_GUN_PROTO, Academy.skillParser("gunnery/protomech"));
        assertEquals(SkillType.S_SMALL_ARMS, Academy.skillParser("smallarms"));
        assertEquals(SkillType.S_ANTI_MECH, Academy.skillParser("anti-mech"));
        assertEquals(SkillType.S_TECH_MECH, Academy.skillParser("tech/mech"));
        assertEquals(SkillType.S_TECH_MECHANIC, Academy.skillParser("tech/mechanic"));
        assertEquals(SkillType.S_TECH_AERO, Academy.skillParser("tech/aero"));
        assertEquals(SkillType.S_TECH_BA, Academy.skillParser("tech/ba"));
        assertEquals(SkillType.S_TECH_VESSEL, Academy.skillParser("tech/vessel"));
        assertEquals(SkillType.S_ASTECH, Academy.skillParser("astech"));
        assertEquals(SkillType.S_DOCTOR, Academy.skillParser("doctor"));
        assertEquals(SkillType.S_MEDTECH, Academy.skillParser("medtech"));
        assertEquals(SkillType.S_NAV, Academy.skillParser("hyperspacenavigation"));
        assertEquals(SkillType.S_ADMIN, Academy.skillParser("administration"));
        assertEquals(SkillType.S_TACTICS, Academy.skillParser("tactics"));
        assertEquals(SkillType.S_STRATEGY, Academy.skillParser("strategy"));
        assertEquals(SkillType.S_NEG, Academy.skillParser("negotiation"));
        assertEquals(SkillType.S_LEADER, Academy.skillParser("leadership"));
        assertEquals(SkillType.S_SCROUNGE, Academy.skillParser("scrounge"));
    }

    @Test
    public void testSkillParser_InvalidSkill() {
        assertThrows(IllegalStateException.class, () -> Academy.skillParser("invalid_skill"));
    }
}