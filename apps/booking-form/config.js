/*
 * Runtime configuration for the static booking frontend.
 *
 * Local development uses the defaults below. For a hosted deployment, replace
 * these values during deployment or provide the same object from the hosting
 * environment before loading the page scripts.
 */
window.APPOINTMENT_CONFIG = Object.freeze({
  apiBaseUrl: "http://localhost:8080",
  paymentWebhookUrl: "http://localhost:5678/webhook/payment-confirmed",
});
