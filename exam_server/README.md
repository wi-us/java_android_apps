# Shop Server Exam

This is a server for a clothing store project, built with FastAPI and Peewee.

## Project Structure

- `main.py`: The main entry point for the FastAPI application.
- `database.py`: Contains all Peewee ORM models.
- `schemas.py`: Contains all Pydantic models for data validation.
- `routes.py`: Defines the REST API endpoints (`/api/...`).
- `admin.py`: Defines the web routes for the admin panel (`/admin/...`).
- `database.sqlite`: The SQLite database file.
- `templates/`: Contains Jinja2 HTML templates for the admin panel.
- `static/`: Contains static files (e.g., CSS).
- `requirements.txt`: Lists the required Python packages.

## How to Run

1.  **Install dependencies:**
    ```bash
    pip install -r requirements.txt
    ```

2.  **(Optional) 2GIS API key (address autocomplete):**
    - Create a key in 2GIS Platform and enable Search/Suggest/Geocoder APIs
    - Set environment variable:
      - Windows (PowerShell):
        ```powershell
        $env:DGIS_API_KEY="YOUR_KEY"
        ```
      - macOS/Linux (bash):
        ```bash
        export DGIS_API_KEY="YOUR_KEY"
        ```
    Without this key, `/api/address_suggest` and `/api/address_details` will return 500.

3.  **Run the server:**
    ```bash
    uvicorn main:app --reload
    ```

    The server will be available at `http://127.0.0.1:8000`.

## Usage

-   **API**: The REST API is available under the `/api/` prefix. You can find interactive documentation at `http://127.0.0.1:8000/docs`.
-   **Admin Panel**: A simple web-based admin panel is available at `http://127.0.0.1:8000/admin`. 