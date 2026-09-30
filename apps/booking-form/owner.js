const API_BASE_URL = "http://localhost:8080";
const params = new URLSearchParams(window.location.search);
const bookingReference = params.get("bookingReference");

const title = document.querySelector("#review-title");
const pill = document.querySelector("#status-pill");
const details = document.querySelector("#booking-details");
const actions = document.querySelector("#review-actions");
const approveButton = document.querySelector("#approve-button");
const rejectButton = document.querySelector("#reject-button");
const reviewStatus = document.querySelector("#review-status");

function escapeHtml(value) {
  return String(value ?? "—").replace(/[&<>'"]/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;",
  }[character]));
}

function formatDate(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-ZA", { dateStyle: "full", timeZone: "Africa/Johannesburg" })
    .format(new Date(`${value}T12:00:00+02:00`));
}

function renderBooking(booking) {
  title.textContent = booking.bookingReference;
  pill.textContent = booking.approvalStatus.replaceAll("_", " ");
  pill.className = `status-pill ${booking.approvalStatus.toLowerCase()}`;
  details.innerHTML = [
    ["Client", booking.clientName],
    ["Service", booking.serviceId],
    ["Date", formatDate(booking.date)],
    ["Time", `${booking.start || "—"} – ${booking.end || "—"}`],
    ["Phone", booking.phone],
    ["Email", booking.email],
    ["Notes", booking.notes],
    ["Payment", booking.paymentStatus],
  ].map(([label, value]) => `<div class="detail-item"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`).join("");

  const canDecide = booking.approvalStatus === "PENDING_REVIEW";
  actions.hidden = !canDecide;
  if (!canDecide) {
    reviewStatus.textContent = `This request is already ${booking.approvalStatus.toLowerCase().replaceAll("_", " ")}.`;
  }
}

async function loadBooking() {
  if (!bookingReference) {
    title.textContent = "Missing booking reference";
    pill.textContent = "Invalid link";
    details.innerHTML = '<p class="review-message">This approval link does not contain a booking reference.</p>';
    return;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/bookings/${encodeURIComponent(bookingReference)}`);
    if (!response.ok) throw new Error("Booking could not be found.");
    renderBooking(await response.json());
  } catch (error) {
    title.textContent = "Unable to load request";
    pill.textContent = "Error";
    details.innerHTML = `<p class="review-message">${escapeHtml(error.message)}</p>`;
  }
}

async function decide(approvalStatus) {
  approveButton.disabled = true;
  rejectButton.disabled = true;
  reviewStatus.textContent = `Saving ${approvalStatus.toLowerCase()} decision…`;
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/bookings/${encodeURIComponent(bookingReference)}/approval`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ approvalStatus }),
    });
    const body = await response.json();
    if (!response.ok) throw new Error(body.detail || body.message || "The decision could not be saved.");
    renderBooking(body);
    reviewStatus.textContent = approvalStatus === "APPROVED"
      ? "Approved. The automation will continue with the payment step."
      : "Rejected. The request has been returned to the workflow.";
  } catch (error) {
    reviewStatus.textContent = error.message;
    approveButton.disabled = false;
    rejectButton.disabled = false;
  }
}

approveButton.addEventListener("click", () => decide("APPROVED"));
rejectButton.addEventListener("click", () => decide("REJECTED"));
loadBooking();
