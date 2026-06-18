import torch
import torch.nn as nn
import torch.nn.functional as F


class Sampling(nn.Module):
    """
    Аналог слоя Sampling в Keras.
    Выполняет репараметризацию (Reparameterization Trick) для VAE.
    """

    def forward(self, z_mean, z_log_var):
        epsilon = torch.randn_like(z_mean)
        return z_mean + torch.exp(0.5 * z_log_var) * epsilon


class AttentionBlock(nn.Module):
    """
    Блок Self-Attention с нормализацией слоев и Feed-Forward сетью.
    """

    def __init__(self, model_dim=64, num_heads=2, ff_dim=128, dropout=0.1):
        super().__init__()
        # batch_first=True обеспечивает работу с формой (batch_size, window_size, features)
        self.mha = nn.MultiheadAttention(embed_dim=model_dim, num_heads=num_heads, dropout=dropout, batch_first=True)
        self.norm1 = nn.LayerNorm(model_dim)

        self.ffn = nn.Sequential(
            nn.Linear(model_dim, ff_dim),
            nn.ReLU(),
            nn.Linear(ff_dim, model_dim)
        )
        self.norm2 = nn.LayerNorm(model_dim)

    def forward(self, x):
        # В режиме самовнимания Query, Key, Value — это один и тот же тензор x
        attn_output, _ = self.mha(x, x, x)
        x = self.norm1(x + attn_output)

        ff_output = self.ffn(x)
        x = self.norm2(x + ff_output)
        return x


class AttentionEncoder(nn.Module):
    def __init__(self, window_size, n_features, latent_dim, model_dim=64):
        super().__init__()
        self.input_dense = nn.Linear(n_features, model_dim)
        self.block1 = AttentionBlock(model_dim, num_heads=2, ff_dim=128)
        self.block2 = AttentionBlock(model_dim, num_heads=2, ff_dim=128)

        self.dense_hidden = nn.Linear(model_dim, 64)
        self.z_mean = nn.Linear(64, latent_dim)
        self.z_log_var = nn.Linear(64, latent_dim)
        self.sampling = Sampling()

    def forward(self, x):
        x = self.input_dense(x)
        x = self.block1(x)
        x = self.block2(x)

        # Аналог GlobalAveragePooling1D (усреднение по временной оси окон)
        x = torch.mean(x, dim=1)

        x = F.relu(self.dense_hidden(x))
        mean = self.z_mean(x)
        log_var = self.z_log_var(x)
        z = self.sampling(mean, log_var)
        return mean, log_var, z


class AttentionDecoder(nn.Module):
    def __init__(self, window_size, n_features, latent_dim, model_dim=64):
        super().__init__()
        self.window_size = window_size
        self.model_dim = model_dim

        self.fc_input = nn.Linear(latent_dim, window_size * model_dim)
        self.block1 = AttentionBlock(model_dim, num_heads=2, ff_dim=128)
        self.block2 = AttentionBlock(model_dim, num_heads=2, ff_dim=128)
        self.output_dense = nn.Linear(model_dim, n_features)

    def forward(self, z):
        x = F.relu(self.fc_input(z))
        # Восстановление 3D формы тензора (batch, window, model_dim)
        x = x.view(-1, self.window_size, self.model_dim)

        x = self.block1(x)
        x = self.block2(x)

        reconstruction = self.output_dense(x)
        return reconstruction


class SequenceVAE(nn.Module):
    def __init__(self, window_size, n_features, latent_dim, beta=0.1):
        super().__init__()
        self.encoder = AttentionEncoder(window_size, n_features, latent_dim)
        self.decoder = AttentionDecoder(window_size, n_features, latent_dim)
        self.beta = beta

    def forward(self, x):
        z_mean, z_log_var, z = self.encoder(x)
        reconstruction = self.decoder(z)
        return reconstruction, z_mean, z_log_var

    def compute_loss(self, data, reconstruction, z_mean, z_log_var):
        # Сумма квадратов ошибок по осям времени и фичей, затем среднее по батчу
        reconstruction_loss = torch.mean(torch.sum((data - reconstruction) ** 2, dim=[1, 2]))

        # Расчет дивергенции Кульбака-Лейблера
        kl_loss = -0.5 * torch.mean(torch.sum(1 + z_log_var - torch.square(z_mean) - torch.exp(z_log_var), dim=1))

        total_loss = reconstruction_loss + self.beta * kl_loss
        return total_loss, reconstruction_loss, kl_loss