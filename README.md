# 📱 VoiceBridge Client

Android-клиент для передачи микрофонного аудио на VoiceBridge Server.

## ✨ Возможности

- 🔒 Защищённое соединение с сервером (TLS 1.3)
- 🔐 Аутентификация по паролю
- 📜 Поддержка самоподписанных сертификатов (можно загрузить свой)
- 🎛️ Голосовые эффекты в реальном времени (повышение/понижение тона, эхо, робот, хорус)
- 📊 Отображение уровня громкости и задержки (пинг)
- 🔄 Автоматическое переподключение при обрыве связи
- 🛠️ Администрирование сервера (бан клиентов)
- 🎚️ Настройка битности аудио (16/24 бит), темы оформления, режима отладки
- 📜 История последних серверов (сохраняется автоматически)
- 📤 Возможность поделиться логами

## 📋 Требования

- Android 11+ (API 30)
- 🎙️ Микрофон
- 📶 Wi-Fi

## 📦 Установка

Скачайте последний APK из раздела [Releases](https://github.com/ovzen/VoiceBridge-Client/releases/latest) и установите на устройство. При первом запуске предоставьте разрешение на запись аудио.

## 🖼️ Демонстрация
<div style="display: flex; overflow-x: auto; gap: 12px; padding: 10px 0;">
  <a href="https://github.com/user-attachments/assets/16f8a608-7bae-40da-806b-277ac82b641f">
    <img src="https://github.com/user-attachments/assets/16f8a608-7bae-40da-806b-277ac82b641f" width="200" style="height: auto; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
  </a>
  <a href="https://github.com/user-attachments/assets/59070bab-03cc-4857-8b98-b33cea88a29b">
    <img src="https://github.com/user-attachments/assets/59070bab-03cc-4857-8b98-b33cea88a29b" width="200" style="height: auto; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
  </a>
  <a href="https://github.com/user-attachments/assets/ff51a9cc-5456-4a48-b089-80407802c20e">
    <img src="https://github.com/user-attachments/assets/ff51a9cc-5456-4a48-b089-80407802c20e" width="200" style="height: auto; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
  </a>
  <a href="https://github.com/user-attachments/assets/1b651dcb-7039-4cdf-af00-29c4fdfba184">
    <img src="https://github.com/user-attachments/assets/1b651dcb-7039-4cdf-af00-29c4fdfba184" width="200" style="height: auto; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
  </a>
  <a href="https://github.com/user-attachments/assets/bc4ca2fb-3979-4104-b874-9b89a574d030">
    <img src="https://github.com/user-attachments/assets/bc4ca2fb-3979-4104-b874-9b89a574d030" width="200" style="height: auto; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
  </a>
</div>

*Примеры интерфейса приложения (фактические изображения могут отличаться).*

## 🚀 Использование

### 🔌 Подключение к серверу
1. На вкладке «Подключение» введите IP и порт сервера, пароль.
2. Нажмите «Подключиться». Статус изменится на «Подключено».
3. Можно отключить микрофон (ползунок «Mute»).

### 🎛️ Голосовые эффекты
На вкладке «Эффекты» выберите нужный эффект. Он применится в реальном времени.

### 📄 Логи
На вкладке «Логи» отображаются события. Можно очистить лог или поделиться им через любое приложение.

### ⚙️ Настройки
- **Безопасный режим** — требует загруженный сертификат сервера.
- **Битность аудио** — 16 бит (стандарт) или 24 бита (требуется Android 12+).
- **Тема** — системная, светлая, тёмная.
- **Режим отладки** — подробные логи (рекомендуется отключать для экономии ресурсов).
- **Сбросить настройки** — удаляет все сохранённые данные.

### 👑 Администрирование
На вкладке «Админ» введите пароль администратора (указывается в конфиге сервера) для управления подключёнными клиентами (бан).

## 🛠️ Сборка из исходников
1. Откройте проект в Android Studio (Meerkat или новее).
2. Синхронизируйте зависимости (Gradle).
3. Соберите APK через `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## 📦 Репозитории

| Компонент | Ссылка |
|-----------|--------|
| 📱 Android-клиент | [VoiceBridge-Client](https://github.com/ovzen/VoiceBridge-Client) |
| 💻 Сервер | [VoiceBridge-Server](https://github.com/ovzen/VoiceBridge-Server) |
