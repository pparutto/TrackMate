/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

@Plugin( type = EdgeAnalyzer.class )
public class EdgeAmbiguityAnalyzer extends AbstractEdgeAnalyzer
{

	public static final String KEY = "Edge ambiguity";
	public static final String AMBIGUITY = "AMBIGUITY";
	public static final List< String > FEATURES = new ArrayList<>( 1 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 1 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 1 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 1 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 1 );
	public double linkDistSq;

	static
	{
		FEATURES.add( AMBIGUITY );
		FEATURE_NAMES.put( AMBIGUITY, "Ambiguity" );
		FEATURE_SHORT_NAMES.put( AMBIGUITY, "Ambig." );
		FEATURE_DIMENSIONS.put( AMBIGUITY, Dimension.COST );
		IS_INT.put( AMBIGUITY, Boolean.TRUE );
	}

	public EdgeAmbiguityAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final DefaultWeightedEdge edge, final Model model, final Settings settings )
	{
		final FeatureModel featureModel = model.getFeatureModel();
		final Spot s = model.getTrackModel().getEdgeSource( edge );
		final Spot t = model.getTrackModel().getEdgeTarget( edge );
		final double linkDistSq = Math.pow( (double) settings.trackerSettings.get( KEY_LINKING_MAX_DISTANCE ), 2 );

		int nspots = 0;
		Iterator<Spot> it = model.getSpots().iterator( t.getFeature( "FRAME" ).intValue(), true );
		while ( it.hasNext() )
		{
			Spot ss = it.next();
			if ( s.squareDistanceTo( ss ) <= linkDistSq )
				++nspots;
		}

		featureModel.putEdgeFeature( edge, AMBIGUITY, nspots - 1.0 );
	}
}