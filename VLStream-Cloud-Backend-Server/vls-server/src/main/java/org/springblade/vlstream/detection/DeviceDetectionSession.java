package org.springblade.vlstream.detection;

/**
 * Device detection session abstraction: Used to judge whether configurations match, and to manage the start/stop life cycle of the session.
 */
public interface DeviceDetectionSession {

	boolean matches(Long algorithmId, String streamUrl, String modelSourcePath);

	boolean start();

	void stop(String reason);
}
