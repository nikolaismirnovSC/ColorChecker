package seedcounter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javafx.util.Pair;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;


public class FindColorChecker {
	private Mat referenceImage;
	private MatchingModel matchingModel;
	private MatOfKeyPoint referenceKeypoints;
	private MatOfKeyPoint referenceDescriptors;
	private FeatureDetector detector;
	private DescriptorExtractor extractor;

	public FindColorChecker(String referenceFile, MatchingModel matchingModel) {
		referenceImage = Highgui.imread(referenceFile,
				Highgui.CV_LOAD_IMAGE_ANYCOLOR | Highgui.CV_LOAD_IMAGE_ANYDEPTH);
		this.matchingModel = matchingModel;

		referenceKeypoints = new MatOfKeyPoint();
		detector = FeatureDetector.create(this.matchingModel.getDetector());
		detector.detect(referenceImage, referenceKeypoints);

		referenceDescriptors = new MatOfKeyPoint();
		extractor = DescriptorExtractor.create(this.matchingModel.getExtractor());
		extractor.compute(referenceImage, referenceKeypoints, referenceDescriptors);
	}

	public Quad findColorChecker(Mat image) {
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		detector.detect(image, keypoints);

		MatOfKeyPoint descriptors = new MatOfKeyPoint();
		extractor.compute(image, keypoints, descriptors);

		LinkedList<DMatch> goodMatches = getGoodMatches(descriptors);

		Mat homography = getHomography(keypoints, goodMatches);

		return getQuad(homography);
	}

	private LinkedList<DMatch> getGoodMatches(MatOfKeyPoint descriptors) {
		List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
		DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(
				matchingModel.getMatcher());
		descriptorMatcher.knnMatch(referenceDescriptors, descriptors, matches, 2);

		LinkedList<DMatch> goodMatches = new LinkedList<DMatch>();

		for (MatOfDMatch matofDMatch : matches) {
			DMatch[] dmatcharray = matofDMatch.toArray();
			DMatch m1 = dmatcharray[0];
			DMatch m2 = dmatcharray[1];

			if (m1.distance <= m2.distance * matchingModel.getThreshold()) {
				goodMatches.addLast(m1);
			}
		}

		return goodMatches;
	}

	private Mat getHomography(MatOfKeyPoint keypoints, LinkedList<DMatch> goodMatches) {
		List<KeyPoint> referenceKeypointlist = referenceKeypoints.toList();
		List<KeyPoint> keypointlist = keypoints.toList();

		LinkedList<Point> referencePoints = new LinkedList<>();
		LinkedList<Point> points = new LinkedList<>();

		for (int i = 0; i < goodMatches.size(); i++) {
			referencePoints.addLast(referenceKeypointlist.get(goodMatches.get(i).queryIdx).pt);
			points.addLast(keypointlist.get(goodMatches.get(i).trainIdx).pt);
		}

		MatOfPoint2f referenceMatOfPoint2f = new MatOfPoint2f();
		referenceMatOfPoint2f.fromList(referencePoints);
		MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
		matOfPoint2f.fromList(points);

		return Calib3d.findHomography(referenceMatOfPoint2f, matOfPoint2f, Calib3d.RANSAC, 3);
	}

	private Quad getQuad(Mat homography) {
		Mat referenceCorners = new Mat(4, 1, CvType.CV_32FC2);
		Mat corners = new Mat(4, 1, CvType.CV_32FC2);

		referenceCorners.put(0, 0, new double[]{-0.01 * referenceImage.cols(), -0.01 * referenceImage.rows()});
		referenceCorners.put(1, 0, new double[]{1.01 * referenceImage.cols(), -0.01 * referenceImage.rows()});
		referenceCorners.put(2, 0, new double[]{1.01 * referenceImage.cols(), 1.01 * referenceImage.rows()});
		referenceCorners.put(3, 0, new double[]{-0.01 * referenceImage.cols(), 1.01 * referenceImage.rows()});

		Core.perspectiveTransform(referenceCorners, corners, homography);

		return new Quad(new Point(corners.get(0, 0)),new Point(corners.get(1, 0)),
				new Point(corners.get(2, 0)), new Point(corners.get(3, 0)));
	}

	public void fillColorChecker(Mat image, Quad quad) {
		MatOfPoint points = new MatOfPoint();

		points.fromArray(quad.getPoints());
		Core.fillConvexPoly(image, points, getBackgroundColor(image, quad));
	}

	private Scalar getBackgroundColor(Mat image, Quad quad) {
		List<double[]> colors = new ArrayList<double[]>();
		double sumBlue = 0.0;
		double sumGreen = 0.0;
		double sumRed = 0.0;

		while (colors.size() < 1000) {
			double x = ThreadLocalRandom.current().nextDouble(0.0, image.cols());
			double y = ThreadLocalRandom.current().nextDouble(0.0, image.rows());

			if (image.get((int) x, (int) y) != null && !quad.isInside(new Point(x, y))) {
				colors.add(image.get((int) x, (int) y));
				sumBlue += colors.get(colors.size() - 1)[0];
				sumGreen += colors.get(colors.size() - 1)[1];
				sumRed += colors.get(colors.size() - 1)[2];
			}
		}
		double meanBlue = sumBlue / colors.size();
		double meanGreen = sumGreen / colors.size();
		double meanRed = sumRed / colors.size();

		for (int i = 0; i < 10; ++i) {
			List<double[]> buffer = new ArrayList<double[]>();
			sumBlue = 0.0;
			sumGreen = 0.0;
			sumRed = 0.0;
			for (double[] c : colors) {
				if (Math.abs(c[0] - meanBlue) <= 25 &&
					Math.abs(c[1] - meanGreen) <= 25 &&
					Math.abs(c[2] - meanRed) <= 25) {
					sumBlue += c[0];
					sumGreen += c[1];
					sumRed += c[2];
					buffer.add(c);
				}
			}
			colors = buffer;
			meanBlue = sumBlue / colors.size();
			meanGreen = sumGreen / colors.size();
			meanRed = sumRed / colors.size();
		}

		return new Scalar(meanBlue, meanGreen, meanRed);
	}

	public Mat whiteThreshold(Mat image) {
		Mat result = image.clone();
		Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(result, result, 200.0, 255.0, Imgproc.THRESH_BINARY_INV);

		return result;
	}

	public Mat binarizeSeed(Mat image) {
		List<Pair<Scalar, Scalar>> targetsAndRanges = new LinkedList<Pair<Scalar, Scalar>>(); 
		Scalar target1 = new Scalar(4, 97, 108); // темный цвет зерна в HSV
		Scalar range1 = new Scalar(50, 100, 80); // диапазон для каждого HSV канала
		targetsAndRanges.add(new Pair<Scalar, Scalar>(target1, range1));
					
		Scalar target2 = new Scalar(17, 67, 232); // светлый цвет зерна в HSV
		Scalar range2 = new Scalar(50, 50, 50); // диапазон для каждого HSV канала
		targetsAndRanges.add(new Pair<Scalar, Scalar>(target2, range2));
		HSVBinarization hsv = new HSVBinarization(targetsAndRanges);
		return hsv.apply(image);
	}
}
