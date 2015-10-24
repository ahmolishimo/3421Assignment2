package ass2.spec;


/**This class is used for calculation of the road
 * including the tangent line at a given point
 * the line perpendicular to the given line
 * normalization etc
 * @author Andy
 *
 */
public class MathUtil {
	
	/**return the length of the vector v
	 * @param v should be 3 dimension
	 * @return
	 */
	public static double length(double[] v) {
		double l = 0;
		l = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		return l;
	}
	
	/**
	 * @param v a 3 dimension vector
	 * @return normalized v, a 3 dimension vector
	 */
	public static double[] normalize(double[] v) {
		double l = length(v);
		v[0] = v[0] / l;
		v[1] = v[1] / l;
		v[2] = v[2] / l;
		return v;
	}
	
	// this function returns v1v2 X v1v3
	/**v1, v2, v3 are all 3 dimension
	 * @param v1
	 * @param v2
	 * @param v3
	 * @return this function returns v1v2 X v1v3 
	 */
	public static double[] normal(double[] v1, double[] v2, double[] v3) {
		double[] n = new double[3];
		n[0] = (v2[1] - v1[1])*(v3[2] - v1[2]) - (v3[1] - v1[1])*(v2[2] - v1[2]);
		n[1] = (v3[0] - v1[0])*(v2[2] - v1[2]) - (v2[0] - v1[0])*(v3[2] - v1[2]);
		n[2] = (v2[0] - v1[0])*(v3[1] - v1[1]) - (v3[0] - v1[0])*(v2[1] - v1[1]);
		n = normalize(n);
		return n;
	}
}
