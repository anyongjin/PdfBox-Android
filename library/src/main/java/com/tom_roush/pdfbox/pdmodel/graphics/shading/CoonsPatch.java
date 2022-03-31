package com.tom_roush.pdfbox.pdmodel.graphics.shading;

import android.graphics.PointF;

import java.util.List;

public class CoonsPatch extends Patch{

    /**
     * Constructor of a patch for type 6 shading.
     *
     * @param points 12 control points
     * @param color 4 corner colors
     */
    protected CoonsPatch(PointF[] points, float[][] color)
    {
        super(color);
        controlPoints = reshapeControlPoints(points);
        level = calcLevel();
        listOfTriangles = getTriangles();
    }

    // adjust the 12 control points to 4 groups, each group defines one edge of a patch
    private PointF[][] reshapeControlPoints(PointF[] points)
    {
        PointF[][] fourRows = new PointF[4][4];
        fourRows[2] = new PointF[]
                {
                        points[0], points[1], points[2], points[3]
                }; // d1
        fourRows[1] = new PointF[]
                {
                        points[3], points[4], points[5], points[6]
                }; // c2
        fourRows[3] = new PointF[]
                {
                        points[9], points[8], points[7], points[6]
                }; // d2
        fourRows[0] = new PointF[]
                {
                        points[0], points[11], points[10], points[9]
                }; // c1
        return fourRows;
    }

    // calculate the dividing level from control points
    private int[] calcLevel()
    {
        int[] l =
                {
                        4, 4
                };
        // if two opposite edges are both lines, there is a possibility to reduce the dividing level
        if (isEdgeALine(controlPoints[0]) && isEdgeALine(controlPoints[1]))
        {
            double lc1 = getLen(controlPoints[0][0], controlPoints[0][3]),
                    lc2 = getLen(controlPoints[1][0], controlPoints[1][3]);
            // determine the dividing level by the lengths of edges
            if (lc1 > 800 || lc2 > 800)
            {
                // keeps init value 4
            }
            else if (lc1 > 400 || lc2 > 400)
            {
                l[0] = 3;
            }
            else if (lc1 > 200 || lc2 > 200)
            {
                l[0] = 2;
            }
            else
            {
                l[0] = 1;
            }
        }

        // the other two opposite edges
        if (isEdgeALine(controlPoints[2]) && isEdgeALine(controlPoints[3]))
        {
            double ld1 = getLen(controlPoints[2][0], controlPoints[2][3]),
                    ld2 = getLen(controlPoints[3][0], controlPoints[3][3]);
            if (ld1 > 800 || ld2 > 800)
            {
                // keeps init value 4
            }
            else if (ld1 > 400 || ld2 > 400)
            {
                l[1] = 3;
            }
            else if (ld1 > 200 || ld2 > 200)
            {
                l[1] = 2;
            }
            else
            {
                l[1] = 1;
            }
        }
        return l;
    }

    // get a list of triangles which compose this coons patch
    private List<ShadedTriangle> getTriangles()
    {
        // 4 edges are 4 cubic Bezier curves
        CubicBezierCurve eC1 = new CubicBezierCurve(controlPoints[0], level[0]);
        CubicBezierCurve eC2 = new CubicBezierCurve(controlPoints[1], level[0]);
        CubicBezierCurve eD1 = new CubicBezierCurve(controlPoints[2], level[1]);
        CubicBezierCurve eD2 = new CubicBezierCurve(controlPoints[3], level[1]);
        CoordinateColorPair[][] patchCC = getPatchCoordinatesColor(eC1, eC2, eD1, eD2);
        return getShadedTriangles(patchCC);
    }

    @Override
    protected PointF[] getFlag1Edge()
    {
        return controlPoints[1].clone();
    }

    @Override
    protected PointF[] getFlag2Edge()
    {
        PointF[] implicitEdge = new PointF[4];
        implicitEdge[0] = controlPoints[3][3];
        implicitEdge[1] = controlPoints[3][2];
        implicitEdge[2] = controlPoints[3][1];
        implicitEdge[3] = controlPoints[3][0];
        return implicitEdge;
    }

    @Override
    protected PointF[] getFlag3Edge()
    {
        PointF[] implicitEdge = new PointF[4];
        implicitEdge[0] = controlPoints[0][3];
        implicitEdge[1] = controlPoints[0][2];
        implicitEdge[2] = controlPoints[0][1];
        implicitEdge[3] = controlPoints[0][0];
        return implicitEdge;
    }

    /*
     dividing a patch into a grid, return a matrix of the coordinate and color at the crossing points of the grid,
     the rule to calculate the coordinate is defined in page 195 of PDF32000_2008.pdf, the rule to calculate the
     corresponding color is bilinear interpolation
     */
    private CoordinateColorPair[][] getPatchCoordinatesColor(CubicBezierCurve c1, CubicBezierCurve c2, CubicBezierCurve d1, CubicBezierCurve d2)
    {
        PointF[] curveC1 = c1.getCubicBezierCurve();
        PointF[] curveC2 = c2.getCubicBezierCurve();
        PointF[] curveD1 = d1.getCubicBezierCurve();
        PointF[] curveD2 = d2.getCubicBezierCurve();

        int numberOfColorComponents = cornerColor[0].length;
        int szV = curveD1.length;
        int szU = curveC1.length;

        CoordinateColorPair[][] patchCC = new CoordinateColorPair[szV][szU];

        double stepV = (double) 1 / (szV - 1);
        double stepU = (double) 1 / (szU - 1);
        double v = -stepV;
        for (int i = 0; i < szV; i++)
        {
            // v and u are the assistant parameters
            v += stepV;
            double u = -stepU;
            for (int j = 0; j < szU; j++)
            {
                u += stepU;
                double scx = (1 - v) * curveC1[j].x + v * curveC2[j].x;
                double scy = (1 - v) * curveC1[j].y + v * curveC2[j].y;
                double sdx = (1 - u) * curveD1[i].x + u * curveD2[i].x;
                double sdy = (1 - u) * curveD1[i].y + u * curveD2[i].y;
                double sbx = (1 - v) * ((1 - u) * controlPoints[0][0].x + u * controlPoints[0][3].x)
                        + v * ((1 - u) * controlPoints[1][0].x + u * controlPoints[1][3].x);
                double sby = (1 - v) * ((1 - u) * controlPoints[0][0].y + u * controlPoints[0][3].y)
                        + v * ((1 - u) * controlPoints[1][0].y + u * controlPoints[1][3].y);

                double sx = scx + sdx - sbx;
                double sy = scy + sdy - sby;
                // the above code in this for loop defines the patch surface (coordinates)

                PointF tmpC = new PointF((float) sx, (float) sy);

                float[] paramSC = new float[numberOfColorComponents];
                for (int ci = 0; ci < numberOfColorComponents; ci++)
                {
                    paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci])
                            + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci])); // bilinear interpolation
                }
                patchCC[i][j] = new CoordinateColorPair(tmpC, paramSC);
            }
        }
        return patchCC;
    }
}