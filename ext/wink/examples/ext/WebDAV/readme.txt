QADefect WebDAV Example
===============================================================================

Description 
===============================================================================
- WebDAV extension to QADefect example
- WebDAVDefectsResource handles WebDAV methods for collection of
  defects (it's read/write collection) and single defect
- DAVTestsResource handles WebDAV methods for collection of tests
  (it's read only collection) and single test
- collections of defects and tests can be accessed in a WebDAV client pointing
  to http://localhost:8080/WebDAV/rest address
- built-in Windows client - Web Folders (My Network Places / Add Network Place)
  can be used
- alternative clients like WebDAV plug-in for Total Commander
  (http://ghisler.fileburst.com/fsplugins/webdav.zip) can be used as well
- make sure you have installed Windows update: KB907306
  (http://support.microsoft.com/?kbid=907306) for Windows Web Folders

Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
