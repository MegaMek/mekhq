package mekhq.campaign.unit.cleanup;

import megamek.common.*;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EquipmentUnscramblerTest {
    @Test
    public void createTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        assertInstanceOf(DefaultEquipmentUnscrambler.class, EquipmentUnscrambler.create(mockUnit));

        mockUnit = mock(Unit.class);
        BattleArmor mockBAEntity = mock(BattleArmor.class);
        when(mockUnit.getEntity()).thenReturn(mockBAEntity);

        assertInstanceOf(BattleArmorEquipmentUnscrambler.class, EquipmentUnscrambler.create(mockUnit));
    }

    @Test
    public void unscrambleSimpleTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        EquipmentType et0 = mock(EquipmentType.class);
        EquipmentType et1 = mock(EquipmentType.class);
        AmmoType at0 = mock(AmmoType.class);
        AmmoType at1 = mock(AmmoType.class);

        Mounted<?> mount0 = createEquipment(mockEntity, 0, et0);
        Mounted<?> mount1 = createEquipment(mockEntity, 1, et1);
        Mounted<?> mount2 = createEquipment(mockEntity, 2, at0);
        Mounted<?> mount3 = createEquipment(mockEntity, 3, at1);
        when(mockEntity.getEquipment()).thenReturn(equipmentList(mount0, mount1, mount2, mount3));

        EquipmentPart ep0 = createPart(0, et0);
        MissingEquipmentPart ep1 = createMissingPart(1, et1);
        AmmoBin ep2 = createPart(2, at0);
        AmmoBin ep3 = createPart(3, at1);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(ep0, ep1, ep2, ep3));

        EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(mockUnit);
        EquipmentUnscramblerResult result = unscrambler.unscramble();

        assertTrue(result.succeeded());

        verify(ep0, times(1)).setEquipmentNum(0);
        verify(ep1, times(1)).setEquipmentNum(1);
        verify(ep2, times(1)).setEquipmentNum(2);
        verify(ep3, times(1)).setEquipmentNum(3);
    }

    @Test
    public void unscrambleSimpleEquipmentMoveTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        EquipmentType et0 = mock(EquipmentType.class);
        EquipmentType et1 = mock(EquipmentType.class);
        AmmoType at0 = mock(AmmoType.class);
        AmmoType at1 = mock(AmmoType.class);

        // The equipment moved positions
        Mounted mount0 = createEquipment(mockEntity, 0, et1);
        Mounted mount1 = createEquipment(mockEntity, 1, et0);

        Mounted mount2 = createEquipment(mockEntity, 2, at0);
        Mounted mount3 = createEquipment(mockEntity, 3, at1);
        when(mockEntity.getEquipment()).thenReturn(equipmentList(mount0, mount1, mount2, mount3));

        EquipmentPart ep0 = createPart(0, et0);
        MissingEquipmentPart ep1 = createMissingPart(1, et1);
        AmmoBin ep2 = createPart(2, at0);
        AmmoBin ep3 = createPart(3, at1);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(ep0, ep1, ep2, ep3));

        EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(mockUnit);
        EquipmentUnscramblerResult result = unscrambler.unscramble();

        assertTrue(result.succeeded());

        // These two moved positions
        verify(ep0, times(1)).setEquipmentNum(1);
        verify(ep1, times(1)).setEquipmentNum(0);

        verify(ep2, times(1)).setEquipmentNum(2);
        verify(ep3, times(1)).setEquipmentNum(3);
    }

    @Test
    public void unscrambleSimpleAmmoTypeDifferenceTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        EquipmentType et0 = mock(EquipmentType.class);
        EquipmentType et1 = mock(EquipmentType.class);
        AmmoType at0 = mock(AmmoType.class);
        AmmoType at1 = mock(AmmoType.class);
        AmmoType at2 = mock(AmmoType.class);

        Mounted mount0 = createEquipment(mockEntity, 0, et0);
        Mounted mount1 = createEquipment(mockEntity, 1, et1);
        Mounted mount2 = createEquipment(mockEntity, 2, at0);
        Mounted mount3 = createEquipment(mockEntity, 3, at1);
        when(mockEntity.getEquipment()).thenReturn(equipmentList(mount0, mount1, mount2, mount3));

        EquipmentPart ep0 = createPart(0, et0);
        MissingEquipmentPart ep1 = createMissingPart(1, et1);
        AmmoBin ep2 = createPart(2, at2); // different ammo type
        doReturn(true).when(ep2).canChangeMunitions(eq(at0)); // compatible with the mount
        AmmoBin ep3 = createPart(3, at1);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(ep0, ep1, ep2, ep3));

        EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(mockUnit);
        EquipmentUnscramblerResult result = unscrambler.unscramble();

        assertTrue(result.succeeded());

        verify(ep0, times(1)).setEquipmentNum(0);
        verify(ep1, times(1)).setEquipmentNum(1);
        verify(ep2, times(1)).setEquipmentNum(2);
        verify(ep3, times(1)).setEquipmentNum(3);
    }

    @Test
    public void unscrambleSimpleAmmoBinsMovedTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        EquipmentType et0 = mock(EquipmentType.class);
        EquipmentType et1 = mock(EquipmentType.class);
        AmmoType at0 = mock(AmmoType.class);
        AmmoType at1 = mock(AmmoType.class);

        Mounted mount0 = createEquipment(mockEntity, 0, et0);
        Mounted mount1 = createEquipment(mockEntity, 1, et1);
        
        // ammo bins swapped positions
        Mounted mount2 = createEquipment(mockEntity, 2, at1);
        Mounted mount3 = createEquipment(mockEntity, 3, at0);

        when(mockEntity.getEquipment()).thenReturn(equipmentList(mount0, mount1, mount2, mount3));

        EquipmentPart ep0 = createPart(0, et0);
        MissingEquipmentPart ep1 = createMissingPart(1, et1);
        AmmoBin ep2 = createPart(2, at0);
        AmmoBin ep3 = createPart(3, at1);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(ep0, ep1, ep2, ep3));

        EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(mockUnit);
        EquipmentUnscramblerResult result = unscrambler.unscramble();

        assertTrue(result.succeeded());

        verify(ep0, times(1)).setEquipmentNum(0);
        verify(ep1, times(1)).setEquipmentNum(1);
        
        // these should swap positions
        verify(ep2, times(1)).setEquipmentNum(3); 
        verify(ep3, times(1)).setEquipmentNum(2);
    }

    @Test
    public void unscrambleDuplicatePartTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        EquipmentType et0 = mock(EquipmentType.class);
        EquipmentType et1 = mock(EquipmentType.class);
        AmmoType at0 = mock(AmmoType.class);
        AmmoType at1 = mock(AmmoType.class);
        AmmoType at2 = mock(AmmoType.class);

        Mounted mount0 = createEquipment(mockEntity, 0, et0);
        Mounted mount1 = createEquipment(mockEntity, 1, et1);
        Mounted mount2 = createEquipment(mockEntity, 2, at0);
        Mounted mount3 = createEquipment(mockEntity, 3, at1);
        when(mockEntity.getEquipment()).thenReturn(equipmentList(mount0, mount1, mount2, mount3));

        EquipmentPart ep0 = createPart(0, et0);
        MissingEquipmentPart ep1 = createMissingPart(1, et1);
        AmmoBin ep2Bad = createPart(2, at2); // This ammo bin is duplicative and bad
        doReturn(true).when(ep2Bad).canChangeMunitions(any());
        AmmoBin ep2 = createPart(2, at0);
        AmmoBin ep3 = createPart(3, at1);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(ep0, ep1, ep2Bad, ep2, ep3));

        EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(mockUnit);
        EquipmentUnscramblerResult result = unscrambler.unscramble();

        assertFalse(result.succeeded());
        assertNotNull(result.getMessage());

        verify(ep0, times(1)).setEquipmentNum(0);
        verify(ep1, times(1)).setEquipmentNum(1);
        verify(ep2, times(1)).setEquipmentNum(2);
        verify(ep3, times(1)).setEquipmentNum(3);

        verify(ep2Bad, times(1)).setEquipmentNum(-1);
    }

    @Test
    public void unscrambleMissingPartTest() {
        Unit mockUnit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        EquipmentType et0 = mock(EquipmentType.class);
        EquipmentType et1 = mock(EquipmentType.class);
        AmmoType at0 = mock(AmmoType.class);
        AmmoType at1 = mock(AmmoType.class);
        MiscType mt0 = mock(MiscType.class);

        Mounted mount0 = createEquipment(mockEntity, 0, et0);
        Mounted mount1 = createEquipment(mockEntity, 1, et1);
        Mounted mount2 = createEquipment(mockEntity, 2, at0);
        Mounted mount3 = createEquipment(mockEntity, 3, at1);
        Mounted mount4 = createEquipment(mockEntity, 4, mt0);
        when(mockEntity.getEquipment()).thenReturn(equipmentList(mount0, mount1, mount2, mount3, mount4));

        EquipmentPart ep0 = createPart(0, et0);
        MissingEquipmentPart ep1 = createMissingPart(1, et1);
        AmmoBin ep2 = createPart(2, at0);
        AmmoBin ep3 = createPart(3, at1);
        EquipmentPart epMissing = createPart(4, et0);
        when(mockUnit.getParts()).thenReturn(Arrays.asList(ep0, ep1, ep2, ep3, epMissing));

        EquipmentUnscrambler unscrambler = EquipmentUnscrambler.create(mockUnit);
        EquipmentUnscramblerResult result = unscrambler.unscramble();

        assertFalse(result.succeeded());
        assertNotNull(result.getMessage());

        verify(ep0, times(1)).setEquipmentNum(0);
        verify(ep1, times(1)).setEquipmentNum(1);
        verify(ep2, times(1)).setEquipmentNum(2);
        verify(ep3, times(1)).setEquipmentNum(3);

        verify(epMissing, times(1)).setEquipmentNum(-1);
    }
    
    private Mounted createEquipment(Entity entity, int equipmentNum, EquipmentType type) {
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(type);
        doReturn(mockMounted).when(entity).getEquipment(eq(equipmentNum));
        doReturn(equipmentNum).when(entity).getEquipmentNum(eq(mockMounted));

        return mockMounted;
    }

    private EquipmentPart createPart(int equipmentNum, EquipmentType type) {
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getEquipmentNum()).thenReturn(equipmentNum);
        doAnswer(inv -> {
            int newEquipmentNum = inv.getArgument(0);
            when(mockPart.getEquipmentNum()).thenReturn(newEquipmentNum);
            return null;
        }).when(mockPart).setEquipmentNum(anyInt());
        when(mockPart.getType()).thenReturn(type);
        return mockPart;
    }

    private MissingEquipmentPart createMissingPart(int equipmentNum, EquipmentType type) {
        MissingEquipmentPart mockPart = mock(MissingEquipmentPart.class);
        when(mockPart.getEquipmentNum()).thenReturn(equipmentNum);
        doAnswer(inv -> {
            int newEquipmentNum = inv.getArgument(0);
            when(mockPart.getEquipmentNum()).thenReturn(newEquipmentNum);
            return null;
        }).when(mockPart).setEquipmentNum(anyInt());
        when(mockPart.getType()).thenReturn(type);
        return mockPart;
    }

    private AmmoBin createPart(int equipmentNum, AmmoType type) {
        AmmoBin mockPart = mock(AmmoBin.class);
        when(mockPart.getEquipmentNum()).thenReturn(equipmentNum);
        doAnswer(inv -> {
            int newEquipmentNum = inv.getArgument(0);
            when(mockPart.getEquipmentNum()).thenReturn(newEquipmentNum);
            return null;
        }).when(mockPart).setEquipmentNum(anyInt());
        when(mockPart.getType()).thenReturn(type);
        doReturn(true).when(mockPart).canChangeMunitions(eq(type));
        return mockPart;
    }

    private List<Mounted<?>> equipmentList(Mounted<?>... equipment) {
        return new ArrayList<>(Arrays.asList(equipment));
    }
}
