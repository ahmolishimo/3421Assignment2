package ass2.spec;


public class MathUtil {
	
	public static double length(double[] v) {
		double l = 0;
		l = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		return l;
	}
	
	public static double[] normalize(double[] v) {
		double l = length(v);
		v[0] = v[0] / l;
		v[1] = v[1] / l;
		v[2] = v[2] / l;
		return v;
	}
	
	// this function returns v1v2 X v1v3
	public static double[] normal(double[] v1, double[] v2, double[] v3) {
		double[] n = new double[3];
		n[0] = (v2[1] - v1[1])*(v3[2] - v1[2]) - (v3[1] - v1[1])*(v2[2] - v1[2]);
		n[1] = (v3[0] - v1[0])*(v2[2] - v1[2]) - (v2[0] - v1[0])*(v3[2] - v1[2]);
		n[2] = (v2[0] - v1[0])*(v3[1] - v1[1]) - (v3[0] - v1[0])*(v2[1] - v1[1]);
		n = normalize(n);
		return n;
	}
}
