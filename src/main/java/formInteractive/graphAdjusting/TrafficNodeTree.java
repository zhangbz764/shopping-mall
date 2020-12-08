package formInteractive.graphAdjusting;

import formInteractive.graphAdjusting.spacialElements.Atrium;
import geometry.ZPoint;
import math.ZGeoMath;
import processing.core.PApplet;
import wblut.geom.WB_GeometryOp;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render3D;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2020/10/21
 * @time 10:24
 * @description the inner control node
 */
public class TrafficNodeTree extends TrafficNode {
    private final WB_Polygon boundary;
    private List<ZPoint> joints;  // join points on bisectors
    private Atrium atrium;
    private boolean atriumActive;

    /* ------------- constructor ------------- */

    public TrafficNodeTree(double x, double y, WB_Polygon boundary) {
        super(x, y);
        this.boundary = boundary;
    }

    public TrafficNodeTree(WB_Point p, WB_Polygon boundary) {
        super(p);
        this.boundary = boundary;
    }

    /* ------------- set & get ------------- */

    /**
     * @return void
     * @description set node location restricted in the boundary polygon
     */
    @Override
    public void setByRestriction(double pointerX, double pointerY) {
        WB_Point point = new WB_Point(pointerX, pointerY);
        if (WB_GeometryOp.contains2D(point, boundary) && WB_GeometryOp.getDistance2D(point, boundary) > this.getRegionR()) {
            this.set(pointerX, pointerY);
        }
    }

    /**
     * @return void
     * @description set join points on each bisector
     */
    @Override
    public void setJoints() {
        if (this.atrium == null) {
            if (this.getNeighbors() != null && this.getNeighbors().size() != 0) {
                this.joints = new ArrayList<>();
                if (this.isEnd()) {  // only has 1 neighbor, make its bisectors to an square cap
                    ZPoint reverse = this.sub(this.getNeighbors().get(0)).unit();
                    joints.add(this.add(reverse.rotate2D(Math.PI / 4).scaleTo(super.getRegionR())));
                    joints.add(this.add(reverse.rotate2D(Math.PI / -4).scaleTo(super.getRegionR())));
                } else {  // 2 or more neighbors, re-order vectors and get each angular bisector
                    ZPoint[] order = ZGeoMath.sortPolarAngle(this.getVecUnitToNeighbors());
                    for (int i = 0; i < order.length; i++) {
                        ZPoint bisector = ZGeoMath.getAngleBisectorOrdered(order[i], order[(i + 1) % order.length]);
                        double sin = Math.abs(order[i].cross2D(bisector));
                        joints.add(this.add(bisector.scaleTo(super.getRegionR() / sin)));
                    }
                }
            }
        } else {
            this.joints = atrium.getJointsFromAtrium();
        }
    }

//    @Override
//    public void setJoints() {
//        if (this.getNeighbor() != null && this.geiNeighborNum() != 0) {
//            this.joints = new ArrayList<>();
//            if (this.isEnd()) {  // only has 1 neighbor, make its bisectors to an square cap
//                ZPoint reverse = this.sub(this.getNeighbor().get(0)).unit();
//                joints.add(this.add(reverse.rotate2D(Math.PI / 4).scaleTo(super.getRegionR())));
//                joints.add(this.add(reverse.rotate2D(Math.PI / -4).scaleTo(super.getRegionR())));
//            } else {  // 2 or more neighbors, re-order vectors and get each angular bisector
//                ZPoint[] order = ZGeoMath.sortPolarAngle(this.getVecToNeighbor());
//                for (int i = 0; i < order.length; i++) {
//                    ZPoint bisector = ZGeoMath.getAngleBisectorOrdered(order[i], order[(i + 1) % order.length]);
//                    joints.add(this.add(bisector.scaleTo(super.getRegionR())));
//                }
//            }
//        }
//    }

    /**
     * @return void
     * @description initialize an atrium
     */
    @Override
    public void setAtrium() {
        if (this.getNeighbors() != null && this.getNeighbors().size() != 0) {
            this.atrium = new Atrium(this);
        } else {
            System.out.println("can't generate an atrium here");
        }
    }

    @Override
    public void clearAtrium() {
        this.atrium = null;
    }

    @Override
    public List<ZPoint> getJoints() {
        return this.joints;
    }

    @Override
    public String getNodeType() {
        return "TrafficNodeTree";
    }

    // ************ about atrium ************
    @Override
    public Atrium getAtrium() {
        return atrium;
    }

    @Override
    public boolean hasAtrium() {
        return this.atrium != null;
    }

    @Override
    public void setAtriumActive(boolean active) {
        this.atriumActive = active;
    }

    @Override
    public boolean isAtriumActive() {
        return atriumActive;
    }

    @Override
    public void switchAtriumControl() {
        atrium.switchActiveControl();
    }

    @Override
    public void updateAtriumLength(double delta) {
        atrium.updateLength(delta);
    }

    @Override
    public void updateAtriumWidth(double delta) {
        atrium.updateWidth(delta);
    }

    /* ------------- draw -------------*/

    @Override
    public void displayJoint(PApplet app, float r) {
        if (joints != null && joints.size() != 0) {
            for (ZPoint joint : joints) {
                joint.displayAsPoint(app, r);
            }
        }
    }

    @Override
    public void displayAtrium(WB_Render3D render, PApplet app) {
        if (this.atrium != null) {
            app.pushStyle();
            app.noFill();
            if (atriumActive) {
                app.stroke(255, 0, 0);
                this.atrium.display(render, app);
                app.stroke(0, 0, 255);
                this.atrium.displayActiveControl(app);
            } else {
                app.stroke(255, 0, 0);
                this.atrium.display(render, app);
            }
            app.popStyle();
        }
    }
}
