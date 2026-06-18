from pydantic import BaseModel
from typing import List, Optional, Dict


class AnalysisRequest(BaseModel):
    taskId: int
    filePath: str
    fileType: str
    timeColumn: str
    featureColumns: List[str]
    windowSize: int
    stride: int
    latentDim: int
    epochs: int
    batchSize: int
    autoThreshold: bool
    thresholdValue: Optional[float] = None
    modelType: str
    labelColumn: Optional[str] = None


class AnalysisResponse(BaseModel):
    status: str
    message: str
    totalRows: int
    totalFeatures: int
    totalWindows: int
    windowSize: int
    detectedTimeColumn: str
    usedFeatures: List[str]

    meanError: float
    maxError: float
    threshold: float
    anomalyWindowIndices: List[int]

    pointThreshold: float
    anomalyPointIndices: List[int]
    timestamps: List[str]
    pointScores: List[float]
    pointAnomalyFlags: List[bool]

    featureSeries: Dict[str, List[float]]

    modelType: str
    finalTrainLoss: float | None = None
    precision: Optional[float] = None
    recall: Optional[float] = None
    f1Score: Optional[float] = None
    rocAuc: Optional[float] = None
    prAuc: Optional[float] = None