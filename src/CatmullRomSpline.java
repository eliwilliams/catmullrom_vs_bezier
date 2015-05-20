import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.gl2.GLUT;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.util.*;

import javax.media.opengl.GL2;

/**
 * Created by eli on 4/8/15.
 * Credit for base class:
 *
 * @author Robert C. Duvall
 */

public class CatmullRomSpline implements Iterable<float[]> {

    private float[] currentBinormal = new float[]{1f, 0f, 0f};
    private float[] currentNormal = new float[]{0f, 1f, 0f};
    private float[] currentTangent = new float[]{1f, 1f, 1f};
    private List<float[]> interpolatedPoints = new ArrayList<>();
    private List<float[]> myControlPoints = new ArrayList<>();
    private ArrayList<float[]> pallet = new ArrayList<>();


    /**
     * Create empty curve.
     */
    public CatmullRomSpline (float[] controlPoints, int numPoints) {
        // BUGBUG: check that it is a multiple of 3
        for (int k = 0; k < controlPoints.length; k += 3) {
            addPoint(controlPoints[k], controlPoints[k+1], controlPoints[k+2]);
        }
        init(numPoints, 3);
    }

    /**
     * Create curve from the control points listed in the given file.
     */
    @SuppressWarnings("resource")
    public CatmullRomSpline (String filename, int numPoints) {
        try {
            Scanner input = new Scanner(new File(filename));
            input.nextLine();  // read starting comment
            while (input.hasNextLine()) {
                Scanner line = new Scanner(input.nextLine());
                addPoint(line.nextFloat(), line.nextFloat(), line.nextFloat());
            }
            input.close();
        } catch (FileNotFoundException e) {
            // BUGBUG: not the best way to handle this error
            e.printStackTrace();
            System.exit(1);
        }
        init(numPoints, 3);
    }

    public ArrayList<float[]> getOriginalPoints() {
        return (ArrayList<float[]>) myControlPoints;
    }

    private void init(int pts, int pps) {
        pallet.add(new float[]{1, 0, 0});
        pallet.add(new float[]{1, 0, 1});
        pallet.add(new float[]{0, 1, 0});
        pallet.add(new float[]{1, 1, 0});
        pallet.add(new float[]{0, 0, 1});
        currentBinormal = new float[]{1f, 0f, 0f};
        currentNormal = new float[]{0f, 1f, 0f};
        currentTangent = new float[]{0f, 0f, 0f};
        while(numControlPoints() < pts) {
            makeRandomPoint();
        }
        interpolatedPoints = calculateInterpolation(pps);
        for(int i = 0; i < interpolatedPoints.size(); i++) {
        }
    }

    public List<float[]> calculateInterpolation(int n) {
        List<float[]> result = new ArrayList<>();
        // When looping, remember that each cycle requires 4 points, starting
        // with i and ending with i+3.  So we don't loop through all the points.
        for (int i = 0; i < myControlPoints.size() - 3; i++) {
            // Actually calculate the Catmull-Rom curve for one segment.
            List<float[]> points = interpolate(myControlPoints, i, n);
            // Since the middle points are added twice, once for each bordering
            // segment, we only add the 0 index result point for the first
            // segment.  Otherwise we will have duplicate points.
            if (result.size() > 0) {
                points.remove(0);
            }
            // Add the coordinates for the segment to the result list.
            result.addAll(points);
        }
        return result;
    }

    public static List<float[]> interpolate(List<float[]> points, int index, int pointsPerSegment) {
        List<float[]> result = new ArrayList<>();
        double[] x = new double[4];
        double[] y = new double[4];
        double[] time = new double[4];
        for (int i = 0; i < 4; i++) {
            x[i] = points.get(index + i)[0];
            y[i] = points.get(index + i)[1];
            time[i] = i;
        }

        double tstart = 1;
        double tend = 2;
        double total = 0;
        for (int i = 1; i < 4; i++) {
            double dx = x[i] - x[i - 1];
            double dy = y[i] - y[i - 1];
            total += Math.pow(dx * dx + dy * dy, .25);
            time[i] = total;
        }
        tstart = time[1];
        tend = time[2];
        double z1 = 0.0;
        double z2 = 0.0;
        if (!Double.isNaN(points.get(index + 1)[2])) {
            z1 = points.get(index + 1)[2];
        }
        if (!Double.isNaN(points.get(index + 2)[2])) {
            z2 = points.get(index + 2)[2];
        }
        double dz = z2 - z1;
        int segments = pointsPerSegment - 1;
        result.add(points.get(index + 1));
        for (int i = 1; i < segments; i++) {
            float xi = interpolate(x, time, tstart + (i * (tend - tstart)) / segments);
            float yi = interpolate(y, time, tstart + (i * (tend - tstart)) / segments);
            float zi = (float) (z1 + (dz * i) / segments);
            result.add(new float[]{xi, yi, zi});
        }
        result.add(points.get(index + 2));
        return result;
    }

    public static float interpolate(double[] p, double[] time, double t) {
        double L01 = p[0] * (time[1] - t) / (time[1] - time[0]) + p[1] * (t - time[0]) / (time[1] - time[0]);
        double L12 = p[1] * (time[2] - t) / (time[2] - time[1]) + p[2] * (t - time[1]) / (time[2] - time[1]);
        double L23 = p[2] * (time[3] - t) / (time[3] - time[2]) + p[3] * (t - time[2]) / (time[3] - time[2]);
        double L012 = L01 * (time[2] - t) / (time[2] - time[0]) + L12 * (t - time[0]) / (time[2] - time[0]);
        double L123 = L12 * (time[3] - t) / (time[3] - time[1]) + L23 * (t - time[1]) / (time[3] - time[1]);
        double C12 = L012 * (time[2] - t) / (time[2] - time[1]) + L123 * (t - time[1]) / (time[2] - time[1]);
        return (float) C12;
    }

    public int addPoint (float x, float y, float z) {
        return addPoint(new float[] { x, y, z});
    }

    public int addPoint (float[] point) {
        myControlPoints.add(point);
        return myControlPoints.size() - 1;
    }

    /**
     * Returns total number of control points around the curve.
     */
    public int numControlPoints () {
        return myControlPoints.size();
    }

    /**
     * Draws the curve as a sequence of lines as the given resolution.
     *
     * The higher the resolution, the more lines generated, the closer
     * the approximation to the actual curve.  A value of 1 will look
     * like the points a connected linearly, with no curve, with smaller
     * values giving better approximations.
     */

    public void draw (GL2 gl, GLUT glut, float resolution, int seed) {
        gl.glBegin(GL2.GL_QUAD_STRIP); {
            Random r = new Random(seed);
            for (float t = 0; t < interpolatedPoints.size() - 3; t+=resolution) {
                float[] pos = evaluateDifferently(t);
                float[] bin = evaluateBinormalDifferently(t, resolution);
                int choice = r.nextInt(pallet.size());
                float[] color = pallet.get(choice);
                gl.glColor3f(1f, 0f, 1f);
                gl.glVertex3f(pos[0] - 0.2f * bin[0], pos[1] - 0.2f * bin[1], pos[2] - 0.2f * bin[2]);
                gl.glColor3f(0f, 0f, 1f);
                gl.glVertex3f(pos[0] + 0.2f * bin[0], pos[1] + 0.2f * bin[1], pos[2] + 0.2f * bin[2]);
            }
        }
        gl.glEnd();
    }

    public void evenlyDrawRoad (GL2 gl, GLUT glut, float resolution, float dist, float t, ArrayList<float[]> pallet) {
        float start = -dist;
        float step = (dist * 2f) / pallet.size();
        float[] pos = evaluateDifferently(t);
        float[] bin = evaluateBinormalDifferently(t, resolution);
        gl.glBegin(GL2.GL_QUAD_STRIP); {
            for(int i = 0; i < pallet.size() - 1; i+=2) {
                gl.glColor3f(pallet.get(i)[0], pallet.get(i)[1], pallet.get(i)[2]);
                gl.glVertex3f(pos[0] + (start + (i * step)) * bin[0],
                        pos[1] + (start + (i * step)) * bin[1],
                        pos[2] + (start + (i * step)) * bin[2]);
                gl.glColor3f(pallet.get(i + 1)[0], pallet.get(i + 1)[1], pallet.get(i + 1)[2]);
                gl.glVertex3f(pos[0] + (start + ((i+1) * step)) * bin[0],
                        pos[1] + (start + ((i+1) * step)) * bin[1],
                        pos[2] + (start + ((i+1) * step)) * bin[2]);
            }
        }
        gl.glEnd();
    }

    public float[] scalePoints(float t[], float scalar) {
        return new float[]{t[0] * scalar, t[1] * scalar, t[2] * scalar};
    }

    public float[] sumPoints(float t[], float s[]) {
        return new float[]{t[0] + s[0], t[1] + s[1], t[2] + s[2]};
    }

    public float[] subPoints(float t[], float s[]) {
        return new float[]{t[0] - s[0], t[1] - s[1], t[2] - s[2]};
    }

    public float[] multPoints(float t[], float s[]) {
        return new float[]{t[0] * s[0], t[1] * s[1], t[2] * s[2]};
    }

    /**
     * Draws control points around the curve as a collection of points.
     */
    public void drawControlPoints (GL2 gl) {
        gl.glBegin(GL2.GL_POINTS); {
            for (float[] pt : interpolatedPoints) {
                gl.glVertex3fv(pt, 0);
            }
        }
        gl.glEnd();
    }

    /**
     * Returns an iterator over the curve's control points, allowing the
     * user to directly iterate over them using a foreach loop.
     */
    @Override
    public Iterator<float[]> iterator () {
        return Collections.unmodifiableList(myControlPoints).iterator();
    }

    /**
     * Returns a string representation of the curve's control points.
     */
    @Override
    public String toString () {
        StringBuffer result = new StringBuffer();
        for (float[] pt : myControlPoints) {
            result.append(Arrays.toString(pt));
        }
        return result.toString();
    }

    public float[] makeControlPointTangent(int i, int j) {
        return subPoints(myControlPoints.get(i), myControlPoints.get(j));
    }

    public float[] evaluateNormalDifferently(float t, float res) {
        float[] tangent = normalize(findTangent(t, res));
        return crossProduct(new float[]{1, 0, 0}, tangent);
    }

    public float[] evaluateBinormalDifferently(float t, float res) {
        float[] tangent = normalize(findTangent(t, res));
        float[] normal = evaluateNormalDifferently(t, res);
        return normalize(crossProduct(tangent, normal));
    }

    public float[] crossProduct(float[] x, float[] y) {
        // performs cross product for x cross y
        float i, j, k;
        i = (x[1] * y[2]) - (x[2] * y[1]);
        j = -(x[0] * y[2]) + (x[2] * y[0]);
        k = (x[0] * y[1]) - (x[1] * y[0]);
        return new float[]{i, j, k};
    }

    public float[] normalize(float[] t) {
        double length = Math.pow(t[0], 2) + Math.pow(t[1], 2) + Math.pow(t[2], 2);
        float scalar = (float) Math.sqrt(length);
        return new float[]{t[0]/scalar, t[1]/scalar, t[2]/scalar};
    }

    public float[] findTangent (float t, float res) {
        if(t < res) {
            t = res;
        }
        return subPoints(evaluateDifferently(t + res), evaluateDifferently(t - res));
    }

    public float[] findLookAt (float t, float res) {
        return normalize(findTangent(t, res));
    }

    public float[] evaluateDifferently (float s) {
        int index = (int) Math.floor(s);
        float t = s - index;
        float t3 = t * t * t;
        float t2 = t * t;
        float[] c0, c1, c2, c3;
        if(index < 1 && index >= 0) {
            // make new starting point
            c0 = sumPoints(interpolatedPoints.get(index), subPoints(interpolatedPoints.get(index), interpolatedPoints.get(index + 1)));
            c1 = interpolatedPoints.get(index);
            c2 = interpolatedPoints.get(index + 1);
            c3 = interpolatedPoints.get(index + 2);
        }
        else if(index > interpolatedPoints.size() - 3 && index < interpolatedPoints.size() - 2) {
            // make new ending point
            c0 = interpolatedPoints.get(index - 1);
            c1 = interpolatedPoints.get(index);
            c2 = interpolatedPoints.get(index + 1);
            c3 = sumPoints(c2, subPoints(interpolatedPoints.get(index + 1), interpolatedPoints.get(index)));
        }
        else {
            c0 = interpolatedPoints.get(index - 1);
            c1 = interpolatedPoints.get(index);
            c2 = interpolatedPoints.get(index + 1);
            c3 = interpolatedPoints.get(index + 2);
        }
        float f1 = -0.5f * t3 + t2 - 0.5f * t;
        float f2 = 1.5f * t3 - 2.5f * t2 + 1.0f;
        float f3 = -1.5f * t3 + 2.0f * t2 + 0.5f * t;
        float f4 = 0.5f * t3 - 0.5f * t2;

        float x = c0[0] * f1 + c1[0] * f2 + c2[0] * f3 + c3[0] * f4;
        float y = c0[1] * f1 + c1[1] * f2 + c2[1] * f3 + c3[1] * f4;
        float z = c0[2] * f1 + c1[2] * f2 + c2[2] * f3 + c3[2] * f4;
        return new float[]{x, y, z};
    }

    public void makeRandomPoint() {
        float[] tangent = makeControlPointTangent(numControlPoints() - 1, numControlPoints() - 2);
        float[] basis = new float[3];
        float[] newPoint = new float[3];
        float dot_val = -0.1f;
        while(dot_val < 0.0f) {
            Random r = new Random();
            basis[0] = r.nextFloat() * 2 - 1;
            basis[1] = r.nextFloat() * 2 - 1;
            basis[2] = r.nextFloat() * 2 - 1;
            newPoint = sumPoints(myControlPoints.get(numControlPoints() - 1), scalePoints(basis, 30f));
            dot_val = computeDotProduct(tangent, subPoints(newPoint, myControlPoints.get(numControlPoints() - 1)));
            if(Math.abs(newPoint[0]) >= 490f || Math.abs(newPoint[1]) >= 490f || Math.abs(newPoint[2]) >= 490f)
                dot_val = -0.1f;
        }
        myControlPoints.add(newPoint);
        float[] printer = sumPoints(myControlPoints.get(numControlPoints() - 1), scalePoints(basis, 20f));
    }

    public float computeDotProduct(float[] v1, float[] v2) {
        float sum = 0.0f;
        if(v1.length != v2.length) {
            return 0.0f;
        }
        for(int i = 0; i < v1.length; i++){
            sum += v1[i] * v2[i];
        }
        return sum;
    }

    public float[] getCurrentBinormal() {
        return this.currentBinormal;
    }

    public float[] getCurrentNormal() {
        return this.currentNormal;
    }

    public float[] getCurrentTangent() {
        return this.currentTangent;
    }
}