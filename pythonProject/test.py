import numpy as np
import pandas as pd
from datetime import datetime, timedelta

# Фиксируем seed для воспроизводимости результатов
np.random.seed(42)

# 1. Настраиваем параметры временного ряда (15 000 строк)
n_rows = 15000
start_time = datetime(2026, 1, 1)
timestamps = [start_time + timedelta(minutes=i) for i in range(n_rows)]

# Генерация базового временного паттерна (синусоида)
t = np.linspace(0, 150, n_rows)

# Нормальное поведение: 3 датчика жестко скоррелированы между собой
sensor_1 = np.sin(t) + np.random.normal(0, 0.05, n_rows)
sensor_2 = np.cos(t) + np.random.normal(0, 0.05, n_rows)
# sensor_3 математически зависит от первых двух (линейная комбинация)
sensor_3 = (sensor_1 * 0.6 + sensor_2 * 0.4) + np.random.normal(0, 0.02, n_rows)

# Инициализируем массив меток нулями (норма)
labels = np.zeros(n_rows, dtype=int)

# 2. ИНЪЕКЦИЯ СЛОЖНЫХ АНОМАЛИЙ ДЛЯ ATTENTION МЕХАНИЗМА
# Аномалия 1: Нарушение пространственной корреляции (индексы 3500 - 4100)
# Значения датчиков остаются в стандартных пределах [-1, 1], но ломается связь:
# sensor_3 внезапно инвертируется относительно своей нормальной формулы
sensor_3[3500:4100] = -(sensor_1[3500:4100] * 0.6 + sensor_2[3500:4100] * 0.4) + np.random.normal(0, 0.02, 600)
labels[3500:4100] = 1

# Аномалия 2: Долговременный фазовый сдвиг и замедление частоты (индексы 9000 - 9700)
# Одиночные точки выглядят нормально, но контекст окна полностью меняется
t_anomaly = np.linspace(9000*0.5, 9700*0.5, 700)
sensor_1[9000:9700] = np.sin(t_anomaly) + np.random.normal(0, 0.05, 700)
sensor_2[9000:9700] = np.cos(t_anomaly) + np.random.normal(0, 0.05, 700)
sensor_3[9000:9700] = (sensor_1[9000:9700] * 0.6 + sensor_2[9000:9700] * 0.4) + np.random.normal(0, 0.02, 700)
labels[9000:9700] = 1

# 3. Сборка финального DataFrame и сохранение в CSV
df = pd.DataFrame({
    'timestamp': timestamps,
    'sensor_1': sensor_1,
    'sensor_2': sensor_2,
    'sensor_3': sensor_3,
    'is_anomaly': labels
})

# Сохраняем в один файл без индексов
df.to_csv('multivariate_anomalies.csv', index=False)

print("="*50)
print("ДАТАСЕТ УСПЕШНО СФОРМИРОВАН!")
print("Файл: 'multivariate_anomalies.csv'")
print(f"Размерность: {df.shape[0]} строк, {df.shape[1]} колонок.")
print(f"Количество аномальных точек: {df['is_anomaly'].sum()} (около {df['is_anomaly'].mean()*100:.2f}%)")
print("="*50)