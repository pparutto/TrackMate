package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import net.imglib2.RealPoint;

public class CloseTrackGaps
{

	public static void run( final Model model, final SelectionModel selectionModel, final CloseGapsParams params, final Logger logger )
	{
		final Set< DefaultWeightedEdge > edges;
		if ( params.selectionOnly )
			edges = selectionModel.getEdgeSelection();
		else
			edges = model.getTrackModel().edgeSet();

		run( edges, model, params, logger );
	}

	public static void run( final Collection< DefaultWeightedEdge > edges, final Model model, final CloseGapsParams params, final Logger logger )
	{
		logger.log( "Interpolating gaps.\n" );
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
					int nspots = 0;
					for ( int f = currentFrame + presign; ( f < nextFrame && presign == 1 )
							|| ( f > nextFrame && presign == -1 ); f += presign )
					{
						final double weight = ( double ) ( nextFrame - f ) / ( nextFrame - currentFrame );

						final double[] position = new double[ 3 ];
						for ( int d = 0; d < currentSpot.numDimensions(); d++ )
							position[ d ] = weight * currentPosition[ d ] + ( 1.0 - weight ) * nextPosition[ d ];

						final RealPoint rp = new RealPoint( position );
						final Spot newSpot = new Spot( rp, 0, 0 );

						// Set some properties of the new spot
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.RADIUS );
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.QUALITY );
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.POSITION_T );

						// Refine position.
						final double searchRadius = Math.max( params.searchRadius, 1. );
						switch ( params.method )
						{
						case LINEAR_INTERPOLATION:
							// Done already, nothing to do.
							break;
						case LOG_DETECTOR:
						{
							final double radius = params.logAutoRadius
									? newSpot.getFeature( Spot.RADIUS ).doubleValue()
									: params.logRadius;
							searchSpotLoG( newSpot, radius, searchRadius );
							break;
						}
						case HESSIAN_DETECTOR:
						{
							final double radiusXY = params.hessianAutoRadius
									? newSpot.getFeature( Spot.RADIUS ).doubleValue()
									: params.hessianRadiusXY;
							final double radiusZ = params.hessianAutoRadius
									? newSpot.getFeature( Spot.RADIUS ).doubleValue()
									: params.hessianRadiusZ;
							searchSpotHessian( newSpot, radiusXY, radiusZ, searchRadius );
							break;
						}
						default:
							throw new IllegalArgumentException( "Unknown gapc-losing method: " + params.method );
						}

						// Store into model.
						model.addSpotTo( newSpot, f );
						model.addEdge( formerSpot, newSpot, 1.0 );
						formerSpot = newSpot;
						nspots++;
					}
					model.addEdge( formerSpot, nextSpot, 1.0 );
					model.removeEdge( currentSpot, nextSpot );
					logger.log( "Added " + nspots + " new spots between spots " + currentSpot + " and " + nextSpot + ".\n" );

				}
			}
		}
		finally
		{
			model.endUpdate();
		}
		logger.log( "Finished.\n" );
	}

	private static void searchSpotLoG( final Spot spot, final double radius, final double searchRadius )
	{
		// TODO Auto-generated method stub

	}

	private static void searchSpotHessian( final Spot spot, final double radiusXY, final double radiusZ, final double searchRadius )
	{
		// TODO Auto-generated method stub

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
