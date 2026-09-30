const API_BASE_URL = "http://localhost:8080";
const N8N_PAYMENT_WEBHOOK_URL = "http://localhost:5678/webhook/payment-confirmed";
const bookingReference = new URLSearchParams(window.location.search).get("bookingReference");
const title = document.querySelector("#payment-title");
const pill = document.querySelector("#payment-pill");
const details = document.querySelector("#payment-details");
const button = document.querySelector("#pay-button");
const status = document.querySelector("#payment-status");

function escapeHtml(value) {
  return String(value ?? "—").replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character]));
}

function render(booking) {
  title.textContent = booking.bookingReference;
  pill.textContent = booking.paymentStatus.replaceAll("_", " ");
  pill.className = `status-pill ${booking.paymentStatus.toLowerCase()}`;
  details.innerHTML = [
    ["Client", booking.clientName],
    ["Service", booking.serviceId],
    ["Date", booking.date],
    ["Time", `${booking.start} – ${booking.end}`],
    ["Approval", booking.approvalStatus],
  ].map(([label, value]) => `<div class="detail-item"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`).join("");
  button.disabled = booking.approvalStatus !== "APPROVED" || booking.paymentStatus === "PAID";
  if (booking.paymentStatus === "PAID") status.textContent = "Payment is already confirmed. The calendar step can be replayed safely.";
  if (booking.approvalStatus !== "APPROVED") status.textContent = "This booking must be approved before payment can be confirmed.";
}

async function loadBooking() {
  if (!bookingReference) { title.textContent = "Missing booking reference"; status.textContent = "This payment link is incomplete."; return; }
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/bookings/${encodeURIComponent(bookingReference)}`);
    if (!response.ok) throw new Error("Booking could not be found.");
    render(await response.json());
  } catch (error) { title.textContent = "Unable to load booking"; status.textContent = error.message; }
}

button.addEventListener("click", async () => {
  button.disabled = true;
  status.textContent = "Sending the demo payment confirmation…";
  try {
    const response = await fetch(N8N_PAYMENT_WEBHOOK_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ bookingReference, paymentStatus: "PAID" }),
    });
    if (!response.ok) throw new Error("The automation could not confirm the demo payment.");
    status.textContent = "Payment confirmed. The appointment is being added to Google Calendar.";
    pill.textContent = "PAID";
    pill.className = "status-pill approved";
  } catch (error) { status.textContent = error.message; button.disabled = false; }
});

loadBooking();
