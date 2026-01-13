# GLPI Mobile & Web Proxy

Este repositório contém o código fonte para o aplicativo móvel nativo (Android) e o proxy backend (Node.js) para a versão web legado.

## 📱 Aplicativo Android

O aplicativo Android é desenvolvido em Kotlin e está localizado na pasta `app/`. Ele foi projetado para técnicos de campo e oferece funcionalidades offline, upload de fotos e integração com o GLPI.

### Pré-requisitos
- Android Studio Iguana ou superior.
- JDK 17.
- Android SDK API 34.

### Como Executar
1. Abra o Android Studio.
2. Selecione "Open" e navegue até a raiz deste repositório (onde estão os arquivos `build.gradle.kts` e `settings.gradle.kts`).
3. Aguarde o Gradle sincronizar as dependências.
4. Conecte um dispositivo Android ou inicie um emulador.
5. Clique em "Run" (Shift+F10).

### Estrutura
- `app/src/main/java`: Código fonte Kotlin.
- `app/src/main/res`: Recursos (layouts, strings, imagens).
- `app/src/main/AndroidManifest.xml`: Manifesto do aplicativo.

---

## 🌐 Web Backend Proxy (Legado)

O backend proxy em Node.js foi utilizado para a versão web protótipo e os arquivos estão localizados na raiz do repositório para compatibilidade com o deploy no Render.

### Como Executar
1. Na raiz do projeto:
   ```bash
   npm install
   ```
2. Inicie o servidor:
   ```bash
   node server.js
   ```

### Notas
- Este backend é necessário apenas se você estiver executando a versão web (`index.html`). O aplicativo Android se conecta diretamente à API do GLPI.
