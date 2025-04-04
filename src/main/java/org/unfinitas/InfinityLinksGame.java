package org.unfinitas;

import org.unfinitas.dto.Level;
import org.unfinitas.generator.PuzzleGenerator;
import org.unfinitas.ui.BoardPanel;
import org.unfinitas.ui.HintButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class: Runs a Swing window with a "Generate Puzzle" button,
 * a board panel to play the generated puzzle, and now a hint button.
 */
public class InfinityLinksGame extends JFrame {
    private BoardPanel boardPanel;
    private HintButton hintButton;
    private JLabel statusLabel;
    private final ExecutorService puzzleGenerator = Executors.newSingleThreadExecutor();
    private JPanel topPanel;

    // Track current difficulty to show appropriate message for hints
    private int currentDifficulty = 1; // Default medium

    public InfinityLinksGame() {
        super("Infinity Links – Flow Puzzle Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel with buttons and difficulty options (without hint button for now)
        topPanel = createTopPanel(false);
        add(topPanel, BorderLayout.NORTH);

        // Create an initial empty board panel
        boardPanel = new BoardPanel(null);
        add(boardPanel, BorderLayout.CENTER);

        // Add bottom panel with instructions
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(600, 650);
        setLocationRelativeTo(null);

        // Clean up executor on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                puzzleGenerator.shutdown();
            }
        });
    }

    /**
     * Creates the top panel with controls.
     *
     * @param includeHintButton Whether to include the hint button
     */
    private JPanel createTopPanel(boolean includeHintButton) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Generate button
        JButton generateButton = new JButton("Generate Puzzle");

        // Difficulty selector
        JComboBox<String> difficultyCombo = new JComboBox<>(new String[]{
                "Easy (4x4, 2 pairs)",
                "Medium (5x5, 4 pairs)",
                "Hard (8x8, 4 pairs)"
        });
        difficultyCombo.setSelectedIndex(currentDifficulty);

        // Unique solution checkbox
        JCheckBox uniqueCheckbox = new JCheckBox("Unique Solution");

        // Status label
        statusLabel = new JLabel("Ready");

        // Setup generate button logic
        generateButton.addActionListener(e -> {
            currentDifficulty = difficultyCombo.getSelectedIndex();
            int[] config = switch (currentDifficulty) {
                case 0 -> new int[]{3, 3, 2}; // Easy
                case 2 -> new int[]{8, 8, 4}; // Hard
                default -> new int[]{5, 5, 4}; // Medium
            };

            boolean requireUniqueSolution = uniqueCheckbox.isSelected();
            generateNewPuzzle(config[0], config[1], config[2], requireUniqueSolution);
        });

        // Add all components to panel
        panel.add(generateButton);
        panel.add(new JLabel("Difficulty:"));
        panel.add(difficultyCombo);
        panel.add(uniqueCheckbox);

        // Only add hint button if requested
        if (includeHintButton && hintButton != null) {
            panel.add(hintButton);
        }

        panel.add(Box.createHorizontalStrut(20));
        panel.add(statusLabel);

        return panel;
    }

    /**
     * Creates the bottom panel with instructions.
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JLabel("Connect matching color dots with paths. Fill the entire board."));
        return bottomPanel;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new InfinityLinksGame().setVisible(true));
    }

    /**
     * Generates a new puzzle with the specified parameters,
     * then replaces the old BoardPanel with a new one.
     */
    private void generateNewPuzzle(
            int rows,
            int cols,
            int numberOfPairs,
            boolean requireUniqueSolution
    ) {
        // Disable UI during generation
        statusLabel.setText("Generating puzzle...");
        statusLabel.setForeground(Color.BLUE);
        if (hintButton != null) {
            hintButton.setEnabled(false);
        }

        // Generate puzzle in background thread
        puzzleGenerator.submit(() -> {
            try {
                // Generate a puzzle
                Level puzzle = PuzzleGenerator.generatePuzzle(rows, cols, numberOfPairs, requireUniqueSolution);

                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    // Rebuild the board panel
                    remove(boardPanel);
                    boardPanel = new BoardPanel(puzzle);
                    add(boardPanel, BorderLayout.CENTER);

                    // Create hint button after board has a valid model
                    if (hintButton == null) {
                        hintButton = new HintButton(boardPanel);

                        // Rebuild top panel to include hint button
                        remove(topPanel);
                        topPanel = createTopPanel(true);
                        add(topPanel, BorderLayout.NORTH);
                    } else {
                        // Update hint button to use new board panel
                        hintButton.updateBoardPanel(boardPanel);
                        hintButton.setEnabled(true);
                    }

                    statusLabel.setText("Puzzle ready!");
                    statusLabel.setForeground(Color.BLACK);
                    revalidate();
                    repaint();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                });
            }
        });
    }
}
