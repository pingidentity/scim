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
<%@page import="org.apache.wink.common.model.synd.SyndLink"%>
<%@page import="org.apache.wink.common.model.synd.SyndCategory"%>

<%@page import="org.apache.wink.server.internal.providers.entity.html.ExpandableSectionHelper"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlSyndEntryAdapter"%>
<%@page import="org.apache.wink.server.internal.providers.entity.html.HtmlConstants"%>
<%@page import="org.apache.wink.common.model.atom.AtomConstants"%><html>
<head>
<STYLE TYPE="text/css">
<%@include file="css/wink-default.css"%>
</STYLE>
</head>

<body>
<SCRIPT type="text/javascript">
<%@include file="js/CollapseExpand.js" %>  
</SCRIPT>

<%
    HtmlSyndEntryAdapter htmlRepresentationEntryAdapter = (HtmlSyndEntryAdapter)request.getAttribute(HtmlConstants.RESOURCE_ATTRIBUTE_NAME_REQUEST);

    String id = htmlRepresentationEntryAdapter.getId();
    String title = htmlRepresentationEntryAdapter.getTitle();
    String updated = htmlRepresentationEntryAdapter.getUpdated();
    String author = htmlRepresentationEntryAdapter.getAuthor();
    String summary = htmlRepresentationEntryAdapter.getSummary();
    String published = htmlRepresentationEntryAdapter.getPublished();
    List<SyndLink> links = htmlRepresentationEntryAdapter.getLinks();
    List<SyndCategory> categories = htmlRepresentationEntryAdapter.getCategories();
    String content = htmlRepresentationEntryAdapter.getContent();
    boolean isContentXml = htmlRepresentationEntryAdapter.isContentXml();
    String borderSize = "0";
%>
<table width="100%" border="<%=borderSize%>">
	<tr>
		<td>
		<table width="100%" border="<%=borderSize%>">
			<tr>
				<td class="page-title page-title-spacing title-line"><%=title%>
				</td>
			</tr>
			<tr>
				<td></td>
			</tr>
			<tr>
				<td class="field-prompt nowrap-prompt"><%=summary%></td>
			</tr>
			<!-- Entry Metadata -->
			<tr>
				<td><%=ExpandableSectionHelper.getFormHeaderHtml("Metadata", "metadata", false)%>
				<table border="<%=borderSize%>">

					<tr>
						<td class="field-prompt nowrap-prompt">Id:</td>
						<td class="field-prompt nowrap-prompt"><%=id%></td>
						<td class="field-prompt nowrap-prompt">&nbsp;</td>

						<td class="field-prompt nowrap-prompt">Updated:</td>
						<td class="field-prompt nowrap-prompt"><%=updated%></td>
						<td class="field-prompt nowrap-prompt">&nbsp;</td>
					</tr>

					<tr>
						<td class="field-prompt nowrap-prompt">Published:</td>
						<td class="field-prompt nowrap-prompt"><%=published%></td>
						<td class="field-prompt nowrap-prompt">&nbsp;</td>
					</tr>

					<tr>
						<td class="field-prompt nowrap-prompt">Author:</td>
						<td class="field-prompt nowrap-prompt"><%=author%></td>
						<td class="field-prompt nowrap-prompt">&nbsp;</td>
					</tr>

				</table>
				<%=ExpandableSectionHelper.getFormFooterHtml("metadata", false)%></td>
			</tr>
			<!-- End Entry Metadata -->

			<!-- Entry Links -->
			<%
			    if (links != null && links.size() > 0) {
			%>

			<tr>
				<td><%=ExpandableSectionHelper.getFormHeaderHtml("Links", "linksId", true)%>
				<table border="<%=borderSize%>" cellpadding="0" cellspacing="0"
					class="wide-table">
					<tr>
						<td class="non-sorting-column-header">Rel</td>
						<td class="non-sorting-column-header">Link</td>
						<td class="non-sorting-column-header">Type</td>
					</tr>
					<%
					    String linkHref = "";
					        String linkRel = "";

					        for (int i = 0; i < links.size(); i++) {
					            linkHref = links.get(i).getHref();
					            linkRel = links.get(i).getRel();
					%>
					<tr>
						<td class="column-data column-data-border"><%=linkRel%></td>
						<td class="column-data column-data-border">
						<%
						    //incase of edit link, don't display it linkable
						            if (AtomConstants.ATOM_REL_EDIT.equals(linkRel)) {
						%> <%=linkHref%>
						<%
						    } else {
						%> <a href="<%=linkHref%>"><%=linkHref%></a> <%
     }
 %>
						</td>
						<td class="column-data column-data-border">
						<%
						    if (links.get(i).getType() != null) {
						%><%=links.get(i).getType()%>
						<%
						    }
						%>&nbsp;</td>
					</tr>

					<%
					    }
					%>

				</table>
				<%=ExpandableSectionHelper.getFormFooterHtml("linksId", false)%></td>
			</tr>
			<%
			    }
			%>
			<!-- End Entry Links -->

			<!-- Entry Categories -->
			<%
			    if (categories != null && categories.size() > 0) {
			%>
			<tr>
				<td><%=ExpandableSectionHelper.getFormHeaderHtml("Categories", "categoryId", true)%>
				<table class="wide-table">
					<tr>
						<td class="non-sorting-column-header">scheme</td>
						<td class="non-sorting-column-header">term</td>
					</tr>
					<%
					    for (int i = 0; i < categories.size(); i++) {
					%>
					<tr>
						<td class="column-data column-data-border"><%=categories.get(i).getScheme()%></td>
						<td class="column-data column-data-border">
						<%
						    if (categories.get(i).getTerm() != null) {
						%><%=categories.get(i).getTerm()%>
						<%
						    }
						%>&nbsp;</td>
					</tr>
					<%
					    }
					%>

				</table>
				<%=ExpandableSectionHelper.getFormFooterHtml("categoryId", true)%>
				</td>
			</tr>
			<%
			    }
			%>
			<!-- End Entry Categories -->

			<!-- Entry Content -->
			<%
			    if (content != null && !content.equals("")) {
			%>
			<tr>
				<td align="left"><%=ExpandableSectionHelper.getFormHeaderHtml("Content", "contentId", true)%>
				<table class="wide-table">
					<tr>
						<td><%=content%></td>

					</tr>
				</table>
				<%=ExpandableSectionHelper.getFormFooterHtml("contentId", true)%></td>
			</tr>
			<%
			    }
			%>
			<!-- End Entry Content -->
		</table>
		</td>
	</tr>


</table>
</body>
</html>