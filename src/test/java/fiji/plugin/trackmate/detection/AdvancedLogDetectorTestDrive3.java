package fiji.plugin.trackmate.detection;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gradient.HessianMatrix;
import net.imglib2.algorithm.linalg.eigen.TensorEigenValues;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.CompositeView;
import net.imglib2.view.composite.RealComposite;

public class AdvancedLogDetectorTestDrive3
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();

//		final ImagePlus imp = IJ.openImage( "samples/Celegans-5pc-17timepoints-t13.tif" );
		final ImagePlus imp = IJ.openImage( "samples/TSabateCell.tif" );
		imp.show();

		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > input = TMUtils.rawWraps( imp );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final double radiusXY = 0.6 / 2.; // um;
		final double radiusZ = 1.6 / 2.;
		final double[] radius = new double[] { radiusXY, radiusXY, radiusZ };

		final int numDimensions = input.numDimensions();
		// Sigmas in pixel units.
		final double[] sigmas = new double[ numDimensions ];
		for ( int d = 0; d < sigmas.length; d++ )
		{
			final double cal = d < calibration.length ? calibration[ d ] : 1;
			sigmas[ d ] = radius[ d ] / cal / Math.sqrt( numDimensions );
		}

		/*
		 * Hessian.
		 */

		// Get a suitable image factory.
		final long[] gradientDims = new long[ numDimensions + 1 ];
		final long[] hessianDims = new long[ numDimensions + 1 ];
		for ( int d = 0; d < numDimensions; d++ )
		{
			hessianDims[ d ] = input.dimension( d );
			gradientDims[ d ] = input.dimension( d );
		}
		hessianDims[ numDimensions ] = numDimensions * ( numDimensions + 1 ) / 2;
		gradientDims[ numDimensions ] = numDimensions;
		final Dimensions hessianDimensions = FinalDimensions.wrap( hessianDims );
		final FinalDimensions gradientDimensions = FinalDimensions.wrap( gradientDims );
		final ImgFactory< DoubleType > factory = Util.getArrayOrCellImgFactory( hessianDimensions, new DoubleType() );
		final Img< DoubleType > hessian = factory.create( hessianDimensions );
		final Img< DoubleType > gradient = factory.create( gradientDimensions );
		final Img< DoubleType > gaussian = factory.create( input );

		// Handle multithreading.
		final int nThreads = Runtime.getRuntime().availableProcessors() / 2;
		final ExecutorService es = Executors.newFixedThreadPool( nThreads );

		try
		{
			// Hessian calculation.
			HessianMatrix.calculateMatrix( Views.extendBorder( input ), gaussian,
					gradient, hessian, new OutOfBoundsBorderFactory<>(), nThreads, es,
					sigmas );

			// Hessian eigenvalues.
			final RandomAccessibleInterval< DoubleType > evs = TensorEigenValues
					.calculateEigenValuesSymmetric( hessian, TensorEigenValues
							.createAppropriateResultImg( hessian, factory ),
							nThreads, es );

			final long nevs = evs.dimension( evs.numDimensions() - 1 );
			for ( int d = 0; d < nevs; d++ )
			{
				final IntervalView< DoubleType > ev = Views.hyperSlice( evs, evs.numDimensions()-1, d );
				ImageJFunctions.wrap( ev, "EV" + d ).show();
			}

			final CompositeIntervalView< DoubleType, RealComposite< DoubleType > > composite = Views.collapseReal( evs );
			final Img< DoubleType > harris = factory.create( input );
			final Img< DoubleType > detImg = factory.create( input );
			final Img< DoubleType > trImg = factory.create( input );

			final Cursor< DoubleType > c = harris.localizingCursor();
			final RandomAccess< DoubleType > raDet = detImg.randomAccess( detImg );
			final RandomAccess< DoubleType > raTr = trImg.randomAccess( trImg );
			final CompositeView< DoubleType, RealComposite< DoubleType > >.CompositeRandomAccess ra = composite.randomAccess( harris );

			while ( c.hasNext() )
			{
				c.fwd();
				ra.setPosition( c );
				raDet.setPosition( c );
				raTr.setPosition( c );
				final RealComposite< DoubleType > rc = ra.get();
				double tr = 0.;
				double det = 1.;
				for ( final DoubleType d : rc )
				{
					tr += d.get();
					det *= d.get();
				}
				raDet.get().set( det );
				raTr.get().set( tr );
				c.get().set( det - 0.05 * Math.pow( tr, numDimensions ) );
			}
			ImageJFunctions.wrap( harris, "Harris" ).show();
			ImageJFunctions.wrap( detImg, "Det" ).show();
			ImageJFunctions.wrap( trImg, "Tr" ).show();

			return;
		}
		catch ( final IncompatibleTypeException | InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
			return;
		}
	}
}
