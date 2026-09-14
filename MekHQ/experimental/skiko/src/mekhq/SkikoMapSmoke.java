package mekhq;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
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
import mekhq.gui.CampaignGUI;
import mekhq.gui.ExperimentalMapView.ViewState;
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

    public static void main(String... args) throws Exception {
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
                            checkPresentation(frame, app, toggle);
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
                        checkLabels(current, true);
                        capture(frame, "skia-labels-detail");
                        current.setViewState(new ViewState(original.centerX(), original.centerY(), 0.6,
                              original.selectedSystem()));
                    }
                    case 2 -> {
                        checkLabels(current, false);
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
                        campaign.setLocalDate(originalDate);
                        current.refresh();
                        current.setViewState(original);
                    }
                    case 6 -> {
                        checkDatedPresentation(current, campaign);
                        checkLabels(current, true);
                        ((Timer) event.getSource()).stop();
                        System.out.println("SKIA_PRESENTATION_CHECK labels=nonoverlapping colors=dated filters=shared zoom=checked");
                        checkTabHover(frame, app, toggle);
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

    private static void checkDatedPresentation(SkiaMap map, Campaign campaign) {
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
        }
    }

    private static void checkVisibility(SkiaMap map, Campaign campaign) {
        Set<String> actual = new HashSet<>(map.getVisibleSystemIds());
        float radius = UIUtil.scaleForGUI(2);
        ViewState state = map.getViewState();
        for (var data : map.getPresentation().systems()) {
            PlanetarySystem system = data.system();
            float horizontal = (float) (map.getWidth() / 2.0 + (system.getX() - state.centerX()) * state.scale());
            float vertical = (float) (map.getHeight() / 2.0 - (system.getY() - state.centerY()) * state.scale());
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
            require(labels.size() <= 2, "Ordinary labels remained visible at overview zoom");
        } else {
            require(labels.size() > 2, "Ordinary labels did not appear at detailed zoom");
        }
        for (int index = 0; index < labels.size(); index++) {
            Rect bounds = labels.get(index).bounds();
            require((bounds.getLeft() >= 0) && (bounds.getTop() >= 0) && (bounds.getRight() <= map.getWidth())
                  && (bounds.getBottom() <= map.getHeight()), "Label outside viewport");
            for (int other = index + 1; other < labels.size(); other++) {
                Rect compared = labels.get(other).bounds();
                require((bounds.getRight() <= compared.getLeft()) || (bounds.getLeft() >= compared.getRight())
                      || (bounds.getBottom() <= compared.getTop()) || (bounds.getTop() >= compared.getBottom()),
                      "Overlapping native labels");
            }
        }
    }

    private static void checkTabHover(JFrame frame, MekHQ app, JCheckBoxMenuItem toggle) throws Exception {
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
                    return;
                }
                ((Timer) event.getSource()).stop();
                capture(frame, "skia-map-toggle-cycles");
                frame.dispose();
                app.getCampaignController().deactivate();
                require(SkiaMap.getActiveSurfaces() == 0, "Native peer survived window disposal");
                System.out.printf("REAL_MAP_SMOKE_COMPLETE failures=%d frames=%d nativeStars=%d hoverSamples=80 additionalToggles=6%n",
                      FAILURES.get(), SkiaMap.getCompletedFrames(), SkiaMap.getCompletedStars());
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
        return directory.resolve(name + ".png");
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
                if (color == 0x060E14) {
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
        ImageIO.write(new Robot().createScreenCapture(frame.getBounds()), "png", output(name).toFile());
        SkiaMap surface = find(frame, SkiaMap.class);
        if (surface != null) {
            BufferedImage map = new Robot().createScreenCapture(new Rectangle(surface.getLocationOnScreen(), surface.getSize()));
            require(hasMapPixels(map), "Native map capture is blank: " + name);
        } else {
            require(find(frame, InterstellarMapPanel.class) != null, "Neither map is attached");
        }
        System.out.println("REAL_MAP_CAPTURE " + name);
    }
}
