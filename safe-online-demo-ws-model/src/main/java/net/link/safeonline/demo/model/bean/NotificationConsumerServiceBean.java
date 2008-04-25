package net.link.safeonline.demo.model.bean;

import java.net.ConnectException;
import java.security.PrivateKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ResourceBundle;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.demo.model.NotificationConsumerService;
import net.link.safeonline.demo.payment.entity.PaymentEntity;
import net.link.safeonline.demo.payment.entity.UserEntity;
import net.link.safeonline.demo.payment.keystore.DemoPaymentKeyStoreUtils;
import net.link.safeonline.demo.ticket.entity.Ticket;
import net.link.safeonline.demo.ticket.entity.User;
import net.link.safeonline.demo.ticket.keystore.DemoTicketKeyStoreUtils;
import net.link.safeonline.model.demo.DemoConstants;
import net.link.safeonline.sdk.exception.AttributeNotFoundException;
import net.link.safeonline.sdk.exception.RequestDeniedException;
import net.link.safeonline.sdk.ws.attrib.AttributeClient;
import net.link.safeonline.sdk.ws.attrib.AttributeClientImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Stateless
public class NotificationConsumerServiceBean implements
		NotificationConsumerService {

	private static final Log LOG = LogFactory
			.getLog(NotificationConsumerServiceBean.class);

	private static final String DEMO_TICKET_APPLICATION_NAME = "demo-ticket";

	private static final String DEMO_PAYMENT_APPLICATION_NAME = "demo-payment";

	/*
	 * Add XXX due to: http://jira.jboss.com/jira/browse/EJBTHREE-1252
	 */
	@PersistenceContext(unitName = "XXXsafe-online-demo-ticket-1.0-SNAPSHOT.jar#DemoTicketEntityManager")
	private EntityManager demoTicketEntityManager;

	@PersistenceContext(unitName = "XXXsafe-online-demo-payment-1.0-SNAPSHOT.jar#DemoPaymentEntityManager")
	private EntityManager demoPaymentEntityManager;

	public void handleMessage(String topic, String destination,
			List<String> message) {
		String userId = message.get(0);
		try {
			if (topic.equals(SafeOnlineConstants.TOPIC_REMOVE_USER)) {
				if (destination.equals(DEMO_TICKET_APPLICATION_NAME)) {
					removeDemoTicketUser(userId);
				} else if (destination.equals(DEMO_PAYMENT_APPLICATION_NAME)) {
					removeDemoPaymentUser(userId);
				}
			}
		} catch (ConnectException e) {
			LOG.debug("ConnectException thrown: " + e.getMessage());
		} catch (AttributeNotFoundException e) {
			LOG
					.debug("AttributeNotFoundException thrown: "
							+ e.getMessage(), e);
		} catch (RequestDeniedException e) {
			LOG.debug("RequestDeniedException thrown: " + e.getMessage());
		}
	}

	private String getWsLocation() {
		ResourceBundle properties = ResourceBundle.getBundle("config");
		String wsLocation = properties.getString("olas.ws.location");
		LOG.debug("wsLocation: " + wsLocation);
		return wsLocation;
	}

	private void removeDemoTicketUser(String userId) throws ConnectException,
			AttributeNotFoundException, RequestDeniedException {
		LOG.debug("remove demo ticket user: " + userId);

		PrivateKeyEntry privateKeyEntry = DemoTicketKeyStoreUtils
				.getPrivateKeyEntry();
		X509Certificate certificate = (X509Certificate) privateKeyEntry
				.getCertificate();
		PrivateKey privateKey = privateKeyEntry.getPrivateKey();
		AttributeClient attributeClient = new AttributeClientImpl(
				getWsLocation(), certificate, privateKey);
		String username = attributeClient.getAttributeValue(userId,
				DemoConstants.DEMO_LOGIN_ATTRIBUTE_NAME, String.class);
		LOG.debug("removing demo ticket user: " + username);

		User user = this.demoTicketEntityManager.find(User.class, username);
		if (null != user) {
			List<Ticket> tickets = user.getTickets();
			for (Ticket ticket : tickets) {
				this.demoTicketEntityManager.remove(ticket);
			}
			this.demoTicketEntityManager.remove(user);
		}

	}

	private void removeDemoPaymentUser(String userId) throws ConnectException,
			AttributeNotFoundException, RequestDeniedException {
		LOG.debug("remove demo payment user: " + userId);

		PrivateKeyEntry privateKeyEntry = DemoPaymentKeyStoreUtils
				.getPrivateKeyEntry();
		X509Certificate certificate = (X509Certificate) privateKeyEntry
				.getCertificate();
		PrivateKey privateKey = privateKeyEntry.getPrivateKey();
		AttributeClient attributeClient = new AttributeClientImpl(
				getWsLocation(), certificate, privateKey);
		String username = attributeClient.getAttributeValue(userId,
				DemoConstants.DEMO_LOGIN_ATTRIBUTE_NAME, String.class);
		LOG.debug("removing demo payment user: " + username);

		UserEntity user = this.demoPaymentEntityManager.find(UserEntity.class,
				username);
		if (null != user) {
			List<PaymentEntity> payments = user.getPayments();
			for (PaymentEntity payment : payments) {
				this.demoPaymentEntityManager.remove(payment);
			}
			this.demoPaymentEntityManager.remove(user);
		}
	}
}