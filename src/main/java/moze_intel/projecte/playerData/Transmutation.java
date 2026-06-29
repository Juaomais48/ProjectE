package moze_intel.projecte.playerData;

import com.google.common.collect.Lists;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import moze_intel.projecte.emc.EMCMapper;
import moze_intel.projecte.emc.SimpleStack;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.KnowledgeSyncPKT;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PELogger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class Transmutation
{
	private static final List<ItemStack> CACHED_TOME_KNOWLEDGE = Lists.newArrayList();

	public static void clearCache()
	{
		CACHED_TOME_KNOWLEDGE.clear();
	}

	public static void cacheFullKnowledge()
	{
		for (SimpleStack stack : EMCMapper.emc.keySet())
		{
			if (!stack.isValid())
			{
				continue;
			}

			try
			{
				ItemStack item = stack.toItemStack();
				item.stackSize = 1;
				if (EMCHelper.doesItemHaveEmc(item) && EMCHelper.getEmcValue(item) > 0 && !ItemHelper.containsItemStack(CACHED_TOME_KNOWLEDGE, item))
				{
					CACHED_TOME_KNOWLEDGE.add(item);
				}
			}
			catch (Exception e)
			{
				PELogger.logInfo("Failed to cache knowledge for " + stack + ": " + e.toString());
			}
		}
	}

	public static List<ItemStack> getKnowledge(EntityPlayer player)
	{
		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			return teamData.getKnowledge(player);
		}
		return TransmutationProps.getDataFor(player).getKnowledge();
	}

	public static void addKnowledge(ItemStack stack, EntityPlayer player)
	{
		if (hasKnowledgeForStack(stack, player))
		{
			return;
		}

		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			teamData.addKnowledge(player, stack);
		}
		else
		{
			TransmutationProps.getDataFor(player).getKnowledge().add(stack);
		}

		if (!player.worldObj.isRemote)
		{
			MinecraftForge.EVENT_BUS.post(new PlayerKnowledgeChangeEvent(player));
		}
	}

	public static void removeKnowledge(ItemStack stack, EntityPlayer player)
	{
		if (!hasKnowledgeForStack(stack, player))
		{
			return;
		}

		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			teamData.removeKnowledge(player, stack);
		}
		else
		{
			Iterator<ItemStack> iterator = TransmutationProps.getDataFor(player).getKnowledge().iterator();
			while (iterator.hasNext())
			{
				if (ItemStack.areItemStacksEqual(stack, iterator.next()))
				{
					iterator.remove();
					break;
				}
			}
		}

		if (!player.worldObj.isRemote)
		{
			MinecraftForge.EVENT_BUS.post(new PlayerKnowledgeChangeEvent(player));
		}
	}

	public static void setInputsAndLocks(ItemStack[] stacks, EntityPlayer player)
	{
		TransmutationProps.getDataFor(player).setInputLocks(stacks);
	}

	public static ItemStack[] getInputsAndLock(EntityPlayer player)
	{
		ItemStack[] locks = TransmutationProps.getDataFor(player).getInputLocks();
		return Arrays.copyOf(locks, locks.length);
	}

	public static boolean hasKnowledgeForStack(ItemStack stack, EntityPlayer player)
	{
		for (ItemStack known : getKnowledge(player))
		{
			if (ItemHelper.basicAreStacksEqual(known, stack))
			{
				return true;
			}
		}
		return false;
	}

	public static void setFullKnowledge(EntityPlayer player)
	{
		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			teamData.setKnowledge(player, CACHED_TOME_KNOWLEDGE);
		}
		else
		{
			TransmutationProps.getDataFor(player).getKnowledge().clear();
			TransmutationProps.getDataFor(player).getKnowledge().addAll(CACHED_TOME_KNOWLEDGE);
		}
		if (!player.worldObj.isRemote)
		{
			MinecraftForge.EVENT_BUS.post(new PlayerKnowledgeChangeEvent(player));
		}
	}

	public static void clearKnowledge(EntityPlayer player)
	{
		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			teamData.clearKnowledge(player);
		}
		else
		{
			TransmutationProps.getDataFor(player).getKnowledge().clear();
		}
		if (!player.worldObj.isRemote)
		{
			MinecraftForge.EVENT_BUS.post(new PlayerKnowledgeChangeEvent(player));
		}
	}

	public static long getEmc(EntityPlayer player)
	{
		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			return teamData.getEmc(player);
		}
		return TransmutationProps.getDataFor(player).getTransmutationEmc();
	}

	public static void setEmc(EntityPlayer player, long emc)
	{
		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null && teamData.hasTeam(player))
		{
			teamData.setEmc(player, emc);
		}
		else
		{
			TransmutationProps.getDataFor(player).setTransmutationEmc(emc);
		}
	}

	public static void sync(EntityPlayer player)
	{
		NBTTagCompound data = TransmutationProps.getDataFor(player).saveForPacket();
		TransmutationTeamData teamData = getTeamData(player);
		if (teamData != null)
		{
			teamData.applyToSyncTag(player, data);
		}
		PacketHandler.sendTo(new KnowledgeSyncPKT(data), (EntityPlayerMP) player);
		PELogger.logDebug("** SENT TRANSMUTATION DATA **");
	}

	private static TransmutationTeamData getTeamData(EntityPlayer player)
	{
		return player.worldObj.isRemote ? null : TransmutationTeamData.get(player.worldObj);
	}
}