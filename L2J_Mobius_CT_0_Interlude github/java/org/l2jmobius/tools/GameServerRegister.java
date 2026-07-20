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
package org.l2jmobius.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.util.HexUtil;
import org.l2jmobius.loginserver.GameServerTable;

/**
 * @author Skache
 */
public class GameServerRegister extends JFrame
{
	private final Scanner _scanner = new Scanner(System.in);
	
	private JPanel _buttonPanel;
	private JButton _btnRegister;
	private JButton _btnList;
	private JButton _btnRemove;
	private JButton _btnRemoveAll;
	private JTable _serverTable;
	private DefaultTableModel _serverTableModel;
	
	private GameServerRegister()
	{
		// GUI
		if (InterfaceConfig.ENABLE_GUI)
		{
			// Disable hardware acceleration.
			System.setProperty("sun.java2d.opengl", "false");
			System.setProperty("sun.java2d.d3d", "false");
			System.setProperty("sun.java2d.noddraw", "true");
			
			if (InterfaceConfig.DARK_THEME)
			{
				DarkTheme.activate();
			}
			
			gui();
		}
		else
		{
			console();
		}
	}
	
	private void gui()
	{
		// Show Splash Screen for 3 seconds.
		new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 3000, this);
		
		setTitle("Mobius - Game Server Register");
		setMinimumSize(new Dimension(500, 300));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		
		// GUI
		if (InterfaceConfig.ENABLE_GUI && InterfaceConfig.DARK_THEME)
		{
			DarkTheme.activate();
		}
		
		// Window Listener to properly close resources when the window is closed.
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				DatabaseFactory.close();
				System.exit(0);
			}
		});
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		
		setLayout(new BorderLayout());
		
		// Button Panel at the top.
		_buttonPanel = new JPanel(new GridLayout(1, 5, 5, 5));
		_btnList = new JButton("Servers List");
		_btnRegister = new JButton("Register Server");
		_btnRemove = new JButton("Remove Server");
		_btnRemoveAll = new JButton("Remove All");
		
		_buttonPanel.add(_btnList);
		_buttonPanel.add(_btnRegister);
		_buttonPanel.add(_btnRemove);
		_buttonPanel.add(_btnRemoveAll);
		
		_btnList.addActionListener(_ -> serversList());
		_btnRegister.addActionListener(_ -> registerServer());
		_btnRemove.addActionListener(_ -> unregisterServer());
		_btnRemoveAll.addActionListener(_ -> unregisterAllServers());
		
		// Create the JTable for displaying server names.
		final String[] columnNames =
		{
			"Server ID",
			"Server Name",
			"Status"
		};
		_serverTableModel = new DefaultTableModel(columnNames, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false; // Make the entire table non-editable.
			}
		};
		_serverTable = new JTable(_serverTableModel);
		_serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// Set custom renderer for the status column to change colors.
		_serverTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
		
		// Server Panel to hold the table.
		final JPanel serverPanel = new JPanel(new BorderLayout());
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Servers");
		serverPanel.setBorder(titledBorder);
		
		// Add serverTable inside a JScrollPane to make it scrollable and visible.
		final JScrollPane tableScrollPane = new JScrollPane(_serverTable);
		serverPanel.add(tableScrollPane, BorderLayout.CENTER);
		
		// Layout Panels.
		final JPanel mainPanel = new JPanel(new BorderLayout()); // Main panel to hold everything.
		mainPanel.add(_buttonPanel, BorderLayout.NORTH); // Buttons at the top.
		mainPanel.add(serverPanel, BorderLayout.CENTER); // Server table in the center.
		
		_serverTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (event.getButton() == MouseEvent.BUTTON3) // Right-click.
				{
					final int row = _serverTable.rowAtPoint(event.getPoint());
					_serverTable.setRowSelectionInterval(row, row); // Select the clicked row.
					
					// Create the popup menu.
					final JPopupMenu popupMenu = new JPopupMenu();
					
					// Create "Register" menu item.
					final JMenuItem registerItem = new JMenuItem("Register Server");
					registerItem.addActionListener(_ ->
					{
						final int serverId = (Integer) _serverTable.getValueAt(row, 0);
						final String serverName = (String) _serverTable.getValueAt(row, 1);
						
						// Check if the server is already registered.
						if (GameServerTable.getInstance().hasRegisteredGameServerOnId(serverId))
						{
							JOptionPane.showMessageDialog(GameServerRegister.this, "Server " + serverName + " with ID " + serverId + " is already registered.", "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						// Show confirmation dialog before registering.
						final int confirm = JOptionPane.showConfirmDialog(GameServerRegister.this, "Do you want to register " + serverName + "?", "Confirm Registration", JOptionPane.YES_NO_OPTION);
						if (confirm == JOptionPane.YES_OPTION)
						{
							try
							{
								createAndRegister(serverId, ".");
								JOptionPane.showMessageDialog(GameServerRegister.this, "Server " + serverName + " registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
								
								// Refresh server list after registration.
								serversList();
							}
							catch (IOException ex)
							{
								JOptionPane.showMessageDialog(GameServerRegister.this, "An error occurred while registering " + serverName + ".", "Error", JOptionPane.ERROR_MESSAGE);
							}
						}
					});
					
					// Create "Unregister" menu item.
					final JMenuItem unregisterItem = new JMenuItem("Unregister Server");
					unregisterItem.addActionListener(_ ->
					{
						final int serverId = (Integer) _serverTable.getValueAt(row, 0);
						final String serverName = (String) _serverTable.getValueAt(row, 1);
						
						// Check if the server is registered before attempting to unregister.
						if (!GameServerTable.getInstance().hasRegisteredGameServerOnId(serverId))
						{
							JOptionPane.showMessageDialog(GameServerRegister.this, "Server " + serverName + " with ID " + serverId + " is not registered.", "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						// Confirm if the user wants to unregister the selected server.
						final int confirm = JOptionPane.showConfirmDialog(GameServerRegister.this, "Are you sure you want to remove GameServer " + serverId + " - " + serverName + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
						if (confirm == JOptionPane.YES_OPTION)
						{
							try
							{
								// Unregister the game server.
								removeServer(serverId);
								JOptionPane.showMessageDialog(GameServerRegister.this, "Game Server ID: " + serverId + " has been successfully removed.", "Success", JOptionPane.INFORMATION_MESSAGE);
								
								// Refresh server list after registration.
								serversList();
							}
							catch (SQLException e1)
							{
								JOptionPane.showMessageDialog(GameServerRegister.this, "An error occurred while trying to unregister the Game Server.", "Error", JOptionPane.ERROR_MESSAGE);
							}
						}
					});
					
					popupMenu.add(registerItem);
					popupMenu.add(unregisterItem);
					popupMenu.show(event.getComponent(), event.getX(), event.getY()); // Show the menu at mouse position.
				}
			}
		});
		
		// Adding the main panel to the frame.
		add(mainPanel, BorderLayout.CENTER);
		
		setIconImages(icons);
	}
	
	private void serversList()
	{
		SwingUtilities.invokeLater(() ->
		{
			final Map<Integer, String> serverNames = GameServerTable.getInstance().getServerNames();
			if (serverNames.isEmpty())
			{
				JOptionPane.showMessageDialog(null, "No game servers found.", "Information", JOptionPane.INFORMATION_MESSAGE);
			}
			else
			{
				final String gsInUse = "In Use";
				final String gsFree = "Free";
				
				// Clear the table before adding new data.
				_serverTableModel.setRowCount(0);
				
				for (Entry<Integer, String> entry : serverNames.entrySet())
				{
					final int id = entry.getKey();
					final String serverName = entry.getValue();
					final boolean inUse = GameServerTable.getInstance().hasRegisteredGameServerOnId(id);
					final String status = inUse ? gsInUse : gsFree;
					
					// Add data to the table.
					_serverTableModel.addRow(new Object[]
					{
						id,
						serverName,
						status
					});
				}
			}
		});
	}
	
	private void registerServer()
	{
		final JTextField idField = new JTextField(10);
		final Object[] message =
		{
			"Enter Game Server ID:",
			idField
		};
		
		final int option = JOptionPane.showConfirmDialog(null, message, "Register Game Server", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION)
		{
			final String input = idField.getText().trim();
			
			// Check if the input is empty.
			if (input.isEmpty())
			{
				JOptionPane.showMessageDialog(null, "Game Server ID cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try
			{
				// Ensure the ID is a valid number.
				final int id = Integer.parseInt(input);
				
				// Validate the ID range (positive IDs are valid).
				if (id <= 0)
				{
					JOptionPane.showMessageDialog(null, "Game Server ID must be a positive number.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// Check if a server with the given ID exists.
				final String serverName = GameServerTable.getInstance().getServerNameById(id);
				
				// If no server name is found.
				if (serverName == null)
				{
					JOptionPane.showMessageDialog(null, "No server found for ID: " + id, "Error", JOptionPane.ERROR_MESSAGE);
				}
				
				// If the server ID is already registered.
				else if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
				{
					JOptionPane.showMessageDialog(null, "Server '" + serverName + "' with ID " + id + " is already registered.", "Error", JOptionPane.ERROR_MESSAGE);
				}
				else
				{
					createAndRegister(id, ".");
					JOptionPane.showMessageDialog(null, "Game server with ID: " + id + " (" + serverName + ") has been successfully registered.", "Success", JOptionPane.INFORMATION_MESSAGE);
					idField.setText(""); // Clear the input field after successful registration.
					
					// Refresh the server list after registration.
					serversList();
				}
			}
			catch (NumberFormatException e)
			{
				JOptionPane.showMessageDialog(null, "Invalid Game Server ID entered. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null, "An error occurred while trying to register the Game Server.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void unregisterServer()
	{
		final JTextField idField = new JTextField(10);
		final Object[] message =
		{
			"Enter Game Server ID to unregister:",
			idField
		};
		
		final int option = JOptionPane.showConfirmDialog(null, message, "Unregister Game Server", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION)
		{
			final String input = idField.getText().trim();
			
			if (input.isEmpty())
			{
				JOptionPane.showMessageDialog(null, "Game Server ID cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try
			{
				final int id = Integer.parseInt(input);
				final String serverName = GameServerTable.getInstance().getServerNameById(id);
				if (serverName == null)
				{
					JOptionPane.showMessageDialog(null, "No Game Server found for ID: " + id, "Error", JOptionPane.ERROR_MESSAGE);
				}
				else if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
				{
					final int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to remove GameServer " + id + "  (" + serverName + ")?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
					if (confirm == JOptionPane.YES_OPTION)
					{
						removeServer(id);
						JOptionPane.showMessageDialog(null, "Game Server ID: " + id + " (" + serverName + ") has been successfully removed.", "Success", JOptionPane.INFORMATION_MESSAGE);
						serversList(); // Refresh the server list
					}
				}
				else
				{
					JOptionPane.showMessageDialog(null, "No GameServer is registered with ID: " + id, "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			catch (NumberFormatException e)
			{
				JOptionPane.showMessageDialog(null, "Invalid Game Server ID entered. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
			}
			catch (SQLException e)
			{
				JOptionPane.showMessageDialog(null, "An error occurred while trying to unregister the Game Server.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void unregisterAllServers()
	{
		// Check if there are any registered game servers.
		if (GameServerTable.getInstance().getRegisteredGameServers().isEmpty())
		{
			JOptionPane.showMessageDialog(null, "No game servers are currently registered.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		// Ask the user for confirmation before proceeding.
		final int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to unregister all game servers?", "Confirm Unregister All Game Servers", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirm == JOptionPane.YES_OPTION)
		{
			try
			{
				removeAllServers();
				JOptionPane.showMessageDialog(null, "All game servers have been unregistered successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
				serversList(); // Refresh the server list.
			}
			catch (SQLException e)
			{
				JOptionPane.showMessageDialog(null, "Error while unregistering game servers: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Unregister operation canceled by the user.", "Canceled", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	private static void createAndRegister(int id, String outDir) throws IOException
	{
		final byte[] hexId = HexUtil.generateHexBytes(16); // Generate Hex ID.
		GameServerTable.getInstance().registerServerOnDB(hexId, id, ""); // Register the server in the database.
		
		final Properties hexSetting = new Properties();
		final File file = new File(outDir, "hexid.txt"); // Create the hexid.txt file.
		
		// Create a new empty file only if it doesn't exist.
		file.createNewFile();
		try (OutputStream out = new FileOutputStream(file))
		{
			hexSetting.setProperty("ServerID", String.valueOf(id));
			hexSetting.setProperty("HexID", new BigInteger(hexId).toString(16)); // Store the Hex ID as a string in the properties file
			hexSetting.store(out, "The HexId to Auth into LoginServer");
		}
	}
	
	private static void removeServer(int id) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM gameservers WHERE server_id = ?"))
		{
			statement.setInt(1, id);
			statement.executeUpdate();
		}
		
		GameServerTable.getInstance().getRegisteredGameServers().remove(id); // Remove it from the GameServerTable.
	}
	
	private static void removeAllServers() throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement())
		{
			statement.executeUpdate("DELETE FROM gameservers");
			
			GameServerTable.getInstance().getRegisteredGameServers().clear();
		}
	}
	
	private void console()
	{
		System.out.println("=========================================================");
		System.out.println("       L2jMobius Development - Game Server Register       ");
		System.out.println("               Created by Skache                         ");
		System.out.println("=========================================================");
		
		showMenu();
		try (Scanner scanner = new Scanner(System.in))
		{
			while (true)
			{
				System.out.print(System.lineSeparator() + "Enter your choice: ");
				
				if (!scanner.hasNextInt())
				{
					System.out.println(System.lineSeparator() + "[ERROR] Invalid input. Please enter a number.");
					scanner.nextLine(); // Clear invalid input.
					continue;
				}
				
				final int choice = scanner.nextInt();
				scanner.nextLine();
				
				switch (choice)
				{
					case 1:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: List Servers");
						listServersConsole();
						break;
					}
					case 2:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Register Server");
						registerServerConsole(scanner);
						break;
					}
					case 3:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Remove Server");
						removeServerConsole(scanner);
						break;
					}
					case 4:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Remove All Servers");
						removeAllServersConsole();
						break;
					}
					case 5:
					{
						System.out.println(System.lineSeparator() + "[EXIT] Exiting the application. Goodbye!");
						try
						{
							Thread.sleep(2000);
						}
						catch (InterruptedException e)
						{
							
						}
						
						System.exit(0);
						break;
					}
					default:
					{
						System.out.println(System.lineSeparator() + "[WARNING] Invalid choice. Please select a valid option.");
						break;
					}
				}
				
				showMenu();
			}
		}
		finally
		{
			// Cleanup resources when exiting console mode.
			System.out.println("[INFO] Cleaning up resources...");
			DatabaseFactory.close();
		}
	}
	
	private void showMenu()
	{
		System.out.println(System.lineSeparator() + "=========================================================");
		System.out.println("                  AVAILABLE COMMANDS                     ");
		System.out.println("---------------------------------------------------------");
		System.out.println("  [1] List Servers");
		System.out.println("  [2] Register Server");
		System.out.println("  [3] Remove Server");
		System.out.println("  [4] Remove All Servers");
		System.out.println("  [5] Exit");
		System.out.println("---------------------------------------------------------");
	}
	
	private void listServersConsole()
	{
		final Map<Integer, String> serverNames = GameServerTable.getInstance().getServerNames();
		if (serverNames.isEmpty())
		{
			System.out.println("No game servers found.");
			return;
		}
		
		System.out.println(System.lineSeparator() + "List Servers:");
		System.out.println("+----------------+------------------------+-----------+");
		System.out.printf("| %-14s | %-22s | %-9s |" + System.lineSeparator(), "Server ID", "Server Name", "Status");
		System.out.println("+----------------+------------------------+-----------+");
		
		for (Entry<Integer, String> entry : serverNames.entrySet())
		{
			final int id = entry.getKey();
			final String serverName = entry.getValue();
			final boolean inUse = GameServerTable.getInstance().hasRegisteredGameServerOnId(id);
			final String status = inUse ? "In Use" : "Free";
			
			System.out.printf("| %-14d | %-22s | %-9s |" + System.lineSeparator(), id, serverName, status);
		}
		
		System.out.println("+----------------+------------------------+-----------+");
	}
	
	private void registerServerConsole(Scanner scanner)
	{
		System.out.print("Enter Game Server ID to register: ");
		final String input = scanner.nextLine().trim();
		
		// Check if the input is empty.
		if (input.isEmpty())
		{
			System.out.println("Game Server ID cannot be empty.");
			return;
		}
		
		try
		{
			// Ensure the ID is a valid number.
			final int id = Integer.parseInt(input);
			
			// Validate the ID range (positive IDs are valid).
			if (id <= 0)
			{
				System.out.println("Game Server ID must be a positive number.");
				return;
			}
			
			// Check if a server with the given ID exists.
			final String serverName = GameServerTable.getInstance().getServerNameById(id);
			
			// If no server name is found.
			if (serverName == null)
			{
				System.out.println("No server found for ID: " + id);
				return;
			}
			
			// If the server ID is already registered.
			if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
			{
				System.out.println("Server '" + serverName + "' with ID " + id + " is already registered.");
				return;
			}
			
			createAndRegister(id, ".");
			System.out.println("Game server with ID: " + id + " (" + serverName + ") has been successfully registered.");
			
			serversList(); // Refresh the server list.
		}
		catch (NumberFormatException e)
		{
			System.out.println("Invalid Game Server ID entered. Please enter a valid number.");
		}
		catch (IOException e)
		{
			System.out.println("An error occurred while trying to register the Game Server: " + e.getMessage());
		}
	}
	
	private void removeServerConsole(Scanner scanner)
	{
		System.out.print("Enter Server ID to remove: ");
		final String input = scanner.nextLine().trim();
		
		// Check if the input is empty or invalid.
		if (input.isEmpty())
		{
			System.out.println("Server ID cannot be empty.");
			return;
		}
		
		try
		{
			final int serverId = Integer.parseInt(input);
			if (!GameServerTable.getInstance().hasRegisteredGameServerOnId(serverId))
			{
				System.out.println("No Game Server is registered with ID " + serverId + ".");
				return;
			}
			
			final String serverName = GameServerTable.getInstance().getServerNameById(serverId);
			
			// Confirm the removal.
			System.out.print("Are you sure you want to remove Game Server " + serverId + " (" + serverName + ")? (y/n): ");
			final String response = scanner.nextLine().trim().toLowerCase();
			if (!response.equals("y"))
			{
				System.out.println("Operation canceled.");
				return;
			}
			
			removeServer(serverId);
			System.out.println("Game Server ID " + serverId + " (" + serverName + ") has been successfully removed.");
		}
		catch (NumberFormatException e)
		{
			System.out.println("Invalid Server ID. Please enter a valid number.");
		}
		catch (SQLException e)
		{
			System.out.println("Error removing server from the database: " + e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("An unexpected error occurred: " + e.getMessage());
		}
	}
	
	private void removeAllServersConsole()
	{
		// Ask for confirmation.
		System.out.print("Are you sure you want to remove all servers? (y/n): ");
		final String response = _scanner.nextLine().trim().toLowerCase();
		
		// Early return if the user doesn't confirm.
		if (!response.equals("y"))
		{
			System.out.println("Operation canceled.");
			return;
		}
		
		try
		{
			removeAllServers();
			System.out.println("All servers have been successfully removed.");
		}
		catch (SQLException e)
		{
			System.out.println("Error removing all servers: " + e.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("An unexpected error occurred: " + e.getMessage());
		}
	}
	
	private class StatusCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			final Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			cellComponent.setForeground(Color.WHITE);
			
			if ((value != null) && value.equals("Free"))
			{
				cellComponent.setForeground(Color.GREEN); // Green text for "Free"
			}
			else if ((value != null) && value.equals("In Use"))
			{
				cellComponent.setForeground(Color.RED); // Red text for "In Use"
			}
			
			// If the row is selected, ensure the text remains visible and the background changes (default selection behavior).
			if (isSelected)
			{
				cellComponent.setBackground(table.getSelectionBackground());
				cellComponent.setForeground(table.getSelectionForeground());
			}
			else
			{
				cellComponent.setBackground(table.getBackground()); // Default background.
			}
			
			return cellComponent;
		}
	}
	
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() ->
		{
			InterfaceConfig.load();
			DatabaseFactory.init(); // Initialize the database connection.
			GameServerTable.getInstance(); // Initialize the GameServerTable.
			new GameServerRegister();
		});
	}
}
