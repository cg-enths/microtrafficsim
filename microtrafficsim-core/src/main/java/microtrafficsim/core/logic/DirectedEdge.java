package microtrafficsim.core.logic;

import microtrafficsim.core.frameworks.shortestpath.IDijkstrableEdge;
import microtrafficsim.core.frameworks.street.ILogicEdge;
import microtrafficsim.core.frameworks.street.StreetEntity;
import microtrafficsim.core.simulation.controller.configs.SimulationConfig;
import microtrafficsim.math.Vec2f;
import microtrafficsim.utils.hashing.FNVHashBuilder;

/**
 * This class is a container for lanes from one node to another. Furthermore, it
 * stores attributes that are important for all lanes, e.g. number of cells, max
 * velocity (yes, max velocity belongs to one directed edge, not to one lane).
 * 
 * @author Jan-Oliver Schmidt, Dominic Parga Cacheiro
 */
public class DirectedEdge implements IDijkstrableEdge, ILogicEdge {

	final long ID;
	private Node origin;
	private Node destination;
	private final int numberOfCells;
	private final int maxVelocity;
	private Lane[] lanes;
	private final byte priorityLevel;
	// visualization
	private StreetEntity entity;
	private final Vec2f originDirection, destinationDirection;

	/**
	 * In addition to standard initialization, this constructor also calculates
	 * the number of cells of this edge and adds this edge to the origin node's
	 * leaving edges.
	 *
	 * @param lengthInMeters
	 *            Real length of this edge in meters
	 * @param maxVelocity
	 *            The max velocity of this edge. It's valid for all lanes.
	 * @param noOfLines
	 *            Number of lines that will be created in this constructor
	 * @param origin
	 *            Origin node of this edge
	 * @param destination
	 *            Destination node of this edge
	 */
	public DirectedEdge(SimulationConfig config, float lengthInMeters, Vec2f originDirection, Vec2f destinationDirection, float maxVelocity,
			int noOfLines, Node origin, Node destination, byte priorityLevel) {

		ID = config.longIDGenerator.next();
		
		// important for shortest path: round up
		numberOfCells = Math.max(1, (int)(Math.ceil(lengthInMeters / config.metersPerCell)));
		this.originDirection = originDirection.normalize();
		this.destinationDirection = destinationDirection.normalize();

		lanes = new Lane[noOfLines];
		lanes[0] = new Lane(this, 0);

		this.origin = origin;
		this.destination = destination;
		// maxVelocity in km/h, but this.maxVelocity in m/s
		this.maxVelocity = Math.max(1, (int) (maxVelocity / 3.6 / config.metersPerCell));
		this.priorityLevel = priorityLevel;

		this.entity = null;
	}
	
	@Override
	public int hashCode() {
		return new FNVHashBuilder()
				.add(ID)
				.add(origin)
				.add(destination)
				.add(numberOfCells)
				.getHash();
	}

	Lane[] getLanes() {
		return lanes;
	}
	
	/**
	 * @return lane of index i counted from right to left, starting with 0
	 */
	public Lane getLane(int i) {
		return lanes[i];
	}
	
	public int getMaxVelocity() {
		return maxVelocity;
	}

	byte getPriorityLevel() {
		return priorityLevel;
	}

	@Override
	public String toString() {
		return ID + ":(" + origin.ID + " -" + numberOfCells + "-> " + destination.ID + ")";
	}

    void reset() {

        lanes = new Lane[lanes.length];
        lanes[0] = new Lane(this, 0);
    }

	// |===============|
	// | visualization |
	// |===============|
	Vec2f getOriginDirection() {
		return originDirection;
	}

	Vec2f getDestinationDirection() {
		return destinationDirection;
	}

	// |======================|
	// | (i) IDijkstrableEdge |
	// |======================|
	@Override
	public int getLength() {
		
		return numberOfCells;
	}
	
	@Override
	public float getCurrentUsage() {
		
		int activeLaneCounter = 0;
		int vehicleCount = 0;
		
		for (Lane lane : lanes) {
			if (lane != null) {
				activeLaneCounter++;
				vehicleCount = vehicleCount + lane.getVehicleCount();
			}
		}
		
		return ((float)vehicleCount) / activeLaneCounter;
	}
	
	@Override
	public float getTimeCostMillis() {
		
		// 1000f because velocity is in m/s
		return 1000f * numberOfCells / maxVelocity;
	}

	@Override
	public Node getOrigin() {
		return origin;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Node getDestination() {
		return destination;
	}

	// |================|
	// | (i) ILogicEdge |
	// |================|
	@Override
	public StreetEntity getEntity() {
		return entity;
	}

	@Override
	public void setEntity(StreetEntity entity) {
		this.entity = entity;
	}
}