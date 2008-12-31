/*
 * SafeOnline project.
 * 
 * Copyright 2006-2008 	Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

using NotificationConsumerWSNameSpace;
using System;
using System.Net;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.Description;
using System.ServiceModel.Dispatcher;
using System.ServiceModel.Security;
using System.ServiceModel.Security.Tokens;
using System.Xml;

namespace safe_online_sdk_dotnet
{
	/// <summary>
	/// Client implementation of the OLAS WS-Notification Consumer Web Service.
	/// </summary>
	public class NotificationConsumerClientImpl : NotificationConsumerClient
	{
		private NotificationConsumerPortClient client;
		
		public NotificationConsumerClientImpl(string location, string appPfxPath, string appPfxPassword, string olasCertPath)
		{
			X509Certificate2 appCertificate = new X509Certificate2(appPfxPath, appPfxPassword);
			X509Certificate2 olasCertificate = new X509Certificate2(olasCertPath);
			
			ServicePointManager.ServerCertificateValidationCallback = 
				new RemoteCertificateValidationCallback(WCFUtil.AnyCertificateValidationCallback);
			
			string address = "https://" + location + "/safe-online-ws/consumer";
			EndpointAddress remoteAddress = new EndpointAddress(address);
					
			this.client = new NotificationConsumerPortClient(new OlasBinding(olasCertificate), remoteAddress);
			
			this.client.ClientCredentials.ClientCertificate.Certificate = appCertificate;
			this.client.ClientCredentials.ServiceCertificate.DefaultCertificate = olasCertificate;
			// To override the validation for our self-signed test certificates
			this.client.ClientCredentials.ServiceCertificate.Authentication.CertificateValidationMode = X509CertificateValidationMode.None;
			
			this.client.Endpoint.Contract.ProtectionLevel = ProtectionLevel.Sign;
			this.client.Endpoint.Behaviors.Add(new LoggingBehavior());
		}
		
		public void sendNotification(string topic, string destination, string subject, string content) {
			Console.WriteLine("send notification for topic " + topic + " to destination " + destination +
			                  " for subject " + subject);
			TopicExpressionType topicExpression = new TopicExpressionType();
			topicExpression.Dialect = NotificationServiceConstants.TOPIC_DIALECT_SIMPLE;
			XmlDocument d = new XmlDocument();
			topicExpression.Any = new XmlNode[1];
			topicExpression.Any[0] = d.CreateTextNode(topic);
			
			Notify notifications = new Notify();
			NotificationMessageHolderType notification = new NotificationMessageHolderType();
			notification.Topic = topicExpression;
			
			NotificationMessageHolderTypeMessage message = new NotificationMessageHolderTypeMessage();
			message.Destination = destination;
			message.Subject = subject;
			message.Content = content;
			notification.Message = message;
			notifications.NotificationMessage = new NotificationMessageHolderType[]{notification};
			
			this.client.Notify(notifications);
		}
		
	}
}
