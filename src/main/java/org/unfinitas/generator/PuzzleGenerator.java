package org.unfinitas.generator;

import org.unfinitas.dto.Level;

import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * Responsible for generating random Dot Knot puzzles and ensuring they're solvable.
 */
public class PuzzleGenerator {

    private static final Random rand = new Random();

    private static final Color[] COLOR_PALETTE = {
            new Color(86, 112, 255),    // Soft Indigo
            new Color(255, 87, 51),     // Vibrant Coral
            new Color(46, 204, 113),    // Emerald Green
            new Color(151, 99, 238),    // Pastel Purple
            new Color(52, 152, 219),    // Bright Blue
            new Color(243, 156, 18),    // Warm Orange
            new Color(231, 76, 60),     // Deep Red
            new Color(26, 188, 156),    // Turquoise
            new Color(127, 140, 141)    // Gray
    };


    // Cache directions array to avoid recreating it
    public static final int[][] DIRECTIONS = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    /**
     * Generates a puzzle with the given dimensions, number of color pairs,
     * and optionally checks for a unique solution.
     */
    public static Level generatePuzzle(int rows, int cols, int numberOfPairs, boolean requireUnique) {
        System.out.println("Generating puzzle: " + rows + "x" + cols + " with " + numberOfPairs + " pairs, requireUnique=" + requireUnique);

        if (numberOfPairs > COLOR_PALETTE.length) {
            throw new IllegalArgumentException("Too many color pairs requested: " + numberOfPairs
                    + ", maximum is " + COLOR_PALETTE.length);
        }

        // Limit generation attempts to avoid infinite loops
        final int MAX_ATTEMPTS = 1000;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            System.out.println("Attempt #" + (attempt + 1));

            // 1) Randomly place anchor pairs
            Map<Color, Point[]> anchorMap = placeRandomAnchors(rows, cols, numberOfPairs);
            System.out.println("Placed " + anchorMap.size() + " anchor pairs");

            // 2) Check solvability (and uniqueness if required)
            // We'll create a temporary board to see if we can place all paths.
            Color[][] board = new Color[rows][cols];
            // Place anchors in board
            for (Map.Entry<Color, Point[]> e : anchorMap.entrySet()) {
                Color color = e.getKey();
                Point[] anchors = e.getValue();
                board[anchors[0].x][anchors[0].y] = color;
                board[anchors[1].x][anchors[1].y] = color;
            }

            Color[] colors = anchorMap.keySet().toArray(new Color[0]);

            // Try to solve
            System.out.println("Checking solvability...");
            boolean solvable = solvePuzzle(colors, 0, board, anchorMap);
            System.out.println("Solvable: " + solvable);

            if (solvable) {
                // Verify the solution actually fills the entire board
                if (!isBoardFull(board)) {
                    System.out.println("Warning: Found 'solution' doesn't fill the board, continuing...");
                    continue;
                }

                if (!requireUnique) {
                    // Good enough
                    System.out.println("Found solvable puzzle, returning");
                    return new Level(rows, cols, anchorMap);
                } else {
                    // Check uniqueness
                    System.out.println("Checking uniqueness...");
                    if (isUniqueSolution(colors, board, anchorMap)) {
                        System.out.println("Found unique solution, returning");
                        return new Level(rows, cols, anchorMap);
                    }
                    System.out.println("Solution not unique, continuing...");
                }
            }
            // Otherwise, loop again
        }

        throw new RuntimeException("Failed to generate a valid puzzle after " + MAX_ATTEMPTS + " attempts");
    }

    /**
     * Randomly place the specified number of color pairs (anchors) on an empty board.
     */
    private static Map<Color, Point[]> placeRandomAnchors(int rows, int cols, int numberOfPairs) {
        Map<Color, Point[]> anchorMap = new HashMap<>();
        Set<Point> used = new HashSet<>();

        // Shuffle colors to get random selection each time
        java.util.List<Color> shuffledColors = new ArrayList<>(Arrays.asList(COLOR_PALETTE));
        Collections.shuffle(shuffledColors);

        for (int i = 0; i < numberOfPairs; i++) {
            Color color = shuffledColors.get(i);

            Point a, b;
            do {
                a = new Point(rand.nextInt(rows), rand.nextInt(cols));
            } while (used.contains(a));
            used.add(a);

            do {
                b = new Point(rand.nextInt(rows), rand.nextInt(cols));
            } while (used.contains(b));
            used.add(b);

            anchorMap.put(color, new Point[]{a, b});
        }
        return anchorMap;
    }

    /**
     * Attempts to solve the puzzle by placing paths for each color in turn.
     * Backtracking approach:
     * 1) For the current color, find all possible paths from anchor1 -> anchor2.
     * 2) Place a path and recurse to the next color.
     * 3) If no path works, backtrack.
     */
    private static boolean solvePuzzle(Color[] colors, int index, Color[][] board, Map<Color, Point[]> anchorMap) {
        if (index == colors.length) {
            // All colors placed successfully
            return isBoardFull(board);
        }

        Color color = colors[index];
        Point[] anchors = anchorMap.get(color);
        Point start = anchors[0], end = anchors[1];

        // All possible paths from start to end
        java.util.List<java.util.List<Point>> allPaths = findAllPaths(board, start, end, color);

        if (allPaths.isEmpty()) {
            System.out.println("No paths found for color " + color);
            return false;
        }

        // Optimization: Shuffle paths to avoid getting stuck in similar patterns
        Collections.shuffle(allPaths);

        for (java.util.List<Point> path : allPaths) {
            // Place path in board
            for (Point p : path) {
                board[p.x][p.y] = color;
            }

            // Recurse
            if (solvePuzzle(colors, index + 1, board, anchorMap)) {
                return true;
            }

            // Backtrack: remove path (but keep anchors)
            for (Point p : path) {
                if (!p.equals(start) && !p.equals(end)) {
                    board[p.x][p.y] = null;
                }
            }
        }

        return false; // no valid path found for this color
    }

    /**
     * Finds all possible simple paths (no revisits) from start to end
     * traveling only through cells that are null (empty) or end cell
     * in the given board.
     */
    private static java.util.List<java.util.List<Point>> findAllPaths(Color[][] board, Point start, Point end, Color color) {
        java.util.List<java.util.List<Point>> result = new ArrayList<>();
        java.util.List<Point> path = new ArrayList<>();
        path.add(start);

        // Use a boolean visited array for faster lookups
        boolean[][] visited = new boolean[board.length][board[0].length];
        visited[start.x][start.y] = true;

        dfsPaths(board, start, end, color, path, result, visited);

        // If we didn't find any paths (which can happen with larger boards), try limiting search depth
        if (result.isEmpty()) {
            System.out.println("Warning: No paths found with standard DFS, trying limited depth");
            // Try a more aggressive search with depth limiting
            visited = new boolean[board.length][board[0].length];
            visited[start.x][start.y] = true;
            path.clear();
            path.add(start);

            // Use A* search heuristic to guide path finding
            findShortestPath(board, start, end, visited, result);
        }

        return result;
    }

    /**
     * A* search to find the shortest path between two points.
     */
    private static void findShortestPath(Color[][] board, Point start, Point end,
                                        boolean[][] visited, List<List<Point>> result) {
        // Priority queue ordered by distance to end
        PriorityQueue<PathNode> queue = new PriorityQueue<>(
            Comparator.comparingInt(node -> node.distanceToEnd));

        // Add start node with distance to end
        queue.add(new PathNode(start, null, manhattanDistance(start, end)));

        // Track visited cells
        Set<Point> visitedPoints = new HashSet<>();
        visitedPoints.add(start);

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();
            Point pos = current.position;

            // Found the end
            if (pos.equals(end)) {
                List<Point> path = reconstructPath(current);
                result.add(path);
                return;
            }

            // Try all directions
            for (int[] d : DIRECTIONS) {
                int nr = pos.x + d[0];
                int nc = pos.y + d[1];
                Point next = new Point(nr, nc);

                if (inBounds(next, board.length, board[0].length) &&
                    !visitedPoints.contains(next) &&
                    (board[nr][nc] == null || next.equals(end))) {

                    // Create a new node
                    PathNode nextNode = new PathNode(
                        next, current, manhattanDistance(next, end));
                    queue.add(nextNode);
                    visitedPoints.add(next);
                }
            }
        }
    }

    /**
     * Manhattan distance between two points (heuristic for A*).
     */
    private static int manhattanDistance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    /**
     * Reconstructs a path from a node chain.
     */
    private static List<Point> reconstructPath(PathNode endNode) {
        List<Point> path = new ArrayList<>();
        PathNode current = endNode;

        while (current != null) {
            path.add(0, current.position);
            current = current.parent;
        }

        return path;
    }

    /**
     * Helper class for A* path finding.
     */
    private static class PathNode {
        Point position;
        PathNode parent;
        int distanceToEnd;

        PathNode(Point position, PathNode parent, int distanceToEnd) {
            this.position = position;
            this.parent = parent;
            this.distanceToEnd = distanceToEnd;
        }
    }

    private static void dfsPaths(Color[][] board, Point current, Point end, Color color,
                                 java.util.List<Point> path, java.util.List<java.util.List<Point>> result, boolean[][] visited) {
        if (current.equals(end)) {
            // Found a path
            result.add(new ArrayList<>(path));
            return;
        }

        // Limit search depth to avoid excessive searching
        if (path.size() > board.length * board[0].length) {
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
     * Checks if the board is completely filled.
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
     * Checks if there is exactly one solution. We do this by:
     * 1) Solve once, mark foundOneSolution = true
     * 2) Keep searching for a second solution => foundSecondSolution = true
     * If we never find a second solution, puzzle is unique.
     *
     * This can be time-consuming for bigger boards.
     */
    private static boolean isUniqueSolution(Color[] colors, Color[][] board, Map<Color, Point[]> anchorMap) {
        UniqueSolver solver = new UniqueSolver(colors, board, anchorMap);
        solver.solvePuzzle(0);
        return (solver.foundOneSolution && !solver.foundSecondSolution);
    }

    // Helper class to track uniqueness search
    private static class UniqueSolver {
        Color[] colors;
        Color[][] board;
        Map<Color, Point[]> anchorMap;
        boolean foundOneSolution = false;
        boolean foundSecondSolution = false;

        UniqueSolver(Color[] colors, Color[][] board, Map<Color, Point[]> anchorMap) {
            this.colors = colors;
            // Copy board so we can manipulate
            this.board = deepCopy(board);
            this.anchorMap = anchorMap;
        }

        void solvePuzzle(int index) {
            if (foundSecondSolution) return; // short-circuit

            if (index == colors.length) {
                // Ensure board is actually full
                if (isBoardFull(board)) {
                    if (!foundOneSolution) {
                        foundOneSolution = true;
                    } else {
                        foundSecondSolution = true;
                    }
                }
                return;
            }

            Color color = colors[index];
            Point[] anchors = anchorMap.get(color);
            Point start = anchors[0], end = anchors[1];

            java.util.List<java.util.List<Point>> allPaths = findAllPaths(board, start, end, color);
            // Randomize path order to find alternative solutions faster
            Collections.shuffle(allPaths);

            for (List<Point> path : allPaths) {
                for (Point p : path) {
                    board[p.x][p.y] = color;
                }
                solvePuzzle(index + 1);
                for (Point p : path) {
                    if (!p.equals(start) && !p.equals(end)) {
                        board[p.x][p.y] = null;
                    }
                }
                if (foundSecondSolution) return;
            }
        }
    }

    private static Color[][] deepCopy(Color[][] original) {
        int rows = original.length;
        int cols = original[0].length;
        Color[][] copy = new Color[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(original[r], 0, copy[r], 0, cols);
        }
        return copy;
    }
}
