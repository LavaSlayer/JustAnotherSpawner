package jas.common.spawner.creature.handler;

import jas.common.DefaultProps;
import jas.common.JASLog;
import jas.common.spawner.creature.type.CreatureType;
import jas.common.spawner.creature.type.CreatureTypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;

public class LivingHandler {
    public final Class<? extends EntityLiving> entityClass;
    public final String creatureTypeID;
    public final boolean useModLocationCheck;
    public final boolean shouldSpawn;
    public final boolean forceDespawn;

    public LivingHandler(Class<? extends EntityLiving> entityClass, String creatureTypeID, boolean useModLocationCheck,
            boolean shouldSpawn, boolean forceDespawn) {
        this.entityClass = entityClass;
        this.useModLocationCheck = useModLocationCheck;
        this.creatureTypeID = CreatureTypeRegistry.INSTANCE.getCreatureType(creatureTypeID) != null ? creatureTypeID
                : CreatureTypeRegistry.NONE;
        this.shouldSpawn = shouldSpawn;
        this.forceDespawn = forceDespawn;
    }
    
    /**
     * Create a new Instance of this LivingHandler. Used to Allow subclasses to Include their own Logic
     * @param entityClass
     * @param creatureTypeID
     * @param useModLocationCheck
     * @param shouldSpawn
     * @param forceDespawn
     * @return
     */
    protected LivingHandler create(Class<? extends EntityLiving> entityClass, String creatureTypeID,
            boolean useModLocationCheck, boolean shouldSpawn, boolean forceDespawn) {
        return new LivingHandler(entityClass, creatureTypeID, useModLocationCheck, shouldSpawn, forceDespawn);
    }
    
    /**
     * Replacement Method for EntitySpecific isCreatureType. Allows Handler specific
     * 
     * @param entity
     * @param creatureType
     * @return
     */
    public boolean isEntityOfType(Entity entity, CreatureType creatureType) {
        return creatureTypeID.equals(CreatureTypeRegistry.NONE) ? false : CreatureTypeRegistry.INSTANCE
                .getCreatureType(creatureTypeID).equals(creatureType);
    }

    /**
     * Replacement Method for EntitySpecific getCanSpawnHere(). Allows Handler to Override Creature functionality. This
     * both ensures that a Modder can implement their own logic indepenently of the modded creature and that end users
     * are allowed to customize their experience
     * 
     * @return True if location is valid For entity to spawn, false otherwise
     */
    public final boolean getCanSpawnHere(EntityLiving entity, CreatureType spawnType) {
        if (useModLocationCheck) {
            return isValidLocation(entity, spawnType);
        } else {
            return isValidLocation(entity);
        }
    }

    /**
     * Called by Despawn to Manually Attempt to Despawn Entity
     * @param entity
     */
    public final void despawnEntity(EntityLiving entity) {
        EntityPlayer entityplayer = entity.worldObj.getClosestPlayerToEntity(entity, -1.0D);

        if (entityplayer != null) {
            double d0 = entityplayer.posX - entity.posX;
            double d1 = entityplayer.posY - entity.posY;
            double d2 = entityplayer.posZ - entity.posZ;
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
            if (entity.getAge() > 600 && entity.worldObj.rand.nextInt(800 / 50) == 0 && d3 > 1024.0D) {
                entity.setDead();
            } else if (d3 < 1024.0D) {
                if (ReflectionHelper.isUnObfuscated(EntityLiving.class, "EntityLiving")) {
                    ReflectionHelper.setFieldUsingReflection("entityAge", EntityLiving.class, entity, true, 0);
                } else {
                    ReflectionHelper.setFieldUsingReflection("field_70708_bq", EntityLiving.class, entity, true, 0);
                }
            }
        }
    }
    
    /**
     * Represents the 'Modders Choice' for Creature SpawnLocation. 
     * 
     * @param entity
     * @param spawnType
     * @return
     */
    protected boolean isValidLocation(EntityLiving entity, CreatureType spawnType) {
        return entity.getCanSpawnHere();
    }

    /**
     * Alternative getCanSpawnHere independent of the Entity. By default this provides a way for End-Users to Skip the
     * EntitySpecific check implemented by Modders while keeping the generic bounding box style checks in EntityLiving
     * 
     * @param entity
     * @return True if location is valid For entity to spawn, false otherwise
     */
    private final boolean isValidLocation(EntityLiving entity) {
        return entity.worldObj.checkIfAABBIsClear(entity.boundingBox)
                && entity.worldObj.getCollidingBoundingBoxes(entity, entity.boundingBox).isEmpty()
                && !entity.worldObj.isAnyLiquid(entity.boundingBox);
    }
    
    /**
     * Creates a new instance of this from configuration using itself as the default
     * @param config
     * @return
     */
    protected LivingHandler createFromConfig(Configuration config) {
        String mobName = (String) EntityList.classToStringMapping.get(entityClass);

        String defaultValue = creatureTypeID.toUpperCase() + DefaultProps.DELIMETER + Boolean.toString(shouldSpawn)
                + DefaultProps.DELIMETER + Boolean.toString(forceDespawn) + DefaultProps.DELIMETER
                + Boolean.toString(useModLocationCheck);

        Property resultValue = config.get("CreatureSettings.LivingHandler", mobName, defaultValue);

        String[] resultParts = resultValue.getString().split("\\" + DefaultProps.DELIMETER);

        if (resultParts.length == 4) {
            String resultCreatureType = LivingRegsitryHelper.parseCreatureTypeID(resultParts[0], creatureTypeID,
                    "creatureTypeID");
            boolean resultShouldSpawn = LivingRegsitryHelper.parseBoolean(resultParts[1], shouldSpawn, "ShouldSpawn");
            boolean resultForceDespawn = LivingRegsitryHelper
                    .parseBoolean(resultParts[2], forceDespawn, "forceDespawn");
            boolean resultLocationCheck = LivingRegsitryHelper.parseBoolean(resultParts[3], useModLocationCheck,
                    "LocationCheck");
            return new LivingHandler(entityClass, resultCreatureType, resultLocationCheck, resultShouldSpawn,
                    resultForceDespawn);
        } else {
            JASLog.severe(
                    "LivingHandler Entry %s was invalid. Data is being ignored and loaded with default settings %s, %s, %s, %s",
                    mobName, creatureTypeID, useModLocationCheck, shouldSpawn, forceDespawn);
            resultValue.set(defaultValue);
            return new LivingHandler(entityClass, creatureTypeID, useModLocationCheck, shouldSpawn, forceDespawn);
        }
    }
}