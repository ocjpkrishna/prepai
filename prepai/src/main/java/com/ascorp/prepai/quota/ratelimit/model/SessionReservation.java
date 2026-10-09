package com.ascorp.prepai.quota.ratelimit.model;

/**
 * One session held for one lesson. {@link #commit()} keeps it; closing the reservation without a commit gives it
 * back, so a failure anywhere in the lesson never uses up the student's quota (spec 4.2).
 */
public final class SessionReservation implements AutoCloseable {

	private final Runnable release;
	private boolean committed;
	private boolean released;

	public SessionReservation(Runnable release) {
		this.release = release;
	}

	public void commit() {
		committed = true;
	}

	@Override
	public void close() {
		if (!committed && !released) {
			released = true;
			release.run();
		}
	}
}
