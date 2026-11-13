import { useEffect, useState } from "react";
import { api, VENDOR_ROLES } from "./api";

const SUB_ROLES = VENDOR_ROLES.filter((r) => r !== "CATERER");

function SubBook({ item, onDone }) {
  const [role, setRole] = useState("BAR_OWNER");
  const [vendors, setVendors] = useState([]);
  const [qty, setQty] = useState({ waiters: 0, captains: 0 });
  const [msg, setMsg] = useState("");

  useEffect(() => {
    api(`/vendors?role=${role}&date=${item.eventDate}&shift=${item.shift}`)
      .then(setVendors).catch((e) => setMsg(e.message));
  }, [role]);

  async function book(v) {
    setMsg("");
    try {
      await api(`/bookings/items/${item.itemId}/sub-book`, { method: "POST", body: { vendorId: v.id, ...qty } });
      setMsg(v.name + " ko sub-booking bhej di. Unke reply ke baad aap accept kar sakte ho.");
      onDone();
    } catch (e) { setMsg(e.message); }
  }

  return (
    <div className="card">
      <b>Sub-booking (apna setup free nahi ho to)</b><br />
      <select value={role} onChange={(e) => setRole(e.target.value)}>
        {SUB_ROLES.map((r) => <option key={r}>{r}</option>)}
      </select>
      {role === "THEKEDAR" && (
        <>
          <input type="number" style={{ width: 90 }} placeholder="Waiters" onChange={(e) => setQty({ ...qty, waiters: Number(e.target.value) })} />
          <input type="number" style={{ width: 90 }} placeholder="Captains" onChange={(e) => setQty({ ...qty, captains: Number(e.target.value) })} />
        </>
      )}
      {msg && <div className="msg">{msg}</div>}
      {vendors.map((v) => (
        <div key={v.id}>
          {v.name} - Rs {v.basePrice ?? "-"}{" "}
          <span className={"badge " + (v.available ? "ok" : "no")}>{v.available ? "Available" : "Busy"}</span>{" "}
          <button disabled={!v.available} onClick={() => book(v)}>Book</button>
        </div>
      ))}
    </div>
  );
}

export default function Vendor({ user, onUser }) {
  const [reqs, setReqs] = useState([]);
  const [menu, setMenu] = useState([]);
  const [item, setItem] = useState({ name: "", price: "" });
  const [prof, setProf] = useState({ basePrice: user.basePrice ?? "", waiterCount: user.waiterCount ?? 0, captainCount: user.captainCount ?? 0 });
  const [open, setOpen] = useState(null);
  const [msg, setMsg] = useState("");
  const isThekedar = user.role === "THEKEDAR";

  const loadReqs = () => api("/bookings/requests").then(setReqs).catch((e) => setMsg(e.message));
  const loadMenu = () => { if (!isThekedar) api("/menu/my").then(setMenu).catch((e) => setMsg(e.message)); };
  useEffect(() => { loadReqs(); loadMenu(); }, []);

  async function respond(id, accept) {
    setMsg("");
    try { await api(`/bookings/items/${id}/respond?accept=${accept}`, { method: "PUT" }); loadReqs(); }
    catch (e) { setMsg(e.message); }
  }

  async function saveProfile() {
    setMsg("");
    try {
      const u = await api("/vendors/me", {
        method: "PUT",
        body: {
          basePrice: prof.basePrice === "" ? null : Number(prof.basePrice),
          waiterCount: Number(prof.waiterCount),
          captainCount: Number(prof.captainCount),
        },
      });
      onUser(u);
      setMsg("Profile save ho gayi.");
    } catch (e) { setMsg(e.message); }
  }

  async function addItem() {
    try {
      await api("/menu", { method: "POST", body: { name: item.name, price: Number(item.price) } });
      setItem({ name: "", price: "" });
      loadMenu();
    } catch (e) { setMsg(e.message); }
  }

  async function delItem(id) {
    try { await api(`/menu/${id}`, { method: "DELETE" }); loadMenu(); }
    catch (e) { setMsg(e.message); }
  }

  const openItem = open ? reqs.find((r) => r.itemId === open) : null;

  return (
    <>
      {msg && <div className="msg">{msg}</div>}

      <div className="card">
        <h3>Meri Profile / Price</h3>
        {isThekedar ? (
          <>
            Total waiters: <input type="number" value={prof.waiterCount} onChange={(e) => setProf({ ...prof, waiterCount: e.target.value })} />
            Total captains: <input type="number" value={prof.captainCount} onChange={(e) => setProf({ ...prof, captainCount: e.target.value })} />
            <br /><small>Rate fixed hai: waiter Rs 1000, captain Rs 1800 per shift.</small>
          </>
        ) : (
          <>
            {user.role === "CATERER" ? "Price per plate (Rs): " : "Price per shift (Rs): "}
            <input type="number" value={prof.basePrice} onChange={(e) => setProf({ ...prof, basePrice: e.target.value })} />
          </>
        )}
        <button onClick={saveProfile}>Save</button>
      </div>

      {!isThekedar && (
        <div className="card">
          <h3>Mera Menu</h3>
          <input placeholder="Item ka naam" value={item.name} onChange={(e) => setItem({ ...item, name: e.target.value })} />
          <input type="number" placeholder="Price" value={item.price} onChange={(e) => setItem({ ...item, price: e.target.value })} />
          <button onClick={addItem} disabled={!item.name || !item.price}>Add</button>
          <ul>
            {menu.map((m) => (
              <li key={m.id}>{m.name} - Rs {m.price} <button className="red" onClick={() => delItem(m.id)}>X</button></li>
            ))}
          </ul>
        </div>
      )}

      <div className="card">
        <h3>Booking Requests</h3>
        {reqs.length === 0 && <p>Abhi koi request nahi aayi.</p>}
        <table>
          <thead>
            <tr><th>Booking</th><th>Date / Shift</th><th>Detail</th><th>Amount</th><th>Kisne</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {reqs.map((r) => (
              <tr key={r.itemId}>
                <td>#{r.bookingId}</td>
                <td>{r.eventDate} {r.shift}</td>
                <td>{isThekedar ? `${r.waiters} waiter, ${r.captains} captain` : user.role === "CATERER" ? `${r.plates} plates` : "-"}</td>
                <td>Rs {r.amount}</td>
                <td>{r.requestedBy}{r.subBooking && " (sub-booking)"}</td>
                <td><span className="badge">{r.status}</span></td>
                <td>
                  {r.status === "PENDING" && (
                    <>
                      <button className="green" onClick={() => respond(r.itemId, true)}>Accept</button>
                      <button className="red" onClick={() => respond(r.itemId, false)}>Reject</button>
                      {user.role === "CATERER" && !r.subBooking && (
                        <button className="gray" onClick={() => setOpen(open === r.itemId ? null : r.itemId)}>Sub-book</button>
                      )}
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {openItem && <SubBook key={openItem.itemId} item={openItem} onDone={loadReqs} />}
      </div>
    </>
  );
}