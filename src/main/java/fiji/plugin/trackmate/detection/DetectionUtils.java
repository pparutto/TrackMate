/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2022 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.util.MedianFilter2D;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class DetectionUtils
{

	/**
	 * Preview a detection results.
	 * <p>
	 * This method returns immediately and execute the detection in a separate
	 * thread. It executes the detection in one frame only and writes the
	 * results in the specified model object.
	 * 
	 * @param model
	 *            the model to write detection results in.
	 * @param settings
	 *            the settings to use for the image input and the ROI input.
	 * @param detectorFactory
	 *            the detector factory to use for detection.
	 * @param detectorSettings
	 *            the settings for the detection, specific to the detector
	 *            factory/
	 * @param frame
	 *            the frame (0-based) to execute the detection in.
	 * @param logger
	 *            a logger to write results and error messages to.
	 * @param buttonEnabler
	 *            a consumer that will receive <code>false</code> at the
	 *            beginning of the preview, and <code>true</code> at its end.
	 *            Can be used to disable GUI elements.
	 */
	public static final void preview(
			final Model model,
			final Settings settings,
			final SpotDetectorFactoryBase< ? > detectorFactory,
			final Map< String, Object > detectorSettings,
			final int frame,
			final Logger logger,
			final Consumer< Boolean > buttonEnabler )
	{
		buttonEnabler.accept( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@Override
			public void run()
			{
				try
				{

					final Settings lSettings = new Settings( settings.imp );
					lSettings.tstart = frame;
					lSettings.tend = frame;
					lSettings.roi = settings.roi;

					lSettings.detectorFactory = detectorFactory;
					lSettings.detectorSettings = detectorSettings;

					final TrackMate trackmate = new TrackMate( lSettings );
					trackmate.getModel().setLogger( logger );

					final boolean detectionOk = trackmate.execDetection();
					if ( !detectionOk )
					{
						logger.error( trackmate.getErrorMessage() );
						return;
					}
					logger.log( "Found " + trackmate.getModel().getSpots().getNSpots( false ) + " spots." );

					// Wrap new spots in a list.
					final SpotCollection newspots = trackmate.getModel().getSpots();
					final Iterator< Spot > it = newspots.iterator( frame, false );
					final ArrayList< Spot > spotsToCopy = new ArrayList<>( newspots.getNSpots( frame, false ) );
					while ( it.hasNext() )
						spotsToCopy.add( it.next() );

					// Pass new spot list to model.
					model.getSpots().put( frame, spotsToCopy );
					// Make them visible
					for ( final Spot spot : spotsToCopy )
						spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );

					// Generate event for listener to reflect changes.
					model.setSpots( model.getSpots(), true );

				}
				catch ( final Exception e )
				{
					logger.error( e.getMessage() );
					e.printStackTrace();
				}
				finally
				{
					buttonEnabler.accept( true );
				}
			}
		}.start();
	}

	/**
	 * Returns <code>true</code> if the specified image is 2D. It can have
	 * multiple channels and multiple time-points; this method only looks at
	 * whether several Z-slices can be found.
	 * 
	 * @param img
	 *            the image.
	 * @return <code>true</code> if the image is 2D, regardless of time and
	 *         channel.
	 */
	public static final boolean is2D( final ImgPlus< ? > img )
	{
		return img.dimensionIndex( Axes.Z ) < 0
				|| img.dimension( img.dimensionIndex( Axes.Z ) ) <= 1;
	}

	public static final boolean is2D( final ImagePlus imp )
	{
		return imp.getNSlices() <= 1;
	}

	/**
	 * Creates a laplacian of gaussian (LoG) kernel tuned for blobs with a
	 * radius specified <b>using calibrated units</b>. The specified calibration
	 * is used to determine the dimensionality of the kernel and to map it on a
	 * pixel grid.
	 * <p>
	 * With this version of the method, the kernel has a different <b>physical
	 * size</b> in X and Y, versus Z, specified by <code>radiusXY</code> and
	 * <code>radiusZ</code> parameters. If the source image has less than 3
	 * dimensions, the <code>radiusZ</code> is ignored.
	 * 
	 * @param radiusXY
	 *            the blob radius in X and Y (in image unit).
	 * @param radiusZ
	 *            the blob radius in Z (in image unit).
	 * @param nDims
	 *            the dimensionality of the desired kernel. Must be 1, 2 or 3.
	 * @param calibration
	 *            the pixel sizes, specified as <code>double[]</code> array.
	 * @return a new image containing the LoG kernel.
	 */
	public static final Img< FloatType > createLoGKernel( final double radiusXY, final double radiusZ, final int nDims, final double[] calibration )
	{
		// Optimal sigma for LoG approach and dimensionality.
		final double sigmaXY = radiusXY / Math.sqrt( nDims );
		final double sigmaZ = radiusZ / Math.sqrt( nDims );
		final double[] sigmaPixels = new double[ nDims ];
		final double[] sigmas = new double[ nDims ];
		for ( int d = 0; d < 2; d++ )
		{
			sigmaPixels[ d ] = sigmaXY / calibration[ d ];
			sigmas[ d ] = sigmaXY;
		}
		for ( int d = 2; d < sigmaPixels.length; d++ )
		{
			sigmaPixels[ d ] = sigmaZ / calibration[ d ];
			sigmas[ d ] = sigmaZ;
		}

		final long[] sizes = new long[ nDims ];
		final long[] middle = new long[ nDims ];
		for ( int d = 0; d < nDims; ++d )
		{
			// From Tobias Gauss3
			final int hksizes = Math.max( 2, ( int ) ( 3 * sigmaPixels[ d ] + 0.5 ) + 1 );
			sizes[ d ] = 3 + 2 * hksizes;
			middle[ d ] = 1 + hksizes;
		}
		final ArrayImg< FloatType, FloatArray > kernel = ArrayImgs.floats( sizes );

		/*
		 * LoG normalization factor, so that the filtered peak have the maximal
		 * value for spots that have the size this kernel is tuned to. With this
		 * value, the peak value will be of the same order of magnitude than the
		 * raw spot (if it has the right size). This value also ensures that if
		 * the image has its calibration changed, one will retrieve the same
		 * peak value than before scaling. However, I (JYT) could not derive the
		 * exact formula if the image is scaled differently across X, Y and Z.
		 */
		final double C = 1. / Math.PI / sigmaPixels[ 0 ] / sigmaPixels[ 0 ];

		final ArrayCursor< FloatType > c = kernel.cursor();
		final long[] coords = new long[ nDims ];
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize( coords );

			double mantissa = 0.;
			double exponent = 0.;
			for ( int d = 0; d < coords.length; d++ )
			{
				// Work in image coordinates
				final double x = calibration[ d ] * ( coords[ d ] - middle[ d ] );
				exponent += ( x * x / 2. / sigmas[ d ] / sigmas[ d ] );
				mantissa += 1. / sigmas[ d ] / sigmas[ d ] * ( x * x / sigmas[ d ] / sigmas[ d ] - 1 );
			}
			c.get().setReal( -C * mantissa * Math.exp( -exponent ) );
		}

		return kernel;
	}

	/**
	 * Creates a laplacian of gaussian (LoG) kernel tuned for blobs with a
	 * radius specified <b>using calibrated units</b>. The specified calibration
	 * is used to determine the dimensionality of the kernel and to map it on a
	 * pixel grid.
	 * <p>
	 * With this version of the method, the kernel has the same <b>physical
	 * size</b> in all dimensions, specified by the <code>radius</code>
	 * parameter.
	 * 
	 * @param radius
	 *            the blob radius (in image unit).
	 * @param nDims
	 *            the dimensionality of the desired kernel. Must be 1, 2 or 3.
	 * @param calibration
	 *            the pixel sizes, specified as <code>double[]</code> array.
	 * @return a new image containing the LoG kernel.
	 */
	public static final Img< FloatType > createLoGKernel( final double radius, final int nDims, final double[] calibration )
	{
		return createLoGKernel( radius, radius, nDims, calibration );
	}

	/**
	 * Returns a metric that quantifies how spherical the detection represented
	 * by the specified eignenvalues is. This metric is high when the detection
	 * corresponds to a spherical blob, and low when it corresponds to a line or
	 * a plane.
	 * 
	 * @param evs
	 *            the eigenvalue array. Is modified.
	 * @return a positive metric.
	 */
	public static final double computeBlobStrength( final double[] evs )
	{
		for ( int i = 0; i < evs.length; i++ )
			evs[ i ] = Math.abs( evs[ i ] );
		Arrays.sort( evs );
		return evs[ 0 ] / evs[ evs.length - 1 ];
	}

	/**
	 * Computes the eigenvalues of the Hessian matrix at the specified location
	 * in the input image.
	 * 
	 * @param ra
	 *            a {@link RandomAccess} on the source image. Must be accessible
	 *            out of bounds by a border of 1 pixel extra.
	 * @param pos
	 *            the location at which to compute the Hessian.
	 * @param calibration
	 *            the physical pixel size.
	 * @param evs
	 *            a double array to store the eigenvalues.
	 */
	public static final void hessianEigenvalues( final RandomAccess< FloatType > ra, final Localizable pos, final double[] calibration, final double[] evs )
	{
		final int n = ra.numDimensions();
		// Number of elements in the triangular matrix.
		final int nelements = n * ( n + 1 ) / 2;
		final double[] H = new double[ nelements ];

		/*
		 * Compute Hessian matrix. Plain central difference for gradients.
		 */
		ra.setPosition( pos );
		int i = 0;
		for ( int d2 = 0; d2 < n; d2++ )
		{
			for ( int d1 = d2; d1 < n; d1++ )
			{
				final double ddy;
				if ( d1 == d2 )
				{
					final double y0 = ra.get().getRealDouble();
					ra.bck( d1 );
					final double ym = ra.get().getRealDouble();
					ra.fwd( d1 );
					ra.fwd( d1 );
					final double yp = ra.get().getRealDouble();
					ddy = ( yp - 2. * y0 + ym ) / ( calibration[ d1 ] * calibration[ d1 ] );
					ra.bck( d1 );
				}
				else
				{
					ra.bck( d1 );
					final double dym = dy( ra, d2, calibration[ d2 ] );
					ra.fwd( d1 );
					ra.fwd( d1 );
					final double dyp = dy( ra, d2, calibration[ d2 ] );
					ddy = ( dyp - dym ) / ( 2. * calibration[ d1 ] );
					ra.bck( d1 );
				}
				H[ i++ ] = ddy;
			}
		}

		/*
		 * Compute eigenvalues.
		 */
		if ( n == 2 )
		{
			final double a00 = H[ 0 ];
			final double a01 = H[ 1 ];
			final double a11 = H[ 2 ];
			final double sum = a00 + a11;
			final double diff = a00 - a11;
			final double sqrt = Math.sqrt( 4 * a01 * a01 + diff * diff );
			evs[ 0 ] = 0.5 * ( sum + sqrt );
			evs[ 1 ] = 0.5 * ( sum - sqrt );
		}
		else
		{
			getEigenvaluesSymmetric33( H, evs );
		}
	}

	/*
	 * Adapted from JTK Eigen.java class, by Dave Hale.
	 */
	private static void getEigenvaluesSymmetric33( final double[] H, final double[] d )
	{
		final double a00 = H[ 0 ];
		final double a01 = H[ 1 ];
		final double a02 = H[ 2 ];
		final double a11 = H[ 3 ];
		final double a12 = H[ 4 ];
		final double a22 = H[ 5 ];

		final double de = a01 * a12;
		final double dd = a01 * a01;
		final double ee = a12 * a12;
		final double ff = a02 * a02;
		final double c2 = a00 + a11 + a22;
		final double c1 = ( a00 * a11 + a00 * a22 + a11 * a22 ) - ( dd + ee + ff );
		final double c0 = a22 * dd + a00 * ee + a11 * ff - a00 * a11 * a22 - 2.0 * a02 * de;
		final double p = c2 * c2 - 3.0 * c1;
		final double q = c2 * ( p - 1.5 * c1 ) - 13.5 * c0; // 13.5 = 27/2
		final double t = 27.0 * ( 0.25 * c1 * c1 * ( p - c1 ) + c0 * ( q + 6.75 * c0 ) );
		// 6.75 = 27/4
		final double phi = 1. / 3. * Math.atan2( Math.sqrt( Math.abs( t ) ), q );
		final double sqrtp = Math.sqrt( Math.abs( p ) );
		final double c = sqrtp * Math.cos( phi );
		final double s = 1. / Math.sqrt( 3. ) * sqrtp * Math.sin( phi );
		final double dt = 1. / 3. * ( c2 - c );
		d[ 0 ] = dt + c;
		d[ 1 ] = dt + s;
		d[ 2 ] = dt - s;
	}

	// Plain central difference.
	private static final double dy( final RandomAccess< FloatType > ra, final int d, final double dx )
	{
		ra.bck( d );
		final double ym = ra.get().getRealDouble();
		ra.fwd( d );
		ra.fwd( d );
		final double yp = ra.get().getRealDouble();
		final double dy = ( yp - ym ) / ( 2. * dx );
		ra.bck( d );
		return dy;
	}

	/**
	 * Copy an interval of the specified source image on a float image.
	 *
	 * @param img
	 *            the source image.
	 * @param interval
	 *            the interval in the source image to copy.
	 * @param factory
	 *            a factory used to build the float image.
	 * @return a new float Img. Careful: even if the specified interval does not
	 *         start at (0, 0), the new image will have its first pixel at
	 *         coordinates (0, 0).
	 */
	public static final < T extends RealType< T > > Img< FloatType > copyToFloatImg( final RandomAccessible< T > img, final Interval interval, final ImgFactory< FloatType > factory )
	{
		final Img< FloatType > output = factory.create( interval );
		final RandomAccess< T > in = Views.zeroMin( Views.interval( img, interval ) ).randomAccess();
		final Cursor< FloatType > out = output.cursor();
		final RealFloatConverter< T > c = new RealFloatConverter<>();

		while ( out.hasNext() )
		{
			out.fwd();
			in.setPosition( out );
			c.convert( in.get(), out.get() );
		}
		return output;
	}

	/**
	 * Returns a new {@link Interval}, built by squeezing out singleton
	 * dimensions from the specified interval.
	 *
	 * @param interval
	 *            the interval to squeeze.
	 * @return a new interval.
	 */
	public static final Interval squeeze( final Interval interval )
	{
		int nNonSingletonDimensions = 0;
		for ( int d = nNonSingletonDimensions; d < interval.numDimensions(); d++ )
		{
			if ( interval.dimension( d ) > 1 )
			{
				nNonSingletonDimensions++;
			}
		}

		final long[] min = new long[ nNonSingletonDimensions ];
		final long[] max = new long[ nNonSingletonDimensions ];
		int index = 0;
		for ( int d = 0; d < interval.numDimensions(); d++ )
		{
			if ( interval.dimension( d ) > 1 )
			{
				min[ index ] = interval.min( d );
				max[ index ] = interval.max( d );
				index++;
			}
		}
		return new FinalInterval( min, max );
	}

	/**
	 * Apply a simple 3x3 median filter to the target image.
	 */
	public static final < R extends RealType< R > & NativeType< R > > Img< R > applyMedianFilter( final RandomAccessibleInterval< R > image )
	{
		final MedianFilter2D< R > medFilt = new MedianFilter2D<>( image, 1 );
		if ( !medFilt.checkInput() || !medFilt.process() )
		{ return null; }
		return medFilt.getResult();
	}

	public static final List< Spot > findLocalMaxima( final RandomAccessibleInterval< FloatType > source, final double threshold, final double[] calibration, final double radius, final boolean doSubPixelLocalization, final int numThreads )
	{
		/*
		 * Find maxima.
		 */

		final FloatType val = new FloatType();
		val.setReal( threshold );
		final LocalNeighborhoodCheck< Point, FloatType > localNeighborhoodCheck = new LocalExtrema.MaximumCheck<>( val );
		final IntervalView< FloatType > dogWithBorder = Views.interval( Views.extendMirrorSingle( source ), Intervals.expand( source, 1 ) );
		final ExecutorService service = Executors.newFixedThreadPool( numThreads );
		List< Point > peaks;
		try
		{
			peaks = LocalExtrema.findLocalExtrema( dogWithBorder, localNeighborhoodCheck, new RectangleShape( 1, true ), service, numThreads );
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
			peaks = Collections.emptyList();
		}
		service.shutdown();

		if ( peaks.isEmpty() )
		{ return Collections.emptyList(); }

		final List< Spot > spots;
		if ( doSubPixelLocalization )
		{

			/*
			 * Sub-pixel localize them.
			 */

			final SubpixelLocalization< Point, FloatType > spl = new SubpixelLocalization<>( source.numDimensions() );
			spl.setNumThreads( numThreads );
			spl.setReturnInvalidPeaks( true );
			spl.setCanMoveOutside( true );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			final ArrayList< RefinedPeak< Point > > refined = spl.process( peaks, dogWithBorder, source );

			spots = new ArrayList<>( refined.size() );
			final RandomAccess< FloatType > ra = source.randomAccess();

			/*
			 * Deal with different dimensionality manually. Profound comment:
			 * this is the proof that this part of the code is sloppy. ImgLib2
			 * is supposed to be dimension-generic. I just did not use properly
			 * here.
			 */

			if ( source.numDimensions() > 2 )
			{ // 3D
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double quality = ra.get().getRealDouble();
					final double x = refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = refinedPeak.getDoublePosition( 1 ) * calibration[ 1 ];
					final double z = refinedPeak.getDoublePosition( 2 ) * calibration[ 2 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else if ( source.numDimensions() > 1 )
			{ // 2D
				final double z = 0;
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double quality = ra.get().getRealDouble();
					final double x = refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = refinedPeak.getDoublePosition( 1 ) * calibration[ 1 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else
			{ // 1D
				final double z = 0;
				final double y = 0;
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double quality = ra.get().getRealDouble();
					final double x = refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}

			}
		}
		else
		{
			spots = new ArrayList<>( peaks.size() );
			final RandomAccess< FloatType > ra = source.randomAccess();
			if ( source.numDimensions() > 2 )
			{ // 3D
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double quality = ra.get().getRealDouble();
					final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = peak.getDoublePosition( 1 ) * calibration[ 1 ];
					final double z = peak.getDoublePosition( 2 ) * calibration[ 2 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else if ( source.numDimensions() > 1 )
			{ // 2D
				final double z = 0;
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double quality = ra.get().getRealDouble();
					final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = peak.getDoublePosition( 1 ) * calibration[ 1 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else
			{ // 1D
				final double z = 0;
				final double y = 0;
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double quality = ra.get().getRealDouble();
					final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}

			}
		}

		return spots;
	}

	/**
	 * Return a view of the specified input image, at the specified channel
	 * (0-based) and the specified frame (0-based too).
	 * 
	 * @param <T>
	 *            the type of the input image.
	 * @param img
	 *            the input image.
	 * @param channel
	 *            the channel to extract.
	 * @param frame
	 *            the frame to extract.
	 * @return a view of the input image.
	 */
	public static final < T extends Type< T > > RandomAccessibleInterval< T > prepareFrameImg(
			final ImgPlus< T > img,
			final int channel,
			final int frame )
	{
		final ImgPlus< T > singleTimePoint;
		if ( img.dimensionIndex( Axes.TIME ) < 0 )
			singleTimePoint = img;
		else
			singleTimePoint = ImgPlusViews.hyperSlice( img, img.dimensionIndex( Axes.TIME ), frame );

		final ImgPlus< T > singleChannel;
		if ( singleTimePoint.dimensionIndex( Axes.CHANNEL ) < 0 )
			singleChannel = singleTimePoint;
		else
			singleChannel = ImgPlusViews.hyperSlice( singleTimePoint, singleTimePoint.dimensionIndex( Axes.CHANNEL ), channel );
		return singleChannel;
	}
}
