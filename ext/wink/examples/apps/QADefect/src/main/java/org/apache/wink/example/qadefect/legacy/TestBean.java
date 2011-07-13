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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TestType", propOrder = {"id", "name", "description", "created", "author", "status"})
@XmlRootElement(name = "test")
public class TestBean {

    @XmlElement(required = true)
    private String           id;
    @XmlElement(required = true)
    private String           name;
    @XmlElement(required = true)
    private String           description;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    private Date             created;
    @XmlElement(required = true)
    private String           author;
    @XmlElement(required = true)
    private String           status;
    @XmlTransient
    private List<DefectBean> defects;

    public TestBean() {

    }

    public TestBean(String id,
                    String name,
                    String description,
                    Date created,
                    String author,
                    String status,
                    List<DefectBean> defects) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.created = created;
        this.author = author;
        this.status = status;
        this.defects = defects;

        if (defects != null) {
            for (DefectBean bean : defects) {
                bean.addTest(this);
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DefectBean> getDefects() {
        return defects;
    }

    public void setDefects(List<DefectBean> defects) {
        this.defects = defects;
    }

    public void addDefect(DefectBean defect) {
        if (this.defects == null) {
            this.defects = new ArrayList<DefectBean>();
        }
        this.defects.add(defect);
    }
}
