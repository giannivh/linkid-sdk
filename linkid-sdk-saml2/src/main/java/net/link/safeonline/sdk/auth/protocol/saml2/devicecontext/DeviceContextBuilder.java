package net.link.safeonline.sdk.auth.protocol.saml2.devicecontext;

import org.opensaml.common.impl.AbstractSAMLObjectBuilder;
import org.opensaml.common.xml.SAMLConstants;


public class DeviceContextBuilder extends AbstractSAMLObjectBuilder<DeviceContext> {

    @Override
    public DeviceContext buildObject() {

        return buildObject( SAMLConstants.SAML20_NS, DeviceContext.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20_PREFIX );
    }

    @Override
    public DeviceContext buildObject(String namespaceURI, String localName, String namespacePrefix) {

        return new DeviceContextImpl( namespaceURI, localName, namespacePrefix );
    }
}
