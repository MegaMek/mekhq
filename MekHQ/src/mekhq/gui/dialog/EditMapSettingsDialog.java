package mekhq.gui.dialog;

import megamek.common.Board;
import megamek.common.BoardDimensions;
import megamek.common.Configuration;
import megamek.common.MapSettings;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.server.GameManager;
import mekhq.campaign.mission.Scenario;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class EditMapSettingsDialog extends JDialog {

    private JFrame frame;
    private int mapSizeX;
    private int mapSizeY;
    private String map;
    private boolean usingFixedMap;
    private int boardType;

    private JCheckBox checkFixed;
    private JComboBox<String> comboBoardType;
    private JComboBox<BoardDimensions> comboMapSize;
    private JSpinner spnMapX;
    private JSpinner spnMapY;
    private JScrollPane scrChooseMap;
    private JList<String> listMapGenerators;
    private JList<String> listFixedMaps;
    DefaultListModel<String> generatorModel = new DefaultListModel<>();
    DefaultListModel<String> fixedMapModel = new DefaultListModel<>();

    JPanel panSizeRandom;
    JPanel panSizeFixed;

    public EditMapSettingsDialog(JFrame parent, boolean modal, int boardType, boolean usingFixedMap, String map,
                                 int mapSizeX, int mapSizeY) {

        super(parent, modal);
        this.boardType = boardType;
        this.usingFixedMap = usingFixedMap;
        this.map = map;
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;

        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    public int getBoardType() {
        return boardType;
    }

    public boolean getUsingFixedMap() {
        return usingFixedMap;
    }

    public String getMap() {
        return map;
    }

    public int getMapSizeX() {
        return mapSizeX;
    }

    public int getMapSizeY() {
        return mapSizeY;
    }

    private void initComponents() {

        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridBagLayout());
        panSizeRandom = new JPanel(new GridBagLayout());
        panSizeFixed = new JPanel(new BorderLayout());
        JPanel panButtons = new JPanel(new GridLayout(0, 2));

        scrChooseMap = new JScrollPane();

        checkFixed = new JCheckBox("Use fixed map");
        checkFixed.setSelected(usingFixedMap);
        checkFixed.addActionListener(evt -> changeMapType());

        spnMapX = new JSpinner(new SpinnerNumberModel(mapSizeX, 0, null, 1));
        spnMapY = new JSpinner(new SpinnerNumberModel(mapSizeY, 0, null, 1));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        panSizeRandom.add(spnMapX, gbc);
        gbc.gridx++;
        panSizeRandom.add(new JLabel("x"));
        gbc.gridx++;
        gbc.weightx = 1.0;
        panSizeRandom.add(spnMapY);

        comboMapSize = new JComboBox<>();
        for (BoardDimensions size : getBoardSizes()) {
            comboMapSize.addItem(size);
        }
        if(mapSizeX > 0 & mapSizeY > 0) {
            comboMapSize.setSelectedItem(new BoardDimensions(mapSizeX, mapSizeY));
        } else {
            // if no board size yet set, use the default
            comboMapSize.setSelectedItem(new BoardDimensions(16, 17));
        }
        comboMapSize.addActionListener(evt -> refreshBoardList());
        panSizeFixed.add(comboMapSize, BorderLayout.CENTER);

        listMapGenerators = new JList<>(generatorModel);
        listMapGenerators.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        generatorModel.addElement("None");
        File dir = new File("data/mapgen/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if(child.isFile()) {
                    String s = child.getName().replace(".xml", "");
                    generatorModel.addElement(s);
                }
            }
        }
        if(!usingFixedMap) {
            listMapGenerators.setSelectedValue(map, true);
            scrChooseMap.setViewportView(listMapGenerators);
        }

        listFixedMaps = new JList<>(fixedMapModel);
        listFixedMaps.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshBoardList();
        if(usingFixedMap) {
            listFixedMaps.setSelectedValue(map, true);
            scrChooseMap.setViewportView(listFixedMaps);
        }

        comboBoardType = new JComboBox();
        for (int i = Scenario.T_GROUND; i <= Scenario.T_SPACE; i++) {
            comboBoardType.addItem(Scenario.getBoardTypeName(i));
        }
        comboBoardType.addActionListener(evt -> changeBoardType());
        comboBoardType.setSelectedIndex(boardType);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel("Board Type:"), gbc);
        gbc.weightx = 1.0;
        gbc.gridx++;
        panMain.add(comboBoardType, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        panMain.add(new JLabel("Map Size:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        panMain.add(panSizeRandom, gbc);
        panMain.add(panSizeFixed, gbc);
        if(usingFixedMap) {
            panSizeRandom.setVisible(false);
        } else {
            panSizeFixed.setVisible(false);
        }

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy++;
        panMain.add(checkFixed, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panMain.add(scrChooseMap, gbc);

        JButton btnOK = new JButton("Done");
        btnOK.addActionListener(evt -> done());
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(evt -> cancel());
        panButtons.add(btnOK);
        panButtons.add(btnCancel);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
    }

    private void changeBoardType() {
        if(comboBoardType.getSelectedIndex() == Scenario.T_SPACE) {
            checkFixed.setSelected(false);
            checkFixed.setEnabled(false);
            panSizeRandom.setVisible(true);
            panSizeFixed.setVisible(false);
            listMapGenerators.setSelectedIndex(0);
            listMapGenerators.setEnabled(false);
            listFixedMaps.setEnabled(false);
        } else {
            checkFixed.setEnabled(true);
            listMapGenerators.setEnabled(true);
            listFixedMaps.setEnabled(true);
        }
    }

    private void changeMapType() {
        if(checkFixed.isSelected()) {
            panSizeRandom.setVisible(false);
            panSizeFixed.setVisible(true);
            scrChooseMap.setViewportView(listFixedMaps);
        } else {
            panSizeRandom.setVisible(true);
            panSizeFixed.setVisible(false);
            scrChooseMap.setViewportView(listMapGenerators);
        }
    }

    private void refreshBoardList() {
        List<String> boards = scanForBoards();
        fixedMapModel.removeAllElements();
        fixedMapModel.addAll(boards);
    }

    private Set<BoardDimensions> getBoardSizes() {
        TreeSet<BoardDimensions> board_sizes = new TreeSet<>();

        File boards_dir = Configuration.boardsDir();
        // Slightly overkill sanity check...
        if (boards_dir.isDirectory()) {
            getBoardSizesInDir(boards_dir, board_sizes);
        }

        return board_sizes;
    }

    /**
     * Recursively scan the specified path to determine the board sizes
     * available.
     *
     * @param searchDir The directory to search below this path (may be null for all
     *                  in base path).
     * @param sizes     Where to store the discovered board sizes
     */
    private void getBoardSizesInDir(final File searchDir, TreeSet<BoardDimensions> sizes) {
        if (searchDir == null) {
            throw new IllegalArgumentException("must provide searchDir");
        }

        if (sizes == null) {
            throw new IllegalArgumentException("must provide sizes");
        }

        String[] file_list = searchDir.list();

        if (file_list != null) {
            for (String filename : file_list) {
                File query_file = new File(searchDir, filename);

                if (query_file.isDirectory()) {
                    getBoardSizesInDir(query_file, sizes);
                } else {
                    try {
                        if (filename.endsWith(".board")) {
                            BoardDimensions size = Board.getSize(query_file);
                            if (size == null) {
                                throw new Exception();
                            }
                            sizes.add(Board.getSize(query_file));
                        }
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error parsing board: " + query_file.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    /**
     * Returns a list of path names of available boards of the size set in the given
     * mapSettings. The path names are minus the '.board' extension and relative to
     * the boards data directory.
     */
    private List<String> scanForBoards() {
        BoardDimensions boardSize = (BoardDimensions) comboMapSize.getSelectedItem();
        java.util.List<String> result = new ArrayList<>();

        // Scan the Megamek boards directory
        File boardDir = Configuration.boardsDir();
        scanForBoardsInDir(boardDir, "", boardSize, result);

        result.sort(String::compareTo);
        return result.stream().map(this::backToForwardSlash).collect(Collectors.toList());
    }

    private String backToForwardSlash(String path) {
        return path.replace("\\", "/");
    }

    /**
     * Scans the given boardDir directory for map boards of the given size and
     * returns them by adding them to the given boards list. Removes the .board extension.
     */
    private void scanForBoardsInDir(final File boardDir, final String basePath, final BoardDimensions dimensions,
                                    List<String> boards) {
        if (boardDir == null) {
            throw new IllegalArgumentException("must provide searchDir");
        } else if (basePath == null) {
            throw new IllegalArgumentException("must provide basePath");
        } else if (dimensions == null) {
            throw new IllegalArgumentException("must provide dimensions");
        } else if (boards == null) {
            throw new IllegalArgumentException("must provide boards");
        }

        String[] fileList = boardDir.list();
        if (fileList != null) {
            for (String filename : fileList) {
                File filePath = new MegaMekFile(boardDir, filename).getFile();
                if (filePath.isDirectory()) {
                    scanForBoardsInDir(filePath, basePath + File.separator + filename, dimensions, boards);
                } else {
                    if (filename.endsWith(".board")) {
                        if (Board.boardIsSize(filePath, dimensions)) {
                            boards.add(basePath + File.separator + filename.substring(0, filename.lastIndexOf(".")));
                        }
                    }
                }
            }
        }
    }

    public void done() {
        boardType = comboBoardType.getSelectedIndex();
        usingFixedMap = checkFixed.isSelected();
        if(usingFixedMap) {
            map = listFixedMaps.getSelectedValue();
            BoardDimensions boardSize = (BoardDimensions) comboMapSize.getSelectedItem();
            mapSizeX = boardSize.width();
            mapSizeY = boardSize.height();
        } else {
            map = listMapGenerators.getSelectedValue();
            if(listMapGenerators.getSelectedIndex() == 0) {
                map = null;
            }
            mapSizeX = (int) spnMapX.getValue();
            mapSizeY = (int) spnMapY.getValue();
        }
        setVisible(false);
    }

    public void cancel() {
        setVisible(false);
    }

}
