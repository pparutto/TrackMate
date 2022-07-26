package fiji.plugin.trackmate.action.closegaps;

import java.awt.Frame;

import javax.swing.JFrame;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;

public class CloseGapsController
{

	// Default values, stored for this session.
	static CloseGapsParams params = CloseGapsParams.create().get();

	private final CloseGapsPanel gui;

	private final Model model;

	private final SelectionModel selectionModel;

	private final Logger logger;

	private final ImgPlus< ? > img;

	public CloseGapsController( final Model model, final SelectionModel selectionModel, final ImgPlus< ? > img, final CloseGapsParams params, final String units, final Logger logger )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.img = img;
		this.logger = logger;
		final int nChannels = ( int ) ( img.dimensionIndex( Axes.CHANNEL ) < 0
				? 1
				: img.dimension( img.dimensionIndex( Axes.CHANNEL ) ) );
		this.gui = new CloseGapsPanel( params, units, nChannels );
		gui.btnGo.addActionListener( e -> execute( gui.getParams() ) );
	}

	public void show( final Frame parent )
	{
		final JFrame frame = new JFrame( "Close track gaps" );
		frame.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		frame.getContentPane().add( gui );
		frame.setSize( 340, 650 );
		GuiUtils.positionWindow( frame, parent );
		frame.setVisible( true );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void execute( final CloseGapsParams params )
	{
		CloseGapsController.params = params;
		CloseTrackGaps.run( model, selectionModel, ( ImgPlus ) img, params, logger );
	}
}
