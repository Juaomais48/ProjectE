package moze_intel.projecte.network.commands;

import com.google.common.collect.Lists;
import moze_intel.projecte.playerData.TransmutationTeamData;
import moze_intel.projecte.utils.Constants;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import java.util.List;
import java.util.Locale;

public class TeamCMD extends ProjectEBaseCMD
{
	private static final String[] ACTIONS = new String[] {"create", "invite", "add", "accept", "remove", "kick", "leave", "disband", "info"};

	@Override
	public String getCommandName() { return "projecte_team"; }

	@Override
	public String getCommandUsage(ICommandSender sender) { return "/projecte team <create|invite|accept|remove|leave|disband|info>"; }

	@Override
	public int getRequiredPermissionLevel() { return 0; }

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] params)
	{
		if (params.length == 1) return getListOfStringsMatchingLastWord(params, ACTIONS);
		if (params.length == 2 && ("invite".equalsIgnoreCase(params[0]) || "add".equalsIgnoreCase(params[0]) || "remove".equalsIgnoreCase(params[0]) || "kick".equalsIgnoreCase(params[0])))
		{
			List<String> names = Lists.newArrayList();
			for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
			{
				names.add(((EntityPlayerMP) object).getCommandSenderName());
			}
			return getListOfStringsMatchingLastWord(params, names.toArray(new String[names.size()]));
		}
		return null;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] params)
	{
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		TransmutationTeamData data = TransmutationTeamData.get(player.worldObj);
		if (params.length == 0)
		{
			showInfo(player, data);
			return;
		}

		String action = params[0].toLowerCase(Locale.ROOT);
		String error;
		if ("create".equals(action))
		{
			if (params.length != 2) { sendUsage(player, "/projecte team create <name>"); return; }
			error = data.createTeam(player, params[1]);
			finish(player, error, "Team " + params[1] + " created.");
		}
		else if ("invite".equals(action) || "add".equals(action))
		{
			if (params.length != 2) { sendUsage(player, "/projecte team invite <player>"); return; }
			EntityPlayerMP target = findOnlinePlayer(params[1]);
			if (target == null) { sendError(player, new ChatComponentText("That player is not online.")); return; }
			error = data.invite(player, target);
			finish(player, error, "Invitation sent to " + target.getCommandSenderName() + ".");
			if (error == null) target.addChatMessage(new ChatComponentText("You were invited to team " + data.getTeamName(player) + ". Use /projecte team accept."));
		}
		else if ("accept".equals(action))
		{
			String team = data.getInvitationName(player);
			error = data.accept(player);
			finish(player, error, "You joined team " + team + ".");
		}
		else if ("remove".equals(action) || "kick".equals(action))
		{
			if (params.length != 2) { sendUsage(player, "/projecte team remove <player>"); return; }
			EntityPlayerMP target = findOnlinePlayer(params[1]);
			error = target == null ? data.kick(player, params[1]) : data.kick(player, target);
			finish(player, error, params[1] + " was removed from the team.");
			if (error == null && target != null) target.addChatMessage(new ChatComponentText("You were removed from your ProjectE team."));
		}
		else if ("leave".equals(action))
		{
			error = data.leave(player);
			finish(player, error, "You left the team.");
		}
		else if ("disband".equals(action))
		{
			error = data.disband(player);
			finish(player, error, "The team was disbanded. Its EMC and knowledge were returned to you.");
		}
		else if ("info".equals(action))
		{
			showInfo(player, data);
		}
		else sendUsage(player, getCommandUsage(player));
	}

	private void showInfo(EntityPlayerMP player, TransmutationTeamData data)
	{
		TransmutationTeamData.TeamInfo info = data.getInfo(player);
		if (info == null)
		{
			String invitation = data.getInvitationName(player);
			sendMessage(player, new ChatComponentText(invitation == null ? "You are not in a ProjectE team." : "Invitation pending for team " + invitation + "."));
			return;
		}
		sendMessage(player, new ChatComponentText("Team: " + info.name + " | Owner: " + info.ownerName));
		sendMessage(player, new ChatComponentText("EMC: " + Constants.formatEmc(info.emc) + " | Knowledge: " + info.knowledgeSize));
		sendMessage(player, new ChatComponentText("Members: " + join(info.members)));
	}

	private static EntityPlayerMP findOnlinePlayer(String name)
	{
		for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
		{
			EntityPlayerMP player = (EntityPlayerMP) object;
			if (player.getCommandSenderName().equalsIgnoreCase(name)) return player;
		}
		return null;
	}

	private void finish(ICommandSender sender, String error, String success)
	{
		if (error == null) sendSuccess(sender, new ChatComponentText(success));
		else sendError(sender, new ChatComponentText(error));
	}

	private void sendUsage(ICommandSender sender, String usage)
	{
		sendError(sender, new ChatComponentText("Usage: " + usage));
	}

	private static String join(List<String> values)
	{
		StringBuilder result = new StringBuilder();
		for (String value : values)
		{
			if (result.length() > 0) result.append(", ");
			result.append(value);
		}
		return result.toString();
	}
}