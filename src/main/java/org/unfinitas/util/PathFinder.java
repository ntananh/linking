package org.unfinitas.util;

import org.unfinitas.model.BoardModel;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Utility class that finds paths between points on the board.
 * Implementation uses a combination of pathfinding algorithms to find optimal paths.
 */
public class PathFinder {
    // Direction vectors for path finding (up, right, down, left)
    private static final int[][] DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    /**
     * Finds a path between two points on the board.
     * The path will only go through empty cells or the start/end points.
     *
     * @param model The board model
     * @param color The color of the path
     * @param start The starting point
     * @param end The ending point
     * @return A list of points representing the path, or null if no path exists
     */
    public static List<Point> findPath(BoardModel model, Color color, Point start, Point end) {
        // Try A* algorithm first for most efficient path
        List<Point> path = findAStarPath(model, color, start, end);

        // If A* fails, try DFS for more exhaustive search
        if (path == null || path.isEmpty()) {
            path = findDFSPath(model, color, start, end);
        }

        return path;
    }

    /**
     * Finds a path using the A* algorithm.
     * A* is efficient for finding the shortest path in most cases.
     */
    private static List<Point> findAStarPath(BoardModel model, Color color, Point start, Point end) {
        int rows = model.getLevel().rows();
        int cols = model.getLevel().cols();

        // Track visited cells and their parent for path reconstruction
        boolean[][] visited = new boolean[rows][cols];
        Point[][] parent = new Point[rows][cols];

        // Priority queue for A* ordered by cost
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));

        // Start with the starting point
        queue.offer(new Node(start, 0, manhattanDistance(start, end)));
        visited[start.x][start.y] = true;

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Point currentPoint = current.point;

            // Check if we've reached the end
            if (currentPoint.equals(end)) {
                return reconstructPath(parent, start, end);
            }

            // Try all four directions
            for (int[] dir : DIRECTIONS) {
                int newRow = currentPoint.x + dir[0];
                int newCol = currentPoint.y + dir[1];
                Point nextPoint = new Point(newRow, newCol);

                if (isValidStep(model, nextPoint, end, visited, color)) {
                    visited[newRow][newCol] = true;
                    parent[newRow][newCol] = currentPoint;

                    int newCost = current.gCost + 1;
                    int heuristic = manhattanDistance(nextPoint, end);
                    int totalCost = newCost + heuristic;

                    queue.offer(new Node(nextPoint, newCost, totalCost));
                }
            }
        }

        // No path found
        return null;
    }

    /**
     * Finds a path using depth-first search.
     * DFS can find paths that A* might miss in certain board configurations.
     */
    private static List<Point> findDFSPath(BoardModel model, Color color, Point start, Point end) {
        int rows = model.getLevel().rows();
        int cols = model.getLevel().cols();

        boolean[][] visited = new boolean[rows][cols];
        List<Point> path = new ArrayList<>();
        path.add(start);
        visited[start.x][start.y] = true;

        if (dfsHelper(model, color, start, end, visited, path)) {
            return path;
        }

        return null;
    }

    /**
     * Helper method for DFS path finding.
     */
    private static boolean dfsHelper(BoardModel model, Color color, Point current, Point end,
                                     boolean[][] visited, List<Point> path) {
        if (current.equals(end)) {
            return true;
        }

        // Try all four directions
        for (int[] dir : DIRECTIONS) {
            int newRow = current.x + dir[0];
            int newCol = current.y + dir[1];
            Point next = new Point(newRow, newCol);

            if (isValidStep(model, next, end, visited, color)) {
                visited[newRow][newCol] = true;
                path.add(next);

                if (dfsHelper(model, color, next, end, visited, path)) {
                    return true;
                }

                // Backtrack
                path.remove(path.size() - 1);
            }
        }

        return false;
    }

    /**
     * Checks if a move to the given point is valid.
     */
    private static boolean isValidStep(BoardModel model, Point p, Point end, boolean[][] visited, Color color) {
        // Check bounds
        if (!model.inBounds(p)) {
            return false;
        }

        // Check if already visited
        if (visited[p.x][p.y]) {
            return false;
        }

        // Valid if empty, is the end point, or is an anchor of this color
        Color cellColor = model.getCellColor(p);
        return cellColor == null || p.equals(end) ||
                (model.isAnchor(p) && cellColor.equals(color));
    }

    /**
     * Calculates the Manhattan distance between two points.
     * Used as a heuristic for A* pathfinding.
     */
    private static int manhattanDistance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    /**
     * Reconstructs a path from the parent pointers.
     */
    private static List<Point> reconstructPath(Point[][] parent, Point start, Point end) {
        List<Point> path = new LinkedList<>();
        Point current = end;

        // Work backwards from end to start
        while (current != null) {
            path.add(0, current);
            if (current.equals(start)) {
                break;
            }
            current = parent[current.x][current.y];
        }

        return path;
    }

    /**
     * Node class for A* algorithm.
     */
    private static class Node {
        Point point;
        int gCost; // Cost from start to this node
        int cost;  // Total cost (g + h)

        Node(Point point, int gCost, int cost) {
            this.point = point;
            this.gCost = gCost;
            this.cost = cost;
        }
    }
}
