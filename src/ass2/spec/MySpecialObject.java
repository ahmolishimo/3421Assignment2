package ass2.spec;

public class MySpecialObject {
	private static float points[] = {
			0, 0, 0, 
			1, 0, 0, 
			1, 0, 1, 
			0, 0, 1, // bottom, lies on y = 0 
			0, 0, 0, 
			0, 0, 1, 
			0, 1, 1, 
			0, 1, 0, // front, lies on x = 0 
			0, 0, 0, 
			0, 1, 0, 
			1, 1, 0, 
			1, 0, 0, // side, lies on z = 0 
			0, 0, 1, 
			1, 0, 1, 
			1, 1, 1, 
			0, 1, 1, // side, lies on z = 1 
			1, 0, 1, 
			1, 0, 0, 
			1, 1, 0, 
			1, 1, 1, // back, lies on x = 1 
			0, 1, 0, 
			0, 1, 1, 
			1, 1, 1, 
			1, 1, 0, // top, lies on y = 1 
			0, 0, 
			0, 1, 
			1, 1, 
			1, 0, // texture coordinates
			0, 0, 
			0, 1, 
			1, 1, 
			1, 0, //
			0, 0, 
			0, 1, 
			1, 1, 
			1, 0, // 
			0, 0, 
			0, 1, 
			1, 1, 
			1, 0, //
			0, 0, 
			0, 1, 
			1, 1, 
			1, 0, //
			0, 0, 
			0, 1, 
			1, 1, 
			1, 0, //
			0, -1, 0, // normals 
			0, -1, 0,
			0, -1, 0,
			0, -1, 0,
			-1, 0, 0,
			-1, 0, 0,
			-1, 0, 0,
			-1, 0, 0,
			0, 0, -1,
			0, 0, -1,
			0, 0, -1,
			0, 0, -1,
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1, 
			1, 0, 0, 
			1, 0, 0, 
			1, 0, 0, 
			1, 0, 0, 
			0, 1, 0,
			0, 1, 0,
			0, 1, 0,
			0, 1, 0};
	public static float[] getPoints() {
		return points;
	}
	
	public static int lengthInBytes() {
		return points.length*Float.BYTES;
	}
	
	public static int numberOfPoints() {
		return 4*6;
	}
	
}
