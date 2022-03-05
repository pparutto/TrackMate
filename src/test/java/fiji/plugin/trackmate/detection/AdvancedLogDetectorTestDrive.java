package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class AdvancedLogDetectorTestDrive
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();

//		final ImagePlus imp = IJ.openImage( "samples/Celegans-5pc-17timepoints-t13.tif" );
		final ImagePlus imp = IJ.openImage( "samples/TSabateCell.tif" );
		imp.show();

		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final double radiusXY = 0.6 / 2.; // um;
		final double radiusZ = 1.6 / 2.;
		final double thresholdQ = 20000.;
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
		
		final List< Spot > spots = filterSpotsRoi( imp.getRoi(), ald.getResult(), calibration );
		spots.sort( Spot.featureComparator( Spot.QUALITY ) );
		
		spots.forEach( s -> System.out.println( String.format( " - %3d: Q = %7.1f, BR = %7.3f, L = %s",
				s.ID(),
				s.getFeature( Spot.QUALITY ),
				s.getFeature( AdvancedLogDetector.BLOB_RATIO ),
				Util.printCoordinates( s ) ) ) );

		final SpotCollection sc = new SpotCollection();
		sc.put( 0, spots );
		sc.setVisible( true );
		final Model model = new Model();
		model.setPhysicalUnits( imp.getCalibration().getUnit(), imp.getCalibration().getTimeUnit() );
		model.setSpots( sc, false );
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
//		final String feature = Spot.QUALITY ;
		final String feature = AdvancedLogDetector.BLOB_RATIO;
		ds.setSpotColorBy( TrackMateObject.SPOTS, feature );
		final double[] mm = FeatureUtils.autoMinMax( model, TrackMateObject.SPOTS, feature );
		ds.setSpotMinMax( mm[ 0 ], mm[ 1 ] );
		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, selectionModel, imp, ds );
		displayer.render();
		displayer.refresh();
	}

	private static final List< Spot > filterSpotsRoi( final Roi roi, final List< Spot > spots, final double[] calibration )
	{
		List< Spot > prunedSpots;
		if ( roi != null )
		{
			prunedSpots = new ArrayList<>();
			for ( final Spot spot : spots )
			{
				if ( roi.contains(
						( int ) Math.round( spot.getFeature( Spot.POSITION_X ) / calibration[ 0 ] ),
						( int ) Math.round( spot.getFeature( Spot.POSITION_Y ) / calibration[ 1 ] ) ) )
					prunedSpots.add( spot );
			}
			return prunedSpots;
		}
		else
		{
			return spots;
		}
	}
}
