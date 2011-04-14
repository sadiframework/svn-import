<html>
  <head>
    <title>SPARQL assist</title>
    <link rel="stylesheet" type="text/css" href="http://sadiframework.org/style/2011-03-14/style.css">
	<link rel="stylesheet" type="text/css" href="style/jquery.autocomplete.css">
	<link rel="stylesheet" type="text/css" href="style/jquery.sparqlassist.css">
	<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js"></script>
	<script type="text/javascript" src="js/jquery.caret.js"></script>
	<script type="text/javascript" src="js/jquery.autocomplete.js"></script>
	<script type="text/javascript" src="js/jquery.sparqlassist.js"></script>
	<script type="text/javascript">
	  $(function() {
	    $("#queryBox").sparqlassist({
               initNamespaces : "resources/LSRN.json",
               initPredicates : "resources/props.json",
//              initIndividuals : {
//                                       url : "http://remote.example.com/needs-jsonp",
//                                      data : { key: "value" },
//                                  dataType : { "jsonp" }
//                                },
             remotePredicates : "autocomplete?category=predicates",
             remoteNamespaces : "autocomplete?category=namespaces",
            remoteIndividuals : "autocomplete?category=individuals"
        });
	  });
	</script>
  </head>
  <body>
    <div id="outer-frame">
      <div id="inner-frame">
        <div id="header">
          <a href="http://sadiframework.org/">
            <h1>SADI - Find. Integrate. Analyze.</h1>
          </a>
        </div>
        <div id="nav"></div>
        <div id="content">
        
    <h2>SPARQL assist</h2>
    <form><!-- action="http://example.com/sparql-endpoint" -->
      <label for="queryBox">Type your SPARQL query into the box below</label>
      <br/>
      <textarea id="queryBox" name="query" cols="80" rows="12"></textarea>
      <input type="submit"/>
    </form>
    <h2>Configuration</h2>
    <p>If you view the source of this web page, you'll see how SPARQL assist is configured.
     This example is very simple.  It looks like this:
    <pre>$(function() { <br>&nbsp;&nbsp;&nbsp;&nbsp;$(&quot;#queryBox&quot;).sparqlassist({ <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;initNamespaces : &quot;resources/LSRN.json&quot;, <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;initPredicates : &quot;resources/props.json&quot; <br>&nbsp;&nbsp;&nbsp;&nbsp;}); <br>});</pre>
    <p>This configuration reads an initial vocabulary of properties from
     <span class='code'>resources/props.json</span> and an initial vocabulary
     of namespaces from <span class='code'>resources/LSRN.json</span>. Each of
     these files contains a JSON array containing objects expected to have the
     following fields:</p>
    <pre>{ <br>&nbsp;&nbsp;&nbsp;&nbsp;label : &quot;this is the text that will appear in the drop-down list&quot;, <br>&nbsp;&nbsp;&nbsp;&nbsp;value : &quot;this is the value that will appear in the text area if this term is selected&quot;, <br>&nbsp;&nbsp;&nbsp;&nbsp;uri : &quot;this is the URI of the term&quot;, <br>&nbsp;&nbsp;&nbsp;&nbsp;description: &quot;this is a longer description of the term&quot; <br>}</pre>
    <p>Of these fields, <span class='code'>label</span> and one of
     <span class='code'>value</span> or <span class='code'>uri</span> are required.
     If <span class='code'>value</span> is missing, it will be computed from the URI.</p>
    <p>The complete list of configuration options follows:</p>
    <dl style='background: #eee; padding: .25em;'>
      <dt class='code'>initNamespaces</dt>
        <dd>an initial list of namespaces (for use when completing PREFIX statements)</dd>
      <dt class='code'>remoteNamespaces</dt>
        <dd>a URL string or a jQuery.ajax() options object used to fetch additional
         namespaces once the initial list has been displayed. See below for more
         information about remote data.</dd>
      <dt class='code'>decorateNamespace</dt>
        <dd>a function that will be called on each namespace object in the initial
         and remote namespace data, allowing you to modify incoming data that is not
         in the expected format.</dd>
      <dt class='code'>initPredicates</dt>
        <dd>as<span class='code'>initNamespaces</span>above, but used when completing
         the predicate position of WHERE triple patterns.</dd>
      <dt class='code'>remotePredicates</dt>
        <dd>as<span class='code'>remoteNamespaces</span>above, but for predicates</dd>
      <dt class='code'>decoratePredicate</dt>
        <dd>as<span class='code'>decorateNamespaces</span>above, but for predicates</dd>
      <dt class='code'>initIndividuals</dt>
        <dd>as<span class='code'>initNamespaces</span>above, but used when completing
         the subject or object position of WHERE triple patterns.</dd>
      <dt class='code'>remoteIndividuals</dt>
        <dd>as<span class='code'>remoteNamespaces</span>above, but for predicates</dd>
      <dt class='code'>decorateIndividuals</dt>
        <dd>as<span class='code'>decorateNamespaces</span>above, but for predicates</dd>
      <dt>
    </dl>
    <h2>Remote data</h2>
    <p>SPARQL assist uses a two-step approach, first presenting local client-side data
     synchronously, then updating the presentation with asynchronous remote data. When
     the remote data call finishes, a new class <span class='code'>sparqlassist-confirmed</span>
     is added to those existing local entries that also appeared by the remote data
     source; similarly, a new class <span class='code'>sparqlassist-unconfirmed</span>
     is added to those local entries that did not appear in the remote data. In addition,
     new entries in the remote data are added to the list.</p>
    <p>The various <span class='code'>remote*</span> options can be either simple URLs,
     populated objects that will be used as the options to a jQuery.ajax() call, or arrays
     of either. The AJAX options object can be useful in particular if the remote data source
     is not on the domain serving the SPARQL query web page, as the call will have to be made
     with JSONP. In either case, the following parameters are sent to the remote URL:</p>
    <dl style='background: #eee; padding: .25em;'>
      <dt class='code'>query</dt>
        <dd>The query word to autocomplete.</dd>
      <dt class='code'>sparql</dt>
        <dd>The entire SPARQL query as entered so far</dd>
      <dt class='code'>caret</dt>
        <dd>The position of the cursor in the SPARQL query</dd>
      <dt class='code'>id</dt>
        <dd>An ID that should be unique to this particular instance of the web page.
         Collisions are possible, but unlikely.</dd>
    </dl>
    <h3>More information</h3>
    <p>That's really it.  If you have other questions or bug reports, post them to the
     <a href='http://groups.google.com/group/sadi-discuss'>SADI Google group</a> or 
     subscribe to the group and email them.</p>
        </div> <!-- content -->
        <div id="footer"></div>
      </div> <!-- inner-frame -->
    <div> <!-- outer-frame -->
  </body>
</html>
