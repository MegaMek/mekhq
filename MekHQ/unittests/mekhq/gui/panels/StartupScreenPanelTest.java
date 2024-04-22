package mekhq.gui.panels;

import megamek.common.preference.PreferenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class StartupScreenPanelTest {
    File dir;

    @BeforeEach
    void setUp() {
        dir = mock(File.class);
    }

    @Test
    void testSaveFilterAllowsValidCampaignSaves(){
        String fileName = "MySave.xml";
        assertTrue(StartupScreenPanel.saveFilter.accept(dir, fileName));
        fileName = "MySave.CPNX";
        assertTrue(StartupScreenPanel.saveFilter.accept(dir, fileName));
        fileName = "20240306T2344 Save Campaign No 666.cpnx.gz";
        assertTrue(StartupScreenPanel.saveFilter.accept(dir, fileName));
    }

    @Test
    void testSaveFilterNotAllowClientSettingsXML(){
        String fileName = PreferenceManager.DEFAULT_CFG_FILE_NAME;
        boolean allowed = StartupScreenPanel.saveFilter.accept(dir, fileName);
        assertFalse(allowed);
    }
}