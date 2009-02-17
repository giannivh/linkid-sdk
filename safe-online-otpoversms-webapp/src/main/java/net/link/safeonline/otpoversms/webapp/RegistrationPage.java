/*
 * SafeOnline project.
 *
 * Copyright 2006-2008 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.otpoversms.webapp;

import java.net.ConnectException;

import javax.ejb.EJB;

import net.link.safeonline.authentication.exception.DeviceDisabledException;
import net.link.safeonline.authentication.exception.DeviceRegistrationNotFoundException;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.exception.SafeOnlineResourceException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.SamlAuthorityService;
import net.link.safeonline.custom.converter.PhoneNumber;
import net.link.safeonline.device.sdk.ProtocolContext;
import net.link.safeonline.device.sdk.saml2.DeviceOperationType;
import net.link.safeonline.helpdesk.HelpdeskLogger;
import net.link.safeonline.model.otpoversms.OtpOverSmsDeviceService;
import net.link.safeonline.shared.helpdesk.LogLevelType;
import net.link.safeonline.webapp.components.ErrorComponentFeedbackLabel;
import net.link.safeonline.webapp.components.ErrorFeedbackPanel;
import net.link.safeonline.webapp.template.ProgressRegistrationPanel;
import net.link.safeonline.webapp.template.TemplatePage;
import net.link.safeonline.wicket.tools.WicketUtil;

import org.apache.wicket.RedirectToUrlException;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;


public class RegistrationPage extends TemplatePage {

    private static final long         serialVersionUID         = 1L;

    public static final String        REQUEST_OTP_FORM_ID      = "request_otp_form";
    public static final String        MOBILE_FIELD_ID          = "mobile";
    public static final String        REQUEST_OTP_BUTTON_ID    = "request_otp";
    public static final String        REQUEST_CANCEL_BUTTON_ID = "request_cancel";

    public static final String        VERIFY_OTP_FORM_ID       = "verify_otp_form";
    public static final String        OTP_FIELD_ID             = "otp";
    public static final String        PIN1_FIELD_ID            = "pin1";
    public static final String        PIN2_FIELD_ID            = "pin2";
    public static final String        SAVE_BUTTON_ID           = "save";
    public static final String        CANCEL_BUTTON_ID         = "cancel";

    @EJB(mappedName = OtpOverSmsDeviceService.JNDI_BINDING)
    transient OtpOverSmsDeviceService otpOverSmsDeviceService;

    @EJB(mappedName = SamlAuthorityService.JNDI_BINDING)
    transient SamlAuthorityService    samlAuthorityService;

    ProtocolContext                   protocolContext;

    Model<PhoneNumber>                mobile;


    public RegistrationPage() {

        protocolContext = ProtocolContext.getProtocolContext(WicketUtil.getHttpSession(getRequest()));

        getHeader();
        getSidebar(localize("helpRegisterOtpOverSms"));

        ProgressRegistrationPanel progress = new ProgressRegistrationPanel("progress", ProgressRegistrationPanel.stage.register);
        progress.setVisible(protocolContext.getDeviceOperation().equals(DeviceOperationType.NEW_ACCOUNT_REGISTER));
        getContent().add(progress);

        getContent().add(new RequestOtpForm(REQUEST_OTP_FORM_ID));
        getContent().add(new VerifyOtpForm(VERIFY_OTP_FORM_ID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPageTitle() {

        return localize("registerANewDevice");
    }


    class RequestOtpForm extends Form<String> {

        private static final long serialVersionUID = 1L;
        TextField<PhoneNumber>    mobileField;


        public RequestOtpForm(String id) {

            super(id);

            mobileField = new TextField<PhoneNumber>(MOBILE_FIELD_ID, mobile = new Model<PhoneNumber>(), PhoneNumber.class);
            mobileField.setRequired(true);
            add(mobileField);
            add(new ErrorComponentFeedbackLabel("mobile_feedback", mobileField));

            add(new Button(REQUEST_OTP_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    LOG.debug("request otp for mobile " + mobile);

                    try {
                        otpOverSmsDeviceService.requestOtp(WicketUtil.getHttpSession(getRequest()), mobile.getObject().getNumber());
                    }

                    catch (ConnectException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorServiceConnection", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "request: failed to send otp" + mobile.getObject(),
                                LogLevelType.ERROR);
                        mobile.setObject(null);
                        return;
                    } catch (SafeOnlineResourceException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorServiceConnection", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "request: failed to send otp" + mobile.getObject(),
                                LogLevelType.ERROR);
                        mobile.setObject(null);
                        return;
                    } catch (SubjectNotFoundException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorSubjectNotFound", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "request: mobile has no registered subject: "
                                + protocolContext.getAttribute(), LogLevelType.ERROR);
                    } catch (DeviceRegistrationNotFoundException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorDeviceRegistrationNotFound", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "request: mobile isn't registered: "
                                + protocolContext.getAttribute(), LogLevelType.ERROR);
                    } catch (DeviceDisabledException e) {
                        RequestOtpForm.this.error(getLocalizer().getString("errorDeviceDisabled", this));
                        HelpdeskLogger.add(WicketUtil.getHttpSession(getRequest()), "request: mobile is disabled: "
                                + protocolContext.getAttribute(), LogLevelType.ERROR);
                    }
                }

            });

            Button cancel = new Button(REQUEST_CANCEL_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    protocolContext.setSuccess(false);
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

        private static final long serialVersionUID = 1L;

        Model<String>             otp;

        Model<String>             pin1;

        Model<String>             pin2;

        TextField<String>         otpField;


        public VerifyOtpForm(String id) {

            super(id);

            otpField = new TextField<String>(OTP_FIELD_ID, otp = new Model<String>());
            otpField.setRequired(true);
            add(otpField);
            add(new ErrorComponentFeedbackLabel("otp_feedback", otpField));

            final PasswordTextField pin1Field = new PasswordTextField(PIN1_FIELD_ID, pin1 = new Model<String>());
            add(pin1Field);
            add(new ErrorComponentFeedbackLabel("pin1_feedback", pin1Field));

            final PasswordTextField pin2Field = new PasswordTextField(PIN2_FIELD_ID, pin2 = new Model<String>());
            add(pin2Field);
            add(new ErrorComponentFeedbackLabel("pin2_feedback", pin2Field));

            add(new EqualPasswordInputValidator(pin1Field, pin2Field));

            add(new Button(SAVE_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    try {
                        if (!otpOverSmsDeviceService.verifyOtp(WicketUtil.getHttpSession(getRequest()), otp.getObject())) {
                            otpField.error(getLocalizer().getString("authenticationFailedMsg", this));
                            HelpdeskLogger.add(WicketUtil.toServletRequest(getRequest()).getSession(),
                                    "mobile otp: verification failed for mobile " + mobile, LogLevelType.ERROR);
                            return;
                        }

                        LOG.debug("register mobile " + mobile + " for " + protocolContext.getSubject());

                        otpOverSmsDeviceService.register(protocolContext.getSubject(), mobile.getObject().getNumber(), pin1.getObject());

                        protocolContext.setSuccess(true);
                        exit();
                    }

                    catch (PermissionDeniedException e) {
                        pin2Field.error(getLocalizer().getString("errorPinNotCorrect", this));
                        HelpdeskLogger.add(WicketUtil.toServletRequest(getRequest()).getSession(), "register: pin not correct",
                                LogLevelType.ERROR);
                    }
                }
            });

            Button cancel = new Button(CANCEL_BUTTON_ID) {

                private static final long serialVersionUID = 1L;


                @Override
                public void onSubmit() {

                    protocolContext.setSuccess(false);
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


    public void exit() {

        protocolContext.setValidity(samlAuthorityService.getAuthnAssertionValidity());
        throw new RedirectToUrlException("deviceexit");
    }
}
