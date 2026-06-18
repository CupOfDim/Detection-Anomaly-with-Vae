from fastapi import FastAPI, HTTPException
from schemas import AnalysisRequest, AnalysisResponse
from service.file_loader import load_dataset
from service.preprocessing import prepare_time_series
from service.windowing import create_sliding_windows
from service.vae_service import train_dense_vae
from service.lstm_vae_service import train_lstm_vae
from service.attention_vae_service import train_attention_vae
from service.anomaly import compute_threshold, detect_anomalous_windows
from service.point_mapping import window_errors_to_point_scores, detect_anomalous_points
import numpy as np
import torch
from sklearn.metrics import precision_score, recall_score, f1_score, roc_auc_score, average_precision_score

app = FastAPI()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze", response_model=AnalysisResponse)
def analyze(request: AnalysisRequest):
    try:
        df = load_dataset(request.filePath, request.fileType)

        labels = None
        if request.labelColumn and request.labelColumn in df.columns:
            labels = df[request.labelColumn].astype(int).values

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

        train_windows = windows
        if labels is not None:
            clean_window_indices = []
            for idx, start in enumerate(start_indices):
                window_labels = labels[start: start + request.windowSize]
                if np.sum(window_labels) == 0:
                    clean_window_indices.append(idx)

            if len(clean_window_indices) > 0:
                train_windows = windows[clean_window_indices]
                print(f"[INFO] Выделено чистых окон для обучения: {len(train_windows)} из {len(windows)}")

        if request.modelType == "DENSE_VAE":
            vae_result = train_dense_vae(
                windows=train_windows,
                latent_dim=request.latentDim,
                epochs=request.epochs,
                batch_size=request.batchSize
            )
        elif request.modelType == "LSTM_VAE":
            vae_result = train_lstm_vae(
                windows=train_windows,
                latent_dim=request.latentDim,
                epochs=request.epochs,
                batch_size=request.batchSize
            )
        elif request.modelType == "ATTENTION_VAE":
            vae_result = train_attention_vae(
                windows=train_windows,
                latent_dim=request.latentDim,
                epochs=request.epochs,
                batch_size=request.batchSize
            )
        else:
            raise ValueError(f"Unsupported model type: {request.modelType}")

        x_all = windows.astype("float32")

        if request.modelType == "ATTENTION_VAE":
            device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
            vae_model = vae_result["vae"]
            vae_model.eval()  # Выключаем Dropout

            with torch.no_grad():
                x_all_tensor = torch.tensor(x_all, dtype=torch.float32).to(device)
                reconstructed_list = []

                # Пакетный расчет предсказаний на GPU
                for i in range(0, len(x_all), request.batchSize):
                    chunk = x_all_tensor[i:i + request.batchSize]
                    recon_chunk, _, _ = vae_model(chunk)
                    reconstructed_list.append(recon_chunk.cpu().numpy())

                reconstructed = np.concatenate(reconstructed_list, axis=0)

        elif request.modelType == "DENSE_VAE":
            encoder = vae_result["encoder"]
            decoder = vae_result["decoder"]
            n_windows, window_size, n_features = x_all.shape
            x_all_flat = x_all.reshape(n_windows, -1)

            z_mean, z_log_var, z = encoder.predict(x_all_flat, verbose=0)
            reconstructed_flat = decoder.predict(z, verbose=0)
            reconstructed = reconstructed_flat.reshape(n_windows, window_size, n_features)
        else:
            # Путь для Keras/TF рекуррентной модели LSTM_VAE
            encoder = vae_result["encoder"]
            decoder = vae_result["decoder"]
            z_mean, z_log_var, z = encoder.predict(x_all, verbose=0)
            reconstructed = decoder.predict(z, verbose=0)

        errors = np.mean(np.square(x_all - reconstructed), axis=(1, 2))

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

        precision_val = None
        recall_val = None
        f1_val = None
        roc_auc_val = None
        pr_auc_val = None

        if labels is not None:
            pred_binary = point_flags.astype(int)

            precision_val = float(precision_score(labels, pred_binary, zero_division=0))
            recall_val = float(recall_score(labels, pred_binary, zero_division=0))
            f1_val = float(f1_score(labels, pred_binary, zero_division=0))

            roc_auc_val = float(roc_auc_score(labels, point_scores))
            pr_auc_val = float(average_precision_score(labels, point_scores))

        timestamps = [
            ts.isoformat() if hasattr(ts, "isoformat") else str(ts)
            for ts in prepared["timestamps"]
        ]

        feature_series = {
            col: prepared["dataframe"][col].astype(float).tolist()
            for col in prepared["used_features"]
        }

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
            precision=precision_val,
            recall=recall_val,
            f1Score=f1_val,
            rocAuc=roc_auc_val,
            prAuc=pr_auc_val
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))