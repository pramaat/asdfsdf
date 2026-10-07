# Employee Directory

A Spring Boot REST API and React single-page app for employee management. Accounts are stored in PostgreSQL, passwords are BCrypt-hashed, and authenticated requests use a server-side session with CSRF protection.
Employee records remain in memory and are cleared when the application stops.

## Start the backend

Create a PostgreSQL database and role, then configure the connection:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/employee_api
export DATABASE_USERNAME=employee_api
export DATABASE_PASSWORD=your-local-database-password
mvn spring-boot:run
```

The app creates the `users` and `authorities` tables on startup. For a production deployment, use a managed PostgreSQL database, set strong credentials, and serve the app over HTTPS.

## Start the React app

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the Vite URL shown in the terminal (normally `http://localhost:5173`). The development server proxies `/api` requests to the Spring Boot server at `http://localhost:8080`.

Accounts can be registered from the sign-in page. Passwords must be at least 12 characters. The SPA supports employee listing, creation, updates, and deletion.

## API

All employee endpoints require an authenticated session:

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/employees` | List employees |
| `GET` | `/api/employees/{id}` | Get an employee |
| `POST` | `/api/employees` | Create an employee |
| `PUT` | `/api/employees/{id}` | Replace an employee |
| `DELETE` | `/api/employees/{id}` | Delete an employee |

Authentication endpoints:

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/auth/csrf` | Initialize the CSRF cookie |
| `POST` | `/api/auth/register` | Register with an email and password |
| `POST` | `/api/auth/login` | Start a session (`username` and `password` form fields) |
| `GET` | `/api/auth/me` | Return the current account |
| `POST` | `/api/auth/logout` | End the current session |

For unsafe requests, send the `X-XSRF-TOKEN` header with the value of the `XSRF-TOKEN` cookie. The React client handles this automatically.

Employee POST and PUT accept a JSON body with a non-blank name (up to 100 characters) and a non-negative salary:

```json
{
  "name": "Alice",
  "salary": 50000.00
}
```

Run the backend tests with:

```bash
mvn test
```

Build the SPA with:

```bash
cd frontend && npm run build
```
