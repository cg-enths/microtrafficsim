package microtrafficsim.core.parser;

import java.util.HashSet;
import java.util.Set;

import microtrafficsim.osm.parser.features.streets.StreetComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import microtrafficsim.core.logic.DirectedEdge;
import microtrafficsim.core.logic.Direction;
import microtrafficsim.core.logic.Node;
import microtrafficsim.core.logic.StreetGraph;
import microtrafficsim.core.map.Coordinate;
import microtrafficsim.core.map.features.info.OnewayInfo;
import microtrafficsim.core.simulation.controller.configs.SimulationConfig;
import microtrafficsim.math.DistanceCalculator;
import microtrafficsim.math.Geometry;
import microtrafficsim.math.HaversineDistanceCalculator;
import microtrafficsim.math.Vec2f;
import microtrafficsim.osm.parser.base.DataSet;
import microtrafficsim.osm.parser.ecs.Component;
import microtrafficsim.osm.parser.ecs.entities.NodeEntity;
import microtrafficsim.osm.parser.ecs.entities.WayEntity;
import microtrafficsim.osm.parser.features.FeatureDefinition;
import microtrafficsim.osm.parser.features.FeatureGenerator;
import microtrafficsim.osm.parser.processing.osm.Connector;
import microtrafficsim.osm.parser.processing.osm.GraphWayComponent;
import microtrafficsim.osm.parser.processing.osm.sanitizer.SanitizerWayComponent;

/**
 * The {@code FeatureGenerator} for the StreetGraph used in the simulation.
 * 
 * @author Dominic Parga Cacheiro, Maximilian Luz
 */
public class StreetGraphGenerator implements FeatureGenerator {
	private static Logger logger = LoggerFactory.getLogger(StreetGraphGenerator.class);

	private StreetGraph graph;
	private SimulationConfig config;
	private DistanceCalculator distcalc;

	/**
	 * Creates a new {@code StreetGraphGenerator} using the
	 * {@link HaversineDistanceCalculator#getDistance(Coordinate, Coordinate)
	 * HaversineDistanceCalculator } as {@code DistanceCalculator}.
	 * 
	 * <p>
	 * This is equivalent to
	 * {@link StreetGraphGenerator#StreetGraphGenerator(SimulationConfig)
	 * StreetGraphGenerator(config, HaversineDistanceCalculator::getDistance) }
	 * </p>
	 */
	public StreetGraphGenerator(SimulationConfig config) {
		// TODO: use vincenty-formular here for better accuracy?
		this(config, HaversineDistanceCalculator::getDistance);
	}

	/**
	 * Creates a new {@code StreetGraphGenerator} with the given
	 * {@code DistanceCalculator}.
	 * 
	 * @param distcalc
	 *            the {@code DistanceCalculator} used to calculate the length of
	 *            streets.
	 */
	public StreetGraphGenerator(SimulationConfig config, DistanceCalculator distcalc) {
		this.config = config;
		this.distcalc = distcalc;
		this.graph = null;
	}

	/**
	 * Returns the generated StreetGraph or {@code null}, if this generator has
	 * not been executed yet.
	 * 
	 * @return the generated StreetGraph.
	 */
	public StreetGraph getStreetGraph() {
		return graph;
	}

	@Override
	public void execute(DataSet dataset, FeatureDefinition feature) {
		/*
		 * STATUS: - all edges are added with only one lane and priority=false -
		 * Lane-Connectors are generated directly from Street-Connectors (one to
		 * one)
		 */

		/*
		 * TODO: basics now - tests for StreetGraph/StreetGraphGenerator
		 */

		/*
		 * TODO: basics future - geographical layout of streets (left/right of
		 * ...) - multi-lane support
		 */

		/*
		 * TODO: nice to have - entfernen kleinerer zusammenhangskomponenten
		 */

		logger.info("generating StreetGraph");
		this.graph = null;
		StreetGraph graph = new StreetGraph(
				(float) dataset.bounds.minlat,
				(float) dataset.bounds.maxlat,
				(float) dataset.bounds.minlon,
				(float) dataset.bounds.maxlon);

		// create required nodes and edges
		for (WayEntity way : dataset.ways.values()) {
			if (!way.features.contains(feature))
				continue;
			createAndAddEdges(dataset, graph, way);
		}

		// add turn-lanes
		for (WayEntity way : dataset.ways.values()) {
			if (!way.features.contains(feature))
				continue;
			addConnectors(dataset, way);
		}

		// delete Components generated by this generator
//		for (WayEntity way : dataset.ways.values())
//			way.remove(StreetGraphWayComponent.class);

		for (NodeEntity node : dataset.nodes.values())
			node.remove(StreetGraphNodeComponent.class);

		// finish
		graph.calcEdgeIndicesPerNode();
		this.graph = graph;
		logger.info("finished generating StreetGraph");
	}

	/**
	 * Creates all necessary {@code DirectedEdges} from the given
	 * {@code WayEntity} and adds them to the StreetGraph.
	 * 
	 * @param dataset
	 *            the {@code DataSet} of which {@code way} is part of.
	 * @param graph
	 *            the StreetGraph to which the generated edges should be added.
	 * @param way
	 *            the {@code WayEntity} for which the edges should be generated.
	 */
	private void createAndAddEdges(DataSet dataset, StreetGraph graph, WayEntity way) {
		NodeEntity node0 = dataset.nodes.get(way.nodes[0]);
		NodeEntity node1 = dataset.nodes.get(way.nodes[1]);
		NodeEntity secondLastNode = dataset.nodes.get(way.nodes[way.nodes.length - 2]);
		NodeEntity lastNode = dataset.nodes.get(way.nodes[way.nodes.length - 1]);
		Node start = getNode(node0);
		Node end = getNode(lastNode);
		StreetComponent streetinfo = way.get(StreetComponent.class);

		// generate edges
		DirectedEdge forward = null;
		DirectedEdge backward = null;

		float length = getLength(dataset, way);

		if (streetinfo.oneway == OnewayInfo.NO || streetinfo.oneway == OnewayInfo.FORWARD
				|| streetinfo.oneway == OnewayInfo.REVERSIBLE) {
			Vec2f originDirection = new Vec2f(
					(float) (node1.lon - node0.lon),
					(float) (node1.lat - node0.lat));

			Vec2f destinationDirection = new Vec2f(
					(float) (lastNode.lon - secondLastNode.lon),
					(float) (lastNode.lat - secondLastNode.lat));

			forward = new DirectedEdge(config, length, originDirection, destinationDirection, streetinfo.maxspeed.forward,
					1, start, end, config.streetPrios.get(streetinfo.streettype));
		}

		if (streetinfo.oneway == OnewayInfo.NO || streetinfo.oneway == OnewayInfo.BACKWARD) {
			Vec2f originDirection = new Vec2f(
					(float) (secondLastNode.lon - lastNode.lon),
					(float) (secondLastNode.lat - lastNode.lat));

			Vec2f destinationDirection = new Vec2f(
					(float) (node0.lon - node1.lon),
					(float) (node0.lat - node1.lat));

			backward = new DirectedEdge(config, length, originDirection, destinationDirection,
					streetinfo.maxspeed.backward, 1, end, start,
					config.streetPrios.get(streetinfo.streettype));
		}

		// create component for ECS
		StreetGraphWayComponent graphinfo = new StreetGraphWayComponent(way, forward, backward);
		way.set(StreetGraphWayComponent.class, graphinfo);

		// register
		if (forward != null) {
			graph.registerEdgeAndNodes(forward);
			start.addEdge(forward);
			end.addEdge(forward);
		}

		if (backward != null) {
			graph.registerEdgeAndNodes(backward);
			start.addEdge(backward);
			end.addEdge(backward);
		}
	}

	/**
	 * Creates and adds all lane-connectors leading from the given {@code
	 * WayEntity} to any other {@code WayEntity}/{@code DirectedEdge} (including
	 * itself).
	 * 
	 * @param dataset
	 *            the {@code DataSet} of which {@code wayFrom} is a part of.
	 * @param wayFrom
	 *            the {@code WayEntity} for which all outgoing lane-connectors
	 *            should be created.
	 */
	private void addConnectors(DataSet dataset, WayEntity wayFrom) {
		StreetGraphWayComponent sgwcFrom = wayFrom.get(StreetGraphWayComponent.class);
		if (sgwcFrom == null)
			return;
		if (sgwcFrom.forward == null && sgwcFrom.backward == null)
			return;

		Node sgNodeStart = dataset.nodes.get(wayFrom.nodes[0]).get(StreetGraphNodeComponent.class).node;
		Node sgNodeEnd = dataset.nodes.get(wayFrom.nodes[wayFrom.nodes.length - 1])
				.get(StreetGraphNodeComponent.class).node;

		GraphWayComponent gwcFrom = wayFrom.get(GraphWayComponent.class);

		// u-turns
		for (Connector c : gwcFrom.uturn) {
			if (sgwcFrom.forward == null || sgwcFrom.backward == null)
				continue;

			// XXX u-turns should probably have a special direction?
			if (c.via.id == wayFrom.nodes[0]) {
				sgNodeStart.addConnector(sgwcFrom.backward.getLane(0), sgwcFrom.forward.getLane(0), Direction.LEFT);
			} else if (c.via.id == wayFrom.nodes[wayFrom.nodes.length - 1]) {
				sgNodeStart.addConnector(sgwcFrom.forward.getLane(0), sgwcFrom.backward.getLane(0), Direction.LEFT);
			}
		}

		// leaving connectors (no 'else if' because of cyclic connectors)
		for (Connector c : gwcFrom.from) {
			StreetGraphWayComponent sgwcTo = c.to.get(StreetGraphWayComponent.class);
			if (sgwcTo == null)
				continue;

			NodeEntity q = dataset.nodes.get(c.via.id);
			// <---o--->
			if (c.via.id == wayFrom.nodes[0] && c.via.id == c.to.nodes[0]) {
				NodeEntity p = dataset.nodes.get(wayFrom.nodes[1]);
				NodeEntity r = dataset.nodes.get(c.to.nodes[1]);
				Direction dir = Geometry.calcCurveDirection(
						(float) p.lon, (float) p.lat, (float) q.lon, (float) q.lat, (float) r.lon, (float) r.lat);
				addEdgeConnectors(sgNodeStart, sgwcFrom.backward, sgwcTo.forward, dir);
			}

			// <---o<---
			if (c.via.id == wayFrom.nodes[0] && c.via.id == c.to.nodes[c.to.nodes.length - 1]) {
				NodeEntity p = dataset.nodes.get(wayFrom.nodes[1]);
				NodeEntity r = dataset.nodes.get(c.to.nodes[c.to.nodes.length - 2]);
				Direction dir = Geometry.calcCurveDirection(
						(float) p.lon, (float) p.lat, (float) q.lon, (float) q.lat, (float) r.lon, (float) r.lat);
				addEdgeConnectors(sgNodeStart, sgwcFrom.backward, sgwcTo.backward, dir);
			}

			// --->o--->
			if (c.via.id == wayFrom.nodes[wayFrom.nodes.length - 1] && c.via.id == c.to.nodes[0]) {
				NodeEntity p = dataset.nodes.get(wayFrom.nodes[wayFrom.nodes.length - 2]);
				NodeEntity r = dataset.nodes.get(c.to.nodes[1]);
				Direction dir = Geometry.calcCurveDirection(
						(float) p.lon, (float) p.lat, (float) q.lon, (float) q.lat, (float) r.lon, (float) r.lat);
				addEdgeConnectors(sgNodeEnd, sgwcFrom.forward, sgwcTo.forward, dir);
			}

			// --->o<---
			if (c.via.id == wayFrom.nodes[wayFrom.nodes.length - 1] && c.via.id == c.to.nodes[c.to.nodes.length - 1]) {
				NodeEntity p = dataset.nodes.get(wayFrom.nodes[wayFrom.nodes.length - 2]);
				NodeEntity r = dataset.nodes.get(c.to.nodes[c.to.nodes.length - 2]);
				Direction dir = Geometry.calcCurveDirection(
						(float) p.lon, (float) p.lat, (float) q.lon, (float) q.lat, (float) r.lon, (float) r.lat);
				addEdgeConnectors(sgNodeEnd, sgwcFrom.forward, sgwcTo.backward, dir);
			}
		}

		// cyclic connectors
		if (sgNodeStart == sgNodeEnd) {
			if (gwcFrom.cyclicEndToStart && sgwcFrom.forward != null)
				sgNodeStart.addConnector(sgwcFrom.forward.getLane(0), sgwcFrom.forward.getLane(0), Direction.STRAIGHT);

			if (gwcFrom.cyclicStartToEnd && sgwcFrom.backward != null)
				sgNodeStart.addConnector(sgwcFrom.backward.getLane(0), sgwcFrom.backward.getLane(0),
						Direction.STRAIGHT);
		}
	}

	/**
	 * Adds all lane-connectors from {@code from} via {@code via} to {@code to}.
	 * 
	 * @param via
	 *            the {@code Node} via which the generated connectors should go.
	 * @param from
	 *            the {@code DirectedEdge} from which the generated connectors
	 *            should originate.
	 * @param to
	 *            the {@code DirectedEdge} to which the generated connectors
	 *            should lead.
	 * @param dir
	 *            the {@code Direction} required for the connector.
	 */
	private void addEdgeConnectors(Node via, DirectedEdge from, DirectedEdge to, Direction dir) {
		if (from == null || to == null)
			return;

		via.addConnector(from.getLane(0), to.getLane(0), dir);
	}

	/**
	 * Calculates the length of the given {@code WayEntity} using this {@code
	 * StreetGenerator}'s {@code DistanceCalculator}. The unit of the returned
	 * value depends on the used {@code DistanceCalculator}.
	 * 
	 * @param dataset
	 *            the {@code DataSet} of which {@code way} is part of.
	 * @param way
	 *            the {@code WayEntity} for which the length should be
	 *            calculated.
	 * @return the length of the given {@code WayEntity}.
	 */
	private float getLength(DataSet dataset, WayEntity way) {
		NodeEntity node = dataset.nodes.get(way.nodes[0]);
		Coordinate a = new Coordinate(node.lat, node.lon);

		float length = 0;
		for (int i = 1; i < way.nodes.length; i++) {
			node = dataset.nodes.get(way.nodes[i]);
			Coordinate b = new Coordinate(node.lat, node.lon);

			length += distcalc.getDistance(a, b);
			a = b;
		}

		return length;
	}

	/**
	 * Returns the StreetGraph-{@code Node} associated with the given {@code
	 * NodeEntity} or creates a new one if it does not exist.
	 * 
	 * @param entity
	 *            the {@code NodeEntity} for which the {@code Node} should be
	 *            returned.
	 * @return the {@code Node} associated with the given {@code NodeEntity}.
	 */
	private Node getNode(NodeEntity entity) {
		StreetGraphNodeComponent graphinfo = entity.get(StreetGraphNodeComponent.class);

		if (graphinfo == null) {
			graphinfo = new StreetGraphNodeComponent(entity,
					new Node(config, new Coordinate(entity.lat, entity.lon)));
			entity.set(StreetGraphNodeComponent.class, graphinfo);
		}

		return graphinfo.node;
	}

	@Override
	public Set<Class<? extends Component>> getRequiredWayComponents() {
		HashSet<Class<? extends Component>> required = new HashSet<>();
		required.add(StreetComponent.class);
		required.add(SanitizerWayComponent.class);
		return required;
	}
}
