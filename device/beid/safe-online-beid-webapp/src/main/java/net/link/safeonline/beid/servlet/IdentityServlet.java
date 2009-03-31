/*
 * SafeOnline project.
 *
 * Copyright 2006 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.beid.servlet;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.link.safeonline.authentication.exception.AlreadyRegisteredException;
import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.PkiExpiredException;
import net.link.safeonline.authentication.exception.PkiInvalidException;
import net.link.safeonline.authentication.exception.PkiNotYetValidException;
import net.link.safeonline.authentication.exception.PkiRevokedException;
import net.link.safeonline.authentication.exception.PkiSuspendedException;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.model.beid.BeIdDeviceService;
import net.link.safeonline.pkix.exception.TrustDomainNotFoundException;
import net.link.safeonline.servlet.AbstractStatementServlet;
import net.link.safeonline.shared.SharedConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * The identity servlet implementation. This servlet receives its data from the BeID via the IdentityApplet.
 * 
 * @author fcorneli
 * 
 */
public class IdentityServlet extends AbstractStatementServlet {

    private static final long    serialVersionUID = 1L;

    private static final Log     LOG              = LogFactory.getLog(IdentityServlet.class);

    @EJB(mappedName = BeIdDeviceService.JNDI_BINDING)
    private BeIdDeviceService    beIdDeviceService;

    @EJB(mappedName = SamlAuthorityService.JNDI_BINDING)
    private SamlAuthorityService samlAuthorityService;


    @Override
    protected void processStatement(byte[] statementData, HttpSession session, HttpServletResponse response)
            throws IOException, ServletException {

        String sessionId = session.getId();
        LOG.debug("session Id: " + sessionId);

        ProtocolContext protocolContext = ProtocolContext.getProtocolContext(session);
        try {
            protocolContext.setValidity(samlAuthorityService.getAuthnAssertionValidity());
            protocolContext.setSuccess(false);

            String userId = protocolContext.getSubject();
            String nodeName = protocolContext.getNodeName();
            String operation = null;
            if (protocolContext.getDeviceOperation() != null) {
                operation = protocolContext.getDeviceOperation().name();
            }

            beIdDeviceService.register(sessionId, nodeName, userId, operation, statementData);
            response.setStatus(HttpServletResponse.SC_OK);
            // notify that registration was successful.
            protocolContext.setSuccess(true);
        }

        catch (TrustDomainNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (PermissionDeniedException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (ArgumentIntegrityException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (AlreadyRegisteredException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (PkiRevokedException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (PkiSuspendedException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (PkiExpiredException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (PkiNotYetValidException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (PkiInvalidException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        } catch (NodeNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader(SharedConstants.SAFE_ONLINE_ERROR_HTTP_HEADER, e.getErrorCode());
        }
    }
}