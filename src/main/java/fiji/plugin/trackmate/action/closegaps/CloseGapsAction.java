package fiji.plugin.trackmate.action.closegaps;

import static fiji.plugin.trackmate.gui.Icons.ORANGE_ASTERISK_ICON;

import java.awt.Frame;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.action.closegaps.CloseGapsParams.Builder;
import fiji.plugin.trackmate.action.closegaps.CloseGapsParams.Method;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.HessianDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;

public class CloseGapsAction extends AbstractTMAction
{

	public static final String NAME = "Close gaps in tracks by introducing new spots";

	public static final String KEY = "CLOSE_TRACKS_GAPS";

	public static final String INFO_TEXT = "<html>"
			+ "This action closes gaps in tracks by introducing new spots. "
			+ "<p> "
			+ "Several methods can be used to create these spots, either "
			+ "simply interpolating their position from predecessors and "
			+ "successors spots, or by searching them using a spot detector. "
			+ "</html>";

	@Override
	public void execute(
			final TrackMate trackmate,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings,
			final Frame parent )
	{
		final Model model = trackmate.getModel();
		final ImgPlus< ? > img = TMUtils.rawWraps( trackmate.getSettings().imp );
		final String units = model.getSpaceUnits();
		CloseGapsParams params = CloseGapsController.params;
		params = fetchDetectionParameters( params, trackmate.getSettings() );
		new CloseGapsController( model, selectionModel, img, params, units, logger ).show( parent );
	}

	private CloseGapsParams fetchDetectionParameters( final CloseGapsParams params, final Settings settings )
	{
		final Builder builder = CloseGapsParams.create( params );

		final SpotDetectorFactoryBase< ? > factory = settings.detectorFactory;
		if ( factory.getKey().equalsIgnoreCase( LogDetectorFactory.DETECTOR_KEY ) || factory.getKey().equalsIgnoreCase( DogDetectorFactory.DETECTOR_KEY ) )
		{
			builder.method( Method.LOG_DETECTOR );
			final Map< String, Object > df = settings.detectorSettings;

			final Object obj1 = df.get( DetectorKeys.KEY_RADIUS );
			if ( obj1 != null && obj1 instanceof Number )
			{
				builder.logRadius( ( ( Number ) obj1 ).doubleValue() );
				builder.logAutoRadius( false );
			}
			else
			{
				builder.logAutoRadius( true );
			}
		}
		else if ( factory.getKey().equalsIgnoreCase( HessianDetectorFactory.DETECTOR_KEY ) )
		{
			builder.method( Method.HESSIAN_DETECTOR );
			final Map< String, Object > df = settings.detectorSettings;

			final Object obj1 = df.get( DetectorKeys.KEY_RADIUS );
			if ( obj1 != null && obj1 instanceof Number )
				builder.hessianRadiusXY( ( ( Number ) obj1 ).doubleValue() );

			final Object obj2 = df.get( DetectorKeys.KEY_RADIUS_Z );
			if ( obj2 != null && obj2 instanceof Number )
			{
				builder.hessianRadiusZ( ( ( Number ) obj2 ).doubleValue() );
				builder.hessianAutoRadius( false );
			}
			else
			{
				builder.hessianAutoRadius( true );
			}
		}
		return builder.get();
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

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
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return ORANGE_ASTERISK_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new CloseGapsAction();
		}
	}
}
