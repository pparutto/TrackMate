package fiji.plugin.trackmate.tracking.sparselap.costfunction;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.GraphDistance;
import ij.ImagePlus;

public class GraphDistCostFunction implements CostFunction< Spot, Spot >
{
	final ImagePlus maskImg;

	public GraphDistCostFunction (final ImagePlus maskImg)
	{
		this.maskImg = maskImg;
	}

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		final double d2 = GraphDistance.dijkstra( maskImg,
										new int[] { ( int ) Math.floor( source.getFeature( Spot.POSITION_X ) ),
					 							    ( int ) Math.floor( source.getFeature( Spot.POSITION_Y ) ) },
										new int[] { ( int ) Math.floor( target.getFeature( Spot.POSITION_X ) ),
					 							    ( int ) Math.floor( target.getFeature( Spot.POSITION_Y ) ) } );
		return ( d2 == 0 ) ? Double.MIN_NORMAL : d2;
	}

}
