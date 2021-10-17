package Cloture;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class which allows for various operations related to edge detection
 */
public class EdgeMap {

    // various layers of edge detection
    private final int[][] map;
    private final int[][] filledMap;
    private final int[][] outerEdges;
    private final int[][] zones;

    // the maximum and minimum flood x and y coordinates
    private int maxFloodX, minFloodX, maxFloodY, minFloodY;

    // the x-coordinates of the edge to the left and to the right of the flood
    private int leftFloodEdgeX, rightFloodEdgeX;

    // the number of zones
    private int zoneCount = 1;

    // the perimeter of the edge
    private double perimeter;

    // the map's width and height
    private final int MAP_WIDTH;
    private final int MAP_HEIGHT;

    /**
     * Class constructor
     * @param map the map to detect the edges of
     */
    public EdgeMap(int[][] map) {
        // sets the perimeter to a default of 0
        perimeter = 0;

        // saves the map height and length
        MAP_HEIGHT = map.length;
        MAP_WIDTH = map[0].length;

        // initialises the x coordinate of the last flood
        // edge to the right and the left of the flood
        leftFloodEdgeX = -1;
        rightFloodEdgeX = -1;

        // initialises the minimum and maximum
        // x and y coordinates of the flood
        minFloodX = MAP_WIDTH;
        minFloodY = MAP_HEIGHT;
        maxFloodX = maxFloodY = 0;

        // initialises the map
        this.map = new int[MAP_HEIGHT][MAP_WIDTH];
        // copies the map
        for (int y = 0; y < MAP_HEIGHT; y++) {
            this.map[y] = Arrays.copyOf(map[y], MAP_WIDTH);
        }

        // initialises the filled map layer
        this.filledMap = new int[MAP_HEIGHT][MAP_WIDTH];
        // fills the map layer
        for (int[] row: filledMap) {
            Arrays.fill(row, 0);
        }

        // initialises the outer edges layer
        this.outerEdges = new int[MAP_HEIGHT][MAP_WIDTH];
        // fills the outer edges layer
        for (int[] row: outerEdges) {
            Arrays.fill(row, 0);
        }

        // initialises the zones layer
        this.zones = new int[MAP_HEIGHT][MAP_WIDTH];
        // fills the zones layer
        for (int[] row: zones) {
            Arrays.fill(row, 0);
        }

        // generates each layer
        detectZones();
        detectOuterEdges();
        fillMap();

        // calculates the fencing perimeter
        perimeter = calculateFencePerimeter();
    }

    // =========================================
    //                  FLOOD
    // =========================================

    /**
     * Floods the layer with 1s
     * @param x the x coordinate at which to start the flood
     * @param y the y coordinate at which to start the flood
     * @param layer the layer to flood
     */
    private void flood(int x, int y, int[][] layer) {
        // calls the default flooding method with a default zone of 1
        flood(x, y, 1, layer);
    }

    /**
     * Flood the layer with a specific zone
     * @param x the x coordinate at which to start the flood
     * @param y the y coordinate at which to start the flood
     * @param zone the zone which will flood the layer
     * @param layer the layer to flood
     */
    private void flood(int x, int y, int zone, int[][] layer) {

        // initialises the flood at the specified starting point
        layer[y][x] = zone;

        // updates the variables used to check if the map is convex
        updateConvexVars(x, y);

        // checks the upper tile if there is one
        checkUpperFloodTile(x, y, zone, layer);

        // checks the lower tile if there is one
        checkLowerFloodTile(x, y, zone, layer);

        // checks the previous tile if there is one
        checkPreviousFloodTile(x, y, zone, layer);

        // checks the next tile if there is one
        checkNextFloodTile(x, y, zone, layer);

        // checks whether the map is convex
        checkConvex(x, y);

        // resets the variables used to check if the map is convex
        resetConvexVars();
    }

    // helper methods
    private void updateConvexVars(int x, int y) {
        if (y < minFloodY) {
            minFloodY = y;
        }
        if (y > maxFloodY) {
            maxFloodY = y;
        }
        if (x < minFloodX) {
            minFloodX = x;
        }
        if (x > maxFloodX) {
            maxFloodX = x;
        }

        // checks whether the tile to the left of the flood is an edge
        if (leftTileIsAnEdge(x, y)) {
            // updates the coordinates of the edge to the left of the flood
            leftFloodEdgeX = x;
        }
        // checks whether the tile to the right of the flood is an edge
        if (rightTileIsAnEdge(x, y)) {
            // updates the coordinates of the edge to the right of the flood
            rightFloodEdgeX = x;
        }
    }
    private void checkConvex(int x, int y) {
        // checks whether the flood is between two edges
        final boolean FLOOD_IS_BETWEEN_EDGES = (leftFloodEdgeX <= rightFloodEdgeX) &&
                ((leftFloodEdgeX != -1) && (rightFloodEdgeX != -1));

        final boolean FLOOD_HAS_REACHED_BORDER = (minFloodX == 0) || (minFloodY == 0) || (maxFloodX == MAP_WIDTH-1) || (maxFloodY == MAP_HEIGHT-1);

        // if the flood is not convex
        if (FLOOD_IS_BETWEEN_EDGES && FLOOD_HAS_REACHED_BORDER) {
            // formats error message
            String errorMessage = String.format("Map does not have correct format: exterior border is entering" +
                    "at position [%s][%s]", y, x);
            // throws a new error
            throw new IllegalArgumentException(errorMessage);
        }
    }
    private void resetConvexVars() {
        minFloodX = MAP_WIDTH;
        minFloodY = MAP_HEIGHT;
        maxFloodY = maxFloodX = 0;

        leftFloodEdgeX = rightFloodEdgeX = -1;
    }
    private void checkUpperFloodTile(int x, int y, int zone, int[][]layer) {
        // checks the upper tile if there is one
        if (y != 0 && layer[y-1][x] == 0) {
            layer[y-1][x] = zone;
            flood(x, y-1, zone, layer);
        }
    }
    private void checkLowerFloodTile(int x, int y, int zone, int[][]layer) {
        if (y < MAP_HEIGHT-1 && layer[y+1][x] == 0) {
            layer[y+1][x] = zone;
            flood(x, y+1, zone, layer);
        }
    }
    private void checkPreviousFloodTile(int x, int y, int zone, int[][]layer) {
        if (x != 0 && layer[y][x-1] == 0){
            layer[y][x-1] = zone;
            flood(x-1, y, zone, layer);
        }
    }
    private void checkNextFloodTile(int x, int y, int zone, int[][]layer) {
        if (x < MAP_WIDTH-1 && layer[y][x+1] == 0) {
            layer[y][x+1] = zone;
            flood(x+1, y, zone, layer);
        }
    }

    // =========================================
    //                  MAP
    // =========================================

    /**
     * Fills the map, removing any lake inside the perimeter
     */
    private void fillMap() {

        // clones the map to the filled map layer
        for (int y = 0; y < MAP_HEIGHT; y++) {
            filledMap[y] = Arrays.copyOf(outerEdges[y], MAP_WIDTH);
        }

        // the current tile coordinates;
        int y = 0;
        int x = 0;

        // the current tiles in the map layer and the outer edge layer
        int curMapTile = map[y][x];
        int curEdgeTile = outerEdges[y][x];

        while ((curMapTile == 0) || (curEdgeTile == 1)) {

            // if the tile has tiles above, bellow and to its left and right
            // (i.e. : we can check the value of these tiles)
            final boolean TILE_IS_SURROUNDED = (y != 0) && (y != MAP_HEIGHT - 1) && (x != 0) && (x != MAP_WIDTH - 1);

            if (TILE_IS_SURROUNDED) {

                // gets the values surrounding the tile
                int upperTile = map[y-1][x];
                int lowerTile = map[y+1][x];
                int leftTile = map[y][x-1];
                int rightTile = map[y][x+1];

                // if all these tiles are used exit the while loop
                final boolean TILE_IS_SURROUND_BY_ONES = (upperTile == 1) && (lowerTile == 1) &&
                                                         (leftTile == 1) && (rightTile == 1);

                if (TILE_IS_SURROUND_BY_ONES) break;
            }

            final boolean HAS_REACHED_END_OF_MAP = (x == MAP_WIDTH-1) && (y == MAP_HEIGHT-1);

            if (HAS_REACHED_END_OF_MAP) return;

            // while the tile hasn't been found move on to the next tile
            y = (y+1) % MAP_HEIGHT;
            x = (x+1) % MAP_WIDTH;

            // updates the tiles
            curMapTile = map[y][x];
            curEdgeTile = outerEdges[y][x];
        }

        // floods the map from there
        flood(x, y, filledMap);
    }

    // =========================================
    //               PERIMETER
    // =========================================

    /**
     * Perimeter getter
     * @return the edge fence perimeter
     */
    public double getFencePerimeter() {
        return perimeter;
    }

    /**
     * Calculates the fence's perimeter
     * @return the fence's perimeter
     */
    private double calculateFencePerimeter() {
        return getFenceCount() * 2.5;
    }

    /**
     * Gets the number of fences need to surround the perimeter
     * @return the number of fences needed
     */
    private double getFenceCount(){

        // the number of edges
        int fenceCount = 0;

        // loops through each row of the edges layer
        for (int y = 0; y < MAP_HEIGHT; y++) {

            // loops through each tile of the row
            for (int x = 0; x < MAP_WIDTH; x++) {

                // the current tile
                int curTile = outerEdges[y][x];

                // if the current tile is not an edge...
                if (curTile != 1) {
                    // ...moves on to the next tile
                    continue;
                }

                // if the current tile is on the top ro bottom border...
                if ((x == 0) || (x == MAP_WIDTH-1)) {
                    // ...adds an extra fence to it
                    fenceCount++;
                }
                // if the current tile is on the right or left border...
                if ((y == 0) || (y == MAP_HEIGHT-1)) {
                    // ...adds an extra fence to it
                    fenceCount++;
                }

                // checks whether the current tile has any adjacent empty tiles
                boolean hasEmptyAbove = upperTileIsEmpty(x, y);
                boolean hasEmptyBelow = lowerTileIsEmpty(x, y);
                boolean hasEmptyLeft  =  leftTileIsEmpty(x, y);
                boolean hasEmptyRight = rightTileIsEmpty(x, y);

                // if the tile above is empty...
                if (hasEmptyAbove) {
                    // ...counts it as an fence
                    fenceCount++;
                }

                // if the tile below is empty...
                if (hasEmptyBelow) {
                    // ...counts it as an fence
                    fenceCount++;
                }

                // if the tile to the left is empty...
                if (hasEmptyLeft) {
                    // ...counts it as an fence
                    fenceCount++;
                }

                // if the tile to the right is empty...
                if (hasEmptyRight) {
                    // ...counts it as an fence
                    fenceCount++;
                }
            }
        }

        // returns the final number of edges
        return fenceCount;
    }

    // helper methods
    private boolean upperTileIsEmpty(int x, int y) {
        // if there is no upper tile...
        if (y == 0) {
            // ...exits the method
            return false;
        }

        // if the tile above is empty
        return filledMap[y - 1][x] == 0;

        // the tile above is not empty
    }
    private boolean lowerTileIsEmpty(int x, int y) {

        // if there is no lower tile...
        if (y == MAP_HEIGHT-1) {
            // ...exits the method
            return false;
        }

        // if the tile below is empty
        return filledMap[y + 1][x] == 0;

        // the tile below is not empty
    }
    private boolean leftTileIsEmpty(int x, int y) {

        // if there is no left tile...
        if (x == 0) {
            // ...exits the method
            return false;
        }

        // if the tile to the left is empty
        return filledMap[y][x - 1] == 0;

        // the tile to the left is not empty
    }
    private boolean rightTileIsEmpty(int x, int y) {

        // if there is no left tile...
        if (x == MAP_WIDTH-1) {
            // ...exits the method
            return false;
        }

        // if the tile to the right is empty
        return filledMap[y][x + 1] == 0;

        // the tile to the right is not empty
    }

    // =========================================
    //              OUTER EDGES
    // =========================================

    /**
     * Detects the outer edges of the base map
     */
    private void detectOuterEdges() {
        // gets the outer zones
        final ArrayList<Integer> OUTER_ZONES = getOuterZones();

        // loops through every row in the map...
        for (int y = 0; y < MAP_HEIGHT; y++) {
            // loops through each tile in the row...
            for (int x = 0; x < MAP_WIDTH; x++) {

                // the current tile
                int curTile = zones[y][x];

                // if the upper tile is an edge, and it hasn't been counted yet...
                if (isAnOuterEdge(OUTER_ZONES, curTile) && upperTileIsAnEdge(x, y) && notInEdges(x, y-1)) {
                    // ...counts it as an edge
                    outerEdges[y-1][x] = 1;
                }

                // if the lower tile is an edge, and it hasn't been counted yet...
                if (isAnOuterEdge(OUTER_ZONES, curTile) && lowerTileIsAnEdge(x, y) && notInEdges(x, y+1)) {
                    // ...counts it as an edge
                    outerEdges[y+1][x] = 1;
                }

                // if the left tile is an edge, and it hasn't been counted yet...
                if (isAnOuterEdge(OUTER_ZONES, curTile) && leftTileIsAnEdge(x, y) && notInEdges(x-1, y)) {
                    // ...counts it as an edge
                    outerEdges[y][x-1] = 1;
                }

                // if the right tile is an edge, and it hast been counted yet...
                if (isAnOuterEdge(OUTER_ZONES, curTile) && rightTileIsAnEdge(x, y) && notInEdges(x+1, y)) {
                    // ...counts it as an edge
                    outerEdges[y][x+1] = 1;
                }
            }

            // Add edges along the upper and lower rows
            addRowEdges();
            // Add edges along the left and right edges
            addColumnEdges();
        }
    }

    // helper methods

    // procedural edges
    private boolean isAnOuterEdge(ArrayList<Integer> outerEdges, int tile) {
        // if the tile is an outer edge
        return outerEdges.contains(tile);
        // the tile is not an outer edge
    }
    private boolean notInEdges(int x, int y) {
        // if the edge has already been counted...
        // ...do not consider this edge against
        return outerEdges[y][x] != 1;
        // the edge hasn't been counted yet
    }
    private boolean upperTileIsAnEdge(int x, int y) {
        // if there is no upper tile...
        if (y == 0) {
            // ...exits the method
            return false;
        }

        // if the upper tile is used
        return map[y - 1][x] == 1;

        // the upper tile is not used
    }
    private boolean lowerTileIsAnEdge(int x, int y) {
        // if there is no lower tile...
        if (y == MAP_HEIGHT-1) {
            // ...exists the method
            return false;
        }

        // if the lower tile is used
        return map[y + 1][x] == 1;

        // the lower tile is not used
    }
    private boolean leftTileIsAnEdge(int x, int y) {
        // if there is no left tile...
        if (x == 0) {
            // ...exists the method
            return false;
        }

        // if the left tile is used
        return map[y][x - 1] == 1;

        // the left tile is not used
    }
    private boolean rightTileIsAnEdge(int x, int y) {
        // if there is no right tile...
        if (x == MAP_WIDTH-1) {
            // ...exists the method
            return false;
        }

        // if the right tile is used
        return map[y][x + 1] == 1;

        // if the right tile is not used
    }

    // map border edges
    private void addRowEdges() {
        // loops through all the tiles of the upper and lower rows
        for (int x = 0; x < MAP_WIDTH; x++) {

            // if the current tile along the upper row is a new edge...
            if (map[0][x] == 1 && notInEdges(x, 0)) {

                // ...adds it to the edges
                outerEdges[0][x] = 1;
            }

            // if the current tile along the lower edge is a new edge...
            if (map[MAP_HEIGHT - 1][x] == 1 && notInEdges(x, MAP_HEIGHT - 1)) {

                // ...adds it to the edges
                outerEdges[MAP_HEIGHT-1][x] = 1;
            }
        }
    }
    private void addColumnEdges() {

        // loops through the left and right edges
        for (int y = 0; y < MAP_HEIGHT; y++) {

            // if the current tile along the left edge is a new edge...
            if (map[y][0] == 1 && notInEdges(0, y)) {

                // ...adds it to the edges
                outerEdges[y][0] = 1;
            }

            // if the current tile along the right edge is a new edge...
            if (map[y][MAP_WIDTH - 1] == 1 && notInEdges(MAP_WIDTH - 1, y)) {

                // ...adds it to the edges
                outerEdges[y][MAP_WIDTH-1] = 1;
            }
        }
    }

    // =========================================
    //                  ZONES
    // =========================================

    /**
     * Detects the zones that compose the base map
     */
    private void detectZones() {

        // copies the original map to the zone array
        for (int y = 0; y < MAP_HEIGHT; y++) {
            zones[y] = Arrays.copyOf(map[y], MAP_WIDTH);
        }

        // loops through every line...
        for (int y = 0; y < MAP_HEIGHT; y++) {
            // loops through every tile in the line...
            for (int x = 0; x < MAP_WIDTH; x++) {
                // if the tile is unused...
                if (zones[y][x] == 0) {
                    // ...floods it
                    flood(x, y, ++zoneCount, zones);
                }
            }

        }

        // removes the ones in the zones layer
        removeOnes(zones);
    }

    private ArrayList<Integer> getOuterZones() {
        // the zones at the edge of the map
        ArrayList<Integer> outerZones = new ArrayList<>();

        // checks the tiles at the top of the map
        for (int tile : zones[0]) {

            // if the tile is part of a zone and that
            // zone isn't yet counted as an outer zone...
            if ((tile > 1) && !(outerZones.contains(tile))) {

                //...counts it as an outer tile
                outerZones.add(tile);
            }
        }

        // checks the tiles at the bottom of the map
        for (int tile : zones[MAP_HEIGHT-1]) {

            // if the tile is part of a zone and that
            // zone isn't yet counted as an outer zone...
            if ((tile > 1) && !(outerZones.contains(tile))) {

                //...counts it as an outer tile
                outerZones.add(tile);
            }
        }

        // checks the tiles to the left and the right of the map
        for (int y = 0; y < MAP_HEIGHT; y++) {

            // the current left tile
            int leftTile = zones[y][0];
            // the current right tile
            int rightTile = zones[y][MAP_WIDTH-1];

            // if the left tile is part of a zone and that
            // zone isn't yet counted as an outer zone...
            if ((leftTile > 1) && !(outerZones.contains(leftTile))) {

                //...counts it as an outer tile
                outerZones.add(leftTile);
            }

            // if the right tile is part of a zone and that
            // zone isn't yet counted as an outer zone...
            if ((rightTile > 1) && !(outerZones.contains(rightTile))) {

                //...counts it as an outer tile
                outerZones.add(rightTile);
            }
        }

        // return the outer zones
        return outerZones;
    }

    private ArrayList<Integer> getInnerZones() {
        // the zones inside the map
        ArrayList<Integer> innerZones = new ArrayList<>();

        // the outer zones
        ArrayList<Integer> outerZones = getOuterZones();

        // finds all the zones...
        for (int zoneNumber = 2; zoneNumber <= zoneCount; zoneNumber++) {
            // ...that are not outer zones...
            if (!outerZones.contains(zoneNumber)) {
                // ...adds it to the inner zones...
                innerZones.add(zoneNumber);
            }
        }

        // returns the inner zones
        return innerZones;
    }

    // helper method
    private void removeOnes(int[][] layer) {
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                if (layer[y][x] == 1) layer[y][x] = 0;
            }
        }
    }

    // =========================================
    //                 DISPLAYS
    // =========================================

    /**
     * Displays the base map
     */
    public void displayMap() {
        display(map);
    }

    /**
     * Displays the zones layer
     */
    public void displayZones() {
        display(zones);
    }

    /**
     * Displays the outer edges layer
     */
    public void displayOuterEdges() {
        display(outerEdges);
    }

    /**
     * Displays the filled map layer
     */
    public void displayFilledMap() {
        display(filledMap);
    }

    // helper method
    private void display(int[][] toDisplay) {
        // the string displaying the edges of the map
        StringBuilder finalDisplay = new StringBuilder();

        // converts each row to a string
        for (int[] row: toDisplay) {
            String rowString = Arrays.toString(row) + "\n";
            finalDisplay.append(rowString);
        }

        finalDisplay = new StringBuilder(finalDisplay.toString().replace("0", "_"));

        // displays the formatted result
        System.out.println(finalDisplay);
    }
}
