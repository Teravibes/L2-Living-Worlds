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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.util.ConfigReader;

/**
 * A search tool that searches for patterns in files within a directory, while ignoring specified folders or files.
 * @author Mobius
 */
public class Search extends JFrame
{
	// Set of allowed file extensions.
	private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>();
	
	// List of folders or files to ignore (e.g., bin, .svn, .git).
	private static final Set<String> IGNORE_LIST = new HashSet<>();
	static
	{
		IGNORE_LIST.add(File.separator + "bin" + File.separator);
		IGNORE_LIST.add(".svn" + File.separator);
		IGNORE_LIST.add(".git" + File.separator);
	}
	
	// Startup directory.
	private static final Path START_DIR = Paths.get(System.getProperty("user.dir"));
	
	// Static variables to track search statistics.
	private static int _totalFilesSearched = 0;
	private static int _totalMatchesFound = 0;
	private static int _filesWithMatches = 0;
	
	// Flag to control case-insensitive search. Default is true.
	private static boolean _caseInsensitive = true;
	
	// GUI Components.
	private JTextField _searchField;
	private DefaultTableModel _tableModel;
	private JLabel _summaryLabel;
	private JLabel _timeLabel;
	private JButton _searchButton;
	private JCheckBox _allCheckBox;
	private JCheckBox _iniCheckBox;
	private JCheckBox _xmlCheckBox;
	private JCheckBox _javaCheckBox;
	private JCheckBox _sqlCheckBox;
	private JCheckBox _htmlCheckBox;
	private JProgressBar _progressBar;
	private JPanel _summaryPanel;
	
	// Flag to control search state.
	private boolean _isSearching = false;
	
	// Variable to store the selected file paths.
	private List<String> _selectedFilePaths = new ArrayList<>();
	
	/**
	 * Starts the search tool and interacts with the user.
	 */
	private Search()
	{
		final ConfigReader interfaceConfig;
		final Path configPath = START_DIR.resolve("dist" + File.separator + "game" + File.separator + "config");
		if (Files.exists(configPath) && Files.isDirectory(configPath))
		{
			interfaceConfig = new ConfigReader(configPath + File.separator + "Interface.ini");
		}
		else
		{
			interfaceConfig = new ConfigReader("." + File.separator + "config" + File.separator + "Interface.ini");
		}
		
		if (interfaceConfig.getBoolean("EnableGUI", true) && !GraphicsEnvironment.isHeadless())
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
		final Path imagesPath = START_DIR.resolve("dist" + File.separator + "images");
		if (Files.exists(imagesPath) && Files.isDirectory(imagesPath))
		{
			new SplashScreen(imagesPath + File.separator + "splash.png", 3000, this);
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_16x16.png").getImage());
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_32x32.png").getImage());
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_64x64.png").getImage());
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_128x128.png").getImage());
			setIconImages(icons);
		}
		else
		{
			new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 3000, this);
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
			icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
			setIconImages(icons);
		}
		
		setTitle("Mobius - Search");
		setMinimumSize(new Dimension(800, 600));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		
		// Create the search panel.
		final JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new GridLayout(2, 1));
		
		// Search text field with label and buttons.
		final JPanel searchTextPanel = new JPanel();
		
		searchTextPanel.add(new JLabel("Search text (regex supported):"));
		_searchField = new JTextField(30);
		searchTextPanel.add(_searchField);
		
		// When Enter key is pressed, trigger the search.
		_searchField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					performGuiSearch();
				}
			}
		});
		
		_searchButton = new JButton("Search");
		searchTextPanel.add(_searchButton);
		
		// Add the caseSensitiveCheckBox to the EAST (right side).
		final JCheckBox caseSensitiveCheckBox = new JCheckBox("Case Sensitive", !_caseInsensitive);
		searchTextPanel.add(caseSensitiveCheckBox, BorderLayout.EAST);
		caseSensitiveCheckBox.addItemListener(_ ->
		{
			_caseInsensitive = !caseSensitiveCheckBox.isSelected();
		});
		
		searchPanel.add(searchTextPanel);
		
		// Checkboxes for file types.
		final JPanel checkBoxPanel = new JPanel(new BorderLayout());
		
		// Create a sub-panel for the checkboxes (center alignment).
		final JPanel centerPanel = new JPanel();
		_allCheckBox = new JCheckBox("All", true);
		_iniCheckBox = new JCheckBox("ini", true);
		_xmlCheckBox = new JCheckBox("xml", true);
		_javaCheckBox = new JCheckBox("java", true);
		_sqlCheckBox = new JCheckBox("sql", true);
		_htmlCheckBox = new JCheckBox("html", true);
		centerPanel.add(_allCheckBox);
		centerPanel.add(_iniCheckBox);
		centerPanel.add(_sqlCheckBox);
		centerPanel.add(_xmlCheckBox);
		centerPanel.add(_javaCheckBox);
		centerPanel.add(_htmlCheckBox);
		
		// Add the centerPanel to the CENTER of the checkBoxPanel.
		checkBoxPanel.add(centerPanel, BorderLayout.CENTER);
		
		// Add item listeners to checkboxes.
		_allCheckBox.addItemListener(_ ->
		{
			final boolean isSelected = _allCheckBox.isSelected();
			_iniCheckBox.setSelected(isSelected);
			_xmlCheckBox.setSelected(isSelected);
			_javaCheckBox.setSelected(isSelected);
			_sqlCheckBox.setSelected(isSelected);
			_htmlCheckBox.setSelected(isSelected);
		});
		
		// Add item listeners to individual checkboxes to deselect "All" when any is deselected.
		final ItemListener checkboxListener = _ ->
		{
			final boolean isIniCheckBoxSelected = _iniCheckBox.isSelected();
			final boolean isXmlCheckBoxSelected = _xmlCheckBox.isSelected();
			final boolean isJavaCheckBoxSelected = _javaCheckBox.isSelected();
			final boolean isSqlCheckBoxSelected = _sqlCheckBox.isSelected();
			final boolean isHtmlCheckBoxSelected = _htmlCheckBox.isSelected();
			if (!isIniCheckBoxSelected || !isXmlCheckBoxSelected || !isJavaCheckBoxSelected || !isSqlCheckBoxSelected || !isHtmlCheckBoxSelected)
			{
				_allCheckBox.setSelected(false);
				_iniCheckBox.setSelected(isIniCheckBoxSelected);
				_xmlCheckBox.setSelected(isXmlCheckBoxSelected);
				_javaCheckBox.setSelected(isJavaCheckBoxSelected);
				_sqlCheckBox.setSelected(isSqlCheckBoxSelected);
				_htmlCheckBox.setSelected(isHtmlCheckBoxSelected);
			}
			else if (isIniCheckBoxSelected && isXmlCheckBoxSelected && isJavaCheckBoxSelected && isSqlCheckBoxSelected && isHtmlCheckBoxSelected)
			{
				_allCheckBox.setSelected(true);
			}
		};
		
		_iniCheckBox.addItemListener(checkboxListener);
		_xmlCheckBox.addItemListener(checkboxListener);
		_javaCheckBox.addItemListener(checkboxListener);
		_sqlCheckBox.addItemListener(checkboxListener);
		_htmlCheckBox.addItemListener(checkboxListener);
		
		// Add the checkBoxPanel to the searchPanel.
		searchPanel.add(checkBoxPanel);
		
		// Create the result table.
		_tableModel = new DefaultTableModel(new String[]
		{
			"File Name",
			"Matches"
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false; // Make all cells non-editable.
			}
		};
		
		// Enable column sorting.
		final JTable resultTable = new JTable(_tableModel);
		resultTable.setAutoCreateRowSorter(true);
		
		// Set the preferred width of the Matches column to a smaller value.
		final TableColumn matchesColumn1 = resultTable.getColumnModel().getColumn(0);
		matchesColumn1.setPreferredWidth(740);
		final TableColumn matchesColumn2 = resultTable.getColumnModel().getColumn(1);
		matchesColumn2.setPreferredWidth(60);
		
		// Create a custom cell renderer to align text to the right.
		final DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		matchesColumn2.setCellRenderer(rightRenderer);
		
		// Use TableRowSorter for sorting.
		final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(_tableModel);
		resultTable.setRowSorter(sorter);
		sorter.setComparator(1, (o1, o2) ->
		{
			// Compare the values in the "Matches" column as integers.
			final int matches1 = Integer.parseInt(o1.toString());
			final int matches2 = Integer.parseInt(o2.toString());
			return Integer.compare(matches1, matches2);
		});
		
		// Create the summary labels
		_summaryPanel = new JPanel(new GridLayout(2, 1)); // Two rows for summary and time labels.
		_summaryLabel = new JLabel("");
		_timeLabel = new JLabel();
		_summaryPanel.add(_summaryLabel);
		_summaryPanel.add(_timeLabel);
		
		// Progress bar
		_progressBar = new JProgressBar(0, 100);
		_progressBar.setStringPainted(true);
		_progressBar.setVisible(false); // Initially hidden.
		
		// Set the preferred size of the progress bar (width, height).
		_progressBar.setPreferredSize(new Dimension(_progressBar.getPreferredSize().width, 30));
		
		// Combine progress bar and summary panel into a single panel.
		final JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(_progressBar, BorderLayout.CENTER); // Progress bar takes full width.
		bottomPanel.add(_summaryPanel, BorderLayout.SOUTH); // Summary labels below the progress bar.
		
		// Add components to the frame.
		setLayout(new BorderLayout());
		add(searchPanel, BorderLayout.NORTH);
		add(new JScrollPane(resultTable), BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH); // Add the combined panel to the SOUTH region.
		
		// Add action listeners.
		_searchButton.addActionListener(_ -> performGuiSearch());
		
		final JPopupMenu popupMenu = new JPopupMenu();
		final JMenuItem openWithMenuItem = new JMenuItem("Open File");
		popupMenu.add(openWithMenuItem);
		final JMenuItem copyFileMenuItem = new JMenuItem("Copy File");
		popupMenu.add(copyFileMenuItem);
		final JMenuItem openLocationMenuItem = new JMenuItem("Open Location");
		popupMenu.add(openLocationMenuItem);
		final JMenuItem copyLocationMenuItem = new JMenuItem("Copy Location");
		popupMenu.add(copyLocationMenuItem);
		final JMenuItem propertiesMenuItem = new JMenuItem("Properties");
		popupMenu.add(propertiesMenuItem);
		
		// Add action listeners.
		openWithMenuItem.addActionListener(_ ->
		{
			for (String filePath : _selectedFilePaths)
			{
				openFile(filePath);
			}
		});
		copyFileMenuItem.addActionListener(_ -> copyFile(_selectedFilePaths));
		openLocationMenuItem.addActionListener(_ ->
		{
			for (String filePath : _selectedFilePaths)
			{
				openFileLocation(filePath);
			}
		});
		copyLocationMenuItem.addActionListener(_ -> copyFileLocation(_selectedFilePaths));
		propertiesMenuItem.addActionListener(_ ->
		{
			for (String filePath : _selectedFilePaths)
			{
				showProperties(filePath);
			}
		});
		
		resultTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (SwingUtilities.isRightMouseButton(event)) // Check for right-click.
				{
					final int row = resultTable.rowAtPoint(event.getPoint()); // Get the row under the mouse pointer.
					if (row >= 0) // Ensure a valid row is selected.
					{
						// Check if the right-clicked row is already selected.
						if (!resultTable.isRowSelected(row))
						{
							resultTable.setRowSelectionInterval(row, row); // Select the row.
						}
						
						// Update the selected file paths.
						_selectedFilePaths = Arrays.stream(resultTable.getSelectedRows()).mapToObj(selectedRow -> (String) _tableModel.getValueAt(resultTable.convertRowIndexToModel(selectedRow), 0)).collect(Collectors.toList());
						popupMenu.show(resultTable, event.getX(), event.getY()); // Show the context menu.
					}
				}
				else if (event.getClickCount() == 2) // Check for double-click.
				{
					final int row = resultTable.getSelectedRow(); // Get the selected row.
					if (row >= 0) // Ensure a valid row is selected.
					{
						final String filePath = (String) _tableModel.getValueAt(row, 0); // Get the file path from the first column.
						openFile(filePath); // Open the file.
					}
				}
			}
		});
	}
	
	/**
	 * Opens the specified file with the operating system's default application.
	 * @param filePath The path of the file to open.
	 */
	private void openFile(String filePath)
	{
		try
		{
			File file = new File(filePath);
			if (file.exists())
			{
				if (Desktop.isDesktopSupported())
				{
					Desktop.getDesktop().open(file); // Open the file with the default application.
				}
				else
				{
					System.err.println("Desktop is not supported on this platform.");
				}
			}
			else
			{
				System.err.println("File does not exist: " + filePath);
			}
		}
		catch (IOException e)
		{
			System.err.println("Failed to open file: " + filePath);
			e.printStackTrace();
		}
	}
	
	/**
	 * Opens the file's location in the system's file explorer.
	 * @param filePath The path of the file.
	 */
	private void openFileLocation(String filePath)
	{
		try
		{
			final File file = new File(filePath);
			if (file.exists())
			{
				if (Desktop.isDesktopSupported())
				{
					try
					{
						Desktop.getDesktop().browseFileDirectory(file); // Open the file's location.
					}
					catch (Exception e)
					{
						openFile(file.getParent());
					}
				}
				else
				{
					System.err.println("Desktop is not supported on this platform.");
				}
			}
			else
			{
				System.err.println("File does not exist: " + filePath);
			}
		}
		catch (Exception e)
		{
			System.err.println("Failed to open file location: " + filePath);
			e.printStackTrace();
		}
	}
	
	/**
	 * Copies the actual file to the system clipboard,<br>
	 * allowing the user to paste the file into another folder or location using the operating system's file explorer.
	 * @param filePaths The paths to copy to the clipboard.
	 */
	private void copyFile(List<String> filePaths)
	{
		// Create a list of File objects from the file paths.
		final List<File> files = filePaths.stream().map(File::new).collect(Collectors.toList());
		
		// Check if all files exist.
		for (File file : files)
		{
			if (!file.exists())
			{
				System.err.println("File does not exist: " + file.getAbsolutePath());
				return;
			}
		}
		
		// Create a Transferable object to hold the files.
		final Transferable transferable = new Transferable()
		{
			private final List<File> fileList = files;
			
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[]
				{
					DataFlavor.javaFileListFlavor
				};
			}
			
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return DataFlavor.javaFileListFlavor.equals(flavor);
			}
			
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
			{
				if (isDataFlavorSupported(flavor))
				{
					return fileList;
				}
				
				throw new UnsupportedFlavorException(flavor);
			}
		};
		
		// Get the system clipboard and set the transferable content.
		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(transferable, null);
	}
	
	/**
	 * Copies the file path to the system clipboard as a string.<br>
	 * This allows the user to paste the file path into other applications.
	 * @param filePaths The paths to copy to the clipboard.
	 */
	private void copyFileLocation(List<String> filePaths)
	{
		// Join all file paths with a line separator.
		final String filePathsString = String.join(System.lineSeparator(), filePaths);
		
		// Copy the concatenated string to the clipboard.
		final StringSelection stringSelection = new StringSelection(filePathsString);
		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
	
	/**
	 * Displays a dialog with the properties of the specified file,<br>
	 * including its name, size, creation date and last modified date.
	 * @param filePath The path of the file whose properties are to be displayed.
	 */
	private void showProperties(String filePath)
	{
		try
		{
			// Convert the file path to a Path object.
			final Path path = Paths.get(filePath);
			final File file = path.toFile();
			
			// Read file attributes.
			final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
			
			// Format creation and modification times.
			final String creationTime = TimeUtil.getDateTimeString(attributes.creationTime().toMillis());
			final String lastModifiedTime = TimeUtil.getDateTimeString(attributes.lastModifiedTime().toMillis());
			
			// Format file size in a human-readable format.
			final String fileSize = formatFileSize(file.length());
			
			// Display file properties in a dialog.
			final String message = "File Name: " + file.getName() + System.lineSeparator() + "File Size: " + fileSize + System.lineSeparator() + "Creation date: " + creationTime + System.lineSeparator() + "Last Modified: " + lastModifiedTime;
			
			JOptionPane.showMessageDialog(null, message, "File Properties", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to retrieve file properties.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Formats the file size in a human-readable format (e.g., KB, MB, GB).
	 * @param size The file size in bytes.
	 * @return A formatted string representing the file size.
	 */
	private String formatFileSize(long size)
	{
		if (size < 1024)
		{
			return size + " bytes";
		}
		else if (size < (1024 * 1024))
		{
			return String.format("%.2f KB", size / 1024.0);
		}
		else if (size < (1024 * 1024 * 1024))
		{
			return String.format("%.2f MB", size / (1024.0 * 1024.0));
		}
		else
		{
			return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
		}
	}
	
	private void performGuiSearch()
	{
		if (_isSearching)
		{
			// If a search is already in progress, stop it.
			_isSearching = false;
			_searchButton.setText("Search");
			return;
		}
		
		final String regexPattern = _searchField.getText();
		if (regexPattern.isEmpty())
		{
			_summaryLabel.setText("Please enter a search text.");
			return;
		}
		
		if (!_iniCheckBox.isSelected() && !_xmlCheckBox.isSelected() && !_javaCheckBox.isSelected() && !_sqlCheckBox.isSelected() && !_htmlCheckBox.isSelected())
		{
			_allCheckBox.setSelected(true);
		}
		
		// Disable search components.
		_searchField.setEnabled(false);
		_searchButton.setText("  Stop  "); // Change the button text to "Stop".
		_isSearching = true;
		
		// Clear previous results.
		_tableModel.setRowCount(0);
		_totalFilesSearched = 0;
		_totalMatchesFound = 0;
		_filesWithMatches = 0;
		
		// Hide the summary label at the start of the search.
		_summaryLabel.setVisible(false);
		_timeLabel.setVisible(false);
		
		// Update allowed extensions based on checkboxes.
		ALLOWED_EXTENSIONS.clear();
		if (_allCheckBox.isSelected())
		{
			ALLOWED_EXTENSIONS.add(".ini");
			ALLOWED_EXTENSIONS.add(".sql");
			ALLOWED_EXTENSIONS.add(".xml");
			ALLOWED_EXTENSIONS.add(".java");
			ALLOWED_EXTENSIONS.add(".htm");
			ALLOWED_EXTENSIONS.add(".html");
		}
		else
		{
			if (_iniCheckBox.isSelected())
			{
				ALLOWED_EXTENSIONS.add(".ini");
			}
			
			if (_sqlCheckBox.isSelected())
			{
				ALLOWED_EXTENSIONS.add(".sql");
			}
			
			if (_xmlCheckBox.isSelected())
			{
				ALLOWED_EXTENSIONS.add(".xml");
			}
			
			if (_javaCheckBox.isSelected())
			{
				ALLOWED_EXTENSIONS.add(".java");
			}
			
			if (_htmlCheckBox.isSelected())
			{
				ALLOWED_EXTENSIONS.add(".htm");
				ALLOWED_EXTENSIONS.add(".html");
			}
		}
		
		// Check if the directory exists.
		if (!Files.exists(START_DIR) || !Files.isDirectory(START_DIR))
		{
			_summaryLabel.setText("The user directory does not exist or is not a directory.");
			_summaryLabel.setVisible(true); // Show the label if there's an error.
			_timeLabel.setVisible(true);
			return;
		}
		
		_summaryPanel.remove(_summaryLabel);
		_summaryPanel.remove(_timeLabel);
		_summaryLabel = new JLabel("");
		_timeLabel = new JLabel();
		_summaryPanel.add(_summaryLabel);
		_summaryPanel.add(_timeLabel);
		
		// Compile the regex pattern with case-insensitive flag.
		final Pattern pattern = Pattern.compile(regexPattern, _caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
		
		// Update window title to show "Searching for...".
		final String truncatedPattern = regexPattern.length() > 50 ? regexPattern.substring(0, 50) : regexPattern;
		setTitle("Mobius - Searching for " + truncatedPattern + "...");
		final long startTime = System.currentTimeMillis();
		
		// Show progress bar.
		_progressBar.setVisible(true);
		_progressBar.setValue(0);
		
		// Use SwingWorker to perform the search in a background thread.
		SwingWorker<Void, Void> worker = new SwingWorker<>()
		{
			@Override
			protected Void doInBackground()
			{
				try (Stream<Path> stream = Files.walk(START_DIR))
				{
					// Count total files that are not excluded.
					final long totalFiles = Files.walk(START_DIR).filter(Files::isRegularFile).filter(path -> shouldProcessFile(path)).count();
					
					long processedFiles = 0;
					
					for (Path path : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator)
					{
						if (!_isSearching) // Check if search is stopped.
						{
							break;
						}
						
						// Skip ignored files.
						if (!shouldProcessFile(path))
						{
							continue;
						}
						
						_totalFilesSearched++; // Increment the total files searched.
						final int matchCount = countPatternMatches(path, pattern);
						if (matchCount > 0)
						{
							_totalMatchesFound += matchCount; // Add to total matches.
							_filesWithMatches++; // Increment files with matches.
							
							// Add the result to the table.
							SwingUtilities.invokeLater(() ->
							{
								_tableModel.addRow(new Object[]
								{
									path.toAbsolutePath().toString(),
									matchCount
								});
							});
						}
						
						// Update progress.
						processedFiles++;
						final int progress = (int) ((processedFiles * 100) / totalFiles);
						SwingUtilities.invokeLater(() ->
						{
							_progressBar.setValue(progress);
						});
					}
				}
				catch (Exception e)
				{
					SwingUtilities.invokeLater(() ->
					{
						_summaryLabel.setText("An error occurred while searching: " + e.getMessage());
						_summaryLabel.setVisible(true); // Show the label if there's an error.
						_timeLabel.setVisible(true);
					});
				}
				
				return null;
			}
			
			@Override
			protected void done()
			{
				// Re-enable search components.
				_searchField.setEnabled(true);
				_searchButton.setText("Search"); // Change the button text back to "Search".
				_isSearching = false;
				
				// Hide progress bar.
				_progressBar.setVisible(false);
				
				// Show the summary label again.
				_summaryLabel.setVisible(true);
				_timeLabel.setVisible(true);
				
				// Update the summary labels.
				if (_filesWithMatches == 0)
				{
					_summaryLabel.setText("No files matching the text were found.");
				}
				else
				{
					_summaryLabel.setText("From " + _totalFilesSearched + " files searched, found " + _totalMatchesFound + " matches in " + _filesWithMatches + " files.");
				}
				
				_timeLabel.setText("Search complete in " + TimeUtil.formatDuration(System.currentTimeMillis() - startTime) + ".");
				
				// Reset window title.
				setTitle("Mobius - Search");
			}
		};
		
		worker.execute();
	}
	
	private void console()
	{
		System.out.println("=========================================================");
		System.out.println("             L2jMobius Development - Search              ");
		System.out.println("=========================================================");
		
		final Scanner scanner = new Scanner(System.in);
		System.out.print("Enter search text (regex supported): ");
		final String regexPattern = scanner.nextLine();
		scanner.close();
		
		// Check if the directory exists.
		if (!Files.exists(START_DIR) || !Files.isDirectory(START_DIR))
		{
			System.err.println("The user directory does not exist or is not a directory.");
			return;
		}
		
		// Compile the regex pattern with case-insensitive flag.
		final Pattern pattern = Pattern.compile(regexPattern, _caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
		
		// Reset static variables before starting the search.
		_totalFilesSearched = 0;
		_totalMatchesFound = 0;
		_filesWithMatches = 0;
		
		final String truncatedPattern = regexPattern.length() > 50 ? regexPattern.substring(0, 50) : regexPattern;
		System.out.println("Searching for " + truncatedPattern + "...");
		final long startTime = System.currentTimeMillis();
		
		// Walk through the directory and search for files matching the regex pattern.
		try (Stream<Path> stream = Files.walk(START_DIR))
		{
			stream.filter(Files::isRegularFile) // Only consider regular files.
				.filter(path -> shouldProcessFile(path)) // Skip ignored folders or files and disallowed extensions.
				.forEach(path ->
				{
					_totalFilesSearched++; // Increment the total files searched.
					final int matchCount = countPatternMatches(path, pattern);
					if (matchCount > 0)
					{
						_totalMatchesFound += matchCount; // Add to total matches.
						_filesWithMatches++; // Increment files with matches.
						
						// Output the result instantly.
						System.out.println(path.toAbsolutePath() + " - " + matchCount + " matches");
					}
				});
		}
		catch (Exception e)
		{
			System.err.println("An error occurred while searching: " + e.getMessage());
		}
		
		// Print the final summary.
		if (_filesWithMatches == 0)
		{
			System.out.println("No files matching the text were found.");
		}
		else
		{
			System.out.println("From " + _totalFilesSearched + " files searched, found " + _totalMatchesFound + " matches in " + _filesWithMatches + " files.");
		}
		
		System.out.println("Search complete in " + TimeUtil.formatDuration(System.currentTimeMillis() - startTime) + ".");
	}
	
	/**
	 * Checks if a file should be processed based on the ignore list and allowed extensions.
	 * @param path The path to check.
	 * @return True if the file should be processed, false otherwise.
	 */
	private boolean shouldProcessFile(Path path)
	{
		// Check if the file should be ignored.
		final String pathString = path.toString();
		for (String ignore : IGNORE_LIST)
		{
			if (pathString.contains(ignore))
			{
				return false;
			}
		}
		
		// Check if the file has an allowed extension.
		if (!ALLOWED_EXTENSIONS.isEmpty())
		{
			final String fileName = path.getFileName().toString().toLowerCase();
			for (String extention : ALLOWED_EXTENSIONS)
			{
				if (fileName.endsWith(extention))
				{
					return true;
				}
			}
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Counts the number of times the specified regex pattern appears in the file.
	 * @param file The file to check.
	 * @param pattern The compiled regex pattern.
	 * @return The number of matches found in the file.
	 */
	private int countPatternMatches(Path file, Pattern pattern)
	{
		try
		{
			// Read the file content with UTF-8 encoding.
			final String content = Files.readString(file, StandardCharsets.UTF_8);
			final Matcher matcher = pattern.matcher(content);
			int matchCount = 0;
			while (matcher.find())
			{
				matchCount++;
			}
			
			return matchCount;
		}
		catch (IOException e)
		{
			// System.err.println("Could not read file: " + file.toAbsolutePath() + " - " + e.getMessage());
			return 0;
		}
	}
	
	/**
	 * Main method to run the search tool.
	 * @param args Command-line arguments. Supported arguments:<br>
	 *            - `-ext`: Comma-separated list of file extensions to search (e.g., `-ext=java,xml,html`).<br>
	 *            - `-caseSensitive`: Disable case-insensitive search (case-sensitive search will be performed).
	 */
	public static void main(String[] args)
	{
		// Parse command-line arguments.
		for (String arg : args)
		{
			if (arg.startsWith("-ext="))
			{
				final String extensions = arg.substring("-ext=".length());
				
				// Split the extensions by comma, prepend a ".", and add to the ALLOWED_EXTENSIONS set. Prepend "." and convert to lowercase.
				ALLOWED_EXTENSIONS.addAll(Arrays.stream(extensions.split(",")).map(ext -> "." + ext.toLowerCase()).collect(Collectors.toSet()));
			}
			else if (arg.equalsIgnoreCase("-caseSensitive"))
			{
				_caseInsensitive = false; // Disable case-insensitive search.
			}
		}
		
		SwingUtilities.invokeLater(Search::new);
	}
}
