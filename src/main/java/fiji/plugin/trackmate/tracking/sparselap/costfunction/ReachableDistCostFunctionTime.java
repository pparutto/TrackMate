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
		int sFrame = source.getFeature( "FRAME" ).intValue();
		int tFrame = target.getFeature( "FRAME" ).intValue();
		int[] src_px2D = { source.getFeature( "PX_X" ).intValue(), source.getFeature( "PX_Y" ).intValue() };
		int[] dst_px2D = { target.getFeature( "PX_X" ).intValue(), target.getFeature( "PX_Y" ).intValue() };

		double d1 = source.squareDistanceTo( target );

		final double base_dist = d1;

		ArrayList<Integer> winsInter = this.intersectWins( this.cdists.getWindowIdxs( sFrame ),
														   this.cdists.getWindowIdxs( tFrame ) );

		if ( winsInter.isEmpty() )
			return Double.MAX_VALUE;


		double[] poss = { source.getFeature( "POSITION_X" ), source.getFeature( "POSITION_Y" ) };
		double[] post = { target.getFeature( "POSITION_X" ), target.getFeature( "POSITION_Y" ) };

		ArrayList<int[]> src_pxs = ReachableDistCostFunctionTime.neighbors(
				src_px2D, this.cdists.w(), this.cdists.h() );

		ArrayList<int[]> dst_pxs = ReachableDistCostFunctionTime.neighbors(
				dst_px2D, this.cdists.w(), this.cdists.h() );

		double best_dist = Float.MAX_VALUE;
		for ( int curWin: winsInter )
		{
			for ( int[] src_px: src_pxs )
			{
				int sComp = this.cdists.getComponent( curWin, src_px );
				int pxs = cdists.px1D( src_px );
				for ( int[] dst_px: dst_pxs )
				{
					int tComp = this.cdists.getComponent( curWin, dst_px );
					if ( sComp != tComp )
						continue;

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
								   this.cdists.dist( px1, px2, sComp, curWin ) +
								   Math.sqrt( ComponentDistancesTime.sq_dist_to_px( post, dst_px, cdists.pxsize() ) ), 2 );

					final double d2 = ( d1 == 0 ) ? Double.MIN_NORMAL : d1;

					if ( d2 < best_dist )
						best_dist = d2;
				}
			}
		}

		assert( best_dist >= base_dist );
		return best_dist;
	}

	public double linkingCostLog( final Spot source, final Spot target )
	{
		int sFrame = source.getFeature( "FRAME" ).intValue();
		int tFrame = target.getFeature( "FRAME" ).intValue();
		int[] src_px2D = { source.getFeature( "PX_X" ).intValue(), source.getFeature( "PX_Y" ).intValue() };
		int[] dst_px2D = { target.getFeature( "PX_X" ).intValue(), target.getFeature( "PX_Y" ).intValue() };
		int sComp = this.cdists.getComponent( sFrame, src_px2D );
		int tComp = this.cdists.getComponent( tFrame, dst_px2D );

		System.out.println(String.format("source: frame=%d pos=(%d %d) comp=%d", sFrame, src_px2D[0], src_px2D[1], sComp));
		System.out.println(String.format("target: frame=%d pos=(%d %d) comp=%d", tFrame, dst_px2D[0], dst_px2D[1], tComp));

		if ( sComp != tComp )
		{
			System.out.println("  sComp != tComp");
			return Double.POSITIVE_INFINITY;
		}

		double d1 = source.squareDistanceTo( target );
		System.out.println(String.format("  edist=%g", d1));

		final double base_dist = d1;

		if ( d1 > 25 )
			return d1;

		ArrayList<Integer> winsInter = this.intersectWins( this.cdists.getWindowIdxs( sFrame ),
														   this.cdists.getWindowIdxs( tFrame ) );

		if ( winsInter.isEmpty() )
		{
			System.out.println("  No intersecting window");
			return Double.POSITIVE_INFINITY;
		}


		double[] poss = { source.getFeature( "POSITION_X" ), source.getFeature( "POSITION_Y" ) };
		double[] post = { target.getFeature( "POSITION_X" ), target.getFeature( "POSITION_Y" ) };

		ArrayList<int[]> src_pxs = ReachableDistCostFunctionTime.neighbors(
				src_px2D, this.cdists.w(), this.cdists.h() );

		ArrayList<int[]> dst_pxs = ReachableDistCostFunctionTime.neighbors(
				dst_px2D, this.cdists.w(), this.cdists.h() );

		double best_dist = Float.MAX_VALUE;
		for ( int curWin: winsInter )
		{
			System.out.println(String.format("  curWin=%d", curWin));
			for ( int[] src_px: src_pxs )
			{
				int pxs = cdists.px1D( src_px );

				System.out.println(String.format("    src=(%d %d) 1D=%d", src_px[0], src_px[1], pxs));

				for ( int[] dst_px: dst_pxs )
				{
					int pxd = cdists.px1D( dst_px );

					System.out.println(String.format("    dst=(%d %d) 1D=%d", dst_px[0], dst_px[1], pxd));

					if ( pxs == pxd )
					{
						d1 = source.squareDistanceTo( target );
						final double d2 = ( d1 == 0 ) ? Double.MIN_NORMAL : d1;

						System.out.println(String.format("    Same px using edist=%g", d2));

						if ( d2 < best_dist )
							best_dist = d2;
						continue;
					}

					int pxs3D = this.cdists.map_1D_to_3D( pxs, curWin );
					int pxd3D = this.cdists.map_1D_to_3D( pxd, curWin );

					System.out.println(String.format("    pxs3D: src=%d dst=%d", pxs3D, pxd3D));

					if ( pxs3D == -1 || pxd3D == -1 )
						continue;

					int px1 = Math.min( pxs3D, pxd3D );
					int px2 = Math.max( pxs3D, pxd3D );

					System.out.println(String.format("    ordering: px1=%d px2=%d", px1, px2));

					d1 = Math.pow( Math.sqrt( ComponentDistancesTime.sq_dist_to_px( poss, src_px, cdists.pxsize() ) ) +
								   this.cdists.dist( px1, px2, sComp, curWin ) +
								   Math.sqrt( ComponentDistancesTime.sq_dist_to_px( post, dst_px, cdists.pxsize() ) ), 2 );

					System.out.println(String.format("    edist_src=%g cdist=%g edist_dst=%g sum_sq=%g",
							Math.sqrt( ComponentDistancesTime.sq_dist_to_px( poss, src_px, cdists.pxsize() ) ),
							this.cdists.dist( px1, px2, sComp, curWin ),
							Math.sqrt( ComponentDistancesTime.sq_dist_to_px( post, dst_px, cdists.pxsize() ) ),
							d1));

					final double d2 = ( d1 == 0 ) ? Double.MIN_NORMAL : d1;

					if ( d2 < best_dist )
						best_dist = d2;
				}
			}
		}

		System.out.println(String.format("  bestDist=%g", best_dist));

		assert( best_dist >= base_dist );
		return best_dist;
	}
}
