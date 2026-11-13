export const BASE = import.meta.env.VITE_API_URL || "http://localhost:8081/api";

export const ROLES = ["CUSTOMER", "CATERER", "STALL_OWNER", "BAR_OWNER", "CROCKERY_OWNER", "TANDOOR_OWNER", "CHINESE_OWNER", "CHAPATI_OWNER", "THEKEDAR"];
export const VENDOR_ROLES = ROLES.slice(1);

export async function api(path, { method = "GET", body } = {}) {
  const token = localStorage.getItem("token");
  const res = await fetch(BASE + path, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) {
    if (data && data.error) throw new Error(data.error);
    if (res.status === 403) throw new Error("Access nahi hai ya login expire ho gaya. Logout karke dobara login karo.");
    throw new Error("Error " + res.status);
  }
  return data;
}