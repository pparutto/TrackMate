package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Map;

import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.Roi;


public class ImageOverlay extends Roi
{

	private static final long serialVersionUID = 1L;

	protected final double[] calibration;

	protected Composite composite = AlphaComposite.getInstance( AlphaComposite.SRC_OVER );

	protected Map< String, Object > displaySettings;

	private final ImagePlus img;

	public ImageOverlay( final ImagePlus img, final ImagePlus imp, final Map< String, Object > displaySettings )
	{
		super( 0, 0, imp );
		this.img = img;
		this.imp = imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.displaySettings = displaySettings;
	}

	@Override
	public void drawOverlay( final Graphics g )
	{
		final int xcorner = ic.offScreenX( 0 );
		final int ycorner = ic.offScreenY( 0 );
		final double magn = getMagnification();

		final Graphics2D g2d = ( Graphics2D ) g;
		int frame = imp.getFrame();

		double m00 = magn;
		double m10 = 0;
		double m01 = 0;
		double m11 = magn;
		double m02 = -xcorner * magn;
		double m12 = -ycorner * magn;

		AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f);
		g2d.setComposite(composite);
		g2d.drawImage(img.getStack().getProcessor( frame ).getBufferedImage(),
				new AffineTransform(m00, m10, m01, m11, m02, m12), null);
	}
}
