/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.model.performance.bean;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.Startable;
import net.link.safeonline.authentication.exception.PermissionDeniedException;
import net.link.safeonline.authentication.service.IdentityAttributeTypeDO;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.DatatypeType;
import net.link.safeonline.entity.IdScopeType;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionOwnerType;
import net.link.safeonline.keystore.SafeOnlineKeyStore;
import net.link.safeonline.keystore.SafeOnlineNodeKeyStore;
import net.link.safeonline.model.bean.AbstractInitBean;
import net.link.safeonline.model.password.PasswordManager;
import net.link.safeonline.model.performance.PerformanceConstants;
import net.link.safeonline.performance.keystore.PerformanceKeyStore;
import net.link.safeonline.service.SubjectService;

import org.jboss.annotation.ejb.LocalBinding;


@Stateless
@Local(Startable.class)
@LocalBinding(jndiBinding = PerformanceStartableBean.JNDI_BINDING)
public class PerformanceStartableBean extends AbstractInitBean {

    public static final String  JNDI_BINDING                      = PerformanceConstants.PERFORMANCE_STARTABLE_JNDI_PREFIX
                                                                          + "PerformanceStartableBean";

    private static final String PERFORMANCE_ATTRIBUTE             = "urn:net:lin-k:safe-online:attribute:test";
    public static final String  PERFORMANCE_APPLICATION_NAME      = "performance-application";
    private static final String LICENSE_AGREEMENT_CONFIRM_TEXT_EN = "This software is for performance testing purposes only.";
    private static final String LICENSE_AGREEMENT_CONFIRM_TEXT_NL = "Deze software dient enkel voor performance testing gebruikt te worden.";


    private static class PasswordRegistration {

        final String login;

        final String password;


        public PasswordRegistration(String login, String password) {

            this.login = login;
            this.password = password;
        }
    }


    private List<PasswordRegistration> passwordRegistrations;

    @EJB(mappedName = PasswordManager.JNDI_BINDING)
    private PasswordManager            passwordManager;

    @EJB(mappedName = SubjectService.JNDI_BINDING)
    private SubjectService             subjectService;


    public PerformanceStartableBean() {

        passwordRegistrations = new LinkedList<PasswordRegistration>();

        configureNode();

        /*
         * Create the performance user.
         */
        users.add("performance");
        passwordRegistrations.add(new PasswordRegistration("performance", "performance"));
        subscriptions.add(new Subscription(SubscriptionOwnerType.APPLICATION, "performance",
                SafeOnlineConstants.SAFE_ONLINE_USER_APPLICATION_NAME));

        /*
         * Obtain the performance application identity.
         */
        PrivateKeyEntry perfPrivateKeyEntry = PerformanceKeyStore.getPrivateKeyEntry();
        X509Certificate perfCertificate = (X509Certificate) perfPrivateKeyEntry.getCertificate();

        /*
         * Register the application and the application certificate.
         */
        trustedCertificates.put(perfCertificate, SafeOnlineConstants.SAFE_ONLINE_APPLICATIONS_TRUST_DOMAIN);
        registeredApplications.add(new Application(PERFORMANCE_APPLICATION_NAME, "owner", null, null, getLogo(), true, false,
                perfCertificate, true, IdScopeType.USER, false, null));

        /*
         * Subscribe the performance user to the performance application.
         */
        subscriptions.add(new Subscription(SubscriptionOwnerType.SUBJECT, "performance", PERFORMANCE_APPLICATION_NAME));

        /*
         * Attribute Types.
         */
        AttributeTypeEntity attributeType = new AttributeTypeEntity(PERFORMANCE_ATTRIBUTE, DatatypeType.STRING, true, true);
        attributeTypes.add(attributeType);

        /*
         * Application Identities
         */
        identities.add(new Identity(PERFORMANCE_APPLICATION_NAME, new IdentityAttributeTypeDO[] { new IdentityAttributeTypeDO(
                PERFORMANCE_ATTRIBUTE, false, false) }));

        /*
         * Application usage agreements
         */
        UsageAgreement usageAgreement = new UsageAgreement(PERFORMANCE_APPLICATION_NAME);
        usageAgreement.addUsageAgreementText(new UsageAgreementText(Locale.ENGLISH.getLanguage(), "English" + "\n\n" + "Lin-k NV" + "\n"
                + "Software License Agreement for " + PERFORMANCE_APPLICATION_NAME + "\n\n" + LICENSE_AGREEMENT_CONFIRM_TEXT_EN));
        usageAgreement.addUsageAgreementText(new UsageAgreementText("nl", "Nederlands" + "\n\n" + "Lin-k NV" + "\n"
                + "Software Gebruikers Overeenkomst voor " + PERFORMANCE_APPLICATION_NAME + "\n\n" + LICENSE_AGREEMENT_CONFIRM_TEXT_NL));
        usageAgreements.add(usageAgreement);
    }

    private byte[] getLogo() {

        return getLogo("/logo.jpg");
    }

    private void configureNode() {

        ResourceBundle properties = ResourceBundle.getBundle("config");
        String nodeName = properties.getString("olas.node.name");
        String protocol = properties.getString("olas.host.protocol");
        String hostname = properties.getString("olas.host.name");
        int hostport = Integer.parseInt(properties.getString("olas.host.port"));
        int hostportssl = Integer.parseInt(properties.getString("olas.host.port.ssl"));

        SafeOnlineKeyStore olasKeyStore = new SafeOnlineKeyStore();
        SafeOnlineNodeKeyStore nodeKeyStore = new SafeOnlineNodeKeyStore();

        node = new Node(nodeName, protocol, hostname, hostport, hostportssl, nodeKeyStore.getCertificate(), olasKeyStore.getCertificate());
        trustedCertificates.put(nodeKeyStore.getCertificate(), SafeOnlineConstants.SAFE_ONLINE_OLAS_TRUST_DOMAIN);
    }

    @Override
    public void postStart() {

        super.postStart();

        for (PasswordRegistration passwordRegistration : passwordRegistrations) {

            SubjectEntity subject = subjectService.findSubjectFromUserName(passwordRegistration.login);
            if (null != subject) {
                continue;
            }

            try {
                passwordManager.setPassword(subject, passwordRegistration.password);
            } catch (PermissionDeniedException e) {
                throw new EJBException("could not set password");
            }

        }

    }

    @Override
    public int getPriority() {

        return Startable.PRIORITY_BOOTSTRAP - 1;
    }
}
