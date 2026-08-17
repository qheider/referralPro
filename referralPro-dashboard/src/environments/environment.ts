export const environment = {
  production: false,
  // Derive the API host from however the page itself was loaded (localhost, LAN IP,
  // or Tailscale IP) instead of hardcoding localhost, so `npm start` works the same
  // whether you open http://localhost:4200 or http://<tailscale-ip>:4200.
  apiUrl: `http://${window.location.hostname}:8080/api`
};
