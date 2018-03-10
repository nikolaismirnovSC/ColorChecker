package seedcounter.regression;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

import seedcounter.Color;

public abstract class AbstractXYZ extends AbstractOLSMLR implements RegressionModel {
	private double[] xBeta;
	private double[] yBeta;
	private double[] zBeta;
	private double aic;

	AbstractXYZ(boolean intercept) {
		super(intercept);
	}

	@Override
	public void train(List<Color> train, List<Color> answers) {
		List<Double> xAnswers = new ArrayList<>();
		List<Double> yAnswers = new ArrayList<>();
		List<Double> zAnswers = new ArrayList<>();

		for (Color c : answers) {
			xAnswers.add(c.X());
			yAnswers.add(c.Y());
			zAnswers.add(c.Z());
		}

		RegressionResult result = trainChannel(train, xAnswers);
		xBeta = result.getFeatures();
		aic = result.getAIC();

		result = trainChannel(train, yAnswers);
		yBeta = result.getFeatures();
		aic += result.getAIC();

		result = trainChannel(train, zAnswers);
		zBeta = result.getFeatures();
		aic += result.getAIC();
	}

	@Override
	public void calibrate(DoubleBuffer color) {
		double[] features = getFeatures(Color.ofBGR(color));
		color.put(Color.ofXYZ(new double[] {getEstimate(features, xBeta),
			getEstimate(features, yBeta), getEstimate(features, zBeta)}).toBGR());
	}

	@Override
	public double getAIC() {
		return aic;
	}

	@Override
	protected double[] getFeatures(Color color) {
		return getFeatures(color.toXYZ());
	}
}