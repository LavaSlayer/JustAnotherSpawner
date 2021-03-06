package jas.spawner.modern.modification;

import jas.spawner.modern.spawner.biome.group.BiomeGroupRegistry;
import jas.spawner.modern.spawner.biome.structure.StructureHandlerRegistry;
import jas.spawner.modern.spawner.creature.entry.BiomeSpawnListRegistry;
import jas.spawner.modern.spawner.creature.handler.LivingGroupRegistry;
import jas.spawner.modern.spawner.creature.handler.LivingHandlerRegistry;
import jas.spawner.modern.spawner.creature.type.CreatureTypeRegistry;
import jas.spawner.modern.world.WorldSettings;

/**
 * Convenience class to allow subclasses to only override a registry they need
 * to change
 */
public class BaseModification implements Modification {

	@Override
	public void applyModification(WorldSettings worldSettings) {
	}

	@Override
	public void applyModification(BiomeGroupRegistry registry) {
	}

	@Override
	public void applyModification(LivingGroupRegistry registry) {
	}

	@Override
	public void applyModification(CreatureTypeRegistry registry) {
	}

	@Override
	public void applyModification(LivingHandlerRegistry registry) {
	}

	@Override
	public void applyModification(StructureHandlerRegistry registry) {
	}

	@Override
	public void applyModification(BiomeSpawnListRegistry registry) {
	}
}
