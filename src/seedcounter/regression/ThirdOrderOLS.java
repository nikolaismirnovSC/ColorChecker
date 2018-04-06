package seedcounter.regression;

import seedcounter.Color;

import java.nio.DoubleBuffer;

public class ThirdOrderOLS extends AbstractOLS implements RegressionModel {
    public ThirdOrderOLS(boolean intercept) {
        super(intercept);
    }

    @Override
    protected double[] getFeatures(DoubleBuffer color) {
        double channel0 = Color.channel(color, 0);
        double channel1 = Color.channel(color, 1);
        double channel2 = Color.channel(color, 2);

        return new double[] {
            channel0, channel1, channel2,
            channel0 * channel0, channel0 * channel1, channel0 * channel2,
            channel1 * channel1, channel1 * channel2, channel2 * channel2,
            channel0 * channel0 * channel0, channel0 * channel0 * channel1,
            channel0 * channel0 * channel2, channel0 * channel1 * channel1,
            channel0 * channel1 * channel2, channel0 * channel2 * channel2,
            channel1 * channel1 * channel1, channel1 * channel1 * channel2,
            channel1 * channel2 * channel2, channel2 * channel2 * channel2,
        };
    }
}
