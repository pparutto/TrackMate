package fiji.plugin.trackmate.tracking.jaqaman.costfunction;

import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.util.IntHashMap;
import ij.ImagePlus;


public class ComponentDistancesTime
{
	public class CompTime
	{
		public float dist;
		public int winStart;
		public int winEnd;

		public CompTime( float dist, int winStart, int winEnd )
		{
			this.dist = dist;
			this.winStart = winStart;
			this.winEnd = winEnd;
		}

		public boolean inInterval( int win )
		{
			return win >= this.winStart && win <= this.winEnd;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash( dist, winEnd, winStart );
			return result;
		}

		@Override
		public boolean equals( Object obj )
		{
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			CompTime other = ( CompTime ) obj;
			return Float.floatToIntBits( dist ) == Float.floatToIntBits( other.dist ) && winEnd == other.winEnd
					&& winStart == other.winStart;
		}
	}

	public class SpotComp
	{
		public int winIdx;
		public int comp;

		public SpotComp( int winIdx, int comp )
		{
			this.winIdx = winIdx;
			this.comp = comp;
		}
	}

	public class Distances
	{
		public int[] comps;
		public int[][] pxSrc;
		public int[][][] pxDst;
		public int[][][][] dists;

		public Distances()
		{
			this.comps = null;
			this.pxSrc = null;
			this.pxDst = null;
			this.dists = null;
		}

		public void initComps( int n )
		{
			this.comps = new int[n];
			this.pxSrc = new int[n][];
			this.pxDst = new int[n][][];
			this.dists = new int[n][][][];
		}

		public void initSrcs( int cidx, int n )
		{
			this.pxSrc[cidx] = new int[n];
			this.pxDst[cidx] = new int[n][];
			this.dists[cidx] = new int[n][][];
		}

		public void initDsts( int cidx, int srcidx, int n )
		{
			this.pxDst[cidx][srcidx] = new int[n];
			this.dists[cidx][srcidx] = new int[n][];
		}

		public int getCompIdx( int comp )
		{
			return Arrays.binarySearch( this.comps, comp );
		}

		public int getCompSrcIdx( int cIdx, int px )
		{
			return Arrays.binarySearch( this.pxSrc[cIdx], px );
		}

		public int getCompDstIdx( int cIdx, int pxSrcIdx, int pxDst )
		{
			return Arrays.binarySearch( this.pxDst[cIdx][pxSrcIdx], pxDst );
		}

		public int getDist( int cIdx, int pxSrcIdx, int pxDstIdx, int winIdx )
		{
			if ( cIdx >= this.dists.length || pxSrcIdx >= this.dists[cIdx].length || pxDstIdx >= this.dists[cIdx][pxSrcIdx].length || winIdx >= this.dists[cIdx][pxSrcIdx][pxDstIdx].length )
				assert(false);
			return this.dists[cIdx][pxSrcIdx][pxDstIdx][winIdx];
		}
	}

	public static class ArrCmp implements Comparator<Integer>
	{
		private final int[] arr;

		public ArrCmp ( final int[] arr )
		{
			this.arr = arr;
		}

		@Override
		public int compare( Integer i, Integer j )
		{
			return Integer.compare( this.arr[i], this.arr[j] );
		}
	}

	public static <T> void sort_simultaneous( int[] arr1, T[] arr2 )
	{
		Integer[] idxs = new Integer[arr1.length];
		for ( int u = 0; u < arr1.length; ++u )
			idxs[u] = u;
		Arrays.sort( idxs, new ArrCmp( arr1 ) );
		int[] tmp1 = new int[arr1.length];
		ArrayList<T> tmp2 = new ArrayList<> ( arr1.length );
		for ( int u = 0; u < arr1.length; ++u )
		{
			tmp1[u] = arr1[idxs[u]];
			tmp2.add( arr2[idxs[u]] );
		}
		arr1 = tmp1;
		tmp2.toArray( arr2 );
	}

	protected HashMap<CompTime, Integer> elts_f;
	protected ArrayList<CompTime> elts_r;
	Distances dists;
	protected ArrayList<Integer> win_starts;
	protected boolean is2D;
	protected int wdur;
	protected int w;
	protected int h;
	protected double pxsize;
	protected final ImagePlus compImg;
	protected IntHashMap< ArrayList<SpotComp > > spotComps;

	protected static int[][] nh_idxs = { { -1, -1 }, { -1, 0 }, { -1, 1 },
										 { 0,  -1 },            {  0, 1 },
										 { 1,  -1 }, {  1, 0 }, {  1, 1 } };

	public int getWinIdx( int cIdx, int pxSrcIdx, int pxDstIdx, int win )
	{
		int[] eltIdxs = this.dists.dists[cIdx][pxSrcIdx][pxDstIdx];
		for ( int i = 0; i < eltIdxs.length; ++i )
			if ( this.elts_r.get( eltIdxs[i] ).inInterval( win ) )
				return i;
		return -1;
	}

	protected static int byte_to_int( final byte[] bs )
	{
		int value = 0;
		for ( final byte b: bs )
			value = ( value << 8 ) + ( b & 0xFF );
		return value;
	}

	public static final float byte_to_float( final byte[] bs )
	{
		return ByteBuffer.wrap( bs ).getFloat();
	}

	public static double sq_dist_to_px( double[] pos, int[] p, double pxsize )
	{
		return Math.pow( pos[0] - ( p[0] * pxsize + pxsize / 2 ), 2 ) +
			   Math.pow( pos[1] - ( p[1] * pxsize + pxsize / 2 ), 2 );
	}

	protected float[] read_dists( FileInputStream is ) throws IOException
	{
		byte[] buff_i = new byte[3];
		byte[] buff_f = new byte[4];

		is.read( buff_i );
		int Nelt = byte_to_int( buff_i );
		float[] res = new float[Nelt];
		for ( int i = 0; i < Nelt; ++i )
		{
			is.read( buff_f );
			res[i] = byte_to_float( buff_f );
		}

		return res;
	}

	protected void read_binary_spts( final String fname ) throws FileNotFoundException
	{
		FileInputStream is = new FileInputStream( fname );
		byte[] buff_i = new byte[3];

		float[] all_dists = null;
		this.win_starts = new ArrayList<> ();

		try
		{
			is.read( buff_i );
			int version = byte_to_int( buff_i );
			System.out.println(String.format("Version=%d", version));

			if ( version == 1 ) //this is 2D+t data
			{
				byte[] buff_f = new byte[4];

				this.is2D = false;

				is.read( buff_i );
				int Wdur = byte_to_int( buff_i );
				this.wdur = Wdur;

				is.read( buff_f );
				float Wover = byte_to_float( buff_f );

				is.read( buff_i );
				int Wmax = byte_to_int( buff_i );

				System.out.println( String.format( "%d %f %d", Wdur, Wover, Wmax ) );

				int dw = ( int ) Math.floor( Wdur * ( 1 - Wover ) );
				if ( Wover == 0 )
					dw = Wdur;

				this.win_starts.add( 0 );
				for ( int i = 1; i < Wmax; ++i )
					this.win_starts.add( ( dw + 1 ) * i );

				String s = "";
				for ( int i = 0; i < this.win_starts.size() ; ++i )
					s = s.concat( String.format( " [%d, %d]", this.win_starts.get( i ), this.win_starts.get( i ) + Wdur -1 ) );
				System.out.println( String.format( "Time windows (%d): %s", this.win_starts.size(), s ) );

				all_dists = read_dists( is );

				is.read( buff_i );
				int Ncomps = byte_to_int( buff_i );
				this.dists.initComps( Ncomps );
				for ( int i = 0; i < Ncomps; ++i )
				{
					is.read( buff_i );
					int comp = byte_to_int( buff_i );
					this.dists.comps[i] = comp;

					is.read( buff_i );
					int Npxs1 = byte_to_int( buff_i );
					this.dists.initSrcs( i, Npxs1 );
					for ( int j = 0; j < Npxs1; ++j )
					{
						is.read( buff_i );
						int px1 = byte_to_int( buff_i );
						this.dists.pxSrc[i][j] = px1;

						is.read( buff_i );
						int Npxs2 = byte_to_int( buff_i );
						this.dists.initDsts( i, j, Npxs2 );
						for ( int k = 0; k < Npxs2; ++k )
						{
							is.read( buff_i );
							int px2 = byte_to_int( buff_i );
							this.dists.pxDst[i][j][k] = px2;

							is.read( buff_i );
							int Nwins = byte_to_int( buff_i );
							this.dists.dists[i][j][k] = new int[Nwins];
							for ( int l = 0; l < Nwins; ++l )
							{
								is.read( buff_i );
								int didx = byte_to_int( buff_i );

								is.read( buff_i );
								int wstart = byte_to_int( buff_i );

								is.read( buff_i );
								int wend = byte_to_int( buff_i );

								CompTime ct = new CompTime( all_dists[didx], wstart, wend );
								if ( ! this.elts_f.containsKey( ct ) )
								{
									int N = this.elts_f.size();
									this.elts_f.put( ct, N );
									this.elts_r.add( ct );
								}

								this.dists.dists[i][j][k][l] = this.elts_f.get( ct );
							}
						}
					}
				}
			}
			else if ( version == 2 ) //this is 2D data
			{
				this.is2D = true;
				all_dists = read_dists( is );

				is.read( buff_i );
				int Ncomps = byte_to_int( buff_i );
				this.dists.initComps( Ncomps );
				for ( int i = 0; i < Ncomps; ++i )
				{
					is.read( buff_i );
					int comp = byte_to_int( buff_i );
					this.dists.comps[i] = comp;

					is.read( buff_i );
					int Npxs1 = byte_to_int( buff_i );
					this.dists.initSrcs( i, Npxs1 );
					for ( int j = 0; j < Npxs1; ++j )
					{
						is.read( buff_i );
						int px1 = byte_to_int( buff_i );
						this.dists.pxSrc[i][j] = px1;

						is.read( buff_i );
						int Npxs2 = byte_to_int( buff_i );
						this.dists.initDsts( i, j, Npxs2 );
						for ( int k = 0; k < Npxs2; ++k )
						{
							is.read( buff_i );
							int px2 = byte_to_int( buff_i );
							this.dists.pxDst[i][j][k] = px2;

							is.read( buff_i );
							int didx = byte_to_int( buff_i );

							CompTime ct = new CompTime( all_dists[didx], 0, 0 );
							if ( ! this.elts_f.containsKey( ct ) )
							{
								int N = this.elts_f.size();
								this.elts_f.put( ct, N );
								this.elts_r.add( ct );
							}

							this.dists.dists[i][j][k] = new int[] { this.elts_f.get( ct ) };
						}
					}
				}
			}
			else
				assert( false );

			assert( is.read(buff_i) == -1 );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public ComponentDistancesTime( final String fname, final ImagePlus compImg, int w, int h, double pxsize ) throws FileNotFoundException
	{
		this.dists = new Distances ();
		this.is2D = false;
		this.w = w;
		this.h = h;
		this.pxsize = pxsize;
		this.compImg = compImg;
		this.spotComps = new IntHashMap<> ();

		this.elts_f = new HashMap<CompTime, Integer> ();
		this.elts_r = new ArrayList<> ();

		this.read_binary_spts( fname );
	}

	public ArrayList<Integer> getWindowIdxs ( int frame )
	{
		if ( this.is2D )
		{
			ArrayList<Integer> res = new ArrayList<Integer> ();
			res.add( 0 );
			return res;
		}

		ArrayList<Integer> res = new ArrayList<> ();
		for ( int i = 0; i < this.win_starts.size(); ++i )
		{
			if ( frame >= this.win_starts.get( i ) &&
				 frame < this.win_starts.get( i ) + this.wdur )
				res.add( Integer.valueOf( i ) );
		}
		return res;
	}

	public void preprocess_spots( final SpotCollection spots )
	{
		for ( final Spot s: spots.iterable( true ) )
		{
			int frame = s.getFeature( "FRAME" ).intValue();
			int px = (int) Math.ceil( s.getFeature( "POSITION_X" ) / this.pxsize );
			int py = (int) Math.ceil( s.getFeature( "POSITION_Y" ) / this.pxsize );

			ArrayList< SpotComp > comps = new ArrayList<>();
			if ( this.is2D )
				assert( this.getWindowIdxs( frame ).size() == 1 );
			for ( int i: this.getWindowIdxs( frame ) )
				comps.add( new SpotComp( i, this.getComponent( i, new int[] {px, py} ) ) );

			this.spotComps.put( s.ID(), comps );
		}
		System.out.println( String.format( "comp size = %d", spotComps.size() ) );
	}

	public void clear_spots_comps()
	{
		System.out.println( "Clearing spots components cache" );
		this.spotComps.clear();
		System.out.println( this.spotComps.size() );
	}

	public float dist( int px1, int px2, int comp, int win )
	{
		if ( px1 > px2 )
		{
			int tmp = px1;
			px1 = px2;
			px2 = tmp;
		}

		int i = this.dists.getCompIdx( comp );
		if ( i < 0 || i >= this.dists.dists.length )
			return Float.MAX_VALUE;
		int j = this.dists.getCompSrcIdx( i,  px1 );
		if ( j < 0 || j >= this.dists.dists[i].length )
			return Float.MAX_VALUE;
		int k = this.dists.getCompDstIdx( i, j, px2 );
		if ( k < 0 || k >= this.dists.dists[i][j].length )
			return Float.MAX_VALUE;
		int l = this.getWinIdx( i, j, k, win );
		if ( l < 0 || l >= this.dists.dists[i][j][k].length )
			return Float.MAX_VALUE;

		return this.elts_r.get( this.dists.dists[i][j][k][l] ).dist;
	}

	public int w()
	{
		return this.w;
	}

	public int h()
	{
		return this.h;
	}

	public double pxsize()
	{
		return this.pxsize;
	}

	public Distances dists()
	{
		return this.dists;
	}

	public ArrayList<Integer> win_starts()
	{
		return this.win_starts;
	}

	public IntHashMap< ArrayList<SpotComp > > spotComps()
	{
		return this.spotComps;
	}

	public int getComponent( int winIdx, int[] px )
	{
		if ( this.compImg.getStackSize() > 1 )
			return this.compImg.getStack().getProcessor( winIdx + 1 ).getPixel( px[0], px[1] );
		else
			return this.compImg.getProcessor().getPixel( px[0], px[1] );
	}

	public int px1D( int[] p )
	{
		return p[1] * this.w + p[0];
	}
}
