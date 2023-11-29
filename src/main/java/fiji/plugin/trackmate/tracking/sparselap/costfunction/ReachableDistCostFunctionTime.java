package fiji.plugin.trackmate.tracking.sparselap.costfunction;

import java.util.ArrayList;
import fiji.plugin.trackmate.Spot;

public class ReachableDistCostFunctionTime implements CostFunction< Spot, Spot >
{
	protected final ComponentDistancesTime cdists;

	//CHECK W and H here
	protected static ArrayList<int[]> neighbors( int[] p, int w, int h )
	{
		int[][] nhs = {{-1, 1}, {0, 1}, {1, 1}, {-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {1, -1}};
		ArrayList<int[]> res = new ArrayList<> ();
		for ( int[] nh: nhs )
		{
			int[] pp = {p[0] + nh[0], p[1] + nh[1]};
			if ( pp[0] < 0 || pp[1] < 0 || pp[0] >= w || pp[1] >= h )
				continue;
			res.add( pp );
		}
		return res;
	}

	public ReachableDistCostFunctionTime( final ComponentDistancesTime cdists )
	{
		this.cdists = cdists;
	}

	protected ArrayList<Integer> intersectWins( ArrayList<Integer> wins1, ArrayList<Integer> wins2 )
	{
		ArrayList<Integer> res = new ArrayList<> ();
		for ( int i: wins1 )
			if ( wins2.contains( i ) )
				res.add( i );
		return res;
	}

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		double d1 = source.squareDistanceTo( target );

		final double base_dist = d1;

		if ( d1 > 25 )
			return d1;

		int frame = source.getFeature( "FRAME" ).intValue();

		ArrayList<Integer> winsInter = this.intersectWins( this.cdists.getWindowIdxs( source.getFeature( "FRAME" ).intValue() ),
														   this.cdists.getWindowIdxs( target.getFeature( "FRAME" ).intValue() ) );

		if ( winsInter.isEmpty() )
			return Double.POSITIVE_INFINITY;

		int[] src_px2D = { source.getFeature( "PX_X" ).intValue(), source.getFeature( "PX_Y" ).intValue() };
		int[] dst_px2D = { target.getFeature( "PX_X" ).intValue(), target.getFeature( "PX_Y" ).intValue() };

		double[] poss = { source.getFeature( "POSITION_X" ), source.getFeature( "POSITION_Y" ) };
		double[] post = { target.getFeature( "POSITION_X" ), target.getFeature( "POSITION_Y" ) };

		ArrayList<int[]> src_pxs = ReachableDistCostFunctionTime.neighbors(
				src_px2D, this.cdists.w(), this.cdists.h() );

		ArrayList<int[]> dst_pxs = ReachableDistCostFunctionTime.neighbors(
				dst_px2D, this.cdists.w(), this.cdists.h() );

		ArrayList<Double[]> nh_dists = new ArrayList<> ();
		double best_dist = Float.MAX_VALUE;
		for ( int curWin: winsInter )
		{
			for ( int[] src_px: src_pxs )
			{
				int pxs = cdists.px1D( src_px );
				for ( int[] dst_px: dst_pxs )
				{
					//System.out.println(String.format("%d %d %d %d %d", curWin, src_px[0], src_px[1], dst_px[0], dst_px[1]));
					int pxd = cdists.px1D( dst_px );
					if ( pxs == pxd )
					{
						d1 = source.squareDistanceTo( target );
						final double d2 = ( d1 == 0 ) ? Double.MIN_NORMAL : d1;
						if ( d2 < best_dist )
							best_dist = d2;
						continue;
					}

					int pxs3D = this.cdists.map_1D_to_3D( pxs, curWin );
					int pxd3D = this.cdists.map_1D_to_3D( pxd, curWin );

					if ( pxs3D == -1 || pxd3D == -1 )
						continue;

					int px1 = Math.min( pxs3D, pxd3D );
					int px2 = Math.max( pxs3D, pxd3D );

					d1 = Math.pow( Math.sqrt( ComponentDistancesTime.sq_dist_to_px( poss, src_px, cdists.pxsize() ) ) +
								   this.cdists.dist( px1, px2 ) +
								   Math.sqrt( ComponentDistancesTime.sq_dist_to_px( post, dst_px, cdists.pxsize() ) ), 2 );

					final double d2 = ( d1 == 0 ) ? Double.MIN_NORMAL : d1;
					nh_dists.add( new Double[] { (double) src_px[0], (double) src_px[1],
												 (double) dst_px[0], (double) dst_px[1], (double) frame, (double) curWin, d2 } );

					if ( d2 < best_dist )
						best_dist = d2;
				}
			}
		}

		assert( best_dist >= base_dist );
		return best_dist;
	}
}
