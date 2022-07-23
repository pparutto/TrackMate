package fiji.plugin.trackmate.action.closegaps;

import static fiji.plugin.trackmate.gui.Icons.ORANGE_ASTERISK_ICON;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

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
		final String units = model.getSpaceUnits();
		new CloseGapsController( model, selectionModel, units ).show( parent );
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
