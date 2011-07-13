/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *  
 *******************************************************************************/

package org.apache.wink.example.locking.legacy;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * Defect bean.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DefectType", propOrder = {"id", "name", "description", "created", "author",
                                           "status", "severity", "assignedTo"})
@XmlRootElement(name = "defect")
public class DefectBean {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private String name;
    @XmlElement(required = true)
    private String description;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime", type=javax.xml.datatype.XMLGregorianCalendar.class)
    private Date   created;
    @XmlElement(required = true)
    private String author;
    @XmlElement(required = true)
    private String severity;
    @XmlElement(required = true)
    private String status;
    @XmlElement(required = true)
    private String assignedTo;

    /**
     * Constructor.
     */
    public DefectBean() {
    }

    /**
     * Constructor.
     * 
     * @param id
     * @param name
     * @param description
     * @param created
     * @param author
     * @param severity
     * @param status
     * @param assignedTo
     */
    public DefectBean(String id,
                      String name,
                      String description,
                      Date created,
                      String author,
                      String severity,
                      String status,
                      String assignedTo) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.created = created;
        this.author = author;
        this.severity = severity;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    /**
     * <code>assignedTo</code> getter.
     * 
     * @return the assignedTo
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    /**
     * <code>assignedTo</code> setter.
     * 
     * @param assignedTo the <code>assignedTo</code> to set
     */
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    /**
     * <code>author</code> getter.
     * 
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * <code>author</code> setter.
     * 
     * @param author the <code>author</code> to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * <code>created</code> getter.
     * 
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * <code>created</code> setter.
     * 
     * @param created the <code>created</code> to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * <code>description</code> getter.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <code>description</code> setter.
     * 
     * @param description the <code>description</code> to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <code>id</code> getter.
     * 
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * <code>id</code> setter.
     * 
     * @param id the <code>id</code> to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * <code>value</code> getter.
     * 
     * @return the value
     */
    public String getName() {
        return name;
    }

    /**
     * <code>name</code> setter.
     * 
     * @param name the <code>name</code> to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <code>severity</code> getter.
     * 
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * <code>severity</code> setter.
     * 
     * @param severity the <code>severity</code> to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * <code>status</code> getter.
     * 
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * <code>status</code> setter.
     * 
     * @param status the <code>status</code> to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * hashCode and equals are overridden in order to identify the object based
     * on the actual object's data
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assignedTo == null) ? 0 : assignedTo.hashCode());
        result = prime * result + ((author == null) ? 0 : author.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    /**
     * hashCode and equals are overridden in order to identify the object based
     * on the actual object's data
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DefectBean other = (DefectBean)obj;
        if (assignedTo == null) {
            if (other.assignedTo != null)
                return false;
        } else if (!assignedTo.equals(other.assignedTo))
            return false;
        if (author == null) {
            if (other.author != null)
                return false;
        } else if (!author.equals(other.author))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (severity == null) {
            if (other.severity != null)
                return false;
        } else if (!severity.equals(other.severity))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        return true;
    }

}
