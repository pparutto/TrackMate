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

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS_Z;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_THRESHOLD_BLOB_RATIO;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS_Z;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD_BLOB_RATIO;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.AdvancedLogDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class AdvancedLogDetectorFactory< T extends RealType< T > & NativeType< T > > extends LogDetectorFactory< T >
{

	public static final String DETECTOR_KEY = "ADVANCED_LOG_DETECTOR";

	public static final String NAME = "Advanced LoG detector";

	public static final String INFO_TEXT = "<html>"
			+ "This detector extends the LoG detector by taking into <br>"
			+ "account the possible elongation of the spots in the Z <br>"
			+ "direction. It also computes a supplementary spot feature, <bt>"
			+ "the blob ratio, that ranges from 0 to 1 and is low for <br>"
			+ "detections that resemble a line or a plane, and high for <br>"
			+ "detection that resembles a spherical blob. "
			+ "</html>";

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final double radiusXY = ( Double ) settings.get( KEY_RADIUS );
		final double radiusZ = ( Double ) settings.get( KEY_RADIUS_Z );
		final double thresholdQuality = ( Double ) settings.get( KEY_THRESHOLD );
		final double thresholdBlobRatio = ( Double ) settings.get( KEY_THRESHOLD_BLOB_RATIO );
		final boolean doMedian = ( Boolean ) settings.get( KEY_DO_MEDIAN_FILTERING );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final RandomAccessible< T > imFrame = prepareFrameImg( frame );

		final AdvancedLogDetector< T > detector = new AdvancedLogDetector<>(
				imFrame,
				interval,
				calibration,
				radiusXY,
				radiusZ,
				thresholdQuality,
				thresholdBlobRatio,
				doSubpixel,
				doMedian );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > lSettings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( lSettings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_RADIUS_Z, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_THRESHOLD_BLOB_RATIO, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_DO_MEDIAN_FILTERING, Boolean.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_RADIUS_Z );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_THRESHOLD_BLOB_RATIO );
		mandatoryKeys.add( KEY_DO_MEDIAN_FILTERING );
		mandatoryKeys.add( KEY_DO_SUBPIXEL_LOCALIZATION );
		ok = ok & checkMapKeys( lSettings, mandatoryKeys, null, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > lSettings, final Element element )
	{
		boolean ok = super.marshall( lSettings, element );
		if ( !ok )
			return false;

		final StringBuilder errorHolder = new StringBuilder();
		ok = writeAttribute( lSettings, element, KEY_RADIUS_Z, Double.class, errorHolder )
				&& writeAttribute( lSettings, element, KEY_THRESHOLD_BLOB_RATIO, Double.class, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > lSettings )
	{
		boolean ok = super.unmarshall( element, lSettings );
		if ( !ok )
			return false;

		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & readDoubleAttribute( element, lSettings, KEY_RADIUS_Z, errorHolder );
		ok = ok & readDoubleAttribute( element, lSettings, KEY_THRESHOLD_BLOB_RATIO, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( lSettings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings lSettings, final Model model )
	{
		return new AdvancedLogDetectorConfigurationPanel( lSettings, model, INFO_TEXT, NAME );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = super.getDefaultSettings();
		lSettings.put( KEY_RADIUS_Z, DEFAULT_RADIUS_Z );
		lSettings.put( KEY_THRESHOLD_BLOB_RATIO, DEFAULT_THRESHOLD_BLOB_RATIO );
		return lSettings;
	}

	@Override
	public AdvancedLogDetectorFactory< T > copy()
	{
		return new AdvancedLogDetectorFactory< >();
	}

	@Override
	public void declareFeatures( final FeatureModel featureModel )
	{
		final String feature = AdvancedLogDetector.BLOB_RATIO;
		final Collection< String > features = Collections.singleton( feature );
		final Map< String, String > featureNames = Collections.singletonMap( feature, "Blob ratio" );
		final Map< String, String > featureShortNames = Collections.singletonMap( feature, "Blob ratio" );
		final Map< String, Dimension > dimensions = Collections.singletonMap( feature, Dimension.QUALITY );
		final Map< String, Boolean > isint = Collections.singletonMap( feature, Boolean.FALSE );
		featureModel.declareSpotFeatures( features, featureNames, featureShortNames, dimensions, isint );
	}
}
