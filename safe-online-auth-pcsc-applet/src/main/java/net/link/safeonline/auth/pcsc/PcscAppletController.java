/*
 * SafeOnline project.
 * 
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.auth.pcsc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import javax.smartcardio.CardTerminals.State;

import net.link.safeonline.applet.AppletController;
import net.link.safeonline.applet.AppletView;
import net.link.safeonline.applet.InfoLevel;
import net.link.safeonline.applet.RuntimeContext;
import net.link.safeonline.applet.StatementProvider;
import net.link.safeonline.auth.pcsc.AuthenticationMessages.KEY;
import net.link.safeonline.shared.SharedConstants;
import net.link.safeonline.shared.Signer;
import net.link.safeonline.shared.statement.IdentityProvider;

public class PcscAppletController implements AppletController, PcscSignerLogger {

	public static final byte[] BEID_ATR_11 = new byte[] { 0x3b, (byte) 0x98,
			0x13, 0x40, 0x0a, (byte) 0xa5, 0x03, 0x01, 0x01, 0x01, (byte) 0xad,
			0x13, 0x11 };

	public static final byte[] BEID_ATR_10 = new byte[] { 0x3b, (byte) 0x98,
			(byte) 0x94, 0x40, 0x0a, (byte) 0xa5, 0x03, 0x01, 0x01, 0x01,
			(byte) 0xad, 0x13, 0x10 };

	private AppletView appletView;

	private RuntimeContext runtimeContext;

	private StatementProvider statementProvider;

	private AuthenticationMessages messages;

	public void init(AppletView appletView, RuntimeContext runtimeContext,
			StatementProvider statementProvider) {
		this.appletView = appletView;
		this.runtimeContext = runtimeContext;
		this.statementProvider = statementProvider;

		Locale locale = this.runtimeContext.getLocale();
		this.messages = new AuthenticationMessages(locale);
	}

	public void run() {
		this.appletView.outputInfoMessage(InfoLevel.NORMAL, this.messages
				.getString(KEY.START));
		Card card = openCard();
		if (null == card) {
			return;
		}
		try {
			CardChannel channel = card.getBasicChannel();
			Signer signer = new PcscSigner(channel, this);
			IdentityProvider identityProvider = new PcscIdentityProvider(
					channel);
			byte[] statement = this.statementProvider.createStatement(signer,
					identityProvider);
			try {
				sendStatement(statement);
			} catch (IOException e) {
				this.appletView.outputDetailMessage("IO error: "
						+ e.getMessage());
				this.appletView.outputInfoMessage(InfoLevel.ERROR,
						this.messages.getString(KEY.ERROR));
			}
		} catch (Exception e) {
			this.appletView.outputDetailMessage("error: " + e.getMessage());
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.ERROR));
		} finally {
			closeCard(card);
		}
	}

	private boolean sendStatement(byte[] statement) throws IOException {
		this.appletView.outputInfoMessage(InfoLevel.NORMAL, this.messages
				.getString(KEY.SENDING));
		this.appletView.outputDetailMessage("Sending statement...");
		URL documentBase = this.runtimeContext.getDocumentBase();
		this.appletView.outputDetailMessage("document base: " + documentBase);
		String servletPath = this.runtimeContext.getParameter("ServletPath");
		URL url = transformUrl(documentBase, servletPath);
		HttpURLConnection httpURLConnection = (HttpURLConnection) url
				.openConnection();

		httpURLConnection.setRequestMethod("POST");
		httpURLConnection.setAllowUserInteraction(false);
		httpURLConnection.setRequestProperty("Content-type",
				"application/octet-stream");
		httpURLConnection.setDoOutput(true);
		OutputStream outputStream = httpURLConnection.getOutputStream();
		outputStream.write(statement);
		outputStream.close();

		httpURLConnection.connect();

		httpURLConnection.disconnect();

		int responseCode = httpURLConnection.getResponseCode();
		if (200 == responseCode) {
			this.appletView
					.outputDetailMessage("Statement successfully transmitted.");
			return true;
		}
		String safeOnlineResultCode = httpURLConnection
				.getHeaderField(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER);
		if (SharedConstants.PERMISSION_DENIED_ERROR
				.equals(safeOnlineResultCode)) {
			this.appletView
					.outputDetailMessage("PERMISSION DENIED. YOUR EID MIGHT BE IN USE BY ANOTHER USER");
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.PERMISSION_DENIED));
			return false;
		}
		if (SharedConstants.SUBSCRIPTION_NOT_FOUND_ERROR
				.equals(safeOnlineResultCode)) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.NOT_SUBSCRIBED));
			return false;
		}
		if (SharedConstants.SUBJECT_NOT_FOUND_ERROR
				.equals(safeOnlineResultCode)) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.EID_NOT_REGISTERED));
			return false;
		}
		throw new IOException("Response code: " + responseCode);
	}

	public static URL transformUrl(URL documentBase, String targetPath) {
		if (targetPath.startsWith("http://")
				|| targetPath.startsWith("https://"))
			try {
				return new URL(targetPath);
			} catch (MalformedURLException e) {
				throw new RuntimeException("URL error: " + e.getMessage());
			}

		String documentBaseStr = documentBase.toString();
		int idx = documentBaseStr.lastIndexOf("/");
		String identityUrlStr = documentBaseStr.substring(0, idx + 1)
				+ targetPath;
		try {
			return new URL(identityUrlStr);
		} catch (MalformedURLException e) {
			throw new RuntimeException("URL error: " + e.getMessage());
		}
	}

	private void closeCard(Card card) {
		try {
			card.disconnect(false);
		} catch (CardException e) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.ERROR));
			this.appletView.outputDetailMessage("error message: "
					+ e.getMessage());
		}
	}

	private Card openCard() {
		TerminalFactory factory = TerminalFactory.getDefault();
		CardTerminals terminals = factory.terminals();
		List<CardTerminal> terminalList;
		try {
			terminalList = terminals.list(State.CARD_PRESENT);
			for (CardTerminal cardTerminal : terminalList) {
				this.appletView.outputDetailMessage("trying card terminal: "
						+ cardTerminal.getName());
				Card card = cardTerminal.connect("T=0");
				ATR atr = card.getATR();
				byte[] atrBytes = atr.getBytes();
				if (false == Arrays.equals(BEID_ATR_11, atrBytes)
						&& false == Arrays.equals(BEID_ATR_10, atrBytes)) {
					continue;
				}
				this.appletView.outputDetailMessage("BeID card found.");
				return card;
			}
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.NO_BEID));
			return null;
		} catch (CardException e) {
			this.appletView.outputInfoMessage(InfoLevel.ERROR, this.messages
					.getString(KEY.ERROR));
			this.appletView.outputDetailMessage("error message: "
					+ e.getMessage());
			return null;
		}
	}

	@Override
	public void log(String message) {
		this.appletView.outputDetailMessage(message);
	}
}
