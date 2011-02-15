google.load("jquery", "1.3.2");
google.setOnLoadCallback(function() {
	jQuery.getScript("../js/jquery.tablesorter.min.js", function() {
		$("#sparql-results-table").tablesorter(); // { sortList: [[0,0]] }
	});
});