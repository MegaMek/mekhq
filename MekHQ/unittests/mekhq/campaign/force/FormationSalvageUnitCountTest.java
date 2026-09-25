package mekhq.campaign.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.mission.scenarios.salvage.CamOpsStrictSalvage;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class FormationSalvageUnitCountTest {
    private final LocalHangar hangar = mock(LocalHangar.class);

    private static Unit unit(boolean canSalvage, boolean isDoomedInSpace) {
        Entity entity = mock(Tank.class);
        when(entity.doomedInSpace()).thenReturn(isDoomedInSpace);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.canSalvage(anyBoolean())).thenReturn(canSalvage);
        when(unit.isRepairable()).thenReturn(true);
        return unit;
    }

    private Formation formationWith(Unit... units) {
        Formation formation = spy(new Formation("Salvage Lance"));
        doReturn(List.of(units)).when(formation).getAllUnitsAsUnits(hangar, false);
        return formation;
    }

    @Test
    void onlyUnitsAvailableForSalvageAreCounted() {
        Formation formation = formationWith(unit(true, false), unit(false, false), unit(true, false));

        assertEquals(2, formation.getSalvageUnitCount(hangar, false, new CamOpsStrictSalvage()));
    }

    @Test
    void unitsThatCantSurviveInSpaceDontCountInSpace() {
        Formation formation = formationWith(unit(true, true), unit(true, false));

        assertEquals(1, formation.getSalvageUnitCount(hangar, true, new CamOpsStrictSalvage()));
    }

    @Test
    void unitsThatCantSurviveInSpaceStillCountOnTheGround() {
        Formation formation = formationWith(unit(true, true));

        assertEquals(1, formation.getSalvageUnitCount(hangar, false, new CamOpsStrictSalvage()));
    }

    @Test
    void emptyFormationHasNoSalvageUnits() {
        assertEquals(0, formationWith().getSalvageUnitCount(hangar, false, new CamOpsStrictSalvage()));
    }
}
