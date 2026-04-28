import numpy as np


def create_sliding_windows(data: np.ndarray, window_size: int, stride: int):
    if window_size <= 0:
        raise ValueError("window_size must be > 0")
    if stride <= 0:
        raise ValueError("stride must be > 0")

    n_rows, n_features = data.shape

    if n_rows < window_size:
        raise ValueError(
            f"Not enough rows to create windows: rows={n_rows}, window_size={window_size}"
        )

    windows = []
    start_indices = []

    for start in range(0, n_rows - window_size + 1, stride):
        end = start + window_size
        windows.append(data[start:end])
        start_indices.append(start)

    windows = np.array(windows)

    return windows, start_indices