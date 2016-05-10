package microtrafficsim.osm.parser.ecs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import microtrafficsim.osm.parser.features.FeatureDefinition;


/**
 * Base-class for creating and initializing Entities with Components.
 * 
 * @param <EntityT>	the type of the {@code Entity}.
 * @param <SourceT> the type of the source-element from which the entity is created.
 * 
 * @author Maximilian Luz
 */
public abstract class EntityManager<EntityT extends Entity, SourceT> implements EntityFactory<EntityT, SourceT> {
	
	protected HashMap<Class<? extends Component>, ComponentFactory<? extends Component, SourceT>> initializers;
	
	
	/**
	 * Creates a new {@code EntityManager} without any {@code Component}-initializers.
	 */
	public EntityManager() {
		this.initializers = new HashMap<>();
	}
	
	
	/**
	 * Set the initializer (Factory) for the specified {@code Component}-type.
	 * 
	 * @param <T>		the type of the {@code Component}.
	 * @param type		the Class of the {@code Component} for which the
	 * 					initializer should be used.
	 * @param factory	the factory used to initialize the specified component-type.
	 * @return the factory previously associated with the specified component-type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Component> ComponentFactory<T, SourceT> putInitializer(Class<T> type, ComponentFactory<T, SourceT> factory) {
		return (ComponentFactory<T, SourceT>) initializers.put(type, factory);
	}
	
	/**
	 * Get the initializer (Factory) for the specified {@code Component}-type.
	 * 
	 * @param <T>	the type of the {@code Component}.
	 * @param type	the type of the {@code Component} for which the initializer
	 * 				is used.
	 * @return the factory associated with the specified component-type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Component> ComponentFactory<T, SourceT> getInitializer(Class<T> type) {
		return (ComponentFactory<T, SourceT>) initializers.get(type);
	}
	
	/**
	 * Remove the initializer (Factory) for the specified {@code Component}-type.
	 * 
	 * @param <T>	the type of the {@code Component}.
	 * @param type	the type of the {@code Component} for which the initializer
	 * 				is used.
	 * @return the factory previously associated with the specified component-type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Component> ComponentFactory<T, SourceT> removeInitializer(Class<T> type) {
		return (ComponentFactory<T, SourceT>) initializers.remove(type);
	}
	
	/**
	 * Checks if the specified component-type has an initializer (Factory)
	 * associated with it.
	 * 
	 * @param <T>	the type of the {@code Component}.
	 * @param type	the type of the {@code Component} for which the initializer
	 * 				is used.
	 * @return true if the specified component-type has an associated factory,
	 * false otherwise.
	 */
	public <T extends Component> boolean hasInitializer(Class<T> type) {
		return initializers.containsKey(type);
	}
	
	/**
	 * Returns the map internally used to store the Initializers. This method
	 * should be <i>used with caution.</i> Use
	 * {@linkplain EntityManager#putInitializer(Class, ComponentFactory)},
	 * {@linkplain EntityManager#getInitializer(Class)} or
	 * {@linkplain EntityManager#removeInitializer(Class)}
	 * instead if possible.
	 * 
	 * <p>
	 * If you are using this method make sure the key-class matches the type
	 * returned by {@linkplain Component#getType()} of the Components generated by
	 * the value-ComponentFactory.
	 * </p>
	 * 
	 * @return the map of Component-Initializers.
	 */
	public Map<Class<? extends Component>, ComponentFactory<? extends Component, SourceT>> getInitializerMap() {
		return initializers;
	}
	
	
	/**
	 * Create all components of types given in the {@code components}-set for
	 * which initializers are present.
	 * 
	 * @param components	the component-types for which {@code Component}s
	 * 						should be created/initialized.
	 * @param entity		the Entity to which the components should be added.
	 * @param source		the source-element from which to create the components.
	 * @param features		the set of {@code FeatureDefinition}s the
	 * 						source-element matches.
	 */
	protected void initializeComponents(Set<Class<? extends Component>> components, EntityT entity, 
			SourceT source, Set<FeatureDefinition> features) {
		
		for (Class<? extends Component> type : components) {
			ComponentFactory<? extends Component, SourceT> factory = initializers.get(type);
			
			if (factory == null)
				continue;
				
			Component component = factory.create(entity, source, features);
			if (component != null)
				entity.getAll().put(component.getType(), component);
		}
	}
}
