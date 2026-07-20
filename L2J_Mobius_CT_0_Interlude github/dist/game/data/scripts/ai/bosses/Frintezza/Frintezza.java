/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.bosses.Frintezza;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillCanceled;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Frintezza AI
 * @author Darki699, sandman, JOJO, Skache
 */
public class Frintezza extends Script
{
	// NPCs
	private static final int SCARLET1 = 29046;
	private static final int SCARLET2 = 29047;
	private static final int FRINTEZZA = 29045;
	private static final int GUIDE = 32011;
	private static final int TELEPORT_CUBE = 29061;
	
	// Frintezza Status Tracking
	private static final byte DORMANT = 0; // Frintezza is spawned and no one has entered yet. Entry is unlocked.
	private static final byte WAITING = 1; // Frintezza is spawend and someone has entered, triggering a 30 minute window for additional people to enter before he unleashes his attack. Entry is unlocked.
	private static final byte FIGHTING = 2; // Frintezza is engaged in battle, annihilating his foes. Entry is locked.
	private static final byte DEAD = 3; // Frintezza has been killed. Entry is locked.
	
	// @formatter:off
	private static final int[][] _invadeLoc =
	{
		{174102, -76039, -5105},
		{173235, -76884, -5105},
		{175003, -76933, -5105},
		{174196, -76190, -5105},
		{174013, -76120, -5105},
		{173263, -75161, -5105}
	};
	private static final int[][] _skill =
	{
		{5015, 1, 5000},
		{5015, 4, 5000},
		{5015, 2, 5000},
		{5015, 5, 5000},
		{5018, 1, 10000},
		{5016, 1, 5000},
		{5015, 3, 5000},
		{5015, 6, 5000},
		{5018, 2, 10000},
		{5019, 1, 10000},
		{5016, 1, 5000}
	};
	private static final int[][] _mobLoc =
	{
		{18328, 172894, -76019, -5107, 243},
		{18328, 174095, -77279, -5107, 16216},
		{18328, 174111, -74833, -5107, 49043},
		{18328, 175344, -76042, -5107, 32847},
		{18330, 173489, -76227, -5134, 63565},
		{18330, 173498, -75724, -5107, 58498},
		{18330, 174365, -76745, -5107, 22424},
		{18330, 174570, -75584, -5107, 31968},
		{18330, 174613, -76179, -5107, 31471},
		{18332, 173620, -75981, -5107, 4588},
		{18332, 173630, -76340, -5107, 62454},
		{18332, 173755, -75613, -5107, 57892},
		{18332, 173823, -76688, -5107, 2411},
		{18332, 174000, -75411, -5107, 54718},
		{18332, 174487, -75555, -5107, 33861},
		{18332, 174517, -76471, -5107, 21893},
		{18332, 174576, -76122, -5107, 31176},
		{18332, 174600, -75841, -5134, 35927},
		{18329, 173481, -76043, -5107, 61312},
		{18329, 173539, -75678, -5107, 59524},
		{18329, 173584, -76386, -5107, 3041},
		{18329, 173773, -75420, -5107, 51115},
		{18329, 173777, -76650, -5107, 12588},
		{18329, 174585, -76510, -5107, 21704},
		{18329, 174623, -75571, -5107, 40141},
		{18329, 174744, -76240, -5107, 29202},
		{18329, 174769, -75895, -5107, 29572},
		{18333, 173861, -76011, -5107, 383},
		{18333, 173872, -76461, -5107, 8041},
		{18333, 173898, -75668, -5107, 51856},
		{18333, 174422, -75689, -5107, 42878},
		{18333, 174460, -76355, -5107, 27311},
		{18333, 174483, -76041, -5107, 30947},
		{18331, 173515, -76184, -5107, 6971},
		{18331, 173516, -75790, -5134, 3142},
		{18331, 173696, -76675, -5107, 6757},
		{18331, 173766, -75502, -5134, 60827},
		{18331, 174473, -75321, -5107, 37147},
		{18331, 174493, -76505, -5107, 34503},
		{18331, 174568, -75654, -5134, 41661},
		{18331, 174584, -76263, -5107, 31729},
		{18339, 173892, -81592, -5123, 50849},
		{18339, 173958, -81820, -5123, 7459},
		{18339, 174128, -81805, -5150, 21495},
		{18339, 174245, -81566, -5123, 41760},
		{18334, 173264, -81529, -5072, 1646},
		{18334, 173265, -81656, -5072, 441},
		{18334, 173267, -81889, -5072, 0},
		{18334, 173271, -82015, -5072, 65382},
		{18334, 174867, -81655, -5073, 32537},
		{18334, 174868, -81890, -5073, 32768},
		{18334, 174869, -81485, -5073, 32315},
		{18334, 174871, -82017, -5073, 33007},
		{18335, 173074, -80817, -5107, 8353},
		{18335, 173128, -82702, -5107, 5345},
		{18335, 173181, -82544, -5107, 65135},
		{18335, 173191, -80981, -5107, 6947},
		{18335, 174859, -80889, -5134, 24103},
		{18335, 174924, -82666, -5107, 38710},
		{18335, 174947, -80733, -5107, 22449},
		{18335, 175096, -82724, -5107, 42205},
		{18336, 173435, -80512, -5107, 65215},
		{18336, 173440, -82948, -5107, 417},
		{18336, 173443, -83120, -5107, 1094},
		{18336, 173463, -83064, -5107, 286},
		{18336, 173465, -80453, -5107, 174},
		{18336, 173465, -83006, -5107, 2604},
		{18336, 173468, -82889, -5107, 316},
		{18336, 173469, -80570, -5107, 65353},
		{18336, 173469, -80628, -5107, 166},
		{18336, 173492, -83121, -5107, 394},
		{18336, 173493, -80683, -5107, 0},
		{18336, 173497, -80510, -5134, 417},
		{18336, 173499, -82947, -5107, 0},
		{18336, 173521, -83063, -5107, 316},
		{18336, 173523, -82889, -5107, 128},
		{18336, 173524, -80627, -5134, 65027},
		{18336, 173524, -83007, -5107, 0},
		{18336, 173526, -80452, -5107, 64735},
		{18336, 173527, -80569, -5134, 65062},
		{18336, 174602, -83122, -5107, 33104},
		{18336, 174604, -82949, -5107, 33184},
		{18336, 174609, -80514, -5107, 33234},
		{18336, 174609, -80684, -5107, 32851},
		{18336, 174629, -80627, -5107, 33346},
		{18336, 174632, -80570, -5107, 32896},
		{18336, 174632, -83066, -5107, 32768},
		{18336, 174635, -82893, -5107, 33594},
		{18336, 174636, -80456, -5107, 32065},
		{18336, 174639, -83008, -5107, 33057},
		{18336, 174660, -80512, -5107, 33057},
		{18336, 174661, -83121, -5107, 32768},
		{18336, 174663, -82948, -5107, 32768},
		{18336, 174664, -80685, -5107, 32676},
		{18336, 174687, -83008, -5107, 32520},
		{18336, 174691, -83066, -5107, 32961},
		{18336, 174692, -80455, -5107, 33202},
		{18336, 174692, -80571, -5107, 32768},
		{18336, 174693, -80630, -5107, 32994},
		{18336, 174693, -82889, -5107, 32622},
		{18337, 172837, -82382, -5107, 58363},
		{18337, 172867, -81123, -5107, 64055},
		{18337, 172883, -82495, -5107, 64764},
		{18337, 172916, -81033, -5107, 7099},
		{18337, 172940, -82325, -5107, 58998},
		{18337, 172946, -82435, -5107, 58038},
		{18337, 172971, -81198, -5107, 14768},
		{18337, 172992, -81091, -5107, 9438},
		{18337, 173032, -82365, -5107, 59041},
		{18337, 173064, -81125, -5107, 5827},
		{18337, 175014, -81173, -5107, 26398},
		{18337, 175061, -82374, -5107, 43290},
		{18337, 175096, -81080, -5107, 24719},
		{18337, 175169, -82453, -5107, 37672},
		{18337, 175172, -80972, -5107, 32315},
		{18337, 175174, -82328, -5107, 41760},
		{18337, 175197, -81157, -5107, 27617},
		{18337, 175245, -82547, -5107, 40275},
		{18337, 175249, -81075, -5107, 28435},
		{18337, 175292, -82432, -5107, 42225},
		{18338, 173014, -82628, -5107, 11874},
		{18338, 173033, -80920, -5107, 10425},
		{18338, 173095, -82520, -5107, 49152},
		{18338, 173115, -80986, -5107, 9611},
		{18338, 173144, -80894, -5107, 5345},
		{18338, 173147, -82602, -5107, 51316},
		{18338, 174912, -80825, -5107, 24270},
		{18338, 174935, -80899, -5107, 18061},
		{18338, 175016, -82697, -5107, 39533},
		{18338, 175041, -80834, -5107, 25420},
		{18338, 175071, -82549, -5107, 39163},
		{18338, 175154, -82619, -5107, 36345}
	};
	// @formatter:on
	
	private static long _lastAction = 0;
	private static int _angle = 0;
	private static int _locCycle = 0;
	private static int _bomber = 0;
	private static int _checkDie = 0;
	private static int _onCheck = 0;
	private static int _onSong = 0;
	private static int _abnormal = 0;
	private static int _onMorph = 0;
	private static int _scarletX = 0;
	private static int _scarletY = 0;
	private static int _scarletZ = 0;
	private static int _scarletH = 0;
	private static int _secondMorph = 0;
	private static int _thirdMorph = 0;
	private static int _killHallAlarmDevice = 0;
	private static int _killDarkChoirPlayer = 0;
	private static int _killDarkChoirCaptain = 0;
	
	private static BossZone _zone;
	private GrandBoss _frintezza, _weakScarlet, _strongScarlet, _activeScarlet;
	private Monster _demon1, _demon2, _demon3, _demon4, _portrait1, _portrait2, _portrait3, _portrait4;
	private Npc _frintezzaDummy, _overheadDummy, _portraitDummy1, _portraitDummy3, _scarletDummy;
	private static List<Player> _playersInside = new CopyOnWriteArrayList<>();
	private static List<Npc> _room1Mobs = new CopyOnWriteArrayList<>();
	private static List<Npc> _room2Mobs = new CopyOnWriteArrayList<>();
	private static List<Attackable> _minions = new CopyOnWriteArrayList<>();
	
	private Frintezza()
	{
		final int[] mobs =
		{
			SCARLET1,
			SCARLET2,
			FRINTEZZA,
			18328,
			18329,
			18330,
			18331,
			18332,
			18333,
			18334,
			18335,
			18336,
			18337,
			18338,
			18339,
			29048,
			29049,
			29050,
			29051
		};
		addAttackId(mobs);
		addKillId(mobs);
		addStartNpc(GUIDE);
		addTalkId(GUIDE);
		addStartNpc(TELEPORT_CUBE);
		addTalkId(TELEPORT_CUBE);
		
		_zone = GrandBossManager.getInstance().getZone(174232, -88020, -5116);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
		final int status = GrandBossManager.getInstance().getStatus(FRINTEZZA);
		if (status == DEAD)
		{
			final long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
			if (temp > 0)
			{
				startQuestTimer("frintezza_unlock", temp, null, null);
			}
			else
			{
				GrandBossManager.getInstance().setStatus(FRINTEZZA, DORMANT);
			}
		}
		else if (status != DORMANT)
		{
			GrandBossManager.getInstance().setStatus(FRINTEZZA, DORMANT);
		}
		
		// Tempfix for messed door cords.
		for (int i = 0; i < 8; i++)
		{
			// DoorData.getInstance().getDoor(25150051+i).setRange(0, 0, 0, 0, 0, 0)
			DoorData.getInstance().getDoor(25150051 + i).setTargetable(false);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		long temp = 0;
		switch (event)
		{
			case "waiting":
			{
				startQuestTimer("close", 27000, npc, null);
				startQuestTimer("camera_1", 30000, npc, null);
				_zone.broadcastPacket(new Earthquake(174232, -88020, -5116, 45, 27));
				break;
			}
			case "room1_spawn":
			{
				for (int i = 0; i <= 17; i++)
				{
					final Npc mob = addSpawn(_mobLoc[i][0], _mobLoc[i][1], _mobLoc[i][2], _mobLoc[i][3], _mobLoc[i][4], false, 0);
					_room1Mobs.add(mob);
				}
				break;
			}
			case "room1_spawn2":
			{
				for (int i = 18; i <= 26; i++)
				{
					final Npc mob = addSpawn(_mobLoc[i][0], _mobLoc[i][1], _mobLoc[i][2], _mobLoc[i][3], _mobLoc[i][4], false, 0);
					_room1Mobs.add(mob);
				}
				break;
			}
			case "room1_spawn3":
			{
				for (int i = 27; i <= 32; i++)
				{
					final Npc mob = addSpawn(_mobLoc[i][0], _mobLoc[i][1], _mobLoc[i][2], _mobLoc[i][3], _mobLoc[i][4], false, 0);
					_room1Mobs.add(mob);
				}
				break;
			}
			case "room1_spawn4":
			{
				for (int i = 33; i <= 40; i++)
				{
					final Npc mob = addSpawn(_mobLoc[i][0], _mobLoc[i][1], _mobLoc[i][2], _mobLoc[i][3], _mobLoc[i][4], false, 0);
					_room1Mobs.add(mob);
				}
				break;
			}
			case "room2_spawn":
			{
				for (int i = 41; i <= 44; i++)
				{
					final Npc mob = addSpawn(_mobLoc[i][0], _mobLoc[i][1], _mobLoc[i][2], _mobLoc[i][3], _mobLoc[i][4], false, 0);
					_room2Mobs.add(mob);
				}
				break;
			}
			case "room2_spawn2":
			{
				for (int i = 45; i <= 131; i++)
				{
					final Npc mob = addSpawn(_mobLoc[i][0], _mobLoc[i][1], _mobLoc[i][2], _mobLoc[i][3], _mobLoc[i][4], false, 0);
					_room2Mobs.add(mob);
				}
				break;
			}
			case "room1_del":
			{
				for (Npc mob : _room1Mobs)
				{
					if (mob != null)
					{
						mob.deleteMe();
					}
				}
				
				_room1Mobs.clear();
				break;
			}
			case "room2_del":
			{
				for (Npc mob : _room2Mobs)
				{
					if (mob != null)
					{
						mob.deleteMe();
					}
				}
				
				_room2Mobs.clear();
				startQuestTimer("waiting", 180000, npc, null);
				break;
			}
			case "room3_del":
			{
				if (_demon1 != null)
				{
					_demon1.deleteMe();
				}
				
				if (_demon2 != null)
				{
					_demon2.deleteMe();
				}
				
				if (_demon3 != null)
				{
					_demon3.deleteMe();
				}
				
				if (_demon4 != null)
				{
					_demon4.deleteMe();
				}
				
				if (_portrait1 != null)
				{
					_portrait1.deleteMe();
				}
				
				if (_portrait2 != null)
				{
					_portrait2.deleteMe();
				}
				
				if (_portrait3 != null)
				{
					_portrait3.deleteMe();
				}
				
				if (_portrait4 != null)
				{
					_portrait4.deleteMe();
				}
				
				if (_frintezza != null)
				{
					_frintezza.deleteMe();
				}
				
				if (_weakScarlet != null)
				{
					_weakScarlet.deleteMe();
				}
				
				if (_strongScarlet != null)
				{
					_strongScarlet.deleteMe();
				}
				
				_demon1 = null;
				_demon2 = null;
				_demon3 = null;
				_demon4 = null;
				_portrait1 = null;
				_portrait2 = null;
				_portrait3 = null;
				_portrait4 = null;
				_frintezza = null;
				_weakScarlet = null;
				_strongScarlet = null;
				_activeScarlet = null;
				break;
			}
			case "clean":
			{
				_lastAction = 0;
				_locCycle = 0;
				_checkDie = 0;
				_onCheck = 0;
				_abnormal = 0;
				_onMorph = 0;
				_secondMorph = 0;
				_thirdMorph = 0;
				_killHallAlarmDevice = 0;
				_killDarkChoirPlayer = 0;
				_killDarkChoirCaptain = 0;
				_playersInside.clear();
				break;
			}
			case "close":
			{
				for (int i = 25150051; i <= 25150058; i++)
				{
					DoorData.getInstance().getDoor(i).closeMe();
				}
				
				for (int i = 25150061; i <= 25150070; i++)
				{
					DoorData.getInstance().getDoor(i).closeMe();
				}
				
				DoorData.getInstance().getDoor(25150042).closeMe();
				DoorData.getInstance().getDoor(25150043).closeMe();
				DoorData.getInstance().getDoor(25150045).closeMe();
				DoorData.getInstance().getDoor(25150046).closeMe();
				break;
			}
			case "loc_check":
			{
				if (GrandBossManager.getInstance().getStatus(FRINTEZZA) == FIGHTING)
				{
					if (!_zone.isInsideZone(npc))
					{
						npc.teleToLocation(174232, -88020, -5116);
					}
					
					if ((npc.getX() < 171932) || (npc.getX() > 176532) || (npc.getY() < -90320) || (npc.getY() > -85720) || (npc.getZ() < -5130))
					{
						npc.teleToLocation(174232, -88020, -5116);
					}
				}
				break;
			}
			case "camera_1":
			{
				GrandBossManager.getInstance().setStatus(FRINTEZZA, FIGHTING);
				_frintezzaDummy = addSpawn(29052, 174240, -89805, -5022, 16048, false, 0);
				_frintezzaDummy.setInvul(true);
				_frintezzaDummy.setImmobilized(true);
				_overheadDummy = addSpawn(29052, 174232, -88020, -5110, 16384, false, 0);
				_overheadDummy.setInvul(true);
				_overheadDummy.setImmobilized(true);
				_overheadDummy.setCollisionHeight(600);
				_overheadDummy.broadcastInfo();
				_portraitDummy1 = addSpawn(29052, 172450, -87890, -5100, 16048, false, 0);
				_portraitDummy1.setImmobilized(true);
				_portraitDummy1.setInvul(true);
				_portraitDummy3 = addSpawn(29052, 176012, -87890, -5100, 16048, false, 0);
				_portraitDummy3.setImmobilized(true);
				_portraitDummy3.setInvul(true);
				_scarletDummy = addSpawn(29053, 174232, -88020, -5110, 16384, false, 0);
				_scarletDummy.setInvul(true);
				_scarletDummy.setImmobilized(true);
				startQuestTimer("stop_pc", 0, npc, null);
				startQuestTimer("camera_2", 1000, _overheadDummy, null);
				break;
			}
			case "camera_2":
			{
				_zone.broadcastPacket(new SpecialCamera(_overheadDummy, 0, 75, -89, 0, 100, 0, 0, 1, 0, 0));
				startQuestTimer("camera_2b", 0, _overheadDummy, null);
				break;
			}
			case "camera_2b":
			{
				_zone.broadcastPacket(new SpecialCamera(_overheadDummy, 0, 75, -89, 0, 100, 0, 0, 1, 0, 0));
				startQuestTimer("camera_3", 0, _overheadDummy, null);
				break;
			}
			case "camera_3":
			{
				_zone.broadcastPacket(new SpecialCamera(_overheadDummy, 300, 90, -10, 6500, 7000, 0, 0, 1, 0, 0));
				_frintezza = (GrandBoss) addSpawn(FRINTEZZA, 174240, -89805, -5022, 16048, false, 0);
				GrandBossManager.getInstance().addBoss(_frintezza);
				_frintezza.setImmobilized(true);
				_frintezza.setInvul(true);
				_frintezza.disableAllSkills();
				_demon2 = addSpawn(29051, 175876, -88713, -5100, 28205, false, 0).asMonster();
				_demon2.setImmobilized(true);
				_demon2.disableAllSkills();
				_demon3 = addSpawn(29051, 172608, -88702, -5100, 64817, false, 0).asMonster();
				_demon3.setImmobilized(true);
				_demon3.disableAllSkills();
				_demon1 = addSpawn(29050, 175833, -87165, -5100, 35048, false, 0).asMonster();
				_demon1.setImmobilized(true);
				_demon1.disableAllSkills();
				_demon4 = addSpawn(29050, 172634, -87165, -5100, 57730, false, 0).asMonster();
				_demon4.setImmobilized(true);
				_demon4.disableAllSkills();
				startQuestTimer("camera_4", 6500, _overheadDummy, null);
				break;
			}
			case "camera_4":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezzaDummy, 1800, 90, 8, 6500, 7000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_5", 900, _frintezzaDummy, null);
				break;
			}
			case "camera_5":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezzaDummy, 140, 90, 10, 2500, 4500, 0, 0, 1, 0, 0));
				startQuestTimer("camera_5b", 4000, _frintezzaDummy, null);
				break;
			}
			case "camera_5b":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 40, 75, -10, 0, 1000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_6", 0, _frintezza, null);
				break;
			}
			case "camera_6":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 40, 75, -10, 0, 12000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_7", 1350, _frintezza, null);
				break;
			}
			case "camera_7":
			{
				_zone.broadcastPacket(new SocialAction(_frintezza.getObjectId(), 2));
				startQuestTimer("camera_8", 7000, _frintezza, null);
				break;
			}
			case "camera_8":
			{
				startQuestTimer("camera_9", 1000, _frintezza, null);
				_frintezzaDummy.deleteMe();
				_frintezzaDummy = null;
				break;
			}
			case "camera_9":
			{
				_zone.broadcastPacket(new SocialAction(_demon2.getObjectId(), 1));
				_zone.broadcastPacket(new SocialAction(_demon3.getObjectId(), 1));
				startQuestTimer("camera_9b", 400, _frintezza, null);
				break;
			}
			case "camera_9b":
			{
				_zone.broadcastPacket(new SocialAction(_demon1.getObjectId(), 1));
				_zone.broadcastPacket(new SocialAction(_demon4.getObjectId(), 1));
				for (Creature pc : _zone.getCharactersInside())
				{
					if (pc.isPlayer())
					{
						if (pc.getX() < 174232)
						{
							pc.broadcastPacket(new SpecialCamera(_portraitDummy1, 1000, 118, 0, 0, 1000, 0, 0, 1, 0, 0));
						}
						else
						{
							pc.broadcastPacket(new SpecialCamera(_portraitDummy3, 1000, 62, 0, 0, 1000, 0, 0, 1, 0, 0));
						}
					}
				}
				
				startQuestTimer("camera_9c", 0, _frintezza, null);
				break;
			}
			case "camera_9c":
			{
				for (Creature pc : _zone.getCharactersInside())
				{
					if (pc instanceof Player)
					{
						if (pc.getX() < 174232)
						{
							pc.broadcastPacket(new SpecialCamera(_portraitDummy1, 1000, 118, 0, 0, 10000, 0, 0, 1, 0, 0));
						}
						else
						{
							pc.broadcastPacket(new SpecialCamera(_portraitDummy3, 1000, 62, 0, 0, 10000, 0, 0, 1, 0, 0));
						}
					}
				}
				
				startQuestTimer("camera_10", 2000, _frintezza, null);
				break;
			}
			case "camera_10":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 240, 90, 0, 0, 1000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_11", 0, _frintezza, null);
				break;
			}
			case "camera_11":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 240, 90, 25, 5500, 10000, 0, 0, 1, 0, 0));
				_zone.broadcastPacket(new SocialAction(_frintezza.getObjectId(), 3));
				_portraitDummy1.deleteMe();
				_portraitDummy3.deleteMe();
				_portraitDummy1 = null;
				_portraitDummy3 = null;
				startQuestTimer("camera_12", 4500, _frintezza, null);
				break;
			}
			case "camera_12":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 100, 195, 35, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_13", 700, _frintezza, null);
				break;
			}
			case "camera_13":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 100, 195, 35, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_14", 1300, _frintezza, null);
				break;
			}
			case "camera_14":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 120, 180, 45, 1500, 10000, 0, 0, 1, 0, 0));
				_zone.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5006, 1, 34000, 0));
				startQuestTimer("camera_16", 1500, _frintezza, null);
				break;
			}
			case "camera_16":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 520, 135, 45, 8000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_17", 7500, _frintezza, null);
				break;
			}
			case "camera_17":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 1500, 110, 25, 10000, 13000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_18", 9500, _frintezza, null);
				break;
			}
			case "camera_18":
			{
				_zone.broadcastPacket(new SpecialCamera(_overheadDummy, 930, 160, -20, 0, 1000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_18b", 0, _overheadDummy, null);
				break;
			}
			case "camera_18b":
			{
				_zone.broadcastPacket(new SpecialCamera(_overheadDummy, 600, 180, -25, 0, 10000, 0, 0, 1, 0, 0));
				_zone.broadcastPacket(new MagicSkillUse(_scarletDummy, _overheadDummy, 5004, 1, 5800, 0));
				_weakScarlet = (GrandBoss) addSpawn(SCARLET1, 174232, -88020, -5110, 16384, false, 0, true);
				_weakScarlet.setInvul(true);
				_weakScarlet.setImmobilized(true);
				_weakScarlet.disableAllSkills();
				_activeScarlet = _weakScarlet;
				startQuestTimer("camera_19", 2400, _scarletDummy, null);
				startQuestTimer("camera_19b", 5000, _scarletDummy, null);
				break;
			}
			case "camera_19":
			{
				_weakScarlet.teleToLocation(174232, -88020, -5110);
				break;
			}
			case "camera_19b":
			{
				_zone.broadcastPacket(new SpecialCamera(_scarletDummy, 800, 180, 10, 1000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_20", 2100, _scarletDummy, null);
				break;
			}
			case "camera_20":
			{
				_zone.broadcastPacket(new SpecialCamera(_weakScarlet, 300, 60, 8, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_21", 2000, _weakScarlet, null);
				break;
			}
			case "camera_21":
			{
				_zone.broadcastPacket(new SpecialCamera(_weakScarlet, 500, 90, 10, 3000, 5000, 0, 0, 1, 0, 0));
				startQuestTimer("camera_22", 3000, _weakScarlet, null);
				break;
			}
			case "camera_22":
			{
				_portrait2 = addSpawn(29049, 175876, -88713, -5000, 28205, false, 0).asMonster();
				_portrait2.setImmobilized(true);
				_portrait2.disableAllSkills();
				_portrait3 = addSpawn(29049, 172608, -88702, -5000, 64817, false, 0).asMonster();
				_portrait3.setImmobilized(true);
				_portrait3.disableAllSkills();
				_portrait1 = addSpawn(29048, 175833, -87165, -5000, 35048, false, 0).asMonster();
				_portrait1.setImmobilized(true);
				_portrait1.disableAllSkills();
				_portrait4 = addSpawn(29048, 172634, -87165, -5000, 57730, false, 0).asMonster();
				_portrait4.setImmobilized(true);
				_portrait4.disableAllSkills();
				_overheadDummy.deleteMe();
				_scarletDummy.deleteMe();
				_overheadDummy = null;
				_scarletDummy = null;
				startQuestTimer("camera_23", 2000, _weakScarlet, null);
				startQuestTimer("start_pc", 2000, _weakScarlet, null);
				startQuestTimer("loc_check", 60000, _weakScarlet, null, true);
				startQuestTimer("songs_play", 10000 + getRandom(10000), _frintezza, null);
				startQuestTimer("skill01", 10000 + getRandom(10000), _weakScarlet, null);
				break;
			}
			case "camera_23":
			{
				_demon1.setImmobilized(false);
				_demon2.setImmobilized(false);
				_demon3.setImmobilized(false);
				_demon4.setImmobilized(false);
				_demon1.enableAllSkills();
				_demon2.enableAllSkills();
				_demon3.enableAllSkills();
				_demon4.enableAllSkills();
				_portrait1.setImmobilized(false);
				_portrait2.setImmobilized(false);
				_portrait3.setImmobilized(false);
				_portrait4.setImmobilized(false);
				_portrait1.enableAllSkills();
				_portrait2.enableAllSkills();
				_portrait3.enableAllSkills();
				_portrait4.enableAllSkills();
				_weakScarlet.setInvul(false);
				_weakScarlet.setImmobilized(false);
				_weakScarlet.enableAllSkills();
				_weakScarlet.setRunning();
				startQuestTimer("spawn_minion", 20000, _portrait1, null);
				startQuestTimer("spawn_minion", 20000, _portrait2, null);
				startQuestTimer("spawn_minion", 20000, _portrait3, null);
				startQuestTimer("spawn_minion", 20000, _portrait4, null);
				break;
			}
			case "stop_pc":
			{
				for (Creature cha : _zone.getCharactersInside())
				{
					cha.abortAttack();
					cha.abortCast();
					cha.disableAllSkills();
					cha.setTarget(null);
					cha.stopMove(null);
					cha.setImmobilized(true);
					cha.getAI().setIntention(Intention.IDLE);
				}
				break;
			}
			case "stop_npc":
			{
				final int heading = npc.getHeading();
				if (heading < 32768)
				{
					_angle = Math.abs(180 - (int) (heading / 182.044444444));
				}
				else
				{
					_angle = Math.abs(540 - (int) (heading / 182.044444444));
				}
				break;
			}
			case "start_pc":
			{
				for (Creature cha : _zone.getCharactersInside())
				{
					if (cha != _frintezza)
					{
						cha.enableAllSkills();
						cha.setImmobilized(false);
					}
				}
				break;
			}
			case "start_npc":
			{
				npc.setRunning();
				npc.setInvul(false);
				break;
			}
			case "morph_end":
			{
				_onMorph = 0;
				break;
			}
			case "morph_01":
			{
				_zone.broadcastPacket(new SpecialCamera(_weakScarlet, 250, _angle, 12, 2000, 15000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_02", 3000, _weakScarlet, null);
				break;
			}
			case "morph_02":
			{
				_zone.broadcastPacket(new SocialAction(_weakScarlet.getObjectId(), 1));
				_weakScarlet.setRHandId(7903);
				startQuestTimer("morph_03", 4000, _weakScarlet, null);
				break;
			}
			case "morph_03":
			{
				startQuestTimer("morph_04", 1500, _weakScarlet, null);
				break;
			}
			case "morph_04":
			{
				_zone.broadcastPacket(new SocialAction(_weakScarlet.getObjectId(), 4));
				final Skill skill = SkillData.getInstance().getSkill(5017, 1);
				if (skill != null)
				{
					skill.applyEffects(_weakScarlet, _weakScarlet);
				}
				
				_weakScarlet.setCollisionHeight(109.4);
				_weakScarlet.setCollisionRadius(54);
				_weakScarlet.broadcastInfo();
				startQuestTimer("morph_end", 6000, _weakScarlet, null);
				startQuestTimer("start_pc", 3000, _weakScarlet, null);
				startQuestTimer("start_npc", 3000, _weakScarlet, null);
				startQuestTimer("songs_play", 10000 + getRandom(10000), _frintezza, null);
				startQuestTimer("skill02", 10000 + getRandom(10000), _weakScarlet, null);
				break;
			}
			case "morph_05a":
			{
				_zone.broadcastPacket(new SocialAction(_frintezza.getObjectId(), 4));
				break;
			}
			case "morph_05":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 250, 120, 15, 0, 1000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_06", 0, _frintezza, null);
				break;
			}
			case "morph_06":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 250, 120, 15, 0, 10000, 0, 0, 1, 0, 0));
				cancelQuestTimers("loc_check");
				_scarletX = _weakScarlet.getX();
				_scarletY = _weakScarlet.getY();
				_scarletZ = _weakScarlet.getZ();
				_scarletH = _weakScarlet.getHeading();
				_weakScarlet.deleteMe();
				_weakScarlet = null;
				_activeScarlet = null;
				_weakScarlet = (GrandBoss) addSpawn(SCARLET1, _scarletX, _scarletY, _scarletZ, _scarletH, false, 0);
				_weakScarlet.setInvul(true);
				_weakScarlet.setImmobilized(true);
				_weakScarlet.disableAllSkills();
				_weakScarlet.setRHandId(7903);
				startQuestTimer("morph_07", 7000, _frintezza, null);
				break;
			}
			case "morph_07":
			{
				_zone.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5006, 1, 34000, 0));
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 500, 70, 15, 3000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_08", 3000, _frintezza, null);
				break;
			}
			case "morph_08":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 2500, 90, 12, 6000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_09", 3000, _frintezza, null);
				break;
			}
			case "morph_09":
			{
				_zone.broadcastPacket(new SpecialCamera(_weakScarlet, 250, _angle, 12, 0, 1000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_10", 0, _weakScarlet, null);
				break;
			}
			case "morph_10":
			{
				_zone.broadcastPacket(new SpecialCamera(_weakScarlet, 250, _angle, 12, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_11", 500, _weakScarlet, null);
				break;
			}
			case "morph_11":
			{
				_weakScarlet.doDie(_weakScarlet);
				_zone.broadcastPacket(new SpecialCamera(_weakScarlet, 450, _angle, 14, 8000, 8000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_12", 6250, _weakScarlet, null);
				startQuestTimer("morph_13", 6250, _weakScarlet, null);
				break;
			}
			case "morph_12":
			{
				_weakScarlet.deleteMe();
				_weakScarlet = null;
				break;
			}
			case "morph_13":
			{
				_strongScarlet = (GrandBoss) addSpawn(SCARLET2, _scarletX, _scarletY, _scarletZ, _scarletH, false, 0);
				_strongScarlet.setInvul(true);
				_strongScarlet.setImmobilized(true);
				_strongScarlet.disableAllSkills();
				_activeScarlet = _strongScarlet;
				_zone.broadcastPacket(new SpecialCamera(_strongScarlet, 450, _angle, 12, 500, 14000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_14", 3000, _strongScarlet, null);
				startQuestTimer("loc_check", 60000, _strongScarlet, null, true);
				break;
			}
			case "morph_14":
			{
				startQuestTimer("morph_15", 5100, _strongScarlet, null);
				break;
			}
			case "morph_15":
			{
				_zone.broadcastPacket(new SocialAction(_strongScarlet.getObjectId(), 2));
				final Skill skill = SkillData.getInstance().getSkill(5017, 1);
				if (skill != null)
				{
					skill.applyEffects(_strongScarlet, _strongScarlet);
				}
				
				_strongScarlet.setCollisionHeight(130);
				_strongScarlet.setCollisionRadius(115);
				_strongScarlet.broadcastInfo();
				startQuestTimer("morph_end", 9000, _strongScarlet, null);
				startQuestTimer("start_pc", 6000, _strongScarlet, null);
				startQuestTimer("start_npc", 6000, _strongScarlet, null);
				startQuestTimer("songs_play", 10000 + getRandom(10000), _frintezza, null);
				startQuestTimer("skill03", 10000 + getRandom(10000), _strongScarlet, null);
				break;
			}
			case "morph_16":
			{
				_zone.broadcastPacket(new SpecialCamera(_strongScarlet, 300, _angle - 180, 5, 0, 7000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_17", 0, _strongScarlet, null);
				break;
			}
			case "morph_17":
			{
				_zone.broadcastPacket(new SpecialCamera(_strongScarlet, 200, _angle, 85, 4000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_17b", 7400, _frintezza, null);
				startQuestTimer("morph_18", 7500, _frintezza, null);
				break;
			}
			case "morph_17b":
			{
				_frintezza.doDie(_frintezza);
				break;
			}
			case "morph_18":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 100, 120, 5, 0, 7000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_19", 0, _frintezza, null);
				break;
			}
			case "morph_19":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 100, 90, 5, 5000, 15000, 0, 0, 1, 0, 0));
				startQuestTimer("morph_20", 7000, _frintezza, null);
				startQuestTimer("spawn_cubes", 7000, _frintezza, null);
				break;
			}
			case "morph_20":
			{
				_zone.broadcastPacket(new SpecialCamera(_frintezza, 900, 90, 25, 7000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("start_pc", 7000, _frintezza, null);
				break;
			}
			case "songs_play":
			{
				if ((_frintezza != null) && !_frintezza.isDead() && (_onMorph == 0))
				{
					_onSong = getRandom(1, 5);
					if ((_onSong == 1) && (_thirdMorph == 1) && (_strongScarlet.getCurrentHp() < (_strongScarlet.getMaxHp() * 0.6)) && (getRandom(100) < 80))
					{
						_zone.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 1, 32000, 0));
						_zone.broadcastPacket(new ExShowScreenMessage("Requiem of Hatred", ExShowScreenMessage.TOP_CENTER, 3000));
						startQuestTimer("songs_effect", 5000, _frintezza, null);
						startQuestTimer("songs_play", 32000 + getRandom(10000), _frintezza, null);
					}
					else if ((_onSong == 2) || (_onSong == 3))
					{
						_zone.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, _onSong, 32000, 0));
						if (_onSong == 2)
						{
							_zone.broadcastPacket(new ExShowScreenMessage("Frenetic Toccata", ExShowScreenMessage.TOP_CENTER, 3000));
						}
						else
						{
							_zone.broadcastPacket(new ExShowScreenMessage("Fugue of Jubilation", ExShowScreenMessage.TOP_CENTER, 3000));
						}
						
						startQuestTimer("songs_effect", 5000, _frintezza, null);
						startQuestTimer("songs_play", 32000 + getRandom(10000), _frintezza, null);
					}
					else if ((_onSong == 4) && (_secondMorph == 1))
					{
						_zone.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 4, 31000, 0));
						_zone.broadcastPacket(new ExShowScreenMessage("Mournful Chorale Prelude", ExShowScreenMessage.TOP_CENTER, 3000));
						startQuestTimer("songs_effect", 5000, _frintezza, null);
						startQuestTimer("songs_play", 31000 + getRandom(10000), _frintezza, null);
					}
					else if ((_onSong == 5) && (_thirdMorph == 1) && (_abnormal == 0))
					{
						_abnormal = 1;
						_zone.broadcastPacket(new MagicSkillUse(_frintezza, _frintezza, 5007, 5, 35000, 0));
						_zone.broadcastPacket(new ExShowScreenMessage("Hypnotic Mazurka", ExShowScreenMessage.TOP_CENTER, 3000));
						startQuestTimer("songs_effect", 5000, _frintezza, null);
						startQuestTimer("songs_play", 35000 + getRandom(10000), _frintezza, null);
					}
					else
					{
						startQuestTimer("songs_play", 5000 + getRandom(5000), _frintezza, null);
					}
				}
				break;
			}
			case "songs_effect":
			{
				final Skill skill = SkillData.getInstance().getSkill(5008, _onSong);
				if (skill == null)
				{
					return null;
				}
				
				if ((_onSong == 1) || (_onSong == 2) || (_onSong == 3))
				{
					if ((_frintezza != null) && !_frintezza.isDead() && (_activeScarlet != null) && !_activeScarlet.isDead())
					{
						skill.applyEffects(_frintezza, _activeScarlet);
					}
				}
				else if (_onSong == 4)
				{
					for (Creature cha : _zone.getCharactersInside())
					{
						if ((cha instanceof Player) && (getRandom(100) < 80))
						{
							skill.applyEffects(_frintezza, cha);
							cha.sendPacket(new SystemMessage(SystemMessageId.THE_EFFECTS_OF_S1_FLOW_THROUGH_YOU).addSkillName(5008, 4));
						}
					}
				}
				else if (_onSong == 5)
				{
					for (Creature cha : _zone.getCharactersInside())
					{
						if ((cha instanceof Player) && (getRandom(100) < 70))
						{
							cha.abortAttack();
							cha.abortCast();
							cha.disableAllSkills();
							cha.stopMove(null);
							cha.setParalyzed(true);
							cha.setImmobilized(true);
							cha.getAI().setIntention(Intention.IDLE);
							skill.applyEffects(_frintezza, cha);
							cha.startAbnormalVisualEffect(true, AbnormalVisualEffect.DANCE_ROOT);
							cha.sendPacket(new SystemMessage(SystemMessageId.THE_EFFECTS_OF_S1_FLOW_THROUGH_YOU).addSkillName(5008, 5));
						}
					}
					
					startQuestTimer("stop_effect", 25000, _frintezza, null);
				}
				break;
			}
			case "stop_effect":
			{
				for (Creature cha : _zone.getCharactersInside())
				{
					if (cha instanceof Player)
					{
						cha.stopAbnormalVisualEffect(true, AbnormalVisualEffect.DANCE_ROOT);
						cha.stopAbnormalVisualEffect(true, AbnormalVisualEffect.FLOATING_ROOT);
						cha.enableAllSkills();
						cha.setImmobilized(false);
						cha.setParalyzed(false);
					}
				}
				
				_abnormal = 0;
				break;
			}
			case "attack_stop":
			{
				cancelQuestTimers("skill01");
				cancelQuestTimers("skill02");
				cancelQuestTimers("skill03");
				cancelQuestTimers("songs_play");
				cancelQuestTimers("songs_effect");
				if (_frintezza != null)
				{
					_zone.broadcastPacket(new MagicSkillCanceled(_frintezza.getObjectId()));
				}
				break;
			}
			case "check_hp":
			{
				if (npc.isDead())
				{
					_onMorph = 1;
					_zone.broadcastPacket(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
					startQuestTimer("attack_stop", 0, _frintezza, null);
					startQuestTimer("stop_pc", 0, npc, null);
					startQuestTimer("stop_npc", 0, npc, null);
					startQuestTimer("morph_16", 0, npc, null);
				}
				else
				{
					_checkDie = _checkDie + 10;
					if (_checkDie < 3000)
					{
						startQuestTimer("check_hp", 10, npc, null);
					}
					else
					{
						_onCheck = 0;
						_checkDie = 0;
					}
				}
				break;
			}
			case "skill01":
			{
				if ((_weakScarlet != null) && !_weakScarlet.isDead() && (_secondMorph == 0) && (_thirdMorph == 0) && (_onMorph == 0))
				{
					final int i = getRandom(0, 1);
					final Skill skill = SkillData.getInstance().getSkill(_skill[i][0], _skill[i][1]);
					if (skill != null)
					{
						_weakScarlet.stopMove(null);
						_weakScarlet.setCastingNow(true);
						_weakScarlet.doCast(skill);
					}
					
					startQuestTimer("skill01", _skill[i][2] + 5000 + getRandom(10000), npc, null);
				}
				break;
			}
			case "skill02":
			{
				if ((_weakScarlet != null) && !_weakScarlet.isDead() && (_secondMorph == 1) && (_thirdMorph == 0) && (_onMorph == 0))
				{
					int i = 0;
					if (_abnormal == 0)
					{
						i = getRandom(2, 5);
					}
					else
					{
						i = getRandom(2, 4);
					}
					
					final Skill skill = SkillData.getInstance().getSkill(_skill[i][0], _skill[i][1]);
					if (skill != null)
					{
						_weakScarlet.stopMove(null);
						_weakScarlet.setCastingNow(true);
						_weakScarlet.doCast(skill);
					}
					
					startQuestTimer("skill02", _skill[i][2] + 5000 + getRandom(10000), npc, null);
					if (i == 5)
					{
						_abnormal = 1;
						startQuestTimer("float_effect", 4000, _weakScarlet, null);
					}
				}
				break;
			}
			case "skill03":
			{
				if ((_strongScarlet != null) && !_strongScarlet.isDead() && (_secondMorph == 1) && (_thirdMorph == 1) && (_onMorph == 0))
				{
					int i = 0;
					if (_abnormal == 0)
					{
						i = getRandom(6, 10);
					}
					else
					{
						i = getRandom(6, 9);
					}
					
					final Skill skill = SkillData.getInstance().getSkill(_skill[i][0], _skill[i][1]);
					if (skill != null)
					{
						_strongScarlet.stopMove(null);
						_strongScarlet.setCastingNow(true);
						_strongScarlet.doCast(skill);
					}
					
					startQuestTimer("skill03", _skill[i][2] + 5000 + getRandom(10000), npc, null);
					if (i == 10)
					{
						_abnormal = 1;
						startQuestTimer("float_effect", 3000, npc, null);
					}
				}
				break;
			}
			case "float_effect":
			{
				if (npc.isCastingNow())
				{
					startQuestTimer("float_effect", 500, npc, null);
				}
				else
				{
					for (Creature cha : _zone.getCharactersInside())
					{
						if ((cha instanceof Player) && (cha.getEffectList().getBuffInfoBySkillId(5016) != null))
						{
							cha.abortAttack();
							cha.abortCast();
							cha.disableAllSkills();
							cha.stopMove(null);
							cha.setParalyzed(true);
							cha.setImmobilized(true);
							cha.getAI().setIntention(Intention.IDLE);
							cha.startAbnormalVisualEffect(true, AbnormalVisualEffect.FLOATING_ROOT);
						}
					}
					
					startQuestTimer("stop_effect", 25000, npc, null);
				}
				break;
			}
			case "action":
			{
				_zone.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
				break;
			}
			case "bomber":
			{
				_bomber = 0;
				break;
			}
			case "room_final":
			{
				_zone.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.SHOUT, npc.getId(), "Exceeded his time limit, challenge failed!"));
				_zone.oustAllPlayers();
				cancelQuestTimers("waiting");
				cancelQuestTimers("frintezza_despawn");
				startQuestTimer("clean", 1000, npc, null);
				startQuestTimer("close", 1000, npc, null);
				startQuestTimer("room1_del", 1000, npc, null);
				startQuestTimer("room2_del", 1000, npc, null);
				GrandBossManager.getInstance().setStatus(FRINTEZZA, DORMANT);
				break;
			}
			case "frintezza_despawn":
			{
				temp = (System.currentTimeMillis() - _lastAction);
				if (temp > 900000)
				{
					_zone.oustAllPlayers();
					
					cancelQuestTimers("loc_check");
					cancelQuestTimers("room_final");
					cancelQuestTimers("spawn_minion");
					startQuestTimer("clean", 1000, npc, null);
					startQuestTimer("close", 1000, npc, null);
					startQuestTimer("attack_stop", 1000, npc, null);
					startQuestTimer("room1_del", 1000, npc, null);
					startQuestTimer("room3_del", 1000, npc, null);
					startQuestTimer("minions_despawn", 1000, npc, null);
					GrandBossManager.getInstance().setStatus(FRINTEZZA, DORMANT);
					cancelQuestTimers("frintezza_despawn");
				}
				break;
			}
			case "minions_despawn":
			{
				for (int i = 0; i < _minions.size(); i++)
				{
					final Attackable mob = _minions.get(i);
					if (mob != null)
					{
						mob.decayMe();
					}
				}
				
				_minions.clear();
				break;
			}
			case "spawn_minion":
			{
				if ((npc != null) && !npc.isDead() && (_frintezza != null) && !_frintezza.isDead())
				{
					final Npc mob = addSpawn(npc.getId() + 2, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
					
					// mob.setIsRaidMinion(true);
					_minions.add(mob.asAttackable());
					startQuestTimer("action", 200, mob, null);
					startQuestTimer("spawn_minion", 18000, npc, null);
				}
				break;
			}
			case "spawn_cubes":
			{
				addSpawn(TELEPORT_CUBE, 174232, -88020, -5114, 16384, false, 900000);
				break;
			}
			case "frintezza_unlock":
			{
				GrandBossManager.getInstance().setStatus(FRINTEZZA, DORMANT);
				break;
			}
			case "remove_players":
			{
				_zone.oustAllPlayers();
				break;
			}
			case "EXIT":
			{
				if (npc.getId() == TELEPORT_CUBE)
				{
					final int x = 150037 + getRandom(500);
					final int y = -57720 + getRandom(500);
					player.teleToLocation(x, y, -2976);
					return null;
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		
		if (player.getWeightPenalty() >= 3)
		{
			player.sendPacket(SystemMessageId.PROGRESS_IN_A_QUEST_IS_POSSIBLE_ONLY_WHEN_YOUR_INVENTORY_S_WEIGHT_AND_VOLUME_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
			return "";
		}
		
		String htmltext = "";
		if (GrandBossManager.getInstance().getStatus(FRINTEZZA) == DEAD)
		{
			htmltext = "<html><body>There is nothing beyond the Magic Force Field. Come back later.<br>(You may not enter because Frintezza is not inside the Imperial Tomb.)</body></html>";
		}
		else if (GrandBossManager.getInstance().getStatus(FRINTEZZA) == DORMANT)
		{
			if (player.isGM() && (player.getParty() == null)) // For single GM testing. Will lock boss for all other players.
			{
				GrandBossManager.getInstance().setStatus(FRINTEZZA, WAITING);
				startQuestTimer("close", 0, npc, null);
				startQuestTimer("room1_spawn", 5000, npc, null);
				startQuestTimer("room_final", 2100000, npc, null);
				startQuestTimer("frintezza_despawn", 60000, npc, null, true);
				_lastAction = System.currentTimeMillis();
				_playersInside.add(player);
				_zone.allowPlayerEntry(player, 300);
				player.teleToLocation(_invadeLoc[_locCycle][0] + getRandom(50), _invadeLoc[_locCycle][1] + getRandom(50), _invadeLoc[_locCycle][2]);
			}
			else
			{
				if ((!player.isInParty() || !player.getParty().isLeader(player)) || (player.getParty().getCommandChannel() == null) || (player.getParty().getCommandChannel().getLeader() != player))
				{
					htmltext = "<html><body>No reaction. Contact must be initiated by the Command Channel Leader.</body></html>";
				}
				else if ((player.getParty().getCommandChannel().getParties().size() < 4) || (player.getParty().getCommandChannel().getParties().size() > 5))
				{
					htmltext = "<html><body>Your command channel needs to have at least 4 parties and a maximum of 5.</body></html>";
				}
				else if (player.getInventory().getItemByItemId(8073) == null)
				{
					htmltext = "<html><head><body>The Magic Force Field is currently inpenetrable. Something else is necessary.<br>(Frintezza's Magic Force Field Removal Scroll may be obtained by completing the \"Jouney to a Settlement\" quest.)</body></html>";
				}
				else
				{
					player.destroyItemByItemId(ItemProcessType.QUEST, 8073, 1, player, true);
					final CommandChannel cc = player.getParty().getCommandChannel();
					GrandBossManager.getInstance().setStatus(FRINTEZZA, WAITING);
					startQuestTimer("close", 0, npc, null);
					startQuestTimer("room1_spawn", 5000, npc, null);
					startQuestTimer("room_final", 2100000, npc, null);
					startQuestTimer("frintezza_despawn", 60000, npc, null, true);
					_lastAction = System.currentTimeMillis();
					for (Party party : cc.getParties())
					{
						if (party == null)
						{
							continue;
						}
						
						for (Player member : party.getMembers())
						{
							if ((member == null) || (member.getLevel() < 74))
							{
								continue;
							}
							
							if (!member.isInsideRadius3D(npc, 700))
							{
								continue;
							}
							
							if (_playersInside.size() > 45)
							{
								member.sendMessage("The number of challenges have been full, so can not enter.");
								break;
							}
							
							_playersInside.add(member);
							_zone.allowPlayerEntry(member, 300);
							member.teleToLocation(_invadeLoc[_locCycle][0] + getRandom(50), _invadeLoc[_locCycle][1] + getRandom(50), _invadeLoc[_locCycle][2]);
						}
						
						if (_playersInside.size() > 45)
						{
							break;
						}
						
						_locCycle++;
						if (_locCycle >= 6)
						{
							_locCycle = 1;
						}
					}
				}
			}
		}
		else
		{
			htmltext = "<html><body>Someone else is already inside the Magic Force Field. Try again later.</body></html>";
		}
		
		return htmltext;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		_lastAction = System.currentTimeMillis();
		
		if (npc.getId() == FRINTEZZA)
		{
			npc.setCurrentHpMp(npc.getMaxHp(), 0);
		}
		else if ((npc.getId() == SCARLET1) && (_secondMorph == 0) && (_thirdMorph == 0) && (_onMorph == 0) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.75)) && (GrandBossManager.getInstance().getStatus(FRINTEZZA) == FIGHTING))
		{
			startQuestTimer("attack_stop", 0, _frintezza, null);
			_secondMorph = 1;
			_onMorph = 1;
			startQuestTimer("stop_pc", 1000, npc, null);
			startQuestTimer("stop_npc", 1000, npc, null);
			startQuestTimer("morph_01", 1100, npc, null);
		}
		else if ((npc.getId() == SCARLET1) && (_secondMorph == 1) && (_thirdMorph == 0) && (_onMorph == 0) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && (GrandBossManager.getInstance().getStatus(FRINTEZZA) == FIGHTING))
		{
			startQuestTimer("attack_stop", 0, _frintezza, null);
			_thirdMorph = 1;
			_onMorph = 1;
			startQuestTimer("stop_pc", 2000, npc, null);
			startQuestTimer("stop_npc", 2000, npc, null);
			startQuestTimer("morph_05a", 2000, npc, null);
			startQuestTimer("morph_05", 2100, npc, null);
		}
		else if ((npc.getId() == SCARLET2) && (_secondMorph == 1) && (_thirdMorph == 1) && (_onCheck == 0) && (damage >= npc.getCurrentHp()) && (GrandBossManager.getInstance().getStatus(FRINTEZZA) == FIGHTING))
		{
			_onCheck = 1;
			startQuestTimer("check_hp", 0, npc, null);
		}
		else if (((npc.getId() == 29050) || (npc.getId() == 29051)) && (_bomber == 0) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.1)) && (getRandom(100) < 30))
		{
			_bomber = 1;
			startQuestTimer("bomber", 3000, npc, null);
			
			final Skill skill = SkillData.getInstance().getSkill(5011, 1);
			if (skill != null)
			{
				npc.setCastingNow(true);
				npc.doCast(skill);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == FRINTEZZA)
		{
			return;
		}
		else if ((npc.getId() == SCARLET2) && (_onCheck == 0) && (GrandBossManager.getInstance().getStatus(FRINTEZZA) == FIGHTING))
		{
			_onCheck = 1;
			startQuestTimer("stop_pc", 0, npc, null);
			startQuestTimer("stop_npc", 0, npc, null);
			startQuestTimer("morph_16", 0, npc, null);
		}
		else if ((npc.getId() == SCARLET2) && (_onCheck == 1) && (GrandBossManager.getInstance().getStatus(FRINTEZZA) == FIGHTING))
		{
			cancelQuestTimers("loc_check");
			cancelQuestTimers("spawn_minion");
			cancelQuestTimers("frintezza_despawn");
			startQuestTimer("clean", 30000, npc, null);
			startQuestTimer("close", 30000, npc, null);
			startQuestTimer("room3_del", 60000, npc, null);
			startQuestTimer("minions_despawn", 60000, npc, null);
			startQuestTimer("remove_players", 900000, npc, null);
			GrandBossManager.getInstance().setStatus(FRINTEZZA, DEAD);
			
			final long baseIntervalMillis = GrandBossConfig.FRINTEZZA_SPAWN_INTERVAL * 3600000;
			final long randomRangeMillis = GrandBossConfig.FRINTEZZA_SPAWN_RANDOM * 3600000;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Frintezza will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer("frintezza_unlock", respawnTime, npc, null);
			
			// Also save the respawn time so that the info is maintained past reboots.
			final StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(FRINTEZZA, info);
		}
		else if (npc.getId() == 18328)
		{
			_killHallAlarmDevice++;
			if (_killHallAlarmDevice == 4)
			{
				startQuestTimer("room1_del", 100, npc, null);
				startQuestTimer("room2_spawn", 100, npc, null);
				DoorData.getInstance().getDoor(25150042).openMe();
				DoorData.getInstance().getDoor(25150043).openMe();
				// DoorData.getInstance().getDoor(25150045).openMe();
				// DoorData.getInstance().getDoor(25150046).openMe();
				for (int i = 25150051; i <= 25150058; i++)
				{
					DoorData.getInstance().getDoor(i).openMe();
				}
			}
		}
		else if (npc.getId() == 18339)
		{
			_killDarkChoirPlayer++;
			if (_killDarkChoirPlayer == 2)
			{
				DoorData.getInstance().getDoor(25150042).closeMe();
				DoorData.getInstance().getDoor(25150043).closeMe();
				DoorData.getInstance().getDoor(25150045).closeMe();
				DoorData.getInstance().getDoor(25150046).closeMe();
				int outside = 0;
				for (Player room2_pc : _playersInside)
				{
					if (_zone.isInsideZone(room2_pc) && (room2_pc.getY() > -86130))
					{
						outside++;
					}
				}
				
				if (outside == 0)
				{
					startQuestTimer("room2_del", 100, npc, null);
					cancelQuestTimers("room_final");
				}
				else
				{
					for (int i = 25150061; i <= 25150070; i++)
					{
						DoorData.getInstance().getDoor(i).openMe();
					}
					
					startQuestTimer("room2_spawn2", 1000, npc, null);
				}
			}
		}
		else if (npc.getId() == 18334)
		{
			_killDarkChoirCaptain++;
			if (_killDarkChoirCaptain == 8)
			{
				startQuestTimer("room2_del", 100, npc, null);
				DoorData.getInstance().getDoor(25150045).openMe();
				DoorData.getInstance().getDoor(25150046).openMe();
				cancelQuestTimers("room_final");
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Frintezza();
	}
}
