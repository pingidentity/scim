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

package org.apache.wink.example.qadefect.legacy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
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
    private String         id;
    @XmlElement(required = true)
    private String         name;
    @XmlElement(required = true)
    private String         description;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    private Date           created;
    @XmlElement(required = true)
    private String         author;
    @XmlElement(required = true)
    private String         severity;
    @XmlElement(required = true)
    private String         status;
    @XmlElement(required = true)
    private String         assignedTo;
    @XmlTransient
    private List<TestBean> tests;
    @XmlTransient
    private String         pathToAttachment;

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
     * @param pathToAttachment
     */
    public DefectBean(String id,
                      String name,
                      String description,
                      Date created,
                      String author,
                      String severity,
                      String status,
                      String assignedTo,
                      List<TestBean> tests,
                      String pathToAttachment) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.created = created;
        this.author = author;
        this.severity = severity;
        this.status = status;
        this.assignedTo = assignedTo;
        this.tests = tests;
        this.pathToAttachment = pathToAttachment;
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

    public List<TestBean> getTests() {
        return tests;
    }

    public void setTests(List<TestBean> tests) {
        this.tests = tests;
    }

    public void addTest(TestBean test) {
        if (this.tests == null) {
            this.tests = new ArrayList<TestBean>();
        }
        this.tests.add(test);
    }

    /**
     * @return the path to the defect's attachment
     */
    public String getPathToAttachment() {
        return this.pathToAttachment;
    }

    /**
     * Sets the path to the defect's attachment
     * 
     * @param pathToAttachment
     */
    public void setPathToAttachment(String pathToAttachment) {
        this.pathToAttachment = pathToAttachment;
    }
}
