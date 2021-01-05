package mekhq.campaign.unit.cleanup;

import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Test;

import megamek.common.EquipmentType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;

public class MovedEquipmentStepTest {
    @Test
    public void noMatchingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        when(mockProposal.getEquipment()).thenReturn(Collections.emptySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt(), any());
    }

    @Test
    public void noMatchingMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        when(mockProposal.getEquipment()).thenReturn(Collections.emptySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt(), any());
    }

    @Test
    public void doesNotMatchDestroyedEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.isDestroyed()).thenReturn(true);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt(), any());
    }

    @Test
    public void doesNotMatchDestroyedMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.isDestroyed()).thenReturn(true);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt(), any());
    }

    @Test
    public void doesNotMatchEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getType()).thenReturn(mock(EquipmentType.class));

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt(), any());
    }

    @Test
    public void doesNotMatchMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);
        when(mockMissingPart.getType()).thenReturn(mock(EquipmentType.class));

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt(), any());
    }
    
    @Test
    public void mountMatchesEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(1, mockMount).entrySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getType()).thenReturn(mockType);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(1)).proposeMapping(eq(mockPart), eq(1), eq(mockMount));
    }

    @Test
    public void mountMatchesMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(1, mockMount).entrySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);
        when(mockMissingPart.getType()).thenReturn(mockType);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(1)).proposeMapping(eq(mockMissingPart), eq(1), eq(mockMount));
    }
}
