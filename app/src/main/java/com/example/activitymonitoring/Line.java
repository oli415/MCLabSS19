package com.example.activitymonitoring;

import android.graphics.Point;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Line {
    public static final int COLINEAR = 0;
    public static final int CLOCKWISE = 1;
    public static final int COUNTERCLOCKWISE = 2;
    private Position begin;
    private Position end;

    public Line(Position begin, Position end){
        this.begin = new Position(begin);
        this.end = new Position(end);

    }

    // Given three colinear points p, q, r, the function checks if
    // point q lies on line segment 'pr'
    boolean onSegment(Position p, Position q, Position r)
    {
        if (q.getX() <= max(p.getX(), r.getX()) && q.getX() >= min(p.getX(), r.getX()) &&
                q.getY() <= max(p.getY(), r.getY()) && q.getY() >= min(p.getY(), r.getY()))
            return true;

        return false;
    }

    // To find orientation of ordered triplet (p, q, r).
    // The function returns following values
    // 0 --> p, q and r are colinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    int orientation(Position p, Position q, Position r)
    {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.
        double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) -
                (q.getX() - p.getX()) * (r.getY() - q.getY());

        if (val == 0.0) return COLINEAR;  // colinear

        return (val > 0)? CLOCKWISE: COUNTERCLOCKWISE; // clock or counterclock wise
    }

    //https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
    public boolean intersects(Line lineToTest) {
        // Find the four orientations needed for general and special cases
        int o1 = orientation(begin, end, lineToTest.begin);
        int o2 = orientation(begin, end, lineToTest.end);
        int o3 = orientation(lineToTest.begin, lineToTest.end, begin);
        int o4 = orientation(lineToTest.begin, lineToTest.end, end);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == COLINEAR && onSegment(begin, lineToTest.begin, end)) return true;

        // p1, q1 and q2 are colinear and q2 lies on segment p1q1
        if (o2 == COLINEAR && onSegment(begin, lineToTest.end, end)) return true;

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == COLINEAR && onSegment(lineToTest.begin, begin, lineToTest.end)) return true;

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == COLINEAR && onSegment(lineToTest.begin, end, lineToTest.end)) return true;

        return false; // Doesn't fall in any of the above cases
    }

}
