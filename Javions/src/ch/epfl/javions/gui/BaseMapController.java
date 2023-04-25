package ch.epfl.javions.gui;

import ch.epfl.javions.GeoPos;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import java.io.IOException;


public final class BaseMapController {

    public static final int PIXEL_PER_TILE = 256;
    private final TileManager tileManager;
    private final MapParameters mapParameters;
    private boolean redrawNeeded;
    private final Canvas canvas;
    private final Pane mainPane;

    public BaseMapController(TileManager tileManager, MapParameters mapParameters) {
        this.tileManager = tileManager;
        this.mapParameters = mapParameters;

        canvas = new Canvas();
        mainPane = new Pane();
        mainPane.getChildren().add(canvas);

        canvas.widthProperty().bind(mainPane.widthProperty());
        canvas.heightProperty().bind(mainPane.heightProperty());
        canvas.sceneProperty().addListener((p, oldS, newS) -> {
            assert oldS == null;
            newS.addPreLayoutPulseListener(this::redrawIfNeeded);
        });
    }

    public Pane pane() {
        return mainPane;
    }

    public void centerOn(GeoPos geoPos) {
    }


    private void redrawIfNeeded() {
        if (!redrawNeeded) return;
        redrawNeeded = false;

        GraphicsContext context = canvas.getGraphicsContext2D();
        double xImageSpace = mapParameters.getMinX() % PIXEL_PER_TILE;
        double yImageSpace = mapParameters.getMinY() % PIXEL_PER_TILE;

        for (int x = 0; x < canvas.getWidth() + PIXEL_PER_TILE; x += PIXEL_PER_TILE) {
            for (int y = 0; y < canvas.getHeight() + PIXEL_PER_TILE; y += PIXEL_PER_TILE) {
                int tileXCoordinate = Math.floorDiv((int) (mapParameters.getMinX() + x), PIXEL_PER_TILE);
                int tileYCoordinate = Math.floorDiv((int) (mapParameters.getMinY() + y), PIXEL_PER_TILE);

                if (TileManager.TileId.isValid(mapParameters.getZoomLevel(), tileXCoordinate, tileYCoordinate)) {
                    TileManager.TileId tiles = new TileManager.TileId(mapParameters.getZoomLevel(),
                            tileXCoordinate,
                            tileYCoordinate);
                    try {
                        context.drawImage(tileManager.imageForTileAt(tiles), x - xImageSpace, y - yImageSpace);
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }


    private void redrawOnNextPulse() {
        redrawNeeded = true;
        Platform.requestNextPulse();
    }

}
