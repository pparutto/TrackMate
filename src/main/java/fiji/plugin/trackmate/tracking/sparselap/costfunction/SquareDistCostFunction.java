package fiji.plugin.trackmate.tracking.sparselap.costfunction;

import fiji.plugin.trackmate.Spot;

/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 */
public class SquareDistCostFunction implements CostFunction< Spot, Spot >
{

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		return source.squareDistanceTo( target );
	}

}
