package mekhq.gui;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryArc;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

public class StoryArcEditorGUI extends JPanel {

    //region Variable Declarations
    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;
    private JFrame frame;
    private MekHQ app;

    /* Menu Bar */
    private JMenuBar menuBar;
    private StoryArc storyArc;
    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.StoryArcEditorGUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    public StoryArcEditorGUI(MekHQ app, StoryArc arc) {
        this.app = app;
        this.storyArc = arc;
        initComponents();
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    protected MekHQ getApplication() {
        return app;
    }

    protected Campaign getCampaign() {
        return getApplication().getCampaign();
    }

    protected ResourceBundle getResourceMap() {
        return null;//resourceMap;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initComponents() {
        frame = new JFrame("MekHQ");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        JLabel test = new JLabel("Just a Test!");
        add(test, BorderLayout.CENTER);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize(Math.min(MAX_START_WIDTH, dim.width),
                Math.min(MAX_START_HEIGHT, dim.height));

        // Determine the new location of the window
        int w = frame.getSize().width;
        int h = frame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;

        // Move the window
        frame.setLocation(x, y);

        initMenu();
        frame.setJMenuBar(menuBar);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();

        if (isMacOSX()) {
            enableFullScreenMode(frame);
        }

        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                getApplication().exit();
            }
        });

    }

    private void initMenu() {
        menuBar = new JMenuBar();

        JMenu menuFile = new JMenu(resourceMap.getString("fileMenu.text"));
        menuFile.setMnemonic(KeyEvent.VK_F);
        JMenuItem miNewArc = new JMenuItem(resourceMap.getString("miNewArc.text"));
        miNewArc.setMnemonic(KeyEvent.VK_N);
        menuFile.add(miNewArc);
        JMenuItem miLoadArc = new JMenuItem(resourceMap.getString("miLoadArc.text"));

        miLoadArc.setMnemonic(KeyEvent.VK_L);
        menuFile.add(miLoadArc);
        JMenuItem miSaveArc = new JMenuItem(resourceMap.getString("miSaveArc.text"));
        miSaveArc.setMnemonic(KeyEvent.VK_S);
        menuFile.add(miSaveArc);

        menuBar.add(menuFile);
    }
    //endregion Initialization

    private static boolean isMacOSX() {
        return System.getProperty("os.name").contains("Mac OS X");
    }

    private static void enableFullScreenMode(Window window) {
        String className = "com.apple.eawt.FullScreenUtilities";
        String methodName = "setWindowCanFullScreen";

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, Window.class, boolean.class);
            method.invoke(null, window, true);
        } catch (Throwable t) {
            LogManager.getLogger().error("Full screen mode is not supported", t);
        }
    }
}
