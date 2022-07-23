package fiji.plugin.trackmate.action.closegaps;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.action.closegaps.CloseGapsParams.Method;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;

public class CloseGapsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final NumberFormat FORMAT = new DecimalFormat( "#.###" );

	final JButton btnGo;

	private final JRadioButton rdbtnLinearInterpolation;

	private final JRadioButton rdbtnLoG;

	private final JRadioButton rdbtnLoGAutoDiameter;

	private final JRadioButton rdbtnLoGManualDiameter;

	private final JFormattedTextField ftfLoGDiameter;

	private final JRadioButton rdbtnHessianAutoDiameter;

	private final JRadioButton rdbtnHessianDiameterXY;

	private final JFormattedTextField ftfHessianDiameterXY;

	private final JFormattedTextField ftfHessianDiameterZ;

	private final JLabel lblScope;

	private final JRadioButton rdbtnSelectionOnly;

	private final JFormattedTextField ftfSearchRadius;

	private final JSlider sliderChannel;

	public CloseGapsPanel( final CloseGapsParams model, final String units, final int nChannels )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Close gaps in tracks" );
		lblTitle.setFont( BIG_FONT );
		lblTitle.setIcon( Icons.ORANGE_ASTERISK_ICON );
		final GridBagConstraints gbcLblTitle = new GridBagConstraints();
		gbcLblTitle.insets = new Insets( 5, 5, 5, 5 );
		gbcLblTitle.gridx = 0;
		gbcLblTitle.gridy = 0;
		add( lblTitle, gbcLblTitle );

		final GridBagConstraints gbcSeparator2 = new GridBagConstraints();
		gbcSeparator2.fill = GridBagConstraints.BOTH;
		gbcSeparator2.insets = new Insets( 5, 5, 5, 5 );
		gbcSeparator2.gridx = 0;
		gbcSeparator2.gridy = 1;
		add( new JSeparator(), gbcSeparator2 );

		final JLabel lblMethod = new JLabel( "Method" );
		lblMethod.setFont( FONT.deriveFont( Font.BOLD ) );
		final GridBagConstraints gbcLblMethod = new GridBagConstraints();
		gbcLblMethod.insets = new Insets( 5, 5, 5, 5 );
		gbcLblMethod.anchor = GridBagConstraints.WEST;
		gbcLblMethod.gridx = 0;
		gbcLblMethod.gridy = 2;
		add( lblMethod, gbcLblMethod );

		rdbtnLinearInterpolation = new JRadioButton( "Linear interpolation" );
		rdbtnLinearInterpolation.setFont( FONT );
		final GridBagConstraints gbcRdbtnLinearInterpolation = new GridBagConstraints();
		gbcRdbtnLinearInterpolation.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnLinearInterpolation.anchor = GridBagConstraints.WEST;
		gbcRdbtnLinearInterpolation.gridx = 0;
		gbcRdbtnLinearInterpolation.gridy = 3;
		add( rdbtnLinearInterpolation, gbcRdbtnLinearInterpolation );

		rdbtnLoG = new JRadioButton( "Search with LoG detector" );
		rdbtnLoG.setFont( FONT );
		final GridBagConstraints gbcRdbtnLoG = new GridBagConstraints();
		gbcRdbtnLoG.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnLoG.anchor = GridBagConstraints.WEST;
		gbcRdbtnLoG.gridx = 0;
		gbcRdbtnLoG.gridy = 4;
		add( rdbtnLoG, gbcRdbtnLoG );

		final JPanel panelLoGParams = new JPanel();
		final GridBagConstraints gbcPanelLoGParams = new GridBagConstraints();
		gbcPanelLoGParams.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelLoGParams.anchor = GridBagConstraints.EAST;
		gbcPanelLoGParams.fill = GridBagConstraints.VERTICAL;
		gbcPanelLoGParams.gridx = 0;
		gbcPanelLoGParams.gridy = 5;
		add( panelLoGParams, gbcPanelLoGParams );
		final GridBagLayout gblPanelLoGParams = new GridBagLayout();
		gblPanelLoGParams.columnWidths = new int[] { 0, 60, 0, 0 };
		gblPanelLoGParams.rowHeights = new int[] { 0, 0, 0 };
		gblPanelLoGParams.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gblPanelLoGParams.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		panelLoGParams.setLayout( gblPanelLoGParams );

		rdbtnLoGAutoDiameter = new JRadioButton( "Automatic diameter" );
		rdbtnLoGAutoDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcRdbtnLoGAutoDiameter = new GridBagConstraints();
		gbcRdbtnLoGAutoDiameter.anchor = GridBagConstraints.EAST;
		gbcRdbtnLoGAutoDiameter.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnLoGAutoDiameter.gridx = 0;
		gbcRdbtnLoGAutoDiameter.gridy = 0;
		panelLoGParams.add( rdbtnLoGAutoDiameter, gbcRdbtnLoGAutoDiameter );

		rdbtnLoGManualDiameter = new JRadioButton( "Estimated diameter:" );
		rdbtnLoGManualDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcRdbtnLoGManualDiameter = new GridBagConstraints();
		gbcRdbtnLoGManualDiameter.anchor = GridBagConstraints.EAST;
		gbcRdbtnLoGManualDiameter.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnLoGManualDiameter.gridx = 0;
		gbcRdbtnLoGManualDiameter.gridy = 1;
		panelLoGParams.add( rdbtnLoGManualDiameter, gbcRdbtnLoGManualDiameter );

		ftfLoGDiameter = new JFormattedTextField( FORMAT );
		ftfLoGDiameter.setHorizontalAlignment( SwingConstants.RIGHT );
		ftfLoGDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcFtfLoGDiameter = new GridBagConstraints();
		gbcFtfLoGDiameter.insets = new Insets( 5, 5, 5, 5 );
		gbcFtfLoGDiameter.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfLoGDiameter.gridx = 1;
		gbcFtfLoGDiameter.gridy = 1;
		panelLoGParams.add( ftfLoGDiameter, gbcFtfLoGDiameter );

		final JLabel lblLoGDiameterUnit = new JLabel( units );
		lblLoGDiameterUnit.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblLoGDiameterUnit = new GridBagConstraints();
		gbcLblLoGDiameterUnit.insets = new Insets( 5, 5, 5, 5 );
		gbcLblLoGDiameterUnit.gridx = 2;
		gbcLblLoGDiameterUnit.gridy = 1;
		panelLoGParams.add( lblLoGDiameterUnit, gbcLblLoGDiameterUnit );

		final JRadioButton rdbtnHessian = new JRadioButton( "Search with Hessian detector" );
		rdbtnHessian.setFont( FONT );
		final GridBagConstraints gbcRdbtnHessian = new GridBagConstraints();
		gbcRdbtnHessian.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnHessian.anchor = GridBagConstraints.WEST;
		gbcRdbtnHessian.gridx = 0;
		gbcRdbtnHessian.gridy = 6;
		add( rdbtnHessian, gbcRdbtnHessian );

		final JPanel panelHessian = new JPanel();
		final GridBagConstraints gbcPanelHessian = new GridBagConstraints();
		gbcPanelHessian.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelHessian.anchor = GridBagConstraints.EAST;
		gbcPanelHessian.fill = GridBagConstraints.VERTICAL;
		gbcPanelHessian.gridx = 0;
		gbcPanelHessian.gridy = 7;
		add( panelHessian, gbcPanelHessian );
		final GridBagLayout gblPanelHessian = new GridBagLayout();
		gblPanelHessian.columnWidths = new int[] { 0, 60, 0, 0 };
		gblPanelHessian.rowHeights = new int[] { 5, 5, 5, 5 };
		gblPanelHessian.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gblPanelHessian.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelHessian.setLayout( gblPanelHessian );

		rdbtnHessianAutoDiameter = new JRadioButton( "Automatic diameter" );
		rdbtnHessianAutoDiameter.setFont( SMALL_FONT );
		final GridBagConstraints gbcRdbtnHessianAutoDiameter = new GridBagConstraints();
		gbcRdbtnHessianAutoDiameter.anchor = GridBagConstraints.WEST;
		gbcRdbtnHessianAutoDiameter.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnHessianAutoDiameter.gridx = 0;
		gbcRdbtnHessianAutoDiameter.gridy = 0;
		panelHessian.add( rdbtnHessianAutoDiameter, gbcRdbtnHessianAutoDiameter );

		rdbtnHessianDiameterXY = new JRadioButton( "Estimated XY diameter:" );
		rdbtnHessianDiameterXY.setFont( SMALL_FONT );
		final GridBagConstraints gbcRdbtnHessianDiameterXY = new GridBagConstraints();
		gbcRdbtnHessianDiameterXY.anchor = GridBagConstraints.WEST;
		gbcRdbtnHessianDiameterXY.insets = new Insets( 5, 5, 5, 5 );
		gbcRdbtnHessianDiameterXY.gridx = 0;
		gbcRdbtnHessianDiameterXY.gridy = 1;
		panelHessian.add( rdbtnHessianDiameterXY, gbcRdbtnHessianDiameterXY );

		ftfHessianDiameterXY = new JFormattedTextField( FORMAT );
		ftfHessianDiameterXY.setHorizontalAlignment( SwingConstants.RIGHT );
		ftfHessianDiameterXY.setFont( SMALL_FONT );
		final GridBagConstraints gbcFtfHessianDiameterXY = new GridBagConstraints();
		gbcFtfHessianDiameterXY.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfHessianDiameterXY.insets = new Insets( 5, 5, 5, 5 );
		gbcFtfHessianDiameterXY.gridx = 1;
		gbcFtfHessianDiameterXY.gridy = 1;
		panelHessian.add( ftfHessianDiameterXY, gbcFtfHessianDiameterXY );

		final JLabel lblHessianDiameterXYUnit = new JLabel( units );
		lblHessianDiameterXYUnit.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblHessianDiameterXYUnit = new GridBagConstraints();
		gbcLblHessianDiameterXYUnit.insets = new Insets( 5, 5, 5, 5 );
		gbcLblHessianDiameterXYUnit.gridx = 2;
		gbcLblHessianDiameterXYUnit.gridy = 1;
		panelHessian.add( lblHessianDiameterXYUnit, gbcLblHessianDiameterXYUnit );

		final JLabel lblHessianDiameterZ = new JLabel( "Estimated Z diameter: " );
		lblHessianDiameterZ.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblHessianDiameterZ = new GridBagConstraints();
		gbcLblHessianDiameterZ.insets = new Insets( 5, 5, 5, 5 );
		gbcLblHessianDiameterZ.anchor = GridBagConstraints.EAST;
		gbcLblHessianDiameterZ.gridx = 0;
		gbcLblHessianDiameterZ.gridy = 2;
		panelHessian.add( lblHessianDiameterZ, gbcLblHessianDiameterZ );

		ftfHessianDiameterZ = new JFormattedTextField( FORMAT );
		ftfHessianDiameterZ.setHorizontalAlignment( SwingConstants.RIGHT );
		ftfHessianDiameterZ.setFont( SMALL_FONT );
		final GridBagConstraints gbcFtfHessianDiameterZ = new GridBagConstraints();
		gbcFtfHessianDiameterZ.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfHessianDiameterZ.insets = new Insets( 5, 5, 5, 5 );
		gbcFtfHessianDiameterZ.gridx = 1;
		gbcFtfHessianDiameterZ.gridy = 2;
		panelHessian.add( ftfHessianDiameterZ, gbcFtfHessianDiameterZ );

		final JLabel lblHessianDiameterZUnit = new JLabel( units );
		lblHessianDiameterZUnit.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblHessianDiameterZUnit = new GridBagConstraints();
		gbcLblHessianDiameterZUnit.gridx = 2;
		gbcLblHessianDiameterZUnit.gridy = 2;
		gbcLblHessianDiameterZUnit.insets = new Insets( 5, 5, 5, 5 );
		panelHessian.add( lblHessianDiameterZUnit, gbcLblHessianDiameterZUnit );

		final JPanel panelSearchRadius = new JPanel();
		final FlowLayout fl1 = ( FlowLayout ) panelSearchRadius.getLayout();
		fl1.setAlignment( FlowLayout.LEFT );
		final GridBagConstraints gbcPanelSearchRadius = new GridBagConstraints();
		gbcPanelSearchRadius.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelSearchRadius.fill = GridBagConstraints.BOTH;
		gbcPanelSearchRadius.gridx = 0;
		gbcPanelSearchRadius.gridy = 8;
		add( panelSearchRadius, gbcPanelSearchRadius );

		final JLabel lbSearchRadius = new JLabel( "Search radius:" );
		lbSearchRadius.setFont( SMALL_FONT );
		panelSearchRadius.add( lbSearchRadius );

		ftfSearchRadius = new JFormattedTextField();
		ftfSearchRadius.setFont( SMALL_FONT );
		ftfSearchRadius.setHorizontalAlignment( SwingConstants.RIGHT );
		ftfSearchRadius.setPreferredSize( new Dimension( 60, 20 ) );
		panelSearchRadius.add( ftfSearchRadius );

		final JLabel lblSearchRadiusUnits = new JLabel( "in units of spot diameter" );
		panelSearchRadius.add( lblSearchRadiusUnits );

		final JPanel panelChannel = new JPanel();
		final FlowLayout fl2 = ( FlowLayout ) panelChannel.getLayout();
		fl2.setAlignment( FlowLayout.LEFT );
		final GridBagConstraints gbcPanelChannel = new GridBagConstraints();
		gbcPanelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelChannel.fill = GridBagConstraints.BOTH;
		gbcPanelChannel.gridx = 0;
		gbcPanelChannel.gridy = 9;
		add( panelChannel, gbcPanelChannel );

		final JLabel lblDetectInChannel = new JLabel( "Detect in channel:" );
		lblDetectInChannel.setFont( SMALL_FONT );
		panelChannel.add( lblDetectInChannel );

		sliderChannel = new JSlider();
		panelChannel.add( sliderChannel );

		final JLabel lblChannel = new JLabel( "1" );
		lblChannel.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
		panelChannel.add( lblChannel );

		final GridBagConstraints gbcSeparator = new GridBagConstraints();
		gbcSeparator.fill = GridBagConstraints.BOTH;
		gbcSeparator.insets = new Insets( 5, 5, 5, 5 );
		gbcSeparator.gridx = 0;
		gbcSeparator.gridy = 10;
		add( new JSeparator(), gbcSeparator );

		lblScope = new JLabel( "Close gaps for:" );
		lblScope.setFont( FONT.deriveFont( Font.BOLD ) );
		final GridBagConstraints gbcLblScope = new GridBagConstraints();
		gbcLblScope.anchor = GridBagConstraints.WEST;
		gbcLblScope.insets = new Insets( 5, 5, 5, 5 );
		gbcLblScope.gridx = 0;
		gbcLblScope.gridy = 11;
		add( lblScope, gbcLblScope );

		final JRadioButton rdbtnAllEdges = new JRadioButton( "All edges" );
		rdbtnAllEdges.setFont( SMALL_FONT );
		final GridBagConstraints gbcRdbtnAllEdges = new GridBagConstraints();
		gbcRdbtnAllEdges.anchor = GridBagConstraints.WEST;
		gbcRdbtnAllEdges.insets = new Insets( 5, 25, 5, 5 );
		gbcRdbtnAllEdges.gridx = 0;
		gbcRdbtnAllEdges.gridy = 12;
		add( rdbtnAllEdges, gbcRdbtnAllEdges );

		rdbtnSelectionOnly = new JRadioButton( "Edges in the current selection" );
		rdbtnSelectionOnly.setFont( SMALL_FONT );
		final GridBagConstraints gbcRdbtnSelectionOnly = new GridBagConstraints();
		gbcRdbtnSelectionOnly.anchor = GridBagConstraints.WEST;
		gbcRdbtnSelectionOnly.insets = new Insets( 5, 25, 5, 5 );
		gbcRdbtnSelectionOnly.gridx = 0;
		gbcRdbtnSelectionOnly.gridy = 13;
		add( rdbtnSelectionOnly, gbcRdbtnSelectionOnly );

		final GridBagConstraints gbcSeparator3 = new GridBagConstraints();
		gbcSeparator3.fill = GridBagConstraints.BOTH;
		gbcSeparator3.insets = new Insets( 5, 5, 5, 5 );
		gbcSeparator3.gridx = 0;
		gbcSeparator3.gridy = 14;
		add( new JSeparator(), gbcSeparator3 );

		btnGo = new JButton( "Start" );
		btnGo.setFont( FONT );
		final GridBagConstraints gbcBtnGo = new GridBagConstraints();
		gbcBtnGo.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnGo.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnGo.gridx = 0;
		gbcBtnGo.gridy = 15;
		add( btnGo, gbcBtnGo );

		/*
		 * Button groups.
		 */

		final ButtonGroup methodGroup = new ButtonGroup();
		methodGroup.add( rdbtnLinearInterpolation );
		methodGroup.add( rdbtnLoG );
		methodGroup.add( rdbtnHessian );

		final ButtonGroup logDiameterGroup = new ButtonGroup();
		logDiameterGroup.add( rdbtnLoGAutoDiameter );
		logDiameterGroup.add( rdbtnLoGManualDiameter );

		final ButtonGroup hessianDiameterGroup = new ButtonGroup();
		hessianDiameterGroup.add( rdbtnHessianAutoDiameter );
		hessianDiameterGroup.add( rdbtnHessianDiameterXY );

		final ButtonGroup scopeGroup = new ButtonGroup();
		scopeGroup.add( rdbtnAllEdges );
		scopeGroup.add( rdbtnSelectionOnly );

		/*
		 * Deal with channels: the slider and channel labels are only visible if
		 * we find more than one channel.
		 */
		sliderChannel.setMaximum( nChannels );
		sliderChannel.setMinimum( 1 );

		if ( nChannels <= 1 )
		{
			lblChannel.setVisible( false );
			lblDetectInChannel.setVisible( false );
			sliderChannel.setVisible( false );
		}
		else
		{
			lblChannel.setVisible( true );
			lblDetectInChannel.setVisible( true );
			sliderChannel.setVisible( true );
		}

		/*
		 * Listeners and stuff.
		 */

		sliderChannel.addChangeListener( e -> lblChannel.setText( "" + sliderChannel.getValue() ) );
		GuiUtils.selectAllOnFocus( ftfSearchRadius );
		GuiUtils.selectAllOnFocus( ftfLoGDiameter );
		GuiUtils.selectAllOnFocus( ftfHessianDiameterXY );
		GuiUtils.selectAllOnFocus( ftfHessianDiameterZ );

		/*
		 * Set values from model.
		 */

		switch ( model.method )
		{
		case LINEAR_INTERPOLATION:
			rdbtnLinearInterpolation.setSelected( true );
			break;
		case LOG_DETECTOR:
			rdbtnLoG.setSelected( true );
			break;
		case HESSIAN_DETECTOR:
			rdbtnHessian.setSelected( true );
			break;
		default:
			throw new IllegalArgumentException( "Unknown gap-closing method: " + model.method );
		}
		ftfSearchRadius.setValue( Double.valueOf( model.searchRadius / 2. ) );
		sliderChannel.setValue( model.sourceChannel + 1 ); // 1-based
		rdbtnLoGAutoDiameter.setSelected( model.logAutoRadius );
		rdbtnLoGManualDiameter.setSelected( !model.logAutoRadius );
		ftfLoGDiameter.setValue( Double.valueOf( model.logRadius * 2. ) );
		rdbtnHessianAutoDiameter.setSelected( model.hessianAutoRadius );
		rdbtnHessianDiameterXY.setSelected( !model.hessianAutoRadius );
		ftfHessianDiameterXY.setValue( Double.valueOf( model.hessianRadiusXY * 2. ) );
		ftfHessianDiameterZ.setValue( Double.valueOf( model.hessianRadiusZ * 2. ) );
		rdbtnSelectionOnly.setSelected( model.selectionOnly );
		rdbtnAllEdges.setSelected( !model.selectionOnly );

		/*
		 * Visibility listeners.
		 */

		final ItemListener logItemsVisibilityListener = e -> {
			final boolean selected = rdbtnLoG.isSelected();
			ftfLoGDiameter.setEnabled( selected );
			rdbtnLoGAutoDiameter.setEnabled( selected );
			rdbtnLoGManualDiameter.setEnabled( selected );
			final boolean anyOf = selected || rdbtnHessian.isSelected();
			ftfSearchRadius.setEnabled( anyOf );
			sliderChannel.setEnabled( anyOf );
		};

		final ItemListener hessianItemsVisibilityListener = e -> {
			final boolean selected = rdbtnHessian.isSelected();
			ftfHessianDiameterXY.setEnabled( selected );
			ftfHessianDiameterZ.setEnabled( selected );
			rdbtnHessianAutoDiameter.setEnabled( selected );
			rdbtnHessianDiameterXY.setEnabled( selected );
			final boolean anyOf = selected || rdbtnLoG.isSelected();
			ftfSearchRadius.setEnabled( anyOf );
			sliderChannel.setEnabled( anyOf );
		};

		rdbtnLoG.addItemListener( logItemsVisibilityListener );
		rdbtnHessian.addItemListener( hessianItemsVisibilityListener );
		logItemsVisibilityListener.itemStateChanged( null );
		hessianItemsVisibilityListener.itemStateChanged( null );
	}

	public CloseGapsParams getParams()
	{
		final Method method = rdbtnLinearInterpolation.isSelected()
				? Method.LINEAR_INTERPOLATION
				: rdbtnLoG.isSelected()
						? Method.LOG_DETECTOR
						: Method.HESSIAN_DETECTOR;

		return CloseGapsParams.create()
				.method( method )
				.searchRadius( ( ( Number ) ftfSearchRadius.getValue() ).doubleValue() * 2. )
				.sourceChannel( sliderChannel.getValue() )
				.logAutoRadius( rdbtnLoGAutoDiameter.isSelected() )
				.logRadius( ( ( Number ) ftfLoGDiameter.getValue() ).doubleValue() / 2. )
				.hessianAutoRadius( rdbtnHessianAutoDiameter.isSelected() )
				.hessianRadiusXY( ( ( Number ) ftfHessianDiameterXY.getValue() ).doubleValue() / 2. )
				.hessianRadiusZ( ( ( Number ) ftfHessianDiameterZ.getValue() ).doubleValue() / 2. )
				.selectionOnly( rdbtnSelectionOnly.isSelected() )
				.get();
	}
}
