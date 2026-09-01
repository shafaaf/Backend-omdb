package com.example.movielist.service;

/** The stored outcome of a previous idempotency-key-guarded request, ready to be
 *  replayed verbatim instead of re-running the operation. */
public record IdempotentReplay(int status, String responseBodyJson) {
}
