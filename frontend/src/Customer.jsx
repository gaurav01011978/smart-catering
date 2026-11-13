import { useEffect, useState } from "react";
import { api, VENDOR_ROLES } from "./api";

export default function Customer() {
  const [form, setForm] = useState({ eventDate: "", shift: "NIGHT", plates: "" });
  const [role, setRole] = useState("CATERER");
  const [vendors, setVendors] = useState([]);
  const [selected, setSelected] = useState({});
  const [bookings, setBookings] = useState([]);
  const [msg, setMsg] = useState("");

  const loadBookings = () => api("/bookings/my").then(setBookings).catch((e) => setMsg(e.message));
  useEffect(() => { loadBookings(); }, []);

  useEffect(() => {
    if (!form.eventDate) { setVendors([]); return; }
    api(`/vendors?role=${role}&date=${form.eventDate}&shift=${form.shift}`)
      .then(setVendors).catch((e) => setMsg(e.message));
  }, [role, form.eventDate, form.shift]);

  const setF = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const toggle = (v) => setSelected((p) => {
    const n = { ...p };
    if (n[v.id]) delete n[v.id];
    else n[v.id] = { id: v.id, name: v.name, role, price: v.basePrice || 0, waiters: 0, captains: 0 };
    return n;
  });
  const setQty = (id, k, val) => setSelected((p) => ({ ...p, [id]: { ...p[id], [k]: Number(val) } }));

  const cost = (s) =>
    s.role === "THEKEDAR" ? s.waiters * 1000 + s.captains * 1800
    : s.role === "CATERER" ? s.price * (Number(form.plates) || 0)
    : s.price;
  const total = Object.values(selected).reduce((a, s) => a + cost(s), 0);

  async function book() {
    setMsg("");
    try {
      await api("/bookings", {
        method: "POST",
        body: {
          eventDate: form.eventDate,
          shift: form.shift,
          plates: Number(form.plates) || 0,
          items: Object.values(selected).map((s) => ({ vendorId: s.id, waiters: s.waiters, captains: s.captains })),
        },
      });
      setSelected({});
      setMsg("Booking request bhej di gayi. Sab providers accept karenge to confirm hogi.");
      loadBookings();
    } catch (e) { setMsg(e.message); }
  }

  async function pay(id, kind) {
    setMsg("");
    try { await api(`/bookings/${id}/pay-${kind}`, { method: "POST" }); loadBookings(); }
    catch (e) { setMsg(e.message); }
  }

  return (
    <>
      {msg && <div className="msg">{msg}</div>}

      <div className="card">
        <h3>Nayi Booking</h3>
        <input type="date" value={form.eventDate} onChange={setF("eventDate")} />
        <select value={form.shift} onChange={setF("shift")}>
          <option>DAY</option><option>NIGHT</option>
        </select>
        <input type="number" placeholder="Plates (caterer ke liye)" value={form.plates} onChange={setF("plates")} />

        <div style={{ marginTop: 10 }}>
          {VENDOR_ROLES.map((r) => (
            <button key={r} className={r === role ? "" : "gray"} onClick={() => setRole(r)}>{r.replace("_", " ")}</button>
          ))}
        </div>

        {!form.eventDate && <p>Pehle date chuno, phir providers ki availability dikhegi.</p>}
        <div className="grid" style={{ marginTop: 12 }}>
          {vendors.map((v) => (
            <div className="card" key={v.id}>
              <b>{v.name}</b> {v.phone && <small>({v.phone})</small>}<br />
              {role === "THEKEDAR"
                ? <small>Waiter free: {v.waitersFree}, Captain free: {v.captainsFree} (Rs 1000 / Rs 1800 per shift)</small>
                : <small>Rs {v.basePrice ?? "-"} {role === "CATERER" ? "per plate" : "per shift"}</small>}
              <br />
              <span className={"badge " + (v.available ? "ok" : "no")}>{v.available ? "Available" : "Busy"}</span>
              {v.menu.length > 0 && (
                <ul style={{ paddingLeft: 18, margin: "8px 0" }}>
                  {v.menu.map((m) => <li key={m.id}>{m.name} - Rs {m.price}</li>)}
                </ul>
              )}
              <button disabled={!v.available} className={selected[v.id] ? "red" : "green"} onClick={() => toggle(v)}>
                {selected[v.id] ? "Hatao" : "Add"}
              </button>
              {selected[v.id] && role === "THEKEDAR" && (
                <div>
                  <input type="number" min="0" style={{ width: 90 }} placeholder="Waiters" value={selected[v.id].waiters} onChange={(e) => setQty(v.id, "waiters", e.target.value)} />
                  <input type="number" min="0" style={{ width: 90 }} placeholder="Captains" value={selected[v.id].captains} onChange={(e) => setQty(v.id, "captains", e.target.value)} />
                </div>
              )}
            </div>
          ))}
        </div>

        {Object.keys(selected).length > 0 && (
          <div className="card">
            <b>Selected:</b>
            <ul>
              {Object.values(selected).map((s) => <li key={s.id}>{s.name} ({s.role}) - Rs {cost(s)}</li>)}
            </ul>
            <b>Estimated total: Rs {total}</b><br />
            <button onClick={book} disabled={!form.eventDate}>Booking bhejo</button>
          </div>
        )}
      </div>

      <div className="card">
        <h3>Meri Bookings</h3>
        {bookings.length === 0 && <p>Abhi koi booking nahi hai.</p>}
        {bookings.map((b) => (
          <div className="card" key={b.id}>
            <b>#{b.id}</b> - {b.eventDate} ({b.shift}) <span className="badge">{b.status}</span><br />
            Total: Rs {b.totalAmount} | Advance paid: Rs {b.advancePaid} | Balance: {b.balancePaid ? "paid" : "Rs " + (b.totalAmount - b.advancePaid)}
            <table>
              <tbody>
                {b.items.map((i) => (
                  <tr key={i.id}>
                    <td>{i.vendor.name} ({i.vendor.role})</td>
                    <td>Rs {i.amount}{!i.chargedToCustomer && " (caterer ke price me)"}</td>
                    <td><span className="badge">{i.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {b.status !== "REJECTED" && b.advancePaid === 0 && <button onClick={() => pay(b.id, "advance")}>Advance pay karo (30%)</button>}
            {b.status === "CONFIRMED" && b.advancePaid > 0 && !b.balancePaid && <button className="green" onClick={() => pay(b.id, "balance")}>Balance pay karo (function ke din)</button>}
          </div>
        ))}
      </div>
    </>
  );
}