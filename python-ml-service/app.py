from fastapi import FastAPI, HTTPException
from schemas import AnalysisRequest, AnalysisResponse
from service.file_loader import load_dataset
from service.preprocessing import prepare_time_series
from service.windowing import create_sliding_windows
from service.vae_service import train_dense_vae
from service.lstm_vae_service import train_lstm_vae
from service.anomaly import compute_threshold, detect_anomalous_windows
from service.point_mapping import window_errors_to_point_scores, detect_anomalous_points
import numpy as np

app = FastAPI()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze", response_model=AnalysisResponse)
def analyze(request: AnalysisRequest):
    try:
        df = load_dataset(request.filePath, request.fileType)

        prepared = prepare_time_series(
            df=df,
            time_column=request.timeColumn,
            feature_columns=request.featureColumns
        )

        windows, start_indices = create_sliding_windows(
            data=prepared["scaled_values"],
            window_size=request.windowSize,
            stride=request.stride
        )

        vae_result = train_dense_vae(
            windows=windows,
            latent_dim=request.latentDim,
            epochs=request.epochs,
            batch_size=request.batchSize
        )

        errors = vae_result["reconstruction_errors"]

        threshold = compute_threshold(
            errors=errors,
            auto_threshold=request.autoThreshold,
            threshold_value=request.thresholdValue
        )

        anomaly_flags, anomaly_indices = detect_anomalous_windows(
            errors=errors,
            threshold=threshold
        )

        point_scores = window_errors_to_point_scores(
            n_points=len(prepared["dataframe"]),
            window_size=request.windowSize,
            stride=request.stride,
            window_errors=errors
        )

        point_threshold, point_flags, point_indices = detect_anomalous_points(
            point_scores=point_scores,
            auto_threshold=request.autoThreshold,
            threshold_value=request.thresholdValue
        )

        timestamps = [
            ts.isoformat() if hasattr(ts, "isoformat") else str(ts)
            for ts in prepared["timestamps"]
        ]

        feature_series = {
            col: prepared["dataframe"][col].astype(float).tolist()
            for col in prepared["used_features"]
        }

        if request.modelType == "DENSE_VAE":
            vae_result = train_dense_vae(
                windows=windows,
                latent_dim=request.latentDim,
                epochs=request.epochs,
                batch_size=request.batchSize
            )
        elif request.modelType == "LSTM_VAE":
            vae_result = train_lstm_vae(
                windows=windows,
                latent_dim=request.latentDim,
                epochs=request.epochs,
                batch_size=request.batchSize
            )
        else:
            raise ValueError(f"Unsupported model type: {request.modelType}")

        return AnalysisResponse(
            status="COMPLETED",
            message="VAE analysis with point-level scoring completed successfully",
            totalRows=len(prepared["dataframe"]),
            totalFeatures=len(prepared["used_features"]),
            totalWindows=len(windows),
            windowSize=request.windowSize,
            detectedTimeColumn=request.timeColumn,
            usedFeatures=prepared["used_features"],
            meanError=float(np.mean(errors)),
            maxError=float(np.max(errors)),
            threshold=float(threshold),
            anomalyWindowIndices=anomaly_indices,
            pointThreshold=float(point_threshold),
            anomalyPointIndices=point_indices,
            timestamps=timestamps,
            pointScores=[float(x) for x in point_scores.tolist()],
            pointAnomalyFlags=[bool(x) for x in point_flags.tolist()],
            featureSeries=feature_series,
            modelType=request.modelType,
            finalTrainLoss=float(vae_result["history"]["total_loss"][-1]) if "total_loss" in vae_result[
                "history"] else None,

        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))