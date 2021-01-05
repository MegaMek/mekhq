package mekhq.campaign.unit.cleanup;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map.Entry;

import org.junit.Test;

import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public class EquipmentProposalTest {

    @Test
    public void getUnitTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);
        assertEquals(unit, proposal.getUnit());
    }

    @Test
    public void considerTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        Part mockPart = mock(Part.class);
        proposal.consider(mockPart);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        proposal.consider(mockEquipmentPart);

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        proposal.consider(mockMissingEquipmentPart);

        assertFalse(proposal.getParts().contains(mockPart));
        assertTrue(proposal.getParts().contains(mockEquipmentPart));
        assertTrue(proposal.getParts().contains(mockMissingEquipmentPart));
    }

    @Test
    public void includeEquipmentTest() {
        Unit unit = mock(Unit.class);

        int equipmentNum = 1;
        EquipmentProposal proposal = new EquipmentProposal(unit);

        assertNull(proposal.getEquipment(equipmentNum));
        assertTrue(proposal.getEquipment().isEmpty());

        Mounted mockMounted = mock(Mounted.class);
        proposal.includeEquipment(equipmentNum, mockMounted);

        assertEquals(mockMounted, proposal.getEquipment(equipmentNum));
        assertFalse(proposal.getEquipment().isEmpty());

        for (Entry<Integer, Mounted> entry : proposal.getEquipment()) {
            assertEquals(equipmentNum, (int) entry.getKey());
            assertEquals(mockMounted, entry.getValue());
        }
    }

    @Test
    public void getOriginalMappingTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        assertTrue(proposal.getOriginalMapping(mock(EquipmentPart.class)) < 0);
        assertEquals(originalEquipmentNum, proposal.getOriginalMapping(mockEquipmentPart));
        assertEquals(originalMissingEquipmentNum, proposal.getOriginalMapping(mockMissingEquipmentPart));
    }

    @Test
    public void proposeMappingTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        assertFalse(proposal.hasProposal(mockEquipmentPart));

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        assertFalse(proposal.hasProposal(mockMissingEquipmentPart));

        Mounted mockMount0 = mock(Mounted.class);
        proposal.includeEquipment(originalEquipmentNum, mockMount0);
        Mounted mockMount1 = mock(Mounted.class);
        proposal.includeEquipment(originalMissingEquipmentNum, mockMount1);

        proposal.proposeMapping(mockEquipmentPart, originalEquipmentNum, mockMount0);

        assertTrue(proposal.hasProposal(mockEquipmentPart));
        assertNull(proposal.getEquipment(originalEquipmentNum));
        assertEquals(mockMount1, proposal.getEquipment(originalMissingEquipmentNum));

        proposal.proposeMapping(mockMissingEquipmentPart, originalMissingEquipmentNum, mockMount1);

        assertTrue(proposal.hasProposal(mockMissingEquipmentPart));
        assertNull(proposal.getEquipment(originalEquipmentNum));
        assertNull(proposal.getEquipment(originalMissingEquipmentNum));
    }
    
    @Test
    public void isReducedTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        assertFalse(proposal.isReduced());

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        assertFalse(proposal.isReduced());

        Mounted mockMount0 = mock(Mounted.class);
        proposal.includeEquipment(originalEquipmentNum, mockMount0);
        Mounted mockMount1 = mock(Mounted.class);
        proposal.includeEquipment(originalMissingEquipmentNum, mockMount1);

        proposal.proposeMapping(mockEquipmentPart, originalEquipmentNum, mockMount0);

        assertFalse(proposal.isReduced());

        proposal.proposeMapping(mockMissingEquipmentPart, originalMissingEquipmentNum, mockMount1);

        assertTrue(proposal.isReduced());
    }

    @Test
    public void applyTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        EquipmentPart mockIncorrectEquipmentPart = mock(EquipmentPart.class);
        int incorrectEquipmentNum = 3;
        when(mockIncorrectEquipmentPart.getEquipmentNum()).thenReturn(incorrectEquipmentNum);
        proposal.consider(mockIncorrectEquipmentPart);

        EquipmentPart mockIncorrectMissingEquipmentPart = mock(EquipmentPart.class);
        int incorrectMissingEquipmentNum = 4;
        when(mockIncorrectMissingEquipmentPart.getEquipmentNum()).thenReturn(incorrectMissingEquipmentNum);
        proposal.consider(mockIncorrectMissingEquipmentPart);

        Mounted mockMount0 = mock(Mounted.class);
        proposal.includeEquipment(originalEquipmentNum, mockMount0);
        Mounted mockMount1 = mock(Mounted.class);
        proposal.includeEquipment(originalMissingEquipmentNum, mockMount1);

        proposal.proposeMapping(mockEquipmentPart, originalEquipmentNum, mockMount0);
        proposal.proposeMapping(mockMissingEquipmentPart, originalMissingEquipmentNum, mockMount1);

        assertFalse(proposal.isReduced());

        proposal.apply();

        verify(mockEquipmentPart, times(1)).setEquipmentNum(eq(originalEquipmentNum));
        verify(mockMissingEquipmentPart, times(1)).setEquipmentNum(eq(originalMissingEquipmentNum));
        verify(mockIncorrectEquipmentPart, times(1)).setEquipmentNum(eq(-1));
        verify(mockIncorrectMissingEquipmentPart, times(1)).setEquipmentNum(eq(-1));
    }
}
