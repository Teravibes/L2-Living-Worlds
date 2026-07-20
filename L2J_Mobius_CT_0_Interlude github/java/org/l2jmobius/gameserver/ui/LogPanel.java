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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;

/**
 * @author Skache
 */
public class LogPanel extends JFrame
{
	private static final Logger LOGGER = Logger.getLogger(LogPanel.class.getName());
	
	private static final String ROOT_LOG_PATH = new File("log").getAbsolutePath();
	private String _currentLogPath = ROOT_LOG_PATH;
	
	private final JTextArea _logTextArea = new JTextArea();
	private JComboBox<String> _fileComboBox;
	private JTextField _searchField;
	private JButton _searchButton;
	private JProgressBar _progressBar;
	private JLabel _fileLabel;
	private JLabel _fileSizeLabel;
	
	private List<Integer> _searchIndexes = new ArrayList<>();
	private int _currentSearchIndex = -1;
	
	protected LogPanel(boolean deleteMode)
	{
		if (InterfaceConfig.DARK_THEME)
		{
			DarkTheme.activate();
		}
		
		setTitle(deleteMode ? "Mobius - Delete Log File" : "Mobius - Log Viewer");
		setMinimumSize(deleteMode ? new Dimension(400, 200) : new Dimension(1000, 600));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		// Create the top panel based on mode.
		final JPanel topPanel = createTopPanel(deleteMode);
		add(topPanel, BorderLayout.NORTH);
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		
		if (!deleteMode)
		{
			_logTextArea.setEditable(false);
			_logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
			final JScrollPane scrollPane = new JScrollPane(_logTextArea);
			add(scrollPane, BorderLayout.CENTER);
		}
		
		setLocationRelativeTo(null);
		setIconImages(icons);
		setVisible(true);
	}
	
	private JPanel createTopPanel(boolean deleteMode)
	{
		final JPanel topPanel = new JPanel();
		
		if (deleteMode)
		{
			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
			topPanel.add(Box.createRigidArea(new Dimension(0, 20)));
			
			// Create and add progress bar.
			_progressBar = new JProgressBar(0, 100);
			_progressBar.setStringPainted(true);
			_progressBar.setPreferredSize(new Dimension(160, 25));
			_progressBar.setMaximumSize(new Dimension(160, 25));
			_progressBar.setMinimumSize(new Dimension(160, 25));
			_progressBar.setVisible(true);
			
			topPanel.add(_progressBar);
			topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
			
			// Create middle panel for dropdown and delete button.
			final JPanel middlePanel = new JPanel();
			middlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
			
			// Log file dropdown.
			_fileComboBox = new JComboBox<>();
			loadLogFiles();
			_fileComboBox.setPreferredSize(new Dimension(180, 28));
			_fileComboBox.setToolTipText("Select a log file.");
			
			// Don't load contents if deleteMode is active.
			_fileComboBox.addActionListener(_ ->
			{
				if (!deleteMode)
				{
					loadLogs();
				}
			});
			
			middlePanel.add(new JLabel("Log File: "));
			middlePanel.add(_fileComboBox);
			
			// Delete button.
			final JButton deleteButton = new JButton("❌ Delete Log");
			deleteButton.setToolTipText("Click to delete the selected log file.");
			deleteButton.setBackground(new Color(220, 53, 69));
			deleteButton.setForeground(Color.WHITE);
			deleteButton.setPreferredSize(new Dimension(110, 30));
			deleteButton.setEnabled(false);
			deleteButton.addActionListener(_ ->
			{
				// Show the progress bar when delete action starts.
				_progressBar.setValue(0);
				SwingUtilities.invokeLater(() ->
				{
					deleteLogFiles(_progressBar);
				});
			});
			
			_fileComboBox.addActionListener(_ ->
			{
				final String selectedFile = (String) _fileComboBox.getSelectedItem();
				if (selectedFile == null)
				{
					return;
				}
				
				if (selectedFile.equals("..//"))
				{
					navigateBack();
					return;
				}
				
				final File selectedPath = new File(_currentLogPath, selectedFile);
				if (selectedPath.isDirectory())
				{
					_currentLogPath = selectedPath.getAbsolutePath();
					loadLogFiles();
					deleteButton.setEnabled(false);
				}
				else
				{
					// Enable delete button only for files.
					deleteButton.setEnabled(true);
					if (selectedPath.exists() && selectedPath.isFile())
					{
						long fileSize = selectedPath.length();
						String humanReadableSize = formatFileSize(fileSize);
						_fileLabel.setText("Selected File: " + selectedPath.getName());
						_fileSizeLabel.setText("Size: " + humanReadableSize);
					}
				}
			});
			
			// Floating label panel for selected file.
			final JPanel floatingPanel = new JPanel();
			floatingPanel.setLayout(new GridBagLayout());
			floatingPanel.setBackground(new Color(230, 230, 230));
			floatingPanel.setPreferredSize(new Dimension(200, 30));
			floatingPanel.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true));
			
			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.CENTER;
			
			_fileLabel = new JLabel("Selected File: None");
			_fileLabel.setFont(new Font("Arial", Font.BOLD, 12));
			_fileLabel.setForeground(Color.DARK_GRAY);
			_fileLabel.setVerticalAlignment(SwingConstants.CENTER);
			
			// File size label.
			_fileSizeLabel = new JLabel("Size: (0 KB)");
			_fileSizeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
			_fileSizeLabel.setForeground(Color.GRAY);
			_fileSizeLabel.setHorizontalAlignment(SwingConstants.CENTER);
			
			// Add components to the floating panel.
			floatingPanel.add(_fileLabel, gbc);
			
			gbc.gridy = 1;
			floatingPanel.add(_fileSizeLabel, gbc);
			
			// Smooth transition effect for changing the file.
			_fileComboBox.addActionListener(_ ->
			{
				final String selectedFile = (String) _fileComboBox.getSelectedItem();
				new Thread(() ->
				{
					for (float i = 1.0f; i >= 0.0f; i -= 0.1f)
					{
						_fileLabel.setForeground(new Color(0, 0, 0, i)); // Fade out.
						try
						{
							Thread.sleep(50);
						}
						catch (InterruptedException ex)
						{
						}
					}
					
					SwingUtilities.invokeLater(() ->
					{
						_fileLabel.setText("Selected File: " + selectedFile);
					});
					
					for (float i = 0.0f; i <= 1.0f; i += 0.1f)
					{
						_fileLabel.setForeground(new Color(0, 0, 0, i)); // Fade in.
						try
						{
							Thread.sleep(50);
						}
						catch (InterruptedException ex)
						{
						}
					}
				}).start();
			});
			
			topPanel.add(floatingPanel);
			topPanel.add(middlePanel);
			middlePanel.add(deleteButton);
			floatingPanel.add(_fileLabel);
			
		}
		else
		{
			topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
			topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			// Log file dropdown.
			_fileComboBox = new JComboBox<>();
			loadLogFiles();
			_fileComboBox.setPreferredSize(new Dimension(180, 28));
			_fileComboBox.setToolTipText("Select a log file.");
			
			// If we're not in deleteMode, load logs when selected.
			_fileComboBox.addActionListener(_ -> loadLogs());
			
			topPanel.add(new JLabel("Log File: "));
			topPanel.add(_fileComboBox);
			
			// Search field and button for reading logs.
			if (!deleteMode)
			{
				_searchField = new JTextField(15);
				_searchField.setToolTipText("Enter search term.");
				_searchField.setPreferredSize(new Dimension(150, 28));
				_searchField.addActionListener(_ -> searchLogs());
				
				_searchButton = new JButton("🔍");
				_searchButton.setToolTipText("Click to search.");
				_searchButton.setPreferredSize(new Dimension(50, 28));
				_searchButton.addActionListener(_ -> searchLogs());
				
				topPanel.add(new JLabel("🔍 Search: "));
				topPanel.add(_searchField);
				topPanel.add(_searchButton);
				
				// Progress bar for reading logs.
				_progressBar = new JProgressBar(0, 100);
				_progressBar.setStringPainted(true);
				_progressBar.setPreferredSize(new Dimension(160, 25));
				_progressBar.setMaximumSize(new Dimension(160, 25));
				_progressBar.setMinimumSize(new Dimension(160, 25));
				_progressBar.setVisible(true);
				
				topPanel.add(_progressBar);
			}
		}
		
		return topPanel;
	}
	
	private void loadLogFiles()
	{
		final File logDirectory = new File(_currentLogPath);
		if (!logDirectory.exists())
		{
			JOptionPane.showMessageDialog(this, "Log directory not found: " + logDirectory.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
			LOGGER.warning(getClass().getName() + ": Log directory not found: " + logDirectory.getAbsolutePath());
			return;
		}
		
		_fileComboBox.removeAllItems();
		listLogFiles(logDirectory);
	}
	
	private void deleteLogFiles(JProgressBar progressBar)
	{
		String selectedFile = (String) _fileComboBox.getSelectedItem();
		if (selectedFile == null)
		{
			JOptionPane.showMessageDialog(this, "No log file selected.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + selectedFile + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION)
		{
			final File logFile = new File(ROOT_LOG_PATH, selectedFile);
			if (logFile.isDirectory())
			{
				JOptionPane.showMessageDialog(this, "Cannot delete a directory. Please select a log file.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			if (logFile.exists())
			{
				_progressBar.setValue(0);
				_progressBar.setStringPainted(true);
				_progressBar.setString("Deleting...");
				
				final SwingWorker<Void, Void> worker = new SwingWorker<>()
				{
					@Override
					protected Void doInBackground() throws Exception
					{
						for (int i = 0; i <= 100; i += 25)
						{
							Thread.sleep(100);
							setProgress(i);
						}
						
						if (!logFile.delete())
						{
							throw new Exception("Failed to delete the file.");
						}
						
						return null;
					}
					
					@Override
					protected void done()
					{
						try
						{
							get();
							Toolkit.getDefaultToolkit().beep();
							JOptionPane.showMessageDialog(null, "Log file deleted successfully.");
							_fileComboBox.removeItem(selectedFile);
							_progressBar.setValue(100);
							_progressBar.setString("Done!");
						}
						catch (Exception e)
						{
							JOptionPane.showMessageDialog(null, "Error deleting log file.", "Error", JOptionPane.ERROR_MESSAGE);
							_progressBar.setString("Error!");
						}
					}
				};
				
				worker.addPropertyChangeListener(event ->
				{
					if ("progress".equals(event.getPropertyName()))
					{
						_progressBar.setValue((Integer) event.getNewValue());
					}
				});
				
				worker.execute();
			}
			else
			{
				JOptionPane.showMessageDialog(this, "File not found: " + selectedFile, "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void listLogFiles(File directory)
	{
		_fileComboBox.removeAllItems();
		if (!_currentLogPath.equals(ROOT_LOG_PATH))
		{
			_fileComboBox.addItem("..//");
		}
		
		final File[] files = directory.listFiles();
		if (files != null)
		{
			Arrays.sort(files, (f1, f2) ->
			{
				if (f1.isDirectory() && !f2.isDirectory())
				{
					return -1;
				}
				
				if (!f1.isDirectory() && f2.isDirectory())
				{
					return 1;
				}
				
				return f1.getName().compareToIgnoreCase(f2.getName());
			});
			
			for (File file : files)
			{
				if (file.isDirectory())
				{
					_fileComboBox.addItem(file.getName() + "/");
				}
				else if (file.isFile() && (file.getName().endsWith(".log") || file.getName().endsWith(".txt")))
				{
					_fileComboBox.addItem(file.getName());
				}
			}
		}
		
		_fileComboBox.setRenderer(new FileComboBoxRenderer());
	}
	
	private void loadLogs()
	{
		final String selectedItem = (String) _fileComboBox.getSelectedItem();
		if ((selectedItem == null) || selectedItem.isEmpty())
		{
			return;
		}
		
		if (selectedItem.equals("..//"))
		{
			navigateBack();
			return;
		}
		
		if (selectedItem.endsWith("/"))
		{
			final String folderName = selectedItem.substring(0, selectedItem.length() - 1);
			final File folder = new File(_currentLogPath + File.separator + folderName);
			if (folder.exists() && folder.isDirectory())
			{
				_currentLogPath = folder.getAbsolutePath();
				listLogFiles(folder);
			}
			return;
		}
		
		final File logFile = new File(_currentLogPath + File.separator + selectedItem);
		if (!logFile.exists() || !logFile.isFile())
		{
			JOptionPane.showMessageDialog(this, "Warning: The selected log file does not exist.", "File Not Found", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		_searchIndexes = new ArrayList<>();
		_currentSearchIndex = -1;
		
		// Disable UI components while loading.
		_searchButton.setEnabled(false);
		_fileComboBox.setEnabled(false);
		_progressBar.setValue(0);
		_progressBar.setStringPainted(true);
		
		// Load the file in the background.
		final SwingWorker<String, Integer> worker = new SwingWorker<>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				final StringBuilder logContent = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new FileReader(logFile)))
				{
					String line;
					long bytesRead = 0;
					int lastReportedProgress = 0;
					final long fileSize = logFile.length();
					while ((line = reader.readLine()) != null)
					{
						logContent.append(line).append("\n");
						bytesRead += line.length();
						final int progress = (int) ((bytesRead * 100) / fileSize);
						
						// Update progress every 25%.
						if (progress >= (lastReportedProgress + 25))
						{
							publish(progress);
							lastReportedProgress = progress;
						}
					}
				}
				
				return logContent.toString();
			}
			
			@Override
			protected void process(List<Integer> chunks)
			{
				final int latestProgress = chunks.get(chunks.size() - 1);
				_progressBar.setValue(latestProgress);
				_progressBar.setString(latestProgress + "%");
			}
			
			@Override
			protected void done()
			{
				try
				{
					final String content = get();
					_logTextArea.setText(content);
					_logTextArea.setCaretPosition(0);
					if (content.isEmpty())
					{
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(null, "The selected log file is empty.", "Empty Log", JOptionPane.INFORMATION_MESSAGE);
					}
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(null, "Error reading log file: " + selectedItem, "Error", JOptionPane.ERROR_MESSAGE);
					LOGGER.warning(getClass().getName() + ": Error reading log file: " + selectedItem + " - " + e.getMessage());
				}
				finally
				{
					// Re-enable UI components.
					_searchButton.setEnabled(true);
					_fileComboBox.setEnabled(true);
					_progressBar.setValue(100);
					_progressBar.setString("Done!");
				}
			}
		};
		
		worker.execute(); // Start the background task.
	}
	
	private void searchLogs()
	{
		final String searchTerm = _searchField.getText().trim();
		if (searchTerm.isEmpty())
		{
			return;
		}
		
		clearHighlights();
		
		// Disable UI while searching.
		_searchButton.setEnabled(false);
		_progressBar.setVisible(true);
		_progressBar.setValue(0);
		_progressBar.setStringPainted(true);
		
		final SwingWorker<List<Integer>, Integer> worker = new SwingWorker<>()
		{
			@Override
			protected List<Integer> doInBackground()
			{
				final List<Integer> indexes = new ArrayList<>();
				final String logContent = _logTextArea.getText();
				if (logContent.isEmpty())
				{
					return indexes;
				}
				
				final String lowerLog = logContent.toLowerCase();
				final String lowerSearch = searchTerm.toLowerCase();
				final int totalLength = logContent.length();
				int index = lowerLog.indexOf(lowerSearch);
				int processedChars = 0;
				int lastReportedProgress = 0;
				while (index >= 0)
				{
					indexes.add(index);
					processedChars += index + searchTerm.length();
					
					// Update progress less frequently.
					final int progress = (int) ((processedChars * 100.0) / totalLength);
					if (progress >= (lastReportedProgress + 25))
					{
						publish(progress);
						lastReportedProgress = progress;
					}
					
					index = lowerLog.indexOf(lowerSearch, index + 1);
				}
				
				return indexes;
			}
			
			@Override
			protected void process(List<Integer> chunks)
			{
				if (!chunks.isEmpty())
				{
					final int latestProgress = chunks.get(chunks.size() - 1);
					_progressBar.setValue(latestProgress);
					_progressBar.setString(latestProgress + "%");
					_progressBar.repaint();
				}
			}
			
			@Override
			protected void done()
			{
				try
				{
					_searchIndexes = get();
					if (_searchIndexes.isEmpty())
					{
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(_searchButton.getParent(), "No matches found for: " + searchTerm, "Search Results", JOptionPane.INFORMATION_MESSAGE);
						_currentSearchIndex = -1;
					}
					else
					{
						_currentSearchIndex = (_currentSearchIndex + 1) % _searchIndexes.size();
						highlightSearchResult(_currentSearchIndex);
					}
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(_searchButton.getParent(), "Error during search: " + e.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
					LOGGER.warning(getClass().getName() + ": Error during search: " + e.getMessage());
				}
				finally
				{
					// Re-enable UI
					_searchButton.setEnabled(true);
					_progressBar.setValue(100);
					_progressBar.setString("100%");
				}
			}
		};
		
		worker.execute(); // Run the search in the background.
	}
	
	private void highlightSearchResult(int index)
	{
		if (_searchIndexes.isEmpty())
		{
			return;
		}
		
		final int start = _searchIndexes.get(index);
		final int end = start + _searchField.getText().length();
		final Highlighter highlighter = _logTextArea.getHighlighter();
		final Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.MAGENTA);
		try
		{
			highlighter.addHighlight(start, end, painter);
		}
		catch (BadLocationException e)
		{
			LOGGER.warning(getClass().getName() + ": Bad location exception. " + e.getMessage());
		}
		
		_logTextArea.setCaretPosition(start);
		_logTextArea.select(start, end);
	}
	
	private void clearHighlights()
	{
		final Highlighter highlighter = _logTextArea.getHighlighter();
		highlighter.removeAllHighlights();
	}
	
	private void refreshComboBox()
	{
		_fileComboBox.removeAllItems();
		
		final File currentDir = new File(_currentLogPath);
		if (!_currentLogPath.equals(ROOT_LOG_PATH))
		{
			_fileComboBox.addItem("..//");
		}
		
		final File[] files = currentDir.listFiles();
		if (files != null)
		{
			Arrays.sort(files, (f1, f2) ->
			{
				if (f1.isDirectory() && !f2.isDirectory())
				{
					return -1;
				}
				
				if (!f1.isDirectory() && f2.isDirectory())
				{
					return 1;
				}
				
				return f1.getName().compareToIgnoreCase(f2.getName());
			});
			
			for (File file : files)
			{
				if (file.isDirectory())
				{
					_fileComboBox.addItem(file.getName() + "/");
				}
				else if (file.isFile() && (file.getName().endsWith(".log") || file.getName().endsWith(".txt")))
				{
					_fileComboBox.addItem(file.getName());
				}
			}
		}
		
		_fileComboBox.setRenderer(new FileComboBoxRenderer());
	}
	
	private void navigateBack()
	{
		final File currentDir = new File(_currentLogPath);
		final File parentDir = currentDir.getParentFile();
		if ((parentDir != null) && parentDir.getAbsolutePath().equals(ROOT_LOG_PATH))
		{
			_currentLogPath = ROOT_LOG_PATH; // Stop at the root folder.
			refreshComboBox(); // Refresh UI.
		}
		else if ((parentDir != null) && !_currentLogPath.equals(ROOT_LOG_PATH))
		{
			_currentLogPath = parentDir.getAbsolutePath();
			refreshComboBox(); // Refresh UI.
		}
	}
	
	private String formatFileSize(long size)
	{
		if (size >= (1024 * 1024))
		{
			return String.format("%.2f MB", size / (1024.0 * 1024.0));
		}
		else if (size >= 1024)
		{
			return String.format("%.2f KB", size / 1024.0);
		}
		else
		{
			return size + " Bytes";
		}
	}
	
	private class FileComboBoxRenderer extends DefaultListCellRenderer
	{
		private final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
		private final Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
		
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value != null)
			{
				final String text = value.toString();
				if (text.endsWith("/"))
				{
					label.setIcon(folderIcon);
					label.setText(text.substring(0, text.length() - 1));
				}
				else
				{
					label.setIcon(fileIcon);
				}
			}
			
			return label;
		}
	}
	
	public static LogPanel getInstance(boolean deleteMode)
	{
		return SingletonHolder.getInstance(deleteMode);
	}
	
	private static class SingletonHolder
	{
		private static LogPanel instance;
		
		public static LogPanel getInstance(boolean deleteMode)
		{
			if (instance == null)
			{
				instance = new LogPanel(deleteMode);
			}
			
			return instance;
		}
	}
}
