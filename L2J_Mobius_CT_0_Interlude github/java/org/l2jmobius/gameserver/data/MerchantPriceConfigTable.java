/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.siege.Castle;

/**
 * @author KenM
 */
public class MerchantPriceConfigTable
{
	// Zoey76: TODO: Implement using IXmlReader.
	private static final Logger LOGGER = Logger.getLogger(MerchantPriceConfigTable.class.getName());
	
	public static MerchantPriceConfigTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final String MPCS_FILE = "MerchantPriceConfig.xml";
	
	private final Map<Integer, MerchantPriceConfig> _mpcs = new HashMap<>();
	private MerchantPriceConfig _defaultMpc;
	
	public MerchantPriceConfig getMerchantPriceConfig(Merchant npc)
	{
		for (MerchantPriceConfig mpc : _mpcs.values())
		{
			if (ZoneManager.getInstance().getRegion(npc).getZones().containsKey(mpc.getZoneId()))
			{
				return mpc;
			}
		}
		
		return _defaultMpc;
	}
	
	public MerchantPriceConfig getMerchantPriceConfig(int id)
	{
		return _mpcs.get(id);
	}
	
	public void loadXML() throws SAXException, IOException, ParserConfigurationException
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(ServerConfig.DATAPACK_ROOT + "/data/" + MPCS_FILE);
		if (file.exists())
		{
			int defaultPriceConfigId;
			final Document document = factory.newDocumentBuilder().parse(file);
			
			Node n = document.getDocumentElement();
			final Node dpcNode = n.getAttributes().getNamedItem("defaultPriceConfig");
			if (dpcNode == null)
			{
				throw new IllegalStateException("merchantPriceConfig must define an 'defaultPriceConfig'");
			}
			
			defaultPriceConfigId = Integer.parseInt(dpcNode.getNodeValue());
			
			MerchantPriceConfig mpc;
			for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			{
				mpc = parseMerchantPriceConfig(n);
				if (mpc != null)
				{
					_mpcs.put(mpc.getId(), mpc);
				}
			}
			
			final MerchantPriceConfig defaultMpc = getMerchantPriceConfig(defaultPriceConfigId);
			if (defaultMpc == null)
			{
				throw new IllegalStateException("'defaultPriceConfig' points to an non-loaded priceConfig");
			}
			
			_defaultMpc = defaultMpc;
		}
	}
	
	private MerchantPriceConfig parseMerchantPriceConfig(Node n)
	{
		if (n.getNodeName().equals("priceConfig"))
		{
			final int id;
			final int baseTax;
			int castleId = -1;
			int zoneId = -1;
			final String name;
			
			Node node = n.getAttributes().getNamedItem("id");
			if (node == null)
			{
				throw new IllegalStateException("Must define the priceConfig 'id'");
			}
			
			id = Integer.parseInt(node.getNodeValue());
			
			node = n.getAttributes().getNamedItem("name");
			if (node == null)
			{
				throw new IllegalStateException("Must define the priceConfig 'name'");
			}
			
			name = node.getNodeValue();
			
			node = n.getAttributes().getNamedItem("baseTax");
			if (node == null)
			{
				throw new IllegalStateException("Must define the priceConfig 'baseTax'");
			}
			
			baseTax = Integer.parseInt(node.getNodeValue());
			
			node = n.getAttributes().getNamedItem("castleId");
			if (node != null)
			{
				castleId = Integer.parseInt(node.getNodeValue());
			}
			
			node = n.getAttributes().getNamedItem("zoneId");
			if (node != null)
			{
				zoneId = Integer.parseInt(node.getNodeValue());
			}
			
			return new MerchantPriceConfig(id, name, baseTax, castleId, zoneId);
		}
		
		return null;
	}
	
	public void loadInstances()
	{
		try
		{
			loadXML();
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _mpcs.size() + " merchant price configs.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed loading MerchantPriceConfigTable. Reason: " + e.getMessage(), e);
		}
	}
	
	public void updateReferences()
	{
		for (MerchantPriceConfig mpc : _mpcs.values())
		{
			mpc.updateReferences();
		}
	}
	
	/**
	 * @author KenM
	 */
	public static class MerchantPriceConfig
	{
		private final int _id;
		private final String _name;
		private final int _baseTax;
		private final int _castleId;
		private Castle _castle;
		private final int _zoneId;
		
		public MerchantPriceConfig(int id, String name, int baseTax, int castleId, int zoneId)
		{
			_id = id;
			_name = name;
			_baseTax = baseTax;
			_castleId = castleId;
			_zoneId = zoneId;
		}
		
		/**
		 * @return Returns the id.
		 */
		public int getId()
		{
			return _id;
		}
		
		/**
		 * @return Returns the name.
		 */
		public String getName()
		{
			return _name;
		}
		
		/**
		 * @return Returns the baseTax.
		 */
		public int getBaseTax()
		{
			return _baseTax;
		}
		
		/**
		 * @return Returns the baseTax / 100.0.
		 */
		public double getBaseTaxRate()
		{
			return _baseTax / 100.0;
		}
		
		/**
		 * @return Returns the castle.
		 */
		public Castle getCastle()
		{
			return _castle;
		}
		
		/**
		 * @return Returns the zoneId.
		 */
		public int getZoneId()
		{
			return _zoneId;
		}
		
		public boolean hasCastle()
		{
			return _castle != null;
		}
		
		public double getCastleTaxRate()
		{
			return hasCastle() ? _castle.getTaxRate() : 0.0;
		}
		
		public int getTotalTax()
		{
			return hasCastle() ? (_castle.getTaxPercent() + _baseTax) : _baseTax;
		}
		
		public double getTotalTaxRate()
		{
			return getTotalTax() / 100.0;
		}
		
		public void updateReferences()
		{
			if (_castleId > 0)
			{
				_castle = CastleManager.getInstance().getCastleById(_castleId);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final MerchantPriceConfigTable INSTANCE = new MerchantPriceConfigTable();
	}
}
