package me.StevenLawson.TotalFreedomMod.Commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import me.StevenLawson.TotalFreedomMod.TFM_Log;
import me.StevenLawson.TotalFreedomMod.TFM_ServerInterface;
import me.StevenLawson.TotalFreedomMod.TFM_SuperadminList;
import me.StevenLawson.TotalFreedomMod.TFM_UserList;
import me.StevenLawson.TotalFreedomMod.TFM_UserList.TFM_UserListEntry;
import me.StevenLawson.TotalFreedomMod.TFM_Util;
import net.minecraft.util.org.apache.commons.lang3.ArrayUtils;
import net.minecraft.util.org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = AdminLevel.SUPER, source = SourceType.BOTH)
@CommandParameters(description = "Ban/Unban any player, even those who are not logged in anymore.", usage = "/<command> <purge | <ban | unban> <username>>")
public class Command_glist extends TFM_Command
{
    @Override
    public boolean run(CommandSender sender, Player sender_p, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        if (args.length == 1)
        {
            if (args[0].equalsIgnoreCase("purge"))
            {
                //Purge does not clear the banlist! This is not for clearing bans! This is for clearing the yaml file that stores the player/IP database!
                if (TFM_SuperadminList.isSeniorAdmin(sender))
                {
                    TFM_UserList.getInstance(plugin).purge();
                }
                else
                {
                    playerMsg("Only Senior Admins may purge the userlist.");
                }
                return true;
            }
            else
            {
                return false;
            }
        }
        else if (args.length >= 2)
        {
            String username;
            List<String> ip_addresses = new ArrayList<String>();

            try
            {
                Player player = getPlayer(args[1]);

                username = player.getName();
                ip_addresses.add(player.getAddress().getAddress().getHostAddress());
            }
            catch (PlayerNotFoundException ex)
            {
                TFM_UserListEntry entry = TFM_UserList.getInstance(plugin).getEntry(args[1]);

                if (entry == null)
                {
                    TFM_Util.playerMsg(sender, "Can't find that user. If target is not logged in, make sure that you spelled the name exactly.");
                    return true;
                }

                username = entry.getUsername();
                ip_addresses = entry.getIpAddresses();
            }

            String mode = args[0].toLowerCase();
            if (mode.equalsIgnoreCase("ban"))
            {
                String ban_reasonRaw = null;
                String ban_reason;
                if (args.length >= 3)
                {
                    ban_reasonRaw = StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ");
                    ban_reason = ban_reasonRaw.replaceAll("'", "&rsquo;");
                }
                
                else
                {
                    ban_reasonRaw = "(Glist Ban)";
                    ban_reason = "(Glist Ban)";
                }
                
                TFM_Util.adminAction(sender.getName(), "Banning " + username + " and IPs: " + StringUtils.join(ip_addresses, ",") + " for '" + ban_reasonRaw + "'", true);

                Player player = server.getPlayerExact(username);
                if (player != null)
                {
                    TFM_ServerInterface.banUsername(player.getName(), ban_reasonRaw, null, null);
                    player.kickPlayer("You have been banned by " + sender.getName() + "for " + ban_reason + "\n If you think you have been banned wrongly, appeal here: http://www.totalfreedom.boards.net");
                }
                else
                {
                    TFM_ServerInterface.banUsername(username, ban_reasonRaw, null, null);
                }

                for (String ip_address : ip_addresses)
                {
                    TFM_ServerInterface.banIP(ip_address, ban_reasonRaw, null, null);
                    String[] ip_address_parts = ip_address.split("\\.");
                    TFM_ServerInterface.banIP(ip_address_parts[0] + "." + ip_address_parts[1] + ".*.*", null, null, null);
                }
                
                long unixTime = System.currentTimeMillis() / 1000L;
                String name;
                if (player != null)
                {
                    name = player.getName();
                }
                
                else
                {
                    name = args[1];
                }
                try
                {
                    plugin.updateDatabase("INSERT INTO cjf_bans (bannedplayer, adminname, reason, time, ip) VALUES ('" + name + "', '" + sender.getName() + "', '" + ban_reason + "', '" + unixTime + "', '" + ip_addresses + "');");
                }
                catch (SQLException ex)
                {
                    sender.sendMessage("Error submitting ban to Database.");
                    TFM_Log.severe(ex);
                }
            }
            else if (mode.equalsIgnoreCase("unban") || mode.equalsIgnoreCase("pardon"))
            {
                TFM_Util.adminAction(sender.getName(), "Unbanning " + username + " and IPs: " + StringUtils.join(ip_addresses, ","), true);

                TFM_ServerInterface.unbanUsername(username);

                for (String ip_address : ip_addresses)
                {
                    TFM_ServerInterface.unbanIP(ip_address);
                    String[] ip_address_parts = ip_address.split("\\.");
                    TFM_ServerInterface.unbanIP(ip_address_parts[0] + "." + ip_address_parts[1] + ".*.*");
                }
            }
            else
            {
                return false;
            }

            return true;
        }
        else
        {
            return false;
        }
    }
}
