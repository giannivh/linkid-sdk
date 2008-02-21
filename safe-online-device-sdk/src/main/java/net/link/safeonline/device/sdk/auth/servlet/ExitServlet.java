package net.link.safeonline.device.sdk.auth.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.link.safeonline.device.sdk.ErrorPage;
import net.link.safeonline.device.sdk.auth.saml2.Saml2Handler;
import net.link.safeonline.device.sdk.exception.AuthenticationFinalizationException;

public class ExitServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Saml2Handler handler = Saml2Handler.getSaml2Handler(request);
		try {
			handler.finalizeAuthentication(request, response);
		} catch (AuthenticationFinalizationException e) {
			ErrorPage.errorPage(e.getMessage(), response);
			return;
		}
	}
}