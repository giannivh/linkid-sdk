/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.entity;

import static net.link.safeonline.entity.CompoundedAttributeTypeMemberEntity.DELETE_WHERE_PARENT;
import static net.link.safeonline.entity.CompoundedAttributeTypeMemberEntity.QUERY_PARENT;
import static net.link.safeonline.entity.CompoundedAttributeTypeMemberEntity.QUERY_WHERE_MEMBER;

import java.io.Serializable;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import net.link.safeonline.jpa.annotation.QueryMethod;
import net.link.safeonline.jpa.annotation.QueryParam;
import net.link.safeonline.jpa.annotation.UpdateMethod;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Index;


@Entity
@Table(name = "comp_attribute_member")
@NamedQueries( {
        @NamedQuery(name = QUERY_PARENT, query = "SELECT catm.parent FROM CompoundedAttributeTypeMemberEntity AS catm "
                + "WHERE catm.member = :member"),
        @NamedQuery(name = QUERY_WHERE_MEMBER, query = "SELECT catm FROM CompoundedAttributeTypeMemberEntity AS catm "
                + "WHERE catm.member = :member"),
        @NamedQuery(name = DELETE_WHERE_PARENT, query = "DELETE FROM CompoundedAttributeTypeMemberEntity AS catm "
                + "WHERE catm.parent = :parent") })
public class CompoundedAttributeTypeMemberEntity implements Serializable {

    private static final long               serialVersionUID    = 1L;

    public static final String              QUERY_PARENT        = "cat.parent";

    public static final String              QUERY_WHERE_MEMBER  = "cat.member";

    public static final String              DELETE_WHERE_PARENT = "cat.delete.parent";

    private CompoundedAttributeTypeMemberPK pk;

    private AttributeTypeEntity             parent;

    private AttributeTypeEntity             member;

    private int                             memberSequence;

    private boolean                         required;


    public CompoundedAttributeTypeMemberEntity() {

        // empty
    }

    public CompoundedAttributeTypeMemberEntity(AttributeTypeEntity parent, AttributeTypeEntity member, int memberSequence, boolean required) {

        this.parent = parent;
        this.member = member;
        this.memberSequence = memberSequence;
        this.required = required;
        pk = new CompoundedAttributeTypeMemberPK(this.parent, this.member);
    }

    @EmbeddedId
    @AttributeOverrides( { @AttributeOverride(name = "parent", column = @Column(name = PARENT_COLUMN_NAME)),
            @AttributeOverride(name = "member", column = @Column(name = MEMBER_COLUMN_NAME)) })
    public CompoundedAttributeTypeMemberPK getPk() {

        return pk;
    }

    public void setPk(CompoundedAttributeTypeMemberPK pk) {

        this.pk = pk;
    }


    public static final String PARENT_COLUMN_NAME = "parent_attribute_type";


    @ManyToOne(optional = false)
    @JoinColumn(name = PARENT_COLUMN_NAME, insertable = false, updatable = false)
    public AttributeTypeEntity getParent() {

        return parent;
    }

    public void setParent(AttributeTypeEntity parent) {

        this.parent = parent;
    }


    public static final String MEMBER_COLUMN_NAME = "member_attribute_type";


    @ManyToOne(optional = false)
    @JoinColumn(name = MEMBER_COLUMN_NAME, insertable = false, updatable = false)
    @Index(name = "memberIndex")
    public AttributeTypeEntity getMember() {

        return member;
    }

    public void setMember(AttributeTypeEntity member) {

        this.member = member;
    }


    public static final String MEMBER_SEQUENCE_COLUMN_NAME = "memberSequence";


    @Column(name = MEMBER_SEQUENCE_COLUMN_NAME)
    public int getMemberSequence() {

        return memberSequence;
    }

    public void setMemberSequence(int memberSequence) {

        this.memberSequence = memberSequence;
    }

    /**
     * Marks whether the member is a required part of the compounded attribute type.
     * 
     */
    public boolean isRequired() {

        return required;
    }

    public void setRequired(boolean required) {

        this.required = required;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (null == obj)
            return false;
        if (false == obj instanceof CompoundedAttributeTypeMemberEntity)
            return false;
        CompoundedAttributeTypeMemberEntity rhs = (CompoundedAttributeTypeMemberEntity) obj;
        return new EqualsBuilder().append(pk, rhs.pk).isEquals();
    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder().append(pk).toHashCode();
    }


    public interface QueryInterface {

        @QueryMethod(value = QUERY_PARENT, nullable = true)
        AttributeTypeEntity findParentAttribute(@QueryParam("member") AttributeTypeEntity memberAttributeType);

        @QueryMethod(QUERY_WHERE_MEMBER)
        List<CompoundedAttributeTypeMemberEntity> listMemberEntries(@QueryParam("member") AttributeTypeEntity memberAttributeType);

        @UpdateMethod(DELETE_WHERE_PARENT)
        int deleteWhereParent(@QueryParam("parent") AttributeTypeEntity parent);
    }
}
