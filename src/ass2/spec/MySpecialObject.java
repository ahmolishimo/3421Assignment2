package ass2.spec;

public class MySpecialObject {
	private static float points[] = {-0.5f, 0, -0.289f, 
							  0.5f, 0, -0.289f, 
							  0, 0, 0.578f,	// bottom face 
							  0, 0, 0.578f,
							  0.5f, 0, -0.289f,
							  0, 0.816f, 0, // one side face
							  0.5f, 0, -0.289f, 
							  -0.5f, 0, -0.289f, 
							  0, 0.816f, 0, // second side face
							  -0.5f, 0, -0.289f, 
							  0, 0, 0.578f, 
							  0, 0.816f, 0};// third side face
	public static float[] getPoints() {
		return points;
	}
	
	public static int lengthInBytes() {
		return points.length*Float.BYTES;
	}
	
	public static int numberOfPoints() {
		return points.length/3;
	}
}
