package fiji.plugin.nperry.features;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class BlobBrightness <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	/*
	 * FIELDS
	 */
	
	/** The {@link Feature} that this FeatureAnalyzer extracts. */
	private static final Feature FEATURE = Feature.BRIGHTNESS;
	/** The original image that is analyzed. */
	private Image<T> img;
	/** The diameter of the blob, in physical units. */
	private float diam;
	/** The calibration of the image, used to convert from physical units to pixel units. */
	private float[] calibration;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public BlobBrightness(Image<T> originalImage, float diam, float[] calibration) {
		this.img = originalImage;
		this.diam = diam;
		this.calibration = calibration;
	}

	public BlobBrightness(Image<T> originalImage, float diam) {
		this(originalImage, diam, originalImage.getCalibration());
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public Feature getFeature() {
		return FEATURE;
	}

	@Override
	public boolean isNormalized() {
		return false;
	}

	@Override
	public void process(Spot spot) {
		
		// 1 - Initialize sphere cursor
		SphereCursor<T> cursor = new SphereCursor<T>(img, spot.getCoordinates(), diam/2, calibration);
		
		// 2 - Iterate pixels in sphere, and aggregate total intensity.
		float intensity = 0;
		//System.out.println();
		//System.out.println("New max: " + MathLib.printCoordinates(spot.getCoordinates()));
		//System.out.println();
		while (cursor.hasNext()) {
			cursor.next();
			//int[] pos = cursor.getPosition();
			//System.out.print(pos[0] + ", " + pos[1] + ", " + pos[2] + "; ");
			intensity += cursor.getType().getRealFloat();
		}
		
		// 3 - Store the total intensity as a feature in this Spot
		spot.addFeature(FEATURE, intensity);
	}
	
	public static void main(String[] args) {

		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {200, 200, 40} );
		
		float[] center = new float[]  {20, 20, 20};
		Spot s1 = new Spot(center);

		float radius = 2;
		float[] calibration = new float[] {0.2f, 0.2f, 1};
		SphereCursor<UnsignedByteType> cursor = new SphereCursor<UnsignedByteType>(
				testImage, 
				s1.getCoordinates(), 
				radius, // µm
				calibration);
		int volume = 0;
		while(cursor.hasNext()) {
			volume++;
			cursor.fwd();
			cursor.getType().inc(); // to check we did not walk multiple times on a single pixel
		}
		cursor.close();
		
		BlobBrightness<UnsignedByteType> bb = new BlobBrightness<UnsignedByteType>(testImage, 2*radius, calibration);
		bb.process(s1);
		
		
		
	}
}