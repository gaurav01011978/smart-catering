import { useState } from "react";
import AuthPage from "./AuthPage";
import Customer from "./Customer";
import Vendor from "./Vendor";

export default function App() {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user") || "null"));

  function saveUser(u) {
    localStorage.setItem("user", JSON.stringify(u));
    setUser(u);
  }
  function login(data) {
    localStorage.setItem("token", data.token);
    saveUser(data.user);
  }
  function logout() {
    localStorage.clear();
    setUser(null);
  }

  if (!user) return <AuthPage onLogin={login} />;

  return (
    <>
      <header>
        <b>Smart Catering</b>
        <span>{user.name} ({user.role}) <button className="gray" onClick={logout}>Logout</button></span>
      </header>
      <main>
        {user.role === "CUSTOMER" ? <Customer /> : <Vendor user={user} onUser={saveUser} />}
      </main>
    </>
  );
}