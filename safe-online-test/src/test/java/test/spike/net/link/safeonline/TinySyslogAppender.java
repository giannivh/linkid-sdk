/*
 *   Copyright 2007, Maarten Billemont
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.link.safeonline.audit;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 * <h2>{@link TinySyslogAppender} - log4j appender that appends events onto the
 * syslog.</h2>
 * <p>
 * This implementation requires syslog to be listening for UDP messages on
 * either the default syslog port (514) or a port provided in code.<br>
 * Messages will be redirected as configured by your syslog configuration, so
 * make sure you know how your syslog daemon is configured to treat messages
 * from the UDP source.
 * </p>
 * <p>
 * <i>Dec 5, 2007</i>
 * </p>
 * 
 * @author mbillemo
 */
public class TinySyslogAppender extends AppenderSkeleton {

	private static final String DEFAULT_SYSLOG_HOST = "localhost";
	private static final int DEFAULT_SYSLOG_PORT = 514;

	private DatagramSocket socket;
	private InetSocketAddress syslog;
	private Facility facility;

	/**
	 * Sends events to the syslog daemon on localhost at port 514.
	 * 
	 * @param facility
	 *            The syslog facility to send messages to. This defaults to
	 *            {@link Facility#USER} if <code>null</code> is given.
	 */
	public TinySyslogAppender(Facility facility) {

		this(facility, DEFAULT_SYSLOG_HOST, DEFAULT_SYSLOG_PORT);
	}

	/**
	 * Sends events to the syslog daemon at port 514.
	 * 
	 * @param facility
	 *            The syslog facility to send messages to. This defaults to
	 *            {@link Facility#USER} if <code>null</code> is given.
	 * @param host
	 *            The IP address or hostname of the syslog daemon.
	 */
	public TinySyslogAppender(Facility facility, String host) {

		this(facility, host, DEFAULT_SYSLOG_PORT);
	}

	/**
	 * Sends events to the given syslog daemon.
	 * 
	 * @param facility
	 *            The syslog facility to send messages to. This defaults to
	 *            {@link Facility#USER} if <code>null</code> is given.
	 * @param host
	 *            The IP address or hostname of the syslog daemon.
	 * @param port
	 *            The UDP port on which the syslog daemon is listening.
	 */
	public TinySyslogAppender(Facility facility, String host, int port) {

		setFacility(facility);
		setRemote(host, port);
	}

	/**
	 * @param facility
	 *            The syslog facility to send messages to. This defaults to
	 *            {@link Facility#USER} if <code>null</code> is given.
	 */
	public void setFacility(Facility facility) {

		if (facility == null)
			this.facility = Facility.USER;
		else
			this.facility = facility;
	}

	/**
	 * @param host
	 *            The IP address or hostname of the syslog daemon.
	 * @param port
	 *            The UDP port on which the syslog daemon is listening.
	 * 
	 * @return <code>false</code>: No local UDP socket could be created.
	 */
	public boolean setRemote(String host, int port) {

		this.syslog = new InetSocketAddress(host, port);

		if (this.socket == null)
			try {
				this.socket = new DatagramSocket();
			} catch (SocketException e) {
				LogLog.error(
						"Couldn't create an UDP socket for communication with syslog on "
								+ host + ":" + port, e);
				return false;
			}

		return true;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void append(LoggingEvent event) {

		if (!isAsSevereAsThreshold(event.getLevel()))
			return;

		if (null == this.socket) {
			this.errorHandler
					.error("No syslog socket available.  Did you forget to connect()?");
			return;
		}

		send(event.getLevel(), format(event));
	}

	/**
	 * TODO: Describe method.
	 */
	private String format(LoggingEvent event) {

		return String.format("%s: [%5s] %s",
				event.getLocationInformation().fullInfo, event.getLevel(),
				event.getRenderedMessage());
	}

	private void send(Level level, String message) {

		// Split at RFC 3164 limit of 1024 bytes.
		byte[] bytes = message.getBytes();
		if (bytes.length <= 1029)
			try {
				// Prepend the message with the facility and level.
				bytes = String.format("<%d> %s",
						this.facility.getId() | level.getSyslogEquivalent(),
						message).getBytes();

				// Create a packet for the message and dispatch it.
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
						this.syslog);
				this.socket.send(packet);
			}

			catch (IOException e) {
				LogLog.error("Couldn't dispatch packet of " + bytes.length
						+ " bytes to syslog at " + this.syslog, e);
			}

		else {
			int split = message.length() / 2;
			send(level, message.substring(0, split) + "...");
			send(level, "..." + message.substring(split));
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void close() {

		this.socket.close();
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public boolean requiresLayout() {

		return false;
	}

	public static enum Facility {

		/** Kernel messages */
		KERN(0),
		/** Random user-level messages */
		USER(1 << 3),
		/** Mail system */
		MAIL(2 << 3),
		/** System daemons */
		DAEMON(3 << 3),
		/** security/authorization messages */
		AUTH(4 << 3),
		/** messages generated internally by syslogd */
		SYSLOG(5 << 3),

		/** line printer subsystem */
		LPR(6 << 3),
		/** network news subsystem */
		NEWS(7 << 3),
		/** UUCP subsystem */
		UUCP(8 << 3),
		/** clock daemon */
		CRON(9 << 3),
		/** security/authorization messages (private) */
		AUTHPRIV(10 << 3),
		/** ftp daemon */
		FTP(11 << 3),

		// other codes through 15 reserved for system use
		/** reserved for local use */
		LOCAL0(16 << 3),
		/** reserved for local use */
		LOCAL1(17 << 3),
		/** reserved for local use */
		LOCAL2(18 << 3),
		/** reserved for local use */
		LOCAL3(19 << 3),
		/** reserved for local use */
		LOCAL4(20 << 3),
		/** reserved for local use */
		LOCAL5(21 << 3),
		/** reserved for local use */
		LOCAL6(22 << 3),
		/** reserved for local use */
		LOCAL7(23 << 3);

		private int id;

		private Facility(int id) {

			this.id = id;
		}

		public int getId() {

			return this.id;
		}
	}
}