package moze_intel.projecte.gameObjs.container.slots.transmutation;

import moze_intel.projecte.api.item.IItemEmc;
import moze_intel.projecte.gameObjs.ObjHandler;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotInput extends Slot
{
	private TransmutationInventory inv;
	
	public SlotInput(TransmutationInventory inv, int par2, int par3, int par4)
	{
		super(inv, par2, par3, par4);
		this.inv = inv;
	}
	
	@Override
	public boolean isItemValid(ItemStack stack)
	{
		return !this.getHasStack() && EMCHelper.doesItemHaveEmc(stack);
	}
	
	@Override
	public void putStack(ItemStack stack)
	{
		if (stack == null)
		{
			return;
		}
		
		super.putStack(stack);
		
		if (stack.getItem() instanceof IItemEmc)
		{
			IItemEmc itemEmc = ((IItemEmc) stack.getItem());
			long remainingEmc = (long) Math.floor(Math.max(0, itemEmc.getMaximumEmc(stack) - itemEmc.getStoredEmc(stack)));
			long toTransfer = Math.min(inv.emc, remainingEmc);
			
			if (toTransfer > 0)
			{
				itemEmc.addEmc(stack, toTransfer);
				inv.removeEmc(toTransfer);
			}
		}
		
		if (stack.getItem() != ObjHandler.tome)
		{
			inv.handleKnowledge(stack.copy());
		}
		else
		{
			inv.updateOutputs();
		}
	}
	
	@Override
	public int getSlotStackLimit()
	{
		return 1;
	}
}
