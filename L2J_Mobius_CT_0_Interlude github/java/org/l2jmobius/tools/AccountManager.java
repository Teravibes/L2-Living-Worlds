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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.SplashScreen;

/**
 * @author Skache
 */
public class AccountManager extends JFrame
{
	private JTabbedPane _tabPanel;
	
	// Create Account components.
	private JTextField _usernameField, _passwordField;
	private JCheckBox _accessLevelCheckBox;
	private JComboBox<String> _addAccessLevelBox;
	private JButton _createButton;
	private JButton _testConnectionButton;
	private JLabel _statusConnection;
	private JProgressBar _progressBar;
	
	// Manage Accounts components.
	private JButton _searchButton;
	private JButton _updateButton;
	private JButton _deleteButton;
	private JTextField _searchAccount;
	private JPasswordField _changePassword;
	private JComboBox<String> _accuntSelcet;
	private JComboBox<String> _changeAccessLevelBox;
	private JLabel _accountCount;
	private JLabel _selectedAccount;
	
	// List Accounts components.
	private JTable _accountsTable;
	private JButton _refreshButton;
	private JButton _nextButton;
	private JButton _prevButton;
	private JLabel _totalAccounts;
	private JLabel _statusPages;
	
	private static final int PAGE_SIZE = 1000; // Page limit.
	private int currentPage = 1; // Initial page.
	
	public AccountManager()
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
		
		setTitle("Mobius - Account Manager");
		setMinimumSize(new Dimension(600, 400));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		
		// Create tab panel.
		_tabPanel = new JTabbedPane();
		_tabPanel.addTab("➕ Create Account", null, createAccountPanel(), "Create a new game account.");
		_tabPanel.addTab("🛠 Manage Accounts", null, manageAccountsPanel(), "Edit or manage existing accounts.");
		_tabPanel.addTab("📜 List Accounts", null, listAccountsPanel(), "View all registered accounts.");
		
		// Listener to ensure page is loaded when switching to List Accounts.
		_tabPanel.addChangeListener(_ ->
		{
			final int selectedIndex = _tabPanel.getSelectedIndex();
			if (selectedIndex == 2)
			{
				loadPage(currentPage);
			}
		});
		
		// Add window listener for cleanup.
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				// Clean up database resources before closing.
				System.out.println("[INFO] Cleaning up resources...");
				DatabaseFactory.close(); // Close database connections.
				System.exit(0); // Exit the application.
			}
		});
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		
		add(_tabPanel);
		setIconImages(icons);
	}
	
	private JPanel createAccountPanel()
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		// Username field.
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 1;
		panel.add(new JLabel("Username:"), gbc);
		gbc.gridx = 1;
		_usernameField = new JTextField(15);
		_usernameField.setToolTipText("Enter the username for the account.");
		panel.add(_usernameField, gbc);
		
		// Password field.
		gbc.gridx = 0;
		gbc.gridy = 2;
		panel.add(new JLabel("Password:"), gbc);
		gbc.gridx = 1;
		_passwordField = new JPasswordField(15);
		_passwordField.setToolTipText("Enter the password for the account.");
		panel.add(_passwordField, gbc);
		
		// Access Level (Checkbox + Dropdown)
		gbc.gridx = 0;
		gbc.gridy = 3;
		final JLabel accessLevelLabel = new JLabel("Access Level:");
		accessLevelLabel.setToolTipText("Disablad check box means default 'User' access level.");
		panel.add(accessLevelLabel, gbc);
		
		final JPanel accessLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		_accessLevelCheckBox = new JCheckBox();
		_accessLevelCheckBox.setToolTipText("Enable checkbox to add access level.");
		_addAccessLevelBox = new JComboBox<>(new String[]
		{
			"0 - User",
			"-1 - Banned",
			"10 - Chat Moderator",
			"20 - Test GM",
			"30 - General GM",
			"40 - Support GM",
			"50 - Event GM",
			"60 - Head GM",
			"70 - Admin",
			"100 - Master"
		});
		_addAccessLevelBox.setEnabled(false);
		_addAccessLevelBox.setToolTipText("Select the access level for the account.");
		_accessLevelCheckBox.addActionListener(_ -> _addAccessLevelBox.setEnabled(_accessLevelCheckBox.isSelected()));
		
		accessLevelPanel.add(_accessLevelCheckBox);
		accessLevelPanel.add(_addAccessLevelBox);
		
		// Create Account button.
		_createButton = new JButton("Create Account  📝");
		_createButton.setPreferredSize(new Dimension(150, 35));
		_createButton.addActionListener(this::createAccount);
		_createButton.setToolTipText("Click to create the new account.");
		
		gbc.gridx = 1;
		panel.add(accessLevelPanel, gbc);
		
		gbc.gridx = 2;
		panel.add(_createButton, gbc);
		
		// Progress bar.
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 3;
		_progressBar = new JProgressBar();
		_progressBar.setPreferredSize(new Dimension(200, 25));
		_progressBar.setVisible(true);
		panel.add(_progressBar, gbc);
		
		// Test Connection button + Status Label.
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		_testConnectionButton = new JButton("Test Connection  📡");
		_testConnectionButton.setPreferredSize(new Dimension(150, 35));
		_testConnectionButton.addActionListener(this::testConnection);
		_testConnectionButton.setToolTipText("Click to test the database connection.");
		panel.add(_testConnectionButton, gbc);
		
		// Status label (Indicator) - Default to white.
		gbc.gridx = 1;
		_statusConnection = new JLabel("●");
		_statusConnection.setFont(new Font("Arial", Font.BOLD, 50));
		_statusConnection.setForeground(Color.WHITE); // Default color is white.
		panel.add(_statusConnection, gbc);
		
		return panel;
	}
	
	private JPanel manageAccountsPanel()
	{
		// Create the main panel.
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		// Create the top panel with two sections: one for the status and one for the selected account.
		final JPanel topPanel = new JPanel(new GridLayout(1, 2));
		topPanel.setPreferredSize(new Dimension(panel.getWidth(), 40));
		
		// Create _selectedAccount.
		_selectedAccount = new JLabel("", SwingConstants.LEFT);
		_selectedAccount.setFont(new Font("Arial", Font.BOLD, 16));
		
		// Make the account label clickable.
		_selectedAccount.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		_selectedAccount.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Check if an account is selected before showing the info popup.
				if (_selectedAccount.getText().isEmpty() || _selectedAccount.getText().equals("No account selected"))
				{
					// Do nothing if no account is selected.
					return;
				}
				
				// When clicked, show the info window (popup).
				showAccountInfoPopup();
			}
			
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (_selectedAccount.getText().isEmpty() || _selectedAccount.getText().equals("No account selected"))
				{
					_selectedAccount.setCursor(Cursor.getDefaultCursor());
				}
				else
				{
					_selectedAccount.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				}
			}
			
			@Override
			public void mouseExited(MouseEvent e)
			{
				_selectedAccount.setCursor(Cursor.getDefaultCursor());
			}
		});
		
		// Custom painting for underlining the text.
		_selectedAccount.setUI(new javax.swing.plaf.basic.BasicLabelUI()
		{
			@Override
			public void paint(Graphics g, JComponent c)
			{
				super.paint(g, c);
				
				// Get the text and draw an underline.
				final String text = ((JLabel) c).getText();
				if ((text != null) && !text.isEmpty())
				{
					FontMetrics fm = g.getFontMetrics();
					final int textWidth = fm.stringWidth(text);
					final int x = 0;
					final int y = c.getHeight() - 8;
					g.drawLine(x, y, x + textWidth, y); // Draw the underline.
				}
			}
		});
		
		// Add the label to the panel.
		topPanel.add(_selectedAccount);
		
		// Create _accountCount (static and only updated when necessary)
		_accountCount = new JLabel("", SwingConstants.LEFT);
		_accountCount.setFont(new Font("Arial", Font.BOLD, 14));
		topPanel.add(_accountCount);
		
		// Add the topPanel to the main panel at the top.
		panel.add(topPanel, BorderLayout.NORTH);
		
		// Create the center panel for other components (search, account select, etc.)
		final JPanel centerPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		// Search field for accounts.
		gbc.gridx = 0;
		gbc.gridy = 0;
		centerPanel.add(new JLabel("Search Account:"), gbc);
		
		gbc.gridx = 1;
		_searchAccount = new JTextField();
		_searchAccount.setPreferredSize(new Dimension(200, 25));
		_searchAccount.setToolTipText("Search for an account by username.");
		centerPanel.add(_searchAccount, gbc);
		
		// Add a DocumentListener to enable/disable the search button based on input.
		_searchAccount.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				toggleSearchButton();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				toggleSearchButton();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				toggleSearchButton();
			}
			
			// Helper method to check the search field length and enable/disable the search button.
			private void toggleSearchButton()
			{
				if (_searchAccount.getText().length() >= 1)
				{
					_searchButton.setEnabled(true);
				}
				else
				{
					_searchButton.setEnabled(false);
				}
			}
		});
		
		// Adding KeyListener for Enter key press to trigger the search action.
		_searchAccount.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if ((e.getKeyCode() == KeyEvent.VK_ENTER) && _searchButton.isEnabled())
				{
					searchAccount(_searchAccount.getText());
				}
			}
		});
		
		// Search button.
		gbc.gridx = 2;
		_searchButton = new JButton("Search 🔍");
		_searchButton.setEnabled(false); // Initially disabled until 2 characters are typed.
		_searchButton.addActionListener(_ -> searchAccount(_searchAccount.getText()));
		centerPanel.add(_searchButton, gbc);
		
		// Account selection dropdown (Will be populated after search).
		gbc.gridx = 0;
		gbc.gridy = 1;
		centerPanel.add(new JLabel("Select Account:"), gbc);
		
		// Create a new combo box for account selection.
		_accuntSelcet = new JComboBox<>();
		_accuntSelcet.setPreferredSize(new Dimension(200, 25));
		_accuntSelcet.setToolTipText("Select an account.");
		_accuntSelcet.setEnabled(false); // Initially disabled until account is found.
		_accuntSelcet.addActionListener(_ ->
		{
			// Enable password field when an account is selected.
			if (_accuntSelcet.getSelectedItem() != null)
			{
				_changePassword.setEnabled(true);
				_updateButton.setEnabled(true); // Enable the update button as well when account is selected.
				_changeAccessLevelBox.setEnabled(true);
			}
			else
			{
				_changePassword.setEnabled(false); // Disable the password field if no account is selected.
				_updateButton.setEnabled(false); // Disable the update button if no account is selected.
				_changeAccessLevelBox.setEnabled(false);
			}
		});
		gbc.gridx = 1;
		centerPanel.add(_accuntSelcet, gbc);
		
		// New password field.
		gbc.gridx = 0;
		gbc.gridy = 3;
		centerPanel.add(new JLabel("New Password:"), gbc);
		
		gbc.gridx = 1;
		_changePassword = new JPasswordField();
		_changePassword.setPreferredSize(new Dimension(200, 25));
		_changePassword.setEnabled(false);
		centerPanel.add(_changePassword, gbc);
		
		// Change Access Level ComboBox.
		gbc.gridx = 0;
		gbc.gridy = 4;
		centerPanel.add(new JLabel("Change Access Level:"), gbc);
		
		gbc.gridx = 1;
		_changeAccessLevelBox = new JComboBox<>(new String[]
		{
			"0 - User",
			"-1 - Banned",
			"10 - Chat Moderator",
			"20 - Test GM",
			"30 - General GM",
			"40 - Support GM",
			"50 - Event GM",
			"60 - Head GM",
			"70 - Admin",
			"100 - Master"
		});
		_changeAccessLevelBox.setEnabled(false);
		_changeAccessLevelBox.setPreferredSize(new Dimension(200, 25));
		centerPanel.add(_changeAccessLevelBox, gbc);
		
		// Update Account button.
		gbc.gridx = 0;
		gbc.gridy = 5;
		_updateButton = new JButton("Update Account Information ✏️");
		_updateButton.setPreferredSize(new Dimension(220, 35));
		_updateButton.setEnabled(false); // Initially disabled until selection.
		_updateButton.addActionListener(this::updateAccount);
		centerPanel.add(_updateButton, gbc);
		
		// Delete Account button.
		gbc.gridx = 1;
		gbc.gridy = 5;
		_deleteButton = new JButton("Delete Account Permanently ❌");
		_deleteButton.setPreferredSize(new Dimension(220, 35));
		_deleteButton.setEnabled(false); // Initially disabled until selection.
		_deleteButton.addActionListener(this::deleteAccount);
		centerPanel.add(_deleteButton, gbc);
		
		panel.add(centerPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
	private JPanel listAccountsPanel()
	{
		final JPanel panel = new JPanel(new BorderLayout());
		
		// Table Column Names.
		final String[] columnNames =
		{
			"User",
			"Access Level",
			"Email",
			"Last IP",
			"Last Server",
			"PC IP",
			"Hop 1",
			"Hop 2",
			"Hop 3",
			"Hop 4",
			"Creation Date",
			"Last Active"
		};
		
		final DefaultTableModel model = new DefaultTableModel(columnNames, 0);
		_accountsTable = new JTable(model)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		_accountsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		final TableColumnModel columnModel = _accountsTable.getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++)
		{
			columnModel.getColumn(i).setPreferredWidth(150);
		}
		
		// Scroll Pane for Table.
		final JScrollPane scrollPane = new JScrollPane(_accountsTable);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		// Search Bar.
		final JTextField searchField = new JTextField(15);
		searchField.setToolTipText("Search by username...");
		searchField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				final String query = searchField.getText().trim().toLowerCase();
				filterAccounts(query);
			}
		});
		
		// Search Panel.
		final JPanel searchPanel = new JPanel(new BorderLayout());
		final JPanel leftSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		leftSearchPanel.add(new JLabel("Filter: "));
		leftSearchPanel.add(searchField);
		
		_totalAccounts = new JLabel(""); // Label to show account count
		final JPanel rightSearchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rightSearchPanel.add(_totalAccounts);
		
		searchPanel.add(leftSearchPanel, BorderLayout.WEST);
		searchPanel.add(rightSearchPanel, BorderLayout.EAST);
		
		_refreshButton = new JButton("Refresh ♻️");
		_refreshButton.setToolTipText("Reload the account list");
		_refreshButton.addActionListener(_ -> refreshAccountsList());
		
		final JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		_prevButton = new JButton("⬅️ Previous");
		_nextButton = new JButton("Next ➡️");
		_prevButton.setToolTipText("Go to the previous page");
		_nextButton.setToolTipText("Go to the next page");
		_prevButton.addActionListener(_ -> loadPage(currentPage - 1));
		_nextButton.addActionListener(_ -> loadPage(currentPage + 1));
		
		paginationPanel.add(_prevButton);
		paginationPanel.add(_nextButton);
		
		// Add components to panel.
		final JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(_refreshButton, BorderLayout.WEST);
		bottomPanel.add(paginationPanel, BorderLayout.EAST);
		
		// Status Pages.
		_statusPages = new JLabel();
		final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		statusPanel.add(_statusPages);
		bottomPanel.add(statusPanel, BorderLayout.CENTER);
		
		panel.add(searchPanel, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.add(bottomPanel, BorderLayout.SOUTH);
		
		refreshAccountsList();
		
		return panel;
	}
	
	private void refreshAccountsList()
	{
		_refreshButton.setEnabled(false);
		
		new SwingWorker<Object[][], Void>()
		{
			@Override
			protected Object[][] doInBackground()
			{
				return loadAccountsData(currentPage);
			}
			
			@Override
			protected void done()
			{
				try
				{
					final Object[][] data = get();
					final DefaultTableModel model = (DefaultTableModel) _accountsTable.getModel();
					model.setRowCount(0);
					
					for (Object[] row : data)
					{
						model.addRow(row);
					}
					
					// Update total accounts label
					final int totalAccounts = getTotalAccountsCount();
					_totalAccounts.setText("<html><b>Total Accounts: " + totalAccounts + "</b></html>");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					_refreshButton.setEnabled(true);
				}
			}
		}.execute();
	}
	
	private void filterAccounts(String query)
	{
		final DefaultTableModel model = (DefaultTableModel) _accountsTable.getModel();
		final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
		_accountsTable.setRowSorter(sorter);
		
		if (query.isEmpty())
		{
			sorter.setRowFilter(null);
		}
		else
		{
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 0));
		}
	}
	
	private void loadPage(int newPage)
	{
		if (newPage < 1)
		{
			return; // Prevent going below page 1.
		}
		
		final int totalPages = getTotalPages();
		if (newPage > totalPages)
		{
			return; // Prevent going beyond the last page.
		}
		
		currentPage = newPage;
		
		_statusPages.setText("Page " + currentPage + " / " + totalPages);
		
		// Enable/Disable pagination buttons based on the page.
		_prevButton.setEnabled(currentPage > 1);
		_nextButton.setEnabled(currentPage < totalPages);
		
		// Refresh the table data for the new page.
		refreshAccountsList();
	}
	
	private int getTotalPages()
	{
		final int totalAccounts = getTotalAccountsCount();
		return (int) Math.ceil((double) totalAccounts / PAGE_SIZE);
	}
	
	private int getTotalAccountsCount()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts");
			ResultSet result = statement.executeQuery())
		{
			if (result.next())
			{
				return result.getInt(1); // Return the total count of accounts.
			}
		}
		catch (SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Error while retrieving total accounts count: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
		}
		
		return -1;
	}
	
	private Object[][] loadAccountsData(int page)
	{
		final int offset = (page - 1) * PAGE_SIZE;
		final List<Object[]> accountList = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT login, accessLevel, email, lastIP, lastServer, pcIp, hop1, hop2, hop3, hop4, created_time, lastactive " + "FROM accounts LIMIT ? OFFSET ?"))
		{
			
			statement.setInt(1, PAGE_SIZE);
			statement.setInt(2, offset);
			
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					accountList.add(new Object[]
					{
						result.getString("login"),
						result.getInt("accessLevel"),
						result.getString("email") != null ? result.getString("email") : "N/A",
						result.getString("lastIP") != null ? result.getString("lastIP") : "Unknown",
						result.getInt("lastServer"),
						result.getString("pcIp") != null ? result.getString("pcIp") : "N/A",
						result.getString("hop1") != null ? result.getString("hop1") : "N/A",
						result.getString("hop2") != null ? result.getString("hop2") : "N/A",
						result.getString("hop3") != null ? result.getString("hop3") : "N/A",
						result.getString("hop4") != null ? result.getString("hop4") : "N/A",
						TimeUtil.getDateTimeString(new Date(result.getTimestamp("created_time").getTime())),
						TimeUtil.getDateTimeString(new Date(result.getLong("lastactive")))
					});
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return accountList.isEmpty() ? new Object[0][0] : accountList.toArray(new Object[0][0]);
	}
	
	private void showAccountInfoPopup()
	{
		// Get the selected account name.
		final String accountName = _selectedAccount.getText();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT login, accessLevel, email, lastIP, created_time, lastactive FROM accounts WHERE login = ?"))
		{
			statement.setString(1, accountName);
			
			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					final String login = result.getString("login");
					final int accessLevel = result.getInt("accessLevel");
					final String email = result.getString("email") != null ? result.getString("email") : "N/A";
					final String lastIP = result.getString("lastIP") != null ? result.getString("lastIP") : "Unknown";
					final String createdTime = result.getTimestamp("created_time") != null ? TimeUtil.getDateTimeString(new Date(result.getTimestamp("created_time").getTime())) : "Unknown";
					final String lastActive = result.getLong("lastactive") > 0 ? TimeUtil.getDateTimeString(new Date(result.getLong("lastactive"))) : "Never";
					
					final String message = String.format("<html><b>🔹 Account Name:</b> %s<br>" + "<b>🔐 Access Level:</b> %d<br>" + "<b>📧 Email:</b> %s<br>" + "<b>🌐 Last IP:</b> %s<br>" + "<b>📅 Created:</b> %s<br>" + "<b>🕒 Last Active:</b> %s</html>", login, accessLevel, email, lastIP, createdTime, lastActive);
					
					final JLabel label = new JLabel(message);
					label.setFont(new Font("Arial", Font.PLAIN, 13));
					
					JOptionPane.showMessageDialog(null, label, "Account Information", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					JOptionPane.showMessageDialog(null, "Account not found!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error retrieving account information!", "Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void searchAccount(String username)
	{
		final List<String> accountList = new ArrayList<>();
		final Map<String, String> accountAccessLevels = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT login, accessLevel FROM accounts WHERE login LIKE ?"))
		{
			statement.setString(1, "%" + username + "%");
			
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					final String login = result.getString("login");
					final String accessLevel = result.getString("accessLevel");
					accountList.add(login);
					accountAccessLevels.put(login, accessLevel);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		// Display the number of results found.
		final int resultsCount = accountList.size();
		if (resultsCount > 0)
		{
			_accuntSelcet.setEnabled(true);
			_accuntSelcet.removeAllItems();
			
			for (String account : accountList)
			{
				_accuntSelcet.addItem(account);
			}
			
			_accuntSelcet.setSelectedIndex(-1);
			_accountCount.setText(resultsCount + " account(s) found.");
			_accountCount.setForeground(Color.GREEN);
			_selectedAccount.setText("");
			_updateButton.setEnabled(true);
			_deleteButton.setEnabled(true);
			_accuntSelcet.addActionListener(_ ->
			{
				final String selectedAccount = (String) _accuntSelcet.getSelectedItem();
				if (selectedAccount != null)
				{
					// Update _selectedAccount label to show the selected account.
					_selectedAccount.setText("" + selectedAccount);
				}
			});
		}
		else
		{
			// Show status: No account found.
			_accountCount.setText("No account found.");
			_accountCount.setForeground(Color.RED);
			
			// Disable account management actions.
			_accuntSelcet.setEnabled(false);
			_changePassword.setEnabled(false);
			_updateButton.setEnabled(false);
			_deleteButton.setEnabled(false);
			_selectedAccount.setText("");
		}
	}
	
	private void createAccount(ActionEvent event)
	{
		final String username = _usernameField.getText().trim();
		final String password = new String(((JPasswordField) _passwordField).getPassword()).trim();
		final int accessLevel = _accessLevelCheckBox.isSelected() ? Integer.parseInt(((String) _addAccessLevelBox.getSelectedItem()).split(" - ")[0]) : 0;
		
		// Validate username and password.
		if (username.isEmpty() || password.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Username and Password cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				// Check if the username already exists.
				try (PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login = ?"))
				{
					statement.setString(1, username);
					try (ResultSet result = statement.executeQuery())
					{
						if (result.next() && (result.getInt(1) > 0))
						{
							JOptionPane.showMessageDialog(this, "Username already exists! Please choose another.", "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
				}
				
				final MessageDigest md = MessageDigest.getInstance("SHA");
				final byte[] raw = password.getBytes(StandardCharsets.UTF_8);
				final String hashBase64 = Base64.getEncoder().encodeToString(md.digest(raw));
				
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO accounts (login, password, accessLevel) VALUES (?, ?, ?)"))
				{
					statement.setString(1, username);
					statement.setString(2, hashBase64);
					statement.setInt(3, accessLevel);
					
					if (statement.executeUpdate() > 0)
					{
						JOptionPane.showMessageDialog(this, "Account created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
						_usernameField.setText("");
						_passwordField.setText("");
					}
					else
					{
						JOptionPane.showMessageDialog(this, "Failed to create the account. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Error creating account: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void updateAccount(ActionEvent event)
	{
		final String password = new String(_changePassword.getPassword()).trim();
		final String selectedUsername = (String) _accuntSelcet.getSelectedItem();
		
		// Ensure a valid access level is selected
		final String accessLevelStr = (String) _changeAccessLevelBox.getSelectedItem();
		if ((accessLevelStr == null) || !accessLevelStr.contains(" - "))
		{
			JOptionPane.showMessageDialog(this, "Invalid access level selection!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final int accessLevel = Integer.parseInt(accessLevelStr.split(" - ")[0]);
		if ((selectedUsername == null) || selectedUsername.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "No account selected!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final int confirmUpdate = JOptionPane.showConfirmDialog(this, "Are you sure you want to update the account?", "Confirm Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirmUpdate != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				final StringBuilder sql = new StringBuilder("UPDATE accounts SET ");
				final List<Object> params = new ArrayList<>();
				
				if (!password.isEmpty())
				{
					final MessageDigest md = MessageDigest.getInstance("SHA");
					final byte[] raw = password.getBytes(StandardCharsets.UTF_8);
					final String hashBase64 = Base64.getEncoder().encodeToString(md.digest(raw));
					
					sql.append("password = ?, ");
					params.add(hashBase64);
				}
				
				// Only add access level if it's changed
				if (_changeAccessLevelBox.isEnabled()) // Check if access level change is allowed
				{
					sql.append("accessLevel = ?, ");
					params.add(accessLevel);
				}
				
				if (params.isEmpty()) // Ensure at least one field is updated
				{
					JOptionPane.showMessageDialog(this, "Nothing to update!", "Warning", JOptionPane.WARNING_MESSAGE);
					return;
				}
				
				// Remove the last ", " from the query
				sql.setLength(sql.length() - 2);
				sql.append(" WHERE login = ?");
				params.add(selectedUsername);
				
				try (PreparedStatement statement = con.prepareStatement(sql.toString()))
				{
					for (int i = 0; i < params.size(); i++)
					{
						statement.setObject(i + 1, params.get(i));
					}
					
					if (statement.executeUpdate() > 0)
					{
						JOptionPane.showMessageDialog(this, "Account updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
						_changePassword.setText("");
					}
					else
					{
						JOptionPane.showMessageDialog(this, "Failed to update the account. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Error updating account: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void deleteAccount(ActionEvent event)
	{
		final String username = (String) _accuntSelcet.getSelectedItem(); // Get selected username
		if ((username == null) || username.trim().isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Please select an account to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		final int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the account '" + username + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				try (PreparedStatement statement = con.prepareStatement("DELETE FROM accounts WHERE login = ?"))
				{
					statement.setString(1, username);
					
					final int rowsAffected = statement.executeUpdate();
					if (rowsAffected > 0)
					{
						JOptionPane.showMessageDialog(this, "Account '" + username + "' deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
						
						// Remove the deleted account from the dropdown.
						_accuntSelcet.removeItem(username);
					}
					else
					{
						JOptionPane.showMessageDialog(this, "No account found with that username.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
		catch (SQLException e)
		{
			JOptionPane.showMessageDialog(this, "SQL Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void testConnection(ActionEvent event)
	{
		_progressBar.setString("Testing...");
		_progressBar.setStringPainted(true);
		_progressBar.setIndeterminate(true);
		
		SwingWorker<Boolean, Void> worker = new SwingWorker<>()
		{
			@Override
			protected Boolean doInBackground() throws Exception
			{
				Thread.sleep(2000);
				return databaseConnection();
			}
			
			@Override
			protected void done()
			{
				try
				{
					final boolean isConnected = get();
					if (isConnected)
					{
						_statusConnection.setForeground(Color.GREEN);
						_progressBar.setString("Connected");
					}
					else
					{
						_statusConnection.setForeground(Color.RED);
						_progressBar.setString("Failed");
					}
					
					_progressBar.setIndeterminate(false);
					_progressBar.setValue(100);
					
					SwingWorker<Void, Void> resetColorWorker = new SwingWorker<>()
					{
						@Override
						protected Void doInBackground() throws Exception
						{
							Thread.sleep(5000);
							return null;
						}
						
						@Override
						protected void done()
						{
							_statusConnection.setForeground(Color.WHITE);
						}
					};
					resetColorWorker.execute();
				}
				catch (Exception ex)
				{
					// Handle any exceptions and update UI accordingly.
					_statusConnection.setForeground(Color.RED);
					_progressBar.setString("Failed");
					_progressBar.setIndeterminate(false);
					_progressBar.setValue(100);
					ex.printStackTrace();
				}
			}
		};
		
		// Execute the worker in a separate thread.
		worker.execute();
	}
	
	private boolean databaseConnection()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				return true; // Successful connection.
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return false; // Failed connection.
	}
	
	private void console()
	{
		System.out.println("=========================================================");
		System.out.println("       L2jMobius Development - Account Manager          ");
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
					scanner.nextLine(); // Clear invalid input
					continue;
				}
				
				final int choice = scanner.nextInt();
				scanner.nextLine();
				
				switch (choice)
				{
					case 1:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Create Account");
						createAccountCmd(scanner);
						break;
					}
					case 2:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Delete Account");
						deleteAccountCmd(scanner);
						break;
					}
					case 3:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: Update Accounts");
						updateAccountCmd();
						break;
					}
					case 4:
					{
						System.out.println(System.lineSeparator() + "[INFO] You selected: List All Accounts");
						listAccountsCmd();
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
							// Who dares interrupt my dramatic exit?!
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
			// Cleanup resources when exiting console mode
			System.out.println("[INFO] Cleaning up resources...");
			
			// Close any open resources, like database connections
			DatabaseFactory.close();
		}
	}
	
	private void showMenu()
	{
		System.out.println(System.lineSeparator() + "=========================================================");
		System.out.println("                  AVAILABLE COMMANDS                     ");
		System.out.println("---------------------------------------------------------");
		System.out.println("  [1] Create Account");
		System.out.println("  [2] Delete Account");
		System.out.println("  [3] Update Accounts");
		System.out.println("  [4] List All Accounts");
		System.out.println("  [5] Exit");
		System.out.println("---------------------------------------------------------");
	}
	
	private void createAccountCmd(Scanner scanner)
	{
		String username = "";
		String password = "";
		int accessLevel = 0; // Initialize access level (default 0 for User).
		
		while (username.isEmpty())
		{
			System.out.print("Enter username: ");
			username = scanner.nextLine().trim();
			
			if (username.isEmpty())
			{
				System.out.println("Username cannot be empty! Please provide a valid username.");
			}
		}
		
		while (password.isEmpty())
		{
			System.out.print("Enter password: ");
			password = scanner.nextLine().trim();
			
			if (password.isEmpty())
			{
				System.out.println("Password cannot be empty! Please provide a valid password.");
			}
		}
		
		// Define the valid access levels.
		final Set<Integer> validAccessLevels = new HashSet<>(Arrays.asList(-1, 0, 10, 20, 30, 40, 50, 60, 70, 100));
		
		// Display the access level options.
		System.out.println("Select access level from the following options:");
		System.out.println("0 - User (default)");
		System.out.println("-1 - Banned");
		System.out.println("10 - Chat Moderator");
		System.out.println("20 - Test GM");
		System.out.println("30 - General GM");
		System.out.println("40 - Support GM");
		System.out.println("50 - Event GM");
		System.out.println("60 - Head GM");
		System.out.println("70 - Admin");
		System.out.println("100 - Master");
		
		// Loop to get a valid access level.
		boolean validAccessLevel = false;
		while (!validAccessLevel)
		{
			System.out.print("Enter access level (default is 0 - User, or choose a number corresponding to the role): ");
			
			// Allow the user to press Enter without input (use default value of 0).
			final String input = scanner.nextLine().trim();
			if (input.isEmpty())
			{
				validAccessLevel = true;
				accessLevel = 0;
				System.out.println("Default access level (0 - User) selected.");
			}
			else
			{
				try
				{
					accessLevel = Integer.parseInt(input);
					
					// Validate if the entered access level is in the allowed list.
					if (validAccessLevels.contains(accessLevel))
					{
						validAccessLevel = true;
					}
					else
					{
						System.out.println("Invalid access level! Please select a valid role.");
					}
				}
				catch (NumberFormatException e)
				{
					System.out.println("Invalid input. Please enter a valid number for access level.");
				}
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login = ?"))
				{
					statement.setString(1, username);
					try (ResultSet result = statement.executeQuery())
					{
						if (result.next() && (result.getInt(1) > 0))
						{
							System.out.println("Username already exists! Please choose another.");
							return;
						}
					}
				}
				
				final MessageDigest md = MessageDigest.getInstance("SHA");
				final byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
				final String hashedPassword = Base64.getEncoder().encodeToString(hashedBytes);
				
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO accounts (login, password, accessLevel) VALUES (?, ?, ?)"))
				{
					statement.setString(1, username);
					statement.setString(2, hashedPassword);
					statement.setInt(3, accessLevel);
					
					if (statement.executeUpdate() > 0)
					{
						System.out.println("Account " + username + " created successfully!");
					}
					else
					{
						System.out.println("Failed to create the account.");
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error creating account: " + e.getMessage());
		}
	}
	
	private void deleteAccountCmd(Scanner scanner)
	{
		System.out.print("Enter the username of the account to delete: ");
		String username = "";
		
		// Check if input is available.
		if (scanner.hasNextLine())
		{
			username = scanner.nextLine().trim();
		}
		else
		{
			System.out.println("No input provided.");
			return;
		}
		
		if (username.isEmpty())
		{
			System.out.println("Username cannot be empty!");
			return;
		}
		
		// Ask for confirmation before deleting the account.
		System.out.print("Are you sure you want to delete the account '" + username + "'? (y/n): ");
		final String confirmation = scanner.nextLine().trim().toLowerCase();
		if (!confirmation.equals("y"))
		{
			System.out.println("Account deletion cancelled.");
			return;
		}
		
		// Proceed with the account deletion logic.
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login = ?"))
				{
					statement.setString(1, username);
					try (ResultSet result = statement.executeQuery())
					{
						if (result.next() && (result.getInt(1) == 0))
						{
							System.out.println("Account with the username '" + username + "' does not exist.");
							return;
						}
					}
				}
				
				try (PreparedStatement statement = con.prepareStatement("DELETE FROM accounts WHERE login = ?"))
				{
					statement.setString(1, username);
					
					if (statement.executeUpdate() > 0)
					{
						System.out.println("Account '" + username + "' deleted successfully.");
					}
					else
					{
						System.out.println("Failed to delete the account. Please try again.");
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error deleting account: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void updateAccountCmd()
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)))
		{
			System.out.print("Enter the username of the account to update: ");
			final String username = reader.readLine().trim();
			
			if (username.isEmpty())
			{
				System.out.println("Username cannot be empty!");
				return;
			}
			
			try (Connection con = DatabaseFactory.getConnection())
			{
				if (con == null)
				{
					System.out.println("Database connection failed!");
					return;
				}
				
				// Check if account exists.
				try (PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login = ?"))
				{
					statement.setString(1, username);
					try (ResultSet result = statement.executeQuery())
					{
						if (result.next() && (result.getInt(1) == 0))
						{
							System.out.println("Account with username '" + username + "' does not exist.");
							return;
						}
					}
				}
				
				// Get new password (optional)
				System.out.print("Enter new password (or leave empty to keep current password): ");
				final String newPassword = reader.readLine().trim();
				String hashBase64 = null;
				
				if (!newPassword.isEmpty())
				{
					final MessageDigest md = MessageDigest.getInstance("SHA");
					final byte[] raw = newPassword.getBytes(StandardCharsets.UTF_8);
					hashBase64 = Base64.getEncoder().encodeToString(md.digest(raw));
				}
				
				// Get new access level (optional)
				Integer newAccessLevel = null;
				while (true)
				{
					System.out.println("Select new access level (leave empty to keep current level):");
					System.out.println("0 - User");
					System.out.println("-1 - Banned");
					System.out.println("10 - Chat Moderator");
					System.out.println("20 - Test GM");
					System.out.println("30 - General GM");
					System.out.println("40 - Support GM");
					System.out.println("50 - Event GM");
					System.out.println("60 - Head GM");
					System.out.println("70 - Admin");
					System.out.println("100 - Master");
					
					System.out.print("Enter new access level: ");
					final String accessLevelInput = reader.readLine().trim();
					
					if (accessLevelInput.isEmpty())
					{
						break;
					}
					
					try
					{
						newAccessLevel = Integer.parseInt(accessLevelInput);
						break;
					}
					catch (NumberFormatException e)
					{
						System.out.println("Invalid access level. Please enter a valid number.");
					}
				}
				
				// If no changes, exit early.
				if ((hashBase64 == null) && (newAccessLevel == null))
				{
					System.out.println("No changes made to the '" + username + "' account.");
					return;
				}
				
				final StringBuilder sqlBuilder = new StringBuilder("UPDATE accounts SET ");
				final List<Object> params = new ArrayList<>();
				if (hashBase64 != null)
				{
					sqlBuilder.append("password = ?, ");
					params.add(hashBase64);
				}
				
				if (newAccessLevel != null)
				{
					sqlBuilder.append("accessLevel = ?, ");
					params.add(newAccessLevel);
				}
				
				sqlBuilder.setLength(sqlBuilder.length() - 2);
				sqlBuilder.append(" WHERE login = ?");
				params.add(username);
				
				try (PreparedStatement statement = con.prepareStatement(sqlBuilder.toString()))
				{
					for (int i = 0; i < params.size(); i++)
					{
						statement.setObject(i + 1, params.get(i));
					}
					
					final int rowsUpdated = statement.executeUpdate();
					if (rowsUpdated > 0)
					{
						System.out.println("Account '" + username + "' updated successfully!");
					}
					else
					{
						System.out.println("Failed to update the account. Please try again.");
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error updating account: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void listAccountsCmd()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (con != null)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT login, accessLevel FROM accounts");
					ResultSet result = statement.executeQuery())
				{
					if (!result.isBeforeFirst())
					{
						System.out.println("No accounts found.");
						return;
					}
					
					// Print the table header.
					System.out.println("================================================");
					System.out.printf("| %-4s | %-22s | %-12s |" + System.lineSeparator(), "No.", "Username", "Access Level");
					System.out.println("================================================");
					
					int userNumber = 1;
					while (result.next())
					{
						final String username = result.getString("login");
						final int accessLevel = result.getInt("accessLevel");
						System.out.printf("| %-4d | %-22s | %-12d |" + System.lineSeparator(), userNumber++, username, accessLevel);
						System.out.println("------------------------------------------------");
					}
					
					// Print the total accounts count.
					final int totalAccounts = getTotalAccountsCount();
					System.out.println(System.lineSeparator() + "Total Accounts: " + (totalAccounts >= 0 ? totalAccounts : "Error retrieving count"));
					
				}
				catch (Exception e)
				{
					System.out.println("Error retrieving accounts: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error with database connection: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		InterfaceConfig.load();
		DatabaseFactory.init();
		SwingUtilities.invokeLater(AccountManager::new);
	}
}
