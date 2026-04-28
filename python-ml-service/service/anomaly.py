import numpy as np


def compute_threshold(errors: np.ndarray, auto_threshold: bool, threshold_value: float | None):
    if auto_threshold:
        return float(np.percentile(errors, 95))

    if threshold_value is None:
        raise ValueError("threshold_value must be provided when auto_threshold is False")

    return float(threshold_value)


def detect_anomalous_windows(errors: np.ndarray, threshold: float):
    anomaly_flags = errors > threshold
    anomaly_indices = np.where(anomaly_flags)[0].tolist()

    return anomaly_flags, anomaly_indices