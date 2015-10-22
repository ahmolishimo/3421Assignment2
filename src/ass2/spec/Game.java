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

	private final int NUM_TEXTURES = 3;
	private MyTexture[] textures;
	private String grassTextureFileName = "src/ass2/spec/grass.png";
	private String trunkTextureFileName = "src/ass2/spec/trunk.png";
	private String bushTextureFileName = "src/ass2/spec/bush.jpg";

	private final int TREE_TRUNK_NUM = 24;
	private final int TREE_HEIGHT = 6;
	private final int TREE_RADIUS = 1;

	private final int ROAD_SEG_NUM = 100;

	// used for special object rendering with vbos
	private int bufferIDs[] = new int[1];
	private int degree = 0;

	// define angle of position
	private float angle = 0.0f;

	// define view mode
	private boolean changeView = false;

	// define position of camera
	private float x = 0.0f;
	private float y = 2.0f;
	private float z = 0.0f;

	// define center point of camera
	private float lx = 0.0f;
	private float ly = 2.0f;
	private float lz = 1.0f;

	private boolean[] keys = new boolean[200];

	private String VERTEX_SHADER = "src/ass2/spec/mySpecialObjectVertex.glsl";
	private String FRAGMENT_SHADER = "src/ass2/spec/mySpecialObjectFragment.glsl";
	private int shaderProgram;

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

		// set perspective camera
		GLU glu = new GLU();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		float widthHeightRatio = (float) getWidth() / (float) getHeight();
		glu.gluPerspective(60, widthHeightRatio, 0.1, 100);

		// change back to model view matrix.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		// set the position of perspective camera
		ly = (float) myTerrain.altitude(x, z) + 0.5f;
		glu.gluLookAt(0, 2, 15, 8, 2, 0, 0, 1, 0);
		//glu.gluLookAt(x, ly, z, lx, ly, lz, 0, 1, 0);

		// draw avatar
		if (changeView) {
			gl.glPushMatrix();
			gl.glTranslated(x, ly, z);
			gl.glScaled(0.3, 0.3, 0.3);
			gl.glRotated(angle, 0, 1, 0);

			gl.glColor3f(1, 0, 0);
			GLUT glut = new GLUT();
			glut.glutWireTeapot(1);
			gl.glPopMatrix();
		}
		// set light position
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, myLightPosition, 0);
		// drawCoordinateFrame(gl);
		setMaterialForGrass(gl);
		drawTerrain(gl);
		drawTrees(gl);
		List<Road> roads = myTerrain.roads();
		for (int i = 0; i < roads.size(); i++) {
			gl.glPushMatrix();
			drawRoad(gl, roads.get(i));
			gl.glPopMatrix();
		}
		// using vbos to draw objects
		gl.glPushMatrix();
		float[] othersPos = myTerrain.getOthers(); 
		gl.glTranslated(othersPos[0], myTerrain.altitude(othersPos[0], othersPos[2]) + 0.01, othersPos[2]);
		gl.glRotated(degree, 0, 1, 0);
		degree++;
		degree = degree % 360;
		gl.glUseProgram(shaderProgram);
		drawSpecialObject(gl);
		gl.glUseProgram(0);
		gl.glPopMatrix();
		
		// draw animated pool
		drawPool(gl);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(1, 1, 1, 1);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		gl.glEnable(GL2.GL_TEXTURE_2D);

		// set light properties
		float[] amb = { 0.3f, 0.3f, 0.3f, 1.0f };
		float[] dif = { 0.25f, 1.0f, 1.0f, 1.0f };
		float[] spe = { 1.0f, 1.0f, 0.25f, 1.0f };

		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, amb, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, spe, 0);

		// initialize textures
		textures = new MyTexture[NUM_TEXTURES];
		textures[0] = new MyTexture(gl, grassTextureFileName, "png", true);
		textures[1] = new MyTexture(gl, trunkTextureFileName, "png", false);
		textures[2] = new MyTexture(gl, bushTextureFileName, "jpg", true);

		// load vbos
		gl.glGenBuffers(1, bufferIDs, 0);
		FloatBuffer posData = Buffers.newDirectFloatBuffer(MySpecialObject.getPoints());
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, MySpecialObject.lengthInBytes(), posData, GL2.GL_STATIC_DRAW);

		// init shaders
		try {
			initShader(gl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// GL2 gl = drawable.getGL().getGL2();
		// gl.glMatrixMode(GL2.GL_PROJECTION);
		// gl.glLoadIdentity();
		//
		// gl.glOrtho(-10, 10, -10, 10, -20, 20);
		// gl.glFrustum(-1, 1, -1, 1, 2, 100);
		// GLU glu = new GLU();
		// glu.gluPerspective(60, 1, 1, 20);
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
		float[] ambientCoeff2 = { 0.3f, 0f, 0f, 1.0f };
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

	private void setMaterialForSpecialObject(GL2 gl) {
		float[] ambientCoeff = { 0.3f, 0.3f, 0.3f, 1.0f };
		float[] diffuseCoeff = { 0.0f, 0.0f, 0.0f, 1.0f };
		float[] specCoeff = { 0.0f, 0.0f, 0.0f, 1.0f };
		float[] emissionCoeff = { 0.0f, 1f, 1f, 1.0f };
		float phong = 10f;
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff, 0);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff, 0);
	}

	private void drawSpecialObject(GL2 gl) {
		setMaterialForSpecialObject(gl);

		gl.glTranslated(2, myTerrain.altitude(2, 8) + 0.1, 4);
		gl.glRotated(degree, 0, 1, 0);
		degree++;
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		gl.glLineWidth(10);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[0]);
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
		gl.glDrawArrays(GL2.GL_TRIANGLES, 0, MySpecialObject.numberOfPoints());
		gl.glLineWidth(1);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
	}

	private void drawRoad(GL2 gl, Road road) {
		setMaterialForRoad(gl);
		gl.glBegin(GL2.GL_QUADS);
		// TODO: try to set the height of the road according to the terrain
		// in this version, the road has the same height/altitude
		double[] p = road.point(0);
		// shift the height a little bit so that it will be drawn above the
		// terrain
		double height = myTerrain.altitude(p[0], p[1]) + 0.01;
		for (int i = 0; i < ROAD_SEG_NUM - 1; i++) {
			double t = road.size() / (double) ROAD_SEG_NUM * i;
			double[] p1 = road.edgePoint(t, false);
			double[] p2 = road.edgePoint(t, true);
			double[] p3 = road.edgePoint(t + road.size() / (double) ROAD_SEG_NUM, true);
			double[] p4 = road.edgePoint(t + road.size() / (double) ROAD_SEG_NUM, false);

			gl.glVertex3d(p1[0], height, p1[1]);
			gl.glVertex3d(p2[0], height, p2[1]);
			gl.glVertex3d(p3[0], height, p3[1]);
			gl.glVertex3d(p4[0], height, p4[1]);

		}
		gl.glEnd();
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

			// draw around
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
			// | /|
			// | / |
			// | / |
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
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e) {
		float increment = 0.1f;
		float incrementAngle = 1.0f;
		float radius = 1.0f;

		switch (e.getKeyCode()) {
		// case KeyEvent.VK_LEFT:
		// myLightPosition[0] --;
		// break;
		//
		// case KeyEvent.VK_RIGHT:
		// myLightPosition[0] ++;
		// break;
		//
		// case KeyEvent.VK_UP:
		// myLightPosition[1] ++;
		// break;
		//
		// case KeyEvent.VK_DOWN:
		// myLightPosition[1]--;
		// break;

		case KeyEvent.VK_UP:
			// moving forward
			z += Math.cos(Math.toRadians(angle)) * increment;
			x += Math.sin(Math.toRadians(angle)) * increment;

			lz += Math.cos(Math.toRadians(angle)) * increment;
			lx += Math.sin(Math.toRadians(angle)) * increment;

			break;

		case KeyEvent.VK_DOWN:
			// moving backward
			z -= Math.cos(Math.toRadians(angle)) * increment;
			x -= Math.sin(Math.toRadians(angle)) * increment;

			lz -= Math.cos(Math.toRadians(angle)) * increment;
			lx -= Math.sin(Math.toRadians(angle)) * increment;
			break;

		case KeyEvent.VK_RIGHT:
			// turning right
			angle -= incrementAngle;
			lx = (float) (x + Math.sin(Math.toRadians(angle)) * radius);
			lz = (float) (z + Math.cos(Math.toRadians(angle)) * radius);
			break;

		case KeyEvent.VK_LEFT:
			// turning left
			angle += incrementAngle;
			lx = (float) (x + Math.sin(Math.toRadians(angle)) * radius);
			lz = (float) (z + Math.cos(Math.toRadians(angle)) * radius);
			break;

		case KeyEvent.VK_1:
			// change to 3rd person view
			changeView = !changeView;
			break;
		}

		if (e.getKeyCode() < 250)
			keys[e.getKeyCode()] = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() < 250)
			keys[e.getKeyCode()] = false;
	}
}
