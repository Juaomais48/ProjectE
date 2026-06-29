package moze_intel.projecte.playerData;

import com.google.common.collect.Lists;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PELogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import java.util.Iterator;
import java.util.List;

public class TransmutationProps implements IExtendedEntityProperties
{
	private final EntityPlayer player;

	private long transmutationEmc;
	private List<ItemStack> knowledge = Lists.newArrayList();
	private ItemStack[] inputLocks = new ItemStack[9];
	public static final String PROP_NAME = "ProjectETransmutation";

	public static void register(EntityPlayer player)
	{
		player.registerExtendedProperties(PROP_NAME, new TransmutationProps(player));
	}

	public static TransmutationProps getDataFor(EntityPlayer player)
	{
		return ((TransmutationProps) player.getExtendedProperties(PROP_NAME));
	}

	public TransmutationProps(EntityPlayer player)
	{
		this.player = player;
	}

	public ItemStack[] getInputLocks()
	{
		return inputLocks;
	}

	public void setInputLocks(ItemStack[] inputLocks)
	{
		this.inputLocks = inputLocks;
	}

	protected long getTransmutationEmc()
	{
		return transmutationEmc;
	}

	protected void setTransmutationEmc(long transmutationEmc)
	{
		this.transmutationEmc = Math.max(0, transmutationEmc);
	}

	protected List<ItemStack> getKnowledge()
	{
		pruneStaleKnowledge();
		return knowledge;
	}

	private void pruneDuplicateKnowledge()
	{
		ItemHelper.compactItemListNoStacksize(knowledge);
		for (ItemStack s : knowledge)
		{
			if (s.stackSize > 1)
			{
				s.stackSize = 1;
			}
		}
	}

	private void pruneStaleKnowledge()
	{
		Iterator<ItemStack> iter = knowledge.iterator();
		while (iter.hasNext())
		{
			if (!EMCHelper.doesItemHaveEmc(iter.next()))
			{
				iter.remove();
			}
		}
	}

	protected NBTTagCompound saveForPacket()
	{
		NBTTagCompound compound = new NBTTagCompound();
		compound.setLong("transmutationEmc", transmutationEmc);

		pruneStaleKnowledge();
		NBTTagList knowledgeWrite = new NBTTagList();
		for (ItemStack i : knowledge)
		{
			NBTTagCompound tag = i.writeToNBT(new NBTTagCompound());
			knowledgeWrite.appendTag(tag);
		}

		NBTTagList inputLockWrite = ItemHelper.toIndexedNBTList(inputLocks);
		compound.setTag("knowledge", knowledgeWrite);
		compound.setTag("inputlocks", inputLockWrite);
		return compound;
	}

	public void readFromPacket(NBTTagCompound compound)
	{
		transmutationEmc = readTransmutationEmc(compound);

		NBTTagList list = compound.getTagList("knowledge", Constants.NBT.TAG_COMPOUND);
		knowledge.clear();
		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemStack item = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
			if (item != null)
			{
				knowledge.add(item);
			}
		}

		NBTTagList list2 = compound.getTagList("inputlocks", Constants.NBT.TAG_COMPOUND);
		inputLocks = ItemHelper.copyIndexedNBTToArray(list2, new ItemStack[9]);
	}

	static long readTransmutationEmc(NBTTagCompound compound)
	{
		if (compound.hasKey("transmutationEmc", Constants.NBT.TAG_LONG))
		{
			return Math.max(0, compound.getLong("transmutationEmc"));
		}

		if (compound.hasKey("transmutationEmc", Constants.NBT.TAG_DOUBLE))
		{
			double legacyEmc = compound.getDouble("transmutationEmc");
			if (legacyEmc <= 0)
			{
				return 0;
			}
			if (legacyEmc >= Long.MAX_VALUE)
			{
				return Long.MAX_VALUE;
			}
			return (long) Math.ceil(legacyEmc);
		}

		return 0;
	}
	@Override
	public void saveNBTData(NBTTagCompound compound)
	{
		NBTTagCompound properties = new NBTTagCompound();
		properties.setLong("transmutationEmc", transmutationEmc);

		pruneStaleKnowledge();
		NBTTagList knowledgeWrite = new NBTTagList();
		for (ItemStack i : knowledge)
		{
			NBTTagCompound tag = i.writeToNBT(new NBTTagCompound());
			knowledgeWrite.appendTag(tag);
		}

		NBTTagList inputLockWrite = ItemHelper.toIndexedNBTList(inputLocks);
		properties.setTag("knowledge", knowledgeWrite);
		properties.setTag("inputlock", inputLockWrite);
		compound.setTag(PROP_NAME, properties);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound)
	{
		NBTTagCompound properties = compound.getCompoundTag(PROP_NAME);

		transmutationEmc = readTransmutationEmc(properties);

		NBTTagList list = properties.getTagList("knowledge", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemStack item = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
			if (item != null)
			{
				knowledge.add(item);
			}
		}
		pruneDuplicateKnowledge();
		NBTTagList list2 = properties.getTagList("inputlock", Constants.NBT.TAG_COMPOUND);
		inputLocks = ItemHelper.copyIndexedNBTToArray(list2, new ItemStack[9]);
	}

	@Override
	public void init(Entity entity, World world) {}
}
