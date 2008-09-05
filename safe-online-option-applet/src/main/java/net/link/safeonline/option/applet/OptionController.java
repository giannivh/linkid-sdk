/*
 * SafeOnline project.
 * 
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.option.applet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import javax.xml.namespace.QName;

import net.link.safeonline.applet.AppletControl;
import net.link.safeonline.applet.AppletController;
import net.link.safeonline.applet.AppletView;
import net.link.safeonline.applet.InfoLevel;
import net.link.safeonline.applet.PinDialog;
import net.link.safeonline.applet.RuntimeContext;
import net.link.safeonline.applet.StatementProvider;
import net.link.safeonline.option.applet.OptionMessages.KEY;
import net.link.safeonline.option.connection.manager.ws.ConnectionManagerConstants;
import net.link.safeonline.option.connection.manager.ws.generated.ConnectionManager;
import net.link.safeonline.option.connection.manager.ws.generated.ConnectionManagerService;
import net.link.safeonline.shared.SharedConstants;

/**
 * <h2>{@link OptionController}<br>
 * <sub>[in short] (TODO).</sub></h2>
 * 
 * <p>
 * [description / usage].
 * </p>
 * 
 * <p>
 * <i>Sep 4, 2008</i>
 * </p>
 * 
 * @author dhouthoo
 */
public class OptionController implements AppletController {

	private AppletView appletView;

	private RuntimeContext runtimeContext;

	private OptionMessages messages;

	private ConnectionManager port = null;

	/**
	 * {@inheritDoc}
	 */
	public void abort() {

		// empty
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(AppletView appletView, RuntimeContext runtimeContext,
			StatementProvider statementProvider) {
		this.appletView = appletView;
		this.runtimeContext = runtimeContext;
		Locale locale = this.runtimeContext.getLocale();
		this.messages = new OptionMessages(locale);
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		this.appletView.outputInfoMessage(InfoLevel.NORMAL, this.messages
				.getString(KEY.START));

		if (null == this.port) {
			try {
				this.appletView
						.outputDetailMessage("Contacting connection manager");
				ConnectionManagerService service = new ConnectionManagerService(
						new URL(ConnectionManagerConstants.URL + "?wsdl"),
						new QName(ConnectionManagerConstants.NAMESPACE,
								ConnectionManagerConstants.LOCALPART));
				this.port = service.getConnectionManagerPort();
			} catch (Throwable e) {
				this.appletView.outputInfoMessage(InfoLevel.ERROR,
						this.messages.getString(KEY.ERROR));
				this.appletView
						.outputDetailMessage("An error occured while contacting the connection manager");
				return;
			}
		}

		String IMEI = this.port.getIMEI();

		if (null == IMEI) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.ERROR));
			this.appletView
					.outputDetailMessage("Could not read IMEI from connection manager");
			return;
		}

		this.appletView.outputDetailMessage("Found datacard  with IMEI: "
				+ IMEI);

		this.appletView.outputInfoMessage(InfoLevel.NORMAL, this.messages
				.getString(KEY.PIN));
		this.appletView.outputDetailMessage("Reading PIN code");

		PinDialog pinDialog = new PinDialog();
		String pin = pinDialog.getPin();

		this.appletView.outputInfoMessage(InfoLevel.NORMAL, this.messages
				.getString(KEY.SENDING));
		this.appletView.outputDetailMessage("Sending data");
		PostResult result = null;
		try {
			result = postData(IMEI, pin);
		} catch (Exception e) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.ERROR));
			this.appletView
					.outputDetailMessage("Could not send login data to server");
			return;
		}

		if (SharedConstants.PERMISSION_DENIED_ERROR.equals(result
				.getResponseCode())) {
			this.appletView
					.outputDetailMessage("PERMISSION DENIED. YOUR DATACARD MIGHT BE IN USE BY ANOTHER USER");
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.PERMISSION_DENIED));
			return;
		}
		if (SharedConstants.SUBSCRIPTION_NOT_FOUND_ERROR.equals(result
				.getResponseCode())) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.NOT_SUBSCRIBED));
			return;
		}
		if (SharedConstants.SUBJECT_NOT_FOUND_ERROR.equals(result
				.getResponseCode())) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.DATACARD_NOT_REGISTERED));
			this.appletView.outputDetailMessage(this.messages
					.getString(KEY.DATACARD_NOT_REGISTERED));
			this.appletView
					.outputDetailMessage("Please login with another authentication device first.");
			return;
		}

		this.appletView.outputInfoMessage(InfoLevel.NORMAL, this.messages
				.getString(KEY.DONE));
	}

	private PostResult postData(String IMEI, String pin) throws IOException {

		URL documentBase = this.runtimeContext.getDocumentBase();
		String servletPath = this.runtimeContext.getParameter("ServletPath");
		URL url = AppletControl.transformUrl(documentBase, servletPath);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		String content = "imei=" + URLEncoder.encode(IMEI, "UTF-8") + "&pin="
				+ URLEncoder.encode(pin, "UTF-8");
		connection.setRequestProperty("Content-length", Integer
				.toString(content.getBytes().length));
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setAllowUserInteraction(false);
		connection.setUseCaches(false);
		DataOutputStream output = new DataOutputStream(connection
				.getOutputStream());
		output.writeBytes(content);
		output.flush();
		output.close();
		return new PostResult(connection);
	}

	private class PostResult {
		private int responseCode;
		private String message = null;

		public PostResult(HttpURLConnection connection) throws IOException {
			this.responseCode = connection.getResponseCode();
			if (200 != this.responseCode) {
				this.message = connection
						.getHeaderField(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER);
			}
		}

		public int getResponseCode() {

			return this.responseCode;
		}

		public String getMessage() {

			return this.message;
		}

	}
}
