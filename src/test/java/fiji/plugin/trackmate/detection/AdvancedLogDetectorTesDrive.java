package fiji.plugin.trackmate.detection;

import java.util.List;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class AdvancedLogDetectorTesDrive
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();

		final ImagePlus imp = IJ.openImage( "samples/Celegans-5pc-17timepoints-t13.tif" );
		imp.show();

		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final double[] calibration = new double[] { 0.198, 0.198, 1. };
		final double radiusXY = 6.5 / 2.; // um;
		final double radiusZ = 11. / 2.;
		final double thresholdQ = 0.1;
		final double thresholdE = 0.;

		final AdvancedLogDetector< T > ald = new AdvancedLogDetector<>(
				img,
				img,
				calibration,
				radiusXY,
				radiusZ,
				thresholdQ,
				thresholdE,
				true,
				false );
		if ( !ald.checkInput() || !ald.process() )
		{
			System.err.println( ald.getErrorMessage() );
			return;
		}
		System.out.println( "Done in " + ald.getProcessingTime() / 1000. + " s." );
		
		final List< Spot > spots = ald.getResult();
		spots.sort( Spot.featureComparator( Spot.QUALITY ) );
		
		spots.forEach( s -> System.out.println( String.format( " - %3d: Q = %7.1f, L = %s",
				s.ID(), s.getFeature( Spot.QUALITY ), Util.printCoordinates( s ) ) ) );
	}
}
