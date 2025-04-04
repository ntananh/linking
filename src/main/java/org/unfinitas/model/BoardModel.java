package org.unfinitas.model;

import org.unfinitas.dto.Level;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * Model for the board state, following the MVC pattern.
 * Provides methods to manipulate and query the game state.
 * Implements Observer pattern for state changes.
 */
public class BoardModel {
    private final Level level;
    private final Color[][] board;
    private final boolean[][] isAnchor;
    private final List<BoardStateListener> listeners = new ArrayList<>();

    /**
     * Interface for board state change listeners.
     * Enables the Observer pattern.
     */
    public interface BoardStateListener {
        void onBoardChanged();
        void onPathCompleted(Color color);
    }

    /**
     * Creates a new board model for the given level.
     *
     * @param level The puzzle level
     */
    public BoardModel(Level level) {
        this.level = level;
        this.board = new Color[level.rows()][level.cols()];
        this.isAnchor = new boolean[level.rows()][level.cols()];

        initializeAnchors();
    }

    /**
     * Initializes anchors on the board.
     */
    private void initializeAnchors() {
        for (Map.Entry<Color, Point[]> e : level.anchorMap().entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();

            for (Point anchor : anchors) {
                board[anchor.x][anchor.y] = color;
                isAnchor[anchor.x][anchor.y] = true;
            }
        }
    }

    /**
     * Adds a listener for board state changes.
     *
     * @param listener The listener to add
     */
    public void addBoardStateListener(BoardStateListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a board state listener.
     *
     * @param listener The listener to remove
     */
    public void removeBoardStateListener(BoardStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners that the board has changed.
     */
    private void notifyBoardChanged() {
        for (BoardStateListener listener : listeners) {
            listener.onBoardChanged();
        }
    }

    /**
     * Notifies all listeners that a path has been completed.
     *
     * @param color The color of the completed path
     */
    private void notifyPathCompleted(Color color) {
        for (BoardStateListener listener : listeners) {
            listener.onPathCompleted(color);
        }
    }

    /**
     * Gets the puzzle level.
     *
     * @return The level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Gets the color of a cell.
     *
     * @param p The cell position
     * @return The color, or null if empty
     */
    public Color getCellColor(Point p) {
        return board[p.x][p.y];
    }

    /**
     * Sets the color of a cell.
     *
     * @param p The cell position
     * @param color The color to set
     */
    public void setCellColor(Point p, Color color) {
        board[p.x][p.y] = color;
        notifyBoardChanged();
    }

    /**
     * Checks if a cell is an anchor.
     *
     * @param p The cell position
     * @return True if the cell is an anchor
     */
    public boolean isAnchor(Point p) {
        return isAnchor[p.x][p.y];
    }

    /**
     * Checks if a point is within the board bounds.
     *
     * @param p The point to check
     * @return True if the point is within bounds
     */
    public boolean inBounds(Point p) {
        return p.x >= 0 && p.x < level.rows() && p.y >= 0 && p.y < level.cols();
    }

    /**
     * Gets a copy of the board array for algorithms.
     *
     * @return A copy of the board array
     */
    public Color[][] getBoardArray() {
        Color[][] copy = new Color[level.rows()][level.cols()];
        for (int r = 0; r < level.rows(); r++) {
            System.arraycopy(board[r], 0, copy[r], 0, level.cols());
        }
        return copy;
    }

    /**
     * Applies a path for a color to the board.
     *
     * @param color The color of the path
     * @param path The list of points in the path
     */
    public void applyPath(Color color, List<Point> path) {
        // Clear existing path first (except anchors)
        removeColorPath(color);

        // Apply new path
        for (Point p : path) {
            board[p.x][p.y] = color;
        }

        notifyBoardChanged();

        // Check if this path connects two anchors
        if (isPathComplete(color)) {
            notifyPathCompleted(color);
        }
    }

    /**
     * Checks if a path connects its anchors.
     *
     * @param color The color to check
     * @return True if the path is complete
     */
    private boolean isPathComplete(Color color) {
        Point[] anchors = level.anchorMap().get(color);
        if (anchors == null || anchors.length != 2) {
            return false;
        }

        // Use BFS to check connectivity
        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[level.rows()][level.cols()];

        Point start = anchors[0];
        Point end = anchors[1];

        queue.offer(start);
        visited[start.x][start.y] = true;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.equals(end)) {
                return true; // Found a path to the end
            }

            for (int[] d : directions) {
                int nr = current.x + d[0];
                int nc = current.y + d[1];
                Point next = new Point(nr, nc);

                if (inBounds(next) && !visited[nr][nc] && color.equals(board[nr][nc])) {
                    visited[nr][nc] = true;
                    queue.offer(next);
                }
            }
        }

        return false; // No path found
    }

    /**
     * Removes all cells of the given color (except anchors).
     *
     * @param color The color to remove
     */
    public void removeColorPath(Color color) {
        for (int r = 0; r < level.rows(); r++) {
            for (int col = 0; col < level.cols(); col++) {
                if (board[r][col] == color && !isAnchor[r][col]) {
                    board[r][col] = null;
                }
            }
        }
        notifyBoardChanged();
    }

    /**
     * Checks if a cell is empty or matches the target color.
     *
     * @param p The cell position
     * @param targetColor The target color
     * @return True if the cell is available
     */
    public boolean isCellAvailable(Point p, Color targetColor) {
        Color cellColor = board[p.x][p.y];
        return cellColor == null ||
                (isAnchor[p.x][p.y] && cellColor.equals(targetColor));
    }

    /**
     * Checks if all cells in the board are filled.
     *
     * @return True if the board is full
     */
    public boolean isBoardFull() {
        for (int r = 0; r < level.rows(); r++) {
            for (int c = 0; c < level.cols(); c++) {
                if (board[r][c] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Iterates over each cell in the board.
     *
     * @param consumer The consumer function to apply
     */
    public void forEachCell(CellConsumer consumer) {
        for (int r = 0; r < level.rows(); r++) {
            for (int c = 0; c < level.cols(); c++) {
                consumer.accept(r, c, board[r][c], isAnchor[r][c]);
            }
        }
    }

    /**
     * Iterates over each anchor in the board.
     *
     * @param consumer The consumer function to apply
     */
    public void forEachAnchor(AnchorConsumer consumer) {
        for (Map.Entry<Color, Point[]> e : level.anchorMap().entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();

            for (Point anchor : anchors) {
                consumer.accept(anchor, color);
            }
        }
    }

    /**
     * Functional interface for cell iteration.
     */
    @FunctionalInterface
    public interface CellConsumer {
        void accept(int row, int col, Color color, boolean isAnchor);
    }

    /**
     * Functional interface for anchor iteration.
     */
    @FunctionalInterface
    public interface AnchorConsumer {
        void accept(Point point, Color color);
    }
}
