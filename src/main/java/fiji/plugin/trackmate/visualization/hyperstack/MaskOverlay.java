package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.Roi;


public class MaskOverlay extends Roi
{

	private static final long serialVersionUID = 1L;

	private static final Font LABEL_FONT = new Font( "Arial", Font.BOLD, 12 );

	private static final boolean DEBUG = false;


	protected final double[] calibration;

	protected Composite composite = AlphaComposite.getInstance( AlphaComposite.SRC_OVER );

	protected Map< String, Object > displaySettings;

	protected final Model model;

	public MaskOverlay( final Model model, final ImagePlus imp, final Map< String, Object > displaySettings )
	{
		super( 0, 0, imp );
		this.model = model;
		this.imp = imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.displaySettings = displaySettings;
	}

	@Override
	public void drawOverlay( final Graphics g )
	{
		final Graphics2D g2d = ( Graphics2D ) g;

		System.out.println(String.format("AAAAAAAAAAA %d", this.model.getMaskImg().getNFrames()));
		if (this.model.getMaskImg().getNFrames() > 1)
			this.model.getMaskImg().setT(imp.getFrame());
		g2d.drawImage(this.model.getMaskImg().getBufferedImage(), 1, 1, null);
	}
}
