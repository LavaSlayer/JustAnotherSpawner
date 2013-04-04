package jas.common.spawner.creature.handler;

import jas.common.DefaultProps;
import jas.common.JASLog;
import jas.common.spawner.creature.entry.SpawnListEntry;
import jas.common.spawner.creature.type.CreatureType;
import jas.common.spawner.creature.type.CreatureTypeRegistry;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.ConfigCategory;
import net.minecraftforge.common.Configuration;

import com.google.common.base.CharMatcher;

public enum CreatureHandlerRegistry {
    INSTANCE;
    private final HashMap<Class<? extends EntityLiving>, LivingHandler> livingHandlers = new HashMap<Class<? extends EntityLiving>, LivingHandler>();
    private final HashMap<Class<? extends EntityLiving>, Class<? extends LivingHandler>> handlersToAdd = new HashMap<Class<? extends EntityLiving>, Class<? extends LivingHandler>>();
    private List<BiomeGenBase> biomeList = new ArrayList<BiomeGenBase>();

    private final HashMap<String, Configuration> modConfigCache = new HashMap<String, Configuration>();
    private List<Class<? extends EntityLiving>> entityList = new ArrayList<Class<? extends EntityLiving>>();
    public static final String delimeter = DefaultProps.DELIMETER;
    public static final String LivingHandlerCategoryComment = "Editable Format: CreatureType" + delimeter
            + "ShouldSpawn" + delimeter + "ForceDespawn" + delimeter + "UseModLocationCheck";
    public static final String SpawnListCategoryComment = "Editable Format: SpawnWeight" + delimeter + "SpawnPackSize"
            + delimeter + "MinChunkPackSize" + delimeter + "MaxChunkPackSize";

    /**
     * Searhes and Process Entities it can Find in EntityList to create default the LivingHandlers and SpawnList
     * Entries. Will Populate the Spawnlists in corresponding CreatureTypes.
     * 
     * @param configDirectory
     */
    public void findProcessEntitesForHandlers(File configDirectory, MinecraftServer minecraftServer) {
        /* Clear Various Lists As ingame whenever we switch Worlds they may already be initialized */
        modConfigCache.clear();
        livingHandlers.clear();
        Iterator<CreatureType> iterator = CreatureTypeRegistry.INSTANCE.getCreatureTypes();
        while (iterator.hasNext()) {
            CreatureType type = iterator.next();
            type.resetSpawns();
        }
        populateEntityList();
        populateBiomeList();

        for (Class<? extends EntityLiving> livingClass : entityList) {
            if (livingHandlers.containsKey(livingClass)) {
                continue;
            }
            String mobName = (String) EntityList.classToStringMapping.get(livingClass);
            Configuration masterConfig = getConfigurationFile(configDirectory, "Master", mobName);
            Configuration worldConfig = getConfigurationFile(configDirectory, minecraftServer.worldServers[0].getWorldInfo().getWorldName(), mobName);
            
            LivingHandler livingHandler = new LivingHandler(livingClass, enumCreatureTypeToLivingType(livingClass,
                    minecraftServer.worldServers[0]), true, false, false).createFromConfig(masterConfig)
                    .createFromConfig(worldConfig);
            livingHandlers.put(livingClass, livingHandler);

            if (livingHandler.shouldSpawn && !livingHandler.creatureTypeID.equals(CreatureTypeRegistry.NONE)) {
                for (BiomeGenBase biomeGenBase : biomeList) {

                    SpawnListEntry spawnListEntry = findVanillaSpawnListEntry(biomeGenBase, livingClass)
                            .createFromConfig(masterConfig).createFromConfig(worldConfig);

                    if (spawnListEntry.itemWeight > 0) {
                        JASLog.info("Adding SpawnListEntry %s of type %s to Biome %s", mobName,
                                spawnListEntry.getLivingHandler().creatureTypeID, spawnListEntry.biomeName);
                        CreatureTypeRegistry.INSTANCE.getCreatureType(spawnListEntry.getLivingHandler().creatureTypeID)
                                .addSpawn(spawnListEntry);
                    } else {
                        JASLog.debug(Level.INFO,
                                "Not adding Generated SpawnListEntry of %s due to Weight. Biomes: %s, ItemWeight: %s",
                                mobName, biomeGenBase.biomeName, spawnListEntry.itemWeight);
                    }
                }
            } else {
                JASLog.debug(Level.INFO,
                        "Not Generating SpawnList entries for %s. ShouldSpawn: %s, CreatureTypeID: %s", mobName,
                        livingHandler.shouldSpawn, livingHandler.creatureTypeID);
            }
        }

        for (Configuration config : modConfigCache.values()) {
            config.save();
        }
    }
    
    /**
     * Caches and Retrieves Configration Files for Individual modIDs. The ModID is inferred from the entity name in the
     * form ModID:EntityName
     * 
     * @param configDirectory
     * @param minecraftServer
     * @param fullMobName
     * @return
     */
    private Configuration getConfigurationFile(File configDirectory, String worldName, String fullMobName) {
        String modID;
        String[] mobNameParts = fullMobName.split("\\.");
        if (mobNameParts.length == 2) {
            String regexRetain = "qwertyuiopasdfghjklzxcvbnm0QWERTYUIOPASDFGHJKLZXCVBNM123456789";
            modID = CharMatcher.anyOf(regexRetain).retainFrom(mobNameParts[0]);
        } else {
            modID = "Vanilla";
        }

        Configuration config;
        if (modConfigCache.get(worldName + modID) == null) {
            config = new Configuration(new File(configDirectory, DefaultProps.WORLDSETTINGSDIR + worldName + "/"
                    + DefaultProps.ENTITYSUBDIR + modID + ".cfg"));
            config.load();
            setupCategories(config);
            modConfigCache.put(worldName + modID, config);
        }
        return modConfigCache.get(worldName + modID);
    }
    
    private void setupCategories(Configuration config) {
        ConfigCategory category = config.getCategory("CreatureSettings.LivingHandler".toLowerCase(Locale.ENGLISH));
        category.setComment(LivingHandlerCategoryComment);

        category = config.getCategory("CreatureSettings.SpawnListEntry".toLowerCase(Locale.ENGLISH));
        category.setComment(SpawnListCategoryComment);
    }

    /**
     * Search EntityList for Valid Creature Entities
     */
    @SuppressWarnings("unchecked")
    private void populateEntityList() {
        entityList.clear();
        Iterator<?> entityIterator = EntityList.stringToClassMapping.keySet().iterator();
        while (entityIterator.hasNext()) {
            Object classKey = entityIterator.next();
            if (EntityLiving.class.isAssignableFrom((Class<?>) EntityList.stringToClassMapping.get(classKey))
                    && !Modifier.isAbstract(((Class<?>) EntityList.stringToClassMapping.get(classKey)).getModifiers())) {
                JASLog.info("Found Entity %s", classKey);
                entityList.add((Class<? extends EntityLiving>) EntityList.stringToClassMapping.get(classKey));
            }
        }
    }

    /**
     * Search BiomeGenBase for Valid Biomes to Spawn In
     */
    private void populateBiomeList() {
        biomeList.clear();
        for (int i = 0; i < BiomeGenBase.biomeList.length; i++) {
            if (BiomeGenBase.biomeList[i] != null) {
                biomeList.add(BiomeGenBase.biomeList[i]);
            }
        }
    }

    /**
     * Searches For a Vanilla SpawnListEntry. Generates using defaults values (spawn rate == 0) if one doesn't exist.
     * 
     * @param biome
     * @param livingClass
     * @return
     */
    public SpawnListEntry findVanillaSpawnListEntry(BiomeGenBase biome, Class<? extends EntityLiving> livingClass) {
        EnumCreatureType creatureType = livingTypeToEnumCreatureType(livingHandlers.get(livingClass).creatureTypeID);
        if (creatureType != null) {
            @SuppressWarnings("unchecked")
            List<net.minecraft.world.biome.SpawnListEntry> spawnListEntries = biome.getSpawnableList(creatureType);
            for (net.minecraft.world.biome.SpawnListEntry spawnListEntry : spawnListEntries) {
                if (spawnListEntry.entityClass.equals(livingClass)) {
                    return new SpawnListEntry(livingClass, biome.biomeName, spawnListEntry.itemWeight, 4,
                            spawnListEntry.minGroupCount, spawnListEntry.maxGroupCount);
                }
            }
        }
        return new SpawnListEntry(livingClass, biome.biomeName, 0, 4, 0, 4);
    }
    
    /**
     * Determines the Default JAS Living Type from the Vanilla EnumCreatureType
     * 
     * @param livingClass
     * @return
     */
    private String enumCreatureTypeToLivingType(Class<? extends EntityLiving> livingClass, World world) {
        EntityLiving creature = LivingHelper.createCreature(livingClass, world);
        for (EnumCreatureType type : EnumCreatureType.values()) {
            boolean isType = creature != null ? creature.isCreatureType(type, true) : type.getClass().isAssignableFrom(
                    livingClass);
            if (isType && CreatureTypeRegistry.INSTANCE.getCreatureType(type.toString()) != null) {
                return type.toString();
            }
        }
        return CreatureTypeRegistry.NONE;
    }

    /**
     * Determines the Vanilla EnumCreatureType from the equivalent JAS living Type
     * 
     * @return
     */
    private EnumCreatureType livingTypeToEnumCreatureType(String creatureTypeID) {
        if (creatureTypeID.equals(CreatureTypeRegistry.MONSTER)) {
            return EnumCreatureType.monster;
        } else if (creatureTypeID.equals(CreatureTypeRegistry.AMBIENT)) {
            return EnumCreatureType.ambient;
        } else if (creatureTypeID.equals(CreatureTypeRegistry.CREATURE)) {
            return EnumCreatureType.creature;
        } else if (creatureTypeID.equals(CreatureTypeRegistry.WATERCREATURE)) {
            return EnumCreatureType.waterCreature;
        } else {
            return null;
        }
    }

    /**
     * Registers a Living Handler to be initialized by the System.
     * 
     * @param handlerID
     * @param handler
     * @return Returns False if Handler is replaced during registration
     */
    public boolean registerHandler(Class<? extends EntityLiving> livingEntity,
            Class<? extends LivingHandler> livingHandler) {
        boolean isReplaced = false;
        if (!handlersToAdd.containsKey(livingEntity)) {
            JASLog.warning("Custom Living Handler %s which was to be registered will be replaced with %s",
                    handlersToAdd.containsKey(livingEntity), livingHandler);
            isReplaced = true;
        }
        handlersToAdd.put(livingEntity, livingHandler);
        return !isReplaced;
    }

    /**
     * Gets the Appropriate LivingHandler from the Provided Key
     * 
     * @param handlerID
     * @return
     */
    public LivingHandler getLivingHandler(Class<? extends Entity> entityClass) {
        return livingHandlers.get(entityClass);
    }
}