# Employee Directory

A Spring Boot REST API and React single-page app for employee management. Accounts are stored in PostgreSQL, passwords are BCrypt-hashed, and authenticated requests use a server-side session with CSRF protection. Employee records use a configurable PostgreSQL or MongoDB store. Salary data is encrypted at the application layer, and employee changes are published to Kafka for downstream consumers.

## Start the backend

Create a PostgreSQL database and role, then configure the connection:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/employee_api
export DATABASE_USERNAME=employee_api
export DATABASE_PASSWORD=your-local-database-password
export EMPLOYEE_LOCAL_ENCRYPTION_KEY="$(openssl rand -base64 32)"
mvn spring-boot:run
```

The app creates the `users`, `authorities`, and `employees` tables on startup. For a production deployment, use a managed PostgreSQL database, set strong credentials, and serve the app over HTTPS.

## Employee database selection

PostgreSQL is the default employee store. Change stores with `EMPLOYEE_STORE`:

```bash
# PostgreSQL (default)
export EMPLOYEE_STORE=postgres

# MongoDB employee records; account/authentication records remain in PostgreSQL
export EMPLOYEE_STORE=mongo
export MONGODB_URI=mongodb://localhost:27017/employee_api
```

Only the employee repository switches; account authentication continues to use PostgreSQL. Changing stores does not migrate existing records. Migrate employee documents before switching a deployed environment.

## Encryption and AWS secrets

Employee salaries are encrypted before being persisted by either store. Local development uses AES-256-GCM with a configurable base64-encoded 32-byte key; generate and securely retain `EMPLOYEE_LOCAL_ENCRYPTION_KEY` before starting the app. If this key is lost or changed, existing local employee salaries cannot be decrypted. Never commit or reuse local keys in production. The encrypted representation stores a version, wrapped-key reference, nonce, and authenticated ciphertext. Kafka events deliberately contain only event type, employee ID, and timestamp; they do not expose salary data.

For production, activate the `prod` Spring profile. It selects AWS KMS envelope encryption and AWS Secrets Manager for PostgreSQL credentials:

```bash
export SPRING_PROFILES_ACTIVE=prod
export EMPLOYEE_KMS_KEY_ID=arn:aws:kms:REGION:ACCOUNT:key/KEY_ID
export DATABASE_CREDENTIALS_SECRET_ID=employee-api/postgres
export DATABASE_URL=jdbc:postgresql://db.example:5432/employee_api
```

The Secrets Manager secret must be a JSON object containing non-empty `username` and `password` values. Provide AWS region and credentials through the AWS SDK default provider chain (prefer an IAM role); grant the application role `kms:GenerateDataKey` and `kms:Decrypt` for the selected key and `secretsmanager:GetSecretValue` for the database secret. The application encrypts salaries with an AES data key wrapped by KMS, using a fixed encryption context. Keep access to database credentials and KMS keys restricted to the application role. Switching crypto providers does not re-encrypt existing employee records; decrypt and re-encrypt them as part of a key/provider migration.

## Business-operation audit

Spring AOP records the operation name, authenticated actor, duration, and success/failure for employee operations and account registration. Method arguments and employee salary data are intentionally excluded from these logs.

The employee event producer connects to `localhost:9092` by default. Start a Kafka broker before creating, updating, or deleting employees, or set `KAFKA_BOOTSTRAP_SERVERS` to the broker address. Ensure the `employee-events` topic exists (for a local single-broker setup, run `kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic employee-events --partitions 3 --replication-factor 1`). Override the topic with `EMPLOYEE_EVENTS_TOPIC`. Each JSON event contains `eventType`, `employeeId`, and `occurredAt`; employee ID is the Kafka key so changes for one employee are routed consistently. Delivery failures are logged without failing an employee request. These are best-effort notifications, not a durable outbox; retry/replay and employee/Kafka atomicity are not provided.

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
mvn verify
```

`mvn verify` generates `target/site/jacoco/index.html` and enforces a minimum overall backend line coverage of 92.55%, the measured baseline before adding Kafka and exception handling.

Build the SPA with:

```bash
cd frontend && npm run build
```
