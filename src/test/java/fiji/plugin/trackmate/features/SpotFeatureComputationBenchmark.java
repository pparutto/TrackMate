package fiji.plugin.trackmate.features;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotMorphologyAnalyzerProvider;
import ij.ImagePlus;
import net.imglib2.util.Util;

public class SpotFeatureComputationBenchmark
{

	public static void main( final String[] args )
	{
		final int N_TESTS = 5;

		final File file = new File( SpotFeatureComputationBenchmark.class.getResource( "LabelImage.xml" ).getFile() );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );
		final TrackMate trackmate = new TrackMate( model, settings );
		System.out.println( "Reading done: " + trackmate );
		System.out.println( "Computing the features of " + model.getSpots().getNSpots( false ) + " spots:" );

		trackmate.setNumThreads( 1 );
		model.setLogger( Logger.VOID_LOGGER );
		
		
		final List< SpotAnalyzerFactoryBase< ? > > factories = new ArrayList<>();

		final SpotAnalyzerProvider provider1 = new SpotAnalyzerProvider( 1 );
		for ( final String key : provider1.getVisibleKeys() )
			factories.add( provider1.getFactory( key ) );

		final SpotMorphologyAnalyzerProvider provider2 = new SpotMorphologyAnalyzerProvider( 1 );
		for ( final String key : provider2.getVisibleKeys() )
			factories.add( provider2.getFactory( key ) );
		

		for ( final SpotAnalyzerFactoryBase< ? > factory : factories )
		{
			settings.clearSpotAnalyzerFactories();
			settings.addSpotAnalyzerFactory( factory );

			System.out.println( "\nFACTORY: " + factory.getName() );
			final double[] durations = new double[ N_TESTS ];
			for ( int i = 0; i < N_TESTS; i++ )
			{
//				System.out.println( "\nTest #" + ( i + 1 ) );
				final long start = System.currentTimeMillis();
				trackmate.computeSpotFeatures( false );
				final long end = System.currentTimeMillis();
				final double duration = ( end - start ) / 1000.;
				durations[ i ] = duration;
//				System.out.println( String.format( "  completed in %.1f s.", duration ) );
			}
			System.out.println( String.format( "Median over %d tests: %.1f s", N_TESTS, Util.median( durations ) ) );
		}
	}
}
