package ass2.spec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * COMMENT: Comment Game 
 *
 * @author malcolmr
 */
public class Game extends JFrame implements GLEventListener, KeyListener{
	
    private Terrain myTerrain;
    private double[] myEyePosition;
    private double[] myEyePositionMovement;
    private float[] myLightPosition;
    
    private final int NUM_TEXTURES = 2;
    private MyTexture[] textures;
    //int[] textures;
	private String grassTextureFileName = "src/ass2/spec/grass.png";
	private String trunkTextureFileName = "src/ass2/spec/trunk.png";
    
	private final int TREE_TRUNK_NUM = 24;
	private final int TREE_HEIGHT = 6;
	private final int TREE_RADIUS = 1;
	
    public Game(Terrain terrain) {
        super("Assignment 2");
        myEyePositionMovement = new double[3];
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
     * @param args - The first argument is a level file in JSON format
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
        glu.gluLookAt(5, 6, 10, 0, 0, 0, 0, 1, 0);
        
        // Set light position
        float[] lightdir = myTerrain.getSunlight();
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, myLightPosition, 0);
        //System.out.println("light position: " + myLightPosition[0] + "  " + myLightPosition[1] + "  " + myLightPosition[2]);
        //drawCoordinateFrame(gl);
        // set material properties to grass
        float[] diffuseCoeff = {0.1f, 0.6f, 0.2f, 1.0f};
        float[] ambientCoeff = {0.1f, 0.6f, 0.2f, 1.0f};
        float[] specCoeff = {0.3f, 0.6f, 0.2f, 1.0f};
        float[] emissionCoeff = {0.3f, 0.6f, 0.2f, 1.0f};
        float phong = 10f;
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong);
        drawTerrain(gl);
        
        // change material properties to trunk
        float[] diffuseCoeff2 = {0.3565f, 0.2174f, 0.2f, 1.0f};
        float[] ambientCoeff2 = {0.3565f, 0.2174f, 0.2f, 1.0f};
        float[] specCoeff2 = {0.3565f, 0.2174f, 0.2f, 1.0f};
        float[] emissionCoeff2 = {0.3565f, 0.2174f, 0.2f, 1.0f};
        float phong2 = 1f;
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, diffuseCoeff2, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientCoeff2, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, specCoeff2, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION, emissionCoeff2, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, phong2);
        drawTrees(gl);
    }
    
    private void drawTrees(GL2 gl) {
    		for(int i = 0; i < myTerrain.trees().size(); i++) {
    			double[] pos = myTerrain.trees().get(i).getPosition();
    			gl.glPushMatrix();
    			gl.glTranslated(pos[0], pos[1], pos[2]);
    			gl.glScaled(0.5, 0.5, 0.5);
    			gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[1].getTextureId());
    			//gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    			// draw around
    			gl.glBegin(GL2.GL_QUAD_STRIP);
    			for(int j = 0; j < TREE_TRUNK_NUM; j++) {
    				double x = Math.cos(2*j*TREE_RADIUS*Math.PI/TREE_TRUNK_NUM);
    				double z = Math.sin(2*j*TREE_RADIUS*Math.PI/TREE_TRUNK_NUM);
    				gl.glNormal3d(x, 0, z);
    				gl.glTexCoord2d(1.0/TREE_TRUNK_NUM * j, 0);
    				gl.glVertex3d(x, 0, z);
    				gl.glTexCoord2d(1.0/TREE_TRUNK_NUM * j, 1);
    				gl.glVertex3d(x, TREE_HEIGHT, z);
    			}
    			gl.glEnd();
    			// draw top ball
    			GLUT glut = new GLUT();
    			gl.glTranslated(0, TREE_HEIGHT, 0);
    			glut.glutSolidSphere(3, 24, 24);
        		gl.glPopMatrix();
    		}
    		
    }
    
    private void drawTerrain(GL2 gl) {
    		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
    		int terrainWidth = (int)myTerrain.size().getWidth();
        int terrainHeight = (int)myTerrain.size().getHeight();
        // second, draw triangles
        gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0].getTextureId());
        gl.glBegin(GL2.GL_TRIANGLES);
        {
        		// for each point, draw a triangle with this point
        		// the point to the right and the point down it
        		for(int i = 0; i < terrainWidth - 1; i++) {
        			for(int j = 0; j < terrainHeight - 1; j++) {
        				double[] thisPoint = {i, myTerrain.getGridAltitude(i, j), j};
        				double[] downPoint = {i, myTerrain.getGridAltitude(i, j+1), j+1};
        				double[] rightPoint = {i+1, myTerrain.getGridAltitude(i+1, j), j};
        				double[] cornerPoint = {i+1, myTerrain.getGridAltitude(i+1, j+1), j+1};
        				double[] n1 = MathUtil.normal(thisPoint, downPoint, rightPoint);
        				double[] n2 = MathUtil.normal(downPoint, cornerPoint, rightPoint);
        				gl.glNormal3dv(n1, 0);
        				gl.glTexCoord2d(0, 0);gl.glVertex3dv(thisPoint, 0);
        				gl.glTexCoord2d(0, 0.1);gl.glVertex3dv(downPoint, 0);
        				gl.glTexCoord2d(0.1, 0);gl.glVertex3dv(rightPoint, 0);
        				
        				gl.glNormal3dv(n2, 0);
        				gl.glTexCoord2d(0, 0);gl.glVertex3dv(downPoint, 0);
        				gl.glTexCoord2d(0, 0.1);gl.glVertex3dv(cornerPoint, 0);
        				gl.glTexCoord2d(0.1, 0);gl.glVertex3dv(rightPoint, 0);
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
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // TODO Auto-generated method stub
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        // set light properties
        float[] amb = {0.1f, 0.2f, 0.3f, 1.0f};
        float[] dif = {1.0f, 0.0f, 0.1f, 1.0f};
        float[] spe = {1.0f, 1.0f, 1.0f, 1.0f};
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, amb, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, spe, 0);
        
        // initialize textures
        textures = new MyTexture[NUM_TEXTURES];
        textures[0] = new MyTexture(gl, grassTextureFileName, "png", true);
        textures[1] = new MyTexture(gl, trunkTextureFileName, "png", false);
        /*
        GLProfile glp = GLProfile.getDefault();
        File textureData = new File(grassTextureFileName);
        TextureData data = null;
        try {
			data = TextureIO.newTextureData(glp, textureData, false, "png");
		} catch (IOException e) {
			e.printStackTrace();
		}
        gl.glGenTextures(NUM_TEXTURES, textures, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, data.getInternalFormat(),
				data.getWidth(), data.getHeight(), 0, data.getPixelFormat(),
				data.getPixelType(), data.getBuffer());
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        textureData = new File(trunkTextureFileName);
        try {
			data = TextureIO.newTextureData(glp, textureData, false, "png");
		} catch (IOException e) {
			e.printStackTrace();
		}
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[1]);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, data.getInternalFormat(),
				data.getWidth(), data.getHeight(), 0, data.getPixelFormat(),
				data.getPixelType(), data.getBuffer());
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);*/
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        //gl.glOrtho(-10, 10, -10, 10, -20, 20);
        gl.glFrustum(-1, 1, -1, 1, 2, 100);
    }

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		switch(e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			myLightPosition[0] --;
			break;
			
		case KeyEvent.VK_RIGHT:
			myLightPosition[0] ++;
			break;
			
		case KeyEvent.VK_UP:
			myLightPosition[1] ++;
			break;
			
		case KeyEvent.VK_DOWN:
			myLightPosition[1]--;
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
    