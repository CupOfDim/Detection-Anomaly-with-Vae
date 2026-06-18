import pandas as pd
from datetime import datetime, timedelta


def prepare_tsb_dataset(input_file_path, output_file_path):
    print("Читаем исходный файл с учетом родных заголовков...")

    # УБРАЛИ header=None. Теперь Pandas понимает, что первая строка — это заголовки
    df = pd.read_csv(input_file_path, sep=None, engine='python')

    # Умное переименование: ищем колонку, содержащую слово 'label' или 'anomaly'
    renamed = False
    for col in df.columns:
        if 'label' in str(col).lower() or 'anomaly' in str(col).lower():
            df.rename(columns={col: 'is_anomaly'}, inplace=True)
            renamed = True
            print(f"[INFO] Родная колонка '{col}' успешно переименована в 'is_anomaly'")
            break

    # Если файл состоял из 2-х колонок и автопоиск не сработал
    if not renamed and df.shape[1] == 2:
        df.columns = ['value', 'is_anomaly']
        print("[INFO] Принудительно установлены имена колонок: ['value', 'is_anomaly']")
    # Если колонок много, переименовываем самую последнюю (там обычно лежит разметка)
    elif not renamed and df.shape[1] > 2:
        last_col = df.columns[-1]
        df.rename(columns={last_col: 'is_anomaly'}, inplace=True)
        print(f"[INFO] Последняя колонка '{last_col}' переименована в 'is_anomaly'")

    # Проверяем, есть ли уже в файле колонка времени
    has_time = any('time' in str(c).lower() or 'date' in str(c).lower() for c in df.columns)

    # Если времени нет — генерируем искусственную ось
    if not has_time:
        start_date = datetime(2026, 1, 1)
        df['datetime'] = [start_date + timedelta(minutes=i) for i in range(len(df))]
        # Сдвигаем datetime на первое место
        cols = ['datetime'] + [c for c in df.columns if c != 'datetime']
        df = df[cols]
        print("[INFO] Успешно добавлена временная ось 'datetime'")

    # Сохраняем в CSV с разделителем ТОЧКА С ЗАПЯТОЙ (;)
    df.to_csv(output_file_path, index=False, sep=';')
    print(f"\n Готово! Чистый файл без сдвигов сохранен: {output_file_path}")
    print(f"Всего валидных строк данных: {len(df)}")


# Укажи имя файла, который ты скачал, и имя файла, который хочешь получить
prepare_tsb_dataset("C:/Users/user/Desktop/archive (2)/TSB-AD-M/057_SMD_id_1_Facility_tr_4529_1st_4629.csv", "ready_to_upload.csv")