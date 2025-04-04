package org.unfinitas.dto;

import java.awt.*;
import java.util.Map;

/**
 * Represents a puzzle level: dimensions + anchor positions for each color.
 */
public record Level(int rows, int cols, Map<Color, Point[]> anchorMap) {
    public Level(int rows, int cols, Map<Color, Point[]> anchorMap) {
        this.rows = rows;
        this.cols = cols;
        this.anchorMap = Map.copyOf(anchorMap);
    }
}
