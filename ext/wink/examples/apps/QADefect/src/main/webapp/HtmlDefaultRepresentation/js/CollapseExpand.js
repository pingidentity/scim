/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

function CollapseExpand(id) {
  this.myId = id;
}

CollapseExpand.prototype.collapseExpand = function() {
  var elem = document.getElementById(this.myId);
  var imageElem = document.getElementById(this.myId + "_div");
  
  if(elem != null) {
    var display = elem.style.display;
    if(display != null && display != 'undefined' && display == 'none') {
      // the element is not visible, we need to expand
      elem.style.display = 'block';      
      imageElem.innerHTML = "-";
      return;
    }else{
      elem.style.display = 'none';
      imageElem.innerHTML = "+";
    }
  }
}

CollapseExpand.prototype.collapse = function() {
	var elementToCollapse = document.getElementById(this.myId);
	elementToCollapse.style.display = 'none';  
}
