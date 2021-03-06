package net.link.safeonline.sdk.auth.protocol.saml2.devicecontext;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.EncryptedAttribute;
import org.opensaml.saml2.core.impl.AttributeStatementUnmarshaller;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.UnmarshallingException;


public class DeviceContextUnmarshaller extends AttributeStatementUnmarshaller {

    @Override
    protected void processChildElement(XMLObject parentObject, XMLObject childObject)
            throws UnmarshallingException {

        DeviceContext deviceContext = (DeviceContext) parentObject;

        if (childObject instanceof Attribute) {
            deviceContext.getAttributes().add( (Attribute) childObject );
        } else if (childObject instanceof EncryptedAttribute) {
            deviceContext.getEncryptedAttributes().add( (EncryptedAttribute) childObject );
        } else {
            super.processChildElement( parentObject, childObject );
        }
    }
}
