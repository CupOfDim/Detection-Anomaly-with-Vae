import pandas as pd

def load_dataset(file_path: str, file_type: str) -> pd.DataFrame:
    file_type = file_type.lower()

    if file_type== "csv":
        return pd.read_csv(file_path, sep=None, engine='python')
    elif file_type.lower() in ["xls", "xlsx"]:
        return pd.read_excel(file_path)

    raise ValueError(f"Неподдерживаемый формат данных: {file_type}")