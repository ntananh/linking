package org.unfinitas.util;

import org.unfinitas.model.BoardModel;

import java.awt.Color;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Validates paths and moves according to game rules.
 * Follows the Strategy pattern for validation operations.
 */
public class PathValidator {
    // Direction vectors for BFS
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final BoardModel model;

    public PathValidator(BoardModel model) {
        this.model = model;
    }

    /**
     * A valid move if:
     * 1) next is in bounds
     * 2) next is orthogonally adjacent to last
     * 3) next is empty or the other anchor for currentColor
     * 4) not already in currentPath
     */
    public boolean isValidMove(Point last, Point next, Color currentColor, List<Point> currentPath) {
        if (!model.inBounds(next)) return false;

        // Check if already in path
        if (currentPath.contains(next)) return false;

        // Check adjacency
        boolean adjacent = (Math.abs(last.x - next.x) == 1 && last.y == next.y)
                || (Math.abs(last.y - next.y) == 1 && last.x == next.x);
        if (!adjacent) return false;

        // Next must be empty or an anchor of current color
        return model.isCellAvailable(next, currentColor);
    }

    /**
     * Check if this path overlaps a different color's path in the board.
     */
    public boolean hasPathOverlap(List<Point> path, Color c) {
        for (Point p : path) {
            Color cellColor = model.getCellColor(p);
            if (cellColor != null && !cellColor.equals(c) && !p.equals(path.get(path.size() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if two anchors of the same color are connected via a path.
     */
    public boolean areAnchorsConnected(Color color, Point a, Point b) {
        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[model.getLevel().rows()][model.getLevel().cols()];

        queue.offer(a);
        visited[a.x][a.y] = true;

        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            if (cur.equals(b)) {
                return true;
            }

            for (int[] d : DIRECTIONS) {
                int nr = cur.x + d[0];
                int nc = cur.y + d[1];
                Point nextPoint = new Point(nr, nc);

                if (model.inBounds(nextPoint) && !visited[nr][nc]) {
                    Color cellColor = model.getCellColor(nextPoint);
                    if (color.equals(cellColor)) {
                        visited[nr][nc] = true;
                        queue.offer(nextPoint);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Win check:
     * 1) For each color, anchor1 must connect to anchor2 (via BFS).
     * 2) No null cells remain in the board (fully filled).
     */
    public boolean isWin() {
        // 1) Check connectivity
        for (Map.Entry<Color, Point[]> e : model.getLevel().anchorMap().entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();
            if (!areAnchorsConnected(color, anchors[0], anchors[1])) {
                return false;
            }
        }

        // 2) Check no null cells
        return model.isBoardFull();
    }
}
