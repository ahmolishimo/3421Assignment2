package ass2.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * COMMENT: Comment Road 
 *
 * @author malcolmr
 */
public class Road {

    private List<Double> myPoints;
    private double myWidth;
    
    /** 
     * Create a new road starting at the specified point
     */
    public Road(double width, double x0, double y0) {
        myWidth = width;
        myPoints = new ArrayList<Double>();
        myPoints.add(x0);
        myPoints.add(y0);
    }

    /**
     * Create a new road with the specified spine 
     *
     * @param width
     * @param spine
     */
    public Road(double width, double[] spine) {
        myWidth = width;
        myPoints = new ArrayList<Double>();
        for (int i = 0; i < spine.length; i++) {
            myPoints.add(spine[i]);
        }
    }

    /**
     * The width of the road.
     * 
     * @return
     */
    public double width() {
        return myWidth;
    }

    /**
     * Add a new segment of road, beginning at the last point added and ending at (x3, y3).
     * (x1, y1) and (x2, y2) are interpolated as bezier control points.
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     */
    public void addSegment(double x1, double y1, double x2, double y2, double x3, double y3) {
        myPoints.add(x1);
        myPoints.add(y1);
        myPoints.add(x2);
        myPoints.add(y2);
        myPoints.add(x3);
        myPoints.add(y3);        
    }
    
    /**
     * Get the number of segments in the curve
     * 
     * @return
     */
    public int size() {
        return myPoints.size() / 6;
    }

    /**
     * Get the specified control point.
     * 
     * @param i
     * @return
     */
    public double[] controlPoint(int i) {
        double[] p = new double[2];
        p[0] = myPoints.get(i*2);
        p[1] = myPoints.get(i*2+1);
        return p;
    }
    
    /**
     * Get a point on the spine. The parameter t may vary from 0 to size().
     * Points on the kth segment take have parameters in the range (k, k+1).
     * 
     * @param t
     * @return
     */
    public double[] point(double t) {
        int i = (int)Math.floor(t);
        t = t - i;
        
        i *= 6;
        
        double x0 = myPoints.get(i++);
        double y0 = myPoints.get(i++);
        double x1 = myPoints.get(i++);
        double y1 = myPoints.get(i++);
        double x2 = myPoints.get(i++);
        double y2 = myPoints.get(i++);
        double x3 = myPoints.get(i++);
        double y3 = myPoints.get(i++);
        
        double[] p = new double[2];

        p[0] = b(0, t) * x0 + b(1, t) * x1 + b(2, t) * x2 + b(3, t) * x3;
        p[1] = b(0, t) * y0 + b(1, t) * y1 + b(2, t) * y2 + b(3, t) * y3;        
        
        return p;
    }
    
    /**
     * Calculate the Bezier coefficients
     * 
     * @param i a degree 3 Bezier curve, same as k in lecture notes
     * @param t
     * @return
     */
    private double b(int i, double t) {
        
        switch(i) {
        
        case 0:
            return (1-t) * (1-t) * (1-t);

        case 1:
            return 3 * (1-t) * (1-t) * t;
            
        case 2:
            return 3 * (1-t) * t * t;

        case 3:
            return t * t * t;
        }
        
        // this should never happen
        throw new IllegalArgumentException("" + i);
    }
    
    /**
     * Calculate the bezier coefficience of a degree less than the original
     * bezier curve. Used in calculaiton of tangent line to the road.
     * @param i should be 0, 1 or 2 
     * @param t
     * @return
     */
    private double bl(int i, double t) {
    		switch(i) {
    		case 0:
    			return (1-t)*(1-t);
    		case 1:
    			return 2*t*(1-t);
    		case 2:
    			return t*t;
    		}
    		throw new IllegalArgumentException("" + i);
    }
    
    /** normalize the given vector
     * @param vector
     * @return
     */
    private double[] normalize(double[] vector) {
    		double l = length(vector);
    		for(int i = 0; i < vector.length; i++) {
    			vector[i] = vector[i]/l;
    		}
    		return vector;
    }
    
    /** calculate the length of the given vector
     * @param vector
     * @return the length of the vector
     */
    private double length(double[] vector) {
    		double l = 0;
    		for(int i = 0; i < vector.length; i++) {
    			l += vector[i]*vector[i];
    		}
    		l = Math.sqrt(l);
    		return l;
    }
    
    /** calculate the tangent line at the given point t
     * @param t given in range(0, size)
     * @return the tangent line at this point, already normalized
     */
    private double[] tangent(double t) {
    		double[] tangent = new double[2];
    		// get old control point(4 control point in the segment of the curve)
    		int i = (int)Math.floor(t);
        t = t - i;
        i *= 6;
        double x0 = myPoints.get(i++);
        double y0 = myPoints.get(i++);
        double x1 = myPoints.get(i++);
        double y1 = myPoints.get(i++);
        double x2 = myPoints.get(i++);
        double y2 = myPoints.get(i++);
        double x3 = myPoints.get(i++);
        double y3 = myPoints.get(i++);
        // get new control point of degree - 1 bezier curve
        double xp0 = x1 - x0;
        double yp0 = y1 - y0;
        double xp1 = x2 - x1;
        double yp1 = y2 - y1;
        double xp2 = x3 - x2;
        double yp2 = y3 - y2;
        // calculate the tangent line to the given point
        tangent[0] = 3*(bl(0, t)*xp0 + bl(1, t)*xp1 + bl(2, t)*xp2);
        tangent[1] = 3*(bl(0, t)*yp0 + bl(1, t)*yp1 + bl(2, t)*yp2);
        normalize(tangent);
    		return tangent;
    }
    
    /** calculate the perpendicular vector to the given line
     * the direction of the perpendicular vector is the same as to rotate
     * the line clockwise by 90 degrees
     * @param line should be 2 dimension
     * @return
     */
    private double[] rightPerpendicular(double[] line) {
    		double[] p = new double[2];
    		p[0] = line[1];
    		p[1] = 0-line[0];
    		return p;
    }
    
    /** calculate the perpendicular vector to the given line
     * the direction of the perpendicular vector is the same as to rotate
     * the line counter-clockwise by 90 degrees
     * @param line
     * @return
     */
    private double[] leftPerpendicular(double[] line) {
    		double[] p = new double[2];
    		p[0] = 0 - line[1];
    		p[1] = line[0];
    		return p;
    }
    
    /** move a point along a direction to the specified distance
     * Note: the direction should be normalized
     * @param direction the direction along which to move the point
     * @param from the origin of the point
     * @param distance the amount of movement
     * @return the destination point after the movement
     */
    private double[] moveAlong(double[] direction, double[] from, double distance) {
    		double[] destination = new double[2];
    		destination[0] = from[0] + distance * direction[0];
    		destination[1] = from[1] + distance * direction[1];
    		return destination;
    }
    
    /** calculate the edge point of the road
     * @param t
     * @param right if right = true, this function returns the edge point to the right of the road
     * @return a point(2D vector)
     */
    public double[] edgePoint(double t, boolean right) {
    		double[] edge = new double[2];
    		edge[0] = 0;
    		edge[1] = 0;
    		double[] p = point(t);
    		double[] tangentVector = tangent(t);
    		double[] perp;
    		if(right) {
    			perp = rightPerpendicular(tangentVector);
    		} else {
    			perp = leftPerpendicular(tangentVector);
    		}
    		// TODO: adjust the divider so that the road looks nice and make the width right
    		edge = moveAlong(perp, p, myWidth/5.0);
    		return edge;
    }
}
