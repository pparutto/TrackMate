package fiji.plugin.trackmate.features.spot;

import org.apache.commons.math4.analysis.ParametricUnivariateFunction;
import org.apache.commons.math4.fitting.AbstractCurveFitter;
import org.apache.commons.math4.fitting.WeightedObservedPoint;
import org.apache.commons.math4.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math4.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.linear.DiagonalMatrix;
import static fiji.plugin.trackmate.features.spot.SpotGaussianAnalyzerFactory.GAUSS_COEFF;
import static fiji.plugin.trackmate.features.spot.SpotGaussianAnalyzerFactory.GAUSS_GOF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;

public class SpotGaussianAnalyzer< T extends RealType< T >> extends IndependentSpotFeatureAnalyzer< T >
{
	public static double computeR(double[] p, double[] mu)
	{
		return Math.sqrt(Math.pow(p[0] - mu[0], 2) + Math.pow(p[1] - mu[1], 2));
	}

	class Gauss2DPolar implements ParametricUnivariateFunction
	{
		protected double s;

		public Gauss2DPolar(double s)
		{
			this.s = s;
		}

		public double value(double r, double... ps)
		{
			return ps[0] * Math.exp(-1/(2 * this.s * this.s) * r * r);
		}


		public double[] gradient(double r, double... ps)
		{
			return new double[] {Math.exp(-1/(2 * this.s * this.s) * r * r)};
		}
	}

	public class Gauss2DPolarFitter extends AbstractCurveFitter
	{
		protected double s;
		protected LevenbergMarquardtOptimizer optim;

		public Gauss2DPolarFitter(double s)
		{
			this.s = s;
			this.optim = new LevenbergMarquardtOptimizer();
		}

		@Override
		protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points)
		{
			final int len = points.size();
			final double[] target  = new double[len];
			final double[] weights = new double[len];
			final double[] initialGuess = { 1.0 };

			int i = 0;
			for(WeightedObservedPoint point : points)
			{
				target[i]  = point.getY();
				weights[i] = point.getWeight();
				i += 1;
			}

			final AbstractCurveFitter.TheoreticalValuesFunction model = new
				AbstractCurveFitter.TheoreticalValuesFunction(new Gauss2DPolar(s), points);

			return new LeastSquaresBuilder().
				maxEvaluations(Integer.MAX_VALUE).
				maxIterations(Integer.MAX_VALUE).
				start(initialGuess).
				target(target).
				weight(new DiagonalMatrix(weights)).
				model(model.getModelFunction(), model.getModelFunctionJacobian()).
				build();
		}

		public Optimum myfit(Collection<WeightedObservedPoint> points)
		{
			return this.optim.optimize(getProblem(points));
		}
	}

	public SpotGaussianAnalyzer( final ImgPlus< T > img, final Iterator< Spot > spots )
	{
		super( img, spots );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Fit 2D Gaussian curve to spot intensity
	 */
	@Override
	public final void process( final Spot spot )
	{
		final double[] clb = TMUtils.getSpatialCalibration( img );
		// Prepare neighborhood
		final SpotNeighborhood< T > neighborhood = new SpotNeighborhood<>( spot, img );
		final int npixels = ( int ) neighborhood.size();

		if ( npixels <= 1 )
		{
			/*
			 * Hack around a bug in spot iterator causing it to never end if the
			 * size of the spot is lower than one pixel.
			 */
			spot.putFeature( GAUSS_COEFF, Double.NaN );
			spot.putFeature( GAUSS_GOF, Double.NaN );
			return;
		}

		SpotNeighborhoodCursor< T > cursor = neighborhood.cursor();
		double cur_min = Double.NaN;
		double cur_max = Double.NaN;
		do
		{
			double v = cursor.get().getRealDouble();
			cur_min = (Double.isNaN(cur_min) || v < cur_min) ? v : cur_min;
			cur_max = (Double.isNaN(cur_max) || v > cur_max) ? v : cur_max;

			cursor.next();
		} while (cursor.hasNext());

		cursor = neighborhood.cursor();
		ArrayList<WeightedObservedPoint> pts = new ArrayList<> ();
		do
		{
			double v = cursor.get().getRealDouble();
//			System.out.println(String.format("%g %g",  computeR(
//					new double[] {clb[0] * cursor.getDoublePosition(0), clb[1] * cursor.getDoublePosition(1)},
//					new double[] {spot.getFeature("POSITION_X"), spot.getFeature("POSITION_Y")}), v - cur_min));
			pts.add(new WeightedObservedPoint(1.0, computeR(
					new double[] {clb[0] * cursor.getDoublePosition(0), clb[1] * cursor.getDoublePosition(1)},
					new double[] {spot.getFeature("POSITION_X"), spot.getFeature("POSITION_Y")}),
									v - cur_min));

			cursor.next();
		} while (cursor.hasNext());


		double s = spot.getFeature("RADIUS") / (2 * Math.sqrt(5.991));

		Gauss2DPolarFitter fitter = new Gauss2DPolarFitter(s);
		final Optimum opt = fitter.myfit(pts);

		double[] coeffs = opt.getPoint().toArray();
		double RMSE = opt.getRMS() / (cur_max - cur_min);
		//System.out.println(String.format("%g %g   %g", s, coeffs[0], RMSE));

		spot.putFeature( GAUSS_COEFF, coeffs[0] );
		spot.putFeature( GAUSS_GOF, RMSE );
	}
}
