import { useState } from "react";
import { api, ROLES } from "./api";

export default function AuthPage({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [err, setErr] = useState("");
  const [f, setF] = useState({ name: "", email: "", password: "", phone: "", role: "CUSTOMER", basePrice: "", waiterCount: "", captainCount: "" });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const showPrice = f.role !== "CUSTOMER" && f.role !== "THEKEDAR";
  const isThekedar = f.role === "THEKEDAR";

  async function submit(e) {
    e.preventDefault();
    setErr("");
    try {
      const body = mode === "login"
        ? { email: f.email, password: f.password }
        : {
            name: f.name, email: f.email, password: f.password, phone: f.phone, role: f.role,
            basePrice: f.basePrice ? Number(f.basePrice) : null,
            waiterCount: f.waiterCount ? Number(f.waiterCount) : null,
            captainCount: f.captainCount ? Number(f.captainCount) : null,
          };
      onLogin(await api(`/auth/${mode}`, { method: "POST", body }));
    } catch (ex) {
      setErr(ex.message);
    }
  }

  return (
    <main style={{ maxWidth: 420 }}>
      <div className="card">
        <h2>Smart Catering - {mode === "login" ? "Login" : "Register"}</h2>
        {err && <div className="msg">{err}</div>}
        <form onSubmit={submit}>
          {mode === "register" && (
            <>
              <input placeholder="Name" value={f.name} onChange={set("name")} required /><br />
              <input placeholder="Phone" value={f.phone} onChange={set("phone")} /><br />
              <select value={f.role} onChange={set("role")}>
                {ROLES.map((r) => <option key={r}>{r}</option>)}
              </select><br />
              {showPrice && <input type="number" placeholder={f.role === "CATERER" ? "Price per plate (Rs)" : "Price per shift (Rs)"} value={f.basePrice} onChange={set("basePrice")} />}
              {isThekedar && (
                <>
                  <input type="number" placeholder="Total waiters" value={f.waiterCount} onChange={set("waiterCount")} />
                  <input type="number" placeholder="Total captains" value={f.captainCount} onChange={set("captainCount")} />
                </>
              )}
            </>
          )}
          <input type="email" placeholder="Email" value={f.email} onChange={set("email")} required /><br />
          <input type="password" placeholder="Password (min 6)" value={f.password} onChange={set("password")} required /><br />
          <button type="submit">{mode === "login" ? "Login" : "Register"}</button>
          <button type="button" className="gray" onClick={() => { setErr(""); setMode(mode === "login" ? "register" : "login"); }}>
            {mode === "login" ? "Naya account banao" : "Login par jao"}
          </button>
        </form>
      </div>
    </main>
  );
}