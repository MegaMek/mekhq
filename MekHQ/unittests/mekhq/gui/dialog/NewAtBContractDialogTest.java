package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;

class NewAtBContractDialogTest {

    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);

    @ParameterizedTest
    @NullAndEmptySource
    void resolvePlanetName_nullOrEmptyReturnsNullWithoutConsultingRegistry(String input) {
        Systems registry = mock(Systems.class);

        PlanetarySystem result = NewAtBContractDialog.resolvePlanetName(input, DATE, registry);

        assertNull(result);

        // Empty/null input must short-circuit; never touch the registry.
        verify(registry, never()).getSystemByName(ArgumentMatchers.anyString(),
              ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    void resolvePlanetName_unknownNameReturnsNull() {
        Systems registry = mock(Systems.class);
        when(registry.getSystemByName("NotARealSystemXYZ", DATE)).thenReturn(null);

        PlanetarySystem result = NewAtBContractDialog.resolvePlanetName("NotARealSystemXYZ", DATE, registry);

        assertNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Terra", "Galatea" })
    void resolvePlanetName_knownNameReturnsRegistryResult(String name) {
        Systems registry = mock(Systems.class);
        PlanetarySystem expected = mock(PlanetarySystem.class);
        when(registry.getSystemByName(name, DATE)).thenReturn(expected);

        PlanetarySystem result = NewAtBContractDialog.resolvePlanetName(name, DATE, registry);

        assertSame(expected, result);
    }
}
