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
package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.util.Locator;

/**
 * @author Mobius
 */
public class SystemPanel extends JPanel
{
	protected static final Logger LOGGER = Logger.getLogger(SystemPanel.class.getName());
	
	protected static final long START_TIME = System.currentTimeMillis();
	
	public SystemPanel()
	{
		if (!InterfaceConfig.DARK_THEME)
		{
			setBackground(Color.WHITE);
		}
		
		setBounds(500, 20, 284, 140);
		setBorder(new LineBorder(new Color(0, 0, 0), 1, false));
		setOpaque(true);
		setLayout(null);
		
		final JLabel lblProtocol = new JLabel("Protocol");
		lblProtocol.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblProtocol.setBounds(10, 5, 264, 17);
		add(lblProtocol);
		
		final JLabel lblConnected = new JLabel("Connected");
		lblConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblConnected.setBounds(10, 23, 264, 17);
		add(lblConnected);
		
		final JLabel lblMaxConnected = new JLabel("Max connected");
		lblMaxConnected.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblMaxConnected.setBounds(10, 41, 264, 17);
		add(lblMaxConnected);
		
		final JLabel lblOfflineShops = new JLabel("Offline trade");
		lblOfflineShops.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblOfflineShops.setBounds(10, 59, 264, 17);
		add(lblOfflineShops);
		
		final JLabel lblElapsedTime = new JLabel("Elapsed time");
		lblElapsedTime.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblElapsedTime.setBounds(10, 77, 264, 17);
		add(lblElapsedTime);
		
		final JLabel lblJavaVersion = new JLabel("Build JDK");
		lblJavaVersion.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblJavaVersion.setBounds(10, 95, 264, 17);
		add(lblJavaVersion);
		
		final JLabel lblBuildDate = new JLabel("Build date");
		lblBuildDate.setFont(new Font("Monospaced", Font.PLAIN, 16));
		lblBuildDate.setBounds(10, 113, 264, 17);
		add(lblBuildDate);
		
		// Set initial values.
		lblProtocol.setText("Protocol: 0");
		lblConnected.setText("Connected: 0");
		lblMaxConnected.setText("Max connected: 0");
		lblOfflineShops.setText("Offline trade: 0");
		lblElapsedTime.setText("Elapsed: 0 sec");
		lblJavaVersion.setText("Java version: " + System.getProperty("java.version"));
		lblBuildDate.setText("Build date: Unavailable");
		try
		{
			final File jarName = Locator.getClassSource(GameServer.class);
			final JarFile jarFile = new JarFile(jarName);
			final Attributes attrs = jarFile.getManifest().getMainAttributes();
			lblBuildDate.setText("Build date: " + attrs.getValue("Build-Date").split(" ")[0]);
			jarFile.close();
		}
		catch (Exception e)
		{
			// Handled above.
		}
		
		// Initial update task.
		new Timer().schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				lblProtocol.setText((ServerConfig.PROTOCOL_LIST.size() > 1 ? "Protocols: " : "Protocol: ") + ServerConfig.PROTOCOL_LIST.toString());
			}
		}, 4500);
		
		// Repeating elapsed time task.
		new Timer().scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				final int playerCount = World.getInstance().getPlayers().size();
				if (World.MAX_CONNECTED_COUNT < playerCount)
				{
					World.MAX_CONNECTED_COUNT = playerCount;
					if (playerCount > 1)
					{
						LOGGER.info("New maximum connected count of " + playerCount + "!");
					}
				}
				
				lblConnected.setText("Connected: " + playerCount);
				lblMaxConnected.setText("Max connected: " + World.MAX_CONNECTED_COUNT);
				lblOfflineShops.setText("Offline trade: " + World.OFFLINE_TRADE_COUNT);
				lblElapsedTime.setText("Elapsed: " + getDurationBreakdown(System.currentTimeMillis() - START_TIME));
			}
		}, 1000, 1000);
	}
	
	static String getDurationBreakdown(long millis)
	{
		long remaining = millis;
		final long days = TimeUnit.MILLISECONDS.toDays(remaining);
		remaining -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(remaining);
		remaining -= TimeUnit.HOURS.toMillis(hours);
		final long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
		remaining -= TimeUnit.MINUTES.toMillis(minutes);
		final long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
		return (days + "d " + hours + "h " + minutes + "m " + seconds + "s");
	}
}
