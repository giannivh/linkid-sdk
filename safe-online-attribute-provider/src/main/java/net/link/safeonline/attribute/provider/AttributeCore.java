package net.link.safeonline.attribute.provider;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import net.link.safeonline.attribute.provider.input.AttributeInputPanel;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.Component;


/**
 * <h2>{@link AttributeCore}</h2>
 *
 * <p> <i>Nov 29, 2010</i>
 *
 * Core Attribute. </p>
 *
 * @author wvdhaute
 */
public class AttributeCore extends AttributeSDK<Serializable> {

    private final AttributeType attributeType;

    private boolean unavailable;

    // used in LinkID pages for feedback of error messages
    private Component           label;
    private AttributeInputPanel panel;

    public AttributeCore(final AttributeType attributeType) {
        super( null, attributeType.getName() );
        this.attributeType = attributeType;
    }

    public AttributeCore(final String attributeId, final AttributeType attributeType) {
        super( attributeId, attributeType.getName() );
        this.attributeType = attributeType;
    }

    public AttributeCore(final String attributeId, final AttributeType attributeType, final Serializable value) {
        super( attributeId, attributeType.getName(), value );
        this.attributeType = attributeType;
    }

    public AttributeCore(final AttributeType attributeType, final Serializable value) {
        super( null, attributeType.getName(), value );
        this.attributeType = attributeType;
    }

    public AttributeCore(final AttributeSDK<Serializable> attribute) {

        super( attribute.getAttributeId(), attribute.getAttributeName(), attribute.getValue() );
        attributeType = new AttributeType( attribute.getAttributeName() );
    }

    public AttributeCore(final AttributeCore attribute) {
        this( attribute.getAttributeId(), attribute.getAttributeType(), attribute.getValue() );
        unavailable = attribute.isUnavailable();
    }

    public AttributeType getAttributeType() {
        return attributeType;
    }

    public boolean isRequired() {
        return attributeType.isRequired();
    }

    public boolean isUnavailable() {
        return unavailable;
    }

    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    public Component getLabel() {
        return label;
    }

    public void setLabel(final Component label) {
        this.label = label;
    }

    public void setPanel(final AttributeInputPanel panel) {
        this.panel = panel;
    }

    public AttributeInputPanel getPanel() {
        return panel;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (null == obj)
            return false;
        if (!(obj instanceof AttributeCore))
            return false;
        AttributeCore rhs = (AttributeCore) obj;
        return new EqualsBuilder().append( attributeType, rhs.attributeType ).isEquals() && super.equals( obj );
    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder().append( attributeType ).append( attributeId ).append( value ).toHashCode();
    }

    public AttributeCore getTemplate() {

        AttributeCore template = new AttributeCore( (String) null, attributeType );
        if (attributeType.isCompound()) {
            List<AttributeCore> memberTemplates = new LinkedList<AttributeCore>();
            for (AttributeAbstract<?> memberAbstract : ((Compound) getValue()).getMembers()) {
                AttributeCore member = (AttributeCore) memberAbstract;
                memberTemplates.add( member.getTemplate() );
            }
            template.setValue( new Compound( memberTemplates ) );
        }
        return template;
    }
}
