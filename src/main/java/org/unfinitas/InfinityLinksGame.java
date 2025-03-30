package org.unfinitas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Main class: Runs a Swing window with a "Generate Puzzle" button
 * and a board panel to play the generated puzzle.
 */
public class InfinityLinksGame extends JFrame {
    private BoardPanel boardPanel; // The current puzzle board

    public InfinityLinksGame() {
        super("Infinity Links – Infinite Dot Knot Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel with "Generate Puzzle" button
        JPanel topPanel = new JPanel();
        JButton generateButton = new JButton("Generate Puzzle");
        generateButton.addActionListener(e -> generateNewPuzzle());
        topPanel.add(generateButton);
        add(topPanel, BorderLayout.NORTH);

        // Create an initial puzzle on startup
        boardPanel = new BoardPanel(null);
        add(boardPanel, BorderLayout.CENTER);

        setSize(600, 650);
        setLocationRelativeTo(null);
    }

    /**
     * Generates a new puzzle (e.g., 6x6 board with 3 color pairs),
     * then replaces the old BoardPanel with a new one.
     */
    private void generateNewPuzzle() {
        // Adjust these to taste
        int rows = 6, cols = 6, numberOfPairs = 3;
        boolean requireUniqueSolution = false;

        // Generate a puzzle
        Level puzzle = PuzzleGenerator.generatePuzzle(rows, cols, numberOfPairs, requireUniqueSolution);
        // Rebuild the board panel
        remove(boardPanel);
        boardPanel = new BoardPanel(puzzle);
        add(boardPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InfinityLinksGame().setVisible(true));
    }
}

/**
 * Represents a puzzle level: dimensions + anchor positions for each color.
 */
class Level {
    int rows, cols;
    Map<Color, Point[]> anchorMap;

    public Level(int rows, int cols, Map<Color, Point[]> anchorMap) {
        this.rows = rows;
        this.cols = cols;
        this.anchorMap = anchorMap;
    }
}

/**
 * Responsible for generating random Dot Knot puzzles and ensuring they're solvable.
 */
class PuzzleGenerator {
    private static final Random rand = new Random();
    // A small palette of colors. Feel free to expand.
    private static final Color[] COLOR_PALETTE = {
            Color.RED, Color.GREEN, Color.BLUE,
            Color.ORANGE, Color.MAGENTA, Color.CYAN
    };

    /**
     * Generates a puzzle with the given dimensions, number of color pairs,
     * and optionally checks for a unique solution.
     */
    public static Level generatePuzzle(int rows, int cols, int numberOfPairs, boolean requireUnique) {
        while (true) {
            // 1) Randomly place anchor pairs
            Map<Color, Point[]> anchorMap = placeRandomAnchors(rows, cols, numberOfPairs);

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
            boolean solvable = solvePuzzle(colors, 0, board, anchorMap);

            if (solvable) {
                if (!requireUnique) {
                    // Good enough
                    return new Level(rows, cols, anchorMap);
                } else {
                    // Check uniqueness
                    if (isUniqueSolution(colors, board, anchorMap)) {
                        return new Level(rows, cols, anchorMap);
                    }
                }
            }
            // Otherwise, loop again
        }
    }

    /**
     * Randomly place the specified number of color pairs (anchors) on an empty board.
     */
    private static Map<Color, Point[]> placeRandomAnchors(int rows, int cols, int numberOfPairs) {
        Map<Color, Point[]> anchorMap = new HashMap<>();
        Set<Point> used = new HashSet<>();
        for (int i = 0; i < numberOfPairs; i++) {
            Color color = COLOR_PALETTE[i % COLOR_PALETTE.length]; // pick color from palette

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
            return true;
        }

        Color color = colors[index];
        Point[] anchors = anchorMap.get(color);
        Point start = anchors[0], end = anchors[1];

        // All possible paths from start to end
        List<List<Point>> allPaths = findAllPaths(board, start, end, color);

        for (List<Point> path : allPaths) {
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
    private static List<List<Point>> findAllPaths(Color[][] board, Point start, Point end, Color color) {
        List<List<Point>> result = new ArrayList<>();
        List<Point> path = new ArrayList<>();
        path.add(start);
        dfsPaths(board, start, end, color, path, result);
        return result;
    }

    private static void dfsPaths(Color[][] board, Point current, Point end, Color color,
                                 List<Point> path, List<List<Point>> result) {
        if (current.equals(end)) {
            // Found a path
            result.add(new ArrayList<>(path));
            return;
        }

        int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] d : directions) {
            int nr = current.x + d[0];
            int nc = current.y + d[1];
            Point next = new Point(nr, nc);

            if (inBounds(next, board.length, board[0].length)) {
                // Check if empty or the end anchor
                if (board[nr][nc] == null || next.equals(end)) {
                    if (!path.contains(next)) {
                        // Try
                        path.add(next);
                        dfsPaths(board, next, end, color, path, result);
                        path.remove(path.size() - 1);
                    }
                }
            }
        }
    }

    private static boolean inBounds(Point p, int rows, int cols) {
        return p.x >= 0 && p.x < rows && p.y >= 0 && p.y < cols;
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
                if (!foundOneSolution) {
                    foundOneSolution = true;
                } else {
                    foundSecondSolution = true;
                }
                return;
            }

            Color color = colors[index];
            Point[] anchors = anchorMap.get(color);
            Point start = anchors[0], end = anchors[1];

            List<List<Point>> allPaths = findAllPaths(board, start, end, color);
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

/**
 * The board panel that allows the user to draw paths between anchors.
 * Once all paths are placed and the board is full, the puzzle is solved.
 */
class BoardPanel extends JPanel {
    private static final int CELL_SIZE = 60;
    private final Level level;
    private final Color[][] board; // The current user state

    // The color currently being drawn, and the path in progress
    private Color currentColor = null;
    private final java.util.List<Point> currentPath = new ArrayList<>();

    public BoardPanel(Level level) {
        if (level == null) {
            // No puzzle yet (shown on startup)
            this.level = null;
            this.board = null;
            setPreferredSize(new Dimension(600, 600));
            return;
        }
        this.level = level;
        setPreferredSize(new Dimension(level.cols * CELL_SIZE, level.rows * CELL_SIZE));
        setBackground(Color.BLACK);

        // Initialize board with anchors in place
        board = new Color[level.rows][level.cols];
        for (Map.Entry<Color, Point[]> e : level.anchorMap.entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();
            board[anchors[0].x][anchors[0].y] = color;
            board[anchors[1].x][anchors[1].y] = color;
        }

        // Mouse listeners
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (level == null) return;
                Point gridPt = toGrid(e.getPoint());
                if (!inBounds(gridPt)) return;

                // Check if user clicked on an anchor
                for (Map.Entry<Color, Point[]> entry : level.anchorMap.entrySet()) {
                    Color c = entry.getKey();
                    Point[] anchors = entry.getValue();
                    if (gridPt.equals(anchors[0]) || gridPt.equals(anchors[1])) {
                        // Start drawing a path for color c
                        currentColor = c;
                        currentPath.clear();
                        currentPath.add(gridPt);
                        repaint();
                        return;
                    }
                }

                // If user didn't click on an anchor, do nothing special
                currentColor = null;
                currentPath.clear();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (level == null) return;
                if (currentColor == null || currentPath.isEmpty()) return;

                Point last = currentPath.get(currentPath.size() - 1);
                Point next = toGrid(e.getPoint());
                if (isValidMove(last, next)) {
                    currentPath.add(next);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (level == null) return;
                if (currentColor == null || currentPath.isEmpty()) return;

                Point last = currentPath.get(currentPath.size() - 1);
                Point[] anchors = level.anchorMap.get(currentColor);
                boolean endedOnAnchor = (last.equals(anchors[0]) || last.equals(anchors[1]));

                if (endedOnAnchor && currentPath.size() > 1) {
                    // Try to finalize path
                    if (!hasOverlap(currentPath, currentColor)) {
                        // Remove old path for this color
                        removeColor(currentColor);
                        // Fill new path
                        for (Point p : currentPath) {
                            board[p.x][p.y] = currentColor;
                        }
                        // Check win
                        if (isWin()) {
                            JOptionPane.showMessageDialog(BoardPanel.this,
                                    "Puzzle Solved! All pairs connected.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(BoardPanel.this,
                                "Path overlaps another color. Try again.");
                    }
                }

                currentPath.clear();
                currentColor = null;
                repaint();
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private Point toGrid(Point p) {
        int row = p.y / CELL_SIZE;
        int col = p.x / CELL_SIZE;
        return new Point(row, col);
    }

    private boolean inBounds(Point p) {
        return p.x >= 0 && p.x < level.rows && p.y >= 0 && p.y < level.cols;
    }

    /**
     * A valid move if:
     * 1) next is in bounds
     * 2) next is orthogonally adjacent to last
     * 3) next is empty or the other anchor for currentColor
     * 4) not already in currentPath
     */
    private boolean isValidMove(Point last, Point next) {
        if (!inBounds(next)) return false;
        if (currentPath.contains(next)) return false;

        boolean adjacent = (Math.abs(last.x - next.x) == 1 && last.y == next.y)
                || (Math.abs(last.y - next.y) == 1 && last.x == next.x);
        if (!adjacent) return false;

        Color cellColor = board[next.x][next.y];
        // next must be empty or the second anchor for currentColor
        if (cellColor == null) {
            return true;
        } else {
            // Check if it's the other anchor
            Point[] anchors = level.anchorMap.get(currentColor);
            if (next.equals(anchors[0]) || next.equals(anchors[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this path overlaps a different color's path in the board.
     */
    private boolean hasOverlap(List<Point> path, Color c) {
        for (Point p : path) {
            Color cellColor = board[p.x][p.y];
            if (cellColor != null && !cellColor.equals(c)) {
                // It's occupied by another color
                // (unless it's the final anchor, but that was allowed in isValidMove)
                // So any non-anchor cell with a different color => overlap
                if (!isAnchorOfColor(p, c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAnchorOfColor(Point p, Color c) {
        Point[] anchors = level.anchorMap.get(c);
        return p.equals(anchors[0]) || p.equals(anchors[1]);
    }

    /**
     * Remove all cells of the given color (except anchors).
     */
    private void removeColor(Color c) {
        Point[] anchors = level.anchorMap.get(c);
        Set<Point> anchorSet = new HashSet<>(Arrays.asList(anchors));
        for (int r = 0; r < level.rows; r++) {
            for (int col = 0; col < level.cols; col++) {
                if (board[r][col] == c) {
                    Point p = new Point(r, col);
                    if (!anchorSet.contains(p)) {
                        board[r][col] = null;
                    }
                }
            }
        }
    }

    /**
     * Win check:
     * 1) For each color, anchor1 must connect to anchor2 (via BFS).
     * 2) No null cells remain in the board (fully filled).
     */
    private boolean isWin() {
        // 1) Check connectivity
        for (Map.Entry<Color, Point[]> e : level.anchorMap.entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();
            if (!areAnchorsConnected(color, anchors[0], anchors[1])) {
                return false;
            }
        }

        // 2) Check no null cells
        for (int r = 0; r < level.rows; r++) {
            for (int c = 0; c < level.cols; c++) {
                if (board[r][c] == null) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean areAnchorsConnected(Color color, Point a, Point b) {
        Queue<Point> queue = new LinkedList<>();
        Set<Point> visited = new HashSet<>();
        queue.offer(a);
        visited.add(a);

        int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            if (cur.equals(b)) {
                return true;
            }
            for (int[] d : dirs) {
                int nr = cur.x + d[0];
                int nc = cur.y + d[1];
                Point nxt = new Point(nr, nc);
                if (inBounds(nxt) && !visited.contains(nxt)) {
                    if (color.equals(board[nr][nc])) {
                        visited.add(nxt);
                        queue.offer(nxt);
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // If no puzzle loaded yet, just show blank
        if (level == null || board == null) {
            g.setColor(Color.WHITE);
            g.drawString("Click 'Generate Puzzle' to begin.", 20, 20);
            return;
        }

        // Draw grid lines
        g.setColor(Color.DARK_GRAY);
        for (int r = 0; r <= level.rows; r++) {
            g.drawLine(0, r * CELL_SIZE, level.cols * CELL_SIZE, r * CELL_SIZE);
        }
        for (int c = 0; c <= level.cols; c++) {
            g.drawLine(c * CELL_SIZE, 0, c * CELL_SIZE, level.rows * CELL_SIZE);
        }

        // Draw filled cells
        for (int r = 0; r < level.rows; r++) {
            for (int c = 0; c < level.cols; c++) {
                Color cellColor = board[r][c];
                if (cellColor != null) {
                    fillCell(g, r, c, cellColor);
                }
            }
        }

        // Draw current path (in-progress)
        if (currentColor != null) {
            g.setColor(currentColor);
            for (Point p : currentPath) {
                int x = p.y * CELL_SIZE;
                int y = p.x * CELL_SIZE;
                g.fillRect(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
            }
        }

        // Draw anchors as circles on top
        for (Map.Entry<Color, Point[]> e : level.anchorMap.entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();
            for (Point anchor : anchors) {
                drawAnchor(g, anchor, color);
            }
        }
    }

    private void fillCell(Graphics g, int r, int c, Color color) {
        g.setColor(color);
        int x = c * CELL_SIZE;
        int y = r * CELL_SIZE;
        g.fillRect(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
    }

    private void drawAnchor(Graphics g, Point anchor, Color color) {
        g.setColor(color.darker());
        int x = anchor.y * CELL_SIZE + CELL_SIZE / 4;
        int y = anchor.x * CELL_SIZE + CELL_SIZE / 4;
        int size = CELL_SIZE / 2;
        g.fillOval(x, y, size, size);
    }
}
