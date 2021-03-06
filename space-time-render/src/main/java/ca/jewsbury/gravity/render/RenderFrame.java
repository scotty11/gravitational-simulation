package ca.jewsbury.gravity.render;

import ca.jewsbury.gravity.form.RenderPropertiesForm;
import ca.jewsbury.gravity.render.engine.DefaultSimulationSet;
import ca.jewsbury.gravity.render.engine.SimulationEngine;
import ca.jewsbury.gravity.render.engine.SimulationSet;
import ca.jewsbury.gravity.util.enumerated.SimulationEngineSignal;
import ca.jewsbury.gravity.render.panel.ConfigPanel;
import ca.jewsbury.gravity.render.panel.GraphPanel;
import ca.jewsbury.gravity.render.panel.ImagePanel;
import ca.jewsbury.gravity.render.panel.UniversePanel;
import ca.jewsbury.gravity.spacetime.SpaceTimeException;
import ca.jewsbury.gravity.util.RenderUtils;
import ca.jewsbury.gravity.util.factory.SimulationSetFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RenderFrame.class
 *
 * Root frame for render project. Defines all GUI functionality.
 *
 * 14-Feb-2015
 *
 * @author Nathan
 */
public class RenderFrame extends ComponentAdapter implements ActionListener, MouseWheelListener {

    public static final Font DISPLAY_FONT = new Font("Courier New", Font.BOLD, 12);

    private final Logger logger = LoggerFactory.getLogger(RenderFrame.class);
    private final String DEFAULT_SIM_DEFINITIONS = "default-sim.json";
    private final String SPACE_BG_IMAGE = "space.jpg";
    private final int CONFIG_WIDTH = 200;
    private final Dimension MINIMUM_PANEL_DIMENSION;
    //
    private Map<String, SimulationSet> simulationSet;
    private SimulationEngine currentSimulation;
    private Thread simulationThread;
    private boolean isPaused = false;
    //
    private UniversePanel universePanel;
    private JFrame renderFrame;
    private ConfigPanel configPanel;
    private GraphPanel graphPanel;
    private ImagePanel bgPanel;
    //
    private JSplitPane rootDivider, configDivider;

    private boolean isVisible = false;

    public RenderFrame(Dimension minimum, Map<String, SimulationSet> simulationSet) {
        MINIMUM_PANEL_DIMENSION = new Dimension((int) minimum.getHeight(), (int) minimum.getHeight());
        currentSimulation = null;
        initializeSimulationSet(simulationSet);
        initializeJPanels(MINIMUM_PANEL_DIMENSION);
        initializeJFrame(minimum);
    }

    /**
     * Initialize the simulation set to have at least the one default entry, or
     * at most the sum of the file provided defaults and provided simulation
     * sets.
     *
     * @param providedSet
     */
    private void initializeSimulationSet(Map<String, SimulationSet> providedSet) {
        // Read in the default JSON sets.
        this.simulationSet = SimulationSetFactory.generateSimulationSetFromFile(DEFAULT_SIM_DEFINITIONS);
        if (this.simulationSet == null) {
            this.simulationSet = new HashMap<String, SimulationSet>();
        }
        //Merge the two sets.
        if (providedSet != null) {
            logger.info("Merging provided sets....");
            this.simulationSet.putAll(providedSet);
            logger.info("Merge complete, new record set contains: " + this.simulationSet.size() + " entries.");
        } else {
            logger.debug("Provided set is null. Using only default simulation sets.");
        }

        if (this.simulationSet.isEmpty()) {
            logger.debug("Unable to load any simulation sets. Adding the hard coded default.");
            this.simulationSet.put("default", new DefaultSimulationSet());
        }
    }

    private void initializeJPanels(Dimension minimum) {
        logger.trace("Initializing all JPanel Objects...");

        logger.trace("Initializing JPanel - Background Panel.");
        bgPanel = new ImagePanel(SPACE_BG_IMAGE, minimum);
        logger.trace("Background Panel created - [" + bgPanel.getWidth() + ", " + bgPanel.getHeight() + "]");

        logger.trace("Initializing JPanel - Universe Panel.");
        universePanel = new UniversePanel(minimum);
        universePanel.addMouseWheelListener(this);
        logger.trace("Universe Panel created - [" + universePanel.getWidth() + ", " + universePanel.getHeight() + "]");

        logger.trace("Initializing JPanel - Config Panel.");
        configPanel = new ConfigPanel(this);
        RenderUtils.setScale(0.5);
        configPanel.setBackground(Color.darkGray);
        configPanel.setNewSize(new Dimension(CONFIG_WIDTH, (int) minimum.getHeight() - CONFIG_WIDTH));
        logger.trace("Config Panel created - [" + configPanel.getWidth() + ", " + configPanel.getHeight() + "]");

        logger.trace("Initializing JPanel - Graph Panel.");
        graphPanel = new GraphPanel(new Dimension(CONFIG_WIDTH, CONFIG_WIDTH));
        logger.trace("Graph Panel created - [" + graphPanel.getWidth() + ", " + graphPanel.getHeight() + "]");

    }

    private void initializeJFrame(Dimension minimum) {
        logger.trace("Initializing JFrame.");

        renderFrame = new JFrame("PHYS 4250 Final Project - Gravitational Simulation Render");
        renderFrame.setResizable(true);
        renderFrame.setMinimumSize(minimum);
        renderFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Toolkit.getDefaultToolkit().setDynamicLayout(false);
        renderFrame.addComponentListener(this);
        logger.trace("JFrame created - [" + renderFrame.getWidth() + ", " + renderFrame.getHeight() + "]");
        addGuiComponents();
    }

    private void addGuiComponents() {
        JLayeredPane displayPane = new JLayeredPane();

        if (renderFrame != null && universePanel != null) {
            configDivider = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            rootDivider = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

            displayPane.setPreferredSize(universePanel.getPreferredSize());
            displayPane.setMinimumSize(universePanel.getMinimumSize());

            displayPane.add(bgPanel, new Integer(1));
            displayPane.add(universePanel, new Integer(2));

            configDivider.setTopComponent(configPanel);
            configDivider.setBottomComponent(graphPanel);
            configDivider.setEnabled(false);

            rootDivider.setLeftComponent(displayPane);
            rootDivider.setRightComponent(configDivider);
            rootDivider.setEnabled(false);
            renderFrame.setContentPane(rootDivider);
        }
    }

    public void display() {
        if (renderFrame != null) {
            logger.trace("Displaying RenderFrame.");
            renderFrame.setVisible(true);
            isVisible = true;
        } else {
            logger.error("Attempted to display jFrame before creation.");
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        Dimension panelDimension, configDimension;
        int width, height;
        if (isVisible) {
            logger.trace("Redraw event :: " + renderFrame.getWidth() + ", " + renderFrame.getHeight());
            width = renderFrame.getWidth() - CONFIG_WIDTH;
            height = renderFrame.getHeight();

            panelDimension = new Dimension(width, height);
            configDimension = new Dimension(CONFIG_WIDTH, height - CONFIG_WIDTH);

            this.configPanel.setNewSize(configDimension);
            this.bgPanel.setNewSize(panelDimension);
            this.universePanel.setNewSize(panelDimension);

            this.configDivider.setDividerLocation(configPanel.getHeight());
            this.rootDivider.setDividerLocation(universePanel.getWidth());
        }
    }

    private void setupNewSimulation() {
        RenderPropertiesForm propForm;

        logger.info("Starting new simulation!");
        propForm = new RenderPropertiesForm();
        try {
            propForm.gatherProperties(configPanel, simulationSet);
            if (propForm.isValid()) {
                currentSimulation = new SimulationEngine(this);
                currentSimulation.setProperties(propForm);

                renderFrame.validate();
                RenderUtils.setScale(propForm.getDisplayScale());
                if (currentSimulation.initializeSimulation()) {
                    this.universePanel.setTraceOrbits(propForm.isTraceOrbits());
                    simulationThread = new Thread(currentSimulation);
                    logger.info("Simulation thread initialized.");
                } else {
                    JOptionPane.showMessageDialog(renderFrame,
                            "Unable to initialize simulation", "SpaceTime Exception",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SpaceTimeException e) {
            JOptionPane.showMessageDialog(renderFrame,
                    e.getMessage(), "SpaceTime Exception",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * This method converts the given actionCommand to an integer for use with
     * the 'actionPerformed' method. This is implemented only to allow targeting
     * Java 1.6+.
     *
     * @param action
     * @return
     */
    private int getActionIndex(String action) {
        int actionIndex = 0;

        if (StringUtils.isNotBlank(action)) {
            if (StringUtils.equalsIgnoreCase("NEW", action)) {
                actionIndex = 1;
            } else if (StringUtils.equalsIgnoreCase("PAUSE", action)) {
                actionIndex = 2;
            } else if (StringUtils.equalsIgnoreCase("PLAY", action)) {
                actionIndex = 3;
            } else if (StringUtils.equalsIgnoreCase("STOP", action)) {
                actionIndex = 4;
            }
        }
        return actionIndex;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int actionIndex = 0;
        if (e != null && e.getActionCommand() != null) {
            actionIndex = getActionIndex(e.getActionCommand());
            switch (actionIndex) {
                case 1: {
                    logger.info("Run New Simulation.");
                    // START NEW SIMULATION
                    setupNewSimulation();
                    //
                    configPanel.getPlaySim().setEnabled(true);
                    configPanel.getPauseSim().setEnabled(false);
                    configPanel.getStopSim().setEnabled(true);
                    configPanel.getNewSim().setEnabled(false);
                    break;
                }
                case 2: {
                    if (!isPaused) {
                        logger.info("Pause simulation.");
                        // PAUSE SIMULATION
                        if (currentSimulation != null) {
                            currentSimulation.sendSignal(SimulationEngineSignal.PAUSE);
                            try {
                                simulationThread.join();
                            } catch (InterruptedException ex) {
                                logger.error(ex.getMessage());
                            }
                        }
                        //
                        isPaused = true;
                        configPanel.getPauseSim().setEnabled(false);
                        configPanel.getPlaySim().setEnabled(true);
                        configPanel.getPlaySim().setText("Resume simulation");
                    }
                    break;
                }
                case 3: {
                    if (isPaused) { // Simulation was paused, restart the simulation
                        logger.info("Resume simulation");
                        isPaused = false;

                        if (currentSimulation != null && simulationThread != null) {
                            simulationThread = new Thread(currentSimulation);
                            currentSimulation.sendSignal(SimulationEngineSignal.RESUME);
                            simulationThread.start();
                        }
                    } else {
                        logger.info("New Simulation.");
                        if (simulationThread != null) {
                            simulationThread.start();
                        }
                    }

                    configPanel.getNewSim().setEnabled(false);
                    configPanel.getPauseSim().setEnabled(true);
                    configPanel.getPlaySim().setEnabled(false);
                    configPanel.getStopSim().setEnabled(true);
                    break;
                }
                case 4: {
                    logger.info("Stop Simulation");
                    isPaused = false;

                    if (currentSimulation != null && simulationThread != null) {
                        if (simulationThread.isAlive()) {
                            currentSimulation.sendSignal(SimulationEngineSignal.STOP);
                            try {
                                simulationThread.join();
                                simulationThread = null;
                                currentSimulation = null;

                            } catch (InterruptedException ex) {
                                logger.error(ex.getMessage());
                            }
                        }
                    }

                    universePanel.refreshPanel();
                    configPanel.getNewSim().setEnabled(true);
                    configPanel.getPauseSim().setEnabled(false);
                    configPanel.getPlaySim().setEnabled(false);
                    configPanel.getStopSim().setEnabled(false);
                    configPanel.getPlaySim().setText("Play simulation");
                    break;
                }
            }
        }
    }

    public Map<String, SimulationSet> getSimulationSet() {
        return simulationSet;
    }

    public UniversePanel getUniversePanel() {
        return universePanel;
    }

    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    public void repaint() {
        if (renderFrame != null) {
            renderFrame.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        double currentScale = RenderUtils.getScale();
        double notchTranslation = 0.05;
        
        if( currentScale <= 0.1 ) {
            notchTranslation = 0.005;
        }
        double amount = -(notches * notchTranslation);

        currentScale += amount;
        if( currentScale >= 0.01 ) {
            RenderUtils.setScale(currentScale);
        } 
        this.configPanel.getScaleInput().setValue(currentScale);
    }
}
