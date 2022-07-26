package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.closegaps.CloseGapsParams.Method;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.HessianDetector;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class CloseTrackGaps
{

	public static < T extends RealType< T > & NativeType< T > > void run( final Model model, final SelectionModel selectionModel, final ImgPlus< T > img, final CloseGapsParams params, final Logger logger )
	{
		final Set< DefaultWeightedEdge > edges;
		if ( params.selectionOnly )
			edges = selectionModel.getEdgeSelection();
		else
			edges = model.getTrackModel().edgeSet();

		run( edges, model, img, params, logger );
	}

	public static < T extends RealType< T > & NativeType< T > > void run( final Collection< DefaultWeightedEdge > edges, final Model model, final ImgPlus< T > img, final CloseGapsParams params, final Logger logger )
	{
		if ( params.method == Method.LOG_DETECTOR || params.method == Method.HESSIAN_DETECTOR )
		{
			if ( img == null )
			{
				logger.error( "Cannot use method '" + params.method + "' for spot detection, because the source image is not set.\n" );
				return;
			}
			final int nChannels = ( int ) ( img.dimensionIndex( Axes.CHANNEL ) < 0
					? 1
					: img.dimension( img.dimensionIndex( Axes.CHANNEL ) ) );
			if ( params.sourceChannel >= nChannels )
			{
				logger.error( "Cannot detect in channel " + ( params.sourceChannel + 1 ) + " as the source image has only " + nChannels + " channels.\n" );
				return;
			}
		}

		logger.log( "Interpolating gaps with method '" + params.method + "'.\n" );
		model.beginUpdate();
		final TrackModel trackModel = model.getTrackModel();

		final ArrayDeque< DefaultWeightedEdge > queue = new ArrayDeque<>( edges );
		try
		{
			/*
			 * Got through all edges, check if the frame distance between spots
			 * is larger than 1.
			 */
			while ( !queue.isEmpty() )
			{
				final DefaultWeightedEdge edge = queue.removeLast();
				final Spot currentSpot = trackModel.getEdgeSource( edge );
				final Spot nextSpot = trackModel.getEdgeTarget( edge );

				final int currentFrame = currentSpot.getFeature( Spot.FRAME ).intValue();
				final int nextFrame = nextSpot.getFeature( Spot.FRAME ).intValue();

				if ( Math.abs( nextFrame - currentFrame ) > 1 )
				{
					logger.log( "Processing edge from spot " + currentSpot.getName() + " at frame " + currentSpot.getFeature( Spot.FRAME ).intValue()
							+ " to spot " + nextSpot.getName() + " at frame " + nextSpot.getFeature( Spot.FRAME ).intValue() + ".\n" );

					final int presign = nextFrame > currentFrame ? 1 : -1;

					final double[] currentPosition = new double[ 3 ];
					final double[] nextPosition = new double[ 3 ];

					nextSpot.localize( nextPosition );
					currentSpot.localize( currentPosition );

					/*
					 * Create new spots in between; interpolate coordinates and
					 * some features.
					 */
					Spot formerSpot = currentSpot;
					for ( int f = currentFrame + presign; ( f < nextFrame && presign == 1 )
							|| ( f > nextFrame && presign == -1 ); f += presign )
					{
						final double weight = ( double ) ( nextFrame - f ) / ( nextFrame - currentFrame );

						final double[] position = new double[ 3 ];
						for ( int d = 0; d < currentSpot.numDimensions(); d++ )
							position[ d ] = weight * currentPosition[ d ] + ( 1.0 - weight ) * nextPosition[ d ];

						final RealPoint rp = new RealPoint( position );
						final Spot newSpot = new Spot( rp, 0, 0 );
						newSpot.putFeature( Spot.FRAME, Double.valueOf( f ) );

						// Set some properties of the new spot
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.RADIUS );
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.QUALITY );
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.POSITION_T );

						// Refine position.
						final Spot refinedSpot = refineSpotPosition( newSpot, img, params, logger );

						// Store into model.
						model.addSpotTo( refinedSpot, f );
						model.addEdge( formerSpot, refinedSpot, -1. );
						formerSpot = refinedSpot;
						
						logger.log( "Added new spot " + refinedSpot.getName() + " at frame "
								+ refinedSpot.getFeature( Spot.FRAME ).intValue() + ".\n" );
					}
					model.addEdge( formerSpot, nextSpot, 1.0 );
					model.removeEdge( currentSpot, nextSpot );
				}
			}
		}
		finally
		{
			model.endUpdate();
		}
		logger.log( "Finished.\n" );
	}

	private static < T extends RealType< T > & NativeType< T > > Spot refineSpotPosition( final Spot spot, final ImgPlus< T > img, final CloseGapsParams params, final Logger logger )
	{
		if ( params.method == Method.LINEAR_INTERPOLATION )
			return spot; // All done already.

		// Check time.
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		if ( frame < 0 )
		{
			logger.error( "Cannot search a spot at frame " + frame + ". Skipping.\n" );
			return spot;
		}
		final int nFrames = ( int ) ( img.dimensionIndex( Axes.TIME ) < 0
				? 1
				: img.dimension( img.dimensionIndex( Axes.TIME ) ) );
		if ( frame >= nFrames )
		{
			logger.error( "Cannot search a spot at frame " + frame + ", as source image has only " + nFrames + " frames. Skipping.\n" );
			return spot;
		}

		// Search region.
		final double searchRadius = Math.max( params.searchRadius, 1. );
		final RandomAccessibleInterval< T > block = DetectionUtils.prepareFrameImg( img, params.sourceChannel, frame );
		final Interval interval = regionAround( spot, searchRadius, img );

		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final double threshold = 0.;

		final SpotDetector< T > detector;
		switch ( params.method )
		{
		case LOG_DETECTOR:
		{
			final double radius = params.logAutoRadius
					? spot.getFeature( Spot.RADIUS ).doubleValue()
					: params.logRadius;
			detector = new LogDetector<>( block, interval, calibration, radius, threshold, true, false );
			break;
		}
		case HESSIAN_DETECTOR:
		{
			final double radiusXY = params.hessianAutoRadius
					? spot.getFeature( Spot.RADIUS ).doubleValue()
					: params.hessianRadiusXY;
			final double radiusZ = params.hessianAutoRadius
					? spot.getFeature( Spot.RADIUS ).doubleValue()
					: params.hessianRadiusZ;
			detector = new HessianDetector<>( Views.extendMirrorDouble( block ), interval, calibration, radiusXY, radiusZ, threshold, true, true );
			break;
		}
		default:
			throw new IllegalArgumentException( "Unknown gap-closing method: " + params.method );
		}
		( ( MultiThreaded ) detector ).setNumThreads( 1 );

		if ( !detector.checkInput() || !detector.process() )
		{
			logger.error( detector.getErrorMessage() );
			return spot;
		}
		final List< Spot > spots = detector.getResult();
		if ( spots.isEmpty() )
		{
			logger.log( "Could not detect a spot within search radius at frame " + frame + ". Skipping.\n " );
			return spot;
		}

		spots.sort( Spot.featureComparator( Spot.QUALITY ) );
//		System.out.println(); // DEBUG
//		spots.forEach( s -> System.out.println( " - " + s.getName() + " - Q = " + s.getFeature( Spot.QUALITY ) ) ); // DEBUG

		final Spot bestSpot = spots.get( spots.size() - 1 );
		bestSpot.putFeature( Spot.POSITION_T, spot.getFeature( Spot.POSITION_T ) );
		return bestSpot;
	}

	public static < T extends Type< T > > Interval regionAround( final Spot spot, final double searchRadius, final ImgPlus< T > img )
	{
		final double radius = spot.getFeature( Spot.RADIUS );

		final double[] location = new double[ 3 ];
		spot.localize( location );

		final double[] cal = TMUtils.getSpatialCalibration( img );
		final double dx = cal[ 0 ];
		final double dy = cal[ 1 ];
		final double dz = cal[ 2 ];

		final long x = Math.round( location[ 0 ] / dx );
		final long y = Math.round( location[ 1 ] / dy );
		final long z = Math.round( location[ 2 ] / dz );
		final long r = ( long ) Math.ceil( searchRadius * radius / dx );
		final long rz = ( long ) Math.abs( Math.ceil( searchRadius * radius / dz ) );

		final long width = img.dimension( 0 );
		final long height = img.dimension( 1 );
		final long x0 = Math.max( 0, x - r );
		final long y0 = Math.max( 0, y - r );
		final long x1 = Math.min( width - 1, x + r );
		final long y1 = Math.min( height - 1, y + r );

		final long[] min;
		final long[] max;
		if ( img.dimensionIndex( Axes.Z ) >= 0 )
		{
			// 3D
			final long depth = img.dimension( img.dimensionIndex( Axes.Z ) );
			final long z0 = Math.max( 0, z - rz );
			final long z1 = Math.min( depth - 1, z + rz );
			min = new long[] { x0, y0, z0 };
			max = new long[] { x1, y1, z1 };
		}
		else
		{
			// 2D
			min = new long[] { x0, y0 };
			max = new long[] { x1, y1 };
		}
		return new FinalInterval( min, max );
	}

	private static void interpolateFeature( final Spot targetSpot, final Spot spot1, final Spot spot2, final double weight, final String feature )
	{
		if ( targetSpot.getFeatures().containsKey( feature ) )
			targetSpot.getFeatures().remove( feature );

		targetSpot.getFeatures().put( feature,
				weight * spot1.getFeature( feature ) + ( 1.0 - weight ) * spot2.getFeature( feature ) );
	}

	private CloseTrackGaps()
	{}
}
