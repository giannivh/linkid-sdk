/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.digipass.webapp;

import javax.ejb.EJB;

import net.link.safeonline.authentication.exception.ArgumentIntegrityException;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.model.digipass.DigipassDeviceService;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.exception.WSClientTransportException;
import net.link.safeonline.sdk.ws.idmapping.NameIdentifierMappingClient;
import net.link.safeonline.util.ee.AuthIdentityServiceClient;
import net.link.safeonline.webapp.components.ErrorComponentFeedbackLabel;
import net.link.safeonline.webapp.components.ErrorFeedbackPanel;
import net.link.safeonline.webapp.template.TemplatePage;
import net.link.safeonline.wicket.tools.WicketUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.StringValidator;


public class RegisterPage extends TemplatePage {

    private static final long       serialVersionUID      = 1L;

    static final Log                LOG                   = LogFactory.getLog(RegisterPage.class);

    public static final String      REGISTER_FORM_ID      = "register_form";

    public static final String      LOGIN_FIELD_ID        = "login";

    public static final String      SERIALNUMBER_FIELD_ID = "serialNumber";

    public static final String      REGISTER_BUTTON_ID    = "register";

    public static final String      CANCEL_BUTTON_ID      = "cancel";

    @EJB(mappedName = DigipassDeviceService.JNDI_BINDING)
    transient DigipassDeviceService digipassDeviceService;


    public RegisterPage() {

        getHeader();
        getSidebar();
        getContent().add(new RegisterForm(REGISTER_FORM_ID));
    }


    class RegisterForm extends Form<String> {

        private static final long serialVersionUID = 1L;

        Model<String>             login;

        Model<String>             serialNumber;


        @SuppressWarnings("unchecked")
        public RegisterForm(String id) {

            super(id);

            final TextField<String> loginField = new TextField<String>(LOGIN_FIELD_ID, this.login = new Model<String>());
            loginField.setRequired(true);

            add(loginField);
            add(new ErrorComponentFeedbackLabel("login_feedback", loginField));

            final TextField serialNumberField = new TextField(SERIALNUMBER_FIELD_ID, this.serialNumber = new Model<String>());
            serialNumberField.setRequired(true);
            serialNumberField.add(StringValidator.lengthBetween(8, 12));

            add(serialNumberField);
            add(new ErrorComponentFeedbackLabel("serialNumber_feedback", serialNumberField));

            add(new Button(REGISTER_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    RegisterPage.LOG.debug("register digipas with sn=" + RegisterForm.this.serialNumber + " for user: "
                            + RegisterForm.this.login);

                    try {
                        RegisterPage.this.digipassDeviceService.register(getUserId(), RegisterForm.this.serialNumber.getObject());
                    } catch (SubjectNotFoundException e) {
                        LOG.debug("subject not found");
                        loginField.error(getLocalizer().getString("errorSubjectNotFound", this));
                        return;
                    } catch (ArgumentIntegrityException e) {
                        LOG.debug("digipass already registered");
                        serialNumberField.error(getLocalizer().getString("errorDigipassRegistered", this));
                        return;
                    } catch (PermissionDeniedException e) {
                        LOG.debug("permission denied: " + e.getMessage());
                        RegisterForm.this.error(getLocalizer().getString("errorPermissionDenied", this));
                        return;
                    } catch (AttributeTypeNotFoundException e) {
                        LOG.debug("attribute type not found: " + e.getMessage());
                        RegisterForm.this.error(getLocalizer().getString("errorAttributeTypeNotFound", this));
                        return;
                    }
                    setResponsePage(MainPage.class);
                }

            });

            Button cancel = new Button(CANCEL_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    setResponsePage(MainPage.class);
                }

            };
            cancel.setDefaultFormProcessing(false);
            add(cancel);

            add(new ErrorFeedbackPanel("feedback", new ComponentFeedbackMessageFilter(this)));
        }

        protected String getUserId()
                throws SubjectNotFoundException, PermissionDeniedException {

            AuthIdentityServiceClient authIdentityServiceClient = new AuthIdentityServiceClient();

            NameIdentifierMappingClient idMappingClient = WicketUtil.getOLASIdMappingService(WicketUtil.toServletRequest(getRequest()),
                    authIdentityServiceClient.getPrivateKey(), authIdentityServiceClient.getCertificate());

            String userId;
            try {
                userId = idMappingClient.getUserId(this.login.getObject());
            } catch (net.link.safeonline.sdk.exception.SubjectNotFoundException e) {
                LOG.error("subject not found: " + this.login);
                throw new SubjectNotFoundException();
            } catch (RequestDeniedException e) {
                LOG.error("request denied: " + e.getMessage());
                throw new PermissionDeniedException(e.getMessage());
            } catch (WSClientTransportException e) {
                LOG.error("failed to contact web service: " + e.getMessage());
                throw new PermissionDeniedException(e.getMessage());
            }
            return userId;
        }
    }
}
