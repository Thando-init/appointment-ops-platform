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
