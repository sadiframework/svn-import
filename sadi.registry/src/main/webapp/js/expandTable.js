jQuery.fn.expandTable = function(callback) {

	var expandedHandle = "&ndash;";
	var collapsedHandle ="+";
	var handleClass = "expandTable-handle";
	var expandedClass = "expandTable-expanded";
	var collapsedClass = "expandTable-collapsed";
	var rowBodyClass = "expandTable-rowBody";
	var loadingClass = "expandTable-loading";
	
	function expandRow(row) {
		row.addClass(expandedClass);
		row.removeClass(collapsedClass);
		
		var rowBody = row.next("tr");
		if (!rowBody.hasClass(rowBodyClass)) {
			// we haven't added it yet...
			var cols = row.children("td").length - 1;
			row.after("<tr class='" + rowBodyClass +"'><td></td><td class='" + loadingClass + "' colspan='" + cols + "'></td></tr>");
			callback(row.next("tr").children("td").slice(1,2));
		} else {
			rowBody.show();
		}
	}
	
	function collapseRow(row) {
		row.addClass(collapsedClass);
		row.removeClass(expandedClass);
		
		var rowBody = row.next("tr");
		if (rowBody.hasClass(rowBodyClass)) {
			rowBody.hide();
		}
	}
	
	function expansionHandler() {
		var handle = $(this);
		var row = handle.parent("tr");
		if (row.hasClass(expandedClass)) {
			handle.html(collapsedHandle);
			collapseRow(row)
		} else {
			handle.html(expandedHandle);
			expandRow(row);
		}
    }
	
    this.each(function() {
    	var table = $(this);
    	table.find("thead tr").prepend("<td></td>");
    	table.find("tbody tr").prepend("<td class='" + handleClass + "'>" + collapsedHandle + "</td>");
    	table.find("tbody td." + handleClass).click(expansionHandler);
    });

};