package fiji.plugin.spottracker;

import ij.ImagePlus;

/**
 * This class is used to store user settings for the {@link Spot_Tracker} plugin.
 * It is simply made of public fields 
 */
public class Settings {
	private static final float 		DEFAULT_EXPECTED_DIAMETER	= 6.5f;
	private static final boolean	DEFAULT_USE_MEDIAN_FILTER 	= false;
	private static final boolean	DEFAULT_ALLOW_EDGE_MAXIMA	= false;
			
	/** The ImagePlus to operate on. */
	public ImagePlus imp;
	/** Stores the expected blob diameter in physical units. */
	public float 	expectedDiameter = DEFAULT_EXPECTED_DIAMETER;
	/** If true, a 3x3 median filter will be applied to the image before segmentation. */
	public boolean 	useMedianFilter = DEFAULT_USE_MEDIAN_FILTER;
	/** If true, blob found at the edge of the image will not be discarded. */
	public boolean 	allowEdgeMaxima = DEFAULT_ALLOW_EDGE_MAXIMA;
	public static final Settings DEFAULT = new Settings(); 
}