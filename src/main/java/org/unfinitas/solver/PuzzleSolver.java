package org.unfinitas.solver;

import org.unfinitas.dto.Level;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * Provides puzzle-solving capabilities for the Infinity Links game.
 * Can be used to check if a puzzle is solvable or to get hints.
 */
public class PuzzleSolver {
    // Direction vectors for path finding
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /**
     * Gets a hint for the next move in the puzzle.
     *
     * @param level The current puzzle level
     * @param currentBoard The current state of the board
     * @return A suggested next move, or null if no hint available
     */
    public static Map.Entry<Color, Point> getHint(Level level, Color[][] currentBoard) {
        // First try to find an empty cell adjacent to an existing path
        Map.Entry<Color, Point> adjacentHint = findAdjacentHint(level, currentBoard);
        if (adjacentHint != null) {
            return adjacentHint;
        }

        // If no obvious hint, solve the entire puzzle
        Map<Color, List<Point>> solution = solve(level);
        if (solution == null) {
            return null; // No solution exists
        }

        // Find an incomplete path that can be extended
        for (Map.Entry<Color, List<Point>> entry : solution.entrySet()) {
            Color color = entry.getKey();
            List<Point> solutionPath = entry.getValue();

            // Find the current path for this color
            List<Point> currentPath = getCurrentPath(currentBoard, color);

            // If current path is shorter than solution path, suggest the next point
            if (currentPath.size() < solutionPath.size()) {
                // Find a point in the solution that would be a valid next step
                for (Point p : solutionPath) {
                    if (!containsPoint(currentPath, p) && isValidNextPoint(currentPath, p, currentBoard)) {
                        return Map.entry(color, p);
                    }
                }
            }
        }

        return null; // No hint available
    }

    /**
     * Find a hint by looking for empty cells adjacent to existing paths.
     */
    private static Map.Entry<Color, Point> findAdjacentHint(Level level, Color[][] board) {
        int rows = board.length;
        int cols = board[0].length;

        // Check each color
        for (Map.Entry<Color, Point[]> entry : level.anchorMap().entrySet()) {
            Color color = entry.getKey();
            Point[] anchors = entry.getValue();

            // Skip if path is already complete
            if (isPathComplete(board, anchors[0], anchors[1], color)) {
                continue;
            }

            // Find all cells of this color
            List<Point> colorCells = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (color.equals(board[r][c])) {
                        colorCells.add(new Point(r, c));
                    }
                }
            }

            // For each cell of this color, look for valid adjacent empty cells
            for (Point p : colorCells) {
                for (int[] dir : DIRECTIONS) {
                    int nr = p.x + dir[0];
                    int nc = p.y + dir[1];

                    // Check bounds
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                        continue;
                    }

                    // Check if empty
                    if (board[nr][nc] == null) {
                        Point nextPoint = new Point(nr, nc);

                        // Check if this would create a valid path
                        if (isValidHint(board, color, nextPoint)) {
                            return Map.entry(color, nextPoint);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if a point would be a valid hint.
     */
    private static boolean isValidHint(Color[][] board, Color color, Point p) {
        // Try placing the color at this point
        board[p.x][p.y] = color;

        // Find anchors for this color
        List<Point> anchors = new ArrayList<>();
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (color.equals(board[r][c])) {
                    // Check if this is likely an anchor (has fewer than 2 neighbors)
                    int neighbors = 0;
                    for (int[] dir : DIRECTIONS) {
                        int nr = r + dir[0];
                        int nc = c + dir[1];
                        if (nr >= 0 && nr < board.length && nc >= 0 && nc < board[0].length) {
                            if (color.equals(board[nr][nc])) {
                                neighbors++;
                            }
                        }
                    }

                    if (neighbors < 2) {
                        anchors.add(new Point(r, c));
                    }
                }
            }
        }

        // Reset the board
        board[p.x][p.y] = null;

        // We need exactly 2 anchors to check
        return anchors.size() == 2;
    }

    /**
     * Check if a path is complete between two anchors.
     */
    private static boolean isPathComplete(Color[][] board, Point a, Point b, Color color) {
        boolean[][] visited = new boolean[board.length][board[0].length];
        Queue<Point> queue = new LinkedList<>();

        queue.offer(a);
        visited[a.x][a.y] = true;

        while (!queue.isEmpty()) {
            Point curr = queue.poll();

            if (curr.equals(b)) {
                return true; // Path found
            }

            for (int[] dir : DIRECTIONS) {
                int nr = curr.x + dir[0];
                int nc = curr.y + dir[1];

                if (nr >= 0 && nr < board.length && nc >= 0 && nc < board[0].length &&
                        !visited[nr][nc] && color.equals(board[nr][nc])) {

                    visited[nr][nc] = true;
                    queue.offer(new Point(nr, nc));
                }
            }
        }

        return false; // No path exists
    }

    /**
     * Attempts to solve the puzzle automatically.
     *
     * @param level The puzzle level to solve
     * @return A solution map with colors as keys and paths as values, or null if no solution exists
     */
    public static Map<Color, List<Point>> solve(Level level) {
        Color[][] board = new Color[level.rows()][level.cols()];

        // Initialize board with anchors
        for (Map.Entry<Color, Point[]> entry : level.anchorMap().entrySet()) {
            Color color = entry.getKey();
            Point[] anchors = entry.getValue();

            board[anchors[0].x][anchors[0].y] = color;
            board[anchors[1].x][anchors[1].y] = color;
        }

        // Convert colors to array for ordered processing
        Color[] colors = level.anchorMap().keySet().toArray(new Color[0]);

        // Store all paths for solution
        Map<Color, List<Point>> solution = new HashMap<>();

        // Attempt to solve recursively
        if (solveRecursive(colors, 0, board, level.anchorMap(), solution)) {
            return solution;
        }

        return null; // No solution found
    }

    /**
     * Recursive solver that attempts to place a valid path for each color.
     */
    private static boolean solveRecursive(
            Color[] colors,
            int colorIndex,
            Color[][] board,
            Map<Color, Point[]> anchorMap,
            Map<Color, List<Point>> solution) {

        // Base case: all colors have been processed
        if (colorIndex == colors.length) {
            return isBoardFull(board); // Ensure the board is completely filled
        }

        Color currentColor = colors[colorIndex];
        Point[] anchors = anchorMap.get(currentColor);
        Point start = anchors[0];
        Point end = anchors[1];

        // Find all possible paths between the anchors
        List<List<Point>> allPaths = findAllPaths(board, start, end, currentColor);

        // Optimization: Shuffle paths to avoid getting stuck in similar patterns
        Collections.shuffle(allPaths);

        // Try each path
        for (List<Point> path : allPaths) {
            // Place this path on the board
            for (Point p : path) {
                board[p.x][p.y] = currentColor;
            }

            // Save this path to our solution
            solution.put(currentColor, new ArrayList<>(path));

            // Recursively try to solve the rest
            if (solveRecursive(colors, colorIndex + 1, board, anchorMap, solution)) {
                return true; // Solution found
            }

            // Backtrack: remove this path (except anchors)
            for (Point p : path) {
                if (!p.equals(start) && !p.equals(end)) {
                    board[p.x][p.y] = null;
                }
            }

            // Remove from solution
            solution.remove(currentColor);
        }

        return false; // No solution with this configuration
    }

    /**
     * Finds all possible simple paths (no revisits) from start to end
     * traveling only through cells that are null (empty) or end cell
     * in the given board.
     */
    private static List<List<Point>> findAllPaths(Color[][] board, Point start, Point end, Color color) {
        List<List<Point>> result = new ArrayList<>();
        List<Point> path = new ArrayList<>();
        path.add(start);

        // Use a boolean visited array for faster lookups
        boolean[][] visited = new boolean[board.length][board[0].length];
        visited[start.x][start.y] = true;

        dfsPaths(board, start, end, color, path, result, visited);
        return result;
    }

    private static void dfsPaths(Color[][] board, Point current, Point end, Color color,
                                 List<Point> path, List<List<Point>> result, boolean[][] visited) {
        if (current.equals(end)) {
            // Found a path
            result.add(new ArrayList<>(path));
            return;
        }

        for (int[] d : DIRECTIONS) {
            int nr = current.x + d[0];
            int nc = current.y + d[1];
            Point next = new Point(nr, nc);

            if (inBounds(next, board.length, board[0].length) && !visited[nr][nc]) {
                // Check if empty or the end anchor
                if (board[nr][nc] == null || next.equals(end)) {
                    // Try
                    path.add(next);
                    visited[nr][nc] = true;
                    dfsPaths(board, next, end, color, path, result, visited);
                    visited[nr][nc] = false;
                    path.remove(path.size() - 1);
                }
            }
        }
    }

    private static boolean inBounds(Point p, int rows, int cols) {
        return p.x >= 0 && p.x < rows && p.y >= 0 && p.y < cols;
    }

    /**
     * Checks if the board is completely filled (no null cells).
     */
    private static boolean isBoardFull(Color[][] board) {
        for (Color[] row : board) {
            for (Color cell : row) {
                if (cell == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Extracts the current path for a color from the board.
     */
    private static List<Point> getCurrentPath(Color[][] board, Color color) {
        List<Point> path = new ArrayList<>();

        // Find all cells with this color
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (color.equals(board[r][c])) {
                    path.add(new Point(r, c));
                }
            }
        }

        return path;
    }

    /**
     * Checks if a point would be a valid next step in a path.
     */
    private static boolean isValidNextPoint(List<Point> path, Point next, Color[][] board) {
        if (path.isEmpty()) {
            return true;
        }

        // Check if it's an empty cell or an anchor we need to connect to
        if (board[next.x][next.y] != null && !isAnchor(board, next)) {
            return false;
        }

        // Must be orthogonally adjacent to at least one point in the path
        for (Point p : path) {
            if ((Math.abs(p.x - next.x) == 1 && p.y == next.y) ||
                    (Math.abs(p.y - next.y) == 1 && p.x == next.x)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a point is likely an anchor (has 1 or fewer neighbors of same color).
     */
    private static boolean isAnchor(Color[][] board, Point p) {
        Color color = board[p.x][p.y];
        if (color == null) return false;

        int neighbors = 0;
        for (int[] dir : DIRECTIONS) {
            int nr = p.x + dir[0];
            int nc = p.y + dir[1];
            if (nr >= 0 && nr < board.length && nc >= 0 && nc < board[0].length) {
                if (color.equals(board[nr][nc])) {
                    neighbors++;
                }
            }
        }

        return neighbors <= 1;
    }

    /**
     * Check if a list contains a point (Point.equals can be unreliable).
     */
    private static boolean containsPoint(List<Point> points, Point p) {
        for (Point point : points) {
            if (point.x == p.x && point.y == p.y) {
                return true;
            }
        }
        return false;
    }
}
