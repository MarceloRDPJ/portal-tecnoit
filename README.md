# TecnoiT - Central de Chamados

This project consists of two main parts:

1.  **Frontend:** A single-page application (`index.html`) that provides the user interface for viewing and managing GLPI tickets.
2.  **Backend Proxy:** A simple Node.js server (`server.js`) that acts as a proxy to the GLPI API. This is necessary to handle authentication and bypass CORS issues.

## Frontend

The frontend is contained entirely within `index.html`. It uses React and Tailwind CSS loaded from a CDN. To use it, simply open the `index.html` file in a web browser.

## Backend Proxy (`server.js`)

The `server.js` file is an Express application that provides the API endpoints the frontend consumes.

### Running the Proxy Server

To run the proxy server, you need to have Node.js and npm installed.

1.  **Install Dependencies:**
    Navigate to the project directory in your terminal and run:
    ```bash
    npm install
    ```

2.  **Set Environment Variables:**
    The server requires one environment variable to be set:
    -   `GLPI_APP_TOKEN`: Your GLPI application token.

    You can set this in your terminal before running the server, or use a `.env` file with a library like `dotenv`.

3.  **Start the Server:**
    Once the dependencies are installed and the environment variable is set, you can start the server with:
    ```bash
    npm start
    ```

    The server will start on port 3001 by default, or the port specified in the `PORT` environment variable.

### Deployment

When deploying this project, you must deploy both the frontend files (`index.html`, `assets/`, etc.) and the backend `server.js` application. The `server.js` application must be running in a Node.js environment (like on onrender.com, Heroku, etc.) and the frontend must be able to reach it.
