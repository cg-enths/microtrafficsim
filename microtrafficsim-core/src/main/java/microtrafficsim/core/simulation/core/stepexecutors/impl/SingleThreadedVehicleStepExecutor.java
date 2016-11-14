package microtrafficsim.core.simulation.core.stepexecutors.impl;

import microtrafficsim.core.logic.Node;
import microtrafficsim.core.logic.vehicles.AbstractVehicle;
import microtrafficsim.core.simulation.core.stepexecutors.VehicleStepExecutor;

import java.util.Iterator;


/**
 * A single-threaded implementation of {@link VehicleStepExecutor}.
 *
 * @author Dominic Parga Cacheiro
 */
public class SingleThreadedVehicleStepExecutor implements VehicleStepExecutor {

    @Override
    public void willMoveAll(Iterator<AbstractVehicle> iteratorSpawned) {
        iteratorSpawned.forEachRemaining((AbstractVehicle vehicle) -> {
            vehicle.accelerate();
            vehicle.dash();
            vehicle.brake();
            vehicle.dawdle();
        });
    }

    @Override
    public void moveAll(Iterator<AbstractVehicle> iteratorSpawned) {
        while (iteratorSpawned.hasNext())
            iteratorSpawned.next().move();
    }

    @Override
    public void didMoveAll(Iterator<AbstractVehicle> iteratorSpawned) {
        iteratorSpawned.forEachRemaining(AbstractVehicle::didMove);
    }

    @Override
    public void spawnAll(Iterator<AbstractVehicle> iteratorNotSpawned) {
        iteratorNotSpawned.forEachRemaining(AbstractVehicle::spawn);
    }

    @Override
    public void updateNodes(Iterator<Node> iter) {
        iter.forEachRemaining(Node::update);
    }
}
