package webMain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import converter.WB_Converter;
import formInteractive.graphAdjusting.TrafficGraph;
import geometry.Segments;
import geometry.Vertices;
import geometry.ZEdge;
import main.ArchiJSON;
import org.locationtech.jts.geom.LineString;
import processing.core.PApplet;
import render.JtsRender;
import transform.ZTransform;
import wblut.geom.WB_Coord;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.geom.WB_Segment;
import wblut.processing.WB_Render;

import java.util.ArrayList;
import java.util.List;

/**
 * main generator
 * catch data from frontend
 * convert data to frontend
 *
 * @author ZHANG Bai-zhou zhangbz
 * @project archijson
 * @date 2021/3/10
 * @time 17:55
 */
public class MallGenerator {
    // display switch
    private boolean draw = false;

    // geometries received from frontend
    private WB_Polygon boundary_receive;            // 轮廓（共用）
    private List<WB_Point> innerNode_receive;       // 内部控制点（共用）
    private List<WB_Point> entryNode_receive;       // 轮廓控制点（共用）
    private List<WB_Polygon> polyAtrium_receive;    // 中庭多边形（共用）

    private List<LineString> bufferCurve_receive;   // 动线边界曲线（不同层）
    private List<WB_Polygon> cellPolys_receive;     // 商铺剖分多边形（不同层）

    // core generate result: floors
    private MallFloor[] floors;

    private boolean drawGraph = false;

    /* ------------- constructor ------------- */

    public MallGenerator() {

    }

    public void init() {
        this.floors = new MallFloor[4];
        for (int i = 0; i < floors.length; i++) {
            floors[i] = new MallFloor(i + 1, boundary_receive);
            floors[i].setStatus(0);
        }
    }

    /* ------------- generating ------------- */

    /**
     * update traffic graph and buffer of current floor
     *
     * @param floorNum    current floor number
     * @param dist        buffer distance
     * @param curvePtsNum curve subdivision number
     * @return void
     */
    public void generateGraphAndBuffer(int floorNum, double dist, int curvePtsNum) {
        if (floorNum == 1) {
            this.floors[floorNum - 1].updateGraph(innerNode_receive, entryNode_receive);
        } else {
            this.floors[floorNum - 1].updateGraph(innerNode_receive, new ArrayList<WB_Point>());
        }
        this.floors[floorNum - 1].updateBuffer(polyAtrium_receive, dist, curvePtsNum);

        this.floors[floorNum - 1].setStatus(1);
    }

    /**
     * update block split and subdivision
     *
     * @param floorNum current floor number
     * @param span     span to subdivision
     * @return void
     */
    public void generateSubdivision(int floorNum, double span) {
        this.floors[floorNum - 1].updatePolygonizer(bufferCurve_receive);
        this.floors[floorNum - 1].updateSubdivision(span);

        this.floors[floorNum - 1].setStatus(2);
    }

    /**
     * generate positions of evacuate stairways
     *
     * @param floorNum current floor number
     * @return void
     */
    public void generateEvacuation(int floorNum) {
        this.floors[floorNum - 1].updateEvacuation(cellPolys_receive);

        this.floors[floorNum - 1].setStatus(3);
    }

    /* ------------- JSON converting ------------- */

    /**
     * convert backend geometries to ArchiJSON
     * graph segments, buffer control points
     *
     * @param floorNum current floor number
     * @param clientID
     * @param gson
     * @return main.ArchiJSON
     */
    public ArchiJSON toArchiJSONGraphAndBuffer(int floorNum, String clientID, Gson gson) {
        // initializing
        ArchiJSON json = new ArchiJSON();
        json.setId(clientID);
        List<JsonElement> elements = new ArrayList<>();

        // preparing data
        List<WB_Segment> graphSegments = floors[floorNum - 1].getGraph().toWB_Segments();
        List<List<WB_Coord>> controlPoints = floors[floorNum - 1].getBufferControlPoints();

        // converting to json
        for (WB_Segment seg : graphSegments) {
            Segments segments = WB_Converter.toSegments(seg);
            JsonObject prop1 = new JsonObject();
            prop1.addProperty("name", "treeEdges");
            segments.setProperties(prop1);
            elements.add(gson.toJsonTree(segments));
        }
        for (List<WB_Coord> splitPointsEach : controlPoints) {
            Vertices bufferControlPointsEach = WB_Converter.toVertices(splitPointsEach, 3);
            JsonObject prop2 = new JsonObject();
            prop2.addProperty("name", "bufferControl");
            bufferControlPointsEach.setProperties(prop2);
            elements.add(gson.toJsonTree(bufferControlPointsEach));
        }

        // setup json
        json.setGeometryElements(elements);
        return json;
    }

    /**
     * convert backend geometries to ArchiJSON
     * first-level subdivision cells
     *
     * @param floorNum current floor number
     * @param clientID
     * @param gson
     * @return main.ArchiJSON
     */
    public ArchiJSON toArchiJSONSubdivision(int floorNum, String clientID, Gson gson) {
        // initializing
        ArchiJSON json = new ArchiJSON();
        json.setId(clientID);
        List<JsonElement> elements = new ArrayList<>();

        // preparing data
        List<WB_Polygon> allCells = floors[floorNum - 1].getAllCells();

        // converting to json
        for (WB_Polygon p : allCells) {
            Segments cell = WB_Converter.toSegments(p);
            JsonObject prop = new JsonObject();
            prop.addProperty("name", "shopCell");
            cell.setProperties(prop);
            elements.add(gson.toJsonTree(cell));
        }

        // setup json
        json.setGeometryElements(elements);
        return json;
    }

    /**
     * convert backend geometries to ArchiJSON
     * evacuation points & segments
     *
     * @param floorNum current floor number
     * @param clientID
     * @param gson
     * @return main.ArchiJSON
     */
    public ArchiJSON toArchiJSONEvacuation(int floorNum, String clientID, Gson gson) {
        // initializing
        ArchiJSON json = new ArchiJSON();
        json.setId(clientID);
        List<JsonElement> elements = new ArrayList<>();

        // preparing data
        List<WB_Point> evacuationPoints = floors[floorNum - 1].getEvacuationPoint();

        // converting to json
        Vertices evacuation = WB_Converter.toVertices(evacuationPoints, 3);
        JsonObject prop = new JsonObject();
        prop.addProperty("name", "evacuation");
        evacuation.setProperties(prop);
        elements.add(gson.toJsonTree(evacuation));

        // setup json
        json.setGeometryElements(elements);
        return json;
    }


    public ArchiJSON toArchiJSONFloor(int floorNum, int status, String clientID, Gson gson) {
        // initializing
        ArchiJSON json = new ArchiJSON();
        json.setId(clientID);
        List<JsonElement> elements = new ArrayList<>();

        // preparing data
        List<WB_Segment> graphSegments = floors[floorNum - 1].getGraph().toWB_Segments();
        List<List<WB_Coord>> controlPoints = floors[floorNum - 1].getBufferControlPoints();

        List<WB_Polygon> allCells = floors[floorNum - 1].getAllCells();

        List<WB_Point> evacuationPoints = floors[floorNum - 1].getEvacuationPoint();

        // setup json
        json.setGeometryElements(elements);
        return json;
    }

    /* ------------- setter & getter ------------- */

    public void setBoundary_receive(WB_Polygon boundary_receive) {
        this.boundary_receive = ZTransform.validateWB_Polygon(boundary_receive);
    }

    public void setInnerNode_receive(List<WB_Point> innerNode_receive) {
        this.innerNode_receive = innerNode_receive;
    }

    public void setEntryNode_receive(List<WB_Point> entryNode_receive) {
        this.entryNode_receive = entryNode_receive;
    }

    public void setPolyAtrium_receive(List<WB_Polygon> polyAtrium_receive) {
        this.polyAtrium_receive = polyAtrium_receive;
    }

    public void setBufferCurve_receive(List<LineString> bufferCurve_receive) {
        this.bufferCurve_receive = bufferCurve_receive;
    }

    public void setCellPolys_receive(List<WB_Polygon> cellPolys_receive) {
        this.cellPolys_receive = cellPolys_receive;
    }

    public MallFloor[] getFloors() {
        return floors;
    }

    /* ------------- draw ------------- */

    public void draw(PApplet app) {
        if (draw) {
            WB_Render render = new WB_Render(app);
            JtsRender jtsRender = new JtsRender(app);

            app.stroke(0);
            app.noFill();

            render.drawPolygonEdges(boundary_receive);

            if (bufferCurve_receive != null) {
                for (LineString ls : bufferCurve_receive) {
                    jtsRender.drawGeometry(ls);
                }
            }
        }
    }
}