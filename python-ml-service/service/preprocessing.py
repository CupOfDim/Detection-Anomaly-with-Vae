import pandas as pd
from sklearn.preprocessing import StandardScaler


def validate_columns(
        df: pd.DataFrame,
        time_column: str,
        feature_columns: list[str]) -> None:

    if time_column not in df.columns:
        raise ValueError(f"Time column '{time_column}' not found in dataset")

    missing_features = [col for col in feature_columns if col not in df.columns]
    if missing_features:
        raise ValueError(f"Feature columns not found: {missing_features}")


def prepare_time_series(
    df: pd.DataFrame,
    time_column: str,
    feature_columns: list[str]
):
    feature_columns = [col for col in feature_columns if col != time_column]
    if not feature_columns:
        raise ValueError("No valid feature columns selected")

    validate_columns(df, time_column, feature_columns)

    working_df = df[[time_column] + feature_columns].copy()

    working_df[time_column] = pd.to_datetime(working_df[time_column], errors="coerce")
    working_df = working_df.dropna(subset=[time_column])

    for col in feature_columns:
        working_df[col] = pd.to_numeric(working_df[col], errors="coerce")

    working_df = working_df.sort_values(by=time_column).reset_index(drop=True)

    working_df[feature_columns] = working_df[feature_columns].interpolate(method="linear", limit_direction="both")
    working_df[feature_columns] = working_df[feature_columns].ffill().bfill()

    working_df = working_df.dropna(subset=feature_columns).reset_index(drop=True)

    if len(working_df) == 0:
        raise ValueError("No valid rows left after preprocessing")

    scaler = StandardScaler()
    scaled_values = scaler.fit_transform(working_df[feature_columns])

    timestamps = working_df[time_column].tolist()

    return {
        "dataframe": working_df,
        "timestamps": timestamps,
        "scaled_values": scaled_values,
        "scaler": scaler,
        "used_features": feature_columns
    }