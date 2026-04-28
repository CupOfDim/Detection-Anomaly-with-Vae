import numpy as np


def window_errors_to_point_scores(
    n_points: int,
    window_size: int,
    stride: int,
    window_errors: np.ndarray
):
    point_score_sums = np.zeros(n_points, dtype=float)
    point_score_counts = np.zeros(n_points, dtype=float)

    window_index = 0
    for start in range(0, n_points - window_size + 1, stride):
        end = start + window_size
        error = float(window_errors[window_index])

        point_score_sums[start:end] += error
        point_score_counts[start:end] += 1.0

        window_index += 1

    point_score_counts[point_score_counts == 0] = 1.0
    point_scores = point_score_sums / point_score_counts

    return point_scores


def detect_anomalous_points(point_scores: np.ndarray, auto_threshold: bool, threshold_value: float | None):
    if auto_threshold:
        threshold = float(np.percentile(point_scores, 95))
    else:
        if threshold_value is None:
            raise ValueError("threshold_value must be provided when auto_threshold is False")
        threshold = float(threshold_value)

    point_flags = point_scores > threshold
    point_indices = np.where(point_flags)[0].tolist()

    return threshold, point_flags, point_indices