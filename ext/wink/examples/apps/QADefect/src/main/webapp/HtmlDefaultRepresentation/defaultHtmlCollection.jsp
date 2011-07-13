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

<html><%@page import="org.apache.wink.common.model.synd.SyndFeed"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.wink.common.model.synd.SyndLink"%>
<%@page import="org.apache.wink.common.model.synd.SyndCategory"%>
<%@page import="org.apache.wink.common.model.synd.SyndEntry"%>

<%
	HtmlSyndFeedAdapter htmlRepresentationCollectionAdapter = (HtmlSyndFeedAdapter)request.getAttribute(HtmlConstants.RESOURCE_ATTRIBUTE_NAME_REQUEST);
	String id = htmlRepresentationCollectionAdapter.getId();
	String title = htmlRepresentationCollectionAdapter.getTitle();
	String subTitle = htmlRepresentationCollectionAdapter.getSubTitle();	
	String updated =  htmlRepresentationCollectionAdapter.getUpdated();
	String author =  htmlRepresentationCollectionAdapter.getAuthor();
	List<SyndLink> links = htmlRepresentationCollectionAdapter.getLinks();
	List<SyndCategory> categories = htmlRepresentationCollectionAdapter.getCategories();
	int numOfEntries = htmlRepresentationCollectionAdapter.getNumOfEntries();
	HtmlSyndEntryAdapter htmlRepresentationEntryAdapter = null;
	SyndEntry documentResource = null;

	String borderSize = "0";
%>


<head>

<%			
	// Add Open Search Link
	if (links != null && links.size() > 0) {
	String linkHref = "";
	String linkRel = "";
	String linkType = "";
	String linkTitle = "";
	String searchLink = "";
	for(int i = 0; i < links.size(); ++i) { 
	    	SyndLink link = links.get(i);
			linkHref = link.getHref();
			linkRel = link.getRel();
			linkType = link.getType();
			linkTitle = link.getTitle();
			if(AtomConstants.ATOM_REL_SEARCH.equals(linkRel)) {
				if(linkTitle == null) {
					linkTitle = title;
				}
			%>
			<link rel="<%=linkRel%>" type="<%=linkType%>" href="<%=linkHref%>" title="<%=linkTitle%>"/>
			<%
			}
		}
}
%>

<STYLE TYPE="text/css"> 
<%@include file="css/wink-default.css" %>
</STYLE>
</head>
<body>
<SCRIPT type="text/javascript">
<%@include file="js/CollapseExpand.js" %>  
</SCRIPT>
<table width="100%" border="<%=borderSize %>"> 	
<tr>
	<td>
		<table width="100%" border="<%=borderSize %>">
			<tr>
				<td class="page-title page-title-spacing title-line"><%=title%> </td>
			</tr>
			<tr><td></td></tr>
			<tr><td class="field-prompt nowrap-prompt"><%=subTitle%></td></tr>
			<!-- Collection Metadata -->
			<tr>
				<td>
			<%=ExpandableSectionHelper.getFormHeaderHtml("Metadata","metadata",false)%>
					<table>
						
							<tr>
								<td class="field-prompt nowrap-prompt">Id:</td>
								<td class="field-prompt nowrap-prompt"><%=id%> </td>
								<td class="field-prompt nowrap-prompt">&nbsp;</td>
													
								<td class="field-prompt nowrap-prompt">Updated:</td>
								<td class="field-prompt nowrap-prompt"><%=updated%> </td>
							</tr>
						
							<tr>								
								<td class="field-prompt nowrap-prompt">Author:</td>
								<td class="field-prompt nowrap-prompt"><%=author%> </td>
								<td class="field-prompt nowrap-prompt">&nbsp;</td>
							</tr>
								
					</table>
			<%=ExpandableSectionHelper.getFormFooterHtml("metadata",false)%>
				</td>
			</tr>		
			<!-- End Collection Metadata -->
			
			
<%			if (links != null && links.size() > 0) {
		%>
			<!-- Collection Links -->		
			<tr>				
				<td>
					<%=ExpandableSectionHelper.getFormHeaderHtml("Links","linksId",true)%>					
					<table cellpadding="0" cellspacing="0" class="wide-table">
						<tr>
							<td class="non-sorting-column-header">
								Rel
							</td>
							<td class="non-sorting-column-header">
								Link
							</td>
							<td class="non-sorting-column-header">
								Type
							</td>
						</tr>
					<%
					    String linkHref = "";
						String linkRel = "";
						for(int i = 0; i < links.size(); i++) { 
							linkHref = links.get(i).getHref();
							linkRel = links.get(i).getRel();
					%>
						<tr>
							<td class="column-data column-data-border"><%=linkRel%></td>
							<td class="column-data column-data-border">									
								<% //incase of edit link, don't display it linkable
									if(AtomConstants.ATOM_REL_EDIT.equals(linkRel)) { %>
										<%= linkHref%>
									<% } else { %>								
										<a href="<%=linkHref%>"><%=linkHref%></a>
									<% } %>	
							</td>
							<td class="column-data column-data-border"><%
							    if(links.get(i).getType()!=null) {
							%><%=links.get(i).getType()%><%
							    }
							%>&nbsp;</td>
						</tr>
						
					<%
						}
					%>
						
					</table>	
					<%=ExpandableSectionHelper.getFormFooterHtml("linksId",true)%>
				</td>
			</tr>		
			<!-- End Collection Links -->							
		<%
			}
			if (categories != null && categories.size() > 0) {
		%>	
			<!-- Collection Categories -->
			<tr>				
				<td>
					<%=ExpandableSectionHelper.getFormHeaderHtml("Categories","categoryId",true)%>					
					<table border="1">
						<tr>
							<td class="non-sorting-column-header">
								scheme
							</td>
							<td class="non-sorting-column-header">
								term
							</td>
						</tr>
						<%
						    for(int i = 0; i < categories.size(); i++) {
						%>
							<tr>
								<td class="column-data column-data-border"><%=categories.get(i).getScheme()%></td>
								<td class="column-data column-data-border">
						<%
								if (categories.get(i).getTerm() != null) {%>
									<%=categories.get(i).getTerm()%>
						<%		} %>&nbsp;</td>
							</tr>						
						<% } %>					
					</table>	
					<%=ExpandableSectionHelper.getFormFooterHtml("categoryId",true)%>					
				</td>
			</tr>
			<!-- End Collection Categories -->	
		<% } 
			
			if(numOfEntries > 0) { %>
			
			<!-- Entries -->
			<tr>
				<td>
					<table border="<%=borderSize %>" cellpadding="0" cellspacing="0" class="wide-table">						
						<%
							for(int i = 0;i < numOfEntries; i++) { 
								documentResource = htmlRepresentationCollectionAdapter.getSyndEntry(i);
								htmlRepresentationEntryAdapter = new HtmlSyndEntryAdapter(documentResource);
						%>
							<tr>								 
								<td>						
									<%=	ExpandableSectionHelper.getFormHeaderHtml(htmlRepresentationEntryAdapter.getTitle(),"entry"+i,true)%>																											
									<table border="<%=borderSize %>" cellpadding="0" cellspacing="0" class="indented-table wide-table">
										<!-- Entry Metadata -->
										<tr>											
											<td>
												<table border="<%=borderSize %>" cellpadding="0" cellspacing="0">
													<tr>
														<td colspan="4" class="field-prompt nowrap-prompt"><%= htmlRepresentationEntryAdapter.getSummary()%>&nbsp;</td>
													</tr>
													<tr>
														<td class="field-prompt nowrap-prompt">Id: </td>
														<td class="field-prompt nowrap-prompt"><%= htmlRepresentationEntryAdapter.getId()%>&nbsp;</td>
														<td>&nbsp;</td>															
														<td class="field-prompt nowrap-prompt">Updated:</td>
														<td class="field-prompt nowrap-prompt"><%= htmlRepresentationEntryAdapter.getUpdated()%>&nbsp;</td>
														
													</tr>
													<tr>
														<td class="field-prompt nowrap-prompt">Published:</td>
														<td class="field-prompt nowrap-prompt"><%= htmlRepresentationEntryAdapter.getPublished()%>&nbsp;</td>
														</tr>
													<tr>														
														<td class="field-prompt nowrap-prompt">Author:</td>
														<td class="field-prompt nowrap-prompt"><%= htmlRepresentationEntryAdapter.getAuthor()%>&nbsp;</td>											
													</tr>
												</table>
											</td>
										</tr>	
										<!-- End Entry Metadata -->		
										
										<!-- Entry Links -->			
										<tr>														
											<td>
											 <% links = htmlRepresentationEntryAdapter.getLinks(); 
											   	if(links != null && links.size() > 0) { %>											 	
											   		<table border="<%=borderSize %>" cellpadding="0" cellspacing="0" class="wide-table">
														<tr>
															<td class="non-sorting-column-header">
																Rel
															</td>
															<td class="non-sorting-column-header">
																Link
															</td>
															<td class="non-sorting-column-header">
																Type
															</td>							
														</tr>
													<%	
														String linkRel = "";
														for(int j=0;j<links.size();j++) { 
													    linkRel = links.get(j).getRel();
													%>
															<tr>
																<td class="column-data column-data-border"><%=linkRel%></td>
																<td class="column-data column-data-border">
																<% //incase of edit link, don't display it linkable
																	if(AtomConstants.ATOM_REL_EDIT.equals(linkRel)) { %>
																		<%= links.get(j).getHref()%>
																	<% } else { %>			
																		<a href="<%=links.get(j).getHref()%>" ><%= links.get(j).getHref() %></a>
																	<% } %>
																</td>
																<td class="column-data column-data-border"><% if (links.get(j).getType()!=null) { %><%=links.get(j).getType()%><% } %>&nbsp;</td>
															</tr>
													<%  } %>
													</table>
											<%	} %>
											   													   		
											</td>
										</tr>
										<!-- End Entry Links -->
										
										<!-- Entry Categories -->			
										<tr>														
											<td>
											<% categories = htmlRepresentationEntryAdapter.getCategories(); 
											   	if(categories != null && categories.size() > 0) { %>											   
											   		<table border="<%=borderSize %>" cellpadding="0" cellspacing="0" class="wide-table">
														<tr>
															<td class="non-sorting-column-header">
																Scheme
															</td>
															<td class="non-sorting-column-header">
																Term
															</td>
														</tr>
													<%	for(int j=0; j < categories.size(); j++) { %>
															<tr>
																<td class="column-data column-data-border"><%= categories.get(j).getScheme() %></td>
																<td class="column-data column-data-border"><% if (categories.get(j).getTerm() != null) { %><%= categories.get(j).getTerm()%><% } %>&nbsp;</td>
															</tr>
													<%  }
												} %>   		
											   		</table>
											</td>	
										</tr>
										<!-- End Entry Categories -->
										<tr><td>&nbsp;</td></tr>
								</table>																				
							<%=	ExpandableSectionHelper.getFormFooterHtml("entry"+i,true) %>		
							</td>
						</tr>
					<% } %>
						</table>
					</td>
				</tr>												
		<% } %>						
		</table>
	</td>
</tr>
</table>
</body>

<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlSyndFeedAdapter"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlSyndEntryAdapter"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlConstants"%>
<%@page import="org.apache.wink.common.model.atom.AtomConstants"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.ExpandableSectionHelper"%></html>
