package moze_intel.projecte.playerData;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TransmutationTeamData extends WorldSavedData
{
	private static final String DATA_NAME = "ProjectETransmutationTeams";

	private final Map<String, Team> teams = Maps.newHashMap();
	private final Map<UUID, String> memberTeams = Maps.newHashMap();
	private final Map<UUID, String> invitations = Maps.newHashMap();

	public TransmutationTeamData()
	{
		super(DATA_NAME);
	}

	public TransmutationTeamData(String name)
	{
		super(name);
	}

	public static TransmutationTeamData get(World world)
	{
		World storageWorld = MinecraftServer.getServer() == null ? world : MinecraftServer.getServer().worldServerForDimension(0);
		MapStorage storage = storageWorld.mapStorage;
		TransmutationTeamData data = (TransmutationTeamData) storage.loadData(TransmutationTeamData.class, DATA_NAME);
		if (data == null)
		{
			data = new TransmutationTeamData();
			storage.setData(DATA_NAME, data);
		}
		return data;
	}

	public boolean hasTeam(EntityPlayer player)
	{
		return hasTeam(player.getUniqueID());
	}

	public boolean hasTeam(UUID player)
	{
		return memberTeams.containsKey(player);
	}

	public String getTeamName(EntityPlayer player)
	{
		Team team = getTeam(player.getUniqueID());
		return team == null ? null : team.name;
	}

	public List<ItemStack> getKnowledge(EntityPlayer player)
	{
		return getKnowledge(player.getUniqueID());
	}

	public List<ItemStack> getKnowledge(UUID player)
	{
		Team team = getTeam(player);
		return team == null ? null : team.knowledge;
	}

	public long getEmc(EntityPlayer player)
	{
		return getEmc(player.getUniqueID());
	}

	public long getEmc(UUID player)
	{
		Team team = getTeam(player);
		return team == null ? -1 : team.emc;
	}

	public boolean hasKnowledgeForStack(UUID player, ItemStack stack)
	{
		List<ItemStack> knowledge = getKnowledge(player);
		if (knowledge == null)
		{
			return false;
		}
		for (ItemStack known : knowledge)
		{
			if (ItemHelper.basicAreStacksEqual(known, stack))
			{
				return true;
			}
		}
		return false;
	}

	public void setEmc(EntityPlayer player, long emc)
	{
		Team team = getTeam(player.getUniqueID());
		if (team != null)
		{
			team.emc = Math.max(0, emc);
			markDirty();
			syncTeam(team);
		}
	}

	public void addKnowledge(EntityPlayer player, ItemStack stack)
	{
		Team team = getTeam(player.getUniqueID());
		if (team != null && addKnowledge(team, stack))
		{
			markDirty();
			syncTeam(team);
		}
	}

	public void removeKnowledge(EntityPlayer player, ItemStack stack)
	{
		Team team = getTeam(player.getUniqueID());
		if (team == null)
		{
			return;
		}

		Iterator<ItemStack> iterator = team.knowledge.iterator();
		while (iterator.hasNext())
		{
			if (ItemHelper.basicAreStacksEqual(stack, iterator.next()))
			{
				iterator.remove();
				markDirty();
				syncTeam(team);
				return;
			}
		}
	}

	public void clearKnowledge(EntityPlayer player)
	{
		Team team = getTeam(player.getUniqueID());
		if (team != null && !team.knowledge.isEmpty())
		{
			team.knowledge.clear();
			markDirty();
			syncTeam(team);
		}
	}

	public void setKnowledge(EntityPlayer player, Iterable<ItemStack> knowledge)
	{
		Team team = getTeam(player.getUniqueID());
		if (team != null)
		{
			team.knowledge.clear();
			for (ItemStack stack : knowledge)
			{
				addKnowledge(team, stack);
			}
			markDirty();
			syncTeam(team);
		}
	}

	public void applyToSyncTag(EntityPlayer player, NBTTagCompound tag)
	{
		Team team = getTeam(player.getUniqueID());
		if (team == null)
		{
			return;
		}

		tag.setLong("transmutationEmc", team.emc);
		NBTTagList knowledge = new NBTTagList();
		for (ItemStack stack : team.knowledge)
		{
			knowledge.appendTag(stack.writeToNBT(new NBTTagCompound()));
		}
		tag.setTag("knowledge", knowledge);
	}

	public String createTeam(EntityPlayerMP owner, String name)
	{
		if (hasTeam(owner))
		{
			return "You are already in a team.";
		}
		if (!name.matches("[A-Za-z0-9_-]{3,24}"))
		{
			return "Team names must have 3-24 letters, numbers, _ or -.";
		}

		String key = normalize(name);
		if (teams.containsKey(key))
		{
			return "A team with that name already exists.";
		}

		Team team = new Team(name, owner.getUniqueID());
		teams.put(key, team);
		mergePersonalData(team, owner);
		addMember(team, owner);
		invitations.remove(owner.getUniqueID());
		markDirty();
		Transmutation.sync(owner);
		return null;
	}

	public String invite(EntityPlayerMP owner, EntityPlayerMP target)
	{
		Team team = getTeam(owner.getUniqueID());
		if (team == null)
		{
			return "You are not in a team.";
		}
		if (!team.owner.equals(owner.getUniqueID()))
		{
			return "Only the team owner can invite players.";
		}
		if (hasTeam(target))
		{
			return "That player is already in a team.";
		}

		invitations.put(target.getUniqueID(), normalize(team.name));
		markDirty();
		return null;
	}

	public String accept(EntityPlayerMP player)
	{
		if (hasTeam(player))
		{
			return "You are already in a team.";
		}

		String key = invitations.get(player.getUniqueID());
		Team team = key == null ? null : teams.get(key);
		if (team == null)
		{
			invitations.remove(player.getUniqueID());
			return "You do not have a valid team invitation.";
		}

		if (!canMergePersonalData(team, player))
		{
			return "Your EMC does not fit in the team balance.";
		}

		mergePersonalData(team, player);
		addMember(team, player);
		invitations.remove(player.getUniqueID());
		markDirty();
		syncTeam(team);
		return null;
	}

	public String kick(EntityPlayerMP owner, EntityPlayerMP target)
	{
		return kick(owner, target.getUniqueID(), target.getCommandSenderName());
	}

	public String kick(EntityPlayerMP owner, String targetName)
	{
		Team team = getTeam(owner.getUniqueID());
		if (team == null)
		{
			return "You are not in a team.";
		}
		for (Map.Entry<UUID, String> entry : team.members.entrySet())
		{
			if (entry.getValue().equalsIgnoreCase(targetName))
			{
				return kick(owner, entry.getKey(), entry.getValue());
			}
		}
		return "That player is not in your team.";
	}

	private String kick(EntityPlayerMP owner, UUID target, String targetName)
	{
		Team team = getTeam(owner.getUniqueID());
		if (team == null)
		{
			return "You are not in a team.";
		}
		if (!team.owner.equals(owner.getUniqueID()))
		{
			return "Only the team owner can remove players.";
		}
		if (owner.getUniqueID().equals(target))
		{
			return "Use /projecte team leave or disband instead.";
		}
		if (!team.members.containsKey(target))
		{
			return "That player is not in your team.";
		}

		removeMember(team, target);
		markDirty();
		syncPlayers(Collections.singletonList(target));
		syncTeam(team);
		return null;
	}

	public String leave(EntityPlayerMP player)
	{
		Team team = getTeam(player.getUniqueID());
		if (team == null)
		{
			return "You are not in a team.";
		}
		if (team.members.size() == 1)
		{
			return disband(player);
		}

		boolean wasOwner = team.owner.equals(player.getUniqueID());
		removeMember(team, player.getUniqueID());
		if (wasOwner)
		{
			team.owner = team.members.keySet().iterator().next();
		}
		markDirty();
		Transmutation.sync(player);
		syncTeam(team);
		return null;
	}

	public String disband(EntityPlayerMP owner)
	{
		Team team = getTeam(owner.getUniqueID());
		if (team == null)
		{
			return "You are not in a team.";
		}
		if (!team.owner.equals(owner.getUniqueID()))
		{
			return "Only the team owner can disband it.";
		}

		List<UUID> oldMembers = new ArrayList<UUID>(team.members.keySet());
		TransmutationProps ownerData = TransmutationProps.getDataFor(owner);
		ownerData.setTransmutationEmc(team.emc);
		ownerData.getKnowledge().clear();
		for (ItemStack stack : team.knowledge)
		{
			ownerData.getKnowledge().add(stack.copy());
		}

		teams.remove(normalize(team.name));
		for (UUID member : oldMembers)
		{
			memberTeams.remove(member);
		}
		removeInvitationsFor(normalize(team.name));
		markDirty();
		syncPlayers(oldMembers);
		return null;
	}

	public TeamInfo getInfo(EntityPlayer player)
	{
		Team team = getTeam(player.getUniqueID());
		if (team == null)
		{
			return null;
		}
		return new TeamInfo(team.name, team.members.get(team.owner), new ArrayList<String>(team.members.values()), team.emc, team.knowledge.size());
	}

	public String getInvitationName(EntityPlayer player)
	{
		String key = invitations.get(player.getUniqueID());
		Team team = key == null ? null : teams.get(key);
		return team == null ? null : team.name;
	}

	public void updatePlayerName(EntityPlayerMP player)
	{
		Team team = getTeam(player.getUniqueID());
		if (team != null && !player.getCommandSenderName().equals(team.members.get(player.getUniqueID())))
		{
			team.members.put(player.getUniqueID(), player.getCommandSenderName());
			markDirty();
			syncTeam(team);
		}
	}

	private Team getTeam(UUID player)
	{
		String key = memberTeams.get(player);
		return key == null ? null : teams.get(key);
	}

	private boolean canMergePersonalData(Team team, EntityPlayerMP player)
	{
		long personalEmc = TransmutationProps.getDataFor(player).getTransmutationEmc();
		return personalEmc <= Long.MAX_VALUE - team.emc;
	}

	private void mergePersonalData(Team team, EntityPlayerMP player)
	{
		TransmutationProps data = TransmutationProps.getDataFor(player);
		long personalEmc = data.getTransmutationEmc();
		if (personalEmc > Long.MAX_VALUE - team.emc)
		{
			throw new IllegalStateException("Team EMC overflow while merging player data");
		}
		team.emc += personalEmc;
		for (ItemStack stack : data.getKnowledge())
		{
			addKnowledge(team, stack);
		}
		data.setTransmutationEmc(0);
		data.getKnowledge().clear();
	}

	private void addMember(Team team, EntityPlayerMP player)
	{
		team.members.put(player.getUniqueID(), player.getCommandSenderName());
		memberTeams.put(player.getUniqueID(), normalize(team.name));
	}

	private void removeMember(Team team, UUID player)
	{
		team.members.remove(player);
		memberTeams.remove(player);
		invitations.remove(player);
	}

	private boolean addKnowledge(Team team, ItemStack stack)
	{
		for (ItemStack known : team.knowledge)
		{
			if (ItemHelper.basicAreStacksEqual(known, stack))
			{
				return false;
			}
		}

		ItemStack copy = stack.copy();
		copy.stackSize = 1;
		team.knowledge.add(copy);
		return true;
	}

	private void syncTeam(Team team)
	{
		syncPlayers(team.members.keySet());
	}

	private void syncPlayers(Iterable<UUID> players)
	{
		for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
		{
			EntityPlayerMP player = (EntityPlayerMP) object;
			for (UUID uuid : players)
			{
				if (uuid.equals(player.getUniqueID()))
				{
					Transmutation.sync(player);
					break;
				}
			}
		}
	}

	private void removeInvitationsFor(String teamKey)
	{
		Iterator<Map.Entry<UUID, String>> iterator = invitations.entrySet().iterator();
		while (iterator.hasNext())
		{
			if (teamKey.equals(iterator.next().getValue()))
			{
				iterator.remove();
			}
		}
	}

	private static String normalize(String name)
	{
		return name.toLowerCase(Locale.ROOT);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		teams.clear();
		memberTeams.clear();
		invitations.clear();

		NBTTagList teamList = compound.getTagList("teams", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < teamList.tagCount(); i++)
		{
			NBTTagCompound teamTag = teamList.getCompoundTagAt(i);
			Team team = new Team(teamTag.getString("name"), UUID.fromString(teamTag.getString("owner")));
			team.emc = Math.max(0, teamTag.getLong("emc"));

			NBTTagList members = teamTag.getTagList("members", Constants.NBT.TAG_COMPOUND);
			for (int j = 0; j < members.tagCount(); j++)
			{
				NBTTagCompound member = members.getCompoundTagAt(j);
				UUID uuid = UUID.fromString(member.getString("uuid"));
				team.members.put(uuid, member.getString("name"));
				memberTeams.put(uuid, normalize(team.name));
			}

			NBTTagList knowledge = teamTag.getTagList("knowledge", Constants.NBT.TAG_COMPOUND);
			for (int j = 0; j < knowledge.tagCount(); j++)
			{
				ItemStack stack = ItemStack.loadItemStackFromNBT(knowledge.getCompoundTagAt(j));
				if (stack != null)
				{
					addKnowledge(team, stack);
				}
			}
			teams.put(normalize(team.name), team);
		}

		NBTTagList inviteList = compound.getTagList("invitations", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < inviteList.tagCount(); i++)
		{
			NBTTagCompound invite = inviteList.getCompoundTagAt(i);
			invitations.put(UUID.fromString(invite.getString("uuid")), invite.getString("team"));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound compound)
	{
		NBTTagList teamList = new NBTTagList();
		for (Team team : teams.values())
		{
			NBTTagCompound teamTag = new NBTTagCompound();
			teamTag.setString("name", team.name);
			teamTag.setString("owner", team.owner.toString());
			teamTag.setLong("emc", team.emc);

			NBTTagList members = new NBTTagList();
			for (Map.Entry<UUID, String> entry : team.members.entrySet())
			{
				NBTTagCompound member = new NBTTagCompound();
				member.setString("uuid", entry.getKey().toString());
				member.setString("name", entry.getValue());
				members.appendTag(member);
			}
			teamTag.setTag("members", members);

			NBTTagList knowledge = new NBTTagList();
			for (ItemStack stack : team.knowledge)
			{
				knowledge.appendTag(stack.writeToNBT(new NBTTagCompound()));
			}
			teamTag.setTag("knowledge", knowledge);
			teamList.appendTag(teamTag);
		}
		compound.setTag("teams", teamList);

		NBTTagList inviteList = new NBTTagList();
		for (Map.Entry<UUID, String> entry : invitations.entrySet())
		{
			NBTTagCompound invite = new NBTTagCompound();
			invite.setString("uuid", entry.getKey().toString());
			invite.setString("team", entry.getValue());
			inviteList.appendTag(invite);
		}
		compound.setTag("invitations", inviteList);
	}

	private static class Team
	{
		private final String name;
		private UUID owner;
		private long emc;
		private final Map<UUID, String> members = Maps.newLinkedHashMap();
		private final List<ItemStack> knowledge = Lists.newArrayList();

		private Team(String name, UUID owner)
		{
			this.name = name;
			this.owner = owner;
		}
	}

	public static class TeamInfo
	{
		public final String name;
		public final String ownerName;
		public final List<String> members;
		public final long emc;
		public final int knowledgeSize;

		private TeamInfo(String name, String ownerName, List<String> members, long emc, int knowledgeSize)
		{
			this.name = name;
			this.ownerName = ownerName;
			this.members = members;
			this.emc = emc;
			this.knowledgeSize = knowledgeSize;
		}
	}
}