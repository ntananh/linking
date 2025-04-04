package org.unfinitas.ui;

import org.unfinitas.dto.Level;
import org.unfinitas.model.BoardModel;
import org.unfinitas.render.BoardRenderer;
import org.unfinitas.util.PathValidator;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * The board panel that allows the user to draw paths between anchors.
 * Once all paths are placed and the board is full, the puzzle is solved.
 * Now includes hint support.
 */
public class BoardPanel extends JPanel implements BoardModel.BoardStateListener {
    private static final String WIN_MESSAGE = "Puzzle Solved! All pairs connected.";
    private static final String WIN_TITLE = "Congratulations";
    private static final String OVERLAP_ERROR = "Path overlaps another color. Try again.";
    private static final String UNSOLVABLE_ERROR = "This path blocks other connections. Try a different route.";
    private static final int TOOLTIP_DELAY = 1500;
    private static final Color BOARD_BACKGROUND = new Color(40, 44, 52);

    // Hint animation fields
    private Point hintPoint = null;
    private Color hintColor = null;
    private Timer hintAnimationTimer = null;
    private boolean hintVisible = false;
    private static final int HINT_BLINK_RATE = 500; // milliseconds

    private final Level level;
    private final BoardModel model;
    private final BoardRenderer renderer;
    private final PathValidator validator;

    private Color currentColor = null;
    private final List<Point> currentPath = new ArrayList<>();
    private final Set<Color> completedColors = new HashSet<>();
    private final Map<Color, List<Point>> connectedPath = new HashMap<>();

    /**
     * Creates a new board panel for the given level.
     *
     * @param level The puzzle level to display, or null for empty board
     */
    public BoardPanel(Level level) {
        this.level = level;
        setBackground(BOARD_BACKGROUND);

        if (level == null) {
            this.model = null;
            this.renderer = null;
            this.validator = null;
            setPreferredSize(new Dimension(600, 600));
            return;
        }

        this.model = new BoardModel(level);
        this.renderer = new BoardRenderer(model);
        this.validator = new PathValidator(model);

        // Register for board state changes
        this.model.addBoardStateListener(this);

        int cellSize = BoardRenderer.getCellSize();
        setPreferredSize(new Dimension(level.cols() * cellSize, level.rows() * cellSize));

        setupMouseListeners();
    }

    private void setupMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased();
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    /**
     * Handles mouse press events to start drawing paths.
     */

    private void handleMousePressed(MouseEvent e) {
        // Cancel any active hint
        cancelHint();
        Point gridPt = toGrid(e.getPoint());
        if (!model.inBounds(gridPt)) return;

        // Check if user clicked on an anchor
        Color cellColor = model.getCellColor(gridPt);
        if (cellColor != null && model.isAnchor(gridPt)) {
            // Debug: Print out the current connected paths
            System.out.println("Connected Paths: " + connectedPath);

            // Retrieve the old path for the clicked color
            var oldPath = connectedPath.get(cellColor);

            // Debug: Print out the retrieved old path
            System.out.println("Old Path for color " + cellColor + ": " + oldPath);

            // If a path of the same color already exists, remove it
            if (oldPath != null && !oldPath.isEmpty()) {
                // Remove the existing path for this color
                model.removeColorPath(cellColor);
                // Remove the stored path
                connectedPath.remove(cellColor);
                completedColors.remove(cellColor);
                repaint();
            }

            // Start drawing a path for this color
            currentColor = cellColor;
            currentPath.clear();
            currentPath.add(gridPt);
            repaint();
            return;
        }

        // If user didn't click on an anchor, reset path and color
        currentColor = null;
        currentPath.clear();
        repaint();
    }

    private void applyPath() {
        // Store current board state to allow rollback
        Color[][] originalBoard = model.getBoardArray();

        // Remove old path for this color
        model.removeColorPath(currentColor);

        // Fill new path
        for (Point p : currentPath) {
            model.setCellColor(p, currentColor);
        }

        // Check if the puzzle is still solvable after this path
        if (!isStillSolvable()) {
            // Undo the path and restore original state
            model.removeColorPath(currentColor);
            for (int r = 0; r < originalBoard.length; r++) {
                for (int c = 0; c < originalBoard[0].length; c++) {
                    if (originalBoard[r][c] != null) {
                        model.setCellColor(new Point(r, c), originalBoard[r][c]);
                    }
                }
            }
            showUnsolvableTooltip();
        } else {
            completedColors.add(currentColor);

            // Ensure to create a new list to store
            connectedPath.put(currentColor, new ArrayList<>(currentPath));

            // Debug: Verify the path was stored
            System.out.println("Stored path for " + currentColor + ": " + connectedPath.get(currentColor));
        }
    }

    /**
     * Handles mouse drag events to continue drawing paths.
     */
    private void handleMouseDragged(MouseEvent e) {
        if (currentColor == null || currentPath.isEmpty()) return;

        Point last = currentPath.get(currentPath.size() - 1);
        Point next = toGrid(e.getPoint());

        if (validator.isValidMove(last, next, currentColor, currentPath)) {
            currentPath.add(next);
            repaint();
        }
    }

    /**
     * Handles mouse release events to finalize paths.
     */
    private void handleMouseReleased() {
        if (currentColor == null || currentPath.isEmpty()) return;

        Point last = currentPath.get(currentPath.size() - 1);
        if (model.isAnchor(last) && model.getCellColor(last) == currentColor && currentPath.size() > 1) {

            // We ended on the other anchor for this color
            if (!validator.hasPathOverlap(currentPath, currentColor)) {
                applyPath();

                // Check win condition
                if (validator.isWin()) {
                    showWinMessage();
                }
            } else {
                showOverlapTooltip();
            }
        }

        // Reset path state regardless of outcome
        currentPath.clear();
        currentColor = null;
        repaint();
    }

    /**
     * Checks if the puzzle is still solvable after placing the current path.
     */
    private boolean isStillSolvable() {
        // Get current board state
        Color[][] boardArray = model.getBoardArray();

        // Check if each unconnected color can still be connected
        for (Map.Entry<Color, Point[]> entry : level.anchorMap().entrySet()) {
            Color color = entry.getKey();
            Point[] anchors = entry.getValue();

            // Skip the color we just connected (since we know it's good)
            if (color.equals(currentColor)) {
                continue;
            }

            // Skip already connected colors
            if (validator.areAnchorsConnected(color, anchors[0], anchors[1])) {
                continue;
            }

            // Check if a path is possible
            if (!canConnect(boardArray, anchors[0], anchors[1])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if it's possible to connect two points through empty cells.
     */
    private boolean canConnect(Color[][] board, Point start, Point end) {
        // Create visited array
        boolean[][] visited = new boolean[board.length][board[0].length];

        // Mark non-empty cells as visited
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                // Mark any cell that's not empty and not the start/end as visited
                if (board[r][c] != null &&
                        !(r == start.x && c == start.y) &&
                        !(r == end.x && c == end.y)) {
                    visited[r][c] = true;
                }
            }
        }

        // Start search from start point
        return hasPath(board, start, end, visited, new ArrayList<>());
    }

    /**
     * DFS to find if there's any path between start and end.
     */
    private boolean hasPath(Color[][] board, Point current, Point end,
                            boolean[][] visited, List<Point> path) {
        if (current.x == end.x && current.y == end.y) {
            return true;
        }

        // Mark as visited
        visited[current.x][current.y] = true;
        path.add(current);

        // Try all 4 directions
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // up, down, left, right
        for (int[] dir : dirs) {
            int nx = current.x + dir[0];
            int ny = current.y + dir[1];

            // Check bounds
            if (nx < 0 || nx >= board.length || ny < 0 || ny >= board[0].length) {
                continue;
            }

            // Check if already visited
            if (visited[nx][ny]) {
                continue;
            }

            // Explore this direction
            if (hasPath(board, new Point(nx, ny), end, visited, path)) {
                return true;
            }
        }

        // Backtrack
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Shows a tooltip when the path makes the puzzle unsolvable.
     */
    private void showUnsolvableTooltip() {
        ToolTipManager.sharedInstance().setInitialDelay(0);
        setToolTipText(UNSOLVABLE_ERROR);

        // Force tooltip to show immediately
        Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, this);
        MouseEvent phantom = new MouseEvent(
                this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                0, location.x, location.y, 0, false);

        ToolTipManager.sharedInstance().mouseMoved(phantom);

        // Reset tooltip after delay
        Timer timer = new Timer(TOOLTIP_DELAY, evt -> setToolTipText(null));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Shows a hint by highlighting a cell on the board.
     *
     * @param color The color of the path to continue
     * @param point The suggested next point in the path
     */
    public void showHint(Color color, Point point) {
        // Cancel any existing hint
        cancelHint();

        // Setup new hint
        this.hintColor = color;
        this.hintPoint = point;
        this.hintVisible = true;

        // Setup blinking animation
        hintAnimationTimer = new Timer(HINT_BLINK_RATE, e -> {
            hintVisible = !hintVisible;
            repaint();
        });
        hintAnimationTimer.start();

        // Ensure we repaint to show the hint immediately
        repaint();
    }

    /**
     * Cancels any active hint.
     */
    public void cancelHint() {
        if (hintAnimationTimer != null && hintAnimationTimer.isRunning()) {
            hintAnimationTimer.stop();
        }

        hintPoint = null;
        hintColor = null;
        hintVisible = false;
        repaint();
    }

    /**
     * Shows a tooltip when paths overlap.
     */
    private void showOverlapTooltip() {
        ToolTipManager.sharedInstance().setInitialDelay(0);
        setToolTipText(OVERLAP_ERROR);

        // Force tooltip to show immediately
        Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, this);
        MouseEvent phantom = new MouseEvent(this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, location.x, location.y, 0, false);

        ToolTipManager.sharedInstance().mouseMoved(phantom);

        // Reset tooltip after a delay
        Timer timer = new Timer(TOOLTIP_DELAY, evt -> setToolTipText(null));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Shows a win message dialog.
     */
    private void showWinMessage() {
        JOptionPane.showMessageDialog(this, WIN_MESSAGE, WIN_TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Converts a screen point to a grid point.
     */
    private Point toGrid(Point p) {
        int cellSize = BoardRenderer.getCellSize();
        int row = p.y / cellSize;
        int col = p.x / cellSize;
        return new Point(row, col);
    }

    // BoardStateListener implementation
    @Override
    public void onBoardChanged() {
        repaint();
    }

    @Override
    public void onPathCompleted(Color color) {
        completedColors.add(color);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // If no puzzle loaded yet, just show instructions
        if (level == null) {
            // Existing startup state rendering
            return;
        }

        // Render the board
        renderer.render(g2d);

        // Render current path (if any)
        renderer.renderCurrentPath(g2d, currentPath, currentColor);

        for (Color completedColor : completedColors) {
            Point[] anchors = level.anchorMap().get(completedColor);
            if (anchors != null) {
                for (Point anchor : anchors) {
                    renderCompletedAnchor(g2d, anchor, completedColor);
                }
            }
        }

        // Render hint if active
        if (hintPoint != null && hintColor != null && hintVisible) {
            renderHint(g2d);
        }
    }

    private void renderCompletedAnchor(Graphics2D g2d, Point anchor, Color color) {
        int cellSize = BoardRenderer.getCellSize();
        int anchorSize = cellSize * 3 / 5;
        int x = anchor.y * cellSize + (cellSize - anchorSize) / 2;
        int y = anchor.x * cellSize + (cellSize - anchorSize) / 2;

        // Outer square (lighter, transparent)
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        g2d.fillRoundRect(x - 10, y - 10, anchorSize + 20, anchorSize + 20, 10, 10);

        // Inner circle (solid color)
        g2d.setColor(color);
        g2d.fillOval(x, y, anchorSize, anchorSize);
    }

    /**
     * Renders the hint on the board.
     */
    private void renderHint(Graphics2D g) {
        int cellSize = BoardRenderer.getCellSize();
        int x = hintPoint.y * cellSize;
        int y = hintPoint.x * cellSize;

        // Draw a highlighting circle around the hint cell
        g.setColor(hintColor);
        g.setStroke(new BasicStroke(3.0f));

        // Inner glow effect
        AlphaComposite alphaComposite = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.5f);
        g.setComposite(alphaComposite);
        g.fillRoundRect(x + 5, y + 5, cellSize - 10, cellSize - 10, 10, 10);

        // Outline
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g.drawRoundRect(x + 5, y + 5, cellSize - 10, cellSize - 10, 10, 10);

        // Draw a pulsing "?" to make it extra clear this is a hint
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("?", x + cellSize / 2 - 5, y + cellSize / 2 + 7);
    }

    /**
     * Gets the board model for use by other components.
     */
    public BoardModel getModel() {
        return model;
    }
}
