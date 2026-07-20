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
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.util.ConfigReader;

/**
 * @author Skache
 */
public class DatabaseInstaller extends JFrame
{
	public static final String INTERFACE_CONFIG_FILE = "./config/Interface.ini";
	
	private JTextField _hostField;
	private JTextField _portField;
	private JTextField _userField;
	private JTextField _dbNameField;
	private JPasswordField _passField;
	private JTextPane _outputArea;
	private JButton _installButton;
	private JButton _testConnectionButton;
	private JCheckBox _loginDbCheckBox;
	private JCheckBox _gameDbCheckBox;
	private JProgressBar _progressBar;
	
	private DatabaseInstaller()
	{
		// GUI
		final ConfigReader interfaceConfig = new ConfigReader(INTERFACE_CONFIG_FILE);
		if (interfaceConfig.getBoolean("EnableGUI", false) && !GraphicsEnvironment.isHeadless())
		{
			// Disable hardware acceleration.
			System.setProperty("sun.java2d.opengl", "false");
			System.setProperty("sun.java2d.d3d", "false");
			System.setProperty("sun.java2d.noddraw", "true");
			
			if (interfaceConfig.getBoolean("DarkTheme", true))
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
		
		setTitle("Mobius - Database Installer");
		setMinimumSize(new Dimension(620, 400));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		
		// GUI
		final ConfigReader interfaceConfig = new ConfigReader(INTERFACE_CONFIG_FILE);
		if (interfaceConfig.getBoolean("EnableGUI", true) && !GraphicsEnvironment.isHeadless() && interfaceConfig.getBoolean("DarkTheme", true))
		{
			DarkTheme.activate();
		}
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		setIconImages(icons);
		
		// SplitPane to divide left and right sections.
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		// Left Panel.
		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		splitPane.setLeftComponent(leftPanel);
		
		// Host field.
		leftPanel.add(new JLabel("Host:"));
		leftPanel.add(Box.createVerticalStrut(5));
		_hostField = new JTextField("localhost", 20);
		_hostField.setPreferredSize(new Dimension(200, 20));
		_hostField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_hostField);
		
		// Port field.
		leftPanel.add(new JLabel("Port:"));
		leftPanel.add(Box.createVerticalStrut(5));
		_portField = new JTextField("3306", 20);
		_portField.setPreferredSize(new Dimension(200, 20));
		_portField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_portField);
		
		// Username field.
		leftPanel.add(new JLabel("Username:"));
		leftPanel.add(Box.createVerticalStrut(5));
		_userField = new JTextField("root", 20);
		_userField.setPreferredSize(new Dimension(200, 20));
		_userField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_userField);
		
		// Password field.
		leftPanel.add(new JLabel("Password:"));
		leftPanel.add(Box.createVerticalStrut(5));
		_passField = new JPasswordField(20);
		_passField.setPreferredSize(new Dimension(200, 20));
		_passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_passField);
		
		// Database field.
		leftPanel.add(new JLabel("Database:"));
		leftPanel.add(Box.createVerticalStrut(5));
		_dbNameField = new JTextField("l2jmobiusinterlude", 20);
		_dbNameField.setPreferredSize(new Dimension(200, 20));
		_dbNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_dbNameField);
		
		// Checkboxes.
		_loginDbCheckBox = new JCheckBox("Install Login");
		leftPanel.add(Box.createVerticalStrut(5));
		_loginDbCheckBox.setSelected(true); // Default checked.
		_loginDbCheckBox.setPreferredSize(new Dimension(200, 20));
		_loginDbCheckBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_loginDbCheckBox);
		
		_gameDbCheckBox = new JCheckBox("Install Game");
		leftPanel.add(Box.createVerticalStrut(5));
		_gameDbCheckBox.setSelected(true); // Default checked.
		_gameDbCheckBox.setPreferredSize(new Dimension(200, 20));
		_gameDbCheckBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		leftPanel.add(_gameDbCheckBox);
		
		// Add listeners to both checkboxes to monitor their state.
		_loginDbCheckBox.addItemListener(_ -> checkCheckboxesAndUpdateButtonState());
		_gameDbCheckBox.addItemListener(_ -> checkCheckboxesAndUpdateButtonState());
		
		// Test Connection Button.
		_testConnectionButton = new JButton("Test Connection");
		_testConnectionButton.addActionListener(_ -> testDatabaseConnection());
		leftPanel.add(Box.createVerticalStrut(5));
		_testConnectionButton.setPreferredSize(new Dimension(200, 30));
		_testConnectionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		leftPanel.add(_testConnectionButton);
		
		// Install Button.
		_installButton = new JButton("Install Database");
		_installButton.addActionListener(_ -> installDatabase());
		leftPanel.add(Box.createVerticalStrut(5));
		_installButton.setPreferredSize(new Dimension(200, 30));
		_installButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		checkCheckboxesAndUpdateButtonState(); // Disable Install button initially if both checkboxes are unchecked.
		leftPanel.add(_installButton);
		
		// Right Panel (console/output area).
		final JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		splitPane.setRightComponent(rightPanel);
		
		// Output area for logs/messages.
		_outputArea = new JTextPane();
		_outputArea.setEditable(false);
		final JScrollPane scrollPane = new JScrollPane(_outputArea);
		rightPanel.add(scrollPane, BorderLayout.CENTER);
		
		// Initialize and set up the progress bar.
		_progressBar = new JProgressBar(0, 100);
		_progressBar.setStringPainted(true);
		_progressBar.setPreferredSize(new Dimension(200, 20));
		_progressBar.setVisible(true);
		rightPanel.add(_progressBar, BorderLayout.SOUTH);
		
		// Set the SplitPane divider location.
		splitPane.setDividerLocation(130);
		add(splitPane);
	}
	
	private void console()
	{
		System.out.println("=========================================================");
		System.out.println("       L2jMobius Development - Database Installer        ");
		System.out.println("               Created by Skache                        ");
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
					scanner.nextLine();
					continue;
				}
				
				final int choice = scanner.nextInt();
				scanner.nextLine();
				
				switch (choice)
				{
					case 1:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Test Database Connection");
						testDatabaseConnectionConsole(scanner);
						break;
					}
					case 2:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Install Database");
						installDatabaseMenu(scanner);
						break;
					}
					case 3:
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
			System.out.println("[INFO] Cleaning up resources...");
			DatabaseFactory.close();
		}
	}
	
	private void showMenu()
	{
		System.out.println(System.lineSeparator() + "=========================================================");
		System.out.println("                  AVAILABLE COMMANDS                     ");
		System.out.println("---------------------------------------------------------");
		System.out.println("  [1] Test Database Connection");
		System.out.println("  [2] Install Database");
		System.out.println("  [3] Exit");
		System.out.println("---------------------------------------------------------");
	}
	
	private void installDatabaseMenu(Scanner scanner)
	{
		// Displaying the installation menu.
		System.out.println("=========================================================");
		System.out.println("             L2jMobius Database Installation             ");
		System.out.println("=========================================================");
		System.out.println("[1] Install Both Login and Game Databases");
		System.out.println("[2] Install Login Database");
		System.out.println("[3] Install Game Database");
		System.out.println("[4] Exit");
		
		// Prompt user for selection.
		System.out.print("\nEnter your choice: ");
		final int choice = scanner.nextInt();
		scanner.nextLine();
		
		boolean installationSuccessful = false;
		String host;
		String port;
		String username;
		String password;
		String dbName;
		
		System.out.print("Enter MySQL Host [localhost]: ");
		host = scanner.nextLine().trim();
		if (host.isEmpty())
		{
			host = "localhost";
		}
		
		System.out.print("Enter MySQL Port [3306]: ");
		port = scanner.nextLine().trim();
		if (port.isEmpty())
		{
			port = "3306";
		}
		
		System.out.print("Enter MySQL Username [root]: ");
		username = scanner.nextLine().trim();
		if (username.isEmpty())
		{
			username = "root";
		}
		
		System.out.print("Enter MySQL Password: ");
		password = scanner.nextLine().trim();
		
		System.out.print("Enter Database Name [l2jmobiusinterlude]: ");
		dbName = scanner.nextLine().trim();
		if (dbName.isEmpty())
		{
			dbName = "l2jmobiusinterlude";
		}
		
		switch (choice)
		{
			case 1:
			{
				// Option 1: Install both Login and Game tables.
				System.out.println("[INFO] You selected: Install Both Login and Game Databases");
				installationSuccessful = installDatabaseConsole("login", host, port, username, password, dbName, scanner, true);
				if (installationSuccessful)
				{
					installationSuccessful = installDatabaseConsole("game", host, port, username, password, dbName, scanner, false);
				}
				break;
			}
			case 2:
			{
				// Option 2: Install only Login tables.
				System.out.println("[INFO] You selected: Install Login Database");
				installationSuccessful = installDatabaseConsole("login", host, port, username, password, dbName, scanner, true);
				break;
			}
			case 3:
			{
				// Option 3: Install only Game tables.
				System.out.println("[INFO] You selected: Install Game Database");
				installationSuccessful = installDatabaseConsole("game", host, port, username, password, dbName, scanner, false);
				break;
			}
			case 4:
			{
				// Exit option.
				System.out.println("[INFO] Exiting installation...");
				return;
			}
			default:
			{
				System.out.println("[ERROR] Invalid choice. Please select a valid option.");
				break;
			}
		}
		
		if (installationSuccessful)
		{
			System.out.println("[INFO] Installation completed successfully.");
		}
		else
		{
			System.out.println("[ERROR] Installation failed. Please check the error logs.");
		}
	}
	
	private boolean installDatabaseConsole(String dbType, String host, String port, String username, String password, String dbName, Scanner scanner, boolean isLogin)
	{
		try
		{
			final boolean isDatabaseCreated = createDatabaseConsole(host, port, username, password, dbName, scanner);
			if (!isDatabaseCreated)
			{
				return false;
			}
			
			// Execute the SQL scripts.
			System.out.println("[INFO] Executing SQL scripts for " + dbType + " database...");
			final boolean isScriptsExecuted = executeDatabaseScriptsConsole(dbType, host, port, username, password, dbName);
			return isScriptsExecuted;
		}
		catch (Exception e)
		{
			System.out.println("[ERROR] An unexpected error occurred: " + e.getMessage());
			e.printStackTrace();
			return false; // Return false in case of any unexpected errors.
		}
	}
	
	private boolean createDatabaseConsole(String host, String port, String username, String password, String dbName, Scanner scanner)
	{
		final String dbUrl = "jdbc:mysql://" + host + ":" + port;
		
		try (Connection connection = DriverManager.getConnection(dbUrl, username, password);
			Statement statement = connection.createStatement())
		{
			// Check if the database already exists.
			ResultSet resultSet = statement.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
			if (resultSet.next())
			{
				System.out.println("[INFO] Database '" + dbName + "' already exists, skipping creation.");
				return true; // Database exists, skip creation.
			}
			
			// Create the database if it doesn't already exist.
			statement.execute("CREATE DATABASE `" + dbName + "`");
			
			System.out.println("[INFO] Database '" + dbName + "' created successfully.");
			return true;
		}
		catch (SQLException e)
		{
			System.out.println("[ERROR] Error creating the database: " + e.getMessage());
			return false;
		}
	}
	
	private boolean executeDatabaseScriptsConsole(String dbType, String host, String port, String username, String password, String dbName)
	{
		final String sqlDirectory = "sql/" + dbType;
		final File dir = new File(sqlDirectory);
		
		if (!dir.exists() || !dir.isDirectory())
		{
			System.out.println("[ERROR] Directory not found: " + dir.getAbsolutePath());
			return false;
		}
		
		final File[] sqlFiles = dir.listFiles((_, name) -> name.endsWith(".sql"));
		if ((sqlFiles == null) || (sqlFiles.length == 0))
		{
			System.out.println("[ERROR] No SQL files found in directory: " + dir.getAbsolutePath());
			return false;
		}
		
		Arrays.sort(sqlFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
		
		try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbName, username, password);
			Statement statement = connection.createStatement())
		{
			for (File sqlFile : sqlFiles)
			{
				executeSQLScriptConsole(statement, sqlFile);
			}
			
			return true;
		}
		catch (SQLException e)
		{
			System.out.println("[ERROR] Error executing SQL: " + e.getMessage());
			return false;
		}
	}
	
	private void executeSQLScriptConsole(Statement statement, File sqlFile)
	{
		System.out.println("[INFO] Executing SQL script: " + sqlFile.getName());
		
		try (Scanner fileScanner = new Scanner(sqlFile))
		{
			final StringBuilder sb = new StringBuilder();
			while (fileScanner.hasNextLine())
			{
				String line = fileScanner.nextLine().trim();
				if (line.startsWith("--") || line.isEmpty())
				{
					continue;
				}
				
				if (line.contains("--"))
				{
					line = line.split("--")[0].trim();
				}
				
				sb.append(line).append(" ");
				
				if (line.endsWith(";"))
				{
					final String sql = sb.toString().trim();
					if (!sql.isEmpty())
					{
						try
						{
							statement.execute(sql);
						}
						catch (SQLException e)
						{
							System.out.println("[ERROR] Error executing SQL: " + sql + " - " + e.getMessage());
						}
					}
					
					sb.setLength(0);
				}
			}
		}
		catch (IOException e)
		{
			System.out.println("[ERROR] Error reading SQL file: " + sqlFile.getName() + " - " + e.getMessage());
		}
	}
	
	private void testDatabaseConnectionConsole(Scanner scanner)
	{
		try
		{
			// Default values.
			final String defaultHost = "localhost";
			final String defaultPort = "3306";
			final String defaultUsername = "root";
			
			System.out.print("Enter Host [" + defaultHost + "]: ");
			String host = scanner.nextLine().trim();
			if (host.isEmpty())
			{
				host = defaultHost;
			}
			
			System.out.print("Enter Port [" + defaultPort + "]: ");
			String port = scanner.nextLine().trim();
			if (port.isEmpty())
			{
				port = defaultPort;
			}
			
			System.out.print("Enter Username [" + defaultUsername + "]: ");
			String username = scanner.nextLine().trim();
			if (username.isEmpty())
			{
				username = defaultUsername;
			}
			
			System.out.print("Enter Password: ");
			final String password = scanner.nextLine().trim();
			
			if (testConnection(host, port, username, password))
			{
				System.out.println(System.lineSeparator() + "[INFO] Connection successful!");
			}
			else
			{
				System.out.println(System.lineSeparator() + "[ERROR] Unable to connect to the database. Check your credentials.");
			}
		}
		catch (Exception e)
		{
			System.out.println(System.lineSeparator() + "[ERROR] An unexpected error occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// Method to test the database connection.
	private void testDatabaseConnection()
	{
		new SwingWorker<Void, Void>()
		{
			@Override
			protected final Void doInBackground()
			{
				final String host = _hostField.getText().trim();
				final String port = _portField.getText().trim();
				final String user = _userField.getText().trim();
				final String password = new String(_passField.getPassword()).trim();
				
				if (testConnection(host, port, user, password))
				{
					installationProgress("Connection successful!" + System.lineSeparator(), "Success");
				}
				else
				{
					installationProgress("Error: Unable to connect to the database. Check your credentials." + System.lineSeparator(), "Error");
				}
				
				return null;
			}
		}.execute();
	}
	
	// Method to validate the database connection.
	private boolean testConnection(String host, String port, String user, String password)
	{
		final String dbUrlWithoutDb = "jdbc:mysql://" + host + ":" + port;
		
		try (Connection _ = DriverManager.getConnection(dbUrlWithoutDb, user, password))
		{
			return true;
		}
		catch (SQLException e)
		{
			System.err.println("[ERROR] SQLException: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	// Method to start database installation.
	private void installDatabase()
	{
		_installButton.setEnabled(false); // Disable button during installation.
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground()
			{
				if (createDatabase())
				{
					final boolean isLoginInstalled = _loginDbCheckBox.isSelected();
					final boolean isGameInstalled = _gameDbCheckBox.isSelected();
					boolean installationSuccessful = false;
					
					if (isLoginInstalled)
					{
						installationSuccessful = installDatabase("login");
					}
					
					if (isGameInstalled)
					{
						installationSuccessful = installDatabase("game");
					}
					
					// Only show installation success message if both databases are installed successfully.
					if (installationSuccessful)
					{
						if (isLoginInstalled && isGameInstalled)
						{
							installationProgress("Login and Game databases are installed." + System.lineSeparator(), "Success");
						}
						else if (isLoginInstalled)
						{
							installationProgress("Login database is installed." + System.lineSeparator(), "Success");
						}
						else if (isGameInstalled)
						{
							installationProgress("Game database is installed." + System.lineSeparator(), "Success");
						}
					}
					else
					{
						installationProgress("Installation failed. Please check the error logs." + System.lineSeparator(), "Error");
					}
				}
				
				_installButton.setEnabled(true); // Re-enable button after installation.
				return null;
			}
		}.execute();
	}
	
	// Method to install a specific database (Login or Game).
	private boolean installDatabase(String dbType)
	{
		boolean installationSuccessful = false;
		try (Connection connection = getDatabaseConnection();
			Statement statement = connection.createStatement())
		{
			installationProgress("Installing " + dbType + " database..." + System.lineSeparator(), "Info");
			if (executeDatabaseScripts(dbType, statement))
			{
				installationSuccessful = true;
			}
			else
			{
				installationProgress("Failed to install " + dbType + " database: Directory does not exist or scripts not executed." + System.lineSeparator(), "Error");
			}
		}
		catch (SQLException e)
		{
			installationProgress("Error installing " + dbType + " database: " + e.getMessage() + System.lineSeparator(), "Error");
		}
		
		return installationSuccessful;
	}
	
	// Method to execute SQL scripts from a directory.
	private boolean executeDatabaseScripts(String dbType, Statement statement)
	{
		final String sqlDirectory = Paths.get("sql", dbType).toString(); // Use Paths.get() for better path handling.
		final File dir = new File(sqlDirectory);
		
		// Check if the directory exists.
		if (!dir.exists() || !dir.isDirectory())
		{
			installationProgress("Error: Directory does not exist: " + dir.getAbsolutePath() + System.lineSeparator(), "Error");
			return false;
		}
		
		// Get all .sql files in the directory.
		final File[] sqlFiles = dir.listFiles((_, name) -> name.endsWith(".sql"));
		if ((sqlFiles == null) || (sqlFiles.length == 0))
		{
			installationProgress("No SQL files found in directory: " + dir.getAbsolutePath() + System.lineSeparator(), "Error");
			return false;
		}
		
		Arrays.sort(sqlFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
		final int totalFiles = sqlFiles.length;
		int completedFiles = 0;
		
		// Execute each SQL file.
		for (File sqlFile : sqlFiles)
		{
			executeSQLScript(statement, sqlFile, false);
			completedFiles++;
			
			// Update progress bar.
			final int progress = (int) (((double) completedFiles / totalFiles) * 100);
			_progressBar.setValue(progress);
		}
		
		return true;
	}
	
	// Method to execute a single SQL script.
	private void executeSQLScript(Statement statement, File sqlFile, boolean skipErrors)
	{
		installationProgress("Installing " + sqlFile.getName() + System.lineSeparator(), "Info");
		
		// First pass: Count the total number of SQL statements.
		int totalStatements = 0;
		try (Scanner countScanner = new Scanner(sqlFile))
		{
			while (countScanner.hasNextLine())
			{
				final String line = countScanner.nextLine().trim();
				if (line.endsWith(";"))
				{
					totalStatements++;
				}
			}
		}
		catch (IOException e)
		{
			installationProgress("Error reading SQL file: " + sqlFile.getName() + " - " + e.getMessage() + System.lineSeparator(), "Error");
			return;
		}
		
		// Second pass: Execute each SQL statement.
		int completedStatements = 0;
		try (Scanner executeScanner = new Scanner(sqlFile))
		{
			final StringBuilder sb = new StringBuilder();
			while (executeScanner.hasNextLine())
			{
				String line = executeScanner.nextLine().trim();
				
				// Ignore comments and empty lines.
				if (line.startsWith("--") || line.isEmpty())
				{
					continue;
				}
				
				// Remove inline comments.
				if (line.contains("--"))
				{
					line = line.split("--")[0].trim();
				}
				
				sb.append(line).append(" ");
				
				// Execute SQL when a semicolon is reached.
				if (line.endsWith(";"))
				{
					final String sql = sb.toString().trim();
					if (!sql.isEmpty())
					{
						try
						{
							statement.execute(sql);
							completedStatements++;
							
							// Update progress bar.
							final int progress = (int) (((double) completedStatements / totalStatements) * 100);
							_progressBar.setValue(progress);
						}
						catch (SQLException e)
						{
							installationProgress("Error executing SQL: " + sql + System.lineSeparator() + "Error: " + e.getMessage() + System.lineSeparator(), "Error");
							if (!skipErrors)
							{
								final Object[] options =
								{
									"Continue",
									"Abort"
								};
								if (JOptionPane.showOptionDialog(null, "MySQL Error: " + e.getMessage(), "Script Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
								{
									System.exit(0);
								}
							}
						}
						
						sb.setLength(0); // Clear buffer after execution.
					}
				}
			}
		}
		catch (IOException e)
		{
			installationProgress("Error reading SQL file: " + sqlFile.getName() + " - " + e.getMessage() + System.lineSeparator(), "Error");
		}
	}
	
	// Method to create the database.
	private boolean createDatabase()
	{
		final String host = _hostField.getText().trim();
		final String port = _portField.getText().trim();
		final String user = _userField.getText().trim();
		final String password = new String(_passField.getPassword()).trim();
		final String dbName = _dbNameField.getText().trim();
		
		final String dbUrl = "jdbc:mysql://" + host + ":" + port;
		
		if (dbName.isEmpty())
		{
			installationProgress("Error: Database name cannot be empty." + System.lineSeparator(), "Error");
			return false;
		}
		
		try (Connection connection = DriverManager.getConnection(dbUrl, user, password);
			Statement statement = connection.createStatement())
		{
			
			installationProgress("Connected." + System.lineSeparator(), "Info");
			
			// Check if the database already exists.
			final ResultSet result = statement.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
			if (result.next())
			{
				// Database already exists, show confirmation dialog.
				final int confirm = JOptionPane.showOptionDialog(null, "Database '" + dbName + "' already exists. Do you want to reset it?" + System.lineSeparator() + "This will delete all existing data in the database.", "Database Exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]
				{
					"Delete and Recreate",
					"Install on Existing Database",
					"Cancel"
				}, "Cancel");
				
				if (confirm == JOptionPane.YES_OPTION)
				{
					// Yes: Delete and recreate the database.
					installationProgress("Backing up existing database..." + System.lineSeparator(), "Info");
					dumpDatabase(); // Backup the database first.
					
					statement.execute("DROP DATABASE `" + dbName + "`");
					installationProgress("Database '" + dbName + "' deleted." + System.lineSeparator(), "Info");
					statement.execute("CREATE DATABASE `" + dbName + "`");
					installationProgress("Database '" + dbName + "' created." + System.lineSeparator(), "Info");
				}
				else if (confirm == JOptionPane.NO_OPTION)
				{
					// No: Proceed with installation on the existing database without deleting it.
					installationProgress("Proceeding with installation on existing database." + System.lineSeparator(), "Info");
				}
				else if (confirm == JOptionPane.CANCEL_OPTION)
				{
					// Cancel: Abort the installation process.
					installationProgress("Installation cancelled." + System.lineSeparator(), "Info");
					return false;
				}
			}
			else
			{
				// Create the database if it doesn't exist.
				statement.execute("CREATE DATABASE `" + dbName + "`");
				installationProgress("Database '" + dbName + "' created successfully." + System.lineSeparator(), "Info");
			}
			
			// Proceed with the rest of the installation after database creation.
			statement.execute("USE `" + dbName + "`");
			installationProgress("Database '" + dbName + "' is ready." + System.lineSeparator(), "Info");
			
			return true; // Database creation successful.
		}
		catch (SQLException e)
		{
			installationProgress("Error: " + e.getMessage() + System.lineSeparator(), "Error");
			return false;
		}
	}
	
	// Method to dump the database.
	private void dumpDatabase()
	{
		final String host = _hostField.getText().trim();
		final String port = _portField.getText().trim();
		final String user = _userField.getText().trim();
		final String password = new String(_passField.getPassword()).trim();
		final String dbName = _dbNameField.getText().trim();
		
		// Ensure the dumps directory exists.
		ensureDumpsDirectoryExists();
		
		// Generate filename with timestamp.
		final String timestamp = new SimpleDateFormat("dd.MM.yyyy_HH-mm").format(new Date());
		final String filename = "dumps/" + dbName + "_dump_" + timestamp + ".sql"; // Set the file path.
		
		installationProgress("Writing dump " + timestamp + System.lineSeparator(), "Info");
		
		try (Connection con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbName, user, password);
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery("SHOW TABLES"))
		{
			
			// Create the output file.
			final File dumpFile = new File(filename);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(dumpFile)))
			{
				// Write dump header.
				writer.write("/* MySQL Dump: " + dbName + " */" + System.lineSeparator() + System.lineSeparator());
				
				while (result.next())
				{
					final String tableName = result.getString(1);
					installationProgress("Dumping Table " + tableName + System.lineSeparator(), "Info");
					
					writer.write("CREATE TABLE `" + tableName + "` (" + System.lineSeparator());
					
					// Get table structure.
					try (Statement descStatement = con.createStatement();
						ResultSet descResult = descStatement.executeQuery("DESC " + tableName))
					{
						
						boolean firstColumn = true;
						while (descResult.next())
						{
							if (!firstColumn)
							{
								writer.write("," + System.lineSeparator());
							}
							
							writer.write("\t`" + descResult.getString(1) + "` " + descResult.getString(2));
							if (descResult.getString(3).equals("NO"))
							{
								writer.write(" NOT NULL");
							}
							
							if (descResult.getString(4) != null)
							{
								writer.write(" DEFAULT '" + descResult.getString(4) + "'");
							}
							
							firstColumn = false;
						}
						
						writer.write(System.lineSeparator() + ");" + System.lineSeparator() + System.lineSeparator());
					}
					
					// Write INSERT INTO statements.
					try (Statement dataStatement = con.createStatement();
						ResultSet dataResult = dataStatement.executeQuery("SELECT * FROM " + tableName))
					{
						int rowCount = 0;
						while (dataResult.next())
						{
							if ((rowCount % 100) == 0)
							{
								writer.write("INSERT INTO `" + tableName + "` VALUES ");
							}
							
							if (rowCount > 0)
							{
								writer.write("," + System.lineSeparator());
							}
							
							writer.write("(");
							for (int i = 1; i <= dataResult.getMetaData().getColumnCount(); i++)
							{
								if (i > 1)
								{
									writer.write(", ");
								}
								
								writer.write("'" + dataResult.getString(i).replace("'", "\\'") + "'");
							}
							
							writer.write(")");
							rowCount++;
							if ((rowCount % 100) == 0)
							{
								writer.write(";" + System.lineSeparator());
							}
						}
						
						if ((rowCount % 100) != 0)
						{
							writer.write(";" + System.lineSeparator());
						}
					}
				}
				
				installationProgress("Database dump completed: " + filename + System.lineSeparator(), "Success");
			}
		}
		catch (SQLException | IOException e)
		{
			installationProgress("Error: " + e.getMessage() + System.lineSeparator(), "Error");
		}
	}
	
	// Method to ensure the 'dumps' directory exists.
	private void ensureDumpsDirectoryExists()
	{
		final File dumpsDirectory = new File("dumps"); // Set path to your dumps directory.
		if (!dumpsDirectory.exists())
		{
			boolean created = dumpsDirectory.mkdirs(); // Create the directory.
			if (created)
			{
				installationProgress("Dumps directory created: " + dumpsDirectory.getAbsolutePath() + System.lineSeparator(), "Info");
			}
			else
			{
				installationProgress("Error: Unable to create dumps directory." + System.lineSeparator(), "Error");
			}
		}
	}
	
	// Method to update the output area in the GUI with colored text.
	private void installationProgress(String text, String style)
	{
		SwingUtilities.invokeLater(() ->
		{
			try
			{
				final StyledDocument doc = _outputArea.getStyledDocument();
				
				// Create styles for different colors.
				final Style errorStyle = _outputArea.addStyle("Error", null);
				StyleConstants.setForeground(errorStyle, Color.RED);
				
				final Style successStyle = _outputArea.addStyle("Success", null);
				StyleConstants.setForeground(successStyle, Color.GREEN);
				
				final Style infoStyle = _outputArea.addStyle("Info", null);
				StyleConstants.setForeground(infoStyle, Color.WHITE);
				
				// Apply the correct style based on the message.
				if (style.equals("Error"))
				{
					doc.insertString(doc.getLength(), text + System.lineSeparator(), errorStyle); // Red for errors.
				}
				else if (style.equals("Success"))
				{
					doc.insertString(doc.getLength(), text + System.lineSeparator(), successStyle); // Green for success.
				}
				else
				{
					doc.insertString(doc.getLength(), text + System.lineSeparator(), infoStyle); // Blue for info.
				}
				
				_outputArea.setCaretPosition(doc.getLength());
			}
			catch (BadLocationException e)
			{
				e.printStackTrace();
			}
		});
	}
	
	// Method to check both checkboxes and enable/disable the Install button
	private void checkCheckboxesAndUpdateButtonState()
	{
		_installButton.setEnabled(_loginDbCheckBox.isSelected() || _gameDbCheckBox.isSelected()); // Enable button if at least one checkbox is selected.
	}
	
	private Connection getDatabaseConnection() throws SQLException
	{
		final String host = _hostField.getText().trim();
		final String port = _portField.getText().trim();
		final String user = _userField.getText().trim();
		final String password = new String(_passField.getPassword()).trim();
		final String dbName = _dbNameField.getText().trim();
		
		final String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
		return DriverManager.getConnection(dbUrl, user, password);
	}
	
	public static void main(String[] args)
	{
		new DatabaseInstaller();
	}
}
