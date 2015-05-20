import framework.Spline;

import java.util.ArrayList;

/**
 * Created by eli on 4/8/15.
 */
public class Controller {

    public CatmullRomSpline track;
    public Spline track2;
    public float fromX;
    public float fromY;
    public float fromZ;
    public float toX;
    public float toY;
    public float toZ;
    public float upX;
    public float upY;
    public float upZ;
    public float spline_path;
    public float speed;
    public float roll;
    public float yaw;
    public float pitch;
    public float up;
    public float side;
    public float currentX;
    public float currentY;
    public float currentZ;
    public float movement;
    public float sPosX = 0.0f;
    public float sPosY = 0.0f;
    public float sPosZ = 0.0f;
    public float sDerX = 0.0f;
    public float sDerY = 0.0f;
    public float sDerZ = 0.0f;
    public float sNormX = 0.0f;
    public float sNormY = 0.0f;
    public float sNormZ = 0.0f;
    public float sBinormX = 0.0f;
    public float sBinormY = 0.0f;
    public float sBinormZ = 0.0f;


    public Controller(float fx, float fy, float fz, float tx, float ty, float tz,
                      float ux, float uy, float uz, String path) {
        track = new CatmullRomSpline(path, 100);
        track2 = new Spline(track.getOriginalPoints());
        fromX = fx;
        fromY = fy;
        fromZ = fz;
        toX = tx;
        toY = ty;
        toZ = tz;
        upX = ux;
        upY = uy;
        upZ = uz;
        spline_path = 0.0f;
        speed = 0.0f;
        roll = 0.0f;
        yaw = 0.0f;
        pitch = 0.0f;
        up = 0.0f;
        side = 0.0f;
        movement = 0.0f;
        currentX = fx;
        currentY = fy;
        currentZ = fz;
    }

    public void adjustPosition(float x, float y, float z) {
        currentX += x;
        currentY += y;
        currentZ += z;
    }

    public void updateSplineVars() {
        sPosX = track2.evaluateAt(spline_path)[0];
        sPosY = track2.evaluateAt(spline_path)[1];
        sPosZ = track2.evaluateAt(spline_path)[2];
        sDerX = track2.normalize(track2.evaluateDerivativeAt(spline_path))[0];
        sDerY = track2.normalize(track2.evaluateDerivativeAt(spline_path))[1];
        sDerZ = track2.normalize(track2.evaluateDerivativeAt(spline_path))[2];
        sNormX = track2.evaluateNormal(spline_path)[0];
        sNormY = track2.evaluateNormal(spline_path)[1];
        sNormZ = track2.evaluateNormal(spline_path)[2];
        sBinormX = track2.evaluateBinormal(spline_path)[0];
        sBinormY = track2.evaluateBinormal(spline_path)[1];
        sBinormZ = track2.evaluateBinormal(spline_path)[2];
    }

    public void updateSplineVars (float res) {
        sPosX = track.evaluateDifferently(spline_path)[0];
        sPosY = track.evaluateDifferently(spline_path)[1];
        sPosZ = track.evaluateDifferently(spline_path)[2];
        sDerX = track.findLookAt(spline_path, res)[0];
        sDerY = track.findLookAt(spline_path, res)[1];
        sDerZ = track.findLookAt(spline_path, res)[2];
        sNormX = track.evaluateNormalDifferently(spline_path, res)[0];
        sNormY = track.evaluateNormalDifferently(spline_path, res)[1];
        sNormZ = track.evaluateNormalDifferently(spline_path, res)[2];
    }

    public void resetAll() {
        spline_path = 0.00f;
        speed = 0.001f;
        roll = 0.0f;
        yaw = 0.0f;
        pitch = 0.0f;
        movement = 0.0f;
        up = 0.0f;
        side = 0.0f;
        currentX = fromX;
        currentY = fromY;
        currentZ = fromZ;
    }
}
