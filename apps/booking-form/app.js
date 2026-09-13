// ---------- DOM references ----------
// These references keep the static page's interactive responsibilities visible.
const form = document.querySelector("#booking-form");
const serviceChoices = [...document.querySelectorAll(".service-choice")];
const selectedServiceName = document.querySelector("#selected-service-name");
const selectedServiceDetail = document.querySelector("#selected-service-detail");
const serviceRequested = document.querySelector("#serviceRequested");
const serviceId = document.querySelector("#serviceId");
const durationMinutes = document.querySelector("#durationMinutes");
const quotedAmount = document.querySelector("#quotedAmount");
const requiresApproval = document.querySelector("#requiresApproval");
const preferredDate = document.querySelector("#preferredDate");
const preferredTime = document.querySelector("#preferredTime");
const dateHelp = document.querySelector("#date-help");
const slotHelp = document.querySelector("#slot-help");
const submitButton = document.querySelector("#submit-button");
const formStatus = document.querySelector("#form-status");
const successState = document.querySelector("#success-state");
const startOverButton = document.querySelector("#start-over-button");
const notes = document.querySelector("#notes");
const notesCount = document.querySelector("#notes-count");

// ---------- Runtime configuration ----------
// Leave empty for demo mode. Replace this with the backend/n8n endpoint later.
const N8N_WEBHOOK_URL = "";
let selectedSlot = null;

// The browser will not allow dates before today.
const today = new Date();
today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
preferredDate.min = today.toISOString().split("T")[0];

// Seeded data demonstrates the same blocked-period logic the backend will use.
const demoBlockedPeriods = {
  "2026-10-03": [{ start: "09:30", end: "10:30" }, { start: "12:00", end: "13:00" }],
};

function timeToMinutes(time) {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + minutes;
}

function minutesToTime(minutes) {
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}

// Produces only slots where the appointment and its 15-minute buffer fit.
function calculateDemoSlots(date, duration) {
  const slots = [];
  const blocked = demoBlockedPeriods[date] || [];
  for (let start = 480; start + duration + 15 <= 1020; start += 30) {
    const endWithBuffer = start + duration + 15;
    const overlaps = blocked.some((period) => start < timeToMinutes(period.end) && endWithBuffer > timeToMinutes(period.start));
    if (!overlaps) slots.push({ start: minutesToTime(start), end: minutesToTime(start + duration) });
  }
  return slots;
}

// Copies the selected service card into the hidden API fields.
function selectService(choice) {
  serviceId.value = choice.value;
  serviceRequested.value = choice.dataset.name;
  durationMinutes.value = choice.dataset.duration;
  quotedAmount.value = choice.dataset.price;
  requiresApproval.value = choice.dataset.approval;
  selectedServiceName.textContent = choice.dataset.name;
  selectedServiceDetail.textContent = `${choice.dataset.duration} minutes · ${choice.dataset.price === "0" ? "Quote after review" : `R${choice.dataset.price}`}`;
  preferredDate.disabled = false;
  dateHelp.textContent = "Choose a date to see available times.";
  preferredDate.value = "";
  resetTimeOptions("Choose a date first");
}

serviceChoices.forEach((choice) => choice.addEventListener("change", () => selectService(choice)));

function resetTimeOptions(message) {
  preferredTime.innerHTML = `<option value="" selected disabled>${message}</option>`;
  preferredTime.disabled = true;
  selectedSlot = null;
}

// Loads demo slots now, or calls the future availability endpoint when configured.
async function loadAvailability() {
  if (!serviceId.value || !preferredDate.value) return;
  resetTimeOptions("Checking availability…");
  slotHelp.textContent = "Checking the studio schedule…";

  try {
    let slots;
    if (N8N_WEBHOOK_URL) {
      const params = new URLSearchParams({ serviceId: serviceId.value, date: preferredDate.value });
      const response = await fetch(`${N8N_WEBHOOK_URL}/availability/slots?${params}`);
      if (!response.ok) throw new Error("Availability could not be loaded.");
      slots = (await response.json()).slots;
    } else {
      await new Promise((resolve) => setTimeout(resolve, 300));
      slots = calculateDemoSlots(preferredDate.value, Number(durationMinutes.value));
    }

    if (!slots.length) {
      slotHelp.textContent = "No times fit this service. Try another date.";
      resetTimeOptions("No slots available");
      return;
    }

    preferredTime.innerHTML = '<option value="" selected disabled>Choose an available time</option>';
    slots.forEach((slot) => {
      const option = document.createElement("option");
      option.value = slot.start;
      option.dataset.end = slot.end;
      option.textContent = `${slot.start} · ends ${slot.end}`;
      preferredTime.appendChild(option);
    });
    preferredTime.disabled = false;
    slotHelp.textContent = `${slots.length} available slots found.`;
  } catch (error) {
    resetTimeOptions("Availability unavailable");
    slotHelp.textContent = error.message;
  }
}

preferredDate.addEventListener("change", () => { clearFieldError("preferredDate"); loadAvailability(); });
preferredTime.addEventListener("change", () => {
  const option = preferredTime.options[preferredTime.selectedIndex];
  selectedSlot = option.value ? { start: option.value, end: option.dataset.end } : null;
  clearFieldError("preferredTime");
});

function showFieldError(name, message) {
  const field = document.querySelector(`#${name}`)?.closest(".field");
  const error = document.querySelector(`[data-error-for="${name}"]`);
  if (field && error) { field.classList.add("has-error"); error.textContent = message; }
}

function clearFieldError(name) {
  const field = document.querySelector(`#${name}`)?.closest(".field");
  const error = document.querySelector(`[data-error-for="${name}"]`);
  if (field && error) { field.classList.remove("has-error"); error.textContent = ""; }
}

function validateForm() {
  let valid = true;
  formStatus.textContent = "";
  ["preferredDate", "preferredTime", "clientName", "phone", "email"].forEach(clearFieldError);
  if (!serviceRequested.value) { formStatus.textContent = "Please choose a service."; valid = false; }
  if (!preferredDate.value) { showFieldError("preferredDate", "Please choose a date."); valid = false; }
  if (!selectedSlot) { showFieldError("preferredTime", "Please choose an available time."); valid = false; }
  if (document.querySelector("#clientName").value.trim().length < 2) { showFieldError("clientName", "Please enter your name."); valid = false; }
  if (document.querySelector("#phone").value.trim().length < 7) { showFieldError("phone", "Please enter a valid WhatsApp number."); valid = false; }
  if (document.querySelector("#email").value && !document.querySelector("#email").validity.valid) { showFieldError("email", "Please check your email."); valid = false; }
  if (!valid && !formStatus.textContent) formStatus.textContent = "Please check the highlighted fields.";
  return valid;
}

// The backend must repeat this availability check before creating a real booking.
function getPayload() {
  const data = new FormData(form);
  return { clientName: data.get("clientName").trim(), phone: data.get("phone").trim(), email: data.get("email") || null, serviceId: data.get("serviceId"), serviceRequested: data.get("serviceRequested"), durationMinutes: Number(data.get("durationMinutes")), quotedAmount: Number(data.get("quotedAmount")), requiresApproval: data.get("requiresApproval") === "true", preferredDate: data.get("preferredDate"), preferredTime: data.get("preferredTime"), selectedStart: `${data.get("preferredDate")}T${data.get("preferredTime")}:00+02:00`, selectedEnd: `${data.get("preferredDate")}T${selectedSlot.end}:00+02:00`, notes: data.get("notes") || null, source: "website" };
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!validateForm()) return;
  submitButton.disabled = true;
  formStatus.textContent = "Sending your request…";
  try {
    if (N8N_WEBHOOK_URL) {
      const response = await fetch(N8N_WEBHOOK_URL, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(getPayload()) });
      if (response.status === 409) throw new Error("That slot was just taken. Please choose another time.");
      if (!response.ok) throw new Error("The booking request could not be submitted.");
    } else {
      await new Promise((resolve) => setTimeout(resolve, 650));
    }
    form.querySelectorAll(".field, .availability-note, .form-actions, .form-status").forEach((element) => { element.hidden = true; });
    successState.hidden = false;
  } catch (error) {
    formStatus.textContent = error.message;
    submitButton.disabled = false;
  }
});

notes.addEventListener("input", () => { notesCount.textContent = notes.value.length; });
startOverButton.addEventListener("click", () => window.location.reload());
document.querySelectorAll("input, select, textarea").forEach((field) => { field.addEventListener("input", () => clearFieldError(field.id)); field.addEventListener("change", () => clearFieldError(field.id)); });
