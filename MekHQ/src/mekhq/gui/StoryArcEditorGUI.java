package mekhq.gui;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.model.StoryPointTableModel;
import mekhq.gui.panels.StoryPointEditorPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class StoryArcEditorGUI extends JPanel {

    //region Variable Declarations
    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;
    private JFrame frame;
    private MekHQ app;
    private StoryPointHyperLinkListener storyPointHLL;

    /* Menu Bar */
    private JMenuBar menuBar;

    /* Story Point Table */
    private JTable storyPointTable;
    private StoryPointTableModel storyPointTableModel;
    private JScrollPane scrollStoryPointEditor;


    private StoryArc storyArc;
    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.StoryArcEditorGUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    public StoryArcEditorGUI(MekHQ app, StoryArc arc) {
        this.app = app;
        this.storyArc = arc;
        storyPointHLL = new StoryPointHyperLinkListener(storyArc, this);
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

    public StoryPointHyperLinkListener getStoryPointHLL() {
        return storyPointHLL;
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
        frame = new JFrame("Story Arc Editor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        storyPointTableModel = new StoryPointTableModel();
        storyPointTable = new JTable(storyPointTableModel);
        storyPointTable.setRowHeight(60);
        storyPointTable.getColumnModel().getColumn(0).setCellRenderer(storyPointTableModel.getRenderer());
        storyPointTable.setOpaque(false);
        JScrollPane scrollStoryPoints = new JScrollPane(storyPointTable);
        storyPointTable.getSelectionModel().addListSelectionListener(ev -> refreshStoryPointEditor());
        refreshStoryPoints();

        add(scrollStoryPoints, BorderLayout.WEST);

        scrollStoryPointEditor = new JScrollPane();
        scrollStoryPointEditor.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollStoryPointEditor.setViewportView(null);

        add(scrollStoryPointEditor, BorderLayout.CENTER);

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

    public void refreshStoryPoints() {
        final int selected = storyPointTable.getSelectedRow();
        final List<StoryPoint> storyPoints = storyArc.getStoryPoints();
        //storyPoints.sort(new PersonTitleSorter().reversed());
        storyPointTableModel.setData(storyPoints);
        if ((selected > -1) && (selected < storyPoints.size())) {
            storyPointTable.setRowSelectionInterval(selected, selected);
        }
    }

    public void focusOnStoryPoint(UUID id) {
        int row = -1;
        for (int i = 0; i < storyPointTable.getRowCount(); i++) {
            if (storyPointTableModel.getStoryPointAt(i).getId().equals(id)) {
                row = i;
                break;
            }
        }
        if (row != -1) {
            storyPointTable.setRowSelectionInterval(row, row);
            storyPointTable.scrollRectToVisible(storyPointTable.getCellRect(row, 0, true));
        }
    }

    public void refreshStoryPointEditor() {
        int row = storyPointTable.getSelectedRow();
        if (row < 0) {
            scrollStoryPointEditor.setViewportView(null);
            return;
        }
        StoryPoint selectedStoryPoint = storyPointTableModel.getStoryPointAt(storyPointTable.convertRowIndexToModel(row));
        scrollStoryPointEditor.setViewportView(new StoryPointEditorPanel(frame, "story point editor", selectedStoryPoint, this));
        SwingUtilities.invokeLater(() -> scrollStoryPointEditor.getVerticalScrollBar().setValue(0));
    }
}
