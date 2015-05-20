import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import framework.JOGLFrame;
import framework.Scene;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Created by eli on 4/20/15.
 */

public class Main extends Scene {
    /**
     * Display a simple scene to demonstrate OpenGL.
     *
     * @author of base class: Robert C. Duvall
     */

    private static String DEFAULT_CONTROL_POINTS = "tracks/catmull_base_points.txt";
    private static String[] TEXTURE_FILES = { "images/purplenebula_ft.tga","images/purplenebula_lf.tga",
            "images/purplenebula_rt.tga","images/purplenebula_up.tga","images/purplenebula_dn.tga","images/purplenebula_bk.tga"};
    private final int TERRAIN_ID = 1;
    private float resolution;
    private int myRenderMode;
    private boolean isCompiled;
    private boolean bspline_cam;
    private boolean cspline_cam;
    private boolean bspline_toggle;
    private boolean cspline_toggle;
    private boolean control_point_toggle;
    private Controller control;
    private TextRenderer renderer;
    private Skybox box;

    public Main (String[] args) {
        super("Final Project Demo");
    }

    /**
     * Initialize general OpenGL values once (in place of constructor).
     */
    @Override
    public void init (GL2 gl, GLU glu, GLUT glut) {
        isCompiled = false;
        bspline_toggle = false;
        cspline_toggle = false;
        bspline_cam = false;
        cspline_cam = false;
        control_point_toggle = false;
        resolution = .01f;
        myRenderMode = GL2.GL_QUADS;
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        box = new Skybox(gl, glu, glut, TEXTURE_FILES);
        renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 24));
        control = new Controller(0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f, DEFAULT_CONTROL_POINTS);
    }

    /**
     * Draw all of the objects to display.
     */
    @Override
    public void display (GL2 gl, GLU glu, GLUT glut) {
        if (!isCompiled) {
            gl.glDeleteLists(TERRAIN_ID, 1);
            gl.glNewList(TERRAIN_ID, GL2.GL_COMPILE);
            box.drawSides(gl, glu, glut);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            if (bspline_toggle) {
                control.track.draw(gl, glut, resolution, 2);
            }
            if (cspline_toggle) {
                control.track2.draw(gl, glut, resolution);
            }
            if (control_point_toggle) {
                gl.glColor3f(0.0f, 1.0f, 0.0f);
                gl.glPointSize(5.0f);
                control.track.drawControlPoints(gl);
            }
            renderer.beginRendering(600, 600);
            renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderer.draw(Float.toString(control.spline_path % control.track.numControlPoints()), 10, 580);
            renderer.endRendering();
            gl.glEndList();
            isCompiled = true;
        }
        gl.glCallList(TERRAIN_ID);
    }

    /**
     * Animate the scene by changing its state slightly.
     */
    @Override
    public void animate (GL2 gl, GLU glu, GLUT glut) {
        control.spline_path += control.speed;
        if(cspline_cam & !bspline_cam)
            control.updateSplineVars(resolution);
        if(bspline_cam & !cspline_cam)
            control.updateSplineVars();
        isCompiled = false;
    }

    /**
     * Set the camera's view of the scene.
     */
    @Override
    public void setCamera(GL2 gl, GLU glu, GLUT glut) {
        float fx, fy, fz, tx, ty, tz;
        if(!bspline_cam && !cspline_cam) {
            fx = control.fromX;
            fy = control.fromY;
            fz = control.fromZ;
            tx = control.toX;
            ty = control.toY;
            tz = control.toZ;
            glu.gluLookAt(fx, fy, fz, // from position
                    tx, ty, tz,   // to position
                    control.upX, control.upY, control.upZ);
        }
        else {
            fx = control.sPosX + (control.sNormX * 0.4f);
            fy = control.sPosY + (control.sNormY * 0.4f);
            fz = control.sPosZ + (control.sNormZ * 0.4f);
            tx = control.sPosX + control.sDerX;
            ty = control.sPosY + control.sDerY;
            tz = control.sPosZ + control.sDerZ;
            glu.gluLookAt(fx, fy, fz, // from position
                    tx, ty, tz,   // to position
                    control.sNormX, control.sNormY, control.sNormZ);
        }
    }

    /**
     * Establish lights in the scene.
     */
    @Override
    public void setLighting (GL2 gl, GLU glu, GLUT glut) {
        float[] light0pos = {0, 400, 0, 1};
        float[] light0dir = { 0, -1, 0, 0};
        float[] ambient = {.6f, .6f, .6f, 0f};
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0pos, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, light0dir, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambient, 0);
//        gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF, 20);
    }

    /**
     * Called when any key is pressed within the canvas.
     */
    @Override
    public void keyPressed(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_T:   // reset animation
                control.resetAll();
                break;
            case KeyEvent.VK_UP:   // speed up flying
                control.speed+=0.001f;
                isCompiled = false;
                break;
            case KeyEvent.VK_DOWN:   // slow down flying
                control.speed-=0.001f;
                isCompiled = false;
                break;
            case KeyEvent.VK_C:   // toggle drawing of catmull-rom spline path
                cspline_toggle = !cspline_toggle;
                isCompiled = false;
                break;
            case KeyEvent.VK_M:  // toggle drawing of bezier spline path
                bspline_toggle = !bspline_toggle;
                isCompiled = false;
                break;
            case KeyEvent.VK_N:  // toggle following of catmull-rom spline path
                control.resetAll();
                bspline_cam = !bspline_cam;
                isCompiled = false;
                break;
            case KeyEvent.VK_B:  // toggle following of bezier spline path
                control.resetAll();
                cspline_cam = !cspline_cam;
                isCompiled = false;
                break;
            case KeyEvent.VK_V:  // toggle drawing control points
                control_point_toggle = !control_point_toggle;
                isCompiled = false;
                break;
        }
    }

        // allow program to be run from here
        public static void main (String[] args) {
            new JOGLFrame(new Main(args));
        }
}