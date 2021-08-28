// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.smilecoms.fprint.match;

class Parameters {
	static final int BLOCK_SIZE = 15;
	static final double DPI_TOLERANCE = 5;
	static final int HISTOGRAM_DEPTH = 256;
	static final double CLIPPED_CONTRAST = 0.08;
	static final double MIN_ABSOLUTE_CONTRAST = 17 / 255.0;
	static final double MIN_RELATIVE_CONTRAST = 0.34;
	static final int RELATIVE_CONTRAST_SAMPLE = 168568;
	static final double RELATIVE_CONTRAST_PERCENTILE = 0.49;
	static final int MASK_VOTE_RADIUS = 7;
	static final double MASK_VOTE_MAJORITY = 0.51;
	static final int MASK_VOTE_BORDER_DISTANCE = 4;
	static final int BLOCK_ERRORS_VOTE_RADIUS = 1;
	static final double BLOCK_ERRORS_VOTE_MAJORITY = 0.7;
	static final int BLOCK_ERRORS_VOTE_BORDER_DISTANCE = 4;
	static final double MAX_EQUALIZATION_SCALING = 3.99;
	static final double MIN_EQUALIZATION_SCALING = 0.25;
	static final double MIN_ORIENTATION_RADIUS = 2;
	static final double MAX_ORIENTATION_RADIUS = 6;
	static final int ORIENTATION_SPLIT = 50;
	static final int ORIENTATIONS_CHECKED = 20;
	static final int ORIENTATION_SMOOTHING_RADIUS = 1;
	static final int PARALLEL_SMOOTHING_RESOLUTION = 32;
	static final int PARALLEL_SMOOTHING_RADIUS = 7;
	static final double PARALLEL_SMOOTHING_STEP = 1.59;
	static final int ORTHOGONAL_SMOOTHING_RESOLUTION = 11;
	static final int ORTHOGONAL_SMOOTHING_RADIUS = 4;
	static final double ORTHOGONAL_SMOOTHING_STEP = 1.11;
	static final int BINARIZED_VOTE_RADIUS = 2;
	static final double BINARIZED_VOTE_MAJORITY = 0.61;
	static final int BINARIZED_VOTE_BORDER_DISTANCE = 17;
	static final int INNER_MASK_BORDER_DISTANCE = 14;
	static final double MASK_DISPLACEMENT = 10.06;
	static final int MINUTIA_CLOUD_RADIUS = 20;
	static final int MAX_CLOUD_SIZE = 4;
	static final int MAX_MINUTIAE = 100;
	static final int SORT_BY_NEIGHBOR = 5;
	static final int EDGE_TABLE_NEIGHBORS = 9;
	static final int THINNING_ITERATIONS = 26;
	static final int MAX_PORE_ARM = 41;
	static final int SHORTEST_JOINED_ENDING = 7;
	static final int MAX_RUPTURE_SIZE = 5;
	static final int MAX_GAP_SIZE = 20;
	static final int GAP_ANGLE_OFFSET = 22;
	static final int TOLERATED_GAP_OVERLAP = 2;
	static final int MIN_TAIL_LENGTH = 21;
	static final int MIN_FRAGMENT_LENGTH = 22;
	static final int MAX_DISTANCE_ERROR = 13;
	static final double MAX_ANGLE_ERROR = Math.toRadians(10);
	static final double MAX_GAP_ANGLE = Math.toRadians(45);
	static final int RIDGE_DIRECTION_SAMPLE = 21;
	static final int RIDGE_DIRECTION_SKIP = 1;
	static final int MAX_TRIED_ROOTS = 70;
	static final int MIN_ROOT_EDGE_LENGTH = 58;
	static final int MAX_ROOT_EDGE_LOOKUPS = 1633;
	static final int MIN_SUPPORTING_EDGES = 1;
	static final double DISTANCE_ERROR_FLATNESS = 0.69;
	static final double ANGLE_ERROR_FLATNESS = 0.27;
	static final double MINUTIA_SCORE = 0.032;
	static final double MINUTIA_FRACTION_SCORE = 8.98;
	static final double MINUTIA_TYPE_SCORE = 0.629;
	static final double SUPPORTED_MINUTIA_SCORE = 0.193;
	static final double EDGE_SCORE = 0.265;
	static final double DISTANCE_ACCURACY_SCORE = 9.9;
	static final double ANGLE_ACCURACY_SCORE = 2.79;
	static final double THRESHOLD_FMR_MAX = 8.48;
	static final double THRESHOLD_FMR_2 = 11.12;
	static final double THRESHOLD_FMR_10 = 14.15;
	static final double THRESHOLD_FMR_100 = 18.22;
	static final double THRESHOLD_FMR_1000 = 22.39;
	static final double THRESHOLD_FMR_10_000 = 27.24;
	static final double THRESHOLD_FMR_100_000 = 32.01;
}