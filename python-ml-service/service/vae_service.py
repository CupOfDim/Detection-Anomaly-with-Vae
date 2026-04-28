import numpy as np
from models.dense_vae import build_dense_vae


def train_dense_vae(windows: np.ndarray, latent_dim: int, epochs: int, batch_size: int):
    n_windows, window_size, n_features = windows.shape
    input_dim = window_size * n_features

    x_train = windows.reshape(n_windows, input_dim).astype("float32")

    vae, encoder, decoder = build_dense_vae(input_dim=input_dim, latent_dim=latent_dim)
    vae.compile(optimizer="adam")

    history = vae.fit(
        x_train,
        epochs=epochs,
        batch_size=batch_size,
        verbose=0
    )

    z_mean, z_log_var, z = encoder.predict(x_train, verbose=0)
    reconstructed = decoder.predict(z, verbose=0)

    reconstruction_errors = np.mean(np.square(x_train - reconstructed), axis=1)

    return {
        "vae": vae,
        "encoder": encoder,
        "decoder": decoder,
        "history": history.history,
        "reconstructed": reconstructed,
        "reconstruction_errors": reconstruction_errors,
        "input_dim": input_dim
    }