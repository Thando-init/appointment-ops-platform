// ---------- DOM references ----------
// These references identify the parts of the catalogue-first page controlled by JavaScript.
const form = document.querySelector("#booking-form");
const serviceChoices = [...document.querySelectorAll(".service-choice")];
const selectionSummary = document.querySelector("#selection-summary");
const selectedServiceName = document.querySelector("#selected-service-name");
const selectedServiceDetail = document.querySelector("#selected-service-detail");
const serviceRequested = document.querySelector("#serviceRequested");
const serviceId = document.querySelector("#serviceId");
const durationMinutes = document.querySelector("#durationMinutes");
const quotedAmount = document.querySelector("#quotedAmount");
const requiresApproval = document.querySelector("#requiresApproval");
const submitButton = document.querySelector("#submit-button");
const formStatus = document.querySelector("#form-status");
const successState = document.querySelector("#success-state");
const startOverButton = document.querySelector("#start-over-button");
const notes = document.querySelector("#notes");
const notesCount = document.querySelector("#notes-count");
const inspirationImage = document.querySelector("#inspirationImage");
const uploadBox = document.querySelector("#upload-box");
const filePreview = document.querySelector("#file-preview");
const preferredDate = document.querySelector("#preferredDate");
const preferredTime = document.querySelector("#preferredTime");
const dateHelp = document.querySelector("#date-help");
const slotHelp = document.querySelector("#slot-help");


// ---------- Static Tilted Card-inspired interaction ----------
// This small enhancement reproduces the visual idea without importing React,
// Motion, or another runtime dependency. It only changes CSS custom properties.
const tiltCards = document.querySelectorAll(".service-card");
const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

if (!reduceMotion) {
  tiltCards.forEach((card) => {
    const surface = card.querySelector(".service-card-content");

    card.addEventListener("pointermove", (event) => {
      const bounds = card.getBoundingClientRect();
      const x = (event.clientX - bounds.left) / bounds.width;
      const y = (event.clientY - bounds.top) / bounds.height;
      const rotateY = ((x - 0.5) * 4).toFixed(2);
      const rotateX = ((0.5 - y) * 4).toFixed(2);

      // Keep the rotation deliberately subtle so service selection remains calm.
      surface.style.setProperty("--rotate-x", `${rotateX}deg`);
      surface.style.setProperty("--rotate-y", `${rotateY}deg`);
    });

    card.addEventListener("pointerleave", () => {
      surface.style.setProperty("--rotate-x", "0deg");
      surface.style.setProperty("--rotate-y", "0deg");
    });
  });
}


// ---------- Runtime configuration ----------
// Leave this empty during local frontend development. When the Java backend
// exists, set this to the backend origin, for example: http://localhost:7070.
const BOOKING_API_BASE_URL = "";

// These values are filled after a slot is selected and sent to the backend so
// the server can perform its own final conflict check before creating a booking.
let selectedSlot = null;

// Prevent dates earlier than today from being selected in the browser.
const today = new Date();
today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
preferredDate.min = today.toISOString().split("T")[0];

// ---------- Demo availability data ----------
// This is temporary seed data for learning and frontend demos. In the real app,
// the availability endpoint will calculate this from the database and calendar.
const demoBlockedPeriods = {
  // These examples create visible unavailable gaps when you test the page.
  "2026-10-03": [
    { start: "09:30", end: "10:30", reason: "Existing booking" },
    { start: "12:00", end: "13:00", reason: "Studio break" },
  ],
};

const OPENING_HOUR = 8;
const CLOSING_HOUR = 17;
const SLOT_INTERVAL_MINUTES = 30;
const DEFAULT_BUFFER_MINUTES = 15;

// Converts a HH:mm string into minutes after midnight for safe comparisons.
function timeToMinutes(time) {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + minutes;
}

// Converts minutes after midnight back into the display format used by the form.
function minutesToTime(totalMinutes) {
  const hours = String(Math.floor(totalMinutes / 60)).padStart(2, "0");
  const minutes = String(totalMinutes % 60).padStart(2, "0");
  return `${hours}:${minutes}`;
}

// Returns true when the proposed appointment overlaps an existing blocked period.
function overlapsBlockedPeriod(startMinutes, endMinutes, period) {
  const blockedStart = timeToMinutes(period.start);
  const blockedEnd = timeToMinutes(period.end);
  return startMinutes < blockedEnd && endMinutes > blockedStart;
}

// Creates local ISO-like strings for the API payload without changing the
// selected date unexpectedly because of browser timezone conversion.
function makeSlotTimestamp(date, time) {
  return `${date}T${time}:00+02:00`;
}

// Calculates available slots for demo mode. The same rules will eventually live
// in the Java availability service, where database and calendar data are known.
function calculateDemoSlots(date, appointmentDuration) {
  const slots = [];
  const blockedPeriods = demoBlockedPeriods[date] || [];
  const totalDuration = appointmentDuration + DEFAULT_BUFFER_MINUTES;
  const openingMinutes = OPENING_HOUR * 60;
  const closingMinutes = CLOSING_HOUR * 60;

  for (
    let startMinutes = openingMinutes;
    startMinutes + totalDuration <= closingMinutes;
    startMinutes += SLOT_INTERVAL_MINUTES
  ) {
    const endMinutes = startMinutes + appointmentDuration;
    const blocked = blockedPeriods.some((period) =>
      overlapsBlockedPeriod(startMinutes, endMinutes + DEFAULT_BUFFER_MINUTES, period)
    );

    if (!blocked) {
      const start = minutesToTime(startMinutes);
      const end = minutesToTime(endMinutes);
      slots.push({
        start: makeSlotTimestamp(date, start),
        end: makeSlotTimestamp(date, end),
        label: start,
      });
    }
  }

  return slots;
}

// ---------- Service catalogue behaviour ----------
// Copies the selected card's data into hidden fields and enables availability.
function selectService(choice) {
  const service = {
    id: choice.value,
    name: choice.dataset.name,
    duration: Number(choice.dataset.duration),
    price: Number(choice.dataset.price),
    requiresApproval: choice.dataset.approval === "true",
  };

  serviceId.value = service.id;
  serviceRequested.value = service.name;
  durationMinutes.value = service.duration;
  quotedAmount.value = service.price;
  requiresApproval.value = String(service.requiresApproval);

  selectedServiceName.textContent = service.name;
  selectedServiceDetail.textContent = service.price === 0
    ? "We’ll help you choose the right service."
    : `${service.duration} minutes · ${service.requiresApproval ? "Quote confirmed after review" : `R${service.price}`}`;

  selectionSummary.classList.add("has-selection");
  preferredDate.disabled = false;
  dateHelp.textContent = "Choose a date to see available times.";
  slotHelp.textContent = `Open slots will fit the full ${service.duration}-minute service.`;
  clearFieldError("serviceRequested");

  // Changing the service changes the required appointment length, so any old
  // date/time selection must be cleared and recalculated.
  selectedSlot = null;
  preferredDate.value = "";
  resetTimeOptions("Choose a date first");
}

serviceChoices.forEach((choice) => {
  // The change event supports mouse, keyboard, and assistive-technology users.
  choice.addEventListener("change", () => selectService(choice));
});

// ---------- Availability loading ----------
// Clears stale time choices before loading slots for a new date or service.
function resetTimeOptions(message) {
  preferredTime.innerHTML = `<option value="" selected disabled>${message}</option>`;
  preferredTime.disabled = true;
  selectedSlot = null;
}

// Renders only available slots returned by the backend or demo calculator.
function renderAvailableSlots(slots) {
  resetTimeOptions(slots.length ? "Choose an available time" : "No slots available");

  if (!slots.length) {
    slotHelp.textContent = "No times fit this service on the selected date. Try another date.";
    return;
  }

  slots.forEach((slot) => {
    const option = document.createElement("option");
    option.value = slot.start;
    option.textContent = `${slot.label} · ends ${slot.end.slice(11, 16)}`;
    option.dataset.end = slot.end;
    preferredTime.appendChild(option);
  });

  preferredTime.disabled = false;
  slotHelp.textContent = `${slots.length} available slot${slots.length === 1 ? "" : "s"} found.`;
}

// Requests availability from the backend when configured, otherwise uses seeded
// demo data so the frontend can be tested before the Java service exists.
async function loadAvailability() {
  if (!serviceId.value || !preferredDate.value) {
    resetTimeOptions("Choose a date first");
    return;
  }

  resetTimeOptions("Checking availability…");
  slotHelp.textContent = "Checking the studio schedule…";

  const params = new URLSearchParams({
    serviceId: serviceId.value,
    date: preferredDate.value,
  });

  try {
    let slots;

    if (BOOKING_API_BASE_URL) {
      const response = await fetch(`${BOOKING_API_BASE_URL}/api/v1/availability/slots?${params}`);
      if (!response.ok) throw new Error("Availability could not be loaded.");
      const data = await response.json();
      slots = data.slots;
    } else {
      // Delay the demo response slightly to make the loading state visible.
      await new Promise((resolve) => setTimeout(resolve, 350));
      slots = calculateDemoSlots(preferredDate.value, Number(durationMinutes.value));
    }

    renderAvailableSlots(slots);
  } catch (error) {
    resetTimeOptions("Availability unavailable");
    slotHelp.textContent = error.message;
  }
}

preferredDate.addEventListener("change", () => {
  clearFieldError("preferredDate");
  loadAvailability();
});

preferredTime.addEventListener("change", () => {
  const option = preferredTime.options[preferredTime.selectedIndex];
  selectedSlot = option?.value
    ? { start: option.value, end: option.dataset.end }
    : null;
  clearFieldError("preferredTime");
});

// ---------- Field-level validation helpers ----------
// Displays an error beside a field and applies the matching error style.
function showFieldError(fieldName, message) {
  const field = document.querySelector(`#${fieldName}`)?.closest(".field");
  const error = document.querySelector(`[data-error-for="${fieldName}"]`);

  if (!field || !error) return;
  field.classList.add("has-error");
  error.textContent = message;
}

// Removes a field error after the user corrects the value.
function clearFieldError(fieldName) {
  const field = document.querySelector(`#${fieldName}`)?.closest(".field");
  const error = document.querySelector(`[data-error-for="${fieldName}"]`);

  if (!field || !error) return;
  field.classList.remove("has-error");
  error.textContent = "";
}

// Validates the selected service, available slot, and contact details.
function validateForm() {
  let isValid = true;
  formStatus.textContent = "";

  ["preferredDate", "preferredTime", "clientName", "phone", "email"].forEach(clearFieldError);

  if (!serviceRequested.value) {
    formStatus.textContent = "Please choose a service to continue.";
    isValid = false;
  }

  if (!preferredDate.value) {
    showFieldError("preferredDate", "Please choose a date.");
    isValid = false;
  }

  if (!selectedSlot || !preferredTime.value) {
    showFieldError("preferredTime", "Please choose one of the available times.");
    isValid = false;
  }

  const clientName = document.querySelector("#clientName");
  if (clientName.value.trim().length < 2) {
    showFieldError("clientName", "Please enter your name.");
    isValid = false;
  }

  const phone = document.querySelector("#phone");
  if (phone.value.trim().length < 7) {
    showFieldError("phone", "Please enter a valid WhatsApp number.");
    isValid = false;
  }

  const email = document.querySelector("#email");
  if (email.value && !email.validity.valid) {
    showFieldError("email", "Please check your email address.");
    isValid = false;
  }

  if (!isValid && !formStatus.textContent) {
    formStatus.textContent = "Please check the highlighted fields.";
  }

  return isValid;
}

// ---------- Payload preparation ----------
// Includes the selected slot timestamps so the backend can recheck availability.
function getPayload() {
  const data = new FormData(form);

  return {
    clientName: data.get("clientName").trim(),
    phone: data.get("phone").trim(),
    email: data.get("email")?.trim() || null,
    serviceRequested: data.get("serviceRequested"),
    serviceId: data.get("serviceId"),
    durationMinutes: Number(data.get("durationMinutes")),
    quotedAmount: Number(data.get("quotedAmount")),
    requiresApproval: data.get("requiresApproval") === "true",
    preferredDate: data.get("preferredDate"),
    preferredTime: data.get("preferredTime"),
    selectedStart: selectedSlot.start,
    selectedEnd: selectedSlot.end,
    notes: data.get("notes")?.trim() || null,
    source: "website",
  };
}

// Creates a temporary reference for frontend-only demo mode. The backend will
// eventually generate the authoritative booking ID after its conflict check.
function makeDemoReference() {
  const date = new Date().toISOString().slice(0, 10).replaceAll("-", "");
  const suffix = Math.floor(Math.random() * 9000) + 1000;
  return `SALON-${date}-${suffix}`;
}

// Uses demo mode if no webhook is configured; otherwise sends JSON to n8n.
async function submitBooking(payload) {
  if (!N8N_WEBHOOK_URL) {
    // Demo mode does not reserve a real slot. The backend must do that later.
    await new Promise((resolve) => setTimeout(resolve, 650));
    return { bookingId: makeDemoReference() };
  }

  const response = await fetch(N8N_WEBHOOK_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  // A 409 response should eventually be mapped to a slot-refresh message.
  if (response.status === 409) {
    throw new Error("That slot was just taken. Please choose another available time.");
  }

  if (!response.ok) {
    throw new Error("The booking request could not be submitted.");
  }

  return response.json();
}

// Replaces the form with a confirmation message after successful submission.
function showSuccess(reference, name) {
  form.querySelector(".form-step").hidden = true;
  successState.hidden = false;
  document.querySelector("#reference-id").textContent = reference;
  document.querySelector("#success-name").textContent = name.split(" ")[0];
}

// ---------- Form submission ----------
form.addEventListener("submit", async (event) => {
  event.preventDefault();

  if (!validateForm()) return;

  // Disable the button to prevent duplicate submissions while the request runs.
  submitButton.disabled = true;
  submitButton.innerHTML = "Sending request <span class=\"button-spinner\" aria-hidden=\"true\"></span>";
  formStatus.textContent = "Sending your request…";

  try {
    const payload = getPayload();
    const result = await submitBooking(payload);
    showSuccess(result.bookingId || makeDemoReference(), payload.clientName);
  } catch (error) {
    // Restore the button when the network or backend request fails so the user can retry.
    formStatus.textContent = error.message;
    submitButton.disabled = false;
    submitButton.innerHTML = "Send request <span aria-hidden=\"true\">→</span>";
  }
});

// Keeps the optional notes counter visible while the user types.
notes.addEventListener("input", () => {
  notesCount.textContent = notes.value.length;
});

// Shows the selected file name and rejects files larger than 5 MB.
inspirationImage.addEventListener("change", () => {
  const file = inspirationImage.files[0];
  if (!file) return;

  if (file.size > 5 * 1024 * 1024) {
    inspirationImage.value = "";
    filePreview.hidden = false;
    filePreview.innerHTML = "<span>That file is larger than 5MB.</span>";
    return;
  }

  filePreview.hidden = false;
  filePreview.innerHTML = `<span>${file.name}</span><button type="button" id="remove-file">Remove</button>`;
  document.querySelector("#remove-file").addEventListener("click", () => {
    inspirationImage.value = "";
    filePreview.hidden = true;
  });
});

// Provides drag-and-drop feedback around the upload area.
["dragenter", "dragover"].forEach((eventName) => {
  uploadBox.addEventListener(eventName, (event) => {
    event.preventDefault();
    uploadBox.classList.add("is-dragging");
  });
});

["dragleave", "drop"].forEach((eventName) => {
  uploadBox.addEventListener(eventName, (event) => {
    event.preventDefault();
    uploadBox.classList.remove("is-dragging");
  });
});

// Copies a dropped file into the hidden input so both upload methods share one path.
uploadBox.addEventListener("drop", (event) => {
  const file = event.dataTransfer.files[0];
  if (!file) return;

  const transfer = new DataTransfer();
  transfer.items.add(file);
  inspirationImage.files = transfer.files;
  inspirationImage.dispatchEvent(new Event("change"));
});

// Restores the initial state so the demo can be repeated without refreshing.
startOverButton.addEventListener("click", () => {
  form.reset();
  serviceRequested.value = "";
  serviceId.value = "";
  durationMinutes.value = "";
  quotedAmount.value = "";
  requiresApproval.value = "";
  preferredDate.value = "";
  preferredDate.disabled = true;
  dateHelp.textContent = "Choose a service first.";
  resetTimeOptions("Choose a date first");
  selectedSlot = null;
  selectedServiceName.textContent = "No service selected";
  selectedServiceDetail.textContent = "Choose a service above to continue.";
  notesCount.textContent = "0";
  filePreview.hidden = true;
  successState.hidden = true;
  form.querySelector(".form-step").hidden = false;
  submitButton.disabled = false;
  submitButton.innerHTML = "Send request <span aria-hidden=\"true\">→</span>";
  formStatus.textContent = "";
});

// Clear a field's error as soon as the user edits or changes it.
document.querySelectorAll("input, select, textarea").forEach((field) => {
  field.addEventListener("input", () => clearFieldError(field.id));
  field.addEventListener("change", () => clearFieldError(field.id));
});
