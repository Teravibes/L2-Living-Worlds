/*
 * Custom admin command handler for live rate editing.
 */
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.ConfigLoader;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * //rates               — opens the rates panel
 * //setrate <key> <val> — sets a rate live and saves Rates.ini
 */
public class AdminRates implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_rates",
		"admin_setrate"
	};

	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_rates"))
		{
			showPanel(activeChar);
		}
		else if (command.startsWith("admin_setrate"))
		{
			final StringTokenizer st = new StringTokenizer(command.substring("admin_setrate".length()).trim());
			if (st.countTokens() < 2)
			{
				activeChar.sendSysMessage("Usage: //setrate <key> <value>");
				showPanel(activeChar);
				return true;
			}
			final String key = st.nextToken();
			final String val = st.nextToken();
			if (applyRate(activeChar, key, val))
			{
				saveRatesIni();
				AdminData.getInstance().broadcastMessageToGMs(activeChar.getName() + ": Set rate " + key + " = " + val);
				activeChar.sendSysMessage("Rate " + key + " set to " + val + " and saved.");
			}
			showPanel(activeChar);
		}
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private boolean applyRate(Player activeChar, String key, String val)
	{
		try
		{
			switch (key.toLowerCase())
			{
				case "xp":
					RatesConfig.RATE_XP = Float.parseFloat(val);
					break;
				case "sp":
					RatesConfig.RATE_SP = Float.parseFloat(val);
					break;
				case "partyxp":
					RatesConfig.RATE_PARTY_XP = Float.parseFloat(val);
					break;
				case "partysp":
					RatesConfig.RATE_PARTY_SP = Float.parseFloat(val);
					break;
				case "adena":
					RatesConfig.RATE_DROP_AMOUNT_BY_ID.put(57, Float.parseFloat(val));
					break;
				case "drop":
					RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER = Float.parseFloat(val);
					RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER = Float.parseFloat(val);
					break;
				case "dropamount":
					RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER = Float.parseFloat(val);
					break;
				case "dropchance":
					RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER = Float.parseFloat(val);
					break;
				case "spoil":
					RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER = Float.parseFloat(val);
					RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER = Float.parseFloat(val);
					break;
				case "raidrop":
					RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER = Float.parseFloat(val);
					RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER = Float.parseFloat(val);
					break;
				case "questxp":
					RatesConfig.RATE_QUEST_REWARD_XP = Float.parseFloat(val);
					break;
				case "questsp":
					RatesConfig.RATE_QUEST_REWARD_SP = Float.parseFloat(val);
					break;
				case "questadena":
					RatesConfig.RATE_QUEST_REWARD_ADENA = Float.parseFloat(val);
					break;
				default:
					activeChar.sendSysMessage("Unknown key: " + key + ". Valid keys: xp sp partyxp partysp adena drop dropamount dropchance spoil raidrop questxp questsp questadena");
					return false;
			}
		}
		catch (NumberFormatException e)
		{
			activeChar.sendSysMessage("Invalid value: " + val + " (must be a number)");
			return false;
		}
		return true;
	}

	private void saveRatesIni()
	{
		// Persist to disk so //reload config picks up the same values.
		final java.io.File file = new java.io.File("./config/Rates.ini");
		try
		{
			final java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
			final java.util.List<String> out = new java.util.ArrayList<>();
			for (String line : lines)
			{
				final String trimmed = line.trim();
				if (trimmed.startsWith("RateXp"))                        line = "RateXp = " + RatesConfig.RATE_XP;
				else if (trimmed.startsWith("RateSp "))                  line = "RateSp = " + RatesConfig.RATE_SP;
				else if (trimmed.startsWith("RatePartyXp"))              line = "RatePartyXp = " + RatesConfig.RATE_PARTY_XP;
				else if (trimmed.startsWith("RatePartySp"))              line = "RatePartySp = " + RatesConfig.RATE_PARTY_SP;
				else if (trimmed.startsWith("DeathDropAmountMultiplier")) line = "DeathDropAmountMultiplier = " + RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
				else if (trimmed.startsWith("DeathDropChanceMultiplier")) line = "DeathDropChanceMultiplier = " + RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
				else if (trimmed.startsWith("SpoilDropAmountMultiplier")) line = "SpoilDropAmountMultiplier = " + RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
				else if (trimmed.startsWith("SpoilDropChanceMultiplier")) line = "SpoilDropChanceMultiplier = " + RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				else if (trimmed.startsWith("RaidDropAmountMultiplier"))  line = "RaidDropAmountMultiplier = " + RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
				else if (trimmed.startsWith("RaidDropChanceMultiplier"))  line = "RaidDropChanceMultiplier = " + RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				else if (trimmed.startsWith("RateQuestRewardXP"))        line = "RateQuestRewardXP = " + RatesConfig.RATE_QUEST_REWARD_XP;
				else if (trimmed.startsWith("RateQuestRewardSP"))        line = "RateQuestRewardSP = " + RatesConfig.RATE_QUEST_REWARD_SP;
				else if (trimmed.startsWith("RateQuestRewardAdena"))     line = "RateQuestRewardAdena = " + RatesConfig.RATE_QUEST_REWARD_ADENA;
				else if (trimmed.startsWith("DropAmountMultiplierByItemId"))
				{
					// Rebuild the adena entry inline, preserve others
					final Float adena = RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(57);
					if (adena != null)
					{
						final StringBuilder sb = new StringBuilder("DropAmountMultiplierByItemId = 57,").append(adena);
						for (java.util.Map.Entry<Integer, Float> e : RatesConfig.RATE_DROP_AMOUNT_BY_ID.entrySet())
						{
							if (e.getKey() != 57)
								sb.append(";").append(e.getKey()).append(",").append(e.getValue());
						}
						line = sb.toString();
					}
				}
				out.add(line);
			}
			java.nio.file.Files.write(file.toPath(), out);
		}
		catch (Exception e)
		{
			// If we can't write the file, the in-memory values are still live until next restart.
		}
	}

	private void showPanel(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/rates.htm");
		// Inject current values
		html.replace("%xp%",          fmt(RatesConfig.RATE_XP));
		html.replace("%sp%",          fmt(RatesConfig.RATE_SP));
		html.replace("%partyxp%",     fmt(RatesConfig.RATE_PARTY_XP));
		html.replace("%partysp%",     fmt(RatesConfig.RATE_PARTY_SP));
		final Float adena = RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(57);
		html.replace("%adena%",       fmt(adena != null ? adena : 1f));
		html.replace("%dropamount%",  fmt(RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER));
		html.replace("%dropchance%",  fmt(RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER));
		html.replace("%spoil%",       fmt(RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER));
		html.replace("%raidrop%",     fmt(RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER));
		html.replace("%questxp%",     fmt(RatesConfig.RATE_QUEST_REWARD_XP));
		html.replace("%questadena%",  fmt(RatesConfig.RATE_QUEST_REWARD_ADENA));
		activeChar.sendPacket(html);
	}

	private String fmt(float v)
	{
		// Show as integer if whole number, otherwise one decimal
		return (v == (int) v) ? String.valueOf((int) v) : String.valueOf(v);
	}
}
