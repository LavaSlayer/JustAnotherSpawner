package jas.spawner.modern.command;

import jas.spawner.modern.MVELProfile;
import jas.spawner.modern.spawner.CountInfo;
import jas.spawner.modern.spawner.CustomSpawner;
import jas.spawner.modern.spawner.EntityCounter;
import jas.spawner.modern.spawner.EntityCounter.CountableInt;
import jas.spawner.modern.spawner.creature.handler.LivingGroupRegistry;
import jas.spawner.modern.spawner.creature.handler.LivingHandler;
import jas.spawner.modern.spawner.creature.handler.LivingHandlerRegistry;
import jas.spawner.modern.spawner.creature.type.CreatureType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

public class CommandComposition extends CommandJasBase {
    public String getCommandName() {
        return "composition";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender commandSender) {
        return "commands.jascomposition.usage";
    }

    @Override
    public void process(ICommandSender commandSender, String[] stringArgs) {
        if (stringArgs.length >= 3) {
            throw new WrongUsageException("commands.jascomposition.usage", new Object[0]);
        }

        EntityPlayerMP targetPlayer;
        if (stringArgs.length == 2) {
            targetPlayer = getPlayer(commandSender, stringArgs[0]);
        } else {
            targetPlayer = getPlayer(commandSender, commandSender.getCommandSenderName());
        }

        String entityCategName = stringArgs.length == 0 ? "*" : stringArgs.length == 1 ? stringArgs[0] : stringArgs[1];

        StringBuilder countedContents = new StringBuilder();

        boolean foundMatch = false;
        Iterator<CreatureType> creatureTypes = MVELProfile.worldSettings().creatureTypeRegistry()
                .getCreatureTypes();
        while (creatureTypes.hasNext()) {
            CreatureType creatureType = creatureTypes.next();
            if (entityCategName.equals("*") || entityCategName.equals(creatureType.typeID)) {
                foundMatch = true;
                EntityCounter despawnTypeCount = new EntityCounter();
                EntityCounter totalTypeCount = new EntityCounter();
                EntityCounter creatureCount = new EntityCounter();
                EntityCounter despawnCreatureCount = new EntityCounter();
                foundMatch = true;
                CountInfo info = CustomSpawner.spawnCounter.countEntities(targetPlayer.worldObj);
                for (Entity entity : CustomSpawner.spawnCounter.countLoadedEntities(targetPlayer.worldObj)) {
                    if (!(entity instanceof EntityLiving)) {
                        continue;
                    }
                    LivingGroupRegistry groupRegistry = MVELProfile.worldSettings().livingGroupRegistry();
                    LivingHandlerRegistry handlerRegistry = MVELProfile.worldSettings().livingHandlerRegistry();
					List<LivingHandler> livingHandlers = handlerRegistry
							.getLivingHandlers(groupRegistry.EntityClasstoJASName.get(entity.getClass()));

                    /*
                     * Used to ensure that if an entity is in multiple groups that are have the same type (i.e.
                     * MONSTER), that it only counts once for each type
                     */
                    Set<String> typesCounted = new HashSet<String>();
            		for (LivingHandler livingHandler : livingHandlers) {
                        if (!typesCounted.contains(creatureType.typeID)
                                && livingHandler.creatureTypeID.equals(creatureType.typeID)) {
                            creatureCount.incrementOrPutIfAbsent(entity.getClass().getSimpleName(), 1);
                            totalTypeCount.incrementOrPutIfAbsent(creatureType.typeID, 1);
                            typesCounted.add(creatureType.typeID);
                            if (livingHandler.canDespawn((EntityLiving) entity, info)) {
                                despawnTypeCount.incrementOrPutIfAbsent(creatureType.typeID, 1);
                                despawnCreatureCount.incrementOrPutIfAbsent(entity.getClass().getSimpleName(), 1);
                            }
                        }
                    }
                }

                countedContents.append(" \u00A71").append(creatureType.typeID).append("\u00A7r: ");
                int despawnable = despawnTypeCount.getOrPutIfAbsent(creatureType.typeID, 0).get();
                int total = totalTypeCount.getOrPutIfAbsent(creatureType.typeID, 0).get();
                if (despawnable == total) {
                    countedContents.append("\u00A72").append(despawnable);
                } else {
                    countedContents.append("\u00A74").append(despawnable).append(" of ").append(total);
                }
                countedContents.append("\u00A7r").append(" despawnable. ");

                Iterator<Entry<String, CountableInt>> iterator = creatureCount.countingHash.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<String, CountableInt> entry = iterator.next();
                    int creatureAmount = entry.getValue().get();
                    int despawnAmount = despawnCreatureCount.getOrPutIfAbsent(entry.getKey(), 0).get();
                    if (despawnAmount == creatureAmount) {
                        countedContents.append("[\u00A72").append(creatureAmount);
                    } else {
                        countedContents.append("[\u00A74").append(despawnAmount).append("/").append(creatureAmount);
                    }
                    countedContents.append("\u00A7r]").append(entry.getKey());

                    if (iterator.hasNext()) {
                        countedContents.append(", ");
                    }
                }
            }
        }

        if (!foundMatch) {
            throw new WrongUsageException("commands.jascomposition.typenotfound", new Object[0]);
        } else {
            commandSender.addChatMessage(new ChatComponentText(countedContents.toString()));
        }
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    @Override
    public List<String> getTabCompletions(ICommandSender commandSender, String[] stringArgs) {
        stringArgs = correctedParseArgs(stringArgs, false);
        List<String> tabCompletions = new ArrayList<String>();
        if (stringArgs.length == 1) {
            addPlayerUsernames(tabCompletions);
            addEntityTypes(tabCompletions);
        } else if (stringArgs.length == 2) {
            addEntityTypes(tabCompletions);
        }

        if (!tabCompletions.isEmpty()) {
            return getStringsMatchingLastWord(stringArgs, tabCompletions);
        } else {
            return tabCompletions;
        }
    }
}
