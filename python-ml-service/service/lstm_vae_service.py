import numpy as np
from models.lstm_vae import build_lstm_vae


def train_lstm_vae(windows: np.ndarray, latent_dim: int, epochs: int, batch_size: int):
    n_windows, window_size, n_features = windows.shape
    x_train = windows.astype("float32")

    vae, encoder, decoder = build_lstm_vae(
        window_size=window_size,
        n_features=n_features,
        latent_dim=latent_dim
    )

    vae.compile(optimizer="adam")

    history = vae.fit(
        x_train,
        epochs=epochs,
        batch_size=batch_size,
        verbose=0
    )

    z_mean, z_log_var, z = encoder.predict(x_train, verbose=0)
    reconstructed = decoder.predict(z, verbose=0)

    reconstruction_errors = np.mean(np.square(x_train - reconstructed), axis=(1, 2))

    return {
        "vae": vae,
        "encoder": encoder,
        "decoder": decoder,
        "history": history.history,
        "reconstructed": reconstructed,
        "reconstruction_errors": reconstruction_errors
    }