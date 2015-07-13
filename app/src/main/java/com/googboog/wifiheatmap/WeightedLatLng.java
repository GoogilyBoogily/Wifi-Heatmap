package com.googboog.wifiheatmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import com.google.maps.android.quadtree.PointQuadTree;

/**
 * A wrapper class that can be used in a PointQuadTree
 * Created from a LatLng and optional intensity: point coordinates of the LatLng and the intensity
 * value can be accessed from it later.
 */
public class WeightedLatLng implements PointQuadTree.Item {

	/**
	 * Default intensity to use when intensity not specified
	 */
	public static final double DEFAULT_INTENSITY = 1;

	/**
	 * Projection to use for points
	 * Converts LatLng to (x, y) coordinates using a SphericalMercatorProjection
	 */
	private static final SphericalMercatorProjection sProjection = new SphericalMercatorProjection(HeatmapTile.WORLD_WIDTH);

	private Point mPoint;

	private double mIntensity;

	/**
	 * Constructor
	 *
	 * @param latLng    LatLng to add to wrapper
	 * @param intensity Intensity to use: should be greater than 0
	 *                  Default value is 1.
	 *                  This represents the "importance" or "value" of this particular point
	 *                  Higher intensity values map to higher colours.
	 *                  Intensity is additive: having two points of intensity 1 at the same
	 *                  location is identical to having one of intensity 2.
	 */
	public WeightedLatLng(LatLng latLng, double intensity) {
		mPoint = sProjection.toPoint(latLng);
		if (intensity >= 0)
			mIntensity = intensity;
		else
			mIntensity = DEFAULT_INTENSITY;
	}

	/**
	 * Constructor that uses default value for intensity
	 *
	 * @param latLng LatLng to add to wrapper
	 */
	public WeightedLatLng(LatLng latLng) {
		this(latLng, DEFAULT_INTENSITY);
	}

	public Point getPoint() {
		return mPoint;
	}

	public double getIntensity() {
		return mIntensity;
	}

}