import { OAuth2Client } from "google-auth-library";
import { env } from "../config/env";

const client = new OAuth2Client(env.GOOGLE_CLIENT_ID);

export async function verifyGoogleToken(idToken: string) {
  if (!env.GOOGLE_CLIENT_ID) throw new Error("Google sign-in is not configured");
  const ticket = await client.verifyIdToken({
    idToken,
    audience: env.GOOGLE_CLIENT_ID,
  });

  return ticket.getPayload();
}
