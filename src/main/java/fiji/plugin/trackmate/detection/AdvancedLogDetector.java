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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fiji.plugin.trackmate.Spot;
import ij.plugin.Duplicator;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class AdvancedLogDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded
{

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "AdvancedLogDetector: ";

	protected String baseErrorMessage;

	protected final RandomAccessible< T > img;

	protected final double[] calibration;

	protected final double radiusXY;

	protected final double radiusZ;

	protected final double thresholdQuality;

	protected final double thresholdEdgeElimination;

	protected final boolean doSubPixelLocalization;

	protected final boolean doMedianFilter;

	protected String errorMessage;

	/** The list of {@link Spot} that will be populated by this detector. */
	protected final List< Spot > spots = new ArrayList<>();

	/** The processing time in ms. */
	protected long processingTime;

	protected int numThreads;

	protected final Interval interval;


	/*
	 * CONSTRUCTORS
	 */

	public AdvancedLogDetector(
			final RandomAccessible< T > img,
			final Interval interval,
			final double[] calibration,
			final double radiusXY,
			final double radiusZ,
			final double thresholdQuality,
			final double thresholdEdgeElimination,
			final boolean doSubPixelLocalization,
			final boolean doMedianFilter )
	{
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.radiusXY = radiusXY;
		this.radiusZ = radiusZ;
		this.thresholdQuality = thresholdQuality;
		this.thresholdEdgeElimination = thresholdEdgeElimination;
		this.doSubPixelLocalization = doSubPixelLocalization;
		this.doMedianFilter = doMedianFilter;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.numDimensions() > 3 )
		{
			errorMessage = baseErrorMessage + "Image must be 1D, 2D or 3D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		spots.clear();

		/*
		 * Copy to float for convolution.
		 */

		final ImgFactory< FloatType > factory = Util.getArrayOrCellImgFactory( interval, new FloatType() );
		Img< FloatType > floatImg = DetectionUtils.copyToFloatImg( img, interval, factory );

		/*
		 * Do median filtering (or not).
		 */

		if ( doMedianFilter )
		{
			floatImg = DetectionUtils.applyMedianFilter( floatImg );
			if ( null == floatImg )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Failed to apply median filter.";
				return false;
			}
		}


		// Squeeze singleton dimensions
		int ndims = interval.numDimensions();
		for ( int d = 0; d < interval.numDimensions(); d++ )
			if ( interval.dimension( d ) <= 1 )
				ndims--;

		final Img< FloatType > kernel = DetectionUtils.createLoGKernel( radiusXY, radiusZ, ndims, calibration );
		final FFTConvolution< FloatType > fftconv = new FFTConvolution<>( floatImg, kernel );

		// Determine the right img factory for FFT calculation.
		Interval fftinterval = floatImg;
		for ( int d = 0; d < kernel.numDimensions(); d++ )
			fftinterval = Intervals.expand( fftinterval, kernel.dimension( d ), d );
		final ImgFactory< ComplexFloatType > imgFactory = Util.getArrayOrCellImgFactory( fftinterval, new ComplexFloatType() );
		fftconv.setFFTImgFactory( imgFactory );

		// Convolve.
		final ExecutorService service = Executors.newFixedThreadPool(numThreads);
		fftconv.setExecutorService(service);
		fftconv.convolve();
		service.shutdown();

		// Find local maxima on the LoG filtered image.
		final long[] minopposite = new long[ interval.numDimensions() ];
		interval.min( minopposite );
		final IntervalView< FloatType > to = Views.translate( floatImg, minopposite );
		final List< Spot > lm = DetectionUtils.findLocalMaxima( to, thresholdQuality, calibration, radiusXY, doSubPixelLocalization, numThreads );

		ImageJFunctions.show( kernel, "Kernel" ); // DEBUG
		new Duplicator().run( ImageJFunctions.wrap( to, "Filtered rXY " + radiusXY + " rZ=" + radiusZ ) ).show(); // DEBUG

		spots.addAll( lm ); // TODO

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}
}
