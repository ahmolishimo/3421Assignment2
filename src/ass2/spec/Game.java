package ass2.spec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import ass2.spec.Shader;
import ass2.spec.Shader.CompilationException;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * COMMENT: Comment Game
 *
 * @author malcolmr
 */
public class Game extends JFrame implements GLEventListener, KeyListener {

	private Terrain myTerrain;
	private float[] myLightPosition;

	private final int NUM_TEXTURES = 6;
	private MyTexture[] textures;
	private String grassTextureFileName = "src/ass2/spec/grass.png";
	private String trunkTextureFileName = "src/ass2/spec/trunk.png";
	private String bushTextureFileName = "src/ass2/spec/bush.jpg";
	private String poolAnimatedTextureFileName = "src/ass2/spec/animationpool.jpg";
	private String roadTextureFileName = "src/ass2/spec/bricks.png";
	private String fourFaceObjTextureFileName = "src/ass2/spec/wood-box.jpg";

	private final int TREE_TRUNK_NUM = 24; // the number of quads used to generate the trunk mesh
	private final int TREE_HEIGHT = 6; // height of the tree
	private final int TREE_RADIUS = 1; // radius of the trunk

	private final int ROAD_SEG_NUM = 100; // how many points along the road are used to generate the mesh
	// used for special object rendering with vbos
	private int bufferIDs[] = new int[1];
	private int degree = 0;// used solely in the animated pool

	// define angle of position(camera & light)
	private float angle = 0.0f;
	private boolean rotateLeft = false;
	private boolean rotateRight = false;
	// define view mode
	private boolean changeView = false;

	private final float moveIncrement = 0.05f;
	private final float rotateIncrement = 5.0f;
	private final float lookAtPointRadius = 1.0f;
	private final float thirdPersonRadius = 5.0f;
	// define position of camera
	private float x = 0.0f;
	private float y = 0.5f;
	private float z = 0.0f;

	// define position of 3rd person camera
	private float x3;
	private float y3;
	private float z3;
	// whether to move or not
	private boolean moveForward = false;
	private boolean moveBackward = false;
	private boolean translateLeft = false;
	private boolean translateRight = false;

	// define center point of camera
	// look at point
	private float lx = 0.0f;
	private float ly = 2.0f;
	private float lz = 1.0f;

	private boolean[] keys = new boolean[200];

	private String VERTEX_SHADER = "src/ass2/spec/mySpecialObjectVertex.glsl";
	private String FRAGMENT_SHADER = "src/ass2/spec/mySpecialObjectFragment.glsl";
	private int shaderProgram;

	private boolean night = false;

	public Game(Terrain terrain) {
		super("Assignment 2");
		myLightPosition = new float[3];
		myLightPosition[0] = -1;
		myLightPosition[1] = 1;
		myLightPosition[2] = 0;
		myTerrain = terrain;
	}

	/**
	 * Run the game.
	 *
	 */
	public void run() {
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp);
		GLJPanel panel = new GLJPanel();
		panel.addGLEventListener(this);
		panel.addKeyListener(this);

		// Add an animator to call 'display' at 60fps
		FPSAnimator animator = new FPSAnimator(60);
		animator.add(panel);
		animator.start();

		getContentPane().add(panel);
		setSize(800, 600);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	/**
	 * Load a level file and display it.
	 * 
	 * @param args
	 *            - The first argument is a level file in JSON format
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Terrain terrain = LevelIO.load(new File(args[0]));
		Game game = new Game(terrain);
		game.run();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		GLU glu = new GLU();
		updateCamera();
		updateModel();
		setCamera(glu, gl);
		setLight(gl);
		// drawCoordinateFrame(gl);
		drawTerrain(gl);
		drawTrees(gl);
		drawRoads(gl);
		// using vbos to draw objects
		if(myTerrain.hasOthers()) 
			drawOthers(gl);
		drawPool(gl);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		// set the clear color to white
		// this is changed afterwards
		gl.glClearColor(1, 1, 1, 1);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		gl.glEnable(GL2.GL_TEXTURE_2D);

		// initialize textures
		textures = new MyTexture[NUM_TEXTURES];
		textures[0] = new MyTexture(gl, grassTextureFileName, "png", true);
		textures[1] = new MyTexture(gl, trunkTextureFileName, "png", false);
		textures[2] = new MyTexture(gl, bushTextureFileName, "jpg", true);
		textures[3] = new MyTexture(gl, poolAnimatedTextureFileName, "jpg", true);
		textures[4] = new MyTexture(gl, roadTextureFileName, "png", true);
		textures[5] = new MyTexture(gl, fourFaceObjTextureFileName, "jpg", true);

		// load vbos
		gl.glGenBuffers(1, bufferIDs, 0);
		FloatBuffer posData = Buffers.newDirectFloatBuffer(MySpecialObject.getPoints());
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, MySpecialObject.lengthInBytes(), posData, GL2.GL_STATIC_DRAW);

		// init shaders
		gl.glShadeModel(GL2.GL_SMOOTH);
		try {
			initShader(gl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU glu = new GLU();
		// calculate aspect ratio
		float widthHeightRatio = (float) getWidth() / (float) getHeight();
		// setup perspective camera
		glu.gluPerspective(60, widthHeightRatio, 0.1, 100);
	}

	private void updateCamera() {
		angle = angle % 360;// normalize the angle to 0~360 degree
		// update look at point height
		ly = (float) myTerrain.altitude(x, z) + 0.5f;
		y = (float) myTerrain.altitude(x, z) + 0.5f;
		if (moveForward) {
			// if the camera is moving forward, move the x and z coordinates
			z += Math.cos(Math.toRadians(angle)) * moveIncrement;
			x += Math.sin(Math.toRadians(angle)) * moveIncrement;
			lz += Math.cos(Math.toRadians(angle)) * moveIncrement;
			lx += Math.sin(Math.toRadians(angle)) * moveIncrement;
		}
		if (moveBackward) {
			z -= Math.cos(Math.toRadians(angle)) * moveIncrement;
			x -= Math.sin(Math.toRadians(angle)) * moveIncrement;
			lz -= Math.cos(Math.toRadians(angle)) * moveIncrement;
			lx -= Math.sin(Math.toRadians(angle)) * moveIncrement;
		}
		if (translateLeft) {
			z -= Math.sin(Math.toRadians(angle)) * moveIncrement;
			x += Math.cos(Math.toRadians(angle)) * moveIncrement;
			lz -= Math.sin(Math.toRadians(angle)) * moveIncrement;
			lx += Math.cos(Math.toRadians(angle)) * moveIncrement;
		}
		if (translateRight) {
			z += Math.sin(Math.toRadians(angle)) * moveIncrement;
			x -= Math.cos(Math.toRadians(angle)) * moveIncrement;
			lz += Math.sin(Math.toRadians(angle)) * moveIncrement;
			lx -= Math.cos(Math.toRadians(angle)) * moveIncrement;
		}
		if (rotateLeft) {
			// in rotation, the camera coordinates stay the same
			// change the look at point coordinates as if the point is 
			// moving on a circle centered at the camera point
			angle += rotateIncrement;
			lx = (float) (x + Math.sin(Math.toRadians(angle)) * lookAtPointRadius);
			lz = (float) (z + Math.cos(Math.toRadians(angle)) * lookAtPointRadius);
		}
		if (rotateRight) {
			angle -= rotateIncrement;
			lx = (float) (x + Math.sin(Math.toRadians(angle)) * lookAtPointRadius);
			lz = (float) (z + Math.cos(Math.toRadians(angle)) * lookAtPointRadius);
		}
		// update third person coordinates anyway
		// the x,z coordinates of the third person eye point is updated
		// by imagining the point on a circle centered the 1st person eye point
		// the height(y) of the third person look at point is calculated
		// by raising the point above horizontal plane by 30 degrees
		z3 = (float) (z - Math.cos(Math.toRadians(angle)) * thirdPersonRadius);
		x3 = (float) (x - Math.sin(Math.toRadians(angle)) * thirdPersonRadius);
		y3 = (float) (y + thirdPersonRadius * Math.tan(Math.toRadians(30)));
	}

	private void updateModel() {
		// just normalize the degree(used in the animated pool)
		degree++;
		degree = degree % 360;
	}
	
	private void setLight(GL2 gl) {
		if (night) {
			// night light
			// set background to black
			gl.glClearColor(0, 0, 0, 1);
			// enable torch light
			gl.glEnable(GL2.GL_LIGHT1);
			// light properties for the "sun" with low ambient/diffuse light
			float[] amb = { 0.1f, 0.1f, 0.1f, 1.0f };
			float[] dif = { 0.2f, 0.2f, 0.2f, 1.0f };
			float[] spe = { 0.0f, 0.0f, 0.0f, 1.0f };
			gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, amb, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, spe, 0);
			// light properties for the torch light with hight diffuse light
			float[] amb1 = { 0.0f, 0.0f, 0.0f, 1.0f };
			float[] dif1 = { 1f, 1f, 1f, 1.0f };
			float[] spe1 = { 0.0f, 0.0f, 0.0f, 1.0f };
			// the position of the light should be at the first person eye point
			float[] lightPos = { x, y, z, 1 };
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPos, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, amb1, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, dif1, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, spe1, 0);
			
			// set night torch light
			// the direction of the torch light should face at +z of the local
			// coordinate frame of the first person eye point
			gl.glPushMatrix();
			gl.glTranslated(x, ly, z);
			gl.glRotated(angle, 0, 1, 0);
			float[] dir = { 0, 0, 1, 0 };// look at z
			gl.glLightf(GL2.GL_LIGHT1, GL2.GL_SPOT_CUTOFF, 36);
			gl.glLightf(GL2.GL_LIGHT1, GL2.GL_SPOT_EXPONENT, 5);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPOT_DIRECTION, dir, 0);
			gl.glPopMatrix();
		} else {
			// day light
			gl.glClearColor(1, 1, 1, 1);// white background color
			gl.glDisable(GL2.GL_LIGHT1);// no torch light
			float[] amb = { 0.3f, 0.3f, 0.3f, 1.0f };
			float[] dif = { 0.25f, 1.0f, 1.0f, 1.0f };
			float[] spe = { 1.0f, 1.0f, 0.25f, 1.0f };
			// set light position
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, myLightPosition, 0);
			gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, amb, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, spe, 0);
		}
	}

	private void setCamera(GLU glu, GL2 gl) {
		if (!changeView) {
			// 1st person view
			glu.gluLookAt(x, y, z, lx, ly, lz, 0, 1, 0);
		}
		else {
			// 3rd person view
			glu.gluLookAt(x3, y3, z3, lx, ly, lz, 0, 1, 0);
			gl.glPushMatrix();
			gl.glTranslated(x, y, z);
			gl.glScaled(0.3, 0.3, 0.3);
			gl.glRotated(angle - 90, 0, 1, 0);

			// draw avatar
			setMaterialForAvatar(gl);
			GLUT glut = new GLUT();
			gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[5].getTextureId());
			glut.glutSolidTeapot(1);
			gl.glPopMatrix();
		}
	}
	
	private void setMaterialForAvatar(GL2 gl) {
		float[] ambientCoeff = { 0.9f, 0.3f, 0.3f, 1.0f };
		float[] diffuseCoeff = { 0.9f, 0.9f, 0.9f, 1.0f };
		float[] specCoeff = { 0.3f, 0.0f, 0.0f, 1.0f };
		float[] emissionCoeff = { 0.0f, 0f, 0f, 1.0f };
		float phong = 10f;
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff, 0);
	}
	
	private void setMaterialForSpecialObject(GL2 gl) {
		float[] ambientCoeff = { 0.9f, 0.3f, 0.3f, 1.0f };
		float[] diffuseCoeff = { 0.9f, 0.9f, 0.9f, 1.0f };
		float[] specCoeff = { 0.3f, 0.0f, 0.0f, 1.0f };
		float[] emissionCoeff = { 0.0f, 0f, 0f, 1.0f };
		float phong = 10f;
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff, 0);
	}

	private void setMaterialForGrass(GL2 gl) {
		// set material properties to grass
		float[] ambientCoeff = { 0.5f, 0.5f, 0.5f, 1.0f };
		float[] diffuseCoeff = { 0.1f, 0.7f, 0.3f, 1.0f };
		float[] specCoeff = { 0.0f, 0.0f, 0.0f, 1.0f };
		float[] emissionCoeff = { 0.0f, 0.0f, 0.0f, 1.0f };
		float phong = 10f;
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff, 0);
	}

	private void setMaterialForTrunk(GL2 gl) {
		// change material properties to trunk
		float[] ambientCoeff2 = { 0.7f, 0.5f, 0.5f, 1.0f };
		float[] diffuseCoeff2 = { 1f, 0.1f, 0.1f, 1.0f };
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff2, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff2, 0);
	}

	private void setMaterialForRoad(GL2 gl) {
		float[] ambientCoeff2 = { 0.9f, 0.3f, 0.3f, 1.0f };
		float[] diffuseCoeff2 = { 1f, 0.1f, 0.1f, 1.0f };
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff2, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff2, 0);
	}

	private void setMaterialForTreeBall(GL2 gl) {
		float[] ambientCoeff2 = { 0.2f, 0.6f, 0.3f, 1.0f };
		float[] diffuseCoeff2 = { 0.2f, 0.6f, 0.3f, 1.0f };
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff2, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff2, 0);
	}

	private void setMaterialForPool(GL2 gl) {
		float[] ambientCoeff = { 0.3f, 0.3f, 0.3f, 1.0f };
		float[] diffuseCoeff = { 1f, 1f, 1f, 1.0f };
		float[] specCoeff = { 0.0f, 0.0f, 0.0f, 1.0f };
		float[] emissionCoeff = { 0.0f, 1f, 1f, 1.0f };
		// float[] emissionCoeff = { 0f, 0f, 0f, 1.0f };
		float phong = 10f;
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff, 0);
	}

	private void drawRoads(GL2 gl) {
		List<Road> roads = myTerrain.roads();
		// for each road
		for (int i = 0; i < roads.size(); i++) {
			gl.glPushMatrix();
			// draw it
			drawRoad(gl, roads.get(i));
			gl.glPopMatrix();
		}
	}
	
	private void drawOthers(GL2 gl) {
		gl.glPushMatrix();
		setMaterialForSpecialObject(gl);
		float[] othersPos = myTerrain.getOthers();
		gl.glTranslated(othersPos[0], myTerrain.altitude(othersPos[0], othersPos[2]) + 0.01, othersPos[2]);
		gl.glUseProgram(shaderProgram);
		drawSpecialObject(gl);
		gl.glUseProgram(0);
		gl.glPopMatrix();
	}
	
	private void drawSpecialObject(GL2 gl) {
		// pass texture
		int texUnitLoc = gl.glGetUniformLocation(shaderProgram, "texUnit1");
		gl.glUniform1i(texUnitLoc, 0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[5].getTextureId());
		// use vbo to draw
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[0]);
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
		gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, Float.BYTES * 3 * 4 * 6);
		gl.glNormalPointer(GL.GL_FLOAT, 0, Float.BYTES * 3 * 4 * 6 + Float.BYTES * 2 * 4 * 6);
		gl.glDrawArrays(GL2.GL_QUADS, 0, MySpecialObject.numberOfPoints());
	}

	private void drawRoad(GL2 gl, Road road) {
		setMaterialForRoad(gl);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[4].getTextureId());
		gl.glBegin(GL2.GL_POLYGON);
		// imagine walking along the spine of the road
		// draw the edge to the left of you first, from the beginning to the end
		for(int i = 0; i < ROAD_SEG_NUM -1; i++) {
			double t = road.size()/(double)ROAD_SEG_NUM*i;
			// get a point on the spine to ask for the height
			double[] pointOnSpine = road.point(t);
			double height = myTerrain.altitude(pointOnSpine[0], pointOnSpine[1]) + 0.01;
			double[] p = road.edgePoint(t, false);
			gl.glTexCoord2d(1.0/ROAD_SEG_NUM*i, 1);
			gl.glVertex3d(p[0], height, p[1]);
		}
		// then draw right, from the end to the beginning
		for(int i = ROAD_SEG_NUM - 1; i >= 0; i--) {
			double t = road.size()/(double)ROAD_SEG_NUM*i;
			double[] pointOnSpine = road.point(t);
			double height = myTerrain.altitude(pointOnSpine[0], pointOnSpine[1]) + 0.01;
			double[] p = road.edgePoint(t, true);
			gl.glTexCoord2d(1.0/ROAD_SEG_NUM*i, 0);
			gl.glVertex3d(p[0], height, p[1]);
		}
		gl.glEnd();
//		for (int i = 0; i < ROAD_SEG_NUM - 1; i++) {
//			double t = road.size() / (double) ROAD_SEG_NUM * i;
//			double[] pointOnSpine = road.point(t);
//			double height = myTerrain.altitude(pointOnSpine[0], pointOnSpine[1]) + 0.01;
//			double[] p1 = road.edgePoint(t, false);
//			double[] p2 = road.edgePoint(t, true);
//			double[] p3 = road.edgePoint(t + road.size() / (double) ROAD_SEG_NUM, true);
//			double[] p4 = road.edgePoint(t + road.size() / (double) ROAD_SEG_NUM, false);
//
//			gl.glTexCoord2d(1.0 / ROAD_SEG_NUM * i, 1);
//			gl.glVertex3d(p1[0], height, p1[1]);
//			gl.glTexCoord2d(1.0 / ROAD_SEG_NUM * i, 0);
//			gl.glVertex3d(p2[0], height, p2[1]);
//			gl.glTexCoord2d(1.0 / ROAD_SEG_NUM * (1 + i), 0);
//			gl.glVertex3d(p3[0], height, p3[1]);
//			gl.glTexCoord2d(1.0 / ROAD_SEG_NUM * (1 + i), 1);
//			gl.glVertex3d(p4[0], height, p4[1]);
//
//		}
//		gl.glEnd();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	private void drawTrees(GL2 gl) {
		for (int i = 0; i < myTerrain.trees().size(); i++) {
			double[] pos = myTerrain.trees().get(i).getPosition();
			gl.glPushMatrix();
			gl.glTranslated(pos[0], pos[1], pos[2]);
			gl.glScaled(0.5, 0.5, 0.5);
			setMaterialForTrunk(gl);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[1].getTextureId());
			// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

			// draw around as a bunch of quads
			gl.glBegin(GL2.GL_QUAD_STRIP);
			for (int j = 0; j < TREE_TRUNK_NUM + 1; j++) {
				double x = Math.cos(2 * j * TREE_RADIUS * Math.PI / TREE_TRUNK_NUM);
				double z = Math.sin(2 * j * TREE_RADIUS * Math.PI / TREE_TRUNK_NUM);
				gl.glNormal3d(x, 0, z);
				gl.glTexCoord2d(1.0 / TREE_TRUNK_NUM * j, 0);
				gl.glVertex3d(x, 0, z);
				gl.glTexCoord2d(1.0 / TREE_TRUNK_NUM * j, 1);
				gl.glVertex3d(x, TREE_HEIGHT, z);
			}
			gl.glEnd();

			// draw top ball
			// just a solid sphere with texture
			// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[2].getTextureId());
			setMaterialForTreeBall(gl);
			GLU glu = new GLU();
			gl.glTranslated(0, TREE_HEIGHT, 0);
			// glut.glutSolidSphere(3, 24, 24);
			GLUquadric quadric = glu.gluNewQuadric();
			glu.gluQuadricTexture(quadric, true);
			glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
			glu.gluSphere(quadric, 3, 64, 64);
			gl.glPopMatrix();
		}
	}

	private void drawTerrain(GL2 gl) {
		setMaterialForGrass(gl);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		int terrainWidth = (int) myTerrain.size().getWidth();
		int terrainHeight = (int) myTerrain.size().getHeight();
		// second, draw triangles
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0].getTextureId());
		// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		gl.glBegin(GL2.GL_TRIANGLES);
		{
			// for each point, draw a triangle with this point
			// the point to the right and the point down it
			//
			// this point *-------* right point
			//            |      /|
			//            |    /  |
			//            |  /    |
			// down point *-------* corner point
			//
			for (int i = 0; i < terrainWidth - 1; i++) {
				for (int j = 0; j < terrainHeight - 1; j++) {
					double[] thisPoint = { i, myTerrain.getGridAltitude(i, j), j };
					double[] downPoint = { i, myTerrain.getGridAltitude(i, j + 1), j + 1 };
					double[] rightPoint = { i + 1, myTerrain.getGridAltitude(i + 1, j), j };
					double[] cornerPoint = { i + 1, myTerrain.getGridAltitude(i + 1, j + 1), j + 1 };

					double[] n1 = MathUtil.normal(thisPoint, downPoint, rightPoint);
					double[] n2 = MathUtil.normal(downPoint, cornerPoint, rightPoint);

					gl.glNormal3dv(n1, 0);
					gl.glTexCoord2d(1.0 / terrainWidth * i, 1.0 / terrainHeight * j);
					gl.glVertex3dv(thisPoint, 0);

					gl.glTexCoord2d(1.0 / terrainWidth * i, 1.0 / terrainHeight * (j + 1));
					gl.glVertex3dv(downPoint, 0);

					gl.glTexCoord2d(1.0 / terrainWidth * (i + 1), 1.0 / terrainHeight * j);
					gl.glVertex3dv(rightPoint, 0);

					gl.glNormal3dv(n2, 0);
					gl.glTexCoord2d(1.0 / terrainWidth * i, 1.0 / terrainHeight * (j + 1));
					gl.glVertex3dv(downPoint, 0);
					gl.glTexCoord2d(1.0 / terrainWidth * (i + 1), 1.0 / terrainHeight * (j + 1));
					gl.glVertex3dv(cornerPoint, 0);
					gl.glTexCoord2d(1.0 / terrainWidth * (i + 1), 1.0 / terrainHeight * j);
					gl.glVertex3dv(rightPoint, 0);
				}
			}
		}
		gl.glEnd();
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
	}

	private void drawPool(GL2 gl) {
		setMaterialForPool(gl);
		gl.glPushMatrix();
		double height = myTerrain.altitude(1, 5) + 0.01;
		gl.glTranslated(1, height, 5);
		// since all pictures for the pool is in one file
		// we need to iterate through the 16 small pictures
		// the 5 here is just the speed of the wave
		// num specifies which small picture to use
		int num = (degree / 5) % 16;
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[3].getTextureId());
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2d(num / 4 * 1.0 / 4.0, num % 4 * 1.0 / 4.0); // upper left
		gl.glVertex3d(0, 0, 0);
		gl.glTexCoord2d((num / 4 + 1) * 1.0 / 4.0, num % 4 * 1.0 / 4.0); // lower
																			// left
		gl.glVertex3d(0, 0, 1);
		gl.glTexCoord2d((num / 4 + 1) * 1.0 / 4.0, (num % 4 + 1) * 1.0 / 4.0); // lower
																				// right
		gl.glVertex3d(1, 0, 1);
		gl.glTexCoord2d(num / 4 * 1.0 / 4.0, (num % 4 + 1) * 1.0 / 4.0); // upper
																			// right
		gl.glVertex3d(1, 0, 0);
		gl.glEnd();
		gl.glPopMatrix();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	private void drawCoordinateFrame(GL2 gl) {
		// test, draw a coordinate frame
		gl.glBegin(GL2.GL_LINES);
		{
			// red x axis
			gl.glColor3d(1, 0, 0);
			gl.glVertex3d(-0.1, -0.1, 0);
			gl.glVertex3d(1, -0.1, 0);

			// green y axis
			gl.glColor3d(0, 1, 0);
			gl.glVertex3d(-0.1, -0.1, 0);
			gl.glVertex3d(-0.1, 1, 0);

			// blue z axis
			gl.glColor3d(0, 0, 1);
			gl.glVertex3d(-0.1, -0.1, 0);
			gl.glVertex3d(-0.1, -0.1, 1);
		}
		gl.glEnd();
	}

	private void initShader(GL2 gl) throws Exception {
		Shader vertexShader = new Shader(GL2.GL_VERTEX_SHADER, new File(VERTEX_SHADER));
		// Shader vertexShader = new Shader(GL2.GL_VERTEX_SHADER, v);
		vertexShader.compile(gl);
		Shader fragmentShader = new Shader(GL2.GL_FRAGMENT_SHADER, new File(FRAGMENT_SHADER));
		fragmentShader.compile(gl);
		// Each shaderProgram must have
		// one vertex shader and one fragment shader.
		shaderProgram = gl.glCreateProgram();
		gl.glAttachShader(shaderProgram, vertexShader.getID());
		gl.glAttachShader(shaderProgram, fragmentShader.getID());
		gl.glLinkProgram(shaderProgram);
		int[] error = new int[2];
		gl.glGetProgramiv(shaderProgram, GL2ES2.GL_LINK_STATUS, error, 0);
		if (error[0] != GL.GL_TRUE) {
			int[] logLength = new int[1];
			gl.glGetProgramiv(shaderProgram, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);

			byte[] log = new byte[logLength[0]];
			gl.glGetProgramInfoLog(shaderProgram, logLength[0], (int[]) null, 0, log, 0);

			System.out.printf("Failed to compile shader! %s\n", new String(log));
			throw new CompilationException("Error compiling the shader: " + new String(log));
		}
		gl.glValidateProgram(shaderProgram);
		gl.glGetProgramiv(shaderProgram, GL2ES2.GL_VALIDATE_STATUS, error, 0);
		if (error[0] != GL.GL_TRUE) {
			System.out.printf("Failed to validate shader!\n");
			throw new Exception("program failed to validate");
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {

		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			// moving forward
			moveForward = true;
			break;

		case KeyEvent.VK_W:
			moveForward = true;
			break;

		case KeyEvent.VK_S:
			moveBackward = true;
			break;

		case KeyEvent.VK_DOWN:
			// moving backward
			moveBackward = true;
			break;

		case KeyEvent.VK_RIGHT:
			// turning right
			rotateRight = true;
			break;

		case KeyEvent.VK_LEFT:
			// turning left
			rotateLeft = true;
			break;

		case KeyEvent.VK_A:
			// translate left
			translateLeft = true;
			break;

		case KeyEvent.VK_D:
			// translate right
			translateRight = true;
			break;

		case KeyEvent.VK_1:
			// change to 3rd person view
			changeView = !changeView;
			break;

		case KeyEvent.VK_N:
			night = !night;
			break;
		}

		if (e.getKeyCode() < 250)
			keys[e.getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			// moving forward
			moveForward = false;
			break;

		case KeyEvent.VK_W:
			moveForward = false;
			break;

		case KeyEvent.VK_DOWN:
			// moving backward
			moveBackward = false;
			break;

		case KeyEvent.VK_S:
			moveBackward = false;
			break;

		case KeyEvent.VK_RIGHT:
			// turning right
			rotateRight = false;
			break;

		case KeyEvent.VK_LEFT:
			// turning left
			rotateLeft = false;
			break;

		case KeyEvent.VK_A:
			translateLeft = false;
			break;

		case KeyEvent.VK_D:
			translateRight = false;
			break;
		}
		if (e.getKeyCode() < 250)
			keys[e.getKeyCode()] = false;
	}
}
