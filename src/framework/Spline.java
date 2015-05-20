package framework;

import com.jogamp.opengl.util.gl2.GLUT;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import javax.media.opengl.GL2;


/** 
 * This class represents a curve loop defined by a sequence of control points.
 * 
 * @author Robert C. Duvall
 *
 * modified by eli
 */
public class Spline implements Iterable<float[]> {

    private List<float[]> myControlPoints = new ArrayList<>();

    /**
     * Create empty curve.
     */
    public Spline (float[] controlPoints) {
        // BUGBUG: check that it is a multiple of 3
        for (int k = 0; k < controlPoints.length; k += 3) {
            addPoint(controlPoints[k], controlPoints[k+1], controlPoints[k+2]);
        }
    }

    public Spline (ArrayList<float[]> pts) {
        myControlPoints = pts;
    }

    /**
     * Create curve from the control points listed in the given file.
     */
    @SuppressWarnings("resource")
    public Spline (String filename, int num) {
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
        init(num);
    }

    public void init(int num) {
        while(numControlPoints() < num) {
            makeRandomPoint();
        }
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

    public float[] makeControlPointTangent(int i, int j) {
        return subPoints(myControlPoints.get(i), myControlPoints.get(j));
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
//            dot_val = 0.1f;
        }
        myControlPoints.add(newPoint);
        float[] printer = sumPoints(myControlPoints.get(numControlPoints() - 1), scalePoints(basis, 20f));
        System.out.printf("New Point: (Index: %d) (%f, %f, %f)\n", numControlPoints(), printer[0], printer[1], printer[2]);
    }

    /**
     * Add control point
     * 
     * @return index of new control point
     */
    public int addPoint (float x, float y, float z) {
        return addPoint(new float[] { x, y, z});
    }

    /**
     * Add control point
     * 
     * @return index of new control point
     */
    public int addPoint (float[] point) {
        myControlPoints.add(point);
        return myControlPoints.size() - 1;
    }

    /**
     * Evaluate a point on the curve at a given time.
     * 
     * Note, t varies from [0 .. 1] across a set of 4 control points and 
     * each set of 4 control points influences the curve within them. 
     * Thus a time value between [0 .. 1] generates a point within the first
     * 4 control points and a value between [n-2 .. n-1] generates a point
     * within the last 4 control points.
     * 
     * A time value outside the range [0 .. n] is wrapped, modded, so it 
     * falls within the appropriate range.
     */
    public float[] evaluateAt (float t) {
        int tn = (int)Math.floor(t);
        float u = t - tn;
        float u_sq = u * u;
        float u_cube = u * u_sq;
        // evaluate basis functions at t, faster than matrix multiply
        float[] basis = {
            -u_cube + 3*u_sq - 3*u + 1,
             3*u_cube - 6*u_sq + 4,
            -3*u_cube + 3*u_sq + 3*u + 1,
             u_cube
        };
        return evaluateBasisAt(tn, basis);
    }

    /**
     * Evaluate the derivative of the curve at a given time.
     * 
     * Note, t varies from [0 .. 1] across a set of 4 control points and 
     * each set of 4 control points influences the curve within them. 
     * Thus a time value between [0 .. 1] generates a derivative within the 
     * first 4 control points and a value between [n-2 .. n-1] generates a
     * derivative within the last 4 control points.
     * 
     * A time value outside the range [0 .. n] is wrapped, modded, so it 
     * falls within the appropriate range.
     */
    public float[] evaluateDerivativeAt (float t) {
        int tn = (int)Math.floor(t);
        float u = t - tn;
        float u_sq = u * u;
        // evaluate basis functions at t, faster than matrix multiply
        float[] basis = {
            -3*u_sq + 6*u - 3,
             9*u_sq - 12*u,
            -9*u_sq + 6*u + 3,
             3*u_sq
        };
        return evaluateBasisAt(tn, basis);
    }

    /**
     * Evaluate the second derivative of the curve at a given time.
     * 
     * Note, t varies from [0 .. 1] across a set of 4 control points and 
     * each set of 4 control points influences the curve within them. 
     * Thus a time value between [0 .. 1] generates a derivative within the 
     * first 4 control points and a value between [n-2 .. n-1] generates a
     * derivative within the last 4 control points.
     * 
     * A time value outside the range [0 .. n] is wrapped, modded, so it 
     * falls within the appropriate range.
     */
    public float[] evaluateSecondDerivativeAt (float t) {
        int tn = (int)Math.floor(t);
        float u = t - tn;
        // evaluate basis functions at t, faster than matrix multiply
        float[] basis = {
            -6*u + 6,
             18*u - 12,
            -18*u + 6,
             6*u
        };
        return evaluateBasisAt(tn, basis);
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

    public void draw(GL2 gl, GLUT glut, float resolution, float time) {
        gl.glBegin(GL2.GL_QUAD_STRIP);
        {
            for (float t = time % numControlPoints(); t < time + 5f; t += resolution) {
                gl.glColor3f(1f, 0f, 0f);
                gl.glVertex3f(evaluateAt(t)[0] - .5f * evaluateBinormal(t)[0],
                        evaluateAt(t)[1] - .5f * evaluateBinormal(t)[1],
                        evaluateAt(t)[2] - .5f * evaluateBinormal(t)[2]);
//                gl.glVertex3fv(evaluateAt(t), 0);
                gl.glColor3f(0, 1, 0);
                gl.glVertex3f(evaluateAt(t)[0] + .5f * evaluateBinormal(t)[0],
                        evaluateAt(t)[1] + .5f * evaluateBinormal(t)[1],
                        evaluateAt(t)[2] + .5f * evaluateBinormal(t)[2]);
            }
        }
        gl.glEnd();
    }

    public void draw(GL2 gl, GLUT glut, float resolution) {
        gl.glBegin(GL2.GL_QUAD_STRIP);
        {
            for (float t = 0; t < numControlPoints(); t += resolution) {
                gl.glColor3f(1f, 0f, 1f);
                gl.glVertex3f(evaluateAt(t)[0] - .2f * evaluateBinormal(t)[0],
                        evaluateAt(t)[1] - .2f * evaluateBinormal(t)[1],
                        evaluateAt(t)[2] - .2f * evaluateBinormal(t)[2]);
//                gl.glVertex3fv(evaluateAt(t), 0);
                gl.glColor3f(0, 1, 0);
                gl.glVertex3f(evaluateAt(t)[0] + .2f * evaluateBinormal(t)[0],
                        evaluateAt(t)[1] + .2f * evaluateBinormal(t)[1],
                        evaluateAt(t)[2] + .2f * evaluateBinormal(t)[2]);
            }
        }
        gl.glEnd();
    }

    /**
     * Draws control points around the curve as a collection of points.
     */
    public void drawControlPoints (GL2 gl) {
        gl.glBegin(GL2.GL_POINTS); {
            for (float[] pt : myControlPoints) {
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
    
    // use the basis functions to evaluate a specific point on the curve
    private float[] evaluateBasisAt (int t, float[] basis) {
        // sum the control points times the basis functions for each dimension
        float[] result = { 0, 0, 0 };
        for (int k = 0; k < 4; k++) {
            int index = (t + k) % numControlPoints();
            result[0] += myControlPoints.get(index)[0] * basis[k];
            result[1] += myControlPoints.get(index)[1] * basis[k];
            result[2] += myControlPoints.get(index)[2] * basis[k];
        }
        // divide through the constant factor
        for (int k = 0; k < result.length; k++) {
            result[k] /= 6.0f;
        }
        return result;
    }

    public float[] evaluateNormal(float t) {
        float[] norm = normalize(crossProduct(evaluateSecondDerivativeAt(t), evaluateDerivativeAt(t)));
        return normalize(crossProduct(evaluateDerivativeAt(t), norm));
    }

    public float[] evaluateBinormal(float t) {
        return normalize(crossProduct(evaluateDerivativeAt(t), evaluateSecondDerivativeAt(t)));
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
}
