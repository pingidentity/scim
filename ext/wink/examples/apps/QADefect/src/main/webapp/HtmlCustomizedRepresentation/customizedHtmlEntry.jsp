<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
    
     http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@page import="java.util.List"%>

<%@page import="org.apache.wink.example.qadefect.legacy.DefectBean"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlConstants"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlSyndEntryAdapter"%>
<%@page import="org.apache.wink.example.qadefect.resources.DefectAsset"%>
<%@page import="org.apache.wink.example.qadefect.legacy.TestBean"%><html>
<head>
<STYLE TYPE="text/css"> 
<%@include file="css/customized.css" %>  
</STYLE>
</head>
	<body>
	<%
			DefectBean defectBean = (DefectBean)request.getAttribute(DefectAsset.CUSTOMIZED_JSP_ATTR);
			List<TestBean> testList = defectBean.getTests();
			
			String borderSize="0";
	%>
		<table width="100%" border="<%=borderSize %>"> 	
			<tr>
				<td class="page-title"><u>QC application - Defect Details</u> (customized application)</td>
			</tr> 
			<tr><td>&nbsp;</td></tr>
			<tr>
				<td class="page-title"><%= defectBean.getName()%></td>
			</tr> 
			<tr>
				<td>
					<table cellpadding="0" cellspacing="0">
						<tr>
								<td colspan="2" class="column-data column-data-border">&nbsp;</td>
						</tr>
						<tr>		
								<td class="column-data-blue column-data-border-left-right">Id</td>
								<td class="column-data column-data-border-right"><%= defectBean.getId()%></td>
						</tr>
						<tr>		
								<td class="column-data-blue column-data-border-left-right">Description</td>
								<td class="column-data column-data-border-right"><%= defectBean.getDescription()%></td>
						</tr>
						<tr>
							<td class="column-data-blue column-data-border-left-right">Severity</td>
							<td class="column-data column-data-border-right"> <%= defectBean.getSeverity()%></td>
						</tr>
						<tr>
							<td class="column-data-blue column-data-border-left-right">Status</td>
							<td class="column-data column-data-border-right">  <%= defectBean.getStatus()%></td>
						</tr>
						<tr>
							<td class="column-data-blue column-data-border-left-right">Created on</td>
							<td class="column-data column-data-border-right"><%= defectBean.getCreated()%></td>
						</tr>
						<tr>
							<td class="column-data-blue column-data-border-left-right">Author</td>
							<td class="column-data column-data-border-right"><%= defectBean.getAuthor()%></td>
						</tr>
						<tr>
							<td class="column-data-blue column-data-border-left-right">Assigned to</td>
							<td class="column-data column-data-border-right"><%= defectBean.getAssignedTo()%></td>
						</tr>
						<tr>
							<td class="column-data-blue column-data-border-left-right">Tests</td>
							<td class="column-data column-data-border-right">
								<% if(testList != null && defectBean.getTests().size()>0) { %>				
									 The defect has <%= defectBean.getTests().size()%> 
									<%	 if(defectBean.getTests().size() == 1) { %>
										 	test									 
										<% } else { %>
											tests
										<% } %>				
								<%} else { %>
									The defect has no tests available.
								<% } %>
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
	</body>
</html>