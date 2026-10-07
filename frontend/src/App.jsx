import { useEffect, useState } from "react";

function csrfCookie() {
  const cookie = document.cookie
    .split("; ")
    .find((item) => item.startsWith("XSRF-TOKEN="));
  return cookie ? decodeURIComponent(cookie.substring("XSRF-TOKEN=".length)) : "";
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof URLSearchParams)) {
    headers.set("Content-Type", "application/json");
  }
  if (options.method && !["GET", "HEAD", "OPTIONS"].includes(options.method.toUpperCase())) {
    await fetch("/api/auth/csrf");
    headers.set("X-XSRF-TOKEN", csrfCookie());
  }

  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    const result = await response.json().catch(() => null);
    const error = new Error(result?.message || (response.status === 401 ? "Email or password is incorrect." : "The request could not be completed."));
    error.status = response.status;
    throw error;
  }
  if (response.status === 204) return null;
  return response.json();
}

export default function App() {
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [authMode, setAuthMode] = useState("login");
  const [error, setError] = useState("");

  useEffect(() => {
    request("/api/auth/csrf")
      .then(() => request("/api/auth/me"))
      .then(setUser)
      .catch((reason) => {
        if (reason.status !== 401) setError(reason.message);
      })
      .finally(() => setCheckingSession(false));
  }, []);

  async function authenticate(event) {
    event.preventDefault();
    setError("");
    const form = new FormData(event.currentTarget);
    const email = form.get("email").trim().toLowerCase();
    const password = form.get("password");
    try {
      if (authMode === "register") {
        await request("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({ email, password }),
        });
      }
      await request("/api/auth/login", {
        method: "POST",
        body: new URLSearchParams({ username: email, password }),
      });
      setUser(await request("/api/auth/me"));
    } catch (reason) {
      setError(reason.message);
    }
  }

  async function logout() {
    setError("");
    try {
      await request("/api/auth/logout", { method: "POST" });
      setUser(null);
    } catch (reason) {
      setError(reason.message);
    }
  }

  if (checkingSession) return <main className="loading">Loading your workspace…</main>;

  return user ? (
    <EmployeeDashboard user={user} onLogout={logout} error={error} setError={setError} />
  ) : (
    <main className="auth-layout">
      <section className="auth-card">
        <div className="brand-mark">ED</div>
        <p className="eyebrow">EMPLOYEE DIRECTORY</p>
        <h1>{authMode === "login" ? "Welcome back" : "Create your account"}</h1>
        <p className="subtle">
          {authMode === "login" ? "Sign in to manage your employee directory." : "Register to get started with your directory."}
        </p>
        <form className="stack" onSubmit={authenticate}>
          <label>
            Email
            <input name="email" type="email" autoComplete="email" required maxLength="254" />
          </label>
          <label>
            Password
            <input
              name="password"
              type="password"
              autoComplete={authMode === "login" ? "current-password" : "new-password"}
              minLength={authMode === "register" ? 12 : undefined}
              maxLength="72"
              required
            />
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button className="primary-button" type="submit">
            {authMode === "login" ? "Sign in" : "Create account"}
          </button>
        </form>
        <p className="switch-mode">
          {authMode === "login" ? "New to the directory?" : "Already have an account?"}{" "}
          <button
            className="text-button"
            type="button"
            onClick={() => {
              setAuthMode(authMode === "login" ? "register" : "login");
              setError("");
            }}
          >
            {authMode === "login" ? "Create an account" : "Sign in"}
          </button>
        </p>
      </section>
    </main>
  );
}

function EmployeeDashboard({ user, onLogout, error, setError }) {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);

  async function loadEmployees() {
    setError("");
    try {
      setEmployees(await request("/api/employees"));
    } catch (reason) {
      setError(reason.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadEmployees();
  }, []);

  async function saveEmployee(event) {
    event.preventDefault();
    setError("");
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const employee = {
      name: form.get("name").trim(),
      salary: Number(form.get("salary")),
    };
    try {
      await request(editingId ? `/api/employees/${editingId}` : "/api/employees", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify(employee),
      });
      setEditingId(null);
      formElement.reset();
      await loadEmployees();
    } catch (reason) {
      setError(reason.message);
    }
  }

  async function deleteEmployee(id) {
    setError("");
    try {
      await request(`/api/employees/${id}`, { method: "DELETE" });
      await loadEmployees();
    } catch (reason) {
      setError(reason.message);
    }
  }

  const selectedEmployee = employees.find((employee) => employee.id === editingId);

  return (
    <main className="dashboard">
      <header className="topbar">
        <a className="wordmark" href="/">Employee<span>Directory</span></a>
        <div className="account">
          <span className="account-email">{user.email}</span>
          <button className="quiet-button" onClick={onLogout}>Sign out</button>
        </div>
      </header>
      <section className="content">
        <div className="page-heading">
          <div>
            <p className="eyebrow">YOUR WORKSPACE</p>
            <h1>Employees</h1>
            <p className="subtle">Keep your team information organized in one place.</p>
          </div>
          <span className="count-badge">{employees.length} {employees.length === 1 ? "employee" : "employees"}</span>
        </div>

        {error && <p className="error notice" role="alert">{error}</p>}

        <section className="panel">
          <div className="panel-heading">
            <h2>{editingId ? "Edit employee" : "Add an employee"}</h2>
            {editingId && <button className="text-button" onClick={() => setEditingId(null)}>Cancel edit</button>}
          </div>
          <form key={editingId ?? "new"} className="employee-form" onSubmit={saveEmployee}>
            <label>
              Full name
              <input name="name" defaultValue={selectedEmployee?.name ?? ""} required maxLength="100" />
            </label>
            <label>
              Annual salary
              <input name="salary" type="number" min="0" step="0.01" defaultValue={selectedEmployee?.salary ?? ""} required />
            </label>
            <button className="primary-button" type="submit">{editingId ? "Save changes" : "Add employee"}</button>
          </form>
        </section>

        <section className="panel directory-panel">
          <div className="panel-heading">
            <h2>Directory</h2>
            <span className="subtle">{employees.length} total</span>
          </div>
          {loading ? <p className="empty-state">Loading employees…</p> : employees.length === 0 ? (
            <p className="empty-state">No employees yet. Add your first team member above.</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead><tr><th>Employee</th><th>Annual salary</th><th><span className="sr-only">Actions</span></th></tr></thead>
                <tbody>
                  {employees.map((employee) => (
                    <tr key={employee.id}>
                      <td><span className="avatar">{employee.name.charAt(0).toUpperCase()}</span>{employee.name}</td>
                      <td>{Number(employee.salary).toLocaleString(undefined, { style: "currency", currency: "USD" })}</td>
                      <td className="row-actions">
                        <button className="text-button" onClick={() => setEditingId(employee.id)}>Edit</button>
                        <button className="danger-button" onClick={() => deleteEmployee(employee.id)}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </section>
    </main>
  );
}
