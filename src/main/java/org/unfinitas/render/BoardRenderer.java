package org.unfinitas.render;

import org.unfinitas.model.BoardModel;
import org.unfinitas.dto.Level;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Responsible for rendering the board.
 * Follows the Single Responsibility Principle by focusing only on rendering.
 */
public class BoardRenderer {
    private static final int CELL_SIZE = 60;
    private static final int CELL_PADDING = 5;
    private static final int ANCHOR_SIZE = CELL_SIZE * 3 / 5;
    private static final int PATH_STROKE_WIDTH = 4;
    private static final int CORNER_RADIUS = 8;
    private static final Color GRID_LINE_COLOR = new Color(70, 70, 70);

    private final BoardModel model;

    public BoardRenderer(BoardModel model) {
        this.model = model;
    }

    public void render(Graphics2D g) {
        // Enable anti-aliasing for smoother graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the grid, cells, and anchors
        drawGrid(g);
        drawFilledCells(g);
        drawAnchors(g);
    }

    public void renderInstructions(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Click 'Generate Puzzle' to begin.", 20, 40);
    }

    public void renderCurrentPath(Graphics2D g, List<Point> currentPath, Color currentColor) {
        if (currentPath == null || currentPath.isEmpty() || currentColor == null) return;

        g.setColor(currentColor);
        g.setStroke(new BasicStroke(PATH_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < currentPath.size(); i++) {
            Point p = currentPath.get(i);
            int x = p.y * CELL_SIZE;
            int y = p.x * CELL_SIZE;

            // Draw path cells
            g.fillRoundRect(x + CELL_PADDING, y + CELL_PADDING,
                    CELL_SIZE - (2 * CELL_PADDING),
                    CELL_SIZE - (2 * CELL_PADDING),
                    CORNER_RADIUS, CORNER_RADIUS);

            // Draw connecting lines between cells
            if (i > 0) {
                Point prev = currentPath.get(i - 1);
                int prevX = prev.y * CELL_SIZE + CELL_SIZE / 2;
                int prevY = prev.x * CELL_SIZE + CELL_SIZE / 2;
                int currX = x + CELL_SIZE / 2;
                int currY = y + CELL_SIZE / 2;

                // Draw small line segments between points
                int segmentLength = CELL_SIZE / 4;
                double angle = Math.atan2(currY - prevY, currX - prevX);

                int startX = prevX + (int)(segmentLength * Math.cos(angle));
                int startY = prevY + (int)(segmentLength * Math.sin(angle));

                int endX = currX - (int)(segmentLength * Math.cos(angle));
                int endY = currY - (int)(segmentLength * Math.sin(angle));

                g.drawLine(startX, startY, endX, endY);
            }
        }
    }

    private void drawGrid(Graphics2D g) {
        Level level = model.getLevel();
        g.setColor(GRID_LINE_COLOR);

        for (int r = 0; r <= level.rows(); r++) {
            g.drawLine(0, r * CELL_SIZE, level.cols() * CELL_SIZE, r * CELL_SIZE);
        }

        for (int c = 0; c <= level.cols(); c++) {
            g.drawLine(c * CELL_SIZE, 0, c * CELL_SIZE, level.rows() * CELL_SIZE);
        }
    }

    private void drawFilledCells(Graphics2D g) {
        Level level = model.getLevel();

        for (int r = 0; r < level.rows(); r++) {
            for (int c = 0; c < level.cols(); c++) {
                Point p = new Point(r, c);
                Color cellColor = model.getCellColor(p);
                if (cellColor != null && !model.isAnchor(p)) {
                    fillCell(g, r, c, cellColor);
                }
            }
        }
    }

    private void drawAnchors(Graphics2D g) {
        for (Map.Entry<Color, Point[]> e : model.getLevel().anchorMap().entrySet()) {
            Color color = e.getKey();
            Point[] anchors = e.getValue();
            for (Point anchor : anchors) {
                drawAnchor(g, anchor, color);
            }
        }
    }

    private void fillCell(Graphics2D g, int r, int c, Color color) {
        g.setColor(color);
        int x = c * CELL_SIZE;
        int y = r * CELL_SIZE;

        g.fillRoundRect(x + CELL_PADDING, y + CELL_PADDING,
                CELL_SIZE - (2 * CELL_PADDING),
                CELL_SIZE - (2 * CELL_PADDING),
                CORNER_RADIUS, CORNER_RADIUS);
    }

    private void drawAnchor(Graphics2D g, Point anchor, Color color) {
        // Make anchor slightly larger and with a border
        g.setColor(color.darker());
        int x = anchor.y * CELL_SIZE + (CELL_SIZE - ANCHOR_SIZE) / 2;
        int y = anchor.x * CELL_SIZE + (CELL_SIZE - ANCHOR_SIZE) / 2;

        // Fill circle
        g.fillOval(x, y, ANCHOR_SIZE, ANCHOR_SIZE);

        // Add white border
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x, y, ANCHOR_SIZE, ANCHOR_SIZE);
    }

    public static int getCellSize() {
        return CELL_SIZE;
    }
}
