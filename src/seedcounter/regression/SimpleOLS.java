package seedcounter.regression;

import java.nio.DoubleBuffer;

public class SimpleOLS extends AbstractOLS implements RegressionModel {
	public SimpleOLS(boolean intercept) {
		super(intercept);
	}

	@Override
	protected double[] getFeatures(DoubleBuffer color) {
		double channel1 = color.get(color.position());
		double channel2 = color.get(color.position() + 1);
		double channel3 = color.get(color.position() + 2);

		return new double[] {
			channel1, channel2, channel3
		};
	}
}