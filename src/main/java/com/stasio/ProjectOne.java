package com.stasio;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.vecmath.Point3f;
import java.io.Serial;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * This is the Main Class of your Game. It should boot up your game and do initial initialisation
 * Move your Logic into AppStates or Controls or other java classes
 */
public class ProjectOne extends SimpleApplication {

    private static final Logger logger = LogManager.getLogger(ProjectOne.class);

    public static final float TER_WIDTH = 0.475f;
    public static final float TER_HEIGHT = 0.475f;
    public static final float TER_THICKNESS = 0.03f;

    public static final float CHAR_WIDTH = 0.3f;
    public static final float CHAR_HEIGHT = 0.3f;
    public static final float CHAR_THICKNESS = 0.01f;
    public static final String PATH_LINE_GEOM_NAME = "PathLineGeom";

    public static float score;
    public static int characterHealth;

    public static final int VISIBLE_PATH_LENGTH = 13;

    private CharacterPlaying characterPlaying;

    public static final float PathMovementSpeed = -0.4f;
    private boolean showingPath = true;

    private static class CharacterPlaying {

        private static final Logger logger = LogManager.getLogger(CharacterPlaying.class);

        private final float width;
        private final float height;
        private final float thickenss;

        private Point3f location;

        private Point3f destination;

        private final String name;

        private Float moveIndicator;

        private Float proximityMargin;

        private final float moveSpeed;

        private final Function<Float, Float> quadratic = (z) -> z*z;

        private Geometry geometry;
        private CollisionShape collisionShape;

        public CharacterPlaying(float width, float height, float thickness, float moveSpeed) {
            this.width = width;
            this.height = height;
            this.thickenss = thickness;
            this.moveSpeed = moveSpeed;
            this.name = "Character";
        }


        public void place(float x, float y, float z) {
            this.location = new Point3f(x, y, z);
        }

        public void move(Point3f point, float x, float y, float z) {
            if (!(location.x - x < -8.6 || location.x + x > 7)) {
                point.set(point.x + x, point.y + y, point.z + z);
            }

        }

        public void loadCharacter(Node parent, AssetManager assetManager) {
            final Box character = new Box(width, height, thickenss);
            geometry = new Geometry(name, character);
            Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor("Color", ColorRGBA.Magenta);
            geometry.setMaterial(material);

            collisionShape = CollisionShapeFactory.createDynamicMeshShape(parent);
            geometry.move(location.x, location.y, location.z);

            parent.attachChild(geometry);

        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void movementRight() {
            if (location.x < 6.99f) {
                movementSetup(1.0f, 0f, 0f);
            }
        }

        public void movementLeft() {
            if (location.x > -7.99f) {
                movementSetup(-1.0f, 0f, 0f);
            }
        }

        public void movementFront() {
            // movementSetup(0f, 1.0f, 0f);
            move(location, 0f, 0.03f, 0f);
        }

        public void jumpFront() {
            movementSetup(0f, 1.0f, 1.0f);
        }

        public void movementBack() {
            movementSetup(0f, -1.0f, 0f);
        }

        private void movementSetup(float x, float y, float z) {
            if (moveIndicator == null) {
                moveIndicator = 0.0f;
                destination = new Point3f(location.x + x, location.y + y, location.z + z);

                proximityMargin = destination.distance(location) * 0.05f;
            }
        }

        public void move(float tpf, float pathSpeed) {
            if (moveIndicator != null) {
                move(destination, 0f, pathSpeed * tpf, 0f);
                if (moveIndicator >= 1.0f || proximityMargin >= location.distance(destination)) {
                    moveIndicator = null;
                    proximityMargin = null;
                    move(location, destination.x - location.x,
                            destination.y - location.y,
                            destination.z - location.z);
                } else { // ruch trwa
                    moveIndicator = Math.min(moveIndicator + tpf * moveSpeed, 1.0f);
                    move(location, (destination.x - location.x) * moveIndicator,
                            (destination.y - location.y) * moveIndicator,
                            (destination.z - location.z) * moveIndicator);
                }
            } else {
                move(location, 0f, pathSpeed * tpf, 0f);
            }
        }

        public void updateView() {
            final Vector3f localTranslation = geometry.getLocalTranslation();
            geometry.move(location.x - localTranslation.x, location.y - localTranslation.y, location.z - localTranslation.z);
        }

    }

    public static void main(String[] args) {
        ProjectOne app = new ProjectOne();
        app.showSettings = false;
        AppSettings appSettings = new AppSettings(true);
        appSettings.put("Width", 1920);
        appSettings.put("Height", 480);
        appSettings.put("Title", "Cell Runner");
        app.setSettings(appSettings);
        app.start();
    }

    private static class PathSource {
        private int row = 0;

        public int[] getNextRow() {
            int[] rowValues = path[path.length - row - 1];
            row = (row + 1) % path.length;
            return rowValues;
        }
    }

    private final PathSource pathSource = new PathSource();

    private final Queue<BatchNode> quePath = new LinkedList<>() {
        @Serial
        private static final long serialVersionUID = -6707803882461262867L;

        public boolean add(BatchNode node) {
            boolean result;
            if (this.size() < VISIBLE_PATH_LENGTH)
                result = super.add(node);
            else {
                super.removeFirst();
                result = super.add(node);
            }
            return result;
        }
    };


    /*public CollisionResults getcharactercollsision() {

        CollisionResults results = new CollisionResults();

        Collidable b = null;
        characterPlaying.geometry.collideWith(b=results.getClosestCollision().getGeometry(),results);
        results.getCollision(1);
        if(b!= null) {
            System.out.println("Number of Collisions between" +
                    characterPlaying.geometry.getName() + " and " + b.getClass() + ": " + results.size());
        }
        // Use the results
        if (results.size() > 0) {
            // how to react when a collision was detected
            CollisionResult closest = results.getClosestCollision();
            System.out.println("What was hit? " + closest.getGeometry().getName());
            System.out.println("Where was it hit? " + closest.getContactPoint());
            System.out.println("Distance? " + closest.getDistance());
        } else {
            System.out.println("no collision");
        }
        return results;

    }*/

    @Override
    public void simpleInitApp() {

        score = 0;
        characterHealth = 4;
        setDisplayStatView(false);
        setDisplayFps(false);
        this.cam.setLocation(cam.getLocation().add(0, 1, 0));
        this.flyCam.setEnabled(false);
        Hud hud = new Hud(guiFont, guiNode);
        loadCharacter();
        loadInitPath();
        initKeys();
        hud.UIBuilder();
        loadAudio();
    }
    private void loadAudio(){
        AudioNode music = new AudioNode(assetManager, "Sounds/cruising-down-8bit-lane-159615.wav", AudioData.DataType.Buffer);
        music.setVolume(1);
        music.playInstance();
    }

    private void loadCharacter() {
        characterPlaying = new CharacterPlaying(CHAR_WIDTH, CHAR_HEIGHT, CHAR_THICKNESS, 0.2f);
        characterPlaying.place(0, 1.0f, getTerrainElevationAt(0, 1.0f) + CHAR_THICKNESS);
        characterPlaying.loadCharacter(rootNode, assetManager);
    }

    private float getTerrainElevationAt(int i, float j) {
        log("getTerrainElevationAt(" + i + ", " + j + ")");
        for (Spatial geometry : rootNode.getChildren()) {
            if (geometry.getName().startsWith(PATH_LINE_GEOM_NAME)) {
                if (j > 0) {
                    j--;
                } else {
                    List<Spatial> tiles = ((BatchNode) geometry).getChildren();
                    List<String> tilesNames = tiles.stream().map(Spatial::getName).collect(Collectors.toList());
                    log("row: " + geometry.getName() + ", " + tilesNames);
                    log("height: " + tiles.get(i).getLocalTranslation().getZ());
                }
            }
        }
        return 0.1f;
    }

    @Override
    public void simpleUpdate(float tpf) {

//        logger.debug(characterPlaying.getGeometry().getLocalTranslation().getY());
        //getcharactercollsision();
        if (score / 10000 > 1) {
            viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        } else {
            viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 0.01f, 1f));
        }
//        System.out.println(score/2000);
        score++;
        //UIBuilder();
        //"x " +characterPlaying.location.x+ "\n" + "y " +characterPlaying.location.y
        System.out.println("z " +characterPlaying.location.z);

//        logger.debug(score / 100);
        //this.cam.lookAt(CharacterGeometry.getLocalTranslation(),cam.getLocation()); dont delete
        updatePathView(tpf);
        characterPlaying.move(tpf, PathMovementSpeed); // move the character with the path + update position during a
        characterPlaying.updateView();
        removeInvisiblePath();
        showPath();
    }

    private void updatePathView(float tpf) {
        for (Spatial geometry : rootNode.getChildren()) {
            if (geometry.getName().startsWith(PATH_LINE_GEOM_NAME)) {
                // move only the path here
                geometry.move((float) 0, PathMovementSpeed * tpf, 0);
            }
        }
    }

    private void removeInvisiblePath() {
        Set<Spatial> toBeRemoved = new LinkedHashSet<>();
        for (Spatial geometry : rootNode.getChildren()) {
            if (geometry.getName().startsWith(PATH_LINE_GEOM_NAME) && geometry.getLocalTranslation().getY() <= -4) {
                toBeRemoved.add(geometry);
            }
        }
        toBeRemoved.forEach(n -> rootNode.getChildren().remove(n));
        if (!toBeRemoved.isEmpty()) {
            log("Rows in the path: " + rootNode.getChildren().size());
            for (Spatial node : toBeRemoved) {
                addTopRow(node.getLocalTranslation().getY() + 1f * VISIBLE_PATH_LENGTH);
            }
        }
    }

    // movement binds
    private void initKeys() {
        /* You can map one or several inputs to one named mapping. */
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_J), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_K), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_I), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_M), new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE), new KeyTrigger(KeyInput.KEY_F));
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_MEMORY);

        /* Add the named mappings to the action listeners. */
        inputManager.addListener(analogListener, "Left", "Right", "Up", "Down", "Jump");
    }

    final private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {

            if (name.equals("Right")) {
                characterPlaying.movementRight();
            }
            if (name.equals("Left")) {
                characterPlaying.movementLeft();
            }
            if (name.equals("Up")) {
                characterPlaying.movementFront();
            }
            if (name.equals("Down")) {
                characterPlaying.movementBack();
            }
            if(name.equals("Jump")){

            }
        }
    };

    @Override
    public void simpleRender(RenderManager rm) {
        //add render code here (if any)
    }

    private void loadInitPath() {
        for (int i = 0; i < VISIBLE_PATH_LENGTH; i++) {
            addTopRow(i);
        }
        log("path created");
        showPath();
    }

    private void addTopRow(float topPosition) {
        List<Integer> line = Arrays.stream(pathSource.getNextRow()).boxed().collect(Collectors.toList());
        BatchNode rowBatchNode = createPathFragment(PATH_LINE_GEOM_NAME, line);
        rowBatchNode.setLocalTranslation(0, topPosition - 3, 0);
        quePath.offer(rowBatchNode);
    }

    private void showPath() {
        if (showingPath) {
            quePath.forEach(n -> {
                if (!rootNode.getChildren().contains(n)) {
                    rootNode.attachChild(n);
                }
            });
        }
    }

    private BatchNode createPathFragment(String rowId, List<Integer> line) {
        BatchNode geomRow = new BatchNode(rowId);
        for (int j = 0; j < line.size(); j++) {
            Integer terrain = line.get(j);
            Box box = getTerrain(terrain);
            Material mat = getMaterial(terrain);
            Geometry geomLine = new Geometry(String.format("%s_%3d", rowId, j));
            geomLine.setMesh(box);
            geomLine.setMaterial(mat);
            geomRow.attachChild(geomLine);
            geomLine.setLocalTranslation(j - line.size() / 2.0f, 0, 0);
        }
        return geomRow;
    }

    private Material getMaterial(int terrain) {
        Material mat;
        if (terrain == 10) {
            mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Green);
            //mat.setTexture("",ColorRGBA.Green);
        } else if (terrain == 20) {
            mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Brown);
        } else { // default
            mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Gray);
        }
        return mat;
    }

    private Box getTerrain(int terrain) {
        Box box;
        if (terrain >= 10) {
            box = new Box(TER_WIDTH, TER_HEIGHT, TER_THICKNESS * (1 + terrain - 10));
        } else { // default
            box = new Box(TER_WIDTH, TER_HEIGHT, TER_THICKNESS);
        }
        return box;
    }

    private void log(Object path_showed) {
        logger.debug(String.valueOf(path_showed));
    }

    private static int[][] path = new int[][]{
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32},
            {28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},
            {24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24},
            {20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20},
            {10, 10, 10, 10, 10, 10, 10, 20, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 20, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 5, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 5, 5, 5, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 5, 10, 5, 10, 5, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 5, 10, 10, 5, 10, 10, 5, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 5, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 5, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 5, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 5, 5, 5, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 5, 20, 5, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10}
    };
}