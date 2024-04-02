package fiji.plugin.trackmate.tracking.sparselap.costfunction;

import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.util.IntHashMap;
import ij.ImagePlus;


public class ComponentDistancesTime
{
	public class PixelGroupTime
	{
		public int px3D;
		public int win_start;
		public int win_end;

		public PixelGroupTime( int px3D, int w_start, int w_end )
		{
			this.px3D = px3D;
			this.win_start = w_start;
			this.win_end = w_end;
		}
	}

	public class CompTime
	{
		public float dist;
		public int frameStart;
		public int frameEnd;

		public CompTime( float dist, int frameStart, int frameEnd )
		{
			this.dist = dist;
			this.frameStart = frameStart;
			this.frameEnd = frameEnd;
		}

		public boolean inInterval( int frame )
		{
			return frame >= this.frameStart && frame <= this.frameEnd;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(dist, frameEnd, frameStart);
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CompTime other = (CompTime) obj;
			return Float.floatToIntBits(dist) == Float.floatToIntBits(other.dist) && frameEnd == other.frameEnd
					&& frameStart == other.frameStart;
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

	protected HashMap<CompTime, Integer> elts_f;
	protected HashMap<Integer, CompTime> elts_r;
	protected IntHashMap< IntHashMap< IntHashMap< ArrayList< Integer > > > > dists;
	protected IntHashMap< ArrayList< PixelGroupTime > > px1D_to_3D;
	protected ArrayList<Integer> win_cents;
	protected int wdur;
	protected int w;
	protected int h;
	protected double pxsize;
	protected final ImagePlus compImg;
	protected IntHashMap< ArrayList<SpotComp > > spotComps;

	protected static int[][] nh_idxs = { { -1, -1 }, { -1, 0 }, { -1, 1 },
										 { 0,  -1 },            {  0, 1 },
										 { 1,  -1 }, {  1, 0 }, {  1, 1 } };

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

	public int map_1D_to_3D( int px1D, int win )
	{
		//Change to use win later
		if ( !this.px1D_to_3D.containsKey( px1D ) )
			return -1;

		for ( PixelGroupTime ct: this.px1D_to_3D.get( px1D ) )
			if ( ct.win_start >= win && ct.win_end <= win )
				return ct.px3D;

		return -1;
	}

	protected void read_binary_spts( final String fname, double dt ) throws FileNotFoundException
	{
		FileInputStream is = new FileInputStream( fname );
		byte[] buff_i = new byte[3];
		byte[] buff_f = new byte[4];

		float[] all_dists = null;

		this.win_cents = new ArrayList<> ();

		try
		{
			is.read( buff_i );
			int Wdur = byte_to_int( buff_i );
			this.wdur = Wdur;

			is.read( buff_f );
			float Wover = byte_to_float( buff_f );

			is.read( buff_i );
			int Wmax = byte_to_int( buff_i );

			//System.out.println(String.format("%d %f %d", Wdur, Wover, Wmax));

			int dw = ( int ) Math.floor( Wdur * Wover );
			if ( Wover == 0 )
				dw = ( int ) Math.floor( ( ( float ) Wdur ) / 2.0 );

			for ( int i = 1; i <= Wmax; ++i )
				this.win_cents.add( i * dw );

			String s = "";
			for ( int i = 0; i < this.win_cents.size() ; ++i )
				s = s.concat( String.format( " %d [%d, %d]", this.win_cents.get ( i ), this.win_cents.get( i ) - dw, this.win_cents.get( i ) + dw ) );
			System.out.println( String.format( "Time windows (%d): %s", this.win_cents.size(), s ) );

			//System.out.println(String.format("%d %g %d %d %d", Wdur, Wover, Wmax, dw, this.win_cents.size()));

			is.read( buff_i );
			int Nelt = byte_to_int( buff_i );
			this.px1D_to_3D = new IntHashMap< ArrayList<PixelGroupTime> > ();
			for ( int i = 0; i < Nelt; ++i )
			{
				is.read( buff_i );
				int px1D = byte_to_int( buff_i );
				is.read( buff_i );
				int wStart = byte_to_int( buff_i );
				is.read( buff_i );
				int wEnd = byte_to_int( buff_i );
				if ( !this.px1D_to_3D.containsKey( px1D ) )
					this.px1D_to_3D.put( px1D, new ArrayList<PixelGroupTime> () );
				this.px1D_to_3D.get( px1D ).add( new PixelGroupTime( i, wStart, wEnd ) );
			}

			is.read( buff_i );
			Nelt = byte_to_int( buff_i );
			all_dists = new float[Nelt];
			for ( int i = 0; i < Nelt; ++i )
			{
				is.read( buff_f );
				all_dists[i] = byte_to_float( buff_f );
			}

			int lab = -1;
			is.read( buff_i );
			int Ncomps = byte_to_int( buff_i );
			for ( int k = 0; k < Ncomps; ++k )
			{
				is.read( buff_i );
				lab = byte_to_int( buff_i );
				this.dists.put(lab, new IntHashMap<> () );

				is.read( buff_i );
				Nelt = byte_to_int( buff_i );
				for ( int i = 0; i < Nelt; ++i )
				{
					is.read( buff_i );
					int px_s3D = byte_to_int( buff_i );
					this.dists.get( lab ).put( px_s3D, new IntHashMap<> ( 2 ) );

					is.read( buff_i );
					int Nelt2 = byte_to_int( buff_i );
					for ( int j = 0; j < Nelt2; ++j )
					{
						is.read( buff_i );
						int px_s3D2 = byte_to_int( buff_i );

						is.read( buff_i );
						int Nelt3 = byte_to_int( buff_i );
						for ( int l = 0; l < Nelt3; ++l )
						{
							is.read( buff_i );
							int didx = byte_to_int( buff_i );
							is.read( buff_i );
							int wStart = byte_to_int( buff_i );
							is.read( buff_i );
							int wEnd = byte_to_int( buff_i );

							//System.out.println(String.format("%d %d %d %d %d %d", comp, px_s, px_d, wStart, wEnd, dw));
							CompTime ct = new CompTime( all_dists[didx], this.win_cents.get( wStart ) - dw,
									this.win_cents.get( wEnd ) + dw );
							if ( ! this.elts_f.containsKey( ct ) )
							{
								int N = this.elts_f.size() - 1;
								this.elts_f.put( ct, N );
								this.elts_r.put( N, ct );
							}

							if ( ! this.dists.get( lab ). get( px_s3D ).containsKey( px_s3D2 ) )
								this.dists.get( lab ).get( px_s3D ).put( px_s3D2, new ArrayList<> ());
							
							this.dists.get( lab ).get( px_s3D ).get( px_s3D2 ).add( this.elts_f.get( ct ) );
						}
					}
				}
			}

			assert( is.read(buff_i) == -1 );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public ComponentDistancesTime( final String fname, final ImagePlus compImg, int w, int h, double dt, double pxsize ) throws FileNotFoundException
	{
		this.dists = new IntHashMap<> ();
		this.w = w;
		this.h = h;
		this.pxsize = pxsize;
		this.compImg = compImg;
		this.spotComps = new IntHashMap<> ();

		this.elts_f = new HashMap<CompTime, Integer> ();
		this.elts_r = new HashMap<Integer, CompTime> ();

		this.read_binary_spts( fname, dt );
	}

	public ArrayList<Integer> getWindowIdxs ( int frame )
	{
		ArrayList<Integer> res = new ArrayList<> ();
		for ( int i = 0; i < this.win_cents.size(); ++i )
		{
			if ( frame >= this.win_cents.get( i ) - this.wdur / 2 &&
				 frame <= this.win_cents.get( i ) + this.wdur / 2 )
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
			assert( this.getWindowIdxs( frame ).size() <= 1 );
			for ( int i: this.getWindowIdxs( frame ) )
				comps.add( new SpotComp( i, this.compImg.getStack().getProcessor( i+1 ).getPixel( px, py ) ) );

			this.spotComps.put( s.ID(), comps );
		}
	}

	public void clear_spots_comps()
	{
		System.out.println("Clearing spots components cache");
		this.spotComps.clear();
		System.out.println(this.spotComps.size());
	}

	public float dist( int px1, int px2, int comp, int winIdx )
	{
		if ( px1 > px2 )
		{
			int tmp = px1;
			px1 = px2;
			px2 = tmp;
		}

		if ( !this.dists.containsKey( comp ) || !this.dists.get( comp ).containsKey( px1 ) ||
			 !this.dists.get( comp ).get( px1 ).containsKey( px2 ) )
			return Float.MAX_VALUE;

		return this.elts_r.get(this.dists.get( comp ).get( px1 ).get( px2 ).get( winIdx ) ).dist;
	}

//	public IntHashMap< IntHashMap< Integer > > dists()
//	{
//		return this.dists;
//	}

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

	public ArrayList<Integer> win_cents()
	{
		return this.win_cents;
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
