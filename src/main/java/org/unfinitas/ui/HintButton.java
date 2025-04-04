package org.unfinitas.ui;

import org.unfinitas.dto.Level;
import org.unfinitas.model.BoardModel;
import org.unfinitas.solver.PuzzleSolver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * Button component that provides hints to the player.
 * Debug version with extra logging.
 */
public class HintButton extends JButton implements ActionListener {
    private BoardPanel boardPanel;

    /**
     * Creates a new hint button.
     *
     * @param boardPanel The board panel to provide hints for
     */
    public HintButton(BoardPanel boardPanel) {
        super("Hint");
        this.boardPanel = boardPanel;
        System.out.println("HintButton created with boardPanel: " + boardPanel);

        setToolTipText("Get a hint for your next move");
        setFocusable(false);
        addActionListener(this);
    }

    /**
     * Updates the board panel reference.
     * This is needed when a new puzzle is generated.
     *
     * @param boardPanel The new board panel
     */
    public void updateBoardPanel(BoardPanel boardPanel) {
        System.out.println("HintButton.updateBoardPanel called with: " + boardPanel);
        this.boardPanel = boardPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("HintButton.actionPerformed called");
        provideHint();
    }

    /**
     * Uses the puzzle solver to provide a hint.
     */
    private void provideHint() {
        System.out.println("HintButton.provideHint called with boardPanel: " + boardPanel);

        // Check if we have a valid board panel and model
        if (boardPanel == null) {
            System.out.println("ERROR: boardPanel is null");
            JOptionPane.showMessageDialog(
                    this,
                    "No active puzzle to provide hints for.",
                    "Hint",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        BoardModel boardModel = boardPanel.getModel();
        System.out.println("BoardModel from boardPanel: " + boardModel);

        if (boardModel == null) {
            System.out.println("ERROR: boardModel is null");
            JOptionPane.showMessageDialog(
                    this,
                    "No active puzzle to provide hints for.",
                    "Hint",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Get level from board model
        Level level = boardModel.getLevel();
        System.out.println("Level from boardModel: " + level);

        if (level == null) {
            System.out.println("ERROR: level is null");
            JOptionPane.showMessageDialog(
                    this,
                    "No valid puzzle level found.",
                    "Hint",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Run in background to avoid UI freezing
        SwingWorker<Map.Entry<Color, Point>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map.Entry<Color, Point> doInBackground() {
                try {
                    System.out.println("Getting board array from model");
                    // Convert board model to color array for solver
                    Color[][] boardArray = boardModel.getBoardArray();
                    System.out.println("Board array: " + (boardArray != null ? boardArray.length + "x" + boardArray[0].length : "null"));

                    System.out.println("Calling PuzzleSolver.getHint");
                    return PuzzleSolver.getHint(level, boardArray);
                } catch (Exception ex) {
                    System.out.println("Exception in doInBackground: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Map.Entry<Color, Point> hint = get();
                    System.out.println("Hint received: " + hint);

                    if (hint != null) {
                        displayHint(hint.getKey(), hint.getValue());
                    } else {
                        JOptionPane.showMessageDialog(
                                boardPanel,
                                "Sorry, no hint available at this time.",
                                "Hint",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    System.out.println("Exception in done: " + ex.getMessage());
                    JOptionPane.showMessageDialog(
                            boardPanel,
                            "Error generating hint: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    ex.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        System.out.println("Executing SwingWorker");
        worker.execute();
    }

    /**
     * Displays a hint to the user by highlighting the suggested cell.
     *
     * @param color The color to continue
     * @param point The suggested next point
     */
    private void displayHint(Color color, Point point) {
        System.out.println("Displaying hint: color=" + color + ", point=" + point);

        // Notify the board panel about the hint
        boardPanel.showHint(color, point);

        // Also show a tooltip with text instructions
        JOptionPane.showMessageDialog(
                boardPanel,
                String.format("Try continuing the %s path to row %d, column %d",
                        getColorName(color), point.x + 1, point.y + 1),
                "Hint",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Gets a user-friendly name for a color.
     */
    private String getColorName(Color color) {
        if (color.equals(Color.RED)) return "red";
        if (color.equals(Color.GREEN)) return "green";
        if (color.equals(Color.BLUE)) return "blue";
        if (color.equals(Color.YELLOW)) return "yellow";
        if (color.equals(Color.ORANGE)) return "orange";
        if (color.equals(Color.MAGENTA)) return "magenta";
        if (color.equals(Color.CYAN)) return "cyan";
        if (color.equals(new Color(128, 0, 128))) return "purple";
        if (color.equals(new Color(165, 42, 42))) return "brown";
        if (color.equals(new Color(0, 128, 128))) return "teal";

        // Default case
        return "colored";
    }
}
