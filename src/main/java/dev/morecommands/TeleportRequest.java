package dev.morecommands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Represents a pending teleport request (/tpa or /tpahere). */
public final class TeleportRequest {
	public enum Type {
		TPA,
		TPAHERE
	}

	private final UUID sender;
	private final UUID target;
	private final Type type;
	private final long timestamp;

	public TeleportRequest(UUID sender, UUID target, Type type) {
		this.sender = sender;
		this.target = target;
		this.type = type;
		this.timestamp = System.currentTimeMillis();
	}

	public UUID getSender() {
		return sender;
	}

	public UUID getTarget() {
		return target;
	}

	public Type getType() {
		return type;
	}

	public boolean isExpired() {
		return System.currentTimeMillis() - timestamp > (Config.get().tpaTimeoutSeconds * 1000L);
	}

	private static final List<TeleportRequest> REQUESTS = new ArrayList<>();

	public static synchronized void addRequest(TeleportRequest request) {
		cleanExpired();
		REQUESTS.removeIf(req -> req.sender.equals(request.sender) && req.target.equals(request.target));
		REQUESTS.add(request);
	}

	public static synchronized TeleportRequest getLatestIncoming(UUID target) {
		cleanExpired();
		for (int i = REQUESTS.size() - 1; i >= 0; i--) {
			TeleportRequest req = REQUESTS.get(i);
			if (req.target.equals(target)) {
				return req;
			}
		}
		return null;
	}

	public static synchronized TeleportRequest getLatestOutgoing(UUID sender) {
		cleanExpired();
		for (int i = REQUESTS.size() - 1; i >= 0; i--) {
			TeleportRequest req = REQUESTS.get(i);
			if (req.sender.equals(sender)) {
				return req;
			}
		}
		return null;
	}

	public static synchronized void removeRequest(TeleportRequest request) {
		REQUESTS.remove(request);
	}

	private static void cleanExpired() {
		long now = System.currentTimeMillis();
		long timeoutMs = Config.get().tpaTimeoutSeconds * 1000L;
		REQUESTS.removeIf(req -> now - req.timestamp > timeoutMs);
	}
}
