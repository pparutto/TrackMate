package fiji.plugin.trackmate.action.closegaps;

import java.awt.Frame;

import javax.swing.JFrame;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.Icons;

public class CloseGapsController
{

	// Default values, stored for this session.
	private static CloseGapsModel params = CloseGapsModel.create().get();

	private final CloseGapsPanel gui;

	private final Model model;

	private final SelectionModel selectionModel;

	public CloseGapsController( final Model model, final SelectionModel selectionModel, final String units )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.gui = new CloseGapsPanel( params, units );
		gui.btnGo.addActionListener( e -> execute( gui.getParams() ) );
	}

	public void show( final Frame parent )
	{
		final JFrame frame = new JFrame( "Close track gaps" );
		frame.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		frame.getContentPane().add( gui );
		frame.setSize( 340, 550 );
		frame.setLocationRelativeTo( parent );
		frame.setVisible( true );
	}

	private void execute( final CloseGapsModel params )
	{
		CloseGapsController.params = params;
		CloseTrackGaps.run( model, selectionModel, params );
	}
}
