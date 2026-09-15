package mekhq;

import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import megamek.MegaMek;
import megamek.SuiteConstants;
import megamek.client.ui.util.UIUtil;
import megameklab.MegaMekLab;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.JumpPath;
import mekhq.campaign.events.TransitStatusChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.gui.CampaignGUI;
import mekhq.gui.ExperimentalMapView;
import mekhq.gui.ExperimentalMapView.ViewState;
import mekhq.gui.ExperimentalMapView.BoundaryDetail;
import mekhq.gui.ExperimentalMapView.CapitalDetail;
import mekhq.gui.ExperimentalMapView.LandmarkLayers;
import mekhq.gui.InterstellarMapPanel;
import mekhq.gui.MapTab;
import mekhq.gui.SkiaMap;
import mekhq.gui.enums.MHQTabType;
import mekhq.utilities.MHQInternationalization;
import org.jetbrains.skiko.SkiaLayer;
import org.jetbrains.skia.Rect;

public final class SkikoMapSmoke {
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private static ViewState expectedView;
    private static double initialScale;
    private static boolean offscreen;
    private static boolean routesOnly;
    private static boolean modesOnly;
    private static boolean controlsOnly;
    private static boolean reachabilityOnly;
    private static boolean hpgOnly;
    private static boolean retainedOnly;

    public static void main(String... args) throws Exception {
        offscreen = List.of(args).contains("--offscreen");
        routesOnly = List.of(args).contains("--routes-only");
        modesOnly = List.of(args).contains("--modes-only");
        controlsOnly = List.of(args).contains("--controls-only");
        reachabilityOnly = List.of(args).contains("--reachability-only");
        hpgOnly = List.of(args).contains("--hpg-only");
        retainedOnly = List.of(args).contains("--retained-only");
        boolean performance = List.of(args).contains("--performance");
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("The real map smoke run requires an interactive desktop");
        }
        System.setProperty("mekhq.experimental.skikoMap", "true");
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            exception.printStackTrace();
            System.exit(1);
        });
        MegaMek.initializeSuiteGraphicalSetups(MHQConstants.PROJECT_NAME);
        CurrencyManager.getInstance().loadCurrencies();
        Eras.initializeEras();
        FinancialInstitutions.initializeFinancialInstitutions();
        InjuryTypes.registerAll();
        Ranks.initializeRankSystems();
        SkillType.initializeTypes();
        SpecialAbility.initializeSPA(false);
        AtBScenarioModifier.initializeScenarioModifiers(false);
        Factions.setInstance(Factions.loadDefault(false));
        Systems.initializeDefaultSystems();
        MHQStaticDirectoryManager.initialize();
        Campaign campaign = Objects.requireNonNull(CampaignFactory.createCampaign());
        SwingUtilities.invokeAndWait(() -> {
            MegaMek.getMMPreferences().loadFromFile(SuiteConstants.MM_PREFERENCES_FILE);
            MegaMekLab.getMMLPreferences().loadFromFile(SuiteConstants.MML_PREFERENCES_FILE);
            MekHQ.getMHQPreferences().loadFromFile(SuiteConstants.MHQ_PREFERENCES_FILE);
            try {
                var preferences = MekHQ.class.getDeclaredMethod("setUserPreferences");
                preferences.setAccessible(true);
                preferences.invoke(null);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to initialize normal MekHQ preferences", exception);
            }
            MekHQ.updateGuiScaling();
            MekHQ app = MekHQ.getInstance();
            app.activateCampaign(campaign);
            CampaignGUI gui = app.getCampaigngui();
            gui.focusOnSystem(campaign.getCurrentSystem());
            JFrame frame = gui.getFrame();
            Rectangle desktop = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setBounds(desktop);
            frame.setAlwaysOnTop(true);
            frame.toFront();
            MapTab mapTab = gui.getNavigationTab().getMapTab();
            if (performance) {
                Timer startPerformance = new Timer(2000, event -> {
                    try {
                        mekhq.gui.SkikoMapPerformance.start(frame, mapTab, campaign);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Unable to start map performance benchmark", exception);
                    }
                });
                startPerformance.setRepeats(false);
                startPerformance.start();
                return;
            }
            JCheckBoxMenuItem toggle = findToggle(frame);
            AtomicInteger step = new AtomicInteger();
            Timer timer = new Timer(1500, event -> {
                try {
                    switch (step.incrementAndGet()) {
                        case 1 -> {
                            require(!mapTab.isExperimentalMapActive() && !toggle.isSelected(), "Java2D is not the default");
                            require(SkiaMap.getActiveSurfaces() == 0, "Native peer created before opt-in");
                            capture(frame, "java2d-map");
                            toggle.doClick(0);
                            require(mapTab.isExperimentalMapActive() && toggle.isSelected(), "Menu did not enable Skia");
                        }
                        case 2 -> {
                            require(SkiaMap.getCompletedFrames() > 0, "No native map frame");
                            require(SkiaMap.getCompletedStars() > 0, "No native stars");
                            require(find(frame, InterstellarMapPanel.class) == null, "Java2D map is still attached");
                            capture(frame, "skia-map");
                            if (retainedOnly) {
                                ((Timer) event.getSource()).stop();
                                checkRetainedTerritories(frame, mapTab, toggle);
                                return;
                            }
                            if (hpgOnly) {
                                ((Timer) event.getSource()).stop();
                                checkHpgNetwork(frame, app, toggle);
                                return;
                            }
                            if (reachabilityOnly) {
                                ((Timer) event.getSource()).stop();
                                checkReachability(frame, app, toggle);
                                return;
                            }
                            if (controlsOnly) {
                                ((Timer) event.getSource()).stop();
                                checkMapUtilities(frame, app, toggle);
                                return;
                            }
                            if (modesOnly) {
                                ((Timer) event.getSource()).stop();
                                checkAnalyticalModes(frame, app, toggle);
                                return;
                            }
                            if (routesOnly) {
                                ((Timer) event.getSource()).stop();
                                checkRoutes(frame, app, toggle);
                                return;
                            }
                            initialScale = find(frame, SkiaMap.class).getViewState().scale();
                            SkiaLayer layer = find(frame, SkiaLayer.class);
                            var canvas = layer.getCanvas();
                            canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL,
                                  System.currentTimeMillis(), 0, canvas.getWidth() / 2, canvas.getHeight() / 2,
                                  0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
                        }
                        case 3 -> {
                            require(find(frame, SkiaMap.class).getViewState().scale() > initialScale, "Native zoom did not change");
                            exerciseMapInput(frame, campaign);
                        }
                        case 4 -> {
                            toggle.doClick(0);
                            require(!mapTab.isExperimentalMapActive() && !toggle.isSelected(), "Menu did not restore Java2D");
                            require(SkiaMap.getActiveSurfaces() == 0, "Switching back leaked a native peer");
                            require(SkiaMap.getActiveTerritoryPaths() == 0, "Switching back leaked native territory paths");
                            require(Objects.equals(find(frame, InterstellarMapPanel.class).getSelectedSystem(),
                                  expectedView.selectedSystem()), "Selection was lost when restoring Java2D");
                        }
                        case 5 -> {
                            capture(frame, "java2d-map-restored");
                            toggle.doClick(0);
                            require(expectedView.equals(find(frame, SkiaMap.class).getViewState()),
                                  "Camera or selection changed during the renderer round trip");
                        }
                        case 6 -> gui.setSelectedTab(MHQTabType.PERSONNEL);
                        case 7 -> gui.setSelectedTab(MHQTabType.NAVIGATION);
                        case 8 -> frame.setSize(frame.getWidth() - UIUtil.scaleForGUI(60),
                              frame.getHeight() - UIUtil.scaleForGUI(40));
                        case 9 -> {
                            capture(frame, "skia-map-resized");
                            ((Timer) event.getSource()).stop();
                            checkMapUtilities(frame, app, toggle);
                        }
                        default -> throw new IllegalStateException("Unexpected smoke step");
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    ((Timer) event.getSource()).stop();
                    frame.dispose();
                    System.exit(1);
                }
            });
            timer.setInitialDelay(12000);
            timer.start();
        });
    }

    private static String mapText(String key) {
        return MHQInternationalization.getTextAt("mekhq.resources.CampaignGUI", key);
    }

    private static void checkRetainedTerritories(JFrame frame, MapTab tab, JCheckBoxMenuItem toggle) throws Exception {
        if (!"false".equals(System.getenv("SKIKO_MOTION_CACHE"))) {
            checkMotionTerritories(frame, tab, toggle);
            return;
        }
        require("true".equals(System.getenv("SKIKO_RETAIN_TERRITORIES")), "Retained territory option is required");
        SkiaMap map = find(frame, SkiaMap.class);
        ViewState original = map.getViewState();
        tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(true, false,
              ExperimentalMapView.BoundaryDetail.OFF));
        int fixtures = 0;
        double worstMean = 0;
        double worstChanged = 0;
        for (double scale : new double[] { 0.8, 6 }) {
            for (float displayScale : new float[] { 1, 1.75f }) {
                double anchorHorizontal = 0;
                double anchorVertical = 0;
                for (double[] offset : new double[][] { { 0, 0 }, { 32, 0 }, { 80.25, 0 }, { 32, 0 },
                    { 0, 32 }, { 0, 80.25 }, { 1, 0 }, { 220, 0 }, { -60, 0 } }) {
                  double pan = offset[0];
                    map.setViewState(new ViewState(original.centerX() + pan / scale,
                      original.centerY() + offset[1] / scale, scale, original.selectedSystem()));
                    BufferedImage reference = ImageIO.read(new ByteArrayInputStream(map.captureTerritoryPng(false, displayScale)));
                  int previousBuilds = map.getRetainedTerritoryBuilds();
                  int previousHits = map.getRetainedTerritoryHits();
                  int previousFallbacks = map.getRetainedTerritoryFallbacks();
                    BufferedImage cached = ImageIO.read(new ByteArrayInputStream(map.captureTerritoryPng(true, displayScale)));
                  require(map.hasRetainedTerritories(), "Retained fixture lost its cache");
                  boolean fallback = false;
                  if (map.getRetainedTerritoryBuilds() != previousBuilds) {
                    anchorHorizontal = pan;
                    anchorVertical = offset[1];
                  } else {
                    double deviceHorizontal = (pan - anchorHorizontal) * displayScale;
                    double deviceVertical = (offset[1] - anchorVertical) * displayScale;
                    fallback = Math.abs(deviceHorizontal - Math.rint(deviceHorizontal)) > 0.0001
                        || Math.abs(deviceVertical - Math.rint(deviceVertical)) > 0.0001;
                    require(map.getRetainedTerritoryFallbacks() == previousFallbacks + (fallback ? 1 : 0),
                        "Device-pixel alignment selected the wrong territory path");
                    require(map.getRetainedTerritoryHits() == previousHits + (fallback ? 0 : 1),
                        "Territory cache reuse counter did not match alignment");
                  }
                    long error = 0;
                    int changed = 0;
                    for (int vertical = 0; vertical < reference.getHeight(); vertical++) {
                        for (int horizontal = 0; horizontal < reference.getWidth(); horizontal++) {
                            int direct = reference.getRGB(horizontal, vertical);
                            int retained = cached.getRGB(horizontal, vertical);
                            int maximum = 0;
                            for (int shift : new int[] { 0, 8, 16 }) {
                                int difference = Math.abs((direct >> shift & 255) - (retained >> shift & 255));
                                error += difference;
                                maximum = Math.max(maximum, difference);
                            }
                            changed += maximum > 24 ? 1 : 0;
                        }
                    }
                    double pixels = reference.getWidth() * reference.getHeight();
                    double mean = error / (pixels * 3);
                    require(!fallback || error == 0, "Fractional-pan fallback differed from direct vectors");
                    double changedFraction = changed / pixels;
                    worstMean = Math.max(worstMean, mean);
                    worstChanged = Math.max(worstChanged, changedFraction);
                    if (mean >= 0.75 || changedFraction >= 0.01) {
                        ImageIO.write(reference, "png", output("retained-reference-failure").toFile());
                        ImageIO.write(cached, "png", output("retained-cache-failure").toFile());
                    }
                    require(mean < 0.75 && changedFraction < 0.01,
                          "Retained territory raster mismatch scale=" + scale + " display=" + displayScale
                                + " pan=" + pan + "," + offset[1] + " mean=" + mean + " changed=" + changedFraction);
                    fixtures++;
                }
            }
        }
        map.captureTerritoryPng(true, 1);
        int builds = map.getRetainedTerritoryBuilds();
        int hits = map.getRetainedTerritoryHits();
        map.captureTerritoryPng(true, 1);
        require(map.getRetainedTerritoryBuilds() == builds, "Unchanged camera rebuilt retained territory");
        require(map.getRetainedTerritoryHits() == hits + 1, "Unchanged camera did not reuse retained territory");
        require(map.getRetainedTerritoryFallbacks() > 0, "No fractional-pan fallback was exercised");
        tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(false, false,
              ExperimentalMapView.BoundaryDetail.OFF));
        map.captureTerritoryPng(true, 1);
        require(!map.hasRetainedTerritories(), "Disabling territory retained its cache");
        tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(true, false,
              ExperimentalMapView.BoundaryDetail.OFF));
        map.captureTerritoryPng(true, 1);
        require(map.hasRetainedTerritories(), "Re-enabling territory did not rebuild its cache");
        toggle.doClick(0);
        require(!map.hasRetainedTerritories() && SkiaMap.getActiveSurfaces() == 0
              && SkiaMap.getActiveTerritoryPaths() == 0, "Retained resources survived native disposal");
        System.out.printf(java.util.Locale.ROOT,
              "RETAINED_MAP_SMOKE_COMPLETE failures=0 fixtures=%d worstMean=%.4f worstChanged=%.6f builds=%d%n",
              fixtures, worstMean, worstChanged, builds);
        frame.dispose();
        System.exit(0);
    }

    private static void checkMotionTerritories(JFrame frame, MapTab tab, JCheckBoxMenuItem toggle) throws Exception {
        SkiaMap map = find(frame, SkiaMap.class);
        ViewState original = map.getViewState();
        tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(true, false,
              ExperimentalMapView.BoundaryDetail.OFF));
        int fixtures = 0;
        double worstMean = 0;
        for (double scale : new double[] { 0.8, 6 }) {
            for (float displayScale : new float[] { 1, 1.75f }) {
                for (double pan : new double[] { 0, 32, 80.25, 32, 220, -60 }) {
                    map.setViewState(new ViewState(original.centerX() + pan / scale,
                          original.centerY() + pan / (2 * scale), scale, original.selectedSystem()));
                    byte[] direct = map.captureTerritoryPng(false, displayScale);
                    BufferedImage reference = ImageIO.read(new ByteArrayInputStream(direct));
                    int builds = map.getRetainedTerritoryBuilds();
                    int hits = map.getRetainedTerritoryHits();
                    BufferedImage moving = ImageIO.read(new ByteArrayInputStream(map.captureTerritoryPng(true, displayScale)));
                    require(map.hasRetainedTerritories(), "Motion cache was not built");
                    require(map.getRetainedTerritoryBuilds() > builds || map.getRetainedTerritoryHits() > hits,
                          "Moving camera neither rebuilt nor reused the tile");
                    require(map.getRetainedTerritoryFallbacks() == 0, "Motion cache rejected fractional translation");
                    long error = 0;
                    for (int vertical = 0; vertical < reference.getHeight(); vertical++) {
                        for (int horizontal = 0; horizontal < reference.getWidth(); horizontal++) {
                            int expected = reference.getRGB(horizontal, vertical);
                            int actual = moving.getRGB(horizontal, vertical);
                            for (int shift = 0; shift <= 16; shift += 8) {
                                error += Math.abs((expected >> shift & 255) - (actual >> shift & 255));
                            }
                        }
                    }
                    double mean = error / (3.0 * reference.getWidth() * reference.getHeight());
                    worstMean = Math.max(worstMean, mean);
                    require(mean < 3, "Motion tile content mismatch: " + mean);
                    map.finishCameraMotion();
                    require(java.util.Arrays.equals(direct, map.captureTerritoryPng(true, displayScale)),
                          "Settled territory rendering differed from direct vectors");
                    fixtures++;
                }
            }
        }
        require(map.getRetainedTerritoryHits() > 0, "Motion fixtures never reused the cache");
          ViewState base = new ViewState(original.centerX(), original.centerY(), 6, original.selectedSystem());
          map.setViewState(base);
          map.captureTerritoryPng(true, 1.75f);
          int zoomBuilds = map.getRetainedTerritoryBuilds();
          map.setViewState(new ViewState(base.centerX(), base.centerY(), base.scale() * 1.175, base.selectedSystem()));
          map.captureTerritoryPng(true, 1.75f);
          require(map.getRetainedTerritoryBuilds() == zoomBuilds, "Small zoom rebuilt the territory tile");
          map.finishCameraMotion();
          require(java.util.Arrays.equals(map.captureTerritoryPng(false, 1.75f), map.captureTerritoryPng(true, 1.75f)),
              "Zoom settle differed from direct vectors");
          java.awt.Dimension originalSize = map.getSize();
          try {
            map.setSize(UIUtil.scaleForGUI(1800), UIUtil.scaleForGUI(1100));
            map.setViewState(base);
            BufferedImage large = ImageIO.read(new ByteArrayInputStream(map.captureTerritoryPng(true, 1.75f)));
            require(map.hasRetainedTerritories() && large.getWidth() > 2048,
                "Large viewport disabled the motion cache");
            map.finishCameraMotion();
            require(java.util.Arrays.equals(map.captureTerritoryPng(false, 1.75f), map.captureTerritoryPng(true, 1.75f)),
                "Large viewport settle differed from direct vectors");
          } finally {
            map.setSize(originalSize);
          }
          tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(true, true,
              ExperimentalMapView.BoundaryDetail.DISTRICTS));
        javax.swing.AbstractButton layerButton = utilityButton(tab, "mapHud.layers.text");
        layerButton.doClick(0);
        javax.swing.AbstractButton hpg = utilityButton(utilityDialog(frame, "mapHud.layers.text"),
              "map.overlay.hpgNetwork.text");
        require(hpg != null, "Missing HPG overlay control");
          if (!hpg.isSelected()) {
            hpg.doClick(0);
          }
        layerButton.doClick(0);
          map.setViewState(new ViewState(base.centerX() + 1, base.centerY(), base.scale(), base.selectedSystem()));
          map.captureNativePng();
          int cartographyBuilds = map.getCartographyMotionBuilds();
          int systemBuilds = map.getSystemMotionBuilds();
          map.setViewState(new ViewState(base.centerX() + 1.25, base.centerY() + 0.5,
              base.scale() * 1.175, base.selectedSystem()));
          BufferedImage movingScene = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
          require(map.getCartographyMotionBuilds() == cartographyBuilds && map.getSystemMotionBuilds() == systemBuilds,
              "Small pan/zoom rebuilt the cartography or system layer");
          map.finishCameraMotion();
          BufferedImage settledScene = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
          long sceneError = 0;
          for (int vertical = 0; vertical < movingScene.getHeight(); vertical++) {
            for (int horizontal = 0; horizontal < movingScene.getWidth(); horizontal++) {
                int movingPixel = movingScene.getRGB(horizontal, vertical);
                int settledPixel = settledScene.getRGB(horizontal, vertical);
                for (int shift = 0; shift <= 16; shift += 8) {
                  sceneError += Math.abs((movingPixel >> shift & 255) - (settledPixel >> shift & 255));
                }
            }
          }
          double sceneMean = sceneError / (3.0 * movingScene.getWidth() * movingScene.getHeight());
          require(sceneMean < 6, "Layered motion scene content mismatch: " + sceneMean);
          tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(false, false,
              ExperimentalMapView.BoundaryDetail.OFF));
          hpg.doClick(0);
          map.setViewState(new ViewState(base.centerX() + 2, base.centerY(), base.scale(), base.selectedSystem()));
          map.captureNativePng();
          require(map.getCartographyMotionBuilds() > cartographyBuilds, "Layer changes retained stale cartography");
          System.out.printf(java.util.Locale.ROOT,
              "MOTION_LAYER_CHECK zoom=reused oversized=retained sceneMean=%.4f invalidation=passed %s%n",
              sceneMean, map.getRetainedTerritoryInfo());
          int completedFixtures = fixtures;
          double maximumMean = worstMean;
          ViewState last = map.getViewState();
          map.setViewState(new ViewState(last.centerX() + 0.25, last.centerY(), last.scale(), last.selectedSystem()));
          require(map.isCameraMoving(), "Camera change did not start motion state");
          Timer settled = new Timer(400, event -> {
            try {
                require(!map.isCameraMoving(), "Camera inactivity did not settle to vectors");
                require(java.util.Arrays.equals(map.captureTerritoryPng(false, 1), map.captureTerritoryPng(true, 1)),
                    "Timer-settled territory rendering differed from direct vectors");
                tab.setCartographyLayers(new ExperimentalMapView.CartographyLayers(false, false,
                    ExperimentalMapView.BoundaryDetail.OFF));
                map.captureTerritoryPng(true, 1);
                require(!map.hasRetainedTerritories(), "Disabling territory retained the motion cache");
                map.setViewState(last);
                toggle.doClick(0);
                    require(!map.isCameraMoving() && !map.hasMotionLayers()
                        && SkiaMap.getActiveSurfaces() == 0 && SkiaMap.getActiveTerritoryPaths() == 0,
                    "Motion resources survived native disposal");
                System.out.printf(java.util.Locale.ROOT,
                    "MOTION_MAP_SMOKE_COMPLETE fixtures=%d worstMovingMean=%.4f settled=exact timer=passed %s%n",
                    completedFixtures, maximumMean, map.getRetainedTerritoryInfo());
                frame.dispose();
                System.exit(0);
            } catch (Exception exception) {
                exception.printStackTrace();
                frame.dispose();
                System.exit(1);
            }
          });
          settled.setRepeats(false);
          settled.start();
    }

    private static javax.swing.AbstractButton utilityButton(Container parent, String key) {
        String text = mapText(key);
        for (Component child : parent.getComponents()) {
            if (child instanceof javax.swing.AbstractButton button
                && (!key.startsWith("map.overlay.") || button instanceof javax.swing.JCheckBox)
                && (!key.startsWith("map.layer.") || button instanceof javax.swing.JRadioButton)
                  && (text.equals(button.getText()) || text.equals(button.getAccessibleContext().getAccessibleName()))) {
                return button;
            }
            if (child instanceof Container container) {
                var found = utilityButton(container, key);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JDialog utilityDialog(JFrame frame, String key) {
        for (var window : frame.getOwnedWindows()) {
            if (window instanceof javax.swing.JDialog dialog && dialog.isShowing()
                  && mapText(key).equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static boolean hasLegendEntry(Container parent, String key) {
        for (Component child : parent.getComponents()) {
            if (child instanceof javax.swing.JComponent component
                  && mapText(key).equals(component.getClientProperty("mapLegendTitle"))) {
                return true;
            }
            if (child instanceof Container container && hasLegendEntry(container, key)) {
                return true;
            }
        }
        return false;
    }

    private static void captureUtility(javax.swing.JDialog dialog, String name) throws Exception {
        BufferedImage image;
        if (offscreen) {
            image = new BufferedImage(dialog.getWidth(), dialog.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            dialog.printAll(graphics);
            graphics.dispose();
        } else {
            image = new Robot().createScreenCapture(dialog.getBounds());
        }
        ImageIO.write(image, "png", output(name).toFile());
    }

    private static void checkMapUtilities(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        var tab = app.getCampaigngui().getNavigationTab().getMapTab();
        var originalMode = tab.getMapMode();
        var layers = Objects.requireNonNull(utilityButton(tab, "mapHud.layers.text"));
        var legend = Objects.requireNonNull(utilityButton(tab, "map.legend.button.text"));
        require(layers.isEnabled() && legend.isEnabled(), "Native map utility buttons disabled");
        layers.doClick(0);
        AtomicInteger phase = new AtomicInteger();
        javax.swing.AbstractButton[] originalControl = new javax.swing.AbstractButton[1];
        Timer checks = new Timer(650, event -> {
            try {
                switch (phase.incrementAndGet()) {
                    case 1 -> {
                        var dialog = Objects.requireNonNull(utilityDialog(frame, "mapHud.layers.text"));
                        require(dialog.getOwner() == frame, "Layers window has wrong owner");
                        for (String key : List.of("map.overlay.operations.text")) {
                            require(!utilityButton(dialog, key).isShowing(), "Unsupported native control shown: " + key);
                        }
                        originalControl[0] = utilityButton(dialog, "map.layer.population.text");
                        originalControl[0].doClick(0);
                        require(find(frame, SkiaMap.class).getPresentation().mapMode()
                              == InterstellarMapPanel.MapMode.POPULATION, "Layers mode action did not refresh native map");
                        for (String key : List.of("map.overlay.emptySystems.text", "map.overlay.territory.text",
                            "map.overlay.emblems.text", "map.overlay.administrative.text",
                            "map.overlay.rechargeStations.text", "map.overlay.capitals.text", "map.overlay.reachability.text",
                            "map.overlay.hpgNetwork.text")) {
                            var control = Objects.requireNonNull(utilityButton(dialog, key));
                            var before = find(frame, SkiaMap.class).getPresentation();
                            control.doClick(0);
                            require(!before.equals(find(frame, SkiaMap.class).getPresentation()),
                                "Layers overlay action did not refresh native map: " + key);
                            control.doClick(0);
                            require(before.equals(find(frame, SkiaMap.class).getPresentation()),
                                "Layers overlay action did not restore state: " + key);
                        }
                        var measure = utilityButton(dialog, "map.overlay.measure.text");
                        measure.doClick(0);
                        require(find(frame, SkiaMap.class).getPresentation().navigation().measurement().enabled(),
                              "Layers measurement action did not refresh native map");
                        measure.doClick(0);
                        captureUtility(dialog, "skia-layers-window");
                        var detail = find(utilityButton(dialog, "map.overlay.capitals.text").getParent(),
                            javax.swing.JComboBox.class);
                        detail.showPopup();
                    }
                    case 2 -> {
                        var dialog = Objects.requireNonNull(utilityDialog(frame, "mapHud.layers.text"));
                        var detail = find(utilityButton(dialog, "map.overlay.capitals.text").getParent(),
                            javax.swing.JComboBox.class);
                        require(detail.isPopupVisible(), "Layers detail popup did not open");
                        captureUtility(dialog, "skia-layers-popup");
                        detail.hidePopup();
                        var before = find(frame, SkiaMap.class).getPresentation();
                        int selectedIndex = detail.getSelectedIndex();
                        detail.setSelectedIndex(detail.getItemCount() - 1);
                        require(!before.equals(find(frame, SkiaMap.class).getPresentation()),
                            "Layers detail action did not refresh native map");
                        detail.setSelectedIndex(selectedIndex);
                        legend.doClick(0);
                      }
                      case 3 -> {
                        var dialog = Objects.requireNonNull(utilityDialog(frame, "map.legend.dialog.title"));
                        require(dialog.getOwner() == frame, "Legend window has wrong owner");
                        require(hasLegendEntry(dialog, "map.legend.population.title"), "Analytical legend missing");
                        require(hasLegendEntry(dialog, "map.legend.reachability.title"), "Reachability legend missing");
                        require(hasLegendEntry(dialog, "map.legend.hpgNetwork.title"), "HPG links legend missing");
                        require(hasLegendEntry(dialog, "map.legend.hpgStations.title"), "HPG station legend missing");
                        for (String key : List.of("map.legend.operation.title", "map.legend.playerBase.title")) {
                            require(!hasLegendEntry(dialog, key), "Unsupported native legend entry shown: " + key);
                        }
                        captureUtility(dialog, "skia-legend-window");
                        layers.doClick(0);
                        require(dialog.isShowing(), "Closing Layers also closed legend");
                        dialog.getRootPane().getActionMap().get("closeMapLegendDialog")
                              .actionPerformed(new java.awt.event.ActionEvent(dialog, 0, "escape"));
                    }
                    case 4 -> {
                        require(utilityDialog(frame, "mapHud.layers.text") == null
                              && utilityDialog(frame, "map.legend.dialog.title") == null, "Utility dismissal failed");
                        layers.doClick(0);
                        legend.doClick(0);
                        toggle.doClick(0);
                        require(utilityDialog(frame, "mapHud.layers.text") == null
                              && utilityDialog(frame, "map.legend.dialog.title") == null, "Switch left utility windows open");
                        layers.doClick(0);
                    }
                    case 5 -> {
                        var java2d = find(frame, InterstellarMapPanel.class);
                        require(utilityButton(java2d, "map.layer.population.text") == originalControl[0],
                              "Renderer switch duplicated layer controls");
                        require(originalControl[0].isShowing() && originalControl[0].isSelected(),
                              "Java2D drawer lost controls or selection");
                        require(utilityButton(java2d, "map.overlay.hpgNetwork.text").isShowing(),
                              "Java2D-only control was not restored");
                        legend.doClick(0);
                        require(hasLegendEntry(utilityDialog(frame, "map.legend.dialog.title"), "map.legend.reachability.title"),
                              "Java2D legend remained filtered");
                        toggle.doClick(0);
                        layers.doClick(0);
                        legend.doClick(0);
                        app.getCampaigngui().setSelectedTab(MHQTabType.PERSONNEL);
                    }
                    case 6 -> {
                        require(utilityDialog(frame, "mapHud.layers.text") == null
                              && utilityDialog(frame, "map.legend.dialog.title") == null, "Hidden map retained utility windows");
                        app.getCampaigngui().setSelectedTab(MHQTabType.NAVIGATION);
                        tab.setMapMode(originalMode);
                        layers.doClick(0);
                        var dialog = Objects.requireNonNull(utilityDialog(frame, "mapHud.layers.text"));
                        dialog.getRootPane().getActionMap().get("closeMapLegendDialog")
                            .actionPerformed(new java.awt.event.ActionEvent(dialog, 0, "escape"));
                      }
                      case 7 -> {
                        require(utilityDialog(frame, "mapHud.layers.text") == null, "Layers Escape dismissal failed");
                        layers.doClick(0);
                        var dialog = Objects.requireNonNull(utilityDialog(frame, "mapHud.layers.text"));
                        require(utilityButton(dialog, "map.layer.population.text") == originalControl[0],
                            "Layers controls were not restored after Escape");
                        dialog.dispose();
                      }
                      case 8 -> {
                        require(utilityDialog(frame, "mapHud.layers.text") == null, "Layers window disposal failed");
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_CONTROLS_CHECK layers=shared legend=filtered measurement=synchronized"
                            + " overlays=checked popup=checked ownership=checked dismissal=checked restoration=checked");
                        if (controlsOnly) {
                            frame.dispose();
                            System.out.println("CONTROLS_MAP_SMOKE_COMPLETE failures=0");
                            System.exit(0);
                        } else {
                            checkHpgNetwork(frame, app, toggle);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected map utility phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void compareHpgSnapshot(SkiaMap map, Campaign campaign) {
        var network = Objects.requireNonNull(map.getPresentation().navigation().hpgNetwork());
        require(network.date().equals(campaign.getLocalDate()), "HPG snapshot date stale");
        require(new HashSet<>(network.links()).equals(new HashSet<>(Systems.getInstance().getHPGNetwork(campaign.getLocalDate()))),
              "Native HPG links differ from Java2D network provider");
        List<ExperimentalMapView.HpgStation> expected = new ArrayList<>();
        for (var data : map.getPresentation().systems()) {
            var rating = data.system().getHPG(campaign.getLocalDate());
            if (rating != null && rating != mekhq.campaign.universe.enums.HPGRating.X) {
                expected.add(new ExperimentalMapView.HpgStation(data.system(), rating));
            }
        }
        require(network.stations().equals(expected), "Native HPG stations differ from dated system ratings");
        map.refresh();
        require(network == map.getPresentation().navigation().hpgNetwork(), "Unchanged HPG snapshot was rebuilt");
    }

    private static PlanetarySystem hpgFixture(SkiaMap map, String rating) {
        for (var station : map.getPresentation().navigation().hpgNetwork().stations()) {
            if (!station.rating().name().equals(rating)) {
                continue;
            }
            boolean isolated = true;
            for (var data : map.getPresentation().systems()) {
                if (!data.system().equals(station.system()) && data.system().getDistanceTo(station.system()) < 7) {
                    isolated = false;
                    break;
                }
            }
            if (isolated) {
                return station.system();
            }
        }
        throw new IllegalStateException("No isolated real HPG fixture for class " + rating);
    }

    private static void checkHpgBadgePixels(SkiaMap map, PlanetarySystem system, String rating) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
        var station = map.getHpgStationPlacements().stream().filter(marker -> marker.systemId().equals(system.getId()))
              .findFirst().orElseThrow(() -> new IllegalStateException("HPG station badge missing: " + rating));
        require(station.rating().equals(rating) && station.alpha() == 1, "Wrong HPG class or badge opacity");
        int expectedColor = switch (rating) {
            case "A" -> 0x59E2EE;
            case "B" -> 0x69AFFF;
            case "C" -> 0xF2B848;
            default -> 0xEA5656;
        };
        int colored = 0;
        int dark = 0;
        Rect bounds = station.bounds();
        for (int vertical = (int) bounds.getTop() + 3; vertical < bounds.getBottom() - 3; vertical++) {
            for (int horizontal = (int) bounds.getLeft() + 3; horizontal < bounds.getRight() - 3; horizontal++) {
                int color = image.getRGB(horizontal, vertical);
                if (nearColor(color, expectedColor)) {
                    colored++;
                }
                if ((color & 0xFFFFFF) < 0x202020) {
                    dark++;
                }
            }
        }
        require(colored > 8 && dark > 2, "HPG badge fill or class glyph missing: " + rating);
        checkLabelAnchors(map);
    }

    private static BufferedImage hpgDashRaster(java.awt.geom.Line2D.Float line, float displayScale,
          boolean optimized) throws Exception {
        try (var surface = org.jetbrains.skia.Surface.Companion.makeRasterN32Premul(
              (int) (160 * displayScale), (int) (120 * displayScale));
              var paint = new org.jetbrains.skia.Paint();
              var dash = org.jetbrains.skia.PathEffect.Companion.makeDash(new float[] { 8, 8 }, 0)) {
            var canvas = surface.getCanvas();
            canvas.clear(0xFF101010);
            canvas.scale(displayScale, displayScale);
            paint.setAntiAlias(true);
            paint.setColor(0x695684CD);
            paint.setStrokeWidth(0.8f);
            paint.setStrokeCap(org.jetbrains.skia.PaintStrokeCap.BUTT);
            if (optimized) {
                var clipped = SkiaMap.clipHpgDash(line, new Rectangle2D.Float(-2, -2, 164, 124), 16);
                if (clipped != null) {
                    SkiaMap.drawHpgDashes(canvas, clipped, 8, paint);
                }
            } else {
                paint.setPathEffect(dash);
                canvas.drawLine(line.x1, line.y1, line.x2, line.y2, paint);
            }
            try (var image = surface.makeImageSnapshot();
                  var encoded = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG, 100, 0)) {
                return ImageIO.read(new ByteArrayInputStream(Objects.requireNonNull(encoded).getBytes()));
            }
        }
    }

    private static void checkHpgDashPixels() {
        float[][] fixtures = {
              { -4000, 60, 4000, 60 }, { 80, -4000, 80, 4000 },
              { -4000, -2000, 4000, 2100 }, { 4000, 2100, -4000, -2000 },
              { 5.5f, 6.25f, 131.75f, 103.5f }, { 131.75f, 103.5f, 5.5f, 6.25f },
              { -200, -200, -100, -100 }, { 30, 30, 30, 30 },
              { -4000, -2, 4000, -2 }, { 0, 119.75f, 159.5f, 119.75f }
        };
        try {
            for (float scale : new float[] { 1, 1.75f }) {
                for (float[] fixture : fixtures) {
                    var line = new java.awt.geom.Line2D.Float(fixture[0], fixture[1], fixture[2], fixture[3]);
                    BufferedImage reference = hpgDashRaster(line, scale, false);
                    BufferedImage optimized = hpgDashRaster(line, scale, true);
                    int changed = 0;
                    long difference = 0;
                    for (int vertical = 0; vertical < reference.getHeight(); vertical++) {
                        for (int horizontal = 0; horizontal < reference.getWidth(); horizontal++) {
                            int expected = reference.getRGB(horizontal, vertical);
                            int actual = optimized.getRGB(horizontal, vertical);
                            int largest = 0;
                            for (int shift : new int[] { 0, 8, 16 }) {
                                int delta = Math.abs(((expected >> shift) & 255) - ((actual >> shift) & 255));
                                difference += delta;
                                largest = Math.max(largest, delta);
                            }
                            changed += largest > 8 ? 1 : 0;
                        }
                    }
                    require(changed < 20 && difference < reference.getWidth() * reference.getHeight(),
                          "HPG dash phase/raster changed: " + line + " scale=" + scale
                                + " changed=" + changed + " channelDifference=" + difference);
                }
            }
            System.out.println("SKIA_HPG_DASH_CHECK fixtures=10 scales=1,1.75 phase=preserved raster=compared");
        } catch (Exception exception) {
            throw new IllegalStateException("HPG dash raster comparison failed", exception);
        }
    }

    private static void checkHpgNetwork(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        checkHpgDashPixels();
        Campaign campaign = app.getCampaigngui().getCampaign();
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        SkiaMap initial = find(frame, SkiaMap.class);
        ViewState original = initial.getViewState();
        var originalDate = campaign.getLocalDate();
        boolean originalEmpty = tab.isShowingEmptySystems();
        var layers = utilityButton(tab, "mapHud.layers.text");
        layers.doClick(0);
        var dialog = utilityDialog(frame, "mapHud.layers.text");
        var enabled = utilityButton(dialog, "map.overlay.hpgNetwork.text");
        var selector = find(enabled.getParent(), javax.swing.JComboBox.class);
        require(enabled.isShowing() && !enabled.isSelected() && !selector.isEnabled(), "HPG defaults wrong");
        enabled.doClick(0);
        require(selector.isEnabled() && selector.getSelectedIndex() == 1, "HPG default detail wrong");
        tab.setShowingEmptySystems(true);
        PlanetarySystem anchor = hpgFixture(initial, "A");
        initial.setViewState(new ViewState(anchor.getX(), anchor.getY(), 0.6, original.selectedSystem()));
        layers.doClick(0);
        AtomicInteger phase = new AtomicInteger();
        PlanetarySystem[] fixture = new PlanetarySystem[1];
        Timer checks = new Timer(750, event -> {
            try {
                SkiaMap map = find(frame, SkiaMap.class);
                int step = phase.incrementAndGet();
                switch (step) {
                    case 1 -> {
                        compareHpgSnapshot(map, campaign);
                        map.captureNativePng();
                        require(!map.getHpgLinkPlacements().isEmpty(), "Overview HPG network blank");
                        require(map.getHpgLinkPlacements().stream().allMatch(link -> link.rating().equals("A") && !link.dashed()),
                              "Overview displayed lower-class links");
                        require(map.getHpgStationPlacements().isEmpty(), "Overview displayed HPG station badges");
                        capture(frame, "skia-hpg-overview");
                        map.setViewState(new ViewState(anchor.getX(), anchor.getY(), 3, original.selectedSystem()));
                    }
                    case 2 -> {
                        map.captureNativePng();
                        require(map.getHpgLinkPlacements().stream().anyMatch(link -> link.rating().equals("B") && link.dashed()),
                              "Medium zoom did not reveal Class B links");
                        require(map.getHpgStationPlacements().stream().allMatch(marker -> marker.rating().equals("A")),
                              "Lower-class station badges appeared before close detail");
                        for (var link : map.getHpgLinkPlacements()) {
                            boolean classA = link.rating().equals("A");
                            require(classA || link.rating().equals("B"), "HPG linked a non-network station class");
                            require(link.dashed() != classA
                                && Math.abs(link.width() - (classA ? 1.35f : 0.8f) * UIUtil.scaleForGUI(1)) < 0.001,
                                "HPG link stroke differs from shared style");
                        }
                        BufferedImage allLinks = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
                        selector.setSelectedIndex(0);
                        BufferedImage classALinks = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
                        require(changedPixels(allLinks, classALinks) > 40, "Class B links did not contribute native pixels");
                        require(map.getHpgLinkPlacements().stream().allMatch(link -> link.rating().equals("A")),
                              "A-only control retained B links");
                        selector.setSelectedIndex(2);
                        fixture[0] = hpgFixture(map, "A");
                        map.setViewState(new ViewState(fixture[0].getX(), fixture[0].getY(), 6, original.selectedSystem()));
                    }
                    case 3, 4, 5, 6 -> {
                        String rating = List.of("A", "B", "C", "D").get(step - 3);
                        checkHpgBadgePixels(map, fixture[0], rating);
                        capture(frame, "skia-hpg-station-" + rating.toLowerCase(java.util.Locale.ROOT));
                        if (step < 6) {
                            fixture[0] = hpgFixture(map, List.of("A", "B", "C", "D").get(step - 2));
                            map.setViewState(new ViewState(fixture[0].getX(), fixture[0].getY(), 6, original.selectedSystem()));
                        } else {
                            selector.setSelectedIndex(1);
                            map.captureNativePng();
                            require(map.getHpgStationPlacements().stream().noneMatch(marker -> marker.rating().equals("C")
                                  || marker.rating().equals("D")), "A-B detail retained C/D badges");
                            selector.setSelectedIndex(2);
                            campaign.setLocalDate(originalDate.minusYears(100));
                            map.refresh();
                        }
                    }
                    case 7 -> {
                        compareHpgSnapshot(map, campaign);
                        capture(frame, "skia-hpg-historical");
                        campaign.setLocalDate(originalDate);
                        map.refresh();
                        compareHpgSnapshot(map, campaign);
                        toggle.doClick(0);
                        var java2d = find(frame, InterstellarMapPanel.class);
                        require(utilityButton(java2d, "map.overlay.hpgNetwork.text") == enabled && enabled.isSelected(),
                              "Renderer switch lost HPG control or state");
                        require(selector.getSelectedIndex() == 2, "Renderer switch lost HPG detail");
                        toggle.doClick(0);
                    }
                    case 8 -> {
                        compareHpgSnapshot(map, campaign);
                        fixture[0] = hpgFixture(map, "A");
                        map.setViewState(new ViewState(fixture[0].getX(), fixture[0].getY(), 1.6, original.selectedSystem()));
                    }
                    case 9 -> {
                        map.captureNativePng();
                        require(map.getHpgStationPlacements().stream().anyMatch(marker -> marker.alpha() > 0 && marker.alpha() < 1),
                              "HPG station zoom did not crossfade");
                        map.setViewState(new ViewState(anchor.getX(), anchor.getY(), 6, original.selectedSystem()));
                        tab.setShowingEmptySystems(false);
                        map.captureNativePng();
                        for (var marker : map.getHpgStationPlacements()) {
                            var data = map.getPresentation().systems().stream()
                                .filter(candidate -> candidate.system().getId().equals(marker.systemId())).findFirst().orElseThrow();
                            require(!data.empty() || data.system().equals(original.selectedSystem())
                                || data.system().equals(campaign.getCurrentSystem())
                                || map.getPresentation().routeSystemIds().contains(marker.systemId()),
                                "HPG badge ignored empty-system visibility");
                        }
                        tab.setShowingEmptySystems(true);
                        BufferedImage before = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
                        enabled.doClick(0);
                        BufferedImage after = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
                        require(changedPixels(before, after) > 100, "HPG toggle did not change native pixels");
                        require(map.getPresentation().navigation().hpgNetwork() == null
                              && map.getHpgLinkPlacements().isEmpty() && map.getHpgStationPlacements().isEmpty(),
                              "Disabled HPG overlay retained native content");
                        require(!selector.isEnabled(), "Disabled HPG overlay retained detail control");
                        selector.setSelectedIndex(1);
                        tab.setShowingEmptySystems(originalEmpty);
                        map.setViewState(original);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_HPG_CHECK links=dated stations=A,B,C,D pixels=checked detail=shared"
                            + " zoom=crossfaded cache=reused labels=clear filters=shared roundtrip=preserved disable=cleared");
                        if (hpgOnly) {
                            frame.dispose();
                            System.out.println("HPG_MAP_SMOKE_COMPLETE failures=0");
                            System.exit(0);
                        } else {
                            checkReachability(frame, app, toggle);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected HPG phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void compareReachability(SkiaMap map, Campaign campaign, PlanetarySystem anchor, int hops) {
        var snapshot = Objects.requireNonNull(map.getPresentation().navigation().reachability());
        require(snapshot.anchor().equals(anchor) && snapshot.maximumHops() == hops, "Reachability anchor/hops stale");
        var expected = campaign.calculateNavigationReachability(anchor, hops, campaign.isUseCommandCircuit());
        List<ExperimentalMapView.ReachabilityEntry> entries = new ArrayList<>();
        for (var entry : expected.reachableSystems()) {
            entries.add(new ExperimentalMapView.ReachabilityEntry(entry.system(), entry.minimumHops(),
                  entry.arrivalAssessment().severity() == mekhq.campaign.NavigationRouteAnalysis.Severity.CAUTION,
                  entry.arrivalAssessment().severity() == mekhq.campaign.NavigationRouteAnalysis.Severity.BLOCKED));
        }
        for (var entry : expected.blockedFrontier()) {
            entries.add(new ExperimentalMapView.ReachabilityEntry(entry.system(), entry.minimumHops(), false, true));
        }
        require(snapshot.entries().equals(entries), "Native reachability differs from campaign calculation");
        require(snapshot.label().contains(anchor.getPrintableName(campaign.getLocalDate())), "Reachability label stale");
        map.refresh();
        require(snapshot == map.getPresentation().navigation().reachability(), "Unchanged reachability snapshot not reused");
    }

    private static void checkReachabilityPixels(SkiaMap map, int hops) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
        require(!map.getReachabilityPlacements().isEmpty(), "No native reachability markers drawn");
        Set<Integer> shells = new HashSet<>();
        int coloredPixels = 0;
        int blockedMarkers = 0;
        for (var marker : map.getReachabilityPlacements()) {
            if (marker.blocked()) {
                blockedMarkers++;
            }
            String shape = marker.blocked() ? "DIAMOND" : marker.caution() ? "TRIANGLE"
                  : switch (marker.minimumHops()) { case 1 -> "CIRCLE"; case 2 -> "SQUARE"; default -> "HEXAGON"; };
            require(marker.shape().equals(shape), "Reachability marker shape mismatch");
            require(marker.minimumHops() <= hops && marker.alpha() == 1, "Wrong reachability shell or detail fade");
            shells.add(marker.minimumHops());
            var bounds = marker.bounds();
            for (int vertical = Math.max(0, (int) bounds.getTop()); vertical < Math.min(image.getHeight(), bounds.getBottom()); vertical++) {
                for (int horizontal = Math.max(0, (int) bounds.getLeft()); horizontal < Math.min(image.getWidth(), bounds.getRight()); horizontal++) {
                    int expectedColor = 0;
                    for (int shift = 0; shift <= 16; shift += 8) {
                        expectedColor |= Math.round(((marker.color() >>> shift) & 255)
                              * (marker.blocked() ? 225 : 185) / 255.0f) << shift;
                    }
                    if (nearColor(image.getRGB(horizontal, vertical), expectedColor)) {
                        coloredPixels++;
                    }
                }
            }
        }
        checkLabelAnchors(map);
        require(shells.contains(hops), "Outermost requested hop shell not drawn");
        require(coloredPixels > 30, "Native reachability marker colors absent");
        if (hops == 3) {
            require(blockedMarkers > 0, "Fixture did not exercise blocked frontier markers");
        }
    }

    private static boolean overlaps(Rect first, Rect second) {
        return first.getRight() > second.getLeft() && first.getLeft() < second.getRight()
              && first.getBottom() > second.getTop() && first.getTop() < second.getBottom();
    }

    private static void checkReachability(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        Campaign campaign = app.getCampaigngui().getCampaign();
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        SkiaMap initial = find(frame, SkiaMap.class);
        ViewState original = initial.getViewState();
        boolean originalEmpty = tab.isShowingEmptySystems();
        PlanetarySystem anchor = campaign.getCurrentSystem();
        var layers = utilityButton(tab, "mapHud.layers.text");
        layers.doClick(0);
        var dialog = utilityDialog(frame, "mapHud.layers.text");
        var enabled = utilityButton(dialog, "map.overlay.reachability.text");
        var spinner = find(enabled.getParent(), javax.swing.JSpinner.class);
        require(enabled.isShowing() && !enabled.isSelected() && !spinner.isEnabled(), "Native reachability defaults wrong");
        app.getCampaigngui().focusOnSystem(anchor);
        tab.setShowingEmptySystems(true);
        initial.setViewState(new ViewState(anchor.getX(), anchor.getY(), 4, anchor));
        enabled.doClick(0);
        require(spinner.isEnabled(), "Reachability hops not enabled");
        layers.doClick(0);
        AtomicInteger phase = new AtomicInteger();
        PlanetarySystem[] changedAnchor = new PlanetarySystem[1];
        boolean[] originalAvoidance = new boolean[1];
        Timer checks = new Timer(750, event -> {
            try {
                SkiaMap map = find(frame, SkiaMap.class);
                int step = phase.incrementAndGet();
                switch (step) {
                    case 1, 2, 3 -> {
                        compareReachability(map, campaign, anchor, step);
                        checkReachabilityPixels(map, step);
                        capture(frame, "skia-reachability-" + step + "-hops");
                        if (step < 3) {
                            spinner.setValue(step + 1);
                        } else {
                            tab.setShowingEmptySystems(false);
                        }
                    }
                    case 4 -> {
                        map.captureNativePng();
                        for (var marker : map.getReachabilityPlacements()) {
                            var data = map.getPresentation().systems().stream()
                                  .filter(candidate -> candidate.system().getId().equals(marker.systemId())).findFirst().orElseThrow();
                            require(!data.empty() || data.system().equals(anchor), "Hidden empty reachability marker drawn");
                        }
                        map.setViewState(new ViewState(anchor.getX(), anchor.getY(), 0.6, anchor));
                    }
                    case 5 -> {
                        map.captureNativePng();
                        require(map.getReachabilityPlacements().isEmpty() && map.getReachabilityAnnotationBounds() == null,
                              "Reachability visible at overview zoom");
                        map.setViewState(new ViewState(anchor.getX(), anchor.getY(), 1.6, anchor));
                    }
                    case 6 -> {
                        map.captureNativePng();
                        require(!map.getReachabilityPlacements().isEmpty(), "Reachability missing during zoom crossfade");
                        require(map.getReachabilityPlacements().getFirst().alpha() > 0
                              && map.getReachabilityPlacements().getFirst().alpha() < 1, "Reachability did not crossfade");
                        changedAnchor[0] = map.getPresentation().navigation().reachability().entries().stream()
                              .filter(entry -> !entry.blocked() && !entry.system().equals(anchor)).findFirst().orElseThrow().system();
                        app.getCampaigngui().focusOnSystem(changedAnchor[0]);
                    }
                    case 7 -> {
                        compareReachability(map, campaign, changedAnchor[0], 3);
                        var before = map.getPresentation().navigation().reachability();
                        var circuit = utilityButton(tab, "chkUseCommandCircuits.text");
                        require(circuit != null, "Routing constraint control unavailable");
                        circuit.doClick(0);
                        require(before != map.getPresentation().navigation().reachability(), "Routing constraint did not refresh reachability");
                        compareReachability(map, campaign, changedAnchor[0], 3);
                        circuit.doClick(0);
                        toggle.doClick(0);
                        require(utilityButton(find(frame, InterstellarMapPanel.class), "map.overlay.reachability.text").isSelected(),
                              "Java2D lost reachability selection");
                        toggle.doClick(0);
                    }
                    case 8 -> {
                        compareReachability(map, campaign, changedAnchor[0], 3);
                        var avoid = utilityButton(tab, "chkAvoidAbandonedSystems.text");
                        originalAvoidance[0] = avoid.isSelected();
                        if (avoid.isSelected()) {
                            avoid.doClick(0);
                        }
                        tab.setShowingEmptySystems(true);
                        compareReachability(map, campaign, changedAnchor[0], 3);
                        var caution = map.getPresentation().navigation().reachability().entries().stream()
                              .filter(ExperimentalMapView.ReachabilityEntry::caution).findFirst().orElseThrow(
                                    () -> new IllegalStateException("Fixture did not exercise abandoned-system caution"));
                        map.setViewState(new ViewState(caution.system().getX(), caution.system().getY(), 6,
                              changedAnchor[0]));
                    }
                    case 9 -> {
                        map.captureNativePng();
                        require(map.getReachabilityPlacements().stream().anyMatch(marker -> marker.caution()
                              && marker.shape().equals("TRIANGLE")), "Caution triangle was not drawn");
                        capture(frame, "skia-reachability-caution");
                        var avoid = utilityButton(tab, "chkAvoidAbandonedSystems.text");
                        if (avoid.isSelected() != originalAvoidance[0]) {
                            avoid.doClick(0);
                        }
                        enabled.doClick(0);
                        require(map.getPresentation().navigation().reachability() == null && !spinner.isEnabled(),
                              "Reachability disable did not clear snapshot");
                        map.captureNativePng();
                        require(map.getReachabilityPlacements().isEmpty(), "Disabled reachability still drawn");
                        spinner.setValue(1);
                        tab.setShowingEmptySystems(originalEmpty);
                        app.getCampaigngui().focusOnSystem(original.selectedSystem());
                        map.setViewState(original);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_REACHABILITY_CHECK hops=1,2,3 calculation=shared pixels=checked"
                            + " caution=checked blocked=checked anchor=updated constraints=updated zoom=crossfaded filters=shared roundtrip=preserved");
                        if (reachabilityOnly) {
                            frame.dispose();
                            System.out.println("REACHABILITY_MAP_SMOKE_COMPLETE failures=0");
                            System.exit(0);
                        } else {
                            checkAnalyticalModes(frame, app, toggle);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected reachability phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void checkAnalyticalModes(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        Campaign campaign = app.getCampaigngui().getCampaign();
        var originalDate = campaign.getLocalDate();
        var originalMode = tab.getMapMode();
        var originalView = find(frame, SkiaMap.class).getViewState();
        var modes = InterstellarMapPanel.MapMode.values();
        AtomicInteger phase = new AtomicInteger();
        Timer checks = new Timer(650, event -> {
            try {
                int index = phase.getAndIncrement();
                if (index >= modes.length * 2) {
                    campaign.setLocalDate(originalDate);
                    tab.setMapMode(originalMode);
                    SkiaMap map = find(frame, SkiaMap.class);
                    map.refresh();
                    map.setViewState(originalView);
                    ((Timer) event.getSource()).stop();
                    System.out.println("SKIA_ANALYTICAL_CHECK modes=" + modes.length
                          + " dates=compared pixels=checked controls=shared roundtrip=preserved");
                    if (modesOnly) {
                        frame.dispose();
                        System.out.println("ANALYTICAL_MAP_SMOKE_COMPLETE failures=0");
                        System.exit(0);
                    } else {
                        checkPresentation(frame, app, toggle);
                    }
                    return;
                }
                var mode = modes[index % modes.length];
                campaign.setLocalDate(index < modes.length ? originalDate : originalDate.minusYears(100));
                JMenu menu = null;
                String title = MHQInternationalization.getTextAt("mekhq.resources.CampaignGUI", "map.layer.heading.text");
                for (int menuIndex = 0; menuIndex < frame.getJMenuBar().getMenuCount(); menuIndex++) {
                    JMenuItem candidate = findMenuItem(frame.getJMenuBar().getMenu(menuIndex), title);
                    if (candidate instanceof JMenu found) {
                        menu = found;
                        break;
                    }
                }
                require(menu != null, "Missing shared analytical mode menu");
                JMenuItem item = findMenuItem(menu, MHQInternationalization.getTextAt(
                      "mekhq.resources.CampaignGUI", mode.resourceKey() + ".text"));
                require(item != null && item.isEnabled(), "Missing analytical mode action: " + mode);
                item.doClick(0);
                SkiaMap map = find(frame, SkiaMap.class);
                require(tab.getMapMode() == mode && map.getPresentation().mapMode() == mode,
                      "Analytical mode not synchronized: " + mode);
                    map.refresh();
                var snapshot = map.getPresentation();
                    map.refresh();
                    require(map.getPresentation().systems() == snapshot.systems(),
                        "Unchanged analytical snapshot was rebuilt");
                toggle.doClick(0);
                InterstellarMapPanel java2d = find(frame, InterstellarMapPanel.class);
                require(java2d.getSelectedMapMode() == mode, "Java2D lost analytical mode");
                if (mode != InterstellarMapPanel.MapMode.FACTION) {
                    for (var system : snapshot.systems()) {
                        require(system.analyticalColor() == java2d.getSystemColor(system.system()).getRGB(),
                              "Dated analytical color mismatch: " + mode + " " + system.system().getId());
                    }
                }
                toggle.doClick(0);
                map = find(frame, SkiaMap.class);
                map.refresh();
                require(map.getPresentation().mapMode() == mode && item.isSelected(), "Mode lost on native reattachment");
                if (mode != InterstellarMapPanel.MapMode.FACTION) {
                    checkAnalyticalPixels(map);
                }
                    if (index < modes.length && (mode == InterstellarMapPanel.MapMode.POPULATION
                        || mode == InterstellarMapPanel.MapMode.RECHARGE_STATIONS)) {
                      map.setViewState(new ViewState(originalView.centerX(), originalView.centerY(), 6,
                          originalView.selectedSystem()));
                      Files.write(output("native-mode-" + mode.name().toLowerCase(java.util.Locale.ROOT)),
                          map.captureNativePng());
                      map.setViewState(originalView);
                    }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void checkAnalyticalPixels(SkiaMap map) throws Exception {
        ViewState original = map.getViewState();
        java.util.Set<Integer> checked = new java.util.HashSet<>();
        int dimension = UIUtil.scaleForGUI(100);
        boolean emptyChecked = false;
        for (var system : map.getPresentation().systems()) {
            if (system.empty()) {
                if (!emptyChecked && !map.getPresentation().routeSystemIds().contains(system.system().getId())) {
                    map.setViewState(new ViewState(original.centerX(), original.centerY(), 0.6, original.selectedSystem()));
                    BufferedImage empty = ImageIO.read(new java.io.ByteArrayInputStream(map.captureNativeStarPng(system, dimension)));
                    require(nearColor(empty.getRGB(dimension / 2, dimension / 2), 0x697880),
                          "Analytical mode recolored an empty-system contact");
                    emptyChecked = true;
                }
                continue;
            }
            if (!checked.add(system.analyticalColor())) {
                continue;
            }
            for (double scale : new double[] { 0.6, 6 }) {
                map.setViewState(new ViewState(original.centerX(), original.centerY(), scale, original.selectedSystem()));
                BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(map.captureNativeStarPng(system, dimension)));
                double radius = map.getPresentation().systemStyle().sizeAt(scale) + UIUtil.scaleForGUI(2.8f);
                int horizontal = dimension / 2 + (scale < 1 ? 0 : (int) Math.floor(radius));
                int color = image.getRGB(horizontal, dimension / 2);
                require(nearColor(color, system.analyticalColor()), "Native analytical pixel mismatch: "
                      + map.getPresentation().mapMode() + " scale=" + scale + " expected="
                      + Integer.toHexString(system.analyticalColor()) + " actual=" + Integer.toHexString(color));
                    if (scale > 1) {
                      require(nearColor(image.getRGB(dimension / 2, dimension / 2), system.star().coreColor()),
                          "Analytical mode replaced intrinsic stellar core color");
                    }
            }
        }
        require(!checked.isEmpty(), "No analytical palette samples");
        require(emptyChecked, "No empty-system contact sample");
        map.setViewState(original);
    }

    private static JCheckBoxMenuItem findToggle(JFrame frame) {
        return findToggle(frame, "miExperimentalSkiaMap.text");
    }

    private static JCheckBoxMenuItem findToggle(JFrame frame, String key) {
        String label = MHQInternationalization.getTextAt("mekhq.resources.MekHQMenuBar", key);
        for (int menuIndex = 0; menuIndex < frame.getJMenuBar().getMenuCount(); menuIndex++) {
            JMenu menu = frame.getJMenuBar().getMenu(menuIndex);
            for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex++) {
                if ((menu.getItem(itemIndex) instanceof JCheckBoxMenuItem item) && label.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new IllegalStateException("Experimental map menu toggle is missing");
    }

    private static void exerciseMapInput(JFrame frame, Campaign campaign) {
        SkiaMap map = find(frame, SkiaMap.class);
        Component canvas = find(frame, SkiaLayer.class).getCanvas();
        ViewState beforePan = map.getViewState();
        Point start = new Point(map.getWidth() / 2, map.getHeight() / 2);
        Point end = new Point(start.x + UIUtil.scaleForGUI(40), start.y + UIUtil.scaleForGUI(25));
        sendMouse(canvas, MouseEvent.MOUSE_PRESSED, start, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        sendMouse(canvas, MouseEvent.MOUSE_DRAGGED, end, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
        sendMouse(canvas, MouseEvent.MOUSE_RELEASED, end, 0, MouseEvent.BUTTON1);
        require(!beforePan.equals(map.getViewState()), "Native drag did not pan");
        ViewState afterPan = map.getViewState();
        PlanetarySystem target = null;
        Point targetPoint = null;
        int margin = UIUtil.scaleForGUI(20);
        for (PlanetarySystem system : campaign.getSystems()) {
            boolean hidden = false;
            for (var data : map.getPresentation().systems()) {
                if (Objects.equals(data.system(), system)) {
                    hidden = data.empty() && !map.getPresentation().showEmptySystems()
                          && !Objects.equals(system, campaign.getCurrentSystem())
                          && !Objects.equals(system, afterPan.selectedSystem())
                          && !map.getPresentation().routeSystemIds().contains(system.getId());
                    break;
                }
            }
            if (hidden) {
                continue;
            }
            int horizontal = (int) Math.round(map.getWidth() / 2.0 + (system.getX() - afterPan.centerX()) * afterPan.scale());
            int vertical = (int) Math.round(map.getHeight() / 2.0 - (system.getY() - afterPan.centerY()) * afterPan.scale());
            if (!Objects.equals(system, afterPan.selectedSystem()) && (horizontal >= margin)
                  && (horizontal < map.getWidth() - margin) && (vertical >= margin)
                  && (vertical < map.getHeight() - margin)) {
                target = system;
                targetPoint = new Point(horizontal, vertical);
                break;
            }
        }
        require(target != null, "No visible star for selection check");
        sendMouse(canvas, MouseEvent.MOUSE_PRESSED, targetPoint, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        sendMouse(canvas, MouseEvent.MOUSE_RELEASED, targetPoint, 0, MouseEvent.BUTTON1);
        require(Objects.equals(map.getViewState().selectedSystem(), target), "Native star selection failed");
        expectedView = map.getViewState();
    }

    private static void sendMouse(Component canvas, int type, Point point, int modifiers, int button) {
        canvas.dispatchEvent(new MouseEvent(canvas, type, System.currentTimeMillis(), modifiers,
              point.x, point.y, 1, false, button));
    }

    private static void checkPresentation(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        SkiaMap map = find(frame, SkiaMap.class);
        Campaign campaign = app.getCampaigngui().getCampaign();
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        JCheckBoxMenuItem emptySystems = findToggle(frame, "miMapEmptySystems.text");
        LocalDate originalDate = campaign.getLocalDate();
        var originalTerritories = map.getPresentation().territories();
        int[] territoryBuilds = new int[1];
          ViewState previous = map.getViewState();
          ViewState original = new ViewState(previous.selectedSystem().getX(), previous.selectedSystem().getY(),
              previous.scale(), previous.selectedSystem());
        map.setViewState(new ViewState(original.centerX(), original.centerY(), 6, original.selectedSystem()));
        AtomicInteger phase = new AtomicInteger();
        Set<String> filteredIds = new HashSet<>();
        Timer checks = new Timer(800, event -> {
            try {
                SkiaMap current = find(frame, SkiaMap.class);
                switch (phase.incrementAndGet()) {
                    case 1 -> {
                        checkDatedPresentation(current, campaign);
                        checkSystemRendering(current, true);
                        checkStellarPixels(current);
                        territoryBuilds[0] = current.getTerritoryBuilds();
                        checkTerritoryPixels(current);
                        checkLabels(current, true);
                        checkPanVisibility(current, campaign);
                        capture(frame, "skia-labels-detail");
                        current.setViewState(new ViewState(original.centerX(), original.centerY(), 0.6,
                              original.selectedSystem()));
                    }
                    case 2 -> {
                        require(current.getTerritoryBuilds() == territoryBuilds[0], "Zoom rebuilt native territory paths");
                        checkSystemRendering(current, false);
                        require(current.getPresentation().territories() == originalTerritories,
                              "Camera movement rebuilt the territory snapshot");
                        checkTerritoryPixels(current);
                        checkLabels(current, false);
                        checkPanVisibility(current, campaign);
                        checkVisibility(current, campaign);
                        filteredIds.addAll(current.getVisibleSystemIds());
                        capture(frame, "skia-labels-overview");
                        require(!emptySystems.isSelected(), "Empty systems should initially be hidden");
                        emptySystems.doClick(0);
                        require(tab.isShowingEmptySystems(), "Shared filter did not enable empty systems");
                    }
                    case 3 -> {
                        require(current.getPresentation().showEmptySystems(), "Native filter did not update");
                        checkVisibility(current, campaign);
                        require(current.getVisibleSystemIds().containsAll(filteredIds), "Enabling empty systems hid stars");
                        require(current.getVisibleSystemIds().size() > filteredIds.size(), "Empty-system toggle added no stars");
                        toggle.doClick(0);
                        require(tab.isShowingEmptySystems() && emptySystems.isSelected(), "Filter was lost returning to Java2D");
                        emptySystems.doClick(0);
                        require(!tab.isShowingEmptySystems(), "Java2D filter did not update");
                        toggle.doClick(0);
                    }
                    case 4 -> {
                        require(!current.getPresentation().showEmptySystems(), "Filter was lost returning to Skia");
                        checkVisibility(current, campaign);
                        require(new HashSet<>(current.getVisibleSystemIds()).equals(filteredIds), "Filtered star set changed");
                        campaign.setLocalDate(originalDate.minusYears(100));
                        current.refresh();
                    }
                    case 5 -> {
                        checkDatedPresentation(current, campaign);
                        require(current.getPresentation().territories() != originalTerritories,
                              "Date change retained stale territory geometry");
                        checkTerritoryPixels(current);
                        campaign.setLocalDate(originalDate);
                        current.refresh();
                        current.setViewState(original);
                    }
                    case 6 -> {
                        checkDatedPresentation(current, campaign);
                        checkLabels(current, true);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_PRESENTATION_CHECK labels=fixed-offset colors=dated filters=shared zoom=checked");
                        checkStellarTransition(frame, app, toggle);
                    }
                    default -> throw new IllegalStateException("Unexpected presentation phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static JMenuItem findMenuItem(JFrame frame, String key) {
        String text = MHQInternationalization.getTextAt("mekhq.resources.MekHQMenuBar", key);
        String parentKey = key.startsWith("miMapCapitals.") ? "menuMapCapitals.text"
              : key.startsWith("miMapAdministrative.") ? "menuMapAdministrative.text" : null;
        if (parentKey != null) {
            JMenuItem found = findMenuItem((JMenu) findMenuItem(frame, parentKey), text);
            if (found == null) {
                throw new IllegalStateException("Missing map submenu item: " + key);
            }
            return found;
        }
        for (int index = 0; index < frame.getJMenuBar().getMenuCount(); index++) {
            JMenuItem found = findMenuItem(frame.getJMenuBar().getMenu(index), text);
            if (found != null) {
                return found;
            }
        }
        throw new IllegalStateException("Missing map menu item: " + key);
    }

    private static JMenuItem findMenuItem(JMenu menu, String text) {
        for (Component component : menu.getMenuComponents()) {
            if (component instanceof JMenuItem item && text.equals(item.getText())) {
                return item;
            }
            if (component instanceof JMenu child) {
                JMenuItem found = findMenuItem(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static BufferedImage captureMap(SkiaMap map) throws Exception {
        if (offscreen) {
            return ImageIO.read(new ByteArrayInputStream(map.captureNativePng()));
        }
        return new Robot().createScreenCapture(new Rectangle(map.getLocationOnScreen(), map.getSize()));
    }

    private static int changedPixels(BufferedImage before, BufferedImage after) {
        require(before.getWidth() == after.getWidth() && before.getHeight() == after.getHeight(),
              "Map capture dimensions changed unexpectedly");
        int changes = 0;
        for (int vertical = 0; vertical < before.getHeight(); vertical++) {
            for (int horizontal = 0; horizontal < before.getWidth(); horizontal++) {
                if (before.getRGB(horizontal, vertical) != after.getRGB(horizontal, vertical)) {
                    changes++;
                }
            }
        }
        return changes;
    }

    private static void checkCartographyControls(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        SkiaMap map = find(frame, SkiaMap.class);
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        ViewState original = map.getViewState();
        var originalLayers = tab.getCartographyLayers();
        var emblem = map.getPresentation().territories().emblems().stream()
              .filter(candidate -> candidate.priority() == 0)
              .max(Comparator.comparingInt(candidate -> candidate.cellCount())).orElseThrow();
        map.setViewState(new ViewState(emblem.anchorX(), emblem.anchorY(), 0.8, original.selectedSystem()));
        JCheckBoxMenuItem emblems = findToggle(frame, "miMapEmblems.text");
        JCheckBoxMenuItem territories = findToggle(frame, "miMapTerritories.text");
        BufferedImage[] baseline = new BufferedImage[1];
        AtomicInteger phase = new AtomicInteger();
        int[] regionCount = new int[1];
        Timer checks = new Timer(900, event -> {
            try {
                SkiaMap current = find(frame, SkiaMap.class);
                switch (phase.incrementAndGet()) {
                    case 1 -> {
                        require(!current.getEmblemPlacements().isEmpty(), "No native emblem at its atlas anchor");
                        require(SkiaMap.getActiveEmblemImages() > 0, "No native emblem images decoded");
                        checkEmblemPlacement(current);
                        baseline[0] = captureMap(current);
                        capture(frame, "skia-faction-emblems");
                        emblems.doClick(0);
                    }
                    case 2 -> {
                        require(current.getEmblemPlacements().isEmpty(), "Emblem visibility toggle failed");
                        require(changedPixels(baseline[0], captureMap(current)) > 100, "Emblem toggle changed no native pixels");
                        baseline[0] = captureMap(current);
                        territories.doClick(0);
                    }
                    case 3 -> {
                        require(current.getRenderedTerritories() == 0, "Territory visibility toggle failed");
                        require(changedPixels(baseline[0], captureMap(current)) > 1000, "Territory toggle changed no native pixels");
                        toggle.doClick(0);
                        require(!tab.getCartographyLayers().territories() && !tab.getCartographyLayers().emblems(),
                              "Layer visibility was lost switching to Java2D");
                        require(SkiaMap.getActiveEmblemImages() == 0 && SkiaMap.getActiveAdministrativePaths() == 0,
                              "Native cartography resources survived switching to Java2D");
                        territories.doClick(0);
                        emblems.doClick(0);
                        toggle.doClick(0);
                        var border = current.getPresentation().territories().administrativeBorders().stream()
                              .filter(candidate -> candidate.region())
                              .max(Comparator.comparingDouble(candidate -> {
                                  Rectangle2D bounds = candidate.shape().getBounds2D();
                                  return bounds.getWidth() * bounds.getHeight();
                              })).orElseThrow(() -> new IllegalStateException("No administrative region fixture"));
                        Rectangle2D bounds = border.shape().getBounds2D();
                        double scale = Math.min(2, Math.min(current.getWidth() / (bounds.getWidth() * 1.1),
                              current.getHeight() / (bounds.getHeight() * 1.1)));
                        current.setViewState(new ViewState(bounds.getCenterX(), bounds.getCenterY(), scale,
                              original.selectedSystem()));
                        emblems.doClick(0);
                    }
                    case 4 -> {
                        require(current.getRenderedAdministrativeBorders() == 0, "Administrative borders defaulted on");
                        baseline[0] = captureMap(current);
                        findMenuItem(frame, "miMapAdministrative.REGIONS.text").doClick(0);
                    }
                    case 5 -> {
                        regionCount[0] = current.getRenderedAdministrativeBorders();
                        require(regionCount[0] > 0, "No native region boundaries drawn");
                        require(changedPixels(baseline[0], captureMap(current)) > 100, "Region boundaries changed no pixels");
                        capture(frame, "skia-administrative-regions");
                        baseline[0] = captureMap(current);
                        findMenuItem(frame, "miMapAdministrative.DISTRICTS.text").doClick(0);
                    }
                    case 6 -> {
                        require(current.getRenderedAdministrativeBorders() > regionCount[0], "District detail added no paths");
                        require(changedPixels(baseline[0], captureMap(current)) > 100, "District boundaries changed no pixels");
                        capture(frame, "skia-administrative-districts");
                        toggle.doClick(0);
                        require(tab.getCartographyLayers().administrative() == BoundaryDetail.DISTRICTS,
                              "Administrative detail was lost returning to Java2D");
                        toggle.doClick(0);
                    }
                    case 7 -> {
                        require(current.getRenderedAdministrativeBorders() > regionCount[0],
                              "Administrative detail was lost returning to Skia");
                        findMenuItem(frame, "miMapAdministrative.OFF.text").doClick(0);
                    }
                    case 8 -> {
                        require(current.getRenderedAdministrativeBorders() == 0, "Administrative Off did not clear borders");
                        tab.setCartographyLayers(originalLayers);
                        current.setViewState(original);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_CARTOGRAPHY_CHECK emblems=pixels regions=pixels districts=pixels controls=shared resources=released");
                        checkLandmarks(frame, app, toggle);
                    }
                    default -> throw new IllegalStateException("Unexpected cartography phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void checkEmblemPlacement(SkiaMap map) {
        List<SkiaMap.EmblemPlacement> placements = map.getEmblemPlacements();
        for (int index = 0; index < placements.size(); index++) {
            var placement = placements.get(index);
            require(placement.emblem().imagePath().equals(Factions.getFactionLogoAddress(
                  map.getPresentation().territories().date().getYear(), placement.emblem().factionCode())),
                  "Emblem uses the wrong historical asset");
            Rect bounds = placement.bounds();
            require(bounds.getWidth() > 0 && bounds.getHeight() > 0, "Empty emblem bounds");
            for (int other = index + 1; other < placements.size(); other++) {
                Rect compared = placements.get(other).bounds();
                require(bounds.getRight() <= compared.getLeft() || bounds.getLeft() >= compared.getRight()
                      || bounds.getBottom() <= compared.getTop() || bounds.getTop() >= compared.getBottom(),
                      "Overlapping native emblems");
            }
        }
    }

    private static void checkDisputedTerritory(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        SkiaMap map = find(frame, SkiaMap.class);
        var disputed = map.getPresentation().territories().contours().stream()
              .filter(territory -> territory.factionColors().size() > 1)
              .max(Comparator.comparingDouble(territory -> {
                  Rectangle2D bounds = territory.shape().getBounds2D();
                  return bounds.getWidth() * bounds.getHeight();
              })).orElseThrow(() -> new IllegalStateException("No disputed territory fixture"));
        ViewState original = map.getViewState();
        Rectangle2D bounds = disputed.shape().getBounds2D();
        double scale = Math.min(6, Math.min(map.getWidth() / (bounds.getWidth() * 1.2),
              map.getHeight() / (bounds.getHeight() * 1.2)));
        map.setViewState(new ViewState(bounds.getCenterX(), bounds.getCenterY(), scale, original.selectedSystem()));
        Timer capture = new Timer(900, event -> {
            ((Timer) event.getSource()).stop();
            try {
                require(map.getRenderedTerritories() > 0, "No native disputed territory frame");
                capture(frame, "skia-territories-disputed");
                map.setViewState(original);
                System.out.println("SKIA_TERRITORY_CHECK pixels=matched cache=reused date=updated disputed=captured");
                checkRoutes(frame, app, toggle);
            } catch (Exception exception) {
                exception.printStackTrace();
                frame.dispose();
                System.exit(1);
            }
        });
        capture.start();
    }

    private static void checkTerritoryPixels(SkiaMap map) throws Exception {
        BufferedImage image = captureMap(map);
        ViewState view = map.getViewState();
        List<Rectangle2D> exclusions = new ArrayList<>();
        for (var emblem : map.getEmblemPlacements()) {
            Rect bounds = emblem.bounds();
            exclusions.add(new Rectangle2D.Double(bounds.getLeft() - 5, bounds.getTop() - 5,
                  bounds.getWidth() + 10, bounds.getHeight() + 10));
        }
        for (var label : map.getLabelPlacements()) {
            Rect bounds = label.bounds();
            exclusions.add(new Rectangle2D.Double(bounds.getLeft() - 3, bounds.getTop() - 3,
                  bounds.getWidth() + 6, bounds.getHeight() + 6));
        }
        java.util.Map<String, Float> starRadii = new java.util.HashMap<>();
        for (var placement : map.getSystemPlacements()) {
            starRadii.put(placement.systemId(), placement.radius());
        }
        for (var landmark : map.getLandmarkPlacements()) {
            Rect bounds = landmark.bounds();
            exclusions.add(new Rectangle2D.Double(bounds.getLeft(), bounds.getTop(), bounds.getWidth(), bounds.getHeight()));
        }
        Set<String> visibleIds = new HashSet<>(map.getVisibleSystemIds());
        for (var system : map.getPresentation().systems()) {
            if (!visibleIds.contains(system.system().getId())) {
                continue;
            }
            double horizontal = map.getWidth() / 2.0 + (system.system().getX() - view.centerX()) * view.scale();
            double vertical = map.getHeight() / 2.0 - (system.system().getY() - view.centerY()) * view.scale();
            boolean priority = Objects.equals(system.system(), view.selectedSystem())
                  || Objects.equals(system.system(), map.getPresentation().routes().currentSystem());
            double radius = starRadii.get(system.system().getId()) + UIUtil.scaleForGUI(priority ? 6 : 2);
            exclusions.add(new Rectangle2D.Double(horizontal - radius, vertical - radius, radius * 2, radius * 2));
        }
        int samples = 0;
        int matches = 0;
        for (int vertical = 20; vertical < image.getHeight() - 20; vertical += 24) {
            for (int horizontal = 20; horizontal < image.getWidth() - 20; horizontal += 24) {
                boolean excluded = false;
                for (Rectangle2D bounds : exclusions) {
                    if (bounds.contains(horizontal, vertical)) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) {
                    continue;
                }
                double mapX = view.centerX() + (horizontal - map.getWidth() / 2.0) / view.scale();
                double mapY = view.centerY() - (vertical - map.getHeight() / 2.0) / view.scale();
                double margin = 5 / view.scale();
                Rectangle2D probe = new Rectangle2D.Double(mapX - margin, mapY - margin, margin * 2, margin * 2);
                Integer color = null;
                for (var territory : map.getPresentation().territories().contours()) {
                    if (!territory.shape().intersects(probe)) {
                        continue;
                    }
                    if (!territory.shape().contains(probe) || territory.pocket() || territory.factionColors().size() != 1) {
                        excluded = true;
                        break;
                    }
                    color = territory.factionColors().getFirst();
                }
                if (excluded || color == null) {
                    continue;
                }
                int actual = image.getRGB(horizontal, vertical);
                int alpha = 29;
                int expectedRed = ((color >> 16 & 255) * alpha + 6 * (255 - alpha)) / 255;
                int expectedGreen = ((color >> 8 & 255) * alpha + 14 * (255 - alpha)) / 255;
                int expectedBlue = ((color & 255) * alpha + 20 * (255 - alpha)) / 255;
                samples++;
                if (Math.abs((actual >> 16 & 255) - expectedRed) <= 5
                      && Math.abs((actual >> 8 & 255) - expectedGreen) <= 5
                      && Math.abs((actual & 255) - expectedBlue) <= 5) {
                    matches++;
                }
            }
        }
        require(samples >= 20 && matches >= samples * 0.9,
              "Native territory interior pixel mismatch: matched=" + matches + " sampled=" + samples);
        System.out.printf("SKIA_TERRITORY_PIXELS matched=%d sampled=%d%n", matches, samples);
    }

    private static void checkRoutes(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        Campaign campaign = app.getCampaigngui().getCampaign();
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        var location = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        PlanetarySystem origin = campaign.getCurrentSystem();
        List<PlanetarySystem> nearby = new ArrayList<>();
        for (PlanetarySystem system : campaign.getSystems()) {
            double distance = Math.hypot(system.getX() - origin.getX(), system.getY() - origin.getY());
            if ((distance > 18) && (distance < 25)) {
                nearby.add(system);
            }
        }
        nearby.sort(Comparator.comparingDouble(system -> Math.hypot(
              system.getX() - origin.getX(), system.getY() - origin.getY())));
        require(nearby.size() >= 2, "Insufficient nearby systems for route smoke");
        PlanetarySystem destination = nearby.getFirst();
        PlanetarySystem extension = nearby.get(1);
        double originalTransit = location.getTransitTime();
        tab.switchSystemsMap(origin);
        SkiaMap map = find(frame, SkiaMap.class);
          map.setViewState(new ViewState((origin.getX() + destination.getX()) / 2,
              (origin.getY() + destination.getY()) / 2, 12, destination));
          AtomicInteger phase = new AtomicInteger(-1);
        AtomicInteger partialPlanned = new AtomicInteger();
        AtomicInteger partialActive = new AtomicInteger();
        AtomicInteger partialHop = new AtomicInteger();
        Timer animationSamples = new Timer(40, event -> {
            try {
                SkiaMap current = find(frame, SkiaMap.class);
                if (current == null || !current.getTravelAnimation().running()) {
                    return;
                }
                current.captureNativePng();
                for (var leg : current.getRouteLegPlacements()) {
                    if (leg.progress() > 0 && leg.progress() < 1) {
                        AtomicInteger samples = leg.active() ? partialActive : partialPlanned;
                        if (samples.getAndIncrement() == 0) {
                            capture(frame, leg.active() ? "skia-route-activation" : "skia-route-reveal");
                        }
                    }
                }
                var animation = current.getTravelAnimation();
                if (animation.hop() > 0 && animation.hop() < 1
                      && current.getFleetPlacements().stream().anyMatch(fleet -> fleet.shipAlpha() > 0
                            && fleet.shipAlpha() < 1)) {
                    if (partialHop.getAndIncrement() == 0) {
                        capture(frame, "skia-fleet-hop");
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                frame.dispose();
                System.exit(1);
            }
        });
        animationSamples.start();
        Timer routes = new Timer(900, event -> {
            try {
                SkiaMap current = find(frame, SkiaMap.class);
                switch (phase.incrementAndGet()) {
                    case 0 -> {
                        capture(frame, "skia-route-ready");
                        tab.plotRoute(destination);
                        require(tab.hasPlannedRoute(), "Campaign planner did not create a route");
                    }
                    case 1 -> {
                        var planned = current.getPresentation().routes().planned();
                        require(planned.getFirst().equals(origin) && planned.getLast().equals(destination),
                              "Planned route endpoint/order mismatch");
                        require(current.getPlannedLegs() == planned.size() - 1 && current.getActiveLegs() == 0,
                              "Planned route was not drawn");
                        require(partialPlanned.get() > 0 && !current.getTravelAnimation().running(),
                            "Planned route did not reveal partial legs and settle");
                        capture(frame, "skia-route-badges");
                        require(current.getRouteBadges().stream().anyMatch(badge -> !badge.active()
                            && badge.systemId().equals(destination.getId()) && badge.number() == 1),
                            "Requested destination badge was not drawn: requested="
                                + current.getPresentation().routes().requestedWaypoints()
                                + " badges=" + current.getRouteBadges());
                        require(current.hasFleetImage() && current.getFleetPlacements().stream()
                            .anyMatch(fleet -> fleet.systemId().equals(origin.getId()) && fleet.shipAlpha() == 1),
                            "Native fleet image was not drawn");
                        checkNavigationLabelClearance(current);
                        checkFleetPixels(current);
                        current.refresh();
                        current.setViewState(current.getViewState());
                        require(!current.getTravelAnimation().running(), "Unchanged route replayed its animation");
                        location.setJumpPath(new JumpPath(new ArrayList<>(planned)));
                        current.refresh();
                        require(current.getTravelAnimation().activation() == 0,
                            "Beginning the planned route did not start activation feedback");
                        tab.appendWaypoint(extension);
                        location.setTransitTime(origin.getTimeToJumpPoint(1.0) * 0.75);
                        MekHQ.triggerEvent(new TransitStatusChangedEvent(location));
                    }
                    case 2 -> {
                        require(partialActive.get() > 0 && !current.getTravelAnimation().running(),
                            "Route activation did not render partial legs and settle");
                        require(current.getPlannedLegs() >= 2 && current.getActiveLegs() >= 1,
                              "Concurrent planned and active routes missing");
                        require(current.getPresentation().routes().planned().getLast().equals(extension),
                              "Appended waypoint missing");
                        require(current.getViewState().selectedSystem().equals(extension), "Route selection not synchronized");
                        require(current.isTransitMarkerDrawn(), "Fleet transit marker missing");
                        require(Math.abs(current.getPresentation().routes().planetProximity() - 0.25) < 0.00001,
                              "Incorrect fleet transit progress");
                        capture(frame, "skia-routes-transit");
                        checkRoutePixels(current);
                        toggle.doClick(0);
                        require(!current.getTravelAnimation().running() && !current.hasFleetImage(),
                            "Renderer disposal retained travel animation or fleet image");
                        require(tab.hasPlannedRoute() && location.getJumpPath().size() >= 2,
                              "Switching renderer changed campaign routes");
                        toggle.doClick(0);
                        location.setTransitTime(origin.getTimeToJumpPoint(1.0) * 0.25);
                        MekHQ.triggerEvent(new TransitStatusChangedEvent(location));
                    }
                    case 3 -> {
                        require(!current.getTravelAnimation().running(), "Reattachment replayed travel feedback");
                        require(current.isTransitMarkerDrawn() && current.getActiveLegs() >= 1,
                              "Route/transit lost on renderer round trip");
                        require(Math.abs(current.getPresentation().routes().planetProximity() - 0.75) < 0.00001,
                              "Transit event did not refresh progress");
                        current.setViewState(new ViewState(origin.getX(), origin.getY(), 0.6, origin));
                        current.captureNativePng();
                        require(current.getRouteBadges().isEmpty() && current.getFleetPlacements().stream()
                            .anyMatch(fleet -> fleet.ringAlpha() == 1 && fleet.shipAlpha() == 0),
                            "Overview retained badges or failed to replace the fleet with a ring");
                        capture(frame, "skia-routes-overview");
                        current.setViewState(new ViewState(origin.getX(), origin.getY(), 1.6, origin));
                        current.captureNativePng();
                        require(current.getFleetPlacements().stream().anyMatch(fleet -> fleet.ringAlpha() > 0
                            && fleet.shipAlpha() > 0 && Math.abs(fleet.ringAlpha() + fleet.shipAlpha() - 1) < 0.001),
                            "Fleet ring and ship did not crossfade");
                        current.setViewState(new ViewState((origin.getX() + destination.getX()) / 2,
                            (origin.getY() + destination.getY()) / 2, 12, destination));
                        var arrival = new mekhq.campaign.CurrentLocation(destination, 0);
                        arrival.setJumpPath(new JumpPath(new ArrayList<>(List.of(destination))));
                        campaign.setLocation(arrival);
                        MekHQ.triggerEvent(new TransitStatusChangedEvent(arrival));
                        current.refresh();
                        require(current.getTravelAnimation().hop() == 0, "System change did not start fleet hop");
                      }
                      case 4 -> {
                        require(partialHop.get() > 0 && !current.getTravelAnimation().running(),
                            "Fleet hop did not render endpoint feedback and settle");
                        require(current.getFleetPlacements().stream().anyMatch(fleet ->
                            fleet.systemId().equals(destination.getId()) && fleet.shipAlpha() == 1),
                            "Fleet hop did not finish at its destination");
                        campaign.setLocation(location);
                        MekHQ.triggerEvent(new TransitStatusChangedEvent(location));
                        current.refresh();
                        require(current.getTravelAnimation().running(), "Return fixture did not animate");
                        current.setVisible(false);
                        require(!current.getTravelAnimation().running(), "Hidden map retained travel timer");
                        current.setVisible(true);
                        require(!current.getTravelAnimation().running(), "Showing map replayed travel feedback");
                        tab.clearPlannedRoute();
                        tab.plotRoute(extension);
                        current.refresh();
                        require(current.getTravelAnimation().running(), "New route did not start reveal feedback");
                        toggle.doClick(0);
                        require(!current.getTravelAnimation().running() && !current.hasFleetImage(),
                            "Detaching an animating map retained its timer or fleet asset");
                        toggle.doClick(0);
                        require(!find(frame, SkiaMap.class).getTravelAnimation().running(),
                            "Reattached map replayed interrupted route feedback");
                        tab.clearPlannedRoute();
                        location.setJumpPath(new JumpPath(new ArrayList<>(List.of(origin))));
                        location.setTransitTime(0);
                        MekHQ.triggerEvent(new TransitStatusChangedEvent(location));
                    }
                    case 5 -> {
                        require(current.getPlannedLegs() == 0 && current.getActiveLegs() == 0,
                              "Cleared routes left stale legs");
                        require(!current.isTransitMarkerDrawn(), "Arrival left a stale transit marker");
                        require(current.getPresentation().routes().active().size() == 1,
                              "Single-system active path not preserved");
                        location.setJumpPath(null);
                        location.setTransitTime(originalTransit);
                    }
                    case 6 -> {
                        require(current.getPresentation().routes().active().isEmpty(), "Canceled active route remained");
                        ((Timer) event.getSource()).stop();
                        animationSamples.stop();
                        System.out.println("SKIA_ROUTE_CHECK planned=revealed active=activated fleet=native hop=animated"
                            + " badges=numbered zoom=crossfaded lifecycle=stopped transit=updated clear=checked");
                        if (routesOnly) {
                            frame.dispose();
                            System.out.println("ROUTE_MAP_SMOKE_COMPLETE failures=0");
                            System.exit(0);
                            return;
                        }
                        checkNavigationInteractions(frame, app, toggle, origin, destination, extension);
                    }
                    default -> throw new IllegalStateException("Unexpected route phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        routes.start();
    }

    private static void checkNavigationLabelClearance(SkiaMap map) {
        checkLabelAnchors(map);
    }

    private static void checkFleetPixels(SkiaMap map) throws Exception {
        BufferedImage image = captureMap(map);
        int amberPixels = 0;
        for (var fleet : map.getFleetPlacements()) {
            if (fleet.shipAlpha() < 1) {
                continue;
            }
            Rect bounds = fleet.bounds();
            for (int vertical = Math.max(0, (int) bounds.getTop());
                  vertical < Math.min(image.getHeight(), bounds.getBottom()); vertical++) {
                for (int horizontal = Math.max(0, (int) bounds.getLeft());
                      horizontal < Math.min(image.getWidth(), bounds.getRight()); horizontal++) {
                    int color = image.getRGB(horizontal, vertical);
                    int red = (color >>> 16) & 255;
                    int green = (color >>> 8) & 255;
                    int blue = color & 255;
                    if (red > 80 && red > green * 1.15 && green > blue * 1.4) {
                        amberPixels++;
                    }
                }
            }
        }
        require(amberPixels > 20, "Native fleet asset has no visible amber pixels");
    }

    private static Point systemPoint(SkiaMap map, PlanetarySystem system) {
        ViewState view = map.getViewState();
        return new Point((int) Math.round(map.getWidth() / 2.0 + (system.getX() - view.centerX()) * view.scale()),
              (int) Math.round(map.getHeight() / 2.0 - (system.getY() - view.centerY()) * view.scale()));
    }

    private static void clickSystem(JFrame frame, PlanetarySystem system, int modifiers, int clickCount) {
        SkiaMap map = find(frame, SkiaMap.class);
        Component canvas = find(frame, SkiaLayer.class).getCanvas();
        Point point = systemPoint(map, system);
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
              modifiers | InputEvent.BUTTON1_DOWN_MASK, point.x, point.y, clickCount, false, MouseEvent.BUTTON1));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
              modifiers, point.x, point.y, clickCount, false, MouseEvent.BUTTON1));
    }

    private static JPopupMenu openMapPopup(JFrame frame, Point point) {
        Component canvas = find(frame, SkiaLayer.class).getCanvas();
        sendMouse(canvas, MouseEvent.MOUSE_PRESSED, point, InputEvent.BUTTON3_DOWN_MASK, MouseEvent.BUTTON3);
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
              0, point.x, point.y, 1, true, MouseEvent.BUTTON3));
        JPopupMenu popup = find(frame, SkiaMap.class).getNavigationPopup();
        require(popup != null && popup.isVisible(), "Native map context menu did not open");
        require(!popup.isLightWeightPopupEnabled(), "Native popup is not heavyweight");
        return popup;
    }

    private static JMenuItem mapAction(JPopupMenu popup, String key) {
        String text = MHQInternationalization.getTextAt("mekhq.resources.CampaignGUI", key);
        for (Component component : popup.getComponents()) {
            if (component instanceof JMenuItem item && text.equals(item.getText())) {
                return item;
            }
        }
        throw new IllegalStateException("Missing native navigation action: " + key);
    }

    private static void invokeMapAction(JPopupMenu popup, String key) {
        JMenuItem item = mapAction(popup, key);
        require(item.isEnabled(), "Disabled native navigation action: " + key);
        item.doClick(0);
        popup.setVisible(false);
    }

    private static void sendMapKey(JFrame frame, int keyCode, int modifiers) {
        Component canvas = find(frame, SkiaLayer.class).getCanvas();
        KeyEvent key = new KeyEvent(canvas, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers,
              keyCode, KeyEvent.CHAR_UNDEFINED);
        for (var listener : canvas.getKeyListeners()) {
            listener.keyPressed(key);
        }
        require(key.isConsumed(), "Native keyboard action was ignored");
    }

    private static void checkNavigationInteractions(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle,
          PlanetarySystem origin, PlanetarySystem destination, PlanetarySystem extension) {
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        Campaign campaign = app.getCampaigngui().getCampaign();
        var location = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        SkiaMap map = find(frame, SkiaMap.class);
        ViewState original = map.getViewState();
        boolean showEmpty = tab.isShowingEmptySystems();
        tab.setShowingEmptySystems(true);
        map.setViewState(new ViewState(origin.getX(), origin.getY(), 6, origin));
        clickSystem(frame, destination, InputEvent.ALT_DOWN_MASK, 1);
        AtomicInteger phase = new AtomicInteger();
        JPopupMenu[] popup = new JPopupMenu[1];
        BufferedImage[] beforeMeasurement = new BufferedImage[1];
        var far = campaign.getSystems().stream().filter(system -> {
            double distance = Math.hypot(system.getX() - origin.getX(), system.getY() - origin.getY());
            return distance > 40 && distance < 60;
        }).findFirst().orElseThrow();
        Timer checks = new Timer(900, event -> {
            try {
                SkiaMap current = find(frame, SkiaMap.class);
                switch (phase.incrementAndGet()) {
                    case 1 -> {
                        require(tab.hasPlannedRoute() && current.getPresentation().routes().planned().getLast().equals(destination),
                              "Alt-click did not plot the destination");
                        clickSystem(frame, extension, InputEvent.SHIFT_DOWN_MASK, 1);
                    }
                    case 2 -> {
                        require(current.getPresentation().routes().planned().getLast().equals(extension),
                              "Shift-click did not append the waypoint");
                        ViewState before = current.getViewState();
                        var route = current.getPresentation().routes().planned();
                        Component canvas = find(frame, SkiaLayer.class).getCanvas();
                        Point start = systemPoint(current, destination);
                        Point end = new Point(start.x + UIUtil.scaleForGUI(40), start.y + UIUtil.scaleForGUI(20));
                        sendMouse(canvas, MouseEvent.MOUSE_PRESSED, start,
                              InputEvent.BUTTON1_DOWN_MASK | InputEvent.ALT_DOWN_MASK, MouseEvent.BUTTON1);
                        sendMouse(canvas, MouseEvent.MOUSE_DRAGGED, end,
                              InputEvent.BUTTON1_DOWN_MASK | InputEvent.ALT_DOWN_MASK, MouseEvent.NOBUTTON);
                        sendMouse(canvas, MouseEvent.MOUSE_RELEASED, end, InputEvent.ALT_DOWN_MASK, MouseEvent.BUTTON1);
                        require(!current.getViewState().equals(before), "Modified drag did not pan");
                        require(current.getPresentation().routes().planned().equals(route), "Modified drag edited the route");
                        current.setViewState(before);
                        popup[0] = openMapPopup(frame, systemPoint(current, extension));
                        require(mapAction(popup[0], "map.route.removeWaypoint.text").isEnabled(), "Waypoint remove action unavailable");
                    }
                    case 3 -> {
                        capture(frame, "skia-navigation-popup");
                        if (!offscreen) {
                            BufferedImage menu = new Robot().createScreenCapture(new Rectangle(popup[0].getLocationOnScreen(), popup[0].getSize()));
                            int lightPixels = 0;
                            for (int vertical = 0; vertical < menu.getHeight(); vertical++) {
                                for (int horizontal = 0; horizontal < menu.getWidth(); horizontal++) {
                                    int color = menu.getRGB(horizontal, vertical);
                                    if ((color >> 16 & 255) > 140 && (color >> 8 & 255) > 140 && (color & 255) > 140) {
                                        lightPixels++;
                                    }
                                }
                            }
                            require(lightPixels > 100, "Navigation popup text is not visible over the canvas");
                        }
                        invokeMapAction(popup[0], "map.route.removeWaypoint.text");
                    }
                    case 4 -> {
                        require(current.getPresentation().routes().planned().getLast().equals(destination),
                              "Context-menu removal did not restore the previous endpoint");
                        popup[0] = openMapPopup(frame, systemPoint(current, extension));
                        invokeMapAction(popup[0], "map.route.appendWaypoint.text");
                    }
                    case 5 -> {
                        require(current.getPresentation().routes().planned().getLast().equals(extension), "Menu append failed");
                        popup[0] = openMapPopup(frame, systemPoint(current, destination));
                        invokeMapAction(popup[0], "map.route.trimHere.text");
                    }
                    case 6 -> {
                        require(current.getPresentation().routes().planned().getLast().equals(destination), "Menu trim failed");
                        popup[0] = openMapPopup(frame, systemPoint(current, destination));
                        invokeMapAction(popup[0], "map.route.clear.text");
                    }
                    case 7 -> {
                        require(!tab.hasPlannedRoute(), "Menu clear failed");
                        beforeMeasurement[0] = captureMap(current);
                        popup[0] = openMapPopup(frame, systemPoint(current, origin));
                        invokeMapAction(popup[0], "map.overlay.measure.text");
                    }
                    case 8 -> {
                        require(current.getPresentation().navigation().measurement().enabled(), "Measurement did not enable");
                        clickSystem(frame, origin, 0, 1);
                        sendMouse(find(frame, SkiaLayer.class).getCanvas(), MouseEvent.MOUSE_MOVED,
                              systemPoint(current, destination), 0, MouseEvent.NOBUTTON);
                    }
                    case 9 -> {
                        var measurement = current.getPresentation().navigation().measurement();
                        require(origin.equals(measurement.start()) && destination.equals(measurement.end())
                              && !measurement.label().isBlank(), "Measurement hover preview is missing");
                        require(current.getMeasurementBounds() != null && current.getHoverBounds() != null,
                              "Native measurement label or hover tooltip is missing");
                        Rect measure = current.getMeasurementBounds();
                        Rect hover = current.getHoverBounds();
                        require(measure.getRight() <= hover.getLeft() || measure.getLeft() >= hover.getRight()
                            || measure.getBottom() <= hover.getTop() || measure.getTop() >= hover.getBottom(),
                            "Measurement and hover overlays overlap");
                        require(hover.getLeft() >= 0 && hover.getTop() >= 0 && hover.getRight() <= current.getWidth()
                            && hover.getBottom() <= current.getHeight(), "Hover overlay left the viewport");
                        require(changedPixels(beforeMeasurement[0], captureMap(current)) > 100,
                              "Measurement and hover changed no native pixels");
                        capture(frame, "skia-navigation-measurement");
                        clickSystem(frame, destination, 0, 1);
                        toggle.doClick(0);
                        toggle.doClick(0);
                    }
                    case 10 -> {
                        var measurement = current.getPresentation().navigation().measurement();
                        require(origin.equals(measurement.start()) && destination.equals(measurement.end()),
                              "Measurement endpoints were lost across renderer switching");
                        sendMapKey(frame, KeyEvent.VK_ESCAPE, 0);
                    }
                    case 11 -> {
                        require(!current.getPresentation().navigation().measurement().enabled()
                              && current.getMeasurementBounds() == null, "Escape did not clear measurement");
                        clickSystem(frame, destination, 0, 2);
                    }
                    case 12 -> {
                        require(find(frame, SkiaMap.class) == null, "Double-click did not open the planetary view");
                        tab.switchSystemsMap();
                    }
                    case 13 -> {
                        require(current != null && current.isShowing(), "Native map did not return from planetary view");
                        current.setViewState(new ViewState(far.getX(), far.getY(), 6, far));
                        location.setJumpPath(new JumpPath(new ArrayList<>(List.of(origin, far))));
                    }
                    case 14 -> {
                        require(current.getPresentation().navigation().constraints().stream().anyMatch(constraint ->
                                    constraint.blocked() && constraint.destination().equals(far)),
                              "Active blocked leg did not update the native assessment");
                        require(current.getConstraintMarkersDrawn() > 0, "Blocked route warning was not drawn");
                        boolean expectedRange = current.getPresentation().navigation().jumpRadius() > 0
                              && current.getViewState().scale() > current.getPresentation().navigation().minimumRangeZoom();
                        require(current.isJumpRadiusDrawn() == expectedRange, "Jump-radius settings not honored");
                        capture(frame, "skia-navigation-constraints");
                        location.setJumpPath(null);
                    }
                    case 15 -> {
                        require(current.getConstraintMarkersDrawn() == 0, "Cleared active path left warning markers");
                        sendMapKey(frame, KeyEvent.VK_CONTEXT_MENU, 0);
                        require(current.getNavigationPopup() != null && current.getNavigationPopup().isVisible(),
                              "Keyboard context menu did not open");
                        JPopupMenu activePopup = current.getNavigationPopup();
                        toggle.doClick(0);
                        require(!activePopup.isVisible(), "Context menu survived native surface disposal");
                        toggle.doClick(0);
                        tab.setShowingEmptySystems(showEmpty);
                        find(frame, SkiaMap.class).setViewState(original);
                    }
                    case 16 -> {
                        require(current.getHoverBounds() == null, "Stale tooltip survived map recreation");
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_INTERACTION_CHECK modifiers=checked drag=checked popup="
                            + (offscreen ? "behavior-only" : "pixels")
                            + " measurement=shared doubleClick=checked constraints=updated");
                        checkTabHover(frame, app, toggle);
                    }
                    default -> throw new IllegalStateException("Unexpected navigation interaction phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void checkRoutePixels(SkiaMap map) throws Exception {
        BufferedImage image = captureMap(map);
        int planned = 0;
        int active = 0;
        for (int vertical = 0; vertical < image.getHeight(); vertical++) {
            for (int horizontal = 0; horizontal < image.getWidth(); horizontal++) {
                int color = image.getRGB(horizontal, vertical) & 0xFFFFFF;
                if (nearColor(color, 0x41D2E0)) {
                    planned++;
                } else if (nearColor(color, 0xEBA642)) {
                    active++;
                }
            }
        }
        require(planned > 20 && active > 20,
              "Missing native route pixels: planned=" + planned + " active=" + active);
    }

    private static boolean nearColor(int actual, int expected) {
        return Math.abs((actual >> 16 & 255) - (expected >> 16 & 255)) <= 30
              && Math.abs((actual >> 8 & 255) - (expected >> 8 & 255)) <= 30
              && Math.abs((actual & 255) - (expected & 255)) <= 30;
    }

    private static void checkLandmarks(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        SkiaMap map = find(frame, SkiaMap.class);
        MapTab tab = app.getCampaigngui().getNavigationTab().getMapTab();
        ViewState original = map.getViewState();
        LandmarkLayers originalLayers = tab.getLandmarkLayers();
        PlanetarySystem national = landmarkFixture(map, CapitalType.NATIONAL);
        PlanetarySystem regional = landmarkFixture(map, CapitalType.REGION);
        PlanetarySystem district = landmarkFixture(map, CapitalType.DISTRICT);
        PlanetarySystem station = map.getPresentation().systems().stream()
              .filter(data -> !data.empty() && data.rechargeStations() > 0)
              .map(data -> data.system()).findFirst().orElseThrow();
        JCheckBoxMenuItem recharge = findToggle(frame, "miMapRechargeStations.text");
        tab.setLandmarkLayers(new LandmarkLayers(CapitalDetail.OFF, false));
        map.setViewState(new ViewState(national.getX(), national.getY(), 6, national));
        BufferedImage[] baseline = new BufferedImage[1];
        AtomicInteger phase = new AtomicInteger();
        Timer checks = new Timer(900, event -> {
            try {
                SkiaMap current = find(frame, SkiaMap.class);
                switch (phase.incrementAndGet()) {
                    case 1 -> {
                        require(current.getLandmarkPlacements().isEmpty(), "Landmarks remained visible when disabled");
                        baseline[0] = captureMap(current);
                        findMenuItem(frame, "miMapCapitals.NATIONAL.text").doClick(0);
                    }
                    case 2 -> {
                        require(hasCapital(current, national, CapitalType.NATIONAL), "National capital was not drawn");
                        require(changedPixels(baseline[0], captureMap(current)) > 20, "Capital toggle changed no pixels");
                        checkLandmarkBounds(current);
                        capture(frame, "skia-capital-national");
                        toggle.doClick(0);
                        require(tab.getLandmarkLayers().capitals() == CapitalDetail.NATIONAL,
                              "Capital detail was lost returning to Java2D");
                        toggle.doClick(0);
                        current.setViewState(new ViewState(regional.getX(), regional.getY(), 6, regional));
                    }
                    case 3 -> {
                        require(!hasCapital(current, regional, CapitalType.REGION), "National detail included regional capitals");
                        findMenuItem(frame, "miMapCapitals.REGIONS.text").doClick(0);
                    }
                    case 4 -> {
                        require(hasCapital(current, regional, CapitalType.REGION), "Regional capital was not drawn");
                        checkLandmarkBounds(current);
                        capture(frame, "skia-capital-regional");
                        current.setViewState(new ViewState(district.getX(), district.getY(), 6, district));
                    }
                    case 5 -> {
                        require(!hasCapital(current, district, CapitalType.DISTRICT), "Regional detail included district capitals");
                        findMenuItem(frame, "miMapCapitals.DISTRICTS.text").doClick(0);
                    }
                    case 6 -> {
                        require(hasCapital(current, district, CapitalType.DISTRICT), "District capital was not drawn");
                        checkLandmarkBounds(current);
                        capture(frame, "skia-capital-district");
                        current.setViewState(new ViewState(district.getX(), district.getY(), 0.6, district));
                    }
                    case 7 -> {
                        for (var marker : current.getLandmarkPlacements()) {
                            require(marker.capitalType() != CapitalType.DISTRICT, "District capital did not fade at overview zoom");
                        }
                        findMenuItem(frame, "miMapCapitals.OFF.text").doClick(0);
                        current.setViewState(new ViewState(station.getX(), station.getY(), 6, station));
                    }
                    case 8 -> {
                        require(current.getLandmarkPlacements().isEmpty(), "Disabled station markers were drawn");
                        baseline[0] = captureMap(current);
                        recharge.doClick(0);
                    }
                    case 9 -> {
                        require(current.getLandmarkPlacements().stream().anyMatch(marker ->
                              marker.systemId().equals(station.getId()) && marker.rechargeStations()
                                    == station.getNumberRechargeStations(app.getCampaigngui().getCampaign().getLocalDate())),
                              "Recharge station marker was not drawn with its dated count");
                        require(changedPixels(baseline[0], captureMap(current)) > 20, "Station toggle changed no pixels");
                        checkLandmarkBounds(current);
                        capture(frame, "skia-recharge-stations");
                        toggle.doClick(0);
                        require(tab.getLandmarkLayers().rechargeStations(), "Station visibility was lost returning to Java2D");
                        recharge.doClick(0);
                        toggle.doClick(0);
                    }
                    case 10 -> {
                        require(current.getLandmarkPlacements().isEmpty(), "Java2D station toggle was not shared with Skia");
                        recharge.doClick(0);
                        current.setViewState(new ViewState(station.getX(), station.getY(), 0.6, station));
                    }
                    case 11 -> {
                        require(current.getLandmarkPlacements().isEmpty(), "Station markers did not fade at overview zoom");
                        tab.setLandmarkLayers(originalLayers);
                        current.setViewState(original);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_LANDMARK_CHECK capitals=hierarchy stations=pixels dates=checked zoom=checked controls=shared bounds=nonoverlapping");
                        checkDisputedTerritory(frame, app, toggle);
                    }
                    default -> throw new IllegalStateException("Unexpected landmark phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static PlanetarySystem landmarkFixture(SkiaMap map, CapitalType type) {
        return map.getPresentation().systems().stream().filter(data -> !data.empty() && data.capitalType() == type)
              .map(data -> data.system()).findFirst().orElseThrow(() -> new IllegalStateException("No capital fixture: " + type));
    }

    private static boolean hasCapital(SkiaMap map, PlanetarySystem system, CapitalType type) {
        for (var marker : map.getLandmarkPlacements()) {
            if (marker.systemId().equals(system.getId()) && marker.capitalType() == type) {
                return true;
            }
        }
        return false;
    }

    private static void checkLandmarkBounds(SkiaMap map) {
        List<Rect> occupied = new ArrayList<>();
        checkLabelAnchors(map);
        for (var marker : map.getLandmarkPlacements()) {
            Rect bounds = marker.bounds();
            require(bounds.getLeft() >= 0 && bounds.getTop() >= 0 && bounds.getRight() <= map.getWidth()
                  && bounds.getBottom() <= map.getHeight(), "Landmark outside viewport");
            for (Rect other : occupied) {
                require(bounds.getRight() <= other.getLeft() || bounds.getLeft() >= other.getRight()
                      || bounds.getBottom() <= other.getTop() || bounds.getTop() >= other.getBottom(),
                      "Landmark overlaps another landmark");
            }
            occupied.add(bounds);
        }
    }

    private static void checkDatedPresentation(SkiaMap map, Campaign campaign) {
        require(map.getPresentation().territories().date().equals(campaign.getLocalDate()), "Stale territory date");
        require(map.getRenderedTerritories() > 0, "No native territories rendered");
        require(SkiaMap.getActiveTerritoryPaths() == map.getPresentation().territories().contours().size(),
              "Native territory paths do not match the current atlas");
        for (var data : map.getPresentation().systems()) {
            require(data.name().equals(data.system().getPrintableName(campaign.getLocalDate())), "Stale system name");
            Set<Faction> factions = data.system().getFactionSet(campaign.getLocalDate());
            List<Faction> ordered = new ArrayList<>(factions == null ? Set.of() : factions);
            ordered.sort(Comparator.comparing(Faction::getShortName));
            List<Integer> colors = new ArrayList<>();
            for (Faction faction : ordered) {
                colors.add(faction.getColor().getRGB());
            }
            require(colors.equals(data.factionColors()), "Incorrect dated faction colors: " + data.name());
            List<Faction> capitals = new ArrayList<>();
            for (Faction faction : Factions.getInstance().getFactions()) {
                if ((ordered.contains(faction) || "MERC".equals(faction.getShortName()))
                      && data.system().getId().equals(faction.getStartingPlanet(campaign.getLocalDate()))) {
                    capitals.add(faction);
                }
            }
            capitals.sort(Comparator.comparing(Faction::getShortName));
            List<Integer> capitalColors = new ArrayList<>();
            for (Faction capital : capitals) {
                capitalColors.add(capital.getColor().getRGB());
            }
            require(data.capitalColors().equals(capitalColors), "Incorrect dated capital colors: " + data.name());
            require(data.capitalType() == (capitals.isEmpty() ? data.system().getCapitalType(campaign.getLocalDate())
                  : CapitalType.NATIONAL), "Incorrect dated capital hierarchy: " + data.name());
            require(data.rechargeStations() == data.system().getNumberRechargeStations(campaign.getLocalDate()),
                  "Incorrect dated recharge-station count: " + data.name());
        }
    }

    private static void checkPanVisibility(SkiaMap map, Campaign campaign) {
        ViewState original = map.getViewState();
        try {
            for (double[] offset : new double[][] { { 32, 0 }, { -32, 0 }, { 0, 32 }, { 0, -32 },
                  { 80.25, -60.5 }, { -80.25, 60.5 } }) {
                map.setViewState(new ViewState(original.centerX() + offset[0] / original.scale(),
                      original.centerY() + offset[1] / original.scale(), original.scale(), original.selectedSystem()));
                map.captureNativePng();
                checkVisibility(map, campaign);
            }
                PlanetarySystem selected = original.selectedSystem();
                double padding = UIUtil.scaleForGUI(16);
                for (double[] position : new double[][] { { -padding, map.getHeight() / 2.0 },
                    { map.getWidth() + padding, map.getHeight() / 2.0 },
                    { map.getWidth() / 2.0, -padding }, { map.getWidth() / 2.0, map.getHeight() + padding } }) {
                    map.setViewState(new ViewState(selected.getX() - (position[0] - map.getWidth() / 2.0) / original.scale(),
                        selected.getY() + (position[1] - map.getHeight() / 2.0) / original.scale(),
                        original.scale(), selected));
                    map.captureNativePng();
                    require(map.getVisibleSystemIds().contains(selected.getId()),
                        "System in viewport overscan was culled at " + position[0] + "," + position[1]);
                    checkVisibility(map, campaign);
                }
        } finally {
            map.setViewState(original);
            map.captureNativePng();
        }
        System.out.printf(java.util.Locale.ROOT, "SKIA_PAN_VISIBILITY scale=%.2f fixtures=10 failures=0%n", original.scale());
    }

    private static void checkVisibility(SkiaMap map, Campaign campaign) {
        checkLabelAnchors(map);
        Set<String> actual = new HashSet<>(map.getVisibleSystemIds());
        ViewState state = map.getViewState();
        double size = map.getPresentation().systemStyle().sizeAt(state.scale());
        for (var data : map.getPresentation().systems()) {
            PlanetarySystem system = data.system();
            float radius = (float) Math.max(UIUtil.scaleForGUI(1.8f), Math.min(UIUtil.scaleForGUI(3.2f), size * 0.62));
            if (state.scale() >= 6) {
                radius = (float) Math.max(size + UIUtil.scaleForGUI(5.1f),
                      Math.max(UIUtil.scaleForGUI(2), size * 1.65) * data.star().luminosityScale());
            }
            float horizontal = (float) (map.getWidth() / 2.0 + (system.getX() - state.centerX()) * state.scale());
            float vertical = (float) (map.getHeight() / 2.0 - (system.getY() - state.centerY()) * state.scale());
            radius += UIUtil.scaleForGUI(32);
            boolean inside = (horizontal >= -radius) && (horizontal <= map.getWidth() + radius)
                  && (vertical >= -radius) && (vertical <= map.getHeight() + radius);
            boolean required = Objects.equals(system, state.selectedSystem())
                  || Objects.equals(system, campaign.getCurrentSystem())
                  || map.getPresentation().routeSystemIds().contains(system.getId());
            boolean expected = inside && (!data.empty() || map.getPresentation().showEmptySystems() || required);
            require(actual.contains(system.getId()) == expected, "Visibility mismatch: " + data.name());
        }
    }

    private static void checkLabels(SkiaMap map, boolean ordinaryExpected) {
        var labels = map.getLabelPlacements();
        require(!labels.isEmpty(), "No native labels");
        require(labels.getFirst().systemId().equals(map.getViewState().selectedSystem().getId()),
              "Selected system did not receive label priority");
        if (!ordinaryExpected) {
            for (var label : labels) {
                require(label.systemId().equals(map.getViewState().selectedSystem().getId())
                      || label.systemId().equals(map.getPresentation().routes().currentSystem().getId())
                      || map.getLandmarkPlacements().stream().anyMatch(marker -> marker.systemId().equals(label.systemId())
                            && marker.capitalType() != CapitalType.NONE), "Ordinary labels remained visible at overview zoom");
            }
        } else {
            require(labels.size() > 2, "Ordinary labels did not appear at detailed zoom");
        }
        checkLabelAnchors(map);
    }

    private static void checkLabelAnchors(SkiaMap map) {
        ViewState state = map.getViewState();
        for (var label : map.getLabelPlacements()) {
            for (var data : map.getPresentation().systems()) {
                if (!data.system().getId().equals(label.systemId())) {
                    continue;
                }
                double horizontal = map.getWidth() / 2.0 + (data.system().getX() - state.centerX()) * state.scale();
                double vertical = map.getHeight() / 2.0 - (data.system().getY() - state.centerY()) * state.scale();
                require(Math.abs(label.bounds().getLeft() - horizontal - UIUtil.scaleForGUI(16)) < 0.01,
                      "Label moved horizontally relative to its planet: " + label.text());
                require(Math.abs((label.bounds().getTop() + label.bounds().getBottom()) / 2.0 - vertical) < 0.01,
                      "Label moved vertically relative to its planet: " + label.text());
                break;
            }
        }
    }

    private static void checkSystemRendering(SkiaMap map, boolean detailed) {
        require(!map.getSystemPlacements().isEmpty(), "No system rendering diagnostics");
        double size = map.getPresentation().systemStyle().sizeAt(map.getViewState().scale());
        for (var placement : map.getSystemPlacements()) {
            if (placement.navigationContact()) {
                require(placement.detailAlpha() == 0, "Hidden route contact exposed intrinsic star art");
                continue;
            }
            require(placement.detailAlpha() == (detailed ? 1 : 0), "Incorrect intrinsic star zoom fade");
            require(placement.contactAlpha() == (detailed ? 0 : 1), "Incorrect faction contact zoom fade");
            require(placement.auraRadius() > placement.coreRadius(), "Star glow did not surround its core");
            require(placement.coreRadius() > 0 && Double.isFinite(size), "Invalid stellar dimensions");
        }
        if (detailed) {
            require(map.getStarShaderCount() > 0 && map.getStarShaderCount() <= 12,
                  "Native spectral shaders were not cached by color");
        }
    }

    private static void checkStellarPixels(SkiaMap map) throws Exception {
        var painter = InterstellarMapPanel.class.getDeclaredMethod("drawIntrinsicStar", Graphics2D.class,
              Arc2D.Double.class, PlanetarySystem.class, double.class, double.class, double.class, double.class);
        painter.setAccessible(true);
        int dimension = UIUtil.scaleForGUI(96);
        double size = map.getPresentation().systemStyle().sizeAt(map.getViewState().scale());
        Set<String> variants = new HashSet<>();
        List<BufferedImage> referenceSprites = new ArrayList<>();
        List<BufferedImage> nativeSprites = new ArrayList<>();
        int samples = 0;
        int matched = 0;
        long difference = 0;
        for (var data : map.getPresentation().systems()) {
            if (data.empty() || data.factionColors().isEmpty()
                  || !variants.add(data.star().spectralColor() + ":" + data.star().luminosityScale())) {
                continue;
            }
            BufferedImage reference = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = reference.createGraphics();
            try {
                graphics.setColor(new Color(0x060E14));
                graphics.fillRect(0, 0, dimension, dimension);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.invoke(null, graphics, new Arc2D.Double(), data.system(), dimension / 2.0,
                      dimension / 2.0, size, 1.0);
            } finally {
                graphics.dispose();
            }
            BufferedImage nativeSprite = ImageIO.read(new ByteArrayInputStream(map.captureNativeStarPng(data, dimension)));
            double auraRadius = Math.max(UIUtil.scaleForGUI(2), size * 1.65) * data.star().luminosityScale();
            double ownershipRadius = size + UIUtil.scaleForGUI(2.8f);
            int fixtureSamples = 0;
            int fixtureMatched = 0;
            for (int vertical = 0; vertical < dimension; vertical++) {
                for (int horizontal = 0; horizontal < dimension; horizontal++) {
                    double radius = Math.hypot(horizontal + 0.5 - dimension / 2.0, vertical + 0.5 - dimension / 2.0);
                    if (radius > auraRadius || Math.abs(radius - ownershipRadius) <= UIUtil.scaleForGUI(3)) {
                        continue;
                    }
                    int expected = reference.getRGB(horizontal, vertical);
                    int actual = nativeSprite.getRGB(horizontal, vertical);
                    int maximum = 0;
                    for (int shift = 0; shift <= 16; shift += 8) {
                        int delta = Math.abs((expected >> shift & 255) - (actual >> shift & 255));
                        maximum = Math.max(maximum, delta);
                        difference += delta;
                    }
                    fixtureSamples++;
                    if (maximum <= 25) {
                        fixtureMatched++;
                    }
                }
            }
            require(fixtureSamples > 50 && fixtureMatched >= fixtureSamples * 0.9,
                  "Native star differs from Java2D: " + data.system().getStar() + " matched="
                        + fixtureMatched + "/" + fixtureSamples);
            samples += fixtureSamples;
            matched += fixtureMatched;
            referenceSprites.add(reference);
            nativeSprites.add(nativeSprite);
        }
        require(variants.size() >= 5, "Insufficient spectral/luminosity fixtures");
        require(difference / (double) (samples * 3) < 10, "Native stellar gradient differs from Java2D");
        int columns = 6;
        BufferedImage comparison = new BufferedImage(columns * dimension * 2,
              ((referenceSprites.size() + columns - 1) / columns) * dimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = comparison.createGraphics();
        try {
            graphics.setColor(new Color(0x060E14));
            graphics.fillRect(0, 0, comparison.getWidth(), comparison.getHeight());
            for (int index = 0; index < referenceSprites.size(); index++) {
                int horizontal = index % columns * dimension * 2;
                int vertical = index / columns * dimension;
                graphics.drawImage(referenceSprites.get(index), horizontal, vertical, null);
                graphics.drawImage(nativeSprites.get(index), horizontal + dimension, vertical, null);
            }
        } finally {
            graphics.dispose();
        }
        ImageIO.write(comparison, "png", output("stellar-comparison-java2d-left-skia-right").toFile());
        System.out.printf("SKIA_STELLAR_PIXELS variants=%d matched=%d samples=%d meanChannelError=%.2f%n",
              variants.size(), matched, samples, difference / (double) (samples * 3));
    }

    private static void checkStellarTransition(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        SkiaMap map = find(frame, SkiaMap.class);
        ViewState original = map.getViewState();
        int shaders = map.getStarShaderCount();
        int shaderBuilds = map.getStarShaderBuilds();
        double reference = Math.clamp(map.getPresentation().labelZoomReference(), 2.4, 3.6);
        double midpoint = (Math.clamp(reference * 1.1, 3, 4) + Math.clamp(reference * 1.6, 4.2, 5.6)) / 2;
        map.setViewState(new ViewState(original.centerX(), original.centerY(), midpoint, original.selectedSystem()));
        AtomicInteger phase = new AtomicInteger();
        Timer checks = new Timer(900, event -> {
            try {
                if (phase.incrementAndGet() == 1) {
                    for (var placement : map.getSystemPlacements()) {
                        if (!placement.navigationContact()) {
                            require(placement.detailAlpha() > 0 && placement.detailAlpha() < 1,
                                  "No intrinsic star crossfade at intermediate zoom");
                            require(Math.abs(placement.contactAlpha() + placement.detailAlpha() - 1) < 0.001,
                                  "Star and contact crossfades are not complementary");
                        }
                    }
                      require(map.getStarShaderCount() >= shaders
                          && map.getStarShaderBuilds() - shaderBuilds == map.getStarShaderCount() - shaders,
                          "Zoom rebuilt existing spectral shaders");
                    capture(frame, "skia-stars-transition");
                    map.setViewState(original);
                    toggle.doClick(0);
                    require(map.getStarShaderCount() == 0, "Native star shaders survived surface disposal");
                } else if (phase.get() == 2) {
                    if (!offscreen) {
                        capture(frame, "java2d-stars-matched-camera");
                    }
                    toggle.doClick(0);
                } else {
                    require(map.getStarShaderCount() > 0, "Native star shaders were not recreated");
                    require(map.getViewState().equals(original), "Star comparison changed camera or selection");
                    capture(frame, "skia-stars-matched-camera");
                    ((Timer) event.getSource()).stop();
                    System.out.println("SKIA_STELLAR_CHECK appearance=java2d pixels=compared zoom=crossfaded shaders=reused-and-released");
                    checkFocusInteractions(frame, app, toggle);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void checkFocusPixels(SkiaMap map, boolean detail) throws Exception {
        Class<?> layoutClass = Class.forName("mekhq.gui.InterstellarMapPanel$SystemMarkerLayout");
        Class<?> routeClass = Class.forName("mekhq.gui.InterstellarMapPanel$RouteMarkerState");
        var createLayout = layoutClass.getDeclaredMethod("create", double.class, double.class, double.class,
              routeClass, boolean.class, boolean.class);
        createLayout.setAccessible(true);
        Object noRoute = routeClass.getEnumConstants()[0];
        var ringPainter = InterstellarMapPanel.class.getDeclaredMethod("drawStrategicFocusMarker",
              Graphics2D.class, layoutClass, Color.class);
        var hoverPainter = InterstellarMapPanel.class.getDeclaredMethod("drawHoveredSystemMarker", Graphics2D.class, layoutClass);
        var selectionPainter = InterstellarMapPanel.class.getDeclaredMethod("drawSelectionAnimationMarker",
              Graphics2D.class, layoutClass, double.class);
        var settledPainter = InterstellarMapPanel.class.getDeclaredMethod("drawSelectedSystemMarker",
              Graphics2D.class, layoutClass, String.class, double.class);
        ringPainter.setAccessible(true);
        hoverPainter.setAccessible(true);
        selectionPainter.setAccessible(true);
        settledPainter.setAccessible(true);
        int dimension = UIUtil.scaleForGUI(80);
        float unit = UIUtil.scaleForGUI(1);
        PlanetarySystem system = map.getViewState().selectedSystem();
        BufferedImage comparison = new BufferedImage(dimension * 8, dimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sheet = comparison.createGraphics();
        try {
            for (int sample = 0; sample < 4; sample++) {
                boolean selected = sample > 0;
                double progress = sample <= 1 ? 0 : sample == 2 ? 0.5 : 1;
                Object layout = createLayout.invoke(null, dimension / (2.0 * unit), dimension / (2.0 * unit),
                      map.getPresentation().systemStyle().sizeAt(map.getViewState().scale()) / unit,
                      noRoute, selected, !selected);
                BufferedImage reference = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = reference.createGraphics();
                try {
                    graphics.setColor(new Color(0x060E14));
                    graphics.fillRect(0, 0, dimension, dimension);
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.scale(unit, unit);
                    graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    if (!detail) {
                        ringPainter.invoke(null, graphics, layout, new Color(selected ? 0xEBA642 : 0x41D2E0));
                    } else if (!selected) {
                        hoverPainter.invoke(null, graphics, layout);
                    } else if (progress < 1) {
                        selectionPainter.invoke(null, graphics, layout, progress);
                    } else {
                        settledPainter.invoke(null, graphics, layout, system.getId(), 0.0);
                    }
                } finally {
                    graphics.dispose();
                }
                BufferedImage actual = ImageIO.read(new ByteArrayInputStream(
                      map.captureNativeFocusPng(system, selected, progress, dimension)));
                int pixels = 0;
                int matches = 0;
                long error = 0;
                for (int vertical = 0; vertical < dimension; vertical++) {
                    for (int horizontal = 0; horizontal < dimension; horizontal++) {
                        int expected = reference.getRGB(horizontal, vertical);
                        int observed = actual.getRGB(horizontal, vertical);
                        if ((expected & 0xFFFFFF) == 0x060E14 && (observed & 0xFFFFFF) == 0x060E14) {
                            continue;
                        }
                        int maximum = 0;
                        for (int shift = 0; shift <= 16; shift += 8) {
                            int delta = Math.abs((expected >> shift & 255) - (observed >> shift & 255));
                            maximum = Math.max(maximum, delta);
                            error += delta;
                        }
                        pixels++;
                        if (maximum <= 35) {
                            matches++;
                        }
                    }
                }
                sheet.drawImage(reference, sample * dimension * 2, 0, null);
                sheet.drawImage(actual, (sample * 2 + 1) * dimension, 0, null);
                require(pixels > 20 && matches >= pixels * 0.85 && error / (double) (pixels * 3) < 15,
                      "Focus pixels differ from Java2D: detail=" + detail + " sample=" + sample
                            + " matched=" + matches + "/" + pixels + " error=" + error / (double) (pixels * 3));
            }
        } finally {
            sheet.dispose();
            ImageIO.write(comparison, "png", output(detail ? "focus-brackets-java2d-left-skia-right"
                  : "focus-rings-java2d-left-skia-right").toFile());
        }
    }

    private static void checkFocusInteractions(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        SkiaMap map = find(frame, SkiaMap.class);
        ViewState original = map.getViewState();
        map.setViewState(new ViewState(original.centerX(), original.centerY(), 6, original.selectedSystem()));
        PlanetarySystem[] target = new PlanetarySystem[1];
        AtomicInteger animationSamples = new AtomicInteger();
        AtomicInteger phase = new AtomicInteger();
        Timer checks = new Timer(700, event -> {
            try {
                Component canvas = find(frame, SkiaLayer.class).getCanvas();
                switch (phase.incrementAndGet()) {
                    case 1 -> {
                        checkFocusPixels(map, true);
                        for (var data : map.getPresentation().systems()) {
                            Point point = systemPoint(map, data.system());
                            if (map.getVisibleSystemIds().contains(data.system().getId())
                                  && !Objects.equals(data.system(), original.selectedSystem())
                                  && point.x > UIUtil.scaleForGUI(50) && point.x < map.getWidth() - UIUtil.scaleForGUI(50)
                                  && point.y > UIUtil.scaleForGUI(50) && point.y < map.getHeight() - UIUtil.scaleForGUI(50)) {
                                target[0] = data.system();
                                break;
                            }
                        }
                        require(target[0] != null, "No hover focus fixture");
                        sendMouse(canvas, MouseEvent.MOUSE_MOVED, systemPoint(map, target[0]), 0, MouseEvent.NOBUTTON);
                        map.captureNativePng();
                        require(map.getFocusPlacements().size() == 2, "Hover marker waited for tooltip delay");
                    }
                    case 2 -> {
                        require(map.getFocusPlacements().stream().anyMatch(marker -> !marker.selected()
                              && marker.systemId().equals(target[0].getId()) && marker.bracketAlpha() == 1),
                              "Missing detailed hover brackets");
                        checkFocusLabelClearance(map);
                        capture(frame, "skia-focus-hover");
                        clickSystem(frame, target[0], 0, 1);
                        require(map.isSelectionAnimating(), "Selection click did not start feedback");
                        Timer samples = new Timer(50, sampleEvent -> {
                            if (!map.isSelectionAnimating()) {
                                ((Timer) sampleEvent.getSource()).stop();
                                return;
                            }
                            map.captureNativePng();
                            for (var marker : map.getFocusPlacements()) {
                                if (marker.selected() && marker.progress() > 0 && marker.progress() < 1) {
                                    animationSamples.incrementAndGet();
                                }
                            }
                        });
                        samples.start();
                    }
                    case 3 -> {
                        require(animationSamples.get() > 0, "Selection animation had no intermediate frames");
                        require(!map.isSelectionAnimating(), "Selection timer did not stop after settling");
                        require(map.getFocusPlacements().size() == 1 && map.getFocusPlacements().getFirst().progress() == 1,
                              "Selection did not settle or retained duplicate hover");
                        checkFocusLabelClearance(map);
                        capture(frame, "skia-focus-selected");
                        clickSystem(frame, target[0], 0, 1);
                        require(!map.isSelectionAnimating(), "Repeated selection restarted feedback");
                        sendMouse(canvas, MouseEvent.MOUSE_MOVED, systemPoint(map, target[0]), 0, MouseEvent.NOBUTTON);
                        map.captureNativePng();
                        require(map.getFocusPlacements().size() == 1, "Selected system did not suppress hover marker");
                        map.setViewState(new ViewState(target[0].getX(), target[0].getY(), 0.6, target[0]));
                    }
                    case 4 -> {
                        require(map.getFocusPlacements().getFirst().ringAlpha() == 1
                              && map.getFocusPlacements().getFirst().bracketAlpha() == 0, "Overview focus was not a ring");
                        checkFocusPixels(map, false);
                        capture(frame, "skia-focus-overview");
                        map.setViewState(new ViewState(target[0].getX(), target[0].getY(), 1.6, target[0]));
                    }
                    case 5 -> {
                        var marker = map.getFocusPlacements().getFirst();
                        require(marker.ringAlpha() > 0 && marker.bracketAlpha() > 0
                              && Math.abs(marker.ringAlpha() + marker.bracketAlpha() - 1) < 0.001,
                              "Focus ring/bracket zoom transition was not complementary");
                        capture(frame, "skia-focus-transition");
                        map.setViewState(new ViewState(original.centerX(), original.centerY(), 6, original.selectedSystem()));
                        require(map.isSelectionAnimating(), "Programmatic selection did not animate");
                        map.setVisible(false);
                        require(!map.isSelectionAnimating(), "Hidden map retained selection timer");
                        map.setVisible(true);
                    }
                    case 6 -> {
                        map.setViewState(new ViewState(original.centerX(), original.centerY(), 6, target[0]));
                        require(map.isSelectionAnimating(), "Selection feedback did not resume after showing");
                        toggle.doClick(0);
                        require(!map.isSelectionAnimating() && map.getFocusPlacements().isEmpty(),
                              "Detached map retained focus animation state");
                        toggle.doClick(0);
                    }
                    case 7 -> {
                        require(!map.isSelectionAnimating(), "Renderer attachment replayed selection feedback");
                        sendMouse(find(frame, SkiaLayer.class).getCanvas(), MouseEvent.MOUSE_EXITED,
                              new Point(-1, -1), 0, MouseEvent.NOBUTTON);
                        map.captureNativePng();
                        require(map.getFocusPlacements().size() == 1, "Mouse exit retained hover focus");
                        map.setViewState(original);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_FOCUS_CHECK pixels=java2d hover=immediate selection=animated zoom=crossfaded lifecycle=stopped");
                        checkCartographyControls(frame, app, toggle);
                    }
                    default -> throw new IllegalStateException("Unexpected focus phase");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        checks.start();
    }

    private static void checkFocusLabelClearance(SkiaMap map) {
        checkLabelAnchors(map);
    }

    private static void checkTabHover(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) throws Exception {
        if (offscreen) {
            System.out.println("OFFSCREEN_LIMITATION desktop pixels and tab-hover compositing are not checked");
            checkToggleCycles(frame, app, toggle);
            return;
        }
        SkiaMap surface = find(frame, SkiaMap.class);
        JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, surface);
        require((tabs != null) && (tabs.getTabCount() >= 2), "Map/Locations tabs unavailable");
        Component sidebar = find(app.getCampaigngui().getNavigationTab().getMapTab(), JSplitPane.class).getRightComponent();
        Rectangle sidebarBounds = new Rectangle(sidebar.getLocationOnScreen(), sidebar.getSize());
        sidebarBounds.grow(-UIUtil.scaleForGUI(8), -UIUtil.scaleForGUI(8));
        Robot robot = new Robot();
        BufferedImage baseline = robot.createScreenCapture(sidebarBounds);
        AtomicInteger samples = new AtomicInteger();
        Timer hover = new Timer(30, event -> {
            try {
                BufferedImage actual = robot.createScreenCapture(sidebarBounds);
                int changed = 0;
                for (int vertical = 0; vertical < baseline.getHeight(); vertical++) {
                    for (int horizontal = 0; horizontal < baseline.getWidth(); horizontal++) {
                        if (baseline.getRGB(horizontal, vertical) != actual.getRGB(horizontal, vertical)) {
                            changed++;
                        }
                    }
                }
                if (changed > baseline.getWidth() * baseline.getHeight() / 100) {
                    ImageIO.write(robot.createScreenCapture(frame.getBounds()), "png",
                          output("skia-map-hover-failure").toFile());
                    throw new IllegalStateException("Tab hover changed sidebar pixels: " + changed);
                }
                int sample = samples.incrementAndGet();
                Rectangle tab = tabs.getBoundsAt((sample / 8) % 2);
                var origin = tabs.getLocationOnScreen();
                robot.mouseMove(origin.x + tab.x + tab.width / 2, origin.y + tab.y + tab.height / 2);
                if (sample >= 80) {
                    ((Timer) event.getSource()).stop();
                    capture(frame, "skia-map-tab-hover");
                    checkToggleCycles(frame, app, toggle);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        hover.start();
    }

    private static void checkToggleCycles(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) {
        AtomicInteger cycles = new AtomicInteger();
        Timer switches = new Timer(500, event -> {
            try {
                if (cycles.incrementAndGet() <= 6) {
                    toggle.doClick(0);
                    require(SkiaMap.getActiveSurfaces() == (toggle.isSelected() ? 1 : 0),
                          "Unexpected native peer count after switching maps");
                    if (!toggle.isSelected()) {
                        require(SkiaMap.getActiveTerritoryPaths() == 0, "Native territory paths survived switching maps");
                    }
                    return;
                }
                ((Timer) event.getSource()).stop();
                capture(frame, "skia-map-toggle-cycles");
                frame.dispose();
                app.getCampaignController().deactivate();
                require(SkiaMap.getActiveSurfaces() == 0, "Native peer survived window disposal");
                require(SkiaMap.getActiveTerritoryPaths() == 0, "Native territory paths survived window disposal");
                require(SkiaMap.getActiveEmblemImages() == 0, "Native emblem images survived window disposal");
                require(SkiaMap.getActiveAdministrativePaths() == 0, "Native administrative paths survived window disposal");
                    System.out.printf("%s_MAP_SMOKE_COMPLETE failures=%d frames=%d nativeStars=%d hoverSamples=%d additionalToggles=6%n",
                        offscreen ? "OFFSCREEN" : "REAL", FAILURES.get(), SkiaMap.getCompletedFrames(),
                        SkiaMap.getCompletedStars(), offscreen ? 0 : 80);
                System.exit(0);
            } catch (Exception exception) {
                exception.printStackTrace();
                ((Timer) event.getSource()).stop();
                frame.dispose();
                System.exit(1);
            }
        });
        switches.start();
    }

    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T match = find(container, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            FAILURES.incrementAndGet();
            throw new IllegalStateException(message);
        }
    }

    private static Path output(String name) throws Exception {
        Path directory = Path.of("build", "skiko-stress");
        Files.createDirectories(directory);
        return directory.resolve((offscreen ? "offscreen-" : "") + name + ".png");
    }

    private static boolean hasMapPixels(BufferedImage image) {
        int background = 0;
        int stars = 0;
        for (int vertical = 0; vertical < image.getHeight(); vertical += 2) {
            for (int horizontal = 0; horizontal < image.getWidth(); horizontal += 2) {
                int color = image.getRGB(horizontal, vertical) & 0xFFFFFF;
                int red = (color >> 16) & 255;
                int green = (color >> 8) & 255;
                int blue = color & 255;
                if ((red < 100) && (green < 100) && (blue < 100)) {
                    background++;
                }
                if ((red > 150) && (green > 150) && (blue > 150)) {
                    stars++;
                }
            }
        }
        return (background > 1000) && (stars > 40);
    }

    private static void capture(JFrame frame, String name) throws Exception {
        if (!offscreen) {
            ImageIO.write(new Robot().createScreenCapture(frame.getBounds()), "png", output(name).toFile());
        }
        SkiaMap surface = find(frame, SkiaMap.class);
        if (surface != null) {
            BufferedImage map = captureMap(surface);
            require(hasMapPixels(map), "Native map capture is blank: " + name);
            if (offscreen) {
                ImageIO.write(map, "png", output(name).toFile());
            }
        } else {
            require(find(frame, InterstellarMapPanel.class) != null, "Neither map is attached");
        }
        System.out.println((offscreen ? "OFFSCREEN_MAP_CAPTURE " : "REAL_MAP_CAPTURE ") + name);
    }
}
