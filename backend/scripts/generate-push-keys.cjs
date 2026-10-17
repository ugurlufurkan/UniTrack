const fs = require("node:fs");
const path = require("node:path");
const webPush = require("web-push");
const directory = path.resolve(__dirname, "../../ops/keys");
fs.mkdirSync(directory, { recursive: true, mode: 0o700 });
const keys = webPush.generateVAPIDKeys();
const output = path.join(directory, "vapid.env");
fs.writeFileSync(output, `VAPID_PUBLIC_KEY=${keys.publicKey}\nVAPID_PRIVATE_KEY=${keys.privateKey}\n`, { mode: 0o600, flag: "wx" });
console.log("VAPID keys saved to ignored ops/keys/vapid.env. Copy them into ops/.env; private key was not printed.");
