package fiji.plugin.trackmate.mesh;

import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;

import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.util.TMUtils;
import gnu.trove.list.array.TDoubleArrayList;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Vertices;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.mesh.naive.NaiveDoubleMesh.Triangles;
import net.imagej.mesh.nio.BufferMeshEdges;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class Demo3DMesh
{

	public static void main( final String[] args )
	{
		try
		{

			ImageJ.main( args );
//			final ImgPlus< BitType > mask = loadTestMask2();
			final ImgPlus< BitType > mask = loadTestMask();

			// Convert it to labeling.
			final ImgLabeling< Integer, IntType > labeling = MaskUtils.toLabeling( mask, mask, 0.5, 1 );
			final ImagePlus out = ImageJFunctions.show( labeling.getIndexImg(), "labeling" );

			// Iterate through all components.
			final LabelRegions< Integer > regions = new LabelRegions< Integer >( labeling );
			final double[] cal = TMUtils.getSpatialCalibration( mask );

			// Holder for the contour coords.
			final TDoubleArrayList cx = new TDoubleArrayList();
			final TDoubleArrayList cy = new TDoubleArrayList();

			// Parse regions to create polygons on boundaries.
			final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
			int j = 0;
			while ( iterator.hasNext() )
			{
				final LabelRegion< Integer > region = iterator.next();

				// To mesh.
				final IntervalView< BoolType > box = Views.zeroMin( region );
				final Mesh mesh = Meshes.marchingCubes( box );
				System.out.println( "Before cleaning: " + mesh.vertices().size() + " vertices and " + mesh.triangles().size() + " faces." );
				final Mesh cleaned = Meshes.removeDuplicateVertices( mesh, 0 );
				System.out.println( "Before simplification: " + cleaned.vertices().size() + " vertices and " + cleaned.triangles().size() + " faces." );
				final Mesh simplified = Meshes.simplify( cleaned, 0.25f, 10 );
//				final Mesh simplified = debugMesh( new long[] { 0, 0, 0 }, region.dimensionsAsLongArray() );

				// Wrap as mesh with edges.
				final BufferMeshEdges emesh = BufferMeshEdges.wrap( simplified, true );
				System.out.println( "After simplification: " + emesh.vertices().size() + " vertices and " + simplified.triangles().size() + " faces." );
				System.out.println();

				// Scale and offset with physical coordinates.
				final double[] origin = region.minAsDoubleArray();
				scale( emesh.vertices(), cal, origin );

				/*
				 * IO.
				 */
				testIO( emesh, ++j );

				/*
				 * Display.
				 */

				// Intersection with a XY plane at a fixed Z position.
				final int zslice = 20; // plan
				final double z = ( zslice ) * cal[ 2 ]; // um

				MeshPlaneIntersection.intersect2( emesh, z, cx, cy );
				toOverlay( cx, cy, out, cal );
			}
			System.out.println( "Done." );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	@SuppressWarnings( "unused" )
	private static Mesh debugMesh( final long[] min, final long[] max )
	{
		final NaiveDoubleMesh mesh = new NaiveDoubleMesh();
		final net.imagej.mesh.naive.NaiveDoubleMesh.Vertices vertices = mesh.vertices();
		final Triangles triangles = mesh.triangles();

		// Coords as X Y Z

		// Bottom square.
		final double[] bnw = new double[] { min[ 0 ], min[ 1 ], min[ 2 ] };
		final double[] bne = new double[] { max[ 0 ], min[ 1 ], min[ 2 ] };
		final double[] bsw = new double[] { min[ 0 ], max[ 1 ], min[ 2 ] };
		final double[] bse = new double[] { max[ 0 ], max[ 1 ], min[ 2 ] };

		// Top square.
		final double[] tnw = new double[] { min[ 0 ], min[ 1 ], max[ 2 ] };
		final double[] tne = new double[] { max[ 0 ], min[ 1 ], max[ 2 ] };
		final double[] tsw = new double[] { min[ 0 ], max[ 1 ], max[ 2 ] };
		final double[] tse = new double[] { max[ 0 ], max[ 1 ], max[ 2 ] };

		// Add vertices.
		final long bnwi = vertices.add( bnw[ 0 ], bnw[ 1 ], bnw[ 2 ] );
		final long bnei = vertices.add( bne[ 0 ], bne[ 1 ], bne[ 2 ] );
		final long bswi = vertices.add( bsw[ 0 ], bsw[ 1 ], bsw[ 2 ] );
		final long bsei = vertices.add( bse[ 0 ], bse[ 1 ], bse[ 2 ] );
		final long tnwi = vertices.add( tnw[ 0 ], tnw[ 1 ], tnw[ 2 ] );
		final long tnei = vertices.add( tne[ 0 ], tne[ 1 ], tne[ 2 ] );
		final long tswi = vertices.add( tsw[ 0 ], tsw[ 1 ], tsw[ 2 ] );
		final long tsei = vertices.add( tse[ 0 ], tse[ 1 ], tse[ 2 ] );

		// Add triangles for the 6 faces.

		// Bottom.
		triangles.add( bnwi, bnei, bswi );
		triangles.add( bnei, bsei, bswi );

		// Top.
		triangles.add( tnwi, tnei, tswi );
		triangles.add( tnei, tsei, tswi );

		// Front (facing south).
		triangles.add( tswi, tsei, bsei );
		triangles.add( tswi, bsei, bswi );

		// Back (facing north).
		triangles.add( tnwi, tnei, bnei );
		triangles.add( tnwi, bnei, bnwi );

		// Left (facing west).
		triangles.add( tnwi, tswi, bswi );
		triangles.add( tnwi, bnwi, bswi );

		// Right (facing east).
		triangles.add( tnei, tsei, bsei );
		triangles.add( tnei, bnei, bsei );

		return mesh;
	}

	private static void toOverlay( final TDoubleArrayList cx, final TDoubleArrayList cy, final ImagePlus out, final double[] cal )
	{
		final int l = cx.size();
		if ( l == 0 )
			return;

		final Roi roi;
		if ( l == 1 )
		{
			roi = new PointRoi(
					cx.get( 0 ) / cal[ 0 ] + 0.5,
					cy.get( 0 ) / cal[ 1 ] + 0.5, null );
		}
		else
		{
			final float[] xRoi = new float[ l ];
			final float[] yRoi = new float[ l ];
			for ( int i = 0; i < l; i++ )
			{
				xRoi[ i ] = ( float ) ( cx.get( i ) / cal[ 0 ] + 0.5 );
				yRoi[ i ] = ( float ) ( cy.get( i ) / cal[ 1 ] + 0.5 );
			}
			roi = new PolygonRoi( xRoi, yRoi, PolygonRoi.POLYGON );
//			roi.setStrokeWidth( 0.2 );
		}

		roi.setStrokeColor( Color.RED );
		Overlay overlay = out.getOverlay();
		if ( overlay == null )
		{
			overlay = new Overlay();
			out.setOverlay( overlay );
		}
		overlay.add( roi );
	}

	private static void testIO( final Mesh simplified, final int j )
	{
		final STLMeshIO meshIO = new STLMeshIO();
		// Serialize to disk.
		try
		{
			meshIO.save( simplified, String.format( "samples/mesh/CElegansMask3D_%02d.stl", j ) );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	private static void scale( final Vertices vertices, final double[] scale, final double[] origin )
	{
		final long nv = vertices.size();
		for ( long i = 0; i < nv; i++ )
		{
			final double x = ( origin[ 0 ] + vertices.x( i ) ) * scale[ 0 ];
			final double y = ( origin[ 1 ] + vertices.y( i ) ) * scale[ 1 ];
			final double z = ( origin[ 2 ] + vertices.z( i ) ) * scale[ 2 ];
			vertices.set( i, x, y, z );
		}
	}

	@SuppressWarnings( "unused" )
	private static < T extends RealType< T > & NumericType< T > > ImgPlus< BitType > loadTestMask()
	{
		final String filePath = "samples/mesh/CElegansMask3D.tif";
		final ImagePlus imp = IJ.openImage( filePath );

		// First channel is the mask.
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final ImgPlus< T > c1 = ImgPlusViews.hyperSlice( img, img.dimensionIndex( Axes.CHANNEL ), 0 );

		// Take the first time-point
		final ImgPlus< T > t1 = ImgPlusViews.hyperSlice( c1, c1.dimensionIndex( Axes.TIME ), 0 );
		// Make it to boolean.
		final RandomAccessibleInterval< BitType > mask = RealTypeConverters.convert( t1, new BitType() );
		return new ImgPlus< BitType >( ImgView.wrap( mask ), t1 );
	}

	@SuppressWarnings( "unused" )
	private static < T extends RealType< T > & NumericType< T > > ImgPlus< BitType > loadTestMask2()
	{
		final String filePath = "samples/mesh/Cube.tif";
		final ImagePlus imp = IJ.openImage( filePath );
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final RandomAccessibleInterval< BitType > mask = RealTypeConverters.convert( img, new BitType() );
		return new ImgPlus<>( ImgView.wrap( mask ), img );
	}
}