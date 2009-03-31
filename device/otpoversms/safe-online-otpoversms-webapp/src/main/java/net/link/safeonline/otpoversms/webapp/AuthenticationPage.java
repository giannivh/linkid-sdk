/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.otpoversms.webapp;

import javax.ejb.EJB;

import net.link.safeonline.authentication.exception.DeviceAuthenticationException;
import net.link.safeonline.authentication.exception.DeviceDisabledException;
import net.link.safeonline.authentication.exception.DeviceRegistrationNotFoundException;
import net.link.safeonline.authentication.exception.InternalInconsistencyException;
import net.link.safeonline.authentication.exception.NodeNotFoundException;
import net.link.safeonline.authentication.exception.SafeOnlineResourceException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.NodeAuthenticationService;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.custom.converter.PhoneNumber;
import net.link.safeonline.device.sdk.AuthenticationContext;
import net.link.safeonline.helpdesk.HelpdeskLogger;
import net.link.safeonline.model.otpoversms.OtpOverSmsConstants;
import net.link.safeonline.model.otpoversms.OtpOverSmsDeviceService;
import net.link.safeonline.osgi.sms.exception.SmsServiceException;
import net.link.safeonline.shared.helpdesk.LogLevelType;
import net.link.safeonline.util.ee.EjbUtils;
import net.link.safeonline.webapp.components.CustomRequiredPasswordTextField;
import net.link.safeonline.webapp.components.CustomRequiredTextField;
import net.link.safeonline.webapp.components.ErrorComponentFeedbackLabel;
import net.link.safeonline.webapp.components.ErrorFeedbackPanel;
import net.link.safeonline.webapp.template.ProgressAuthenticationPanel;
import net.link.safeonline.webapp.template.SideLink;
import net.link.safeonline.webapp.template.SidebarBorder;
import net.link.safeonline.webapp.template.TemplatePage;
import net.link.safeonline.wicket.tools.WicketUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;


public class AuthenticationPage extends TemplatePage {

    static final Log           LOG                      = LogFactory.getLog(AuthenticationPage.class);

    private static final long  serialVersionUID         = 1L;

    public static final String REQUEST_OTP_FORM_ID      = "request_otp_form";
    public static final String MOBILE_FIELD_ID          = "mobile";
    public static final String REQUEST_OTP_BUTTON_ID    = "request_otp";
    public static final String REQUEST_CANCEL_BUTTON_ID = "request_cancel";

    public static final String VERIFY_OTP_FORM_ID       = "verify_otp_form";
    public static final String OTP_FIELD_ID             = "otp";
    public static final String PIN_FIELD_ID             = "pin";
    public static final String LOGIN_BUTTON_ID          = "login";
    public static final String CANCEL_BUTTON_ID         = "cancel";

    @EJB(mappedName = SamlAuthorityService.JNDI_BINDING)
    SamlAuthorityService       samlAuthorityService;

    @EJB(mappedName = NodeAuthenticationService.JNDI_BINDING)
    NodeAuthenticationService  nodeAuthenticationService;

    Model<PhoneNumber>         mobile;


    public AuthenticationPage() {

        getHeader();
        Link<String> tryAnotherDeviceLink = new Link<String>(SidebarBorder.LINK_ID) {

            private static final long serialVersionUID = 1L;


            @Override
            public void onClick() {

                AuthenticationContext.getAuthenticationContext(WicketUtil.getHttpSession(getRequest())).setUsedDevice(
                        OtpOverSmsConstants.OTPOVERSMS_DEVICE_ID);
                exit();
            }
        };
        getSidebar(localize("helpOtpOverSmsAuthentication"), new SideLink(tryAnotherDeviceLink, localize("tryAnotherDevice")));
        getContent().add(new ProgressAuthenticationPanel("progress", ProgressAuthenticationPanel.stage.authenticate));

        String title = localize("%l %s", "authenticatingFor", AuthenticationContext.getAuthenticationContext(
                WicketUtil.getHttpSession(getRequest())).getApplication());
        getContent().add(new Label("title", title));
        getContent().add(new RequestOtpForm(REQUEST_OTP_FORM_ID));
        getContent().add(new VerifyOtpForm(VERIFY_OTP_FORM_ID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPageTitle() {

        return localize("otpOverSmsAuthentication");
    }


    class RequestOtpForm extends Form<String> {

        private static final long            serialVersionUID = 1L;
        CustomRequiredTextField<PhoneNumber> mobileField;


        public RequestOtpForm(String id) {

            super(id);

            mobileField = new CustomRequiredTextField<PhoneNumber>(MOBILE_FIELD_ID, mobile = new Model<PhoneNumber>(), PhoneNumber.class);
            mobileField.setRequired(true);
            mobileField.setRequiredMessageKey("errorMissingMobileNumber");
            add(mobileField);
            add(new ErrorComponentFeedbackLabel("mobile_feedback", mobileField));

            add(new Button(REQUEST_OTP_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    LOG.debug("request OTP for mobile: " + mobile.getObject());
                    try {
                        OtpOverSmsDeviceService otpOverSmsDeviceService = EjbUtils.getEJB(OtpOverSmsDeviceService.JNDI_BINDING,
                                OtpOverSmsDeviceService.class);

                        otpOverSmsDeviceService.requestOtp(mobile.getObject().getNumber());
                        OtpOverSmsSession.get().setDeviceBean(otpOverSmsDeviceService);
                    }

                    catch (SmsServiceException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorServiceConnection", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "login: failed to send otp to " + mobile.getObject(),
                                LogLevelType.ERROR);
                        mobile.setObject(null);
                    } catch (SafeOnlineResourceException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorServiceConnection", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "login: failed to send otp to " + mobile.getObject(),
                                LogLevelType.ERROR);
                        mobile.setObject(null);
                    } catch (SubjectNotFoundException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorSubjectNotFound", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "login: mobile has no registered subject: "
                                + mobile.getObject(), LogLevelType.ERROR);
                    } catch (DeviceRegistrationNotFoundException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorDeviceRegistrationNotFound", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()),
                                "login: mobile isn't registered: " + mobile.getObject(), LogLevelType.ERROR);
                    }
                }
            });

            Button cancel = new Button(REQUEST_CANCEL_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    exit();
                }

            };
            cancel.setDefaultFormProcessing(false);
            add(cancel);

            add(new ErrorFeedbackPanel("request_feedback", new ComponentFeedbackMessageFilter(this)));

        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onBeforeRender() {

            focus(mobileField);

            super.onBeforeRender();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isVisible() {

            return null == mobile.getObject();
        }
    }

    class VerifyOtpForm extends Form<String> {

        private static final long       serialVersionUID = 1L;

        Model<String>                   otp;

        Model<String>                   pin;

        CustomRequiredTextField<String> otpField;


        public VerifyOtpForm(String id) {

            super(id);

            otpField = new CustomRequiredTextField<String>(OTP_FIELD_ID, otp = new Model<String>());
            otpField.setRequired(true);
            otpField.setRequiredMessageKey("errorMissingMobileOTP");
            add(otpField);
            add(new ErrorComponentFeedbackLabel("otp_feedback", otpField));

            final CustomRequiredPasswordTextField pinField = new CustomRequiredPasswordTextField(PIN_FIELD_ID, pin = new Model<String>());
            pinField.setRequiredMessageKey("errorMissingMobilePIN");
            add(pinField);
            add(new ErrorComponentFeedbackLabel("pin_feedback", pinField));

            add(new Button(LOGIN_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    OtpOverSmsDeviceService otpOverSmsDeviceService = OtpOverSmsSession.get().getDeviceService();

                    try {
                        String userId = otpOverSmsDeviceService.authenticate(pin.getObject(), otp.getObject());
                        login(userId);
                    }

                    catch (DeviceAuthenticationException e) {
                        VerifyOtpForm.this.error(localize("authenticationFailedMsg"));
                        HelpdeskLogger.add(localize("login failed: %s", mobile.getObject()), LogLevelType.ERROR);
                    } catch (SubjectNotFoundException e) {
                        VerifyOtpForm.this.error(localize("errorSubjectNotFound"));
                        HelpdeskLogger.add(localize("subject not found: %s", mobile.getObject()), LogLevelType.ERROR);
                    } catch (DeviceDisabledException e) {
                        VerifyOtpForm.this.error(localize("errorDeviceDisabled"));
                        HelpdeskLogger.add(localize("login: mobile %s disabled", mobile.getObject()), LogLevelType.ERROR);
                    } catch (DeviceRegistrationNotFoundException e) {
                        VerifyOtpForm.this.error(localize("errorDeviceRegistrationNotFound"));
                        HelpdeskLogger.add(localize("login: mobile %s not registered", mobile.getObject()), LogLevelType.ERROR);
                    }
                }

            });

            Button cancel = new Button(CANCEL_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    exit();
                }

            };
            cancel.setDefaultFormProcessing(false);
            add(cancel);

            add(new ErrorFeedbackPanel("feedback", new ComponentFeedbackMessageFilter(this)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onBeforeRender() {

            focus(otpField);

            super.onBeforeRender();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isVisible() {

            return mobile.getObject() != null;
        }
    }


    public void login(String userId) {

        try {
            AuthenticationContext authenticationContext = AuthenticationContext
                                                                               .getAuthenticationContext(WicketUtil
                                                                                                                   .getHttpSession(getRequest()));
            authenticationContext.setIssuer(nodeAuthenticationService.getLocalNode().getName());
            authenticationContext.setValidity(samlAuthorityService.getAuthnAssertionValidity());
            authenticationContext.setUsedDevice(OtpOverSmsConstants.OTPOVERSMS_DEVICE_ID);
            authenticationContext.setUserId(userId);
        }

        catch (NodeNotFoundException e) {
            throw new InternalInconsistencyException("Couldn't look up local node.");
        }

        exit();

    }

    public void exit() {

        getResponse().redirect("authenticationexit");
        setRedirect(false);
    }
}