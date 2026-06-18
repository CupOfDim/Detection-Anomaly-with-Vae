import numpy as np
import torch
from torch.utils.data import DataLoader, TensorDataset
from models.attention_vae import SequenceVAE


def train_attention_vae(windows: np.ndarray, latent_dim: int, epochs: int, batch_size: int):
    # Автоопределение GPU/CUDA
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[INFO] PyTorch переводит обучение модели на устройство: {device}")

    n_windows, window_size, n_features = windows.shape

    # Инициализация модели и отправка весов на GPU
    vae = SequenceVAE(window_size, n_features, latent_dim, beta=0.1).to(device)
    optimizer = torch.optim.Adam(vae.parameters(), lr=1e-3)

    # Упаковка данных в DataLoader для батчинга в памяти GPU
    tensor_windows = torch.tensor(windows, dtype=torch.float32)
    dataset = TensorDataset(tensor_windows)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    history = {"total_loss": [], "reconstruction_loss": [], "kl_loss": []}

    # --- ЭТАП 1: ХОД ОБУЧЕНИЯ МОДЕЛИ ---
    for epoch in range(epochs):
        vae.train()  # Включаем Dropout для регуляризации
        epoch_total, epoch_recon, epoch_kl = 0.0, 0.0, 0.0
        batches = 0

        for batch in dataloader:
            batch_x = batch[0].to(device)
            optimizer.zero_grad()

            # В PyTorch функции вызываются НАПРЯМУЮ, без .predict()
            reconstruction, z_mean, z_log_var = vae(batch_x)
            total_loss, recon_loss, kl_loss = vae.compute_loss(batch_x, reconstruction, z_mean, z_log_var)

            total_loss.backward()
            optimizer.step()

            epoch_total += total_loss.item()
            epoch_recon += recon_loss.item()
            epoch_kl += kl_loss.item()
            batches += 1

        history["total_loss"].append(epoch_total / batches)
        history["reconstruction_loss"].append(epoch_recon / batches)
        history["kl_loss"].append(epoch_kl / batches)

    # --- ЭТАП 2: ФИНАЛЬНЫЙ ИНФЕРЕНС (Оценка ошибок без градиентов) ---
    vae.eval()  # Отключаем Dropout для точной детекции
    with torch.no_grad():
        x_train_tensor = tensor_windows.to(device)
        reconstructed_list = []

        # Безопасный пакетный расчет, чтобы не перегрузить видеопамять
        for i in range(0, len(windows), batch_size):
            chunk = x_train_tensor[i:i + batch_size]
            recon_chunk, _, _ = vae(chunk)  # Прямой вызов модели без .predict()
            # Выгружаем кусок тензора обратно в ОЗУ компьютера в формате numpy
            reconstructed_list.append(recon_chunk.cpu().numpy())

        reconstructed = np.concatenate(reconstructed_list, axis=0)
        reconstruction_errors = np.mean(np.square(windows - reconstructed), axis=(1, 2))

    return {
        "vae": vae,
        "encoder": vae.encoder,
        "decoder": vae.decoder,
        "history": history,
        "reconstructed": reconstructed,
        "reconstruction_errors": reconstruction_errors
    }