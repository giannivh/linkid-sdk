/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity;

import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_CATEGORIZE_BOOLEAN;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_CATEGORIZE_DATE;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_CATEGORIZE_DOUBLE;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_CATEGORIZE_INTEGER;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_CATEGORIZE_STRING;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_WHERE_ALL;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_WHERE_NODE;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_WHERE_TYPE;
import static net.link.safeonline.entity.AttributeTypeEntity.QUERY_WHERE_VISIBLE;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.link.safeonline.jpa.annotation.QueryMethod;
import net.link.safeonline.jpa.annotation.QueryParam;
import net.link.safeonline.keystore.SafeOnlineNodeKeyStore;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.IndexColumn;


@Entity
@Table(name = "attribute_type")
@NamedQueries( {
        @NamedQuery(name = QUERY_WHERE_ALL, query = "FROM AttributeTypeEntity"),
        @NamedQuery(name = QUERY_WHERE_NODE, query = "FROM AttributeTypeEntity a WHERE a.location = :location"),
        @NamedQuery(name = QUERY_WHERE_VISIBLE, query = "FROM AttributeTypeEntity a WHERE a.userVisible = true"),
        @NamedQuery(name = QUERY_WHERE_TYPE, query = "FROM AttributeTypeEntity a WHERE a.type = :type"),
        @NamedQuery(name = QUERY_CATEGORIZE_STRING, query = "SELECT a.stringValue, COUNT(a.stringValue) "
                + "FROM AttributeEntity a, SubscriptionEntity s, " + "ApplicationIdentityEntity i, ApplicationIdentityAttributeEntity aia "
                + "WHERE a.subject = s.subject " + "AND s.confirmedIdentityVersion = i.pk.identityVersion "
                + "AND s.application = i.application " + "AND :application = s.application " + "AND aia.applicationIdentity = i "
                + "AND :attributeType = aia.attributeType " + "AND aia.attributeType = a.attributeType "
                + "AND a.stringValue IS NOT NULL GROUP BY a.stringValue"),
        @NamedQuery(name = QUERY_CATEGORIZE_BOOLEAN, query = "SELECT a.booleanValue, COUNT(a.booleanValue) "
                + "FROM AttributeEntity a, SubscriptionEntity s, " + "ApplicationIdentityEntity i, ApplicationIdentityAttributeEntity aia "
                + "WHERE a.subject = s.subject " + "AND s.confirmedIdentityVersion = i.pk.identityVersion "
                + "AND s.application = i.application " + "AND :application = s.application " + "AND aia.applicationIdentity = i "
                + "AND :attributeType = aia.attributeType " + "AND aia.attributeType = a.attributeType "
                + "AND a.booleanValue IS NOT NULL GROUP BY a.booleanValue"),
        @NamedQuery(name = QUERY_CATEGORIZE_INTEGER, query = "SELECT a.integerValue, COUNT(a.integerValue) "
                + "FROM AttributeEntity a, SubscriptionEntity s, " + "ApplicationIdentityEntity i, ApplicationIdentityAttributeEntity aia "
                + "WHERE a.subject = s.subject " + "AND s.confirmedIdentityVersion = i.pk.identityVersion "
                + "AND s.application = i.application " + "AND :application = s.application " + "AND aia.applicationIdentity = i "
                + "AND :attributeType = aia.attributeType " + "AND aia.attributeType = a.attributeType "
                + "AND a.integerValue IS NOT NULL GROUP BY a.integerValue"),
        @NamedQuery(name = QUERY_CATEGORIZE_DOUBLE, query = "SELECT a.doubleValue, COUNT(a.doubleValue) "
                + "FROM AttributeEntity a, SubscriptionEntity s, " + "ApplicationIdentityEntity i, ApplicationIdentityAttributeEntity aia "
                + "WHERE a.subject = s.subject " + "AND s.confirmedIdentityVersion = i.pk.identityVersion "
                + "AND s.application = i.application " + "AND :application = s.application " + "AND aia.applicationIdentity = i "
                + "AND :attributeType = aia.attributeType " + "AND aia.attributeType = a.attributeType "
                + "AND a.doubleValue IS NOT NULL GROUP BY a.doubleValue"),
        @NamedQuery(name = QUERY_CATEGORIZE_DATE, query = "SELECT a.dateValue, COUNT(a.dateValue) "
                + "FROM AttributeEntity a, SubscriptionEntity s, " + "ApplicationIdentityEntity i, ApplicationIdentityAttributeEntity aia "
                + "WHERE a.subject = s.subject " + "AND s.confirmedIdentityVersion = i.pk.identityVersion "
                + "AND s.application = i.application " + "AND :application = s.application " + "AND aia.applicationIdentity = i "
                + "AND :attributeType = aia.attributeType " + "AND aia.attributeType = a.attributeType "
                + "AND a.dateValue IS NOT NULL GROUP BY a.dateValue") })
public class AttributeTypeEntity implements Serializable {

    public static final String                          QUERY_WHERE_ALL          = "at.all";

    public static final String                          QUERY_WHERE_NODE         = "at.node";

    public static final String                          QUERY_WHERE_VISIBLE      = "at.visible";

    public static final String                          QUERY_WHERE_TYPE         = "at.type";

    public static final String                          QUERY_CATEGORIZE_STRING  = "at.cat.string";

    public static final String                          QUERY_CATEGORIZE_BOOLEAN = "at.cat.boolean";

    public static final String                          QUERY_CATEGORIZE_INTEGER = "at.cat.integer";

    public static final String                          QUERY_CATEGORIZE_DOUBLE  = "at.cat.double";

    public static final String                          QUERY_CATEGORIZE_DATE    = "at.cat.date";

    private static final long                           serialVersionUID         = 1L;

    private String                                      name;

    private DatatypeType                                type;

    private boolean                                     userVisible;

    private boolean                                     userEditable;

    private boolean                                     multivalued;

    private boolean                                     compoundMember;

    private Map<String, AttributeTypeDescriptionEntity> descriptions;

    private List<CompoundedAttributeTypeMemberEntity>   members;

    private NodeEntity                                  location;

    private String                                      pluginConfiguration;

    private String                                      pluginName;

    private long                                        attributeCacheTimeoutMillis;


    public AttributeTypeEntity() {

        this(null, null, false, false);
    }

    public AttributeTypeEntity(String name, DatatypeType type, boolean userVisible, boolean userEditable) {

        this.name = name;
        this.type = type;
        this.userVisible = userVisible;
        this.userEditable = userEditable;
        descriptions = new HashMap<String, AttributeTypeDescriptionEntity>();
        members = new LinkedList<CompoundedAttributeTypeMemberEntity>();
    }

    /**
     * FIXME: SOS-416
     * 
     * @return <code>true</code> when this attribute has its value stored locally and is not a plugin.
     */
    @Transient
    public boolean isLocal() {

        if (isExternal())
            return false;

        if (null == getLocation())
            return true;

        SafeOnlineNodeKeyStore olasKeyStore = new SafeOnlineNodeKeyStore();
        if (olasKeyStore.getCertificate().getSubjectX500Principal().getName().equals(getLocation().getCertificateSubject()))
            return true;

        return false;
    }

    @Id
    @Column(name = "name")
    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    public DatatypeType getType() {

        return type;
    }

    public void setType(DatatypeType type) {

        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AttributeTypeEntity rhs = (AttributeTypeEntity) obj;
        return new EqualsBuilder().append(name, rhs.name).isEquals();
    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder().append(name).toHashCode();
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this).append("name", name).toString();
    }

    public boolean isUserVisible() {

        return userVisible;
    }

    public void setUserVisible(boolean userVisible) {

        this.userVisible = userVisible;
    }

    public boolean isUserEditable() {

        return userEditable;
    }

    public void setUserEditable(boolean userEditable) {

        this.userEditable = userEditable;
    }

    /**
     * Marks whether this attribute type allows for multivalued attributes.
     * 
     */
    public boolean isMultivalued() {

        return multivalued;
    }

    public void setMultivalued(boolean multivalued) {

        this.multivalued = multivalued;
    }

    /**
     * Marks whether this attribute type is a member of a compounded attribute type. This field is used to have a performant implementation
     * of the restriction that an attribute type can only participate in one compounded attribute type.
     * 
     */
    public boolean isCompoundMember() {

        return compoundMember;
    }

    public void setCompoundMember(boolean compoundMember) {

        this.compoundMember = compoundMember;
    }

    @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @IndexColumn(name = CompoundedAttributeTypeMemberEntity.MEMBER_SEQUENCE_COLUMN_NAME)
    public List<CompoundedAttributeTypeMemberEntity> getMembers() {

        return members;
    }

    public void setMembers(List<CompoundedAttributeTypeMemberEntity> members) {

        this.members = members;
    }

    /**
     * Adds a member to this compounded attribute type. This method also marks the member attribute type as being such.
     * 
     * @param memberAttributeType
     * @param memberSequence
     * @param required
     */
    public void addMember(AttributeTypeEntity memberAttributeType, int memberSequence, boolean required) {

        if (memberAttributeType.isCompoundMember())
            throw new EJBException("attribute type cannot be member of more than one compounded: " + memberAttributeType.getName());

        getMembers().add(new CompoundedAttributeTypeMemberEntity(this, memberAttributeType, memberSequence, required));
        memberAttributeType.setCompoundMember(true);
    }

    @Transient
    public boolean isCompounded() {

        return false == members.isEmpty();
    }

    @OneToMany(mappedBy = "attributeType")
    @MapKey(name = "language")
    public Map<String, AttributeTypeDescriptionEntity> getDescriptions() {

        return descriptions;
    }

    public void setDescriptions(Map<String, AttributeTypeDescriptionEntity> descriptions) {

        this.descriptions = descriptions;
    }

    /**
     * Returns the OLAS node which holds the attribute values of this type.
     * 
     */
    @ManyToOne
    public NodeEntity getLocation() {

        return location;
    }

    public void setLocation(NodeEntity location) {

        this.location = location;
    }

    public String getPluginConfiguration() {

        return pluginConfiguration;
    }

    public void setPluginConfiguration(String pluginConfiguration) {

        this.pluginConfiguration = pluginConfiguration;
    }

    public String getPluginName() {

        return pluginName;
    }

    public void setPluginName(String pluginName) {

        this.pluginName = pluginName;
    }

    @Transient
    public boolean isExternal() {

        return null != pluginName;
    }

    /**
     * Indicates how long an {@link AttributeCacheEntity} stays valid.
     * 
     */
    public long getAttributeCacheTimeoutMillis() {

        return attributeCacheTimeoutMillis;
    }

    public void setAttributeCacheTimeoutMillis(long attributeCacheTimeoutMillis) {

        this.attributeCacheTimeoutMillis = attributeCacheTimeoutMillis;
    }


    public interface QueryInterface {

        @QueryMethod(QUERY_WHERE_ALL)
        List<AttributeTypeEntity> listAttributeTypes();

        @QueryMethod(QUERY_WHERE_NODE)
        List<AttributeTypeEntity> listAttributeTypes(@QueryParam("location") NodeEntity node);

        @QueryMethod(QUERY_WHERE_VISIBLE)
        List<AttributeTypeEntity> listVisibleAttributeTypes();

        @QueryMethod(QUERY_WHERE_TYPE)
        List<AttributeTypeEntity> listAttributeTypes(@QueryParam("type") DatatypeType datatype);

        @QueryMethod(QUERY_CATEGORIZE_STRING)
        Query createQueryCategorizeString(@QueryParam("application") ApplicationEntity application,
                                          @QueryParam("attributeType") AttributeTypeEntity attributeType);

        @QueryMethod(QUERY_CATEGORIZE_BOOLEAN)
        Query createQueryCategorizeBoolean(@QueryParam("application") ApplicationEntity application,
                                           @QueryParam("attributeType") AttributeTypeEntity attributeType);

        @QueryMethod(QUERY_CATEGORIZE_INTEGER)
        Query createQueryCategorizeInteger(@QueryParam("application") ApplicationEntity application,
                                           @QueryParam("attributeType") AttributeTypeEntity attributeType);

        @QueryMethod(QUERY_CATEGORIZE_DOUBLE)
        Query createQueryCategorizeDouble(@QueryParam("application") ApplicationEntity application,
                                          @QueryParam("attributeType") AttributeTypeEntity attributeType);

        @QueryMethod(QUERY_CATEGORIZE_DATE)
        Query createQueryCategorizeDate(@QueryParam("application") ApplicationEntity application,
                                        @QueryParam("attributeType") AttributeTypeEntity attributeType);

    }
}
