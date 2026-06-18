import torch

print("=== ПРОВЕРКА PYTORCH GPU ===")
print("Версия PyTorch:", torch.__version__)
print("Доступен ли CUDA (GPU):", torch.cuda.is_available())

if torch.cuda.is_available():
    print("Имя видеокарты:", torch.cuda.get_device_name(0))
    print("Текущее устройство CUDA:", torch.cuda.current_device())
else:
    print("\n[ВНИМАНИЕ] PyTorch не видит видеокарту. Используется CPU-only версия.")