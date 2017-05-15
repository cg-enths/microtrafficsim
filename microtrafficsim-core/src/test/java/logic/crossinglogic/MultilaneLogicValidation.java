package logic.crossinglogic;

import com.jogamp.newt.event.KeyEvent;
import logic.crossinglogic.scenarios.MultilaneScenario;
import microtrafficsim.core.convenience.mapviewer.MapViewer;
import microtrafficsim.core.convenience.mapviewer.TileBasedMapViewer;
import microtrafficsim.core.logic.streetgraph.Graph;
import microtrafficsim.core.simulation.builder.impl.VehicleScenarioBuilder;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.core.simulation.core.Simulation;
import microtrafficsim.core.simulation.core.VehicleSimulation;
import microtrafficsim.core.simulation.scenarios.Scenario;
import microtrafficsim.core.simulation.scenarios.impl.AreaScenario;
import microtrafficsim.core.simulation.scenarios.impl.RandomRouteScenario;
import microtrafficsim.core.vis.UnsupportedFeatureException;
import microtrafficsim.core.vis.simulation.SpriteBasedVehicleOverlay;
import microtrafficsim.core.vis.simulation.VehicleOverlay;
import microtrafficsim.utils.logging.EasyMarkableLogger;
import microtrafficsim.utils.logging.LoggingLevel;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Dominic Parga Cacheiro
 */
public class MultilaneLogicValidation {
    private static Logger logger = new EasyMarkableLogger(MultilaneLogicValidation.class);


    public static void main(String[] args) {
        /* build setup: scenario */
        LoggingLevel.setEnabledGlobally(false, true, true, true, true);


        /* simulation config */
        SimulationConfig config = new SimulationConfig();
        MultilaneScenario.setupConfig(config);


        SwingUtilities.invokeLater(() -> {
            /* visualization */
            MapViewer mapviewer    = new TileBasedMapViewer(config.visualization.style);
            VehicleOverlay overlay = new SpriteBasedVehicleOverlay(
                    mapviewer.getProjection(),
                    config.visualization.style);
            try {
                mapviewer.create(config);
            } catch (UnsupportedFeatureException e) {
                e.printStackTrace();
            }
            mapviewer.addOverlay(0, overlay);


            /* setup JFrame */
            JFrame frame = new JFrame("MicroTrafficSim - Validation Scenario");
            frame.setSize(mapviewer.getInitialWindowWidth(), mapviewer.getInitialWindowHeight());
            frame.add(mapviewer.getVisualizationPanel());

            /*
             * Note: JOGL automatically calls glViewport, we need to make sure that this
             * function is not called with a height or width of 0! Otherwise the program
             * crashes.
             */
            frame.setMinimumSize(new Dimension(100, 100));

            /* on close: stop the visualization and exit */
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    mapviewer.stop();
                    System.exit(0);
                }
            });


            /* show */
            frame.setLocationRelativeTo(null);    // center on screen; close to setVisible
            frame.setVisible(true);
            mapviewer.show();


            /* setup graph */
            MultilaneTestGraph result = new MultilaneTestGraph(config);
            Graph graph = result.graph;
            try {
                mapviewer.setMap(result.segment);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Runtime.getRuntime().halt(1);
            }


            logger.debug("\n" + graph);


            /* initialize the simulation */
//            QueueScenarioSmall scenario = new MultilaneScenario(config, graph, overlay.getVehicleFactory());
//            scenario.setLooping(true);
            AreaScenario scenario = new RandomRouteScenario(config.seed, config, graph);
            Simulation sim = new VehicleSimulation();
            overlay.setSimulation(sim);

            scenario.redefineMetaRoutes();
            try {
                new VehicleScenarioBuilder(config.seed, overlay.getVehicleFactory()).prepare(scenario);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sim.setAndInitPreparedScenario(scenario);

            sim.runOneStep();


            /* shortcuts */
            mapviewer.addKeyCommand(KeyEvent.EVENT_KEY_RELEASED,
                    KeyEvent.VK_SPACE,
                    e -> {
                        if (sim.isPaused())
                            sim.run();
                        else
                            sim.cancel();
                    }
            );

            mapviewer.addKeyCommand(KeyEvent.EVENT_KEY_RELEASED,
                    KeyEvent.VK_RIGHT,
                    e -> {
                        sim.cancel();
                        sim.runOneStep();
                    }
            );
        });
    }
}
