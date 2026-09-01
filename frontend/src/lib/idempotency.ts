/**
 * One key per logical action (e.g. one click of "Add to list"), reused across
 * any retries of that same action, then discarded on success. See
 * components/AddToListButton.tsx for how the key's lifecycle is tied to a
 * single add attempt — generating a fresh key per HTTP request instead would
 * defeat the whole point: the backend couldn't tell a genuine retry apart from
 * a brand-new request.
 */
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}
